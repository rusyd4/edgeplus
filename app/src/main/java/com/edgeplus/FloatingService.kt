package com.edgeplus

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.sqrt

class FloatingService : Service() {

    private var windowManager: WindowManager? = null
    private var displayManager: DisplayManager? = null
    private var buttonView: View? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var vibrator: Vibrator? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private var tapRunnable: Runnable? = null
    private var longPressRunnable: Runnable? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            repositionButtonForCurrentOrientation()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        displayManager?.registerDisplayListener(displayListener, mainHandler)
        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {
                repositionButtonForCurrentOrientation()
            }
            override fun onLowMemory() {}
            override fun onTrimMemory(level: Int) {}
        })

        createFloatingButton()
        if (Prefs.isRootMode(this)) {
            RootShell.warmUp()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RELOAD) {
            removeFloatingButton()
            createFloatingButton()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        repositionButtonForCurrentOrientation()
    }

    private fun repositionButtonForCurrentOrientation() {
        val view = buttonView ?: return
        val params = windowParams ?: return

        val screenWidth: Int
        val screenHeight: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val bounds = windowManager!!.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val dm = resources.displayMetrics
            screenWidth = dm.widthPixels
            screenHeight = dm.heightPixels
        }

        val sizePx = params.width
        val isRight = Prefs.isAttachedRight(this)
        val yPct = Prefs.getLastPositionYPercent(this)

        params.x = if (isRight) screenWidth - sizePx else 0
        params.y = ((screenHeight - sizePx) * yPct).toInt().coerceIn(30, (screenHeight - sizePx - 30).coerceAtLeast(30))

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Throwable) {}
    }

    private fun removeFloatingButton() {
        buttonView?.let {
            try { windowManager?.removeView(it) } catch (_: Throwable) {}
        }
        buttonView = null
        windowParams = null
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
    private fun createFloatingButton() {
        val dm = resources.displayMetrics
        val density = dm.density
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels

        val sizeDp = Prefs.getButtonSizeDp(this)
        val sizePx = (sizeDp * density).toInt()
        val idleAlphaPercent = Prefs.getButtonAlphaIdle(this)

        val isRight = Prefs.isAttachedRight(this)
        val yPct = Prefs.getLastPositionYPercent(this)
        val initialY = ((screenHeight - sizePx) * yPct).toInt().coerceIn(50, (screenHeight - sizePx - 50).coerceAtLeast(50))
        val initialX = if (isRight) screenWidth - sizePx else 0

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        display
                    } else {
                        @Suppress("DEPRECATION")
                        windowManager?.defaultDisplay
                    }
                    val modeId = findHighestRefreshRateModeId(display)
                    if (modeId > 0) preferredDisplayModeId = modeId
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    preferredRefreshRate = 120f
                }
            } catch (_: Throwable) {}
        }
        windowParams = params

        val outerContainer = FrameLayout(this)
        val innerCircle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(200, 15, 23, 42)) // Slate 900
                setStroke((2.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            alpha = idleAlphaPercent / 100f
        }

        val centerDot = View(this).apply {
            val dotSize = (10 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#38BDF8"))
            }
            alpha = 0.85f
        }

        outerContainer.addView(innerCircle)
        outerContainer.addView(centerDot)

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        outerContainer.setOnTouchListener(object : View.OnTouchListener {
            private var startRawX = 0f
            private var startRawY = 0f
            private var initialWindowX = 0
            private var initialWindowY = 0
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val curScreenWidth: Int
                val curScreenHeight: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
                    val bounds = windowManager!!.currentWindowMetrics.bounds
                    curScreenWidth = bounds.width()
                    curScreenHeight = bounds.height()
                } else {
                    val dm = resources.displayMetrics
                    curScreenWidth = dm.widthPixels
                    curScreenHeight = dm.heightPixels
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startRawX = event.rawX
                        startRawY = event.rawY
                        initialWindowX = params.x
                        initialWindowY = params.y
                        isDragging = false

                        innerCircle.alpha = 0.95f

                        longPressRunnable = Runnable {
                            isDragging = true
                            vibrateTick(heavy = true)
                            innerCircle.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start()
                        }
                        mainHandler.postDelayed(longPressRunnable!!, 300)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - startRawX
                        val dy = event.rawY - startRawY
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        if (isDragging) {
                            params.x = (initialWindowX + dx).toInt().coerceIn(0, curScreenWidth - sizePx)
                            params.y = (initialWindowY + dy).toInt().coerceIn(0, curScreenHeight - sizePx)
                            try { windowManager?.updateViewLayout(outerContainer, params) } catch (_: Throwable) {}
                        } else {
                            if (dist > touchSlop) {
                                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                        innerCircle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                        innerCircle.alpha = idleAlphaPercent / 100f

                        val dx = event.rawX - startRawX
                        val dy = event.rawY - startRawY
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        if (isDragging) {
                            isDragging = false
                            val currentCenterX = params.x + (sizePx / 2)
                            val snapRight = currentCenterX >= (curScreenWidth / 2)
                            val targetX = if (snapRight) curScreenWidth - sizePx else 0

                            ValueAnimator.ofInt(params.x, targetX).apply {
                                duration = 160
                                interpolator = DecelerateInterpolator()
                                addUpdateListener { va ->
                                    params.x = va.animatedValue as Int
                                    try { windowManager?.updateViewLayout(outerContainer, params) } catch (_: Throwable) {}
                                }
                                start()
                            }

                            Prefs.setAttachedRight(this@FloatingService, snapRight)
                            val finalYPercent = params.y.toFloat() / (curScreenHeight - sizePx).coerceAtLeast(1)
                            Prefs.setLastPositionYPercent(this@FloatingService, finalYPercent)
                            vibrateTick(heavy = false)
                        } else {
                            val density = resources.displayMetrics.density
                            val swipeThreshold = 28 * density
                            val currentIsRight = Prefs.isAttachedRight(this@FloatingService)
                            val inwardDist = if (currentIsRight) -dx else dx
                            val absDy = abs(dy)

                            if (dist >= swipeThreshold) {
                                if (absDy >= swipeThreshold && absDy > (inwardDist * 2.0f)) {
                                    if (dy < 0) dispatchGesture(Gesture.SWIPE_UP) else dispatchGesture(Gesture.SWIPE_DOWN)
                                } else if (inwardDist >= (18 * density)) {
                                    val slope = absDy / inwardDist.coerceAtLeast(1f)
                                    if (absDy >= (16 * density) && slope > 0.40f) {
                                        if (dy < 0) dispatchGesture(Gesture.SWIPE_DIAGONAL_UP) else dispatchGesture(Gesture.SWIPE_DIAGONAL_DOWN)
                                    } else {
                                        dispatchGesture(Gesture.SWIPE_INWARD)
                                    }
                                } else if (absDy >= swipeThreshold) {
                                    if (dy < 0) dispatchGesture(Gesture.SWIPE_UP) else dispatchGesture(Gesture.SWIPE_DOWN)
                                }
                            } else {
                                registerTap()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(outerContainer, params)
            buttonView = outerContainer
        } catch (_: Throwable) {}
    }

    fun moveToOppositeSide() {
        val params = windowParams ?: return
        val view = buttonView ?: return

        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            windowManager!!.currentWindowMetrics.bounds.width()
        } else {
            resources.displayMetrics.widthPixels
        }
        val sizePx = params.width
        val currentIsRight = Prefs.isAttachedRight(this)
        val newIsRight = !currentIsRight
        val targetX = if (newIsRight) screenWidth - sizePx else 0

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 130
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                params.x = va.animatedValue as Int
                try { windowManager?.updateViewLayout(view, params) } catch (_: Throwable) {}
            }
            start()
        }

        Prefs.setAttachedRight(this, newIsRight)
        vibrateTick(heavy = false)
    }

    private fun registerTap() {
        tapCount++
        tapRunnable?.let { mainHandler.removeCallbacks(it) }

        if (tapCount >= 3) {
            tapCount = 0
            dispatchGesture(Gesture.TRIPLE_TAP)
            return
        }

        tapRunnable = Runnable {
            val count = tapCount
            tapCount = 0
            if (count == 1) {
                dispatchGesture(Gesture.SINGLE_TAP)
            } else if (count == 2) {
                dispatchGesture(Gesture.DOUBLE_TAP)
            }
        }
        mainHandler.postDelayed(tapRunnable!!, 280)
    }

    private fun dispatchGesture(gesture: Gesture) {
        val action = Prefs.getAction(this, gesture)
        if (action == Action.NONE) return

        if (Prefs.isVibrationEnabled(this)) {
            vibrateTick(heavy = false)
        }

        if (action == Action.SWITCH_SIDE) {
            moveToOppositeSide()
        } else {
            ActionExecutor.execute(this, action)
        }
    }

    private fun vibrateTick(heavy: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = if (heavy) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK
            vibrator?.vibrate(VibrationEffect.createPredefined(effect))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ms = if (heavy) 30L else 15L
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
        mainHandler.removeCallbacksAndMessages(null)
        RootShell.close()
    }

    companion object {
        const val ACTION_RELOAD = "com.edgeplus.ACTION_RELOAD"
    }
}
