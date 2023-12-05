package com.ts.service.media.bind;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.ts.sdk.media.IMusicPlayerBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.callback.IMusicPlayerEventListener;
import com.ts.sdk.media.callback.IMusicPlayerInfoListener;
import com.ts.sdk.media.callback.IUsbDevicesListener;
import com.ts.sdk.media.callback.IUsbMusicCallback;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.presenter.MusicMediaPlayer;
import com.ts.service.media.receiver.UsbDeviceMonitor;
import com.ts.service.media.utils.LogUtil;
import com.ts.service.media.utils.MediaScannerFile;

import java.util.List;

public class MusicPlayerBinder extends IMusicPlayerBinderInterface.Stub
        implements MediaScannerFile.IAudioScanListener {
    private static final String TAG = MusicPlayerBinder.class.getSimpleName();
    private MusicMediaPlayer mMusicMediaPlayer;
    private IUsbMusicCallback mUsbMusicCallback;
    private final RemoteCallbackList<IUsbDevicesListener> mUsbDevicesListener = new RemoteCallbackList<>();
    private final Context mContext;

    /**
     * Initialization.
     *
     * @param context Context
     */
    public MusicPlayerBinder(Context context) {
        mContext = context;
        if (null == mMusicMediaPlayer) {
            mMusicMediaPlayer = new MusicMediaPlayer(context);
        }
        LogUtil.debug("MusicPlayerBinder**", "MusicPlayerBinder****");
        UsbDeviceMonitor.getInstance(context).setUsbDeviceListener(new UsbDeviceMonitor
                .UsbDeviceListener() {
            @Override
            public void onDeviceChange(List<UsbDevicesInfoBean> usbDevices) {
                try {
                    if (mUsbDevicesListener != null) {
                        synchronized (mUsbDevicesListener) {
                            checkListener();
                            int listenerCount = mUsbDevicesListener.beginBroadcast();
                            for (int index = 0; index < listenerCount; index++) {
                                mUsbDevicesListener.getBroadcastItem(index)
                                        .onUsbDevicesChange(usbDevices);
                            }
                            mUsbDevicesListener.finishBroadcast();
                        }
                    }
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }

            @Override
            public void onScanChange(int state, String deviceId, int portId) {
                try {
                    if (mUsbDevicesListener != null) {
                        synchronized (mUsbDevicesListener) {
                            checkListener();
                            int listenerCount = mUsbDevicesListener.beginBroadcast();
                            for (int index = 0; index < listenerCount; index++) {
                                mUsbDevicesListener.getBroadcastItem(index)
                                        .onScanStateChange(state, deviceId, portId);
                            }
                            mUsbDevicesListener.finishBroadcast();
                        }
                    }
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }
        });
        MediaScannerFile.getInstance(context).setAudioScanListener(this);
    }

    private void checkListener() {
        try {
            if (mUsbDevicesListener != null) {
                mUsbDevicesListener.finishBroadcast();
            }
        } catch (IllegalStateException ex) {
            LogUtil.debug(TAG, "RemoteCallbackList checkListener");
        }
    }

    @Override
    public List<AudioInfoBean> getAudioInfo(String keyword) throws RemoteException {
        return MediaScannerFile.getInstance(mContext).queryMusic(keyword);
    }

    @Override
    public List<AudioInfoBean> getAllAudio() throws RemoteException {
        MediaScannerFile.getInstance(mContext).queryAllMusic(MusicConstants.QUERY_MUSIC_CLIENT);
        return null;
    }

    @Override
    public List<UsbDevicesInfoBean> getUsbDevices() throws RemoteException {
        return UsbDeviceMonitor.getInstance(mContext).getUsbDevices();
    }

    @Override
    public void registerAudioStatusObserver(IUsbMusicCallback callback) throws RemoteException {
        mUsbMusicCallback = callback;
    }

    @Override
    public void unRegisterAudioStatusObserver(IUsbMusicCallback callback) throws RemoteException {
        mUsbMusicCallback = null;
    }

    @Override
    public void registerUsbDevicesStatusObserver(IUsbDevicesListener listener) {
        if (listener != null) {
            mUsbDevicesListener.register(listener);
        }
    }

    @Override
    public void unRegisterUsbDevicesStatusObserver(IUsbDevicesListener listener) {
        if (listener != null) {
            mUsbDevicesListener.unregister(listener);
        }
    }

    /**
     * Start playing a new audio queue and the player will replace the new music list.
     *
     * @param musicList Data set to be played
     * @param position  Specify where to play, 0-data.size()
     */
    @Override
    public void startPlayMusic(List<AudioInfoBean> musicList, int position) throws
            RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.startPlayMusic(musicList, position);
        }
    }

    /**
     * Start playing the audio file in the specified location, if the playlist exists.
     *
     * @param position Specified location, 0-data.size()
     */
    @Override
    public void startPlayMusicIndex(int position) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.startPlayMusicIndex(position);
        }
    }

    @Override
    public void startPlayMusicInfo(AudioInfoBean audioInfoBean) throws RemoteException {
        if (null != mUsbMusicCallback) {
            mUsbMusicCallback.startPlayMusic(audioInfoBean);
        } else {
            if (null != mMusicMediaPlayer) {
                mMusicMediaPlayer.startPlayMusic(audioInfoBean);
            }
        }
    }

    /**
     * Start a new play task, and the player will automatically add it to the top of the queue,
     * that is, queue play.
     *
     * @param audioInfo Audio object
     */
    @Override
    public void addPlayMusicToTop(AudioInfoBean audioInfo) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.addPlayMusicToTop(audioInfo);
        }
    }

    /**
     * Start, pause playback.
     */
    @Override
    public void playOrPause() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.playOrPause();
        }
    }

    /**
     * Pause playback.
     */
    @Override
    public void pause() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.pause();
        }
    }

    /**
     * Start playing.
     */
    @Override
    public boolean play() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.play();
        }
        return false;
    }

    /**
     * Whether to loop.
     *
     * @param loop true:loop
     */
    @Override
    public void setLoop(boolean loop) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.setLoop(loop);
        }
    }

    /**
     * Continue last playback.
     *
     * @param sourcePath Absolute address of audio file
     */
    @Override
    public void continuePlay(String sourcePath) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.continuePlay(sourcePath);
        }
    }

    /**
     * Continue last playback.
     *
     * @param sourcePath Absolute address of audio file
     * @param position   Where to retry playback
     */
    @Override
    public void continuePlayIndex(String sourcePath, int position) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.continuePlayIndex(sourcePath, position);
        }
    }

    /**
     * Release.
     */
    @Override
    public void onReset() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.onReset();
        }
    }

    /**
     * stop playing.
     */
    @Override
    public void onStop() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.onStop();
        }
    }

    /**
     * Destroy playing.
     */
    @Override
    public void onDestroy() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.destroy();
        }
    }

    /**
     * Replace the waiting list in the player.
     *
     * @param audios To play list
     * @param index  Position
     */
    @Override
    public void updateMusicPlayerData(List<AudioInfoBean> audios, int index) throws
            RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.updateMusicPlayerData(audios, index);
        }
    }

    /**
     * Set playback mode.
     *
     * @param mode Playback mode
     * @return Successfully set playback mode
     */
    @Override
    public int setPlayerMode(int mode) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.setPlayerMode(mode);
        }
        return MusicConstants.MUSIC_MODE_LOOP;
    }

    /**
     * Get playback mode.
     *
     * @return Player play mode
     */
    @Override
    public int getPlayerMode() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getPlayerMode();
        }
        return MusicConstants.MUSIC_MODE_LOOP;
    }

    /**
     * Try to jump to a buffer.
     *
     * @param currentTime Time position
     */
    @Override
    public void seekTo(long currentTime) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.seekTo(currentTime);
        }
    }

    /**
     * Play the last song,
     * The player handles it automatically according to the playback mode set by the user.
     */
    @Override
    public void playLastMusic() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.playLastMusic();
        }
    }

    /**
     * Play the next song,
     * and the player will automatically process it according to the playback mode set by the user.
     */
    @Override
    public void playNextMusic() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.playNextMusic();
        }
    }

    /**
     * Detect the playing position of the previous song.
     *
     * @return Legal playable location
     */
    @Override
    public int playLastIndex() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.playLastIndex();
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Detect the next song's playing position.
     *
     * @return Legal playable location
     */
    @Override
    public int playNextIndex() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.playNextIndex();
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Random detection of the next song position will not trigger the play task.
     *
     * @return Legal playable location
     */
    @Override
    public int playRandomNextIndex() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.playRandomNextIndex();
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Return to the internal working state of the player.
     *
     * @return Start preparing, buffering, playing, etc :true,other： false
     */
    @Override
    public boolean isPlaying() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * Returns the total duration of the media audio object.
     *
     * @return Unit: ms
     */
    @Override
    public long getDuration() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getDuration();
        }
        return MusicConstants.MUSIC_PLAYER_STOP;
    }

    /**
     * Returns the ID of the audio object currently playing.
     *
     * @return Audio ID
     */
    @Override
    public long getCurrentPlayerId() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getCurrentPlayerId();
        }
        return MusicConstants.MUSIC_PLAYER_STOP;
    }

    /**
     * Returns the currently playing audio object.
     *
     * @return Audio object
     */
    @Override
    public AudioInfoBean getCurrentPlayerMusic() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getCurrentPlayerMusic();
        }
        return null;
    }

    /**
     * Return to the currently playing audio queue.
     *
     * @return Audio queue
     */
    @Override
    public List<AudioInfoBean> getCurrentPlayList() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getCurrentPlayList();
        }
        return null;
    }

    /**
     * Update the source property of the object being processed inside the player.
     */
    @Override
    public void setPlayingChannel(int channel) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.setPlayingChannel(channel);
        }
    }

    /**
     * Returns the source property of the object being processed inside the player.
     */
    @Override
    public int getPlayingChannel() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getPlayingChannel();
        }
        return MusicConstants.CHANNEL_LOCATION;
    }

    /**
     * Check player configuration.
     */
    @Override
    public void onCheckedPlayerConfig() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.onCheckedPlayerConfig();
        }
    }

    /**
     * Check the audio object being processed inside the player.
     */
    @Override
    public void onCheckedCurrentPlayTask() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.onCheckedCurrentPlayTask();
        }
    }

    /**
     * Return to the internal working state of the player.
     */
    @Override
    public int getPlayerState() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            return mMusicMediaPlayer.getPlayerState();
        }
        return MusicConstants.MUSIC_PLAYER_STOP;
    }

    /**
     * Try to change the playback mode, and switch between single, list loop and random modes.
     */
    @Override
    public void changedPlayerPlayMode() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.changedPlayerPlayMode();
        }
    }

    /**
     * Fast forward.
     */
    @Override
    public void fastForward() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.fastForward();
        }
    }

    /**
     * Fast back.
     */
    @Override
    public void fastBack() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.fastBack();
        }
    }

    /**
     * Add player status listener.
     */
    @Override
    public void addOnPlayerEventListener(IMusicPlayerEventListener listener) throws
            RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.addOnPlayerEventListener(listener);
        }
    }

    /**
     * Remove player status listener.
     */
    @Override
    public void removePlayerListener(IMusicPlayerEventListener listener) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.removePlayerListener(listener);
        }
    }

    /**
     * Remove all player status listeners.
     */
    @Override
    public void removeAllPlayerListener() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.removeAllPlayerListener();
        }
    }

    /**
     * Listen to what the player is processing.
     */
    @Override
    public void setPlayInfoListener(IMusicPlayerInfoListener listener) throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.setPlayInfoListener(listener);
        }
    }

    /**
     * Remove listen to play object event.
     */
    @Override
    public void removePlayInfoListener() throws RemoteException {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.removePlayInfoListener();
        }
    }

    /**
     * Destroy.
     */
    public void destroy() {
        if (null != mMusicMediaPlayer) {
            mMusicMediaPlayer.destroy();
        }
    }

    /**
     * Add audio info.
     *
     * @param videoInfoBean AudioInfoBean,
     */
    @Override
    public void addAudio(AudioInfoBean videoInfoBean) {
        try {
            if (null != mUsbMusicCallback) {
                mUsbMusicCallback.addAudio(videoInfoBean);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void queryComplete(List<AudioInfoBean> musicList) {
        try {
            if (null != mUsbMusicCallback) {
                mUsbMusicCallback.onAudioQueryCompleted(musicList);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void updateScanList(List<AudioInfoBean> scanList) {
        try {
            if (null != mUsbMusicCallback) {
                mUsbMusicCallback.onScanListUpdated(scanList);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }
}
