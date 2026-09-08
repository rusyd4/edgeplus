package com.edgeplus

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class AppLauncherOverlay(private val context: Context) {

    data class AppItem(val label: String, val packageName: String, val icon: Drawable)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var appList = listOf<AppItem>()

    init {
        loadApps()
    }

    private fun loadApps() {
        executor.execute {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            val items = resolved.mapNotNull { info ->
                try {
                    val label = info.loadLabel(pm).toString()
                    val pkg = info.activityInfo.packageName
                    val icon = info.loadIcon(pm)
                    AppItem(label, pkg, icon)
                } catch (_: Throwable) {
                    null
                }
            }.sortedBy { it.label.lowercase() }

            mainHandler.post {
                appList = items
            }
        }
    }

    fun show() {
        if (overlayView != null) return
        val density = context.resources.displayMetrics.density

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.argb(245, 15, 23, 42)) // Slate 900
                cornerRadius = 28 * density
                setStroke((2 * density).toInt(), Color.parseColor("#38BDF8"))
            }
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        val title = TextView(context).apply {
            text = "FLOATING APPS"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
            setOnClickListener { hide() }
        }
        header.addView(title)
        header.addView(closeBtn)
        root.addView(header)

        val gridView = GridView(context).apply {
            numColumns = 4
            verticalSpacing = (12 * density).toInt()
            horizontalSpacing = (8 * density).toInt()
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
        }

        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = appList.size
            override fun getItem(p: Int): AppItem = appList[p]
            override fun getItemId(p: Int): Long = p.toLong()

            override fun getView(p: Int, convertView: View?, parent: ViewGroup?): View {
                val item = getItem(p)
                val cell = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
                    background = GradientDrawable().apply {
                        cornerRadius = 14 * density
                    }
                }
                val iconView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
                    setImageDrawable(item.icon)
                }
                val labelView = TextView(context).apply {
                    text = item.label
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), 0)
                }
                cell.addView(iconView)
                cell.addView(labelView)

                cell.setOnClickListener {
                    ActionExecutor.launchAppInFloatingWindow(context, item.packageName)
                    hide()
                }
                return cell
            }
        }

        gridView.adapter = adapter
        root.addView(gridView)

        val dm = context.resources.displayMetrics
        val width = (dm.widthPixels * 0.90f).toInt()
        val height = (dm.heightPixels * 0.65f).toInt()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hide()
                true
            } else {
                false
            }
        }

        try {
            windowManager.addView(root, params)
            overlayView = root
        } catch (_: Throwable) {}
    }

    fun hide() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Throwable) {}
        }
        overlayView = null
    }
}
