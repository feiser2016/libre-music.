package com.damsky.danny.libremusic.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.LayerDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.RemoteException
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.ui.main.MainPresenter
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom

/**
 * Service class containing static variables/functions for use with MediaPlayerService

 * @author Danny Damsky
 * @since 2017-12-04
 */


class MediaPlayerCompanion {
    companion object {
        private const val NOTIFICATION_ID = 101
        private val isOreoPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        var mediaPlayer: MediaPlayer? = null
        lateinit var transportControls: MediaControllerCompat.TransportControls

        @Throws(RemoteException::class)
        fun MediaPlayerService.initMediaSession() {
            mediaSessionManager?.let { return }

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
                    seekFromTo(position.toInt(), appReference.appDbHelper.getSong().endTime)
                }
            })

            transportControls = mediaSession!!.controller.transportControls
        }

        fun MediaPlayerService.initMediaPlayer() {
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
        fun MediaPlayerService.requestAudioFocus(): Boolean {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val result: Int = if (isOreoPlus)
                audioManager.requestAudioFocus(audioFocusRequest())
            else
                audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        @SuppressLint("NewApi")
        fun MediaPlayerService.removeAudioFocus(): Boolean =
                if (isOreoPlus)
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocusRequest(audioFocusRequest())
                else
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this)

        fun MediaPlayerService.getBecomingNoisyReceiver() = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                pauseMedia()
                buildNotification(PlaybackStatus.PAUSED)
            }
        }

        fun MediaPlayerService.getPlayAudioReceiver() = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                stopMedia()
                mediaPlayer!!.reset()
                initMediaPlayer()
                buildNotification(PlaybackStatus.PLAYING)
            }
        }

        fun MediaPlayerService.registerReceivers() {
            registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            registerReceiver(playAudioReceiver, IntentFilter(MainPresenter.Broadcast_PLAY_AUDIO))
        }

        fun MediaPlayerService.callStateListener() {
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

        fun MediaPlayerService.buildNotification(playbackStatus: PlaybackStatus) {
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

            val largeIcon: Bitmap = if (song.cover != "none")
                BitmapFactory.decodeFile(song.cover)
            else
                findBitmapById(R.drawable.song_big)

            createChannel()
            val notificationBuilder = NotificationCompat.Builder(this, "media_player_id")
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
                    .addAction(R.drawable.prev, "previous", playbackAction(PlaybackAction.ACTION_PREVIOUS))
                    .addAction(notificationAction, "pause", playPauseAction)
                    .addAction(R.drawable.next, "next", playbackAction(PlaybackAction.ACTION_NEXT))
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, notificationBuilder.build())
        }

        fun MediaPlayerService.removeNotification() {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(NOTIFICATION_ID)
        }

        private fun MediaPlayerService.playbackAction(action: PlaybackAction): PendingIntent {
            val playbackAction = Intent(this, MediaPlayerService::class.java)
            playbackAction.action = action.id
            return PendingIntent.getService(this, action.index, playbackAction, 0)
        }

        fun handleIncomingActions(playbackAction: Intent?) {
            if (playbackAction == null || playbackAction.action == null) return
            when (playbackAction.action) {
                PlaybackAction.ACTION_PLAY.id -> transportControls.play()
                PlaybackAction.ACTION_PAUSE.id -> transportControls.pause()
                PlaybackAction.ACTION_NEXT.id -> transportControls.skipToNext()
                PlaybackAction.ACTION_PREVIOUS.id -> transportControls.skipToPrevious()
                PlaybackAction.ACTION_STOP.id -> transportControls.stop()
            }
        }

        private fun MediaPlayerService.findBitmapById(resourceId: Int): Bitmap {
            val layerDrawable = getDrawable(resourceId) as LayerDrawable
            val bitmap = Bitmap.createBitmap(layerDrawable.intrinsicWidth,
                    layerDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            layerDrawable.setBounds(0, 0, canvas.width, canvas.height)
            layerDrawable.draw(canvas)
            return bitmap
        }

        // Attributes that the media player will use
        private fun audioAttributes() = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

        private fun MediaPlayerService.uniColor(resourceId: Int) =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    getColor(resourceId)
                else
                    resources.getColor(resourceId)

        @TargetApi(Build.VERSION_CODES.O)
        private fun audioFocusRequest() = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes())
                .build()

        @TargetApi(Build.VERSION_CODES.O)
        fun MediaPlayerService.createChannel() {
            if (isOreoPlus) {
                val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val mChannel = NotificationChannel("media_player_id", "Media Playback", NotificationManager.IMPORTANCE_LOW)
                mChannel.description = "Media playback controls"
                mChannel.setShowBadge(false)
                mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                mNotificationManager.createNotificationChannel(mChannel)
            }
        }

        fun MediaPlayerService.playMedia() {
            if (!mediaPlayer!!.isPlaying) {
                val song = appReference.appDbHelper.getSong()
                seekFromTo(song.startTime, song.endTime)
            }
        }

        fun MediaPlayerService.stopMedia() {
            if (mediaPlayer == null) return
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
                handler.removeCallbacksAndMessages(null)
            }
        }

        fun MediaPlayerService.pauseMedia() {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.pause()
                handler.removeCallbacksAndMessages(null)
                resumePosition = mediaPlayer!!.currentPosition
            }
        }

        fun MediaPlayerService.resumeMedia() {
            if (!mediaPlayer!!.isPlaying)
                seekFromTo(resumePosition, appReference.appDbHelper.getSong().endTime)
        }

        fun MediaPlayerService.skipToNext() = skipToNextOrPrevious {
            appReference.appDbHelper.incrementAudioIndex()
        }


        fun MediaPlayerService.skipToPrevious() = skipToNextOrPrevious {
            appReference.appDbHelper.decrementAudioIndex()
        }

        private fun MediaPlayerService.skipToNextOrPrevious(incrementOrDecrement: () -> Unit) {
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

        fun MediaPlayerService.seekFromTo(from: Int, to: Int) {
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
    }
}
