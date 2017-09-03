/*
Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Helpers

import com.damsky.danny.libremusic.DB.Song
import java.util.regex.Matcher
import java.util.regex.Pattern

class SongSearcher(private val songList: ArrayList<Song>) {
    private var searchString = ""
    private val stringList = ArrayList<String>(0)
    private lateinit var matches : ArrayList<Int>
    var Searching = false

    private fun pattern(s: String, match : String) : Matcher = Pattern.compile(s).matcher(match) // Regex pattern matcher

    private fun getMatches() : ArrayList<Int> {
        val list = ArrayList<Int>(0)
        (0 until stringList.size).filterTo(list) { pattern(searchString, stringList[it].toLowerCase()).find() }
        return list
    }

    fun Search() : ArrayList<Song> {
        val list = ArrayList<Song>(0)
        matches = getMatches()
        if (matches.isNotEmpty())
            matches.mapTo(list) { songList[it] }
        return list
    }

    fun Update(search: String) {
        val _search = search.toLowerCase()
        if (!_search.contains("^$searchString") || searchString.isBlank()) {
            stringList.clear()
            songList.mapTo(stringList) { it.title }
        }
        searchString = _search
    }

    fun getPosition(position: Int) = matches[position]
}
