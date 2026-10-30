package com.example.testdemo

import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.testdemo.widget.WatchRingProgressBar

/**
 * 指纹录入环效果 Demo：黑底铺满 + 210px 圆盘 + 230px 渐变/发光环 + 标题渐隐 + 底部提示。
 * 底部调试条可拖动进度；点圆盘在“提示 / 错误文案”间切换。
 */
class FingerprintDemoActivity : AppCompatActivity() {

    private var showingError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContentView(R.layout.activity_fingerprint_demo)

        val ring = findViewById<WatchRingProgressBar>(R.id.fp_ring)
        val hint = findViewById<TextView>(R.id.fp_hint)
        val label = findViewById<TextView>(R.id.fp_seek_label)
        val seek = findViewById<SeekBar>(R.id.fp_seek)

        val hintText = getString(R.string.fp_demo_hint)
        val errorText = getString(R.string.fp_demo_error)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                ring.progress = progress
                label.text = "progress ${progress * 100 / 10000}% · 点圆盘切换提示"
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit

            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })

        // 点圆盘区域：在默认提示与错误文案之间切换（两者共用同一 TextView）
        findViewById<android.view.View>(R.id.fp_center).setOnClickListener {
            showingError = !showingError
            hint.text = if (showingError) errorText else hintText
        }
    }
}
