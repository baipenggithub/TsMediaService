package com.ts.service.media.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usb_video")
public class VideoEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "name")
    private String displayName;

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "duration")
    private int duration;

    /**
     * Constructor.
     */
    public VideoEntity(String displayName, String path, int duration) {
        this.displayName = displayName;
        this.path = path;
        this.duration = duration;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @NonNull
    @Override
    public String toString() {
        return "VideoEntity{"
                + "displayName = " + displayName
                + "path = " + path
                + "duration = " + duration
                + "}";
    }
}
