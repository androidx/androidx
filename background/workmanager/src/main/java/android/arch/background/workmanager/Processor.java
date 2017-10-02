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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A Processor can intelligently schedule and execute work on demand.
 */

public abstract class Processor implements ExecutionListener {

    protected Context mAppContext;
    protected WorkDatabase mWorkDatabase;

    protected ExecutorService mExecutorService;
    protected Map<String, Future<?>> mEnqueuedWorkMap;

    protected Scheduler mScheduler;

    public Processor(Context appContext, WorkDatabase workDatabase, Scheduler scheduler) {
        mAppContext = appContext;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mExecutorService = createExecutorService();
        mScheduler = scheduler;
    }

    /**
     * Creates an {@link ExecutorService} appropriate for this Processor.  This will be called once
     * in the constructor.
     *
     * @return An {@link ExecutorService} for this Processor.
     */
    public abstract ExecutorService createExecutorService();

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
     * @param id The work id to execute.
     */
    public void process(String id) {
        if (isActive()) {
            WorkerWrapper workWrapper = new WorkerWrapper.Builder(mAppContext, mWorkDatabase, id)
                    .withListener(this)
                    .withScheduler(mScheduler)
                    .build();
            Future<?> future = mExecutorService.submit(workWrapper);   // TODO(sumir): Delays
            mEnqueuedWorkMap.put(id, future);
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
        Future<?> future = mEnqueuedWorkMap.get(id);
        if (future != null) {
            boolean cancelled = future.cancel(mayInterruptIfRunning);
            if (cancelled) {
                mEnqueuedWorkMap.remove(id);
            }
            return cancelled;
        }
        return false;
    }

    @Override
    public void onExecuted(String workSpecId, @WorkerWrapper.ExecutionResult int result) {
        mEnqueuedWorkMap.remove(workSpecId);
    }
}
