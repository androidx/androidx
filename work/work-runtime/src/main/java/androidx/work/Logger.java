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

package androidx.work;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * The class that handles logging requests for {@link WorkManager}.  Currently, this class is not
 * accessible and has only one default implementation, {@link LogcatLogger}, that writes to logcat
 * when the logging request is of a certain verbosity or higher.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Logger {

    private static Logger sLogger;

    // tagging
    private static final String TAG_PREFIX = "WM-";
    private static final int MAX_TAG_LENGTH = 23;
    private static final int MAX_PREFIXED_TAG_LENGTH = MAX_TAG_LENGTH - TAG_PREFIX.length();

    /**
     * @param logger The {@link Logger} to use for all {@link WorkManager} logging.
     */
    public static synchronized void setLogger(Logger logger) {
        sLogger = logger;
    }

    /**
     * @param tag The {@link String} tag to use when logging
     * @return The prefixed {@link String} tag to use when logging
     */
    public static String tagWithPrefix(@NonNull String tag) {
        int length = tag.length();
        StringBuilder withPrefix = new StringBuilder(MAX_TAG_LENGTH);
        withPrefix.append(TAG_PREFIX);
        if (length >= MAX_PREFIXED_TAG_LENGTH) {
            // truncate
            withPrefix.append(tag.substring(0, MAX_PREFIXED_TAG_LENGTH));
        } else {
            withPrefix.append(tag);
        }
        return withPrefix.toString();
    }

    /**
     * @return The current {@link Logger}.
     */
    public static synchronized Logger get() {
        // Logger may not be explicitly initialized by some tests which do not instantiate
        // WorkManagerImpl directly.
        //
        // This is not being initialized on the field directly to avoid a
        // class loading deadlock; when the parent class, Logger tries to reference an inner
        // class, LogcatLogger and there might be another Thread trying to do the same.
        if (sLogger == null) {
            sLogger = new LogcatLogger(Log.DEBUG);
        }
        return sLogger;
    }

    public Logger(int loggingLevel) {
    }

    /**
     * Equivalent to Log.v.
     */
    public abstract void verbose(@NonNull String tag, @NonNull String message);

    /**
     * Equivalent to Log.v.
     */
    public abstract void verbose(@NonNull String tag, @NonNull String message,
            @NonNull Throwable throwable);

    /**
     * Equivalent to Log.d.
     */
    public abstract void debug(@NonNull String tag, @NonNull String message);

    /**
     * Equivalent to Log.d.
     */
    public abstract void debug(@NonNull String tag, @NonNull String message,
            @NonNull Throwable throwable);

    /**
     * Equivalent to Log.i.
     */
    public abstract void info(@NonNull String tag, @NonNull String message);

    /**
     * Equivalent to Log.i.
     */
    public abstract void info(@NonNull String tag, @NonNull String message,
            @NonNull Throwable throwable);

    /**
     * Equivalent to Log.w.
     */
    public abstract void warning(@NonNull String tag, @NonNull String message);

    /**
     * Equivalent to Log.w.
     */
    public abstract void warning(@NonNull String tag, @NonNull String message,
            @NonNull Throwable throwable);

    /**
     * Equivalent to Log.e.
     */
    public abstract void error(@NonNull String tag, @NonNull String message);

    /**
     * Equivalent to Log.e.
     */
    public abstract void error(@NonNull String tag, @NonNull String message,
            @NonNull Throwable throwable);

    /**
     * The default {@link Logger} implementation that writes to logcat when the requests meet or
     * exceed the {@code loggingLevel} specified in the constructor.  This class offers no threading
     * guarantees.
     */
    public static class LogcatLogger extends Logger {

        private int mLoggingLevel;

        public LogcatLogger(int loggingLevel) {
            super(loggingLevel);
            mLoggingLevel = loggingLevel;
        }

        @Override
        public void verbose(@NonNull String tag, @NonNull String message) {
            if (mLoggingLevel <= Log.VERBOSE) {
                Log.v(tag, message);
            }
        }

        @Override
        public void verbose(@NonNull String tag, @NonNull String message,
                @NonNull Throwable throwable) {
            if (mLoggingLevel <= Log.VERBOSE) {
                Log.v(tag, message, throwable);
            }
        }

        @Override
        public void debug(@NonNull String tag, @NonNull String message) {
            if (mLoggingLevel <= Log.DEBUG) {
                Log.d(tag, message);
            }
        }

        @Override
        public void debug(@NonNull String tag, @NonNull String message,
                @NonNull Throwable throwable) {
            if (mLoggingLevel <= Log.DEBUG) {
                Log.d(tag, message, throwable);
            }
        }

        @Override
        public void info(@NonNull String tag, @NonNull String message) {
            if (mLoggingLevel <= Log.INFO) {
                Log.i(tag, message);
            }
        }

        @Override
        public void info(@NonNull String tag, @NonNull String message,
                @NonNull Throwable throwable) {
            if (mLoggingLevel <= Log.INFO) {
                Log.i(tag, message, throwable);
            }
        }

        @Override
        public void warning(@NonNull String tag, @NonNull String message) {
            if (mLoggingLevel <= Log.WARN) {
                Log.w(tag, message);
            }
        }

        @Override
        public void warning(@NonNull String tag, @NonNull String message,
                @NonNull Throwable throwable) {
            if (mLoggingLevel <= Log.WARN) {
                Log.w(tag, message, throwable);
            }
        }

        @Override
        public void error(@NonNull String tag, @NonNull String message) {
            if (mLoggingLevel <= Log.ERROR) {
                Log.e(tag, message);
            }
        }

        @Override
        public void error(@NonNull String tag, @NonNull String message,
                @NonNull Throwable throwable) {
            if (mLoggingLevel <= Log.ERROR) {
                Log.e(tag, message, throwable);
            }
        }
    }
}
