// IMusicPlayerEventListener.aidl
package com.ts.sdk.media.callback;

// Declare any non-default types here with import statements
import com.ts.sdk.media.bean.AudioInfoBean;
interface IMusicPlayerEventListener {

    /**
     * All status callbacks of player.
     *
     * @param playerState Player internal status
     */
    oneway void onMusicPlayerState(int playerState, String message);

    /**
     * The player is ready.
     *
     * @param totalDurtion Total duration
     */
    void onPrepared(long totalDurtion);

    /**
     * Player feedback.
     *
     * @param event Event
     */
    void onInfo(int event, int extra);

    /**
     * Tasks currently playing.
     *
     * @param musicInfo Objects playing
     * @param position  Current playing position
     */

    oneway void onPlayMusicOnInfo(in AudioInfoBean musicInfo, int position);

    /**
     * The audio address is invalid, and the component can process logic such as paid purchase.
     *
     * @param musicInfo Playback objects
     * @param position  Indexes
     */
    void onMusicPathInvalid(in AudioInfoBean musicInfo, int position);

    /**
     * Asynchronous initialization.
     *
     * @param totalDurtion   Total audio time
     * @param currentDurtion Current playing position
     * @param bufferProgress Current buffer progress
     */
    oneway void onTaskRuntime(long totalDurtion, long currentDurtion,
                       int bufferProgress, in AudioInfoBean musicInfo);

    /**
     * Configure.
     *
     * @param playMode Playback mode
     * @param isToast   Toast prompt or not
     */
    void onPlayerConfig(int playMode, boolean isToast);
}
