package com.ts.sdk.media.mananger;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.ts.sdk.media.IUsbBinderInterface;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.bean.VideoInfoBean;
import com.ts.sdk.media.callback.IUsbDevicesListener;
import com.ts.sdk.media.callback.IUsbVideoCallback;
import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.sdk.media.contractinterface.IMediaServiceListener;

import java.util.ArrayList;
import java.util.List;

public class UsbVideoManager extends BaseManager {
    /**
     * this class log tag.
     */
    private static final String TAG = UsbVideoManager.class.getSimpleName();

    /**
     * service listener.
     */
    private static List<IMediaServiceListener> sListener = new ArrayList<>();

    /**
     * instance.
     */
    private static volatile UsbVideoManager sInstance = null;

    /**
     * is service connect flag.
     */
    private static boolean sIsServiceCon = false;

    /**
     * binder object.
     */
    private IUsbBinderInterface mIUsbBinder;

    private UsbVideoManager(Context context) {
        super(context);
    }

    /**
     * get Instance.
     *
     * @param context  the context of the app
     * @param listener service connected listener
     */
    public static synchronized UsbVideoManager getInstance(Context context,
                                                           IMediaServiceListener listener) {
        if (listener != null) {
            sListener.add(listener);
        }
        if (sInstance == null) {
            sInstance = new UsbVideoManager(context);
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
        return ServiceConstants.SERVICE_USB_SCAN_ACTION;
    }

    @Override
    protected void setBinder(IBinder binder) {
        super.setBinder(binder);
        synchronized (UsbVideoManager.class) {
            if (binder != null) {
                mIUsbBinder = IUsbBinderInterface.Stub.asInterface(binder);
                if (sListener != null) {
                    for (int i = 0;i < sListener.size(); i++) {
                        sListener.get(i).onServiceConnected(sInstance);
                    }
                }
                sIsServiceCon = true;
            } else {
                mIUsbBinder = null;
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

    public List<VideoInfoBean> getVideoInfo(String key) throws RemoteException {
        return mIUsbBinder != null ? mIUsbBinder.getVideoInfo(key) : null;
    }

    public List<VideoInfoBean> getAllVideo() throws RemoteException {
        return mIUsbBinder != null ? mIUsbBinder.getAllVideo() : null;
    }

    public List<UsbDevicesInfoBean> getUsbDevices() throws RemoteException {
        return mIUsbBinder != null ? mIUsbBinder.getUsbDevices() : null;
    }

    /**
     *  Register usb listener.
     *
     * @param listener IUsbDevicesListener
     */
    public void registerUsbDevicesStatusObserver(IUsbDevicesListener listener) {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.registerUsbDevicesStatusObserver(listener);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * UnRegister.
     */
    public void unRegisterUsbDevicesStatusObserver(IUsbDevicesListener listener) {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.unRegisterUsbDevicesStatusObserver(listener);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Register Video file listener.
     *
     * @param callback IUsbVideoCallback
     */
    public void registerVideoStatusObserver(IUsbVideoCallback callback) {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.registerVideoStatusObserver(callback);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * UnRegister.
     */
    public void unRegisterVideoStatusObserver(IUsbVideoCallback callback) {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.unRegisterVideoStatusObserver(callback);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
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
     * Play next video.
     */
    public void next() {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.next();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }


    /**
     * Play prev video.
     */
    public void prev() {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.prev();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Pause play video.
     */
    public void pause() {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.pause();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Resume play video.
     */
    public void resume() {
        try {
            if (mIUsbBinder != null) {
                mIUsbBinder.play();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Get Play State.
     */
    public boolean getPlayState() {
        try {
            if (mIUsbBinder != null) {
                return mIUsbBinder.getPlayState();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
        return false;
    }

}
