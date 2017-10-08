/*
This activity opens up immediately after the Splash activity
finishes loading all the song information
Information is loaded into songList (for songs), albumList (for albums) and artistList (for artists)
This activity shows the music library to the user.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.AdapterView
import com.damsky.danny.libremusic.Adapters.SongAdapter
import com.damsky.danny.libremusic.DB.DaoMaster
import com.damsky.danny.libremusic.Enum.ListLevel
import com.damsky.danny.libremusic.Helpers.AudioConfig
import com.damsky.danny.libremusic.Helpers.SongSearcher
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.Services.MediaPlayerService
import kotlinx.android.synthetic.main.activity_libre_player.*

class LibrePlayer : AppCompatActivity(), AdapterView.OnItemClickListener, BottomNavigationView.OnNavigationItemSelectedListener, SearchView.OnQueryTextListener {
    /*
    Kotlin Extensions:
    my_music = ListView
    navigation = BottomNavigationView
     */
    private var destroy = false
    private val REQUEST_CODE_PREFERENCES = 321
    private lateinit var searcher: SongSearcher

    companion object {
        lateinit var audioConfig : AudioConfig
        var pitch_black = R.style.AppTheme
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        evaluateTheme()
        setContentView(R.layout.activity_libre_player)
        audioConfig.context = this

        if (audioConfig.isUsable) {
            navigation.setOnNavigationItemSelectedListener(this)
            searcher = SongSearcher(audioConfig.songList)
            navigation.selectedItemId = R.id.navigation_artists
        }

        my_music.onItemClickListener = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (destroy) {
            val daoSession = DaoMaster(DaoMaster.DevOpenHelper(this, "song-db").writableDb).newSession()
            daoSession.albumDao.deleteAll()
            daoSession.artistDao.deleteAll()
            daoSession.songDao.deleteAll()
        }

        try {
            NowPlaying.player.stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        val adapter = audioConfig.goBack()
        if (adapter == null)
            finish()
        else
            my_music.adapter = adapter
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        my_music.adapter = when (item.itemId) {
            R.id.navigation_artists -> audioConfig.getArtistAdapter()
            R.id.navigation_albums -> audioConfig.getAlbumAdapter()
            R.id.navigation_songs -> audioConfig.getSongAdapter()
            else -> null
        }
        searcher.Searching = false
        return true
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (audioConfig.listLevel) {
            ListLevel.ARTISTS -> my_music.adapter = audioConfig.getArtistAlbumAdapter(position)
            ListLevel.ALBUMS -> my_music.adapter = audioConfig.getAlbumSongAdapter(position)
            ListLevel.ARTIST_ALBUMS -> my_music.adapter = audioConfig.getArtistSongAdapter(position)
            ListLevel.SONGS -> {
                MediaPlayerService.audioList = audioConfig.songList
                val nowPlaying = Intent(this, NowPlaying::class.java)
                if (searcher.Searching)
                    nowPlaying.putExtra("position", searcher.getPosition(position))
                else
                    nowPlaying.putExtra("position", position)
                startActivity(nowPlaying)
            }
            ListLevel.ALBUM_SONGS -> {
                MediaPlayerService.audioList = ArrayList(audioConfig.albumList[audioConfig.getArtistPosition()].songs)
                val nowPlaying = Intent(this, NowPlaying::class.java)
                nowPlaying.putExtra("position", position)
                startActivity(nowPlaying)
            }
            ListLevel.ARTIST_SONGS -> {
                MediaPlayerService.audioList = ArrayList(audioConfig.artistList[audioConfig.getArtistPosition()].albums[audioConfig.getAlbumPosition()].songs)
                val nowPlaying = Intent(this, NowPlaying::class.java)
                nowPlaying.putExtra("position", position)
                startActivity(nowPlaying)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.libre_player_menu, menu)

        if (audioConfig.isUsable) {
            // Associate searchable configuration with the SearchView
            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchView = menu.findItem(R.id.search).actionView as SearchView
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchView.setOnQueryTextListener(this)

            val mSearchEditFrame = searchView.findViewById<View>(android.support.v7.appcompat.R.id.search_edit_frame)
            mSearchEditFrame.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                internal var oldVisibility = -1
                override fun onGlobalLayout() {
                    val currentVisibility = mSearchEditFrame.visibility
                    if (currentVisibility != oldVisibility) {
                        if (currentVisibility == View.VISIBLE)
                            searcher.Searching = true
                        else
                            navigation.selectedItemId = R.id.navigation_artists
                        oldVisibility = currentVisibility
                    }
                }
            })
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(p0: String) = false

    override fun onQueryTextChange(p0: String): Boolean {
        searcher.Update(p0)
        val search = searcher.Search()
        my_music.adapter = SongAdapter(this, search)
        audioConfig.listLevel = ListLevel.SONGS
        searcher.Searching = true
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PREFERENCES)
            setTheme(pitch_black)
    }

    fun resetButton(item: MenuItem) {
        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setTitle(R.string.reset_library)
        builder.setMessage(R.string.reset_library_question)
        builder.setPositiveButton(R.string.yes, {dialog, _ ->
            destroy = true
            Snackbar.make(findViewById(android.R.id.content), R.string.reset_library_message, Snackbar.LENGTH_SHORT).show()
            item.isVisible = false
            dialog.dismiss()
        })
        builder.setNegativeButton(R.string.no, {dialog, _ -> dialog.dismiss()})
        builder.create().show()
    }

    fun launchSettingsMenu(item: MenuItem) {
        startActivityForResult(Intent(this, PreferenceSetter::class.java), REQUEST_CODE_PREFERENCES)
    }

    private fun evaluateTheme() {
        val value_array = resources.getStringArray(R.array.app_themes_values)
        when (PreferenceManager.getDefaultSharedPreferences(this).getString("app_theme_preferences", value_array[0])) {
            value_array[1] -> // Night Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            value_array[2] -> { // Black Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                pitch_black = R.style.AppTheme_Black
                setTheme(pitch_black)
            }
        }
    }
}
