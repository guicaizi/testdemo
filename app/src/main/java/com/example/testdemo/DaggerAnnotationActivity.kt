package com.example.testdemo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.testdemo.dagger.Registry
import com.example.testdemo.dagger.T

/**
 * Dagger2 注解测试列表。
 *
 * 每一项都对应一个/一组注解，点「运行」立即在图里跑一遍并显示断言结果。
 * 测试代码在 `com.example.testdemo.dagger` 包下，注解处理由 Dagger 在编译期完成。
 */
class DaggerAnnotationActivity : AppCompatActivity() {

    private class RowHolder(
        val item: Registry.Item,
        val root: View,
        val status: TextView,
        val detail: TextView
    ) {
        var ran = false
        var passedItems = 0
        var failedItems = 0
        var totalChecks = 0
        var failedChecks = 0
    }

    private val rows = ArrayList<RowHolder>()
    private lateinit var tvSummary: TextView
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dagger_annotation)

        @Suppress("DEPRECATION")
        window.statusBarColor = Color.BLACK

        container = findViewById(R.id.container)
        tvSummary = findViewById(R.id.tv_summary)

        val items = Registry.all()
        findViewById<TextView>(R.id.tv_subtitle).text =
            "${items.size} 项注解用法，点「运行」单独验证，也可以一次全跑"

        items.forEach { container.addView(buildRow(it)) }

        findViewById<Button>(R.id.btn_run_all).setOnClickListener { runAll() }
        findViewById<Button>(R.id.btn_reset).setOnClickListener { resetAll() }

        updateSummary()
    }

    private fun buildRow(item: Registry.Item): View {
        val root = layoutInflater.inflate(R.layout.item_dagger_test, container, false)
        root.findViewById<TextView>(R.id.tv_ann).text = item.ann
        root.findViewById<TextView>(R.id.tv_title).text = item.title
        root.findViewById<TextView>(R.id.tv_summary).text = item.summary

        val status = root.findViewById<TextView>(R.id.tv_status)
        val detail = root.findViewById<TextView>(R.id.tv_detail)
        val holder = RowHolder(item, root, status, detail)
        rows.add(holder)

        root.findViewById<Button>(R.id.btn_run).setOnClickListener { runItem(holder) }
        return root
    }

    /** 跑单项；异常也当作失败展示，不让界面崩 */
    private fun runItem(holder: RowHolder) {
        val checks: List<T.Row> = try {
            holder.item.run()
        } catch (t: Throwable) {
            holder.ran = true
            holder.passedItems = 0
            holder.failedItems = 1
            holder.totalChecks = 1
            holder.failedChecks = 1
            holder.status.text = "✗ 抛异常"
            holder.status.setTextColor(COLOR_FAIL)
            holder.detail.visibility = View.VISIBLE
            holder.detail.text = t.toString()
            updateSummary()
            return
        }

        var failed = 0
        val sb = StringBuilder()
        checks.forEach { row ->
            if (row.pass) {
                sb.append("✓ ").append(row.name).append('\n')
            } else {
                failed++
                sb.append("✗ ").append(row.name)
                row.detail?.let { sb.append("  → ").append(it) }
                sb.append('\n')
            }
        }

        holder.ran = true
        holder.totalChecks = checks.size
        holder.failedChecks = failed
        if (failed == 0) {
            holder.passedItems = 1
            holder.failedItems = 0
            holder.status.text = "✓ PASS  ${checks.size}/${checks.size}"
            holder.status.setTextColor(COLOR_PASS)
        } else {
            holder.passedItems = 0
            holder.failedItems = 1
            holder.status.text = "✗ FAIL  ${checks.size - failed}/${checks.size}"
            holder.status.setTextColor(COLOR_FAIL)
        }
        holder.detail.visibility = View.VISIBLE
        holder.detail.text = sb.toString().trimEnd()
        updateSummary()
    }

    private fun runAll() {
        rows.forEach { runItem(it) }
        container.post { container.requestFocus() }
    }

    private fun resetAll() {
        rows.forEach {
            it.ran = false
            it.passedItems = 0
            it.failedItems = 0
            it.totalChecks = 0
            it.failedChecks = 0
            it.status.text = "未运行"
            it.status.setTextColor(COLOR_IDLE)
            it.detail.visibility = View.GONE
            it.detail.text = ""
        }
        updateSummary()
    }

    private fun updateSummary() {
        val done = rows.count { it.ran }
        val ok = rows.count { it.ran && it.failedChecks == 0 }
        val bad = done - ok
        val checks = rows.sumOf { it.totalChecks }
        val failedChecks = rows.sumOf { it.failedChecks }

        tvSummary.text = when {
            done == 0 -> "共 ${rows.size} 项 · 尚未运行"
            bad == 0 -> "已跑 $done/${rows.size} 项 · 全部通过（$checks 条断言）"
            else -> "已跑 $done/${rows.size} 项 · 通过 $ok · 失败 $bad" +
                "（$failedChecks/$checks 条断言失败）"
        }
        tvSummary.setTextColor(if (bad > 0) COLOR_FAIL else COLOR_PASS)
    }

    private companion object {
        val COLOR_PASS = Color.parseColor("#FF7BD88F")
        val COLOR_FAIL = Color.parseColor("#FFFF6B6B")
        val COLOR_IDLE = Color.parseColor("#FF757575")
    }
}
