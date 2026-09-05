package com.edgeplus

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.view.accessibility.AccessibilityEvent

class EdgeAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    companion object {
        private var instance: EdgeAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        fun openQuickPanel(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        }

        fun openNotifications(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun lockScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        }

        fun execute(context: Context, action: Action) {
            when (action) {
                Action.NONE -> {}
                Action.VOLUME_PANEL -> openVolumePanel(context)
                Action.QUICK_PANEL -> openQuickPanel()
                Action.NOTIFICATIONS -> openNotifications()
                Action.SCREEN_OFF -> lockScreen()
                Action.BACK -> instance?.performGlobalAction(GLOBAL_ACTION_BACK)
                Action.HOME -> instance?.performGlobalAction(GLOBAL_ACTION_HOME)
                Action.RECENTS -> instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
        }
    }
}
