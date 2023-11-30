package com.ts.sdk.media.mananger;

import static com.ts.sdk.media.constants.MusicConstants.ERROR_CODE;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.ts.sdk.media.IBtMusicBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.callback.IBtMusicCallback;
import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.sdk.media.contractinterface.IMediaServiceListener;

import java.util.ArrayList;
import java.util.List;

public class BtMusicManager extends BaseManager {

    private static final String TAG = BtMusicManager.class.getSimpleName();
    private static List<IMediaServiceListener> sListener = new ArrayList<>();
    private static volatile BtMusicManager sInstance = null;
    private static boolean sIsServiceCon = false;
    private static IBtMusicBinderInterface sBinder;

    private BtMusicManager(Context context) {
        super(context);
    }

    /**
     * get Instance.
     *
     * @param context  the context of the app
     * @param listener service connected listener
     */
    public static synchronized BtMusicManager getInstance(
            Context context, IMediaServiceListener listener) {
        if (listener != null) {
            sListener.add(listener);
        }
        if (sInstance == null) {
            sInstance = new BtMusicManager(context);
        }
        if (sIsServiceCon) {
            for (int i = 0; i < sListener.size(); i++) {
                sListener.get(i).onServiceConnected(sInstance);
            }
        }
        return sInstance;
    }

    @Override
    protected String getAction() {
        return ServiceConstants.SERVICE_BT_MUSIC_PLAYER_ACTION;
    }

    @Override
    protected void setBinder(IBinder binder) {
        super.setBinder(binder);
        synchronized (BtMusicManager.class) {
            if (binder != null) {
                sBinder = IBtMusicBinderInterface.Stub.asInterface(binder);
                if (sListener != null) {
                    for (int i = 0; i < sListener.size(); i++) {
                        sListener.get(i).onServiceConnected(sInstance);
                    }
                }
                sIsServiceCon = true;
            } else {
                sBinder = null;
                if (sListener != null) {
                    for (int i = 0; i < sListener.size(); i++) {
                        sListener.get(i).onServiceDisconnected();
                    }
                }
                sIsServiceCon = false;
            }
        }
    }

    /**
     * Clear registered listening.
     */
    public void removeMediaServiceListener(IMediaServiceListener listener) throws RuntimeException {
        if (sListener != null) {
            sListener.remove(listener);
        }
    }

    /**
     * registerAudioCallbackListenser.
     *
     * @param callback IBtMusicCallback
     */
    public void registerAudioCallbackListenser(IBtMusicCallback callback) throws RemoteException {
        if (null != sBinder) {
            sBinder.registerAudioCallbackListenser(callback);
        }
    }

    /**
     * unRegisterAudioCallbackListenser.
     *
     * @param callback IBtMusicCallback
     */
    public void unRegisterAudioCallbackListenser(IBtMusicCallback callback) throws RemoteException {
        if (null != sBinder) {
            sBinder.unRegisterAudioCallbackListenser(callback);
        }
    }

    /**
     * Pause playback.
     */
    public boolean pause() throws RemoteException {
        if (null != sBinder) {
            return sBinder.pause();
        }
        return false;
    }

    /**
     * Resume playback.
     */
    public boolean play() throws RemoteException {
        if (null != sBinder) {
            return sBinder.play();
        }
        return false;
    }

    /**
     * Play bt music from media id.
     */
    public boolean playFromMediaId(String mediaId) throws RemoteException {
        if (null != sBinder) {
            return sBinder.playFromMediaId(mediaId);
        }
        return false;
    }

    /**
     * Play the previous song and maintain the previous logic internally.
     */
    public boolean prev() throws RemoteException {
        if (null != sBinder) {
            return sBinder.prev();
        }
        return false;
    }

    /**
     * Play the next song and maintain the next logic internally.
     */
    public boolean next() throws RemoteException {
        if (null != sBinder) {
            sBinder.next();
        }
        return false;
    }

    /**
     * Get media information.
     */
    public AudioInfoBean getMetaData() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getMetaData();
        }
        return null;
    }

    /**
     * Get playing state.
     */
    public boolean getPlayState() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getPlayState();
        }
        return false;
    }

    /**
     * Set Play Mode.
     */
    public void setPlayMode(int mode) throws RemoteException {
        if (null != sBinder) {
            sBinder.setPlayMode(mode);
        }
    }

    /**
     * Get Play Mode.
     */
    public int getPlayMode() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getPlayMode();
        }
        return ERROR_CODE;
    }

    /**
     * Get AndroidAuto connection State.
     */
    public boolean getAAConnectionState() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getAAConnectionState();
        }
        return false;
    }

    /**
     * Get Bluetooth State.
     */
    public int getBluetoothState() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getBluetoothState();
        }
        return ERROR_CODE;
    }

    /**
     * Get current play list.
     *
     * @return play list
     * @throws RemoteException throw exception
     */
    public List<AudioInfoBean> getCurrentPlayList() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getCurrentPlayList();
        }
        return null;
    }

    @Override
    protected void destroy() {
        // TODO Destroy
    }

    @Override
    public void release() {
        super.release();
    }
}
