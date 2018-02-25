package com.damsky.danny.libremusic.data.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getTime
import com.damsky.danny.libremusic.ui.main.MenuAction
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener

/**
 * A TypeModel designed to take an array of Song objects.
 * @param songs An array of songs to use with the SongModel.
 *
 * @author Danny Damsky
 * @since 2018-02-25
 */

class SongModel(private val songs: Array<Song>) : TypeModel {

    private lateinit var current: Song

    override fun getItemImage(): String {
        return current.cover
    }

    override fun getPlaceHolderImage(): Int {
        return R.drawable.song
    }

    override fun getItemTitle(): String {
        return current.title
    }

    override fun getItemInfo(resources: Resources): String {
        return "${current.artist} - ${current.album}"
    }

    override fun getItemDuration(): String {
        return current.duration.getTime()
    }

    override fun getSize(): Int {
        return songs.size
    }

    override fun getItemMenu(popupMenu: PopupMenu, onClickListener: CustomOnClickListener, listLevel: ListLevel?): PopupMenu {
        val list = arrayOf(current)

        popupMenu.inflate(when (listLevel) {
            ListLevel.PLAYLIST_SONGS -> R.menu.menu_playlist_songs
            ListLevel.SONGS -> R.menu.menu_songs
            else -> R.menu.menu_queue // ListLevel.Queue
        })

        popupMenu.setOnMenuItemClickListener { item ->
            val action = when (item.itemId) {
                R.id.addToQueue -> MenuAction.ACTION_ADD_TO_QUEUE
                R.id.setAsRingtone -> MenuAction.ACTION_SET_AS_RINGTONE
                R.id.removeFromPlaylist -> MenuAction.ACTION_REMOVE_FROM_PLAYLIST
                R.id.shareSongs -> MenuAction.ACTION_SHARE
                R.id.addSongsToPlaylist -> MenuAction.ACTION_ADD_TO_PLAYLIST
                else -> MenuAction.ACTION_PLAY // R.id.playSongs
            }
            onClickListener.onContextMenuClick(list, action)
            true
        }

        return popupMenu
    }

    override fun setPosition(position: Int) {
        current = songs[position]
    }

    override fun search(matches: Array<Int>): SongModel {
        val list = ArrayList<Song>(0)
        if (matches.isNotEmpty())
            matches.mapTo(list) { songs[it] }
        return SongModel(list.toTypedArray())
    }
}
