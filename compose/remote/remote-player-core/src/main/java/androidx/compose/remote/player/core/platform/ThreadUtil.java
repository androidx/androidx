/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/** Thread utility methods. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

public class ThreadUtil {
    private ThreadUtil() {}

    private static final Thread sMainThread = Looper.getMainLooper().getThread();
    private static final Handler sMainThreadHandler = new Handler(Looper.getMainLooper());

    /** Returns true if the current thread is the main thread. */
    public static boolean isMainThread() {
        return Thread.currentThread().equals(sMainThread);
    }

    /** Checks that the current thread is the main thread. Otherwise throws an exception. */
    public static void ensureMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("Must be called on the main thread");
        }
    }

    /**
     * Executes the runnable directly if already on main thread, otherwise, post it on the main
     * thread.
     */
    public static void runOnMainThread(@NonNull Runnable r) {
        if (isMainThread()) {
            r.run();
        } else {
            sMainThreadHandler.post(r);
        }
    }
}
