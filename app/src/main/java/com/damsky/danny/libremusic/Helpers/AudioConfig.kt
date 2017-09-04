/*
HI
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

class AudioConfig(private val context: Context, val songList: ArrayList<Song>, val albumList: ArrayList<Album>, val artistList: ArrayList<Artist>) {
    val position = intArrayOf(0, 0, 0)
    lateinit var listLevel : ListLevel

    fun getAdapter() = when (listLevel) {
        ListLevel.ARTISTS ->
            ArtistAdapter(context, artistList)
        ListLevel.ALBUMS ->
            AlbumAdapter(context, albumList)
        ListLevel.SONGS ->
            SongAdapter(context, songList)
        ListLevel.ALBUM_SONGS ->
            SongAdapter(context, ArrayList(albumList[position[0]].songs))
        ListLevel.ARTIST_SONGS ->
            SongAdapter(context, ArrayList(artistList[position[0]].albums[position[1]].songs))
        ListLevel.ARTIST_ALBUMS ->
            AlbumAdapter(context, ArrayList(artistList[position[0]].albums))
    }

    fun isUsable() = try {
        songList.isNotEmpty()
    } catch (e: Exception) {
        false
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun goBack() = when (listLevel) {
        ListLevel.ALBUM_SONGS -> {
            listLevel = ListLevel.ALBUMS
            AlbumAdapter(context, albumList)
        }
        ListLevel.ARTIST_SONGS -> {
            listLevel = ListLevel.ARTIST_ALBUMS
            AlbumAdapter(context, ArrayList(artistList[position[0]].albums))
        }
        ListLevel.ARTIST_ALBUMS -> {
            listLevel = ListLevel.ARTISTS
            ArtistAdapter(context, artistList)
        }
        else -> null
    }
}
