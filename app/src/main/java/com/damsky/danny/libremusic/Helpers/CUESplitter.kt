/*
This class is used for parsing cue sheets within binary or non-binary files.
After the sheet is parsed you can use the list() function to retrieve a list of songs
that the cue sheet pointed to in the given song file.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Helpers

import com.damsky.danny.libremusic.DB.Song
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

class CUESplitter(val data: File, private val song_file: String, val cover: String, private val duration: Int, private val encoding: String) {
    // These two variables are initialized immediately after creating an instance of CUESplitter
    private val content = read() // Stores the cue sheet in String form
    val isReadable = if (data.absolutePath == song_file) // Public constructor, checks if cue sheet exists within given file.
        pattern("cuesheet=").find()
    else
        true

    private fun pattern(s: String) : Matcher = Pattern.compile(s).matcher(content) // Regex pattern matcher
    // convert_to_ms converts HH:MM:SS format to milliseconds
    private fun convert_to_ms(s: String) : Int = ((s.substring(0, 2).toInt() * 60000) + (s.substring(3, 5).toInt() * 1000) + s.substring(6, 8).toInt())

    private fun read() : String {
        val bis = BufferedInputStream(FileInputStream(data)) // In charge of reading the file
        val contents = ByteArray(1024) // A byte array that will store contents of the file

        var bytesRead = bis.read(contents) // start reading, store length in bytesRead
        val builder = StringBuilder() // initialize StringBuilder() object for appending text in loop
        while (bytesRead != -1 && builder.length < 500000) {
            // Append to builder a String object that takes the length and the bytes that were read from data and encodes it in Windows-1251 encoding
            builder.append(kotlin.text.String(contents, 0, bytesRead, Charset.forName(encoding)))
            bytesRead = bis.read(contents) // Continue reading
        } // Loop doesn't stop until there's nothing left to read OR the 500KB limit has been reached
        bis.close() // Close the input stream
        return builder.toString() // Returns a string what was read during the loop
    } // Reads the first 500 kilobytes of the file allegedly containing the cue sheet.

    private fun artist() : String {
        val get = pattern("PERFORMER\\s\"(.*)\"|PERFORMER\\s(.*)") // Regex pattern used to get artist name
        if (get.find()) { // find() moves the pointer to the next match, returns True if anything was found
            if (get.group(1) == null) // First group contains "", second group doesn't
                return get.group(2)
            return get.group(1)
        } // The first match of PERFORMER contains the artist's name, the others aren't important.
        return "<unknown>" // In case no match was found, the artist will be called "<unknown>"
    } // Returns the name of the artist

    private fun titles() : ArrayList<String> {
        /* The first title is the album's name,
           the rest are the names of songs in the album. */
        val get = pattern("TITLE\\s\"(.*)\"|TITLE\\s(.*)") // Regex pattern used to get titles
        val rethis = ArrayList<String>(0) // Initialize the ArrayList
        while (get.find()) { // find() moves the pointer to the next match, returns True if anything was found
            if (get.group(1) == null) // First group contains "", second group doesn't
                rethis.add(get.group(2))
            else
                rethis.add(get.group(1))
        } // This loop adds all matches to the ArrayList
        return rethis
    } // Returns album title in index 0 and all the song titles in indexes > 0

    private fun date() : Int {
        val get_date = pattern("DATE\\s(\\d+)") // Regex pattern used to get the date
        if (get_date.find())
            return get_date.group(1).toInt()
        return 0
    } // Returns the album's date of publishing

    private fun get_indexone() : ArrayList<Int> {
        val get = pattern("INDEX 01 (\\d\\d:\\d\\d:\\d\\d)") // Regex pattern used to get song start times
        val list = ArrayList<Int>(0) // Initialize the ArrayList

        list.add(0) // Album starts at 0 seconds
        get.find()  // Skip the first index, as it is irrelevant (Album has to start at 0 seconds)

        while (get.find()) // Loop through the start times of songs
            list.add(convert_to_ms(get.group(1))) // Add each index to the ArrayList once it's been converted to milliseconds

        list.add(duration) // Finally, add the end time of the album to the end of the ArrayList
        return list
    } // Returns start times of songs throughout the album

    fun list() : ArrayList<Song> {
        val titles = titles() // Get all titles
        val artist = artist() // Get the album's artist name
        val album = titles[0] // The first index in titles is the album's name
        titles.removeAt(0)    // Remove the album's name from the ArrayList (Now it only contains song names)
        val indexes = get_indexone() // Get all song start times
        val year = date()            // Get the album's publishing date
        val songs = ArrayList<Song>(0) // Initialize the ArrayList of songs
        (0 until titles.size).mapTo(songs) {
            Song(null, song_file, titles[it], album, artist, it + 1, year, indexes[it], indexes[it + 1], indexes[it + 1] - indexes[it], cover)
        } // Loop through all the song names, every time add a song to the ArrayList
        return songs
    } // The only public function in the class, returns a list of all the songs in the album
}