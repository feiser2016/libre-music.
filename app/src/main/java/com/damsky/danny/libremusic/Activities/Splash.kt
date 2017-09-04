/*
This activity opens up with the application.
It is in charge of querying for music and loading it into the database.
After it's done it will open up LibrePlayer.
HI
Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.damsky.danny.libremusic.DB.*
import com.damsky.danny.libremusic.Helpers.AudioConfig
import com.damsky.danny.libremusic.Helpers.CUESplitter
import com.damsky.danny.libremusic.R
import kotlinx.android.synthetic.main.activity_splash.*
import java.io.File

class Splash : AppCompatActivity() {
    /*
    Kotlin Extensions:
    loadingText = TextView
    progressBar = ProgressBar
     */
    lateinit var daoSession : DaoSession // A database session
    lateinit var encoding : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        encoding = PreferenceManager.getDefaultSharedPreferences(this).getString("file_encoding", "Cp1251")
        makePermissions() // Splash screen initialization starts by checking for permissions
    }

    private fun makePermissions() {
        if (Build.VERSION.SDK_INT >= 23) // No need to ask for permissions if API is below 23
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
                Handler().postDelayed({
                    /*
                    The request below asks the user for 2 permissions:
                    1. The permission to read/write to external storage
                    2. The permission to read phone state (Improves mediaPlayer experience)
                     */
                    ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_PHONE_STATE), 1)
                }, 400)
            else
                DataCollector().execute() // If all permissions are granted, start the AsyncTask
        else
            DataCollector().execute() // If API is below 23, start the AsyncTask
    } // checks for permissions and continues to initializing the database

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            startActivity(Intent(this, LibrePlayer::class.java))
            finish()
        } // If the user denied permission to read storage, load an empty library.
        else
            DataCollector().execute() // Continue to start the AsyncTask
    } // this function runs after the makePermissions() function if permissions were requested

    inner class DataCollector : AsyncTask<Void?, Void?, Void?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            runOnUiThread { progressBar.visibility = View.VISIBLE }
        } // Show the progressBar before starting any operations.

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            startActivity(Intent(this@Splash, LibrePlayer::class.java))
            finish()
        } // After everything is done start LibrePlayer

        override fun doInBackground(vararg p0: Void?): Void? {
            // Get a new session of song-db
            daoSession = DaoMaster(DaoMaster.DevOpenHelper(this@Splash, "song-db").writableDb).newSession()

            val songQuery = daoSession.songDao
                    .queryBuilder()
                    .orderAsc(SongDao.Properties.Artist,
                            SongDao.Properties.Year,
                            SongDao.Properties.Track,
                            SongDao.Properties.Album)
                    .build() // Query for songs from the session

            if (songQuery.list().isEmpty()) // If there are no songs call loadAudio()
                loadAudio() // Queries the device's storage for songs and adds them to the database

            val songList = songQuery.list() as ArrayList<Song> // finally fill the arrayList of songs

            val albumList = daoSession.albumDao // Fill the arrayList of albums
                    .queryBuilder()
                    .orderAsc(AlbumDao.Properties.Artist,
                            AlbumDao.Properties.Year,
                            AlbumDao.Properties.Album)
                    .build().list() as ArrayList<Album>

            val artistList = daoSession.artistDao // Fill the arrayList of artists
                    .queryBuilder()
                    .orderAsc(ArtistDao.Properties.Artist)
                    .build().list() as ArrayList<Artist>

            LibrePlayer.audioConfig = AudioConfig(this@Splash, songList, albumList, artistList)

            return null // Function expects a return statement
        }

        private fun checkAuthenticity(duration: Int, data: String, cover: String) : Boolean {
            if (duration > 600000) { // Files shorter than 10 minutes won't be checked
                val file_data = File(data) // The audio file
                val folder = file_data.parentFile // The audio file's directory
                val list_files = folder.listFiles()
                val extension = file_data.extension // The audio file's extension
                var cue_file : File? = null
                if (list_files != null)
                    for (file in list_files) { // Scan the audio file's folder
                        if (file.absolutePath != data && file.name.endsWith(extension)) // If songs are found in the folder
                            return true // A cue sheet isn't required in a folder which contains more than 1 audio file, as it is most likely an album of itself.
                        if (file.name.endsWith(".cue")) // If cue sheet is found
                            cue_file = file // Set cue_file to the cue sheet
                    }
                if (cue_file == null) // If cue sheet was found during for-loop
                    cue_file = File(data) // Set the cue_file to be the file itself

                val splitter = CUESplitter(cue_file, data, cover, duration, encoding)
                if (splitter.isReadable) {
                    // Use CUESplitter object to get a list of songs from the cue sheet
                    val list = splitter.list() // ArrayList<Song>
                    for (i in list) // For each song in splitter
                        daoSession.songDao.insert(i) // Add the song to the database
                    val song = list[0] // Get the first song from the album, use it as a sample
                    daoSession.albumDao.insert(Album(null, song.artist, song.album, song.year, cover)) // Add the album to the database
                    val test_artist = daoSession.artistDao.queryBuilder().where(ArtistDao.Properties.Artist.eq(song.artist)).unique()
                    if (test_artist == null) // This checks if the artist doesn't already exist in the artists table
                        daoSession.artistDao.insert(Artist(null, song.artist)) // Insert a new artist to the artists table
                    return false
                }
            }
            return true
        } // Checks if given file has a parsable cue sheet

        private fun loadAudio() {
            val cursor = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // URI of external storage
                    null,
                    "${MediaStore.Audio.Media.IS_MUSIC}!= 0",  // Queries for music files
                    null, null) // Queries the external storage for music files

            if (cursor != null && cursor.count > 0) {
                while (cursor.moveToNext()) { // Every item queried is a song
                    // The metadata of the song is gathered by the cursor and is added to the database
                    val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    runOnUiThread { // Update the TextView to show which file is currently being scanned.
                        loadingText.text = StringBuilder().append("${resources.getString(R.string.loading)}: ").append(data).toString()
                    }
                    val cover = getCoverArtPath(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)))
                    val duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    if (checkAuthenticity(duration, data, cover)) { // Checks if file doesn't have a parsable cue sheet
                        val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                        val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                        val year = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR))
                        daoSession.songDao.insert(Song(null, // Song ID in database is auto-increment
                                data, // URI of the file
                                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)), // Song's title
                                album, // Song's album
                                artist, // Song's artist
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)), // Song's track number
                                year, // Year of publish
                                0, // Song start time
                                duration, // Song end time
                                duration, // Song duration
                                cover // Song's cover art URI
                        )) // Insert a new song to the songs table

                        val test_album = daoSession.albumDao.queryBuilder().where(AlbumDao.Properties.Album.eq(album), AlbumDao.Properties.Artist.eq(artist)).unique()
                        if (test_album == null) // This checks if the album doesn't already exist in the albums table
                            daoSession.albumDao.insert(Album(null, artist, album, year, cover)) // Insert a new album to the albums table

                        val test_artist = daoSession.artistDao.queryBuilder().where(ArtistDao.Properties.Artist.eq(artist)).unique()
                        if (test_artist == null) // This checks if the artist doesn't already exist in the artists table
                            daoSession.artistDao.insert(Artist(null, artist)) // Insert a new artist to the artists table
                    }
                }
            }
            cursor.close()
        }

        private fun getCoverArtPath(albumId: Long): String {
            val albumCursor = contentResolver.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM_ART),
                    "${MediaStore.Audio.Albums._ID} = ?",
                    arrayOf(java.lang.Long.toString(albumId)),
                    null
            ) // query for album cover art using the parameter albumId
            val queryResult = albumCursor.moveToFirst()
            var result = "none" // if there is no cover art then it will simply return "none"
            if (queryResult) result = try {
                albumCursor.getString(0)
            } catch (e: Exception) {
                "none"
            }
            albumCursor.close()
            return result
        }
    } // AsyncTask in charge of loading the music library and starting LibrePlayer.
}
