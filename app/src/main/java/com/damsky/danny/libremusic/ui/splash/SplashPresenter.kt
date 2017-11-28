package com.damsky.danny.libremusic.ui.splash

import android.content.Intent
import android.provider.MediaStore
import android.view.View
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.main.MainActivity
import com.damsky.danny.libremusic.utils.CueParser
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.io.File

/**
 * Service class containing static variables/functions for use with SplashActivity
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */
class SplashPresenter {
    companion object {
        const val START_INTRO_REQUEST = 101
        const val START_MAIN_REQUEST = 102

        private const val MIN_SONG_FILE_DURATION = 600_000

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
            if ((application as App).appDbHelper.songsEmpty()) {
                loadAudio(CueParser((application as App).preferencesHelper.getEncoding()))
                (application as App).appDbHelper.setSongs()
            }
            (application as App).appDbHelper.setAlbums()
            (application as App).appDbHelper.setArtists()
            (application as App).appDbHelper.setGenres()
            (application as App).appDbHelper.setPlaylists()
        }

        /**
         * Queries for songs using a cursor.
         * @param cueParser CueParser object that is passed to checkForCueParsing
         */
        private fun SplashActivity.loadAudio(cueParser: CueParser) {
            val cursor = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    "${MediaStore.Audio.Media.IS_MUSIC}!= 0",
                    null, null)

            if (cursor != null && cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    runOnUiThread {
                        loadingText.text = StringBuilder()
                                .append(resources.getString(R.string.loading))
                                .append(": ")
                                .append(data)
                                .toString()
                    }

                    val cover = getCoverArtPath(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)))
                    val duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    if (checkForCueParsing(cueParser, duration, data, cover)) {
                        val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                        val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                        val year = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR))
                        val genre = "<unknown>" // TODO Fix this
                        (application as App).appDbHelper.insertSong(
                                data,
                                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                                album,
                                artist,
                                genre,
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)),
                                year,
                                0,
                                duration,
                                duration,
                                cover
                        )

                        (application as App).appDbHelper.insertAlbum(album, artist, year, cover)
                        (application as App).appDbHelper.insertArtist(artist)
                        (application as App).appDbHelper.insertGenre(genre)
                    }
                }
            }

            cursor.close()
        }

        /**
         * Queries for album art using a cursor.
         * @param albumId The ID of the album, allows the cursor to correctly find the album art.
         * @return        A string containing the path of the album art (Or "none" if not found).
         */
        private fun SplashActivity.getCoverArtPath(albumId: Long): String {
            val albumCursor = contentResolver.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM_ART),
                    "${MediaStore.Audio.Albums._ID} = ?",
                    arrayOf(java.lang.Long.toString(albumId)),
                    null
            )

            val queryResult = albumCursor.moveToFirst()

            var result = "none"
            if (queryResult) result = try {
                albumCursor.getString(0)
            } catch (e: Exception) {
                "none"
            }

            albumCursor.close()
            return result
        }

        /**
         * Checks if data parameter is viable for parsing by CUE, if it is it adds all the songs
         * that were found during the parsing to the DB.
         *
         * @param cueParser CueParser instance used for parsing all CUE sheets during the songs query.
         * @param duration  The duration of the audio file that was passed.
         * @param data      The path of the audio file.
         * @param cover     The path of the album art for the audio file.
         *
         * @return          True if the file is parsable else False.
         */
        private fun SplashActivity.checkForCueParsing(cueParser: CueParser,
                                                      duration: Int,
                                                      data: String,
                                                      cover: String): Boolean {
            if (duration > MIN_SONG_FILE_DURATION) {
                val fileData = File(data)
                val folder = fileData.parentFile
                val listFiles = folder.listFiles()
                val extension = fileData.extension
                var cueFile: File? = null
                if (listFiles != null)
                    for (file in listFiles) {
                        if (file.absolutePath != data && file.name.endsWith(extension))
                            return true
                        if (file.name.endsWith(".cue"))
                            cueFile = file
                    }

                if (cueFile == null)
                    cueFile = File(data)

                cueParser.setDataSource(cueFile, data, cover, duration)

                if (cueParser.isReadable) {
                    val list = cueParser.listSongs()
                    for (i in list) {
                        (application as App).appDbHelper.insertSong(i.data, i.title, i.album,
                                i.artist, i.genre, i.track, i.year, i.startTime, i.endTime,
                                i.duration, i.cover)

                        (application as App).appDbHelper.insertGenre(i.genre)
                    }
                    val song = list[0]
                    (application as App).appDbHelper.insertAlbum(song.album, song.artist, song.year, cover)
                    (application as App).appDbHelper.insertArtist(song.artist)
                    return false
                }
            }
            return true
        }
    }
}
