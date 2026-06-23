package com.example.locationtodo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class HabitStore(context: Context) {
    private val prefs = context.getSharedPreferences("habit_tracker", Context.MODE_PRIVATE)

    fun all(): List<Habit> {
        val raw = prefs.getString(KEY_HABITS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val completed = item.optJSONArray("completedDates") ?: JSONArray()
                add(
                    Habit(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        completedDates = buildSet {
                            for (dateIndex in 0 until completed.length()) {
                                add(completed.getString(dateIndex))
                            }
                        }
                    )
                )
            }
        }
    }

    fun add(name: String): Habit {
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            completedDates = emptySet()
        )
        save(all() + habit)
        return habit
    }

    fun toggleToday(id: String): Boolean {
        val today = LocalDate.now().toString()
        var completedNow = false
        val updated = all().map { habit ->
            if (habit.id != id) {
                habit
            } else if (today in habit.completedDates) {
                completedNow = false
                habit.copy(completedDates = habit.completedDates - today)
            } else {
                completedNow = true
                habit.copy(completedDates = habit.completedDates + today)
            }
        }
        save(updated)
        return completedNow
    }

    fun clear() {
        save(emptyList())
    }

    fun soundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun notificationHour(): Int = prefs.getInt(KEY_NOTIFICATION_HOUR, 20)

    fun notificationMinute(): Int = prefs.getInt(KEY_NOTIFICATION_MINUTE, 0)

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NOTIFICATION_HOUR, hour)
            .putInt(KEY_NOTIFICATION_MINUTE, minute)
            .apply()
    }

    private fun save(habits: List<Habit>) {
        val array = JSONArray()
        habits.forEach { habit ->
            array.put(
                JSONObject()
                    .put("id", habit.id)
                    .put("name", habit.name)
                    .put("completedDates", JSONArray(habit.completedDates.sorted()))
            )
        }
        prefs.edit().putString(KEY_HABITS, array.toString()).apply()
    }

    private companion object {
        const val KEY_HABITS = "habits"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_NOTIFICATION_HOUR = "notification_hour"
        const val KEY_NOTIFICATION_MINUTE = "notification_minute"
    }
}
