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

package androidx.camera.core.impl.utils.executor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import androidx.camera.core.CameraXThreads;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * A singleton high priority {@link Executor} suitable for lightweight, non-blocking tasks.
 *
 * <p>The executor is backed by a lazily started {@link HandlerThread} so that it can be unwrapped
 * to its {@link Handler} via {@link CameraXExecutors#unwrapHandler(Executor)}. This allows
 * framework callbacks which only accept a {@code Handler} (e.g. {@link
 * android.media.ImageReader.OnImageAvailableListener}) to be delivered directly on the high
 * priority thread instead of hopping through the main thread.
 */
final class HighPriorityExecutor implements Executor {
    private static final String THREAD_NAME = CameraXThreads.TAG + "camerax_high_priority";

    private static final HighPriorityExecutor INSTANCE = new HighPriorityExecutor();

    private final Object mLock = new Object();

    // Written only under mLock. Volatile so getOrCreateHandler() can read it without locking once
    // the thread has been started.
    private volatile @Nullable Handler mHandler;

    /** Package-private for testing. Does not start any thread. */
    HighPriorityExecutor() {}

    static @NonNull Executor getInstance() {
        return INSTANCE;
    }

    /** Returns the {@link Handler} of the backing thread, starting the thread if needed. */
    @NonNull Handler getOrCreateHandler() {
        Handler handler = mHandler;
        if (handler != null) {
            return handler;
        }
        synchronized (mLock) {
            handler = mHandler;
            if (handler == null) {
                // THREAD_PRIORITY_URGENT_DISPLAY (-8) is the niceness ART maps
                // Thread.MAX_PRIORITY to, which the previous thread-pool implementation used.
                HandlerThread thread =
                        new HandlerThread(THREAD_NAME, Process.THREAD_PRIORITY_URGENT_DISPLAY);
                thread.start();
                handler = new Handler(thread.getLooper());
                mHandler = handler;
            }
            return handler;
        }
    }

    @Override
    public void execute(@NonNull Runnable command) {
        // Handler#post(null) would silently post an empty message, so enforce the Executor
        // contract.
        Preconditions.checkNotNull(command);
        if (!getOrCreateHandler().post(command)) {
            throw new RejectedExecutionException(THREAD_NAME + " is shutting down");
        }
    }
}
