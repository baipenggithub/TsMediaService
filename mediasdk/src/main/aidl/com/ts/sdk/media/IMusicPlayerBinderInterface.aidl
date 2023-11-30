// IMusicPlayerBinderInterface.aidl
package com.ts.sdk.media;

// Declare any non-default types here with import statements
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.callback.IMusicPlayerEventListener;
import com.ts.sdk.media.callback.IMusicPlayerInfoListener;
import com.ts.sdk.media.callback.IUsbMusicCallback;
import com.ts.sdk.media.callback.IUsbDevicesListener;
interface IMusicPlayerBinderInterface {

     /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     List<AudioInfoBean> getAudioInfo(String keyword);

     List<AudioInfoBean> getAllAudio();

     List<UsbDevicesInfoBean> getUsbDevices();

     /**
     * Register usb listener.
     *
     * @param listener IUsbDevicesListener
     */
     void registerAudioStatusObserver(IUsbMusicCallback callback);

     /**
     * UnRegister.
     */
     void unRegisterAudioStatusObserver(IUsbMusicCallback callback);

     /**
     * Register Video file listener.
     *
     * @param callback IUsbVideoCallback
     */
     void registerUsbDevicesStatusObserver(IUsbDevicesListener listener);

     /**
     * UnRegister.
     */
     void unRegisterUsbDevicesStatusObserver(IUsbDevicesListener callback);

     /**
     * Start play task.
     *
     * @param audios Data set to be played
     * @param index  Specified location, 0-data.size()
     */
    void startPlayMusic(in List<AudioInfoBean> audios, int index);

    /**
     * Start playing the audio file in the specified location.
     *
     * @param index Specified location, 0-data.size()
     */
    void startPlayMusicIndex(int index);

    /**
     * Start playing the audio file in the specified location.
     *
     */
    void startPlayMusicInfo(in AudioInfoBean audio);

    /**
     * Start playback and add this playback object to the top of the queue being played.
     *
     * @param audioInfo Audio object
     */
    void addPlayMusicToTop(in AudioInfoBean audioInfo);

    /**
     * Start, pause.
     */
    void playOrPause();

    /**
     * Pause playback.
     */
    void pause();

    /**
     * Resume playback.
     */
    boolean play();

    /**
     * Set player mode.
     *
     * @param loop true:loop
     */
    void setLoop(boolean loop);

    /**
     * Continue where you just played.
     *
     * @param sourcePath Absolute address of audio file
     */
    void continuePlay(String sourcePath);

    /**
     * Continue where you just played.
     *
     * @param sourcePath Absolute address of audio file.
     * @param index      Specific location
     */
    void continuePlayIndex(String sourcePath, int index);

    /**
     * Player internal release.
     */
    void onReset();

    /**
     * Player stops working.
     */
    void onStop();

    /**
     * Destroy stops working.
     */
     void onDestroy();

    /**
     * Update data set location of internal player.
     *
     * @param audios To play list
     * @param index  Position
     */
    void updateMusicPlayerData(in List<AudioInfoBean> audios, int index);

    /**
     * Set playback mode.
     *
     * @param model Playback mode
     */
    int setPlayerMode(int mode);

    /**
     * Return to player internal playback mode.
     */
    int getPlayerMode();

    /**
     * Jump to play somewhere.
     */
    void seekTo(long currentTime);

    /**
     * Play the previous song and maintain the previous logic internally.
     */
    void playLastMusic();

    /**
     * Play the next song and maintain the next logic internally.
     */
    void playNextMusic();

    /**
     * Test the position of the previous song, and the play task will not be started.
     */
    int playLastIndex();

    /**
     * Test the position of the next song, no play task will be started.
     */
    int playNextIndex();

    /**
     * Test the position of the next song randomly, and the play task will not be started.
     */
    int playRandomNextIndex();

    /**
     * Player internal play status.
     *
     * @return true:Playing
     */
    boolean isPlaying();

    /**
     * Returns the total length of audio being played.
     *
     * @return Unit millisecond
     */
    long getDuration();

    /**
     * Return the playing audio ID.
     *
     * @return Audio ID
     */
    long getCurrentPlayerId();

    /**
     * Returns the audio object being played.
     *
     * @return Audio object
     */
    AudioInfoBean getCurrentPlayerMusic();

    /**
     * Return to the queue being played by the internal player.
     */
    List<AudioInfoBean> getCurrentPlayList();

    /**
     * Change the playing data source being processed inside the player channel.
     */
    void setPlayingChannel(int channel);

    /**
     * Return to channel, the playback data source being processed inside the player.
     */
    int getPlayingChannel();

    /**
     * Return to the internal working state of the player.
     */
    int getPlayerState();

    /**
     * Check the playback configuration, and call in the player interface echo generally.
     */
    void onCheckedPlayerConfig();

    /**
     * Check what is playing inside the player.
     */
    void onCheckedCurrentPlayTask();

    /**
     * Add a play state listener to the listener pool.
     *
     * @param listener Objects that implement listeners
     */
    void addOnPlayerEventListener(in IMusicPlayerEventListener listener);

    /**
     * Remove a listener from the listener pool.
     *
     * @param listener Objects that implement listeners
     */
    void removePlayerListener(in IMusicPlayerEventListener listener);

    /**
     * Clear all listener objects in the listener pool.
     */
    void removeAllPlayerListener();

    /**
     * Set the listening of playing object.
     *
     * @param listener Objects that implement listeners
     */
    void setPlayInfoListener(in IMusicPlayerInfoListener listener);

    /**
     * Remove listen to play object event.
     */
    void removePlayInfoListener();

    /**
     * Sequence change player play mode.
     */
    void changedPlayerPlayMode();

     /**
      * Fast forward.
      */
    void fastForward();

    /**
    * Fast back.
    */
    void fastBack();
}
