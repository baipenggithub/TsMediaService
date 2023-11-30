package com.ts.service.media.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.service.media.constants.BtConstants;
import com.ts.service.media.model.entity.RecordAudioInfo;

import java.util.List;


/**
 * SharedPreferences Helps.
 */
public class SharedPreferencesHelps {
    private static final String TAG = "SharedPreferencesHelps";

    private static SharedPreferences sPreferences = null;
    private static SharedPreferences.Editor sEditor;

    private static SharedPreferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sEditor = sPreferences.edit();
        }
        return sPreferences;
    }

    /**
     * Save player mode.
     */
    public static synchronized String getPlayerMode(Context context, String key,
                                                    String defaultValue) {
        return getPreferences(context).getString(key, defaultValue);
    }

    /**
     * Get player mode.
     */
    public static synchronized void setPlayerMode(Context context, String key, String value) {
        getPreferences(context).edit().putString(key, value).apply();
    }

    /**
     * Save bluetooth audio state.
     *
     * @param context context
     * @param state   audio state
     */
    public static void saveAudioState(Context context, int state) {
        LogUtil.debug(TAG, "saveAudioState play state : " + state);
        if (state != getPlayState()) {
            sEditor.putInt(BtConstants.BT_MUSIC_PLAY_STATE, state);
            sEditor.apply();
        }
    }

    /**
     * Get bluetooth audio play state.
     *
     * @return audio play state
     */
    public static int getPlayState() {
        return sPreferences.getInt(BtConstants.BT_MUSIC_PLAY_STATE, BtConstants.PLAY_STATE_STOPPED);
    }

    /**
     * Save bluetooth device address.
     *
     * @param addr bluetooth address
     */
    public static void saveBluetoothDeviceAddress(String addr) {
        LogUtil.debug(TAG, "saveBluetoothDeviceAddress addr : " + addr);
        sEditor.putString(BtConstants.BT_CONNECTED_DEVICE_ADDR, addr);
        sEditor.apply();
    }

    /**
     * Get bluetooth address which is connected.
     *
     * @return bluetooth address
     */
    public static String getBluetoothDeviceAddress() {
        return sPreferences.getString(BtConstants.BT_CONNECTED_DEVICE_ADDR,
                BtConstants.DEFAULT_BT_ADDR);
    }

    /*
     * Set object data.
     *
     * @param context Context
     * @param key     Key
     * @param value   Value
     */
    public static void setObjectData(Context context, String key, RecordAudioInfo value) {
        Gson gson = new Gson();
        getPreferences(context).edit().putString(key, gson.toJson(value)).apply();
    }

    /**
     * Set object list data.
     *
     * @param context Context
     * @param key     Key
     * @param value   Value
     */
    public static void setObjectData(Context context, String key, List<AudioInfoBean> value) {
        Gson gson = new Gson();
        getPreferences(context).edit().putString(key, gson.toJson(value)).apply();
    }

    /**
     * Get object data.
     *
     * @param context Context
     * @param key     Key
     */
    public static String getObjectData(Context context, String key) {
        return getPreferences(context).getString(key, "");
    }
}