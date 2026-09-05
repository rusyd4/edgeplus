package com.edgeplus

import android.content.Context

enum class Action(val id: String, val title: String) {
    NONE("none", "None"),
    VOLUME_PANEL("volume_panel", "Open Volume Panel"),
    QUICK_PANEL("quick_panel", "Open Quick Settings"),
    NOTIFICATIONS("notifications", "Open Notifications"),
    SCREEN_OFF("screen_off", "Turn Off Screen"),
    BACK("back", "Back"),
    HOME("home", "Home"),
    RECENTS("recents", "Recent Apps");

    companion object {
        fun fromId(id: String?): Action = entries.firstOrNull { it.id == id } ?: NONE
    }
}

object Prefs {
    private const val PREFS_NAME = "edgeplus_prefs"

    // Keys format: {side}_{type}_{dir}
    // side: "left", "right"
    // type: "short", "long"
    // dir: "straight", "up", "down"

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

    private fun defaultActionFor(side: String, type: String, dir: String): Action {
        return if (type == "short") {
            when (dir) {
                "straight" -> Action.VOLUME_PANEL
                "up" -> Action.SCREEN_OFF
                "down" -> Action.QUICK_PANEL
                else -> Action.NONE
            }
        } else {
            when (dir) {
                "straight" -> Action.NOTIFICATIONS
                "up" -> Action.SCREEN_OFF
                "down" -> Action.RECENTS
                else -> Action.NONE
            }
        }
    }
}
