package com.ts.sdk.media.callback;

import com.ts.sdk.media.bean.AudioInfoBean;

interface IOnlineMusicCallback {

    void onMusicChange(in AudioInfoBean musicInfo);

    void onMusicState(int playState);

    void onMusicProgress(int progress, in AudioInfoBean musicInfo);

    void onBufferingUpdate(int progress);
}
