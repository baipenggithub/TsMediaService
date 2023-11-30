package com.ts.service.media.model.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface VideoDao {
    @Query("DELETE FROM usb_video")
    void deleteAll();
}
