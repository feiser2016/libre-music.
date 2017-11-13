/*
Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Helpers

import android.content.Context
import com.damsky.danny.libremusic.Adapters.AlbumAdapter
import com.damsky.danny.libremusic.Adapters.ArtistAdapter
import com.damsky.danny.libremusic.Adapters.SongAdapter
import com.damsky.danny.libremusic.DB.Album
import com.damsky.danny.libremusic.DB.Artist
import com.damsky.danny.libremusic.DB.Song
import com.damsky.danny.libremusic.Enum.ListLevel

class AudioConfig(val songList: ArrayList<Song>, val albumList: ArrayList<Album>, val artistList: ArrayList<Artist>) {
    private val position = intArrayOf(0, 0)
    lateinit var context: Context
    var listLevel = ListLevel.ARTISTS

    val isUsable = try {
        songList.isNotEmpty()
    } catch (e: Exception) {
        false
    }

    fun getArtistAdapter(): ArtistAdapter {
        listLevel = ListLevel.ARTISTS
        return ArtistAdapter(context, artistList)
    }

    fun getArtistAlbumAdapter(pos: Int): AlbumAdapter {
        listLevel = ListLevel.ARTIST_ALBUMS
        setArtistPosition(pos)
        return AlbumAdapter(context, ArrayList(artistList[getArtistPosition()].albums))
    }

    fun getArtistSongAdapter(pos: Int): SongAdapter {
        listLevel = ListLevel.ARTIST_SONGS
        setAlbumPosition(pos)
        return SongAdapter(context, ArrayList(artistList[getArtistPosition()].albums[getAlbumPosition()].songs))
    }

    fun getAlbumAdapter(): AlbumAdapter {
        listLevel = ListLevel.ALBUMS
        return AlbumAdapter(context, albumList)
    }

    fun getAlbumSongAdapter(pos: Int): SongAdapter {
        listLevel = ListLevel.ALBUM_SONGS
        setAlbumPosition(pos)
        return SongAdapter(context, ArrayList(albumList[getAlbumPosition()].songs))
    }

    fun getSongAdapter(): SongAdapter {
        listLevel = ListLevel.SONGS
        return SongAdapter(context, songList)
    }

    private fun setArtistPosition(pos: Int) {
        position[0] = pos
    }

    private fun setAlbumPosition(pos: Int) {
        position[1] = pos
    }

    fun getArtistPosition() = position[0]
    fun getAlbumPosition() = position[1]

    fun goBack() = when (listLevel) {
        ListLevel.ALBUM_SONGS -> getAlbumAdapter()
        ListLevel.ARTIST_SONGS -> getArtistAlbumAdapter(position[0])
        ListLevel.ARTIST_ALBUMS -> getArtistAdapter()
        else -> null
    }
}
