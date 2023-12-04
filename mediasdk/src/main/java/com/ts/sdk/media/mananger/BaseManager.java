package com.ts.sdk.media.mananger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.ts.sdk.media.constants.ServiceConstants;

public abstract class BaseManager {

    private static final String TAG = BaseManager.class.getSimpleName();
    private static final int MSG_RETRY_BIND_SERVICE = 0;
    private static final int TIME_RETRY_BIND_SERVICE = 2000;
    private static final String RETRY = "media_retry_thread";
    private final Context mContext;
    private ServiceConnection mServiceConnection;
    private IBinder mBinder;
    private boolean mBinderResult;
    private boolean mServiceConnected;
    private RetryHandler mRetryHandler;
    private HandlerThread mRetryHandlerThread;

    protected BaseManager(Context context) {
        mContext = context.getApplicationContext();
        if (mRetryHandlerThread == null) {
            mRetryHandlerThread = new HandlerThread(RETRY);
            mRetryHandlerThread.start();
        }
        if (mRetryHandler == null) {
            mRetryHandler = new RetryHandler(mRetryHandlerThread.getLooper());
        }
        if (!mBinderResult) {
            bindService();
        }
    }

    protected void bindService() {
        Log.d(TAG, "BindService mBinderResult##########");
        Intent intent = new Intent();
        intent.setAction(getAction());
        Log.d(TAG, "getAction: " + getAction());
        intent.setClassName(ServiceConstants.SERVICE_PACKAGE, ServiceConstants.SERVICE_CLASS_NAME);
        Log.d(TAG, "getAction: " + getAction());
        Log.d(TAG, "mContext: " + mContext);
        if (mContext != null) {
            mBinderResult = mContext.bindService(intent, getConnection(), Context.BIND_AUTO_CREATE);
            Log.d(TAG, "######Service mBinderResult##" + mBinderResult);
            if (!mBinderResult) {
                if (mRetryHandler.hasMessages(MSG_RETRY_BIND_SERVICE)) {
                    mRetryHandler.removeMessages(MSG_RETRY_BIND_SERVICE);
                }
                mRetryHandler.sendEmptyMessageDelayed(MSG_RETRY_BIND_SERVICE, TIME_RETRY_BIND_SERVICE);
            }
        }
    }

    protected void unBindService() {
        mContext.unbindService(mServiceConnection);
    }

    protected void setBinder(IBinder binder) {
        mBinder = binder;
    }

    private ServiceConnection getConnection() {
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    if (null != iBinder) {
                        setBinder(iBinder);
                        mServiceConnected = true;
                        try {
                            iBinder.linkToDeath(mDeathRecipient, ServiceConstants.DEATH_RECIPIENT_FLAGS);
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        Log.d(TAG, "Media onServiceConnected: iBinder is null!");
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    setBinder(null);
                    mServiceConnected = false;
                }
            };
        }
        return mServiceConnection;
    }

    //Get action
    protected abstract String getAction();

    //Destroy
    protected abstract void destroy();

    /**
     * Destroy.
     */
    public void release() {
        if (mBinderResult) {
            unBindService();
        }
        mBinderResult = false;
        mServiceConnected = false;
        destroy();
    }

    /**
     * Re-bind Media Service.
     */
    private void reBindService() {
        Log.i(TAG, "bindService-----" + mServiceConnected);
        if (!mServiceConnected) {
            bindService();
        }
    }

    private class RetryHandler extends Handler {
        private RetryHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_RETRY_BIND_SERVICE) {
                Log.i(TAG, "handleMessage-----" + mServiceConnected);
                reBindService();
            }
        }
    }

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.d(TAG, "Binder object suddenly stops service during use...");
            //Cancel death monitoring
            if (null != mBinder) {
                mBinder.unlinkToDeath(mDeathRecipient, ServiceConstants.DEATH_RECIPIENT_FLAGS);
                //Release resources
                mBinder = null;
            }
            //Reconnect service
            bindService();
        }
    };
}
