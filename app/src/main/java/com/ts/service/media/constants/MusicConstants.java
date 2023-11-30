package com.ts.service.media.constants;

import java.io.File;

/**
 * MusicConstants.
 */
public interface MusicConstants {
    /**
     * Play mode configuration.
     */
    // Single mode
    String SP_VALUE_MUSIC_MODE_SINGLE = "sp_value_music_mode_single";
    // List loop mode
    String SP_VALUE_MUSIC_MODE_LOOP = "sp_value_music_mode_loop";
    // Order play
    String SP_VALUE_MUSIC_MODE_ORDER = "sp_value_music_mode_order";
    // Random play
    String SP_VALUE_MUSIC_MODE_RANDOM = "sp_value_music_mode_random";
    // ParamsKey
    String KEY_PARAMS_MODE = "key_params_music";
    // Around Package
    String AROUND_PACKAGE = "com.ts.hmi.aroundview";

    /**
     * Various states inside the player.
     */
    // Ended or not started
    int MUSIC_PLAYER_STOP = 0;
    // In prepare
    int MUSIC_PLAYER_PREPARE = 1;
    // Buffer
    int MUSIC_PLAYER_BUFFER = 2;
    // In play
    int MUSIC_PLAYER_PLAYING = 3;
    // Pause
    int MUSIC_PLAYER_PAUSE = 4;
    // Error
    int MUSIC_PLAYER_ERROR = 5;
    // Idle
    int MUSIC_PLAYER_IDLE = 6;
    // Init
    int MUSIC_PLAYER_INIT = 7;
    // Preparing
    int MUSIC_PLAYER_PREPARING = 8;
    // End
    int MUSIC_PLAYER_END = 9;

    // Player status, -1: Destroyed 0: Stop 1: Pause 2: Play 3: Prepare 4: Fail
    int PLAYER_STATUS_DEFAULT = -2;
    int PLAYER_STATUS_DESTROY = -1;
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

    /**
     * Time format.
     * Hour
     * Minute
     * Second
     * Millisecond
     */
    int FORMATTER_HOUR = 24;
    int FORMATTER_MINUTE = 60;
    int FORMATTER_SECOND = 1000;
    int FORMATTER_MILLISECOND = 3600;
    int CURRENT_DURATION = 500;
    int DELAY_DURATION = 200;
    int MAX_PROGRESS = 100;

    class ErrorCode {
        public static final String UNKNOWN_ERROR = "1";
        // Received error (s) app must re instantiate new mediaplayer
        public static final String PLAYER_INTERNAL_ERROR = "2";
        // Stream start position error
        public static final String MEDIA_STREAMING_ERROR = "3";
        // IO,timeout error
        public static final String NETWORK_CONNECTION_TIMEOUT = "4";
        // 403
        public static final String PLAY_REQUEST_FAILED = "5";
    }

    int FAST_FORWARD_OFFSET = 10000;
    String FILE_SIGN = File.separator;
    int UUID_INDEX = 2;
    String SP_MUSIC_USB_KEY = "sp_music_usb_key_";
    String USB_1_PORT = "usb1";
    String USB_2_PORT = "usb3";
    int PORT_1 = 1;
    int PORT_2 = 3;
    int PATH_MIN_LENTH = 3;
    int MEDIA_SCANNER_START = 1;
    int MEDIA_SCANNER_FINISHED = 2;
    int DELAY_HIDDEN_TIME = 2500;
    String MEDIA_PLAY_ERROR = "6";
    String USB_FIRST_UUID = "USB_FIRST_UUID";
    String USB_SECOND_UUID = "USB_SECOND_UUID";
    int VEHICLE_STATUS_POWER = 6;
    int REQUEST_DELAY = 2;
    int KEYCODE_VOLUME_MAX = 32;
    int QUERY_MUSIC_LOCAL = 2;
    int QUERY_MUSIC_CLIENT = 3;
    int AUDIO_SCAN_UPDATE = 4;

    // Car power mode
    int POWER_STAND_BY = 2;
    int POWER_RUN = 3;
    int POWER_TEMP_RUN = 6;
    int POWER_VOLTAGE = 8;

    // Online music status
    int ONLINE_PLAYER_IDLE = 0;
    int ONLINE_PLAYER_PREPARING = 1;
    int ONLINE_PLAYER_PLAYING = 2;
    int ONLINE_PLAYER_PAUSED = 3;
    int MUSIC_PLAY_RUN_TASK = 100;

    // Music genre regex
    String REGEX_CLASSIC = "(?i).*classic.*";
    String REGEX_POP = "(?i).*pop.*";
    String REGEX_VOCALS = "(?i).*vocals.*";
    String REGEX_JAZZ = "(?i).*jazz.*";
    String REGEX_ROCK = "(?i).*rock.*";
    String REGEX_CUSTOM = "(?i).*custom.*";

    // Eq band
    int SETTING_PRESET_EQ_SMART = 1;
    int SETTING_PRESET_EQ_CLASSIC = 2;
    int SETTING_PRESET_EQ_POPS = 3;
    int SETTING_PRESET_EQ_VOCAL = 4;
    int SETTING_PRESET_EQ_JAZZ = 5;
    int SETTING_PRESET_EQ_ROCK = 6;
    int SETTING_PRESET_EQ_USER = 7;
}
