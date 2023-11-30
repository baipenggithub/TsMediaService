package com.ts.service.media;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.service.R;
import com.ts.service.media.bind.BtMusicPlayerBinder;
import com.ts.service.media.bind.MediaPlayerBinder;
import com.ts.service.media.bind.MusicPlayerBinder;
import com.ts.service.media.bind.OnlineMusicPlayerBinder;
import com.ts.service.media.bind.UsbBinder;
import com.ts.service.media.presenter.AndroidAutoManager;
import com.ts.service.media.receiver.HardKeyMonitor;
import com.ts.service.media.receiver.MediaAudioObserver;
import com.ts.service.media.receiver.MediaVideoObserver;
import com.ts.service.media.receiver.UsbDeviceMonitor;


public class MediaService extends Service {

    private static final String TAG = MediaService.class.getSimpleName();
    private static final String CHANNEL_ID_STRING = "MediaService_Channel";
    private UsbDeviceMonitor mUsbDeviceMonitor;
    private HardKeyMonitor mHardKeyMonitor;
    private UsbBinder mUsbBinder;
    private MusicPlayerBinder mMusicPlayerBinder;
    private BtMusicPlayerBinder mBtMusicPlayerBinder;
    private MediaPlayerBinder mMediaPlayerBinder;
    private OnlineMusicPlayerBinder mOnlineMusicPlayerBinder;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind:onBind ");
        return getBinder(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate:onCreate ");
        setForeground();
        mUsbDeviceMonitor = UsbDeviceMonitor.getInstance(getApplicationContext());
        mUsbDeviceMonitor.initialization();
        mHardKeyMonitor = HardKeyMonitor.getInstance(getApplicationContext());
        mHardKeyMonitor.initialization();

        mUsbBinder = new UsbBinder(getApplicationContext());
        mMusicPlayerBinder = new MusicPlayerBinder(getApplicationContext());
        mBtMusicPlayerBinder = new BtMusicPlayerBinder(getApplicationContext());
        mMediaPlayerBinder = new MediaPlayerBinder(getApplicationContext());
        mOnlineMusicPlayerBinder = new OnlineMusicPlayerBinder(getApplicationContext());
        // Observer video table.
        getApplicationContext().getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
                new MediaVideoObserver(new Handler(), getApplicationContext()));

        getApplicationContext().getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true,
                new MediaAudioObserver(new Handler(), getApplicationContext()));

        new AndroidAutoManager(getApplicationContext(), mBtMusicPlayerBinder);
    }

    private IBinder getBinder(Intent intent) {
        String action;
        if (intent != null) {
            action = intent.getAction();
            if (null != action) {
                switch (action) {
                    case ServiceConstants.SERVICE_USB_SCAN_ACTION:
                        return mUsbBinder;
                    case ServiceConstants.SERVICE_MUSIC_PLAYER_ACTION:
                        return mMusicPlayerBinder;
                    case ServiceConstants.SERVICE_BT_MUSIC_PLAYER_ACTION:
                        return mBtMusicPlayerBinder;
                    case ServiceConstants.SERVICE_PLAY_STATUS_ACTION:
                        return mMediaPlayerBinder;
                    case ServiceConstants.SERVICE_ONLINE_MUSIC_PLAYER_ACTION:
                        return mOnlineMusicPlayerBinder;
                }
            }
        }
        return null;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        Log.d(TAG, "unbindService:unbindService ");
        if (null != mMusicPlayerBinder) {
            mMusicPlayerBinder = null;
        }
        if (null != mBtMusicPlayerBinder) {
            mBtMusicPlayerBinder = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy:onDestroy ");
        if (null != mMusicPlayerBinder) {
            mMusicPlayerBinder.destroy();
        }
        if (null != mBtMusicPlayerBinder) {
            mBtMusicPlayerBinder.destroy();
        }
    }

    private void setForeground() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID_STRING, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_STRING).build();
        }
        startForeground(1, notification);
    }
}
