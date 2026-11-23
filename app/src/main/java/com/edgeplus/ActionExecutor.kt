package com.edgeplus

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.Surface

object ActionExecutor {

    fun execute(context: Context, action: Action) {
        val rootMode = Prefs.isRootMode(context)
        when (action) {
            Action.NONE -> {}
            Action.ROTATE_SCREEN -> {
                rotateScreen(context, rootMode)
            }
            Action.TOGGLE_AUTO_ROTATE -> {
                toggleAutoRotate(context, rootMode)
            }
            Action.NOTIFICATIONS -> {
                if (rootMode) {
                    RootShell.exec("cmd statusbar expand-notifications")
                } else {
                    EdgeAccessibilityService.openNotifications()
                }
            }
            Action.QUICK_PANEL -> {
                if (rootMode) {
                    RootShell.exec("cmd statusbar expand-settings")
                } else {
                    EdgeAccessibilityService.openQuickPanel()
                }
            }
            Action.VOLUME_UP -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            Action.VOLUME_DOWN -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            Action.VOLUME_PANEL -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            }
            Action.SCREEN_OFF -> {
                if (rootMode) {
                    RootShell.exec("input keyevent 223")
                } else {
                    EdgeAccessibilityService.lockScreen()
                }
            }
            Action.SCREENSHOT -> {
                if (rootMode) {
                    RootShell.exec("input keyevent 120")
                } else {
                    EdgeAccessibilityService.takeScreenshot()
                }
            }
            Action.SCREEN_RECORD -> {
                if (rootMode) {
                    RootShell.exec("am start --user 0 -n com.android.systemui/.screenrecord.ScreenRecordDialog 2>/dev/null || cmd statusbar expand-settings")
                } else {
                    EdgeAccessibilityService.startScreenRecord()
                }
            }
            Action.BACK -> {
                if (rootMode) {
                    RootShell.exec("input keyevent 4")
                } else {
                    EdgeAccessibilityService.back()
                }
            }
            Action.HOME -> {
                if (rootMode) {
                    RootShell.exec("input keyevent 3")
                } else {
                    EdgeAccessibilityService.home()
                }
            }
            Action.RECENTS -> {
                if (rootMode) {
                    RootShell.exec("input keyevent 187")
                } else {
                    EdgeAccessibilityService.recents()
                }
            }
            Action.SWITCH_SIDE -> {} // Handled directly in FloatingService with smooth animation
        }
    }

    private fun rotateScreen(context: Context, rootMode: Boolean) {
        try {
            val cr = context.contentResolver
            val dm = context.resources.displayMetrics
            val isPortrait = dm.heightPixels >= dm.widthPixels
            val targetRotation = if (isPortrait) Surface.ROTATION_90 else Surface.ROTATION_0

            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, targetRotation)
            } else if (rootMode) {
                RootShell.exec("settings put system accelerometer_rotation 0 && settings put system user_rotation $targetRotation")
            }
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to rotate screen", e)
        }
    }

    private fun toggleAutoRotate(context: Context, rootMode: Boolean) {
        try {
            val cr = context.contentResolver
            val current = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
            val newState = if (current == 1) 0 else 1
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, newState)
            } else if (rootMode) {
                RootShell.exec("settings put system accelerometer_rotation $newState")
            }
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to toggle auto rotate", e)
        }
    }
}
