package com.ts.service.media.receiver;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.ts.service.media.utils.MediaScannerFile;

public class MediaAudioObserver extends ContentObserver {

    private static final String AUDIO_MEDIA = "audio/media/";
    private Context mContext;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public MediaAudioObserver(Handler handler, Context context) {
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
        if (uri.toString().contains(AUDIO_MEDIA)) {
            MediaScannerFile.getInstance(mContext).queryAudioByUri(uri);
        }
    }
}
