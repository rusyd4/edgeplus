package com.edgeplus

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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
        private val handler = Handler(Looper.getMainLooper())

        fun isRunning(): Boolean = instance != null

        fun openNotifications(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun openQuickPanel(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        }

        fun lockScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        }

        fun takeScreenshot(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) ?: false
        }

        fun back(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        }

        fun home(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun recents(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
        }

        fun startScreenRecord(): Boolean {
            val inst = instance ?: return false
            // 1. Expand Quick Settings
            inst.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            // 2. Search for record screen tile node after short delay and click it
            handler.postDelayed({
                val root = inst.rootInActiveWindow ?: return@postDelayed
                findAndClickRecordTile(root)
            }, 500)
            return true
        }

        private fun findAndClickRecordTile(node: AccessibilityNodeInfo): Boolean {
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            if (desc.contains("record screen") || desc.contains("rekam layar") ||
                text.contains("record screen") || text.contains("rekam layar") ||
                desc.contains("screen record") || text.contains("screen record")
            ) {
                var target = node
                while (!target.isClickable && target.parent != null) {
                    target = target.parent
                }
                if (target.isClickable) {
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (findAndClickRecordTile(child)) return true
            }
            return false
        }
    }
}
