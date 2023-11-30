package com.ts.service.media.bind;

import static com.ts.sdk.media.constants.MusicConstants.ERROR_CODE;

import android.content.Context;
import android.os.RemoteException;

import com.ts.sdk.media.IBtMusicBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.callback.IBtMusicCallback;
import com.ts.service.media.presenter.BtMusicMediaPlayer;

import java.util.List;

public class BtMusicPlayerBinder extends IBtMusicBinderInterface.Stub {

    private BtMusicMediaPlayer mBtMusicMediaPlayer;

    /**
     * Initialization.
     *
     * @param context ContextBtMusicPlayerBinder
     */
    public BtMusicPlayerBinder(Context context) {
        if (mBtMusicMediaPlayer == null) {
            mBtMusicMediaPlayer = new BtMusicMediaPlayer(context);
        }
    }

    @Override
    public boolean pause() throws RemoteException {
        if (null != mBtMusicMediaPlayer && mBtMusicMediaPlayer.getAvrcpConnectionState()) {
            return mBtMusicMediaPlayer.pause();
        }
        return false;
    }

    @Override
    public boolean play() throws RemoteException {
        if (null != mBtMusicMediaPlayer && mBtMusicMediaPlayer.getAvrcpConnectionState()) {
            return mBtMusicMediaPlayer.play();
        }
        return false;
    }

    @Override
    public boolean playFromMediaId(String mediaId) throws RemoteException {
        if (null != mBtMusicMediaPlayer && mBtMusicMediaPlayer.getAvrcpConnectionState()) {
            return mBtMusicMediaPlayer.playFromMediaId(mediaId);
        }
        return false;
    }

    @Override
    public boolean prev() throws RemoteException {
        if (null != mBtMusicMediaPlayer && mBtMusicMediaPlayer.getAvrcpConnectionState()) {
            return mBtMusicMediaPlayer.prev();
        }
        return false;
    }

    @Override
    public boolean next() throws RemoteException {
        if (null != mBtMusicMediaPlayer && mBtMusicMediaPlayer.getAvrcpConnectionState()) {
            return mBtMusicMediaPlayer.next();
        }
        return false;
    }

    @Override
    public AudioInfoBean getMetaData() throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            return mBtMusicMediaPlayer.getMetaData();
        }
        return null;
    }

    @Override
    public void setPlayMode(int value) throws RemoteException {
        if (null != mBtMusicMediaPlayer && mBtMusicMediaPlayer.getAvrcpConnectionState()) {
            mBtMusicMediaPlayer.setPlayMode(value);
        }
    }

    @Override
    public int getPlayMode() throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            return mBtMusicMediaPlayer.getPlayMode();
        }
        return ERROR_CODE;
    }

    @Override
    public boolean getPlayState() throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            return mBtMusicMediaPlayer.getPlayState();
        }
        return false;
    }

    @Override
    public boolean getAAConnectionState() throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            return mBtMusicMediaPlayer.getAAConnectionState();
        }
        return false;
    }

    @Override
    public int getBluetoothState() throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            return mBtMusicMediaPlayer.getBluetoothState();
        }
        return ERROR_CODE;
    }

    @Override
    public List<AudioInfoBean> getCurrentPlayList() throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            return mBtMusicMediaPlayer.getCurrentPlayList();
        }
        return null;
    }

    @Override
    public void registerAudioCallbackListenser(IBtMusicCallback callbackListener)
            throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            mBtMusicMediaPlayer.registerAudioCallbackListenser(callbackListener);
        }
    }

    @Override
    public void unRegisterAudioCallbackListenser(IBtMusicCallback callbackListener)
            throws RemoteException {
        if (null != mBtMusicMediaPlayer) {
            mBtMusicMediaPlayer.unRegisterAudioCallbackListenser(callbackListener);
        }
    }

    /**
     * Destroy.
     */
    public void destroy() {
        if (null != mBtMusicMediaPlayer) {
            mBtMusicMediaPlayer.destroy();
        }
    }

    /**
     * Register call back.
     */
    public void registerCallback() {
        if (null != mBtMusicMediaPlayer) {
            mBtMusicMediaPlayer.registerCallback();
        }
    }

    /**
     * unRegister call back.
     */
    public void unRegisterCallback() {
        if (null != mBtMusicMediaPlayer) {
            mBtMusicMediaPlayer.unRegisterCallback();
        }
    }

    /**
     * Notify AndroidAuto connection state changed.
     *
     * @param isConnection flag AA connection
     */
    public void notifyAAConnectionState(boolean isConnection) {
        if (null != mBtMusicMediaPlayer) {
            mBtMusicMediaPlayer.notifyAAConnectionState(isConnection);
        }
    }
}
