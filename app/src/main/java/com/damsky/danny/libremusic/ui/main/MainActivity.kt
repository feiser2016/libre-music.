package com.damsky.danny.libremusic.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import com.damsky.danny.dannydamskyutils.Display
import com.damsky.danny.libremusic.App
import com.damsky.danny.libremusic.R
import com.damsky.danny.libremusic.data.db.ListLevel
import com.damsky.danny.libremusic.data.db.model.Song
import com.damsky.danny.libremusic.data.models.*
import com.damsky.danny.libremusic.service.MediaPlayerService
import com.damsky.danny.libremusic.ui.about.AboutActivity
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.getTime
import com.damsky.danny.libremusic.ui.main.MainPresenter.Companion.glideLoad
import com.damsky.danny.libremusic.ui.main.adapters.RecycleAdapter
import com.damsky.danny.libremusic.ui.main.listeners.CustomOnClickListener
import com.damsky.danny.libremusic.ui.main.listeners.OnSwipeTouchListener
import com.damsky.danny.libremusic.ui.prefs.PreferencesActivity
import com.damsky.danny.libremusic.utils.Constants
import com.damsky.danny.libremusic.utils.LibrarySearcher
import com.mancj.slideup.SlideUp
import com.mancj.slideup.SlideUpBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nowplaying_main.*
import kotlinx.android.synthetic.main.songinfo_main.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File

/**
 * This activity contains the music library and the music player UI.
 *
 * @author Danny Damsky
 * @since 2018-02-25
 */
