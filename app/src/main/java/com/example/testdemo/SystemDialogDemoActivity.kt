package com.example.testdemo

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.util.Calendar

/**
 * 系统 Dialog 类型演示页。
 *
 * 每个按钮对应一种系统弹窗，覆盖：基础三按钮、列表、单选、多选、自定义 View、
 * 日期选择、时间选择、环形进度、水平进度、单按钮提示、双按钮提示、带图标提示。
 */
class SystemDialogDemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_system_dialog_demo)

        findViewById<View>(R.id.btn_basic).setOnClickListener { showBasicDialog() }
        findViewById<View>(R.id.btn_list).setOnClickListener { showListDialog() }
        findViewById<View>(R.id.btn_single_choice).setOnClickListener { showSingleChoiceDialog() }
        findViewById<View>(R.id.btn_multi_choice).setOnClickListener { showMultiChoiceDialog() }
        findViewById<View>(R.id.btn_custom_view).setOnClickListener { showCustomViewDialog() }
        findViewById<View>(R.id.btn_date_picker).setOnClickListener { showDatePickerDialog() }
        findViewById<View>(R.id.btn_time_picker).setOnClickListener { showTimePickerDialog() }
        findViewById<View>(R.id.btn_progress_spinner).setOnClickListener { showSpinnerProgressDialog() }
        findViewById<View>(R.id.btn_progress_horizontal).setOnClickListener { showHorizontalProgressDialog() }
        findViewById<View>(R.id.btn_single_button_tip).setOnClickListener { showSingleButtonTipDialog() }
        findViewById<View>(R.id.btn_double_button_tip).setOnClickListener { showDoubleButtonTipDialog() }
        findViewById<View>(R.id.btn_icon_tip).setOnClickListener { showIconTipDialog() }
    }

    /** 1. 基础弹窗：标题 + 正文 + 确定/中立/取消 三按钮。 */
    private fun showBasicDialog() {
        AlertDialog.Builder(this)
            .setTitle("基础弹窗")
            .setMessage("这是一个带三个按钮的系统 AlertDialog。")
            .setPositiveButton("确定") { _, _ -> toast("点击了确定") }
            .setNeutralButton("稍后") { _, _ -> toast("点击了稍后") }
            .setNegativeButton("取消") { _, _ -> toast("点击了取消") }
            .show()
    }

    /** 2. 列表弹窗：简单文本列表项。 */
    private fun showListDialog() {
        val items = arrayOf("选项一", "选项二", "选项三", "选项四")
        AlertDialog.Builder(this)
            .setTitle("列表弹窗")
            .setItems(items) { _, which -> toast("选中了：${items[which]}") }
            .show()
    }

    /** 3. 单选弹窗：RadioButton 列表，默认选中第二项。 */
    private fun showSingleChoiceDialog() {
        val items = arrayOf("红色", "绿色", "蓝色")
        var selected = 1
        AlertDialog.Builder(this)
            .setTitle("单选弹窗")
            .setSingleChoiceItems(items, selected) { _, which -> selected = which }
            .setPositiveButton("确定") { _, _ -> toast("选中了：${items[selected]}") }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 4. 多选弹窗：Checkbox 列表。 */
    private fun showMultiChoiceDialog() {
        val items = arrayOf("苹果", "香蕉", "橙子", "葡萄")
        val checked = booleanArrayOf(true, false, true, false)
        AlertDialog.Builder(this)
            .setTitle("多选弹窗")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("确定") { _, _ ->
                val result = items.filterIndexed { i, _ -> checked[i] }
                toast("选中了：${result.joinToString("、")}")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 5. 自定义 View 弹窗：在弹窗中嵌入一个 EditText。 */
    private fun showCustomViewDialog() {
        val input = EditText(this).apply {
            hint = "请输入内容"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("自定义 View 弹窗")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val text = input.text.toString().ifEmpty { "(空)" }
                toast("输入内容：$text")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 6. 日期选择弹窗。 */
    private fun showDatePickerDialog() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day -> toast("选中日期：$year-${month + 1}-$day") },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** 7. 时间选择弹窗。 */
    private fun showTimePickerDialog() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute -> toast("选中时间：${"%02d".format(hour)}:${"%02d".format(minute)}") },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    /** 8. 环形进度弹窗，2 秒后自动消失。 */
    private fun showSpinnerProgressDialog() {
        val dialog = ProgressDialog(this).apply {
            setTitle("加载中")
            setMessage("请稍候...")
            setCancelable(false)
        }
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) dialog.dismiss()
        }, 2000)
    }

    /** 9. 水平进度条弹窗，模拟进度递增到 100% 后关闭。 */
    private fun showHorizontalProgressDialog() {
        val dialog = ProgressDialog(this).apply {
            setTitle("下载中")
            setMessage("正在下载...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            setCancelable(false)
        }
        dialog.show()

        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        val runnable = object : Runnable {
            override fun run() {
                progress += 10
                dialog.progress = progress
                if (progress >= 100) {
                    dialog.dismiss()
                    toast("下载完成")
                } else {
                    handler.postDelayed(this, 300)
                }
            }
        }
        handler.postDelayed(runnable, 300)
    }

    /** 10. 单按钮提示弹窗：仅一个"确定"按钮，常用于纯提示场景。 */
    private fun showSingleButtonTipDialog() {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("操作已完成。")
            .setPositiveButton("确定") { _, _ -> toast("点击了确定") }
            .show()
    }

    /** 11. 双按钮提示弹窗：取消 + 确定，常用于需要用户确认的场景。 */
    private fun showDoubleButtonTipDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认操作")
            .setMessage("确定要执行此操作吗？此操作不可撤销。")
            .setPositiveButton("确定") { _, _ -> toast("点击了确定") }
            .setNegativeButton("取消") { _, _ -> toast("点击了取消") }
            .show()
    }

    /** 12. 带图标提示弹窗：setIcon 设置顶部图片，图标在标题上方、整组居中；
     *  对比用例 10/11（未设图标 → 无图标居中样式）。 */
    private fun showIconTipDialog() {
        AlertDialog.Builder(this)
            .setTitle("更新完成")
            .setMessage("新版本已准备好，是否立即安装？")
            .setIcon(R.mipmap.ic_toast_default_img)
            .setPositiveButton("安装") { _, _ -> toast("点击了安装") }
            .setNegativeButton("稍后") { _, _ -> toast("点击了稍后") }
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
