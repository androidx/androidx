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

import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.InputMerger;
import androidx.work.InputMergerFactory;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.background.systemalarm.RescheduleReceiver;
import androidx.work.impl.foreground.ForegroundProcessor;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTagDao;
import androidx.work.impl.utils.PackageManagerHelper;
import androidx.work.impl.utils.WorkForegroundUpdater;
import androidx.work.impl.utils.WorkProgressUpdater;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.LinkedList;
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

    // Avoid Synthetic accessor
    static final String TAG = Logger.tagWithPrefix("WorkerWrapper");

    // Avoid Synthetic accessor
    Context mAppContext;
    private String mWorkSpecId;
    private List<Scheduler> mSchedulers;
    private WorkerParameters.RuntimeExtras mRuntimeExtras;
    // Avoid Synthetic accessor
    WorkSpec mWorkSpec;
    ListenableWorker mWorker;

    // Package-private for synthetic accessor.
    @NonNull
    ListenableWorker.Result mResult = ListenableWorker.Result.failure();

    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private ForegroundProcessor mForegroundProcessor;
    private WorkDatabase mWorkDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private WorkTagDao mWorkTagDao;

    private List<String> mTags;
    private String mWorkDescription;

    // Synthetic access
    @NonNull
    SettableFuture<Boolean> mFuture = SettableFuture.create();

    // Package-private for synthetic accessor.
    @Nullable ListenableFuture<ListenableWorker.Result> mInnerFuture = null;

    private volatile boolean mInterrupted;

    // Package-private for synthetic accessor.
    WorkerWrapper(@NonNull Builder builder) {
        mAppContext = builder.mAppContext;
        mWorkTaskExecutor = builder.mWorkTaskExecutor;
        mForegroundProcessor = builder.mForegroundProcessor;
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
                Logger.get().error(
                        TAG,
                        String.format("Didn't find WorkSpec for id %s", mWorkSpecId));
                resolve(false);
                mWorkDatabase.setTransactionSuccessful();
                return;
            }

            // Do a quick check to make sure we don't need to bail out in case this work is already
            // running, finished, or is blocked.
            if (mWorkSpec.state != ENQUEUED) {
                resolveIncorrectStatus();
                mWorkDatabase.setTransactionSuccessful();
                Logger.get().debug(TAG,
                        String.format("%s is not in ENQUEUED state. Nothing more to do.",
                                mWorkSpec.workerClassName));
                return;
            }

            // Case 1:
            // Ensure that Workers that are backed off are only executed when they are supposed to.
            // GreedyScheduler can schedule WorkSpecs that have already been backed off because
            // it is holding on to snapshots of WorkSpecs. So WorkerWrapper needs to determine
            // if the ListenableWorker is actually eligible to execute at this point in time.

            // Case 2:
            // On API 23, we double scheduler Workers because JobScheduler prefers batching.
            // So is the Work is periodic, we only need to execute it once per interval.
            // Also potential bugs in the platform may cause a Job to run more than once.

            if (mWorkSpec.isPeriodic() || mWorkSpec.isBackedOff()) {
                long now = System.currentTimeMillis();
                // Allow first run of a PeriodicWorkRequest
                // to go through. This is because when periodStartTime=0;
                // calculateNextRunTime() always > now.
                // For more information refer to b/124274584
                boolean isFirstRun = mWorkSpec.periodStartTime == 0;
                if (!isFirstRun && now < mWorkSpec.calculateNextRunTime()) {
                    Logger.get().debug(TAG,
                            String.format(
                                    "Delaying execution for %s because it is being executed "
                                            + "before schedule.",
                                    mWorkSpec.workerClassName));
                    // For AlarmManager implementation we need to reschedule this kind  of Work.
                    // This is not a problem for JobScheduler because we will only reschedule
                    // work if JobScheduler is unaware of a jobId.
                    resolve(true);
                    mWorkDatabase.setTransactionSuccessful();
                    return;
                }
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
            InputMergerFactory inputMergerFactory = mConfiguration.getInputMergerFactory();
            String inputMergerClassName = mWorkSpec.inputMergerClassName;
            InputMerger inputMerger =
                    inputMergerFactory.createInputMergerWithDefaultFallback(inputMergerClassName);
            if (inputMerger == null) {
                Logger.get().error(TAG, String.format("Could not create Input Merger %s",
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
                mConfiguration.getWorkerFactory(),
                new WorkProgressUpdater(mWorkDatabase, mWorkTaskExecutor),
                new WorkForegroundUpdater(mWorkDatabase, mForegroundProcessor, mWorkTaskExecutor));

        // Not always creating a worker here, as the WorkerWrapper.Builder can set a worker override
        // in test mode.
        if (mWorker == null) {
            mWorker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                    mAppContext,
                    mWorkSpec.workerClassName,
                    params);
        }

        if (mWorker == null) {
            Logger.get().error(TAG,
                    String.format("Could not create Worker %s", mWorkSpec.workerClassName));
            setFailedAndResolve();
            return;
        }

        if (mWorker.isUsed()) {
            Logger.get().error(TAG,
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

            final SettableFuture<ListenableWorker.Result> future = SettableFuture.create();
            // Call mWorker.startWork() on the main thread.
            mWorkTaskExecutor.getMainThreadExecutor()
                    .execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Logger.get().debug(TAG, String.format("Starting work for %s",
                                        mWorkSpec.workerClassName));
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
                @SuppressLint("SyntheticAccessor")
                public void run() {
                    try {
                        // If the ListenableWorker returns a null result treat it as a failure.
                        ListenableWorker.Result result = future.get();
                        if (result == null) {
                            Logger.get().error(TAG, String.format(
                                    "%s returned a null result. Treating it as a failure.",
                                    mWorkSpec.workerClassName));
                        } else {
                            Logger.get().debug(TAG, String.format("%s returned a %s result.",
                                    mWorkSpec.workerClassName, result));
                            mResult = result;
                        }
                    } catch (CancellationException exception) {
                        // Cancellations need to be treated with care here because innerFuture
                        // cancellations will bubble up, and we need to gracefully handle that.
                        Logger.get().info(TAG, String.format("%s was cancelled", workDescription),
                                exception);
                    } catch (InterruptedException | ExecutionException exception) {
                        Logger.get().error(TAG,
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
        if (!tryCheckForInterruptionAndResolve()) {
            mWorkDatabase.beginTransaction();
            try {
                WorkInfo.State state = mWorkSpecDao.getState(mWorkSpecId);
                mWorkDatabase.workProgressDao().delete(mWorkSpecId);
                if (state == null) {
                    // state can be null here with a REPLACE on beginUniqueWork().
                    // Treat it as a failure, and rescheduleAndResolve() will
                    // turn into a no-op. We still need to notify potential observers
                    // holding on to wake locks on our behalf.
                    resolve(false);
                } else if (state == RUNNING) {
                    handleResult(mResult);
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
        // handled by GreedyScheduler, we should make sure JobScheduler is informed
        // that it should remove this job and AlarmManager should remove all related alarms.
        if (mSchedulers != null) {
            for (Scheduler scheduler : mSchedulers) {
                scheduler.cancel(mWorkSpecId);
            }
            Schedulers.schedule(mConfiguration, mWorkDatabase, mSchedulers);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void interrupt() {
        mInterrupted = true;
        // Resolve WorkerWrapper's future so we do the right thing and setup a reschedule
        // if necessary. mInterrupted is always true here, we don't really care about the return
        // value.
        tryCheckForInterruptionAndResolve();
        boolean isDone = false;
        if (mInnerFuture != null) {
            // Propagate the cancellations to the inner future.
            isDone = mInnerFuture.isDone();
            mInnerFuture.cancel(true);
        }
        // Worker can be null if run() hasn't been called yet
        if (mWorker != null && !isDone) {
            mWorker.stop();
        } else {
            String message =
                    String.format("WorkSpec %s is already done. Not interrupting.", mWorkSpec);
            Logger.get().debug(TAG, message);
        }
    }

    private void resolveIncorrectStatus() {
        WorkInfo.State status = mWorkSpecDao.getState(mWorkSpecId);
        if (status == RUNNING) {
            Logger.get().debug(TAG, String.format("Status for %s is RUNNING;"
                    + "not doing any work and rescheduling for later execution", mWorkSpecId));
            resolve(true);
        } else {
            Logger.get().debug(TAG,
                    String.format("Status for %s is %s; not doing any work", mWorkSpecId, status));
            resolve(false);
        }
    }

    private boolean tryCheckForInterruptionAndResolve() {
        // Interruptions can happen when:
        // An explicit cancel* signal
        // A change in constraint, which causes WorkManager to stop the Worker.
        // Worker exceeding a 10 min execution window.
        // One scheduler completing a Worker, and telling other Schedulers to cleanup.
        if (mInterrupted) {
            Logger.get().debug(TAG, String.format("Work interrupted for %s", mWorkDescription));
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
        mWorkDatabase.beginTransaction();
        try {
            // IMPORTANT: We are using a transaction here as to ensure that we have some guarantees
            // about the state of the world before we disable RescheduleReceiver.

            // Check to see if there is more work to be done. If there is no more work, then
            // disable RescheduleReceiver. Using a transaction here, as there could be more than
            // one thread looking at the list of eligible WorkSpecs.
            boolean hasUnfinishedWork = mWorkDatabase.workSpecDao().hasUnfinishedWork();
            if (!hasUnfinishedWork) {
                PackageManagerHelper.setComponentEnabled(
                        mAppContext, RescheduleReceiver.class, false);
            }
            if (needsReschedule) {
                // Set state to ENQUEUED again.
                // Reset scheduled state so its picked up by background schedulers again.
                mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
                mWorkSpecDao.markWorkSpecScheduled(mWorkSpecId, SCHEDULE_NOT_REQUESTED_YET);
            }
            if (mWorkSpec != null && mWorker != null && mWorker.isRunInForeground()) {
                mForegroundProcessor.stopForeground(mWorkSpecId);
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
        mFuture.set(needsReschedule);
    }

    private void handleResult(ListenableWorker.Result result) {
        if (result instanceof ListenableWorker.Result.Success) {
            Logger.get().info(
                    TAG,
                    String.format("Worker result SUCCESS for %s", mWorkDescription));
            if (mWorkSpec.isPeriodic()) {
                resetPeriodicAndResolve();
            } else {
                setSucceededAndResolve();
            }

        } else if (result instanceof ListenableWorker.Result.Retry) {
            Logger.get().info(
                    TAG,
                    String.format("Worker result RETRY for %s", mWorkDescription));
            rescheduleAndResolve();
        } else {
            Logger.get().info(
                    TAG,
                    String.format("Worker result FAILURE for %s", mWorkDescription));
            if (mWorkSpec.isPeriodic()) {
                resetPeriodicAndResolve();
            } else {
                setFailedAndResolve();
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

    @VisibleForTesting
    void setFailedAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            iterativelyFailWorkAndDependents(mWorkSpecId);
            ListenableWorker.Result.Failure failure = (ListenableWorker.Result.Failure) mResult;
            // Update Data as necessary.
            Data output = failure.getOutputData();
            mWorkSpecDao.setOutput(mWorkSpecId, output);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            resolve(false);
        }
    }

    private void iterativelyFailWorkAndDependents(String workSpecId) {
        @SuppressWarnings("JdkObsolete") // TODO(b/141962522): Suppressed during upgrade to AGP 3.6.
        LinkedList<String> idsToProcess = new LinkedList<>();
        idsToProcess.add(workSpecId);
        while (!idsToProcess.isEmpty()) {
            String id = idsToProcess.remove();
            // Don't fail already cancelled work.
            if (mWorkSpecDao.getState(id) != CANCELLED) {
                mWorkSpecDao.setState(FAILED, id);
            }
            idsToProcess.addAll(mDependencyDao.getDependentWorkIds(id));
        }
    }

    private void rescheduleAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, System.currentTimeMillis());
            mWorkSpecDao.markWorkSpecScheduled(mWorkSpecId, SCHEDULE_NOT_REQUESTED_YET);
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
            resolve(true);
        }
    }

    private void resetPeriodicAndResolve() {
        mWorkDatabase.beginTransaction();
        try {
            // The system clock may have been changed such that the periodStartTime was in the past.
            // Therefore we always use the current time to determine the next run time of a Worker.
            // This way, the Schedulers will correctly schedule the next instance of the
            // PeriodicWork in the future. This happens in calculateNextRunTime() in WorkSpec.
            mWorkSpecDao.setPeriodStartTime(mWorkSpecId, System.currentTimeMillis());
            mWorkSpecDao.setState(ENQUEUED, mWorkSpecId);
            mWorkSpecDao.resetWorkSpecRunAttemptCount(mWorkSpecId);
            mWorkSpecDao.markWorkSpecScheduled(mWorkSpecId, SCHEDULE_NOT_REQUESTED_YET);
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
            ListenableWorker.Result.Success success = (ListenableWorker.Result.Success) mResult;
            // Update Data as necessary.
            Data output = success.getOutputData();
            mWorkSpecDao.setOutput(mWorkSpecId, output);

            // Unblock Dependencies and set Period Start Time
            long currentTimeMillis = System.currentTimeMillis();
            List<String> dependentWorkIds = mDependencyDao.getDependentWorkIds(mWorkSpecId);
            for (String dependentWorkId : dependentWorkIds) {
                if (mWorkSpecDao.getState(dependentWorkId) == BLOCKED
                        && mDependencyDao.hasCompletedAllPrerequisites(dependentWorkId)) {
                    Logger.get().info(TAG,
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
        @NonNull ForegroundProcessor mForegroundProcessor;
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
                @NonNull ForegroundProcessor foregroundProcessor,
                @NonNull WorkDatabase database,
                @NonNull String workSpecId) {
            mAppContext = context.getApplicationContext();
            mWorkTaskExecutor = workTaskExecutor;
            mForegroundProcessor = foregroundProcessor;
            mConfiguration = configuration;
            mWorkDatabase = database;
            mWorkSpecId = workSpecId;
        }

        /**
         * @param schedulers The list of {@link Scheduler}s used for scheduling {@link Worker}s.
         * @return The instance of {@link Builder} for chaining.
         */
        @NonNull
        public Builder withSchedulers(@NonNull List<Scheduler> schedulers) {
            mSchedulers = schedulers;
            return this;
        }

        /**
         * @param runtimeExtras The {@link WorkerParameters.RuntimeExtras} for the {@link Worker};
         *                      if this is {@code null}, it will be ignored and the default value
         *                      will be retained.
         * @return The instance of {@link Builder} for chaining.
         */
        @NonNull
        public Builder withRuntimeExtras(@Nullable WorkerParameters.RuntimeExtras runtimeExtras) {
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
        @NonNull
        @VisibleForTesting
        public Builder withWorker(@NonNull ListenableWorker worker) {
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
