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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;
import androidx.work.WorkRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Manages timers to enforce a time limit for processing {@link WorkRequest}.
 * Notifies a {@link TimeLimitExceededListener} when the time limit
 * is exceeded.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkTimer {

    private static final String TAG = Logger.tagWithPrefix("WorkTimer");

    private final ThreadFactory mBackgroundThreadFactory = new ThreadFactory() {

        private int mThreadsCreated = 0;

        @Override
        public Thread newThread(@NonNull Runnable r) {
            // Delegate to the default factory, but keep track of the current thread being used.
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("WorkManager-WorkTimer-thread-" + mThreadsCreated);
            mThreadsCreated++;
            return thread;
        }
    };

    private final ScheduledExecutorService mExecutorService;
    final Map<String, WorkTimerRunnable> mTimerMap;
    final Map<String, TimeLimitExceededListener> mListeners;
    final Object mLock;

    public WorkTimer() {
        mTimerMap = new HashMap<>();
        mListeners = new HashMap<>();
        mLock = new Object();
        mExecutorService = Executors.newSingleThreadScheduledExecutor(mBackgroundThreadFactory);
    }

    /**
     * Keeps track of execution time for a given {@link androidx.work.impl.model.WorkSpec}.
     * The {@link TimeLimitExceededListener} is notified when the execution time exceeds {@code
     * processingTimeMillis}.
     *
     * @param workSpecId           The {@link androidx.work.impl.model.WorkSpec} id
     * @param processingTimeMillis The allocated time for execution in milliseconds
     * @param listener             The listener which is notified when the execution time exceeds
     *                             {@code processingTimeMillis}
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public void startTimer(@NonNull final String workSpecId,
            long processingTimeMillis,
            @NonNull TimeLimitExceededListener listener) {

        synchronized (mLock) {
            Logger.get().debug(TAG, "Starting timer for " + workSpecId);
            // clear existing timer's first
            stopTimer(workSpecId);
            WorkTimerRunnable runnable = new WorkTimerRunnable(this, workSpecId);
            mTimerMap.put(workSpecId, runnable);
            mListeners.put(workSpecId, listener);
            mExecutorService.schedule(runnable, processingTimeMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops tracking the execution time for a given {@link androidx.work.impl.model.WorkSpec}.
     *
     * @param workSpecId The {@link androidx.work.impl.model.WorkSpec} id
     */
    public void stopTimer(@NonNull final String workSpecId) {
        synchronized (mLock) {
            WorkTimerRunnable removed = mTimerMap.remove(workSpecId);
            if (removed != null) {
                Logger.get().debug(TAG, "Stopping timer for " + workSpecId);
                mListeners.remove(workSpecId);
            }
        }
    }

    /**
     * This method needs to be idempotent. This could be called more than once, and therefore,
     * this method should only perform cleanup when necessary.
     */
    public void onDestroy() {
        if (!mExecutorService.isShutdown()) {
            // Calling shutdown() waits for pending scheduled WorkTimerRunnable's which is not
            // something we care about. Hence call shutdownNow().
            mExecutorService.shutdownNow();
        }
    }

    @VisibleForTesting
    @NonNull
    public synchronized Map<String, WorkTimerRunnable> getTimerMap() {
        return mTimerMap;
    }

    @VisibleForTesting
    @NonNull
    public synchronized Map<String, TimeLimitExceededListener> getListeners() {
        return mListeners;
    }

    @VisibleForTesting
    @NonNull
    public ScheduledExecutorService getExecutorService() {
        return mExecutorService;
    }

    /**
     * The actual runnable scheduled on the scheduled executor.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class WorkTimerRunnable implements Runnable {
        static final String TAG = "WrkTimerRunnable";

        private final WorkTimer mWorkTimer;
        private final String mWorkSpecId;

        WorkTimerRunnable(@NonNull WorkTimer workTimer, @NonNull String workSpecId) {
            mWorkTimer = workTimer;
            mWorkSpecId = workSpecId;
        }

        @Override
        public void run() {
            synchronized (mWorkTimer.mLock) {
                WorkTimerRunnable removed = mWorkTimer.mTimerMap.remove(mWorkSpecId);
                if (removed != null) {
                    // notify time limit exceeded.
                    TimeLimitExceededListener listener = mWorkTimer.mListeners.remove(mWorkSpecId);
                    if (listener != null) {
                        listener.onTimeLimitExceeded(mWorkSpecId);
                    }
                } else {
                    Logger.get().debug(TAG, String.format(
                            "Timer with %s is already marked as complete.", mWorkSpecId));
                }
            }
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface TimeLimitExceededListener {
        /**
         * The time limit exceeded listener.
         *
         * @param workSpecId The {@link androidx.work.impl.model.WorkSpec} id for which time limit
         *                   has exceeded.
         */
        void onTimeLimitExceeded(@NonNull String workSpecId);
    }
}
