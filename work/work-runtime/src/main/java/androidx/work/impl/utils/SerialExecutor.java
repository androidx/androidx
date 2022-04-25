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

package androidx.work.impl.utils;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * A {@link Executor} which delegates to another {@link Executor} but ensures that tasks are
 * executed serially, like a single threaded executor.
 */
public class SerialExecutor implements Executor {
    private final ArrayDeque<Task> mTasks;
    private final Executor mExecutor;

    @GuardedBy("mLock")
    private Runnable mActive;

    final Object mLock;

    public SerialExecutor(@NonNull Executor executor) {
        mExecutor = executor;
        mTasks = new ArrayDeque<>();
        mLock = new Object();
    }

    @Override
    public void execute(@NonNull Runnable command) {
        synchronized (mLock) {
            mTasks.add(new Task(this, command));
            if (mActive == null) {
                scheduleNext();
            }
        }
    }

    // Synthetic access
    @GuardedBy("mLock")
    void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            mExecutor.execute(mActive);
        }
    }

    /**
     * @return {@code true} if there are tasks to execute in the queue.
     */
    public boolean hasPendingTasks() {
        synchronized (mLock) {
            return !mTasks.isEmpty();
        }
    }

    @NonNull
    @VisibleForTesting
    public Executor getDelegatedExecutor() {
        return mExecutor;
    }

    /**
     * A {@link Runnable} which tells the {@link SerialExecutor} to schedule the next command
     * after completion.
     */
    static class Task implements Runnable {
        final SerialExecutor mSerialExecutor;
        final Runnable mRunnable;

        Task(@NonNull SerialExecutor serialExecutor, @NonNull Runnable runnable) {
            mSerialExecutor = serialExecutor;
            mRunnable = runnable;
        }

        @Override
        public void run() {
            try {
                mRunnable.run();
            } finally {
                synchronized (mSerialExecutor.mLock) {
                    mSerialExecutor.scheduleNext();
                }
            }
        }
    }
}
