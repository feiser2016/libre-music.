package com.damsky.danny.libremusic.ui.main.adapters.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Playlist
import com.damsky.danny.libremusic.ui.main.MenuAction
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener

/**
 * A TypeModel designed to take an array of Playlist objects.
 * @param playlists An array of playlists to use with the PlaylistModel.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class PlaylistModel(val playlists: Array<Playlist>) : TypeModel {
    private lateinit var current: Playlist
    private var position = 0

    override fun getItemImage(): String {
        current.songs
                .filter { it.cover != "none" }
                .forEach { return it.cover }
        return "none"
    }

    override fun getPlaceHolderImage() = R.drawable.playlist

    override fun getItemTitle(): String = current.playList

    override fun getItemInfo(resources: Resources): String {
        val songsCount = current.songs.size
        return resources.getQuantityString(R.plurals.songs, songsCount, songsCount)
    }

    override fun getItemDuration(): String = ""

    override fun getSize(): Int = playlists.size

    override fun getItemMenu(popupMenu: PopupMenu, onClickListener: CustomOnClickListener, listLevel: ListLevel?): PopupMenu {
        popupMenu.inflate(R.menu.menu_playlists)

        popupMenu.setOnMenuItemClickListener { item ->
            val action = when (item.itemId) {
                R.id.addToQueue -> MenuAction.ACTION_ADD_TO_QUEUE
                R.id.renamePlaylist -> MenuAction.ACTION_RENAME_PLAYLIST
                R.id.deletePlaylist -> MenuAction.ACTION_REMOVE_PLAYLIST
                R.id.shareSongs -> MenuAction.ACTION_SHARE
                else -> MenuAction.ACTION_PLAY // R.id.playSongs
            }
            onClickListener.onContextMenuClick(current.songs.toTypedArray(), action, position)
            true
        }

        return popupMenu
    }

    override fun setPosition(position: Int) {
        this.position = position
        current = playlists[position]
    }

    override fun search(matches: Array<Int>): PlaylistModel {
        val list = ArrayList<Playlist>(0)
        if (matches.isNotEmpty())
            matches.mapTo(list) { playlists[it] }
        return PlaylistModel(list.toTypedArray())
    }
}
