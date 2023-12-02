package com.ts.service.media.presenter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.os.Message;

import com.ts.service.media.constants.BtConstants;
import com.ts.service.media.receiver.HardKeyMonitor;
import com.ts.service.media.utils.LogUtil;

import java.util.List;

public class A2dpAudioFocusHandler extends Handler {

    private static final String TAG = "A2dpAudioFocusHandler";

    // Configuration Variables
    private static final int DEFAULT_DUCK_PERCENT = 25;
    private static final int SETTLE_TIMEOUT = 1000;
    private static final int DELAY_PLAY_MUSIC = 500;

    /**
     * Incoming events.
     */
    // Audio stream from remote device started
    public static final int SRC_STR_START = 0;
    // Audio stream from remote device stopped
    public static final int SRC_STR_STOP = 1;
    // Play command was generated from local device
    public static final int SNK_PLAY = 2;
    // Pause command was generated from local device
    public static final int SNK_PAUSE = 3;
    // Play command was generated from remote device
    public static final int SRC_PLAY = 4;
    // Pause command was generated from remote device
    public static final int SRC_PAUSE = 5;
    // Remote device was disconnected
    public static final int DISCONNECT = 6;
    // Audio focus callback with associated change
    public static final int AUDIO_FOCUS_CHANGE = 7;
    // Request focus when the media service is active
    public static final int REQUEST_FOCUS = 8;
    // If a call just ended allow stack time to settle
    public static final int DELAYED_RESUME = 9;
    public static final int START_PLAY_MUSIC = 10;

    // Used to indicate focus lost
    private static final int STATE_FOCUS_LOST = 0;
    // Used to inform bluedroid that focus is granted
    private static final int STATE_FOCUS_GRANTED = 1;

    private Context mContext;
    private AudioManager mAudioManager;
    // Keep track if the remote device is providing audio
    private boolean mStreamAvailable = false;
    private boolean mSentPause = false;
    // Keep track of the relevant audio focus (None, Transient, Gain)
    private int mAudioFocus = AudioManager.AUDIOFOCUS_NONE;
    private BluetoothDevice mDevice = null;
    private BluetoothA2dpSink mA2dpSinkService;
    private IAudioFocusListener mIAudioFocusListener;
    private BtManager mBtManager;
    private boolean mIsPlaying = false;
    private boolean mOldIsPlaying = false;
    private boolean mIsTransientLossFocus = false;

