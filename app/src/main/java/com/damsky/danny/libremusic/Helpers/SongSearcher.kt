package com.damsky.danny.libremusic.Helpers

import com.damsky.danny.libremusic.DB.Song
import java.util.regex.Matcher
import java.util.regex.Pattern

class SongSearcher(private val songList: ArrayList<Song>) {
    private var searchString = ""
    private val stringList = ArrayList<String>(0)
    private var matches : ArrayList<Int>? = null
    var Searching = false

    private fun pattern(s: String, match : String) : Matcher = Pattern.compile(s).matcher(match) // Regex pattern matcher

    private fun getMatches() : ArrayList<Int>? {
        val list = ArrayList<Int>(0)
        (0 until stringList.size).filterTo(list) { pattern(searchString, stringList[it].toLowerCase()).find() }
        return if (list.isNotEmpty()) list else null
    }

    fun Search() : ArrayList<Song> {
        val list = ArrayList<Song>(0)
        matches = getMatches()
        matches?.mapTo(list) { songList[it] }
        return if (list.size > 0 && searchString.isNotBlank()) list else ArrayList(0)
    }

    fun Update(search: String) {
        val _search = search.toLowerCase()
        if (!_search.contains("^$searchString") || searchString.isBlank()) {
            stringList.clear()
            songList.mapTo(stringList) { it.title }
        }
        searchString = _search
    }

    fun getPosition(position: Int) = matches!![position]
}
