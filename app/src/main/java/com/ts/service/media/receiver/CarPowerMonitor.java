package com.ts.service.media.receiver;


import android.content.Context;

import com.saicmotor.sdk.engmode.BaseManager;
import com.saicmotor.sdk.engmode.SystemSettingsManager;
import com.saicmotor.sdk.engmode.listener.EngModeServiceContract;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class CarPowerMonitor {

    private static final String TAG = "CarPowerMonitor";
    private static volatile CarPowerMonitor sInstance;
    private SystemSettingsManager mSystemSettingsManager;
    private List<IPowerEventListener> mPowerEventListener = new ArrayList<>();
    private int mPowerState = -1;

    /**
     * Get instance.
     */
    public static CarPowerMonitor getInstance(Context context) {
        if (sInstance == null) {
            synchronized (CarPowerMonitor.class) {
                if (sInstance == null) {
                    sInstance = new CarPowerMonitor(context);
                }
            }
        }
        return sInstance;
    }

    private EngModeServiceContract
            .EngModeStartCallBack mVehicleCallback = (type, state, floatStatus) -> {
                if (type == MusicConstants.VEHICLE_STATUS_POWER) {
                    mPowerState = state;
                    LogUtil.debug(TAG, "powerState : " + mPowerState);
                    for (IPowerEventListener listener : mPowerEventListener) {
                        listener.powerStateChanged(state);
                    }

                }
            };

    private CarPowerMonitor(Context context) {
        SystemSettingsManager.init(context, new
            EngModeServiceContract.EngModeServiceListener() {
                @Override
                public void onServiceConnected(BaseManager baseManager) {
                    LogUtil.debug(TAG, "SystemSettingsManager : onServiceConnected");
                    mSystemSettingsManager = (SystemSettingsManager) baseManager;
                    if (mSystemSettingsManager != null) {
                        mPowerState = mSystemSettingsManager.getPowerRunType();
                        for (IPowerEventListener listener : mPowerEventListener) {
                            listener.powerStateChanged(mPowerState);
                        }
                        mSystemSettingsManager.registerEngModeStartCallback(mVehicleCallback);
                    }
                }

                @Override
                public void onServiceDisconnected() {
                    mSystemSettingsManager = null;
                }
            });
    }

    public void addPowerEventListener(IPowerEventListener listener) {
        mPowerEventListener.add(listener);
    }

    public void removePowerEventListener(IPowerEventListener listener) {
        mPowerEventListener.remove(listener);
    }

    public interface IPowerEventListener {
        void powerStateChanged(int state);
    }
}
