package com.damsky.danny.libremusic.ui.prefs

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R

/**
 * Activity for setting preferences such as App Theme and CUE sheet encoding.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */
class PreferencesActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(
                if ((application as App).preferencesHelper
                        .detectAppTheme(resources.getStringArray(R.array.app_themes_values)).first)
                    R.style.AppTheme_Black
                else
                    R.style.AppTheme
        )
        fragmentManager.beginTransaction().replace(android.R.id.content,
                PreferencesFragment()).commit()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "app_theme_preferences") {
            AppCompatDelegate.setDefaultNightMode((application as App).preferencesHelper
                    .detectAppTheme(resources.getStringArray(R.array.app_themes_values)).second)
            finish()
        }
    }

    class PreferencesFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }
}
