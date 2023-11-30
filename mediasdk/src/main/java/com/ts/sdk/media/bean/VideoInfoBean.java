package com.ts.sdk.media.bean;

import static android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME;
import static android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID;
import static android.provider.DocumentsContract.Document.COLUMN_FLAGS;
import static android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED;
import static android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE;
import static android.provider.DocumentsContract.Document.COLUMN_SIZE;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.ts.sdk.media.constants.VideoConstants;

public class VideoInfoBean implements Parcelable {

    private String mAuthority;
    private String mDocumentId;
    private String mMimeType;
    private String mDisplayName;
    private long mLastModified;
    private int mFlags;
    private int mDuration;
    private long mSize;
    private Uri mUri;
    private String mPath;
    private String mUsbName;
    private int mLastPlaytime;
    private String mVideoThumbnails;

    public VideoInfoBean() {
    }

    public VideoInfoBean(String title, Uri uri) {
        mDisplayName = title;
        mUri = uri;
    }

    private VideoInfoBean(Parcel videoInfo) {
        Bundle bundle = videoInfo.readBundle();
        mDocumentId = bundle.getString(COLUMN_DOCUMENT_ID);
        mMimeType = bundle.getString(COLUMN_MIME_TYPE);
        mDisplayName = bundle.getString(COLUMN_DISPLAY_NAME);
        mLastModified = bundle.getLong(COLUMN_LAST_MODIFIED);
        mFlags = bundle.getInt(COLUMN_FLAGS);
        mSize = bundle.getLong(COLUMN_SIZE);
        mUri = bundle.getParcelable(VideoConstants.COLUMN_URI);
        mDuration = videoInfo.readInt();
        mPath = videoInfo.readString();
        mUsbName = videoInfo.readString();
        mLastPlaytime = videoInfo.readInt();
        mVideoThumbnails = videoInfo.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int isBeWritten) {
        Bundle bundle = new Bundle();
        bundle.putString(COLUMN_DOCUMENT_ID, mDocumentId);
        bundle.putString(COLUMN_MIME_TYPE, mMimeType);
        bundle.putString(COLUMN_DISPLAY_NAME, mDisplayName);
        bundle.putLong(COLUMN_LAST_MODIFIED, mLastModified);
        bundle.putInt(COLUMN_FLAGS, mFlags);
        bundle.putLong(COLUMN_SIZE, mSize);
        bundle.putParcelable(VideoConstants.COLUMN_URI, mUri);
        parcel.writeBundle(bundle);
        parcel.writeInt(mDuration);
        parcel.writeString(mPath);
        parcel.writeString(mUsbName);
        parcel.writeInt(mLastPlaytime);
        parcel.writeString(mVideoThumbnails);
    }
    @Override
    public String toString() {
        return "UsbVideoInfoEntity {"
                + "authority=" + mAuthority
                + ", mDocumentId=" + mDocumentId
                + ", mDisplayName=" + mDisplayName
                + ", mLastModified=" + mLastModified
                + ", mMimeType=" + mMimeType
                + "} @ "
                + getUri();
    }

    public static final Creator<VideoInfoBean> CREATOR = new Creator<VideoInfoBean>() {
        @Override
        public VideoInfoBean createFromParcel(Parcel inParcel) {
            VideoInfoBean document = new VideoInfoBean(inParcel);
            return document;
        }

        @Override
        public VideoInfoBean[] newArray(int size) {
            return new VideoInfoBean[size];
        }
    };

    public void setDocumentId(String documentId) {
        mDocumentId = documentId;
    }

    public String getDocumentId() {
        return mDocumentId;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public void setLastModified(long lastModified) {
        mLastModified = lastModified;
    }

    private long getLastModified() {
        return mLastModified;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public String getUsbName() {
        return mUsbName;
    }

    public void setUsbName(String usbName) {
        mUsbName = usbName;
    }

    public int getLastPlaytime() {
        return mLastPlaytime;
    }

    public void setLastPlaytime(int lastPlaytime) {
        mLastPlaytime = lastPlaytime;
    }

    public String getVideoThumbnails() {
        return mVideoThumbnails;
    }

    public void setVideoThumbnails(String url) {
        mVideoThumbnails = url;
    }
}
