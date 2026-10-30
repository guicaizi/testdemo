package com.example.testdemo.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.widget.ProgressBar
import com.example.testdemo.R

/**
 * 指纹录入环（Demo 版）：
 * 单条进度弧，弧线中心对准圆盘灰色描边线（圆盘半径）；sweep 渐变亮绿 → 深绿；
 * 弧线边缘通过同半径 shadowLayer 做轻微发光，不产生第二条环。
 */
class WatchRingProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal
) : ProgressBar(context, attrs, defStyleAttr) {

    private val strokeWidthPx = context.resources.getDimension(R.dimen.fp_demo_ring_thickness)
    private val glowRadiusPx = context.resources.getDimension(R.dimen.fp_demo_glow_radius)
    private val discSizePx = context.resources.getDimension(R.dimen.fp_demo_disc_size)
    private val glowColor = context.getColor(R.color.fp_demo_glow)
    private val arcStartColor = context.getColor(R.color.fp_demo_arc_start)
    private val arcEndColor = context.getColor(R.color.fp_demo_arc_end)

    // 发光底层：与实线弧同半径，shadowLayer 贴边发轻微光
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokeWidthPx
        color = glowColor
        setShadowLayer(glowRadiusPx, 0f, 0f, glowColor)
    }

    // 实线渐变层
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokeWidthPx
    }

    private val arcBounds = RectF()
    private val gradientMatrix = Matrix()

    override fun onDraw(canvas: Canvas) {
        val maxValue = max
        val value = progress
        if (maxValue > 0 && value > 0 && width > 0 && height > 0) {
            // 弧线中心对准圆盘灰色描边线：inset = (视图尺寸 - 圆盘尺寸) / 2
            val inset = (width - discSizePx) / 2f
            arcBounds.set(inset, inset, width - inset, height - inset)

            val sweep = 360f * value / maxValue
            val cx = width / 2f
            val cy = height / 2f

            // 渐变从弧起点 12 点方向（-90°）开始，顺时针亮绿 → 深绿
            gradientMatrix.setRotate(-90f, cx, cy)
            arcPaint.shader = SweepGradient(
                cx, cy,
                intArrayOf(arcStartColor, arcEndColor),
                null
            ).apply { setLocalMatrix(gradientMatrix) }

            // 先发光弧再实线弧：同一条弧，边缘轻微发光，单环
            canvas.drawArc(arcBounds, -90f, sweep, false, glowPaint)
            canvas.drawArc(arcBounds, -90f, sweep, false, arcPaint)
        }
    }
}