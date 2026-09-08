package com.edgeplus

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.Surface

object ActionExecutor {

    fun execute(context: Context, action: Action) {
        when (action) {
            Action.NONE -> {}
            Action.ROTATE_SCREEN -> {
                rotateScreen(context)
            }
            Action.TOGGLE_AUTO_ROTATE -> {
                toggleAutoRotate(context)
            }
            Action.NOTIFICATIONS -> {
                EdgeAccessibilityService.openNotifications()
            }
            Action.QUICK_PANEL -> {
                EdgeAccessibilityService.openQuickPanel()
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
                EdgeAccessibilityService.lockScreen()
            }
            Action.SCREENSHOT -> {
                EdgeAccessibilityService.takeScreenshot()
            }
            Action.SCREEN_RECORD -> {
                EdgeAccessibilityService.startScreenRecord()
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
            Action.SWITCH_SIDE -> {} // Handled directly in FloatingService with smooth animation
        }
    }

    private fun rotateScreen(context: Context) {
        try {
            val cr = context.contentResolver
            val dm = context.resources.displayMetrics
            val isPortrait = dm.heightPixels >= dm.widthPixels

            // Turn off accelerometer auto-rotation to enforce the manual orientation
            Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
            val targetRotation = if (isPortrait) Surface.ROTATION_90 else Surface.ROTATION_0
            Settings.System.putInt(cr, Settings.System.USER_ROTATION, targetRotation)
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to rotate screen", e)
        }
    }

    private fun toggleAutoRotate(context: Context) {
        try {
            val cr = context.contentResolver
            val current = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
            val newState = if (current == 1) 0 else 1
            Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, newState)
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to toggle auto rotate", e)
        }
    }
}
