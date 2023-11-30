package com.ts.service.media.utils;

import android.car.CarNotConnectedException;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.model.entity.RecordAudioInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MusicUtils.
 */
public class MusicUtils {
    private static final String TAG = MusicUtils.class.getSimpleName();
    private static final String PATH_SEPARATOR = "/";
    private static final int USB_MIN_DEPTH = 3;
    private static final String[] AUDIO_TYPE = {"mp3", "aac", "ogg", "wav", "ape", "flac"};
    private static volatile MusicUtils sInstance;

    /**
     * Initialization.
     */
    public static MusicUtils getInstance() {
        if (null == sInstance) {
            synchronized (MusicUtils.class) {
                if (null == sInstance) {
                    sInstance = new MusicUtils();
                }
            }
        }
        return sInstance;
    }

    /**
     * Generate a random number between min and Max, including min max.
     *
     * @param min Minimum
     * @param max Maximum number
     */
    public int getRandomNum(int min, int max, int currentIndex) {
        int index = 0;
        if (max > 0) {
            do {
                index = min + (Math.round((float) Math.random() * max));
            } while (index == currentIndex);
        }
        return index;
    }

    /**
     * Get the cover page address of the audio file.
     */
    public String getMusicFrontPath(AudioInfoBean audioInfo) {
        return audioInfo.getAudioPath();
    }

