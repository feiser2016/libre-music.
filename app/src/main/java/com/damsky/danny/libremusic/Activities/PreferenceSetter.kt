/*
This activity shows the available settings for Libre Music, and allows the user
to configure them.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Activities

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.libremusic.R

class PreferenceSetter : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(LibrePlayer.pitch_black)
        fragmentManager.beginTransaction().replace(android.R.id.content,
                MyPreferenceFragment()).commit()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this) // OnSharedPreferenceChangeListener

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "app_theme_preferences") {
            val value_array = resources.getStringArray(R.array.app_themes_values)
            when (sharedPreferences.getString(key, value_array[0])) {
                value_array[0] -> { // Light Mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    LibrePlayer.pitch_black = R.style.AppTheme
                }
                value_array[1] -> { // Night Mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    LibrePlayer.pitch_black = R.style.AppTheme
                }
                value_array[2] -> { // Black Mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    LibrePlayer.pitch_black = R.style.AppTheme_Black
                }
            }
            finish() // exit the preferences activity (needed in order to apply changes)
        }
    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences) // xml preferences resource file
        }
    }
} // EOF