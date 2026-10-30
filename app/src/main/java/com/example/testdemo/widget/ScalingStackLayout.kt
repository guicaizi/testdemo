package com.example.testdemo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import com.example.testdemo.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * prize add by yanliang 20261030-start
 * 竖排「聚焦缩放」列表：
 *  - 第一槽位是聚焦位，落在该位置的项目保持 1.0 缩放，离它越远越小（最小 [minScale]）；
 *  - 子 View 按**视觉高度**（含缩放后的高度）依次堆叠，间距恒定为 [itemGap]，
 *    不会因为项目被缩小而把间距撑大；
 *  - 支持拖拽 + 惯性滑动，松手后吸附到最近的聚焦位。
 *
 * 之所以不用 ScrollView/RecyclerView：两者都只能按未缩放的布局尺寸堆叠，
 * 缩放后每个项目会凭空多出 (1 - scale) * 高度 的空档，间距就不均匀了。
 */
class ScalingStackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    /** 非聚焦项的最小缩放比 */
    private var minScale = 0.87f

    /** 相邻两项之间的视觉间距（px） */
    private var itemGap = 0

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.ScalingStackLayout)
            minScale = ta.getFloat(R.styleable.ScalingStackLayout_minItemScale, minScale)
            itemGap = ta.getDimensionPixelSize(R.styleable.ScalingStackLayout_itemGap, itemGap)
            ta.recycle()
        }
    }

    /** 子 View 的原始高度（未缩放） */
    private var cardHeight = 0

    /** 连续焦点：整数表示第几项正好落在聚焦槽，小数表示滚动过程中的过渡量 */
    private var focus = 0f

    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastTouchY = 0f
    private var downY = 0f
    private var isDragging = false
    private var flingInProgress = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    /** 焦点动画用的放大轴：把 0..1 的 focus 映射成整数，交给 OverScroller 做缓动 */
    private val axis = 10_000f

    /** 滚动一格的位移：上一项缩到最小时的高度 + 间距 */
    private val stepPx: Float
        get() = cardHeight * minScale + itemGap

    private fun visibleCount(): Int {
        var n = 0
        for (i in 0 until childCount) if (getChildAt(i).visibility != View.GONE) n++
        return n
    }

    /** 第 index 项在焦点为 focusPos 时的缩放比 */
    private fun scaleAt(index: Int, focusPos: Float): Float {
        val distance = min(abs(index - focusPos), 1f)
        return minScale + (1f - minScale) * (1f - distance)
    }

    /** 焦点落在两项之间时，聚焦位的中心（两项中心线性插值） */
    private fun interpolatedCenter(centers: FloatArray, focusPos: Float): Float {
        val n = centers.size
        if (n <= 1) return centers.firstOrNull() ?: 0f
        val lower = focusPos.toInt().coerceIn(0, n - 2)
        val fraction = (focusPos - lower).coerceIn(0f, 1f)
        return centers[lower] + (centers[lower + 1] - centers[lower]) * fraction
    }

    /** 焦点为 focusPos 时，最后一项的视觉底边（用于判断还能不能再往下滚） */
    private fun bottomAt(focusPos: Float): Float {
        val n = visibleCount()
        if (n == 0 || cardHeight == 0) return 0f
        var top = paddingTop.toFloat()
        val centers = FloatArray(n)
        for (i in 0 until n) {
            val scale = scaleAt(i, focusPos)
            val height = cardHeight * scale
            centers[i] = top + height / 2f
            top += height + itemGap
        }
        val shift = (paddingTop + cardHeight / 2f) - interpolatedCenter(centers, focusPos)
        return top - itemGap + shift
    }

    /** 内容不溢出时不允许滚动；溢出时允许滚到最后一项占据聚焦位 */
    private fun maxFocus(): Float {
        val n = visibleCount()
        if (n <= 1) return 0f
        val available = (height - paddingTop - paddingBottom).toFloat()
        if (available <= 0f) return 0f
        return if (bottomAt(0f) <= available) 0f else (n - 1).toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var tallest = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val lp = child.layoutParams
            child.measure(
                getChildMeasureSpec(
                    widthMeasureSpec, paddingLeft + paddingRight, lp.width
                ),
                if (lp.height > 0) {
                    MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
                } else {
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                }
            )
            tallest = max(tallest, child.measuredHeight)
        }
        cardHeight = tallest
        setMeasuredDimension(widthSize, MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val n = visibleCount()
        if (n == 0 || cardHeight == 0) return

        focus = focus.coerceIn(0f, maxFocus())

        val childWidth = (r - l) - paddingLeft - paddingRight
        val anchorCenter = paddingTop + cardHeight / 2f
        val tops = FloatArray(n)
        val centers = FloatArray(n)
        val scales = FloatArray(n)

        var top = paddingTop.toFloat()
        for (i in 0 until n) {
            val scale = scaleAt(i, focus)
            val height = cardHeight * scale
            tops[i] = top
            centers[i] = top + height / 2f
            scales[i] = scale
            top += height + itemGap
        }

        // 让“聚焦位中心”对齐锚点，再反推每项的布局位置
        val shift = anchorCenter - interpolatedCenter(centers, focus)
        var index = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val scale = scales[index]
            val visualTop = tops[index] + shift
            // 缩放以中心为轴，所以布局框要往下让出 (1 - scale) * h / 2，视觉顶边才落在 visualTop
            val laidTop = (visualTop - cardHeight * (1f - scale) / 2f).roundToInt()
            child.scaleX = scale
            child.scaleY = scale
            child.layout(paddingLeft, laidTop, paddingLeft + childWidth, laidTop + cardHeight)
            index++
        }
    }

    // ---------------------------------------------------------------- 滚动交互

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.y
                lastTouchY = ev.y
                isDragging = false
                flingInProgress = false
                scroller.abortAnimation()
                activePointerId = ev.getPointerId(0)
                trackVelocity(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                trackVelocity(ev)
                if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val index = ev.findPointerIndex(activePointerId)
                    if (index >= 0 && abs(ev.getY(index) - downY) > touchSlop) {
                        isDragging = true
                        lastTouchY = ev.getY(index)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> releaseVelocityTracker()
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.y
                lastTouchY = ev.y
                isDragging = false
                flingInProgress = false
                scroller.abortAnimation()
                activePointerId = ev.getPointerId(0)
                trackVelocity(ev)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                trackVelocity(ev)
                val index = ev.findPointerIndex(activePointerId)
                if (index < 0) return false
                val y = ev.getY(index)
                val dy = y - lastTouchY
                if (!isDragging && abs(y - downY) > touchSlop) isDragging = true
                if (isDragging && stepPx > 0f) {
                    // 手指上滑（dy < 0）→ 焦点后移
                    focus = (focus - dy / stepPx).coerceIn(0f, maxFocus())
                    requestLayout()
                }
                lastTouchY = y
                return true
            }

            MotionEvent.ACTION_UP -> {
                trackVelocity(ev)
                val velocity = velocityTracker?.let {
                    it.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    it.getYVelocity(activePointerId)
                } ?: 0f
                releaseVelocityTracker()
                if (isDragging) {
                    if (abs(velocity) > minFlingVelocity) fling(-velocity) else snapToNearest()
                } else {
                    snapToNearest()
                }
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                releaseVelocityTracker()
                snapToNearest()
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun trackVelocity(ev: MotionEvent) {
        val tracker = velocityTracker ?: VelocityTracker.obtain().also { velocityTracker = it }
        tracker.addMovement(ev)
    }

    private fun releaseVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
        activePointerId = MotionEvent.INVALID_POINTER_ID
    }

    /** 惯性滑动：把速度换算到焦点轴上交给 OverScroller 做减速 */
    private fun fling(velocityOnFocus: Float) {
        if (stepPx <= 0f) return
        val start = (focus * axis).toInt()
        val end = (maxFocus() * axis).toInt()
        flingInProgress = true
        scroller.fling(0, start, 0, (velocityOnFocus / stepPx * axis).toInt(), 0, 0, 0, end)
        postInvalidateOnAnimation()
    }

    /** 吸附到最近的整数焦点位 */
    private fun snapToNearest() {
        val target = focus.roundToInt().toFloat().coerceIn(0f, maxFocus())
        if (abs(target - focus) < 0.001f) return
        scroller.startScroll(
            0, (focus * axis).toInt(), 0, ((target - focus) * axis).toInt(), 260
        )
        postInvalidateOnAnimation()
    }

    override fun computeScroll() {
        if (!scroller.computeScrollOffset()) return
        focus = (scroller.currY / axis).coerceIn(0f, maxFocus())
        requestLayout()
        if (scroller.isFinished) {
            if (flingInProgress) {
                flingInProgress = false
                snapToNearest()
            }
        } else {
            postInvalidateOnAnimation()
        }
    }

    /** 列表项增删后调用，把焦点收敛回合法范围 */
    fun onItemsChanged() {
        focus = focus.coerceIn(0f, maxFocus())
        requestLayout()
    }
}
// prize add by yanliang for 新增可滚动的聚焦缩放列表控件, 20261030-end
