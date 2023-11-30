package com.ts.sdk.media;

import com.ts.sdk.media.callback.IBtMusicCallback;
import com.ts.sdk.media.bean.AudioInfoBean;

interface IBtMusicBinderInterface {

    /**
     * Pause playback.
     */
    boolean pause();

    /**
     * Resume playback.
     */
    boolean play();

    /**
    * Play bt musci from media id.
    */
    boolean playFromMediaId(String mediaId);

    /**
     * Play the previous song and maintain the previous logic internally.
     */
    boolean prev();

    /**
     * Play the next song and maintain the next logic internally.
     */
    boolean next();

    /**
     * Get MetaData.
     *
     * @return metadataItem
     */
     AudioInfoBean getMetaData();

    /**
     * Set Playing Mode.
     */
     void setPlayMode(int value);

    /**
     * Get Playing Mode.
     */
     int getPlayMode();

    /**
     * Get Playing State.
     */
     boolean getPlayState();

     /**
     * Get Android auto connection state.
     */
     boolean getAAConnectionState();

    /**
     * Get Bluetooth State.
     */
     int getBluetoothState();

     /**
     * Get current play list
     */
     List<AudioInfoBean> getCurrentPlayList();

    /**
     * registerAudioCallbackListenser.
     */
     void registerAudioCallbackListenser(IBtMusicCallback callbackListener);

    /**
     * unRegisterAudioCallbackListenser.
     */
     void unRegisterAudioCallbackListenser(IBtMusicCallback callbackListener);
}
