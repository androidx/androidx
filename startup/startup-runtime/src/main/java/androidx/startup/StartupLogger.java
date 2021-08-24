/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.startup;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("WeakerAccess")
public final class StartupLogger {

    private StartupLogger() {
        // Does nothing.
    }

    /**
     * The log tag.
     */
    private static final String TAG = "StartupLogger";

    /**
     * To enable logging set this to true.
     */
    static final boolean DEBUG = false;

    /**
     * Info level logging.
     *
     * @param message The message being logged
     */
    public static void i(@NonNull String message) {
        Log.i(TAG, message);
    }

    /**
     * Warning level logging.
     *
     * @param message The message being logged
     */
    public static void w(@NonNull String message) {
        Log.w(TAG, message);
    }

    /**
     * Error level logging
     *
     * @param message   The message being logged
     * @param throwable The optional {@link Throwable} exception
     */
    public static void e(@NonNull String message, @Nullable Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
}
