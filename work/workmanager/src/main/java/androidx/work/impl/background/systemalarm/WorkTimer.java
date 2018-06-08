/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.background.systemalarm;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.WorkRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages timers to enforce a time limit for processing {@link WorkRequest}.
 * Notifies a {@link TimeLimitExceededListener} when the time limit
 * is exceeded.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WorkTimer {

    private static final String TAG = "WorkTimer";

    private final ScheduledExecutorService mExecutorService;
    private final Map<String, WorkTimerRunnable> mTimerMap;
    private final Map<String, TimeLimitExceededListener> mListeners;
    private final Object mLock;

    WorkTimer() {
        mTimerMap = new HashMap<>();
        mListeners = new HashMap<>();
        mLock = new Object();
        mExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void startTimer(@NonNull final String workSpecId,
            long processingTimeMillis,
            @NonNull TimeLimitExceededListener listener) {

        synchronized (mLock) {
            Log.d(TAG, String.format("Starting timer for %s", workSpecId));
            // clear existing timer's first
            stopTimer(workSpecId);
            WorkTimerRunnable runnable = new WorkTimerRunnable(this, workSpecId);
            mTimerMap.put(workSpecId, runnable);
            mListeners.put(workSpecId, listener);
            mExecutorService.schedule(runnable, processingTimeMillis, TimeUnit.MILLISECONDS);
        }
    }

    void stopTimer(@NonNull final String workSpecId) {
        synchronized (mLock) {
            if (mTimerMap.containsKey(workSpecId)) {
                Log.d(TAG, String.format("Stopping timer for %s", workSpecId));
                mTimerMap.remove(workSpecId);
                mListeners.remove(workSpecId);
            }
        }
    }

    @VisibleForTesting
    synchronized Map<String, WorkTimerRunnable> getTimerMap() {
        return mTimerMap;
    }

    @VisibleForTesting
    synchronized Map<String, TimeLimitExceededListener> getListeners() {
        return mListeners;
    }

    /**
     * The actual runnable scheduled on the scheduled executor.
     */
    static class WorkTimerRunnable implements Runnable {
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
                if (mWorkTimer.mTimerMap.containsKey(mWorkSpecId)) {
                    mWorkTimer.mTimerMap.remove(mWorkSpecId);
                    // notify time limit exceeded.
                    TimeLimitExceededListener listener = mWorkTimer.mListeners.remove(mWorkSpecId);
                    if (listener != null) {
                        listener.onTimeLimitExceeded(mWorkSpecId);
                    }
                } else {
                    Log.d(TAG, String.format(
                            "Timer with %s is already marked as complete.", mWorkSpecId));
                }
            }
        }
    }

    interface TimeLimitExceededListener {
        void onTimeLimitExceeded(@NonNull String workSpecId);
    }
}