class MainActivity : AppCompatActivity(), View.OnClickListener, CustomOnClickListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener, SearchView.OnQueryTextListener,
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
    private lateinit var currentView: SlideUp
    private lateinit var nowPlayingView: SlideUp
    private lateinit var navigationUp: SlideUp
    private lateinit var songNavImage: ImageView
    private lateinit var addPlaylist: MenuItem
    private val searcher = LibrarySearcher()

    val handler = Handler()
    private val run: Runnable = object : Runnable {
        override fun run() {
            setupPlayerUi(appReference.appDbHelper.getSong())
            handler.postDelayed(this, Constants.UI_UPDATE_INTERVAL_MILLIS)
        }
    }

    private lateinit var appReference: App
    private lateinit var display: Display

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appReference = application as App
        display = Display(this, R.mipmap.ic_launcher)

        val pair: Pair<Int, Int> = appReference.preferencesHelper.getThemeAndDayNightModeNoActionBar()
        setTheme(pair.first)
        AppCompatDelegate.setDefaultNightMode(pair.second)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        songNavImage = navigationDrawer.getHeaderView(0).findViewById(R.id.songImageNav)

        mToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(mToggle)
        mToggle.syncState()

        supportActionBar!!.setDefaultDisplayHomeAsUpEnabled(true)

        superToolbar.navigationIcon = getDrawable(R.drawable.back)

        currentView = SlideUpBuilder(slideView).withGesturesEnabled(false).build()

        nowPlayingView = SlideUpBuilder(superView).withListeners(object : SlideUp.Listener.Events {
            override fun onSlide(percent: Float) {
                superView.alpha = 1 - (percent / 100)
            }

            override fun onVisibilityChanged(visibility: Int) {
                if (visibility == View.GONE) {
                    fab.show()
                    currentView.show()
                } else {
                    fab.hide()
                    currentView.hide()
                }
            }
        }).withTouchableAreaDp(1000.toFloat()).build()

        navigationUp = SlideUpBuilder(navigation).withGesturesEnabled(false).withStartState(SlideUp.State.SHOWED).build()

        myList.layoutManager = LinearLayoutManager(this)
        myList.itemAnimator = DefaultItemAnimator()

        superToolbar.setNavigationOnClickListener({ nowPlayingView.hide() })
        navigationDrawer.setNavigationItemSelectedListener(this)

        if (!appReference.appDbHelper.songsEmpty()) {
            fab.setOnClickListener(this)
            slideView.setOnClickListener(this)
            navigation.setOnNavigationItemSelectedListener(this)
            seekBar.setOnSeekBarChangeListener(this)
            myList.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
                override fun onSwipeRight() {
                    if (navigationUp.isVisible) {
                        val selected = this@MainActivity.navigation.selectedItemId
                        this@MainActivity.navigation.selectedItemId = when (selected) {
                            R.id.navigation_artists ->
                                R.id.navigation_playlists

                            R.id.navigation_songs ->
                                R.id.navigation_albums

                            R.id.navigation_genres ->
                                R.id.navigation_songs

                            R.id.navigation_playlists ->
                                R.id.navigation_genres

                            else ->
                                R.id.navigation_artists
                        }
                    }
                }

                override fun onSwipeLeft() {
                    if (navigationUp.isVisible) {
                        val selected = this@MainActivity.navigation.selectedItemId
                        this@MainActivity.navigation.selectedItemId = when (selected) {
                            R.id.navigation_artists ->
                                R.id.navigation_albums

                            R.id.navigation_albums ->
                                R.id.navigation_songs

                            R.id.navigation_songs ->
                                R.id.navigation_genres

                            R.id.navigation_genres ->
                                R.id.navigation_playlists

                            else ->
                                R.id.navigation_artists
                        }
                    }
                }
            })

            initializeUi()
        }
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    override fun onRestart() {
        super.onRestart()
        if (!appReference.appDbHelper.songsEmpty()) {
            setupPlayerUi(appReference.appDbHelper.getSong())
            handler.postDelayed(run, Constants.UI_UPDATE_INTERVAL_MILLIS)
        }
    }

    override fun onClick(view: View) {
        when (view) {
            slideView ->
                nowPlayingView.show()

            fab ->
                playOrPause()
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
                        navigationUp.isVisible -> navigation.selectedItemId = navigation.selectedItemId
                        else -> myList.adapter = getAdapter(appReference.appDbHelper.getQueueModel())
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
            display.showEditTextDialog(R.string.action_add_playlist, R.string.action_add_playlist_hint, editText,
                    {
                        if (editText.text.isEmpty())
                            display.showSnackShort(R.string.text_empty)
                        else {
                            appReference.appDbHelper.insertPlaylist("${editText.text}")
                            appReference.appDbHelper.setPlaylists()
                            myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
                        }
                    })
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) ->
                drawerLayout.closeDrawer(GravityCompat.START)

            nowPlayingView.isVisible ->
                nowPlayingView.hide()

            else -> when (appReference.appDbHelper.getLevel()) {
                ListLevel.ARTISTS,
                ListLevel.ALBUMS,
                ListLevel.SONGS,
                ListLevel.GENRES,
                ListLevel.PLAYLISTS ->
                    customFinish()

                ListLevel.QUEUE ->
                    drawerLayout.openDrawer(Gravity.START)

                ListLevel.ARTIST_ALBUMS ->
                    myList.adapter = getAdapter(appReference.appDbHelper.getArtistModel())

                ListLevel.ARTIST_SONGS ->
                    myList.adapter = getAdapter(appReference.appDbHelper.getArtistAlbumsModel(appReference.appDbHelper.getArtistIndex()))

                ListLevel.ALBUM_SONGS ->
                    myList.adapter = getAdapter(appReference.appDbHelper.getAlbumModel())

                ListLevel.GENRE_SONGS ->
                    myList.adapter = getAdapter(appReference.appDbHelper.getGenreModel())

                ListLevel.PLAYLIST_SONGS ->
                    myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_artists -> {
                myList.adapter = getAdapter(appReference.appDbHelper.getArtistModel())
                addPlaylist.isVisible = false
            }

            R.id.navigation_albums -> {
                myList.adapter = getAdapter(appReference.appDbHelper.getAlbumModel())
                addPlaylist.isVisible = false
            }

            R.id.navigation_songs -> {
                myList.adapter = getAdapter(appReference.appDbHelper.getSongModel())
                addPlaylist.isVisible = false
            }

            R.id.navigation_genres -> {
                myList.adapter = getAdapter(appReference.appDbHelper.getGenreModel())
                addPlaylist.isVisible = false
            }

            R.id.navigation_playlists -> {
                myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
                addPlaylist.isVisible = true
            }

            R.id.action_library -> {
                navigation.selectedItemId = R.id.navigation_artists
                navigationUp.show()
                drawerLayout.closeDrawers()
            }

            R.id.action_queue -> {
                myList.adapter = getAdapter(appReference.appDbHelper.getQueueModel())
                navigationUp.hide()
                drawerLayout.closeDrawers()
                appReference.appDbHelper.setLevel(ListLevel.QUEUE)
            }

            R.id.action_settings -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
                drawerLayout.closeDrawers()
            }

            R.id.action_reset -> {
                display.showBasicDialog(R.string.reset_library,
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
                if (appReference.sleepTime == "") {
                    val sleepTimer = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
                        appReference.onSleepTimerEnabled(hours, minutes)
                    }, 0, 0, true)
                    sleepTimer.setTitle(R.string.action_sleep_message)
                    sleepTimer.show()
                } else
                    display.showBasicDialog("${getString(R.string.action_sleep_timer)} - ${appReference.sleepTime}",
                            R.string.action_sleep_disable,
                            { appReference.onSleepTimerDisabled() })
                drawerLayout.closeDrawers()
            }
        }
        return true
    }

    override fun onRecyclerClick(position: Int) {
        when (appReference.appDbHelper.getLevel()) {
            ListLevel.ARTIST_ALBUMS ->
                myList.adapter = getAdapter(appReference.appDbHelper.getArtistSongsModel(position))

            ListLevel.ARTISTS ->
                myList.adapter = getAdapter(appReference.appDbHelper.getArtistAlbumsModel(searcher.getPosition(position)))

            ListLevel.ALBUMS ->
                myList.adapter = getAdapter(appReference.appDbHelper.getAlbumSongsModel(searcher.getPosition(position)))

            ListLevel.GENRES ->
                myList.adapter = getAdapter(appReference.appDbHelper.getGenreSongsModel(searcher.getPosition(position)))

            ListLevel.PLAYLISTS ->
                myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistSongsModel(searcher.getPosition(position)))

            ListLevel.SONGS ->
                songPress(appReference.appDbHelper.getSong(searcher.getPosition(position)))

            ListLevel.ARTIST_SONGS ->
                songPress(appReference.appDbHelper.getArtistSong(position))

            ListLevel.ALBUM_SONGS ->
                songPress(appReference.appDbHelper.getAlbumSong(position))

            ListLevel.GENRE_SONGS ->
                songPress(appReference.appDbHelper.getGenreSong(position))

            ListLevel.PLAYLIST_SONGS ->
                songPress(appReference.appDbHelper.getPlaylistSong(position))

            ListLevel.QUEUE -> {
                val posInt = searcher.getPosition(position)
                val pos = appReference.appDbHelper.getQueueSong(posInt)
                playAudio()
                setupPlayerUi(pos)
                appReference.preferencesHelper.updateIndex(posInt)
            }
        }
    }

    override fun onContextMenuClick(songsList: Array<Song>, action: MenuAction, index: Int) {
        when (action) {
            MenuAction.ACTION_PLAY ->
                playNewList(songsList)

            MenuAction.ACTION_ADD_TO_QUEUE ->
                appReference.appDbHelper.addToQueue(songsList)

            MenuAction.ACTION_ADD_TO_PLAYLIST ->
                setSongsToPlaylist(songsList)

            MenuAction.ACTION_SHARE ->
                shareFiles(songsList)

            MenuAction.ACTION_SET_AS_RINGTONE ->
                setAsRingtone(songsList[0])

            MenuAction.ACTION_RENAME_PLAYLIST ->
                renamePlaylist(index)

            MenuAction.ACTION_REMOVE_PLAYLIST ->
                removePlaylist(index)

            MenuAction.ACTION_REMOVE_FROM_PLAYLIST ->
                removeFromPlaylist(songsList[0])
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        var listLevel: ListLevel? = null
        if (navigationUp.isVisible) {
            when (navigation.selectedItemId) {
                R.id.navigation_artists ->
                    searcher.setDataSource(ArtistModel(appReference.appDbHelper.getArtists()))

                R.id.navigation_albums ->
                    searcher.setDataSource(AlbumModel(appReference.appDbHelper.getAlbums()))

                R.id.navigation_songs -> {
                    searcher.setDataSource(SongModel(appReference.appDbHelper.getSongs()))
                    listLevel = ListLevel.SONGS
                }

                R.id.navigation_genres ->
                    searcher.setDataSource(GenreModel(appReference.appDbHelper.getGenres()))

                R.id.navigation_playlists ->
                    searcher.setDataSource(PlaylistModel(appReference.appDbHelper.getPlaylists()))
            }
        } else {
            searcher.setDataSource(SongModel(appReference.appDbHelper.getQueue()))
            listLevel = ListLevel.QUEUE
        }
        searcher.update(newText)
        myList.adapter = getAdapter(Pair(searcher.search(), listLevel))
        return true
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        MediaPlayerService.mediaPlayer?.let {
            if (p2)
                MediaPlayerService.transportControls.seekTo(p0!!.progress.toLong() +
                        appReference.appDbHelper.getSong().startTime)
        }
    }

    fun playPrevious(view: View) {
        playNextOrPrevious(
                { appReference.appDbHelper.decrementAudioIndex() },
                { MediaPlayerService.transportControls.skipToPrevious() }
        )
    }

    fun playNext(view: View) {
        playNextOrPrevious(
                { appReference.appDbHelper.incrementAudioIndex() },
                { MediaPlayerService.transportControls.skipToNext() }
        )
    }

    fun playPause(view: View) {
        playOrPause()
    }

    fun onShuffle(view: View) {
        val bool = appReference.preferencesHelper.getShufflePreference()
        appReference.preferencesHelper.setShufflePreference(!bool)
        if (!bool)
            shuffle.setImageResource(R.drawable.shuffle_on)
        else
            shuffle.setImageResource(R.drawable.shuffle)
    }

    fun onRepeat(view: View) {
        val bool = appReference.preferencesHelper.getRepeatPreference()
        appReference.preferencesHelper.setRepeatPreference(!bool)
        if (!bool)
            repeat.setImageResource(R.drawable.repeat_one)
        else
            repeat.setImageResource(R.drawable.repeat_all)

    }

    private fun songPress(song: Song) {
        playAudio()
        setupPlayerUi(song)
        appReference.updateIndexes()
    }

    private fun customFinish() {
        val returnIntent = intent
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

    fun getAdapter(pair: Pair<TypeModel, ListLevel?>): RecycleAdapter {
        return RecycleAdapter(this, pair.first, pair.second)
    }

    /**
     * @param songsList List of songs to add to a playlist
     *
     * @if              More than one playlist exists
     * @return          An AlertDialog with options to choose which playlists to add the songs to.
     * @else
     * @return          An AlertDialog with an editText asking the user to create a new playlist to add the songs to.
     */
    private fun setSongsToPlaylist(songsList: Array<Song>) {
        val playList = appReference.appDbHelper.getPlaylistsClean()
        if (playList.isNotEmpty()) {
            val playSize = playList.size
            val itemList = Array(playSize, { i -> playList[i].playList })
            val boolList = BooleanArray(playSize, { _ -> false })

            display.showMultiChoiceDialog(R.string.add_to_playlist, itemList, boolList, {
                (0 until playSize).filter { boolList[it] }.forEach {
                    appReference.appDbHelper.insertSongsToPlaylist(itemList[it], songsList)
                }
                display.showSnackShort(R.string.success)
            })
        } else {
            val editText = EditText(this)
            display.showEditTextDialog(R.string.add_to_playlist, R.string.action_add_playlist_hint, editText, {
                if (editText.text.isEmpty())
                    display.showSnackShort(R.string.text_empty)
                else {
                    appReference.appDbHelper.insertPlaylist("${editText.text}")
                    appReference.appDbHelper.setPlaylists()
                    appReference.appDbHelper.insertSongsToPlaylist(editText.text.toString(), songsList)
                    display.showSnackShort(R.string.success)
                }
            })
        }
    }

    private fun shareFiles(songsList: Array<Song>) {
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

    private fun shareFile(fileToSend: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_STREAM, fileToSend)
        startActivity(Intent.createChooser(intent, resources.getString(R.string.share_song)))
    }

    private fun setRingtone(song: Song) = launch {
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
                display.showToastShort(R.string.ringtone_success)
            }
        }.await()
    }

    /**
     * Ringtone will be set once permissions are set to allow modifying system settings.
     *
     *  @param song Song to set as ringtone.
     */
    @SuppressLint("InlinedApi")
    private fun setAsRingtone(song: Song) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(applicationContext)) {
            display.showToastLong(R.string.permission_modify_settings)
            startActivityForResult(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")), Constants.REQUEST_WRITE_SETTINGS)
        } else
            setRingtone(song)
    }

    private fun playAudio() {
        if (!appReference.serviceBound) {
            val playerIntent = Intent(applicationContext, MediaPlayerService::class.java)
            startService(playerIntent)
            bindService(playerIntent, appReference.serviceConnection, Context.BIND_AUTO_CREATE)
        } else
            sendBroadcast(Intent(Constants.ACTION_PLAY_AUDIO))
    }

    private fun initializeUi() = launch {
        async {
            val indexes = appReference.preferencesHelper.getIndexes()
            val song: Song
            if (indexes != null) {
                song = when (indexes.second) {
                    ListLevel.ARTIST_SONGS ->
                        appReference.appDbHelper.getArtistSong(appReference.appDbHelper.getAudioIndex())

                    ListLevel.ALBUM_SONGS ->
                        appReference.appDbHelper.getAlbumSong(appReference.appDbHelper.getAudioIndex())

                    ListLevel.SONGS ->
                        appReference.appDbHelper.getSong(appReference.appDbHelper.getAudioIndex())

                    ListLevel.GENRE_SONGS ->
                        appReference.appDbHelper.getGenreSong(appReference.appDbHelper.getAudioIndex())

                    ListLevel.PLAYLIST_SONGS ->
                        appReference.appDbHelper.getPlaylistSong(appReference.appDbHelper.getAudioIndex())

                    else ->
                        appReference.appDbHelper.getSong(0)
                }
            } else
                song = appReference.appDbHelper.getSong(0)

            setupPlayerUi(song)
            val repeatVal = if (appReference.preferencesHelper.getRepeatPreference())
                R.drawable.repeat_one
            else
                R.drawable.repeat_all

            val shuffleVal = if (appReference.preferencesHelper.getShufflePreference())
                R.drawable.shuffle_on
            else
                R.drawable.shuffle
            runOnUiThread {
                repeat.setImageResource(repeatVal)
                shuffle.setImageResource(shuffleVal)
                handler.postDelayed(run, Constants.UI_UPDATE_INTERVAL_MILLIS)
            }
        }.await()
    }

    private fun setupPlayerUi(song: Song) = launch {
        async {
            val playVal = try {
                if (MediaPlayerService.mediaPlayer!!.isPlaying)
                    R.drawable.pause
                else
                    R.drawable.play
            } catch (e: Exception) {
                R.drawable.play
            }

            val position = try {
                MediaPlayerService.mediaPlayer!!.currentPosition
            } catch (e: Exception) {
                song.startTime
            }

            val seekBarMax = song.endTime - song.startTime
            val seekBarProgress = position - song.startTime
            val durationInTime = "${(position - song.startTime).getTime()} / ${song.duration.getTime()}"
            val artistNameString = "${song.artist} - ${song.album}"
            val infoString = "${appReference.appDbHelper.getAudioIndex() + 1} / ${appReference.appDbHelper.getQueue().size}"

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
                slideView_cover.glideLoad(this@MainActivity, song.cover)
                songNavImage.glideLoad(this@MainActivity, song.cover)
                coverArt.glideLoad(this@MainActivity, song.cover)
            }
        }.await()
    }

    private fun setImages(resourceId: Int) {
        playPause.setImageResource(resourceId)
        fab.setImageResource(resourceId)
    }

    private fun playOrPause() {
        when {
            MediaPlayerService.mediaPlayer == null -> {
                playAudio()
                setImages(R.drawable.pause)
            }

            MediaPlayerService.mediaPlayer!!.isPlaying -> {
                MediaPlayerService.transportControls.pause()
                setImages(R.drawable.play)
            }

            else -> {
                MediaPlayerService.transportControls.play()
                setImages(R.drawable.pause)
            }
        }
    }

    private fun playNextOrPrevious(incrementOrDecrement: () -> Unit, playNextOrPrevious: () -> Unit) {
        if (MediaPlayerService.mediaPlayer == null) {
            incrementOrDecrement()
            setImages(R.drawable.pause)
            playAudio()
        } else {
            if (!MediaPlayerService.mediaPlayer!!.isPlaying)
                setImages(R.drawable.pause)
            playNextOrPrevious()
        }
    }

    private fun playNewList(audioList: Array<Song>) {
        appReference.appDbHelper.setQueue(audioList)
        playAudio()
    }

    private fun renamePlaylist(index: Int) {
        val editText = EditText(this)
        display.showEditTextDialog(R.string.action_add_playlist, R.string.action_add_playlist_hint, editText,
                {
                    if (editText.text.isEmpty())
                        display.showSnackShort(R.string.text_empty)
                    else {
                        appReference.appDbHelper.updatePlaylist(index, editText.text.toString())
                        myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
                    }
                })
    }

    private fun removePlaylist(index: Int) {
        appReference.appDbHelper.deletePlaylist(index)
        myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistModel())
    }

    private fun removeFromPlaylist(song: Song) {
        appReference.appDbHelper.deleteSongFromPlaylist(song)
        myList.adapter = getAdapter(appReference.appDbHelper.getPlaylistSongsModel(appReference.appDbHelper.getSecondIndex()))
    }

    // Useless method, part of SearchView listener
    override fun onQueryTextSubmit(query: String?): Boolean = false

    // Useless methods, part of SeekBar listener
    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

    override fun onStopTrackingTouch(p0: SeekBar?) = Unit
}
