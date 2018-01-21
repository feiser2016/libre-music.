package com.damsky.danny.libremusic.ui.main

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.data.models.TypeModel
import com.damsky.danny.libremusic.service.MediaPlayerCompanion
import com.damsky.danny.libremusic.service.MediaPlayerService
import com.damsky.danny.libremusic.ui.main.adapters.RecycleAdapter
import com.mancj.slideup.SlideUp
import com.mancj.slideup.SlideUpBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nowplaying_main.*
import kotlinx.android.synthetic.main.songinfo_main.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Service class containing static variables/functions for use with MainActivity

 * @author Danny Damsky
 * @since 2018-01-04
 */

class MainPresenter {
    companion object {
        private const val REQUEST_WRITE_SETTINGS = 200

        const val Broadcast_PLAY_AUDIO = "com.damsky.danny.libremusic.PlayAudio"
        const val UI_UPDATE_INTERVAL_MILLIS: Long = 500

        /**
         * @param fab     Is shown when the returned SlideUp is swiped out of visibility.
         * @param slideUp Is shown when the returned SlideUp is swiped out of visibility.
         * @return            SlideUp object with the following settings.
         */
        fun View.getSlideUp(fab: FloatingActionButton, slideUp: SlideUp): SlideUp =
                SlideUpBuilder(this).withListeners(object : SlideUp.Listener.Events {
                    override fun onSlide(percent: Float) {
                        this@getSlideUp.alpha = 1 - (percent / 100)
                    }

                    override fun onVisibilityChanged(visibility: Int) {
                        if (visibility == View.GONE) {
                            fab.show()
                            slideUp.show()
                        }
                    }
                }).withTouchableAreaDp(1000.toFloat()).build()

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

        fun MainActivity.getAdapter(pair: Pair<TypeModel, ListLevel?>): RecycleAdapter {
            val adapter = RecycleAdapter(pair.first, pair.second)
            adapter.setCustomOnClickListener(this)
            return adapter
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

        /**
         * @param songsList List of songs to add to a playlist
         *
         * @if              More than one playlist exists
         * @return          An AlertDialog with options to choose which playlists to add the songs to.
         * @else
         * @return          An AlertDialog with an editText asking the user to create a new playlist to add the songs to.
         */
        fun MainActivity.setSongsToPlaylist(songsList: Array<Song>) {
            val playList = appReference.appDbHelper.getPlaylistsClean()
            if (playList.isNotEmpty()) {
                val playSize = playList.size
                val itemList = Array(playSize, { i -> playList[i].playList })
                val boolList = BooleanArray(playSize, { _ -> false })

                display.showDialog(R.string.add_to_playlist, itemList, boolList, {
                    (0 until playSize).filter { boolList[it] }.forEach {
                        appReference.appDbHelper.insertSongsToPlaylist(itemList[it], songsList)
                    }
                    display.showSnackShort(R.string.success)
                })
            } else {
                val editText = EditText(this)
                display.showDialog(R.string.add_to_playlist, R.string.action_add_playlist_hint, editText, {
                    if (editText.text.isEmpty())
                        display.showSnackShort(R.string.text_empty)
                    else {
                        appReference.appDbHelper.insertPlaylist("${editText.text}")
                        appReference.appDbHelper.setPlaylists()
                        appReference.appDbHelper.insertSongsToPlaylist(editText.text.toString(), songsList)
                        display.showSnackShort(R.string.success)
                    }
                })
            }
        }

        fun MainActivity.shareFiles(songsList: Array<Song>) {
            if (songsList.size > 1) {
                val filesToSend = ArrayList<Uri>(songsList.size)
                songsList.mapTo(filesToSend) { Uri.parse(it.data) }

                val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent.type = "audio/*"
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesToSend)
                startActivity(Intent.createChooser(intent, resources.getString(R.string.share_songs)))
            } else
                shareFile(Uri.parse(songsList[0].data))
        }

        private fun MainActivity.shareFile(fileToSend: Uri) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "audio/*"
            intent.putExtra(Intent.EXTRA_STREAM, fileToSend)
            startActivity(Intent.createChooser(intent, resources.getString(R.string.share_song)))
        }

        private fun MainActivity.setRingtone(song: Song) = launch {
            async {
                val ringtoneFile = File(song.data)
                val content = ContentValues()
                content.put(MediaStore.MediaColumns.DATA, song.data)
                content.put(MediaStore.MediaColumns.TITLE, song.title)
                content.put(MediaStore.MediaColumns.SIZE, ringtoneFile.length())
                content.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*")
                content.put(MediaStore.Audio.Media.DURATION, song.duration)
                content.put(MediaStore.Audio.Media.IS_RINGTONE, true)
                content.put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                content.put(MediaStore.Audio.Media.IS_ALARM, false)
                content.put(MediaStore.Audio.Media.IS_MUSIC, false)

                val uri = MediaStore.Audio.Media.getContentUriForPath(song.data)

                applicationContext.contentResolver.delete(uri,
                        "${MediaStore.MediaColumns.DATA}=\"${song.data}\"",
                        null)

                val newUri = applicationContext.contentResolver.insert(uri, content)
                RingtoneManager.setActualDefaultRingtoneUri(applicationContext,
                        RingtoneManager.TYPE_RINGTONE, newUri)

                runOnUiThread {
                    display.showToastShort(R.string.ringtone_success)
                }
            }.await()
        }

