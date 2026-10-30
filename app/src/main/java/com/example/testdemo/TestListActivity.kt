package com.example.testdemo

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 测试功能列表页。
 *
 * 列出项目中所有测试 Activity，点击对应按钮跳转进去测试对应功能。
 * 作为应用启动入口。
 */
class TestListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 状态栏/导航栏透明，内容绘制到其下方（盖在布局上面，不预留空间）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // API 29+ 默认会给系统栏加对比度遮罩，透明时看起来像黑块，这里关掉
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        // 深色背景，系统栏图标用浅色（白色）
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(R.layout.activity_test_list)
        // 只给顶部预留状态栏高度，底部不预留：让导航栏直接盖在内容上面
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.test_list_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

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
        findViewById<View>(R.id.btn_glass_share_test).setOnClickListener {
            startActivity(Intent(this, LiquidGlassShareTestActivity::class.java))
        }
        // prize add by yanliang for 液态玻璃库 API 演示入口, 20261023-end

        // prize add by yanliang for 指纹录入环效果 Demo 入口, 20261028-start
        findViewById<View>(R.id.btn_fingerprint_demo).setOnClickListener {
            startActivity(Intent(this, FingerprintDemoActivity::class.java))
        }
        // prize add by yanliang for 指纹录入环效果 Demo 入口, 20261028-end
    }
}
