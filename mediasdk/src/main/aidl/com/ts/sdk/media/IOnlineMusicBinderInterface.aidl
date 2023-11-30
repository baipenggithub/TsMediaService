package com.ts.sdk.media;

import com.ts.sdk.media.callback.IOnlineMusicCallback;
import com.ts.sdk.media.bean.AudioInfoBean;

interface IOnlineMusicBinderInterface {

    /**
     * Pause playback.
     */
    boolean pause();

    /**
     * Resume playback.
     */
    boolean play();

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
    AudioInfoBean getOnlineMusic();

    /**
     * Get Player Status.
     *
     */
    int getPlayerStatus();

    /**
     * registerOnlineMusicListenser.
     */
    void registerOnlineMusicListener(IOnlineMusicCallback callbackListener);

    /**
     * unRegisterOnlineMusicListenser.
     */
    void unRegisterOnlineMusicListener(IOnlineMusicCallback callbackListener);

}
