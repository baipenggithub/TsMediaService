package com.ts.service.media.bind;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.ts.sdk.media.IUsbBinderInterface;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.bean.VideoInfoBean;
import com.ts.sdk.media.callback.IUsbDevicesListener;
import com.ts.sdk.media.callback.IUsbVideoCallback;
import com.ts.service.media.constants.VideoConstants;
import com.ts.service.media.presenter.AudioSourceManager;
import com.ts.service.media.receiver.HardKeyMonitor;
import com.ts.service.media.receiver.UsbDeviceMonitor;
import com.ts.service.media.utils.LogUtil;
import com.ts.service.media.utils.MediaScannerFile;

import java.util.List;

public class UsbBinder extends IUsbBinderInterface.Stub implements
        MediaScannerFile.IVideoScanListener, HardKeyMonitor.IKeyEventListener {
    private static final String TAG = MusicPlayerBinder.class.getSimpleName();
    private final Context mContext;
    private IUsbVideoCallback mVideoCallback;
    private final AudioSourceManager mAudioSourceManager;
    private final RemoteCallbackList<IUsbDevicesListener> mUsbDevicesListener = new RemoteCallbackList<>();

    @Override
    public List<VideoInfoBean> getVideoInfo(String key) throws RemoteException {

        List<VideoInfoBean> videoInfoBeans = MediaScannerFile.getInstance(mContext)
                .getVideoInfo(key);
        return videoInfoBeans;
    }

    @Override
    public List<VideoInfoBean> getAllVideo() throws RemoteException {
        MediaScannerFile.getInstance(mContext).queryAllVideo(VideoConstants.QUERY_VIDEO_CLIENT);
        return null;
    }

    @Override
    public List<UsbDevicesInfoBean> getUsbDevices() throws RemoteException {
        return UsbDeviceMonitor.getInstance(mContext).getUsbDevices();
    }

    @Override
    public void registerVideoStatusObserver(IUsbVideoCallback callback) {
        mVideoCallback = callback;
    }

    @Override
    public void unRegisterVideoStatusObserver(IUsbVideoCallback callback) {
        mVideoCallback = null;
    }

    @Override
    public void registerUsbDevicesStatusObserver(IUsbDevicesListener listener) {
        if (listener != null) {
            mUsbDevicesListener.register(listener);
        }
    }

    @Override
    public void unRegisterUsbDevicesStatusObserver(IUsbDevicesListener listener) {
        if (listener != null) {
            mUsbDevicesListener.unregister(listener);
        }
    }

    @Override
    public boolean pause() throws RemoteException {
        if (mVideoCallback != null) {
            mVideoCallback.pause();
            return true;
        }
        return false;
    }

    @Override
    public boolean play() throws RemoteException {
        if (mVideoCallback != null) {
            mVideoCallback.resume();
            return true;
        }
        return false;
    }

    @Override
    public boolean prev() throws RemoteException {
        if (mVideoCallback != null) {
            mVideoCallback.prev();
            return true;
        }
        return false;
    }

    @Override
    public boolean next() throws RemoteException {
        // TODO
        return false;
    }

    @Override
    public boolean getPlayState() throws RemoteException {
        if (mVideoCallback != null) {
            return mVideoCallback.getPlayState();
        }
        return false;
    }

    /**
     * IHotspotBinder.
     *
     * @param context context
     */
    public UsbBinder(Context context) {
        mContext = context;
        mAudioSourceManager = AudioSourceManager.getInstance(mContext);
        UsbDeviceMonitor.getInstance(context).setUsbDeviceListener(new UsbDeviceMonitor
                .UsbDeviceListener() {
            @Override
            public void onDeviceChange(List<UsbDevicesInfoBean> usbDevices) {
                try {
                    if (mUsbDevicesListener != null) {
                        synchronized (mUsbDevicesListener) {
                            checkListener();
                            int listenerCount = mUsbDevicesListener.beginBroadcast();
                            for (int index = 0; index < listenerCount; index++) {
                                mUsbDevicesListener.getBroadcastItem(index)
                                        .onUsbDevicesChange(usbDevices);
                            }
                            mUsbDevicesListener.finishBroadcast();
                        }
                    }
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }

            @Override
            public void onScanChange(int state, String deviceId, int portId) {
                try {
                    if (mUsbDevicesListener != null) {
                        synchronized (mUsbDevicesListener) {
                            checkListener();
                            int listenerCount = mUsbDevicesListener.beginBroadcast();
                            for (int index = 0; index < listenerCount; index++) {
                                mUsbDevicesListener.getBroadcastItem(index)
                                        .onScanStateChange(state, deviceId, portId);
                            }
                            mUsbDevicesListener.finishBroadcast();
                        }
                    }
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }
        });
        MediaScannerFile.getInstance(context).setVideoScanListener(this);
        HardKeyMonitor.getInstance(context).addKeyEventListener(this);
    }

    private void checkListener() {
        try {
            if (mUsbDevicesListener != null) {
                mUsbDevicesListener.finishBroadcast();
            }
        } catch (IllegalStateException ex) {
            LogUtil.debug(TAG, "RemoteCallbackList checkListener");
        }
    }

    @Override
    public void addVideo(VideoInfoBean videoInfoBean) {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.addVideo(videoInfoBean);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void queryComplete(List<VideoInfoBean> videoList) {
        try {
            if (null != mVideoCallback) {
                mVideoCallback.onVideoQueryCompleted(videoList);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }


    @Override
    public void mediaPrevious() {
        try {
            prev();
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void mediaNext() {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.next();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void mediaFastForward() {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.fastForward();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void mediaFastBack() {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.fastBack();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void keyEventUp(int keyCode, boolean isLongPress) {
        // do nothing.
    }

    @Override
    public void keyEventUp() {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.keyEventUp();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void keyEventDown(int keyCode, boolean isLongPress) {
        // do nothing.
    }

    @Override
    public void muteChanged(boolean isMute) {
        int mediaSource = mAudioSourceManager.getCurrentMediaSource();
        try {
            if (mVideoCallback != null) {
                if (isMute) {
                    mVideoCallback.pause();
                } else {
                    mVideoCallback.resume();
                }
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void muteChanged() {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.muteChanged();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void resumePlay() {
        try {
            if (mVideoCallback != null) {
                mVideoCallback.resume();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }
}
