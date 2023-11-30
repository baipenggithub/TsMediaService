package com.ts.service.media.utils;

import android.util.Log;

public class LogUtil {
    private static final String NULL = "null";
    private static final boolean DEBUG;
    private static final String BUILD_TYPE = "ro.build.type";
    private static final String DEFAULT_BUILD_TYPE = "userdebug";
    private static final String USER_TYPE = "user";

    static {
        // DEBUG = !TextUtils.equals(USER_TYPE, SystemProperties
        // .get(BUILD_TYPE, DEFAULT_BUILD_TYPE));
        DEBUG = true;
    }

    /**
     * Log.i .
     *
     * @param objTag tag
     * @param objMsg msg
     */
    public static void info(Object objTag, Object objMsg) {
        if (DEBUG) {
            String tag = getTag(objTag);
            String msg = (objMsg == null || objMsg.toString() == null) ? NULL : objMsg.toString();
            Log.i(tag, msg);
        }
    }

    /**
     * Log.d .
     *
     * @param objTag tag
     * @param objMsg msg
     */
    public static void debug(Object objTag, Object objMsg) {
        if (DEBUG) {
            String tag = getTag(objTag);
            String msg = (objMsg == null || objMsg.toString() == null) ? NULL : objMsg.toString();
            Log.d(tag, msg);
        }
    }

    /**
     * Log.w .
     *
     * @param objTag tag
     * @param objMsg msg
     */
    public static void warning(Object objTag, Object objMsg) {
        if (DEBUG) {
            String tag = getTag(objTag);
            String msg = (objMsg == null || objMsg.toString() == null) ? NULL : objMsg.toString();
            Log.w(tag, msg);
        }
    }

    /**
     * Log.d .
     *
     * @param objTag tag
     * @param objMsg msg
     */
    public static void error(Object objTag, Object objMsg) {
        if (DEBUG) {
            String tag = getTag(objTag);
            String msg = (objMsg == null || objMsg.toString() == null) ? NULL : objMsg.toString();
            Log.e(tag, msg);
        }
    }

    /**
     * Get tag.
     *
     * @param objTag tag
     * @return value
     */
    private static String getTag(Object objTag) {
        String tag;
        if (objTag instanceof String) {
            tag = (String) objTag;
        } else if (objTag instanceof Class) {
            tag = ((Class<?>) objTag).getSimpleName();
        } else {
            tag = objTag.getClass().getSimpleName();
        }
        return tag;
    }
}
