// IUsbDevicesListener.aidl
package com.ts.sdk.media.callback;

import com.ts.sdk.media.bean.UsbDevicesInfoBean;

// Declare any non-default types here with import statements

interface IUsbDevicesListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onUsbDevicesChange(in List<UsbDevicesInfoBean> usbDevices);

    void onScanStateChange(int state, String deviceId, int portId);
}
