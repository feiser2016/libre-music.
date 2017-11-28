package com.damsky.danny.libremusic.data.prefs

import android.content.Context
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.libremusic.data.db.ListLevel

/**
 * This class is used to handle preferences operations.
 *
 * @param context Required to gain access to the application's preferences.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class AppPreferencesHelper(private val context: Context) {
    private val preferences = context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE)

    private var repeat = preferences.getBoolean("repeat_preference", false)
    private var shuffle = preferences.getBoolean("shuffle_preference", false)

    private fun updateBoolean(prefName: String, boolean: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(prefName, boolean)
        editor.apply()
    }

    fun isFirstRun(boolean: Boolean) = updateBoolean("first_run_preference", boolean)

    fun isFirstRun(): Boolean = preferences.getBoolean("first_run_preference", true)

    fun updateIndexes(listLevel: ListLevel, positions: IntArray) {
        val editor = preferences.edit()
        editor.putInt("audio_list_level_preference", listLevel.index)
        editor.putInt("audio_position_zero_preference", positions[0])
        editor.putInt("audio_position_one_preference", positions[1])
        editor.putInt("audio_position_two_preference", positions[2])
        editor.apply()
    }

    fun updateIndex(position: Int) {
        val editor = preferences.edit()
        editor.putInt("audio_position_two_preference", position)
        editor.apply()
    }

    fun getIndexes(): Pair<IntArray, ListLevel>? {
        val listLevel = ListLevel.values()[preferences.getInt("audio_list_level_preference", 0)]
        val index = preferences.getInt("audio_position_two_preference", -1)
        if (index == -1)
            return null

        val positions = intArrayOf(preferences.getInt("audio_position_zero_preference", -1),
                preferences.getInt("audio_position_one_preference", -1), index)

        return Pair(positions, listLevel)
    }

    fun setRepeatPreference(boolean: Boolean) {
        repeat = boolean
        updateBoolean("repeat_preference", boolean)
    }

    fun getRepeatPreference(): Boolean = repeat

    fun setShufflePreference(boolean: Boolean) {
        shuffle = boolean
        updateBoolean("shuffle_preference", boolean)
    }

    fun getShufflePreference(): Boolean = shuffle

    fun detectAppTheme(valueArray: Array<String>): Pair<Boolean, Int> =
            when (PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("app_theme_preferences", valueArray[0])) {
                valueArray[0] -> Pair(false, AppCompatDelegate.MODE_NIGHT_NO)
                valueArray[1] -> Pair(false, AppCompatDelegate.MODE_NIGHT_YES)
                valueArray[2] -> Pair(true, AppCompatDelegate.MODE_NIGHT_YES)
                else -> Pair(false, -1)
            }

    fun getEncoding(): String = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("file_encoding", "Cp1251")
}