    /**
     * Record playback progress.
     *
     * @param recordAudioInfo RecordInfo
     */
    public void saveAudioPlaybackInfo(Context context, RecordAudioInfo recordAudioInfo,
                                      List<AudioInfoBean> audioInfoBeanList) {
        Gson gson = new Gson();
        RecordAudioInfo usbFirst = gson.fromJson(SharedPreferencesHelps
                        .getObjectData(context, MusicConstants.USB_FIRST_UUID),
                RecordAudioInfo.class);
        RecordAudioInfo usbSecond = gson.fromJson(SharedPreferencesHelps
                        .getObjectData(context, MusicConstants.USB_SECOND_UUID),
                RecordAudioInfo.class);
        List<AudioInfoBean> usbSecondPlayList = gson.fromJson(SharedPreferencesHelps
                        .getObjectData(context, MusicConstants.SP_MUSIC_USB_KEY
                                + MusicConstants.USB_SECOND_UUID),
                new TypeToken<List<AudioInfoBean>>() {
                }.getType());
        LogUtil.debug(TAG, "saveAudioPlaybackInfo :: usbFirst :"
                + usbFirst + " : usbSecond : " + usbSecond);
        if (null == usbSecond) {
            LogUtil.debug(TAG, "saveAudioPlaybackInfo :: usbSecond  isEmpty");
            LogUtil.debug(TAG, "saveAudioPlaybackInfo :: usbFirst diff ");
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.USB_SECOND_UUID, recordAudioInfo);
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.SP_MUSIC_USB_KEY
                            + MusicConstants.USB_SECOND_UUID, audioInfoBeanList);
        } else if (usbSecond.getUuid().equals(recordAudioInfo.getUuid())) {
            LogUtil.debug(TAG, "saveAudioPlaybackInfo :: usbSecond same");
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.USB_SECOND_UUID, recordAudioInfo);
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.SP_MUSIC_USB_KEY
                            + MusicConstants.USB_SECOND_UUID, audioInfoBeanList);
        } else {
            LogUtil.debug(TAG, "saveAudioPlaybackInfo :: usbFirst same :: change ");
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.USB_FIRST_UUID, usbSecond);
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.SP_MUSIC_USB_KEY
                            + MusicConstants.USB_FIRST_UUID, usbSecondPlayList);
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.USB_SECOND_UUID, recordAudioInfo);
            SharedPreferencesHelps.setObjectData(context,
                    MusicConstants.SP_MUSIC_USB_KEY
                            + MusicConstants.USB_SECOND_UUID, audioInfoBeanList);
        }
    }

    public String getUuidFromPath(String path) {
        String[] strings = path.split(MusicConstants.FILE_SIGN);
        return strings[MusicConstants.UUID_INDEX];
    }

    /**
     * Return to the playing position.
     */
    public int getCurrentPlayIndex(List<AudioInfoBean> audioInfo, long musicId, String path) {
        int index = -1;
        if (null != audioInfo && audioInfo.size() > 0) {
            for (int i = 0; i < audioInfo.size(); i++) {
                if (path.equals(audioInfo.get(i).getAudioPath())) {
                    if (checkFileExist(audioInfo.get(i).getAudioPath())) {
                        index = i;
                    }
                }
            }
        }
        return index;
    }

    /**
     * check file exist or not.
     */
    public boolean checkFileExist(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * Filter audio files.
     *
     * @param audioInfoBeans Audio list
     * @param targetDir      Filter condition
     * @return Filter result
     */
    public List<AudioInfoBean> filterAudio(List<AudioInfoBean> audioInfoBeans, String targetDir) {
        if (targetDir.isEmpty() || null == audioInfoBeans || audioInfoBeans.size() == 0) {
            return null;
        }
        // Add corresponding data according to USB road strength
        List<AudioInfoBean> usbSourceData = new ArrayList<>();
        for (int i = 0; i < audioInfoBeans.size(); i++) {
            if (audioInfoBeans.get(i).getAudioPath().contains(targetDir)) {
                usbSourceData.add(audioInfoBeans.get(i));
            }
        }
        SortUtils.sortAudioListData(usbSourceData);
        return usbSourceData;
    }

    /**
     * Filter audio files.
     *
     * @param audioInfoBeans Audio list
     * @param targetDir      Filter condition
     * @return Filter result
     */
    public List<AudioInfoBean> filterCurrentFolderAudio(List<AudioInfoBean> audioInfoBeans,
                                                        String targetDir) {

        if (targetDir.isEmpty() || null == audioInfoBeans || audioInfoBeans.size() == 0) {
            return null;
        }
        LogUtil.debug(TAG, "filterAudio targetDir: " + targetDir + ", count: "
                + audioInfoBeans.size());
        int targetDirDepth = targetDir.split(PATH_SEPARATOR).length;
        LogUtil.debug(TAG, "filterAudio targetDir depth: " + targetDirDepth);
        if (targetDirDepth < USB_MIN_DEPTH) {
            return null;
        }
        Iterator<AudioInfoBean> iterator = audioInfoBeans.iterator();
        List<AudioInfoBean> resultAudios = new ArrayList<>();
        AudioInfoBean bean;
        while (iterator.hasNext()) {
            bean = iterator.next();
            String path = bean.getAudioPath();
            if (!path.contains(targetDir)) {
                continue;
            }
            String[] strings = path.split(PATH_SEPARATOR);
            int depth = strings.length;
            if (depth == targetDirDepth + 1) {  // music
                LogUtil.debug(TAG, "filterAudio add music: " + path);
                resultAudios.add(bean);
            }

        }
        SortUtils.sortAudioListData(resultAudios);
        return resultAudios;
    }

    /**
     * Get previous level path.
     */
    public String getPreviousLevelPath(String path) {
        LogUtil.debug(TAG, "getPreviousLevelPath :: invoke :: path" + path);
        if (path.split(PATH_SEPARATOR).length > 2) {
            path = new StringBuffer(path).substring(0, path.lastIndexOf(PATH_SEPARATOR));
        }
        return path;
    }

    /**
     * Filter the types of songs that can be played.
     */
    public boolean filterAudioType(String audioPath) {
        LogUtil.debug(TAG, "filterAudioType :: invoke :: audioPath :: " + audioPath);
        boolean vaild = false;
        if (!TextUtils.isEmpty(audioPath)) {
            int start = audioPath.lastIndexOf(".") + 1;
            int end = audioPath.length();
            if (start > 0 && end > 0 && start < end) {
                String audioType = new StringBuffer(audioPath.toLowerCase()).substring(start, end);
                LogUtil.debug(TAG, "filterAudioType ::audioType :: " + audioType);
                for (String vaildType : AUDIO_TYPE) {
                    if (vaildType.equals(audioType)) {
                        LogUtil.debug(TAG, "vaild audioType :: " + audioType);
                        vaild = true;
                    }
                }
            }
        }
        return vaild;
    }

    /**
     * According to genre set eq if smart.
     * @param genre Music genre.
     * @param carAudioManager Car audio.
     */
    public void setPresetEqForSmart(String genre, CarAudioManager carAudioManager) {
        LogUtil.debug(TAG, "setPresetEqForSmart");
        try {
            if (genre.matches(MusicConstants.REGEX_CLASSIC)) {
                LogUtil.debug(TAG, "setPresetEqForSmart is classic");
                carAudioManager.setPresetEQForSmart(MusicConstants.SETTING_PRESET_EQ_CLASSIC);
            } else if (genre.matches(MusicConstants.REGEX_POP)) {
                LogUtil.debug(TAG, "setPresetEqForSmart is pop");
                carAudioManager.setPresetEQForSmart(MusicConstants.SETTING_PRESET_EQ_POPS);
            } else if (genre.matches(MusicConstants.REGEX_VOCALS)) {
                LogUtil.debug(TAG, "setPresetEqForSmart is vocals");
                carAudioManager.setPresetEQForSmart(MusicConstants.SETTING_PRESET_EQ_VOCAL);
            } else if (genre.matches(MusicConstants.REGEX_JAZZ)) {
                LogUtil.debug(TAG, "setPresetEqForSmart is jazz");
                carAudioManager.setPresetEQForSmart(MusicConstants.SETTING_PRESET_EQ_JAZZ);
            } else if (genre.matches(MusicConstants.REGEX_ROCK)) {
                LogUtil.debug(TAG, "setPresetEqForSmart is rock");
                carAudioManager.setPresetEQForSmart(MusicConstants.SETTING_PRESET_EQ_ROCK);
            } else if (genre.matches(MusicConstants.REGEX_CUSTOM)) {
                LogUtil.debug(TAG, "setPresetEqForSmart is custom");
                carAudioManager.setPresetEQForSmart(MusicConstants.SETTING_PRESET_EQ_USER);
            } else {
                LogUtil.debug(TAG, "setPresetEqForSmart :: genre is invalid");
            }
        } catch (CarNotConnectedException ex) {
            ex.printStackTrace();
        }
    }
}
