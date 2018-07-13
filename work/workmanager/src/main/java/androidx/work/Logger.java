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

import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * A class that handles logging to logcat.  It internally delegates to {@link Log} methods but
 * handles library-level verbosity settings.  This class offers no threading guarantees.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Logger {

    private static int sLoggingLevel = Log.INFO;

    /**
     * @param loggingLevel The minimum logging level
     */
    public static void setMinimumLoggingLevel(int loggingLevel) {
        sLoggingLevel = loggingLevel;
    }

    /**
     * Equivalent to Log.v.
     */
    public static void verbose(String tag, String message, Throwable... throwables)  {
        if (sLoggingLevel <= Log.VERBOSE) {
            if (throwables != null && throwables.length >= 1) {
                Log.v(tag, message, throwables[0]);
            } else {
                Log.v(tag, message);
            }
        }
    }

    /**
     * Equivalent to Log.d.
     */
    public static void debug(String tag, String message, Throwable... throwables)  {
        if (sLoggingLevel <= Log.DEBUG) {
            if (throwables != null && throwables.length >= 1) {
                Log.d(tag, message, throwables[0]);
            } else {
                Log.d(tag, message);
            }
        }
    }

    /**
     * Equivalent to Log.i.
     */
    public static void info(String tag, String message, Throwable... throwables)  {
        if (sLoggingLevel <= Log.INFO) {
            if (throwables != null && throwables.length >= 1) {
                Log.i(tag, message, throwables[0]);
            } else {
                Log.i(tag, message);
            }
        }
    }

    /**
     * Equivalent to Log.w.
     */
    public static void warning(String tag, String message, Throwable... throwables)  {
        if (sLoggingLevel <= Log.WARN) {
            if (throwables != null && throwables.length >= 1) {
                Log.w(tag, message, throwables[0]);
            } else {
                Log.w(tag, message);
            }
        }
    }

    /**
     * Equivalent to Log.e.
     */
    public static void error(String tag, String message, Throwable... throwables)  {
        if (sLoggingLevel <= Log.ERROR) {
            if (throwables != null && throwables.length >= 1) {
                Log.e(tag, message, throwables[0]);
            } else {
                Log.e(tag, message);
            }
        }
    }

    private Logger() {
    }
}
