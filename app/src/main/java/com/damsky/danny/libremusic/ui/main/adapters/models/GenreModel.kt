package com.damsky.danny.libremusic.ui.main.adapters.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Genre
import com.damsky.danny.libremusic.ui.main.MenuAction
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener

/**
 * A TypeModel designed to take an array of Genre objects.
 * @param genres An array of genres to use with the GenreModel.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class GenreModel(val genres: Array<Genre>) : TypeModel {
    private lateinit var current: Genre

    override fun getItemImage(): String {
        current.songs
                .filter { it.cover != "none" }
                .forEach { return it.cover }
        return "none"
    }

    override fun getPlaceHolderImage() = R.drawable.genre

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
            onClickListener.onContextMenuClick(current.songs.toTypedArray(), action)
            true
        }

        return popupMenu
    }

    override fun setPosition(position: Int) {
        current = genres[position]
    }

    override fun search(matches: Array<Int>): GenreModel {
        val list = ArrayList<Genre>(0)
        if (matches.isNotEmpty())
            matches.mapTo(list) { genres[it] }
        return GenreModel(list.toTypedArray())
    }
}
