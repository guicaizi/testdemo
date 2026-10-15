package com.example.testdemo.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.testdemo.R
import kotlin.math.min

/**
 * 横向胶囊亮度滑块。
 *
 * 深色轨道与白色填充共用同一条胶囊路径：填充右缘等于手指横坐标，
 * 再与胶囊求交，因此拖动时跟手，且白色不会画出胶囊边框。
 */
class BrightnessCapsuleSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.qs_control_panel_pill_track_dark)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.white)
    }

    private val capsulePath = Path()
    private val slicePath = Path()
    private val contentRect = RectF()

    private val iconSizePx =
        resources.getDimensionPixelSize(R.dimen.qs_control_panel_volume_icon_size)
    private val iconInsetPx =
        resources.getDimensionPixelSize(R.dimen.qs_control_panel_volume_icon_inset)
    private val iconDrawable =
        requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_control_panel_brightness_level)).mutate()

    /** 0 为最暗，1 为最亮。拖动时按手指在胶囊内的位置直接赋值。 */
    var progressFraction: Float = 0.5f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field == clamped) return
            field = clamped
            applyIconLevel()
            invalidate()
        }

    init {
        isClickable = true
        isFocusable = true
        applyIconLevel()
    }

    override fun onDraw(canvas: Canvas) {
        contentRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )
        if (contentRect.width() <= 0f || contentRect.height() <= 0f) return

        val radius = min(contentRect.width(), contentRect.height()) / 2f
        val fraction = progressFraction.coerceIn(0f, 1f)
        val fillWidth = contentRect.width() * fraction
        val whiteLeft: Float
        val whiteRight: Float
        if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            whiteRight = contentRect.right
            whiteLeft = contentRect.right - fillWidth
        } else {
            whiteLeft = contentRect.left
            whiteRight = contentRect.left + fillWidth
        }

        if (fraction <= 0f) {
            canvas.drawRoundRect(contentRect, radius, radius, trackPaint)
        } else if (fraction >= 1f) {
            canvas.drawRoundRect(contentRect, radius, radius, fillPaint)
        } else {
            drawCapsuleSlice(canvas, contentRect, radius, contentRect.left, whiteLeft, trackPaint)
            drawCapsuleSlice(canvas, contentRect, radius, whiteLeft, whiteRight, fillPaint)
            drawCapsuleSlice(canvas, contentRect, radius, whiteRight, contentRect.right, trackPaint)
        }
        drawIcon(canvas)
    }

    /**
     * 只绘制胶囊与 [left, right] 的交集，填充像素不会超出胶囊轮廓。
     */
    private fun drawCapsuleSlice(
        canvas: Canvas,
        capsule: RectF,
        radius: Float,
        left: Float,
        right: Float,
        paint: Paint
    ) {
        if (right - left <= 0f) return
        capsulePath.reset()
        capsulePath.addRoundRect(capsule, radius, radius, Path.Direction.CW)
        slicePath.reset()
        slicePath.addRect(left, capsule.top, right, capsule.bottom, Path.Direction.CW)
        if (!capsulePath.op(slicePath, Path.Op.INTERSECT)) return
        canvas.drawPath(capsulePath, paint)
    }

    private fun drawIcon(canvas: Canvas) {
        val top = paddingTop + (contentRect.height().toInt() - iconSizePx) / 2
        val left = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            width - paddingRight - iconInsetPx - iconSizePx
        } else {
            paddingLeft + iconInsetPx
        }
        iconDrawable.setBounds(left, top, left + iconSizePx, top + iconSizePx)
        iconDrawable.draw(canvas)
    }

    private fun applyIconLevel() {
        val level = (progressFraction.coerceIn(0f, 1f) * MAX_LEVEL).toInt()
        if (iconDrawable.level != level) {
            iconDrawable.level = level
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isPressed = true
                updateFromTouch(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.x)
                return true
            }

            MotionEvent.ACTION_UP -> {
                updateFromTouch(event.x)
                isPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** 手指横坐标即填充右缘，限制在胶囊内容区内。 */
    private fun updateFromTouch(x: Float) {
        val left = paddingLeft.toFloat()
        val widthPx = (width - paddingLeft - paddingRight).toFloat()
        if (widthPx <= 0f) return
        val along = ((x - left) / widthPx).coerceIn(0f, 1f)
        progressFraction = if (layoutDirection == LAYOUT_DIRECTION_RTL) 1f - along else along
    }

    private companion object {
        const val MAX_LEVEL = 10000
    }
}
