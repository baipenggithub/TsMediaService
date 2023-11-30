package com.ts.sdk.media.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.ts.sdk.media.constants.MusicConstants;

/**
 * MusicStatus.
 */
public class MusicStatusBean implements Parcelable {
    private long mId;
    private int mPlayerStatus = MusicConstants.PLAYER_STATUS_DEFAULT;
    private String mTitle;
    private String mCover;

    /**
     * Constructor.
     */
    public MusicStatusBean() {
        // TODO MusicStatus
    }

    public MusicStatusBean(int status) {
        mPlayerStatus = status;
    }

    /**
     * Constructor.
     */
    public MusicStatusBean(int status, long musicId) {
        mPlayerStatus = status;
        mId = musicId;
    }

    /**
     * Constructor.
     */
    public MusicStatusBean(int status, String cover) {
        mPlayerStatus = status;
        mCover = cover;
    }

    /**
     * Constructor.
     */
    public MusicStatusBean(long id, int playerStatus, String title, String cover) {
        mId = id;
        mPlayerStatus = playerStatus;
        mTitle = title;
        mCover = cover;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public int getPlayerStatus() {
        return mPlayerStatus;
    }

    public void setPlayerStatus(int playerStatus) {
        mPlayerStatus = playerStatus;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getCover() {
        return mCover;
    }

    public void setCover(String cover) {
        mCover = cover;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeInt(mPlayerStatus);
        dest.writeString(mTitle);
        dest.writeString(mCover);
    }

    protected MusicStatusBean(Parcel in) {
        mId = in.readLong();
        mPlayerStatus = in.readInt();
        mTitle = in.readString();
        mCover = in.readString();
    }

    public static final Creator<MusicStatusBean> CREATOR = new Creator<MusicStatusBean>() {
        @Override
        public MusicStatusBean createFromParcel(Parcel source) {
            return new MusicStatusBean(source);
        }

        @Override
        public MusicStatusBean[] newArray(int size) {
            return new MusicStatusBean[size];
        }
    };
}
