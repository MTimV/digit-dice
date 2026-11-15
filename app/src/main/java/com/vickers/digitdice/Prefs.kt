package com.vickers.digitdice

import android.content.Context

enum class Mode(val label: String) {
    Winner("Winner"),
    Loser("Loser")
}

enum class Theme(val label: String) {
    Dots("Dots"),
    Dinos("Dinos"),
    Unicorns("Unicorns")
}

/** Settings survive a restart. Nothing here is worth a database. */
class Prefs(context: Context) {

    private val store = context.getSharedPreferences("digitdice", Context.MODE_PRIVATE)

    var mode: Mode
        get() {
            val saved = store.getString(KEY_MODE, null) ?: return Mode.Winner
            return Mode.entries.firstOrNull { it.name == saved } ?: Mode.Winner
        }
        set(value) = store.edit().putString(KEY_MODE, value.name).apply()

    var theme: Theme
        get() {
            val saved = store.getString(KEY_THEME, null) ?: return Theme.Dots
            return Theme.entries.firstOrNull { it.name == saved } ?: Theme.Dots
        }
        set(value) = store.edit().putString(KEY_THEME, value.name).apply()

    var holdMillis: Int
        get() = store.getInt(KEY_HOLD, 2500)
        set(value) = store.edit().putInt(KEY_HOLD, value).apply()

    var haptics: Boolean
        get() = store.getBoolean(KEY_HAPTICS, true)
        set(value) = store.edit().putBoolean(KEY_HAPTICS, value).apply()

    var sound: Boolean
        get() = store.getBoolean(KEY_SOUND, true)
        set(value) = store.edit().putBoolean(KEY_SOUND, value).apply()

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_THEME = "theme"
        const val KEY_HOLD = "hold_millis"
        const val KEY_HAPTICS = "haptics"
        const val KEY_SOUND = "sound"
    }
}
