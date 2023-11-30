package com.ts.sdk.media.mananger;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;


import com.ts.sdk.media.IOnlineMusicBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.callback.IOnlineMusicCallback;
import com.ts.sdk.media.constants.MusicConstants;
import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.sdk.media.contractinterface.IMediaServiceListener;

import java.util.ArrayList;
import java.util.List;

public class OnlineMusicManager extends BaseManager {

    private static final String TAG = OnlineMusicManager.class.getSimpleName();

    /**
     * Service listener.
     */
    private static List<IMediaServiceListener> sListener = new ArrayList<>();

    /**
     * Instance.
     */
    private static volatile OnlineMusicManager sInstance = null;

    /**
     * Is service connect flag.
     */
    private static boolean sIsServiceCon = false;

    /**
     * Binder object.
     */
    private IOnlineMusicBinderInterface mIOnlineBinder;

    private OnlineMusicManager(Context context) {
        super(context);
    }

    /**
     * Get Instance.
     *
     * @param context  the context of the app
     * @param listener service connected listener
     */
    public static synchronized OnlineMusicManager getInstance(Context context,
                                                              IMediaServiceListener listener) {
        if (listener != null) {
            sListener.add(listener);
        }
        if (sInstance == null) {
            sInstance = new OnlineMusicManager(context);
        }
        if (sIsServiceCon) {
            for (int i = 0;i < sListener.size(); i++) {
                sListener.get(i).onServiceConnected(sInstance);
            }
        }
        return sInstance;
    }

    @Override
    protected String getAction() {
        return ServiceConstants.SERVICE_ONLINE_MUSIC_PLAYER_ACTION;
    }

    @Override
    protected void setBinder(IBinder binder) {
        super.setBinder(binder);
        synchronized (OnlineMusicManager.class) {
            if (binder != null) {
                mIOnlineBinder = IOnlineMusicBinderInterface.Stub.asInterface(binder);
                if (sListener != null) {
                    for (int i = 0;i < sListener.size(); i++) {
                        sListener.get(i).onServiceConnected(sInstance);
                    }
                }
                sIsServiceCon = true;
            } else {
                mIOnlineBinder = null;
                if (sListener != null) {
                    for (int i = 0; i < sListener.size(); i++) {
                        sListener.get(i).onServiceDisconnected();
                    }
                }
                sIsServiceCon = false;
            }
        }
    }

    @Override
    protected void destroy() {
        // TODO destroy
    }

    /**
     * Get Online Music info.
     */
    public AudioInfoBean getOnlineMusic() throws RemoteException {
        if (mIOnlineBinder != null) {
            return mIOnlineBinder.getOnlineMusic();
        }
        return null;
    }

    /**
     * Get Online Music Player Status.
     */
    public int getPlayerStatus() throws RemoteException {
        if (mIOnlineBinder != null) {
            return mIOnlineBinder.getPlayerStatus();
        }
        return MusicConstants.PLAYER_STATUS_IDLE;
    }

    /**
     * Register online music listener.
     *
     * @param callback IOnlineMusicCallback
     */
    public void registerOnlineMusicListener(IOnlineMusicCallback callback) throws RemoteException {
        if (mIOnlineBinder != null) {
            mIOnlineBinder.registerOnlineMusicListener(callback);
        }
    }

    /**
     * UnRegister.
     */
    public void unRegisterOnlineMusicListener(IOnlineMusicCallback callback)
            throws RemoteException {
        if (mIOnlineBinder != null) {
            mIOnlineBinder.unRegisterOnlineMusicListener(callback);
        }
    }

    /**
     * Play next video.
     */
    public void next() throws RemoteException {
        if (mIOnlineBinder != null) {
            mIOnlineBinder.next();
        }
    }


    /**
     * Play prev video.
     */
    public void prev() throws RemoteException {
        if (mIOnlineBinder != null) {
            mIOnlineBinder.prev();
        }
    }

    /**
     * Pause play video.
     */
    public void pause() throws RemoteException {
        if (mIOnlineBinder != null) {
            mIOnlineBinder.pause();
        }
    }

    /**
     * Resume play video.
     */
    public void resume() throws RemoteException {
        if (mIOnlineBinder != null) {
            mIOnlineBinder.play();
        }
    }
}
