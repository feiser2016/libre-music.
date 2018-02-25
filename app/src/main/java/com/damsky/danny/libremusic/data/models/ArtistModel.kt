package com.damsky.danny.libremusic.data.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Artist
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.ui.main.MenuAction
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener
import com.damsky.danny.libremusic.utils.Constants

/**
 * A TypeModel designed to take an array of Artist objects.
 * @param artists An array of artists to use with the ArtistModel.
 *
 * @author Danny Damsky
 * @since 2018-02-25
 */

class ArtistModel(private val artists: Array<Artist>) : TypeModel {

    private lateinit var current: Artist
    private var songsCount = 0

    override fun getItemImage(): String {
        val albums = current.albums
        var iterate = Constants.ALBUM_COVER_NONE
        for (i in albums) {
            if (iterate == Constants.ALBUM_COVER_NONE && i.cover != Constants.ALBUM_COVER_NONE)
                iterate = i.cover
            songsCount += i.songs.size
        }
        return iterate
    }

    override fun getPlaceHolderImage(): Int {
        return R.drawable.artist
    }

    override fun getItemTitle(): String {
        return current.artist
    }

    override fun getItemInfo(resources: Resources): String {
        val albums = current.albums
        return "${resources.getQuantityString(R.plurals.albums, albums.size, albums.size)} | ${resources.getQuantityString(R.plurals.songs, songsCount, songsCount)}"
    }

    override fun getItemDuration(): String = ""

    override fun getSize(): Int {
        return artists.size
    }

    override fun getItemMenu(popupMenu: PopupMenu,
                             onClickListener: CustomOnClickListener,
                             listLevel: ListLevel?): PopupMenu {
        val albums = current.albums
        popupMenu.inflate(R.menu.menu_artists_albums_genres)

        popupMenu.setOnMenuItemClickListener { item ->
            val list = ArrayList<Song>(0)
            for (i in albums)
                list.addAll(i.songs)
            val action = when (item.itemId) {
                R.id.addToQueue -> MenuAction.ACTION_ADD_TO_QUEUE
                R.id.addSongsToPlaylist -> MenuAction.ACTION_ADD_TO_PLAYLIST
                R.id.shareSongs -> MenuAction.ACTION_SHARE
                else -> MenuAction.ACTION_PLAY // R.id.playSongs
            }
            onClickListener.onContextMenuClick(list.toTypedArray(), action)
            true
        }

        return popupMenu
    }

    override fun setPosition(position: Int) {
        current = artists[position]
        songsCount = 0
    }


    override fun search(matches: Array<Int>): ArtistModel {
        val list = ArrayList<Artist>(0)
        if (matches.isNotEmpty())
            matches.mapTo(list) { artists[it] }
        return ArtistModel(list.toTypedArray())
    }
}
