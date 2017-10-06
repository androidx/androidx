/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A Processor can intelligently schedule and execute work on demand.
 */

public abstract class Processor implements ExecutionListener {
    private static final String TAG = "Processor";
    protected Context mAppContext;
    protected WorkDatabase mWorkDatabase;

    protected Map<String, Future<?>> mEnqueuedWorkMap;
    protected Scheduler mScheduler;
    protected ScheduledExecutorService mExecutorService;

    protected Processor(
            Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            ScheduledExecutorService executorService) {
        mAppContext = appContext;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mScheduler = scheduler;
        mExecutorService = executorService;
    }

    /**
     * Checks if the Processor should be considered active when processing new jobs.  Some
     * Processors are always active; others depend on a particular Lifecycle.
     *
     * @return {@code true} if the Processor is active.
     */
    public abstract boolean isActive();

    /**
     * Processes a given unit of work in the background.
     *
     * @param id    The work id to execute.
     * @param delay The delay (in milliseconds) to execute this work with.
     */
    public void process(String id, long delay) {
        if (isActive()) {
            WorkerWrapper workWrapper = new WorkerWrapper.Builder(mAppContext, mWorkDatabase, id)
                    .withListener(this)
                    .withScheduler(mScheduler)
                    .build();
            Future<?> future = mExecutorService.schedule(workWrapper, delay, TimeUnit.MILLISECONDS);
            mEnqueuedWorkMap.put(id, future);
            Log.d(TAG, "Submitted " + id + " to ExecutorService");
        } else {
            Log.d(TAG, "Could not process " + id + ". Processor inactive.");
        }
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
        Log.d(TAG, "Canceling " + id + "; mayInterruptIfRunning = " + mayInterruptIfRunning);
        Future<?> future = mEnqueuedWorkMap.get(id);
        if (future != null) {
            boolean cancelled = future.cancel(mayInterruptIfRunning);
            if (cancelled) {
                mEnqueuedWorkMap.remove(id);
                Log.d(TAG, "Future successfully canceled for " + id);
            } else {
                Log.d(TAG, "Future could not be canceled for " + id);
            }
            return cancelled;
        } else {
            Log.d(TAG, "Future not found for " + id);
        }
        return false;
    }

    @Override
    public void onExecuted(String workSpecId, @WorkerWrapper.ExecutionResult int result) {
        mEnqueuedWorkMap.remove(workSpecId);
        Log.d(TAG, workSpecId + " executed; result = " + result);
    }
}
