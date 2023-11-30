package com.ts.sdk.media.constants;

/**
 * MusicConstants.
 */
public interface MusicConstants {

    // Player status, -1: Destroyed 1: Stop 2: Pause 3: Play 4: Prepare 5: Fail
    int PLAYER_STATUS_DEFAULT = -2;
    int PLAYER_STATUS_DESTROY = -1;
    int PLAYER_STATUS_IDLE = 0;
    int PLAYER_STATUS_STOP = 1;
    int PLAYER_STATUS_PAUSE = 2;
    int PLAYER_STATUS_START = 3;
    int PLAYER_STATUS_PREPARED = 4;
    int PLAYER_STATUS_ERROR = 5;

    /**
     * Identification of data source processed inside the player.
     */
    // Network
    int CHANNEL_NET = 0;
    // Local
    int CHANNEL_LOCATION = 1;

    /**
     * ErrorCode.
     */
    int BT_STATE_DISCONNECTED = 0;
    int BT_STATE_CONNECTED = 2;
    int ERROR_CODE = -1;
    String PLAYER_INTERNAL_ERROR = "6";

    /**
     * Playback mode.
     */
    // List loop mode
    int MUSIC_MODE_LOOP = 0;
    // Single mode
    int MUSIC_MODE_SINGLE = 1;
    // Order play
    int MUSIC_MODE_ORDER = 2;
    // Random play
    int MUSIC_MODE_RANDOM = 3;

    String BT_MUSIC_NAME = "BT";
    String USB_MUSIC_NAME = "USB";
    String ONLINE_MUSIC_NAME = "ONLINE";
    String USB_VIDEO_NAME = "VIDEO";

    /**
     * Media Source Code.
     */
    int CP_MEDIA_SOURCE_CODE = 50;
    int AA_MEDIA_SOURCE_CODE = 70;
    int USB_VIDEO_SOURCE_CODE = 18;
}
