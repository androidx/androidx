/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Utility class for temporarily disabling StrictMode to do I/O on UI threads. */
// TODO: Cleanup this class as I/O ops should not be done on UI thread
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StrictModeUtils {

    private StrictModeUtils() {
    }

    /** Temporarily disable StrictMode, execute a code block and return the result. */
    public static <T> T bypassAndReturn(@NonNull CallbackWithReturnValue<T> callback) {
        ThreadPolicy policy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(
                    new ThreadPolicy.Builder().permitDiskReads().permitDiskWrites().build());
            return callback.run();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    /** Temporarily disable StrictMode, execute a code block. */
    public static void bypass(@NonNull CallbackWithoutReturnValue callback) {
        bypassAndReturn((CallbackWithReturnValue<?>) () -> {
            callback.run();
            return null;
        });
    }

    /**
     * Callback to be executed with return value.
     *
     * @param <T> the type of the return value
     */
    public interface CallbackWithReturnValue<T> {
        /** Method to execute the callback. */
        T run();
    }

    /**
     * Callback to be executed without return values.
     */
    public interface CallbackWithoutReturnValue {
        /** Method to execute the callback. */
        void run();
    }
}
