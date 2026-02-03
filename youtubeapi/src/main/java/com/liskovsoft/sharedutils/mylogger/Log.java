package com.liskovsoft.sharedutils.mylogger;

import android.util.Log as AndroidLog;

public class Log {
    private static final String TAG_PREFIX = "YTApi";
    
    public static void d(String tag, String msg, Object... args) {
        AndroidLog.d(TAG_PREFIX + ":" + tag, String.format(msg, args));
    }
    
    public static void i(String tag, String msg, Object... args) {
        AndroidLog.i(TAG_PREFIX + ":" + tag, String.format(msg, args));
    }
    
    public static void w(String tag, String msg, Object... args) {
        AndroidLog.w(TAG_PREFIX + ":" + tag, String.format(msg, args));
    }
    
    public static void e(String tag, String msg, Object... args) {
        AndroidLog.e(TAG_PREFIX + ":" + tag, String.format(msg, args));
    }
    
    public static void e(String tag, String msg, Throwable throwable) {
        AndroidLog.e(TAG_PREFIX + ":" + tag, msg, throwable);
    }
}
