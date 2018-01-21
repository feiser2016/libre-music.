package com.damsky.danny.libremusic.utils

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.util.ArrayMap
import com.damsky.danny.libremusic.data.db.model.Song
import java.io.File

/**
 * Helper class for loading songs using a cursor that queries the internal storage.
 *
 * @param contentResolver gives the class access to the content model
 * (i.e. allows it to query files on the internal storage)
 *
 * @author Danny Damsky
 * @since 2018-01-21
 */

class SongLoader(private val contentResolver: ContentResolver) {
    companion object {
        private const val MIN_DURATION_FOR_CUE_PARSING = 600_000
    }

    private val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
            "${MediaStore.Audio.Media.IS_MUSIC}!= 0", null, null)

    private val mMap = ArrayMap<String, Int>()

    /**
     * Function for caching the columns of the cursor (Improves performance)
     * @param columnName The name of the column to add to cache.
     * @return           The index of the column
     */
    private fun getColumnIndex(columnName: String): Int {
        if (!mMap.containsKey(columnName))
            mMap[columnName] = cursor.getColumnIndex(columnName)
        return mMap[columnName]!!
    }

    private fun getInt(string: String) = cursor.getInt(getColumnIndex(string))

    private fun getString(string: String) = cursor.getString(getColumnIndex(string))

    /**
     * Queries for album art using a cursor.
     * @param albumId The ID of the album, allows the cursor to correctly find the album art.
     * @return        A string containing the path of the album art (Or "none" if not found).
     */
    private fun getCoverArtPath(albumId: Long): String {
        val albumCursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Albums.ALBUM_ART), "${MediaStore.Audio.Albums._ID} = ?",
                arrayOf("$albumId"), null)

        val queryResult = albumCursor.moveToFirst()

        var result = Constants.ALBUM_COVER_NONE
        if (queryResult) result = try {
            albumCursor.getString(0)
        } catch (e: Exception) {
            Constants.ALBUM_COVER_NONE
        }

        albumCursor.close()
        return result
    }

    fun isAble(): Boolean =
            cursor != null && cursor.count > 0

    fun hasNext(): Boolean =
            cursor.moveToNext()

    fun getData(): String =
            getString(MediaStore.Audio.Media.DATA)

    fun getCover(): String =
            getCoverArtPath(cursor.getLong(getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)))

    fun getDuration(): Int =
            getInt(MediaStore.Audio.Media.DURATION)

    fun getArtist(): String =
            getString(MediaStore.Audio.Media.ARTIST)

    fun getAlbum(): String =
            getString(MediaStore.Audio.Media.ALBUM)

    fun getYear(): Int =
            getInt(MediaStore.Audio.Media.YEAR)

    fun getGenre(): String =
            Constants.DEFAULT_LIBRARY_ENTRANCE // TODO Fix this

    fun getTitle(): String =
            getString(MediaStore.Audio.Media.TITLE)

    fun getTrackNum(): Int =
            getInt(MediaStore.Audio.Media.TRACK)

    /**
     * Checks if data parameter is viable for parsing by CUE.
     *
     * @param cueParser CueParser instance used for parsing all CUE sheets during the songs query.
     * @param duration  The duration of the audio file that was passed.
     * @param data      The path of the audio file.
     * @param cover     The path of the album art for the audio file.
     *
     * @return          Array of songs if the file is parsable else null.
     */
    fun getParsedCue(cueParser: CueParser, duration: Int, data: String, cover: String): Array<Song>? {
        if (duration > MIN_DURATION_FOR_CUE_PARSING) {
            val fileData = File(data)
            val listFiles = fileData.parentFile.listFiles()
            val extension = fileData.extension
            var cueFile: File? = null
            if (listFiles != null)
                for (file in listFiles) {
                    if (file.absolutePath != data && file.name.endsWith(extension))
                        return null
                    if (file.name.endsWith(".cue"))
                        cueFile = file
                }

            if (cueFile == null)
                cueFile = File(data)

            cueParser.setDataSource(cueFile, data, cover, duration)

            if (cueParser.isReadable)
                return cueParser.listSongs()
        }
        return null
    }

    fun close() {
        mMap.clear()
        cursor.close()
    }

}
