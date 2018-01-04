package com.damsky.danny.libremusic.data.models

import android.content.res.Resources
import android.widget.PopupMenu
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener

/**
 * Interface used to communicate different objects with the RecyclerView
 *
 * @author Danny Damsky
 * @since 2018-01-04
 */
interface TypeModel {
    /**
     * @return A string containing the path to an image.
     */
    fun getItemImage(): String

    /**
     * @return An image resource of the default image (In case getItemImage returns "none")
     */
    fun getPlaceHolderImage(): Int

    /**
     * @return A string with the Item's title.
     */
    fun getItemTitle(): String

    /**
     * @param resources Allows searching of strings in the resources file.
     * @return          A string containing info about the item.
     */
    fun getItemInfo(resources: Resources): String

    /**
     * @return A string representing the duration of the item (For example: 2017, 09:58, etc.).
     */
    fun getItemDuration(): String

    /**
     * @return Amount of items in the object.
     */
    fun getSize(): Int

    /**
     * @param popupMenu       Gets an initialized PopupMenu from the RecyclerView.
     * @param onClickListener Used to activate OnClick for the PopupMenu.
     * @param listLevel       The level of the given list (necessary for certain lists, null by default).
     *
     * @return                The same PopupMenu with the onClick events already set-up.
     */
    fun getItemMenu(popupMenu: PopupMenu, onClickListener: CustomOnClickListener, listLevel: ListLevel? = null): PopupMenu

    /**
     * @param position Position to set for the list of items inside the TypeModel
     */
    fun setPosition(position: Int)

    /**
     * @param matches An array containing indexes to get from the list in TypeModel
     *
     * @return A TypeModel with a list that matches the indexes given.
     */
    fun search(matches: Array<Int>): TypeModel

}
