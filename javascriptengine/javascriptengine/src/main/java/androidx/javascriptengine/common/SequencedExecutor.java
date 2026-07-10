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

package androidx.javascriptengine.common;

import androidx.annotation.GuardedBy;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Wrapper around given {@link Executor} which allows to run tasks in the same order
 * as they were posted to the execution queue.
 *
 * Tasks are executed using the provided executor. It can be single or multi threaded.
 * <p>
 * If the given executor rejects tasks for any reason, the SequencedExecutor will stop
 * running current tasks and silently drop new ones.
 */
public final class SequencedExecutor implements Executor {
    private final Object mLock = new Object();
    @NonNull
    private final Executor mInner;
    @GuardedBy("mLock")
    @Nullable
    private ArrayDeque<Runnable> mQueue = new ArrayDeque<>();
    @GuardedBy("mLock")
    private boolean mIsDraining = false;

    /**
     * Create an executor that run tasks in the same order they are posted.
     *
     * @param inner The executor which runs the tasks. Can be single or multi threaded.
     */
    public SequencedExecutor(@NonNull Executor inner) {
        mInner = inner;
    }

    /**
     * Post a task to the execution queue.
     *
     * Queued tasks are executed in the order they are posted.
     * <p>
     * Drops the request if the underlying executor is shutdown.
     *
     * @param runnable The task to add to the execution queue.
     */
    @Override
    public void execute(@NonNull Runnable runnable) {
        synchronized (mLock) {
            if (mQueue == null) return;
            mQueue.add(runnable);
            if (mIsDraining) return;
            mIsDraining = true;
        }

        try {
            mInner.execute(this::drainOnInner);
        } catch (RejectedExecutionException e) {
            synchronized (mLock) {
                mIsDraining = false;
                mQueue = null;
            }
        }
    }

    /**
     * Process queued tasks using the inner executor.
     *
     * Stops running tasks if the underlying executor is shutdown.
     */
    private void drainOnInner() {
        Runnable task;
        synchronized (mLock) {
            if (mQueue == null) {
                mIsDraining = false;
                return;
            }
            task = mQueue.poll();
            if (task == null) {
                mIsDraining = false;
                return;
            }
        }

        try {
            task.run();
        } finally {
            boolean shouldContinue = true;
            synchronized (mLock) {
                if (mQueue == null || mQueue.isEmpty()) {
                    mIsDraining = false;
                    shouldContinue = false;
                }
            }

            if (shouldContinue) {
                try {
                    mInner.execute(this::drainOnInner);
                } catch (RejectedExecutionException e) {
                    synchronized (mLock) {
                        mIsDraining = false;
                        mQueue = null;
                    }
                }
            }
        }
    }
}
