package com.example.testdemo

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.testdemo.widget.ScalingStackLayout

// prize add by yanliang 20261030-start
/**
 * 指纹管理页 Demo：黑底 480×480 画布（圆屏）。
 * 标题 / 副标题 / “Fingerprint unlock” 开关 / 指纹列表（可滚动、非聚焦项缩小、可删除）/ “Add fingerprint” 按钮。
 * 列表最多 3 个指纹，超过可视高度后靠 [ScalingStackLayout] 滚动。
 */
// prize add by yanliang for 指纹列表改为可滚动的缩放列表, 20261030-end
class FingerprintManagementActivity : AppCompatActivity() {

    private companion object {
        const val MAX_FINGERPRINTS = 3
        const val INITIAL_FINGERPRINTS = 2
    }

    private lateinit var list: ScalingStackLayout
    private lateinit var addButton: View

    /** 当前指纹数量，列表中显示为 Fingerprint 1..n */
    private var count = INITIAL_FINGERPRINTS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContentView(R.layout.activity_fingerprint_manage)

        val body = findViewById<View>(R.id.fp_mgr_body)
        val switch = findViewById<SwitchCompat>(R.id.fp_mgr_switch)
        list = findViewById(R.id.fp_mgr_list)
        addButton = findViewById(R.id.fp_mgr_add)

        // 开关关闭时，列表与添加按钮置灰禁用，体现“解锁开关”对列表的依赖
        switch.setOnCheckedChangeListener { _, checked ->
            body.alpha = if (checked) 1f else 0.35f
            body.isEnabled = checked
            list.isEnabled = checked
            addButton.isEnabled = checked
            // prize add by yanliang for 按钮移出 body 后需要单独跟随开关置灰, 20261030-start
            addButton.alpha = if (checked) 1f else 0.35f
            // prize add by yanliang for 按钮移出 body 后需要单独跟随开关置灰, 20261030-end
        }

        addButton.setOnClickListener { addFingerprint() }
        // prize add by yanliang for 按钮背景改由 XML 矢量资源提供, 20261030
        // （原来在这里 new ArcTopButtonDrawable(...) 覆盖 background，现已删除；
        //   形状 / 渐变 / 常态-按下-禁用三态都收敛到 @drawable/fp_mgr_button_add_bg）
        renderList()
    }

    private fun addFingerprint() {
        if (count >= MAX_FINGERPRINTS) {
            Toast.makeText(this, R.string.fp_mgr_limit, Toast.LENGTH_SHORT).show()
            return
        }
        count++
        renderList()
        Toast.makeText(this, getString(R.string.fp_mgr_add), Toast.LENGTH_SHORT).show()
    }

    private fun deleteFingerprint(index: Int) {
        if (count <= 0) return
        count--
        renderList()
        Toast.makeText(
            this,
            getString(R.string.fp_mgr_deleted, index + 1),
            Toast.LENGTH_SHORT
        ).show()
    }

    /** 列表项数量变化后整体重建（最多 3 项，重建比做局部增删动画更省事也更稳） */
    private fun renderList() {
        list.removeAllViews()
        repeat(count) { index ->
            val item = layoutInflater.inflate(R.layout.item_fp_fingerprint, list, false)
            item.findViewById<TextView>(R.id.fp_item_title).text =
                getString(R.string.fp_mgr_item, index + 1)
            item.findViewById<View>(R.id.fp_item_delete).setOnClickListener {
                deleteFingerprint(index)
            }
            list.addView(item)
        }
        list.onItemsChanged()
    }
}
