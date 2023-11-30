package com.ts.service.media.bind;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.saicmotor.onlinemedia.IPlayService;
import com.saicmotor.onlinemedia.MusicServiceManage;
import com.saicmotor.onlinemedia.OnPlayEvent;
import com.saicmotor.onlinemedia.modle.OnLineMusic;
import com.ts.sdk.media.IOnlineMusicBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.callback.IOnlineMusicCallback;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.utils.LogUtil;

public class OnlineMusicPlayerBinder extends IOnlineMusicBinderInterface.Stub {

    private static final String TAG = "OnlineMusicPlayerBinder";
    private Context mContext;
    private IPlayService mPlayService;
    private AudioInfoBean mCurrentPlayAudioInfo;
    private boolean mOnlineMusicConnected = false;
    private final RemoteCallbackList<IOnlineMusicCallback> mIOnlineMusicCallback = new RemoteCallbackList<>();
    private OnPlayEvent mPlayEvent;
    private int mPlayState = 0;

    /**
     * MediaPlayerBinder.
     *
     * @param context context
     */
    public OnlineMusicPlayerBinder(Context context) {
        mContext = context;
        try {
            MusicServiceManage.bindOnLineMusicService(context, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mPlayService = IPlayService.Stub.asInterface(service);
                    LogUtil.debug(TAG, "Online music ---> onServiceConnected "
                            + (mPlayService == null));
                    try {
                        if (mPlayService != null) {
                            initListener();
                            mPlayService.addOnPlayEventListener(mPlayEvent);
                        }
                        mOnlineMusicConnected = true;
                        OnLineMusic onLineMusic = mPlayService.getOnlIneMusic();
                        if (onLineMusic != null) {
                            AudioInfoBean audioInfoBean = new AudioInfoBean();
                            audioInfoBean.setAudioArtistName(onLineMusic.getArtist());
                            audioInfoBean.setAudioName(onLineMusic.getMusicName());
                            audioInfoBean.setAvatar(onLineMusic.getAlbumUrl());
                            audioInfoBean.setAudioDuration(onLineMusic.getDuration());
                            mCurrentPlayAudioInfo = audioInfoBean;
                        }
                    } catch (RemoteException exception) {
                        exception.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    LogUtil.debug(TAG, "Online music ---> onServiceDisconnected");
                    mPlayService = null;
                    mOnlineMusicConnected = false;
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean pause() throws RemoteException {
        if (mPlayService != null) {
            LogUtil.debug(TAG, "Online music ---> pause");
            mPlayService.pause();
            return true;
        }
        return false;
    }

    @Override
    public boolean play() throws RemoteException {
        if (mPlayService != null) {
            LogUtil.debug(TAG, "Online music ---> play");
            mPlayService.play();
            return true;
        }
        return false;
    }

    @Override
    public boolean prev() throws RemoteException {
        if (mPlayService != null) {
            LogUtil.debug(TAG, "Online music ---> previous");
            mPlayService.previous();
            return true;
        }
        return false;
    }

    @Override
    public boolean next() throws RemoteException {
        if (mPlayService != null) {
            LogUtil.debug(TAG, "Online music ---> next");
            mPlayService.next();
            return true;
        }
        return false;
    }

    @Override
    public AudioInfoBean getOnlineMusic() throws RemoteException {
        if (mPlayService != null) {
            OnLineMusic onLineMusic = mPlayService.getOnlIneMusic();
            AudioInfoBean audioInfoBean = new AudioInfoBean();
            if (onLineMusic != null) {
                audioInfoBean.setAudioArtistName(onLineMusic.getArtist());
                audioInfoBean.setAudioName(onLineMusic.getMusicName());
                audioInfoBean.setAvatar(onLineMusic.getAlbumUrl());
                audioInfoBean.setAudioDuration(onLineMusic.getDuration());
                mCurrentPlayAudioInfo = audioInfoBean;
            }
            return audioInfoBean;
        }
        return null;
    }

    @Override
    public int getPlayerStatus() throws RemoteException {
        if (mPlayService != null) {
            LogUtil.debug(TAG, "Online music ---> getPlayerStatus");
            int state =  mPlayService.getPlayerStatus();
            if (state == MusicConstants.ONLINE_PLAYER_PLAYING) {
                return MusicConstants.MUSIC_PLAYER_PLAYING;
            } else if (state == MusicConstants.ONLINE_PLAYER_PAUSED) {
                return MusicConstants.MUSIC_PLAYER_PAUSE;
            } else {
                return state;
            }
        }
        return MusicConstants.ONLINE_PLAYER_IDLE;
    }

    @Override
    public void registerOnlineMusicListener(IOnlineMusicCallback callback) throws RemoteException {
        LogUtil.debug(TAG, "Online music registerOnlineMusicListener");
        if (mIOnlineMusicCallback != null) {
            mIOnlineMusicCallback.register(callback);
        }
    }

    @Override
    public void unRegisterOnlineMusicListener(IOnlineMusicCallback callback)
            throws RemoteException {
        if (mIOnlineMusicCallback != null) {
            mIOnlineMusicCallback.unregister(callback);
        }
    }

    private void initListener() {
        mPlayEvent = new OnPlayEvent.Stub() {
            @Override
            public void onMusicChange(OnLineMusic onLineMusic) {
                AudioInfoBean audioInfoBean = new AudioInfoBean();
                if (onLineMusic != null) {
                    audioInfoBean.setAudioArtistName(onLineMusic.getArtist());
                    audioInfoBean.setAudioName(onLineMusic.getMusicName());
                    audioInfoBean.setAvatar(onLineMusic.getImgUrl());
                    audioInfoBean.setAudioDuration(onLineMusic.getDuration());
                }
                LogUtil.debug(TAG, "Online music onMusicChange --->  song : "
                        + audioInfoBean.getAudioName() + ",  singer : "
                        + audioInfoBean.getAudioArtistName());
                mCurrentPlayAudioInfo = audioInfoBean;
                if (null != mIOnlineMusicCallback) {
                    synchronized (mIOnlineMusicCallback) {
                        int listenerCount = mIOnlineMusicCallback.beginBroadcast();
                        for (int index = 0; index < listenerCount; index++) {
                            try {
                                mIOnlineMusicCallback.getBroadcastItem(index)
                                        .onMusicChange(mCurrentPlayAudioInfo);
                            } catch (RemoteException ex) {
                                ex.printStackTrace();
                            }
                        }
                        mIOnlineMusicCallback.finishBroadcast();
                    }
                }
            }

            @Override
            public void onMusicStart() {
                mPlayState = MusicConstants.MUSIC_PLAYER_PLAYING;
                mCurrentPlayAudioInfo.setPlayState(mPlayState);
                LogUtil.debug(TAG, "Online music stateChange --->  state : "
                        + mCurrentPlayAudioInfo.getPlayState());
                if (null != mIOnlineMusicCallback) {
                    synchronized (mIOnlineMusicCallback) {
                        int listenerCount = mIOnlineMusicCallback.beginBroadcast();
                        for (int index = 0; index < listenerCount; index++) {
                            try {
                                mIOnlineMusicCallback.getBroadcastItem(index)
                                        .onMusicState(MusicConstants.MUSIC_PLAYER_PLAYING);
                            } catch (RemoteException ex) {
                                ex.printStackTrace();
                            }
                        }
                        mIOnlineMusicCallback.finishBroadcast();
                    }
                }
            }

            @Override
            public void onMusicPause() {
                mPlayState = MusicConstants.MUSIC_PLAYER_PAUSE;
                mCurrentPlayAudioInfo.setPlayState(mPlayState);
                LogUtil.debug(TAG, "Online music stateChange --->  state : "
                        + mCurrentPlayAudioInfo.getPlayState());
                if (null != mIOnlineMusicCallback) {
                    synchronized (mIOnlineMusicCallback) {
                        int listenerCount = mIOnlineMusicCallback.beginBroadcast();
                        for (int index = 0; index < listenerCount; index++) {
                            try {
                                mIOnlineMusicCallback.getBroadcastItem(index)
                                        .onMusicState(MusicConstants.MUSIC_PLAYER_PAUSE);
                            } catch (RemoteException ex) {
                                ex.printStackTrace();
                            }
                        }
                        mIOnlineMusicCallback.finishBroadcast();
                    }
                }
            }

            @Override
            public void onMusicProgress(int progress) {
                LogUtil.debug(TAG, "Online music onMusicProgress --->  progress : "
                        + progress);
                if (null != mIOnlineMusicCallback) {
                    synchronized (mIOnlineMusicCallback) {
                        int listenerCount = mIOnlineMusicCallback.beginBroadcast();
                        for (int index = 0; index < listenerCount; index++) {
                            try {
                                mIOnlineMusicCallback.getBroadcastItem(index)
                                        .onMusicProgress(progress, mCurrentPlayAudioInfo);
                            } catch (RemoteException ex) {
                                ex.printStackTrace();
                            }
                        }
                        mIOnlineMusicCallback.finishBroadcast();
                    }
                }
            }

            @Override
            public void onBufferingUpdate(int progress) {
                // TODO nothing
            }
        };
    }
}
