/*
This is a custom adapter for a ListView.
It holds one ImageView and two TextViews.
The ImageView will be populated with one of the artist's albums' cover art,
the "content" TextView will be populated with the artist's name, and the
"duration" TextView will not be populated (but is required for other adapters)

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
import com.damsky.danny.libremusic.DB.Artist
import com.damsky.danny.libremusic.R

class ArtistAdapter(context: Context, xItems: ArrayList<Artist>) : ArrayAdapter<Artist>(context, 0, xItems) {

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
        viewHolder.content.text = xItem.artist
        val albums = xItem.albums
        var iterate = "none"
        for (i in 0 until albums.size) {
            iterate = albums[i].cover
            if (iterate != "none")
                break
        }
        Glide
                .with(context)
                .load(iterate)
                .apply(RequestOptions()
                        .fitCenter()
                        .circleCrop()
                        .placeholder(R.drawable.artist)
                        .override(height, height))
                .into(viewHolder.cover)
        
        // Return the completed view to render on screen
        return cv
    }
}