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

package android.arch.background.workmanager.impl.background.systemalarm;

import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.impl.logger.Logger;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages timers to enforce a time limit for processing {@link BaseWork}. Notifies a
 * {@link TimeLimitExceededListener} when the time limit is exceeded.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkTimer {
    private static final String TAG = "WorkTimer";

    private final Map<String, Future<?>> mWorkIdTimerMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService mScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private TimeLimitExceededListener mListener;

    /** Must be public for mocking */
    public void setOnTimeLimitExceededListener(@NonNull TimeLimitExceededListener listener) {
        mListener = listener;
    }

    /** Must be public for mocking */
    public void startTimer(@NonNull final String workSpecId, long processingTimeMillis) {
        stopTimer(workSpecId); // Clear any existing timers for the workSpecId.

        mWorkIdTimerMap.put(workSpecId, mScheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                Future<?> timer = mWorkIdTimerMap.remove(workSpecId);
                if (timer != null && mListener != null) {
                    mListener.onTimeLimitExceeded(workSpecId);
                    Logger.debug(TAG, "Time limit exceeded for Work %s", workSpecId);
                }
            }
        }, processingTimeMillis, TimeUnit.MILLISECONDS));

        Logger.debug(TAG, "Started timer for Work %s", workSpecId);
    }

    /** Must be public for mocking */
    public void stopTimer(@NonNull final String workSpecId) {
        Future<?> timer = mWorkIdTimerMap.remove(workSpecId);
        if (timer != null) {
            timer.cancel(false);
            Logger.debug(TAG, "Stopped timer for Work %s", workSpecId);
        }
    }

    interface TimeLimitExceededListener {
        void onTimeLimitExceeded(@NonNull String workSpecId);
    }
}