        /**
         * Ringtone will be set once permissions are set to allow modifying system settings.
         *
         *  @param song Song to set as ringtone.
         */
        @SuppressLint("InlinedApi")
        fun MainActivity.setAsRingtone(song: Song) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(applicationContext)) {
                display.showToastLong(R.string.permission_modify_settings)
                startActivityForResult(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:$packageName")), REQUEST_WRITE_SETTINGS)
            } else
                setRingtone(song)
        }

        fun MainActivity.playAudio() {
            if (!appReference.serviceBound) {
                val playerIntent = Intent(applicationContext, MediaPlayerService::class.java)
                startService(playerIntent)
                bindService(playerIntent, appReference.serviceConnection, Context.BIND_AUTO_CREATE)
            } else
                sendBroadcast(Intent(Broadcast_PLAY_AUDIO))
        }

        fun MainActivity.initializeUi() = launch {
            async {
                val indexes = appReference.preferencesHelper.getIndexes()
                val song: Song
                if (indexes != null) {
                    song = when (indexes.second) {
                        ListLevel.ARTIST_SONGS ->
                            appReference.appDbHelper.getArtistSong(appReference.appDbHelper.getAudioIndex())
                        ListLevel.ALBUM_SONGS ->
                            appReference.appDbHelper.getAlbumSong(appReference.appDbHelper.getAudioIndex())
                        ListLevel.SONGS ->
                            appReference.appDbHelper.getSong(appReference.appDbHelper.getAudioIndex())
                        ListLevel.GENRE_SONGS ->
                            appReference.appDbHelper.getGenreSong(appReference.appDbHelper.getAudioIndex())
                        ListLevel.PLAYLIST_SONGS ->
                            appReference.appDbHelper.getPlaylistSong(appReference.appDbHelper.getAudioIndex())
                        else ->
                            appReference.appDbHelper.getSong(0)
                    }
                } else
                    song = appReference.appDbHelper.getSong(0)

                setupPlayerUi(song)
                val repeatVal = if (appReference.preferencesHelper.getRepeatPreference())
                    R.drawable.repeat_one
                else
                    R.drawable.repeat_all

                val shuffleVal = if (appReference.preferencesHelper.getShufflePreference())
                    R.drawable.shuffle_on
                else
                    R.drawable.shuffle
                runOnUiThread {
                    repeat.setImageResource(repeatVal)
                    shuffle.setImageResource(shuffleVal)
                    handler.postDelayed(run, UI_UPDATE_INTERVAL_MILLIS)
                }
            }.await()
        }

        fun MainActivity.setupPlayerUi(song: Song) = launch {
            async {
                val playVal = try {
                    if (MediaPlayerCompanion.mediaPlayer!!.isPlaying)
                        R.drawable.pause
                    else
                        R.drawable.play
                } catch (e: Exception) {
                    R.drawable.play
                }

                val position = try {
                    MediaPlayerCompanion.mediaPlayer!!.currentPosition
                } catch (e: Exception) {
                    song.startTime
                }

                val seekBarMax = song.endTime - song.startTime
                val seekBarProgress = position - song.startTime
                val durationInTime = "${(position - song.startTime).getTime()} / ${song.duration.getTime()}"
                val artistNameString = "${song.artist} - ${song.album}"
                val infoString = "${appReference.appDbHelper.getAudioIndex() + 1} / ${appReference.appDbHelper.getQueue().size}"

                runOnUiThread {
                    setImages(playVal)
                    seekBar.max = seekBarMax
                    seekBar.progress = seekBarProgress
                    artistName.text = artistNameString
                    songName.text = song.title
                    countTime.text = slideView_duration.text
                    indexInfo.text = infoString
                    slideView_artistName.text = song.artist
                    slideView_songName.text = song.title
                    slideView_duration.text = durationInTime
                    slideView_cover.glideLoad(this@setupPlayerUi, song.cover)
                    songNavImage.glideLoad(this@setupPlayerUi, song.cover)
                    coverArt.glideLoad(this@setupPlayerUi, song.cover)
                }
            }.await()
        }

        private fun MainActivity.setImages(resourceId: Int) {
            playPause.setImageResource(resourceId)
            fab.setImageResource(resourceId)
        }

        fun MainActivity.playOrPause() =
                when {
                    MediaPlayerCompanion.mediaPlayer == null -> {
                        playAudio()
                        setImages(R.drawable.pause)
                    }
                    MediaPlayerCompanion.mediaPlayer!!.isPlaying -> {
                        MediaPlayerCompanion.transportControls.pause()
                        setImages(R.drawable.play)
                    }
                    else -> {
                        MediaPlayerCompanion.transportControls.play()
                        setImages(R.drawable.pause)
                    }
                }

        fun MainActivity.playNextOrPrevious(incrementOrDecrement: () -> Unit, playNextOrPrevious: () -> Unit) =
                if (MediaPlayerCompanion.mediaPlayer == null) {
                    incrementOrDecrement()
                    setImages(R.drawable.pause)
                    playAudio()
                } else {
                    if (!MediaPlayerCompanion.mediaPlayer!!.isPlaying)
                        setImages(R.drawable.pause)
                    playNextOrPrevious()
                }

        fun MainActivity.playNewList(audioList: Array<Song>) {
            appReference.appDbHelper.setQueue(audioList)
            playAudio()
        }

        fun MainActivity.renamePlaylist(index: Int) {
            val editText = EditText(this)
            display.showDialog(R.string.action_add_playlist, R.string.action_add_playlist_hint, editText,
                    {
                        if (editText.text.isEmpty())
                            display.showSnackShort(R.string.text_empty)
                        else {
                            appReference.appDbHelper.updatePlaylist(index, editText.text.toString())
                            myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
                        }
                    })
        }

        fun MainActivity.removePlaylist(index: Int) {
            appReference.appDbHelper.deletePlaylist(index)
            myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
        }

        fun MainActivity.removeFromPlaylist(song: Song) {
            appReference.appDbHelper.deleteSongFromPlaylist(song)
            myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistSongsModel(appReference.appDbHelper.getSecondIndex()))
        }
    }
}
