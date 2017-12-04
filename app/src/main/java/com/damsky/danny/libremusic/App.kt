package com.damsky.danny.libremusic

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.damsky.danny.libremusic.data.db.AppDbHelper
import com.damsky.danny.libremusic.data.db.model.DaoMaster
import com.damsky.danny.libremusic.data.prefs.AppPreferencesHelper
import com.damsky.danny.libremusic.service.MediaPlayerService

/**
 * The main Application class, it holds the application's database and preferences.
 * In addition, the MediaPlayerService is bound to it.
 *
 * @author Danny Damsky
 * @since 2017-11-28
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

    override fun onCreate() {
        super.onCreate()
        appDbHelper = AppDbHelper(DaoMaster(
                DaoMaster.DevOpenHelper(this, "library-db").writableDb).newSession())

        preferencesHelper = AppPreferencesHelper(this)

        val pair = preferencesHelper.getIndexes()
        pair?.let { appDbHelper.updateLocations(pair.first, pair.second) }

        appDbHelper.setSongs()
    }
}
