package com.edgeplus

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

class EdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var rightHandle: View? = null
    private var leftHandle: View? = null
    private var vibrator: Vibrator? = null

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
        rightHandle = null
        leftHandle = null
    }

    private fun reloadHandles() {
        removeHandles()
        leftHandle = createHandle(isRight = false)
        rightHandle = createHandle(isRight = true)
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
        }

        val alphaVal = ((alphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
        val handle = View(this).apply {
            setBackgroundColor(Color.argb(alphaVal, 0, 150, 255))
            setOnTouchListener(object : View.OnTouchListener {
                private var startX = 0f
                private var startY = 0f
                private var hasVibratedLong = false

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            hasVibratedLong = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - startX
                            val inwardDist = if (isRight) -dx else dx
                            val longSwipeDistPx = 150 * density
                            if (inwardDist >= longSwipeDistPx && !hasVibratedLong) {
                                hasVibratedLong = true
                                vibrate()
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
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

    private fun evaluateGesture(isRight: Boolean, dx: Float, dy: Float, density: Float) {
        val inwardDist = if (isRight) -dx else dx
        val minSwipeDistPx = 30 * density
        val longSwipeDistPx = 150 * density

        if (inwardDist < minSwipeDistPx) {
            return
        }

        val type = if (inwardDist >= longSwipeDistPx) "long" else "short"
        val side = if (isRight) "right" else "left"

        val diagonalThresholdPx = 40 * density
        val dir = when {
            dy < -diagonalThresholdPx -> "up"
            dy > diagonalThresholdPx -> "down"
            else -> "straight"
        }

        val action = Prefs.getAction(this, side, type, dir)
        EdgeAccessibilityService.execute(this, action)
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20)
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
