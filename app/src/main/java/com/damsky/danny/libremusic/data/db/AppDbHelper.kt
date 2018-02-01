package com.damsky.danny.libremusic.data.db

import android.content.Context
import com.damsky.danny.libremusic.data.db.model.*
import com.damsky.danny.libremusic.data.models.*
import com.damsky.danny.libremusic.utils.Constants

/**
 * This class is used to handle database operations.
 *
 * @param context Required to gain access to the application's database file. (Recommended: ApplicationContext)
 *
 * @author Danny Damsky
 * @since 2018-02-01
 */

class AppDbHelper(private val context: Context) {
    private lateinit var artistList: Array<Artist>
    private lateinit var albumList: Array<Album>
    private lateinit var songList: Array<Song>
    private lateinit var genreList: Array<Genre>
    private lateinit var playList: Array<Playlist>
    private var listLevel = ListLevel.ARTISTS
    private var playableLevel = ListLevel.SONGS
    private var positions = intArrayOf(-1, -1, -1)

    private lateinit var songQueue: Array<Song>

    private fun getDaoSession(): DaoSession {
        return DaoMaster(DaoMaster.DevOpenHelper(context, Constants.DB_NAME).writableDb).newSession()
    }

    /**
     * Adds new artist to the database if the artist doesn't already exist.
     *
     * @param artist String containing an artist's name.
     */
    fun insertArtist(artist: String) {
        if (getDaoSession().artistDao.queryBuilder().where(ArtistDao.Properties.Artist.eq(artist)).unique() == null)
            getDaoSession().artistDao.insert(Artist(null, artist))
    }

