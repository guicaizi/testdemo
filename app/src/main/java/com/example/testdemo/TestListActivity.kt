package com.example.testdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * 测试功能列表页。
 *
 * 列出项目中所有测试 Activity，点击对应按钮跳转进去测试对应功能。
 * 作为应用启动入口。
 */
class TestListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_list)

        findViewById<View>(R.id.btn_main).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<View>(R.id.btn_glass_footer).setOnClickListener {
            startActivity(Intent(this, GlassFooterDemoActivity::class.java))
        }
        findViewById<View>(R.id.btn_system_dialog).setOnClickListener {
            startActivity(Intent(this, SystemDialogDemoActivity::class.java))
        }
        findViewById<View>(R.id.btn_framework_dialog).setOnClickListener {
            startActivity(Intent(this, FrameworkDialogTestActivity::class.java))
        }
        findViewById<View>(R.id.btn_glass_dialog_launch).setOnClickListener {
            startActivity(Intent(this, GlassDialogLaunchActivity::class.java))
        }
        findViewById<View>(R.id.btn_dagger_annotation).setOnClickListener {
            startActivity(Intent(this, DaggerAnnotationActivity::class.java))
        }
        findViewById<View>(R.id.btn_permission_demo).setOnClickListener {
            startActivity(Intent(this, PermissionDemoActivity::class.java))
        }
        // prize add by yanliang for 液态玻璃库 API 演示入口, 20261023-start
        findViewById<View>(R.id.btn_liquid_glass).setOnClickListener {
            startActivity(Intent(this, LiquidGlassDemoActivity::class.java))
        }
        // prize add by yanliang for 液态玻璃库 API 演示入口, 20261023-end
    }
}
