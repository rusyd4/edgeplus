package com.edgeplus

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.AudioManager

object ActionExecutor {

    fun execute(
        context: Context,
        action: Action,
        onShowAppLauncher: () -> Unit = {}
    ) {
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
            Action.APP_LAUNCHER -> {
                onShowAppLauncher()
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

    fun launchAppInFloatingWindow(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            } ?: return

            val options = ActivityOptions.makeBasic()
            // Windowing mode 5 = WINDOWING_MODE_FREEFORM
            try {
                val method = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, 5)
            } catch (_: Throwable) {}

            val dm = context.resources.displayMetrics
            val width = dm.widthPixels
            val height = dm.heightPixels

            val left = (width * 0.10f).toInt()
            val top = (height * 0.15f).toInt()
            val right = (width * 0.90f).toInt()
            val bottom = (height * 0.85f).toInt()
            options.launchBounds = Rect(left, top, right, bottom)

            context.startActivity(intent, options.toBundle())
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to launch freeform", e)
            try {
                val fallbackIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fallbackIntent != null) context.startActivity(fallbackIntent)
            } catch (_: Throwable) {}
        }
    }
}
