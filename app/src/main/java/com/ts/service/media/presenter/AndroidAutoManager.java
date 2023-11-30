package com.ts.service.media.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.allgo.rui.IRemoteDeviceCallback;
import com.allgo.rui.IRemoteSessionCallback;
import com.allgo.rui.IRemoteUIService;
import com.allgo.rui.RemoteDevice;
import com.ts.service.media.bind.BtMusicPlayerBinder;
import com.ts.service.media.constants.BtConstants;
import com.ts.service.media.utils.LogUtil;

public class AndroidAutoManager {
    private static final String TAG = "AndroidAutoManager";
    private static Handler mHandler = new Handler();
    private IRemoteUIService mService;
    private BtMusicPlayerBinder mBinder;

    private Context mContext;

    /**
     * Constructor.
     *
     * @param context context
     * @param binder binder
     */
    public AndroidAutoManager(Context context, BtMusicPlayerBinder binder) {
        LogUtil.debug(TAG, "AndroidAutoManager");
        mContext = context;
        mBinder = binder;
        bindAllGoService();
    }

    /**
     * Service connection callback.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IRemoteUIService.Stub.asInterface(service);
            try {
                if (mService != null) {
                    mService.register(mDeviceCallback, mSessionCallback, null);
                }
                LogUtil.debug(TAG, "onServiceConnected Service = " + mService);
            } catch (RemoteException exception) {
                exception.printStackTrace();
                LogUtil.debug(TAG, "onServiceConnected error | Service = " + mService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            LogUtil.debug(TAG, "onServiceDisconnected " + mService);
            mService = null;
        }
    };

    /**
     * Remote UI device callback.
     */
    private final IRemoteDeviceCallback.Stub mDeviceCallback = new IRemoteDeviceCallback.Stub() {
        @Override
        public void onDeviceConnected(RemoteDevice remoteDevice) throws RemoteException {
            LogUtil.debug(TAG, "onDeviceConnected : " + remoteDevice.name);
        }

        @Override
        public void onDeviceDisconnected(RemoteDevice remoteDevice) throws RemoteException {
            if (remoteDevice.type == RemoteDevice.ANDROID_AUTO) {
                LogUtil.debug(TAG, "onDeviceDisconnected Device type : Android-Auto");
            } else {
                LogUtil.debug(TAG, "onDeviceDisconnected Device type : Carplay");
            }
        }

        @Override
        public void onDeviceUpdated(RemoteDevice remoteDevice) throws RemoteException {
            if (remoteDevice.type == RemoteDevice.ANDROID_AUTO) {
                LogUtil.debug(TAG, "Device type : Android-Auto");
            } else {
                LogUtil.debug(TAG, "Device type : CarPlay");
            }
        }

        @Override
        public void onDeviceNotCapable(String s) throws RemoteException {
            LogUtil.debug(TAG, "Device not capable for RUI");
        }
    };

    private final IRemoteSessionCallback.Stub mSessionCallback = new IRemoteSessionCallback.Stub() {
        /**
         * Callback when Native UI is launched.
         */
        @Override
        public void onNativeUILaunch(int state, int remoteUiType) {
            LogUtil.debug(TAG, "onNativeUILaunch : " + state + " Protocol : " + remoteUiType);
        }

        /**
         * Callback gets invoked when Remote session PhoneCall starts/stops.
         */
        @Override
        public void onPhoneStateUpdate(int callState, int remoteUiType) {
            LogUtil.debug(TAG, "onPhoneStateUpdate : callstate " + callState
                    + " Protocol :" + remoteUiType);
        }

        /**
         * Callback gets involked when Remote session Playback starts/stops.
         */
        @Override
        public void onPlayBackUpdate(int playbackState, int remoteUiType) {
            LogUtil.debug(TAG, "onPlayBackUpdate: state " + playbackState
                    + " Protocol :" + remoteUiType);
        }

        /**
         * Callback gets invoked when Remote Screen starts.
         */
        @Override
        public void onScreenStarted(RemoteDevice remoteDevice) {
            LogUtil.debug(TAG, "onScreenStarted:" + "Device : " + remoteDevice.getName());
        }

        /**
         * Callback gets invoked when Remote Screen stops.
         */
        @Override
        public void onScreenStopped(RemoteDevice remoteDevice) {
            LogUtil.debug(TAG, "onScreenStopped: " + "Device : " + remoteDevice.getName());
        }

        /**
         * Callback gets invoked when remote session starts sucessfully.
         */
        @Override
        public void onSessionStarted(RemoteDevice remoteDevice, int launchStatus) {
            LogUtil.debug(TAG, "onSessionStarted: " + "Device : "
                    + remoteDevice.getName() + "No resume session \n"
                    + " launchStatus = " + launchStatus
                    + "  type : " + remoteDevice.type);
            if (remoteDevice.type == RemoteDevice.ANDROID_AUTO && null != mBinder) {
                mHandler.post(() -> {
                    mBinder.unRegisterCallback();
                    mBinder.notifyAAConnectionState(true);
                });
            }
        }

        /**
         * Callback gets invoked when Remote Session ends.
         */
        @Override
        public void onSessionClosed(RemoteDevice remoteDevice) {
            LogUtil.debug(TAG, "onSessionClosed " + "Device : " + remoteDevice.getName()
                    + "  type : " + remoteDevice.type);
            if (remoteDevice.type == RemoteDevice.ANDROID_AUTO && null != mBinder) {
                mHandler.post(() -> {
                    mBinder.notifyAAConnectionState(false);
                    mBinder.registerCallback();
                });
            }
        }

        /**
         * Callback gets invoked when start session fails.
         */
        @Override
        public void onSessionFailed(RemoteDevice remoteDevice) {
            LogUtil.debug(TAG, "onSessionFailed " + "Device : " + remoteDevice.getName()
                    + "  type : " + remoteDevice.type);
            if (remoteDevice.type == RemoteDevice.ANDROID_AUTO && null != mBinder) {
                mHandler.post(() -> {
                    mBinder.notifyAAConnectionState(false);
                    mBinder.registerCallback();
                });
            }
        }

        /**
         * Custom callback API for specific callbacks from specific services.
         */
        @Override
        public void onExtrasChanged(String action, Bundle extras) {
            LogUtil.debug(TAG, "onExtrasChanged " + action);
        }
    };

    /**
     * Bind service for car play and android auto.
     */
    public synchronized void bindAllGoService() {
        Intent intent = new Intent().setComponent(new ComponentName(
                BtConstants.ALLGO_CONTAINER_PACKAGE_NAME, BtConstants.ALLGO_CONTAINER_PACKAGE_URI));
        boolean isConnect = mContext
                .bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        LogUtil.debug(TAG, "bindAllGoService isConnect ->  " + isConnect);
        if (!isConnect) {
            mHandler.postDelayed(this::bindAllGoService, BtConstants.MAX_PROGRESS);
        }
    }
}
