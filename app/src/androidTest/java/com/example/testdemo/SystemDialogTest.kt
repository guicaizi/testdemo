package com.example.testdemo

import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 系统 AlertDialog 仪器化测试。
 *
 * 基于 Espresso + ActivityScenario，在真实设备上验证系统弹窗的展示与交互。
 */
@RunWith(AndroidJUnit4::class)
class SystemDialogTest {

    /**
     * 测试一：系统 AlertDialog 的标题、正文与按钮文案能正确展示。
     */
    @Test
    fun systemAlertDialog_titleMessageAndButton_displayed() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("系统提示")
                    .setMessage("这是一条系统弹窗消息")
                    .setPositiveButton("确定", null)
                    .setNegativeButton("取消", null)
                    .show()
            }

            onView(withText("系统提示")).check(matches(isDisplayed()))
            onView(withText("这是一条系统弹窗消息")).check(matches(isDisplayed()))
            onView(withText("确定")).check(matches(isDisplayed()))
            onView(withText("取消")).check(matches(isDisplayed()))
        }
    }

    /**
     * 测试二：点击系统 AlertDialog 的确定按钮后，点击回调被触发。
     */
    @Test
    fun systemAlertDialog_positiveButtonClick_triggersCallback() {
        var positiveClicked = false

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("确认操作")
                    .setMessage("确定要执行此操作吗？")
                    .setPositiveButton("确定") { _, _ -> positiveClicked = true }
                    .setNegativeButton("取消", null)
                    .show()
            }

            onView(withText("确定")).perform(click())

            assert(positiveClicked) { "确定按钮的点击回调未被触发" }
        }
    }
}
