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

import static androidx.work.impl.foreground.SystemForegroundDispatcher.createStartForegroundIntent;
import static androidx.work.impl.foreground.SystemForegroundDispatcher.createStopForegroundIntent;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.work.Configuration;
import androidx.work.ForegroundInfo;
import androidx.work.Logger;
import androidx.work.WorkerParameters;
import androidx.work.impl.foreground.ForegroundProcessor;
import androidx.work.impl.utils.WakeLocks;
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
public class Processor implements ExecutionListener, ForegroundProcessor {
    private static final String TAG = Logger.tagWithPrefix("Processor");
    private static final String FOREGROUND_WAKELOCK_TAG = "ProcessorForegroundLck";

    @Nullable
    private PowerManager.WakeLock mForegroundLock;

    private Context mAppContext;
    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private WorkDatabase mWorkDatabase;
    private Map<String, WorkerWrapper> mForegroundWorkMap;
    private Map<String, WorkerWrapper> mEnqueuedWorkMap;
    private List<Scheduler> mSchedulers;

    private Set<String> mCancelledIds;

    private final List<ExecutionListener> mOuterListeners;
    private final Object mLock;

    public Processor(
            @NonNull Context appContext,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase,
            @NonNull List<Scheduler> schedulers) {
        mAppContext = appContext;
        mConfiguration = configuration;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mForegroundWorkMap = new HashMap<>();
        mSchedulers = schedulers;
        mCancelledIds = new HashSet<>();
        mOuterListeners = new ArrayList<>();
        mForegroundLock = null;
        mLock = new Object();
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public boolean startWork(@NonNull String id) {
        return startWork(id, null);
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @param runtimeExtras The {@link WorkerParameters.RuntimeExtras} for this work, if any.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public boolean startWork(
            @NonNull String id,
            @Nullable WorkerParameters.RuntimeExtras runtimeExtras) {

        WorkerWrapper workWrapper;
        synchronized (mLock) {
            // Work may get triggered multiple times if they have passing constraints
            // and new work with those constraints are added.
            if (isEnqueued(id)) {
                Logger.get().debug(TAG, "Work " + id + " is already enqueued for processing");
                return false;
            }

            workWrapper =
                    new WorkerWrapper.Builder(
                            mAppContext,
                            mConfiguration,
                            mWorkTaskExecutor,
                            this,
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
        Logger.get().debug(TAG, getClass().getSimpleName() + ": processing " + id);
        return true;
    }

    @Override
    public void startForeground(@NonNull String workSpecId,
            @NonNull ForegroundInfo foregroundInfo) {
        synchronized (mLock) {
            Logger.get().info(TAG, "Moving WorkSpec (" + workSpecId + ") to the foreground");
            WorkerWrapper wrapper = mEnqueuedWorkMap.remove(workSpecId);
            if (wrapper != null) {
                if (mForegroundLock == null) {
                    mForegroundLock = WakeLocks.newWakeLock(mAppContext, FOREGROUND_WAKELOCK_TAG);
                    mForegroundLock.acquire();
                }
                mForegroundWorkMap.put(workSpecId, wrapper);
                Intent intent = createStartForegroundIntent(mAppContext, workSpecId,
                        foregroundInfo);
                ContextCompat.startForegroundService(mAppContext, intent);
            }
        }
    }

    /**
     * Stops a unit of work running in the context of a foreground service.
     *
     * @param id The work id to stop
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopForegroundWork(@NonNull String id) {
        synchronized (mLock) {
            Logger.get().debug(TAG, "Processor stopping foreground work " + id);
            WorkerWrapper wrapper = mForegroundWorkMap.remove(id);
            return interrupt(id, wrapper);
        }
    }

    /**
     * Stops a unit of work.
     *
     * @param id The work id to stop
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopWork(@NonNull String id) {
        synchronized (mLock) {
            Logger.get().debug(TAG, "Processor stopping background work " + id);
            WorkerWrapper wrapper = mEnqueuedWorkMap.remove(id);
            return interrupt(id, wrapper);
        }
    }

    /**
     * Stops a unit of work and marks it as cancelled.
     *
     * @param id The work id to stop and cancel
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopAndCancelWork(@NonNull String id) {
        synchronized (mLock) {
            Logger.get().debug(TAG, "Processor cancelling " + id);
            mCancelledIds.add(id);
            WorkerWrapper wrapper;
            // Check if running in the context of a foreground service
            wrapper = mForegroundWorkMap.remove(id);
            boolean isForegroundWork = wrapper != null;
            if (wrapper == null) {
                // Fallback to enqueued Work
                wrapper = mEnqueuedWorkMap.remove(id);
            }
            boolean interrupted = interrupt(id, wrapper);
            if (isForegroundWork) {
                stopForegroundService();
            }
            return interrupted;
        }
    }

    @Override
    public void stopForeground(@NonNull String workSpecId) {
        synchronized (mLock) {
            mForegroundWorkMap.remove(workSpecId);
            stopForegroundService();
        }
    }

    /**
     * Determines if the given {@code id} is marked as cancelled.
     *
     * @param id The work id to query
     * @return {@code true} if the id has already been marked as cancelled
     */
    public boolean isCancelled(@NonNull String id) {
        synchronized (mLock) {
            return mCancelledIds.contains(id);
        }
    }

    /**
     * @return {@code true} if the processor has work to process.
     */
    public boolean hasWork() {
        synchronized (mLock) {
            return !(mEnqueuedWorkMap.isEmpty()
                    && mForegroundWorkMap.isEmpty());
        }
    }

    /**
     * @param workSpecId The {@link androidx.work.impl.model.WorkSpec} id
     * @return {@code true} if the id was enqueued in the processor.
     */
    public boolean isEnqueued(@NonNull String workSpecId) {
        synchronized (mLock) {
            return mEnqueuedWorkMap.containsKey(workSpecId)
                    || mForegroundWorkMap.containsKey(workSpecId);
        }
    }

    /**
     * @param workSpecId The {@link androidx.work.impl.model.WorkSpec} id
     * @return {@code true} if the id was enqueued as foreground work in the processor.
     */
    public boolean isEnqueuedInForeground(@NonNull String workSpecId) {
        synchronized (mLock) {
            return mForegroundWorkMap.containsKey(workSpecId);
        }
    }

    /**
     * Adds an {@link ExecutionListener} to track when work finishes.
     *
     * @param executionListener The {@link ExecutionListener} to add
     */
    public void addExecutionListener(@NonNull ExecutionListener executionListener) {
        synchronized (mLock) {
            mOuterListeners.add(executionListener);
        }
    }

    /**
     * Removes a tracked {@link ExecutionListener}.
     *
     * @param executionListener The {@link ExecutionListener} to remove
     */
    public void removeExecutionListener(@NonNull ExecutionListener executionListener) {
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
            Logger.get().debug(TAG,
                    getClass().getSimpleName() + " " + workSpecId +
                            " executed; reschedule = " + needsReschedule);
            for (ExecutionListener executionListener : mOuterListeners) {
                executionListener.onExecuted(workSpecId, needsReschedule);
            }
        }
    }

    private void stopForegroundService() {
        synchronized (mLock) {
            boolean hasForegroundWork = !mForegroundWorkMap.isEmpty();
            if (!hasForegroundWork) {
                Intent intent = createStopForegroundIntent(mAppContext);
                try {
                    // Wrapping this inside a try..catch, because there are bugs the platform
                    // that cause an IllegalStateException when an intent is dispatched to stop
                    // the foreground service that is running.
                    mAppContext.startService(intent);
                } catch (Throwable throwable) {
                    Logger.get().error(TAG, "Unable to stop foreground service", throwable);
                }
                // Release wake lock if there is no more pending work.
                if (mForegroundLock != null) {
                    mForegroundLock.release();
                    mForegroundLock = null;
                }
            }
        }
    }

    /**
     * Interrupts a unit of work.
     *
     * @param id      The {@link androidx.work.impl.model.WorkSpec} id
     * @param wrapper The {@link WorkerWrapper}
     * @return {@code true} if the work was stopped successfully
     */
    private static boolean interrupt(@NonNull String id, @Nullable WorkerWrapper wrapper) {
        if (wrapper != null) {
            wrapper.interrupt();
            Logger.get().debug(TAG, "WorkerWrapper interrupted for " + id);
            return true;
        } else {
            Logger.get().debug(TAG, "WorkerWrapper could not be found for " + id);
            return false;
        }
    }

    /**
     * An {@link ExecutionListener} for the {@link ListenableFuture} returned by
     * {@link WorkerWrapper}.
     */
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
