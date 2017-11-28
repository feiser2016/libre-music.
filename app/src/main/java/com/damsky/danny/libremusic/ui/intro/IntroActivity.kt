package com.damsky.danny.libremusic.ui.intro

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.intro.IntroPresenter.Companion.addSlides
import com.damsky.danny.libremusic.ui.intro.IntroPresenter.Companion.finishWithResultOk
import com.damsky.danny.libremusic.ui.intro.IntroPresenter.Companion.getColour
import com.damsky.danny.libremusic.ui.intro.IntroPresenter.Companion.isMarshmallowPlus
import com.github.paolorotolo.appintro.AppIntro2

/**
 * This activity features slides which introduce the user to the app.
 * When the activity finishes it makes sure that the app permissions are set up before closing.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class IntroActivity : AppIntro2() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showSkipButton(false)
        showStatusBar(false)
        setIndicatorColor(getColour(R.color.colorAccent), getColour(android.R.color.white))
        setColorTransitionsEnabled(true)

        addSlides()
    }

    override fun onBackPressed() = Unit

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED)
            finish()
        else
            finishWithResultOk()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        if (isMarshmallowPlus)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE),
                    REQUEST_CODE_PERMISSIONS)
        else
            finishWithResultOk()
    }
}
