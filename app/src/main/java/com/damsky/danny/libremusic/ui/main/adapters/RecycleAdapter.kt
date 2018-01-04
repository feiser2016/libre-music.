package com.damsky.danny.libremusic.ui.main.adapters

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.models.TypeModel
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.glideLoad
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener

/**
 * A RecyclerView adapter that is used with the MainActivity.
 *
 * @param contents  A TypeModel of any type.
 * @param listLevel The level of the list passed (important for certain lists, null by default)
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

class RecycleAdapter(private val contents: TypeModel, private val listLevel: ListLevel? = null) :
        RecyclerView.Adapter<RecycleAdapter.ItemViewHolder>() {

    private lateinit var onClickListener: CustomOnClickListener

    /**
     * Takes some context and applies the onClickListener to it.
     *
     * @param customOnClickListener
     */
    fun setCustomOnClickListener(customOnClickListener: CustomOnClickListener) {
        onClickListener = customOnClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemViewHolder(LayoutInflater
            .from(parent.context).inflate(R.layout.activity_main_cardview, parent, false))

    override fun getItemCount(): Int = contents.getSize()

    /**
     * @param holder   ItemViewHolder type containing all internal variables to be set.
     * @param position The position of the pressed item in the list
     */
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        contents.setPosition(position)

        holder.itemImage.glideLoad(holder.cards.context, contents.getItemImage(), contents.getPlaceHolderImage())

        holder.itemTitle.text = contents.getItemTitle()
        holder.itemInfo.text = contents.getItemInfo(holder.cards.context.resources)
        holder.itemDuration.text = contents.getItemDuration()

        holder.cards.setOnClickListener {
            onClickListener.onRecyclerClick(position)
        }

        holder.itemMenu.setOnClickListener({
            contents.setPosition(position)
            contents.getItemMenu(PopupMenu(holder.cards.context, holder.itemMenu),
                    onClickListener, listLevel).show()
        })
    }

    /**
     * In charge of finding all views of the RecyclerView and setting them to internal variables.
     *
     * @param itemView A View object
     */
    class ItemViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val cards: CardView = itemView.findViewById(R.id.cards)
        internal val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        internal val itemTitle: TextView = itemView.findViewById(R.id.itemTitle)
        internal val itemInfo: TextView = itemView.findViewById(R.id.itemInfo)
        internal val itemDuration: TextView = itemView.findViewById(R.id.itemDuration)
        internal val itemMenu: ImageButton = itemView.findViewById(R.id.itemMenu)
    }
}
