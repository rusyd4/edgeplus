package com.edgeplus

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import dev.rikka.shizuku.Shizuku
import java.io.OutputStream
import java.util.concurrent.Executors

object ActionExecutor {

    private val executor = Executors.newSingleThreadExecutor()

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int) {
        if (isShizukuAvailable() && !hasShizukuPermission()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun execute(context: Context, action: Action) {
        when (action) {
            Action.NONE -> {}
            Action.VOLUME_PANEL -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            }
            Action.BACK -> executeAction(action, "input keyevent 4") {
                EdgeAccessibilityService.back()
            }
            Action.HOME -> executeAction(action, "input keyevent 3") {
                EdgeAccessibilityService.home()
            }
            Action.RECENTS -> executeAction(action, "input keyevent 187") {
                EdgeAccessibilityService.recents()
            }
            Action.SCREEN_OFF -> executeAction(action, "input keyevent 26") {
                EdgeAccessibilityService.lockScreen()
            }
            Action.QUICK_PANEL -> executeAction(action, "cmd statusbar expand-settings") {
                EdgeAccessibilityService.openQuickPanel()
            }
            Action.NOTIFICATIONS -> executeAction(action, "cmd statusbar expand-notifications") {
                EdgeAccessibilityService.openNotifications()
            }
        }
    }

    private fun executeAction(
        action: Action,
        shellCommand: String,
        fallbackAccessibility: () -> Unit
    ) {
        if (hasShizukuPermission()) {
            runShellCommand(shellCommand)
        } else {
            fallbackAccessibility()
        }
    }

    private fun runShellCommand(cmd: String) {
        executor.execute {
            try {
                val method = Shizuku::class.java.getMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                val process = method.invoke(null, arrayOf("sh"), null, null) as Process
                val os: OutputStream = process.outputStream
                os.write("$cmd\nexit\n".toByteArray())
                os.flush()
                process.waitFor()
            } catch (e: Throwable) {
                // ponytail: fail silent if shell IPC fails
            }
        }
    }
}
