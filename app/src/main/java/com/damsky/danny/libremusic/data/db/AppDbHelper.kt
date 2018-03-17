package com.damsky.danny.libremusic.data.db

import android.content.Context
import com.damsky.danny.libremusic.data.db.model.*
import com.damsky.danny.libremusic.data.models.*
import com.damsky.danny.libremusic.utils.Constants

/**
 * This class is used to handle database operations.
 *
 * @param context Required to gain access to the application's database file.
 * @note It is recommended to use ApplicationContext.
 *
 * @note
 * Most set functions in this class accept a DaoSession object, this is in order to allow multiple
 * database insertions without the need to re-open the database every time.
 * It is NOT RECOMMENDED to use a database other than the one provided by the getDaoSession() function!
 *
 * @author Danny Damsky
 */

class AppDbHelper(private val context: Context) {
    private lateinit var artistList: ArrayList<Artist>
    private lateinit var albumList: ArrayList<Album>
    private lateinit var songList: ArrayList<Song>
    private lateinit var genreList: ArrayList<Genre>
    private lateinit var playList: ArrayList<Playlist>
    private var listLevel = ListLevel.ARTISTS
    private var playableLevel = ListLevel.SONGS
    private var positions = intArrayOf(-1, -1, -1)

    private lateinit var songQueue: ArrayList<Song>

    fun getDaoSession(): DaoSession =
            DaoMaster(DaoMaster.DevOpenHelper(context, Constants.DB_NAME).writableDb).newSession()

    /**
     * Adds new artist to the database if the artist doesn't already exist.
     *
     * @param artist String containing an artist's name.
     */
    fun insertArtist(artist: String, daoSession: DaoSession = getDaoSession()) {
        val queryObject = daoSession.artistDao.queryBuilder()
                .where(ArtistDao.Properties.Artist.eq(artist))
                .unique()

        if (queryObject == null) {
            val newArtist = Artist(null, artist)
            daoSession.artistDao.insert(newArtist)
        }
    }

    /**
     * Adds new Album to the database if the album doesn't already exist.
     *
     * @param album  A string containing the album's title.
     * @param artist A string containing the name of the album's artist.
     * @param year   The year when the album was created.
     * @param cover  A string containing the path to the album art.
     */
    fun insertAlbum(album: String, artist: String, year: Int, cover: String,
                    daoSession: DaoSession = getDaoSession()) {

        val queryObject = daoSession.albumDao.queryBuilder()
                .where(AlbumDao.Properties.Album.eq(album),
                        AlbumDao.Properties.Artist.eq(artist))
                .unique()

        if (queryObject == null) {
            val newAlbum = Album(null, album, artist, year, cover)
            daoSession.albumDao.insert(newAlbum)
        }
    }

    /**
     * Adds a new song to the database.
     *
     * @param data      A string containing the path to the song file.
     * @param title     A string containing the song's title.
     * @param album     A string containing the title of the song's album.
     * @param artist    A string containing the name of the song's artist.
     * @param genre     A string containing the name of the song's genre.
     * @param track     The track number of the song according to its album.
     * @param year      The year when the song was created.
     * @param startTime The start time of the song.
     * @param endTime   The end time of the song.
     * @param duration  The song's duration
     * @param cover     A string containing the path to the album art.
     */
    fun insertSong(data: String, title: String, album: String, artist: String, genre: String,
                   track: Int, year: Int, startTime: Int, endTime: Int, duration: Int,
                   cover: String, daoSession: DaoSession = getDaoSession()) {

        val newSong = Song(null, data, title, album, artist, genre, track, year, startTime, endTime,
                duration, cover)

        daoSession.songDao.insert(newSong)
    }

    /**
     * Adds a new genre to the database if the genre doesn't already exist
     *
     * @param genre A string containing the name of the genre.
     */
    fun insertGenre(genre: String, daoSession: DaoSession = getDaoSession()) {
        val queryObject = daoSession.genreDao.queryBuilder()
                .where(GenreDao.Properties.Genre.eq(genre))
                .unique()

        if (queryObject == null) {
            val newGenre = Genre(null, genre)
            daoSession.genreDao.insert(newGenre)
        }
    }

    /**
     * Adds a new playlist to the database if the playlist doesn't already exist
     *
     * @param playlist A string containing the name of the playlist.
     */
    fun insertPlaylist(playlist: String, daoSession: DaoSession = getDaoSession()) {
        val queryObject = daoSession.playlistDao.queryBuilder()
                .where(PlaylistDao.Properties.PlayList.eq(playlist))
                .unique()

        if (queryObject == null) {
            val newPlaylist = Playlist(null, playlist)
            daoSession.playlistDao.insert(newPlaylist)
        }
    }

    /**
     * @param playlist The name of the playlist to add songs to.
     * @param songs    An array of songs to add to the playlist.
     */
    fun insertSongsToPlaylist(playlist: String, songs: ArrayList<Song>, daoSession: DaoSession = getDaoSession()) {
        var playList: Playlist? = null

        for (i in this.playList)
            if (i.playList == playlist) {
                playList = i
                break
            }

        if (playList != null)
            for (i in songs)
                insertSongToPlaylist(playList, i, daoSession)
    }

