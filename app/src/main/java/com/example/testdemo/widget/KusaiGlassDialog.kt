package com.example.testdemo.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.testdemo.R

/**
 * 液态玻璃风格 Dialog —— 按 framework `AlertDialog` 的设计思想同构改造。
 *
 * 结构对应关系（framework -> 本类）：
 * - `AlertDialog`（API 门面，protected 构造）   -> [KusaiGlassDialog]（private 构造）
 * - `AlertDialog.Builder`（流式收集参数）       -> [Builder]
 * - `AlertController.AlertParams`（参数袋）     -> [GlassParams]
 * - `AlertController.installContent/setupXxx`  -> [installContent] + 各 setupXxx（模板装配 + 可见性开关）
 * - `buttonPanel`（按钮面板）                   -> [KusaiPatternFooterView] 玻璃底栏（单/双按钮）
 *
 * 与 framework 一致的核心原则：
 * 1. Builder 只收集参数到 [GlassParams]，`create()` 时一次性 apply，避免半初始化；
 * 2. 装配阶段（[installContent]）才创建 View，保证使用 dialog 的 themed context；
 * 3. 布局是模板：标题/副标题/内容/按钮四个面板，没内容就 GONE，靠可见性开关组合出所有形态。
 *
 * 黑底 + 居中圆盘背景，支持：
 * - 顶部图标（[Builder.setIcon] / [Builder.setIconDrawable]，不设置或为 null 则隐藏该面板）
 * - 标题（[Builder.setDialogTitle]）
 * - 内容文本（[Builder.setContent]）
 * - 底部按钮：单按钮（[Builder.setSingleButton]）或双按钮（[Builder.setDoubleButtons]）
 */
