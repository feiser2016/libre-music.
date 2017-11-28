package com.damsky.danny.libremusic.data.db

/**
 * This enum class is used to handle associations between RecyclerView and its adapter.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

enum class ListLevel(val index: Int) {
    ARTISTS(0),            // Artists: First level
    ARTIST_ALBUMS(1),      // Artists: Second level, Artists -> Albums
    ARTIST_SONGS(2),       // Artists: Third level,  Artists -> Albums -> Songs
    ALBUMS(3),             // Albums:  First level
    ALBUM_SONGS(4),        // Albums:  Second level, Albums -> Songs
    SONGS(5),              // Songs:   First level
    GENRES(6),             // Genres: First level
    GENRE_SONGS(7),        // Genres: Second level, Genres -> Songs
    PLAYLISTS(8),          // Playlists: First level
    PLAYLIST_SONGS(9),     // Playlists: Second level, Playlists -> Songs
    QUEUE(10)              // Queue: First level (Songs)
}
