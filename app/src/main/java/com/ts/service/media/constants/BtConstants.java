package com.ts.service.media.constants;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;

/**
 * Defines constants used in Bluetooth SDK.
 */
public interface BtConstants {

    /**
     * The Bluetooth adapter state is off.
     */
    public static final int ADAPTER_STATE_OFF = BluetoothAdapter.STATE_OFF;

    /**
     * The Bluetooth adapter state is turning on.
     */
    public static final int ADAPTER_STATE_TURNING_ON = BluetoothAdapter.STATE_TURNING_ON;

    /**
     * The Bluetooth adapter state is on.
     */
    public static final int ADAPTER_STATE_ON = BluetoothAdapter.STATE_ON;

    /**
     * The Bluetooth adapter state is turning off.
     */
    public static final int ADAPTER_STATE_TURNING_OFF = BluetoothAdapter.STATE_TURNING_OFF;

    /**
     * connection state disconnected.
     */
    public static final int CONNECTION_STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;

    /**
     * connection state connecting.
     */
    public static final int CONNECTION_STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;

    /**
     * connection state connection.
     */
    public static final int CONNECTION_STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    /**
     * connection state disconnecting.
     */
    public static final int CONNECTION_STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    /**
     * A2DP Sink Profile.
     */
    public static final int A2DP_SINK = 11;

    /**
     * AVRCP CONTRLLER Profile.
     */
    public static final int AVRCP_CONTROLLER = 12;

    /**
     * Constant to specify A2DP profile.
     */
    public static final int PROFILE_A2DP = 3;

    /**
     * Constant to specify AVRCP profile.
     */
    public static final int PROFILE_AVRCP = 4;

    /**
     * The bond type that a passkey is displayed on the local device. The same passkey should also
     * be displayed on the remote device for confirmation.
     */
    public static final int BOND_TYPE_PASSKEY_CONFIRMATION = 1;

    /**
     * Invalide handle of request.
     */
    public static final int INVALID_HANDLE = -1;

    /**
     * Constant to indicate REPEAT_MODE_NONE.
     */
    public static final int REPEAT_MODE_NONE = 1;

    /**
     * Constant to indicate the repeat mode of repeating all tracks.
     */
    public static final int REPEAT_MODE_ALL_TRACK = 2;

    /**
     * Constant to indicate the repeat mode of repeating one track.
     */
    public static final int REPEAT_MODE_ONE_TRACK = 3;

    /**
     * Constant to indicate the play state of stopped.
     */
    public static final int PLAY_STATE_STOPPED = 1;

    /**
     * Constant to indicate the play state of paused.
     */
    public static final int PLAY_STATE_PAUSED = 2;

    /**
     * Constant to indicate the play state of playing.
     */
    public static final int PLAY_STATE_PLAYING = 3;

    /**
     * Only represent number zero string.
     */
    public static final String STR_NUMBER_ZERO = "0";

    /**
     * Only represent number zero.
     */
    public static final int INT_NUMBER_ZERO = 0;

    /**
     * Bluetooth music play state key for sp.
     */
    public static final String BT_MUSIC_PLAY_STATE = "play_state";

    /**
     * Bluetooth device addr key for sp.
     */
    public static final String BT_CONNECTED_DEVICE_ADDR = "bt_connected_device_addr";

    /**
     * Bluetooth device default addr value for sp.
     */
    public static final String DEFAULT_BT_ADDR = "00:00:00:00:00:00";

    /**
     * Bluetooth play list media id.
     */
    public static final String BT_PLAY_LIST_ROOT = "__ROOT__";
    public static final String BT_PLAY_LIST_PLAYING = "PLAYER1";
    public static final String BT_PLAY_LIST_NOW_PLAYING = "NOW_PLAYING:1";

    /**
     * Bluetooth music default name.
     */
    public static final String BT_MUSIC_DEFAULT_NAME = "";

    public static final String ALLGO_CONTAINER_PACKAGE_NAME = "com.allgo.rui";
    public static final String ALLGO_CONTAINER_PACKAGE_URI = "com.allgo.rui.RemoteUIService";

    public static final int MAX_PROGRESS = 1000;

    // custom action to send cmd.
    public static final String CUSTOM_ACTION_SEND_PASS_THRU_CMD = "com.android.bluetooth.a2dpsink.mbs.CUSTOM_ACTION_SEND_PASS_THRU_CMD";
    // key to set fastforword or rewind.
    public static final String KEY_CMD = "cmd";
    // state of pressing or releasing.
    public static final String KEY_STATE = "state";
    // rewind command.
    public static final int PASS_THRU_CMD_ID_REWIND = 0x48;
    // fast forward command.
    public static final int PASS_THRU_CMD_ID_FF = 0x49;
    // pressed state.
    public static final int KEY_STATE_PRESSED = 0;
    // released state.
    public static final int KEY_STATE_RELEASED = 1;

    // key of hardKey type.
    String HARD_KEY_TYPE = "hard_key_type";
    // key of hardKey action.
    String HARD_KEY_ACTION = "hard_key_action";
}
