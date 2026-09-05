package com.edgeplus

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.roundToInt

class EdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var rightHandle: View? = null
    private var leftHandle: View? = null
    private var vibrator: Vibrator? = null

    // Brightness indicator overlay
    private var brightnessView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        reloadHandles()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RELOAD) {
            reloadHandles()
        }
        return START_STICKY
    }

    private fun removeHandles() {
        rightHandle?.let { windowManager?.removeView(it) }
        leftHandle?.let { windowManager?.removeView(it) }
        hideBrightnessIndicator()
        rightHandle = null
        leftHandle = null
    }

    private fun reloadHandles() {
        removeHandles()
        leftHandle = createHandle(isRight = false)
        rightHandle = createHandle(isRight = true)
    }

    @Suppress("DEPRECATION")
    private fun findHighestRefreshRateModeId(display: Display?): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
            val modes = display.supportedModes
            var bestMode = display.mode
            var highestRate = bestMode.refreshRate
            for (m in modes) {
                if (m.refreshRate > highestRate) {
                    highestRate = m.refreshRate
                    bestMode = m
                }
            }
            return bestMode.modeId
        }
        return 0
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHandle(isRight: Boolean): View {
        val dm = resources.displayMetrics
        val density = dm.density
        val screenHeight = dm.heightPixels

        val widthDp = Prefs.getHandleWidthDp(this)
        val heightPercent = Prefs.getHandleHeightPercent(this)
        val alphaPercent = Prefs.getHandleAlphaPercent(this)

        val widthPx = (widthDp * density).toInt().coerceAtLeast(10)
        val heightPx = if (heightPercent >= 100) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            (screenHeight * (heightPercent / 100f)).toInt()
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        display
                    } else {
                        @Suppress("DEPRECATION")
                        windowManager?.defaultDisplay
                    }
                    val modeId = findHighestRefreshRateModeId(display)
                    if (modeId > 0) {
                        preferredDisplayModeId = modeId
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    preferredRefreshRate = 120f
                }
            } catch (_: Throwable) {}
        }

        val alphaVal = ((alphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
        val handle = View(this).apply {
            setBackgroundColor(Color.argb(alphaVal, 56, 189, 248))
            setOnTouchListener(object : View.OnTouchListener {
                private var startX = 0f
                private var startY = 0f
                private var hasVibratedShort = false
                private var hasVibratedLong = false
                private var isDraggingBrightness = false
                private var initialBrightness = 128
                private var lastReportedPercent = -1

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            hasVibratedShort = false
                            hasVibratedLong = false
                            isDraggingBrightness = false
                            lastReportedPercent = -1
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            val inwardDist = if (isRight) -dx else dx
                            val shortSwipeDistPx = 30 * density
                            val longSwipeDistPx = 150 * density

                            if (isDraggingBrightness) {
                                // Real-time drag adjusting brightness: drag up = brighter, drag down = darker
                                val dragY = -dy // Invert so dragging upwards increases
                                val deltaFraction = dragY / (300f * density)
                                val newBrightness = (initialBrightness + (deltaFraction * 255)).roundToInt().coerceIn(1, 255)
                                ActionExecutor.setBrightness(this@EdgeService, newBrightness)

                                val percent = ((newBrightness / 255f) * 100).roundToInt()
                                if (percent != lastReportedPercent) {
                                    lastReportedPercent = percent
                                    showBrightnessIndicator(percent)
                                }
                                return true
                            }

                            if (inwardDist >= shortSwipeDistPx && !hasVibratedShort) {
                                hasVibratedShort = true
                                if (Prefs.isVibrateOnShortSwipe(this@EdgeService)) {
                                    vibrateShort()
                                }
                            }

                            if (inwardDist >= longSwipeDistPx && !hasVibratedLong) {
                                hasVibratedLong = true
                                vibrateLong()

                                // Check if the long-swipe action for this direction is BRIGHTNESS_SLIDER
                                val side = if (isRight) "right" else "left"
                                val dir = getDirection(dx, dy, density, isRight)
                                val assignedAction = Prefs.getAction(this@EdgeService, side, "long", dir)
                                if (assignedAction == Action.BRIGHTNESS_SLIDER) {
                                    isDraggingBrightness = true
                                    initialBrightness = ActionExecutor.getCurrentBrightness(this@EdgeService)
                                    val percent = ((initialBrightness / 255f) * 100).roundToInt()
                                    showBrightnessIndicator(percent)
                                }
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isDraggingBrightness) {
                                isDraggingBrightness = false
                                hideBrightnessIndicator()
                                return true
                            }
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            evaluateGesture(isRight, dx, dy, density)
                            return true
                        }
                    }
                    return false
                }
            })
        }

        windowManager?.addView(handle, params)
        return handle
    }

    private fun getDirection(dx: Float, dy: Float, density: Float, isRight: Boolean): String {
        val inwardDist = if (isRight) -dx else dx
        val absDy = kotlin.math.abs(dy)
        val minVerticalDeltaPx = 35 * density

        return when {
            // If vertical movement is greater than inward horizontal movement AND exceeds 35dp -> diagonal
            absDy >= minVerticalDeltaPx && absDy > inwardDist -> {
                if (dy < 0) "up" else "down"
            }
            // All other forward movement towards the center is a clean straight inward swipe (Back)
            else -> "straight"
        }
    }

    private fun evaluateGesture(isRight: Boolean, dx: Float, dy: Float, density: Float) {
        val inwardDist = if (isRight) -dx else dx
        val minSwipeDistPx = 30 * density
        val longSwipeDistPx = 150 * density

        if (inwardDist < minSwipeDistPx) {
            return
        }

        val type = if (inwardDist >= longSwipeDistPx) "long" else "short"
        val side = if (isRight) "right" else "left"
        val dir = getDirection(dx, dy, density, isRight)

        val action = Prefs.getAction(this, side, type, dir)
        android.util.Log.i("EdgeService", "Gesture trigger: side=$side type=$type dir=$dir action=$action")
        ActionExecutor.execute(this, action)
    }

    private fun showBrightnessIndicator(percent: Int) {
        if (brightnessView == null) {
            val density = resources.displayMetrics.density
            val tv = TextView(this).apply {
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.argb(220, 15, 23, 42)) // Slate-900 with high opacity
                    cornerRadius = 20 * density
                    setStroke(2, Color.parseColor("#38BDF8"))
                }
            }
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            windowManager?.addView(tv, params)
            brightnessView = tv
        }
        brightnessView?.text = "Brightness: $percent%"
    }

    private fun hideBrightnessIndicator() {
        brightnessView?.let { windowManager?.removeView(it) }
        brightnessView = null
    }

    private fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(12, 100))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(12)
        }
    }

    private fun vibrateLong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(28)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeHandles()
    }

    companion object {
        const val ACTION_RELOAD = "com.edgeplus.ACTION_RELOAD"
    }
}
