package com.edgeplus

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager

object ActionExecutor {

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        return dpm?.isAdminActive(adminComponent) == true
    }

    fun lockScreenDeviceAdmin(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        return if (isDeviceAdminActive(context)) {
            dpm?.lockNow()
            true
        } else {
            false
        }
    }

    fun execute(context: Context, action: Action) {
        when (action) {
            Action.NONE -> {}
            Action.VOLUME_PANEL -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            }
            Action.SCREEN_OFF -> {
                // Priority 1: Native Device Admin (100% bypasses accessibility detection)
                val locked = lockScreenDeviceAdmin(context)
                if (!locked) {
                    // Priority 2: Fallback to Accessibility if admin not activated
                    EdgeAccessibilityService.lockScreen()
                }
            }
            Action.QUICK_PANEL -> {
                EdgeAccessibilityService.openQuickPanel()
            }
            Action.NOTIFICATIONS -> {
                EdgeAccessibilityService.openNotifications()
            }
            Action.BACK -> {
                EdgeAccessibilityService.back()
            }
            Action.HOME -> {
                EdgeAccessibilityService.home()
            }
            Action.RECENTS -> {
                EdgeAccessibilityService.recents()
            }
        }
    }
}
