package com.ts.service.media.receiver;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.ts.service.media.utils.MediaScannerFile;

public class MediaVideoObserver extends ContentObserver {

    private static final String VIDEO_MEDIA = "video/media/";
    private static final String EQUAL_FLAG = "=";
    private Context mContext;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public MediaVideoObserver(Handler handler, Context context) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (uri.toString().contains(VIDEO_MEDIA) && !uri.toString().contains(EQUAL_FLAG)) {
            MediaScannerFile.getInstance(mContext).queryVideoByUri(uri);
        }
    }
}
