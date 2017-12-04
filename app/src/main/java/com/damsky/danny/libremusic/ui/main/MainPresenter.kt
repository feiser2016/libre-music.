package com.damsky.danny.libremusic.ui.main

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.service.MediaPlayerCompanion
import com.damsky.danny.libremusic.service.MediaPlayerService
import com.damsky.danny.libremusic.ui.main.adapters.RecycleAdapter
import com.damsky.danny.libremusic.ui.main.adapters.models.*
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
 * @since 2017-11-28
 */

class MainPresenter {
    companion object {
        val Broadcast_PLAY_AUDIO = "com.damsky.danny.libremusic.PlayAudio"

        /**
         * @param   fab       Is shown when the returned SlideUp is swiped out of visibility.
         * @param   slideUp   Is shown when the returned SlideUp is swiped out of visibility.
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
                time.append(0).append(hours).append(":")
            else if (hours > 9)
                time.append(hours).append(":")

            if (minutes < 10)
                time.append(0)
            time.append(minutes).append(":")

            if (seconds < 10)
                time.append(0)
            time.append(seconds)

            return time.toString()
        }

        fun MainActivity.getAdapter(model: TypeModel, listLevel: ListLevel? = null): RecycleAdapter {
            val adapter = RecycleAdapter(model, listLevel)
            adapter.setCustomOnClickListener(this)
            return adapter
        }

        fun MainActivity.getArtistAdapter(): RecycleAdapter =
                getAdapter(ArtistModel((this.application as App).appDbHelper.getArtists()))

        fun MainActivity.getAlbumAdapter(): RecycleAdapter =
                getAdapter(AlbumModel((this.application as App).appDbHelper.getAlbums()))

        fun MainActivity.getSongAdapter(): RecycleAdapter =
                getAdapter(SongModel((application as App).appDbHelper.getSongs()),
                        (application as App).appDbHelper.getLevel())

        fun MainActivity.getGenreAdapter(): RecycleAdapter =
                getAdapter(GenreModel((application as App).appDbHelper.getGenres()))

        fun MainActivity.getPlaylistAdapter(): RecycleAdapter =
                getAdapter(PlaylistModel((application as App).appDbHelper.getPlaylists()))

        fun MainActivity.getArtistAlbumsAdapter(position: Int): RecycleAdapter =
                getAdapter(AlbumModel((application as App).appDbHelper.getArtistAlbums(position)))

        fun MainActivity.getArtistSongsAdapter(position: Int): RecycleAdapter =
                getAdapter(SongModel((application as App).appDbHelper.getArtistSongs(position)),
                        (application as App).appDbHelper.getLevel())

        fun MainActivity.getAlbumSongsAdapter(position: Int): RecycleAdapter =
                getAdapter(SongModel((application as App).appDbHelper.getAlbumSongs(position)),
                        (application as App).appDbHelper.getLevel())

        fun MainActivity.getGenreSongsAdapter(position: Int): RecycleAdapter =
                getAdapter(SongModel((application as App).appDbHelper.getGenreSongs(position)),
                        (application as App).appDbHelper.getLevel())

        fun MainActivity.getPlaylistSongsAdapter(position: Int): RecycleAdapter =
                getAdapter(SongModel((application as App).appDbHelper.getPlaylistSongs(position)),
                        (application as App).appDbHelper.getLevel())

        fun MainActivity.getQueueAdapter(): RecycleAdapter =
                getAdapter(SongModel((application as App).appDbHelper.getQueue()), ListLevel.QUEUE)

        fun MainActivity.getSong(position: Int): Song =
                (application as App).appDbHelper.getSong(position)

        fun MainActivity.getQueueSong(position: Int): Song =
                (application as App).appDbHelper.getQueueSong(position)

        fun MainActivity.getAlbumSong(position: Int): Song =
                (application as App).appDbHelper.getAlbumSong(position)

        fun MainActivity.getArtistSong(position: Int): Song =
                (application as App).appDbHelper.getArtistSong(position)

        fun MainActivity.getGenreSong(position: Int): Song =
                (application as App).appDbHelper.getGenreSong(position)

        fun MainActivity.getPlaylistSong(position: Int): Song =
                (application as App).appDbHelper.getPlaylistSong(position)

        fun ImageView.glideLoad(context: MainActivity, imageString: String) {
            val size = this.height
            Glide.with(context.applicationContext).load(imageString)
                    .apply(RequestOptions()
                            .fitCenter()
                            .placeholder(R.drawable.song_big)
                            .override(size, size))
                    .into(this)
        }

        fun ImageView.glideLoad(context: Context, imageString: String, placeholderDrawable: Int) {
            val size = this.height
            Glide.with(context).load(imageString)
                    .apply(RequestOptions()
                            .fitCenter()
                            .circleCrop()
                            .placeholder(placeholderDrawable)
                            .override(size, size))
                    .into(this)
        }

        fun MainActivity.showDialog(title: Int, message: Int, positiveAction: () -> Unit) {
            val builder = getBuilder(title, message)
            builder.setPositiveButton(R.string.yes, { dialog, _ ->
                positiveAction()
                dialog.dismiss()
            })
            builder.setNegativeButton(R.string.no, { dialog, _ -> dialog.dismiss() })
            builder.create().show()
        }

        fun MainActivity.showDialog(editText: EditText, hint: Int, title: Int, positiveAction: () -> Unit) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            editText.setHint(hint)

            val builder = getBuilder(title)
            builder.setView(editText)
            builder.setPositiveButton(R.string.ok, { dialog, _ ->
                positiveAction()
                dialog.dismiss()
            })
            builder.setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
            val dialog = builder.create()
            dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.show()
        }

        private fun MainActivity.getBuilder(title: Int): AlertDialog.Builder =
                AlertDialog.Builder(this)
                        .setTitle(title)
                        .setIcon(R.mipmap.ic_launcher)

        private fun MainActivity.getBuilder(title: Int, message: Int): AlertDialog.Builder =
                AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setIcon(R.mipmap.ic_launcher)

        /**
         * @param songsList List of songs to add to a playlist
         *
         * @if              More than one playlist exists
         * @return          An AlertDialog with options to choose which playlists to add the songs to.
         * @else
         * @return          An AlertDialog with an editText asking the user to create a new playlist to add the songs to.
         */
        fun MainActivity.setSongsToPlaylist(songsList: Array<Song>) {
            val playList = (application as App).appDbHelper.getPlaylistsClean()
            if (playList.isNotEmpty()) {
                val builder = getBuilder(R.string.add_to_playlist)
                val playSize = playList.size
                val itemList = Array(playSize, { i -> playList[i].playList })
                val boolList = BooleanArray(playSize, { _ -> false })

                builder.setMultiChoiceItems(itemList, boolList, { _: DialogInterface, which: Int, isChecked: Boolean ->
                    boolList[which] = isChecked
                })

                builder.setPositiveButton(R.string.ok, { dialog, _ ->
                    (0 until playSize)
                            .filter { boolList[it] }
                            .forEach {
                                (application as App).
                                        appDbHelper.insertSongsToPlaylist(itemList[it], songsList)
                            }
                    Snackbar.make(findViewById(R.id.main_content), R.string.success, Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                })

                builder.setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
                builder.create().show()
            } else {
                val editText = EditText(this)
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                editText.setHint(R.string.action_add_playlist_hint)

                val builder = getBuilder(R.string.add_to_playlist)
                builder.setView(editText)
                builder.setPositiveButton(R.string.ok, { dialog, _ ->
                    if (editText.text.isEmpty())
                        Snackbar.make(findViewById<CoordinatorLayout>(R.id.main_content), R.string.text_empty, Snackbar.LENGTH_SHORT).show()
                    else {
                        (application as App).appDbHelper.insertPlaylist("${editText.text}")
                        (application as App).appDbHelper.setPlaylists()
                        (application as App).appDbHelper.insertSongsToPlaylist(editText.text.toString(), songsList)
                        Snackbar.make(findViewById(R.id.main_content), R.string.success, Snackbar.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                })
                builder.setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
                val dialog = builder.create()
                dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                dialog.show()
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
                    Snackbar.make(findViewById(R.id.main_content), R.string.ringtone_success,
                            Snackbar.LENGTH_SHORT).show()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(applicationContext)) {
                    Toast.makeText(this,
                            R.string.permission_modify_settings,
                            Toast.LENGTH_LONG).show()
                    startActivityForResult(
                            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                    Uri.parse("package:$packageName")),
                            200)
                } else
                    setRingtone(song)
            } else
                setRingtone(song)
        }

        fun MainActivity.updateIndexes() {
            (application as App).preferencesHelper.updateIndexes(
                    (application as App).appDbHelper.getPlayableLevel(),
                    (application as App).appDbHelper.getPositions()
            )
        }

        fun MainActivity.updateIndex(position: Int) {
            (application as App).preferencesHelper.updateIndex(position)
        }

        fun MainActivity.addSongsToQueue(list: Array<Song>) {
            (application as App).appDbHelper.addToQueue(list)
        }

        fun MainActivity.playAudio() {
            if (!(application as App).serviceBound) {
                val playerIntent = Intent(applicationContext, MediaPlayerService::class.java)
                startService(playerIntent)
                bindService(playerIntent, (application as App).serviceConnection, Context.BIND_AUTO_CREATE)
            } else
                sendBroadcast(Intent(Broadcast_PLAY_AUDIO))
        }

        fun MainActivity.initializeUi() = launch {
            async {
                val indexes = (application as App).preferencesHelper.getIndexes()
                val song: Song
                if (indexes != null) {
                    song = when (indexes.second) {
                        ListLevel.ARTIST_SONGS ->
                            getArtistSong((application as App).appDbHelper.getAudioIndex())
                        ListLevel.ALBUM_SONGS ->
                            getAlbumSong((application as App).appDbHelper.getAudioIndex())
                        ListLevel.SONGS ->
                            getSong((application as App).appDbHelper.getAudioIndex())
                        ListLevel.GENRE_SONGS ->
                            getGenreSong((application as App).appDbHelper.getAudioIndex())
                        ListLevel.PLAYLIST_SONGS ->
                            getPlaylistSong((application as App).appDbHelper.getAudioIndex())
                        else ->
                            getSong(0)
                    }
                } else
                    song = getSong(0)

                setupPlayerUi(song)
                val repeatVal = if ((application as App).preferencesHelper.getRepeatPreference())
                    R.drawable.repeat_one
                else
                    R.drawable.repeat_all

                val shuffleVal = if ((application as App).preferencesHelper.getShufflePreference())
                    R.drawable.shuffle_on
                else
                    R.drawable.shuffle
                runOnUiThread {
                    repeat.setImageResource(repeatVal)
                    shuffle.setImageResource(shuffleVal)
                    handler.postDelayed(run, 500)
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
                val infoString = "${(application as App).appDbHelper.getAudioIndex() + 1} / ${(application as App).appDbHelper.getQueue().size}"

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
                if (MediaPlayerCompanion.mediaPlayer == null) {
                    playAudio()
                    setImages(R.drawable.pause)
                } else {
                    if (MediaPlayerCompanion.mediaPlayer!!.isPlaying) {
                        MediaPlayerCompanion.transportControls.pause()
                        setImages(R.drawable.play)
                    } else {
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
            (application as App).appDbHelper.setQueue(audioList)
            playAudio()
        }

        fun MainActivity.renamePlaylist(index: Int) {
            val editText = EditText(this)
            showDialog(editText, R.string.action_add_playlist_hint,
                    R.string.action_add_playlist,
                    {
                        if (editText.text.isEmpty())
                            Snackbar.make(findViewById<CoordinatorLayout>(R.id.main_content),
                                    R.string.text_empty, Snackbar.LENGTH_SHORT).show()
                        else {
                            (application as App).appDbHelper.updatePlaylist(
                                    (application as App).appDbHelper.getPlaylistsClean()[index],
                                    editText.text.toString()
                            )
                            myList.adapter = getPlaylistAdapter()
                        }
                    })
        }

        fun MainActivity.removePlaylist(index: Int) {
            (application as App).appDbHelper.deletePlaylist(
                    (application as App).appDbHelper.getPlaylistsClean()[index]
            )
            myList.adapter = getPlaylistAdapter()
        }

        fun MainActivity.removeFromPlaylist(song: Song) {
            (application as App).appDbHelper.deleteSongFromPlaylist(song,
                    (application as App).appDbHelper.getPlaylistsClean()[
                            (application as App).appDbHelper.getSecondIndex()])
            myList.adapter = getPlaylistSongsAdapter((application as App).appDbHelper.getSecondIndex())
        }
    }
}