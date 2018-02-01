package com.damsky.danny.libremusic.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.utils.Constants

/**
 * This class is used to handle preferences operations.
 *
 * @param context Required to gain access to the application's preferences. (Recommended: ApplicationContext)
 *
 * @author Danny Damsky
 * @since 2018-02-01
 */

class AppPreferencesHelper(private val context: Context) {
    private var repeat = getSharedPreferences().getBoolean(Constants.PREFERENCE_REPEAT, false)
    private var shuffle = getSharedPreferences().getBoolean(Constants.PREFERENCE_SHUFFLE, false)

    private fun updateBoolean(prefName: String, boolean: Boolean) {
        val editor = getSharedPreferences().edit()
        editor.putBoolean(prefName, boolean)
        editor.apply()
    }

    private fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun getDefaultSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun isFirstRun(boolean: Boolean) {
        updateBoolean(Constants.PREFERENCE_FIRST_RUN, boolean)
    }

    fun isFirstRun(): Boolean {
        return getSharedPreferences().getBoolean(Constants.PREFERENCE_FIRST_RUN, true)
    }

    fun updateIndexes(listLevel: ListLevel, positions: IntArray) {
        val editor = getSharedPreferences().edit()
        editor.putInt(Constants.PREFERENCE_LIST_LEVEL, listLevel.index)
        editor.putInt(Constants.PREFERENCE_INDEX_ZERO, positions[0])
        editor.putInt(Constants.PREFERENCE_INDEX_ONE, positions[1])
        editor.putInt(Constants.PREFERENCE_INDEX_TWO, positions[2])
        editor.apply()
    }

    fun updateIndex(position: Int) {
        val editor = getSharedPreferences().edit()
        editor.putInt(Constants.PREFERENCE_INDEX_TWO, position)
        editor.apply()
    }

    fun getIndexes(): Pair<IntArray, ListLevel>? {
        val listLevel = ListLevel.values()[getSharedPreferences().getInt(Constants.PREFERENCE_LIST_LEVEL, 0)]
        val index = getSharedPreferences().getInt(Constants.PREFERENCE_INDEX_TWO, -1)
        if (index == -1)
            return null

        val positions = intArrayOf(getSharedPreferences().getInt(Constants.PREFERENCE_INDEX_ZERO, -1),
                getSharedPreferences().getInt(Constants.PREFERENCE_INDEX_ONE, -1), index)

        return Pair(positions, listLevel)
    }

    fun setRepeatPreference(boolean: Boolean) {
        repeat = boolean
        updateBoolean(Constants.PREFERENCE_REPEAT, boolean)
    }

    fun getRepeatPreference(): Boolean {
        return repeat
    }

    fun setShufflePreference(boolean: Boolean) {
        shuffle = boolean
        updateBoolean(Constants.PREFERENCE_SHUFFLE, boolean)
    }

    fun getShufflePreference(): Boolean {
        return shuffle
    }

    fun getTheme(): Int {
        val appThemesValues = context.resources.getStringArray(R.array.app_themes_values)

        if (getDefaultSharedPreferences().getString(Constants.PREFERENCE_APP_THEME, appThemesValues[0]) == appThemesValues[2])
            return R.style.AppTheme_Black

        return R.style.AppTheme
    }

    fun getThemeNoActionBar(): Int {
        val appThemesValues = context.resources.getStringArray(R.array.app_themes_values)

        if (getDefaultSharedPreferences().getString(Constants.PREFERENCE_APP_THEME, appThemesValues[0]) == appThemesValues[2])
            return R.style.AppTheme_BlackNoActionBar

        return R.style.AppTheme_NoActionBar
    }

    fun getNightMode(): Int {
        val appThemesValues = context.resources.getStringArray(R.array.app_themes_values)

        if (getDefaultSharedPreferences().getString(Constants.PREFERENCE_APP_THEME, appThemesValues[0]) == appThemesValues[0])
            return AppCompatDelegate.MODE_NIGHT_NO

        return AppCompatDelegate.MODE_NIGHT_YES
    }

    fun getThemeAndDayNightMode(): Pair<Int, Int> {
        val appThemesValues = context.resources.getStringArray(R.array.app_themes_values)

        when (getDefaultSharedPreferences().getString(Constants.PREFERENCE_APP_THEME, appThemesValues[0])) {
            appThemesValues[0] ->
                return Pair(R.style.AppTheme, AppCompatDelegate.MODE_NIGHT_NO)

            appThemesValues[1] ->
                return Pair(R.style.AppTheme, AppCompatDelegate.MODE_NIGHT_YES)

            appThemesValues[2] ->
                return Pair(R.style.AppTheme_Black, AppCompatDelegate.MODE_NIGHT_YES)
        }

        return Pair(-1, -1)
    }

    fun getThemeAndDayNightModeNoActionBar(): Pair<Int, Int> {
        val appThemesValues = context.resources.getStringArray(R.array.app_themes_values)

        when (getDefaultSharedPreferences().getString(Constants.PREFERENCE_APP_THEME, appThemesValues[0])) {
            appThemesValues[0] ->
                return Pair(R.style.AppTheme_NoActionBar, AppCompatDelegate.MODE_NIGHT_NO)

            appThemesValues[1] ->
                return Pair(R.style.AppTheme_NoActionBar, AppCompatDelegate.MODE_NIGHT_YES)

            appThemesValues[2] ->
                return Pair(R.style.AppTheme_BlackNoActionBar, AppCompatDelegate.MODE_NIGHT_YES)
        }

        return Pair(-1, -1)
    }

    fun getEncoding(): String {
        return getDefaultSharedPreferences().getString(Constants.PREFERENCE_ENCODING, Constants.DEFAULT_ENCODING)
    }

}
