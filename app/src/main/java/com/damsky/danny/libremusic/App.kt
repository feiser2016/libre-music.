package com.damsky.danny.libremusic

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import com.damsky.danny.libremusic.data.db.AppDbHelper
import com.damsky.danny.libremusic.data.db.model.DaoMaster
import com.damsky.danny.libremusic.data.prefs.AppPreferencesHelper
import com.damsky.danny.libremusic.service.MediaPlayerService
import com.damsky.danny.libremusic.utils.Constants
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The main Application class, it holds the application's database and preferences.
 * In addition, the MediaPlayerService is bound to it.
 *
 * @author Danny Damsky
 * @since 2018-02-01
 */

class App : Application() {
    lateinit var appDbHelper: AppDbHelper // access to class with all DB functions
    lateinit var preferencesHelper: AppPreferencesHelper // access to preferences functions

    lateinit var player: MediaPlayerService
    var serviceBound = false
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.LocalBinder
            player = binder.service
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    } // Binds MediaPlayerService to the application

    private val handler = Handler()
    var sleepTime = ""

    override fun onCreate() {
        super.onCreate()
        appDbHelper = AppDbHelper(this)

        preferencesHelper = AppPreferencesHelper(this)

        val pair = preferencesHelper.getIndexes()
        pair?.let { appDbHelper.updateLocations(pair.first, pair.second) }

        appDbHelper.setSongs()
    }

    /**
     * Enables sleep timer that will stop after the given hours/minutes
     *
     * @param hours   The amount of hours until playback stops
     * @param minutes The amount of minutes until playback stops
     */
    fun onSleepTimerEnabled(hours: Int, minutes: Int) {
        val delayMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong())

        handler.postDelayed({
            MediaPlayerService.mediaPlayer?.let {
                if (MediaPlayerService.mediaPlayer!!.isPlaying)
                    MediaPlayerService.transportControls.pause()
            }
            onSleepTimerDisabled()
        }, delayMillis)

        sleepTime = getRealTimePlusDuration(hours, minutes)

        Toast.makeText(this,
                "${getString(R.string.action_sleep_toast)} $sleepTime",
                Toast.LENGTH_LONG).show()
    }

    fun onSleepTimerDisabled() {
        handler.removeCallbacksAndMessages(null)
        sleepTime = ""
        Toast.makeText(this, R.string.sleep_timer_disabled, Toast.LENGTH_SHORT).show()
    }

    fun updateIndexes() {
        preferencesHelper.updateIndexes(appDbHelper.getPlayableLevel(), appDbHelper.getPositions())
    }

    /**
     * @param hoursToAdd   Hours to add to real time
     * @param minutesToAdd Minutes to add to real time
     *
     * @return Real time with hours/minutes added to it, will look like this HH:MM (The next day)
     */
    private fun getRealTimePlusDuration(hoursToAdd: Int, minutesToAdd: Int): String {
        val realTime = Calendar.getInstance()
        val realHours = realTime.get(Calendar.HOUR_OF_DAY)
        realTime.add(Calendar.HOUR_OF_DAY, hoursToAdd)
        realTime.add(Calendar.MINUTE, minutesToAdd)

        val hours = realTime.get(Calendar.HOUR_OF_DAY)
        val minutes = realTime.get(Calendar.MINUTE)

        val timeFormat = StringBuilder()
        if (hours < 10)
            timeFormat.append(0)
        timeFormat.append(hours).append(":")
        if (minutes < 10)
            timeFormat.append(0)
        timeFormat.append(minutes)

        if (hoursToAdd == realHours || hours < realHours)
            timeFormat.append(" ").append(getString(R.string.action_sleep_time_extension))

        return timeFormat.toString()
    }
}
