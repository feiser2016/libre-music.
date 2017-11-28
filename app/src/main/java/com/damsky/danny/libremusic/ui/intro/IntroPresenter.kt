package com.damsky.danny.libremusic.ui.intro

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Build
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage

/**
 * Service class containing static variables/functions for use with IntroActivity
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class IntroPresenter {
    companion object {
        val isMarshmallowPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        private val slider = SliderPage()

        fun IntroActivity.finishWithResultOk() {
            (application as App).preferencesHelper.isFirstRun(false)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        @SuppressLint("NewApi")
        fun IntroActivity.getColour(color: Int): Int =
                if (isMarshmallowPlus)
                    getColor(color)
                else
                    resources.getColor(color)

        fun IntroActivity.addSlides() {
            addSlide(firstSlide())
            addSlide(secondSlide())
            addSlide(thirdSlide())
            addSlide(fourthSlide())
            addSlide(fifthSlide())
        }

        private fun IntroActivity.firstSlide(): AppIntroFragment = helpSlide(getColour(R.color.colorPrimary),
                R.drawable.intro1,
                resources.getString(R.string.first_slide_title),
                resources.getString(R.string.first_slide_desc))

        private fun IntroActivity.secondSlide(): AppIntroFragment = helpSlide(Color.parseColor("#795548"),
                R.drawable.intro2,
                resources.getString(R.string.second_slide_title),
                resources.getString(R.string.second_slide_desc))

        private fun IntroActivity.thirdSlide(): AppIntroFragment = helpSlide(Color.parseColor("#28292e"),
                R.drawable.intro3,
                resources.getString(R.string.third_slide_title),
                resources.getString(R.string.third_slide_desc))

        private fun IntroActivity.fourthSlide(): AppIntroFragment = helpSlide(getColour(android.R.color.holo_blue_dark),
                R.drawable.intro4,
                resources.getString(R.string.fourth_slide_title),
                resources.getString(R.string.fourth_slide_desc))

        private fun IntroActivity.fifthSlide(): AppIntroFragment = helpSlide(getColour(android.R.color.holo_red_light),
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
}
