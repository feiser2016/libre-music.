package com.damsky.danny.libremusic.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.buildNotification
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.callStateListener
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.createChannel
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.getBecomingNoisyReceiver
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.getPlayAudioReceiver
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.handleIncomingActions
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.initMediaPlayer
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.initMediaSession
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.mediaPlayer
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.playMedia
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.registerReceivers
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.removeAudioFocus
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.removeNotification
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.requestAudioFocus
import com.damsky.danny.libremusic.service.MediaPlayerCompanion.Companion.stopMedia

/**
 * This class is a service which is in charge of all music playback operations.
 *
 * @author Danny Damsky
 * @since 2017-12-04
 */

class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    private val iBinder = LocalBinder()
    lateinit var audioManager: AudioManager

    val becomingNoisyReceiver = getBecomingNoisyReceiver()
    val playAudioReceiver = getPlayAudioReceiver()

    val handler = Handler()
    var resumePosition = -1

    // Handle incoming phone calls
    var ongoingCall = false
    var phoneStateListener: PhoneStateListener? = null
    lateinit var telephonyManager: TelephonyManager

    // Handle state changes in the media player
    var mediaSessionManager: MediaSessionManager? = null
    var mediaSession: MediaSessionCompat? = null

    lateinit var appReference: App

    override fun onBind(intent: Intent?): IBinder = iBinder

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

    // The class used by the binder
    inner class LocalBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }
}
