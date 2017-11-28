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
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

/**
 * This activity provides the user with information about the app.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class AboutActivity : AppCompatActivity() {

    private lateinit var sourceCodeUri: Uri
    private lateinit var emailUri: Uri
    private lateinit var storeUri: Uri
    private lateinit var storeBackupUri: Uri
    private lateinit var donateUri: Uri
    private lateinit var donateBackupUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(if ((application as App).preferencesHelper
                .detectAppTheme(resources.getStringArray(R.array.app_themes_values)).first)
            R.style.AppTheme_Black
        else
            R.style.AppTheme)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        launch {
            async {
                sourceCodeUri = Uri.parse("https://bitbucket.org/dannydamsky/libre-music/src")
                emailUri = Uri.parse("mailto:dannydamskypublic@gmail.com")
                storeUri = Uri.parse("market://details?id=com.damsky.danny.libremusic")
                storeBackupUri = Uri.parse("https://play.google.com/store/apps/details?id=com.damsky.danny.libremusic")
                donateUri = Uri.parse("market://details?id=com.damsky.danny.schoolassistpro")
                donateBackupUri = Uri.parse("https://play.google.com/store/apps/details?id=com.damsky.danny.schoolassistpro")
            }.await()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    fun openIntro(view: View) = startActivity(Intent(this, IntroActivity::class.java))

    fun openSourceCode(view: View) = startActivity(Intent(Intent.ACTION_VIEW, sourceCodeUri))

    fun writeEmail(view: View) = startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SENDTO, emailUri), resources.getString(R.string.send_mail_title)))

    fun openStorePageRating(view: View) = try {
        startActivity(Intent(Intent.ACTION_VIEW, storeUri))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, storeBackupUri))
    }

    fun openStoreDonationPage(view: View) = try {
        startActivity(Intent(Intent.ACTION_VIEW, donateUri))
    } catch (e: Exception) {
        startActivity(Intent(Intent.ACTION_VIEW, donateBackupUri))
    }
}
