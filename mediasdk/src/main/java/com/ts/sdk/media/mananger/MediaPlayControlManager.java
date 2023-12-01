package com.ts.sdk.media.mananger;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;


import com.ts.sdk.media.IPlayStatusBinderInterface;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.callback.IBtMusicCallback;
import com.ts.sdk.media.callback.IMediaSourceCallback;
import com.ts.sdk.media.callback.IMusicPlayerEventListener;
import com.ts.sdk.media.callback.IOnlineMusicCallback;
import com.ts.sdk.media.constants.MusicConstants;
import com.ts.sdk.media.constants.ServiceConstants;
import com.ts.sdk.media.constants.VideoConstants;
import com.ts.sdk.media.contractinterface.IMediaServiceListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaPlayControlManager extends BaseManager {

    private static final String TAG = MediaPlayControlManager.class.getSimpleName();
    private static final int MSG_RETRY_CONNECT_SERVICE = 0;
    private static final int TIME_RETRY_CONNECT_SERVICE = 2000;
    private static final int MAX_RETRY_CONNECT_TIME = 4;
    private static final int MEDIA_SOURCE_BT_ID = 5;
    private static final int MEDIA_SOURCE_USB_ID = 6;
    private static final int MEDIA_SOURCE_ONLINE_ID = 14;

    /**
     * Service listener.
     */
    private static List<IMediaServiceListener> sListener = new ArrayList<>();

    /**
     * Is service connect flag.
     */
    private static boolean sIsServiceCon = false;

    public static final int NO_PLAYING = 0;

    public static final int USB_VIDEO_PLAYING = 1;

    public static final int USB_MUSIC_PLAYING = 2;

    public static final int ONLINE_MUSIC_PLAYING = 3;

    public static final int BT_MUSIC_PLAYING = 4;

    public static final int CP_MUSIC_PLAYING = 5;

    public static final int AA_MUSIC_PLAYING = 6;

    /**
     * Instance.
     */
    private static volatile MediaPlayControlManager sInstance = null;

    private UsbVideoManager mUsbVideoManager;
    private BtMusicManager mBtMusicManager;
    private UsbMusicManager mUsbMusicManager;
    private OnlineMusicManager mOnlineMusicManager;
    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;
    private boolean mUsbMusicConnected = false;
    private boolean mBtMusicConnected = false;
    private boolean mUsbVideoConnected = false;
    private boolean mOnlineMusicConnected = false;
    private int mRetryConnectTime = 0;
    private int mCurrentMediaSource = 0;
    /**
     * Binder object.
     */
    private IPlayStatusBinderInterface mIPlayBinder;
    private IAudioStatusListener mAudioStatusListener;
    private IMusicPlayerEventListener mMusicPlayerEventListener;
    private IBtMusicCallback mBtMusicCallback;
    private IOnlineMusicCallback mOnlineMusicCallback;
    private int mCurrentPlayer = NO_PLAYING;
    private AudioInfoBean mCpAaAudioInfo;
    private boolean mMediaConnected = false;
    private int mTargetPlayer = NO_PLAYING;

    private ComponentName componentNameRemoteUi() {
        return new ComponentName(ServiceConstants.CP_AA_SERVICE_PKG_NAME, ServiceConstants.CP_AA_SERVICE_CLS_NAME);
    }

    private final Handler mReConnectHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_RETRY_CONNECT_SERVICE) {
                mediaReConnect();
            }
            return false;
        }
    });

    private MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            Log.d(TAG, "onPlaybackStateChanged: " + state);
            if (state == null) {
                return;
            }
            mCpAaAudioInfo.setAudioPlayTime(Long.toString(state.getPosition()));
            @SuppressLint("WrongConstant")
            boolean playStateChanged = state.getState() != mCpAaAudioInfo.getPlayState();
            if (playStateChanged) {
                mCpAaAudioInfo.setPlayState(state.getState());
            }

            if (mAudioStatusListener != null && (mCurrentMediaSource == MusicConstants
                    .CP_MEDIA_SOURCE_CODE || mCurrentMediaSource == MusicConstants
                    .AA_MEDIA_SOURCE_CODE)) {
                Log.d(TAG, "onPlaybackStateChanged: \n" + mCpAaAudioInfo.toString());

                if (playStateChanged) {
                    mAudioStatusListener.onPlayStateChanged(state.getState());
                }
                if (mCurrentMediaSource == MusicConstants.CP_MEDIA_SOURCE_CODE) {
                    if (mCurrentPlayer != CP_MUSIC_PLAYING) {
                        mCurrentPlayer = CP_MUSIC_PLAYING;
                        mAudioStatusListener.onPlayerChanged(CP_MUSIC_PLAYING);
                        setCurrentPlayer(CP_MUSIC_PLAYING);
                    }
                } else if (mCurrentMediaSource == MusicConstants.AA_MEDIA_SOURCE_CODE) {
                    if (mCurrentPlayer != AA_MUSIC_PLAYING) {
                        mCurrentPlayer = AA_MUSIC_PLAYING;
                        mAudioStatusListener.onPlayerChanged(AA_MUSIC_PLAYING);
                        setCurrentPlayer(AA_MUSIC_PLAYING);
                    }
                }
                mCpAaAudioInfo.setPlayerType(mCurrentPlayer);
                mAudioStatusListener.onProgressChanged(mCpAaAudioInfo.getAudioDuration(),
                        state.getPosition(), 0, mCpAaAudioInfo);
            }
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            Log.d(TAG, "onMetadataChanged: " + metadata);
            if (metadata == null) {
                return;
            }
            mCpAaAudioInfo.setAudioName(metadata.getString(
                    MediaMetadata.METADATA_KEY_TITLE));
            mCpAaAudioInfo.setAudioArtistName(metadata.getString(
                    MediaMetadata.METADATA_KEY_ARTIST));
            mCpAaAudioInfo.setAudioAlbumName(metadata.getString(
                    MediaMetadata.METADATA_KEY_ALBUM));
            mCpAaAudioInfo.setCpAlbumArt(metadata.getBitmap(
                    MediaMetadata.METADATA_KEY_ALBUM_ART));
            mCpAaAudioInfo.setAudioDuration(metadata.getLong(
                    MediaMetadata.METADATA_KEY_DURATION));
            Log.d(TAG, "onMetadataChanged: \n" + mCpAaAudioInfo.toString());
            if (mAudioStatusListener != null && (mCurrentMediaSource == MusicConstants
                    .CP_MEDIA_SOURCE_CODE || mCurrentMediaSource == MusicConstants
                    .AA_MEDIA_SOURCE_CODE)) {
                if (mCurrentMediaSource == MusicConstants.CP_MEDIA_SOURCE_CODE) {
                    mCurrentPlayer = CP_MUSIC_PLAYING;
                    mAudioStatusListener.onPlayerChanged(CP_MUSIC_PLAYING);
                    setCurrentPlayer(CP_MUSIC_PLAYING);
                } else if (mCurrentMediaSource == MusicConstants.AA_MEDIA_SOURCE_CODE) {
                    mCurrentPlayer = AA_MUSIC_PLAYING;
                    mAudioStatusListener.onPlayerChanged(AA_MUSIC_PLAYING);
                    setCurrentPlayer(AA_MUSIC_PLAYING);
                }
                mCpAaAudioInfo.setPlayerType(mCurrentPlayer);
                mAudioStatusListener.onPlayMusicInfoChanged(mCpAaAudioInfo);
            }
        }
    };

    private MediaPlayControlManager(Context context) {
        super(context);
        Log.d(TAG, "MediaPlayControlManager=====mUsbVideoManager");
        mUsbVideoManager = UsbVideoManager.getInstance(context, new IMediaServiceListener() {
            @Override
            public void onServiceConnected(BaseManager manager) {
                Log.d(TAG, "MediaPlayControlManager=====onServiceConnected==");
                mUsbVideoConnected = true;
                checkServiceConnected();
            }

            @Override
            public void onServiceDisconnected() {
                Log.d(TAG, "MediaPlayControlManager=====onServiceDisconnected===");
                mUsbVideoConnected = false;
            }
        });
        Log.d(TAG, "mUsbMusicManager=====onServiceConnected");
        mUsbMusicManager = UsbMusicManager.getInstance(context, new IMediaServiceListener() {
            @Override
            public void onServiceConnected(BaseManager manager) {
                Log.d(TAG, "mUsbMusicManager=====onServiceConnected==");
                mUsbMusicConnected = true;
                checkServiceConnected();
            }

            @Override
            public void onServiceDisconnected() {
                Log.d(TAG, "mUsbMusicManager=====onServiceDisconnected===");
                mUsbMusicConnected = false;
            }
        });
        Log.d(TAG, "BtMusicManager=====onServiceConnected");
        mBtMusicManager = BtMusicManager.getInstance(context, new IMediaServiceListener() {
            @Override
            public void onServiceConnected(BaseManager manager) {
                Log.d(TAG, "BtMusicManager=====onServiceConnected===");
                mBtMusicConnected = true;
                checkServiceConnected();
            }

            @Override
            public void onServiceDisconnected() {
                Log.d(TAG, "BtMusicManager=====onServiceDisconnected===");
                mBtMusicConnected = false;
            }
        });

        mOnlineMusicManager = OnlineMusicManager.getInstance(context, new IMediaServiceListener() {
            @Override
            public void onServiceConnected(BaseManager manager) {
                mOnlineMusicConnected = true;
                checkServiceConnected();
            }

            @Override
            public void onServiceDisconnected() {
                mOnlineMusicConnected = false;
            }
        });

        // for CarPlay & Android Auto MediaBrowserService
        mCpAaAudioInfo = new AudioInfoBean();
        mMediaBrowser = new MediaBrowser(context, componentNameRemoteUi(),
                new MediaBrowser.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        super.onConnected();
                        Log.d(TAG, "MediaBrowser onConnected: ");
                        mMediaConnected = true;
                        mRetryConnectTime = 0;
                        if (mReConnectHandler.hasMessages(MSG_RETRY_CONNECT_SERVICE)) {
                            mReConnectHandler.removeMessages(MSG_RETRY_CONNECT_SERVICE);
                        }
                        mMediaController = new MediaController(context,
                                mMediaBrowser.getSessionToken());
                        mMediaController.registerCallback(mMediaControllerCallback);
                        if (mMediaController.getMetadata() != null) {
                            Log.d(TAG, "MediaBrowser onConnected: metadata exist!");
                            mMediaControllerCallback.onMetadataChanged(
                                    mMediaController.getMetadata());
                        }
                    }

                    @Override
                    public void onConnectionSuspended() {
                        super.onConnectionSuspended();
                        Log.d(TAG, "MediaBrowser onConnectionSuspended: ");
                        mMediaConnected = false;
                        mReConnectHandler.sendEmptyMessageDelayed(MSG_RETRY_CONNECT_SERVICE,
                                TIME_RETRY_CONNECT_SERVICE);
                    }

                    @Override
                    public void onConnectionFailed() {
                        super.onConnectionFailed();
                        Log.d(TAG, "MediaBrowser onConnectionFailed: ");
                        mMediaConnected = false;
                        mReConnectHandler.sendEmptyMessageDelayed(MSG_RETRY_CONNECT_SERVICE,
                                TIME_RETRY_CONNECT_SERVICE);
                    }
                }, null);
        mMediaBrowser.connect();
    }

    private void mediaReConnect() {
        if (!mMediaConnected && mRetryConnectTime < MAX_RETRY_CONNECT_TIME) {
            mMediaBrowser.connect();
            mRetryConnectTime += 1;
        } else {
            mRetryConnectTime = 0;
            Log.d(TAG, "CarPlay MediaBrowser Connection Failed in 10s");
            if (mReConnectHandler.hasMessages(MSG_RETRY_CONNECT_SERVICE)) {
                mReConnectHandler.removeMessages(MSG_RETRY_CONNECT_SERVICE);
            }
        }
    }

    @Override
    protected String getAction() {
        return ServiceConstants.SERVICE_PLAY_STATUS_ACTION;
    }

    @Override
    protected void destroy() {
        // TODO destroy
    }

    /**
     * Get Instance.
     *
     * @param context the context of the app
     */
    public static synchronized MediaPlayControlManager getInstance(Context context,
                                                                   IMediaServiceListener listener) {
        if (listener != null) {
            sListener.add(listener);
        }
        if (sInstance == null) {
            sInstance = new MediaPlayControlManager(context);
        }
        if (sIsServiceCon) {
            for (int i = 0; i < sListener.size(); i++) {
                sListener.get(i).onServiceConnected(sInstance);
            }
        }
        return sInstance;
    }

    @Override
    protected void setBinder(IBinder binder) {
        super.setBinder(binder);
        synchronized (MediaPlayControlManager.class) {
            if (binder != null) {
                mIPlayBinder = IPlayStatusBinderInterface.Stub.asInterface(binder);
                sIsServiceCon = true;
                registerMediaSourceCallback(new IMediaSourceCallback.Stub() {
                    @Override
                    public void onMediaSourceChange(int source) {
                        Log.d(TAG, "onMediaSourceChange : " + source);
                        if (mCurrentMediaSource != source) {
                            mCurrentMediaSource = source;
                            if (mAudioStatusListener != null) {
                                mAudioStatusListener
                                        .onPlayStateChanged(MusicConstants.PLAYER_STATUS_PAUSE);
                            }
                        }
                    }
                });
                checkServiceConnected();
            } else {
                mIPlayBinder = null;
                sIsServiceCon = false;
                if (sListener != null) {
                    for (int i = 0; i < sListener.size(); i++) {
                        sListener.get(i).onServiceDisconnected();
                    }
                }
            }
        }
    }

    /**
     * Play next video or audio.
     */
    public void next() {
        Log.d(TAG, "Media SDK operation: next  " + getCurrentPlayer());
        try {
            if (mCurrentMediaSource == MusicConstants.USB_VIDEO_SOURCE_CODE
                    && isVideoForeGround()) {
                return;
            }
            switch (getCurrentPlayer()) {
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.playNextMusic();
                    break;
                case BT_MUSIC_PLAYING:
                    mBtMusicManager.next();
                    break;
                case ONLINE_MUSIC_PLAYING:
                    mOnlineMusicManager.next();
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Play prev video or audio.
     */
    public void prev() {
        Log.d(TAG, "Media SDK operation: prev  " + getCurrentPlayer());
        try {
            if (mCurrentMediaSource == MusicConstants.USB_VIDEO_SOURCE_CODE
                    && isVideoForeGround()) {
                return;
            }
            switch (getCurrentPlayer()) {
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.playLastMusic();
                    break;
                case BT_MUSIC_PLAYING:
                    mBtMusicManager.prev();
                    break;
                case ONLINE_MUSIC_PLAYING:
                    mOnlineMusicManager.prev();
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Pause play video or audio.
     */
    public void pause() {
        Log.d(TAG, "Media SDK operation: pause  " + getCurrentPlayer());
        try {
            if (mCurrentMediaSource == MusicConstants.USB_VIDEO_SOURCE_CODE
                    && isVideoForeGround()) {
                return;
            }
            switch (getCurrentPlayer()) {
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.pause();
                    break;
                case BT_MUSIC_PLAYING:
                    mBtMusicManager.pause();
                    break;
                case ONLINE_MUSIC_PLAYING:
                    mOnlineMusicManager.pause();
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Pause audio.
     */
    public void pause(int player) {
        Log.d(TAG, "Media SDK operation: pause " + player);
        try {
            switch (player) {
                case USB_MUSIC_PLAYING:
                    if (mUsbMusicManager != null) {
                        mUsbMusicManager.pause();
                    }
                    break;
                case BT_MUSIC_PLAYING:
                    if (mBtMusicManager != null) {
                        mBtMusicManager.pause();
                    }
                    break;
                case ONLINE_MUSIC_PLAYING:
                    if (mOnlineMusicManager != null) {
                        mOnlineMusicManager.pause();
                    }
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Play audio.
     */
    public void play(int player) {
        Log.d(TAG, "Media SDK operation: play" + player);
        mTargetPlayer = player;
        try {
            switch (player) {
                case USB_MUSIC_PLAYING:
                    if (mUsbMusicManager != null) {
                        mUsbMusicManager.play();
                    }
                    break;
                case BT_MUSIC_PLAYING:
                    if (mBtMusicManager != null) {
                        mBtMusicManager.play();
                    }
                    break;
                case ONLINE_MUSIC_PLAYING:
                    if (mBtMusicManager != null) {
                        mOnlineMusicManager.resume();
                    }
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Resume play video or audio.
     */
    public void resume() {
        int player = getCurrentPlayer();
        Log.d(TAG, "Media SDK operation: resume  " + player);
        try {
            if (mCurrentMediaSource == MusicConstants.USB_VIDEO_SOURCE_CODE
                    && isVideoForeGround()) {
                return;
            }
            if (player == NO_PLAYING) {
                player = mTargetPlayer;
            }
            switch (player) {
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.play();
                    break;
                case BT_MUSIC_PLAYING:
                    mBtMusicManager.play();
                    break;
                case ONLINE_MUSIC_PLAYING:
                    mOnlineMusicManager.resume();
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set audio play mode.
     */
    public void setPlayMode(int mode) {
        Log.d(TAG, "Media SDK operation: setPlayMode");
        try {
            switch (getCurrentPlayer()) {
                case USB_VIDEO_PLAYING:
                    // TODO nothing
                    break;
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.setPlayerMode(mode);
                    break;
                case BT_MUSIC_PLAYING:
                    mBtMusicManager.setPlayMode(mode);
                    break;
                default:
                    break;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Fast forward.
     */
    public void fastForward() {
        Log.d(TAG, "Media SDK operation: fast forward");
        try {
            switch (getCurrentPlayer()) {
                case USB_VIDEO_PLAYING:
                    // TODO nothing
                    break;
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.fastForward();
                    break;
                case BT_MUSIC_PLAYING:
                    // TODO nothing
                    break;
                default:
                    break;
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Fast back.
     */
    public void fastBack() {
        Log.d(TAG, "Media SDK operation: fast back");
        try {
            switch (getCurrentPlayer()) {
                case USB_VIDEO_PLAYING:
                    // TODO nothing
                    break;
                case USB_MUSIC_PLAYING:
                    mUsbMusicManager.fastBack();
                    break;
                case BT_MUSIC_PLAYING:
                    // TODO nothing
                    break;
                default:
                    break;
            }

        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get Current player.
     */
    public int getCurrentPlayer() {
        try {
            if (mIPlayBinder != null) {
                mCurrentPlayer = mIPlayBinder.getCurrentPlayer();
                return mCurrentPlayer;
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
        return NO_PLAYING;
    }

    /**
     * Set player to media service.
     */
    public void setCurrentPlayer(int player) {
        try {
            if (mIPlayBinder != null) {
                mIPlayBinder.setCurrentPlayer(player);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    private boolean isVideoForeGround() {
        try {
            if (mIPlayBinder != null) {
                return mIPlayBinder.isVideoForeGround();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    /**
     * Add audio play status listener.
     *
     * @param listener IAudioStatusListener
     */
    public void addAudioStatusListener(IAudioStatusListener listener) {
        mAudioStatusListener = listener;
        initListener();
    }

    /**
     * Remove audio play status listener.
     */
    public void removeAudioStatusListener() {
        mAudioStatusListener = null;
        try {
            mOnlineMusicManager.unRegisterOnlineMusicListener(mOnlineMusicCallback);
            mUsbMusicManager.removePlayerListener(mMusicPlayerEventListener);
            mBtMusicManager.unRegisterAudioCallbackListenser(mBtMusicCallback);
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set Tab position to media service.
     */
    public void setCurrentTabPosition(int position) {
        try {
            if (mIPlayBinder != null) {
                mIPlayBinder.setCurrentMusicTab(position);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Get Tab position from media service.
     */
    public int getCurrentTabPosition() {
        try {
            if (mIPlayBinder != null) {
                return mIPlayBinder.getCurrentMusicTab();
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
        return NO_PLAYING;
    }

    private void initListener() {
        mMusicPlayerEventListener = new IMusicPlayerEventListener.Stub() {
            @Override
            public void onMusicPlayerState(int playerState, String message) {
                if (MusicConstants.PLAYER_INTERNAL_ERROR.equals(message)
                        && mCurrentPlayer == USB_MUSIC_PLAYING) {
                    if (mAudioStatusListener != null) {
                        mAudioStatusListener.onProgressChanged(0, 0, 0, null);
                    }
                    mCurrentPlayer = NO_PLAYING;
                    return;
                }
                if (mCurrentMediaSource != MEDIA_SOURCE_USB_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    mAudioStatusListener.onPlayStateChanged(playerState);
                }
            }

            @Override
            public void onPrepared(long totalDuration) {
                // TODO nothing
            }

            @Override
            public void onInfo(int event, int extra) {
                // TODO nothing
            }

            @Override
            public void onPlayMusicOnInfo(AudioInfoBean musicInfo, int position) {
                if (mIPlayBinder != null) {
                    try {
                        if (mIPlayBinder.getCurrentMediaSource() != MEDIA_SOURCE_USB_ID) {
                            return;
                        }
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    if (mCurrentMediaSource != MEDIA_SOURCE_USB_ID) {
                        return;
                    }
                }
                if (mAudioStatusListener != null) {
                    mCurrentPlayer = USB_MUSIC_PLAYING;
                    mAudioStatusListener.onPlayerChanged(USB_MUSIC_PLAYING);
                    setCurrentPlayer(USB_MUSIC_PLAYING);
                    if (musicInfo != null) {
                        musicInfo.setPlayerType(mCurrentPlayer);
                    }
                    mAudioStatusListener.onPlayMusicInfoChanged(musicInfo);
                }
            }

            @Override
            public void onMusicPathInvalid(AudioInfoBean musicInfo, int position) {
                // TODO nothing
            }

            @Override
            public void onTaskRuntime(long totalDuration, long currentDuration,
                                      int bufferProgress, AudioInfoBean musicInfo) {
                if (mAudioStatusListener != null) {
                    if (mCurrentPlayer != USB_MUSIC_PLAYING) {
                        mCurrentPlayer = USB_MUSIC_PLAYING;
                        mAudioStatusListener.onPlayerChanged(USB_MUSIC_PLAYING);
                        setCurrentPlayer(USB_MUSIC_PLAYING);
                    }
                    if (musicInfo != null) {
                        musicInfo.setPlayerType(mCurrentPlayer);
                    }
                    mAudioStatusListener.onProgressChanged(totalDuration,
                            currentDuration, bufferProgress, musicInfo);
                }
            }

            @Override
            public void onPlayerConfig(int playMode, boolean isToast) {
                // TODO nothing
            }
        };
        mBtMusicCallback = new IBtMusicCallback.Stub() {
            @Override
            public void onServiceStateChanged(boolean connected) {
                // TODO nothing
            }

            @Override
            public void onPlayProgressChanged(long progress, AudioInfoBean audioInfo) {
                if (mCurrentMediaSource != MEDIA_SOURCE_BT_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    if (mCurrentPlayer != BT_MUSIC_PLAYING) {
                        mCurrentPlayer = BT_MUSIC_PLAYING;
                        mAudioStatusListener.onPlayerChanged(BT_MUSIC_PLAYING);
                        setCurrentPlayer(BT_MUSIC_PLAYING);
                    }
                    if (audioInfo != null) {
                        audioInfo.setPlayerType(mCurrentPlayer);
                        mAudioStatusListener.onProgressChanged(Long.parseLong(
                                audioInfo.getAudioPlayTime()), progress, 0, audioInfo);
                    }
                }
            }

            @Override
            public void onPlayStateChanged(int state) {
                if (mCurrentMediaSource != MEDIA_SOURCE_BT_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    mAudioStatusListener.onPlayStateChanged(state);
                }
            }

            @Override
            public void onMetadataChanged(AudioInfoBean metadata) {
                if (mCurrentMediaSource != MEDIA_SOURCE_BT_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    mCurrentPlayer = BT_MUSIC_PLAYING;
                    mAudioStatusListener.onPlayerChanged(BT_MUSIC_PLAYING);
                    setCurrentPlayer(BT_MUSIC_PLAYING);
                    if (metadata != null) {
                        metadata.setPlayerType(mCurrentPlayer);
                    }
                    mAudioStatusListener.onPlayMusicInfoChanged(metadata);
                }
            }

            @Override
            public void onPlayPositionChanged(int position) {
                // TODO nothing
            }

            @Override
            public void onConnectionStateChanged(int state) {
                if (state == MusicConstants.BT_STATE_DISCONNECTED
                        && mCurrentPlayer == BT_MUSIC_PLAYING) {
                    if (!checkBluetoothStatus()) {
                        if (mAudioStatusListener != null) {
                            mAudioStatusListener.onProgressChanged(0, 0, 0, null);
                        }
                        mCurrentPlayer = NO_PLAYING;
                    }
                }
            }

            @Override
            public void onMediaItemListRetrieved(int handle, List<AudioInfoBean> list) {
                // TODO nothing
            }

            @Override
            public void onPlayerModeChanged(int repeatMode, boolean shuffle) {
                // TODO nothing
            }

            @Override
            public void onMediaBrowserConnected() {
                // TODO nothing
            }

            @Override
            public void onAAConnected(int type, boolean isConnected) throws RemoteException {
                // TODO nothing
            }
        };
        mOnlineMusicCallback = new IOnlineMusicCallback.Stub() {
            @Override
            public void onMusicChange(AudioInfoBean musicInfo) {
                if (mCurrentMediaSource != MEDIA_SOURCE_ONLINE_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    mCurrentPlayer = ONLINE_MUSIC_PLAYING;
                    mAudioStatusListener.onPlayerChanged(ONLINE_MUSIC_PLAYING);
                    setCurrentPlayer(ONLINE_MUSIC_PLAYING);
                    if (musicInfo != null) {
                        musicInfo.setPlayerType(mCurrentPlayer);
                    }
                    mAudioStatusListener.onPlayMusicInfoChanged(musicInfo);
                }
            }

            @Override
            public void onMusicState(int playState) {
                if (mCurrentMediaSource != MEDIA_SOURCE_ONLINE_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    mAudioStatusListener.onPlayStateChanged(playState);
                }
            }

            @Override
            public void onMusicProgress(int progress, AudioInfoBean musicInfo) {
                if (mCurrentMediaSource != MEDIA_SOURCE_ONLINE_ID) {
                    return;
                }
                if (mAudioStatusListener != null) {
                    if (mCurrentPlayer != ONLINE_MUSIC_PLAYING) {
                        mCurrentPlayer = ONLINE_MUSIC_PLAYING;
                        mAudioStatusListener.onPlayerChanged(ONLINE_MUSIC_PLAYING);
                        setCurrentPlayer(ONLINE_MUSIC_PLAYING);
                    }
                    if (musicInfo != null) {
                        musicInfo.setPlayerType(mCurrentPlayer);
                        mAudioStatusListener.onProgressChanged(musicInfo.getAudioDuration(),
                                progress, 0, musicInfo);
                    }
                }
            }

            @Override
            public void onBufferingUpdate(int progress) {
                // TODO nothing
            }
        };
        try {
            mOnlineMusicManager.registerOnlineMusicListener(mOnlineMusicCallback);
            mUsbMusicManager.addOnPlayerEventListener(mMusicPlayerEventListener);
            mBtMusicManager.registerAudioCallbackListenser(mBtMusicCallback);
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Check service link status.
     */
    private void checkServiceConnected() {
        if (mOnlineMusicConnected && mBtMusicConnected && mUsbMusicConnected
                && mUsbVideoConnected && sIsServiceCon) {
            if (sListener != null) {
                for (int i = 0; i < sListener.size(); i++) {
                    sListener.get(i).onServiceConnected(sInstance);
                }
            }
        }
    }

    private Boolean checkBluetoothStatus() {
        if (mBtMusicManager == null) {
            return false;
        }
        try {
            int state = mBtMusicManager.getBluetoothState();
            return state == MusicConstants.BT_STATE_CONNECTED;
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    /**
     * Get Usb Device State.
     */
    public Map<String, Boolean> getUsbDeviceState() throws RemoteException {
        Map<String, Boolean> usbDevicesState = new HashMap<String, Boolean>();
        usbDevicesState.put(VideoConstants.USB_1_NAME, false);
        usbDevicesState.put(VideoConstants.USB_2_NAME, false);
        if (mUsbMusicManager == null) {
            return usbDevicesState;
        }
        List<UsbDevicesInfoBean> devicesInfo = mUsbMusicManager.getUsbDevices();
        for (UsbDevicesInfoBean device : devicesInfo) {
            if (device != null && VideoConstants.USB_1_PORT.equals(device.getPort())) {
                usbDevicesState.put(VideoConstants.USB_1_NAME, true);
            } else if (device != null && VideoConstants.USB_2_PORT.equals(device.getPort())) {
                usbDevicesState.put(VideoConstants.USB_2_NAME, true);
            }
        }
        return usbDevicesState;
    }

    /**
     * Search songs by singer and song.
     */
    public List<AudioInfoBean> searchAudio(String singer, String song) {
        try {
            if (mIPlayBinder != null) {
                return mIPlayBinder.searchAudio(singer, song);
            }
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    /**
     * Start playing a new audio.
     */
    public void startPlayMusic(AudioInfoBean audio) {
        if (mUsbMusicManager != null) {
            try {
                mUsbMusicManager.startPlayMusic(audio);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Get Music Play State.
     */
    public Map<String, Boolean> getMusicPlayState() throws RemoteException {
        Map<String, Boolean> musicPlayState = new HashMap<String, Boolean>();
        musicPlayState.put(MusicConstants.BT_MUSIC_NAME, false);
        musicPlayState.put(MusicConstants.USB_MUSIC_NAME, false);
        musicPlayState.put(MusicConstants.ONLINE_MUSIC_NAME, false);
        musicPlayState.put(MusicConstants.USB_VIDEO_NAME, false);
        if (mBtMusicManager != null) {
            musicPlayState.put(MusicConstants.BT_MUSIC_NAME, mBtMusicManager.getPlayState());
        }
        if (mUsbMusicManager != null) {
            musicPlayState.put(MusicConstants.USB_MUSIC_NAME,
                    mUsbMusicManager.getPlayerState() == MusicConstants.PLAYER_STATUS_START);
        }
        if (mOnlineMusicManager != null) {
            musicPlayState.put(MusicConstants.ONLINE_MUSIC_NAME, mOnlineMusicManager
                    .getPlayerStatus() == MusicConstants.PLAYER_STATUS_START);
        }
        if (mUsbVideoManager != null) {
            musicPlayState.put(MusicConstants.USB_VIDEO_NAME, mUsbVideoManager.getPlayState());
        }
        return musicPlayState;
    }

    /**
     * Register MediaSource listener.
     *
     * @param callback IMediaSourceCallback
     */
    public void registerMediaSourceCallback(IMediaSourceCallback callback) {
        try {
            mIPlayBinder.registerMediaSourceCallback(callback);
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * UnRegister.
     */
    public void unRegisterMediaSourceCallback(IMediaSourceCallback callback) {
        try {
            mIPlayBinder.unRegisterMediaSourceCallback(callback);
        } catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    public interface IAudioStatusListener {
        void onProgressChanged(long totalDuration, long currentDuration,
                               int bufferProgress, AudioInfoBean musicInfo);

        void onPlayStateChanged(int state);

        void onPlayMusicInfoChanged(AudioInfoBean musicInfo);

        void onPlayerChanged(int player);
    }
}
