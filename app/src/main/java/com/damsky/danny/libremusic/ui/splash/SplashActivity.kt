package com.damsky.danny.libremusic.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.model.DaoSession
import com.damsky.danny.libremusic.ui.intro.IntroActivity
import com.damsky.danny.libremusic.ui.main.MainActivity
import com.damsky.danny.libremusic.utils.Constants
import com.damsky.danny.libremusic.utils.CueParser
import com.damsky.danny.libremusic.utils.SongLoader
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * The first activity to start when the app is launched.
 * This activity is in charge of making sure the DB and preferences are set-up before continuing.
 *
 * @author Danny Damsky
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var appReference: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        appReference = application as App

        if (appReference.preferencesHelper.isFirstRun())
            startActivityForResult(Intent(this, IntroActivity::class.java),
                    Constants.REQUEST_START_INTRO)
        else
            onActivityResult(Constants.REQUEST_START_INTRO, Activity.RESULT_OK, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            finish()
        else {
            if (requestCode == Constants.REQUEST_START_MAIN)
                appReference.appDbHelper.deleteAll()

            scanSongs()
        }
    }

    /**
     * Asynchronously runs asyncLoading() in order to set up the DB before continuing and
     * starting the MainActivity.
     */
    private fun scanSongs() = launch {
        async {
            runOnUiThread { progressBar.visibility = View.VISIBLE }

            runBlocking { asyncLoading() }

            startActivityForResult(
                    Intent(this@SplashActivity, MainActivity::class.java),
                    Constants.REQUEST_START_MAIN)
        }.await()
    }

    /**
     * Sets up the database after loading it if it was initially empty.
     */
    private fun asyncLoading() {
        val daoSession = appReference.appDbHelper.getDaoSession()

        if (appReference.appDbHelper.songsEmpty()) {
            val cueParser = CueParser(appReference.preferencesHelper.getEncoding())
            loadAudio(cueParser, daoSession)
            appReference.appDbHelper.setSongs(daoSession)
        }

        appReference.appDbHelper.setAlbums(daoSession)
        appReference.appDbHelper.setArtists(daoSession)
        appReference.appDbHelper.setGenres(daoSession)
        appReference.appDbHelper.setPlaylists(daoSession)
    }

    /**
     * Queries for songs using a SongLoader object.
     * @param cueParser CueParser object that is passed to the SongLoader.
     * @param daoSession Database session caching object.
     */
    private fun loadAudio(cueParser: CueParser, daoSession: DaoSession) = SongLoader(contentResolver).use {
        if (it.isAble()) {
            while (it.hasNext()) {
                val data = it.getData()
                runOnUiThread {
                    loadingText.text = StringBuilder()
                            .append(resources.getString(R.string.loading))
                            .append(": ").append(data).toString()
                }

                val cover = it.getCover()
                val duration = it.getDuration()
                val cueSheet = it.getParsedCue(cueParser, duration, data, cover)
                if (cueSheet == null) {
                    val artist = it.getArtist()
                    val album = it.getAlbum()
                    val year = it.getYear()
                    val genre = it.getGenre()

                    appReference.appDbHelper.insertSong(data, it.getTitle(),
                            album, artist, genre, it.getTrackNum(), year,
                            0, duration, duration, cover, daoSession)

                    appReference.appDbHelper.insertAlbum(album, artist, year, cover, daoSession)
                    appReference.appDbHelper.insertArtist(artist, daoSession)
                    appReference.appDbHelper.insertGenre(genre, daoSession)
                } else {
                    for (i in cueSheet) {
                        appReference.appDbHelper.insertSong(i.data, i.title, i.album,
                                i.artist, i.genre, i.track, i.year, i.startTime, i.endTime,
                                i.duration, i.cover, daoSession)

                        appReference.appDbHelper.insertGenre(i.genre, daoSession)
                    }
                    val song = cueSheet[0]

                    appReference.appDbHelper.insertAlbum(song.album, song.artist, song.year, cover,
                            daoSession)

                    appReference.appDbHelper.insertArtist(song.artist, daoSession)
                }
            }
        }
    }
}
