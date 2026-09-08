package com.edgeplus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusCard: LinearLayout
    private lateinit var overlayBadge: TextView
    private lateinit var a11yBadge: TextView
    private lateinit var writeSettingsBadge: TextView
    private lateinit var hzBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request 120Hz on Activity window
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes.preferredRefreshRate = 120f
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this.display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager?.defaultDisplay
                }
                val mode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (mode != null) {
                    window.attributes.preferredDisplayModeId = mode.modeId
                }
            }
        } catch (_: Throwable) {}

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A")) // Slate 900
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 80)
        }
        scroll.addView(root)

        // Header
        val titleText = TextView(this).apply {
            text = "EdgePlus"
            textSize = 26f
            setTextColor(Color.parseColor("#F8FAFC"))
            typeface = Typeface.DEFAULT_BOLD
        }
        val subText = TextView(this).apply {
            text = "Assistive Floating Button (OHO+ Style)"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 4, 0, 24)
        }
        root.addView(titleText)
        root.addView(subText)

        // Status Card
        statusCard = createCardLayout()
        val cardTitle = TextView(this).apply {
            text = "SYSTEM STATUS"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, 16)
        }
        statusCard.addView(cardTitle)

        overlayBadge = createStatusRow(statusCard, "Overlay Permission")
        a11yBadge = createStatusRow(statusCard, "Accessibility Service")
        writeSettingsBadge = createStatusRow(statusCard, "Write Settings (Screen Rotate)")
        hzBadge = createStatusRow(statusCard, "Display Refresh Rate")

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 24, 0, 0)
        }
        val overlayBtn = createButton("Grant Overlay", "#0284C7") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        val a11yBtn = createButton("Enable A11y", "#0F766E") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        val writeBtn = createButton("Grant Rotate", "#475569") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
        btnRow.addView(overlayBtn)
        btnRow.addView(createSpacer(8))
        btnRow.addView(a11yBtn)
        btnRow.addView(createSpacer(8))
        btnRow.addView(writeBtn)
        statusCard.addView(btnRow)

        root.addView(statusCard)
        root.addView(createVerticalSpacer(24))

        // Service Control Buttons
        val serviceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val startBtn = createButton("Start Floating Button", "#0284C7") {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                return@createButton
            }
            startService(Intent(this@MainActivity, FloatingService::class.java))
            updateStatus()
        }
        val stopBtn = createButton("Stop", "#334155") {
            stopService(Intent(this@MainActivity, FloatingService::class.java))
            updateStatus()
        }
        serviceRow.addView(startBtn)
        serviceRow.addView(createSpacer(12))
        serviceRow.addView(stopBtn)
        root.addView(serviceRow)
        root.addView(createVerticalSpacer(28))

        // Appearance Card
        val appearCard = createCardLayout()
        appearCard.addView(createCardHeader("BUTTON APPEARANCE"))

        val vibrateBox = CheckBox(this).apply {
            text = "Haptic feedback on action"
            setTextColor(Color.parseColor("#E2E8F0"))
            isChecked = Prefs.isVibrationEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked ->
                Prefs.setVibrationEnabled(this@MainActivity, isChecked)
            }
            setPadding(8, 12, 0, 16)
        }
        appearCard.addView(vibrateBox)

        buildSlider(appearCard, "Button Size", Prefs.getButtonSizeDp(this), 38, 75, "dp") {
            Prefs.setButtonSizeDp(this, it)
            reloadService()
        }
        buildSlider(appearCard, "Idle Opacity", Prefs.getButtonAlphaIdle(this), 20, 100, "%") {
            Prefs.setButtonAlphaIdle(this, it)
            reloadService()
        }
        root.addView(appearCard)
        root.addView(createVerticalSpacer(24))

        // Gesture Actions Card
        val gestureCard = createCardLayout()
        gestureCard.addView(createCardHeader("BUTTON GESTURE MAPPINGS"))
        val note = TextView(this).apply {
            text = "• Hold and drag to move anywhere (auto-sticks to edge)\n• Swipes and multi-taps trigger actions below"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 16)
        }
        gestureCard.addView(note)

        val gestures = Gesture.entries
        val actions = Action.entries
        val actionTitles = actions.map { it.title }

        for (g in gestures) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 12)
            }

            val label = TextView(this).apply {
                text = g.title
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
            }
            row.addView(label)

            val spinner = Spinner(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#334155"))
                    cornerRadius = 12f
                    setStroke(2, Color.parseColor("#64748B"))
                }
                setPadding(24, 16, 24, 16)
            }

            val adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                actionTitles
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    (v as? TextView)?.apply {
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    return v
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent)
                    (v as? TextView)?.apply {
                        setTextColor(Color.parseColor("#0F172A"))
                        setBackgroundColor(Color.WHITE)
                        textSize = 15f
                        setPadding(32, 24, 32, 24)
                    }
                    return v
                }
            }
            spinner.adapter = adapter

            val currentAction = Prefs.getAction(this, g)
            spinner.setSelection(actions.indexOf(currentAction))

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                    Prefs.setAction(this@MainActivity, g, actions[position])
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            row.addView(spinner)
            gestureCard.addView(row)
        }

        root.addView(gestureCard)
        setContentView(scroll)
    }

    private fun reloadService() {
        val intent = Intent(this, FloatingService::class.java).apply {
            action = FloatingService.ACTION_RELOAD
        }
        startService(intent)
    }

    private fun createCardLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 24f
            }
        }
    }

    private fun createCardHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, 16)
        }
    }

    private fun createStatusRow(parent: LinearLayout, label: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 6, 0, 6)
        }
        val labelView = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.parseColor("#CBD5E1"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueView = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        row.addView(labelView)
        row.addView(valueView)
        parent.addView(row)
        return valueView
    }

    private fun createButton(title: String, bgColor: String, onClick: () -> Unit): Button {
        val density = resources.displayMetrics.density
        val minHeightPx = (44 * density).toInt()
        return Button(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            minHeight = minHeightPx
            layoutParams = LinearLayout.LayoutParams(0, minHeightPx, 1f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 14 * density
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSpacer(sizeDp: Int): View {
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(px, px)
        }
    }

    private fun createVerticalSpacer(sizeDp: Int): View {
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px
            )
        }
    }

    private fun buildSlider(
        parent: LinearLayout,
        title: String,
        current: Int,
        min: Int,
        max: Int,
        unit: String,
        onChanged: (Int) -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        val label = TextView(this).apply {
            text = "$title: $current$unit"
            textSize = 13f
            setTextColor(Color.parseColor("#CBD5E1"))
        }
        row.addView(label)

        val seekBar = SeekBar(this).apply {
            this.max = max - min
            progress = current - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + min
                    label.text = "$title: $value$unit"
                    if (fromUser) onChanged(value)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        row.addView(seekBar)
        parent.addView(row)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        overlayBadge.text = if (overlayOk) "GRANTED" else "MISSING"
        overlayBadge.setTextColor(Color.parseColor(if (overlayOk) "#4ADE80" else "#F87171"))

        val a11yOk = EdgeAccessibilityService.isRunning()
        a11yBadge.text = if (a11yOk) "RUNNING" else "DISABLED"
        a11yBadge.setTextColor(Color.parseColor(if (a11yOk) "#4ADE80" else "#F87171"))

        val writeOk = Settings.System.canWrite(this)
        writeSettingsBadge.text = if (writeOk) "GRANTED" else "MISSING"
        writeSettingsBadge.setTextColor(Color.parseColor(if (writeOk) "#4ADE80" else "#F87171"))

        val currentRate = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.display?.mode?.refreshRate ?: 60f
            } else {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.refreshRate ?: 60f
            }
        } catch (_: Throwable) {
            60f
        }
        hzBadge.text = "${currentRate.toInt()} Hz"
        hzBadge.setTextColor(Color.parseColor("#38BDF8"))
    }
}
