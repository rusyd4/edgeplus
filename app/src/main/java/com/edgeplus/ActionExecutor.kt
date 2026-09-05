package com.edgeplus

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

object ActionExecutor {

    fun openVolumePanel(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        am?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    fun toggleRingerMode(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                AudioManager.RINGER_MODE_VIBRATE, AudioManager.RINGER_MODE_SILENT -> {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
            // Show UI confirmation via volume panel flag
            am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to toggle ringer mode", e)
        }
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

    fun openSmartSidebar(context: Context) {
        Thread {
            try {
                val imClass = Class.forName("android.hardware.input.InputManager")
                val getInstanceMethod = imClass.getMethod("getInstance")
                val im = getInstanceMethod.invoke(null)
                val injectMethod = imClass.getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)

                val dm = context.resources.displayMetrics
                val screenWidth = dm.widthPixels.toFloat()

                // Target the Vivo side_dock_gesture_bar touch area (X: edge, Y: center-top)
                val startX = screenWidth - 2f
                val endX = screenWidth - (120f * dm.density)
                val y = 480f * (dm.heightPixels / 2400f)

                val downTime = SystemClock.uptimeMillis()

                fun injectTouch(action: Int, x: Float, y: Float, eventTime: Long) {
                    val event = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        action,
                        x,
                        y,
                        1f,
                        1f,
                        0,
                        1f,
                        1f,
                        0,
                        0
                    ).apply {
                        source = InputDevice.SOURCE_TOUCHSCREEN
                    }
                    injectMethod.invoke(im, event, 0)
                    event.recycle()
                }

                injectTouch(MotionEvent.ACTION_DOWN, startX, y, downTime)

                // Drag inward and hold slightly to trigger Vivo's gesture dock threshold
                for (i in 1..10) {
                    Thread.sleep(20)
                    val currTime = SystemClock.uptimeMillis()
                    val currX = startX + (endX - startX) * (i / 10f)
                    injectTouch(MotionEvent.ACTION_MOVE, currX, y, currTime)
                }

                Thread.sleep(400) // Hold duration required by Vivo SideSlide
                val upTime = SystemClock.uptimeMillis()
                injectTouch(MotionEvent.ACTION_UP, endX, y, upTime)

            } catch (e: Throwable) {
                android.util.Log.e("ActionExecutor", "Failed to trigger Smart Sidebar", e)
            }
        }.start()
    }

    fun triggerBack() {
        Thread {
            try {
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
            Action.TOGGLE_RINGER -> toggleRingerMode(context)
            Action.SMART_SIDEBAR -> openSmartSidebar(context)
        }
    }
}