    /**
     * Adds new Album to the database if the album doesn't already exist.
     *
     * @param album  A string containing the album's title.
     * @param artist A string containing the name of the album's artist.
     * @param year   The year when the album was created.
     * @param cover  A string containing the path to the album art.
     */
    fun insertAlbum(album: String, artist: String, year: Int, cover: String) {
        if (getDaoSession().albumDao.queryBuilder().where(AlbumDao.Properties.Album.eq(album), AlbumDao.Properties.Artist.eq(artist)).unique() == null)
            getDaoSession().albumDao.insert(Album(null, album, artist, year, cover))
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
    fun insertSong(data: String, title: String, album: String, artist: String, genre: String, track: Int, year: Int, startTime: Int, endTime: Int, duration: Int, cover: String) {
        getDaoSession().songDao.insert(Song(null, data, title, album, artist, genre, track, year, startTime, endTime, duration, cover))
    }

    /**
     * Adds a new genre to the database if the genre doesn't already exist
     *
     * @param genre A string containing the name of the genre.
     */
    fun insertGenre(genre: String) {
        if (getDaoSession().genreDao.queryBuilder().where(GenreDao.Properties.Genre.eq(genre)).unique() == null)
            getDaoSession().genreDao.insert(Genre(null, genre))
    }

    /**
     * Adds a new playlist to the database if the playlist doesn't already exist
     *
     * @param playlist A string containing the name of the playlist.
     */
    fun insertPlaylist(playlist: String) {
        if (lookForPlaylist(playlist) == null)
            getDaoSession().playlistDao.insert(Playlist(null, playlist))
    }

    /**
     * @param playlist The name of the playlist to add songs to.
     * @param songs    An array of songs to add to the playlist.
     */
    fun insertSongsToPlaylist(playlist: String, songs: Array<Song>) {
        val playList = lookForPlaylist(playlist)
        for (i in songs)
            insertSongToPlaylist(playList, i)
    }

    private fun insertSongToPlaylist(playlist: Playlist?, song: Song) {
        if (playlist != null) {
            if (getDaoSession().linkDao.queryBuilder().where(LinkDao.Properties.PlayListId.eq(playlist.id),
                            LinkDao.Properties.SongId.eq(song.id)).unique() == null) {
                getDaoSession().linkDao.insert(Link(null, playlist.id, song.id))
                playlist.resetSongs()
            }
        }
    }

    private fun lookForPlaylist(playlist: String): Playlist? =
            getDaoSession().playlistDao.queryBuilder().where(PlaylistDao.Properties.PlayList.eq(playlist)).unique()

    /**
     * Deletes everything from the database.
     */
    fun deleteAll() {
        getDaoSession().playlistDao.deleteAll()
        getDaoSession().genreDao.deleteAll()
        getDaoSession().songDao.deleteAll()
        getDaoSession().albumDao.deleteAll()
        getDaoSession().artistDao.deleteAll()
        getDaoSession().linkDao.deleteAll()

        setSongs()
    }


    /**
     * Deletes a single song from the database.
     *
     * @param song     The song to delete
     * @param playlist The playlist to delete the song from.
     */
    fun deleteSongFromPlaylist(song: Song, playlist: Playlist) {
        val link = getDaoSession().linkDao.queryBuilder().where(LinkDao.Properties.SongId.eq(song.id),
                LinkDao.Properties.PlayListId.eq(playlist.id)).unique()
        if (link != null) {
            getDaoSession().linkDao.delete(link)
            playlist.resetSongs()
            setPlaylists()
        }
    }

    /**
     * Same as above function but this function assumes that the playlist currently
     * being viewed by the user.
     */
    fun deleteSongFromPlaylist(song: Song) {
        deleteSongFromPlaylist(song, getPlaylistsClean()[getSecondIndex()])
    }

    /**
     * @param index The array index of the playlist to delete
     */
    fun deletePlaylist(index: Int) {
        getDaoSession().playlistDao.delete(playList[index])
        setPlaylists()
    }

    /**
     * @param index The index of the playlist to be updated in the array.
     * @param newName  The name that the playlist should be updated with to.
     */
    fun updatePlaylist(index: Int, newName: String) {
        val playlist = playList[index]
        playlist.playList = newName
        getDaoSession().playlistDao.update(playlist)
        setPlaylists()
    }

    /**
     * The following functions query the database for a sorted list and cache it into an array.
     */

    fun setArtists() {
        artistList = getDaoSession().artistDao
                .queryBuilder()
                .orderAsc(ArtistDao.Properties.Artist)
                .build().list().toTypedArray()
    }

    fun setAlbums() {
        albumList = getDaoSession().albumDao
                .queryBuilder()
                .orderAsc(AlbumDao.Properties.Artist,
                        AlbumDao.Properties.Year,
                        AlbumDao.Properties.Album)
                .build().list().toTypedArray()
    }

    /**
     * The queue is set to the songList.
     */
    fun setSongs() {
        songList = getDaoSession().songDao
                .queryBuilder()
                .orderAsc(SongDao.Properties.Artist,
                        SongDao.Properties.Year,
                        SongDao.Properties.Track,
                        SongDao.Properties.Album)
                .build().list().toTypedArray()

        songQueue = songList
    }

    fun setGenres() {
        genreList = getDaoSession().genreDao
                .queryBuilder()
                .orderAsc(GenreDao.Properties.Genre)
                .build().list().toTypedArray()
    }

    fun setPlaylists() {
        playList = getDaoSession().playlistDao
                .queryBuilder()
                .orderAsc(PlaylistDao.Properties.PlayList)
                .build().list().toTypedArray()
    }

    /**
     * The following functions set the proper ListLevel and return the requested array.
     */

    fun getArtists(): Array<Artist> {
        listLevel = ListLevel.ARTISTS
        return artistList
    }

    fun getAlbums(): Array<Album> {
        listLevel = ListLevel.ALBUMS
        return albumList
    }

    fun getSongs(): Array<Song> {
        listLevel = ListLevel.SONGS
        return songList
    }

    fun getQueue() = songQueue

    fun getGenres(): Array<Genre> {
        listLevel = ListLevel.GENRES
        return genreList
    }

    fun getPlaylists(): Array<Playlist> {
        listLevel = ListLevel.PLAYLISTS
        return playList
    }

    fun getPlaylistsClean() = playList

    fun getArtistAlbums(position: Int): Array<Album> {
        positions[0] = position
        listLevel = ListLevel.ARTIST_ALBUMS
        return artistList[position].albums.toTypedArray()
    }

    fun getArtistSongs(position: Int): Array<Song> {
        positions[1] = position
        listLevel = ListLevel.ARTIST_SONGS
        return artistList[positions[0]].albums[position].songs.toTypedArray()
    }

    fun getAlbumSongs(position: Int): Array<Song> {
        positions[1] = position
        listLevel = ListLevel.ALBUM_SONGS
        return albumList[position].songs.toTypedArray()
    }

    fun getGenreSongs(position: Int): Array<Song> {
        positions[1] = position
        listLevel = ListLevel.GENRE_SONGS
        return genreList[position].songs.toTypedArray()
    }

    fun getPlaylistSongs(position: Int): Array<Song> {
        positions[1] = position
        listLevel = ListLevel.PLAYLIST_SONGS
        return playList[position].songs.toTypedArray()
    }

    fun getSong(): Song = songQueue[positions[2]]

    fun getArtistSong(position: Int): Song {
        playableLevel = ListLevel.ARTIST_SONGS
        positions[2] = position
        songQueue = artistList[positions[0]].albums[positions[1]].songs.toTypedArray()
        return songQueue[position]
    }

    fun getAlbumSong(position: Int): Song {
        playableLevel = ListLevel.ALBUM_SONGS
        positions[2] = position
        songQueue = albumList[positions[1]].songs.toTypedArray()
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
        songQueue = genreList[positions[1]].songs.toTypedArray()
        return songQueue[position]
    }

    fun getPlaylistSong(position: Int): Song {
        playableLevel = ListLevel.PLAYLIST_SONGS
        positions[2] = position
        songQueue = playList[positions[1]].songs.toTypedArray()
        return songQueue[position]
    }

    fun getQueueSong(position: Int): Song {
        positions[2] = position
        return songQueue[position]
    }

    fun getArtistIndex() = positions[0]
    fun getSecondIndex() = positions[1]
    fun getAudioIndex() = positions[2]

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

    fun addToQueue(list: Array<Song>) {
        val arr = ArrayList<Song>(list.size + songQueue.size)
        arr.addAll(songQueue)
        arr.addAll(list)
        songQueue = arr.toTypedArray()
    }

    fun setQueue(list: Array<Song>) {
        songQueue = list
        positions[2] = 0
    }

    fun getLevel(): ListLevel {
        return listLevel
    }

    fun getPlayableLevel(): ListLevel {
        return playableLevel
    }

    fun getPositions(): IntArray {
        return positions
    }

    fun songsEmpty(): Boolean {
        return songList.isEmpty()
    }

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

    fun getArtistModel(): Pair<TypeModel, ListLevel?> {
        return Pair(ArtistModel(getArtists()), null)
    }

    fun getAlbumModel(): Pair<TypeModel, ListLevel?> {
        return Pair(AlbumModel(getAlbums()), null)
    }

    fun getSongModel(): Pair<TypeModel, ListLevel?> {
        return Pair(SongModel(getSongs()), getLevel())
    }

    fun getGenreModel(): Pair<TypeModel, ListLevel?> {
        return Pair(GenreModel(getGenres()), null)
    }

    fun getPlaylistModel(): Pair<TypeModel, ListLevel?> {
        return Pair(PlaylistModel(getPlaylists()), null)
    }

    fun getArtistAlbumsModel(position: Int): Pair<TypeModel, ListLevel?> {
        return Pair(AlbumModel(getArtistAlbums(position)), null)
    }

    fun getArtistSongsModel(position: Int): Pair<TypeModel, ListLevel?> {
        return Pair(SongModel(getArtistSongs(position)), getLevel())
    }

    fun getAlbumSongsModel(position: Int): Pair<TypeModel, ListLevel?> {
        return Pair(SongModel(getAlbumSongs(position)), getLevel())
    }

    fun getGenreSongsModel(position: Int): Pair<TypeModel, ListLevel?> {
        return Pair(SongModel(getGenreSongs(position)), getLevel())
    }

    fun getPlaylistSongsModel(position: Int): Pair<TypeModel, ListLevel?> {
        return Pair(SongModel(getPlaylistSongs(position)), getLevel())
    }

    fun getQueueModel(): Pair<TypeModel, ListLevel?> {
        return Pair(SongModel(getQueue()), ListLevel.QUEUE)
    }
}
