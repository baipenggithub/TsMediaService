package com.ts.sdk.media.mananger;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.ts.sdk.media.IMusicPlayerBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.callback.IMusicPlayerEventListener;
import com.ts.sdk.media.callback.IMusicPlayerInfoListener;
import com.ts.sdk.media.callback.IUsbDevicesListener;
import com.ts.sdk.media.callback.IUsbMusicCallback;
import com.ts.sdk.media.constants.MusicConstants;
import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.sdk.media.contractinterface.IMediaServiceListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Music Player Manager.
 */
public class UsbMusicManager extends BaseManager {
    private static volatile UsbMusicManager sInstance = null;
    private static IMusicPlayerBinderInterface sBinder;
    private static boolean sIsServiceCon = false;
    private static List<IMediaServiceListener> sListener = new ArrayList<>();

    private UsbMusicManager(Context context) {
        super(context);
    }

    /**
     * Instantiation.
     */
    public static synchronized UsbMusicManager getInstance(Context context, IMediaServiceListener listener) {
        if (listener != null) {
            sListener.add(listener);
        }
        if (sInstance == null) {
            sInstance = new UsbMusicManager(context);
        }
        if (sIsServiceCon) {
            if (listener != null) {
                int index = sListener.indexOf(listener);
                sListener.get(index).onServiceConnected(sInstance);
            }
        }
        return sInstance;
    }

    /**
     * Clear registered listening.
     */
    public void removeMediaServiceListener(IMediaServiceListener listener) throws RuntimeException {
        if (sListener != null) {
            sListener.remove(listener);
        }
    }

    @Override
    protected String getAction() {
        return ServiceConstants.SERVICE_MUSIC_PLAYER_ACTION;
    }

    @Override
    protected void setBinder(IBinder binder) {
        super.setBinder(binder);
        synchronized (UsbMusicManager.class) {
            if (binder != null) {
                sBinder = IMusicPlayerBinderInterface.Stub.asInterface(binder);
                if (sListener != null) {
                    for (int i = 0; i < sListener.size(); i++) {
                        sListener.get(i).onServiceConnected(sInstance);
                    }
                }
                sIsServiceCon = true;
            } else {
                sBinder = null;
                if (sListener != null) {
                    for (int i = 0; i < sListener.size(); i++) {
                        sListener.get(i).onServiceDisconnected();
                    }
                }
                sIsServiceCon = false;
            }
        }
    }

    @Override
    protected void destroy() {
        // TODO Destroy
    }

    /**
     * Get audio info.
     *
     * @return AudioInfoBean list.
     */
    public List<AudioInfoBean> getAudioInfo(String keyword) throws RemoteException {
        if (null != sBinder) {
            return sBinder.getAudioInfo(keyword);
        }
        return null;
    }

