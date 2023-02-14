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
import androidx.work.RunnableScheduler;
import androidx.work.WorkRequest;
import androidx.work.impl.model.WorkGenerationalId;

import java.util.HashMap;
import java.util.Map;

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

    final RunnableScheduler mRunnableScheduler;
    final Map<WorkGenerationalId, WorkTimerRunnable> mTimerMap;
    final Map<WorkGenerationalId, TimeLimitExceededListener> mListeners;
    final Object mLock;

    public WorkTimer(@NonNull RunnableScheduler scheduler) {
        mTimerMap = new HashMap<>();
        mListeners = new HashMap<>();
        mLock = new Object();
        mRunnableScheduler = scheduler;
    }

    /**
     * Keeps track of execution time for a given {@link androidx.work.impl.model.WorkSpec}.
     * The {@link TimeLimitExceededListener} is notified when the execution time exceeds {@code
     * processingTimeMillis}.
     *
     * @param id           The {@link androidx.work.impl.model.WorkSpec} id
     * @param processingTimeMillis The allocated time for execution in milliseconds
     * @param listener             The listener which is notified when the execution time exceeds
     *                             {@code processingTimeMillis}
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public void startTimer(@NonNull final WorkGenerationalId id,
            long processingTimeMillis,
            @NonNull TimeLimitExceededListener listener) {

        synchronized (mLock) {
            Logger.get().debug(TAG, "Starting timer for " + id);
            // clear existing timer's first
            stopTimer(id);
            WorkTimerRunnable runnable = new WorkTimerRunnable(this, id);
            mTimerMap.put(id, runnable);
            mListeners.put(id, listener);
            mRunnableScheduler.scheduleWithDelay(processingTimeMillis, runnable);
        }
    }

    /**
     * Stops tracking the execution time for a given {@link androidx.work.impl.model.WorkSpec}.
     *
     * @param id The {@link androidx.work.impl.model.WorkSpec} id
     */
    public void stopTimer(@NonNull final WorkGenerationalId id) {
        synchronized (mLock) {
            WorkTimerRunnable removed = mTimerMap.remove(id);
            if (removed != null) {
                Logger.get().debug(TAG, "Stopping timer for " + id);
                mListeners.remove(id);
            }
        }
    }

    @VisibleForTesting
    @NonNull
    public Map<WorkGenerationalId, WorkTimerRunnable> getTimerMap() {
        synchronized (mLock) {
            return mTimerMap;
        }
    }

    @VisibleForTesting
    @NonNull
    public Map<WorkGenerationalId, TimeLimitExceededListener> getListeners() {
        synchronized (mLock) {
            return mListeners;
        }
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
        private final WorkGenerationalId mWorkGenerationalId;

        WorkTimerRunnable(@NonNull WorkTimer workTimer, @NonNull WorkGenerationalId id) {
            mWorkTimer = workTimer;
            mWorkGenerationalId = id;
        }

        @Override
        public void run() {
            synchronized (mWorkTimer.mLock) {
                WorkTimerRunnable removed = mWorkTimer.mTimerMap.remove(mWorkGenerationalId);
                if (removed != null) {
                    // notify time limit exceeded.
                    TimeLimitExceededListener listener = mWorkTimer.mListeners
                            .remove(mWorkGenerationalId);
                    if (listener != null) {
                        listener.onTimeLimitExceeded(mWorkGenerationalId);
                    }
                } else {
                    Logger.get().debug(TAG, String.format(
                            "Timer with %s is already marked as complete.", mWorkGenerationalId));
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
         * @param id The {@link androidx.work.impl.model.WorkSpec} id for which time limit
         *                   has exceeded.
         */
        void onTimeLimitExceeded(@NonNull WorkGenerationalId id);
    }
}
