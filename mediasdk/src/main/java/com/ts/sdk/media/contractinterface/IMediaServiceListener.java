package com.ts.sdk.media.contractinterface;


import com.ts.sdk.media.mananger.BaseManager;

public interface IMediaServiceListener {
    /**
     * service connected success.
     *
     * @param manager manager
     */
    void onServiceConnected(BaseManager manager);

    /**
     * service disconnected success.
     */
    void onServiceDisconnected();
}
