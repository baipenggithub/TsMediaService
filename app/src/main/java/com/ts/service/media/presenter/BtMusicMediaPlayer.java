package com.ts.service.media.presenter;

import static com.ts.sdk.media.constants.MusicConstants.ERROR_CODE;

import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioSetting;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.allgo.rui.RemoteDevice;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.callback.IBtMusicCallback;
import com.ts.service.media.constants.BtConstants;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.receiver.CarPowerMonitor;
import com.ts.service.media.receiver.HardKeyMonitor;
import com.ts.service.media.utils.LogUtil;
import com.ts.service.media.utils.MusicUtils;
import com.ts.service.media.utils.SharedPreferencesHelps;

import java.util.ArrayList;
import java.util.List;

public class BtMusicMediaPlayer implements A2dpAudioFocusHandler.IAudioFocusListener,
        HardKeyMonitor.IKeyEventListener {

    private static final String TAG = BtMusicMediaPlayer.class.getSimpleName();
    private static final String KEY_MEDIA_PACKAGE = "com.android.bluetooth";
    private static final String KEY_MEDIA_CLASS = "com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService";
    private static final String ACTION_BROWSE_CONNECTION_STATE_CHANGED = "android.bluetooth.avrcp-controller.profile.action.BROWSE_CONNECTION_STATE_CHANGED";
    private static final int REQUEST_INVALID = 0;
    private static final int TYPE_PLAY_MUSIC = 1;
    private static final int TYPE_FAST_FORWARD_BACK = 2;
    private static final int TYPE_UPDATE_PLAY_LIST = 4;
    private static final int TYPE_SUBSCRIBE_PLAY_LIST = 5;
    private static final int DELAY_MUSIC_PROGRESS_UPDATE = 500;
    private static final int DELAY_BT_LOAD_PLAY_LIST = 500;
    private static final int DELAY_PLAY_MUSIC = 200;
    private static final int INTERVAL_FAST_FORWARD_BACK = 1000;

    private static final String BR = ".BR";
    private static BtManager sBtManager = null;
    private static MusicAudioFocusManager sAudioFocusManager = null;
    private Context mContext;
    private AudioSourceManager mAudioSourceManager;
    private A2dpAudioFocusHandler mStreaming = null;
    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;
    private MediaController.TransportControls mTransportControls;
    private AudioInfoBean mCurrentAudioInfo;
    private MediaLibrary mMediaLibrary = null;
    private int mPlayState = PlaybackState.STATE_NONE;
    // Component callback registration pool
    private BluetoothAvrcpPlayerSettings mPlayerSetting;
    private final RemoteCallbackList<IBtMusicCallback> mCallbackListener
            = new RemoteCallbackList<>();
    private List<AudioInfoBean> mAudioBeans = new ArrayList<>();

    private boolean mIsAvrcpConnection = false;
    private int mMediaSource = ERROR_CODE;
    private boolean mIsServiceRestart = false;
    private boolean mIsAAConnection = false;
    private int mA2dpSinkState = BtConstants.CONNECTION_STATE_DISCONNECTED;
    private volatile String mMediaId = BtConstants.BT_PLAY_LIST_ROOT;
    private CarPowerMonitor mCarPowerMonitor;
    private boolean mIsPowerStandby = false;
    private int mCurrentPowerState;
    private Car mCar;
    private CarAudioManager mCarAudioManager;

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            LogUtil.debug(TAG, "handleMessage type : " + msg.what);
            switch (msg.what) {
                case TYPE_PLAY_MUSIC:
                    play();
                    break;
                case TYPE_FAST_FORWARD_BACK:
                    break;
                case TYPE_SUBSCRIBE_PLAY_LIST:
                    LogUtil.debug(TAG, "BtMusic list TYPE_SUBSCRIBE_PLAY_LIST mediaId : "
                            + msg.obj);
                    mMediaId = (String) msg.obj;
                    registerSubscribe((String) msg.obj);
                    break;
                case TYPE_UPDATE_PLAY_LIST:
                    notifyPlayList(mAudioBeans);
                    break;
                default:
                    break;
            }

            return false;
        }
    });

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.debug(TAG, "onReceive() : action =" + action);
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    mHandler.post(() -> {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.STATE_OFF);
                        if (state == BluetoothAdapter.STATE_OFF) {
                            mA2dpSinkState = BtConstants.CONNECTION_STATE_DISCONNECTED;
                            broadcastConnectionStateChange(BtConstants.CONNECTION_STATE_DISCONNECTED);
                            mPlayState = PlaybackState.STATE_NONE;
                            mTransportControls = null;
                        }
                    });
                    break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    int connectState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                            BluetoothAdapter.STATE_DISCONNECTED);
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    LogUtil.debug(TAG, "bluetooth connect state : " + connectState);
                    if (connectState == BluetoothAdapter.STATE_CONNECTED) {
                        SharedPreferencesHelps.saveBluetoothDeviceAddress(device.getAddress());
                    } else if (connectState == BluetoothAdapter.STATE_DISCONNECTED) {
                        mPlayState = PlaybackState.STATE_NONE;
                    }
                    break;
                case ACTION_BROWSE_CONNECTION_STATE_CHANGED:
                    int conState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                            BluetoothProfile.STATE_DISCONNECTED);
                    LogUtil.debug(TAG, "onReceive STATE_CHANGED " + conState);
                    break;
                case BluetoothAvrcpController.ACTION_PLAYER_SETTING:
                    LogUtil.debug(TAG, "onReceive ACTION_PLAYER_SETTING");
                    mPlayerSetting = intent.getParcelableExtra(
                            BluetoothAvrcpController.EXTRA_PLAYER_SETTING);
                    controlPlaybackMode(mPlayerSetting.getSettingValue(
                            BluetoothAvrcpPlayerSettings.SETTING_REPEAT));
                    break;
                case BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED:
                    LogUtil.debug(TAG, "onReceive BluetoothA2dpSink");
                    mA2dpSinkState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                            BluetoothProfile.STATE_DISCONNECTED);
                    mHandler.post(() -> {
                        mAudioBeans.clear();
                        if (mA2dpSinkState == BtConstants.CONNECTION_STATE_CONNECTED) {
                            mStreaming.obtainMessage(A2dpAudioFocusHandler.SRC_STR_STOP)
                                    .sendToTarget();
                            sendLoadPlayListMessage(mMediaId);
                            broadcastConnectionStateChange(mA2dpSinkState);
                        } else if (mA2dpSinkState == BtConstants.CONNECTION_STATE_DISCONNECTED) {
                            unRegisterSubscribe(mMediaId);
                            mMediaId = BtConstants.BT_PLAY_LIST_ROOT;
                            notifyListeners(PlaybackState.STATE_NONE);
                            broadcastConnectionStateChange(mA2dpSinkState);
                        }
                    });
                    break;
                case BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED:
                    int avrcpState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                            BluetoothProfile.STATE_DISCONNECTED);
                    LogUtil.debug(TAG, "onReceive BluetoothAvrcpController avrcp1 state : "
                            + avrcpState);

                    if (avrcpState == BtConstants.CONNECTION_STATE_CONNECTED) {
                        LogUtil.debug(TAG, "mIsServiceRestart : " + mIsServiceRestart);
                        if (mIsServiceRestart) {
                            restoreAudioPlay();
                            mIsServiceRestart = false;
                        }
                        mIsAvrcpConnection = true;
                    } else {
                        mIsAvrcpConnection = false;
                    }
                    break;
                case BluetoothAvrcpController.ACTION_ADDRESSED_PLAYER_CHANGED:
                    LogUtil.debug(TAG, "BtMusic list player has changed");
                    unRegisterSubscribe(mMediaId);
                    mHandler.removeMessages(TYPE_SUBSCRIBE_PLAY_LIST);
                    sendLoadPlayListMessage(BtConstants.BT_PLAY_LIST_ROOT);
                    break;
                default:
                    break;
            }
        }
    };

    private void sendLoadPlayListMessage(String mediaId) {
        Message msg = new Message();
        msg.what = TYPE_SUBSCRIBE_PLAY_LIST;
        msg.obj = mediaId;
        mHandler.sendMessageDelayed(msg, DELAY_BT_LOAD_PLAY_LIST);
    }

    private synchronized void broadcastConnectionStateChange(int a2dpSinkState) {
        try {
            synchronized (mCallbackListener) {
                int listenerCount = mCallbackListener.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    mCallbackListener.getBroadcastItem(index)
                            .onConnectionStateChanged(a2dpSinkState);
                }
                mCallbackListener.finishBroadcast();
            }
            LogUtil.debug(TAG, "a2dpSink State : " + a2dpSinkState);
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Restore bluetooth music play state when reboot is completed.
     * Don't restore if bt isn't connected in 6s.
     */
    private synchronized void restoreAudioPlay() {
        int state = SharedPreferencesHelps.getPlayState();
        String address = SharedPreferencesHelps.getBluetoothDeviceAddress();
        LogUtil.debug(TAG, "restoreAudioPlay state : " + state + "  bluetooth device : " + address + "  connected state : " + getBluetoothState());
        if (state == BtConstants.PLAY_STATE_PLAYING && address.equals(SharedPreferencesHelps.getBluetoothDeviceAddress())) {
            play();
        }
    }

    /**
     * Initialization.
     *
     * @param context Context.
     */
    public BtMusicMediaPlayer(Context context) {
        mContext = context;
        ServiceConnection carServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LogUtil.debug(TAG, "mCarServiceConnection");
                try {
                    mCarAudioManager = (CarAudioManager) mCar.getCarManager(Car.AUDIO_SERVICE);
                } catch (CarNotConnectedException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        mCar = Car.createCar(context, carServiceConnection);
        mCar.connect();
        sAudioFocusManager = new MusicAudioFocusManager(context);
        sBtManager = BtManager.getInstance(mContext);
        sBtManager.initBtAdapter();
        mCarPowerMonitor = CarPowerMonitor.getInstance(mContext);
        mCarPowerMonitor.addPowerEventListener(state -> {
            powerStateChanged(state);
        });
        mA2dpSinkState = sBtManager.getBluetoothState();
        mIsAvrcpConnection = sBtManager.isAvrcpConnection();
        if (mIsAvrcpConnection) {
            mIsServiceRestart = false;
        } else {
            mIsServiceRestart = true;
        }
        // HardKeyMonitor.getInstance(context).addKeyEventListener(this);
        mMediaLibrary = new MediaLibrary();
        // init mTransportControls when MediaBrowser is reconnected.
        // init data when media browser is connected.
        MediaBrowser.ConnectionCallback connectionCallback = new MediaBrowser.ConnectionCallback() {
            @Override
            public void onConnected() {
                LogUtil.error(TAG, "onConnectionSuspended&##############");
                final MediaSession.Token token = mMediaBrowser.getSessionToken();
                // init mTransportControls when MediaBrowser is reconnected.
                mTransportControls = null;
                mMediaController = new MediaController(mContext, token);
                if (!mIsAAConnection) {
                    mMediaController.registerCallback(mMediaControllerCallback);
                }
                // init data when media browser is connected.
                getMetaData();
                updateMetaData();
                sendLoadPlayListMessage(mMediaBrowser.getRoot());
            }

            @Override
            public void onConnectionSuspended() {
                LogUtil.error(TAG, "onConnectionSuspended=============");
                if (mMediaController != null) {
                    mMediaController.unregisterCallback(mMediaControllerCallback);
                }
                mMediaController = null;
                mTransportControls = null;
                unRegisterSubscribe(mMediaId);
            }

            @Override
            public void onConnectionFailed() {
                LogUtil.error(TAG, "onConnectionFailed>>>>>>>>>>>>>>>>");
                mMediaController = null;
                mTransportControls = null;
                unRegisterSubscribe(mMediaId);
            }
        };
        mMediaBrowser = new MediaBrowser(mContext, new ComponentName(KEY_MEDIA_PACKAGE, KEY_MEDIA_CLASS), connectionCallback, null);
        LogUtil.debug(TAG, "mMediaBrowser.isConnected() ===>" + mMediaBrowser.isConnected());
        if (!mMediaBrowser.isConnected()) {
            mMediaBrowser.connect();
        }
        init();
        if (mStreaming == null) {
            LogUtil.debug(TAG, "Creating New A2dpSinkStreamHandler");
            mStreaming = new A2dpAudioFocusHandler(mContext, this);
        }
    }

    private void init() {
        LogUtil.debug(TAG, "init");
        initSourceManager();
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        HandlerThread brThread = new HandlerThread(TAG + BR);
        brThread.start();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAvrcpController.ACTION_PLAYER_SETTING);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(ACTION_BROWSE_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAvrcpController.ACTION_ADDRESSED_PLAYER_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void updateMetaData() {
        if (mCurrentAudioInfo == null) {
            return;
        }
        try {
            synchronized (mCallbackListener) {
                int listenerCount = mCallbackListener.beginBroadcast();
                mCurrentAudioInfo.setPlayState(mPlayState);
                for (int index = 0; index < listenerCount; index++) {
                    mCallbackListener.getBroadcastItem(index)
                            .onMetadataChanged(mCurrentAudioInfo);
                }
                mCallbackListener.finishBroadcast();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    private final MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            LogUtil.debug(TAG, "onPlaybackStateChanged, mMediaSource : " + mMediaSource + ", isTransientLossFocus : " + mStreaming.isTransientLossFocus());
            if (mStreaming.isTransientLossFocus()) {
                LogUtil.debug(TAG, "audio source is not in bt music");
                if (state != null && state.getPosition() < MusicConstants.CURRENT_DURATION) {
                    updatePlayProgress(state);
                }
                return;
            }
            notifyListeners(state.getState());
            updatePlayProgress(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            LogUtil.debug(TAG, "onMetadataChanged");
            if (metadata == null) {
                return;
            }
            mCurrentAudioInfo = getMetadataItem(metadata);
            LogUtil.debug(TAG, "metadata : " + mCurrentAudioInfo.getAudioName());
            updateMetaData();
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            LogUtil.debug(TAG, "onAudioInfoChanged");
            // TODO nothing
        }
    };

    private synchronized void notifyListeners(int state) {
        LogUtil.debug(TAG, "notifyListeners play state : "
                + mPlayState + " newest play state : " + state);
        if (mIsServiceRestart) {
            return;
        }

        if (state == PlaybackState.STATE_FAST_FORWARDING
                || state == PlaybackState.STATE_REWINDING) {
            return;
        }

        if (mPlayState == state) {
            return;
        }
        mPlayState = state;
        if (!mIsPowerStandby) {
            SharedPreferencesHelps.saveAudioState(mContext, mPlayState);
        }

        if (mCurrentAudioInfo == null) {
            mCurrentAudioInfo = getMetaData();
        }
        if (null != mCurrentAudioInfo) {
            mCurrentAudioInfo.setPlayState(mPlayState);
        }

        LogUtil.debug(TAG, "broadcast music play state changed :"
                + state + "MediaSource :" + mMediaSource);
        broadcastPlayStateChanged(state);
    }

    private void broadcastPlayStateChanged(int state) {
        LogUtil.debug(TAG, "notify state :" + state + "MediaSource :" + mMediaSource);
        switch (state) {
            case PlaybackState.STATE_PLAYING:
                mStreaming.obtainMessage(A2dpAudioFocusHandler.SRC_PLAY).sendToTarget();
                break;
            case PlaybackState.STATE_FAST_FORWARDING:
            case PlaybackState.STATE_REWINDING:
                break;
            default:
                mStreaming.obtainMessage(A2dpAudioFocusHandler.SRC_PAUSE).sendToTarget();
                break;
        }
        try {
            synchronized (mCallbackListener) {
                int listenerCount = mCallbackListener.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    mCallbackListener.getBroadcastItem(index)
                            .onPlayStateChanged(state);
                }
                mCallbackListener.finishBroadcast();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    private void updatePlayProgress(PlaybackState playbackState) {
        if (mMediaController == null || playbackState == null) {
            return;
        }
        int playState = playbackState.getState();
        LogUtil.debug(TAG, "updatePlayProgress state : " + playState
                + "  position : " + playbackState.getPosition());
        if (playState == PlaybackState.STATE_PLAYING
                || playState == PlaybackState.STATE_FAST_FORWARDING
                || playState == PlaybackState.STATE_REWINDING) {
            if (mCurrentAudioInfo != null) {
                mCurrentAudioInfo.setPlayState(mPlayState);
                mCurrentAudioInfo.setLastPlayTime(playbackState.getPosition());
            }
        }
        synchronized (mCallbackListener) {
            int count = mCallbackListener.beginBroadcast();
            for (int index = 0; index < count; index++) {
                try {
                    mCallbackListener.getBroadcastItem(index)
                            .onPlayProgressChanged(playbackState.getPosition(),
                                    mCurrentAudioInfo);
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }
            mCallbackListener.finishBroadcast();
        }
    }

    private void broadcastAAConnectionStateChanged(boolean isConnection) {
        LogUtil.debug(TAG, "broadcastAAConnectionStateChanged isAAConnection:"
                + isConnection + "MediaSource :" + mMediaSource);
        try {
            synchronized (mCallbackListener) {
                int listenerCount = mCallbackListener.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    mCallbackListener.getBroadcastItem(index)
                            .onAAConnected(RemoteDevice.ANDROID_AUTO, isConnection);
                }
                mCallbackListener.finishBroadcast();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    private AudioInfoBean getMetadataItem(MediaMetadata mediaMetadata) {
        if (mediaMetadata != null) {
            AudioInfoBean metadataItem = new AudioInfoBean();
            metadataItem.setBtAudioType(AudioInfoBean.ITEM_TYPE_TRACK);
            metadataItem.setUserId(mediaMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
            metadataItem.setAudioName(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE));
            metadataItem.setAudioArtistName(
                    mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
            metadataItem.setAudioAlbumName(
                    mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            metadataItem.setAudioGenre(mediaMetadata.getString(MediaMetadata.METADATA_KEY_GENRE));
            long tmp = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER);
            metadataItem.setAudioTrackNumber(tmp == BtConstants.INT_NUMBER_ZERO
                    ? BtConstants.STR_NUMBER_ZERO : Long.toString(tmp));
            tmp = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            metadataItem.setAudioPlayTime(tmp == BtConstants.INT_NUMBER_ZERO
                    ? BtConstants.STR_NUMBER_ZERO : Long.toString(tmp));
            String genre = metadataItem.getAudioGenre();
            LogUtil.debug(TAG, "genre :" + genre + ",mCarAudioManager:" + mCarAudioManager);
            if (genre != null && !"".equals(genre) && mCarAudioManager != null) {
                AudioSetting setting = new AudioSetting(AudioSetting.AUDIO_SETTING_PRESET_EQ,
                        0, 0, 0);
                try {
                    int band = mCarAudioManager.getAudioSetting(setting);
                    LogUtil.debug(TAG, "band:" + band);
                    if (band == MusicConstants.SETTING_PRESET_EQ_SMART) {
                        MusicUtils.getInstance().setPresetEqForSmart(genre, mCarAudioManager);
                    }
                } catch (CarNotConnectedException ex) {
                    ex.printStackTrace();
                }
            }
            if (mMediaController != null) {
                PlaybackState playbackState = mMediaController.getPlaybackState();
                long position = playbackState == null
                        ? BtConstants.INT_NUMBER_ZERO : playbackState.getPosition();
                metadataItem.setLastPlayTime(position < BtConstants.INT_NUMBER_ZERO
                        ? BtConstants.INT_NUMBER_ZERO : position);
            }
            LogUtil.debug(TAG, "audio cover url:" + mediaMetadata.getString(MediaMetadata
                    .METADATA_KEY_ALBUM_ART_URI));
            metadataItem.setAudioCover(mediaMetadata.getString(MediaMetadata
                    .METADATA_KEY_ALBUM_ART_URI));
            return metadataItem;
        }
        return null;
    }

    /**
     * Get metadata.
     *
     * @return metadataItem
     */
    public AudioInfoBean getMetaData() {
        if (mIsAAConnection) {
            return null;
        }
        LogUtil.debug(TAG, "getMetaData: enter, address = ");
        AudioInfoBean metadataItem = null;
        if (mMediaController != null) {
            MediaMetadata mediaMetadata = mMediaController.getMetadata();
            if (mediaMetadata != null) {
                metadataItem = getMetadataItem(mediaMetadata);
            }
            mCurrentAudioInfo = metadataItem;
        }
        LogUtil.debug(TAG, "getMetaData: exit, ret = " + metadataItem);
        return metadataItem;
    }

    protected synchronized MediaController.TransportControls getTransportControls() {
        if (mTransportControls == null) {
            if (mMediaController != null) {
                mTransportControls = mMediaController.getTransportControls();
            }
        }
        return mTransportControls;
    }

    private void initSourceManager() {
        LogUtil.info(TAG, "PageWatcher.initAudioSourceManager");
        mAudioSourceManager = AudioSourceManager.getInstance(mContext);
        if (null != mAudioSourceManager) {
            mAudioSourceManager.registerAudioFocusChangedListener(onAudioSourceChangedListener);
            mMediaSource = mAudioSourceManager.getCurrentMediaSource();

        } else {
            LogUtil.warning(TAG, "initAudioSourceManager failed");
        }
    }

    private AudioSourceManager.OnAudioSourceChangedListener onAudioSourceChangedListener =
            mediaSourceType -> {
                LogUtil.debug(TAG, "mediaSourceType : " + mediaSourceType);
                mMediaSource = mediaSourceType;
            };

    @Override
    public void stopBtMusic() {
        LogUtil.debug(TAG, "stopBtMusic");
        pause();
        notifyListeners(PlaybackState.STATE_PAUSED);
    }

    @Override
    public void startBtMusic() {
        LogUtil.debug(TAG, "startBtMusic, isTransientLossFocus : "
                + mStreaming.isTransientLossFocus());
        play();
    }

    @Override
    public void mediaPrevious() {
        prev();
    }

    @Override
    public void mediaNext() {
        next();
    }

    @Override
    public void mediaFastForward() {
        // do nothing.
    }

    @Override
    public void mediaFastBack() {
        // do nothing.
    }

    @Override
    public void keyEventUp() {
        // do nothing.
    }

    @Override
    public void keyEventUp(int keyCode, boolean isLongPress) {
        LogUtil.debug(TAG, "keyEventUp keyCode : " + keyCode + " isLongPress : " + isLongPress);
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            fastForwardOrBack(false, BtConstants.PASS_THRU_CMD_ID_FF);
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            fastForwardOrBack(false, BtConstants.PASS_THRU_CMD_ID_REWIND);
        }
    }

    @Override
    public void keyEventDown(int keyCode, boolean isLongPress) {
        LogUtil.debug(TAG, "keyEventDown keyCode : " + keyCode + " isLongPress : " + isLongPress);
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            fastForwardOrBack(true, BtConstants.PASS_THRU_CMD_ID_FF);
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            fastForwardOrBack(true, BtConstants.PASS_THRU_CMD_ID_REWIND);
        }
    }

    @Override
    public void muteChanged(boolean isMute) {
        // TODO nothings
    }

    @Override
    public void muteChanged() {
        if (mPlayState == PlaybackState.STATE_PLAYING) {
            pause();
        } else {
            play();
        }
    }

    @Override
    public void resumePlay() {
        int mediaSource = mAudioSourceManager.getCurrentMediaSource();
        LogUtil.debug(TAG, "resumePlay, mediaSource : " + mediaSource + "mPlayState : " + mPlayState);
        if (mPlayState != PlaybackState.STATE_PLAYING) {
            play();
        }
    }

    /* Maintains media library for a peer Target device. */
    private class MediaLibrary {
        ArrayList<MediaBrowser.MediaItem> players = new ArrayList<MediaBrowser.MediaItem>();
        MediaBrowser.MediaItem currentBrowsePlayer = null;
        /* Only cache folder elements (album, artist, playlist, etc.) except tracks in now playing
         * list. */
        boolean nowPlayingCached = false;
        ArrayList<MediaBrowser.MediaItem> nowPlayingTracks =
                new ArrayList<MediaBrowser.MediaItem>();
        boolean albumsCached = false;
        ArrayList<MediaBrowser.MediaItem> albums = new ArrayList<MediaBrowser.MediaItem>();
        boolean artistsCached = false;
        ArrayList<MediaBrowser.MediaItem> artists = new ArrayList<MediaBrowser.MediaItem>();
        boolean playlistsCached = false;
        ArrayList<MediaBrowser.MediaItem> playlists = new ArrayList<MediaBrowser.MediaItem>();

        void reset() {
            players.clear();
            currentBrowsePlayer = null;
            nowPlayingCached = false;
            nowPlayingTracks.clear();
            albumsCached = false;
            albums.clear();
            artistsCached = false;
            artists.clear();
            playlistsCached = false;
            playlists.clear();
        }
    }

    private AudioInfoBean convertMediaItem(int type, MediaBrowser.MediaItem media) {
        MediaDescription description = media.getDescription();
        AudioInfoBean item = new AudioInfoBean();
        item.setBtAudioType(type);
        item.setUserId(description.getMediaId());
        item.setAudioName(description.getTitle().toString());
        LogUtil.debug(TAG, "convertMediaItem: type = " + type
                + ", title = " + item.getAudioName());
        return item;
    }

    private MediaBrowser.MediaItem getMediaByUid(String uid, List<MediaBrowser.MediaItem> list) {
        for (MediaBrowser.MediaItem item : list) {
            if (uid.equals(item.getDescription().getMediaId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * RegisterAudioCallbackListenser.
     *
     * @param btMusicCallback IBtMusicCallback
     */
    public void registerAudioCallbackListenser(IBtMusicCallback btMusicCallback) {
        if (null != mCallbackListener) {
            mCallbackListener.register(btMusicCallback);
        }
    }

    /**
     * UnRegisterAudioCallbackListenser.
     *
     * @param btMusicCallback IBtMusicCallback
     */
    public void unRegisterAudioCallbackListenser(IBtMusicCallback btMusicCallback) {
        if (null != mCallbackListener) {
            mCallbackListener.unregister(btMusicCallback);
        }
    }

    /**
     * Prev.
     */
    public synchronized boolean prev() {
        if (isNotAllowOperation()) {
            return false;
        }
        MediaController.TransportControls transportControls = getTransportControls();
        boolean ret = false;
        if (transportControls != null) {
            mStreaming.obtainMessage(A2dpAudioFocusHandler.SNK_PLAY).sendToTarget();
            transportControls.skipToPrevious();
            mHandler.removeMessages(TYPE_PLAY_MUSIC);
            mHandler.sendEmptyMessageDelayed(TYPE_PLAY_MUSIC, DELAY_PLAY_MUSIC);
            mPlayState = PlaybackState.STATE_NONE;
            ret = true;
        }
        LogUtil.debug(TAG, "prev: exit, ret =" + ret);
        return ret;
    }

    /**
     * Play.
     */
    public boolean play() {
        if (isNotAllowOperation()) {
            return false;
        }
        MediaController.TransportControls transportControls = getTransportControls();
        boolean ret = false;
        if (transportControls != null) {
            mStreaming.obtainMessage(A2dpAudioFocusHandler.SNK_PLAY).sendToTarget();
            transportControls.play();
            ret = true;
        }
        LogUtil.debug(TAG, "play: exit, ret=" + ret);
        return ret;
    }

    /**
     * Play from media id.
     */
    public boolean playFromMediaId(String mediaId) {
        if (isNotAllowOperation()) {
            return false;
        }
        MediaController.TransportControls transportControls = getTransportControls();
        boolean ret = false;
        if (transportControls != null) {
            mStreaming.obtainMessage(A2dpAudioFocusHandler.SNK_PLAY).sendToTarget();
            transportControls.playFromMediaId(mediaId, null);
            ret = true;
        }
        LogUtil.debug(TAG, "playFromMediaId: exit, ret=" + ret + "  mediaId : " + mediaId);
        return ret;
    }

    /**
     * Pause.
     */
    public boolean pause() {
        if (mIsAAConnection) {
            return true;
        }
        MediaController.TransportControls transportControls = getTransportControls();
        boolean ret = false;
        if (transportControls != null) {
            mStreaming.obtainMessage(A2dpAudioFocusHandler.SNK_PAUSE).sendToTarget();
            transportControls.pause();
            ret = true;
        }
        LogUtil.debug(TAG, "pause: exit, ret =" + ret);
        return ret;
    }

    private boolean isNotAllowOperation() {
        if (mIsAAConnection || mStreaming.isTransientLossFocus()) {
            return true;
        }
        return false;
    }

    /**
     * Next.
     */
    public synchronized boolean next() {
        if (isNotAllowOperation()) {
            return false;
        }
        MediaController.TransportControls transportControls = getTransportControls();
        boolean ret = false;
        if (transportControls != null) {
            mStreaming.obtainMessage(A2dpAudioFocusHandler.SNK_PLAY).sendToTarget();
            transportControls.skipToNext();
            mHandler.removeMessages(TYPE_PLAY_MUSIC);
            mHandler.sendEmptyMessageDelayed(TYPE_PLAY_MUSIC, DELAY_PLAY_MUSIC);
            mPlayState = PlaybackState.STATE_NONE;
            ret = true;
        }
        LogUtil.debug(TAG, "next: exit, ret =" + ret);
        return ret;
    }

    /**
     * FastForwardOrBack.
     */
    public void fastForwardOrBack(boolean down, int type) {
        LogUtil.debug(TAG, "fastForwardOrBack, down : " + down + ", type : " + type);
        if (mIsAAConnection) {
            return;
        }
        mHandler.removeMessages(TYPE_FAST_FORWARD_BACK);
        Message msg = new Message();
        msg.what = TYPE_FAST_FORWARD_BACK;
        Bundle msgBundle = new Bundle();
        msgBundle.putBoolean(BtConstants.HARD_KEY_ACTION, down);
        msgBundle.putInt(BtConstants.HARD_KEY_TYPE, type);
        msg.setData(msgBundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Get play mode.
     */
    public int getPlayMode() {
        mPlayerSetting = sBtManager.getPlayerSettings();
        if (mPlayerSetting != null) {
            int settings = mPlayerSetting.getSettings();
            if ((settings & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) != 0) {
                return mPlayerSetting.getSettingValue(
                        BluetoothAvrcpPlayerSettings.SETTING_REPEAT);
            }
        }
        return ERROR_CODE;
    }

    /**
     * Set play mode.
     */
    public void setPlayMode(int currentValue) {
        LogUtil.debug(TAG, "setPlayMode == " + currentValue);
        if (currentValue == BluetoothAvrcpPlayerSettings.STATE_OFF
                || currentValue == BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK
                || currentValue == BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK) {
            if (!sBtManager.setAvrcpSettings(BluetoothAvrcpPlayerSettings.SETTING_REPEAT,
                    currentValue)) {
                LogUtil.warning(TAG, "Repeat click set STATE_OFF warning ");
            }
        }
    }

    /**
     * Get paly state bt mediaController.
     *
     * @return boolean state
     */
    public boolean getPlayState() {
        LogUtil.debug(TAG, "getPlayState state : " + mPlayState);
        return (mPlayState == BtConstants.PLAY_STATE_PLAYING);
    }

    /**
     * Get current play list.
     *
     * @return boolean state
     */
    public List<AudioInfoBean> getCurrentPlayList() {
        LogUtil.debug(TAG, "getCurrentPlayList");
        return mAudioBeans;
    }

    /**
     * Get paly state bt mediaController.
     *
     * @return boolean state
     */
    public int getBluetoothState() {
        return mA2dpSinkState;
    }

    private int getPlayStateRet() {
        int ret = PlaybackState.STATE_NONE;
        if (mMediaController != null) {
            PlaybackState playbackState = mMediaController.getPlaybackState();
            if (playbackState != null) {
                ret = playbackState.getState();
            }
        }
        LogUtil.debug(TAG, "getPlayState: exit, ret =" + ret);
        return ret;
    }

    public boolean getAAConnectionState() {
        return mIsAAConnection;
    }

    // Accept play mode status displayed on HMI.
    private void controlPlaybackMode(int repeatStatus) {
        if (repeatStatus == ERROR_CODE) {
            LogUtil.warning(TAG, "get playback mode warning =="
                    + repeatStatus);
            return;
        }
        LogUtil.debug(TAG, "control playback mode : repeatStatus =="
                + repeatStatus);

        if (mCallbackListener == null) {
            return;
        }
        try {
            synchronized (mCallbackListener) {
                int listenerCount = mCallbackListener.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    switch (repeatStatus) {
                        case BluetoothAvrcpPlayerSettings.STATE_OFF:
                            mCallbackListener.getBroadcastItem(index)
                                    .onPlayerModeChanged(MusicConstants.MUSIC_MODE_ORDER, true);
                            break;
                        case BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK:
                            mCallbackListener.getBroadcastItem(index)
                                    .onPlayerModeChanged(MusicConstants.MUSIC_MODE_SINGLE, true);
                            break;
                        case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                            mCallbackListener.getBroadcastItem(index)
                                    .onPlayerModeChanged(MusicConstants.MUSIC_MODE_LOOP, true);
                            break;
                        default:
                            LogUtil.warning(TAG, "repeatStatus : " + repeatStatus);
                            break;
                    }
                }
                mCallbackListener.finishBroadcast();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * destroy.
     */
    public void destroy() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    private void registerSubscribe(String mediaId) {
        if (TextUtils.isEmpty(mediaId)) {
            return;
        }
        LogUtil.debug(TAG, "BtMusic list registerSubscribe mediaId : " + mediaId);
        mMediaBrowser.subscribe(mediaId, mPlayListCallback);
    }

    private void unRegisterSubscribe(String mediaId) {
        LogUtil.debug(TAG, "BtMusic list unRegisterSubscribe mediaId : " + mediaId);
        mMediaBrowser.unsubscribe(mMediaId, mPlayListCallback);
    }

    private MediaBrowser.SubscriptionCallback mPlayListCallback
            = new MediaBrowser.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(String parentId,
                                     List<MediaBrowser.MediaItem> playList) {
            super.onChildrenLoaded(parentId, playList);
            LogUtil.debug(TAG, "BtMusic list onChildrenLoaded list size : "
                    + playList.size() + "  parent id : " + parentId);
            if (parentId.equals(BtConstants.BT_PLAY_LIST_NOW_PLAYING)
                    || playList.size() <= 0) {
                mAudioBeans = generateData(playList);
                mHandler.removeMessages(TYPE_UPDATE_PLAY_LIST);
                mHandler.sendEmptyMessageDelayed(
                        TYPE_UPDATE_PLAY_LIST, DELAY_BT_LOAD_PLAY_LIST);
                return;
            }
            Message msg = new Message();
            msg.what = TYPE_SUBSCRIBE_PLAY_LIST;
            LogUtil.debug(TAG, "media id : " + mMediaId);
            if (mMediaId.equals(BtConstants.BT_PLAY_LIST_ROOT)) {
                msg.obj = BtConstants.BT_PLAY_LIST_PLAYING;
                if (mHandler.hasMessages(TYPE_SUBSCRIBE_PLAY_LIST, msg.obj)) {
                    LogUtil.debug(TAG, "BtMusic list has msg : PLAYER1");
                    return;
                }
                unRegisterSubscribe(BtConstants.BT_PLAY_LIST_ROOT);
                mHandler.sendMessageDelayed(msg, DELAY_MUSIC_PROGRESS_UPDATE);
            } else if (parentId.equals(BtConstants.BT_PLAY_LIST_PLAYING)) {
                msg.obj = BtConstants.BT_PLAY_LIST_NOW_PLAYING;
                if (mHandler.hasMessages(TYPE_SUBSCRIBE_PLAY_LIST, msg.obj)) {
                    LogUtil.debug(TAG, "BtMusic list has msg : NOW_PLAYING:1");
                    return;
                }
                unRegisterSubscribe(mMediaId);
                mHandler.sendMessageDelayed(msg, DELAY_MUSIC_PROGRESS_UPDATE);
            } else {
                // do nothing.
            }
        }

        @Override
        public void onChildrenLoaded(String parentId,
                                     List<MediaBrowser.MediaItem> children,
                                     Bundle options) {
            super.onChildrenLoaded(parentId, children, options);
            LogUtil.debug(TAG, "onChildrenLoaded3");
        }

        @Override
        public void onError(String parentId, Bundle options) {
            super.onError(parentId, options);
        }

        @Override
        public void onError(String parentId) {
            super.onError(parentId);
        }
    };

    protected void notifyPlayList(List<AudioInfoBean> audioInfoBeans) {
        synchronized (mCallbackListener) {
            int count = mCallbackListener.beginBroadcast();
            for (int index = 0; index < count; index++) {
                try {
                    mCallbackListener.getBroadcastItem(index)
                            .onMediaItemListRetrieved(audioInfoBeans.size(), audioInfoBeans);
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }
            mCallbackListener.finishBroadcast();
        }
    }

    private ArrayList<AudioInfoBean> generateData(List<MediaBrowser.MediaItem> playList) {
        ArrayList<AudioInfoBean> audioInfoBeans = new ArrayList<>();
        for (MediaBrowser.MediaItem item : playList) {
            AudioInfoBean infoBean = new AudioInfoBean();
            MediaDescription description = item.getDescription();
            infoBean.setUserId(description.getMediaId());
            infoBean.setAudioName(description.getTitle().toString());
            CharSequence artistName = description.getSubtitle();
            infoBean.setAudioArtistName(artistName == null
                    ? BtConstants.BT_MUSIC_DEFAULT_NAME : artistName.toString());
            audioInfoBeans.add(infoBean);
            LogUtil.info(TAG, "audio name : " + description.getTitle()
                    + "  mediaId : " + description.getMediaId());
        }
        return audioInfoBeans;
    }

    /**
     * Regisiter media call back.
     */
    public void registerCallback() {
        LogUtil.debug(TAG, "registerCallback");
        if (mMediaController != null && mMediaControllerCallback != null) {
            mMediaController.registerCallback(mMediaControllerCallback);
        }
    }

    /**
     * UnRegisiter media call back.
     */
    public void unRegisterCallback() {
        LogUtil.debug(TAG, "unRegisterCallback");
        if (mMediaController != null && mMediaControllerCallback != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
    }

    /**
     * Notify AndroidAuto connection state changed.
     *
     * @param isConnection flag AA connection
     */
    public void notifyAAConnectionState(boolean isConnection) {
        LogUtil.debug(TAG, "notifyAAConnectionState isConnection : " + isConnection);
        mIsAAConnection = isConnection;
        broadcastAAConnectionStateChanged(isConnection);
        if (isConnection) {
            broadcastPlayStateChanged(PlaybackState.STATE_PAUSED);
        }
    }

    public boolean getAvrcpConnectionState() {
        return mIsAvrcpConnection;
    }

    private void powerStateChanged(int state) {
        if (state != mCurrentPowerState) {
            LogUtil.debug(TAG, "mCurrentPowerState : " + mCurrentPowerState
                    + ",    state : " + state);
            if (state == MusicConstants.POWER_STAND_BY || state == MusicConstants.POWER_VOLTAGE) {
                mIsPowerStandby = true;
            } else if ((state == MusicConstants.POWER_RUN || state == MusicConstants.POWER_TEMP_RUN)
                    && mCurrentPowerState == MusicConstants.POWER_STAND_BY) {
                mIsServiceRestart = true;
                mIsPowerStandby = false;
            }
            mCurrentPowerState = state;
        }
    }
}
