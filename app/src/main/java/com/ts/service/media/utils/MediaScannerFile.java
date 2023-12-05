package com.ts.service.media.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import com.ts.sdk.media.bean.AudioInfoBean;
import com.ts.sdk.media.bean.VideoInfoBean;
import com.ts.service.media.constants.MusicConstants;
import com.ts.service.media.constants.VideoConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaScannerFile {
    private static final String TAG = "MediaScannerFile";
    private static final int VIDEO_THUMBNAILS_MSG = 1;
    private static final int SCAN_LIST_MAX = 50;
    private static final int SCAN_LIST_UPDATE_TIME = 2000;
    private static final String[] VIDEO_PROJECT = {MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.MIME_TYPE};
    private static final String[] VIDEO_FILTER_TYPE = {VideoConstants.VIDEO_TYPE_MPG,
        VideoConstants.VIDEO_TYPE_MPEG};
    private static final String[] THUMB_COLUMNS = {MediaStore.Video.Thumbnails.DATA,
        MediaStore.Video.Thumbnails.VIDEO_ID};
    private static final String PERCENT_SIGN = "%";
    private static final String SEARCH_LIKE = " like ?";
    private static MediaScannerFile sInstance;
    private static Context sContext;
    private static ContentResolver sContentResolver;
    private IVideoScanListener mVideoScanListener;
    private IAudioScanListener mAudioScanListener;
    private IAudioQueryListener mAudioQueryListener;
    private List<VideoInfoBean> mVideoInfoList = new ArrayList<>();
    private List<AudioInfoBean> mAudioQueryClientList = new ArrayList<>();
    private List<AudioInfoBean> mAudioQueryLocalList = new ArrayList<>();
    private List<VideoInfoBean> mVideoQueryList = new ArrayList<>();
    private List<AudioInfoBean> mAudioScanningList = new ArrayList<>();
    private Handler mHandler =  new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case VIDEO_THUMBNAILS_MSG:
                    MediaStore.Video.Thumbnails.getThumbnail(sContentResolver,
                            (Long) msg.obj, MediaStore.Video.Thumbnails.MINI_KIND, null);
                    break;
                case MusicConstants.QUERY_MUSIC_CLIENT:
                    mAudioScanListener.queryComplete(mAudioQueryClientList);
                    break;
                case MusicConstants.QUERY_MUSIC_LOCAL:
                    mAudioQueryListener.queryComplete(mAudioQueryLocalList);
                    break;
                case VideoConstants.QUERY_VIDEO_CLIENT:
                    mVideoScanListener.queryComplete(mVideoQueryList);
                    break;
                case MusicConstants.AUDIO_SCAN_UPDATE:
                    mHandler.removeMessages(MusicConstants.AUDIO_SCAN_UPDATE);
                    updateScanListToClient();
                    break;
                default:
                    break;
            }

            return false;
        }
    });

    /**
     * Get instance of MediaScannerFile.
     *
     * @param context Context
     * @return Instance
     */
    public static MediaScannerFile getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MediaScannerFile.class) {
                if (sInstance == null) {
                    sInstance = new MediaScannerFile();
                    sContext = context;
                    sContentResolver = context.getContentResolver();
                }
            }
        }
        return sInstance;
    }

    /**
     * Media database query music files.
     */
    public void queryAllMusic(int what) {
        if (what == MusicConstants.QUERY_MUSIC_CLIENT) {
            LogUtil.debug(TAG, "queryAllMusic: client");
        } else if (what == MusicConstants.QUERY_MUSIC_LOCAL) {
            LogUtil.debug(TAG, "queryAllMusic: local");
        }

        new Thread(() -> {
            handleQueryAudio(what);
            Message message = new Message();
            message.what = what;
            mHandler.sendMessage(message);
        }).start();
    }

    /**
     * Media database query video files.
     */
    public void queryAllVideo(int what) {
        new Thread(() -> {
            handleQueryVideo();
            Message message = new Message();
            message.what = what;
            mHandler.sendMessage(message);
        }).start();
    }

    /**
     * Media database query music files.
     */
    public List<AudioInfoBean> queryMusic(String keyword) {
        List<AudioInfoBean> musicInfoList = new ArrayList<>();
        String selection;
        String[] selectionArgs;
        if (keyword == null || keyword.isEmpty()) {
            selection = null;
            selectionArgs = null;
        } else {
            selection = MediaStore.Audio.Media.TITLE + SEARCH_LIKE + " OR "
                    + MediaStore.Audio.Media.ARTIST + SEARCH_LIKE + " OR "
                    + MediaStore.Audio.Media.ALBUM + SEARCH_LIKE;
            selectionArgs = new String[]{PERCENT_SIGN + keyword
                    + PERCENT_SIGN,PERCENT_SIGN + keyword
                    + PERCENT_SIGN, PERCENT_SIGN + keyword + PERCENT_SIGN};
        }
        Cursor cursor = sContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor == null) {
            return musicInfoList;
        }
        // id title singer data time image
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

            // If not music
            String isMusic = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) {
                continue;
            }
            AudioInfoBean music = new AudioInfoBean();
            // id
            music.setAudioId(cursor.getInt(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            // album
            music.setAudioAlbumName(cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
            // name
            music.setAudioName(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            // songster
            music.setAudioArtistName(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            // path
            music.setAudioPath(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            // duration
            music.setAddTime(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
            // size
            music.setAudioSize(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)));
            // album id
            music.setAvatar(String.valueOf(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
            if (MusicUtils.getInstance().filterAudioType(music.getAudioPath())) {
                musicInfoList.add(music);
                LogUtil.error("queryAudioByUri####", "audio:" + music.toString());
            }
        }
        cursor.close();
        return musicInfoList;
    }

    private void handleQueryAudio(int msg) {
        List<AudioInfoBean> list = new ArrayList<>();
        Cursor cursor = sContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor == null) {
            return;
        }
        // id title singer data time image
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

            // If not music
            String isMusic = cursor
                    .getString(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) {
                continue;
            }
            AudioInfoBean music = new AudioInfoBean();
            // id
            music.setAudioId(cursor.getInt(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            // album
            music.setAudioAlbumName(cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
            // name
            music.setAudioName(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE)));
            // songster
            music.setAudioArtistName(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            // path
            music.setAudioPath(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA)));
            // duration
            music.setAddTime(cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION)));
            // size
            music.setAudioSize(cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.SIZE)));
            // album id
            music.setAvatar(String.valueOf(cursor.getInt(cursor.getColumnIndex(
                    MediaStore.Audio.Media.ALBUM_ID))));
            if (MusicUtils.getInstance().filterAudioType(music.getAudioPath())) {
                list.add(music);
                LogUtil.error("queryAudioByUri####", "audio:" + music.toString());
            }
        }
        cursor.close();
        if (msg == MusicConstants.QUERY_MUSIC_CLIENT) {
            if (mAudioQueryClientList != null) {
                mAudioQueryClientList = new ArrayList<>();
            } else {
                mAudioQueryClientList.clear();
            }
            mAudioQueryClientList.addAll(list);
        } else if (msg == MusicConstants.QUERY_MUSIC_LOCAL) {
            if (mAudioQueryLocalList != null) {
                mAudioQueryLocalList = new ArrayList<>();
            } else {
                mAudioQueryLocalList.clear();
            }
            mAudioQueryLocalList.addAll(list);
        }
    }

    /**
     * Media database query music files.
     */
    public List<AudioInfoBean> queryMusic(String singer, String song) {
        List<AudioInfoBean> musicInfoList = new ArrayList<>();

        if (singer == null) {
            singer = "";
        }
        if (song == null) {
            song = "";
        }
        LogUtil.debug("queryAudioBy Song and singer ","singer:" + singer + ", song" + song);
        String selection;
        String[] selectionArgs;
        selection = MediaStore.Audio.Media.TITLE + SEARCH_LIKE + " AND "
                    + MediaStore.Audio.Media.ARTIST + SEARCH_LIKE;
        selectionArgs = new String[]{PERCENT_SIGN + song
                    + PERCENT_SIGN,PERCENT_SIGN + singer
                    + PERCENT_SIGN };

        Cursor cursor = sContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor == null) {
            return null;
        }
        // id title singer data time image
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

            // If not music
            String isMusic = cursor
                    .getString(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) {
                continue;
            }
            AudioInfoBean music = new AudioInfoBean();
            // id
            music.setAudioId(cursor.getInt(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            // album
            music.setAudioAlbumName(cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
            // name
            music.setAudioName(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE)));
            // songster
            music.setAudioArtistName(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            // path
            music.setAudioPath(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA)));
            // duration
            music.setAddTime(cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION)));
            // size
            music.setAudioSize(cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.SIZE)));
            // album id
            music.setAvatar(String.valueOf(cursor.getInt(cursor.getColumnIndex(
                    MediaStore.Audio.Media.ALBUM_ID))));
            if (MusicUtils.getInstance().filterAudioType(music.getAudioPath())) {
                musicInfoList.add(music);
                LogUtil.debug("queryAudioBy Song and singer ","audio:" + music.toString());
            }
        }
        cursor.close();
        return musicInfoList;
    }

    /**
     * Media database query music files.
     */
    public void queryAudioByUri(Uri uri) {
        Cursor cursor = sContentResolver.query(
                uri,
                null,
                null,
                null,
                null);
        if (cursor == null) {
            return;
        }
        // id title singer data time image
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            // If not music
            String isMusic = cursor
                    .getString(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) {
                continue;
            }
            AudioInfoBean music = new AudioInfoBean();
            // id
            music.setAudioId(cursor.getInt(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            // album
            music.setAudioAlbumName(cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
            // name
            music.setAudioName(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE)));
            // songster
            music.setAudioArtistName(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            // path
            music.setAudioPath(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA)));
            // duration
            music.setAddTime(cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION)));
            // size
            music.setAudioSize(cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Media.SIZE)));
            // album id
            music.setAvatar(String.valueOf(cursor.getInt(cursor.getColumnIndex(
                    MediaStore.Audio.Media.ALBUM_ID))));
            if (MusicUtils.getInstance().filterAudioType(music.getAudioPath())) {
                addAudioScanResult(music);
                mAudioScanListener.addAudio(music);
                LogUtil.error("queryAudioByUri****", "audio:" + music.toString());
            }
        }
        cursor.close();
    }

    /**
     * Query specified file.
     *
     * @param uri Content uri
     */
    public void queryVideoByUri(Uri uri) {
        List<VideoInfoBean> videoInfoEntityList = new ArrayList<>();
        Cursor cursor = sContentResolver.query(
                uri,
                VIDEO_PROJECT,
                null,
                null,
                null);
        if (cursor == null) {
            return;
        }
        int idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
        int nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
        int typeIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
        int durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
        int dataIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);

        while (cursor.moveToNext()) {
            if (isInvalidVideo(cursor.getString(typeIndex), cursor.getString(nameIndex))) {
                continue;
            }
            VideoInfoBean videoInfoEntity = new VideoInfoBean();
            int id = cursor.getInt(idIndex);
            mHandler.obtainMessage(VIDEO_THUMBNAILS_MSG, id);
            videoInfoEntity.setDisplayName(cursor.getString(nameIndex));
            videoInfoEntity.setDuration(cursor.getInt(durationIndex));
            videoInfoEntity.setMimeType(cursor.getString(typeIndex));
            videoInfoEntity.setPath(cursor.getString(dataIndex));
            videoInfoEntityList.add(videoInfoEntity);
            mVideoScanListener.addVideo(videoInfoEntity);
            LogUtil.debug(TAG,
                    "Add video---fileName:" + videoInfoEntity.getDisplayName()
                            + "---type :" + videoInfoEntity.getMimeType()
                            + "--duration:" + videoInfoEntity.getDuration()
                            + "--path" + videoInfoEntity.getPath()
                            + "--size" + videoInfoEntityList.size());
        }
        cursor.close();
    }

    /**
     * Get video file.
     *
     * @return VideoInfo List
     */
    public List<VideoInfoBean> queryVideo(String key) {
        Cursor cursor;
        List<VideoInfoBean> videoInfoEntityList = new ArrayList<>();
        if (key == null || key.isEmpty()) {
            cursor = sContentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    VIDEO_PROJECT,
                    null,
                    null,
                    null);
        } else {
            String selection = MediaStore.Video.Media.DISPLAY_NAME + SEARCH_LIKE;
            String[] selectionArgs = new String[]{PERCENT_SIGN + key + PERCENT_SIGN};
            cursor = sContentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    VIDEO_PROJECT,
                    selection,
                    selectionArgs,
                    null);
        }
        if (cursor == null) {
            return null;
        }
        int idindex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
        int modifiedindex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
        int durationindex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
        int dataindex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
        int takenindex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
        int nameindex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
        int typeindex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
        while (cursor.moveToNext()) {
            if (isInvalidVideo(cursor.getString(typeindex), cursor.getString(nameindex))) {
                continue;
            }
            VideoInfoBean videoInfoEntity = new VideoInfoBean();
            Cursor thumbCursor = sContentResolver.query(
                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    THUMB_COLUMNS, MediaStore.Video.Thumbnails.VIDEO_ID
                            + "=" + cursor.getInt(idindex), null, null);
            if (thumbCursor.moveToFirst()) {
                videoInfoEntity.setVideoThumbnails(thumbCursor.getString(thumbCursor
                        .getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
            }
            thumbCursor.close();
            videoInfoEntity.setDocumentId(String.valueOf(cursor.getInt(idindex)));
            videoInfoEntity.setDisplayName(cursor.getString(nameindex));
            videoInfoEntity.setDuration(cursor.getInt(durationindex));
            videoInfoEntity.setMimeType(cursor.getString(typeindex));
            videoInfoEntity.setPath(cursor.getString(dataindex));
            videoInfoEntityList.add(videoInfoEntity);
            LogUtil.debug(TAG,
                    "Query Video---fileName:" + videoInfoEntity.getDisplayName()
                            + "---type :" + videoInfoEntity.getMimeType()
                            + "--duration:" + videoInfoEntity.getDuration()
                            + "--path" + videoInfoEntity.getPath());
        }
        cursor.close();
        if (key == null || key.isEmpty()) {
            mVideoInfoList = videoInfoEntityList;
        }
        return videoInfoEntityList;
    }

    private synchronized void handleQueryVideo() {
        List<VideoInfoBean> videoQueryList = new ArrayList<>();
        Cursor cursor = sContentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                VIDEO_PROJECT,
                null,
                null,
                null);
        if (cursor == null) {
            return;
        }
        int idindex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
        int modifiedindex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
        int durationindex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
        int dataindex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
        int takenindex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
        int nameindex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
        int typeindex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
        while (cursor.moveToNext()) {
            if (isInvalidVideo(cursor.getString(typeindex), cursor.getString(nameindex))) {
                continue;
            }
            VideoInfoBean videoInfoEntity = new VideoInfoBean();
            Cursor thumbCursor = sContentResolver.query(
                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    THUMB_COLUMNS, MediaStore.Video.Thumbnails.VIDEO_ID
                            + "=" + cursor.getInt(idindex), null, null);
            if (thumbCursor.moveToFirst()) {
                videoInfoEntity.setVideoThumbnails(thumbCursor.getString(thumbCursor
                        .getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
            }
            thumbCursor.close();
            videoInfoEntity.setDocumentId(String.valueOf(cursor.getInt(idindex)));
            videoInfoEntity.setDisplayName(cursor.getString(nameindex));
            videoInfoEntity.setDuration(cursor.getInt(durationindex));
            videoInfoEntity.setMimeType(cursor.getString(typeindex));
            videoInfoEntity.setPath(cursor.getString(dataindex));
            videoQueryList.add(videoInfoEntity);
            LogUtil.debug(TAG,
                    "Query All Video---fileName:" + videoInfoEntity.getDisplayName()
                            + "---type :" + videoInfoEntity.getMimeType()
                            + "--duration:" + videoInfoEntity.getDuration()
                            + "--path" + videoInfoEntity.getPath());
        }
        if (mVideoQueryList == null) {
            mVideoQueryList = new ArrayList<>();
        } else {
            mVideoQueryList.clear();
        }
        mVideoQueryList.addAll(videoQueryList);
        cursor.close();
    }

    private boolean isInvalidVideo(String mimeType, String name) {
        if (mimeType == null || name == null) {
            return true;
        }
        String type = name.substring(name.lastIndexOf(VideoConstants.TYPE_SEPARATOR) + 1);
        return Arrays.asList(VIDEO_FILTER_TYPE).contains(type)
                || !mimeType.contains(VideoConstants.VIDEO_TYPE_COMMON);
    }

    /**
     * Get video file.
     *
     * @param key Key work
     * @return VideoInfo List
     */
    public ArrayList<VideoInfoBean> getVideoInfo(String key) {
        return (ArrayList<VideoInfoBean>) queryVideo(key);
    }

    public void clearUsbList() {
        List<AudioInfoBean> audioInfoBeans = new ArrayList<AudioInfoBean>();
        mAudioScanListener.queryComplete(audioInfoBeans);
    }

    public void setVideoScanListener(IVideoScanListener listener) {
        mVideoScanListener = listener;
    }

    public interface IVideoScanListener {
        void addVideo(VideoInfoBean videoInfoBean);

        void queryComplete(List<VideoInfoBean> videoList);
    }

    public void setAudioScanListener(IAudioScanListener listener) {
        mAudioScanListener = listener;
    }

    public void setAudioQueryListener(IAudioQueryListener listener) {
        mAudioQueryListener = listener;
    }

    private void addAudioScanResult(AudioInfoBean audioInfoBean) {
        if (mAudioScanningList.size() == 0) {
            mHandler.sendEmptyMessageDelayed(MusicConstants.AUDIO_SCAN_UPDATE,
                    SCAN_LIST_UPDATE_TIME);
        }
        mAudioScanningList.add(audioInfoBean);
        if (mAudioScanningList.size() == SCAN_LIST_MAX) {
            updateScanListToClient();
        }
    }

    private void updateScanListToClient() {
        mHandler.removeMessages(MusicConstants.AUDIO_SCAN_UPDATE);
        mAudioScanListener.updateScanList(mAudioScanningList);
        mAudioScanningList.clear();
        mHandler.sendEmptyMessageDelayed(MusicConstants.AUDIO_SCAN_UPDATE, SCAN_LIST_UPDATE_TIME);
    }

    public void scanCompleted() {
        mHandler.removeMessages(MusicConstants.AUDIO_SCAN_UPDATE);
    }
    public interface IAudioScanListener {
        void addAudio(AudioInfoBean videoInfoBean);

        void queryComplete(List<AudioInfoBean> musicList);

        void updateScanList(List<AudioInfoBean> scanList);
    }

    public interface IAudioQueryListener {
        void queryComplete(List<AudioInfoBean> musicList);
    }
}
