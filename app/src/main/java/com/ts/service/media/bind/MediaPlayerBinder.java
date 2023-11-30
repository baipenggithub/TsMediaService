package com.ts.service.media.bind;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.ts.sdk.media.IPlayStatusBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.callback.IMediaSourceCallback;
import com.ts.service.media.constants.VideoConstants;
import com.ts.service.media.presenter.AudioSourceManager;
import com.ts.service.media.utils.LogUtil;
import com.ts.service.media.utils.MediaScannerFile;

import java.util.List;

public class MediaPlayerBinder extends IPlayStatusBinderInterface.Stub {

    private static final String TAG = "MediaPlayerBinder";
    private final Context mContext;
    private int mCurrentPlayer = 0;
    private int mCurrentMusicTab = 0;
    private final RemoteCallbackList<IMediaSourceCallback> mMediaSourceCallback = new RemoteCallbackList<>();
    private final AudioSourceManager mAudioSourceManager;
    private int mMediaSource = -1;

    /**
     * MediaPlayerBinder.
     *
     * @param context context
     */
    public MediaPlayerBinder(Context context) {
        mContext = context;
        mAudioSourceManager = AudioSourceManager.getInstance(mContext);
        if (null != mAudioSourceManager) {
            AudioSourceManager.OnAudioSourceChangedListener onAudioSourceChangedListener = mediaSourceType -> {
                mMediaSource = mediaSourceType;
                try {
                    synchronized (mMediaSourceCallback) {
                        checkListener();
                        int listenerCount = mMediaSourceCallback.beginBroadcast();
                        for (int index = 0; index < listenerCount; index++) {
                            mMediaSourceCallback.getBroadcastItem(index)
                                    .onMediaSourceChange(mMediaSource);
                        }
                        mMediaSourceCallback.finishBroadcast();
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            };
            mAudioSourceManager.registerAudioFocusChangedListener(onAudioSourceChangedListener);
            mMediaSource = mAudioSourceManager.getCurrentMediaSource();
        }
    }

    @Override
    public int getCurrentPlayer() throws RemoteException {
        return mCurrentPlayer;
    }

    @Override
    public void setCurrentPlayer(int player) throws RemoteException {
        if (player != VideoConstants.VIDEO_PLAYER) {
            mCurrentPlayer = player;
        }
    }

    @Override
    public int getCurrentMusicTab() throws RemoteException {
        return mCurrentMusicTab;
    }

    @Override
    public void setCurrentMusicTab(int position) throws RemoteException {
        mCurrentMusicTab = position;
    }

    @Override
    public void registerMediaSourceCallback(IMediaSourceCallback mediaSourceCallback) {
        mMediaSourceCallback.register(mediaSourceCallback);
        try {
            synchronized (mMediaSourceCallback) {
                checkListener();
                int listenerCount = mMediaSourceCallback.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    mMediaSourceCallback.getBroadcastItem(index)
                            .onMediaSourceChange(mMediaSource);
                }
                mMediaSourceCallback.finishBroadcast();
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void unRegisterMediaSourceCallback(IMediaSourceCallback mediaSourceCallback) {
        mMediaSourceCallback.unregister(mediaSourceCallback);
    }

    @Override
    public List<AudioInfoBean> searchAudio(String singer, String song) {
        return MediaScannerFile.getInstance(mContext).queryMusic(singer, song);
    }

    @Override
    public boolean isVideoForeGround() {
        return false;
    }

    @Override
    public int getCurrentMediaSource() throws RemoteException {
        if (null != mAudioSourceManager) {
            return mAudioSourceManager.getCurrentMediaSource();
        }
        return mMediaSource;
    }

    private void checkListener() {
        try {
            mMediaSourceCallback.finishBroadcast();
        } catch (IllegalStateException ex) {
            LogUtil.debug(TAG, "mMediaSourceCallback checkListener");
        }
    }
}
