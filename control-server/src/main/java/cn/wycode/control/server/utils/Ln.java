package cn.wycode.control.server.utils;

import android.util.Log;

/**
 * Log both to Android logger (so that logs are visible in "adb logcat") and standard output/error (so that they are visible in the terminal
 * directly).
 */
public final class Ln {

    private static final String TAG = "[wycs]";


    private Ln() {
        // not instantiable
    }

    public static void d(String message) {
        Log.d(TAG, message);
        System.out.println(TAG + "DEBUG: " + message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
        System.out.println(TAG + "INFO: " + message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
        System.out.println(TAG + "WARN: " + message);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        System.out.println(TAG + "ERROR: " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    public static void e(String message) {
        e(message, null);
    }
}
