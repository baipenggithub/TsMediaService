package com.ts.sdk.media.callback;

import com.ts.sdk.media.bean.AudioInfoBean;

interface IBtMusicCallback {

        /**
         * Called when connected to or disconnected from the backend service serving the APIs in
         * this class.
         *
         * @param connected true if connected and false if disconnected.
         */
         void onServiceStateChanged(boolean connected);

        /**
         * The current playback progress has changed.
         *
         * @param progress song progress.
         */
         void onPlayProgressChanged(long progress, in AudioInfoBean audioInfo);

        /**
         * Called when the play state on a peer device chagned
         *
         * @param state   The new state.
         */
         void onPlayStateChanged(int state);

        /**
         * Called when the metadata of the currently played track chagned
         *
         * @param metadata The new metadata
         */
        void onMetadataChanged(in AudioInfoBean metadata);

        /**
         * Called when the position of the currently played track chagned
         *
         * @param position The new position.
         */
        void onPlayPositionChanged(int position);

        /**
         * Called when the connection state with a peer device chagned
         *
         * @param state   The new state.
         */
        void onConnectionStateChanged(int state);

        /**
         * Called when media items are retrieved, which is requested via
         *
         * @param handle  The handle of the request which requested the retrieval.
         * @param list    The list of retrieved media items.
         */
        void onMediaItemListRetrieved(int handle, in List<AudioInfoBean> list);

        /**
         * Called when player mode changed.
         *
         * @param repeatMode The repeatMode.
         * @param shuffle The shuffle.
         */
        void onPlayerModeChanged(int repeatMode, boolean shuffle);

       /**
         * onMediaBrowserConnected.
         */
        void onMediaBrowserConnected();

        /**
        * Android auto connection call back.
        */
        void onAAConnected(int type, boolean isConnected);
}
