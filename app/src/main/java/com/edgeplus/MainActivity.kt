package com.edgeplus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        statusText = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(statusText)

        val overlayBtn = Button(this).apply {
            text = "1. Grant Overlay Permission"
            setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        layout.addView(overlayBtn)

        val a11yBtn = Button(this).apply {
            text = "2. Enable Accessibility Service"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        layout.addView(a11yBtn)

        val startBtn = Button(this).apply {
            text = "3. Start Edge Handle"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    statusText.text = "Error: Overlay permission missing"
                    return@setOnClickListener
                }
                val intent = Intent(this@MainActivity, EdgeService::class.java)
                startService(intent)
                statusText.text = "Edge service running!"
            }
        }
        layout.addView(startBtn)

        val stopBtn = Button(this).apply {
            text = "Stop Edge Handle"
            setOnClickListener {
                stopService(Intent(this@MainActivity, EdgeService::class.java))
                statusText.text = "Edge service stopped."
            }
        }
        layout.addView(stopBtn)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val a11yOk = EdgeAccessibilityService.isRunning()
        statusText.text = buildString {
            append("Overlay Permission: ").append(if (overlayOk) "GRANTED\n" else "MISSING\n")
            append("Accessibility: ").append(if (a11yOk) "ENABLED\n" else "DISABLED\n")
            append("\nGestures on right edge:\n")
            append("- Tap or Swipe Left: Open Volume Panel\n")
            append("- Diagonal Down: Quick Settings / Notification\n")
            append("- Diagonal Up: Turn Off Screen\n")
        }
    }
}
