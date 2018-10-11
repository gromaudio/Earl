package com.einmalfel.earl;

final class Logger {

    private static boolean logEnabled = false;

    public static void w(String tag, String message) {
        if (logEnabled)
            Logger.w(tag, message);
    }

    public static void w(String tag, String message, Throwable throwable) {
        if (logEnabled)
            Logger.w(tag, message, throwable);
    }

    static void setLogging(boolean enabled) {
        logEnabled = enabled;
    }
}
