package com.example.focusease

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ✅ Manager untuk simpan dan load alarm data
 * Data disimpan dalam format JSON di SharedPreferences
 */
object AlarmStorageManager {

    private const val PREFS_NAME = "alarm_prefs"
    private const val KEY_ALARMS = "alarms_list"
    private const val KEY_COUNTER = "alarm_id_counter"

    private val gson = Gson()

    /**
     * Simpan semua alarm ke SharedPreferences
     */
    fun saveAlarms(context: Context, alarms: List<AlarmData>) {
        val prefs = getPrefs(context)
        val json = gson.toJson(alarms)
        prefs.edit().putString(KEY_ALARMS, json).apply()
    }

    /**
     * Load semua alarm dari SharedPreferences
     */
    fun loadAlarms(context: Context): MutableList<AlarmData> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_ALARMS, null) ?: return mutableListOf()

        return try {
            val type = object : TypeToken<MutableList<AlarmData>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    /**
     * Simpan alarm counter (untuk ID alarm berikutnya)
     */
    fun saveAlarmCounter(context: Context, counter: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_COUNTER, counter).apply()
    }

    /**
     * Load alarm counter
     */
    fun loadAlarmCounter(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_COUNTER, 0)
    }

    /**
     * Hapus semua data alarm (untuk reset/clear data)
     */
    fun clearAllAlarms(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
    }

    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

/**
 * ✅ Data class untuk Alarm (harus serializable untuk Gson)
 */
data class AlarmData(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var isActive: Boolean,
    val colorOff: String,
    val colorOn: String,
    var repeatDays: MutableSet<Int> = mutableSetOf(),
    var repeatMode: RepeatMode = RepeatMode.ONCE,
    var alarmName: String = "Alarm",
    var specificDate: Long? = null
)

enum class RepeatMode {
    ONCE,
    SPECIFIC_DATE,
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM
}