    private fun insertSongToPlaylist(playlist: Playlist, song: Song, daoSession: DaoSession = getDaoSession()) {
        val queryObject = daoSession.linkDao.queryBuilder()
                .where(LinkDao.Properties.PlayListId.eq(playlist.id),
                        LinkDao.Properties.SongId.eq(song.id))
                .unique()

        if (queryObject == null) {
            val newLink = Link(null, playlist.id, song.id)
            daoSession.linkDao.insert(newLink)
            playlist.resetSongs()
        }
    }

    /**
     * Deletes everything from the database.
     */
    fun deleteAll(daoSession: DaoSession = getDaoSession()) {
        daoSession.playlistDao.deleteAll()
        daoSession.genreDao.deleteAll()
        daoSession.songDao.deleteAll()
        daoSession.albumDao.deleteAll()
        daoSession.artistDao.deleteAll()
        daoSession.linkDao.deleteAll()

        setSongs(daoSession)
    }


    /**
     * Deletes a single song from the database.
     *
     * @param song     The song to delete
     * @param playlist The playlist to delete the song from.
     */
    fun deleteSongFromPlaylist(song: Song, playlist: Playlist, daoSession: DaoSession = getDaoSession()) {
        val queryObject = daoSession.linkDao.queryBuilder()
                .where(LinkDao.Properties.SongId.eq(song.id),
                        LinkDao.Properties.PlayListId.eq(playlist.id))
                .unique()

        if (queryObject != null) {
            daoSession.linkDao.delete(queryObject)
            playlist.resetSongs()
        }
    }

    /**
     * Same as above function but this function assumes that the playlist currently
     * being viewed by the user.
     */
    fun deleteSongFromPlaylist(song: Song, daoSession: DaoSession = getDaoSession()) {
        deleteSongFromPlaylist(song, getPlaylistsClean()[getSecondIndex()], daoSession)
    }

    /**
     * @param index The array index of the playlist to delete
     */
    fun deletePlaylist(index: Int, daoSession: DaoSession = getDaoSession()) {
        daoSession.playlistDao.delete(playList[index])
        playList.removeAt(index)
    }

    /**
     * @param index The index of the playlist to be updated in the array.
     * @param newName  The name that the playlist should be updated with to.
     */
    fun updatePlaylist(index: Int, newName: String, daoSession: DaoSession = getDaoSession()) {
        val playlist = playList[index]
        playlist.playList = newName
        daoSession.playlistDao.update(playlist)
    }

    /**
     * The following functions query the database for a sorted list and cache it into an array.
     */

    fun setArtists(daoSession: DaoSession = getDaoSession()) {
        artistList = daoSession.artistDao
                .queryBuilder()
                .orderAsc(ArtistDao.Properties.Artist)
                .build().list() as ArrayList<Artist>
    }

    fun setAlbums(daoSession: DaoSession = getDaoSession()) {
        albumList = daoSession.albumDao
                .queryBuilder()
                .orderAsc(AlbumDao.Properties.Artist,
                        AlbumDao.Properties.Year,
                        AlbumDao.Properties.Album)
                .build().list() as ArrayList<Album>
    }

    /**
     * The queue is set to the songList.
     */
    fun setSongs(daoSession: DaoSession = getDaoSession()) {
        songList = daoSession.songDao
                .queryBuilder()
                .orderAsc(SongDao.Properties.Artist,
                        SongDao.Properties.Year,
                        SongDao.Properties.Track,
                        SongDao.Properties.Album)
                .build().list() as ArrayList<Song>

        songQueue = songList
    }

    fun setGenres(daoSession: DaoSession = getDaoSession()) {
        genreList = daoSession.genreDao
                .queryBuilder()
                .orderAsc(GenreDao.Properties.Genre)
                .build().list() as ArrayList<Genre>
    }

    fun setPlaylists(daoSession: DaoSession = getDaoSession()) {
        playList = daoSession.playlistDao
                .queryBuilder()
                .orderAsc(PlaylistDao.Properties.PlayList)
                .build().list() as ArrayList<Playlist>
    }

    /**
     * The following functions set the proper ListLevel and return the requested array.
     */

    fun getArtists(): ArrayList<Artist> {
        listLevel = ListLevel.ARTISTS
        return artistList
    }

    fun getAlbums(): ArrayList<Album> {
        listLevel = ListLevel.ALBUMS
        return albumList
    }

    fun getSongs(): ArrayList<Song> {
        listLevel = ListLevel.SONGS
        return songList
    }

    fun getQueue(): ArrayList<Song> {
        return songQueue
    }

    fun getGenres(): ArrayList<Genre> {
        listLevel = ListLevel.GENRES
        return genreList
    }

    fun getPlaylists(): ArrayList<Playlist> {
        listLevel = ListLevel.PLAYLISTS
        return playList
    }

    fun getPlaylistsClean(): ArrayList<Playlist> {
        return playList
    }

