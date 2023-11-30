// IMusicPlayerInfoListener.aidl
package com.ts.sdk.media.callback;

// Declare any non-default types here with import statements
import com.ts.sdk.media.bean.AudioInfoBean;
interface IMusicPlayerInfoListener {
        /**
        * The player object has changed.
        * @param musicInfo Audio object being processed inside the player
        * @param position location
        */
       void onPlayMusicInfo(in AudioInfoBean musicInfo, int position);
}
