package com.ts.service.media.receiver;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.constants.VideoConstants;
import com.ts.service.media.utils.LogUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UsbDeviceMonitor {

    private static final String TAG = "UsbDeviceMonitor";
    private static volatile UsbDeviceMonitor sInstance;
    private static final String USB_MUSIC_PACKAGE_NAME = "com.ts.hmi.music";
    private static final String USB_VIDEO_PACKAGE_NAME = "com.ts.hmi.video";

    private final UsbManager mUsbManager;
    private Context mContext;
    private UsbBroadcastReceiver mUsbBroadcastReceiver;
   // private UsbScannerManager mUsbScannerManager;
   // private IUsbScannerEventListener.Stub mScannerEventListener;
    private List<UsbDeviceListener> mListenerList = new ArrayList<>();
    private List<UsbDevicesInfoBean> mDevicesList = new ArrayList<>();

    public List<UsbDevicesInfoBean> getUsbDevices() {
        return mDevicesList;
    }

    /**
     * Get UsbDeviceMonitor instance.
     *
     * @param context context
     * @return instance
     */
    public static UsbDeviceMonitor getInstance(Context context) {
        if (sInstance == null) {
            synchronized (UsbDeviceMonitor.class) {
                if (sInstance == null) {
                    sInstance = new UsbDeviceMonitor(context);
                }
            }
        }
        return sInstance;
    }

    public interface UsbDeviceListener {
        void onDeviceChange(List<UsbDevicesInfoBean> usbDevices);

        void onScanChange(int state, String deviceId, int portId);
    }

    /**
     * Set listener for usb device attached/detached.
     *
     * @param listener listener
     */
    public void setUsbDeviceListener(UsbDeviceListener listener) {
        if (!mListenerList.contains(listener)) {
            mListenerList.add(listener);
        }
    }

    private UsbDeviceMonitor(Context context) {
        mContext = context;
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
      //  mUsbScannerManager = new UsbScannerManager(context);
    }

    /**
     * Initialization of UsbDeviceMonitor.
     */
    public void initialization() {
        mUsbBroadcastReceiver = new UsbBroadcastReceiver();
        IntentFilter mediaFilter = new IntentFilter();
        mediaFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mediaFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        mediaFilter.addDataScheme(VideoConstants.VIDEO_DATA_SCHEME);
        mContext.registerReceiver(mUsbBroadcastReceiver, mediaFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(VideoConstants.ACTION_USB_PERMISSION);
        mContext.registerReceiver(mUsbBroadcastReceiver, filter);
//
//        mScannerEventListener = new IUsbScannerEventListener.Stub() {
//
//            @Override
//            public void onMountReceived(int type, String deviceId, int portId) {
//                LogUtil.debug(TAG, "onMountReceived type : " + type + "deviceId : " + deviceId + "portId : " + portId);
//                if (mUsbScannerManager != null) {
//                    LogUtil.debug(TAG, "onMountReceived ComponentName : ");
//                    mUsbScannerManager.setPriority(UsbScannerEvent.TYPE_USB, 1, UsbScannerEvent.SCAN_AUDIO);
//                }
//            }
//
//            @Override
//            public void onUnMountReceived(int type, String deviceId, int portId) {
//                LogUtil.debug(TAG, "onUnMountReceived type : " + type
//                        + "deviceId : " + deviceId + "portId : " + portId);
//            }
//
//            @Override
//            public void onMediaSyncStarted(int type, String deviceId, int portId) {
//                for (UsbDevicesInfoBean devicesInfoBean : mDevicesList) {
//                    if (devicesInfoBean.getUuid().equals(deviceId)) {
//                        devicesInfoBean.setScanState(ServiceConstants.MEDIA_SCANNER_STARTED);
//                    }
//                }
//                for (UsbDeviceListener listener : mListenerList) {
//                    listener.onScanChange(ServiceConstants.MEDIA_SCANNER_STARTED, deviceId, portId);
//                }
//                LogUtil.debug(TAG, "onMediaSyncStarted type : " + type
//                        + "deviceId : " + deviceId + "portId : " + portId);
//            }
//
//            @Override
//            public void onMediaSyncInProgress(int type, String deviceId,
//                                              int portId, UsbScannerResult result) {
//                // TODO nothing
//            }
//
//            @Override
//            public void onMediaSyncCompleted(int type, String deviceId, int portId) {
//                for (UsbDevicesInfoBean devicesInfoBean : mDevicesList) {
//                    if (devicesInfoBean.getUuid().equals(deviceId)) {
//                        devicesInfoBean.setScanState(ServiceConstants.MEDIA_SCANNER_FINISHED);
//                    }
//                }
//                for (UsbDeviceListener listener : mListenerList) {
//                    listener.onScanChange(ServiceConstants.MEDIA_SCANNER_FINISHED,
//                            deviceId, portId);
//                }
//                MediaScannerFile.getInstance(mContext).scanCompleted();
//                LogUtil.debug(TAG, "onMediaSyncCompleted type : " + type
//                        + "deviceId : " + deviceId + "portId : " + portId);
//            }
//        };
//        mUsbScannerManager.registerListener(mScannerEventListener);
        mDevicesList = scanUsbUuid(null, mDevicesList);
        for (UsbDevicesInfoBean devicesInfoBean : mDevicesList) {
            devicesInfoBean.setScanState(ServiceConstants.MEDIA_SCANNER_FINISHED);
        }
    }

    /**
     * The closing handler.
     */
    public void close() {
        if (mUsbBroadcastReceiver != null) {
            mContext.unregisterReceiver(mUsbBroadcastReceiver);
        }
       // mUsbScannerManager.unregisterListener(mScannerEventListener);
    }

    /**
     * BroadcastReceiver to monitor USB.
     */
    private class UsbBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                LogUtil.debug(TAG, "ACTION_MEDIA_MOUNTED");
                String uuid = getDeviceId(String.valueOf(intent.getData()));
                LogUtil.debug(TAG, "ACTION_MEDIA_MOUNTED :: uuid :" + uuid);
                mDevicesList = scanUsbUuid(uuid, mDevicesList);
                for (UsbDeviceListener listener : mListenerList) {
                    listener.onDeviceChange(mDevicesList);
                }
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                LogUtil.debug(TAG, "ACTION_MEDIA_UNMOUNTED");
                String uuid = getDeviceId(String.valueOf(intent.getData()));
                for (UsbDevicesInfoBean infoBean : mDevicesList) {
                    if (uuid.equals(infoBean.getUuid())) {
                        removeDevices(uuid);
                    }
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                LogUtil.debug(TAG, "ACTION_MEDIA_SCANNER_STARTED :" + intent.getData());
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                LogUtil.debug(TAG, "ACTION_MEDIA_SCANNER_FINISHED :" + intent.getData());
            } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                String uuid = getDeviceId(String.valueOf(intent.getData()));
                LogUtil.debug(TAG, "ACTION_MEDIA_EJECT : " + intent.getData() + ", uuid : " + uuid);
                removeDevices(uuid);
            } else {
                final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device == null) {
                    LogUtil.error(TAG, "UsbBroadcastReceiver, usb device is null");
                    return;
                }

                switch (action) {
                    case VideoConstants.ACTION_USB_PERMISSION:
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            // TODO
                        } else {
                            LogUtil.error(TAG, "Failed to get the permission of USB Device !");
                        }
                        break;
                    case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                        LogUtil.debug(TAG, "ACTION_USB_DEVICE_ATTACHED  " + device.toString());
                        break;
                    case UsbManager.ACTION_USB_DEVICE_DETACHED:
                        LogUtil.debug(TAG, "ACTION_USB_DEVICE_DETACHED  " + device.toString());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Get the port number according to the current device UUID,
     * and clear all devices of the port number.
     */
    private void removeDevices(String uuid) {
        LogUtil.debug(TAG, "removeDevices :: invoke ");
        if (!TextUtils.isEmpty(uuid) && null != mDevicesList && mDevicesList.size() > 0) {
            LogUtil.debug(TAG, "removeDevices :: do ");
            String port = "";
            for (UsbDevicesInfoBean infoBean : mDevicesList) {
                if (uuid.equals(infoBean.getUuid())) {
                    port = infoBean.getPort();
                }
            }
            if (!TextUtils.isEmpty(port)) {
                LogUtil.debug(TAG, "removeDevices :: has port ");
                Iterator<UsbDevicesInfoBean> iterator = mDevicesList.iterator();
                for (; iterator.hasNext(); ) {
                    UsbDevicesInfoBean deviceInfo = iterator.next();
                    if (port.equals(deviceInfo.getPort())) {
                        iterator.remove();
                    }
                }
                for (UsbDeviceListener listener : mListenerList) {
                    listener.onDeviceChange(mDevicesList);
                }
            }
        } else {
            LogUtil.debug(TAG, "removeDevices :: null  ");
            for (UsbDeviceListener listener : mListenerList) {
                listener.onDeviceChange(mDevicesList);
            }
        }
    }

    private String getDeviceId(String path) {
        return path.substring(path.lastIndexOf(VideoConstants.FILE_SEPARATOR)
                + VideoConstants.UUID_INDEX);
    }

    /**
     * Check the type of usb device.
     *
     * @param device    usb device.
     * @param connected whether is connected.
     */
    private void checkUsbDevice(UsbDevice device, boolean connected) {
        if (!mUsbManager.hasPermission(device)) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
                    VideoConstants.REQUEST_CODE, new Intent(VideoConstants.ACTION_USB_PERMISSION),
                    FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            mUsbManager.requestPermission(device, pendingIntent);
            return;
        }
        int deviceClass = device.getDeviceClass();
        int interfaceClass = device.getInterface(0).getInterfaceClass();
        if (UsbConstants.USB_CLASS_PER_INTERFACE == deviceClass) {
            String authority = "";
            if (UsbConstants.USB_CLASS_MASS_STORAGE == interfaceClass) {
                authority = VideoConstants.AUTHORITY_STORAGE;
            }
            if (connected && !authority.isEmpty()) {
                Uri rootsUri = DocumentsContract.buildRootsUri(authority);
                mContext.getContentResolver().notifyChange(rootsUri, null);
            }
        }
    }

    /**
     * Scan usb devices.
     */
    public synchronized List<UsbDevicesInfoBean> scanUsbUuid(String targetUuid,
                                                             List<UsbDevicesInfoBean> oldList) {
        LogUtil.debug(TAG, "scanUsbUuid :: invoke :: targetUuid : " + targetUuid);
        try {
            List<UsbDevicesInfoBean> devicesList = new ArrayList<>();
            if (oldList != null) {
                devicesList.addAll(oldList);
            }
            StorageManager storageManager = mContext.getSystemService(StorageManager.class);
            List<StorageVolume> list = storageManager.getStorageVolumes();
            Class smClass = Class.forName(storageManager.getClass().getName());
            Method getVolumes = smClass.getDeclaredMethod("getVolumes", (Class<?>[]) null);
            getVolumes.setAccessible(true);
            List<VolumeInfo> volumeInfos = (List<VolumeInfo>) getVolumes.invoke(storageManager);
            String sysPath = "";
            String port = "";
            if (volumeInfos != null) {
                for (VolumeInfo volumeInfo : volumeInfos) {
                    UsbDevicesInfoBean devicesInfoBean = new UsbDevicesInfoBean();
                    String uuid = volumeInfo.getFsUuid();
                    if (volumeInfo.getPath() == null) {
                        continue;
                    }
                    String path = volumeInfo.getPath().getAbsolutePath();
                    String label = volumeInfo.getDescription();
                    devicesInfoBean.setPath(path);
                    devicesInfoBean.setUuid(uuid);
                    devicesInfoBean.setLabel(label);
                    DiskInfo disk = volumeInfo.getDisk();
                    if (disk != null) {
                        sysPath = disk.sysPath;
                        port = sysPath.substring(VideoConstants.DEVICE_PORT_START,
                                VideoConstants.DEVICE_PORT_END);
                    }
                    devicesInfoBean.setPort(port);
                    LogUtil.debug(TAG, "uuid: " + uuid + "  label: " + label
                            + "  path: " + path + "  port: " + port);
                    if (targetUuid != null) {
                        if (TextUtils.isEmpty(devicesInfoBean.getUuid())
                                || !devicesInfoBean.getUuid().equals(targetUuid)) {
                            continue;
                        }
                    }
                    if (!TextUtils.isEmpty(devicesInfoBean.getPort())
                            && (devicesInfoBean.getPort().equals(MusicConstants.USB_1_PORT)
                            || devicesInfoBean.getPort().equals(MusicConstants.USB_2_PORT))) {
                        if (devicesList.size() > 0) {
                            boolean needAdd = true;
                            for (UsbDevicesInfoBean usbDevicesInfoBean : devicesList) {
                                if (devicesInfoBean != null
                                        && !TextUtils.isEmpty(devicesInfoBean.getPort())
                                        && usbDevicesInfoBean != null
                                        && !TextUtils.isEmpty(usbDevicesInfoBean.getPort())
                                        && devicesInfoBean.getPort()
                                        .equals(usbDevicesInfoBean.getPort())) {
                                    needAdd = false;
                                }
                            }
                            if (needAdd) {
                                devicesList.add(devicesInfoBean);
                                LogUtil.debug(TAG, "scanUsbUuid :: "
                                        + "devicesList.size() > 0:: add uuid : " + uuid);
                            }
                        } else {
                            devicesList.add(devicesInfoBean);
                            LogUtil.debug(TAG, "scanUsbUuid ::  add uuid : " + uuid);
                        }
                    }
                }
                for (UsbDevicesInfoBean newDevice : devicesList) {
                    for (UsbDevicesInfoBean oldDevice : mDevicesList) {
                        if (newDevice.getUuid().equals(oldDevice.getUuid())) {
                            newDevice.setScanState(oldDevice.getScanState());
                        }
                    }
                }
                return devicesList;
            }
        } catch (ClassNotFoundException | NoSuchMethodException
                 | InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
        return mDevicesList;
    }
}
