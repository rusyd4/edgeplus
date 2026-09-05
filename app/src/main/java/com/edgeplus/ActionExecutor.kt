package com.edgeplus

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager

object ActionExecutor {

    fun openVolumePanel(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        am?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    @SuppressLint("WrongConstant")
    fun openNotificationPanel(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to expand notifications", e)
        }
    }

    fun execute(context: Context, action: Action) {
        when (action) {
            Action.NONE -> {}
            Action.VOLUME_PANEL -> openVolumePanel(context)
            Action.NOTIFICATIONS, Action.QUICK_PANEL -> openNotificationPanel(context)
        }
    }
}
