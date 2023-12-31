package com.ts.service.media.presenter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.ts.sdk.media.constants.BtConstants;
import com.ts.service.media.utils.LogUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public final class BtManager {

    private static final String TAG = "BtManager";
    private final Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBtDevice;
    private static BtManager mBtManager = null;
    private static final int A2DP_SINK = 11;
    private static final int AVRCP_CONTROLLER = 12;
    private String selectedAddress = "00:00:00:00:00:11";
    private BluetoothA2dpSink mBluetoothA2dpSinkProxy;
    private BluetoothAvrcpController mBtAvrcpContrller;

    private BtManager(Context context) {
        mContext = context;
    }

    /**
     * BtManager singleInstance.
     *
     * @param context context
     * @return BtManager instance
     */
    public static synchronized BtManager getInstance(Context context) {
        if (null == mBtManager) {
            mBtManager = new BtManager(context);
        }
        return mBtManager;
    }

    /**
     * Init bt adapter.
     */
    public void initBtAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getProfileProxy(mContext, mServiceListener, AVRCP_CONTROLLER);
            mBluetoothAdapter.getProfileProxy(mContext, mServiceListener, A2DP_SINK);
        }
    }

    private final BluetoothProfile.ServiceListener mServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceDisconnected(int profile) {
                    if (profile == AVRCP_CONTROLLER) {
                        LogUtil.debug(TAG, "mBtAvrcpContrller is null");
                        mBtAvrcpContrller = null;
                    }
                    if (profile == A2DP_SINK) {
                        LogUtil.debug(TAG, "mBluetoothA2dpSinkProxy is null");
                        mBluetoothA2dpSinkProxy = null;
                    }
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    LogUtil.debug(TAG, "onServiceConnected");
                    if (profile == AVRCP_CONTROLLER) {
                        LogUtil.debug(TAG, "onServiceConnected : AVRCP_CONTROLLER");
                        mBtAvrcpContrller = (BluetoothAvrcpController) proxy;
                        mBtDevice = getConnectedDevice();
                    }
                    if (profile == A2DP_SINK) {
                        LogUtil.debug(TAG, "onServiceConnected : A2DP_SINK");
                        mBluetoothA2dpSinkProxy = (BluetoothA2dpSink) proxy;
                        List<BluetoothDevice> list = mBluetoothA2dpSinkProxy.getConnectedDevices();
                        LogUtil.debug(TAG, "devices list size = " + list.size());
                        if (list.size() > 0) {
                            BluetoothDevice bluetoothDevice = list.get(0);
                            LogUtil.debug(TAG, "devices name  = " + bluetoothDevice.getName() + " , address " + bluetoothDevice.getAddress());
                            selectedAddress = bluetoothDevice.getAddress();
                            LogUtil.debug(TAG, "selectedAddress" + selectedAddress);
                        }
                    }
                }
            };

    /**
     * Get connected device.
     *
     * @return BluetoothDevice
     */
    @SuppressLint("MissingPermission")
    public BluetoothDevice getConnectedDevice() {
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            LogUtil.debug(TAG, "getConnectedDevice===>" + device);
            Method isConnectedMethod;
            try {
                isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                isConnectedMethod.setAccessible(true);
                boolean isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
                if (isConnected && getBluetoothState() == BluetoothAdapter.STATE_CONNECTED) {
                    return device;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return null;
    }

    /**
     * Get bluetooth state.
     *
     * @return ret state
     */
    @SuppressLint("MissingPermission")
    public int getBluetoothState() {
        if (mBluetoothAdapter != null) {
            int a2dpState = mBluetoothAdapter.getProfileConnectionState(A2DP_SINK);
            LogUtil.debug(TAG, "a2dp state : " + a2dpState);
            if (a2dpState == BluetoothProfile.STATE_CONNECTED) {
                return BtConstants.BT_STATE_CONNECTED;
            } else if (a2dpState == BluetoothProfile.STATE_DISCONNECTED) {
                return BtConstants.BT_STATE_DISCONNECTED;
            }
        }
        return BtConstants.BT_STATE_DISCONNECTED;
    }

    /**
     * Whether avrcp is connected.
     *
     * @return ret state
     */
    @SuppressLint("MissingPermission")
    public boolean isAvrcpConnection() {
        if (mBluetoothAdapter != null) {
            int a2dpState = mBluetoothAdapter.getProfileConnectionState(AVRCP_CONTROLLER);
            LogUtil.debug(TAG, "avrcp state : " + a2dpState);
            if (a2dpState == BluetoothProfile.STATE_CONNECTED) {
                return true;
            } else if (a2dpState == BluetoothProfile.STATE_DISCONNECTED) {
                return false;
            }
        }
        return false;
    }

    /**
     * Get player setting.
     *
     * @return BluetoothAvrcpPlayerSettings settings.
     */
    public BluetoothAvrcpPlayerSettings getPlayerSettings() {
        if (mBtAvrcpContrller == null || mBtDevice == null) {
            LogUtil.debug(TAG, "Player Settings is null ");
            return null;
        }
        return mBtAvrcpContrller.getPlayerSettings(mBtDevice);
    }

    /**
     * Set player setting.
     *
     * @param type  set type.
     * @param value set value.
     */
    public boolean setAvrcpSettings(int type, int value) {
        LogUtil.debug(TAG, "setAvrcpSettings type=" + type + "---value=" + value);
        BluetoothAvrcpPlayerSettings settings = new BluetoothAvrcpPlayerSettings(type);
        settings.addSettingValue(type, value);
        return mBtAvrcpContrller.setPlayerApplicationSetting(settings);
    }
}
