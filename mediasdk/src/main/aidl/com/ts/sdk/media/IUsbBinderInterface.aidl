// IUsbBinderInterface.aidl
package com.ts.sdk.media;

// Declare any non-default types here with import statements
import com.ts.sdk.media.bean.VideoInfoBean;
import com.ts.sdk.media.callback.IUsbVideoCallback;
import com.ts.sdk.media.callback.IUsbDevicesListener;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
interface IUsbBinderInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     List<VideoInfoBean> getVideoInfo(String key);
     List<VideoInfoBean> getAllVideo();
     List<UsbDevicesInfoBean> getUsbDevices();

     void registerVideoStatusObserver(IUsbVideoCallback callback);

     void unRegisterVideoStatusObserver(IUsbVideoCallback callback);

     void registerUsbDevicesStatusObserver(IUsbDevicesListener listener);

     void unRegisterUsbDevicesStatusObserver(IUsbDevicesListener callback);

     /**
     * Pause video.
     */
     boolean pause();

     /**
     * Resume video.
     */
     boolean play();

     /**
     * Play the previous video.
     */
     boolean prev();

     /**
     * Play the next video.
     */
     boolean next();

     boolean getPlayState();

}
