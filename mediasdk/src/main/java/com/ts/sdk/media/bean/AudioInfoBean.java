package com.ts.sdk.media.bean;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class AudioInfoBean implements Parcelable {
    private long mAudioId;
    private long mAudioDuration;
    private String mAudioName;
    private String mAudioCover;
    private String mAudioPath;
    private String mAudioArtistName;
    private String mAudioAlbumName;
    private String mUserId;
    private String mAvatar;
    private long mAudioSize;
    private String mAudioGenre;
    private String mAudioTrackNumber;
    private String mAudioPlayTime;
    private String mAudioType;
    private int mBtAudioType;
    private String mAudioDescribe;
    private long mAddTime;
    private long mLastPlayTime;
    private int mPlayState;
    private String mSortLetters;
    private int mPlayerType;
    private Bitmap mCpAlbumArt;
    /**
     * Constant indicating the item type of track.
     */
    public static final int ITEM_TYPE_TRACK = 1;

    /**
     * Constant indicating the item type of album.
     */
    public static final int ITEM_TYPE_ALBUM = 2;

    /**
     * Constant indicating the item type of artist.
     */
    public static final int ITEM_TYPE_ARTIST = 3;

    /**
     * Constant indicating the item type of playlist.
     */
    public static final int ITEM_TYPE_PLAYLIST = 4;

    public long getAudioId() {
        return mAudioId;
    }

    public void setAudioId(long audioId) {
        mAudioId = audioId;
    }

    public long getAudioDuration() {
        return mAudioDuration;
    }

    public void setAudioDuration(long audioDuration) {
        mAudioDuration = audioDuration;
    }

    public String getAudioName() {
        return mAudioName;
    }

    public void setAudioName(String audioName) {
        mAudioName = audioName;
    }

    public String getAudioCover() {
        return mAudioCover;
    }

    public void setAudioCover(String audioCover) {
        mAudioCover = audioCover;
    }

    public String getAudioPath() {
        return mAudioPath;
    }

    public void setAudioPath(String audioPath) {
        mAudioPath = audioPath;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String avatar) {
        mAvatar = avatar;
    }

    public long getAudioSize() {
        return mAudioSize;
    }

    public void setAudioSize(long audioSize) {
        mAudioSize = audioSize;
    }

    public String getAudioAlbumName() {
        return mAudioAlbumName;
    }

    public void setAudioAlbumName(String audioAlbumName) {
        mAudioAlbumName = audioAlbumName;
    }

    public String getAudioArtistName() {
        return mAudioArtistName;
    }

    public void setAudioArtistName(String audioArtistName) {
        mAudioArtistName = audioArtistName;
    }

    public String getAudioGenre() {
        return mAudioGenre;
    }

    public void setAudioGenre(String audioGenre) {
        mAudioGenre = audioGenre;
    }

    public String getAudioTrackNumber() {
        return mAudioTrackNumber;
    }

    public void setAudioTrackNumber(String audioTrackNumber) {
        mAudioTrackNumber = audioTrackNumber;
    }

    public String getAudioPlayTime() {
        return mAudioPlayTime;
    }

    public void setAudioPlayTime(String audioPlayTime) {
        mAudioPlayTime = audioPlayTime;
    }

    public String getAudioType() {
        return mAudioType;
    }

    public void setAudioType(String audioType) {
        mAudioType = audioType;
    }

    public String getAudioDescribe() {
        return mAudioDescribe;
    }

    public void setAudioDescribe(String audioDescribe) {
        mAudioDescribe = audioDescribe;
    }

    public long getAddTime() {
        return mAddTime;
    }

    public void setAddTime(long addTime) {
        mAddTime = addTime;
    }

    public long getLastPlayTime() {
        return mLastPlayTime;
    }

    public void setLastPlayTime(long lastPlayTime) {
        mLastPlayTime = lastPlayTime;
    }

    public int getBtAudioType() {
        return mBtAudioType;
    }

    public void setBtAudioType(int btAudioType) {
        mBtAudioType = btAudioType;
    }

    public int getPlayState() {
        return mPlayState;
    }

    public void setPlayState(int state) {
        mPlayState = state;
    }

    public String getSortLetters() {
        return mSortLetters;
    }

    public void setSortLetters(String sortLetters) {
        mSortLetters = sortLetters;
    }

    public int getPlayerType() {
        return mPlayerType;
    }

    public void setPlayerType(int type) {
        mPlayerType = type;
    }

    public Bitmap getCpAlbumArt() {
        return mCpAlbumArt;
    }

    public void setCpAlbumArt(Bitmap cpAlbumArt) {
        mCpAlbumArt = cpAlbumArt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mAudioId);
        dest.writeLong(mAudioDuration);
        dest.writeString(mAudioName);
        dest.writeString(mAudioCover);
        dest.writeString(mAudioPath);
        dest.writeString(mAudioArtistName);
        dest.writeString(mUserId);
        dest.writeString(mAvatar);
        dest.writeLong(mAudioSize);
        dest.writeString(mAudioAlbumName);
        dest.writeString(mAudioType);
        dest.writeString(mAudioDescribe);
        dest.writeLong(mAddTime);
        dest.writeLong(mLastPlayTime);
        dest.writeString(mAudioGenre);
        dest.writeString(mAudioTrackNumber);
        dest.writeString(mAudioPlayTime);
        dest.writeString(mAudioType);
        dest.writeInt(mBtAudioType);
        dest.writeInt(mPlayState);
        dest.writeString(mSortLetters);
        dest.writeInt(mPlayerType);
    }

    public AudioInfoBean() {
    }

    protected AudioInfoBean(Parcel in) {
        mAudioId = in.readLong();
        mAudioDuration = in.readLong();
        mAudioName = in.readString();
        mAudioCover = in.readString();
        mAudioPath = in.readString();
        mAudioArtistName = in.readString();
        mUserId = in.readString();
        mAvatar = in.readString();
        mAudioSize = in.readLong();
        mAudioAlbumName = in.readString();
        mAudioType = in.readString();
        mAudioDescribe = in.readString();
        mAddTime = in.readLong();
        mLastPlayTime = in.readLong();
        mAudioGenre = in.readString();
        mAudioTrackNumber = in.readString();
        mAudioPlayTime = in.readString();
        mAudioType = in.readString();
        mBtAudioType = in.readInt();
        mPlayState = in.readInt();
        mSortLetters = in.readString();
        mPlayerType = in.readInt();
    }

    public static final Creator<AudioInfoBean> CREATOR = new Creator<AudioInfoBean>() {
        @Override
        public AudioInfoBean createFromParcel(Parcel source) {
            return new AudioInfoBean(source);
        }

        @Override
        public AudioInfoBean[] newArray(int size) {
            return new AudioInfoBean[size];
        }
    };

    @Override
    public String toString() {
        return "AudioInfoBean{"
                + "mAudioId=" + mAudioId
                + ", mAudioDuration=" + mAudioDuration
                + ", mAudioName='" + mAudioName + '\''
                + ", mAudioCover='" + mAudioCover + '\''
                + ", mAudioPath='" + mAudioPath + '\''
                + ", mAudioArtistName='" + mAudioArtistName + '\''
                + ", mAudioAlbumName='" + mAudioAlbumName + '\''
                + ", mUserId='" + mUserId + '\''
                + ", mAvatar='" + mAvatar + '\''
                + ", mAudioSize=" + mAudioSize
                + ", mAudioGenre='" + mAudioGenre + '\''
                + ", mAudioTrackNumber='" + mAudioTrackNumber + '\''
                + ", mAudioPlayTime='" + mAudioPlayTime + '\''
                + ", mAudioType='" + mAudioType + '\''
                + ", mBtAudioType=" + mBtAudioType
                + ", mAudioDescribe='" + mAudioDescribe + '\''
                + ", mAddTime=" + mAddTime
                + ", mLastPlayTime=" + mLastPlayTime
                + ", mPlayState=" + mPlayState
                + ", mSortLetters='" + mSortLetters
                + ", mPlayerType=" + mPlayerType + '\'' + '}';
    }
}
