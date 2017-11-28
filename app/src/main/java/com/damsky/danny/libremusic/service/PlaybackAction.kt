package com.damsky.danny.libremusic.service

/**
 * This enum class is used to handle playback actions performed by the MediaPlayerService class.
 *
 * @author Danny Damsky
 * @since 2017-11-28
 */

enum class PlaybackAction(val id: String, val index: Int) {
    ACTION_PLAY("com.damsky.danny.libremusic.ACTION_PLAY", 0),
    ACTION_PAUSE("com.damsky.danny.libremusic.ACTION_PAUSE", 1),
    ACTION_NEXT("com.damsky.danny.libremusic.ACTION_PREVIOUS", 2),
    ACTION_PREVIOUS("com.damsky.danny.libremusic.ACTION_NEXT", 3),
    ACTION_STOP("com.damsky.danny.libremusic.ACTION_STOP", 4)
}
