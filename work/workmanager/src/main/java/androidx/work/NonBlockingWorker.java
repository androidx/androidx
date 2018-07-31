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
import android.net.Network;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import androidx.work.impl.Extras;

import java.util.Set;
import java.util.UUID;

/**
 * The basic object that performs work.  Worker classes are instantiated at runtime by
 * {@link WorkManager} and the {@link #onStartWork(WorkFinishedCallback)} method is called on
 * the background thread. In case the work is preempted for any reason, the same instance of
 * {@link NonBlockingWorker} is not reused. This means that
 * {@link #onStartWork(WorkFinishedCallback)} is called exactly once per {@link NonBlockingWorker}
 * instance. The {@link NonBlockingWorker} signals work completion by using
 * {@link WorkFinishedCallback}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class NonBlockingWorker implements WorkFinishedCallback {

    @SuppressWarnings("NullableProblems")   // Set by internalInit
    private @NonNull Context mAppContext;

    @SuppressWarnings("NullableProblems")   // Set by internalInit
    private @NonNull UUID mId;

    @SuppressWarnings("NullableProblems")   // Set by internalInit
    private @NonNull Extras mExtras;

    private volatile boolean mStopped;
    private volatile boolean mCancelled;

    private @NonNull volatile Data mOutputData = Data.EMPTY;
    private @NonNull volatile Worker.Result mResult = Worker.Result.FAILURE;

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
        return mId;
    }

    /**
     * Gets the input data.  Note that in the case that there are multiple prerequisites for this
     * Worker, the input data has been run through an {@link InputMerger}.
     *
     * @return The input data for this work
     * @see OneTimeWorkRequest.Builder#setInputMerger(Class)
     */
    public final @NonNull Data getInputData() {
        return mExtras.getInputData();
    }

    /**
     * Gets a {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}.
     *
     * @return The {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}
     * @see WorkRequest.Builder#addTag(String)
     */
    public final @NonNull Set<String> getTags() {
        return mExtras.getTags();
    }

    /**
     * Gets the array of content {@link android.net.Uri}s that caused this Worker to execute
     *
     * @return The array of content {@link android.net.Uri}s that caused this Worker to execute
     * @see Constraints.Builder#addContentUriTrigger(android.net.Uri, boolean)
     */
    @RequiresApi(24)
    public final @Nullable Uri[] getTriggeredContentUris() {
        Extras.RuntimeExtras runtimeExtras = mExtras.getRuntimeExtras();
        return (runtimeExtras == null) ? null : runtimeExtras.triggeredContentUris;
    }

    /**
     * Gets the array of content authorities that caused this Worker to execute
     *
     * @return The array of content authorities that caused this Worker to execute
     */
    @RequiresApi(24)
    public final @Nullable String[] getTriggeredContentAuthorities() {
        Extras.RuntimeExtras runtimeExtras = mExtras.getRuntimeExtras();
        return (runtimeExtras == null) ? null : runtimeExtras.triggeredContentAuthorities;
    }

    /**
     * Gets the {@link android.net.Network} to use for this Worker.
     * This method returns {@code null} if there is no network needed for this work request.
     *
     * @return The {@link android.net.Network} specified by the OS to be used with this Worker
     */
    @RequiresApi(28)
    public final @Nullable Network getNetwork() {
        Extras.RuntimeExtras runtimeExtras = mExtras.getRuntimeExtras();
        return (runtimeExtras == null) ? null : runtimeExtras.network;
    }

    /**
     * Gets the current run attempt count for this work.
     *
     * @return The current run attempt count for this work.
     */
    public final int getRunAttemptCount() {
        return mExtras.getRunAttemptCount();
    }

    /**
     * Override this method to do your actual background processing.
     * Typical flow involves, starting the execution of work on a background thread, and notifying
     * completion via the completion callback WorkFinishedCallback.
     *
     * @param callback The WorkFinishedCallback that helps signal work completion.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @WorkerThread
    public abstract void onStartWork(@NonNull WorkFinishedCallback callback);

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
     * Returns {@code true} if this Worker has been told to stop and explicitly informed that it is
     * cancelled and will never execute again.  If {@link #isStopped()} returns {@code true} but
     * this method returns {@code false}, that means the system has decided to preempt the task.
     * <p>
     * Note that it is almost never sufficient to check only this method; its value is only
     * meaningful when {@link #isStopped()} returns {@code true}.
     *
     * @return {@code true} if this work operation has been cancelled
     */
    public final boolean isCancelled() {
        return mCancelled;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void stop(boolean cancelled) {
        mStopped = true;
        mCancelled = cancelled;
        onStopped(cancelled);
    }

    /**
     * This method is invoked when this Worker has been told to stop.  This could happen due
     * to an explicit cancellation signal by the user, or because the system has decided to preempt
     * the task.  In these cases, the results of the work will be ignored by WorkManager.  All
     * processing in this method should be lightweight - there are no contractual guarantees about
     * which thread will invoke this call, so this should not be a long-running or blocking
     * operation.
     *
     * @param cancelled If {@code true}, the work has been explicitly cancelled
     */
    public void onStopped(boolean cancelled) {
        // Do nothing by default.
    }

    /**
     * Call this method to pass a {@link Data} object as the output of this {@link Worker}.  This
     * result can be observed and passed to Workers that are dependent on this one.
     *
     * In cases like where two or more {@link OneTimeWorkRequest}s share a dependent WorkRequest,
     * their Data will be merged together using an {@link InputMerger}.  The default InputMerger is
     * {@link OverwritingInputMerger}, unless otherwise specified using the
     * {@link OneTimeWorkRequest.Builder#setInputMerger(Class)} method.
     * <p>
     * This method is invoked after {@link #onStartWork(WorkFinishedCallback)}
     * returns {@link Worker.Result#SUCCESS} or {@link Worker.Result#FAILURE}.
     * <p>
     * For example, if you had this structure:
     * <pre>
     * {@code WorkManager.getInstance(context)
     *             .beginWith(workRequestA, workRequestB)
     *             .then(workRequestC)
     *             .enqueue()}</pre>
     *
     * This method would be called for both {@code workRequestA} and {@code workRequestB} after
     * their completion, modifying the input Data for {@code workRequestC}.
     *
     * @param outputData An {@link Data} object that will be merged into the input Data of any
     *                   OneTimeWorkRequest that is dependent on this one, or {@link Data#EMPTY} if
     *                   there is nothing to contribute
     */
    public void setOutputData(@NonNull Data outputData) {
        mOutputData = outputData;
    }

    /**
     * @return the output {@link Data} set by the {@link Worker}.
     */
    public Data getOutputData() {
        return mOutputData;
    }

    /**
     * @return the {@link Worker.Result} of executing the {@link Worker}.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Worker.Result getResult() {
        return mResult;
    }

    /**
     * Sets the {@link Worker.Result} of the {@link Worker}s execution.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setResult(@NonNull Worker.Result result) {
        mResult = result;
    }

    /**
     * The completion callback for {@link NonBlockingWorker}.
     *
     * @param result the {@link Worker.Result} of the {@link NonBlockingWorker}'s execution.
     * @hide
     */
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void onWorkFinished(@NonNull Worker.Result result) {
        setResult(result);
        Extras.RuntimeExtras runtimeExtras = mExtras.getRuntimeExtras();
        if (runtimeExtras.mExecutionListener != null) {
            // The ExecutionListener is just a way to communicate with the WorkerWrapper.
            // The isSuccessful & needsReschedule flags are computed based on the pre and post
            // completion state of the NonBlockingWorker.
            boolean isSuccessful = result == Worker.Result.SUCCESS;
            boolean needsReschedule = result == Worker.Result.RETRY;
            runtimeExtras.mExecutionListener
                    .onExecuted(mId.toString(), isSuccessful, needsReschedule);
        }
    }

    @Keep
    @SuppressWarnings("unused")
    protected void internalInit(
            @NonNull Context appContext,
            @NonNull UUID id,
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
