package com.ts.service.media.model.entity;

import com.ts.service.media.constants.MusicConstants;

public class RecordAudioInfo {
    private String mUuid;
    private String mPath;
    private int mProgress;
    private int mPlayerState;
    private int mCurrentIndex;
    private int mPlayMode = MusicConstants.MUSIC_MODE_LOOP;
    private long mCurrentTime;
    private long mAudioId;

    public long getAudioId() {
        return mAudioId;
    }

    public void setAudioId(long audioId) {
        mAudioId = audioId;
    }

    public long getCurrentTime() {
        return mCurrentTime;
    }

    public void setCurrentTime(long currentTime) {
        mCurrentTime = currentTime;
    }

    public int getPlayMode() {
        return mPlayMode;
    }

    public void setPlayMode(int playMode) {
        mPlayMode = playMode;
    }

    public int getPlayerState() {
        return mPlayerState;
    }

    public void setPlayerState(int playerState) {
        mPlayerState = playerState;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        mCurrentIndex = currentIndex;
    }

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        mProgress = progress;
    }

    @Override
    public String toString() {
        return "RecordAudioInfo{"
                + "mUuid='" + mUuid + '\''
                + ", mPath='" + mPath + '\''
                + ", mProgress=" + mProgress
                + ", mPlayerState=" + mPlayerState
                + ", mCurrentIndex=" + mCurrentIndex
                + ",currentTime=" + mCurrentTime
                + '}';
    }
}
