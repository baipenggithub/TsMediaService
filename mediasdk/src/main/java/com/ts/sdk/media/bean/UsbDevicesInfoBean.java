package com.ts.sdk.media.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class UsbDevicesInfoBean implements Parcelable {
    private String mUuid;
    private String mLabel;
    private String mPort;
    private String mPath;
    private int mScanState;

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getPort() {
        return mPort;
    }

    public void setPort(String port) {
        mPort = port;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public int getScanState() {
        return mScanState;
    }

    public void setScanState(int state) {
        mScanState = state;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUuid);
        dest.writeString(mLabel);
        dest.writeString(mPort);
        dest.writeString(mPath);
        dest.writeInt(mScanState);
    }

    public UsbDevicesInfoBean() {
        // Do nothing
    }

    protected UsbDevicesInfoBean(Parcel in) {
        mUuid = in.readString();
        mLabel = in.readString();
        mPort = in.readString();
        mPath = in.readString();
        mScanState = in.readInt();
    }

    public static final Creator<UsbDevicesInfoBean> CREATOR = new Creator<UsbDevicesInfoBean>() {
        @Override
        public UsbDevicesInfoBean createFromParcel(Parcel source) {
            return new UsbDevicesInfoBean(source);
        }

        @Override
        public UsbDevicesInfoBean[] newArray(int size) {
            return new UsbDevicesInfoBean[size];
        }
    };
}
