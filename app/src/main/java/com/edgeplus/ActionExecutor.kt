package com.edgeplus

import android.content.Context
import android.media.AudioManager

object ActionExecutor {

    fun execute(context: Context, action: Action) {
        when (action) {
            Action.NONE -> {}
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
}
