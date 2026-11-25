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

import androidx.annotation.GuardedBy;
import androidx.camera.core.CameraXThreads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors.HandlerLease;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A singleton high priority {@link Executor} suitable for lightweight, non-blocking tasks.
 *
 * <p>The executor is backed by an on-demand {@link HandlerThread} whose {@link Handler} can be
 * leased via {@link CameraXExecutors#acquireHandlerLease(Executor)}. This allows framework
 * callbacks which only accept a {@code Handler} (e.g. {@link
 * android.media.ImageReader.OnImageAvailableListener}) to be delivered directly on the high
 * priority thread instead of hopping through the main thread.
 *
 * <p>The backing {@link HandlerThread} stays alive while at least one {@link HandlerLease} is open
 * or an {@link #execute(Runnable)} task is in flight, and exits via {@link
 * HandlerThread#quitSafely()} when the active reference count returns to zero so unbound camera
 * sessions and unit tests do not retain an idle background thread. Work submitted after the
 * reference count reaches zero starts a new backing thread; repeated standalone {@link
 * #execute(Runnable)} calls made without an open {@link HandlerLease} that drain completely between
 * submissions will therefore start and quit a backing thread per drained burst.
 */
final class HighPriorityExecutor implements Executor {
    private static final String THREAD_NAME = CameraXThreads.TAG + "camerax_high_priority";

    private static final HighPriorityExecutor INSTANCE = new HighPriorityExecutor();

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private @Nullable HandlerThread mThread;

    @GuardedBy("mLock")
    private @Nullable Handler mHandler;

    @GuardedBy("mLock")
    private int mRefCount = 0;

    /** Package-private for testing. Does not start any thread. */
    HighPriorityExecutor() {}

    static @NonNull Executor getInstance() {
        return INSTANCE;
    }

    /**
     * Acquires a reference-counted {@link HandlerLease} for the backing thread's {@link Handler},
     * starting the thread if needed.
     */
    @NonNull HandlerLease acquireHandlerLease() {
        final Handler handler = retainHandler();
        return new HandlerLease() {
            private final AtomicBoolean mClosed = new AtomicBoolean(false);

            @Override
            public @NonNull Handler getHandler() {
                return handler;
            }

            @Override
            public void close() {
                if (mClosed.compareAndSet(false, true)) {
                    releaseHandler();
                }
            }
        };
    }

    private @NonNull Handler retainHandler() {
        synchronized (mLock) {
            if (mHandler == null) {
                // THREAD_PRIORITY_URGENT_DISPLAY (-8) is the niceness ART maps
                // Thread.MAX_PRIORITY to, which the previous thread-pool implementation used.
                HandlerThread thread =
                        new HandlerThread(THREAD_NAME, Process.THREAD_PRIORITY_URGENT_DISPLAY);
                thread.start();
                mHandler = new Handler(Preconditions.checkNotNull(thread.getLooper()));
                mThread = thread;
            }
            mRefCount++;
            return mHandler;
        }
    }

    private void releaseHandler() {
        HandlerThread threadToQuit = null;
        synchronized (mLock) {
            Preconditions.checkState(mRefCount > 0, "Ref count is already 0");
            mRefCount--;
            if (mRefCount == 0) {
                threadToQuit = mThread;
                mThread = null;
                mHandler = null;
            }
        }
        if (threadToQuit != null) {
            threadToQuit.quitSafely();
        }
    }

    @Override
    public void execute(@NonNull Runnable command) {
        // Handler#post(null) would silently post an empty message, so enforce the Executor
        // contract.
        Preconditions.checkNotNull(command);
        Handler handler = retainHandler();
        boolean posted = false;
        try {
            posted =
                    handler.post(
                            () -> {
                                try {
                                    command.run();
                                } finally {
                                    releaseHandler();
                                }
                            });
            if (!posted) {
                throw new RejectedExecutionException(THREAD_NAME + " is shutting down");
            }
        } finally {
            if (!posted) {
                releaseHandler();
            }
        }
    }
}
