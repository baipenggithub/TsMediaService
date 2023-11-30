// IUsbMusicCallback.aidl
package com.ts.sdk.media.callback;

// Declare any non-default types here with import statements
import com.ts.sdk.media.bean.VideoInfoBean;
import com.ts.sdk.media.bean.AudioInfoBean;
interface IUsbVideoCallback {
    void onVideoStateChange(in VideoInfoBean videoInfo);
    void onVideoQueryCompleted(in List<VideoInfoBean> videoInfos);

    /**
     * Inform HMI media database to add video file
     */
    void addVideo(in VideoInfoBean videoInfo);

    /**
     * Pause play video.
     */
    void pause();

    /**
     * Resume play video.
     */
    void resume();

    /**
     * Play next video.
     */
    void prev();

    /**
     * Play next video.
     */
    void next();

    /**
     * Fast Forward video.
     */
    void fastForward();

    /**
     * Fast Back video.
     */
    void fastBack();

    /**
     * Key Event up.
      */
    void keyEventUp();

    /**
     * Mute Changed.
      */
    void muteChanged();

    boolean getPlayState();

    oneway void onScanListUpdated(in List<VideoInfoBean> videoInfos);
}
