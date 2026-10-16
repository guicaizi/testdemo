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
        // 对应 ChooseLockPattern.applyRoundPatternChrome：黑底 + 黑导航栏
        window.navigationBarColor = android.graphics.Color.BLACK
        setContentView(R.layout.activity_glass_footer_demo)

        val footer = findViewById<KusaiPatternFooterView>(R.id.footer)
        footer.setRetryEnabled(true)
        footer.setContinueEnabled(true)
        footer.setOnRetryClickListener {
            Toast.makeText(this, "Retry", Toast.LENGTH_SHORT).show()
        }
        footer.setOnContinueClickListener {
            Toast.makeText(this, "Continue", Toast.LENGTH_SHORT).show()
        }
    }
}
