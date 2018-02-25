package com.damsky.danny.libremusic.ui.main.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.models.TypeModel
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.glideLoad
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener
import kotlinx.android.synthetic.main.activity_main_cardview.view.*

/**
 * A RecyclerView adapter that is used with the MainActivity.
 *
 * @param onClickListener An interface to communicate with the activity that uses the RecyclerView.
 * @param contents  A TypeModel of any type.
 * @param listLevel The level of the list passed (important for certain lists, null by default).
 *
 * @author Danny Damsky
 * @since 2018-02-25
 */

class RecycleAdapter(private val onClickListener: CustomOnClickListener,
                     private val contents: TypeModel, private val listLevel: ListLevel? = null) :
        RecyclerView.Adapter<RecycleAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.activity_main_cardview, parent, false))
    }

    override fun getItemCount(): Int {
        return contents.getSize()
    }

    /**
     * @param holder   ItemViewHolder type containing all internal variables to be set.
     * @param position The position of the pressed item in the list
     */
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(contents, position, listLevel, onClickListener)
    }

    /**
     * In charge of finding all views of the RecyclerView and setting them to internal variables.
     *
     * @param itemView A View object
     */
    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(contents: TypeModel, position: Int, listLevel: ListLevel?,
                 onClickListener: CustomOnClickListener) = with(itemView) {

            contents.setPosition(position)

            itemImage.glideLoad(cards.context, contents.getItemImage(), contents.getPlaceHolderImage())

            itemTitle.text = contents.getItemTitle()
            itemInfo.text = contents.getItemInfo(cards.context.resources)
            itemDuration.text = contents.getItemDuration()

            cards.setOnClickListener {
                onClickListener.onRecyclerClick(position)
            }

            itemMenu.setOnClickListener({
                contents.setPosition(position)
                contents.getItemMenu(PopupMenu(cards.context, itemMenu),
                        onClickListener, listLevel).show()
            })
        }

    }
}
