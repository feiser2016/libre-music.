package com.damsky.danny.libremusic.ui.main

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.damsky.danny.libremusic.R
import java.util.concurrent.TimeUnit

/**
 * Service class containing static util functions.

 * @author Danny Damsky
 * @since 2018-01-21
 */

class MainPresenter {
    companion object {

        fun Int.getTime(): String {
            val long = this.toLong()
            val hours = TimeUnit.MILLISECONDS.toHours(long)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(long) - TimeUnit.HOURS.toMinutes(hours)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(long) - TimeUnit.MINUTES.toSeconds(minutes)

            val time = StringBuilder()

            if (hours in 1..9)
                time.append(0).append(hours).append(':')
            else if (hours > 9)
                time.append(hours).append(':')

            if (minutes < 10)
                time.append(0)
            time.append(minutes).append(':')

            if (seconds < 10)
                time.append(0)
            time.append(seconds)

            return time.toString()
        }

        private fun ImageView.glideLoad(context: Context, imageString: String, requestOptions: RequestOptions) {
            Glide.with(context.applicationContext).load(imageString)
                    .apply(requestOptions)
                    .into(this)
        }

        private fun ImageView.getDefaultRequestOptions(placeholderDrawable: Int)
                = RequestOptions().fitCenter().placeholder(placeholderDrawable).override(this.height)

        fun ImageView.glideLoad(context: MainActivity, imageString: String)
                = glideLoad(context, imageString, getDefaultRequestOptions(R.drawable.song_big))

        fun ImageView.glideLoad(context: Context, imageString: String, placeholderDrawable: Int)
                = glideLoad(context, imageString, getDefaultRequestOptions(placeholderDrawable).circleCrop())
    }
}
