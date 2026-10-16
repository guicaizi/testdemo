package com.example.testdemo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.testdemo.widget.BrightnessCapsuleSlider
import com.example.testdemo.widget.VolumeCapsuleSlider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始亮度设在中间，方便观察胶囊填充
        findViewById<BrightnessCapsuleSlider>(R.id.brightness_slider).progressFraction = 0.5f

        // 初始音量设在中间，方便观察胶囊填充
        findViewById<VolumeCapsuleSlider>(R.id.volume_slider).progressFraction = 0.5f
    }
}
