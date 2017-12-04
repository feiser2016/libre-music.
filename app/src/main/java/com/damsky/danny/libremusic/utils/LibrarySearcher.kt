package com.damsky.danny.libremusic.utils

import com.damsky.danny.libremusic.ui.main.adapters.models.TypeModel
import java.util.regex.Pattern

/**
 * Searches the music library for items.
 * Possible item types: Artist, Album, Song, Genre, Playlist
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */
class LibrarySearcher {
    private lateinit var list: TypeModel
    private var searchString = ""
    private lateinit var stringList: Array<String>
    private lateinit var matches: Array<Int>
    private lateinit var pattern: Pattern
    var isSearching = false

    fun setDataSource(list: TypeModel) {
        this.list = list
    }

    private fun getMatches(): Array<Int> {
        val list = ArrayList<Int>(0)
        (0 until stringList.size).filterTo(list) { pattern.matcher(stringList[it].toLowerCase()).find() }
        return list.toTypedArray()
    }

    fun search(): TypeModel {
        matches = getMatches()
        return list.search(matches)
    }

    /**
     * @param search A string containing the value to be searched
     * @return       Update the searchString, get a list containing item candidates and compile the pattern.
     */
    fun update(search: String) {
        val searchToLower = search.toLowerCase()
        if (!searchToLower.contains("^$searchString") || searchString.isBlank())
            stringList = Array(list.getSize(), { i ->
                list.setPosition(i)
                list.getItemTitle()
            })
        searchString = searchToLower
        pattern = Pattern.compile(searchString)
    }

    /**
     * @param position Index of item to get position
     * @return          position if search is off, original position if search is on
     */
    fun getPosition(position: Int) = matches[position]
}