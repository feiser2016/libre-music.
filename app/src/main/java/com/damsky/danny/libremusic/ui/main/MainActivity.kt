package com.damsky.danny.libremusic.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.service.MediaPlayerCompanion
import com.damsky.danny.libremusic.ui.about.AboutActivity
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.UI_UPDATE_INTERVAL_MILLIS
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.addSongsToQueue
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getAlbumAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getAlbumSong
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getAlbumSongsAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getArtistAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getArtistAlbumsAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getArtistSong
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getArtistSongsAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getGenreAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getGenreSong
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getGenreSongsAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getPlaylistAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getPlaylistSong
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getPlaylistSongsAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getQueueAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getQueueSong
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getSlideUp
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getSong
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getSongAdapter
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.initializeUi
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.playAudio
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.playNewList
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.playNextOrPrevious
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.playOrPause
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.removeFromPlaylist
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.removePlaylist
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.renamePlaylist
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.setAsRingtone
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.setSongsToPlaylist
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.setupPlayerUi
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.shareFiles
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.showDialog
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.updateIndex
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.updateIndexes
import com.damsky.danny.libremusic.ui.main.adapters.models.*
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener
import com.damsky.danny.libremusic.ui.main.listeners.OnSwipeTouchListener
import com.damsky.danny.libremusic.ui.prefs.PreferencesActivity
import com.damsky.danny.libremusic.utils.LibrarySearcher
import com.mancj.slideup.SlideUp
import com.mancj.slideup.SlideUpBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nowplaying_main.*
import kotlinx.android.synthetic.main.songinfo_main.*

/**
 * This activity contains the music library and the music player UI.
 *
 * @author Danny Damsky
 * @since 2017-12-17
 */
