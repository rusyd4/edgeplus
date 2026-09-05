package com.edgeplus

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

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

    fun triggerBack() {
        Thread {
            try {
                // Method 1: InputManager injectKeyEvent (via INJECT_EVENTS grant)
                val imClass = Class.forName("android.hardware.input.InputManager")
                val getInstanceMethod = imClass.getMethod("getInstance")
                val im = getInstanceMethod.invoke(null)
                val injectMethod = imClass.getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)

                val downTime = SystemClock.uptimeMillis()
                val eventDown = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)
                val eventUp = KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0)

                injectMethod.invoke(im, eventDown, 0)
                injectMethod.invoke(im, eventUp, 0)
            } catch (e: Throwable) {
                // Method 2: Instrumentation fallback
                try {
                    android.app.Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                } catch (t: Throwable) {
                    android.util.Log.e("ActionExecutor", "Failed to inject back", t)
                }
            }
        }.start()
    }

    fun execute(context: Context, action: Action) {
        when (action) {
            Action.NONE -> {}
            Action.BACK -> triggerBack()
            Action.VOLUME_PANEL -> openVolumePanel(context)
            Action.NOTIFICATIONS -> openNotificationPanel(context)
        }
    }
}
