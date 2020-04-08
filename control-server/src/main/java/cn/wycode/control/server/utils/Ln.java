package cn.wycode.control.server.utils;

import android.util.Log;

import static cn.wycode.control.common.ConstantsKt.LOG_TAG;

/**
 * Log both to Android logger (so that logs are visible in "adb logcat") and standard output/error (so that they are visible in the terminal
 * directly).
 */
public final class Ln {

    private Ln() {
        // not instantiable
    }

    public static void d(String message) {
        Log.d(LOG_TAG, message);
        System.out.println(LOG_TAG + "DEBUG: " + message);
    }

    public static void i(String message) {
        Log.i(LOG_TAG, message);
        System.out.println(LOG_TAG + "INFO: " + message);
    }

    public static void w(String message) {
        Log.w(LOG_TAG, message);
        System.out.println(LOG_TAG + "WARN: " + message);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(LOG_TAG, message, throwable);
        System.out.println(LOG_TAG + "ERROR: " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    public static void e(String message) {
        e(message, null);
    }
}
