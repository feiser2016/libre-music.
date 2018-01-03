package com.damsky.danny.libremusic.ui.prefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.utils.Display

/**
 * Activity for setting preferences such as App Theme and CUE sheet encoding.
 *
 * @author Danny Damsky
 * @since 2018-01-03
 */
class PreferencesActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var appReference: App
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appReference = application as App

        setTheme(
                if (appReference.preferencesHelper
                        .detectAppTheme(resources.getStringArray(R.array.app_themes_values)).first)
                    R.style.AppTheme_Black
                else
                    R.style.AppTheme
        )

        fragmentManager.beginTransaction().replace(android.R.id.content, PreferencesFragment()).commit()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "app_theme_preferences") {
            AppCompatDelegate.setDefaultNightMode(appReference.preferencesHelper
                    .detectAppTheme(resources.getStringArray(R.array.app_themes_values)).second)
            finish()
        }
    }

    /**
     * This function only runs when user pressed on "Equalizer" in the preferences activity.
     * Overriding this functions prevents the app from crashing in case the equalizer was not
     * found on the device.
     */
    override fun startActivity(intent: Intent) {
        if (intent.action == "android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL")
            try {
                super.startActivity(intent)
            } catch (e: Exception) {
                Display(this).showSnack(R.string.eq_error, Snackbar.LENGTH_SHORT)
            }
    }

    class PreferencesFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }
}
