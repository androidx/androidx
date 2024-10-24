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

package androidx.car.app.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Utility functions to handle running functions on the main thread.
 *
 */
@RestrictTo(LIBRARY_GROUP)
public final class ThreadUtils {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    /** Executes the {@code action} on the main thread. */
    public static void runOnMain(@NonNull Runnable action) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            action.run();
        } else {
            HANDLER.post(action);
        }
    }

    /**
     * Checks that currently running on the main thread.
     *
     * @throws IllegalStateException if the current thread is not the main thread
     */
    public static void checkMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("Not running on main thread when it is required to");
        }
    }

    private ThreadUtils() {
    }
}
