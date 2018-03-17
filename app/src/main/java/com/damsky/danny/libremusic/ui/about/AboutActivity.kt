package com.damsky.danny.libremusic.ui.about

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.damsky.danny.dannydamskyutils.AboutPage
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.intro.IntroActivity
import com.damsky.danny.libremusic.utils.Constants

/**
 * This activity provides the user with information about the app.
 *
 * @author Danny Damsky
 */

class AboutActivity : AboutPage() {

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBackButton(true)
    }

    override fun openIntro() {
        startActivity(Intent(this, IntroActivity::class.java))
    }

    override fun openSourceCode() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_SOURCE_CODE)))
    }

    override fun openStoreDonationPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_DONATE)))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_DONATE_BACKUP)))
        }
    }

    override fun openStorePageRating() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_STORE)))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_STORE_BACKUP)))
        }
    }

    override fun setActivityTheme(): Int = (application as App).preferencesHelper.getTheme()

    override fun setAppIcon(): Int = R.mipmap.ic_launcher

    override fun setAppName(): String = getString(R.string.app_name)

    override fun setAuthorLocation(): String = getString(R.string.hadera_israel)

    override fun setAuthorLocationIcon(): Int = R.drawable.about_location

    override fun setAuthorName(): String = getString(R.string.danny_damsky)

    override fun setAuthorNameIcon(): Int = R.drawable.about_info

    override fun setAuthorTitleIcon(): Int = R.drawable.about_author

    override fun setDonateIcon(): Int = R.drawable.about_donate

    override fun setEmailIcon(): Int = R.drawable.about_email

    override fun setIntroIcon(): Int = R.drawable.about_intro

    override fun setRateIcon(): Int = R.drawable.about_rate

    override fun setSourceCodeIcon(): Int = R.drawable.about_code

    override fun setSupportIcon(): Int = R.drawable.about_support

    override fun setVersion(): String = getString(R.string.versionName)

    override fun setVersionIcon(): Int = R.drawable.about_info

    override fun writeEmail() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO,
                Uri.parse(Constants.URL_EMAIL)), resources.getString(R.string.send_mail_title)))
    }
}
