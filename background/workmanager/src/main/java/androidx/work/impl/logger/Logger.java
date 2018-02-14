/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work.impl.logger;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * Helps with logging among other things.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Logger {

    /** The default log level. */
    private static final int DEFAULT_LOG_LEVEL = Log.WARN;

    /** The log level being currently used by the logger. */
    public static int LOG_LEVEL = DEFAULT_LOG_LEVEL;

    /**
     * @return if the logLevel is loggable.
     */
    public static boolean isLoggable(int logLevel) {
        return logLevel >= LOG_LEVEL;
    }

    /**
     * Logs a message with the log level set to Log.VERBOSE
     */
    public static void verbose(String tag, String message, @Nullable Object... args) {
        logInternal(Log.VERBOSE, tag, message, null, args);
    }

    /**
     * Logs a message with the log level set to Log.VERBOSE
     */
    public static void verbose(String tag, String message, Throwable throwable,
            @Nullable Object... args) {
        logInternal(Log.VERBOSE, tag, message, throwable, args);
    }

    /**
     * Logs a message with the log level set to Log.DEBUG
     */
    public static void debug(String tag, String message, @Nullable Object... args) {
        logInternal(Log.DEBUG, tag, message, null, args);
    }

    /**
     * Logs a message with the log level set to Log.DEBUG
     */
    public static void debug(String tag, String message, Throwable throwable,
            @Nullable Object... args) {
        logInternal(Log.DEBUG, tag, message, throwable, args);
    }

    /**
     * Logs a message with the log level set to Log.INFO
     */
    public static void info(String tag, String message, @Nullable Object... args) {
        logInternal(Log.INFO, tag, message, null, args);
    }

    /**
     * Logs a message with the log level set to Log.INFO
     */
    public static void info(String tag, String message, Throwable throwable,
            @Nullable Object... args) {
        logInternal(Log.INFO, tag, message, throwable, args);
    }

    /**
     * Logs a message with the log level set to Log.WARN
     */
    public static void warn(String tag, String message, @Nullable Object... args) {
        logInternal(Log.WARN, tag, message, null, args);
    }

    /**
     * Logs a message with the log level set to Log.WARN
     */
    public static void warn(String tag, String message, Throwable throwable,
            @Nullable Object... args) {
        logInternal(Log.WARN, tag, message, throwable, args);
    }

    /**
     * Logs a message with the log level set to Log.ERROR
     */
    public static void error(String tag, String message, @Nullable Object... args) {
        logInternal(Log.ERROR, tag, message, null, args);
    }

    /**
     * Logs a message with the log level set to Log.ERROR
     */
    public static void error(String tag, String message, Throwable throwable,
            @Nullable Object... args) {
        logInternal(Log.ERROR, tag, message, throwable, args);
    }

    /**
     * Creates an instance of the {@link Logger}.
     */
    private Logger() {

    }

    /**
     * Internally routes the log requests to the appropriate logger.
     */
    private static void logInternal(
            int logLevel,
            String tag,
            String message,
            @Nullable Throwable throwable,
            @Nullable Object... args) {

        if (isLoggable(logLevel)) {
            switch (logLevel) {
                case Log.VERBOSE: {
                    if (throwable != null) {
                        Log.v(tag, format(message, args), throwable);
                    } else {
                        Log.v(tag, format(message, args));
                    }
                    break;
                }
                case Log.DEBUG: {
                    if (throwable != null) {
                        Log.d(tag, format(message, args), throwable);
                    } else {
                        Log.d(tag, format(message, args));
                    }
                    break;
                }
                case Log.INFO: {
                    if (throwable != null) {
                        Log.i(tag, format(message, args), throwable);
                    } else {
                        Log.i(tag, format(message, args));
                    }
                    break;
                }
                case Log.WARN: {
                    if (throwable != null) {
                        Log.w(tag, format(message, args), throwable);
                    } else {
                        Log.w(tag, format(message, args));
                    }
                    break;
                }
                case Log.ERROR: {
                    if (throwable != null) {
                        Log.e(tag, format(message, args), throwable);
                    } else {
                        Log.e(tag, format(message, args));
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static String format(String message, @Nullable Object... arguments) {
        // checking for arguments to avoid creating instance of Formatter which will
        // be used by String.format(...)
        if (arguments != null && arguments.length > 0) {
            return String.format(message, arguments);
        } else {
            return message;
        }
    }
}
