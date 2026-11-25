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
import android.os.Looper;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Utility class for generating specific implementations of {@link Executor}.
 */
public final class CameraXExecutors {

    // Should not be instantiated
    private CameraXExecutors() {
    }

    /** Returns a cached {@link ScheduledExecutorService} which posts to the main thread. */
    public static @NonNull ScheduledExecutorService mainThreadExecutor() {
        return MainThreadExecutor.getInstance();
    }

    /** Returns a cached {@link Executor} suitable for disk I/O. */
    public static @NonNull Executor ioExecutor() {
        return IoExecutor.getInstance();
    }

    /** Returns a cached {@link Executor} suitable for audio I/O. */
    public static @NonNull Executor audioExecutor() {
        return AudioExecutor.getInstance();
    }

    /** Returns a cached executor that runs tasks directly from the calling thread. */
    public static @NonNull Executor directExecutor() {
        return DirectExecutor.getInstance();
    }

    /**
     * Returns a new executor which will perform all tasks sequentially.
     *
     * <p>The returned executor delegates all tasks to the provided delegate Executor, but will
     * ensure all tasks are run in order and without overlapping. Note this can only be
     * guaranteed for tasks that are submitted via the same sequential executor. Tasks submitted
     * directly to the delegate or to different instances of the sequential executor do not have
     * any ordering guarantees.
     */
    public static @NonNull Executor newSequentialExecutor(@NonNull Executor delegate) {
        return new SequentialExecutor(delegate);
    }

    /**
     * Returns whether the executor is a sequential executor as returned by
     * {@link #newSequentialExecutor(Executor)}.
     */
    public static boolean isSequentialExecutor(@NonNull Executor executor) {
        return executor instanceof SequentialExecutor;
    }

    /**
     * Returns an executor which posts to the thread's current {@link Looper}.
     *
     * @return An executor which posts to the thread's current looper.
     * @throws IllegalStateException if the current thread does not have a looper.
     */
    public static @NonNull ScheduledExecutorService myLooperExecutor() {
        return HandlerScheduledExecutorService.currentThreadExecutor();
    }

    /**
     * Returns an executor which posts to the given {@link Handler}.
     *
     * @return An executor which posts to the given handler.
     */
    public static @NonNull ScheduledExecutorService newHandlerExecutor(@NonNull Handler handler) {
        return new HandlerScheduledExecutorService(handler);
    }

    /**
     * Returns {@code executor} as a {@link ScheduledExecutorService}.
     *
     * <p>If {@code executor} already is a {@link ScheduledExecutorService}, it is returned as-is so
     * that its own scheduling is kept. Otherwise, it is wrapped in a stateless service that tracks
     * delayed tasks on {@code schedulerHandler} and always executes tasks on {@code executor}.
     */
    public static @NonNull ScheduledExecutorService asScheduledExecutor(
            @NonNull Executor executor, @NonNull Handler schedulerHandler) {
        if (executor instanceof ScheduledExecutorService) {
            return (ScheduledExecutorService) executor;
        }
        return new DelegatingScheduledExecutorService(executor, schedulerHandler);
    }

    /**
     * @return a cached high priority {@link Executor} suitable for lightweight tasks.
     */
    public static @NonNull Executor highPriorityExecutor() {
        return HighPriorityExecutor.getInstance();
    }

    /**
     * A lease for an {@link Executor}'s underlying {@link Handler}.
     *
     * <p>Holding an open lease keeps any on-demand backing thread (such as {@link
     * #highPriorityExecutor()}'s {@link android.os.HandlerThread}) alive so framework callbacks can
     * be delivered directly on its {@link Looper}. Calling {@link #close()} releases this lease; it
     * is idempotent and never quits a caller-owned or main {@link Looper}. Callers must never quit
     * the returned {@link Handler}'s {@link Looper} directly.
     */
    public interface HandlerLease extends Closeable {
        /** Returns the leased {@link Handler}. */
        @NonNull Handler getHandler();

        /** Releases this lease. Calling this method more than once has no effect. */
        @Override
        void close();
    }

    /**
     * Acquires a {@link HandlerLease} for the {@link Handler} that {@code executor} posts its tasks
     * to, or returns {@code null} if {@code executor} is not directly backed by a {@link Handler}.
     *
     * <p>Executors returned by {@link #mainThreadExecutor()}, {@link #myLooperExecutor()},
     * {@link #newHandlerExecutor(Handler)} and {@link #highPriorityExecutor()} are backed by a
     * {@link Handler}. Wrapping executors, such as {@link #newSequentialExecutor(Executor)}, are
     * not unwrapped.
     *
     * <p>Callers must {@link HandlerLease#close() close} the returned lease when the {@link
     * Handler} is no longer needed so on-demand backing threads (such as {@link
     * #highPriorityExecutor()}) can terminate cleanly.
     */
    public static @Nullable HandlerLease acquireHandlerLease(@NonNull Executor executor) {
        if (executor instanceof HighPriorityExecutor) {
            return ((HighPriorityExecutor) executor).acquireHandlerLease();
        }
        if (executor instanceof HandlerScheduledExecutorService) {
            final Handler handler = ((HandlerScheduledExecutorService) executor).getHandler();
            return new HandlerLease() {
                @Override
                public @NonNull Handler getHandler() {
                    return handler;
                }

                @Override
                public void close() {
                    // Lifecycle of caller-provided and main-thread Loopers is owned externally.
                }
            };
        }
        return null;
    }
}