class MainActivity : AppCompatActivity(),
        View.OnClickListener,
        CustomOnClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener,
        SearchView.OnQueryTextListener,
        SeekBar.OnSeekBarChangeListener {


    /******************KOTLIN*EXTENSIONS**********************|
    |*********************************************************|
    |               1. myList: RecyclerView                   |
    |               2. drawerLayout: DrawerLayout             |
    |               3. navigation: BottomNavigationView       |
    |               4. fab: FloatingActionButton              |
    |               5. slideView: RelativeLayout              |
    |               6. superView: LinearLayout                |
    |               7. seekBar: SeekBar                       |
    |*********************************************************|
    |*********************************************************/

    private lateinit var mToggle: ActionBarDrawerToggle
    private lateinit var slideUp: SlideUp
    private lateinit var superUp: SlideUp
    private lateinit var navigationUp: SlideUp
    lateinit var songNavImage: ImageView
    private lateinit var addPlaylist: MenuItem
    private val searcher = LibrarySearcher()

    val handler = Handler()
    val run: Runnable = object : Runnable {
        override fun run() {
            setupPlayerUi((application as App).appDbHelper.getSong())
            handler.postDelayed(this, UI_UPDATE_INTERVAL_MILLIS)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pair: Pair<Boolean, Int> = (application as App).preferencesHelper
                .detectAppTheme(resources.getStringArray(R.array.app_themes_values))

        if (pair.first)
            setTheme(R.style.AppTheme_BlackNoActionBar)
        AppCompatDelegate.setDefaultNightMode(pair.second)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        songNavImage = navigationDrawer.getHeaderView(0).findViewById(R.id.songImageNav)

        mToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(mToggle)
        mToggle.syncState()

        supportActionBar!!.setDefaultDisplayHomeAsUpEnabled(true)

        superToolbar.navigationIcon = getDrawable(R.drawable.back)

        slideUp = SlideUpBuilder(slideView).withGesturesEnabled(false).build()
        superUp = superView.getSlideUp(fab, slideUp)
        navigationUp = SlideUpBuilder(navigation).withGesturesEnabled(false).withStartState(SlideUp.State.SHOWED).build()

        myList.layoutManager = LinearLayoutManager(this)
        myList.itemAnimator = DefaultItemAnimator()

        superToolbar.setNavigationOnClickListener({ toggleView(false) })
        navigationDrawer.setNavigationItemSelectedListener(this)

        if (!(application as App).appDbHelper.songsEmpty()) {
            fab.setOnClickListener(this)
            slideView.setOnClickListener(this)
            navigation.setOnNavigationItemSelectedListener(this)
            seekBar.setOnSeekBarChangeListener(this)
            myList.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
                override fun onSwipeRight() {
                    if (navigationUp.isVisible) {
                        val selected = this@MainActivity.navigation.selectedItemId
                        this@MainActivity.navigation.selectedItemId = when (selected) {
                            R.id.navigation_artists -> R.id.navigation_playlists
                            R.id.navigation_songs -> R.id.navigation_albums
                            R.id.navigation_genres -> R.id.navigation_songs
                            R.id.navigation_playlists -> R.id.navigation_genres
                            else -> R.id.navigation_artists
                        }
                    }
                }

                override fun onSwipeLeft() {
                    if (navigationUp.isVisible) {
                        val selected = this@MainActivity.navigation.selectedItemId
                        this@MainActivity.navigation.selectedItemId = when (selected) {
                            R.id.navigation_artists -> R.id.navigation_albums
                            R.id.navigation_albums -> R.id.navigation_songs
                            R.id.navigation_songs -> R.id.navigation_genres
                            R.id.navigation_genres -> R.id.navigation_playlists
                            else -> R.id.navigation_artists
                        }
                    }
                }
            })

            initializeUi()
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onRestart() {
        super.onRestart()
        setupPlayerUi((application as App).appDbHelper.getSong())
        handler.postDelayed(run, UI_UPDATE_INTERVAL_MILLIS)
    }

    override fun onClick(view: View) {
        when (view) {
            slideView -> toggleView(true)
            fab -> playOrPause()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        addPlaylist = menu.findItem(R.id.addPlaylist)
        navigation.selectedItemId = R.id.navigation_artists

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.itemSearch).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(this)

        val mSearchEditFrame = searchView.findViewById<View>(android.support.v7.appcompat.R.id.search_edit_frame)
        mSearchEditFrame.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            internal var oldVisibility = -1
            override fun onGlobalLayout() {
                val currentVisibility = mSearchEditFrame.visibility
                if (currentVisibility != oldVisibility) {
                    when {
                        currentVisibility == View.VISIBLE -> searcher.isSearching = true
                        navigationUp.isVisible -> navigation.selectedItemId = navigation.selectedItemId
                        else -> myList.adapter = getQueueAdapter()
                    }
                    oldVisibility = currentVisibility
                }
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.addPlaylist) {
            val editText = EditText(this)
            showDialog(editText, R.string.action_add_playlist_hint,
                    R.string.action_add_playlist,
                    {
                        if (editText.text.isEmpty())
                            Toast.makeText(this, R.string.text_empty, Toast.LENGTH_SHORT).show()
                        else {
                            (application as App).appDbHelper.insertPlaylist("${editText.text}")
                            (application as App).appDbHelper.setPlaylists()
                            myList.adapter = getPlaylistAdapter()
                        }
                    })
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() = when {
        drawerLayout.isDrawerOpen(GravityCompat.START) ->
            drawerLayout.closeDrawer(GravityCompat.START)

        superUp.isVisible ->
            toggleView(false)

        else -> when ((application as App).appDbHelper.getLevel()) {
            ListLevel.ARTISTS,
            ListLevel.ALBUMS,
            ListLevel.SONGS,
            ListLevel.GENRES,
            ListLevel.PLAYLISTS ->
                customFinish()

            ListLevel.QUEUE ->
                drawerLayout.openDrawer(Gravity.START)

            ListLevel.ARTIST_ALBUMS ->
                myList.adapter = getArtistAdapter()

            ListLevel.ARTIST_SONGS ->
                myList.adapter = getArtistAlbumsAdapter((application as App).appDbHelper.getArtistIndex())

            ListLevel.ALBUM_SONGS ->
                myList.adapter = getAlbumAdapter()

            ListLevel.GENRE_SONGS ->
                myList.adapter = getGenreAdapter()

            ListLevel.PLAYLIST_SONGS ->
                myList.adapter = getPlaylistAdapter()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_artists -> {
                myList.adapter = getArtistAdapter()
                addPlaylist.isVisible = false
                searcher.isSearching = false
            }

            R.id.navigation_albums -> {
                myList.adapter = getAlbumAdapter()
                addPlaylist.isVisible = false
                searcher.isSearching = false
            }

            R.id.navigation_songs -> {
                myList.adapter = getSongAdapter()
                addPlaylist.isVisible = false
                searcher.isSearching = false
            }

            R.id.navigation_genres -> {
                myList.adapter = getGenreAdapter()
                addPlaylist.isVisible = false
                searcher.isSearching = false
            }

            R.id.navigation_playlists -> {
                myList.adapter = getPlaylistAdapter()
                addPlaylist.isVisible = true
                searcher.isSearching = false
            }

            R.id.action_library -> {
                navigation.selectedItemId = R.id.navigation_artists
                navigationUp.show()
                drawerLayout.closeDrawers()
            }

            R.id.action_queue -> {
                myList.adapter = getQueueAdapter()
                navigationUp.hide()
                drawerLayout.closeDrawers()
                (application as App).appDbHelper.setLevel(ListLevel.QUEUE)
            }

            R.id.action_settings -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
                drawerLayout.closeDrawers()
            }

            R.id.action_reset -> {
                showDialog(R.string.reset_library,
                        R.string.reset_library_question,
                        {
                            handler.removeCallbacksAndMessages(null)
                            val returnIntent = intent
                            setResult(Activity.RESULT_OK, returnIntent)
                            finish()
                        }
                )
                drawerLayout.closeDrawers()
            }

            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                drawerLayout.closeDrawers()
            }

            R.id.action_sleep -> {
                if ((application as App).sleepTime == "") {
                    val sleepTimer = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
                        (application as App).onSleepTimerEnabled(hours, minutes)
                    }, 0, 0, true)
                    sleepTimer.setTitle(R.string.action_sleep_message)
                    sleepTimer.show()
                } else
                    AlertDialog.Builder(this)
                            .setTitle("${getString(R.string.action_sleep_timer)} - ${(application as App).sleepTime}")
                            .setMessage(R.string.action_sleep_disable)
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton(R.string.yes, { dialog, _ ->
                                (application as App).onSleepTimerDisabled()
                                dialog.dismiss()
                            })
                            .setNegativeButton(R.string.no, { dialog, _ -> dialog.dismiss() })
                            .create().show()
                drawerLayout.closeDrawers()
            }
        }
        return true
    }

    override fun onRecyclerClick(position: Int) =
            when ((application as App).appDbHelper.getLevel()) {
                ListLevel.ARTIST_ALBUMS ->
                    myList.adapter = getArtistSongsAdapter(position)

                ListLevel.ARTISTS ->
                    myList.adapter = getArtistAlbumsAdapter(getSearchablePosition(position))

                ListLevel.ALBUMS ->
                    myList.adapter = getAlbumSongsAdapter(getSearchablePosition(position))

                ListLevel.GENRES ->
                    myList.adapter = getGenreSongsAdapter(getSearchablePosition(position))

                ListLevel.PLAYLISTS ->
                    myList.adapter = getPlaylistSongsAdapter(getSearchablePosition(position))

                ListLevel.SONGS ->
                    songPress(getSong(getSearchablePosition(position)))

                ListLevel.ARTIST_SONGS ->
                    songPress(getArtistSong(position))

                ListLevel.ALBUM_SONGS ->
                    songPress(getAlbumSong(position))

                ListLevel.GENRE_SONGS ->
                    songPress(getGenreSong(position))

                ListLevel.PLAYLIST_SONGS ->
                    songPress(getPlaylistSong(position))

                ListLevel.QUEUE -> {
                    val posInt = getSearchablePosition(position)
                    val pos = getQueueSong(posInt)
                    playAudio()
                    setupPlayerUi(pos)
                    updateIndex(posInt)
                }
            }

    override fun onContextMenuClick(songsList: Array<Song>, action: MenuAction, index: Int) =
            when (action) {
                MenuAction.ACTION_PLAY -> playNewList(songsList)
                MenuAction.ACTION_ADD_TO_QUEUE -> addSongsToQueue(songsList)
                MenuAction.ACTION_ADD_TO_PLAYLIST -> setSongsToPlaylist(songsList)
                MenuAction.ACTION_SHARE -> shareFiles(songsList)
                MenuAction.ACTION_SET_AS_RINGTONE -> setAsRingtone(songsList[0])
                MenuAction.ACTION_RENAME_PLAYLIST -> renamePlaylist(index)
                MenuAction.ACTION_REMOVE_PLAYLIST -> removePlaylist(index)
                MenuAction.ACTION_REMOVE_FROM_PLAYLIST -> removeFromPlaylist(songsList[0])
            }

    override fun onQueryTextChange(newText: String): Boolean {
        var listLevel: ListLevel? = null
        if (navigationUp.isVisible) {
            when (navigation.selectedItemId) {
                R.id.navigation_artists ->
                    searcher.setDataSource(ArtistModel((application as App).appDbHelper.getArtists()))
                R.id.navigation_albums ->
                    searcher.setDataSource(AlbumModel((application as App).appDbHelper.getAlbums()))
                R.id.navigation_songs -> {
                    searcher.setDataSource(SongModel((application as App).appDbHelper.getSongs()))
                    listLevel = ListLevel.SONGS
                }
                R.id.navigation_genres ->
                    searcher.setDataSource(GenreModel((application as App).appDbHelper.getGenres()))
                R.id.navigation_playlists ->
                    searcher.setDataSource(PlaylistModel((application as App).appDbHelper.getPlaylists()))
            }
        } else {
            searcher.setDataSource(SongModel((application as App).appDbHelper.getQueue()))
            listLevel = ListLevel.QUEUE
        }
        searcher.update(newText)
        myList.adapter = getAdapter(searcher.search(), listLevel)
        searcher.isSearching = true
        return true
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        MediaPlayerCompanion.mediaPlayer?.let {
            if (p2)
                MediaPlayerCompanion.transportControls.seekTo(p0!!.progress.toLong() +
                        (application as App).appDbHelper.getSong().startTime)
        }
    }

    fun playPrevious(view: View) = playNextOrPrevious(
            { (application as App).appDbHelper.decrementAudioIndex() },
            { MediaPlayerCompanion.transportControls.skipToPrevious() }
    )

    fun playNext(view: View) = playNextOrPrevious(
            { (application as App).appDbHelper.incrementAudioIndex() },
            { MediaPlayerCompanion.transportControls.skipToNext() }
    )

    fun playPause(view: View) = playOrPause()

    fun onShuffle(view: View) {
        val bool = (application as App).preferencesHelper.getShufflePreference()
        (application as App).preferencesHelper.setShufflePreference(!bool)
        if (!bool)
            shuffle.setImageResource(R.drawable.shuffle_on)
        else
            shuffle.setImageResource(R.drawable.shuffle)
    }

    fun onRepeat(view: View) {
        val bool = (application as App).preferencesHelper.getRepeatPreference()
        (application as App).preferencesHelper.setRepeatPreference(!bool)
        if (!bool)
            repeat.setImageResource(R.drawable.repeat_one)
        else
            repeat.setImageResource(R.drawable.repeat_all)

    }

    private fun toggleView(bool: Boolean) = if (bool) {
        superUp.show()
        slideUp.hide()
        fab.hide()
    } else {
        superUp.hide()
        slideUp.show()
        fab.show()
    }

    private fun songPress(song: Song) {
        playAudio()
        setupPlayerUi(song)
        updateIndexes()
    }

    private fun getSearchablePosition(position: Int) =
            if (searcher.isSearching)
                searcher.getPosition(position)
            else
                position

    private fun customFinish() {
        val returnIntent = intent
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

    // Useless method, part of SearchView listener
    override fun onQueryTextSubmit(query: String?): Boolean = false

    // Useless methods, part of SeekBar listener
    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

    override fun onStopTrackingTouch(p0: SeekBar?) = Unit
}
