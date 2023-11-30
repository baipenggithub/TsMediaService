// IUsbMusicCallback.aidl
package com.ts.sdk.media.callback;

// Declare any non-default types here with import statements
import com.ts.sdk.media.bean.AudioInfoBean;
interface IUsbMusicCallback {
    void onAudioStateChange(in AudioInfoBean audioInfo);
    void addAudio(in AudioInfoBean audioInfo);
    void startPlayMusic(in AudioInfoBean audioInfo);
    void onAudioQueryCompleted(in List<AudioInfoBean> audioInfos);
    oneway void onScanListUpdated(in List<AudioInfoBean> audioInfos);
}
