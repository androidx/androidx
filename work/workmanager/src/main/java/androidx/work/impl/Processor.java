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

import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
    private TaskExecutor mWorkTaskExecutor;
    private WorkDatabase mWorkDatabase;
    private Map<String, WorkerWrapper> mEnqueuedWorkMap;
    private List<Scheduler> mSchedulers;

    private Set<String> mCancelledIds;

    private final List<ExecutionListener> mOuterListeners;
    private final Object mLock;

    public Processor(
            Context appContext,
            Configuration configuration,
            TaskExecutor workTaskExecutor,
            WorkDatabase workDatabase,
            List<Scheduler> schedulers) {
        mAppContext = appContext;
        mConfiguration = configuration;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mSchedulers = schedulers;
        mCancelledIds = new HashSet<>();
        mOuterListeners = new ArrayList<>();
        mLock = new Object();
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public boolean startWork(String id) {
        return startWork(id, null);
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @param runtimeExtras The {@link WorkerParameters.RuntimeExtras} for this work, if any.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public boolean startWork(String id, WorkerParameters.RuntimeExtras runtimeExtras) {
        WorkerWrapper workWrapper;
        synchronized (mLock) {
            // Work may get triggered multiple times if they have passing constraints
            // and new work with those constraints are added.
            if (mEnqueuedWorkMap.containsKey(id)) {
                Logger.get().debug(
                        TAG,
                        String.format("Work %s is already enqueued for processing", id));
                return false;
            }

            workWrapper =
                    new WorkerWrapper.Builder(
                            mAppContext,
                            mConfiguration,
                            mWorkTaskExecutor,
                            mWorkDatabase,
                            id)
                            .withSchedulers(mSchedulers)
                            .withRuntimeExtras(runtimeExtras)
                            .build();
            ListenableFuture<Boolean> future = workWrapper.getFuture();
            future.addListener(
                    new FutureListener(this, id, future),
                    mWorkTaskExecutor.getMainThreadExecutor());
            mEnqueuedWorkMap.put(id, workWrapper);
        }
        mWorkTaskExecutor.getBackgroundExecutor().execute(workWrapper);
        Logger.get().debug(TAG, String.format("%s: processing %s", getClass().getSimpleName(), id));
        return true;
    }

    /**
     * Stops a unit of work.
     *
     * @param id The work id to stop
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopWork(String id) {
        synchronized (mLock) {
            Logger.get().debug(TAG, String.format("Processor stopping %s", id));
            WorkerWrapper wrapper = mEnqueuedWorkMap.remove(id);
            if (wrapper != null) {
                wrapper.interrupt(false);
                Logger.get().debug(TAG, String.format("WorkerWrapper stopped for %s", id));
                return true;
            }
            Logger.get().debug(TAG, String.format("WorkerWrapper could not be found for %s", id));
            return false;
        }
    }

    /**
     * Stops a unit of work and marks it as cancelled.
     *
     * @param id The work id to stop and cancel
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopAndCancelWork(String id) {
        synchronized (mLock) {
            Logger.get().debug(TAG, String.format("Processor cancelling %s", id));
            mCancelledIds.add(id);
            WorkerWrapper wrapper = mEnqueuedWorkMap.remove(id);
            if (wrapper != null) {
                wrapper.interrupt(true);
                Logger.get().debug(TAG, String.format("WorkerWrapper cancelled for %s", id));
                return true;
            }
            Logger.get().debug(TAG, String.format("WorkerWrapper could not be found for %s", id));
            return false;
        }
    }

    /**
     * Determines if the given {@code id} is marked as cancelled.
     *
     * @param id The work id to query
     * @return {@code true} if the id has already been marked as cancelled
     */
    public boolean isCancelled(String id) {
        synchronized (mLock) {
            return mCancelledIds.contains(id);
        }
    }

    /**
     * @return {@code true} if the processor has work to process.
     */
    public boolean hasWork() {
        synchronized (mLock) {
            return !mEnqueuedWorkMap.isEmpty();
        }
    }

    /**
     * @param workSpecId The {@link androidx.work.impl.model.WorkSpec} id
     * @return {@code true} if the id was enqueued in the processor.
     */
    public boolean isEnqueued(@NonNull String workSpecId) {
        synchronized (mLock) {
            return mEnqueuedWorkMap.containsKey(workSpecId);
        }
    }

    /**
     * Adds an {@link ExecutionListener} to track when work finishes.
     *
     * @param executionListener The {@link ExecutionListener} to add
     */
    public void addExecutionListener(ExecutionListener executionListener) {
        synchronized (mLock) {
            mOuterListeners.add(executionListener);
        }
    }

    /**
     * Removes a tracked {@link ExecutionListener}.
     *
     * @param executionListener The {@link ExecutionListener} to remove
     */
    public void removeExecutionListener(ExecutionListener executionListener) {
        synchronized (mLock) {
            mOuterListeners.remove(executionListener);
        }
    }

    @Override
    public void onExecuted(
            @NonNull final String workSpecId,
            boolean needsReschedule) {

        synchronized (mLock) {
            mEnqueuedWorkMap.remove(workSpecId);
            Logger.get().debug(TAG, String.format("%s %s executed; reschedule = %s",
                    getClass().getSimpleName(), workSpecId, needsReschedule));

            for (ExecutionListener executionListener : mOuterListeners) {
                executionListener.onExecuted(workSpecId, needsReschedule);
            }
        }
    }

    // TODO: Clean this up some more.
    private static class FutureListener implements Runnable {

        private @NonNull ExecutionListener mExecutionListener;
        private @NonNull String mWorkSpecId;
        private @NonNull ListenableFuture<Boolean> mFuture;

        FutureListener(
                @NonNull ExecutionListener executionListener,
                @NonNull String workSpecId,
                @NonNull ListenableFuture<Boolean> future) {
            mExecutionListener = executionListener;
            mWorkSpecId = workSpecId;
            mFuture = future;
        }

        @Override
        public void run() {
            boolean needsReschedule;
            try {
                needsReschedule = mFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                // Should never really happen(?)
                needsReschedule = true;
            }
            mExecutionListener.onExecuted(mWorkSpecId, needsReschedule);
        }
    }
}
