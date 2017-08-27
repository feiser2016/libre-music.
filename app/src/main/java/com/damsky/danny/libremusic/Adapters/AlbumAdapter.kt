/*
This is a custom adapter for a ListView.
It holds one ImageView and two TextViews.
The ImageView will be populated with the album's cover art,
the "content" TextView will be populated with the album's name, and the
"duration" TextView will be populated with the album's year of publish.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.damsky.danny.libremusic.DB.Album
import com.damsky.danny.libremusic.R

class AlbumAdapter(context: Context, xItems: ArrayList<Album>) : ArrayAdapter<Album>(context, 0, xItems) {

    // View lookup cache
    private class ViewHolder {
        internal lateinit var content: TextView
        internal lateinit var duration: TextView
        internal lateinit var cover: ImageView
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var cv = convertView
        // Get the data item for this position
        val xItem = getItem(position)

        val viewHolder: ViewHolder // view lookup cache stored in tag
        if (cv == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            cv = inflater.inflate(R.layout.simple_audio_item_1, parent, false)
            viewHolder.content = cv!!.findViewById(R.id.itemText)
            viewHolder.duration = cv.findViewById(R.id.itemDuration)
            viewHolder.cover = cv.findViewById(R.id.itemImage)
            // Cache the viewHolder object inside the fresh view
            cv.tag = viewHolder
        } else
        // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = cv.tag as ViewHolder

        val height = viewHolder.cover.height

        // Populate the data from the data object via the viewHolder object
        // into the template view.
        viewHolder.content.text = xItem.album
        Glide
                .with(context)
                .load(xItem.cover)
                .apply(RequestOptions()
                        .fitCenter()
                        .circleCrop()
                        .placeholder(R.drawable.album)
                        .override(height, height))
                .into(viewHolder.cover)
        viewHolder.duration.text = if (xItem.year > 0) "${xItem.year}" else "        "
        // Return the completed view to render on screen
        return cv
    }
}