    // Focus changes when we are currently holding focus.
    private final OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            LogUtil.debug(TAG, "onAudioFocusChangeListener focuschange " + focusChange);
            // notify focus state;
            A2dpAudioFocusHandler.this.obtainMessage(AUDIO_FOCUS_CHANGE, focusChange)
                    .sendToTarget();
        }
    };

    /**
     * A2dpAudioFocusHandler Constructor.
     */
    public A2dpAudioFocusHandler(Context context, IAudioFocusListener listener) {
        mContext = context;
        mIAudioFocusListener = listener;
        mBtManager = BtManager.getInstance(mContext);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile.ServiceListener a2dpSinkServiceListener = new BluetoothProfile
                .ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BtConstants.A2DP_SINK) {
                    mA2dpSinkService = (BluetoothA2dpSink) proxy;
                    getConnectDevice(mA2dpSinkService);
                    mIsTransientLossFocus = false;
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                LogUtil.debug(TAG, "A2dpSinkService disconnected");
                mAudioFocus = AudioManager.AUDIOFOCUS_NONE;
                stopA2dpRender();
                mA2dpSinkService = null;
                mDevice = null;
            }
        };
        bluetoothAdapter.getProfileProxy(mContext, a2dpSinkServiceListener, BtConstants.A2DP_SINK);
    }

    @SuppressLint("MissingPermission")
    private void getConnectDevice(BluetoothA2dpSink bluetoothA2dpSink) {
        List<BluetoothDevice> devices = bluetoothA2dpSink.getConnectedDevices();
        LogUtil.debug(TAG, "A2dpSinkService devices : " + devices);
        if (devices == null) {
            return;
        }
        LogUtil.debug(TAG, "A2dpSinkService devices size： " + devices.size());
        for (BluetoothDevice device : devices) {

            LogUtil.debug(TAG, "A2dpSinkService device name : " + device.getName()
                    + " ,address : " + device.getAddress()
                    + " ,bondState : " + device.getBondState());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                mDevice = device;
                LogUtil.debug(TAG, "A2dpSinkService connected device : " + mDevice);
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        LogUtil.debug(TAG, " process message: "
                + message.what + " audioFocus =  " + mAudioFocus);
        if (mA2dpSinkService == null) {
            LogUtil.debug(TAG, " Failed because of A2dpSinkService connection.");
            return;
        }
        switch (message.what) {
            case SRC_STR_STOP:
                LogUtil.debug(TAG, "SRC_STR_STOP");
                stopA2dpRender();
                break;
            case SNK_PLAY:
                mIsPlaying = true;
                LogUtil.debug(TAG, "mAudioFocus : " + mAudioFocus);
                if (mAudioFocus == AudioManager.AUDIOFOCUS_GAIN) {
                    startA2dpRender();
                } else {
                    requestAudioFocus();
                }
                break;
            case SRC_PLAY:
                mIsPlaying = true;
                break;
            case SNK_PAUSE:
            case SRC_PAUSE:
                LogUtil.debug(TAG, "music paused");
                mIsPlaying = false;
                removeMessages(START_PLAY_MUSIC);
                break;
            case START_PLAY_MUSIC:
                LogUtil.debug(TAG, "delay startBtMusic, mOldIsPlaying : "
                        + mOldIsPlaying);
                if (mOldIsPlaying) {
                    mIAudioFocusListener.startBtMusic();
                    LogUtil.debug(TAG, "startBtMusic :: invoke");
                }
                break;
            case AUDIO_FOCUS_CHANGE:
                LogUtil.debug(TAG, " audioFocus change =  " + message.obj);
                switch ((int) message.obj) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // Begin playing audio, if we paused the remote, send a play now.
                        LogUtil.debug(TAG, "audio focus gain.");
                        mIsTransientLossFocus = false;
                        mAudioFocus = AudioManager.AUDIOFOCUS_GAIN;
                        sendEmptyMessageDelayed(START_PLAY_MUSIC, DELAY_PLAY_MUSIC);
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        mIsTransientLossFocus = false;
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // Temporary loss of focus, if we are actively streaming pause the remote
                        // and make sure we resume playback when we regain focus.
                        LogUtil.debug(TAG, "audio focus loss transient, mIsPlaying : "
                                + mIsPlaying);
                        mIsTransientLossFocus = true;
                        mAudioFocus = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                        stopA2dpRender();
                        HardKeyMonitor.getInstance(mContext).focusOut();
                        mOldIsPlaying = mIsPlaying;
                        if (mIsPlaying) {
                            LogUtil.debug(TAG, "stopBtMusic.");
                            mIAudioFocusListener.stopBtMusic();
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Permanent loss of focus probably due to another audio app, abandon focus
                        // and stop playback.
                        LogUtil.debug(TAG, "audio focus loss");
                        mIsTransientLossFocus = false;
                        abandonAudioFocus();
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Utility functions.
     */
    private synchronized int requestAudioFocus() {
        // Bluetooth A2DP may carry Music, Audio Books, Navigation, or other sounds so mark content
        // type unknown.
        AudioAttributes streamAttributes =
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build();
        // Bluetooth ducking is handled at the native layer so tell the Audio Manger to notify the
        // focus change listener via .setWillPauseWhenDucked().
        AudioFocusRequest focusRequest = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                            streamAttributes)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(mAudioFocusListener, this)
                    .build();
        }
        int focusRequestStatus = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            focusRequestStatus = mAudioManager.requestAudioFocus(focusRequest);
        }
        LogUtil.debug(TAG, "focusRequestStatus = " + focusRequestStatus
                + " device : " + mDevice);
        // If the request is granted begin streaming immediately and schedule an upgrade.
        if (focusRequestStatus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            LogUtil.debug(TAG, "audio focus requested");
            mAudioFocus = AudioManager.AUDIOFOCUS_GAIN;
            startA2dpRender();
        }
        return focusRequestStatus;
    }


    private void startA2dpRender() {
        LogUtil.debug(TAG, "startA2dpRender_mIsStartA2dpRender : "
                + mDevice + "mAudioFocus : " + mAudioFocus);
        getConnectDevice(mA2dpSinkService);
        // TODO baipeng
       // mA2dpSinkService.startA2dpRender(mDevice);
    }


    private void stopA2dpRender() {
        LogUtil.debug(TAG, "stopA2dpRender_mIsStartA2dpRender : "
                + "mDevice : " + mDevice);
        getConnectDevice(mA2dpSinkService);
        // TODO baipeng
      //  mA2dpSinkService.stopA2dpRender(mDevice);
    }

    private BluetoothDevice getConnectedDevice() {
        if (mDevice == null) {
            mDevice = mBtManager.getConnectedDevice();
        }
        LogUtil.debug(TAG, "getConnectedDevice : " + mDevice);
        return mDevice;
    }

    private synchronized void abandonAudioFocus() {
        LogUtil.debug(TAG, "abandonAudioFocus");
        stopA2dpRender();
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mAudioFocus = AudioManager.AUDIOFOCUS_NONE;
        mIAudioFocusListener.stopBtMusic();
    }

    public boolean isTransientLossFocus() {
        return mIsTransientLossFocus;
    }

    public interface IAudioFocusListener {
        void stopBtMusic();

        void startBtMusic();
    }
}