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
    private lateinit var hzBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request 120Hz+ on Activity window
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

        // App Header
        val titleText = TextView(this).apply {
            text = "EdgePlus"
            textSize = 26f
            setTextColor(Color.parseColor("#F8FAFC"))
            typeface = Typeface.DEFAULT_BOLD
        }
        val subText = TextView(this).apply {
            text = "Volume & Notification Edge (Zero Accessibility • 100% Bank Safe)"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 4, 0, 24)
        }
        root.addView(titleText)
        root.addView(subText)

        // Service & Permission Card
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
        hzBadge = createStatusRow(statusCard, "Display Refresh Rate")

        // Quick Permission Action Buttons
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
        btnRow.addView(overlayBtn)
        statusCard.addView(btnRow)

        root.addView(statusCard)
        root.addView(createVerticalSpacer(24))

        // Service Start / Stop Action
        val serviceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val startBtn = createButton("Start Service", "#0284C7") {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                return@createButton
            }
            startService(Intent(this@MainActivity, EdgeService::class.java))
            updateStatus()
        }
        val stopBtn = createButton("Stop Service", "#334155") {
            stopService(Intent(this@MainActivity, EdgeService::class.java))
            updateStatus()
        }
        serviceRow.addView(startBtn)
        serviceRow.addView(createSpacer(16))
        serviceRow.addView(stopBtn)
        root.addView(serviceRow)
        root.addView(createVerticalSpacer(28))

        // Handle Size & Haptic Section Card
        val sizeCard = createCardLayout()
        sizeCard.addView(createCardHeader("HANDLE & FEEDBACK"))

        val vibrateBox = CheckBox(this).apply {
            text = "Vibrate on gesture detection"
            setTextColor(Color.parseColor("#E2E8F0"))
            isChecked = Prefs.isVibrateOnShortSwipe(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked ->
                Prefs.setVibrateOnShortSwipe(this@MainActivity, isChecked)
            }
            setPadding(8, 16, 0, 16)
        }
        sizeCard.addView(vibrateBox)

        buildSlider(sizeCard, "Handle Width", Prefs.getHandleWidthDp(this), 12, 60, "dp") {
            Prefs.setHandleWidthDp(this, it)
            reloadService()
        }
        buildSlider(sizeCard, "Handle Height", Prefs.getHandleHeightPercent(this), 25, 100, "%") {
            Prefs.setHandleHeightPercent(this, it)
            reloadService()
        }
        buildSlider(sizeCard, "Transparency", Prefs.getHandleAlphaPercent(this), 0, 100, "%") {
            Prefs.setHandleAlphaPercent(this, it)
            reloadService()
        }
        root.addView(sizeCard)
        root.addView(createVerticalSpacer(24))

        // Right Handle Gestures Card
        val rightCard = createCardLayout()
        rightCard.addView(createCardHeader("RIGHT HANDLE GESTURES"))
        buildGestureGroup(rightCard, side = "right", type = "short", labelPrefix = "Short Swipe")
        buildGestureGroup(rightCard, side = "right", type = "long", labelPrefix = "Long Swipe")
        root.addView(rightCard)
        root.addView(createVerticalSpacer(24))

        // Left Handle Gestures Card
        val leftCard = createCardLayout()
        leftCard.addView(createCardHeader("LEFT HANDLE GESTURES"))
        buildGestureGroup(leftCard, side = "left", type = "short", labelPrefix = "Short Swipe")
        buildGestureGroup(leftCard, side = "left", type = "long", labelPrefix = "Long Swipe")
        root.addView(leftCard)

        setContentView(scroll)
    }

    private fun reloadService() {
        val intent = Intent(this, EdgeService::class.java).apply {
            action = EdgeService.ACTION_RELOAD
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
            layoutParams = LinearLayout.LayoutParams(
                px,
                px
            )
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

    private fun buildGestureGroup(
        parent: LinearLayout,
        side: String,
        type: String,
        labelPrefix: String
    ) {
        val directions = listOf(
            "down" to "$labelPrefix • Diagonal Down",
            "straight" to "$labelPrefix • Inward",
            "up" to "$labelPrefix • Diagonal Up"
        )
        val actions = Action.entries
        val actionTitles = actions.map { it.title }

        for ((dir, label) in directions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 12)
            }

            val text = TextView(this).apply {
                this.text = label
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
            }
            row.addView(text)

            val spinner = Spinner(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 12f
                    setStroke(2, Color.parseColor("#334155"))
                }
            }
            val adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                actionTitles
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    (v as? TextView)?.apply {
                        setTextColor(Color.parseColor("#F8FAFC"))
                        textSize = 14f
                    }
                    return v
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent)
                    (v as? TextView)?.apply {
                        setTextColor(Color.parseColor("#0F172A"))
                        textSize = 14f
                    }
                    return v
                }
            }
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
        overlayBadge.text = if (overlayOk) "GRANTED" else "MISSING"
        overlayBadge.setTextColor(Color.parseColor(if (overlayOk) "#4ADE80" else "#F87171"))

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
