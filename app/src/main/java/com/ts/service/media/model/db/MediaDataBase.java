package com.ts.service.media.model.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.ts.service.media.constants.VideoConstants;
import com.ts.service.media.model.dao.VideoDao;
import com.ts.service.media.model.entity.VideoEntity;

@Database(entities = {VideoEntity.class}, version = 1, exportSchema = false)
public abstract class MediaDataBase extends RoomDatabase {
    private static final String TAG = "MediaDataBase";

    public abstract VideoDao videoDao();

    private static MediaDataBase sInstance = null;

    /**
     * get LauncherDatabase Singleton.
     *
     * @param context nonNull
     * @return LauncherDatabase
     */
    public static MediaDataBase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(context, MediaDataBase.class, VideoConstants.DB_NAME)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Log.d(TAG, "Create database:" + db.getPath());
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            Log.d(TAG, "open database:" + db.getPath());
                        }
                    }).allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
        }
        return sInstance;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // TODO  data migration when db upgrade
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // TODO  data migration when db upgrade
        }
    };
}
