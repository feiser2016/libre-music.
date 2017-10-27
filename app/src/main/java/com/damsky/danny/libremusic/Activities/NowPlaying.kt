/*
This activity opens up when a song is pressed in the library presented in LibrePlayer,
it's in charge of playing said song as well as showing the song's information and allowing the user
to control the song's progress with a SeekBar as well as media control buttons.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.Services.MediaPlayerService
import kotlinx.android.synthetic.main.activity_now_playing.*
import java.util.concurrent.TimeUnit

class NowPlaying : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    /*
    Kotlin Extensions:
    TextViews: artistName, songName, countTime, indexInfo
    SeekBar: seekBar
    ImageView: coverArt
     */
    private val handler = Handler()
    private val run : Runnable = object : Runnable {
        override fun run() {
            setupUI()
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        lateinit var player: MediaPlayerService
        var serviceBound = false

        val serviceConnection = object : ServiceConnection { // Binding this Client to the AudioPlayer Service
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                val binder = service as MediaPlayerService.LocalBinder
                player = binder.service
                serviceBound = true
            }

            override fun onServiceDisconnected(name: ComponentName) {
                serviceBound = false
            }
        }

        const val Broadcast_PLAY_NEW_AUDIO = "com.damsky.danny.libremusic.PlayNewAudio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        evaluateTheme()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        seekBar.setOnSeekBarChangeListener(this)
        val pos = intent.getIntExtra("position", -1)
        if (pos != -1)
            playAudio(pos)
        handler.postDelayed(run, 200)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("ServiceState", serviceBound)
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("ServiceState")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.now_playing_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onRestart() {
        super.onRestart()
        setupUI()
        handler.postDelayed(run, 200)
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if (p2)
            MediaPlayerService.transportControls.seekTo(p0!!.progress.toLong() +
            MediaPlayerService.activeAudio.starttime)
    }

    // Useless methods
    override fun onStartTrackingTouch(p0: SeekBar?) {}
    override fun onStopTrackingTouch(p0: SeekBar?) {}

    private fun setupUI () {
        if (!MediaPlayerService.mediaPlayer!!.isPlaying)
            playPause.setImageResource(R.drawable.play)
        else
            playPause.setImageResource(R.drawable.pause)

        if (!MediaPlayerService.repeat)
            repeat.setImageResource(R.drawable.repeat_all)
        else
            repeat.setImageResource(R.drawable.repeat_one)

        if (!MediaPlayerService.shuffle)
            shuffle.setImageResource(R.drawable.shuffle)
        else
            shuffle.setImageResource(R.drawable.shuffle_on)

        val song = MediaPlayerService.activeAudio
        val position = MediaPlayerService.mediaPlayer!!.currentPosition
        artistName.text = "${song.artist} - ${song.album}"
        songName.text = song.title
        countTime.text = "${getTime(position - song.starttime)} / ${getTime(song.duration)}"
        indexInfo.text = "${MediaPlayerService.audioIndex + 1} / ${MediaPlayerService.audioList.size}"
        seekBar.max = song.endtime - song.starttime
        seekBar.progress = position - song.starttime
        val height = coverArt.height
        Glide
                .with(this)
                .load(song.cover)
                .apply(RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.song_big)
                        .override(height, height))
                .into(coverArt)
    }

    private fun getTime(timeInMillis: Int): String {
        val long = timeInMillis.toLong()
        val hours = TimeUnit.MILLISECONDS.toHours(long)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(long) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(long) - TimeUnit.MINUTES.toSeconds(minutes)

        val time = StringBuilder()

        if (hours > 9)
            time.append(hours).append(":")

        if (minutes < 9)
            time.append(0)
        time.append(minutes).append(":")

        if (seconds < 9)
            time.append(0)
        time.append(seconds)

        return time.toString()
    }

    fun playPrev(view: View) {
        if (!MediaPlayerService.mediaPlayer!!.isPlaying)
            playPause.setImageResource(R.drawable.pause)
        MediaPlayerService.transportControls.skipToPrevious()
    }

    fun playNext(view: View) {
        if (!MediaPlayerService.mediaPlayer!!.isPlaying)
            playPause.setImageResource(R.drawable.pause)
        MediaPlayerService.transportControls.skipToNext()
    }

    fun playPause(view: View) {
        if (MediaPlayerService.mediaPlayer!!.isPlaying) {
            playPause.setImageResource(R.drawable.play)
            MediaPlayerService.transportControls.pause()
        }
        else {
            playPause.setImageResource(R.drawable.pause)
            MediaPlayerService.transportControls.play()
        }
    }

    fun repeatSet(view: View) {
        MediaPlayerService.repeat = !MediaPlayerService.repeat
        if (!MediaPlayerService.repeat)
            repeat.setImageResource(R.drawable.repeat_all)
        else
            repeat.setImageResource(R.drawable.repeat_one)
    }

    fun shufflePlaylist(view: View) {
        MediaPlayerService.shuffle = !MediaPlayerService.shuffle
        if (!MediaPlayerService.shuffle)
            shuffle.setImageResource(R.drawable.shuffle)
        else
            shuffle.setImageResource(R.drawable.shuffle_on)
    }

    private fun playAudio(position: Int) {
        //Check is service is active
        if (!serviceBound) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            playerIntent.putExtra("position", position)
            startService(playerIntent)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            MediaPlayerService.audioIndex = position
            val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
        }
    }

    private fun evaluateTheme() {
        if (LibrePlayer.pitch_black == R.style.AppTheme_Black) {
            setTheme(LibrePlayer.pitch_black)
            setContentView(R.layout.activity_now_playing)
            val grayDark = Color.parseColor("#101010")
            now_playing_layout.setBackgroundColor(android.R.color.black)
            now_playing_linearlayout.setBackgroundColor(grayDark)
            seekBar.setBackgroundColor(grayDark)
        }
        else
            setContentView(R.layout.activity_now_playing)
    }
}