    fun getArtistAlbums(position: Int): ArrayList<Album> {
        positions[0] = position
        listLevel = ListLevel.ARTIST_ALBUMS
        return artistList[position].albums as ArrayList<Album>
    }

    fun getArtistSongs(position: Int): ArrayList<Song> {
        positions[1] = position
        listLevel = ListLevel.ARTIST_SONGS
        return artistList[positions[0]].albums[position].songs as ArrayList<Song>
    }

    fun getAlbumSongs(position: Int): ArrayList<Song> {
        positions[1] = position
        listLevel = ListLevel.ALBUM_SONGS
        return albumList[position].songs as ArrayList<Song>
    }

    fun getGenreSongs(position: Int): ArrayList<Song> {
        positions[1] = position
        listLevel = ListLevel.GENRE_SONGS
        return genreList[position].songs as ArrayList<Song>
    }

    fun getPlaylistSongs(position: Int): ArrayList<Song> {
        positions[1] = position
        listLevel = ListLevel.PLAYLIST_SONGS
        return playList[position].songs as ArrayList<Song>
    }

    fun getSong(): Song = songQueue[positions[2]]

    fun getArtistSong(position: Int): Song {
        playableLevel = ListLevel.ARTIST_SONGS
        positions[2] = position
        songQueue = artistList[positions[0]].albums[positions[1]].songs as ArrayList<Song>
        return songQueue[position]
    }

    fun getAlbumSong(position: Int): Song {
        playableLevel = ListLevel.ALBUM_SONGS
        positions[2] = position
        songQueue = albumList[positions[1]].songs as ArrayList<Song>
        return songQueue[position]
    }

    fun getSong(position: Int): Song {
        playableLevel = ListLevel.SONGS
        positions[2] = position
        songQueue = songList
        return songQueue[position]
    }

    fun getGenreSong(position: Int): Song {
        playableLevel = ListLevel.GENRE_SONGS
        positions[2] = position
        songQueue = genreList[positions[1]].songs as ArrayList<Song>
        return songQueue[position]
    }

    fun getPlaylistSong(position: Int): Song {
        playableLevel = ListLevel.PLAYLIST_SONGS
        positions[2] = position
        songQueue = playList[positions[1]].songs as ArrayList<Song>
        return songQueue[position]
    }

    fun getQueueSong(position: Int): Song {
        positions[2] = position
        return songQueue[position]
    }

    fun getArtistIndex(): Int = positions[0]

    fun getSecondIndex(): Int = positions[1]

    fun getAudioIndex(): Int = positions[2]

    fun incrementAudioIndex() {
        if (positions[2] == songQueue.size - 1)
            positions[2] = 0
        else
            positions[2]++
    }

    fun decrementAudioIndex() {
        if (positions[2] == 0)
            positions[2] = songQueue.size - 1
        else
            positions[2]--
    }

    fun setAudioIndex(position: Int) {
        positions[2] = position
    }

    fun addToQueue(list: ArrayList<Song>) {
        songQueue.addAll(list)
    }

    fun setQueue(list: ArrayList<Song>) {
        songQueue = list
        positions[2] = 0
    }

    fun getLevel(): ListLevel = listLevel

    fun getPlayableLevel(): ListLevel = playableLevel

    fun getPositions(): IntArray = positions

    fun songsEmpty(): Boolean = songList.isEmpty()

    fun updateLocations(newPositions: IntArray, newListLevel: ListLevel) {
        positions = newPositions
        listLevel = newListLevel
    }

    fun setLevel(level: ListLevel) {
        listLevel = level
    }

    /**
     * The following functions are used for adapter setup.
     */

    fun getArtistModel(): Pair<TypeModel, ListLevel?> = Pair(ArtistModel(getArtists()), null)

    fun getAlbumModel(): Pair<TypeModel, ListLevel?> = Pair(AlbumModel(getAlbums()), null)

    fun getSongModel(): Pair<TypeModel, ListLevel?> = Pair(SongModel(getSongs()), getLevel())

    fun getGenreModel(): Pair<TypeModel, ListLevel?> = Pair(GenreModel(getGenres()), null)

    fun getPlaylistModel(): Pair<TypeModel, ListLevel?> = Pair(PlaylistModel(getPlaylists()), null)

    fun getArtistAlbumsModel(position: Int): Pair<TypeModel, ListLevel?> = Pair(AlbumModel(getArtistAlbums(position)), null)

    fun getArtistSongsModel(position: Int): Pair<TypeModel, ListLevel?> = Pair(SongModel(getArtistSongs(position)), getLevel())

    fun getAlbumSongsModel(position: Int): Pair<TypeModel, ListLevel?> = Pair(SongModel(getAlbumSongs(position)), getLevel())

    fun getGenreSongsModel(position: Int): Pair<TypeModel, ListLevel?> = Pair(SongModel(getGenreSongs(position)), getLevel())

    fun getPlaylistSongsModel(position: Int): Pair<TypeModel, ListLevel?> = Pair(SongModel(getPlaylistSongs(position)), getLevel())

    fun getQueueModel(): Pair<TypeModel, ListLevel?> = Pair(SongModel(getQueue()), ListLevel.QUEUE)

}
