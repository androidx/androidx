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
import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.arch.background.workmanager.impl.logger.Logger;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;

/**
 * A {@link BackgroundProcessor} with restrictions on how long {@link BaseWork} can run.
 *
 * The time limit for processing {@link BaseWork} is specified by
 * {@link #PROCESSING_TIME_LIMIT_MILLIS}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TimedBackgroundProcessor extends BackgroundProcessor
        implements WorkTimer.TimeLimitExceededListener {
    private static final String TAG = "TimedBackgroundProc";

    /** 10 minutes to be consistent with JobScheduler */
    @VisibleForTesting
    static final long PROCESSING_TIME_LIMIT_MILLIS = 10 * 60 * 1000L;

    private final WorkTimer mWorkTimer;

    TimedBackgroundProcessor(Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            ExecutorService executorService,
            ExecutionListener executionListener,
            WorkTimer workTimer) {
        super(appContext, workDatabase, scheduler, executorService, executionListener);

        mWorkTimer = workTimer;
        mWorkTimer.setOnTimeLimitExceededListener(this);
    }

    @Override
    public boolean process(String id) {
        boolean willProcess = super.process(id);
        if (willProcess) {
            mWorkTimer.startTimer(id, PROCESSING_TIME_LIMIT_MILLIS);
        }
        return willProcess;
    }

    @Override
    public boolean cancel(String id, boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(id, mayInterruptIfRunning);
        if (cancelled) {
            mWorkTimer.stopTimer(id);
            // TODO(janclarin): SystemAlarmService should be notified of cancel to release wakelock.
        }
        return cancelled;
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        mWorkTimer.stopTimer(workSpecId);
        super.onExecuted(workSpecId, isSuccessful, needsReschedule);
    }

    @Override
    public void onTimeLimitExceeded(@NonNull String workSpecId) {
        Logger.debug(TAG, "Processing time limit exceeded for Work %s", workSpecId);
        cancel(workSpecId, true);
    }
}
