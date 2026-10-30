package com.example.testdemo.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.asin
import kotlin.math.sqrt

/**
 * prize add by yanliang  20261030-start
 * 底部主按钮背景：上边缘是「从半径 [arcRadius] 的圆上裁下来」的一段圆弧 —— 左右对称、中间最高；
 * 两侧为直边，下边缘沿用原胶囊圆角 [bottomRadius]。
 * 填充沿用设计稿的竖向渐变：深绿 [colorTop] → 亮绿 [colorBottom]（在 [gradientStop] 处到达亮绿）。
 * 之所以不用 shape xml，是因为圆弧只能靠自定义 Path 画出来，圆角矩形做不了这种曲率。
 */
class ArcTopButtonDrawable(
    private val arcRadius: Float,
    private val bottomRadius: Float,
    private val colorTop: Int,
    private val colorBottom: Int,
    private val gradientStop: Float = 0.55f,
) : Drawable() {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shader: Shader? = null

    override fun onBoundsChange(bounds: Rect) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return

        val halfW = w / 2f
        // 半径小于半宽时退化直线，避免 sqrt 负数
        val radius = if (arcRadius > halfW) arcRadius else 0f
        // 弧顶到底部弦的垂距（sagitta）
        val sag = if (radius > 0f) radius - sqrt(radius * radius - halfW * halfW) else 0f
        // 圆弧对应的半张角
        val halfAngle = if (radius > 0f) Math.toDegrees(asin(halfW / radius).toDouble()).toFloat() else 0f

        val br = minOf(bottomRadius, h / 2f, halfW)

        path.reset()
        if (radius > 0f) {
            // 弧顶过 (halfW, 0)，故圆心在 (halfW, radius)；左端点落在 (0, sag) 上
            val oval = RectF(halfW - radius, 0f, halfW + radius, 2f * radius)
            path.moveTo(0f, sag)
            path.arcTo(oval, 270f - halfAngle, 2f * halfAngle, false)
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(w, 0f)
        }
        // 右侧直边 → 右下圆角 → 底边 → 左下圆角 → 左侧直边
        path.lineTo(w, h - br)
        path.arcTo(RectF(w - 2f * br, h - 2f * br, w, h), 0f, 90f, false)
        path.lineTo(br, h)
        path.arcTo(RectF(0f, h - 2f * br, 2f * br, h), 90f, 90f, false)
        path.close()

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(colorTop, colorBottom, colorBottom),
            floatArrayOf(0f, gradientStop, 1f),
            Shader.TileMode.CLAMP,
        ).also { shader = it }
    }

    override fun draw(canvas: Canvas) {
        if (path.isEmpty) return
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
// prize add by yanliang for 按钮上边缘改为圆弧裁切, 20261030-end
