/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.multiprocess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.ListenableWorker;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkManagerImpl;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.UUID;

/**
 * A subset of {@link androidx.work.WorkManager} APIs that are available for apps that use
 * multiple processes.
 */
public abstract class RemoteWorkManager {
    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected RemoteWorkManager() {
        // Does nothing
    }

    /**
     * Enqueues one item for background processing.
     *
     * @param request The {@link WorkRequest} to enqueue
     * @return A {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueue(@NonNull WorkRequest request);

    /**
     * Enqueues one or more items for background processing.
     *
     * @param requests One or more {@link WorkRequest} to enqueue
     * @return A {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueue(@NonNull List<WorkRequest> requests);

    /**
     * Enqueues the instance of {@link WorkContinuation} for background processing.
     *
     * @return A {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueue(@NonNull WorkContinuation continuation);

    /**
     * Cancels work with the given id if it isn't finished.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.  Upon cancellation,
     * {@link ListenableWorker#onStopped()} will be invoked for any affected workers.
     *
     * @param id The id of the work
     * @return A {@link ListenableFuture} that can be used to determine when the cancelWorkById has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> cancelWorkById(@NonNull UUID id);

    /**
     * Cancels all unfinished work with the given tag.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.  Upon cancellation,
     * {@link ListenableWorker#onStopped()} will be invoked for any affected workers.
     *
     * @param tag The tag used to identify the work
     * @return An {@link ListenableFuture} that can be used to determine when the
     * cancelAllWorkByTag has completed
     */
    @NonNull
    public abstract ListenableFuture<Void> cancelAllWorkByTag(@NonNull String tag);

    /**
     * Cancels all unfinished work in the work chain with the given name.  Note that cancellation is
     * a best-effort policy and work that is already executing may continue to run.  Upon
     * cancellation, {@link ListenableWorker#onStopped()} will be invoked for any affected workers.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A {@link ListenableFuture} that can be used to determine when the cancelUniqueWork
     * has completed
     */
    @NonNull
    public abstract ListenableFuture<Void> cancelUniqueWork(@NonNull String uniqueWorkName);

    /**
     * Cancels all unfinished work.  <b>Use this method with extreme caution!</b>  By invoking it,
     * you will potentially affect other modules or libraries in your codebase.  It is strongly
     * recommended that you use one of the other cancellation methods at your disposal.
     * <p>
     * Upon cancellation, {@link ListenableWorker#onStopped()} will be invoked for any affected
     * workers.
     *
     * @return A {@link ListenableFuture} that can be used to determine when the cancelAllWork has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> cancelAllWork();

    /**
     * Gets the {@link ListenableFuture} of the {@link List} of {@link WorkInfo} for all work
     * referenced by the {@link WorkQuery} specification.
     *
     * @param workQuery The work query specification
     * @return A {@link ListenableFuture} of the {@link List} of {@link WorkInfo} for work
     * referenced by this {@link WorkQuery}.
     */
    public abstract @NonNull ListenableFuture<List<WorkInfo>> getWorkInfos(
            @NonNull WorkQuery workQuery);

    /**
     * Gets the instance of {@link RemoteWorkManager} which provides a subset of
     * {@link WorkManager} APIs that are safe to use for apps that use multiple processes.
     *
     * @param context The application context.
     * @return The instance of {@link RemoteWorkManager}.
     */
    @NonNull
    public static RemoteWorkManager getInstance(@NonNull Context context) {
        WorkManagerImpl workManager = WorkManagerImpl.getInstance(context);
        RemoteWorkManager remoteWorkManager = workManager.getRemoteWorkManager();
        if (remoteWorkManager == null) {
            // Should never really happen.
            throw new IllegalStateException("Unable to initialize RemoteWorkManager");
        }
        return remoteWorkManager;
    }
}
