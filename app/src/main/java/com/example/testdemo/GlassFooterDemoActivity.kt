package com.example.testdemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testdemo.widget.KusaiGlassDialog
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

        // 单按钮 Dialog 演示（Builder 模式，与 framework AlertDialog.Builder 同构）
        findViewById<android.widget.Button>(R.id.btn_dialog_single).setOnClickListener {
            KusaiGlassDialog.Builder(this)
                .setDialogTitle("Start Pairing")
                .setIcon(R.mipmap.ic_toast_default_img)
                .setContent("Please make sure your Mac or PC is online.")
                .setSingleButton("Start Pairing") { dialog ->
                    Toast.makeText(this, "Start Pairing clicked", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        // 双按钮 Dialog 演示
        findViewById<android.widget.Button>(R.id.btn_dialog_double).setOnClickListener {
            KusaiGlassDialog.Builder(this)
                .setDialogTitle("Draw Pattern 111")
                .setIcon(R.mipmap.ic_launcher)
                .setContent("Use at least 4 dots. Connect the dots to draw your unlock pattern.")
                .setDoubleButtons(
                    retryLabel = "Retry",
                    retryListener = { dialog ->
                        Toast.makeText(this, "Retry clicked", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    continueLabel = "Continue",
                    continueListener = { dialog ->
                        Toast.makeText(this, "Continue clicked", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                )
                .show()
        }
    }
}
