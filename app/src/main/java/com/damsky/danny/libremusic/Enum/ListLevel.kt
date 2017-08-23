/*
This Enum class is used by the ListView in the LibrePlayer activity.
It is required to keep track of where the ListView is pointing.

Year: 2017
Editor: Danny Damsky
 */

package com.damsky.danny.libremusic.Enum

enum class ListLevel {
    ARTISTS,            // Artists: First level
    ARTIST_ALBUMS,      // Artists: Second level, Artists -> Albums
    ARTIST_SONGS,       // Artists: Third level,  Artists -> Albums -> Songs
    ALBUMS,             // Albums:  First level
    ALBUM_SONGS,        // Albums:  Second level, Albums -> Songs
    SONGS               // Songs:   First level
}