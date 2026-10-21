package com.example.testdemo

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * framework 原生 AlertDialog 验证页。
 *
 * 全局开启 controllerType=kusai 后，framework AlertDialog 应走玻璃控制器：
 * - 单/双按钮 + message → 玻璃样式（全屏黑底 + 480 圆盘 + 玻璃 footer）
 * - 三按钮 / 列表 → fallback 标准 Material 布局
 * - 自定义 View → 玻璃内容区
 *
 * 对应 framework 侧 GlassAlertController；fallback 时 logcat 会有
 * GlassAlertController TAG 的 warn（仅布局缺失时），正常 fallback 无日志。
 */
class FrameworkDialogTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_framework_dialog_test)

        findViewById<Button>(R.id.btn_fw_single).setOnClickListener { showSingle() }
        findViewById<Button>(R.id.btn_fw_double).setOnClickListener { showDouble() }
        findViewById<Button>(R.id.btn_fw_triple).setOnClickListener { showTriple() }
        findViewById<Button>(R.id.btn_fw_list).setOnClickListener { showList() }
        findViewById<Button>(R.id.btn_fw_custom_view).setOnClickListener { showCustomView() }
    }

    /** 1 个按钮 → 玻璃单按钮模式（横跨全宽绿色）。 */
    private fun showSingle() {
        AlertDialog.Builder(this)
            .setTitle("确认配对")
            .setMessage("请确保手机与电脑在同一网络。")
            .setPositiveButton("Start Pairing") { _, _ ->
                toast("Start Pairing")
            }
            .show()
    }

    /** 2 个按钮 → 玻璃双按钮模式：negative 在左（Retry 白），positive 在右（Continue 绿）。 */
    private fun showDouble() {
        AlertDialog.Builder(this)
            .setTitle("重试或继续")
            .setMessage("图案不匹配，重试还是继续？")
            .setPositiveButton("Continue") { _, _ -> toast("Continue") }
            .setNegativeButton("Retry") { _, _ -> toast("Retry") }
            .show()
    }

    /** 3 个按钮 → fallback 标准布局。 */
    private fun showTriple() {
        AlertDialog.Builder(this)
            .setTitle("三按钮")
            .setMessage("三个按钮超出玻璃 footer 承载，应显示标准 Material 弹窗。")
            .setPositiveButton("确定") { _, _ -> toast("确定") }
            .setNeutralButton("稍后") { _, _ -> toast("稍后") }
            .setNegativeButton("取消") { _, _ -> toast("取消") }
            .show()
    }

    /** 列表 → fallback 标准布局。 */
    private fun showList() {
        val items = arrayOf("选项一", "选项二", "选项三")
        AlertDialog.Builder(this)
            .setTitle("列表选择")
            .setItems(items) { _, which -> toast("选中：${items[which]}") }
            .show()
    }

    /** 自定义 View → 玻璃内容区。 */
    private fun showCustomView() {
        val input = EditText(this).apply { hint = "请输入内容" }
        AlertDialog.Builder(this)
            .setTitle("自定义输入")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                toast("输入：${input.text}")
            }
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}