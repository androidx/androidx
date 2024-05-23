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

import static androidx.core.util.Preconditions.checkState;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Helpers for {@link Thread}s.
 */
public final class Threads {
    private static final long TIMEOUT_RUN_ON_MAIN_MS = 30_000L; // milliseconds

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
        checkState(isMainThread(), "Not in application's main thread");
    }

    /**
     * Ensures that we're currently not running in the application's main thread.
     *
     * @throws IllegalStateException if the caller is running on the main thread.
     */
    public static void checkBackgroundThread() {
        checkState(isBackgroundThread(), "In application's main thread");
    }
    /**
     * Executes the {@link Runnable} on main thread.
     *
     * <p>If the caller thread is already main thread, then runnable will be executed immediately.
     * Otherwise, the runnable will be posted to main thread.
     */
    public static void runOnMain(@NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
            return;
        }
        checkState(getMainHandler().post(runnable), "Unable to post to main thread");
    }

    /**
     * Executes the {@link Runnable} on main thread and block until the Runnable is complete.
     *
     * <p>If the caller thread is already main thread, then runnable will be executed immediately.
     * Otherwise, the runnable will be posted to main thread and caller thread will be blocked until
     * the runnable is complete.
     *
     * <p> A 30 second timeout is basically to prevent unit tests from waiting infinitely if
     * there is any error. Normal flow should not expect this timeout. Basically main
     * thread should not be occupied for too long or an ANR could occur.
     *
     * @param runnable the runnable to execute.
     *
     * @throws IllegalStateException if timed out waiting for the posted runnable to complete.
     * @throws InterruptedRuntimeException if the waiting is interrupted.
     */
    public static void runOnMainSync(@NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
            return;
        }
        // Post to main thread and wait for the completion.
        CountDownLatch latch = new CountDownLatch(1);
        boolean postResult = getMainHandler().post(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        checkState(postResult, "Unable to post to main thread");
        try {
            if (!latch.await(TIMEOUT_RUN_ON_MAIN_MS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timeout to wait main thread execution");
            }
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
    }

    @NonNull
    private static Handler getMainHandler() {
        return new Handler(Looper.getMainLooper());
    }
}

