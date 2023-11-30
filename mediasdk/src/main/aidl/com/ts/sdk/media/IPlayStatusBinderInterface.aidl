// IUsbBinderInterface.aidl
package com.ts.sdk.media;

import com.ts.sdk.media.callback.IMediaSourceCallback;
import com.ts.sdk.media.bean.AudioInfoBean;
interface IPlayStatusBinderInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     int getCurrentPlayer();

     void setCurrentPlayer(int player);

     int getCurrentMusicTab();

     void setCurrentMusicTab(int position);

     void registerMediaSourceCallback(IMediaSourceCallback callback);

     void unRegisterMediaSourceCallback(IMediaSourceCallback callback);

     List<AudioInfoBean> searchAudio(String singer, String song);

     boolean isVideoForeGround();

     int getCurrentMediaSource();
}
