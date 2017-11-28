package com.damsky.danny.libremusic.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.intro.IntroActivity
import com.damsky.danny.libremusic.ui.splash.SplashPresenter.Companion.START_INTRO_REQUEST
import com.damsky.danny.libremusic.ui.splash.SplashPresenter.Companion.START_MAIN_REQUEST
import com.damsky.danny.libremusic.ui.splash.SplashPresenter.Companion.scanSongs

/**
 * The first activity to start when the app is launched.
 * This activity is in charge of making sure the DB and preferences are set-up before continuing.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if ((application as App).preferencesHelper.isFirstRun())
            startActivityForResult(Intent(this, IntroActivity::class.java),
                    START_INTRO_REQUEST)
        else
            onActivityResult(START_INTRO_REQUEST, Activity.RESULT_OK, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            finish()
        else {
            if (requestCode == START_MAIN_REQUEST)
                (application as App).appDbHelper.deleteAll()

            scanSongs()
        }
    }
}
