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

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import androidx.work.impl.Extras;

import java.util.Set;

/**
 * The basic unit of work.
 */
public abstract class Worker {

    public enum WorkerResult {
        SUCCESS,
        FAILURE,
        RETRY
    }

    private @NonNull Context mAppContext;
    private @NonNull String mId;
    private @NonNull Extras mExtras;
    private @NonNull Data mOutputData = Data.EMPTY;
    private volatile boolean mStopped;

    public final @NonNull Context getApplicationContext() {
        return mAppContext;
    }

    public final @NonNull String getId() {
        return mId;
    }

    public final @NonNull Data getInputData() {
        return mExtras.getInputData();
    }

    public final @NonNull Set<String> getTags() {
        return mExtras.getTags();
    }

    @RequiresApi(24)
    public final @Nullable Uri[] getTriggeredContentUris() {
        Extras.RuntimeExtras runtimeExtras = mExtras.getRuntimeExtras();
        return (runtimeExtras == null) ? null : runtimeExtras.triggeredContentUris;
    }

    @RequiresApi(24)
    public final @Nullable String[] getTriggeredContentAuthorities() {
        Extras.RuntimeExtras runtimeExtras = mExtras.getRuntimeExtras();
        return (runtimeExtras == null) ? null : runtimeExtras.triggeredContentAuthorities;
    }

    /**
     * Override this method to do your actual background processing.
     *
     * @return The result of the work, corresponding to a {@link WorkerResult} value.  If a
     * different value is returned, the result shall be defaulted to
     * {@link Worker.WorkerResult#FAILURE}.
     */
    @WorkerThread
    public abstract @NonNull WorkerResult doWork();

    /**
     * Call this method to pass an {@link Data} object to {@link Worker} that is
     * dependent on this one.
     *
     * Note that if there are multiple {@link Worker}s that contribute to the target, the
     * Data will be merged together, so it is up to the developer to make sure that keys are
     * unique.  New values and types will clobber old values and types, and if there are multiple
     * parent Workers of a child Worker, the order of clobbering may not be deterministic.
     *
     * This method is invoked after {@link #doWork()} returns {@link Worker.WorkerResult#SUCCESS}
     * and there are chained jobs available.
     *
     * For example, if you had this structure:
     *
     * {@code WorkManager.getInstance(context)
     *             .enqueueWithDefaults(WorkerA.class, WorkerB.class)
     *             .then(WorkerC.class)
     *             .enqueue()}
     *
     * This method would be called for both WorkerA and WorkerB after their successful completion,
     * modifying the input Data for WorkerC.
     *
     * @param outputData An {@link Data} object that will be merged into the input Data of any
     *                   OneTimeWorkRequest that is dependent on this one, or {@code null} if there
     *                   is nothing to contribute
     */
    public final void setOutputData(@NonNull Data outputData) {
        mOutputData = outputData;
    }

    public final @NonNull Data getOutputData() {
        return mOutputData;
    }

    /**
     * Returns {@code true} if this Worker has been told to stop.  This could be because of an
     * explicit cancellation signal by the user, or because the system has decided to preempt the
     * task. In these cases, the results of the work will be ignored by WorkManager and it is safe
     * to stop the computation.
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
     * This method is invoked when this Worker has been told to stop.  This could happen due
     * to an explicit cancellation signal by the user, or because the system has decided to preempt
     * the task.  In these cases, the results of the work will be ignored by WorkManager.  All
     * processing in this method should be lightweight - there are no contractual guarantees about
     * which thread will invoke this call, so this should not be a long-running or blocking
     * operation.
     */
    public void onStopped() {
        // Do nothing by default.
    }

    @Keep
    private void internalInit(
            @NonNull Context appContext,
            @NonNull String id,
            @NonNull Extras extras) {
        mAppContext = appContext;
        mId = id;
        mExtras = extras;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Extras getExtras() {
        return mExtras;
    }
}