    /**
     * Get All audio info.
     *
     * @return AudioInfoBean list.
     */
    public List<AudioInfoBean> getAllAudio() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getAllAudio();
        }
        return null;
    }

    /**
     * Get usb devices.
     */
    public List<UsbDevicesInfoBean> getUsbDevices() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getUsbDevices();
        }
        return null;
    }

    /**
     * Register usb listener.
     *
     * @param listener IUsbDevicesListener
     */
    public void registerUsbDevicesStatusObserver(IUsbDevicesListener listener)
            throws RemoteException {
        if (null != sBinder) {
            sBinder.registerUsbDevicesStatusObserver(listener);
        }
    }

    /**
     * UnRegister.
     */
    public void unRegisterUsbDevicesStatusObserver(IUsbDevicesListener listener)
            throws RemoteException {
        if (null != sBinder) {
            sBinder.unRegisterUsbDevicesStatusObserver(listener);
        }
    }

    /**
     * Register Video file listener.
     *
     * @param callback IUsbVideoCallback
     */
    public void registerVideoStatusObserver(IUsbMusicCallback callback) throws RemoteException {
        if (null != sBinder) {
            sBinder.registerAudioStatusObserver(callback);
        }
    }

    /**
     * UnRegister.
     */
    public void unRegisterVideoStatusObserver(IUsbMusicCallback callback) throws RemoteException {
        if (null != sBinder) {
            sBinder.unRegisterAudioStatusObserver(callback);
        }
    }

    /**
     * Start playing a new audio queue and the player will replace the new music list.
     *
     * @param audios Data set to be played
     * @param index  Specify where to play, 0-data.size()
     */
    public void startPlayMusic(List<AudioInfoBean> audios, int index) throws RemoteException {
        if (null != sBinder) {
            sBinder.startPlayMusic(audios, index);
        }
    }

    /**
     * Start playing the audio file in the specified location, if the playlist exists.
     *
     * @param index Specified location, 0-data.size()
     */
    public void startPlayMusic(int index) throws RemoteException {
        if (null != sBinder) {
            sBinder.startPlayMusicIndex(index);
        }
    }

    /**
     * Start playing a new audio.
     */
    public void startPlayMusic(AudioInfoBean audio) throws RemoteException {
        if (null != sBinder) {
            sBinder.startPlayMusicInfo(audio);
        }
    }

    /**
     * Start a new play task, and the player will automatically add it to the top of the queue,
     * that is, queue play.
     *
     * @param audioInfo Audio object
     */
    public void addPlayMusicToTop(AudioInfoBean audioInfo) throws RemoteException {
        if (null != sBinder) {
            sBinder.addPlayMusicToTop(audioInfo);
        }
    }

    /**
     * Start, pause playback.
     */
    public void playOrPause() throws RemoteException {
        if (null != sBinder) {
            sBinder.playOrPause();
        }
    }

    /**
     * Pause playback.
     */
    public void pause() throws RemoteException {
        if (null != sBinder) {
            sBinder.pause();
        }
    }

    /**
     * Start playing.
     */
    public boolean play() throws RemoteException {
        if (null != sBinder) {
            return sBinder.play();
        }
        return false;
    }

    /**
     * Whether to loop.
     *
     * @param loop true:loop
     */
    public void setLoop(boolean loop) throws RemoteException {
        if (null != sBinder) {
            sBinder.setLoop(loop);
        }
    }

    /**
     * Continue last playback.
     *
     * @param sourcePath Absolute address of audio file
     */
    public void continuePlay(String sourcePath) throws RemoteException {
        if (null != sBinder) {
            sBinder.continuePlay(sourcePath);
        }
    }

    /**
     * Continue last playback.
     *
     * @param sourcePath Absolute address of audio file
     * @param index      Where to retry playback
     */
    public void continuePlay(String sourcePath, int index) throws RemoteException {
        if (null != sBinder) {
            sBinder.continuePlayIndex(sourcePath, index);
        }
    }

    /**
     * Release.
     */
    public void onReset() throws RemoteException {
        if (null != sBinder) {
            sBinder.onReset();
        }
    }

    /**
     * stop playing.
     */
    public void onStop() throws RemoteException {
        if (null != sBinder) {
            sBinder.onStop();
        }
    }

    /**
     * Destroy playing.
     */
    public void onDestroy() throws RemoteException {
        if (null != sBinder) {
            sBinder.onDestroy();
        }
    }

    /**
     * Replace the waiting list in the player.
     *
     * @param audios To play list
     * @param index  Position
     */
    public void updateMusicPlayerData(List<AudioInfoBean> audios, int index) throws
            RemoteException {
        if (null != sBinder) {
            sBinder.updateMusicPlayerData(audios, index);
        }
    }

    /**
     * Set playback mode.
     *
     * @param mode Playback mode
     * @return Successfully set playback mode
     */
    public int setPlayerMode(int mode) throws RemoteException {
        if (null != sBinder) {
            return sBinder.setPlayerMode(mode);
        }
        return MusicConstants.MUSIC_MODE_LOOP;
    }

    /**
     * Restore settings.
     *
     * @return boolean
     */
    public boolean restorePlayerMode() {
        if (null != sBinder) {
            try {
                return MusicConstants.MUSIC_MODE_LOOP
                        == sBinder.setPlayerMode(MusicConstants.MUSIC_MODE_LOOP);
            } catch (RemoteException exception) {
                exception.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Get playback mode.
     *
     * @return Player play mode
     */
    public int getPlayerMode() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getPlayerMode();
        }
        return MusicConstants.MUSIC_MODE_LOOP;
    }

    /**
     * Try to jump to a buffer.
     *
     * @param currentTime Time position
     */
    public void seekTo(long currentTime) throws RemoteException {
        if (null != sBinder) {
            sBinder.seekTo(currentTime);
        }
    }

    /**
     * Play the last song,
     * The player handles it automatically according to the playback mode set by the user.
     */
    public void playLastMusic() throws RemoteException {
        if (null != sBinder) {
            sBinder.playLastMusic();
        }
    }

    /**
     * Play the next song,
     * and the player will automatically process it according to the playback mode set by the user.
     */
    public void playNextMusic() throws RemoteException {
        if (null != sBinder) {
            sBinder.playNextMusic();
        }
    }

    /**
     * Detect the playing position of the previous song.
     *
     * @return Legal playable location
     */
    public int playLastIndex() throws RemoteException {
        if (null != sBinder) {
            return sBinder.playLastIndex();
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Detect the next song's playing position.
     *
     * @return Legal playable location
     */
    public int playNextIndex() throws RemoteException {
        if (null != sBinder) {
            return sBinder.playNextIndex();
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Random detection of the next song position will not trigger the play task.
     *
     * @return Legal playable location
     */
    public int playRandomNextIndex() throws RemoteException {
        if (null != sBinder) {
            return sBinder.playRandomNextIndex();
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Return to the internal working state of the player.
     *
     * @return Start preparing, buffering, playing, etc :true,other： false
     */
    public boolean isPlaying() throws RemoteException {
        if (null != sBinder) {
            return sBinder.isPlaying();
        }
        return false;
    }

    /**
     * Returns the total duration of the media audio object.
     *
     * @return Unit: ms
     */
    public long getDurtion() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getDuration();
        }
        return MusicConstants.PLAYER_STATUS_STOP;
    }

    /**
     * Returns the ID of the audio object currently playing.
     *
     * @return Audio ID
     */
    public long getCurrentPlayerId() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getCurrentPlayerId();
        }
        return MusicConstants.PLAYER_STATUS_STOP;
    }

    /**
     * Returns the currently playing audio object.
     *
     * @return Audio object
     */
    public AudioInfoBean getCurrentPlayerMusic() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getCurrentPlayerMusic();
        }
        return null;
    }

    /**
     * Return to the currently playing audio queue.
     *
     * @return Audio queue
     */
    public List<AudioInfoBean> getCurrentPlayList() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getCurrentPlayList();
        }
        return null;
    }

    /**
     * Update the source property of the object being processed inside the player.
     */
    public void setPlayingChannel(int channel) throws RemoteException {
        if (null != sBinder) {
            sBinder.setPlayingChannel(channel);
        }
    }

    /**
     * Returns the source property of the object being processed inside the player.
     */
    public int getPlayingChannel() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getPlayingChannel();
        }
        return MusicConstants.CHANNEL_LOCATION;
    }

    /**
     * Return to the internal working state of the player.
     */
    public int getPlayerState() throws RemoteException {
        if (null != sBinder) {
            return sBinder.getPlayerState();
        }
        return MusicConstants.PLAYER_STATUS_STOP;
    }

    /**
     * Check player configuration.
     */
    public void onCheckedPlayerConfig() throws RemoteException {
        if (null != sBinder) {
            sBinder.onCheckedPlayerConfig();
        }
    }

    /**
     * Check the audio object being processed inside the player.
     */
    public void onCheckedCurrentPlayTask() throws RemoteException {
        if (null != sBinder) {
            sBinder.onCheckedCurrentPlayTask();
        }
    }

    /**
     * Add player status listener.
     */
    public void addOnPlayerEventListener(IMusicPlayerEventListener listener) throws
            RemoteException {
        if (null != sBinder) {
            sBinder.addOnPlayerEventListener(listener);
        }
    }

    /**
     * Remove player status listener.
     */
    public void removePlayerListener(IMusicPlayerEventListener listener) throws RemoteException {
        if (null != sBinder) {
            sBinder.removePlayerListener(listener);
        }
    }

    /**
     * Remove all player status listeners.
     */
    public void removeAllPlayerListener() throws RemoteException {
        if (null != sBinder) {
            sBinder.removeAllPlayerListener();
        }
    }

    /**
     * Listen to what the player is processing.
     */
    public UsbMusicManager setPlayInfoListener(IMusicPlayerInfoListener listener) throws
            RemoteException {
        if (null != sBinder) {
            sBinder.setPlayInfoListener(listener);
        }
        return sInstance;
    }

    /**
     * Remove listen to play object event.
     */
    public void removePlayInfoListener() throws RemoteException {
        if (null != sBinder) {
            sBinder.removePlayInfoListener();
        }
    }

    /**
     * Try to change the playback mode, and switch between single, list loop and random modes.
     */
    public void changedPlayerPlayMode() throws RemoteException {
        if (null != sBinder) {
            sBinder.changedPlayerPlayMode();
        }
    }

    /**
     * Fast forward.
     */
    public void fastForward() throws RemoteException {
        if (null != sBinder) {
            sBinder.fastForward();
        }
    }

    /**
     * Fast back.
     */
    public void fastBack() throws RemoteException {
        if (null != sBinder) {
            sBinder.fastBack();
        }
    }
}
