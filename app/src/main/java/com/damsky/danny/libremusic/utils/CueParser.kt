package com.damsky.danny.libremusic.utils

import com.damsky.danny.libremusic.data.db.model.Song
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * A class that parses an album containing CUE cueFile.
 *
 * @param encoding A string containing a charset to be used for encoding when reading the cue file.
 *
 * @author Danny Damsky
 */
class CueParser(encoding: String) {

    companion object {
        const val MAX_BYTES_TO_READ = 500_000
        private val CUESHEET_PATTERN = Pattern.compile("cuesheet=")
        private val PERFORMER_PATTERN = Pattern.compile("PERFORMER\\s\"(.*)\"|PERFORMER\\s(.*)")
        private val GENRE_PATTERN = Pattern.compile("GENRE\\s\"(.*)\"|GENRE\\s(.*)")
        private val TITLE_PATTERN = Pattern.compile("TITLE\\s\"(.*)\"|TITLE\\s(.*)")
        private val DATE_PATTERN = Pattern.compile("DATE\\s(\\d+)")
        private val INDEX_PATTERN = Pattern.compile("INDEX 01 (\\d\\d:\\d\\d:\\d\\d)")
        private val TIME_PATTERN = Pattern.compile("\\d+")
    }

    private val charset = Charset.forName(encoding)

    private lateinit var cueFile: File
    private lateinit var songFile: String
    private lateinit var cover: String
    private var duration = -1
    var isReadable = false
    private lateinit var content: String

    fun setDataSource(sheetFile: File, songPath: String, coverArtPath: String, songDuration: Int) {
        cueFile = sheetFile
        songFile = songPath
        cover = coverArtPath
        duration = songDuration

        content = readFile()

        isReadable = if (cueFile.absolutePath == songFile)
            CUESHEET_PATTERN.matcher(content).find()
        else
            true
    }

    /**
     * @param timeFormat A string containing a time formatted with Minutes:Secs:Millis
     * @return           The time converted to milliseconds.
     */
    private fun convertToMs(timeFormat: String): Int {
        val matcher = TIME_PATTERN.matcher(timeFormat)

        matcher.find()
        val minutes = matcher.group().toLong()

        matcher.find()
        val seconds = matcher.group().toLong()

        matcher.find()
        val millis = matcher.group().toLong()

        return (TimeUnit.MINUTES.toMillis(minutes) +
                TimeUnit.SECONDS.toMillis(seconds) +
                millis).toInt()
    }

    private fun readFile(): String {
        val builder = StringBuilder()

        BufferedInputStream(FileInputStream(cueFile)).use {
            val contents = ByteArray(4_096)
            var bytesRead = it.read(contents)
            while (bytesRead != -1 && builder.length < MAX_BYTES_TO_READ) {
                builder.append(kotlin.text.String(contents, 0, bytesRead, charset))
                bytesRead = it.read(contents)
            }
        }

        return builder.toString()
    }

    /**
     * @param pattern A regex pattern used to get results from.
     * @return        The first result that the matcher finds using the given pattern.
     */
    private fun getFirstResult(pattern: Pattern): String {
        val matcher = pattern.matcher(content)
        return if (matcher.find())
            if (matcher.group(1) == null)
                matcher.group(2)
            else
                matcher.group(1)
        else
            Constants.DEFAULT_LIBRARY_ENTRANCE
    }

    private fun getArtist(): String = getFirstResult(PERFORMER_PATTERN)

    private fun getGenre(): String = getFirstResult(GENRE_PATTERN)

    private fun getTitles(): ArrayList<String> {
        val matcher = TITLE_PATTERN.matcher(content)
        val returnThis = ArrayList<String>(0)

        while (matcher.find()) {
            if (matcher.group(1) == null)
                returnThis.add(matcher.group(2))
            else
                returnThis.add(matcher.group(1))
        }

        return returnThis
    }

    private fun getDate(): Int {
        val getDate = DATE_PATTERN.matcher(content)

        if (getDate.find())
            return getDate.group(1).toInt()

        return 0
    }

    private fun getIndexOne(): ArrayList<Int> {
        val matcher = INDEX_PATTERN.matcher(content)
        val list = ArrayList<Int>(1)

        list.add(0)
        matcher.find()

        while (matcher.find())
            list.add(convertToMs(matcher.group(1)))

        list.add(duration)
        return list
    }

    /**
     * @return An array of songs parsed from the given cueFile
     */
    fun listSongs(): Array<Song> {
        val titles = getTitles()
        val artist = getArtist()
        val album = titles[0]
        titles.removeAt(0)
        val genre = getGenre()
        val indexes = getIndexOne()
        val year = getDate()

        return Array(titles.size) { i ->
            val plusOne = i + 1
            val endTime = indexes[plusOne]
            val startTime = indexes[i]
            Song(null, songFile, titles[i], album, artist, genre, plusOne, year, startTime,
                    endTime, endTime - startTime, cover)
        }
    }
}
