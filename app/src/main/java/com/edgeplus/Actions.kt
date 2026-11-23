package com.edgeplus

import android.content.Context

enum class Gesture(val id: String, val title: String) {
    SINGLE_TAP("single_tap", "Single Tap"),
    DOUBLE_TAP("double_tap", "Double Tap"),
    TRIPLE_TAP("triple_tap", "Triple Tap"),
    SWIPE_INWARD("swipe_inward", "Swipe Inward (Lurus Masuk)"),
    SWIPE_DIAGONAL_UP("swipe_diag_up", "Swipe Diagonal Up (Serong Atas)"),
    SWIPE_DIAGONAL_DOWN("swipe_diag_down", "Swipe Diagonal Down (Serong Bawah)"),
    SWIPE_UP("swipe_up", "Swipe Up (Tegak Atas)"),
    SWIPE_DOWN("swipe_down", "Swipe Down (Tegak Bawah)");

    companion object {
        fun fromId(id: String?): Gesture = entries.firstOrNull { it.id == id } ?: SINGLE_TAP
    }
}

enum class Action(val id: String, val title: String) {
    NONE("none", "None"),
    SWITCH_SIDE("switch_side", "Move to Opposite Edge (Pindah Seberang)"),
    ROTATE_SCREEN("rotate_screen", "Rotate Phone (Portrait ⇄ Landscape)"),
    TOGGLE_AUTO_ROTATE("toggle_auto_rotate", "Toggle Auto-Rotate"),
    NOTIFICATIONS("notifications", "Open Notification Panel"),
    QUICK_PANEL("quick_panel", "Open Quick Settings"),
    VOLUME_UP("volume_up", "Volume Up"),
    VOLUME_DOWN("volume_down", "Volume Down"),
    VOLUME_PANEL("volume_panel", "Open Volume Panel"),
    SCREEN_OFF("screen_off", "Turn Off Screen (Fingerprint OK)"),
    SCREENSHOT("screenshot", "Take Screenshot"),
    SCREEN_RECORD("screen_record", "Start Screen Recording"),
    BACK("back", "Back"),
    HOME("home", "Home"),
    RECENTS("recents", "Recent Apps");

    companion object {
        fun fromId(id: String?): Action = entries.firstOrNull { it.id == id } ?: NONE
    }
}

object Prefs {
    private const val PREFS_NAME = "edgeplus_prefs"

    fun getAction(context: Context, gesture: Gesture): Action {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultAction = defaultActionFor(gesture)
        val raw = prefs.getString("gesture_${gesture.id}", defaultAction.id)
        return Action.fromId(raw)
    }

    fun setAction(context: Context, gesture: Gesture, action: Action) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("gesture_${gesture.id}", action.id)
            .apply()
    }

    fun getButtonSizeDp(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("button_size_dp", 52)
    }

    fun setButtonSizeDp(context: Context, size: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("button_size_dp", size)
            .apply()
    }

    fun getButtonAlphaIdle(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("button_alpha_idle", 55)
    }

    fun setButtonAlphaIdle(context: Context, alpha: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("button_alpha_idle", alpha)
            .apply()
    }

    fun getLastPositionYPercent(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat("button_pos_y_pct", 0.5f)
    }

    fun setLastPositionYPercent(context: Context, yPct: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat("button_pos_y_pct", yPct)
            .apply()
    }

    fun isAttachedRight(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("button_is_right", true)
    }

    fun setAttachedRight(context: Context, isRight: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("button_is_right", isRight)
            .apply()
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("vibration_enabled", true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("vibration_enabled", enabled)
            .apply()
    }

    fun isRootMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("root_mode", false)
    }

    fun setRootMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("root_mode", enabled)
            .apply()
    }

    private fun defaultActionFor(gesture: Gesture): Action {
        return when (gesture) {
            Gesture.SINGLE_TAP -> Action.VOLUME_PANEL
            Gesture.DOUBLE_TAP -> Action.SCREENSHOT
            Gesture.TRIPLE_TAP -> Action.SCREEN_OFF
            Gesture.SWIPE_INWARD -> Action.BACK
            Gesture.SWIPE_DIAGONAL_UP -> Action.ROTATE_SCREEN
            Gesture.SWIPE_DIAGONAL_DOWN -> Action.NOTIFICATIONS
            Gesture.SWIPE_UP -> Action.VOLUME_UP
            Gesture.SWIPE_DOWN -> Action.VOLUME_DOWN
        }
    }
}
