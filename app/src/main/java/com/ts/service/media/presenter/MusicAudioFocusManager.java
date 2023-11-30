package com.ts.service.media.presenter;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;

import com.ts.sdk.media.constants.MusicConstants;
import com.ts.service.media.utils.LogUtil;

/**
 * Music Audio Focus Manager.
 */
public final class MusicAudioFocusManager {

    private static final String TAG = "MusicAudioFocusManager";
    private int mVolumeWhenFocusLossTransientCanDuck;
    private AudioManager mAudioManager;
    private OnAudioFocusListener mFocusListener;

    public MusicAudioFocusManager(Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Request audio focus.
     */
    public int requestAudioFocus(OnAudioFocusListener focusListener) {
        if (null != focusListener) {
            mFocusListener = focusListener;
        }
        if (null != mAudioManager) {
            AudioAttributes streamAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            AudioFocusRequest focusRequest = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = new AudioFocusRequest
                        .Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(streamAttributes)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                        .build();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return mAudioManager.requestAudioFocus(focusRequest);
            }
        }
        return MusicConstants.PLAYER_STATUS_STOP;
    }

    /**
     * Stop playing to release audio focus.
     */
    public void releaseAudioFocus() {
        if (null != mAudioManager && null != onAudioFocusChangeListener) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }
    }

    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager
            .OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            LogUtil.debug(TAG, "onAudioFocusChange:focusChange:" + focusChange);
            int volume;
            switch (focusChange) {
                // Regained focus
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    LogUtil.debug(TAG, "Regained focus");
                    volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (mVolumeWhenFocusLossTransientCanDuck > 0
                            && volume == mVolumeWhenFocusLossTransientCanDuck
                            / MusicConstants.MUSIC_MODE_ORDER) {
                        // Restore volume
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                mVolumeWhenFocusLossTransientCanDuck,
                                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    }
                    // Resume playback
                    if (null != mFocusListener) {
                        mFocusListener.onFocusGet();
                    }
                    break;
                // Preempted by another player
                case AudioManager.AUDIOFOCUS_LOSS:
                    LogUtil.debug(TAG, "Captured by other players");
                    releaseAudioFocus();
                    if (null != mFocusListener) {
                        mFocusListener.onFocusOut();
                    }
                    break;
                // Temporary loss of focus, such as an incoming call occupying audio output
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    LogUtil.debug(TAG, "Temporarily lose focus");
                    if (null != mFocusListener) {
                        mFocusListener.onFocusOut();
                    }
                    break;
                // Instant loss of focus, such as notifications taking up audio output
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    LogUtil.debug(TAG, "Instant loss of focus");
                    if (null != mFocusListener && mFocusListener.isPlaying()) {
                        volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        if (volume > 0) {
                            mVolumeWhenFocusLossTransientCanDuck = volume;
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                    mVolumeWhenFocusLossTransientCanDuck
                                            / MusicConstants.MUSIC_MODE_ORDER,
                                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public interface OnAudioFocusListener {
        /**
         * Restore focus.
         */
        void onFocusGet();

        /**
         * loseFocus.
         */
        void onFocusOut();

        /**
         * Whether the internal player is playing.
         *
         * @return Is playing
         */
        boolean isPlaying();
    }

    public void onDestroy() {
        mAudioManager = null;
    }
}
