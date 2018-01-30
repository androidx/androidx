/*
 * Copyright 2017 The Android Open Source Project
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
package android.arch.background.workmanager.impl;

import android.arch.background.workmanager.impl.logger.Logger;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A Processor can intelligently schedule and execute work on demand.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Processor implements ExecutionListener {
    private static final String TAG = "Processor";
    protected Context mAppContext;
    protected WorkDatabase mWorkDatabase;

    protected Map<String, Future<?>> mEnqueuedWorkMap;
    protected Scheduler mScheduler;
    protected ExecutorService mExecutorService;

    protected Processor(
            Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            ExecutorService executorService) {
        mAppContext = appContext;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mScheduler = scheduler;
        mExecutorService = executorService;
    }

    /**
     * Processes a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public boolean process(String id) {
        // Work may get triggered multiple times if they have passing constraints and new work with
        // those constraints are added.
        if (mEnqueuedWorkMap.containsKey(id)) {
            Logger.debug(TAG, "Work %s is already enqueued for processing", id);
            return false;
        }

        WorkerWrapper workWrapper = new WorkerWrapper.Builder(mAppContext, mWorkDatabase, id)
                .withListener(this)
                .withScheduler(mScheduler)
                .build();
        mEnqueuedWorkMap.put(id, mExecutorService.submit(workWrapper));
        Logger.debug(TAG, "%s: processing %s", getClass().getSimpleName(), id);
        return true;
    }

    /**
     * Tries to cancel a unit of work.
     *
     * @param id The work id to cancel.
     * @param mayInterruptIfRunning If {@code true}, we try to interrupt the {@link Future} if it's
     *                              running
     * @return {@code true} if the work was cancelled successfully.
     */
    public boolean cancel(String id, boolean mayInterruptIfRunning) {
        Logger.debug(TAG, "%s canceling %s; mayInterruptIfRunning = %s", getClass().getSimpleName(),
                id, mayInterruptIfRunning);
        Future<?> future = mEnqueuedWorkMap.get(id);
        if (future != null) {
            boolean cancelled = future.cancel(mayInterruptIfRunning);
            if (cancelled) {
                mEnqueuedWorkMap.remove(id);
                Logger.debug(TAG, "Future successfully canceled for %s", id);
            } else {
                Logger.debug(TAG, "Future could not be canceled for %s", id);
            }
            return cancelled;
        } else {
            Logger.debug(TAG, "%s future could not be found for %s",
                    getClass().getSimpleName(), id);
        }
        return false;
    }

    /**
     * @return {@code true} if the processor has work to process.
     */
    public boolean hasWork() {
        return !mEnqueuedWorkMap.isEmpty();
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        mEnqueuedWorkMap.remove(workSpecId);
        Logger.debug(TAG, "%s %s executed; isSuccessful = %s, reschedule = %s",
                getClass().getSimpleName(), workSpecId, isSuccessful, needsReschedule);

    }
}
