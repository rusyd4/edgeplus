package com.edgeplus

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class EdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var handleView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupEdgeHandle()
    }

    private fun setupEdgeHandle() {
        val density = resources.displayMetrics.density
        val widthPx = (24 * density).toInt()
        val heightPx = (180 * density).toInt()

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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        val handle = View(this).apply {
            setBackgroundColor(Color.argb(50, 0, 150, 255))
            setOnTouchListener(object : View.OnTouchListener {
                private var startX = 0f
                private var startY = 0f
                private var startTime = 0L

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            startTime = System.currentTimeMillis()
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            val dt = System.currentTimeMillis() - startTime
                            handleGesture(dx, dy, dt)
                            return true
                        }
                    }
                    return false
                }
            })
        }

        handleView = handle
        windowManager?.addView(handle, params)
    }

    private fun handleGesture(dx: Float, dy: Float, dt: Long) {
        val minSwipeDist = 50f
        // ponytail: fixed gesture map. swipe left = volume, diagonal down = quick settings, diagonal up = screen off.
        when {
            // Tap / short touch without swipe: open volume panel
            abs(dx) < minSwipeDist && abs(dy) < minSwipeDist -> {
                EdgeAccessibilityService.openVolumePanel(this)
            }
            // Swipe left (inward from right edge)
            dx < -minSwipeDist -> {
                when {
                    dy > minSwipeDist -> EdgeAccessibilityService.openQuickPanel()
                    dy < -minSwipeDist -> EdgeAccessibilityService.lockScreen()
                    else -> EdgeAccessibilityService.openVolumePanel(this)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handleView?.let { windowManager?.removeView(it) }
        handleView = null
    }
}
