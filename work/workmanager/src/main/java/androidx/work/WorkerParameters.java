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

import android.net.Network;
import android.net.Uri;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Setup parameters for a {@link ListenableWorker}.
 */

public final class WorkerParameters {

    private @NonNull UUID mId;
    private @NonNull Data mInputData;
    private @NonNull Set<String> mTags;
    private @NonNull RuntimeExtras mRuntimeExtras;
    private int mRunAttemptCount;
    private @NonNull Executor mBackgroundExecutor;
    private @NonNull TaskExecutor mWorkTaskExecutor;
    private @NonNull WorkerFactory mWorkerFactory;
    private @NonNull ProgressUpdater mProgressUpdater;
    private @NonNull ForegroundUpdater mForegroundUpdater;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkerParameters(
            @NonNull UUID id,
            @NonNull Data inputData,
            @NonNull Collection<String> tags,
            @NonNull RuntimeExtras runtimeExtras,
            @IntRange(from = 0) int runAttemptCount,
            @NonNull Executor backgroundExecutor,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkerFactory workerFactory,
            @NonNull ProgressUpdater progressUpdater,
            @NonNull ForegroundUpdater foregroundUpdater) {
        mId = id;
        mInputData = inputData;
        mTags = new HashSet<>(tags);
        mRuntimeExtras = runtimeExtras;
        mRunAttemptCount = runAttemptCount;
        mBackgroundExecutor = backgroundExecutor;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkerFactory = workerFactory;
        mProgressUpdater = progressUpdater;
        mForegroundUpdater = foregroundUpdater;
    }

    /**
     * Gets the ID of the {@link WorkRequest} that created this {@link ListenableWorker}.
     *
     * @return The ID of the creating {@link WorkRequest}
     */
    public @NonNull UUID getId() {
        return mId;
    }

    /**
     * Gets the input data.  Note that in the case that there are multiple prerequisites for this
     * {@link ListenableWorker}, the input data has been run through an {@link InputMerger}.
     *
     * @return The input data for this work
     * @see OneTimeWorkRequest.Builder#setInputMerger(Class)
     */
    public @NonNull Data getInputData() {
        return mInputData;
    }

    /**
     * Gets a {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}.
     *
     * @return The {@link java.util.Set} of tags associated with this Worker's {@link WorkRequest}
     * @see WorkRequest.Builder#addTag(String)
     */
    public @NonNull Set<String> getTags() {
        return mTags;
    }

    /**
     * Gets the list of content {@link android.net.Uri}s that caused this Worker to execute.  See
     * @code JobParameters#getTriggeredContentUris()} for relevant {@code JobScheduler} code.
     *
     * @return The list of content {@link android.net.Uri}s that caused this Worker to execute
     * @see Constraints.Builder#addContentUriTrigger(android.net.Uri, boolean)
     */
    @RequiresApi(24)
    public @NonNull List<Uri> getTriggeredContentUris() {
        return mRuntimeExtras.triggeredContentUris;
    }

    /**
     * Gets the list of content authorities that caused this Worker to execute.  See
     * {@code JobParameters#getTriggeredContentAuthorities()} for relevant {@code JobScheduler}
     * code.
     *
     * @return The list of content authorities that caused this Worker to execute
     */
    @RequiresApi(24)
    public @NonNull List<String> getTriggeredContentAuthorities() {
        return mRuntimeExtras.triggeredContentAuthorities;
    }

    /**
     * Gets the {@link android.net.Network} to use for this Worker.  This method returns
     * {@code null} if there is no network needed for this work request.
     *
     * @return The {@link android.net.Network} specified by the OS to be used with this Worker
     */
    @RequiresApi(28)
    public @Nullable Network getNetwork() {
        return mRuntimeExtras.network;
    }

    /**
     * Gets the current run attempt count for this work.  Note that for periodic work, this value
     * gets reset between periods.
     *
     * @return The current run attempt count for this work.
     */
    @IntRange(from = 0)
    public int getRunAttemptCount() {
        return mRunAttemptCount;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Executor getBackgroundExecutor() {
        return mBackgroundExecutor;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull TaskExecutor getTaskExecutor() {
        return mWorkTaskExecutor;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull WorkerFactory getWorkerFactory() {
        return mWorkerFactory;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull ProgressUpdater getProgressUpdater() {
        return mProgressUpdater;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull ForegroundUpdater getForegroundUpdater() {
        return mForegroundUpdater;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull RuntimeExtras getRuntimeExtras() {
        return mRuntimeExtras;
    }

    /**
     * Extra runtime information for Workers.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class RuntimeExtras {
        public @NonNull List<String> triggeredContentAuthorities = Collections.emptyList();
        public @NonNull List<Uri> triggeredContentUris = Collections.emptyList();

        @RequiresApi(28)
        public Network network;
    }
}
