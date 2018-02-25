package com.damsky.danny.libremusic.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.utils.Constants
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom

/**
 * This class is a service which is in charge of all music playback operations.
 *
 * @author Danny Damsky
 * @since 2018-02-25
 */

class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        lateinit var transportControls: MediaControllerCompat.TransportControls
    }

    private val iBinder = LocalBinder()
    private lateinit var audioManager: AudioManager

    private val becomingNoisyReceiver = getBecomingNoisyReceiver()
    private val playAudioReceiver = getPlayAudioReceiver()

    private val handler = Handler()
    private var resumePosition = -1

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private lateinit var telephonyManager: TelephonyManager

    // Handle state changes in the media player
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null

    private lateinit var appReference: App

    override fun onBind(intent: Intent?): IBinder {
        return iBinder
    }

    override fun onCreate() {
        super.onCreate()
        appReference = application as App

        callStateListener()
        registerReceivers()
        createChannel()
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaPlayer?.let {
            stopMedia()
            mediaPlayer!!.release()
        }

        removeAudioFocus()

        phoneStateListener?.let {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }

        removeNotification()

        unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(playAudioReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!requestAudioFocus())
            stopSelf()

        if (mediaSessionManager == null) {
            try {
                initMediaSession()
                initMediaPlayer()
            } catch (e: RemoteException) {
                stopSelf()
            }

            buildNotification(PlaybackStatus.PLAYING)
        }

        handleIncomingActions(intent)
        return START_NOT_STICKY
    }

    override fun onCompletion(mp: MediaPlayer?) {
        stopMedia()
        stopSelf()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        playMedia()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaPlayer == null)
                    initMediaPlayer()
                else if (!mediaPlayer!!.isPlaying)
                    mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1f, 1f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer!!.isPlaying)
                    mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (mediaPlayer!!.isPlaying)
                    mediaPlayer!!.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer!!.isPlaying)
                    mediaPlayer!!.setVolume(0.1f, 0.1f)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        removeNotification()
    }

    @Throws(RemoteException::class)
    private fun initMediaSession() {
        mediaSessionManager?.let { return }

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        mediaSession = MediaSessionCompat(applicationContext, Constants.MEDIA_SESSION_TAG)
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
                seekFromTo(position.toInt(), appReference.appDbHelper.getSong().endTime)
            }
        })

        transportControls = mediaSession!!.controller.transportControls
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()

        mediaPlayer!!.setOnCompletionListener(this)
        mediaPlayer!!.setOnPreparedListener(this)

        mediaPlayer!!.reset()

        mediaPlayer!!.setAudioAttributes(audioAttributes())

        try {
            mediaPlayer!!.setDataSource(appReference.appDbHelper.getSong().data)
        } catch (e: IOException) {
            stopSelf()
        }

        mediaPlayer!!.prepareAsync()
    }

    @SuppressLint("NewApi")
    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result: Int = if (Constants.IS_OREO_OR_ABOVE)
            audioManager.requestAudioFocus(audioFocusRequest())
        else
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @SuppressLint("NewApi")
    private fun removeAudioFocus(): Boolean =
            if (Constants.IS_OREO_OR_ABOVE)
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocusRequest(audioFocusRequest())
            else
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this)

    private fun getBecomingNoisyReceiver() = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            pauseMedia()
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private fun getPlayAudioReceiver() = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            stopMedia()
            mediaPlayer!!.reset()
            initMediaPlayer()
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private fun registerReceivers() {
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        registerReceiver(playAudioReceiver, IntentFilter(Constants.ACTION_PLAY_AUDIO))
    }

    private fun callStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                        mediaPlayer?.let {
                            ongoingCall = mediaPlayer!!.isPlaying
                            pauseMedia()
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        mediaPlayer?.let {
                            if (ongoingCall) {
                                ongoingCall = false
                                resumeMedia()
                            }
                        }
                    }
                }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun buildNotification(playbackStatus: PlaybackStatus) {
        val song = appReference.appDbHelper.getSong()

        var notificationAction = R.drawable.pause
        var playPauseAction: PendingIntent? = null

        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = R.drawable.pause
            playPauseAction = playbackAction(PlaybackAction.ACTION_PAUSE)
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.play
            playPauseAction = playbackAction(PlaybackAction.ACTION_PLAY)
        }

        val largeIcon: Bitmap = try {
            BitmapFactory.decodeFile(song.cover)
        } catch (e: IllegalStateException) {
            findBitmapById(R.drawable.song_square)
        }

        createChannel()
        val notificationBuilder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setOngoing(playbackStatus == PlaybackStatus.PLAYING)

                .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession!!.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2))

                .setColor(uniColor(R.color.colorPrimary))
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.mipmap.ic_foreground)
                .setContentText(song.artist)
                .setContentTitle(song.title)
                .setContentInfo(song.album)

                .addAction(R.drawable.prev, Constants.NOTIFICATION_PREVIOUS_BUTTON,
                        playbackAction(PlaybackAction.ACTION_PREVIOUS))

                .addAction(notificationAction, Constants.NOTIFICATION_PLAY_PAUSE_BUTTON, playPauseAction)

                .addAction(R.drawable.next, Constants.NOTIFICATION_NEXT_BUTTON,
                        playbackAction(PlaybackAction.ACTION_NEXT))

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(Constants.NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun removeNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(Constants.NOTIFICATION_ID)
    }

    private fun playbackAction(action: PlaybackAction): PendingIntent {
        val playbackAction = Intent(this, MediaPlayerService::class.java)
        playbackAction.action = action.id
        return PendingIntent.getService(this, action.index, playbackAction, 0)
    }

    private fun handleIncomingActions(playbackAction: Intent?) {
        if (playbackAction != null && playbackAction.action != null) {
            when (playbackAction.action) {
                PlaybackAction.ACTION_PLAY.id -> transportControls.play()
                PlaybackAction.ACTION_PAUSE.id -> transportControls.pause()
                PlaybackAction.ACTION_NEXT.id -> transportControls.skipToNext()
                PlaybackAction.ACTION_PREVIOUS.id -> transportControls.skipToPrevious()
                PlaybackAction.ACTION_STOP.id -> transportControls.stop()
            }
        }
    }

    private fun findBitmapById(resourceId: Int): Bitmap {
        val drawable = getDrawable(resourceId)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // Attributes that the media player will use
    private fun audioAttributes() = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

    @SuppressLint("NewApi")
    private fun uniColor(resourceId: Int) =
            if (Constants.IS_MARSHMALLOW_OR_ABOVE)
                getColor(resourceId)
            else
                resources.getColor(resourceId)

    @TargetApi(Build.VERSION_CODES.O)
    private fun audioFocusRequest() = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes())
            .build()

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (Constants.IS_OREO_OR_ABOVE) {
            val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_PURPOSE, NotificationManager.IMPORTANCE_LOW)
            mChannel.description = Constants.NOTIFICATION_CHANNEL_DESCRIPTION
            mChannel.setShowBadge(false)
            mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun playMedia() {
        if (!mediaPlayer!!.isPlaying) {
            val song = appReference.appDbHelper.getSong()
            seekFromTo(song.startTime, song.endTime)
        }
    }

    fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            handler.removeCallbacksAndMessages(null)
        }
    }

    fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            handler.removeCallbacksAndMessages(null)
            resumePosition = mediaPlayer!!.currentPosition
        }
    }

    fun resumeMedia() {
        if (!mediaPlayer!!.isPlaying)
            seekFromTo(resumePosition, appReference.appDbHelper.getSong().endTime)
    }

    fun skipToNext() = skipToNextOrPrevious {
        appReference.appDbHelper.incrementAudioIndex()
    }


    fun skipToPrevious() = skipToNextOrPrevious {
        appReference.appDbHelper.decrementAudioIndex()
    }

    private fun skipToNextOrPrevious(incrementOrDecrement: () -> Unit) {
        if (!appReference.preferencesHelper.getShufflePreference())
            incrementOrDecrement()
        else
            appReference.appDbHelper.setAudioIndex(ThreadLocalRandom.current()
                    .nextInt(0, appReference.appDbHelper.getQueue().size))

        stopMedia()
        mediaPlayer!!.reset()
        initMediaPlayer()

        appReference.preferencesHelper.updateIndex(
                appReference.appDbHelper.getAudioIndex()
        )
    }

    fun seekFromTo(from: Int, to: Int) {
        handler.removeCallbacksAndMessages(null)
        val duration = to - from
        mediaPlayer!!.seekTo(from)
        mediaPlayer!!.start()
        handler.postDelayed({
            if (appReference.preferencesHelper.getRepeatPreference())
                seekFromTo(appReference.appDbHelper.getSong().startTime,
                        appReference.appDbHelper.getSong().endTime)
            else {
                skipToNext()
                buildNotification(PlaybackStatus.PLAYING)
            }
        }, duration.toLong())
    }

    // The class used by the binder
    inner class LocalBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }
}
