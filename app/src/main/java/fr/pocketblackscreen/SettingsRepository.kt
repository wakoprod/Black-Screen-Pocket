package fr.pocketblackscreen

import android.content.Context

internal class SettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var durationMinutes: Int
        get() = preferences.getInt("duration_minutes", DEFAULT_MINUTES)
            .takeIf { it in durations } ?: DEFAULT_MINUTES
        set(value) {
            require(value in durations)
            preferences.edit().putInt("duration_minutes", value).apply()
        }

    companion object {
        const val DEFAULT_MINUTES = 120
        val durations = listOf(30, 60, 120, 240)
    }
}
