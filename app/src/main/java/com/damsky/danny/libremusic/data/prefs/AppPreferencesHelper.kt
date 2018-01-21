package com.damsky.danny.libremusic.data.prefs

import android.content.Context
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.utils.Constants

/**
 * This class is used to handle preferences operations.
 *
 * @param context Required to gain access to the application's preferences.
 *
 * @author Danny Damsky
 * @since 2018-01-21
 */

class AppPreferencesHelper(private val context: Context) {
    private val preferences = context.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE)

    private var repeat = preferences.getBoolean(Constants.PREFERENCE_REPEAT, false)
    private var shuffle = preferences.getBoolean(Constants.PREFERENCE_SHUFFLE, false)

    private fun updateBoolean(prefName: String, boolean: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(prefName, boolean)
        editor.apply()
    }

    fun isFirstRun(boolean: Boolean) = updateBoolean(Constants.PREFERENCE_FIRST_RUN, boolean)

    fun isFirstRun(): Boolean = preferences.getBoolean(Constants.PREFERENCE_FIRST_RUN, true)

    fun updateIndexes(listLevel: ListLevel, positions: IntArray) {
        val editor = preferences.edit()
        editor.putInt(Constants.PREFERENCE_LIST_LEVEL, listLevel.index)
        editor.putInt(Constants.PREFERENCE_INDEX_ZERO, positions[0])
        editor.putInt(Constants.PREFERENCE_INDEX_ONE, positions[1])
        editor.putInt(Constants.PREFERENCE_INDEX_TWO, positions[2])
        editor.apply()
    }

    fun updateIndex(position: Int) {
        val editor = preferences.edit()
        editor.putInt(Constants.PREFERENCE_INDEX_TWO, position)
        editor.apply()
    }

    fun getIndexes(): Pair<IntArray, ListLevel>? {
        val listLevel = ListLevel.values()[preferences.getInt(Constants.PREFERENCE_LIST_LEVEL, 0)]
        val index = preferences.getInt(Constants.PREFERENCE_INDEX_TWO, -1)
        if (index == -1)
            return null

        val positions = intArrayOf(preferences.getInt(Constants.PREFERENCE_INDEX_ZERO, -1),
                preferences.getInt(Constants.PREFERENCE_INDEX_ONE, -1), index)

        return Pair(positions, listLevel)
    }

    fun setRepeatPreference(boolean: Boolean) {
        repeat = boolean
        updateBoolean(Constants.PREFERENCE_REPEAT, boolean)
    }

    fun getRepeatPreference(): Boolean = repeat

    fun setShufflePreference(boolean: Boolean) {
        shuffle = boolean
        updateBoolean(Constants.PREFERENCE_SHUFFLE, boolean)
    }

    fun getShufflePreference(): Boolean = shuffle

    fun detectAppTheme(valueArray: Array<String>): Pair<Boolean, Int> =
            when (PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.PREFERENCE_APP_THEME, valueArray[0])) {
                valueArray[0] -> Pair(false, AppCompatDelegate.MODE_NIGHT_NO)
                valueArray[1] -> Pair(false, AppCompatDelegate.MODE_NIGHT_YES)
                valueArray[2] -> Pair(true, AppCompatDelegate.MODE_NIGHT_YES)
                else -> Pair(false, -1)
            }

    fun getEncoding(): String = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.PREFERENCE_ENCODING, Constants.DEFAULT_ENCODING)
}
