package com.edgeplus

import android.content.Context

enum class Action(val id: String, val title: String) {
    NONE("none", "None"),
    BACK("back", "Back"),
    VOLUME_PANEL("volume_panel", "Open Volume Panel"),
    NOTIFICATIONS("notifications", "Open Notification Panel"),
    TOGGLE_RINGER("toggle_ringer", "Toggle Ringer (Sound / Vibrate)"),
    BRIGHTNESS_SLIDER("brightness_slider", "Brightness Control (Drag Up/Down)");

    companion object {
        fun fromId(id: String?): Action = entries.firstOrNull { it.id == id } ?: NONE
    }
}

object Prefs {
    private const val PREFS_NAME = "edgeplus_prefs"

    fun getAction(context: Context, side: String, type: String, dir: String): Action {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultAction = defaultActionFor(side, type, dir)
        val raw = prefs.getString("${side}_${type}_${dir}", defaultAction.id)
        return Action.fromId(raw)
    }

    fun setAction(context: Context, side: String, type: String, dir: String, action: Action) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("${side}_${type}_${dir}", action.id)
            .apply()
    }

    fun getHandleWidthDp(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("handle_width_dp", 24)
    }

    fun setHandleWidthDp(context: Context, width: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("handle_width_dp", width)
            .apply()
    }

    fun getHandleHeightPercent(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("handle_height_percent", 100)
    }

    fun setHandleHeightPercent(context: Context, percent: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("handle_height_percent", percent)
            .apply()
    }

    fun getHandleAlphaPercent(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("handle_alpha_percent", 10)
    }

    fun setHandleAlphaPercent(context: Context, alpha: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("handle_alpha_percent", alpha)
            .apply()
    }

    fun isVibrateOnShortSwipe(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("vibrate_short", true)
    }

    fun setVibrateOnShortSwipe(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("vibrate_short", enabled)
            .apply()
    }

    private fun defaultActionFor(side: String, type: String, dir: String): Action {
        return if (type == "short") {
            when (dir) {
                "straight" -> Action.BACK
                "down" -> Action.VOLUME_PANEL
                "up" -> Action.NONE
                else -> Action.NONE
            }
        } else {
            // long swipe
            when (dir) {
                "down" -> Action.NOTIFICATIONS
                "straight" -> Action.BRIGHTNESS_SLIDER
                "up" -> Action.TOGGLE_RINGER
                else -> Action.NONE
            }
        }
    }
}