class KusaiGlassDialog private constructor(
    context: Context,
    private val params: GlassParams
) : Dialog(context, R.style.KusaiGlassDialogTheme) {

    /** 对应 framework `Dialog.setCanceledOnTouchOutside`；本窗口铺满全屏，需自行处理。 */
    private var cancelOnTouchOutside = true

    /** 对应 framework `GlassAlertController.mGlassInstalled`：装配完成后，post-show 的
     *  setTitle/setMessage/setIcon 才会同步刷新玻璃面板。 */
    private var installed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_kusai_glass)

        setupWindow()
        installContent()

        // framework 的 touch-outside 取消依赖非全屏窗口；此处窗口铺满全屏，
        // 点击事件不会派发到 window 之外，故等价实现在根布局上：点卡片外的黑底区域即取消。
        // 对齐 framework 语义：cancelable 同时管辖 back 键与点外部，
        // 故 setCancelable(false) 后点黑幕不再取消（✕ 仍可 dismiss）。
        findViewById<View>(R.id.kusai_dialog_root).setOnClickListener {
            if (isShowing && cancelOnTouchOutside) dismiss()
        }
    }

    /** 窗口铺满全屏（黑底），内容（480 圆盘）居中只占一小块；状态栏不隐藏，
     *  浮在弹窗顶部黑区上方（透明背景、不占布局空间），480 画布与整屏 1:1。 */
    private fun setupWindow() {
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            // DecorView 默认会把状态栏 inset 当 padding 下推内容（内容区变 432），
            // 这里让它不做 inset 适配，内容铺满整窗、状态栏浮在上面。
            WindowCompat.setDecorFitsSystemWindows(this, false)
            setDimAmount(0f) // 自身已黑底，不再额外 dim
        }
        // 点击黑底取消需要根布局可点击以接收事件
        findViewById<View>(R.id.kusai_dialog_root).isClickable = true
    }

    /**
     * 模板装配（对应 framework `AlertController.installContent`）。
     * 四个面板按条件填充，顺序与模板布局一致。
     */
    private fun installContent() {
        // 设计稿基准 480×480，目标屏幕也是 480×480。
        // 为了让 Dialog 看起来是"小弹窗"而非铺满，把整张 480 设计稿等比缩到 2/3（320px）。
        // 标题/副标题/内容/底栏的所有 px 坐标与字号同步缩放，底栏内部 min/480 缩放自动适配。
        findViewById<View>(R.id.kusai_dialog_card)?.apply {
            scaleX = 2f / 3f
            scaleY = 2f / 3f
        }
        setupCloseButton()
        setupIcon()
        setupTitle()
        setupContent()
        setupFooter()
        installed = true
    }

    /** 关闭面板：右上角 X，点击直接 dismiss（关闭按钮不受 cancelable 限制）。 */
    private fun setupCloseButton() {
        findViewById<View>(R.id.kusai_dialog_close).setOnClickListener { dismiss() }
    }

    /**
     * 顶部图标面板：外部设置了图标则显示并填充，
     * 未设置或设置为 null 时 GONE（无默认图），此时标题/内容居中显示。
     */
    private fun setupIcon() {
        val iconView = findViewById<ImageView>(R.id.kusai_dialog_icon)
        when {
            params.iconDrawable != null -> {
                iconView.setImageDrawable(params.iconDrawable)
                iconView.visibility = View.VISIBLE
            }
            params.iconResId != View.NO_ID -> {
                iconView.setImageResource(params.iconResId)
                iconView.visibility = View.VISIBLE
            }
            else -> iconView.visibility = View.GONE
        }
    }

    /** 标题面板：无内容即 GONE（对应 framework `setupTitle` 的可见性开关）。 */
    private fun setupTitle() {
        val titleView = findViewById<TextView>(R.id.kusai_dialog_title)
        if (params.title.isNullOrEmpty()) {
            titleView.visibility = View.GONE
        } else {
            titleView.text = params.title
            titleView.visibility = View.VISIBLE
        }
    }

    /**
     * 内容面板（对应 framework `setupContent`）：纯文本，无内容即 GONE。
     */
    private fun setupContent() {
        val contentView = findViewById<TextView>(R.id.kusai_dialog_content)
        if (params.contentText.isNullOrEmpty()) {
            contentView.visibility = View.GONE
        } else {
            contentView.text = params.contentText
            contentView.visibility = View.VISIBLE
        }
    }

    /** 按钮面板（对应 framework `setupButtons`）：单/双按钮两种形态由底栏自行分化。
     *  点击行为对齐 framework `AlertController.mButtonHandler`：先回调 listener，
     *  再自动 dismiss（framework ButtonHandler 先派发 MSG_BUTTON_CLICKED、
     *  再派发 MSG_DISMISS_DIALOG）。 */
    private fun setupFooter() {
        val footer = findViewById<KusaiPatternFooterView>(R.id.kusai_dialog_footer)
        if (params.singleButton) {
            footer.setSingleButton(true)
            footer.setSingleButtonLabel(params.singleLabel ?: "")
            footer.setOnSingleClickListener {
                params.singleClickListener?.invoke(this@KusaiGlassDialog)
                dismiss()
            }
        } else {
            footer.setSingleButton(false)
            footer.setRetryLabel(params.retryLabel ?: "")
            footer.setContinueLabel(params.continueLabel ?: "")
            footer.setRetryEnabled(true)
            footer.setContinueEnabled(true)
            footer.setOnRetryClickListener {
                params.retryClickListener?.invoke(this@KusaiGlassDialog)
                dismiss()
            }
            footer.setOnContinueClickListener {
                params.continueClickListener?.invoke(this@KusaiGlassDialog)
                dismiss()
            }
        }
    }

    // ---- post-show 同步 API（对齐 framework GlassAlertController 的 setTitle/setMessage/
    //      setButton 覆写：玻璃已装配后调用，同步刷新面板而不是只改内部字段）----

    /** 对应 framework `GlassAlertController.setTitle`：show 前调用仅存参；show 后调用
     *  同步刷新玻璃标题面板（无内容则 GONE）。 */
    override fun setTitle(title: CharSequence?) {
        params.title = title
        super.setTitle(title) // window title
        if (installed) setupTitle()
    }

    /** 对应 framework `GlassAlertController.setMessage`：show 前调用仅存参；show 后调用
     *  重建内容面板（幂等），多行/空文本均安全。 */
    fun setMessage(text: CharSequence?) {
        params.contentText = text
        if (installed) setupContent()
    }

    /** show 后替换顶部图片；传 null 或未设置则隐藏图标面板，同步刷新。 */
    fun setIcon(resId: Int) {
        params.iconResId = resId
        params.iconDrawable = null
        if (installed) setupIcon()
    }

    /** show 后替换顶部图片（Drawable 优先）；传 null 则隐藏图标面板，同步刷新。 */
    fun setIconDrawable(drawable: Drawable?) {
        params.iconDrawable = drawable
        // 显式传 null = 清除图标（隐藏面板），而非回退到之前设置的资源 id
        if (drawable == null) params.iconResId = View.NO_ID
        if (installed) setupIcon()
    }

    /**
     * 参数袋（对应 framework `AlertController.AlertParams`）。
     * Builder 的所有 setXxx 只写这里；`create()` 后由 dialog 在装配阶段读取。
     * 收集与应用分离，是 framework Builder 模式的关键。
     */
    internal class GlassParams {
        var title: CharSequence? = null

        /** 顶部图标资源 id；`View.NO_ID` 表示未设置，图标面板 GONE。 */
        var iconResId: Int = View.NO_ID

        /** 顶部图标 Drawable，优先于 [iconResId]；两者皆空时图标面板 GONE。 */
        var iconDrawable: Drawable? = null

        /** 纯文本内容；装配时写入模板 TextView。 */
        var contentText: CharSequence? = null

        /** true = 单按钮模式；false = 双按钮（Retry/Continue）模式。 */
        var singleButton: Boolean = false
        var singleLabel: CharSequence? = null
        var singleClickListener: ((KusaiGlassDialog) -> Unit)? = null
        var retryLabel: CharSequence? = null
        var retryClickListener: ((KusaiGlassDialog) -> Unit)? = null
        var continueLabel: CharSequence? = null
        var continueClickListener: ((KusaiGlassDialog) -> Unit)? = null

        /** 默认可取消（back 键 + 点击黑底），与 framework AlertDialog.Builder 默认一致。 */
        var cancelable: Boolean = true
    }

    /**
     * Builder（对应 framework `AlertDialog.Builder`）。
     * 流式收集参数，[create] 一次性构建，[show] 构建并立即显示。
     */
    class Builder(private val context: Context) {
        private val P = GlassParams()

        /** 设置标题。 */
        fun setDialogTitle(text: CharSequence?) = apply { P.title = text }

        /** 设置顶部图标（资源 id），替换布局默认图；传 `View.NO_ID` 隐藏图标面板。 */
        fun setIcon(resId: Int) = apply {
            P.iconResId = resId
            P.iconDrawable = null
        }

        /** 设置顶部图标（Drawable），优先于 [setIcon]；传 null 隐藏图标面板。 */
        fun setIconDrawable(drawable: Drawable?) = apply {
            P.iconDrawable = drawable
            // 显式传 null = 清除图标（隐藏面板），而非回退到之前设置的资源 id
            if (drawable == null) P.iconResId = View.NO_ID
        }

        /** 设置文本内容。 */
        fun setContent(text: CharSequence?) = apply {
            P.contentText = text
        }

        /** 单按钮模式：一个横跨全宽的绿色玻璃按钮。 */
        fun setSingleButton(
            label: CharSequence,
            listener: ((KusaiGlassDialog) -> Unit)? = null
        ) = apply {
            P.singleButton = true
            P.singleLabel = label
            P.singleClickListener = listener
        }

        /** 双按钮模式：左 Retry 风格（偏白）+ 右 Continue 风格（绿色）。 */
        fun setDoubleButtons(
            retryLabel: CharSequence,
            retryListener: ((KusaiGlassDialog) -> Unit)? = null,
            continueLabel: CharSequence,
            continueListener: ((KusaiGlassDialog) -> Unit)? = null
        ) = apply {
            P.singleButton = false
            P.retryLabel = retryLabel
            P.retryClickListener = retryListener
            P.continueLabel = continueLabel
            P.continueClickListener = continueListener
        }

        /** 是否可取消（back 键 + 点击黑底区域），默认 true，与 framework 默认一致。 */
        fun setCancelable(flag: Boolean) = apply { P.cancelable = flag }

        /** 创建 Dialog（对应 framework `Builder.create()`），不显示。 */
        fun create(): KusaiGlassDialog {
            val dialog = KusaiGlassDialog(context, P)
            dialog.setCancelable(P.cancelable)
            if (P.cancelable) {
                dialog.cancelOnTouchOutside = true
            }
            return dialog
        }

        /** 创建并立即显示（对应 framework `Builder.show()`）。 */
        fun show(): KusaiGlassDialog = create().apply { show() }
    }
}