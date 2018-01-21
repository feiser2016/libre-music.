package com.damsky.danny.libremusic.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.intro.IntroActivity
import com.damsky.danny.libremusic.utils.Constants
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

/**
 * This activity provides the user with information about the app.
 *
 * @author Danny Damsky
 * @since 2018-01-21
 */

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme((application as App).preferencesHelper.getTheme())
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        versionButton.text = "${getString(R.string.about_version)} ${getString(R.string.versionName)}"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    fun openIntro(view: View) =
            startActivity(Intent(this, IntroActivity::class.java))

    fun openSourceCode(view: View) =
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_SOURCE_CODE)))

    fun writeEmail(view: View) = startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SENDTO, Uri.parse(Constants.URL_EMAIL)), resources.getString(R.string.send_mail_title)))

    fun openStorePageRating(view: View) =
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_STORE)))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_STORE_BACKUP)))
            }

    fun openStoreDonationPage(view: View) =
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_DONATE)))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_DONATE_BACKUP)))
            }
}
