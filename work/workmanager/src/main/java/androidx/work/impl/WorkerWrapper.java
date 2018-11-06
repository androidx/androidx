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

import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.InputMerger;
import androidx.work.ListenableWorker;
import androidx.work.ListenableWorker.Result;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.background.systemalarm.RescheduleReceiver;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTagDao;
import androidx.work.impl.utils.PackageManagerHelper;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A runnable that looks up the {@link WorkSpec} from the database for a given id, instantiates
 * its Worker, and then calls it.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkerWrapper implements Runnable {

    private static final String TAG = "WorkerWrapper";

    private Context mAppContext;
    private String mWorkSpecId;
    private List<Scheduler> mSchedulers;
    private WorkerParameters.RuntimeExtras mRuntimeExtras;
    private WorkSpec mWorkSpec;
    ListenableWorker mWorker;

    // Package-private for synthetic accessor.
    @NonNull ListenableWorker.Payload mPayload = new ListenableWorker.Payload(Result.FAILURE);

    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private WorkDatabase mWorkDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private WorkTagDao mWorkTagDao;

    private List<String> mTags;
    private String mWorkDescription;

    private @NonNull SettableFuture<Boolean> mFuture = SettableFuture.create();

    // Package-private for synthetic accessor.
    @Nullable ListenableFuture<ListenableWorker.Payload> mInnerFuture = null;


    private volatile boolean mInterrupted;

    // Package-private for synthetic accessor.
    WorkerWrapper(Builder builder) {
        mAppContext = builder.mAppContext;
        mWorkTaskExecutor = builder.mWorkTaskExecutor;
        mWorkSpecId = builder.mWorkSpecId;
        mSchedulers = builder.mSchedulers;
        mRuntimeExtras = builder.mRuntimeExtras;
        mWorker = builder.mWorker;

        mConfiguration = builder.mConfiguration;
        mWorkDatabase = builder.mWorkDatabase;
        mWorkSpecDao = mWorkDatabase.workSpecDao();
        mDependencyDao = mWorkDatabase.dependencyDao();
        mWorkTagDao = mWorkDatabase.workTagDao();
    }

    public @NonNull ListenableFuture<Boolean> getFuture() {
        return mFuture;
    }

    @WorkerThread
    @Override
    public void run() {
        mTags = mWorkTagDao.getTagsForWorkSpecId(mWorkSpecId);
        mWorkDescription = createWorkDescription(mTags);
        runWorker();
    }

    private void runWorker() {
        if (tryCheckForInterruptionAndResolve()) {
            return;
        }

        mWorkDatabase.beginTransaction();
        try {
            mWorkSpec = mWorkSpecDao.getWorkSpec(mWorkSpecId);
            if (mWorkSpec == null) {
                Logger.error(TAG, String.format("Didn't find WorkSpec for id %s", mWorkSpecId));
                resolve(false);
                return;
            }

            // Do a quick check to make sure we don't need to bail out in case this work is already
            // running, finished, or is blocked.
            if (mWorkSpec.state != ENQUEUED) {
                resolveIncorrectStatus();
                mWorkDatabase.setTransactionSuccessful();
                return;
            }

            // Needed for nested transactions, such as when we're in a dependent work request when
            // using a SynchronousExecutor.
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }

        // Merge inputs.  This can be potentially expensive code, so this should not be done inside
        // a database transaction.
        Data input;
        if (mWorkSpec.isPeriodic()) {
            input = mWorkSpec.input;
        } else {
            InputMerger inputMerger = InputMerger.fromClassName(mWorkSpec.inputMergerClassName);
            if (inputMerger == null) {
                Logger.error(TAG, String.format("Could not create Input Merger %s",
                        mWorkSpec.inputMergerClassName));
                setFailedAndResolve();
                return;
            }
            List<Data> inputs = new ArrayList<>();
            inputs.add(mWorkSpec.input);
            inputs.addAll(mWorkSpecDao.getInputsFromPrerequisites(mWorkSpecId));
            input = inputMerger.merge(inputs);
        }

        WorkerParameters params = new WorkerParameters(
                UUID.fromString(mWorkSpecId),
                input,
                mTags,
                mRuntimeExtras,
                mWorkSpec.runAttemptCount,
                mConfiguration.getExecutor(),
                mWorkTaskExecutor,
                mConfiguration.getWorkerFactory());

        // Not always creating a worker here, as the WorkerWrapper.Builder can set a worker override
        // in test mode.
        if (mWorker == null) {
            mWorker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                    mAppContext,
                    mWorkSpec.workerClassName,
                    params);
        }

        if (mWorker == null) {
            Logger.error(TAG,
                    String.format("Could not create Worker %s", mWorkSpec.workerClassName));
            setFailedAndResolve();
            return;
        }

        if (mWorker.isUsed()) {
            Logger.error(TAG,
                    String.format("Received an already-used Worker %s; WorkerFactory should return "
                            + "new instances",
                            mWorkSpec.workerClassName));
            setFailedAndResolve();
            return;
        }
        mWorker.setUsed();

        // Try to set the work to the running state.  Note that this may fail because another thread
        // may have modified the DB since we checked last at the top of this function.
        if (trySetRunning()) {
            if (tryCheckForInterruptionAndResolve()) {
                return;
            }

            final SettableFuture<ListenableWorker.Payload> future = SettableFuture.create();
            // Call mWorker.startWork() on the main thread.
            mWorkTaskExecutor.getMainThreadExecutor()
                    .execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mInnerFuture = mWorker.startWork();
                                future.setFuture(mInnerFuture);
                            } catch (Throwable e) {
                                future.setException(e);
                            }

                        }
                    });

            // Avoid synthetic accessors.
            final String workDescription = mWorkDescription;
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        mPayload = future.get();
                    } catch (CancellationException exception) {
                        // Cancellations need to be treated with care here because innerFuture
                        // cancellations will bubble up, and we need to gracefully handle that.
                        Logger.info(TAG, String.format("%s was cancelled", workDescription),
                                exception);
                    } catch (InterruptedException | ExecutionException exception) {
                        Logger.error(TAG,
                                String.format("%s failed because it threw an exception/error",
                                        workDescription), exception);
                    } finally {
                        onWorkFinished();
                    }
                }
            }, mWorkTaskExecutor.getBackgroundExecutor());
        } else {
            resolveIncorrectStatus();
        }
    }

    // Package-private for synthetic accessor.
    void onWorkFinished() {
        assertBackgroundExecutorThread();
        boolean isWorkFinished = false;
        if (!tryCheckForInterruptionAndResolve()) {
            try {
                mWorkDatabase.beginTransaction();
                WorkInfo.State state = mWorkSpecDao.getState(mWorkSpecId);
                if (state == null) {
                    // state can be null here with a REPLACE on beginUniqueWork().
                    // Treat it as a failure, and rescheduleAndResolve() will
                    // turn into a no-op. We still need to notify potential observers
                    // holding on to wake locks on our behalf.
                    resolve(false);
                    isWorkFinished = true;
                } else if (state == RUNNING) {
                    handleResult(mPayload.getResult());
                    // Update state after a call to handleResult()
                    state = mWorkSpecDao.getState(mWorkSpecId);
                    isWorkFinished = state.isFinished();
                } else if (!state.isFinished()) {
                    rescheduleAndResolve();
                }
                mWorkDatabase.setTransactionSuccessful();
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
        // Try to schedule any newly-unblocked workers, and workers requiring rescheduling (such as
        // periodic work using AlarmManager).  This code runs after runWorker() because it should
        // happen in its own transaction.

        // Cancel this work in other schedulers.  For example, if this work was
        // completed by GreedyScheduler, we should make sure JobScheduler is informed
        // that it should remove this job and AlarmManager should remove all related alarms.
        if (mSchedulers != null) {
            if (isWorkFinished) {
                for (Scheduler scheduler : mSchedulers) {
                    scheduler.cancel(mWorkSpecId);
                }
            }
            Schedulers.schedule(mConfiguration, mWorkDatabase, mSchedulers);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void interrupt(boolean cancelled) {
        mInterrupted = true;
        // Resolve WorkerWrapper's future so we do the right thing and setup a reschedule
        // if necessary. mInterrupted is always true here, we don't really care about the return
        // value.
        tryCheckForInterruptionAndResolve();
        if (mInnerFuture != null) {
            // Propagate the cancellations to the inner future.
            mInnerFuture.cancel(true);
        }
        // Worker can be null if run() hasn't been called yet.
        if (mWorker != null) {
            mWorker.stop();
        }
    }

    private void resolveIncorrectStatus() {
        WorkInfo.State status = mWorkSpecDao.getState(mWorkSpecId);
        if (status == RUNNING) {
            Logger.debug(TAG, String.format("Status for %s is RUNNING;"
                    + "not doing any work and rescheduling for later execution", mWorkSpecId));
            resolve(true);
        } else {
            Logger.debug(TAG,
                    String.format("Status for %s is %s; not doing any work", mWorkSpecId, status));
            resolve(false);
        }
    }

    private boolean tryCheckForInterruptionAndResolve() {
        if (mInterrupted) {
            Logger.info(TAG, String.format("Work interrupted for %s", mWorkDescription));
            WorkInfo.State currentState = mWorkSpecDao.getState(mWorkSpecId);
            if (currentState == null) {
                // This can happen because of a beginUniqueWork(..., REPLACE, ...).  Notify the
                // listeners so we can clean up any wake locks, etc.
                resolve(false);
            } else {
                resolve(!currentState.isFinished());
            }
            return true;
        }
        return false;
    }

    private void resolve(final boolean needsReschedule) {
        try {
            // IMPORTANT: We are using a transaction here as to ensure that we have some guarantees
            // about the state of the world before we disable RescheduleReceiver.

            // Check to see if there is more work to be done. If there is no more work, then
            // disable RescheduleReceiver. Using a transaction here, as there could be more than
            // one thread looking at the list of eligible WorkSpecs.
            mWorkDatabase.beginTransaction();
            List<String> unfinishedWork = mWorkDatabase.workSpecDao().getAllUnfinishedWork();
            boolean noMoreWork = unfinishedWork == null || unfinishedWork.isEmpty();
            if (noMoreWork) {
                PackageManagerHelper.setComponentEnabled(
                        mAppContext, RescheduleReceiver.class, false);
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }

        mFuture.set(needsReschedule);
    }

    private void handleResult(Result result) {
        switch (result) {
            case SUCCESS: {
                Logger.info(TAG, String.format("Worker result SUCCESS for %s", mWorkDescription));
                if (mWorkSpec.isPeriodic()) {
                    resetPeriodicAndResolve();
                } else {
                    setSucceededAndResolve();
                }
                break;
            }

            case RETRY: {
                Logger.info(TAG, String.format("Worker result RETRY for %s", mWorkDescription));
                rescheduleAndResolve();
                break;
            }

            case FAILURE:
            default: {
                Logger.info(TAG, String.format("Worker result FAILURE for %s", mWorkDescription));
                if (mWorkSpec.isPeriodic()) {
                    resetPeriodicAndResolve();
                } else {
                    setFailedAndResolve();
                }
            }
        }
    }

    private boolean trySetRunning() {
        boolean setToRunning = false;
        mWorkDatabase.beginTransaction();
        try {
            WorkInfo.State currentState = mWorkSpecDao.getState(mWorkSpecId);
            if (currentState == ENQUEUED) {
                mWorkSpecDao.setState(RUNNING, mWorkSpecId);
                mWorkSpecDao.incrementWorkSpecRunAttemptCount(mWorkSpecId);
                setToRunning = true;
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
        return setToRunning;
    }

    private void setFailedAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            recursivelyFailWorkAndDependents(mWorkSpecId);

            // Try to set the output for the failed work but check if the Payload exists; this could
            // happen if we couldn't find or create the worker class.
            if (mPayload != null) {
                // Update Data as necessary.
                Data output = mPayload.getOutputData();
                mWorkSpecDao.setOutput(mWorkSpecId, output);
            }

            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            resolve(false);
        }
    }

    private void recursivelyFailWorkAndDependents(String workSpecId) {
        List<String> dependentIds = mDependencyDao.getDependentWorkIds(workSpecId);
        for (String id : dependentIds) {
            recursivelyFailWorkAndDependents(id);
        }

        // Don't fail already cancelled work.
        if (mWorkSpecDao.getState(workSpecId) != CANCELLED) {
            mWorkSpecDao.setState(FAILED, workSpecId);
        }
    }

    private void rescheduleAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, System.currentTimeMillis());
            if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
                // We only need to reset the schedule_requested_at bit for the AlarmManager
                // implementation because AlarmManager does not know about periodic WorkRequests.
                // Otherwise we end up double scheduling the Worker with an identical jobId, and
                // JobScheduler treats it as the first schedule for a PeriodicWorker. With the
                // AlarmManager implementation, this is not an problem as AlarmManager only cares
                // about the actual alarm itself.

                mWorkSpecDao.markWorkSpecScheduled(mWorkSpecId, SCHEDULE_NOT_REQUESTED_YET);
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            resolve(true);
        }
    }

    private void resetPeriodicAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            long currentPeriodStartTime = mWorkSpec.periodStartTime;
            long now = System.currentTimeMillis();
            long potentialNextStartTime = currentPeriodStartTime + mWorkSpec.intervalDuration;
            // The system clock may have been changed such that the potentialNextStartTime is still
            // in the past. When this happens, we need to move the nextPeriodStartTime to the
            // present. This way, the Schedulers will correctly schedule the next instance of the
            // PeriodicWork in the future.
            long nextPeriodStartTime = Math.max(now, potentialNextStartTime);
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, nextPeriodStartTime);
            mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
            mWorkSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
                // We only need to reset the schedule_requested_at bit for the AlarmManager
                // implementation because AlarmManager does not know about periodic WorkRequests.
                // Otherwise we end up double scheduling the Worker with an identical jobId, and
                // JobScheduler treats it as the first schedule for a PeriodicWorker. With the
                // AlarmManager implementation, this is not an problem as AlarmManager only cares
                // about the actual alarm itself.

                // We need to tell the schedulers that this WorkSpec is no longer occupying a slot.
                mWorkSpecDao.markWorkSpecScheduled(mWorkSpecId, SCHEDULE_NOT_REQUESTED_YET);
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            resolve(false);
        }
    }

    private void setSucceededAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setState(SUCCEEDED, mWorkSpecId);

            // Update Data as necessary.
            Data output = mPayload.getOutputData();
            mWorkSpecDao.setOutput(mWorkSpecId, output);

            // Unblock Dependencies and set Period Start Time
            long currentTimeMillis = System.currentTimeMillis();
            List<String> dependentWorkIds = mDependencyDao.getDependentWorkIds(mWorkSpecId);
            for (String dependentWorkId : dependentWorkIds) {
                if (mDependencyDao.hasCompletedAllPrerequisites(dependentWorkId)) {
                    Logger.info(TAG,
                            String.format("Setting status to enqueued for %s", dependentWorkId));
                    mWorkSpecDao.setState(ENQUEUED, dependentWorkId);
                    mWorkSpecDao.setPeriodStartTime(dependentWorkId, currentTimeMillis);
                }
            }

            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            resolve(false);
        }
    }

    private void assertBackgroundExecutorThread() {
        if (mWorkTaskExecutor.getBackgroundExecutorThread() != Thread.currentThread()) {
            throw new IllegalStateException(
                    "Needs to be executed on the Background executor thread.");
        }
    }

    private String createWorkDescription(List<String> tags) {
        StringBuilder sb = new StringBuilder("Work [ id=")
                .append(mWorkSpecId)
                .append(", tags={ ");

        boolean first = true;
        for (String tag : tags) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(tag);
        }
        sb.append(" } ]");

        return sb.toString();
    }

    /**
     * Builder class for {@link WorkerWrapper}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Builder {

        @NonNull Context mAppContext;
        @Nullable
        ListenableWorker mWorker;
        @NonNull TaskExecutor mWorkTaskExecutor;
        @NonNull Configuration mConfiguration;
        @NonNull WorkDatabase mWorkDatabase;
        @NonNull String mWorkSpecId;
        List<Scheduler> mSchedulers;
        @NonNull
        WorkerParameters.RuntimeExtras mRuntimeExtras = new WorkerParameters.RuntimeExtras();

        public Builder(@NonNull Context context,
                @NonNull Configuration configuration,
                @NonNull TaskExecutor workTaskExecutor,
                @NonNull WorkDatabase database,
                @NonNull String workSpecId) {
            mAppContext = context.getApplicationContext();
            mWorkTaskExecutor = workTaskExecutor;
            mConfiguration = configuration;
            mWorkDatabase = database;
            mWorkSpecId = workSpecId;
        }

        /**
         * @param schedulers The list of {@link Scheduler}s used for scheduling {@link Worker}s.
         * @return The instance of {@link Builder} for chaining.
         */
        public Builder withSchedulers(List<Scheduler> schedulers) {
            mSchedulers = schedulers;
            return this;
        }

        /**
         * @param runtimeExtras The {@link WorkerParameters.RuntimeExtras} for the {@link Worker};
         *                      if this is {@code null}, it will be ignored and the default value
         *                      will be retained.
         * @return The instance of {@link Builder} for chaining.
         */
        public Builder withRuntimeExtras(WorkerParameters.RuntimeExtras runtimeExtras) {
            if (runtimeExtras != null) {
                mRuntimeExtras = runtimeExtras;
            }
            return this;
        }

        /**
         * @param worker The instance of {@link ListenableWorker} to be executed by
         * {@link WorkerWrapper}. Useful in the context of testing.
         * @return The instance of {@link Builder} for chaining.
         */
        @VisibleForTesting
        public Builder withWorker(ListenableWorker worker) {
            mWorker = worker;
            return this;
        }

        /**
         * @return The instance of {@link WorkerWrapper}.
         */
        public WorkerWrapper build() {
            return new WorkerWrapper(this);
        }
    }
}
