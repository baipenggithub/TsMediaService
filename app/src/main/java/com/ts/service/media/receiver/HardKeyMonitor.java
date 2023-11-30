package com.ts.service.media.receiver;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.saicmotor.sdk.systemsettings.BaseManager;
import com.saicmotor.sdk.systemsettings.ISettingsServiceListener;
import com.saicmotor.sdk.systemsettings.SmartSoundManager;
import com.saicmotor.sdk.systemsettings.consts.SmartSound;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class HardKeyMonitor {

    private static final String TAG = "HardKeyMonitor";
    private static final int INVALID_VALUE = -1;
    private static final int TYPE_PREV_LONG_PRESS = 1;
    private static final int TYPE_NEXT_LONG_PRESS = 2;
    private static final int HARD_EVENT_TYPE_PREV = 87;
    private static final int HARD_EVENT_TYPE_NEXT = 88;
    private static final int DELAY_TIME = 500;
    private static volatile HardKeyMonitor sInstance;
    private Context mContext;
    private List<IKeyEventListener> mKeyEventListener = new ArrayList<>();
    private SmartSoundManager mSmartSoundManager;
    private CarPowerMonitor mCarPowerMonitor;
    private boolean mCurDown = false;
    private boolean mCurLongPress = false;
    private int mCurLongPressType = 0;
    private int mCurVolume;
    private int mCurrentPowerState = -1;

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            LogUtil.debug(TAG, "handleMessage type : " + msg.what);
            if (!isNoStandbyState()) {
                LogUtil.debug(TAG, "KEYCODE handleMessage break by POWER");
                return false;
            }
            if (msg.what == TYPE_PREV_LONG_PRESS) {
                for (IKeyEventListener listener : mKeyEventListener) {
                    listener.mediaFastBack();
                }
                mHandler.sendEmptyMessageDelayed(TYPE_PREV_LONG_PRESS,
                        DELAY_TIME);
            } else {
                for (IKeyEventListener listener : mKeyEventListener) {
                    listener.mediaFastForward();
                }
                mHandler.sendEmptyMessageDelayed(TYPE_NEXT_LONG_PRESS,
                        DELAY_TIME);
            }
            return false;
        }
    });

    /**
     * Get instance.
     */
    public static HardKeyMonitor getInstance(Context context) {
        if (sInstance == null) {
            synchronized (UsbDeviceMonitor.class) {
                if (sInstance == null) {
                    sInstance = new HardKeyMonitor(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Constructor.
     */
    private HardKeyMonitor(Context context) {
        mContext = context;
        mCarPowerMonitor = CarPowerMonitor.getInstance(mContext);
        mCarPowerMonitor.addPowerEventListener(state -> {
            LogUtil.debug(TAG, "CarPowerMonitor state changed : " + state);
            mCurrentPowerState = state;
        });
    }

    /**
     * Initialize listening.
     */
    public void initialization() {
        SmartSoundManager.init(mContext, new ISettingsServiceListener() {
            @Override
            public void onServiceConnected(BaseManager baseManager) {
                LogUtil.debug(TAG, "Smart sound connected");
                mSmartSoundManager = (SmartSoundManager) baseManager;
                if (null != mSmartSoundManager) {
                    mCurVolume = mSmartSoundManager.getVolume(SmartSound.VOL_TYPE_MEDIA);
                    LogUtil.debug(TAG, "mCurVolume : " + mCurVolume);
                }
            }

            @Override
            public void onServiceDisconnected() {
                LogUtil.debug(TAG, "Smart sound not connected");
                mSmartSoundManager = null;
            }
        });
    }


    /**
     * When the focus is lost, clear the hard key fast forward and fast backward operation.
     */
    public void focusOut() {
        if (null != mHandler) {
            if (mCurLongPressType == TYPE_NEXT_LONG_PRESS) {
                for (IKeyEventListener listener : mKeyEventListener) {
                    listener.keyEventUp(HARD_EVENT_TYPE_PREV, false);
                }
            } else if (mCurLongPressType == TYPE_PREV_LONG_PRESS) {
                for (IKeyEventListener listener : mKeyEventListener) {
                    listener.keyEventUp(HARD_EVENT_TYPE_NEXT, false);
                }
            }
            mCurLongPressType = 0;
            mHandler.removeMessages(TYPE_NEXT_LONG_PRESS);
            mHandler.removeMessages(TYPE_PREV_LONG_PRESS);
        }
    }

    public void addKeyEventListener(IKeyEventListener listener) {
        mKeyEventListener.add(listener);
    }

    public void removeKeyEventListener(IKeyEventListener listener) {
        mKeyEventListener.remove(listener);
    }

    private boolean isNoStandbyState() {
        return mCurrentPowerState != MusicConstants.POWER_STAND_BY;
    }

    public interface IKeyEventListener {
        void mediaPrevious();

        void mediaNext();

        void mediaFastForward();

        void mediaFastBack();

        void keyEventUp();

        void keyEventUp(int keyCode, boolean isLongPress);

        void keyEventDown(int keyCode, boolean isLongPress);

        void muteChanged(boolean isMute);

        void muteChanged();

        void resumePlay();
    }
}
