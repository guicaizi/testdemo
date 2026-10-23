// prize add by yanliang for 液态玻璃库 API 使用演示页, 20261023-start
package com.example.testdemo

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kyant.liquidglass2.GlassButton
import com.kyant.liquidglass2.GlassFrameLayout
import com.kyant.liquidglass2.GlassStyle

/**
 * 液态玻璃库（com.kyant.liquidglass）API 使用演示。
 *
 * 演示三个核心控件：
 * - [GlassButton]：胶囊玻璃按钮（blur + vibrancy + 按下跟手高光），点击 Toast 验证回调
 * - 运行时换样式：点击第二个按钮在「胶囊 ↔ 圆角方」两种 GlassStyle 间切换
 * - GlassFrameLayout + GlassTextView：玻璃容器与玻璃文字
 *
 * 注意：玻璃要有内容可折射才明显，布局里垫了 kusai_pin_bg 圆盘背景。
 */
class LiquidGlassDemoActivity : AppCompatActivity() {

    private var squareStyle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 黑底黑状态栏，玻璃观感与 kusai 系列演示一致
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_liquid_glass_demo)


        // prize add by yanliang for 胶囊液态玻璃按钮测试（KusaiPatternFooterView 配方）, 20261023-start
        val capsule = findViewById<com.example.testdemo.widget.KusaiGlassCapsuleView>(
            R.id.kusai_capsule
        )
        capsule.setLabel("胶囊玻璃测试")
        capsule.setOnClickListener {
            Toast.makeText(this, "胶囊玻璃点击", Toast.LENGTH_SHORT).show()
        }
        // prize add by yanliang for 胶囊液态玻璃按钮测试（KusaiPatternFooterView 配方）, 20261023-end
    }
}
// prize add by yanliang for 液态玻璃库 API 使用演示页, 20261023-end
