package com.example.testdemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testdemo.widget.KusaiPatternFooterView

/**
 * 液态玻璃按钮（图案锁底栏 Retry / Continue）效果演示。
 *
 * 直接复用自 Settings 的 KusaiPatternFooterView，底层依赖 kusai-liquidglass 库
 * （折射 + 渐变填充 + 方向高光描边四层绘制）。
 */
class GlassFooterDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 对应 ChooseLockPattern.applyRoundPatternChrome：黑底 + 黑状态栏 + 黑导航栏
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContentView(R.layout.activity_glass_footer_demo)

        val footer = findViewById<KusaiPatternFooterView>(R.id.footer)
        // 单按钮模式：横跨编组宽度的绿色玻璃按钮（对应截图 Start Pairing 样式）
        footer.setSingleButton(true)
        footer.setSingleButtonLabel("Start Pairing")
        footer.setOnSingleClickListener {
            Toast.makeText(this, "Start Pairing", Toast.LENGTH_SHORT).show()
        }
        // 双按钮模式备用
        footer.setRetryEnabled(true)
        footer.setContinueEnabled(true)
        footer.setOnRetryClickListener {
            Toast.makeText(this, "Retry", Toast.LENGTH_SHORT).show()
        }
        footer.setOnContinueClickListener {
            Toast.makeText(this, "Continue", Toast.LENGTH_SHORT).show()
        }
        // 测试按钮：单/双按钮模式切换
        val toggleBtn = findViewById<android.widget.Button>(R.id.btn_toggle_mode)
        toggleBtn.setOnClickListener {
            val single = !footer.isSingleButton()
            footer.setSingleButton(single)
            footer.setSingleButtonLabel("Start Pairing")
            toggleBtn.text = if (single) "当前：单按钮" else "当前：双按钮"
        }
        toggleBtn.text = "当前：单按钮"
    }
}
