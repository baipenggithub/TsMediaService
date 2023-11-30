package com.ts.service.media;

import android.app.Application;
import android.content.Context;

public class UsbServiceApplication extends Application {
    /**
     * The application context.
     */
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }

    /**
     * Get application context.
     */
    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
