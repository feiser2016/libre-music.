package com.damsky.danny.libremusic.ui.intro

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.utils.Constants
import com.github.paolorotolo.appintro.AppIntro2
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage

/**
 * This activity features slides which introduce the user to the app.
 * When the activity finishes it makes sure that the app permissions are set up before closing.
 *
 * @author Danny Damsky
 */

class IntroActivity : AppIntro2() {
    private val slider = SliderPage()

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
        if (Constants.IS_MARSHMALLOW_OR_ABOVE)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE),
                    Constants.REQUEST_CODE_PERMISSIONS)
        else
            finishWithResultOk()
    }

    private fun finishWithResultOk() {
        (application as App).preferencesHelper.isFirstRun(false)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    @SuppressLint("NewApi")
    private fun getColour(color: Int): Int =
            if (Constants.IS_MARSHMALLOW_OR_ABOVE)
                getColor(color)
            else
                resources.getColor(color)

    private fun addSlides() {
        addSlide(firstSlide())
        addSlide(secondSlide())
        addSlide(thirdSlide())
        addSlide(fourthSlide())
        addSlide(fifthSlide())
    }

    private fun firstSlide(): AppIntroFragment = helpSlide(getColour(R.color.colorPrimary),
            R.drawable.intro1,
            resources.getString(R.string.first_slide_title),
            resources.getString(R.string.first_slide_desc))

    private fun secondSlide(): AppIntroFragment = helpSlide(Color.parseColor("#795548"),
                R.drawable.intro2,
                resources.getString(R.string.second_slide_title),
                resources.getString(R.string.second_slide_desc))

    private fun thirdSlide(): AppIntroFragment = helpSlide(Color.parseColor("#28292e"),
                R.drawable.intro3,
                resources.getString(R.string.third_slide_title),
                resources.getString(R.string.third_slide_desc))

    private fun fourthSlide(): AppIntroFragment = helpSlide(getColour(android.R.color.holo_blue_dark),
                R.drawable.intro4,
                resources.getString(R.string.fourth_slide_title),
                resources.getString(R.string.fourth_slide_desc))

    private fun fifthSlide(): AppIntroFragment = helpSlide(getColour(android.R.color.holo_red_light),
                R.drawable.intro5,
                resources.getString(R.string.fifth_slide_title),
                resources.getString(R.string.fifth_slide_desc))

    private fun helpSlide(color: Int, imageDrawable: Int, title: String, desc: String): AppIntroFragment {
        slider.bgColor = color
        slider.imageDrawable = imageDrawable
        slider.title = title
        slider.description = desc

        return AppIntroFragment.newInstance(slider)
    }
}
