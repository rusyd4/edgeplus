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
import android.widget.SeekBar
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
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
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

        // Handle appearance & sizing section
        layout.addView(createSectionHeader("Handle Size & Visibility"))
        buildSlider(
            parent = layout,
            title = "Handle Width (dp)",
            current = Prefs.getHandleWidthDp(this),
            min = 10,
            max = 60
        ) { value ->
            Prefs.setHandleWidthDp(this, value)
            reloadService()
        }

        buildSlider(
            parent = layout,
            title = "Handle Height (% of screen, 100 = full)",
            current = Prefs.getHandleHeightPercent(this),
            min = 20,
            max = 100
        ) { value ->
            Prefs.setHandleHeightPercent(this, value)
            reloadService()
        }

        buildSlider(
            parent = layout,
            title = "Handle Transparency (0 = invisible, 100 = solid)",
            current = Prefs.getHandleAlphaPercent(this),
            min = 0,
            max = 100
        ) { value ->
            Prefs.setHandleAlphaPercent(this, value)
            reloadService()
        }

        // Gesture settings sections
        layout.addView(createSectionHeader("Right Handle Gestures"))
        buildGestureGroup(layout, side = "right", type = "short", labelPrefix = "Short Swipe")
        buildGestureGroup(layout, side = "right", type = "long", labelPrefix = "Long Swipe")

        layout.addView(createSectionHeader("Left Handle Gestures"))
        buildGestureGroup(layout, side = "left", type = "short", labelPrefix = "Short Swipe")
        buildGestureGroup(layout, side = "left", type = "long", labelPrefix = "Long Swipe")

        setContentView(scroll)
    }

    private fun reloadService() {
        val intent = Intent(this, EdgeService::class.java).apply {
            action = EdgeService.ACTION_RELOAD
        }
        startService(intent)
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 32, 0, 16)
        }
    }

    private fun buildSlider(
        parent: LinearLayout,
        title: String,
        current: Int,
        min: Int,
        max: Int,
        onChanged: (Int) -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 16)
        }
        val label = TextView(this).apply {
            text = "$title: $current"
            textSize = 14f
        }
        row.addView(label)

        val seekBar = SeekBar(this).apply {
            this.max = max - min
            progress = current - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + min
                    label.text = "$title: $value"
                    if (fromUser) {
                        onChanged(value)
                    }
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        row.addView(seekBar)
        parent.addView(row)
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
