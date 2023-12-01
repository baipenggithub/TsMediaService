package com.ts.service.media.presenter;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioSetting;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.UsbDevicesInfoBean;
import com.ts.sdk.media.callback.IMusicPlayerEventListener;
import com.ts.sdk.media.callback.IMusicPlayerInfoListener;
import com.ts.service.R;
import com.ts.service.media.UsbServiceApplication;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.model.entity.RecordAudioInfo;
import com.ts.service.media.receiver.CarPowerMonitor;
import com.ts.service.media.receiver.HardKeyMonitor;
import com.ts.service.media.receiver.UsbDeviceMonitor;
import com.ts.service.media.utils.LogUtil;
import com.ts.service.media.utils.MediaScannerFile;
import com.ts.service.media.utils.MusicUtils;
import com.ts.service.media.utils.SharedPreferencesHelps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MusicMediaPlayer implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, HardKeyMonitor.IKeyEventListener,
        MediaScannerFile.IAudioQueryListener {
    private static final String TAG = MusicMediaPlayer.class.getSimpleName();
    private String mDataSource = "setDataSource";
    // Currently working player object
    private static MediaPlayer sMediaPlayer;
    // Component callback registration pool
    private static final RemoteCallbackList<IMusicPlayerEventListener> sOnPlayerEventListeners =
            new RemoteCallbackList<>();
    // Player listening
    private static IMusicPlayerInfoListener sMusicPlayerInfoListener;
    // Audio focus　Manager
    private static MusicAudioFocusManager sAudioFocusManager;
    // Audio queue pool to be played
    private static List<AudioInfoBean> sAudios = new ArrayList<>();
    // The location of the object currently being processed by the player
    private int mCurrentPlayIndex = 0;
    // Cycle mode
    private static boolean sLoop;
    // Internal player playing mode set by the user,Default MusicPlayMode.MUSIC_MODE_LOOP
    private int mPlayMode = MusicConstants.MUSIC_MODE_LOOP;
    // Player working status
    private int mMusicPlayerState = MusicConstants.MUSIC_PLAYER_STOP;
    private PlayTimerTask mPlayTimerTask;
    private Timer mTimer;
    // The data channel of the object being processed inside the player
    private int mPlayChannel = MusicConstants.CHANNEL_LOCATION;
    // Progress of the currently playing object buffer
    private int mBufferProgress;
    // Whether to pause passively to deal with the loss of audio focus mark
    private boolean mIsPassive;
    private Context mContext;
    private AudioInfoBean mCurrentPlayAudioInfo = new AudioInfoBean();
    private String mUsbOneUuid = "";
    private String mUsbTwoUuid = "";
    private String mUsbOnePath = "";
    private String mUsbTwoPath = "";
    private RecordAudioInfo mUsbOneAudioInfo;
    private RecordAudioInfo mUsbTwoAudioInfo;
    private int mNeedPlayUsbId;
    private List<AudioInfoBean> mUsbOneAudioInfoList = new ArrayList<>();
    private List<AudioInfoBean> mUsbTwoAudioInfoList = new ArrayList<>();
    private AudioSourceManager mAudioSourceManager;
    private Car mCar;
    private CarAudioManager mCarAudioManager;
    private final List<UsbDevicesInfoBean> mUsbDevicesInfoBeans = new ArrayList<>();
    private boolean mIsPressing = false;
    private boolean mIsRemoveCurrentDevice = false;
    private List<AudioInfoBean> mAudioBeans = new ArrayList<>();
    private CarPowerMonitor mCarPowerMonitor;
    private int mCurrentPowerState = -1;
    private boolean mIsStopByPower;
    private boolean mPlayTaskRunning = false;
    private final Map<String, List<AudioInfoBean>> mLastAudios = new HashMap<>();
    private boolean mMemoryContorl = true;
    private int mCurrentUpdateUsbPort;
    private final Handler mHandleTask = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            LogUtil.debug(TAG, "handleMessage type : " + message.what);
            if (mPlayTaskRunning) {
                if (message.what == MusicConstants.MUSIC_PLAY_RUN_TASK) {
                    playRunTask();
                    if (mHandleTask.hasMessages(MusicConstants.MUSIC_PLAY_RUN_TASK)) {
                        return true;
                    }
                    mHandleTask.sendEmptyMessageDelayed(MusicConstants.MUSIC_PLAY_RUN_TASK,
                            MusicConstants.FORMATTER_SECOND);
                    return true;
                }
            }
            return false;
        }
    });

    /**
     * Initialization.
     *
     * @param context Context.
     */
    public MusicMediaPlayer(Context context) {
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
        sAudioFocusManager = new MusicAudioFocusManager(mContext);
        mAudioSourceManager = AudioSourceManager.getInstance(mContext);
        HardKeyMonitor.getInstance(context).addKeyEventListener(this);
        mCarPowerMonitor = CarPowerMonitor.getInstance(mContext);
        mCarPowerMonitor.addPowerEventListener(state -> {
            powerStateChanged(state);
        });
        MediaScannerFile.getInstance(mContext).setAudioQueryListener(this);
        initPlayerConfig();
        usbDeviceChangeListener();
        initRecentAudio();
    }

    private void powerStateChanged(int state) {
        if (state != mCurrentPowerState) {
            LogUtil.debug(TAG, "mCurrentPowerState : " + mCurrentPowerState
                    + ",    mIsStopByPower : " + mIsStopByPower + ",    state : " + state);
            if (state == MusicConstants.POWER_STAND_BY || state == MusicConstants.POWER_VOLTAGE) {
                if (isPlaying()) {
                    mIsStopByPower = true;
                }
            } else if ((state == MusicConstants.POWER_RUN || state == MusicConstants.POWER_TEMP_RUN)
                    && mCurrentPowerState == MusicConstants.POWER_STAND_BY && mIsStopByPower) {
                play();
                mIsStopByPower = false;
            }
            mCurrentPowerState = state;
        }
    }

    /**
     * Initialize player configuration.
     */
    private void initPlayerConfig() {
        String value = SharedPreferencesHelps.getPlayerMode(
                mContext,
                MusicConstants.KEY_PARAMS_MODE,
                MusicConstants.SP_VALUE_MUSIC_MODE_LOOP);
        int mode = MusicConstants.MUSIC_MODE_LOOP;
        switch (value) {
            case MusicConstants.SP_VALUE_MUSIC_MODE_SINGLE:
                sLoop = true;
                mode = MusicConstants.MUSIC_MODE_SINGLE;
                break;
            case MusicConstants.SP_VALUE_MUSIC_MODE_LOOP:
                mode = MusicConstants.MUSIC_MODE_LOOP;
                sLoop = false;
                break;
            case MusicConstants.SP_VALUE_MUSIC_MODE_RANDOM:
                mode = MusicConstants.MUSIC_MODE_RANDOM;
                sLoop = false;
                break;
            default:
                break;
        }
        mPlayMode = mode;
    }

    private void initRecentAudio() {
        mUsbDevicesInfoBeans.clear();
        mUsbDevicesInfoBeans.addAll(UsbDeviceMonitor.getInstance(mContext).getUsbDevices());
        playByLastMode();
    }

    /**
     * According to the port to save the most recently played songs,
     * ensure that in the case of dual U-disk,
     * pull out one U-disk and switch to another U-disk recently played song list.
     */
    private void savePlayList() {
        LogUtil.debug(TAG, "savePlayList:: invoke");
        if (null != sAudios && sAudios.size() > 0) {
            LogUtil.debug(TAG, "savePlayList:: has audio :: size :: " + sAudios.size());
            List<AudioInfoBean> playList = new ArrayList<>(sAudios);
            AudioInfoBean audioInfoBean = playList.get(0);
            if (null != audioInfoBean && !TextUtils.isEmpty(audioInfoBean.getAudioPath())) {
                LogUtil.debug(TAG, "savePlayList:: has getAudioPath :: "
                        + audioInfoBean.getAudioPath());
                if (!TextUtils.isEmpty(mUsbOnePath)) {
                    LogUtil.debug(TAG, "savePlayList::  mUsbOnePath :: " + mUsbOnePath);
                    if (audioInfoBean.getAudioPath().contains(mUsbOnePath)) {
                        LogUtil.debug(TAG, "savePlayList:: mUsbOnePath::   save list ");
                        mLastAudios.put(MusicConstants.USB_1_PORT, playList);
                    }
                }
                if (!TextUtils.isEmpty(mUsbTwoPath)) {
                    LogUtil.debug(TAG, "savePlayList::  mUsbTwoPath :: " + mUsbTwoPath);
                    if (audioInfoBean.getAudioPath().contains(mUsbTwoPath)) {
                        LogUtil.debug(TAG, "savePlayList:: mUsbTwoPath:: save list ");
                        mLastAudios.put(MusicConstants.USB_2_PORT, playList);
                    }
                }
            }
        }
    }

    private void usbDeviceChangeListener() {
        UsbDeviceMonitor.getInstance(mContext).setUsbDeviceListener(
                new UsbDeviceMonitor.UsbDeviceListener() {
                    @Override
                    public void onDeviceChange(List<UsbDevicesInfoBean> usbDevices) {
                        if (null != usbDevices) {
                            LogUtil.debug(TAG, "onDeviceChange:  " + usbDevices.size());
                            String removeDevicePort = "";
                            if (mUsbDevicesInfoBeans.size() > usbDevices.size()) {
                                LogUtil.debug(TAG, "onDeviceChange size reduce");
                                if (usbDevices.size() == 0) {
                                    LogUtil.debug(TAG, "usbDevices  size = 0 ");
                                    removeAudiosInfo();
                                } else {
                                    usbDevices.size();
                                    mIsRemoveCurrentDevice = true;
                                    if (null != sAudios && sAudios.size() > 0) {
                                        LogUtil.debug(TAG, "onDeviceChange "
                                                + "sAudios size > 0 ");
                                        AudioInfoBean currentAudio = sAudios
                                                .get(mCurrentPlayIndex);
                                        if (null != currentAudio && !TextUtils
                                                .isEmpty(currentAudio.getAudioPath())) {
                                            String audioPath = currentAudio.getAudioPath();
                                            if (audioPath.contains(mUsbOnePath)) {
                                                removeDevicePort = MusicConstants.USB_1_PORT;
                                            } else if (audioPath.contains(mUsbTwoPath)) {
                                                removeDevicePort = MusicConstants.USB_2_PORT;
                                            }
                                            LogUtil.debug(TAG, "onDeviceChange "
                                                    + "removeDevicePort :: " + removeDevicePort);
                                            for (UsbDevicesInfoBean device : usbDevices) {
                                                if (removeDevicePort.equals(device.getPort())) {
                                                    LogUtil.debug(TAG, "onDeviceChange "
                                                            + "mIsRemoveCurrentDevice false ");
                                                    mIsRemoveCurrentDevice = false;
                                                }
                                            }
                                        }
                                        if (mIsRemoveCurrentDevice) {
                                            LogUtil.debug(TAG, "onDeviceChange "
                                                    + "mIsRemoveCurrentDevice true ");
                                            removeAudiosInfo();
                                            refreshPlayList(removeDevicePort);
                                        }
                                    }
                                }
                            }
                            mUsbDevicesInfoBeans.clear();
                            mUsbDevicesInfoBeans.addAll(usbDevices);
                        }
                    }

                    @Override
                    public void onScanChange(int state, String deviceId, int portId) {
                        LogUtil.debug(TAG, "onScanChange :: state : " + state
                                + " : deviceId : " + deviceId + " : portId : " + portId);
                        if (mUsbDevicesInfoBeans.size() != 0) {
                            if (state == MusicConstants.MEDIA_SCANNER_START) {
                                playByLastMode();
                            } else if (state == MusicConstants.MEDIA_SCANNER_FINISHED) {
                                mCurrentUpdateUsbPort = portId;
                                MediaScannerFile.getInstance(mContext)
                                        .queryAllMusic(MusicConstants.QUERY_MUSIC_LOCAL);
                            }
                        }
                    }
                });
    }

    /**
     * Clear the current playing data after pulling out the USB flash disk.
     */
    private void removeAudiosInfo() {
        LogUtil.debug(TAG, "removeAudiosInfo :: invoke ");
        destroy();
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    sOnPlayerEventListeners.getBroadcastItem(index)
                            .onMusicPlayerState(mMusicPlayerState,
                                    MusicConstants.MEDIA_PLAY_ERROR);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
    }

    /**
     * Called when the data needs to be switched after pulling out the USB flash disk.
     * 1.Get the most recently played songs. If not, get all the lists and get the first song.
     * 2.Get the latest play list, if not, all songs.
     */
    private void refreshPlayList(String removePort) {
        LogUtil.debug(TAG, "refreshPlayList :: invoke ");
        if (!TextUtils.isEmpty(mUsbOnePath) && !TextUtils.isEmpty(mUsbTwoPath)
                && !TextUtils.isEmpty(removePort)) {
            LogUtil.debug(TAG, "refreshPlayList :: do ");
            String path = "";
            String uuid = "";
            List<AudioInfoBean> playList = new ArrayList<>();
            RecordAudioInfo lastRecord = new RecordAudioInfo();
            if (removePort.equals(MusicConstants.USB_1_PORT)) {
                LogUtil.debug(TAG, "refreshPlayList :: mUsbTwoUuid ");
                uuid = mUsbTwoUuid;
                path = mUsbTwoPath;
            } else if (removePort.equals(MusicConstants.USB_2_PORT)) {
                LogUtil.debug(TAG, "refreshPlayList :: mUsbOneUuid ");
                uuid = mUsbOneUuid;
                path = mUsbOnePath;
            }
            try {
                Gson gson = new Gson();
                RecordAudioInfo usbFirst = null;
                RecordAudioInfo usbSecond = null;
                String data = SharedPreferencesHelps
                        .getObjectData(mContext, MusicConstants.USB_FIRST_UUID);
                if (!TextUtils.isEmpty(data)) {
                    usbFirst = gson.fromJson(data, RecordAudioInfo.class);
                }
                data = SharedPreferencesHelps
                        .getObjectData(mContext, MusicConstants.USB_SECOND_UUID);
                if (!TextUtils.isEmpty(data)) {
                    usbSecond = gson.fromJson(data, RecordAudioInfo.class);
                }
                boolean hasData = false;
                if (null != usbFirst) {
                    LogUtil.debug(TAG, "refreshPlayList :: usbFirst :: getUuid :: "
                            + usbFirst.getUuid() + " : uuid :: " + uuid);
                    if (usbFirst.getUuid().equals(uuid)) {
                        lastRecord = usbFirst;
                        hasData = true;
                    }
                }
                if (null != usbSecond) {
                    LogUtil.debug(TAG, "refreshPlayList :: usbSecond :: getUuid :: "
                            + usbSecond.getUuid() + " : uuid :: " + uuid);
                    if (usbSecond.getUuid().equals(uuid)) {
                        lastRecord = usbSecond;
                        hasData = true;
                    }
                }
                if (!hasData) {
                    LogUtil.debug(TAG, "refreshPlayList :: hasData :: false");
                    playList = MusicUtils.getInstance().filterAudio(mAudioBeans, path);
                    if (null != playList && playList.size() > 0) {
                        AudioInfoBean audioInfoBean = playList.get(0);
                        lastRecord.setAudioId(audioInfoBean.getAudioId());
                        lastRecord.setPath(audioInfoBean.getAudioPath());
                    }
                }
                if (mLastAudios.size() > 0 && hasData) {
                    LogUtil.debug(TAG, "refreshPlayList :: mLastAudios :: not null ");
                    if (removePort.equals(MusicConstants.USB_1_PORT)) {
                        LogUtil.debug(TAG, "refreshPlayList :: get :: USB_2_PORT ");
                        List<AudioInfoBean> list = mLastAudios.get(MusicConstants.USB_2_PORT);
                        mLastAudios.remove(MusicConstants.USB_1_PORT);
                        if (list == null) {
                            LogUtil.debug(TAG, "refreshPlayList::get ::USB_2_PORT :: null");
                        } else {
                            playList.addAll(list);
                            LogUtil.debug(TAG, "refreshPlayList ::get "
                                    + "::USB_2_PORT ::size::" + playList.size());
                        }
                    } else if (removePort.equals(MusicConstants.USB_2_PORT)) {
                        LogUtil.debug(TAG, "refreshPlayList :: get :: USB_1_PORT ");
                        List<AudioInfoBean> list = mLastAudios.get(MusicConstants.USB_1_PORT);
                        mLastAudios.remove(MusicConstants.USB_2_PORT);
                        if (list == null) {
                            LogUtil.debug(TAG, "refreshPlayList::get::USB_2_PORT ::null ");
                        } else {
                            playList.addAll(list);
                            LogUtil.debug(TAG, "refreshPlayList :: get :: USB_2_PORT  "
                                    + ":: size :: " + playList.size());
                        }
                    }
                }
                if (null == playList || playList.size() == 0) {
                    LogUtil.debug(TAG, "refreshPlayList :: playList :: last mode  ");
                    playList = MusicUtils.getInstance().filterAudio(mAudioBeans, path);
                }
                if (null != playList && playList.size() > 0) {
                    LogUtil.debug(TAG, "refreshPlayList :: will update playList size :: "
                            + playList.size());
                    if (!TextUtils.isEmpty(lastRecord.getPath())) {
                        int position = MusicUtils.getInstance()
                                .getCurrentPlayIndex(playList, lastRecord.getAudioId(),
                                        lastRecord.getPath());
                        LogUtil.debug(TAG, "refreshPlayList :: position ::" + position);
                        updateMusicPlayerData(playList, position);
                        postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    }
                } else {
                    LogUtil.debug(TAG, "refreshPlayList :: other USB no data");
                    if (sAudios != null) {
                        sAudios.clear();
                        mCurrentPlayIndex = -1;
                        AudioInfoBean audioInfoBean = new AudioInfoBean();
                        audioInfoBean.setAudioName(UsbServiceApplication.getContext()
                                .getString(R.string.no_music));
                        audioInfoBean.setAudioId(0);
                        audioInfoBean.setAudioArtistName("");
                        synchronized (sOnPlayerEventListeners) {
                            checkListener();
                            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                            for (int index = 0; index < listenerCount; index++) {
                                try {
                                    sOnPlayerEventListeners.getBroadcastItem(index)
                                            .onPlayMusicOnInfo(audioInfoBean, mCurrentPlayIndex);
                                } catch (RemoteException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            sOnPlayerEventListeners.finishBroadcast();
                        }
                    }
                }
            } catch (JsonSyntaxException error) {
                error.printStackTrace();
            }
        }
    }

    private void getUsbUuid(List<UsbDevicesInfoBean> usbDevices) {
        if (null != usbDevices && usbDevices.size() > 0) {
            mUsbOneUuid = "";
            mUsbOnePath = "";
            mUsbTwoUuid = "";
            mUsbTwoPath = "";
            for (UsbDevicesInfoBean device : usbDevices) {
                if (device.getPort().equals(MusicConstants.USB_1_PORT)) {
                    mUsbOneUuid = device.getUuid();
                    mUsbOnePath = device.getPath();
                } else if (device.getPort().equals(MusicConstants.USB_2_PORT)) {
                    mUsbTwoUuid = device.getUuid();
                    mUsbTwoPath = device.getPath();
                }
            }
        }
    }

    /**
     * Memory play function.
     */
    private void playByLastMode() {
        LogUtil.debug(TAG, "playByLastMode :: invoke");
        if (mUsbDevicesInfoBeans.size() > 0) {
            LogUtil.debug(TAG, "playByLastMode :: mUsbDevicesInfoBeans size : "
                    + mUsbDevicesInfoBeans.size());
            getUsbUuid(mUsbDevicesInfoBeans);
            getLastModeAudios();
            LogUtil.debug(TAG, "playByLastMode :: isMediaPlayerhasData " + isMediaPlayerhasData());
            if (!isMediaPlayerhasData()) {
                playRecentAudio();
            }
        } else {
            LogUtil.debug(TAG, "playByLastMode :: mUsbDevicesInfoBeans null ");
        }
    }

    private synchronized void updatePlayListAfterScan(int port) {
        LogUtil.debug(TAG, "updatePlayListAfterScan :: invoke");
        boolean needInit = false;
        filterAudios();
        if (sAudios != null && sAudios.size() > 0) {
            LogUtil.debug(TAG, "updatePlayListAfterScan :: sAudios has data");
            AudioInfoBean currentAudio = null;
            UsbDevicesInfoBean usb = null;
            if (mCurrentPlayIndex > -1 && mCurrentPlayIndex < sAudios.size()) {
                currentAudio = sAudios.get(mCurrentPlayIndex);
                if (null != currentAudio) {
                    usb = getUsbInfoByAudioPath(currentAudio.getAudioPath(), mUsbDevicesInfoBeans);
                    if (null != usb) {
                        if (port == MusicConstants.PORT_1
                                && usb.getPort().equals(MusicConstants.USB_1_PORT)) {
                            if (mUsbOneAudioInfoList != null && mUsbOneAudioInfoList.size() > 0) {
                                int position = MusicUtils.getInstance()
                                        .getCurrentPlayIndex(mUsbOneAudioInfoList,
                                                currentAudio.getAudioId(),
                                                currentAudio.getAudioPath());
                                updateMusicPlayerData(mUsbOneAudioInfoList, position);
                                LogUtil.debug(TAG, "updatePlayListAfterScan ::"
                                        + " updateMusicPlayerData  USB_1_PORT");
                            }
                        } else if (port == MusicConstants.PORT_2
                                && usb.getPort().equals(MusicConstants.USB_2_PORT)) {
                            if (mUsbTwoAudioInfoList != null && mUsbTwoAudioInfoList.size() > 0) {
                                int position = MusicUtils.getInstance()
                                        .getCurrentPlayIndex(mUsbTwoAudioInfoList,
                                                currentAudio.getAudioId(),
                                                currentAudio.getAudioPath());
                                updateMusicPlayerData(mUsbTwoAudioInfoList, position);
                                LogUtil.debug(TAG, "updatePlayListAfterScan ::"
                                        + " updateMusicPlayerData  USB_2_PORT");
                            }
                        }
                    }
                } else {
                    needInit = true;
                }
            } else {
                needInit = true;
            }
        } else {
            needInit = true;
        }
        if (needInit) {
            LogUtil.debug(TAG, "updatePlayListAfterScan :: needInit : port" + port);
            if (port == MusicConstants.PORT_1) {
                mMemoryContorl = false;
                startPlayMusic(mUsbOneAudioInfoList, 0);
            } else if (port == MusicConstants.PORT_2) {
                mMemoryContorl = false;
                startPlayMusic(mUsbTwoAudioInfoList, 0);
            }
        }
    }

    private UsbDevicesInfoBean getUsbInfoByAudioPath(String path,
                                                     List<UsbDevicesInfoBean>
                                                             usbDevicesInfoBeanList) {
        LogUtil.debug(TAG, "getUsbInfoByAudioPath :: invoke");
        UsbDevicesInfoBean usbDevicesInfoBean = null;
        if (null != path
                && null != usbDevicesInfoBeanList && usbDevicesInfoBeanList.size() > 0) {
            String[] tmp = path.split("/");
            if (tmp.length >= MusicConstants.PATH_MIN_LENTH) {
                String audioUuid = tmp[2];
                LogUtil.debug(TAG, "getUsbInfoByAudioPath :: path" + path
                        + " : audioUuid :" + audioUuid);
                for (UsbDevicesInfoBean usb : usbDevicesInfoBeanList) {
                    if (usb.getUuid().equals(audioUuid)) {
                        usbDevicesInfoBean = usb;
                        LogUtil.debug(TAG, "getUsbInfoByAudioPath :: get usb");
                    }
                }
            }
        }
        return usbDevicesInfoBean;
    }

    private void filterAudios() {
        LogUtil.debug(TAG, "filterAudios : invoke");
        if (!TextUtils.isEmpty(mUsbOnePath)) {
            mUsbOneAudioInfoList = MusicUtils.getInstance().filterAudio(mAudioBeans, mUsbOnePath);
        }
        if (!TextUtils.isEmpty(mUsbTwoPath)) {
            mUsbTwoAudioInfoList = MusicUtils.getInstance().filterAudio(mAudioBeans, mUsbTwoPath);
        }
    }

    private void playRecentAudio() {
        LogUtil.debug(TAG, "playRecentAudio : invoke");
        // usb1
        if (!TextUtils.isEmpty(mUsbOneUuid) && TextUtils.isEmpty(mUsbTwoUuid)) {
            if (null != mUsbOneAudioInfoList) {
                getRecentAudioLocation(mUsbOneAudioInfoList, mUsbOneAudioInfo);
                LogUtil.debug(TAG, "playRecentAudio mUsbOneAudioInfoList size : "
                        + mUsbOneAudioInfoList.size());
            }
            // usb 2
        } else if (TextUtils.isEmpty(mUsbOneUuid) && !TextUtils.isEmpty(mUsbTwoUuid)) {
            if (null != mUsbTwoAudioInfoList) {
                getRecentAudioLocation(mUsbTwoAudioInfoList, mUsbTwoAudioInfo);
                LogUtil.debug(TAG, "playRecentAudio mUsbTwoAudioInfoList size : "
                        + mUsbTwoAudioInfoList.size());
            }
        } else if (!TextUtils.isEmpty(mUsbOneUuid) && !TextUtils.isEmpty(mUsbTwoUuid)) {
            if (mUsbOneAudioInfo == null && mUsbTwoAudioInfo == null) {
                getRecentAudioLocation(mUsbOneAudioInfoList, mUsbOneAudioInfo);
                LogUtil.debug(TAG, "playRecentAudio all null");
            }
            if (null != mUsbOneAudioInfo && null != mUsbTwoAudioInfo) {
                if (mNeedPlayUsbId == MusicConstants.PORT_1) {
                    getRecentAudioLocation(mUsbOneAudioInfoList, mUsbOneAudioInfo);
                } else if (mNeedPlayUsbId == MusicConstants.PORT_2) {
                    getRecentAudioLocation(mUsbTwoAudioInfoList, mUsbTwoAudioInfo);
                }
            } else {
                if (null != mUsbOneAudioInfo) {
                    getRecentAudioLocation(mUsbOneAudioInfoList, mUsbOneAudioInfo);
                    LogUtil.debug(TAG, "playRecentAudio mUsbOneAudioInfo : "
                            + mUsbOneAudioInfo.toString() + " ,size："
                            + mUsbOneAudioInfoList.size());
                } else if (null != mUsbTwoAudioInfo) {
                    getRecentAudioLocation(mUsbTwoAudioInfoList, mUsbTwoAudioInfo);
                    LogUtil.debug(TAG, "playRecentAudio mUsbTwoAudioInfo :: "
                            + mUsbTwoAudioInfo.toString() + " ,size："
                            + mUsbTwoAudioInfoList.size());
                }
            }
        }
    }

    private void getLastModeAudios() {
        LogUtil.debug(TAG, "getLastModeAudios invoke ");
        mUsbOneAudioInfo = null;
        mUsbTwoAudioInfo = null;
        mUsbOneAudioInfoList = null;
        mUsbTwoAudioInfoList = null;
        mNeedPlayUsbId = 0;
        try {
            Gson gson = new Gson();
            RecordAudioInfo usbFirst = null;
            RecordAudioInfo usbSecond = null;
            List<AudioInfoBean> usbFirstPlayList = null;
            List<AudioInfoBean> usbSecondPlayList = null;
            String data = SharedPreferencesHelps
                    .getObjectData(mContext, MusicConstants.USB_FIRST_UUID);
            String list = SharedPreferencesHelps
                    .getObjectData(mContext, MusicConstants.SP_MUSIC_USB_KEY
                            + MusicConstants.USB_FIRST_UUID);
            if (!TextUtils.isEmpty(data)) {
                usbFirst = gson.fromJson(data, RecordAudioInfo.class);
            }
            if (!TextUtils.isEmpty(list)) {
                usbFirstPlayList = gson.fromJson(list,
                        new TypeToken<List<AudioInfoBean>>() {
                        }.getType());
            }
            data = SharedPreferencesHelps
                    .getObjectData(mContext, MusicConstants.USB_SECOND_UUID);
            list = SharedPreferencesHelps
                    .getObjectData(mContext, MusicConstants.SP_MUSIC_USB_KEY
                            + MusicConstants.USB_SECOND_UUID);
            if (!TextUtils.isEmpty(data)) {
                usbSecond = gson.fromJson(data, RecordAudioInfo.class);
            }
            if (!TextUtils.isEmpty(list)) {
                usbSecondPlayList = gson.fromJson(list,
                        new TypeToken<List<AudioInfoBean>>() {
                        }.getType());
            }
            if (null != usbFirst) {
                LogUtil.debug(TAG, "getLastModeAudios usbFirst getUuid : " + usbFirst.getUuid()
                        + " ,mUsbOneUuid : " + mUsbOneUuid + " ,mUsbTwoUuid : " + mUsbTwoUuid);
                if (usbFirst.getUuid().equals(mUsbOneUuid)) {
                    mUsbOneAudioInfo = usbFirst;
                    mUsbOneAudioInfoList = usbFirstPlayList;
                    mNeedPlayUsbId = MusicConstants.PORT_1;
                } else if (usbFirst.getUuid().equals(mUsbTwoUuid)) {
                    mUsbTwoAudioInfo = usbFirst;
                    mUsbTwoAudioInfoList = usbFirstPlayList;
                    mNeedPlayUsbId = MusicConstants.PORT_2;
                }
            }
            if (null != usbSecond) {
                LogUtil.debug(TAG, "getLastModeAudios usbSecond getUuid : "
                        + usbSecond.getUuid() + " ,mUsbOneUuid : "
                        + mUsbOneUuid + " ,mUsbTwoUuid : " + mUsbTwoUuid);
                if (usbSecond.getUuid().equals(mUsbOneUuid)) {
                    mUsbOneAudioInfo = usbSecond;
                    mUsbOneAudioInfoList = usbSecondPlayList;
                    mNeedPlayUsbId = MusicConstants.PORT_1;
                } else if (usbSecond.getUuid().equals(mUsbTwoUuid)) {
                    mUsbTwoAudioInfo = usbSecond;
                    mUsbTwoAudioInfoList = usbSecondPlayList;
                    mNeedPlayUsbId = MusicConstants.PORT_2;
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void getRecentAudio() {
        LogUtil.debug(TAG, "getRecentAudio invoke ");
        mUsbOneAudioInfo = null;
        mUsbTwoAudioInfo = null;
        mUsbOneAudioInfoList = null;
        mUsbTwoAudioInfoList = null;
        try {
            Gson gson = new Gson();
            RecordAudioInfo usbFirst = null;
            RecordAudioInfo usbSecond = null;
            String data = SharedPreferencesHelps
                    .getObjectData(mContext, MusicConstants.USB_FIRST_UUID);
            if (!TextUtils.isEmpty(data)) {
                usbFirst = gson.fromJson(data, RecordAudioInfo.class);
            }
            data = SharedPreferencesHelps
                    .getObjectData(mContext, MusicConstants.USB_SECOND_UUID);
            if (!TextUtils.isEmpty(data)) {
                usbSecond = gson.fromJson(data, RecordAudioInfo.class);
            }
            if (!TextUtils.isEmpty(mUsbOnePath)) {
                mUsbOneAudioInfoList = MusicUtils.getInstance()
                        .filterAudio(mAudioBeans, mUsbOnePath);
            }
            if (!TextUtils.isEmpty(mUsbTwoPath)) {
                mUsbTwoAudioInfoList = MusicUtils.getInstance()
                        .filterAudio(mAudioBeans, mUsbTwoPath);
            }
            if (null != usbFirst) {
                LogUtil.debug(TAG, "getRecentAudio usbFirst getUuid : " + usbFirst.getUuid()
                        + " ,mUsbOneUuid : " + mUsbOneUuid + " ,mUsbTwoUuid : " + mUsbTwoUuid);
                if (usbFirst.getUuid().equals(mUsbOneUuid)) {
                    mUsbOneAudioInfo = usbFirst;
                } else if (usbFirst.getUuid().equals(mUsbTwoUuid)) {
                    mUsbTwoAudioInfo = usbFirst;
                }
            }
            if (null != usbSecond) {
                LogUtil.debug(TAG, "getRecentAudio usbSecond getUuid : "
                        + usbSecond.getUuid() + " ,mUsbOneUuid : "
                        + mUsbOneUuid + " ,mUsbTwoUuid : " + mUsbTwoUuid);
                if (usbSecond.getUuid().equals(mUsbOneUuid)) {
                    mUsbOneAudioInfo = usbSecond;
                } else if (usbSecond.getUuid().equals(mUsbTwoUuid)) {
                    mUsbTwoAudioInfo = usbSecond;
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Search for the location of recently played music if it exists. start to play
     */
    private void getRecentAudioLocation(List<AudioInfoBean> audioInfoBeanList,
                                        RecordAudioInfo recordAudioInfo) {
        LogUtil.debug(TAG, "getRecentAudioLocation invoke ");
        if (null != audioInfoBeanList && audioInfoBeanList.size() > 0) {
            LogUtil.debug(TAG, "getRecentAudioLocation audioInfoBeanList size :  "
                    + audioInfoBeanList.size());
            List<AudioInfoBean> checkList = new ArrayList<>();
            for (AudioInfoBean audioInfoBean : audioInfoBeanList) {
                if (MusicUtils.getInstance().checkFileExist(audioInfoBean.getAudioPath())) {
                    checkList.add(audioInfoBean);
                }
            }
            LogUtil.debug(TAG, "getRecentAudioLocation checkList size : "
                    + checkList.size());
            if (checkList.size() > 0) {
                audioInfoBeanList.clear();
                audioInfoBeanList.addAll(checkList);
            } else {
                return;
            }
            if (null != recordAudioInfo) {
                LogUtil.debug(TAG, "getRecentAudioLocation recordAudioInfo : " + recordAudioInfo);
                int position = MusicUtils.getInstance()
                        .getCurrentPlayIndex(audioInfoBeanList,
                                recordAudioInfo.getAudioId(), recordAudioInfo.getPath());
                if (position != -1) {
                    LogUtil.debug(TAG, "getRecentAudioLocation position : " + position);
                    if (recordAudioInfo.getPlayerState() != MusicConstants.MUSIC_PLAYER_PLAYING) {
                        mMemoryContorl = false;
                    }
                    startPlayMusic(audioInfoBeanList, position);
                    setPlayerMode(recordAudioInfo.getPlayMode());
                } else {
                    LogUtil.debug(TAG, "getRecentAudioLocation position  -1 ");
                    mMemoryContorl = false;
                    startPlayMusic(audioInfoBeanList, 0);
                }
            } else {
                LogUtil.debug(TAG, "getRecentAudioLocation recordAudio null ");
                mMemoryContorl = false;
                startPlayMusic(audioInfoBeanList, 0);
            }
        } else {
            LogUtil.debug(TAG, "getRecentAudioLocation null ");
        }
    }

    /**
     * Start playing a new audio queue and the player will replace the new music list.
     *
     * @param musicList Data set to be played
     * @param index     Specify where to play, 0-data.size()
     */

    public void startPlayMusic(List<AudioInfoBean> musicList, int index) {
        LogUtil.debug(TAG, "startPlayMusic invoke ");
        if (null != musicList && musicList.size() > 0) {
            LogUtil.debug(TAG, "startPlayMusic musicList size ::" + musicList.size());
            savePlayList();
            sAudios.clear();
            sAudios.addAll(musicList);
            startPlayMusicIndex(index);
        }
    }

    /**
     * Start playing a new audio.
     */
    public void startPlayMusic(AudioInfoBean audioInfo) {
        LogUtil.debug(TAG, "startPlayMusic invoke audioInfo = "
                + (audioInfo == null ? "" : audioInfo.getAudioName()));
    }

    /**
     * Start playing the audio file in the specified location, if the playlist exists.
     *
     * @param index Specified location, 0-data.size()
     */
    public void startPlayMusicIndex(int index) {
        LogUtil.debug(TAG, "startPlayMusicIndex :: invoke :: index" + index);
        if (null != sAudios && sAudios.size() > index) {
            mCurrentPlayIndex = index;
            AudioInfoBean baseMusicInfo = sAudios.get(index);
            mCurrentPlayAudioInfo = baseMusicInfo;
            startPlay(baseMusicInfo);
        }
    }

    /**
     * Start a new play task, and the player will automatically add it to the top of the queue,
     * that is, queue play.
     *
     * @param audioInfo Audio object
     */
    public void addPlayMusicToTop(AudioInfoBean audioInfo) {
        if (null == audioInfo) {
            return;
        }
        if (null == sAudios) {
            sAudios = new ArrayList<>();
        }
        AudioInfoBean playerMusic = getCurrentPlayerMusic();
        if (null != playerMusic && playerMusic.getAudioId() == audioInfo.getAudioId()) {
            return;
        }
        if (sAudios.size() > 0) {
            int position = -1;
            for (int i = 0; i < sAudios.size(); i++) {
                AudioInfoBean musicInfo = sAudios.get(i);
                if (audioInfo.getAudioId() == musicInfo.getAudioId()) {
                    position = i;
                    break;
                }
            }
            if (position > -1) {
                onReset();
                sAudios.remove(position);
            }
        }
        sAudios.add(0, audioInfo);
        startPlayMusicIndex(0);
    }

    /**
     * Start, pause playback.
     */
    public synchronized void playOrPause() {
        LogUtil.debug(TAG, "playOrPause :: invoke");
        if (null != sAudios && sAudios.size() > 0) {
            LogUtil.debug(TAG, "playOrPause :: do");
            switch (getPlayerState()) {
                case MusicConstants.MUSIC_PLAYER_INIT:
                case MusicConstants.MUSIC_PLAYER_IDLE:
                case MusicConstants.MUSIC_PLAYER_PREPARING:
                case MusicConstants.MUSIC_PLAYER_STOP:
                case MusicConstants.MUSIC_PLAYER_ERROR:
                case MusicConstants.MUSIC_PLAYER_END:
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                case MusicConstants.MUSIC_PLAYER_BUFFER:
                case MusicConstants.MUSIC_PLAYER_PLAYING:
                    pause();
                    break;
                case MusicConstants.MUSIC_PLAYER_PREPARE:
                case MusicConstants.MUSIC_PLAYER_PAUSE:
                    if (null != sAudioFocusManager) {
                        sAudioFocusManager.requestAudioFocus(null);
                    }
                    play();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Check the current state of mediaplayer to prevent the wrong operation
     * process from throwing wrong signals.
     */
    public boolean checkMediaPlayerState() {
        if (mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PREPARE
                || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PAUSE
                || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PLAYING) {
            return true;
        }
        return false;
    }

    /**
     * Pause playback.
     */
    public void pause() {
        LogUtil.debug(TAG, "pause :: invoke");
        try {
            if (null != sMediaPlayer && sMediaPlayer.isPlaying()) {
                LogUtil.debug(TAG, "pause :: do");
                sMediaPlayer.pause();
                stopTimer();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();

        } finally {
            mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PAUSE;
            checkCurrentPlayInfo();
            if (null != mCurrentPlayAudioInfo) {
                mCurrentPlayAudioInfo.setPlayState(mMusicPlayerState);
            }
            if (sMediaPlayer != null) {
                synchronized (sOnPlayerEventListeners) {
                    checkListener();
                    int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                    for (int index = 0; index < listenerCount; index++) {
                        try {
                            if (checkMediaPlayerState()) {
                                sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                        sMediaPlayer.getDuration(),
                                        sMediaPlayer.getCurrentPosition()
                                                + MusicConstants.CURRENT_DURATION, mBufferProgress,
                                        mCurrentPlayAudioInfo);
                            }
                            sOnPlayerEventListeners.getBroadcastItem(index)
                                    .onMusicPlayerState(mMusicPlayerState, null);
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                    sOnPlayerEventListeners.finishBroadcast();
                }
            }
            saveAudioPlaybackInfo();
        }
    }

    /**
     * Passive pause playback, only available for internal call when the focus is lost.
     */
    private void passivePause() {
        LogUtil.debug(TAG, "passivePause :: invoke");
        try {
            if (mIsPressing) {
                mIsPressing = false;
                mIsPassive = true;
                HardKeyMonitor.getInstance(mContext).focusOut();
            }
            if (null != sMediaPlayer && sMediaPlayer.isPlaying()) {
                LogUtil.debug(TAG, "passivePause :: do");
                mIsPassive = true;
                sMediaPlayer.pause();
                stopTimer();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PAUSE;
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onMusicPlayerState(mMusicPlayerState, null);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * Start playing.
     */
    public boolean play() {
        LogUtil.debug(TAG, "play :: invoke");
        if (mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PLAYING) {
            LogUtil.debug(TAG, "play :: is playing");
            return true;
        }
        try {
            if (sAudios == null || sAudios.size() == 0) {
                return false;
            }
            if (null != sMediaPlayer && checkMediaPlayerState()) {
                int requestAudioFocus = sAudioFocusManager.requestAudioFocus(
                        new MusicAudioFocusManager.OnAudioFocusListener() {
                            @Override
                            public void onFocusGet() {
                                if (mIsPassive) {
                                    play();
                                }
                            }

                            @Override
                            public void onFocusOut() {
                                passivePause();
                            }

                            @Override
                            public boolean isPlaying() {
                                return MusicMediaPlayer.this.isPlaying();
                            }
                        });
                if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus) {
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    sMediaPlayer.start();
                    startTimer();
                    mIsPassive = false;
                    mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PLAYING;
                    if (sMediaPlayer != null) {
                        LogUtil.debug(TAG, "sOnPlayerEventListeners :: is not null ");
                        synchronized (sOnPlayerEventListeners) {
                            checkListener();
                            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                            for (int index = 0; index < listenerCount; index++) {
                                try {
                                    sOnPlayerEventListeners.getBroadcastItem(index)
                                            .onMusicPlayerState(mMusicPlayerState, "");
                                } catch (RemoteException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            sOnPlayerEventListeners.finishBroadcast();
                        }
                    }
                } else {
                    LogUtil.debug(TAG, "request audio focus not granted, request result:"
                            + requestAudioFocus);
                    mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PAUSE;
                    stopTimer();
                    synchronized (sOnPlayerEventListeners) {
                        checkListener();
                        int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                        for (int index = 0; index < listenerCount; index++) {
                            try {
                                sOnPlayerEventListeners.getBroadcastItem(index)
                                        .onMusicPlayerState(mMusicPlayerState,
                                                "Failed to get audio output focus");
                            } catch (RemoteException ex) {
                                ex.printStackTrace();
                            }
                        }
                        sOnPlayerEventListeners.finishBroadcast();
                    }
                    if (requestAudioFocus == MusicConstants.REQUEST_DELAY) {
                        mIsPassive = true;
                        return true;
                    }
                    return false;
                }
            } else {
                LogUtil.debug(TAG, "play :: startPlayMusicIndex  :: " + mCurrentPlayIndex);
                startPlayMusicIndex(mCurrentPlayIndex);
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            mHandleTask.postDelayed(this::checkCurrentPlayState, MusicConstants.DELAY_DURATION);
        }
        return true;
    }

    private void checkCurrentPlayState() {
        if (null != sMediaPlayer && sMediaPlayer.isPlaying()) {
            mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PLAYING;
            LogUtil.debug(TAG, "play :: sOnPlayerEventListeners ");
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onMusicPlayerState(mMusicPlayerState, null);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * Set cycle mode.
     *
     * @param loop true:loop
     */
    public void setLoop(boolean loop) {
        LogUtil.debug(TAG, "setLoop :: invoke :: loop" + loop);
        sLoop = loop;
        try {
            if (null != sMediaPlayer && checkMediaPlayerState()) {
                sMediaPlayer.setLooping(loop);
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Continue last playback.
     *
     * @param sourcePath Absolute address of audio file
     */
    public void continuePlay(String sourcePath) {
        LogUtil.debug(TAG, "continuePlay :: invoke :: sourcePath" + sourcePath);
        if (TextUtils.isEmpty(sourcePath)) {
            return;
        }
        if (null != sAudios && sAudios.size() > mCurrentPlayIndex) {
            LogUtil.debug(TAG, "continuePlay :: do");
            (sAudios.get(mCurrentPlayIndex)).setAudioPath(sourcePath);
            startPlayMusicIndex(mCurrentPlayIndex);
        }
    }

    /**
     * Continue last playback.
     *
     * @param sourcePath Absolute address of audio file
     * @param index      Where to retry playback
     */
    public void continuePlayIndex(String sourcePath, int index) {
        LogUtil.debug(TAG, "continuePlayIndex :: invoke :: sourcePath"
                + sourcePath + ":: index::" + index);
        if (TextUtils.isEmpty(sourcePath)) {
            return;
        }
        if (null != sAudios && sAudios.size() > index) {
            LogUtil.debug(TAG, "continuePlayIndex :: do ");
            (sAudios.get(index)).setAudioPath(sourcePath);
            startPlayMusicIndex(index);
        }
    }

    /**
     * Set playback mode.
     *
     * @param mode Playback mode
     * @return Successfully set playback mode
     */
    public int setPlayerMode(int mode) {
        LogUtil.debug(TAG, "setPlayerMode" + mode);
        mPlayMode = mode;
        sLoop = false;
        if (mode == MusicConstants.MUSIC_MODE_SINGLE) {
            SharedPreferencesHelps.setPlayerMode(mContext, MusicConstants.KEY_PARAMS_MODE,
                    MusicConstants.SP_VALUE_MUSIC_MODE_SINGLE);
            sLoop = true;
        } else if (mode == MusicConstants.MUSIC_MODE_LOOP) {
            SharedPreferencesHelps.setPlayerMode(mContext, MusicConstants.KEY_PARAMS_MODE,
                    MusicConstants.SP_VALUE_MUSIC_MODE_LOOP);
        } else if (mode == MusicConstants.MUSIC_MODE_RANDOM) {
            SharedPreferencesHelps.setPlayerMode(mContext, MusicConstants.KEY_PARAMS_MODE,
                    MusicConstants.SP_VALUE_MUSIC_MODE_RANDOM);
        }
        if (sLoop && null != sMediaPlayer && checkMediaPlayerState()) {
            sMediaPlayer.setLooping(sLoop);
        }
        onCheckedPlayerConfig();
        return mPlayMode;
    }

    /**
     * Get playback mode.
     *
     * @return Player play mode
     */
    public int getPlayerMode() {
        return mPlayMode;
    }

    /**
     * Try to jump to a buffer.
     *
     * @param currentTime Time position
     */

    public void seekTo(long currentTime) {
        LogUtil.debug(TAG, "seekTo :: invoke :: currentTime" + currentTime);
        try {
            if (null != sMediaPlayer && checkMediaPlayerState()) {
                sMediaPlayer.seekTo((int) currentTime);
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Play the last song,
     * The player handles it automatically according to the playback mode set by the user.
     */
    public synchronized void playLastMusic() {
        LogUtil.debug(TAG, "playLastMusic :: invoke ");
        if (null != sAudios && sAudios.size() > 0) {
            LogUtil.debug(TAG, "playLastMusic :: do ");
            switch (getPlayerMode()) {
                // Single: equivalent list loop ; //List loop
                case MusicConstants.MUSIC_MODE_SINGLE:
                case MusicConstants.MUSIC_MODE_LOOP:
                    mCurrentPlayIndex--;
                    if (mCurrentPlayIndex < 0) {
                        mCurrentPlayIndex = sAudios.size() - 1;
                    }
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                // Random
                case MusicConstants.MUSIC_MODE_RANDOM:
                    mCurrentPlayIndex = MusicUtils.getInstance().getRandomNum(
                            0, sAudios.size() - 1, mCurrentPlayIndex);
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                default:
                    break;
            }
        }
        LogUtil.debug(TAG, "playLastMusic--newPlayIndex:" + mCurrentPlayIndex
                + ",MODE:" + getPlayerMode());
    }

    /**
     * Play the next song and maintain the next logic internally.
     */
    public synchronized void playNextMusic() {
        LogUtil.debug(TAG, "playNextMusic :: invoke ");
        if (null != sAudios && sAudios.size() > 0) {
            LogUtil.debug(TAG, "playNextMusic :: do ");
            switch (getPlayerMode()) {
                // Single: equivalent list loop; List loop
                case MusicConstants.MUSIC_MODE_SINGLE:
                case MusicConstants.MUSIC_MODE_LOOP:
                    if (mCurrentPlayIndex >= sAudios.size() - 1) {
                        mCurrentPlayIndex = 0;
                    } else {
                        mCurrentPlayIndex++;
                    }
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                // Random
                case MusicConstants.MUSIC_MODE_RANDOM:
                    mCurrentPlayIndex = MusicUtils.getInstance().getRandomNum(
                            0, sAudios.size() - 1, mCurrentPlayIndex);
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                default:
                    break;
            }
        }
        LogUtil.debug(TAG, "playNextMusic--newPlayIndex:" + mCurrentPlayIndex
                + ",MODE:" + getPlayerMode());
    }

    /**
     * Detect the playing position of the previous song.
     *
     * @return Legal playable location
     */
    public int playLastIndex() {
        LogUtil.debug(TAG, "playLastIndex :: invoke ");
        int tempIndex = mCurrentPlayIndex;
        if (null != sAudios && sAudios.size() > 0) {
            LogUtil.debug(TAG, "playLastIndex :: do ");
            switch (getPlayerMode()) {
                // Single: equivalent list loop;List loop
                case MusicConstants.MUSIC_MODE_SINGLE:
                case MusicConstants.MUSIC_MODE_LOOP:
                    tempIndex--;
                    if (tempIndex < 0) {
                        tempIndex = sAudios.size() - 1;
                    }
                    break;
                // Random
                case MusicConstants.MUSIC_MODE_RANDOM:
                    tempIndex = MusicUtils.getInstance().getRandomNum(0, sAudios.size() - 1,
                            mCurrentPlayIndex);
                    break;
                default:
                    break;
            }
        }
        LogUtil.debug(TAG, "playLastIndex--LAST_INDEX:" + tempIndex
                + ",MODE:" + getPlayerMode() + ",CURRENT_INDEX：" + mCurrentPlayIndex);
        return tempIndex;
    }

    /**
     * Detect the next song's playing position.
     *
     * @return Legal playable location
     */
    public int playNextIndex() {
        LogUtil.debug(TAG, "playNextIndex :: invoke ");
        int tempIndex = mCurrentPlayIndex;
        if (null != sAudios && sAudios.size() > 0) {
            LogUtil.debug(TAG, "playNextIndex :: do ");
            switch (getPlayerMode()) {
                // Single: equivalent list loop ; List loop
                case MusicConstants.MUSIC_MODE_SINGLE:
                case MusicConstants.MUSIC_MODE_LOOP:
                    if (tempIndex >= sAudios.size() - 1) {
                        tempIndex = 0;
                    } else {
                        tempIndex++;
                    }
                    break;
                // Random
                case MusicConstants.MUSIC_MODE_RANDOM:
                    tempIndex = MusicUtils.getInstance().getRandomNum(0, sAudios.size() - 1,
                            mCurrentPlayIndex);
                    break;
                default:
                    break;
            }
        }
        LogUtil.debug(TAG, "playNextIndex--NEWX_INDEX:" + tempIndex
                + ",MODE:" + getPlayerMode() + ",CURRENT_INDEX:" + mCurrentPlayIndex);
        return tempIndex;
    }

    /**
     * Random detection of the next song position will not trigger the play task.
     *
     * @return Legal playable location
     */
    public int playRandomNextIndex() {
        if (null != sAudios && sAudios.size() > 0) {
            return MusicUtils.getInstance().getRandomNum(0, sAudios.size() - 1, mCurrentPlayIndex);
        }
        return MusicConstants.PLAYER_STATUS_DESTROY;
    }

    /**
     * Return to the internal working state of the player.
     *
     * @return Start preparing, buffering, playing, etc :true,other： false
     */
    public boolean isPlaying() {
        try {
            return null != sMediaPlayer && (mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PLAYING
                    || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_BUFFER);
        } catch (RuntimeException ex) {
            ex.printStackTrace();

        }
        return false;
    }

    /**
     * Returns whether mediaplayer hasdata currently has data.
     */
    public boolean isMediaPlayerhasData() {
        try {
            return null != sMediaPlayer && (mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PREPARE
                    || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PLAYING
                    || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PAUSE
                    || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_BUFFER);
        } catch (RuntimeException ex) {
            ex.printStackTrace();

        }
        return false;
    }

    /**
     * Returns the total duration of the media audio object.
     *
     * @return Unit: ms
     */
    public long getDuration() {
        try {
            if (null != sMediaPlayer && (mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PREPARE
                    || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PLAYING
                    || mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PAUSE)) {
                return sMediaPlayer.getDuration();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();

        }
        return MusicConstants.MUSIC_PLAYER_STOP;
    }

    /**
     * Returns the ID of the audio object currently playing.
     *
     * @return Audio ID
     */
    public long getCurrentPlayerId() {
        if (mMusicPlayerState == MusicConstants.MUSIC_PLAYER_STOP) {
            return MusicConstants.MUSIC_PLAYER_STOP;
        }
        if (null != sAudios && sAudios.size() > mCurrentPlayIndex) {
            return (sAudios.get(mCurrentPlayIndex)).getAudioId();
        }
        return MusicConstants.MUSIC_PLAYER_STOP;
    }

    /**
     * Returns the currently playing audio object.
     *
     * @return Audio object
     */
    public AudioInfoBean getCurrentPlayerMusic() {
        LogUtil.debug(TAG, "getCurrentPlayerMusic :: invoke ");
        if (null != sAudios && sAudios.size() > mCurrentPlayIndex) {
            LogUtil.debug(TAG, "getCurrentPlayerMusic :: sAudios size " + sAudios.size()
                    + " : mCurrentPlayIndex " + mCurrentPlayIndex);
            return sAudios.get(mCurrentPlayIndex);
        }
        return null;
    }

    /**
     * Return to the currently playing audio queue.
     *
     * @return Audio queue
     */
    public List<AudioInfoBean> getCurrentPlayList() {
        return sAudios;
    }

    /**
     * Update the source property of the object being processed inside the player.
     */
    public void setPlayingChannel(int channel) {
        mPlayChannel = channel;
    }

    /**
     * Returns the source property of the object being processed inside the player.
     */
    public int getPlayingChannel() {
        return mPlayChannel;
    }

    /**
     * Return to the internal working state of the player.
     */
    public int getPlayerState() {
        return mMusicPlayerState;
    }

    /**
     * Check player configuration.
     */
    public void onCheckedPlayerConfig() {
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    sOnPlayerEventListeners.getBroadcastItem(index)
                            .onPlayerConfig(mPlayMode, false);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
    }

    /**
     * Check the current playing song data status.
     */
    public void checkCurrentPlayInfo() {
        if (null == mCurrentPlayAudioInfo) {
            if (null != sAudios && sAudios.size() > 0) {
                if (mCurrentPlayIndex > -1 && mCurrentPlayIndex < (sAudios.size() - 1)) {
                    mCurrentPlayAudioInfo = sAudios.get(mCurrentPlayIndex);
                }
            }
        }
    }

    /**
     * Check the audio object being processed inside the player.
     */
    public void onCheckedCurrentPlayTask() {
        if (null != sMediaPlayer && null != sAudios && sAudios.size() > 0) {
            AudioInfoBean musicInfo = sAudios.get(mCurrentPlayIndex);
            try {
                synchronized (sOnPlayerEventListeners) {
                    checkListener();
                    int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                    for (int index = 0; index < listenerCount; index++) {
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onMusicPlayerState(mMusicPlayerState, null);
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onPlayMusicOnInfo(musicInfo, mCurrentPlayIndex);
                        checkCurrentPlayInfo();
                        if (null != mCurrentPlayAudioInfo) {
                            mCurrentPlayAudioInfo.setPlayState(mMusicPlayerState);
                        }
                        if (checkMediaPlayerState()) {
                            sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                    sMediaPlayer.getDuration(),
                                    sMediaPlayer.getCurrentPosition()
                                            + MusicConstants.CURRENT_DURATION,
                                    mBufferProgress, mCurrentPlayAudioInfo);
                        } else {
                            sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(0, 0,
                                    mBufferProgress, mCurrentPlayAudioInfo);
                        }
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onPlayerConfig(mPlayMode, false);

                    }
                    sOnPlayerEventListeners.finishBroadcast();
                }

            } catch (RuntimeException | RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Remove player status listener.
     */
    public void addOnPlayerEventListener(IMusicPlayerEventListener listener) {
        sOnPlayerEventListeners.register(listener);
        if (isUsbMusicSource()) {
            onCheckedCurrentPlayTask();
        }
    }

    /**
     * Remove player status listener.
     */
    public void removePlayerListener(IMusicPlayerEventListener listener) {
        sOnPlayerEventListeners.unregister(listener);
    }

    /**
     * Remove all player status listeners.
     */
    public void removeAllPlayerListener() {
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                sOnPlayerEventListeners.unregister(sOnPlayerEventListeners
                        .getBroadcastItem(index));
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
    }

    /**
     * Listen to what the player is processing.
     */
    public void setPlayInfoListener(IMusicPlayerInfoListener listener) {
        sMusicPlayerInfoListener = listener;
    }

    /**
     * Remove listen to play object event.
     */
    public void removePlayInfoListener() {
        sMusicPlayerInfoListener = null;
    }

    /**
     * Release.
     */
    public void onReset() {
        mBufferProgress = 0;
        mIsPassive = false;
        try {
            if (null != sMediaPlayer) {
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_IDLE;
                sMediaPlayer.reset();
                sMediaPlayer.release();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_END;
                sMediaPlayer = null;
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * stop playing.
     */
    public void onStop() {
        LogUtil.debug(TAG, "onStop");
        mBufferProgress = 0;
        // Restore playback channel
        setPlayingChannel(MusicConstants.CHANNEL_LOCATION);
        seekTo(0);
        stopTimer();
        if (null != sAudioFocusManager) {
            sAudioFocusManager.releaseAudioFocus();
        }
        try {
            if (null != sMediaPlayer) {
                if (sMediaPlayer.isPlaying()) {
                    sMediaPlayer.stop();
                    mMusicPlayerState = MusicConstants.MUSIC_PLAYER_STOP;
                }
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_IDLE;
                sMediaPlayer.reset();
                sMediaPlayer.release();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_END;
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            sMediaPlayer = null;
            mMusicPlayerState = MusicConstants.MUSIC_PLAYER_END;
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onMusicPlayerState(mMusicPlayerState, null);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * Replace the waiting list in the player.
     *
     * @param audios To play list
     * @param index  Position
     */
    public void updateMusicPlayerData(List<AudioInfoBean> audios, int index) {
        LogUtil.debug(TAG, "updateMusicPlayerData :: invoke");
        if (null != audios && audios.size() > 0) {
            LogUtil.debug(TAG, "updateMusicPlayerData :: audios size ::" + audios.size());
            if (null == sAudios) {
                sAudios = new ArrayList<>();
            }
            savePlayList();
            sAudios.clear();
            sAudios.addAll(audios);
            mCurrentPlayIndex = Math.max(index, 0);
            mCurrentPlayAudioInfo = sAudios.get(index);
        }
    }

    /**
     * Try to change the playback mode, and switch between single, list loop and random modes.
     */
    public void changedPlayerPlayMode() {
        LogUtil.debug(TAG, "changedPlayerPlayMode :: invoke");
        // List loop
        if (mPlayMode == MusicConstants.MUSIC_MODE_LOOP) {
            mPlayMode = MusicConstants.MUSIC_MODE_SINGLE;
            sLoop = true;
            SharedPreferencesHelps.setPlayerMode(mContext, MusicConstants.KEY_PARAMS_MODE,
                    MusicConstants.SP_VALUE_MUSIC_MODE_SINGLE);
            // Single tune circulation
        } else if (mPlayMode == MusicConstants.MUSIC_MODE_SINGLE) {
            mPlayMode = MusicConstants.MUSIC_MODE_RANDOM;
            sLoop = false;
            SharedPreferencesHelps.setPlayerMode(mContext, MusicConstants.KEY_PARAMS_MODE,
                    MusicConstants.SP_VALUE_MUSIC_MODE_RANDOM);
            // Random play
        } else if (mPlayMode == MusicConstants.MUSIC_MODE_RANDOM) {
            mPlayMode = MusicConstants.MUSIC_MODE_LOOP;
            SharedPreferencesHelps.setPlayerMode(mContext, MusicConstants.KEY_PARAMS_MODE,
                    MusicConstants.SP_VALUE_MUSIC_MODE_LOOP);
            sLoop = false;
        }
        try {
            if (null != sMediaPlayer && checkMediaPlayerState()) {
                sMediaPlayer.setLooping(sLoop);
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();

        } finally {
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onPlayerConfig(mPlayMode, true);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * Start timing task.
     */
    private void startTimer() {
        LogUtil.debug(TAG, "startTimer :: invoke");
        if (null != mHandleTask) {
            LogUtil.debug(TAG, "startTimer :: do");
            mPlayTaskRunning = true;
            mHandleTask.removeCallbacksAndMessages(MusicConstants.MUSIC_PLAY_RUN_TASK);
            mHandleTask.sendEmptyMessage(MusicConstants.MUSIC_PLAY_RUN_TASK);
        }
    }

    /**
     * End timing task.
     */
    private void stopTimer() {
        LogUtil.debug(TAG, "stopTimer :: invoke");
        if (null != mHandleTask) {
            LogUtil.debug(TAG, "stopTimer :: do");
            mPlayTaskRunning = false;
            mHandleTask.removeCallbacksAndMessages(MusicConstants.MUSIC_PLAY_RUN_TASK);
        }
    }

    @Override
    public void mediaPrevious() {
        LogUtil.debug(TAG, "mediaPrevious :: invoke");
        if (isUsbMusicSource()) {
            LogUtil.debug(TAG, "mediaPrevious :: do");
            playLastMusic();
        }
    }

    @Override
    public void mediaNext() {
        LogUtil.debug(TAG, "mediaNext :: invoke");
        if (isUsbMusicSource()) {
            LogUtil.debug(TAG, "mediaNext :: do");
            playNextMusic();
        }
    }

    @Override
    public void mediaFastForward() {
        LogUtil.debug(TAG, "mediaFastForward :: invoke");
        if (isUsbMusicSource()) {
            LogUtil.debug(TAG, "isUsbMusicSource :: true");
            fastForward();
        }
    }

    @Override
    public void mediaFastBack() {
        LogUtil.debug(TAG, "mediaFastBack :: invoke");
        if (isUsbMusicSource()) {
            LogUtil.debug(TAG, "isUsbMusicSource :: true");
            fastBack();
        }
    }

    @Override
    public void keyEventUp() {
        LogUtil.debug(TAG, "keyEventUp :: invoke");
        if (isUsbMusicSource()) {
            LogUtil.debug(TAG, "isUsbMusicSource :: true");
            keyUp();
        }
    }

    @Override
    public void keyEventUp(int keyCode, boolean isLongPress) {
        // do nothing.
    }

    @Override
    public void keyEventDown(int keyCode, boolean isLongPress) {
        // do nothing.
    }

    @Override
    public void muteChanged(boolean isMute) {
        if (isUsbMusicSource() && isMute) {
            pause();
        } else if (isUsbMusicSource() && !isMute) {
            play();
        }
    }

    @Override
    public void muteChanged() {
        if (isUsbMusicSource() && mMusicPlayerState == MusicConstants.MUSIC_PLAYER_PLAYING) {
            pause();
        } else if (isUsbMusicSource()) {
            play();
        }
    }

    @Override
    public void resumePlay() {
        if (isUsbMusicSource()) {
            play();
        }
    }

    private boolean isUsbMusicSource() {
        // TODO baipeng
        return null != sMediaPlayer;
    }

    @Override
    public void queryComplete(List<AudioInfoBean> musicList) {
        if (mCurrentUpdateUsbPort != 0) {
            mAudioBeans = musicList;
            updatePlayListAfterScan(mCurrentUpdateUsbPort);
        }
        mCurrentUpdateUsbPort = 0;
    }

    /**
     * Play progress, alarm clock countdown progress timer.
     */
    private class PlayTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                saveAudioPlaybackInfo();
                checkCurrentPlayInfo();
                if (null != mCurrentPlayAudioInfo) {
                    mCurrentPlayAudioInfo.setPlayState(mMusicPlayerState);
                }
                synchronized (sOnPlayerEventListeners) {
                    checkListener();
                    int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                    for (int index = 0; index < listenerCount; index++) {
                        if (null != sMediaPlayer && checkMediaPlayerState()) {
                            sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                    sMediaPlayer.getDuration(), sMediaPlayer
                                            .getCurrentPosition() + MusicConstants
                                            .CURRENT_DURATION, mBufferProgress,
                                    mCurrentPlayAudioInfo);
                        } else {
                            sOnPlayerEventListeners.getBroadcastItem(index)
                                    .onTaskRuntime(-1, -1,
                                            mBufferProgress, mCurrentPlayAudioInfo);
                        }
                    }
                    sOnPlayerEventListeners.finishBroadcast();
                }
            } catch (RuntimeException | RemoteException ex) {
                ex.fillInStackTrace();
                onTaskRuntime();
            }
        }
    }

    private void playRunTask() {
        try {
            saveAudioPlaybackInfo();
            checkCurrentPlayInfo();
            if (null != mCurrentPlayAudioInfo) {
                mCurrentPlayAudioInfo.setPlayState(mMusicPlayerState);
            }
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                LogUtil.debug(TAG, "onTaskRuntime: sOnPlayerEventListeners = " + listenerCount);
                for (int index = 0; index < listenerCount; index++) {
                    if (null != sMediaPlayer && checkMediaPlayerState()) {
                        sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                sMediaPlayer.getDuration(), sMediaPlayer
                                        .getCurrentPosition() + MusicConstants
                                        .CURRENT_DURATION, mBufferProgress,
                                mCurrentPlayAudioInfo);
                    } else {
                        sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(-1, -1, mBufferProgress, mCurrentPlayAudioInfo);
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        } catch (RuntimeException | RemoteException ex) {
            ex.fillInStackTrace();
            onTaskRuntime();
        }
    }

    private void saveAudioPlaybackInfo() {
        if (null != sAudios && null != sMediaPlayer && sAudios.size() > 0) {
            RecordAudioInfo recordAudioInfo = new RecordAudioInfo();
            String path = sAudios.get(mCurrentPlayIndex).getAudioPath();
            // Play mode
            recordAudioInfo.setPlayMode(mPlayMode);
            // Path
            recordAudioInfo.setPath(path);
            // AudioId
            recordAudioInfo.setAudioId(sAudios.get(mCurrentPlayIndex).getAudioId());
            // UuId
            recordAudioInfo.setUuid(MusicUtils.getInstance().getUuidFromPath(path));
            // Player state
            recordAudioInfo.setPlayerState(mMusicPlayerState);
            // Current play index
            recordAudioInfo.setCurrentIndex(mCurrentPlayIndex);
            // Current time
            recordAudioInfo.setCurrentTime(System.currentTimeMillis());
            // Cache info
            UsbDevicesInfoBean usbDevicesInfoBean = getUsbInfoByAudioPath(path, mUsbDevicesInfoBeans);
            if (null != usbDevicesInfoBean) {
                LogUtil.debug(TAG, "saveAudioPlaybackInfo getPort " + usbDevicesInfoBean.getPort());
                if (usbDevicesInfoBean.getPort().equals(MusicConstants.USB_1_PORT)) {
                    if (mUsbOneAudioInfoList == null) {
                        if (!TextUtils.isEmpty(mUsbOnePath)) {
                            mUsbOneAudioInfoList = MusicUtils.getInstance().filterAudio(mAudioBeans, mUsbTwoPath);
                        }
                    }
                    MusicUtils.getInstance().saveAudioPlaybackInfo(mContext, recordAudioInfo, mUsbOneAudioInfoList);
                } else if (usbDevicesInfoBean.getPort().equals(MusicConstants.USB_2_PORT)) {
                    if (mUsbTwoAudioInfoList == null) {
                        if (!TextUtils.isEmpty(mUsbTwoPath)) {
                            mUsbTwoAudioInfoList = MusicUtils.getInstance().filterAudio(mAudioBeans, mUsbOnePath);
                        }
                    }
                    MusicUtils.getInstance().saveAudioPlaybackInfo(mContext, recordAudioInfo, mUsbTwoAudioInfoList);
                }
                LogUtil.debug(TAG, "cache info=" + recordAudioInfo.toString());
            }
        }
    }


    private void onTaskRuntime() {
        LogUtil.debug(TAG, "service : onTaskRuntime:: invoke");
        checkCurrentPlayInfo();
        if (null != mCurrentPlayAudioInfo) {
            mCurrentPlayAudioInfo.setPlayState(mMusicPlayerState);
        }
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    sOnPlayerEventListeners.getBroadcastItem(index)
                            .onTaskRuntime(
                                    MusicConstants.PLAYER_STATUS_DESTROY,
                                    MusicConstants.PLAYER_STATUS_DESTROY,
                                    mBufferProgress, mCurrentPlayAudioInfo);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
    }

    /**
     * Start playing media files.
     */
    private synchronized void startPlay(AudioInfoBean musicInfo) {
        onReset();
        if (null != musicInfo && !TextUtils.isEmpty(musicInfo.getAudioPath())) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(musicInfo.getAudioPath());
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            LogUtil.debug(TAG, "genre :" + genre + " mCarAudioManager:" + mCarAudioManager);
            if (genre != null && !"".equals(genre) && mCarAudioManager != null) {
                AudioSetting setting = new AudioSetting(AudioSetting.AUDIO_SETTING_PRESET_EQ, 0, 0, 0);
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
            if (null == sAudioFocusManager) {
                sAudioFocusManager = new MusicAudioFocusManager(mContext);
            }
            mIsPassive = false;
            if (null != sMusicPlayerInfoListener) {
                try {
                    sMusicPlayerInfoListener.onPlayMusicInfo(musicInfo, mCurrentPlayIndex);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            postViewHandlerCurrentPosition(mCurrentPlayIndex);
            try {
                sMediaPlayer = new MediaPlayer();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_IDLE;
                sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                sMediaPlayer.setOnPreparedListener(this);
                sMediaPlayer.setOnCompletionListener(this);
                sMediaPlayer.setOnBufferingUpdateListener(this);
                sMediaPlayer.setOnSeekCompleteListener(this);
                sMediaPlayer.setOnErrorListener(this);
                sMediaPlayer.setOnInfoListener(this);
                sMediaPlayer.setLooping(sLoop);
                sMediaPlayer.setDataSource(musicInfo.getAudioPath());
                LogUtil.debug(TAG, "startPlay-->: ID:" + musicInfo.getAudioId() + ",TITLE:" + musicInfo.getAudioName() + ",PATH:" + musicInfo.getAudioPath());
                synchronized (sOnPlayerEventListeners) {
                    checkListener();
                    int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                    for (int index = 0; index < listenerCount; index++) {
                        sOnPlayerEventListeners.getBroadcastItem(index).onMusicPlayerState(mMusicPlayerState, "Playe preparation");
                    }
                    sOnPlayerEventListeners.finishBroadcast();
                }
                sMediaPlayer.prepareAsync();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PREPARING;
            } catch (Exception ex) {
                ex.printStackTrace();
                LogUtil.debug(TAG, "startPlay---" + ex.getMessage());
                stopTimer();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_ERROR;
                synchronized (sOnPlayerEventListeners) {
                    checkListener();
                    int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                    for (int index = 0; index < listenerCount; index++) {
                        try {
                            sOnPlayerEventListeners.getBroadcastItem(index).onMusicPlayerState(mMusicPlayerState, "Broadcast failure，" + ex.getMessage());
                        } catch (RemoteException re) {
                            re.printStackTrace();
                        }
                    }
                    sOnPlayerEventListeners.finishBroadcast();
                }
            }
        } else {
            LogUtil.debug(TAG, "startPlay---" + "Audio Path isEmpty---");
            mMusicPlayerState = MusicConstants.MUSIC_PLAYER_ERROR;
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        sOnPlayerEventListeners.getBroadcastItem(index).onMusicPlayerState(mMusicPlayerState, null);
                        sOnPlayerEventListeners.getBroadcastItem(index).onMusicPathInvalid(musicInfo, mCurrentPlayIndex);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * Convert playback address.
     */
    private String getPlayPath(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            return Uri.parse(filePath).getPath();
        }
        return null;
    }

    /**
     * The player starts the next task automatically according to the playing mode.
     */
    private void onCompletionPlay() {
        if (null != sAudios && sAudios.size() > 0) {
            switch (getPlayerMode()) {
                // single
                case MusicConstants.MUSIC_MODE_SINGLE:
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                // List loop
                case MusicConstants.MUSIC_MODE_LOOP:
                    if (mCurrentPlayIndex >= sAudios.size() - 1) {
                        mCurrentPlayIndex = 0;
                    } else {
                        mCurrentPlayIndex++;
                    }
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                // Random
                case MusicConstants.MUSIC_MODE_RANDOM:
                    mCurrentPlayIndex = MusicUtils.getInstance().getRandomNum(0, sAudios.size() - 1, mCurrentPlayIndex);
                    postViewHandlerCurrentPosition(mCurrentPlayIndex);
                    startPlayMusicIndex(mCurrentPlayIndex);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Submit to UI component,
     * and the object location that is currently being processed automatically internally.
     *
     * @param currentPlayIndex Index
     */
    private void postViewHandlerCurrentPosition(int currentPlayIndex) {
        if (null != sAudios && sAudios.size() > currentPlayIndex) {
            mCurrentPlayIndex = currentPlayIndex;
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onPlayMusicOnInfo(sAudios.get(currentPlayIndex), currentPlayIndex);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * After the buffer is completed, the call is successful only when the buffer is successful.
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        LogUtil.debug(TAG, "onPrepared invoke");
        mMusicPlayerState = MusicConstants.MUSIC_PLAYER_PREPARE;
        if (sMediaPlayer == null) {
            return;
        }
        if (!mMemoryContorl) {
            stopTimer();
            mMemoryContorl = true;
            return;
        }
        play();
    }

    /**
     * Call after playing is completed. After playing,
     * it will not stop actively and start the next song automatically.
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mMusicPlayerState = MusicConstants.MUSIC_PLAYER_STOP;
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    sOnPlayerEventListeners.getBroadcastItem(index)
                            .onMusicPlayerState(mMusicPlayerState, null);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
        // After playing,
        // play the next song automatically according to the playback mode set by the user
        if (isUsbMusicSource()) {
            onCompletionPlay();
        }
    }

    /**
     * Buffer progress.
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int progress) {
        mBufferProgress = progress;
    }

    /**
     * set progress complete call.
     */
    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        if (mIsPressing) {
            return;
        }
        if (null != mCurrentPlayAudioInfo) {
            mCurrentPlayAudioInfo.setPlayState(mMusicPlayerState);
        }
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    if (checkMediaPlayerState()) {
                        sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                sMediaPlayer.getDuration(),
                                sMediaPlayer.getCurrentPosition(),
                                mBufferProgress,
                                mCurrentPlayAudioInfo);
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
    }

    /**
     * Broadcast failure.
     */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int event, int extra) {
        LogUtil.debug(TAG, "onError--EVENT:" + event + ",EXTRA:" + extra);
        mMusicPlayerState = MusicConstants.MUSIC_PLAYER_STOP;
        stopTimer();
        onReset();
        String content = getErrorMessage(event);
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    sOnPlayerEventListeners.getBroadcastItem(index)
                            .onMusicPlayerState(mMusicPlayerState, content);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }

        if (!isUsbMusicSource()) {
            return false;
        }

        if (mPlayMode != MusicConstants.MUSIC_MODE_SINGLE
                && null != sAudios && sAudios.size() > 1) {
            LogUtil.debug(TAG, "onError :: will   onCompletionPlay ");
            onCompletionPlay();
        } else {
            LogUtil.debug(TAG, "onError :: won't   onCompletionPlay ");
        }
        return false;
    }

    private void checkListener() {
        try {
            sOnPlayerEventListeners.finishBroadcast();
        } catch (IllegalStateException ex) {
            LogUtil.debug(TAG, "RemoteCallbackList checkListener");
        }
    }

    private String getErrorMessage(int event) {
        String content = MusicConstants.ErrorCode.UNKNOWN_ERROR;
        switch (event) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                content = MusicConstants.ErrorCode.UNKNOWN_ERROR;
                break;
            // Received error (s) app must re instantiate new mediaplayer
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                content = MusicConstants.ErrorCode.PLAYER_INTERNAL_ERROR;
                break;
            // Stream start position error
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                content = MusicConstants.ErrorCode.MEDIA_STREAMING_ERROR;
                break;
            // IO,timeout error
            case MediaPlayer.MEDIA_ERROR_IO:
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                content = MusicConstants.ErrorCode.NETWORK_CONNECTION_TIMEOUT;
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                content = MusicConstants.ErrorCode.PLAY_REQUEST_FAILED;
                break;
            default:
                break;
        }
        return content;
    }

    /**
     * Get audio information.
     */
    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int event, int extra) {
        int state = -1;
        if (event == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            state = MusicConstants.MUSIC_PLAYER_BUFFER;
        } else if (event == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            state = MusicConstants.MUSIC_PLAYER_PLAYING;
        } else if (event == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            state = MusicConstants.MUSIC_PLAYER_PLAYING;
        }
        if (state > -1) {
            mMusicPlayerState = state;
        }
        synchronized (sOnPlayerEventListeners) {
            checkListener();
            int listenerCount = sOnPlayerEventListeners.beginBroadcast();
            for (int index = 0; index < listenerCount; index++) {
                try {
                    sOnPlayerEventListeners.getBroadcastItem(index)
                            .onMusicPlayerState(mMusicPlayerState, null);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            sOnPlayerEventListeners.finishBroadcast();
        }
        return false;
    }

    /**
     * Destroy.
     */
    public void destroy() {
        stopTimer();
        if (null != sAudioFocusManager) {
            sAudioFocusManager.releaseAudioFocus();
            sAudioFocusManager = null;
        }
        try {
            if (null != sMediaPlayer) {
                if (sMediaPlayer.isPlaying()) {
                    sMediaPlayer.stop();
                    mMusicPlayerState = MusicConstants.MUSIC_PLAYER_STOP;
                }
                sMediaPlayer.reset();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_IDLE;
                sMediaPlayer.release();
                mMusicPlayerState = MusicConstants.MUSIC_PLAYER_END;
                mUsbOneAudioInfoList = null;
                mUsbTwoAudioInfoList = null;
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            sMediaPlayer = null;
            mMusicPlayerState = MusicConstants.MUSIC_PLAYER_END;
        }
        mIsPassive = false;
        if (null != sAudios) {
            sAudios.clear();
        }
        if (null != sAudioFocusManager) {
            sAudioFocusManager.onDestroy();
            sAudioFocusManager = null;
        }
    }

    /**
     * Fast forward.
     */
    public void fastForward() {
        LogUtil.debug(TAG, "fastForward :: invoke");
        if (null != sMediaPlayer && checkMediaPlayerState()) {
            if (!mIsPressing) {
                mIsPressing = true;
                pause();
            }
            int position = sMediaPlayer.getCurrentPosition() + MusicConstants.FAST_FORWARD_OFFSET;
            position = Math.min(position, sMediaPlayer.getDuration());
            LogUtil.debug(TAG, " seekTo position ::  " + String.valueOf(position));
            sMediaPlayer.seekTo(position);
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        if (checkMediaPlayerState()) {
                            sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                    sMediaPlayer.getDuration(),
                                    sMediaPlayer.getCurrentPosition(),
                                    mBufferProgress,
                                    mCurrentPlayAudioInfo);
                        }
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onMusicPlayerState(mMusicPlayerState, null);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * Fast back.
     */
    public void fastBack() {
        LogUtil.debug(TAG, "fastBack :: invoke");
        if (null != sMediaPlayer && checkMediaPlayerState()) {
            if (!mIsPressing) {
                mIsPressing = true;
                pause();
            }
            int position = sMediaPlayer.getCurrentPosition();
            if (position > MusicConstants.FAST_FORWARD_OFFSET) {
                position -= MusicConstants.FAST_FORWARD_OFFSET;
            } else {
                position = 1;
            }
            LogUtil.debug(TAG, "seekTo position ::  " + String.valueOf(position));
            sMediaPlayer.seekTo(position);
            synchronized (sOnPlayerEventListeners) {
                checkListener();
                int listenerCount = sOnPlayerEventListeners.beginBroadcast();
                for (int index = 0; index < listenerCount; index++) {
                    try {
                        if (checkMediaPlayerState()) {
                            sOnPlayerEventListeners.getBroadcastItem(index).onTaskRuntime(
                                    sMediaPlayer.getDuration(),
                                    sMediaPlayer.getCurrentPosition(),
                                    mBufferProgress,
                                    mCurrentPlayAudioInfo);
                        }
                        sOnPlayerEventListeners.getBroadcastItem(index)
                                .onMusicPlayerState(mMusicPlayerState, null);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
                sOnPlayerEventListeners.finishBroadcast();
            }
        }
    }

    /**
     * keyUp.
     */
    public void keyUp() {
        LogUtil.debug(TAG, "keyUp :: invoke");
        if (mIsPressing) {
            mIsPressing = false;
            play();
        }
    }
}
