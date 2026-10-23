package com.example.testdemo

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 系统权限申请仪器化测试。
 *
 * 验证运行时权限申请的完整流程：
 * 1. 首次拒绝后弹出用途解释对话框。
 * 2. 永久拒绝后弹出引导对话框，点击"去设置"跳转系统应用详情页。
 *
 * 系统权限弹窗属于系统级 UI，Espresso 无法直接操作，
 * 因此借助 UiAutomator 点击"拒绝"按钮与"不再询问"复选框。
 */
@RunWith(AndroidJUnit4::class)
class PermissionTest {

    private lateinit var device: UiDevice
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetPackage = instrumentation.targetContext.packageName

    @Before
    fun setup() {
        device = UiDevice.getInstance(instrumentation)
        // 测试前重置相机权限状态，确保从"未授权"开始
        instrumentation.uiAutomation.revokeRuntimePermission(
            targetPackage,
            Manifest.permission.CAMERA
        )
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        // 测试结束后再次重置权限，避免影响其他用例
        instrumentation.uiAutomation.revokeRuntimePermission(
            targetPackage,
            Manifest.permission.CAMERA
        )
    }

    /**
     * 测试：永久拒绝相机权限后，点击"去设置"会跳转系统应用详情页。
     *
     * 流程：申请权限 → 拒绝（首次，弹用途解释框）→ 重新申请 → 再次拒绝（永久拒绝）
     * → 弹引导框 → 点击"去设置" → 校验跳转 Intent 的 action 与 data。
     */
    @Test
    fun permanentlyDenied_clickGoToSettings_opensApplicationDetailsSettings() {
        ActivityScenario.launch(PermissionDemoActivity::class.java).use { scenario ->
            scenario.onActivity {
                // 确认权限已被重置为未授予
                val granted = it.checkSelfPermission(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                assert(!granted) { "测试前置条件失败：权限未被重置为未授予" }
            }

            // 1. 点击申请权限按钮
            onView(withId(R.id.btn_request_permission)).perform(click())

            // 2. 首次拒绝（不勾选"不再询问"），确保走用途解释分支
            denyPermissionOnSystemDialog(permanent = false)

            // 3. 首次拒绝后应弹出用途解释对话框，点击"重新申请"再次发起请求
            onView(withText("重新申请")).check(matches(isDisplayed()))
            onView(withText("重新申请")).perform(click())

            // 4. 再次拒绝并勾选"不再询问"（旧版本），此时为永久拒绝
            denyPermissionOnSystemDialog(permanent = true)

            // 5. 永久拒绝后应弹出引导对话框，点击"去设置"
            onView(withText("去设置")).check(matches(isDisplayed()))
            onView(withText("去设置")).perform(click())

            // 6. 校验跳转 Intent：action 为应用详情设置页，data 为本应用包名
            intended(
                allOf(
                    hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                    hasData("package:$targetPackage")
                )
            )
        }
    }

    /**
     * 操作系统权限弹窗：点击拒绝按钮。
     *
     * @param permanent 是否永久拒绝。为 true 时，旧版本（API < 30）会勾选"不再询问"复选框，
     *                  使一次拒绝即永久拒绝；API 30+ 无复选框，需拒绝两次才永久拒绝。
     */
    private fun denyPermissionOnSystemDialog(permanent: Boolean) {
        // 等待系统权限弹窗出现
        val denyButton = device.findObject(
            UiSelector().resourceIdMatches(".*permission_deny_button")
        )
        if (!denyButton.waitForExists(3000)) {
            throw AssertionError("未找到系统权限弹窗的拒绝按钮")
        }

        // 旧版本（API < 30）存在"不再询问"复选框，按需勾选以实现永久拒绝
        if (permanent) {
            val doNotAskCheckbox = device.findObject(
                UiSelector().resourceIdMatches(".*do_not_ask_checkbox")
            )
            if (doNotAskCheckbox.exists() && !doNotAskCheckbox.isChecked) {
                doNotAskCheckbox.click()
            }
        }

        denyButton.click()
    }
}
