package com.damsky.danny.libremusic.ui.main.listeners

import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.ui.main.MenuAction

/**
 * This interface is used to communicate between RecyclerView and MainActivity
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

interface CustomOnClickListener {
    /**
     * OnClick function for items of the RecyclerView
     *
     * @param position The position in the list where the user pressed.
     */
    fun onRecyclerClick(position: Int)


    /**
     * OnClick function for the menu items of the RecyclerView
     *
     * @param songsList Array of songs
     * @param action    Tells which action should be performed (according to menu item pressed).
     * @param index     The index of a specific item whose menu item was pressed (default is -1)
     */
    fun onContextMenuClick(songsList: Array<Song>, action: MenuAction, index: Int = -1)
}
