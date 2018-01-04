package com.damsky.danny.libremusic.ui.splash

import android.content.Intent
import android.view.View
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.main.MainActivity
import com.damsky.danny.libremusic.utils.CueParser
import com.damsky.danny.libremusic.utils.SongLoader
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * Service class containing static variables/functions for use with SplashActivity
 *
 * @author Danny Damsky
 * @since 2018-01-04
 */
class SplashPresenter {
    companion object {
        const val START_INTRO_REQUEST = 101
        const val START_MAIN_REQUEST = 102

        /**
         * Asynchronously runs asyncLoading() in order to set up the DB before continuing and
         * starting the MainActivity.
         */
        fun SplashActivity.scanSongs() = launch {
            async {
                progressBar.visibility = View.VISIBLE
                runBlocking {
                    asyncLoading()
                }
                startActivityForResult(
                        Intent(this@scanSongs, MainActivity::class.java),
                        START_MAIN_REQUEST)
            }.await()
        }

        /**
         * Sets up the database after loading it if it was initially empty.
         */
        private fun SplashActivity.asyncLoading() {
            if (appReference.appDbHelper.songsEmpty()) {
                loadAudio(CueParser(appReference.preferencesHelper.getEncoding()))
                appReference.appDbHelper.setSongs()
            }
            appReference.appDbHelper.setAlbums()
            appReference.appDbHelper.setArtists()
            appReference.appDbHelper.setGenres()
            appReference.appDbHelper.setPlaylists()
        }

        /**
         * Queries for songs using a SongLoader object.
         * @param cueParser CueParser object that is passed to the SongLoader
         */
        private fun SplashActivity.loadAudio(cueParser: CueParser) {
            val songLoader = SongLoader(contentResolver)

            if (songLoader.isAble()) {
                while (songLoader.hasNext()) {
                    val data = songLoader.getData()
                    runOnUiThread {
                        loadingText.text = StringBuilder().append(resources.getString(R.string.loading)).append(": ").append(data).toString()
                    }

                    val cover = songLoader.getCover()
                    val duration = songLoader.getDuration()
                    val cueSheet = songLoader.getParsedCue(cueParser, duration, data, cover)
                    if (cueSheet == null) {
                        val artist = songLoader.getArtist()
                        val album = songLoader.getAlbum()
                        val year = songLoader.getYear()
                        val genre = songLoader.getGenre()
                        appReference.appDbHelper.insertSong(data, songLoader.getTitle(),
                                album, artist, genre, songLoader.getTrackNum(), year,
                                0, duration, duration, cover)

                        appReference.appDbHelper.insertAlbum(album, artist, year, cover)
                        appReference.appDbHelper.insertArtist(artist)
                        appReference.appDbHelper.insertGenre(genre)
                    } else {
                        for (i in cueSheet) {
                            appReference.appDbHelper.insertSong(i.data, i.title, i.album,
                                    i.artist, i.genre, i.track, i.year, i.startTime, i.endTime,
                                    i.duration, i.cover)

                            appReference.appDbHelper.insertGenre(i.genre)
                        }
                        val song = cueSheet[0]
                        appReference.appDbHelper.insertAlbum(song.album, song.artist, song.year, cover)
                        appReference.appDbHelper.insertArtist(song.artist)
                    }
                }
            }

            songLoader.close()
        }
    }
}
