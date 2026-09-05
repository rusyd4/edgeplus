package com.edgeplus

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 64)
        }
        scroll.addView(layout)

        statusText = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, 24)
        }
        layout.addView(statusText)

        val overlayBtn = Button(this).apply {
            text = "Grant Overlay Permission"
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
            text = "Enable Accessibility Service"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(a11yBtn)

        val serviceControlLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 32)
        }

        val startBtn = Button(this).apply {
            text = "Start Service"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    statusText.text = "Error: Overlay permission missing"
                    return@setOnClickListener
                }
                startService(Intent(this@MainActivity, EdgeService::class.java))
                updateStatus()
            }
        }
        val stopBtn = Button(this).apply {
            text = "Stop Service"
            setOnClickListener {
                stopService(Intent(this@MainActivity, EdgeService::class.java))
                updateStatus()
            }
        }
        serviceControlLayout.addView(startBtn)
        serviceControlLayout.addView(stopBtn)
        layout.addView(serviceControlLayout)

        // Gesture settings sections
        layout.addView(createSectionHeader("Right Handle Gestures"))
        buildGestureGroup(layout, side = "right", type = "short", labelPrefix = "Short Swipe")
        buildGestureGroup(layout, side = "right", type = "long", labelPrefix = "Long Swipe")

        layout.addView(createSectionHeader("Left Handle Gestures"))
        buildGestureGroup(layout, side = "left", type = "short", labelPrefix = "Short Swipe")
        buildGestureGroup(layout, side = "left", type = "long", labelPrefix = "Long Swipe")

        setContentView(scroll)
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 32, 0, 16)
        }
    }

    private fun buildGestureGroup(
        parent: LinearLayout,
        side: String,
        type: String,
        labelPrefix: String
    ) {
        val directions = listOf(
            "straight" to "$labelPrefix - Inward",
            "up" to "$labelPrefix - Diagonal Up",
            "down" to "$labelPrefix - Diagonal Down"
        )

        val actions = Action.entries
        val actionTitles = actions.map { it.title }

        for ((dir, label) in directions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 16)
            }

            val text = TextView(this).apply {
                this.text = label
                textSize = 14f
            }
            row.addView(text)

            val spinner = Spinner(this)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actionTitles)
            spinner.adapter = adapter

            val currentAction = Prefs.getAction(this, side, type, dir)
            spinner.setSelection(actions.indexOf(currentAction))

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                    Prefs.setAction(this@MainActivity, side, type, dir, actions[position])
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            row.addView(spinner)
            parent.addView(row)
        }
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
        }
    }
}
