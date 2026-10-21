package com.example.testdemo

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testdemo.widget.KusaiGlassDialog

/**
 * 启动即弹 KusaiGlassDialog 的测试入口。
 *
 * 冷启动自动弹单按钮弹窗；关闭弹窗后回到黑底页，
 * 可用两个按钮随时重弹单按钮 / 双按钮弹窗验证样式。
 */
class GlassDialogLaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 弹窗自身黑底，宿主页同样压黑，避免关闭弹窗后闪白
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_glass_dialog_launch)

        findViewById<Button>(R.id.btn_launch_single).setOnClickListener { showSingle() }
        findViewById<Button>(R.id.btn_launch_double).setOnClickListener { showDouble() }
        findViewById<Button>(R.id.btn_launch_icon).setOnClickListener { showCustomIcon() }
        findViewById<Button>(R.id.btn_launch_no_icon).setOnClickListener { showNoIcon() }

    }

    private fun showSingle() {
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

    private fun showDouble() {
        KusaiGlassDialog.Builder(this)
            .setDialogTitle("Draw Pattern")
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

    /** 顶部图片扩展：自定义图替换图标面板；不调 setIcon / 传 null 则图标面板 GONE。 */
    private fun showCustomIcon() {
        KusaiGlassDialog.Builder(this)
            .setDialogTitle("Update Ready")
            .setIcon(R.drawable.img)   // 自定义顶部图片；不调 setIcon 则隐藏图标面板
            .setContent("A new version is available. Install now?")
            .setDoubleButtons(
                retryLabel = "Later",
                retryListener = { dialog ->
                    Toast.makeText(this, "Later clicked", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                },
                continueLabel = "Install",
                continueListener = { dialog ->
                    Toast.makeText(this, "Install clicked", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            )
            .show()
    }

    /** 无图标形态：不设置图标，图标面板 GONE，标题 + 内容在圆盘中垂直居中。 */
    private fun showNoIcon() {
        KusaiGlassDialog.Builder(this)
            .setDialogTitle("No Icon")
            .setContent("Title and content are vertically centered without an icon.")
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