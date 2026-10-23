package com.example.testdemo

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 系统权限申请演示页。
 *
 * 演示运行时权限申请的完整流程：
 * 1. 调用 Activity Result API 申请危险权限（此处以 CAMERA 为例）。
 * 2. 授予：更新状态文案。
 * 3. 拒绝但可再次询问（shouldShowRequestPermissionRationale == true）：
 *    弹出解释用途的对话框，引导用户重新授权。
 * 4. 永久拒绝（shouldShowRequestPermissionRationale == false）：
 *    弹出引导对话框，提供"去设置"按钮，跳转系统应用详情页让用户手动开启权限。
 */
class PermissionDemoActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    /**
     * 权限申请回调：根据结果更新状态，并区分"可再次询问"与"永久拒绝"两种拒绝场景。
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateStatus("权限状态：已授予")
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // 用户拒绝但未勾选"不再询问"，仍可再次申请，先解释用途
            showRationaleDialog()
        } else {
            // 用户永久拒绝（勾选了"不再询问"或多次拒绝），只能引导去系统设置
            showPermanentlyDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_demo)

        tvStatus = findViewById(R.id.tv_permission_status)

        findViewById<View>(R.id.btn_request_permission).setOnClickListener {
            requestCameraPermission()
        }
        findViewById<View>(R.id.btn_open_settings).setOnClickListener {
            openAppSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置页返回后，刷新一次权限状态，便于观察手动开启后的结果
        refreshPermissionStatus()
    }

    /** 申请相机权限：已授予则直接更新状态，否则发起权限请求。 */
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                updateStatus("权限状态：已授予")
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /** 根据当前权限状态刷新顶部文案。 */
    private fun refreshPermissionStatus() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        updateStatus(if (granted) "权限状态：已授予" else "权限状态：未授予")
    }

    /** 更新顶部权限状态文案。 */
    private fun updateStatus(text: String) {
        tvStatus.text = text
    }

    /** 解释用途对话框：用户拒绝但仍可再次询问时弹出，引导重新授权。 */
    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("该功能需要使用相机")
            .setPositiveButton("重新申请") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("取消", null)
            .show()



    }

    /** 永久拒绝对话框：用户已勾选"不再询问"，只能引导跳转系统设置页手动开启。 */
    private fun showPermanentlyDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限已被永久拒绝")
            .setMessage("相机权限已被永久拒绝，无法再次弹出授权框。请前往应用设置页手动开启权限。")
            .setPositiveButton("去设置") { _, _ -> openAppSettings() }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 跳转系统应用详情设置页（用户可在此手动开启权限）。 */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        Toast.makeText(this, "已跳转应用设置页", Toast.LENGTH_SHORT).show()
    }
}
