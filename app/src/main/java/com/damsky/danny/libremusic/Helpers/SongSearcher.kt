package com.damsky.danny.libremusic.Helpers

import android.widget.ListAdapter
import com.damsky.danny.libremusic.DB.Song
import com.damsky.danny.libremusic.Enum.ListLevel
import java.util.regex.Matcher
import java.util.regex.Pattern

class SongSearcher(private val songList: ArrayList<Song>) {
    private lateinit var saveType : ListAdapter
    private lateinit var saveLevel : ListLevel
    private var searchString = ""
    private val stringList = ArrayList<String>(0)
    private var matches : ArrayList<Int>? = null
    var Searching = false

    private fun pattern(s: String, match : String) : Matcher = Pattern.compile(s).matcher(match) // Regex pattern matcher

    private fun getMatches() : ArrayList<Int>? {
        val list = ArrayList<Int>(0)
        (0 until stringList.size).filterTo(list) { pattern("^$searchString", stringList[it]).find() }
        return if (list.isNotEmpty()) list else null
    }

    fun Search() : ArrayList<Song> {
        val list = ArrayList<Song>(0)
        matches = getMatches()
        matches?.mapTo(list) { songList[it] }
        return if (list.size > 0 && searchString.isNotBlank()) list else ArrayList(0)
    }

    fun Update(search: String) {
        if (!search.contains("^$searchString") || searchString.isBlank()) {
            stringList.clear()
            songList.mapTo(stringList) { it.title }
        }
        searchString = search
    }

    fun getPosition(position: Int) = matches!![position]

    fun setSave(save_one: ListAdapter, save_two: ListLevel) {
        saveType = save_one
        saveLevel = save_two
    }
    fun getSave() = Pair(saveType, saveLevel)

}
