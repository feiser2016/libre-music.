/*
This service is in charge of all the media player's functions,
including setting a notification, allowing media controls, reading the phone's state
to determine which action to undergo, etc.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Services

import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.app.NotificationCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.damsky.danny.libremusic.Activities.NowPlaying
import com.damsky.danny.libremusic.DB.Song
import com.damsky.danny.libremusic.Enum.PlaybackStatus
import com.damsky.danny.libremusic.R
import java.io.IOException

class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private val iBinder = LocalBinder() // A binder for this entire service
    private lateinit var audioManager: AudioManager // AudioManager is in charge of requests

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    } // Pauses the audio upon listening device state change

    private val playNewAudio = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (audioIndex != -1 && audioIndex < audioList.size)
                activeAudio = audioList[audioIndex]
            else
                stopSelf()

            stopMedia()
            mediaPlayer!!.reset()
            initMediaPlayer()
            buildNotification(PlaybackStatus.PLAYING)
        }
    } // Receives "Play new audio file" requests from an activity

    private val handler = Handler() // Handler used for counting until song ends.

    private var resumePosition = -1 // Saves the media player position when a song is paused

    //Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private lateinit var telephonyManager: TelephonyManager

    // Handle state changes in the media player
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null

    // Constants used by the MediaPlayerService class
    private val NOTIFICATION_ID = 101
    private val ACTION_PLAY = "com.damsky.danny.libremusic.ACTION_PLAY"
    private val ACTION_PAUSE = "com.damsky.danny.libremusic.ACTION_PAUSE"
    private val ACTION_PREVIOUS = "com.damsky.danny.libremusic.ACTION_PREVIOUS"
    private val ACTION_NEXT = "com.damsky.danny.libremusic.ACTION_NEXT"
    private val ACTION_STOP = "com.damsky.danny.libremusic.ACTION_STOP"

    companion object {
        lateinit var audioList : ArrayList<Song> // A queue of songs to be played
        var audioIndex = -1 // Current song number in the audioList that's active
        lateinit var activeAudio : Song // The actual song that's active
        var mediaPlayer: MediaPlayer? = null
        lateinit var transportControls: MediaControllerCompat.TransportControls
    }

    // The class used by the binder
    inner class LocalBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    override fun onCreate() {
        super.onCreate()                // When the class is created
        callStateListener()             // 1. Start listening for calls
        registerBecomingNoisyReceiver() // 2. Register the becomingNoisyReceiver object
        registerPlayNewAudio()          // 3. Register the playNewAudio object
    }

    // Returns the binder when an activity is trying to bind this service
    override fun onBind(p0: Intent?) = iBinder

    override fun onCompletion(p0: MediaPlayer?) {
        stopMedia()
        stopSelf()
    } // Stops everything when playback has finished

    override fun onPrepared(p0: MediaPlayer?) {
        playMedia()
    } // If the media player is prepared - start playing

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        when (p1) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK ->
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $p2")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED ->
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED $p2")
            MediaPlayer.MEDIA_ERROR_UNKNOWN ->
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN $p2")
        }
        return false
    } // Invoked when there has been an error during an asynchronous operation.

    override fun onSeekComplete(p0: MediaPlayer?) {} // Indicates that a seek operation completed

    override fun onInfo(p0: MediaPlayer?, p1: Int, p2: Int): Boolean = false // Communicates info

    override fun onBufferingUpdate(p0: MediaPlayer?, p1: Int) {} // Used for internet buffering updates

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            audioIndex = intent!!.extras.getInt("position")
        } catch (e: Exception) {
            e.printStackTrace()
        } // An audio file position is passed to the service through putExtra
        activeAudio = audioList[audioIndex] // set the requested song to activeAudio

        if (!requestAudioFocus()) // Try to request audio focus
            stopSelf()            // Stop the service upon failure

          if (mediaSessionManager == null) { // if the media session manager hasn't been initialized
              try { // Initialize the media session and the media player
                  initMediaSession()
                  initMediaPlayer()
              } catch (e: RemoteException) {
                  e.printStackTrace()
                  stopSelf()
              }

              buildNotification(PlaybackStatus.PLAYING) // Creates an audio notification
          }

        handleIncomingActions(intent) // Sorts out what to do with the passed intent's action
        return super.onStartCommand(intent, flags, startId)
    } // Runs when the service starts, initialization is done here

    override fun onDestroy() {
        super.onDestroy()

        // Stops the media player
        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer!!.release()
        }

        // Removes audio focus
        removeAudioFocus()

        // Stops listening to calls
        if (phoneStateListener != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        removeNotification()

        // Unregisters BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(playNewAudio)
    } // Runs when the service got a self-destruct request

    override fun onAudioFocusChange(p0: Int) {
        when (p0) {
            AudioManager.AUDIOFOCUS_GAIN -> { // Resume playback
                if (mediaPlayer == null)
                    initMediaPlayer()
                else if (!mediaPlayer!!.isPlaying)
                    mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> { // Lost focus for an unbounded amount of time
                if (mediaPlayer!!.isPlaying)
                    mediaPlayer!!.stop()      // Stop playback
                mediaPlayer!!.release()       // Release the media player
                mediaPlayer = null
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> // Lost focus for a short time
                if (mediaPlayer!!.isPlaying)
                    mediaPlayer!!.pause()             // Pause the media player

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> // Lost focus, ducking is allowed
                if (mediaPlayer!!.isPlaying)
                    mediaPlayer!!.setVolume(0.1f, 0.1f)       // Set music volume to 10 percent
        }
    } // Runs when the audio focus changes

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result : Int = if (Build.VERSION.SDK_INT >= 26)
            audioManager.requestAudioFocus(audioFocusRequest())
        else
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        return (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) // True if focus gained
    } // Requests audio focus using the audioManager, returns true if focus was gained

    // Removes audio focus using the audioManager, returns true if focus was removed
    private fun removeAudioFocus(): Boolean =
            if (Build.VERSION.SDK_INT >= 26)
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocusRequest(audioFocusRequest())
            else
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this)

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()

        // set up media event listeners
        mediaPlayer!!.setOnCompletionListener(this)
        mediaPlayer!!.setOnErrorListener(this)
        mediaPlayer!!.setOnPreparedListener(this)
        mediaPlayer!!.setOnBufferingUpdateListener(this)
        mediaPlayer!!.setOnSeekCompleteListener(this)
        mediaPlayer!!.setOnInfoListener(this)

        // Make sure that the MediaPlayer is not pointing to another data source
        mediaPlayer!!.reset()

        mediaPlayer!!.setAudioAttributes(audioAttributes())

        try {
            mediaPlayer!!.setDataSource(activeAudio.data) // Set the data source to the file location
        } catch (e: IOException) {
            e.printStackTrace()
            stopSelf()
        }

        mediaPlayer!!.prepareAsync() // Asynchronously prepares to play audio
    } // Initialization of the media player

    private fun playMedia() {
        if (!mediaPlayer!!.isPlaying)
            seekFromTo(activeAudio.starttime, activeAudio.endtime)
    }

    private fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            handler.removeCallbacksAndMessages(null)
            resumePosition = mediaPlayer!!.currentPosition
        }
    }

    private fun resumeMedia() {
        if (!mediaPlayer!!.isPlaying)
            seekFromTo(resumePosition, activeAudio.endtime)
    }

    private fun registerBecomingNoisyReceiver() {
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    } // Registers the becomingNoisyReceiver BroadcastReceiver

    private fun registerPlayNewAudio() {
        registerReceiver(playNewAudio, IntentFilter(NowPlaying.Broadcast_PLAY_NEW_AUDIO))
    } // Registers the playNewAudio BroadcastReceiver

    private fun callStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                    // Pause upon any call
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING ->
                        if (mediaPlayer != null) {
                            pauseMedia()
                            ongoingCall = true
                        }
                    TelephonyManager.CALL_STATE_IDLE -> // Phone idle, start playing.
                        if (mediaPlayer != null && ongoingCall) {
                            ongoingCall = false
                            resumeMedia()
                        }
                }
            }
        }
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    } // Listens for phone calls

    @Throws(RemoteException::class)
    private fun initMediaSession() {
        if (mediaSessionManager != null) return  // mediaSessionManager is already initialized

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        mediaSession = MediaSessionCompat(applicationContext, "AudioPlayer")
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            // Implement callbacks
            override fun onPlay() {
                super.onPlay()
                resumeMedia()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onPause() {
                super.onPause()
                pauseMedia()
                buildNotification(PlaybackStatus.PAUSED)
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                skipToNext()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                skipToPrevious()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                stopSelf()
            }

            override fun onSeekTo(position: Long) {
                super.onSeekTo(position)
                seekFromTo(position.toInt(), activeAudio.endtime)
            }
        })

        transportControls = mediaSession!!.controller.transportControls
    } // Initialization of the media session objects

    private fun skipToNext() {
        if (audioIndex == audioList.size - 1)
            audioIndex = 0
        else
            audioIndex++
        activeAudio = audioList[audioIndex]

        stopMedia()
        mediaPlayer!!.reset()
        initMediaPlayer()
    } // Skips to next song

    private fun skipToPrevious() {
        if (audioIndex == 0)
            audioIndex = audioList.size - 1
        else
            audioIndex--
        activeAudio = audioList[audioIndex]

        stopMedia()
        // reset mediaPlayer
        mediaPlayer!!.reset()
        initMediaPlayer()
    } // Skips to previous song

    private fun buildNotification(playbackStatus: PlaybackStatus) {
        var notificationAction = android.R.drawable.ic_media_pause
        var playPauseAction: PendingIntent? = null

        // Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause
            playPauseAction = playbackAction(1)
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play
            playPauseAction = playbackAction(0)
        }

        val largeIcon : Bitmap = if (activeAudio.cover != "none")
            BitmapFactory.decodeFile(activeAudio.cover)
        else
            BitmapFactory.decodeResource(resources, R.drawable.song_big)

        val contentIntent = PendingIntent.getActivity(applicationContext, 102,
                Intent(applicationContext, NowPlaying::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this)
                .setShowWhen(false)
                .setOngoing(playbackStatus == PlaybackStatus.PLAYING)
                .setStyle(NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession!!.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(R.color.colorPrimary)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.mipmap.ic_foreground)
                .setContentText(activeAudio.artist)
                .setContentTitle(activeAudio.title)
                .setContentInfo(activeAudio.album)
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", playPauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, notificationBuilder.build())
    } // Builds the media player notification

    private fun removeNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    } // Removes the notification


    private fun playbackAction(actionNumber: Int) : PendingIntent? {
        val playbackAction = Intent(this, MediaPlayerService::class.java)
        when (actionNumber) {
            0 -> playbackAction.action = ACTION_PLAY
            1 -> playbackAction.action = ACTION_PAUSE
            2 -> playbackAction.action = ACTION_NEXT
            3 -> playbackAction.action = ACTION_PREVIOUS
            else -> return null
        }
        return PendingIntent.getService(this, actionNumber, playbackAction, 0)
    } // Decide which action to send to the playNewAudio BroadcastReceiver

    private fun handleIncomingActions(playbackAction: Intent?) {
        if (playbackAction == null || playbackAction.action == null) return
        when (playbackAction.action) {
            ACTION_PLAY -> transportControls.play()
            ACTION_PAUSE -> transportControls.pause()
            ACTION_NEXT -> transportControls.skipToNext()
            ACTION_PREVIOUS -> transportControls.skipToPrevious()
            ACTION_STOP -> transportControls.stop()
        }
    }

    private fun seekFromTo(from: Int, to: Int) {
        handler.removeCallbacksAndMessages(null)
        val dur = to - from
        mediaPlayer!!.seekTo(from)
        mediaPlayer!!.start()
        handler.postDelayed({
            skipToNext()
            buildNotification(PlaybackStatus.PLAYING)
        }, dur.toLong())
    } // Responsible for dealing correctly with Image+Cue style files

    // Attributes that the media player will use
    private fun audioAttributes() = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

    /* This request is only needed from API 26 and above for
       when the media player requests audio focus */
    @TargetApi(26)
    private fun audioFocusRequest() = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes())
            .build()
}
