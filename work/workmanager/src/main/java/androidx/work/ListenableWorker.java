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

package androidx.work;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Network;
import android.net.Uri;

import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A class that can perform work asynchronously in {@link WorkManager}.  For most cases, we
 * recommend using {@link Worker}, which offers a simple synchronous API that is executed on a
 * pre-specified background thread.
 * <p>
 * ListenableWorker classes are instantiated at runtime by the {@link WorkerFactory} specified in
 * the {@link Configuration}.  The {@link #startWork()} method is called on the main thread.
 * <p>
 * In case the work is preempted and later restarted for any reason, a new instance of
 * ListenableWorker is created. This means that {@code startWork} is called exactly once per
 * ListenableWorker instance.  A new ListenableWorker is created if a unit of work needs to be
 * rerun.
 * <p>
 * A ListenableWorker is given a maximum of ten minutes to finish its execution and return a
 * {@link Result}.  After this time has expired, the worker will be signalled to stop and its
 * {@link ListenableFuture} will be cancelled.
 * <p>
 * Exercise caution when <a href="WorkManager.html#worker_class_names">renaming or removing
 * ListenableWorkers</a> from your codebase.
 */

public abstract class ListenableWorker {

    private @NonNull Context mAppContext;
    private @NonNull WorkerParameters mWorkerParams;

    private volatile boolean mStopped;

    private boolean mUsed;
    private boolean mRunInForeground;

    /**
     * @param appContext The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    @Keep
    @SuppressLint("BanKeepAnnotation")
    public ListenableWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        // Actually make sure we don't get nulls.
        if (appContext == null) {
            throw new IllegalArgumentException("Application Context is null");
        }

        if (workerParams == null) {
            throw new IllegalArgumentException("WorkerParameters is null");
        }

        mAppContext = appContext;
        mWorkerParams = workerParams;
    }

    /**
     * Gets the application {@link android.content.Context}.
     *
     * @return The application {@link android.content.Context}
     */
    public final @NonNull Context getApplicationContext() {
        return mAppContext;
    }

    /**
     * Gets the ID of the {@link WorkRequest} that created this Worker.
     *
     * @return The ID of the creating {@link WorkRequest}
     */
    public final @NonNull UUID getId() {
        return mWorkerParams.getId();
    }

    /**
     * Gets the input data.  Note that in the case that there are multiple prerequisites for this
     * Worker, the input data has been run through an {@link InputMerger}.
     *
     * @return The input data for this work
     * @see OneTimeWorkRequest.Builder#setInputMerger(Class)
     */
    public final @NonNull Data getInputData() {
        return mWorkerParams.getInputData();
    }

    /**
     * Gets a {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}.
     *
     * @return The {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}
     * @see WorkRequest.Builder#addTag(String)
     */
    public final @NonNull Set<String> getTags() {
        return mWorkerParams.getTags();
    }

    /**
     * Gets the list of content {@link android.net.Uri}s that caused this Worker to execute.  See
     * {@code JobParameters#getTriggeredContentUris()} for relevant {@code JobScheduler} code.
     *
     * @return The list of content {@link android.net.Uri}s that caused this Worker to execute
     * @see Constraints.Builder#addContentUriTrigger(android.net.Uri, boolean)
     */
    @RequiresApi(24)
    public final @NonNull List<Uri> getTriggeredContentUris() {
        return mWorkerParams.getTriggeredContentUris();
    }

    /**
     * Gets the list of content authorities that caused this Worker to execute.  See
     * {@code JobParameters#getTriggeredContentAuthorities()} for relevant {@code JobScheduler}
     * code.
     *
     * @return The list of content authorities that caused this Worker to execute
     */
    @RequiresApi(24)
    public final @NonNull List<String> getTriggeredContentAuthorities() {
        return mWorkerParams.getTriggeredContentAuthorities();
    }

    /**
     * Gets the {@link android.net.Network} to use for this Worker.  This method returns
     * {@code null} if there is no network needed for this work request.
     *
     * @return The {@link android.net.Network} specified by the OS to be used with this Worker
     */
    @RequiresApi(28)
    public final @Nullable Network getNetwork() {
        return mWorkerParams.getNetwork();
    }

    /**
     * Gets the current run attempt count for this work.  Note that for periodic work, this value
     * gets reset between periods.
     *
     * @return The current run attempt count for this work.
     */
    @IntRange(from = 0)
    public final int getRunAttemptCount() {
        return mWorkerParams.getRunAttemptCount();
    }

    /**
     * Override this method to start your actual background processing. This method is called on
     * the main thread.
     * <p>
     * A ListenableWorker has a well defined
     * <a href="https://d.android.com/reference/android/app/job/JobScheduler">execution window</a>
     * to to finish its execution and return a {@link Result}.  After this time has expired, the
     * worker will be signalled to stop and its {@link ListenableFuture} will be cancelled.
     * <p>
     * The future will also be cancelled if this worker is stopped for any reason
     * (see {@link #onStopped()}).
     *
     * @return A {@link ListenableFuture} with the {@link Result} of the computation.  If you
     * cancel this Future, WorkManager will treat this unit of work as failed.
     */
    @MainThread
    public abstract @NonNull ListenableFuture<Result> startWork();

    /**
     * Updates {@link ListenableWorker} progress.
     *
     * @param data The progress {@link Data}
     * @return A {@link ListenableFuture} which resolves after progress is persisted.
     * Cancelling this future is a no-op.
     */
    @NonNull
    public ListenableFuture<Void> setProgressAsync(@NonNull Data data) {
        return mWorkerParams.getProgressUpdater()
                .updateProgress(getApplicationContext(), getId(), data);
    }

    /**
     * This specifies that the {@link WorkRequest} is long-running or otherwise important.  In
     * this case, WorkManager provides a signal to the OS that the process should be kept alive
     * if possible while this work is executing.
     * <p>
     * Calls to {@code setForegroundAsync} *must* complete before a {@link ListenableWorker}
     * signals completion by returning a {@link Result}.
     * <p>
     * Under the hood, WorkManager manages and runs a foreground service on your behalf to
     * execute this WorkRequest, showing the notification provided in
     * {@link ForegroundInfo}.
     * <p>
     * Calling {@code setForegroundAsync} will fail with an
     * {@link IllegalStateException} when the process is subject to foreground
     * service restrictions. Consider using
     * {@link WorkRequest.Builder#setExpedited(OutOfQuotaPolicy)} and
     * {@link ListenableWorker#getForegroundInfoAsync()} instead.
     *
     * @param foregroundInfo The {@link ForegroundInfo}
     * @return A {@link ListenableFuture} which resolves after the {@link ListenableWorker}
     * transitions to running in the context of a foreground {@link android.app.Service}.
     */
    @NonNull
    public final ListenableFuture<Void> setForegroundAsync(@NonNull ForegroundInfo foregroundInfo) {
        mRunInForeground = true;
        return mWorkerParams.getForegroundUpdater()
                .setForegroundAsync(getApplicationContext(), getId(), foregroundInfo);
    }

    /**
     * Return an instance of {@link  ForegroundInfo} if the {@link WorkRequest} is important to
     * the user.  In this case, WorkManager provides a signal to the OS that the process should
     * be kept alive while this work is executing.
     * <p>
     * Prior to Android S, WorkManager manages and runs a foreground service on your behalf to
     * execute the WorkRequest, showing the notification provided in the {@link ForegroundInfo}.
     * To update this notification subsequently, the application can use
     * {@link android.app.NotificationManager}.
     * <p>
     * Starting in Android S and above, WorkManager manages this WorkRequest using an immediate job.
     *
     * @return A {@link ListenableFuture} of {@link ForegroundInfo} instance if the WorkRequest
     * is marked immediate. For more information look at
     * {@link WorkRequest.Builder#setExpedited(OutOfQuotaPolicy)}.
     */
    @NonNull
    @ExperimentalExpeditedWork
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        SettableFuture<ForegroundInfo> future = SettableFuture.create();
        future.setException(new IllegalStateException("Not implemented"));
        return future;
    }

    /**
     * Returns {@code true} if this Worker has been told to stop.  This could be because of an
     * explicit cancellation signal by the user, or because the system has decided to preempt the
     * task. In these cases, the results of the work will be ignored by WorkManager and it is safe
     * to stop the computation.  WorkManager will retry the work at a later time if necessary.
     *
     *
     * @return {@code true} if the work operation has been interrupted
     */
    public final boolean isStopped() {
        return mStopped;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void stop() {
        mStopped = true;
        onStopped();
    }

    /**
     * This method is invoked when this Worker has been told to stop.  At this point, the
     * {@link ListenableFuture} returned by the instance of {@link #startWork()} is
     * also cancelled.  This could happen due to an explicit cancellation signal by the user, or
     * because the system has decided to preempt the task.  In these cases, the results of the
     * work will be ignored by WorkManager.  All processing in this method should be lightweight
     * - there are no contractual guarantees about which thread will invoke this call, so this
     * should not be a long-running or blocking operation.
     */
    public void onStopped() {
        // Do nothing by default.
    }

    /**
     * @return {@code true} if this worker has already been marked as used
     * @see #setUsed()
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final boolean isUsed() {
        return mUsed;
    }

    /**
     * Marks this worker as used to make sure we enforce the policy that workers can only be used
     * once and that WorkerFactories return a new instance each time.
     * @see #isUsed()
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void setUsed() {
        mUsed = true;
    }

    /**
     * @return {@code true} if the {@link ListenableWorker} is running in the context of a
     * foreground {@link android.app.Service}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isRunInForeground() {
        return mRunInForeground;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRunInForeground(boolean runInForeground) {
        mRunInForeground = runInForeground;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Executor getBackgroundExecutor() {
        return mWorkerParams.getBackgroundExecutor();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull TaskExecutor getTaskExecutor() {
        return mWorkerParams.getTaskExecutor();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull WorkerFactory getWorkerFactory() {
        return mWorkerParams.getWorkerFactory();
    }

    /**
     * The result of a {@link ListenableWorker}'s computation. Call {@link #success()},
     * {@link #failure()}, or {@link #retry()} or one of their variants to generate an object
     * indicating what happened in your background work.
     */
    public abstract static class Result {
        /**
         * Returns an instance of {@link Result} that can be used to indicate that the work
         * completed successfully. Any work that depends on this can be executed as long as all of
         * its other dependencies and constraints are met.
         *
         * @return An instance of {@link Result} indicating successful execution of work
         */
        @NonNull
        public static Result success() {
            return new Success();
        }

        /**
         * Returns an instance of {@link Result} that can be used to indicate that the work
         * completed successfully. Any work that depends on this can be executed as long as all of
         * its other dependencies and constraints are met.
         *
         * @param outputData A {@link Data} object that will be merged into the input Data of any
         *                   OneTimeWorkRequest that is dependent on this work
         * @return An instance of {@link Result} indicating successful execution of work
         */
        @NonNull
        public static Result success(@NonNull Data outputData) {
            return new Success(outputData);
        }

        /**
         * Returns an instance of {@link Result} that can be used to indicate that the work
         * encountered a transient failure and should be retried with backoff specified in
         * {@link WorkRequest.Builder#setBackoffCriteria(BackoffPolicy, long, TimeUnit)}.
         *
         * @return An instance of {@link Result} indicating that the work needs to be retried
         */
        @NonNull
        public static Result retry() {
            return new Retry();
        }

        /**
         * Returns an instance of {@link Result} that can be used to indicate that the work
         * completed with a permanent failure. Any work that depends on this will also be marked as
         * failed and will not be run. <b>If you need child workers to run, you need to use
         * {@link #success()} or {@link #success(Data)}</b>; failure indicates a permanent stoppage
         * of the chain of work.
         *
         * @return An instance of {@link Result} indicating failure when executing work
         */
        @NonNull
        public static Result failure() {
            return new Failure();
        }

        /**
         * Returns an instance of {@link Result} that can be used to indicate that the work
         * completed with a permanent failure. Any work that depends on this will also be marked as
         * failed and will not be run. <b>If you need child workers to run, you need to use
         * {@link #success()} or {@link #success(Data)}</b>; failure indicates a permanent stoppage
         * of the chain of work.
         *
         * @param outputData A {@link Data} object that can be used to keep track of why the work
         *                   failed
         * @return An instance of {@link Result} indicating failure when executing work
         */
        @NonNull
        public static Result failure(@NonNull Data outputData) {
            return new Failure(outputData);
        }

        /**
         * @return The output {@link Data} which will be merged into the input {@link Data} of
         * any {@link OneTimeWorkRequest} that is dependent on this work request.
         */
        @NonNull
        public abstract Data getOutputData();

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        Result() {
            // Restricting access to the constructor, to give Result a sealed class
            // like behavior.
        }

        /**
         * Used to indicate that the work completed successfully.  Any work that depends on this
         * can be executed as long as all of its other dependencies and constraints are met.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final class Success extends Result {
            private final Data mOutputData;

            public Success() {
                this(Data.EMPTY);
            }

            /**
             * @param outputData A {@link Data} object that will be merged into the input Data of
             *                   any OneTimeWorkRequest that is dependent on this work
             */
            public Success(@NonNull Data outputData) {
                super();
                mOutputData = outputData;
            }

            @Override
            public @NonNull Data getOutputData() {
                return mOutputData;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Success success = (Success) o;

                return mOutputData.equals(success.mOutputData);
            }

            @Override
            public int hashCode() {
                String name = Success.class.getName();
                return 31 * name.hashCode() + mOutputData.hashCode();
            }

            @Override
            public String toString() {
                return "Success {" + "mOutputData=" + mOutputData + '}';
            }
        }

        /**
         * Used to indicate that the work completed with a permanent failure.  Any work that depends
         * on this will also be marked as failed and will not be run. <b>If you need child workers
         * to run, you need to return {@link Result.Success}</b>; failure indicates a permanent
         * stoppage of the chain of work.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final class Failure extends Result {
            private final Data mOutputData;

            public Failure() {
                this(Data.EMPTY);
            }

            /**
             * @param outputData A {@link Data} object that can be used to keep track of why the
             *                   work failed
             */
            public Failure(@NonNull Data outputData) {
                super();
                mOutputData = outputData;
            }

            @Override
            public @NonNull Data getOutputData() {
                return mOutputData;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Failure failure = (Failure) o;

                return mOutputData.equals(failure.mOutputData);
            }

            @Override
            public int hashCode() {
                String name = Failure.class.getName();
                return 31 * name.hashCode() + mOutputData.hashCode();
            }

            @Override
            public String toString() {
                return "Failure {" +  "mOutputData=" + mOutputData +  '}';
            }
        }

        /**
         * Used to indicate that the work encountered a transient failure and should be retried with
         * backoff specified in
         * {@link WorkRequest.Builder#setBackoffCriteria(BackoffPolicy, long, TimeUnit)}.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final class Retry extends Result {
            public Retry() {
                super();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                // We are treating all instances of Retry as equivalent.
                return o != null && getClass() == o.getClass();
            }

            @Override
            public int hashCode() {
                String name = Retry.class.getName();
                return name.hashCode();
            }

            @NonNull
            @Override
            public Data getOutputData() {
                return Data.EMPTY;
            }

            @Override
            public String toString() {
                return "Retry";
            }
        }
    }
}
