package com.ts.sdk.media.constants;

public class ServiceConstants {
    /**
     * service action.
     */
    public static final String SERVICE_MUSIC_PLAYER_ACTION = "com.ts.service.media.MUSIC_PLAYER_ACTION";

    public static final String SERVICE_BT_MUSIC_PLAYER_ACTION = "com.ts.service.media.BT_MUSIC_ACTION";

    public static final String SERVICE_ONLINE_MUSIC_PLAYER_ACTION = "com.ts.service.media.ONLINE_MUSIC_ACTION";

    public static final String SERVICE_USB_SCAN_ACTION = "com.ts.service.media.USB_SCAN_ACTION";

    public static final String SERVICE_PLAY_STATUS_ACTION = "com.ts.service.media.PLAY_STATUS_ACTION";

    /**
     * Service package/class name.
     */
    public static final String SERVICE_PACKAGE = "com.ts.service";
    public static final String SERVICE_CLASS_NAME = "com.ts.service.media.MediaService";

    public static final String CP_AA_SERVICE_PKG_NAME = "com.allgo.remoteui.mediabrowserservice";
    public static final String CP_AA_SERVICE_CLS_NAME = "com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserService";

    public static final int MEDIA_SCANNER_STARTED = 1;
    public static final int MEDIA_SCANNER_FINISHED = 2;
    public static final int DEATH_RECIPIENT_FLAGS = 0;
}
