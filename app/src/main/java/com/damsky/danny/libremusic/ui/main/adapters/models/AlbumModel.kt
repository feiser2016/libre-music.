package com.damsky.danny.libremusic.ui.main.adapters.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Album
import com.damsky.danny.libremusic.ui.main.MenuAction
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener

/**
 * A TypeModel designed to take an array of Album objects.
 * @param albums An array of albums to use with the AlbumModel.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class AlbumModel(val albums: Array<Album>) : TypeModel {
    private lateinit var current: Album

    override fun getItemImage(): String = current.cover

    override fun getPlaceHolderImage() = R.drawable.album

    override fun getItemTitle(): String = current.album

    override fun getItemInfo(resources: Resources): String {
        val songsCount = current.songs.size
        return "${current.artist} | ${resources.getQuantityString(R.plurals.songs, songsCount, songsCount)}"
    }

    override fun getItemDuration(): String = if (current.year > 0) "${current.year}" else ""

    override fun getSize(): Int = albums.size

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
        current = albums[position]
    }

    override fun search(matches: Array<Int>): AlbumModel {
        val list = ArrayList<Album>(0)
        if (matches.isNotEmpty())
            matches.mapTo(list) { albums[it] }
        return AlbumModel(list.toTypedArray())
    }
}
