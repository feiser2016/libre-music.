package com.damsky.danny.libremusic.data.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Genre
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.ui.main.MenuAction
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener
import com.damsky.danny.libremusic.utils.Constants

/**
 * A TypeModel designed to take an array of Genre objects.
 * @param genres An array of genres to use with the GenreModel.
 *
 * @author Danny Damsky
 */

class GenreModel(private val genres: ArrayList<Genre>) : TypeModel {

    private lateinit var current: Genre

    override fun getItemImage(): String {
        current.songs
                .filter { it.cover != Constants.ALBUM_COVER_NONE }
                .forEach { return it.cover }
        return Constants.ALBUM_COVER_NONE
    }

    override fun getPlaceHolderImage(): Int = R.drawable.genre

    override fun getItemTitle(): String = current.genre

    override fun getItemInfo(resources: Resources): String {
        val songsCount = current.songs.size
        return resources.getQuantityString(R.plurals.songs, songsCount, songsCount)
    }

    override fun getItemDuration(): String = ""

    override fun getSize(): Int = genres.size

    override fun getItemMenu(popupMenu: PopupMenu, onClickListener: CustomOnClickListener, listLevel: ListLevel?): PopupMenu {
        popupMenu.inflate(R.menu.menu_artists_albums_genres)

        popupMenu.setOnMenuItemClickListener { item ->
            val action = when (item.itemId) {
                R.id.addToQueue -> MenuAction.ACTION_ADD_TO_QUEUE
                R.id.addSongsToPlaylist -> MenuAction.ACTION_ADD_TO_PLAYLIST
                R.id.shareSongs -> MenuAction.ACTION_SHARE
                else -> MenuAction.ACTION_PLAY // R.id.playSongs
            }
            onClickListener.onContextMenuClick(current.songs as ArrayList<Song>, action)
            true
        }

        return popupMenu
    }

    override fun setPosition(position: Int) {
        current = genres[position]
    }

    override fun search(matches: ArrayList<Int>): GenreModel {
        val list = ArrayList<Genre>(0)
        if (matches.isNotEmpty())
            matches.mapTo(list) { genres[it] }
        return GenreModel(list)
    }
}
