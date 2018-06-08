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
package androidx.work.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import androidx.work.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * A Processor can intelligently schedule and execute work on demand.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Processor implements ExecutionListener {
    private static final String TAG = "Processor";

    private Context mAppContext;
    private Configuration mConfiguration;
    private WorkDatabase mWorkDatabase;
    private Map<String, WorkerWrapper> mEnqueuedWorkMap;
    private List<Scheduler> mSchedulers;
    private Executor mExecutor;

    private Set<String> mCancelledIds;

    private final List<ExecutionListener> mOuterListeners;

    public Processor(
            Context appContext,
            Configuration configuration,
            WorkDatabase workDatabase,
            List<Scheduler> schedulers,
            Executor executor) {
        mAppContext = appContext;
        mConfiguration = configuration;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mSchedulers = schedulers;
        mExecutor = executor;
        mCancelledIds = new HashSet<>();
        mOuterListeners = new ArrayList<>();
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public synchronized boolean startWork(String id) {
        return startWork(id, null);
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @param runtimeExtras The {@link Extras.RuntimeExtras} for this work, if any.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public synchronized boolean startWork(String id, Extras.RuntimeExtras runtimeExtras) {
        // Work may get triggered multiple times if they have passing constraints and new work with
        // those constraints are added.
        if (mEnqueuedWorkMap.containsKey(id)) {
            Log.d(TAG, String.format("Work %s is already enqueued for processing", id));
            return false;
        }

        WorkerWrapper workWrapper =
                new WorkerWrapper.Builder(mAppContext, mConfiguration, mWorkDatabase, id)
                        .withListener(this)
                        .withSchedulers(mSchedulers)
                        .withRuntimeExtras(runtimeExtras)
                        .build();
        mEnqueuedWorkMap.put(id, workWrapper);
        mExecutor.execute(workWrapper);
        Log.d(TAG, String.format("%s: processing %s", getClass().getSimpleName(), id));
        return true;
    }

    /**
     * Stops a unit of work.
     *
     * @param id The work id to stop
     * @return {@code true} if the work was stopped successfully
     */
    public synchronized boolean stopWork(String id) {
        Log.d(TAG, String.format("Processor stopping %s", id));
        WorkerWrapper wrapper = mEnqueuedWorkMap.remove(id);
        if (wrapper != null) {
            wrapper.interrupt(false);
            Log.d(TAG, String.format("WorkerWrapper stopped for %s", id));
            return true;
        }
        Log.d(TAG, String.format("WorkerWrapper could not be found for %s", id));
        return false;
    }

    /**
     * Stops a unit of work and marks it as cancelled.
     *
     * @param id The work id to stop and cancel
     * @return {@code true} if the work was stopped successfully
     */
    public synchronized boolean stopAndCancelWork(String id) {
        Log.d(TAG, String.format("Processor cancelling %s", id));
        mCancelledIds.add(id);
        WorkerWrapper wrapper = mEnqueuedWorkMap.remove(id);
        if (wrapper != null) {
            wrapper.interrupt(true);
            Log.d(TAG, String.format("WorkerWrapper cancelled for %s", id));
            return true;
        }
        Log.d(TAG, String.format("WorkerWrapper could not be found for %s", id));
        return false;
    }

    /**
     * Determines if the given {@code id} is marked as cancelled.
     *
     * @param id The work id to query
     * @return {@code true} if the id has already been marked as cancelled
     */
    public synchronized boolean isCancelled(String id) {
        return mCancelledIds.contains(id);
    }

    /**
     * @return {@code true} if the processor has work to process.
     */
    public synchronized boolean hasWork() {
        return !mEnqueuedWorkMap.isEmpty();
    }

    /**
     * @param workSpecId The {@link androidx.work.impl.model.WorkSpec} id
     * @return {@code true} if the id was enqueued in the processor.
     */
    public synchronized boolean isEnqueued(@NonNull String workSpecId) {
        return mEnqueuedWorkMap.containsKey(workSpecId);
    }

    /**
     * Adds an {@link ExecutionListener} to track when work finishes.
     *
     * @param executionListener The {@link ExecutionListener} to add
     */
    public synchronized void addExecutionListener(ExecutionListener executionListener) {
        // TODO(sumir): Let's get some synchronization guarantees here.
        mOuterListeners.add(executionListener);
    }

    /**
     * Removes a tracked {@link ExecutionListener}.
     *
     * @param executionListener The {@link ExecutionListener} to remove
     */
    public synchronized void removeExecutionListener(ExecutionListener executionListener) {
        // TODO(sumir): Let's get some synchronization guarantees here.
        mOuterListeners.remove(executionListener);
    }

    @Override
    public synchronized void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        mEnqueuedWorkMap.remove(workSpecId);
        Log.d(TAG, String.format("%s %s executed; isSuccessful = %s, reschedule = %s",
                getClass().getSimpleName(), workSpecId, isSuccessful, needsReschedule));

        // TODO(sumir): Let's get some synchronization guarantees here.
        for (ExecutionListener executionListener : mOuterListeners) {
            executionListener.onExecuted(workSpecId, isSuccessful, needsReschedule);
        }
    }
}
