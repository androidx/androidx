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
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.model.WorkSpec;
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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Processor implements ForegroundProcessor {
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
    //  workSpecId  to a  Set<WorkRunId>
    private Map<String, Set<StartStopToken>> mWorkRuns;
    private Set<String> mCancelledIds;

    private final List<ExecutionListener> mOuterListeners;
    private final Object mLock;

    public Processor(
            @NonNull Context appContext,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase) {
        mAppContext = appContext;
        mConfiguration = configuration;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkDatabase = workDatabase;
        mEnqueuedWorkMap = new HashMap<>();
        mForegroundWorkMap = new HashMap<>();
        mCancelledIds = new HashSet<>();
        mOuterListeners = new ArrayList<>();
        mForegroundLock = null;
        mLock = new Object();
        mWorkRuns = new HashMap<>();
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param id The work id to execute.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    public boolean startWork(@NonNull StartStopToken id) {
        return startWork(id, null);
    }

    /**
     * Starts a given unit of work in the background.
     *
     * @param startStopToken The work id to execute.
     * @param runtimeExtras  The {@link WorkerParameters.RuntimeExtras} for this work, if any.
     * @return {@code true} if the work was successfully enqueued for processing
     */
    @SuppressWarnings("ConstantConditions")
    public boolean startWork(
            @NonNull StartStopToken startStopToken,
            @Nullable WorkerParameters.RuntimeExtras runtimeExtras) {
        WorkGenerationalId id = startStopToken.getId();
        String workSpecId = id.getWorkSpecId();
        ArrayList<String> tags = new ArrayList<>();
        WorkSpec workSpec = mWorkDatabase.runInTransaction(
                () -> {
                    tags.addAll(mWorkDatabase.workTagDao().getTagsForWorkSpecId(workSpecId));
                    return mWorkDatabase.workSpecDao().getWorkSpec(workSpecId);
                }
        );
        if (workSpec == null) {
            Logger.get().warning(TAG, "Didn't find WorkSpec for id " + id);
            runOnExecuted(id, false);
            return false;
        }
        WorkerWrapper workWrapper;
        synchronized (mLock) {
            // Work may get triggered multiple times if they have passing constraints
            // and new work with those constraints are added.
            if (isEnqueued(workSpecId)) {
                // there must be another run if it is enqueued.
                Set<StartStopToken> tokens = mWorkRuns.get(workSpecId);
                StartStopToken previousRun = tokens.iterator().next();
                int previousRunGeneration = previousRun.getId().getGeneration();
                if (previousRunGeneration == id.getGeneration()) {
                    tokens.add(startStopToken);
                    Logger.get().debug(TAG, "Work " + id + " is already enqueued for processing");
                } else {
                    // Implementation detail.
                    // If previousRunGeneration > id.getGeneration(), then we don't have to do
                    // anything because newer generation is already running
                    //
                    // Case of previousRunGeneration < id.getGeneration():
                    // it should happen only in the case of the periodic worker,
                    // so we let run a current Worker, and periodic worker will schedule
                    // next iteration with updated work spec.
                    runOnExecuted(id, false);
                }
                return false;
            }

            if (workSpec.getGeneration() != id.getGeneration()) {
                // not the latest generation, so ignoring this start request,
                // new request with newer generation should arrive shortly.
                runOnExecuted(id, false);
                return false;
            }
            workWrapper =
                    new WorkerWrapper.Builder(
                            mAppContext,
                            mConfiguration,
                            mWorkTaskExecutor,
                            this,
                            mWorkDatabase,
                            workSpec,
                            tags)
                            .withRuntimeExtras(runtimeExtras)
                            .build();
            ListenableFuture<Boolean> future = workWrapper.getFuture();
            future.addListener(
                    () -> {
                        boolean needsReschedule;
                        try {
                            needsReschedule = future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            // Should never really happen(?)
                            needsReschedule = true;
                        }
                        onExecuted(workWrapper, needsReschedule);
                    },
                    mWorkTaskExecutor.getMainThreadExecutor());
            mEnqueuedWorkMap.put(workSpecId, workWrapper);
            HashSet<StartStopToken> set = new HashSet<>();
            set.add(startStopToken);
            mWorkRuns.put(workSpecId, set);
        }
        mWorkTaskExecutor.getSerialTaskExecutor().execute(workWrapper);
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
                Intent intent = createStartForegroundIntent(mAppContext,
                        wrapper.getWorkGenerationalId(), foregroundInfo);
                ContextCompat.startForegroundService(mAppContext, intent);
            }
        }
    }

    /**
     * Stops a unit of work running in the context of a foreground service.
     *
     * @param token The work to stop
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopForegroundWork(@NonNull StartStopToken token, int reason) {
        String id = token.getId().getWorkSpecId();
        WorkerWrapper wrapper;
        synchronized (mLock) {
            // TODO: race, we can cancel next run of the worker.
            wrapper = cleanUpWorkerUnsafe(id);
        }
        // Move interrupt() outside the critical section.
        // This is because calling interrupt() eventually calls ListenableWorker.onStopped()
        // If onStopped() takes too long, there is a good chance this causes an ANR
        // in Processor.onExecuted().
        return interrupt(id, wrapper, reason);
    }

    /**
     * Stops a unit of work.
     *
     * @param runId The work id to stop
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopWork(@NonNull StartStopToken runId, int reason) {
        String id = runId.getId().getWorkSpecId();
        WorkerWrapper wrapper;
        synchronized (mLock) {
            if (mForegroundWorkMap.get(id) != null) {
                Logger.get().debug(TAG,
                        "Ignored stopWork. WorkerWrapper " + id + " is in foreground");
                return false;
            }
            // Processor _only_ receives stopWork() requests from the schedulers that originally
            // scheduled the work, and not others. This means others are still notified about
            // completion, but we avoid a accidental "stops" and lot of redundant work when
            // attempting to stop.
            Set<StartStopToken> runs = mWorkRuns.get(id);
            if (runs == null || !runs.contains(runId)) {
                return false;
            }
            wrapper = cleanUpWorkerUnsafe(id);
        }
        // Move interrupt() outside the critical section.
        // This is because calling interrupt() eventually calls ListenableWorker.onStopped()
        // If onStopped() takes too long, there is a good chance this causes an ANR
        // in Processor.onExecuted().
        return interrupt(id, wrapper, reason);
    }

    /**
     * Stops a unit of work and marks it as cancelled.
     *
     * @param id The work id to stop and cancel
     * @return {@code true} if the work was stopped successfully
     */
    public boolean stopAndCancelWork(@NonNull String id, int reason) {
        WorkerWrapper wrapper;
        synchronized (mLock) {
            Logger.get().debug(TAG, "Processor cancelling " + id);
            mCancelledIds.add(id);
            // Check if running in the context of a foreground service
            wrapper = cleanUpWorkerUnsafe(id);
        }
        // Move interrupt() outside the critical section.
        // This is because calling interrupt() eventually calls ListenableWorker.onStopped()
        // If onStopped() takes too long, there is a good chance this causes an ANR
        // in Processor.onExecuted().
        return interrupt(id, wrapper, reason);
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
            return getWorkerWrapperUnsafe(workSpecId) != null;
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

    private void onExecuted(@NonNull WorkerWrapper wrapper, boolean needsReschedule) {
        synchronized (mLock) {
            WorkGenerationalId id = wrapper.getWorkGenerationalId();
            String workSpecId = id.getWorkSpecId();
            WorkerWrapper workerWrapper = getWorkerWrapperUnsafe(workSpecId);
            // can be called for another generation, so we shouldn't remove it
            if (workerWrapper == wrapper) {
                cleanUpWorkerUnsafe(workSpecId);
            }
            Logger.get().debug(TAG,
                    getClass().getSimpleName() + " " + workSpecId
                            + " executed; reschedule = " + needsReschedule);
            for (ExecutionListener executionListener : mOuterListeners) {
                executionListener.onExecuted(id, needsReschedule);
            }
        }
    }

    @Nullable
    private WorkerWrapper getWorkerWrapperUnsafe(@NonNull String workSpecId) {
        WorkerWrapper workerWrapper = mForegroundWorkMap.get(workSpecId);
        if (workerWrapper == null) {
            workerWrapper = mEnqueuedWorkMap.get(workSpecId);
        }
        return workerWrapper;
    }

    /**
     * Returns a spec of the running worker by the given id
     *
     * @param workSpecId id of running worker
     */
    @Nullable
    public WorkSpec getRunningWorkSpec(@NonNull String workSpecId) {
        synchronized (mLock) {
            WorkerWrapper workerWrapper = getWorkerWrapperUnsafe(workSpecId);
            if (workerWrapper != null) {
                return workerWrapper.getWorkSpec();
            } else {
                return null;
            }
        }
    }

    private void runOnExecuted(@NonNull final WorkGenerationalId id, boolean needsReschedule) {
        mWorkTaskExecutor.getMainThreadExecutor().execute(
                () -> {
                    synchronized (mLock) {
                        for (ExecutionListener executionListener : mOuterListeners) {
                            executionListener.onExecuted(id, needsReschedule);
                        }
                    }
                }
        );
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

    @Nullable
    private WorkerWrapper cleanUpWorkerUnsafe(@NonNull String id) {
        WorkerWrapper wrapper = mForegroundWorkMap.remove(id);
        boolean wasForeground = wrapper != null;
        if (!wasForeground) {
            wrapper = mEnqueuedWorkMap.remove(id);
        }
        mWorkRuns.remove(id);
        if (wasForeground) {
            stopForegroundService();
        }
        return wrapper;
    }

    /**
     * Interrupts a unit of work.
     *
     * @param id      The {@link androidx.work.impl.model.WorkSpec} id
     * @param wrapper The {@link WorkerWrapper}
     * @return {@code true} if the work was stopped successfully
     */
    private static boolean interrupt(@NonNull String id,
            @Nullable WorkerWrapper wrapper, int stopReason) {
        if (wrapper != null) {
            wrapper.interrupt(stopReason);
            Logger.get().debug(TAG, "WorkerWrapper interrupted for " + id);
            return true;
        } else {
            Logger.get().debug(TAG, "WorkerWrapper could not be found for " + id);
            return false;
        }
    }
}
