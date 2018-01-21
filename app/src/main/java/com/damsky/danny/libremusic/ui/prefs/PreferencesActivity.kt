package com.damsky.danny.libremusic.ui.prefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.damsky.danny.dannydamskyutils.Display
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.utils.Constants

/**
 * Activity for setting preferences such as App Theme and CUE sheet encoding.
 *
 * @author Danny Damsky
 * @since 2018-01-21
 */
class PreferencesActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var appReference: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appReference = application as App

        setTheme(appReference.preferencesHelper.getTheme())

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
        if (key == Constants.PREFERENCE_APP_THEME) {
            AppCompatDelegate.setDefaultNightMode(appReference.preferencesHelper.getNightMode())
            finish()
        }
    }

    /**
     * This function only runs when user pressed on "Equalizer" in the preferences activity.
     * Overriding this functions prevents the app from crashing in case the equalizer was not
     * found on the device.
     */
    override fun startActivity(intent: Intent) {
        if (intent.action == Constants.ACTION_OPEN_EQUALIZER)
            try {
                super.startActivity(intent)
            } catch (e: Exception) {
                Display(this, R.mipmap.ic_launcher).showSnackShort(R.string.eq_error)
            }
    }

    class PreferencesFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }
}
