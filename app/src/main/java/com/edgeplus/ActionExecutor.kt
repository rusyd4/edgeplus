package com.edgeplus

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.roundToInt

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

    fun getCurrentBrightness(context: Context): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Throwable) {
            128
        }
    }

    fun isAutoBrightness(context: Context): Boolean {
        return try {
            val mode = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (_: Throwable) {
            false
        }
    }

    fun setBrightness(context: Context, value: Int) {
        try {
            val cr = context.contentResolver
            // If auto-brightness is active, temporarily switch to manual so manual adjustments take effect immediately
            val mode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            }
            val clamped = value.coerceIn(1, 255)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, clamped)

            // Also set auto-brightness adjustment float (-1.0 to 1.0) so systems supporting adaptive brightness respect the offset
            val adj = ((clamped / 255f) * 2f) - 1f
            try {
                Settings.System.putFloat(cr, "screen_auto_brightness_adj", adj)
            } catch (_: Throwable) {}
        } catch (e: Throwable) {
            android.util.Log.e("ActionExecutor", "Failed to write brightness", e)
        }
    }

    fun openSmartSidebar(context: Context) {
        Thread {
            try {
                val imClass = Class.forName("android.hardware.input.InputManager")
                val getInstanceMethod = imClass.getMethod("getInstance")
                val im = getInstanceMethod.invoke(null)
                val injectMethod = imClass.getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)

                // Query Vivo's exact registered gesture range in System Settings
                var dockY = 520f
                try {
                    val rangeStr = Settings.Secure.getString(context.contentResolver, "side_dock_gesture_range")
                    if (!rangeStr.isNullOrEmpty()) {
                        val parts = rangeStr.split(";")
                        if (parts.size >= 4) {
                            val top = parts[1].toFloat()
                            val bottom = parts[3].toFloat()
                            dockY = (top + bottom) / 2f
                        }
                    }
                } catch (_: Throwable) {}

                val dm = context.resources.displayMetrics
                val screenWidth = dm.widthPixels.toFloat()

                val startX = screenWidth - 5f
                val endX = screenWidth - (120f * dm.density)

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

                injectTouch(MotionEvent.ACTION_DOWN, startX, dockY, downTime)

                for (i in 1..15) {
                    Thread.sleep(25)
                    val currTime = SystemClock.uptimeMillis()
                    val currX = startX + (endX - startX) * (i / 15f)
                    injectTouch(MotionEvent.ACTION_MOVE, currX, dockY, currTime)
                }

                Thread.sleep(600) // Vivo requires a longer hold threshold on OriginOS 5
                val upTime = SystemClock.uptimeMillis()
                injectTouch(MotionEvent.ACTION_UP, endX, dockY, upTime)

            } catch (e: Throwable) {
                android.util.Log.e("ActionExecutor", "Failed to trigger Smart Sidebar", e)
            }
        }.start()
    }

    fun startScreenRecording(context: Context) {
        val intentsToTry = listOf(
            // Vivo / OriginOS S-Capture broadcasts & activities
            Intent("com.vivo.smartshot.action.START_RECORD_SCREEN").apply {
                setPackage("com.vivo.smartshot")
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            },
            Intent().apply {
                setClassName("com.vivo.smartshot", "com.vivo.smartshot.ui.ScreenRecordService")
            },
            Intent().apply {
                setClassName("com.vivo.smartshot", "com.vivo.smartshot.ui.FloatWindowService")
            },
            Intent("android.intent.action.MAIN").apply {
                setClassName("com.vivo.smartshot", "com.vivo.smartshot.ui.ScreenRecordActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in intentsToTry) {
            try {
                if (intent.component?.className?.contains("Service") == true) {
                    context.startService(intent)
                    return
                } else if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
                    context.startActivity(intent)
                    return
                } else {
                    context.sendBroadcast(intent)
                }
            } catch (_: Throwable) {}
        }
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
            Action.SCREEN_RECORD -> startScreenRecording(context)
            Action.BRIGHTNESS_SLIDER -> {} // Handled interactively during drag in EdgeService
        }
    }
}
