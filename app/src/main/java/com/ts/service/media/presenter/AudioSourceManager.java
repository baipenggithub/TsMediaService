package com.ts.service.media.presenter;

import static com.ts.sdk.media.constants.MusicConstants.ERROR_CODE;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.ts.service.media.utils.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AudioFocusManager.
 */
public class AudioSourceManager {
    private static final String TAG = "AudioSourceManager";

    private static volatile AudioSourceManager sInstance = null;
    private List<OnAudioSourceChangedListener> mOnAudioSourceChangedListeners;

    private AudioManager mAudioManager;
    private int mMediaSourceType = ERROR_CODE;

    /**
     * GetInstance.
     *
     * @return AudioSourceManager
     */
    public static AudioSourceManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AudioSourceManager.class) {
                if (sInstance == null) {
                    sInstance = new AudioSourceManager(context);
                }
            }
        }
        return sInstance;
    }

    private AudioSourceManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        LogUtil.info(TAG, "AudioSourceManager.init");

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                LogUtil.info(TAG, "onAudioDevicesAdded init: " + Arrays.toString(addedDevices));
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                LogUtil.info(TAG, "onAudioDevicesRemoved init: " + Arrays.toString(removedDevices));
            }
        }, new Handler());
    }

    /**
     * Get current MediaSourceType.
     *
     * @return {@link int}
     */
    public int getCurrentMediaSource() {
        return mMediaSourceType;
    }

    public interface OnAudioSourceChangedListener {
        void onAudioSourceChanged(int mediaSourceType);
    }

    /**
     * Register OnAudioSourceChangedListener .
     *
     * @param listener {@link OnAudioSourceChangedListener}
     */
    public void registerAudioFocusChangedListener(@NonNull OnAudioSourceChangedListener listener) {
        if (null == mOnAudioSourceChangedListeners) {
            mOnAudioSourceChangedListeners = new ArrayList<>();
        }
        if (!mOnAudioSourceChangedListeners.contains(listener)) {
            mOnAudioSourceChangedListeners.add(listener);
        }
    }

    /**
     * Remove OnAudioSourceChangedListener .
     *
     * @param listener {@link OnAudioSourceChangedListener}
     */
    public void removeAudioSourceChangedListener(@NonNull OnAudioSourceChangedListener listener) {
        if (null != mOnAudioSourceChangedListeners) {
            mOnAudioSourceChangedListeners.remove(listener);
        }
    }

    private void notifyAudioSourceChanged(int mediaSourceType) {
        if (null != mOnAudioSourceChangedListeners) {
            for (OnAudioSourceChangedListener item : mOnAudioSourceChangedListeners) {
                item.onAudioSourceChanged(mediaSourceType);
            }
        }
    }

}
