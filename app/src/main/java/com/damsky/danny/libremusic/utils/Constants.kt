package com.damsky.danny.libremusic.utils

import android.os.Build

/**
 * A class to keep all the constant values that are used throughout the applications.
 *
 * @author Danny Damsky
 * @since 2018-01-21
 */
class Constants {
    companion object {
        const val PREFERENCES_NAME = "my_preferences"
        const val PREFERENCE_REPEAT = "repeat_preference"
        const val PREFERENCE_SHUFFLE = "shuffle_preference"
        const val PREFERENCE_FIRST_RUN = "first_run_preference"
        const val PREFERENCE_LIST_LEVEL = "audio_list_level_preference"
        const val PREFERENCE_INDEX_ZERO = "audio_position_zero_preference"
        const val PREFERENCE_INDEX_ONE = "audio_position_one_preference"
        const val PREFERENCE_INDEX_TWO = "audio_position_two_preference"
        const val PREFERENCE_APP_THEME = "app_theme_preferences"
        const val PREFERENCE_ENCODING = "file_encoding"

        const val DEFAULT_ENCODING = "Cp1251"
        const val DEFAULT_LIBRARY_ENTRANCE = "<unknown>"

        const val DB_NAME = "library-db"

        const val REQUEST_START_INTRO = 101
        const val REQUEST_START_MAIN = 102
        const val REQUEST_WRITE_SETTINGS = 200
        const val REQUEST_CODE_PERMISSIONS = 201

        const val ACTION_OPEN_EQUALIZER = "android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"
        const val ACTION_PLAY_AUDIO = "com.damsky.danny.libremusic.PlayAudio"

        const val UI_UPDATE_INTERVAL_MILLIS = 500L

        const val NOTIFICATION_ID = 301
        const val NOTIFICATION_PREVIOUS_BUTTON = "Previous"
        const val NOTIFICATION_PLAY_PAUSE_BUTTON = "Play/Pause"
        const val NOTIFICATION_NEXT_BUTTON = "Next"
        const val NOTIFICATION_CHANNEL_ID = "media_player_id"
        const val NOTIFICATION_CHANNEL_PURPOSE = "Media Playback"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Media playback controls."

        const val MEDIA_SESSION_TAG = "Audio Player"

        const val ALBUM_COVER_NONE = "none"

        val IS_MARSHMALLOW_OR_ABOVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        val IS_OREO_OR_ABOVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
