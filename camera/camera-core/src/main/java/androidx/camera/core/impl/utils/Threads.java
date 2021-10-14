/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * Helpers for {@link Thread}s.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Threads {

    // Prevent instantiation.
    private Threads() {
    }

    /** Returns true if we're currently running in the application's main thread. */
    public static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    /** Returns true if we're currently running on a background thread. */
    public static boolean isBackgroundThread() {
        return !isMainThread();
    }

    /**
     * Ensures that we're currently running in the application's main thread.
     *
     * @throws IllegalStateException If the caller is not running on the main thread,
     */
    public static void checkMainThread() {
        Preconditions.checkState(isMainThread(), "Not in application's main thread");
    }

    /**
     * Ensures that we're currently not running in the application's main thread.
     *
     * @throws IllegalStateException if the caller is running on the main thread.
     */
    public static void checkBackgroundThread() {
        Preconditions.checkState(isBackgroundThread(), "In application's main thread");
    }
}

