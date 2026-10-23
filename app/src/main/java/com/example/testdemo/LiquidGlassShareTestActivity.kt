// prize add by yanliang for 液态玻璃背景采样测试页（单胶囊拖动）, 20261023-start
package com.example.testdemo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.testdemo.widget.ControlPanelPillView

/**
 * 液态玻璃「采样哪块背景」验证页。
 *
 * 背景是四象限不同明暗/颜色，中间一个玻璃胶囊（参数提取自 n06SystemUi
 * 控制面板工作模式胶囊）。按住胶囊拖到任意象限，看玻璃里透出的是不是
 * 它当前覆盖的那块背景（而不是首帧位置的老背景）。
 *
 * 拖动逻辑全部放在 Activity 侧（OnTouchListener），不改控件内部的触摸事件。
 */
class LiquidGlassShareTestActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_liquid_glass_share_test)

        val pill = findViewById<ControlPanelPillView>(R.id.pill_mode)
        pill.setLabel("工作模式")
        pill.setActive(true) // 选中态：#0DDB58 竖向渐变
        val root = findViewById<View>(android.R.id.content)
        var downRawX = 0f
        var downRawY = 0f
        var startTx = 0f
        var startTy = 0f

        pill.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startTx = v.translationX
                    startTy = v.translationY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val nx = startTx + (event.rawX - downRawX)
                    val ny = startTy + (event.rawY - downRawY)
                    // 限制在父容器（全屏）内
                    v.translationX = nx.coerceIn(-v.left.toFloat(), (root.width - v.right).toFloat())
                    v.translationY = ny.coerceIn(-v.top.toFloat(), (root.height - v.bottom).toFloat())
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // translation 不会触发 onLayout，这里手动刷新采样位置并重抓背景
                    pill.refreshGlassBackdrop()
                    true
                }

                else -> false
            }
        }
    }
}
// prize add by yanliang for 液态玻璃背景采样测试页（单胶囊拖动）, 20261023-end
