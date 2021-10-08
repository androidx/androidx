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
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkManagerImpl;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
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
     * This method allows you to enqueue {@code work} requests to a uniquely-named
     * {@link RemoteWorkContinuation}, where only one continuation of a particular name can be
     * active at a time. For example, you may only want one sync operation to be active. If there
     * is one pending, you can choose to let it run or replace it with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link RemoteWorkContinuation}.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}; see below for more information
     * @param work The {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures that if there
     *             is pending work labelled with {@code uniqueWorkName}, it will be cancelled and
     *             the new work will run. {@code KEEP} will run the new OneTimeWorkRequests only if
     *             there is no pending work labelled with {@code uniqueWorkName}.  {@code APPEND}
     *             will append the OneTimeWorkRequests as leaf nodes labelled with
     *             {@code uniqueWorkName}.
     * @return A {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public final ListenableFuture<Void> enqueueUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull OneTimeWorkRequest work) {
        return enqueueUniqueWork(
                uniqueWorkName,
                existingWorkPolicy,
                Collections.singletonList(work));
    }

    /**
     * This method allows you to enqueue {@code work} requests to a uniquely-named
     * {@link RemoteWorkContinuation}, where only one continuation of a particular name can be
     * active at a time. For example, you may only want one sync operation to be active. If there
     * is one pending, you can choose to let it run or replace it with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link RemoteWorkContinuation}.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}
     * @param work {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures
     *                     that if there is pending work labelled with {@code uniqueWorkName}, it
     *                     will be cancelled and the new work will run. {@code KEEP} will run the
     *                     new OneTimeWorkRequests only if there is no pending work labelled with
     *                     {@code uniqueWorkName}. {@code APPEND} will append the
     *                     OneTimeWorkRequests as leaf nodes labelled with {@code uniqueWorkName}.
     * @return A {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueueUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work);

    /**
     * This method allows you to enqueue a uniquely-named {@link PeriodicWorkRequest}, where only
     * one PeriodicWorkRequest of a particular name can be active at a time.  For example, you may
     * only want one sync operation to be active.  If there is one pending, you can choose to let it
     * run or replace it with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this PeriodicWorkRequest.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingPeriodicWorkPolicy An {@link ExistingPeriodicWorkPolicy}
     * @param periodicWork A {@link PeriodicWorkRequest} to enqueue. {@code REPLACE} ensures that if
     *                     there is pending work labelled with {@code uniqueWorkName}, it will be
     *                     cancelled and the new work will run. {@code KEEP} will run the new
     *                     PeriodicWorkRequest only if there is no pending work labelled with
     *                     {@code uniqueWorkName}.
     * @return An {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork);

    /**
     * Begins a chain with one or more {@link OneTimeWorkRequest}s, which can be enqueued together
     * in the future using {@link RemoteWorkContinuation#enqueue()}.
     * <p>
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link RemoteWorkContinuation} that allows for further chaining of dependent
     * {@link OneTimeWorkRequest}
     */
    @NonNull
    public final RemoteWorkContinuation beginWith(@NonNull OneTimeWorkRequest work) {
        return beginWith(Collections.singletonList(work));
    }

    /**
     * Begins a chain with one or more {@link OneTimeWorkRequest}s, which can be enqueued together
     * in the future using {@link RemoteWorkContinuation#enqueue()}.
     * <p>
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link RemoteWorkContinuation} that allows for further chaining of dependent
     * {@link OneTimeWorkRequest}
     */
    @NonNull
    public abstract RemoteWorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work);

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time.  For example, you may only want one sync
     * operation to be active.  If there is one pending, you can choose to let it run or replace it
     * with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this set of work.
     * <p>
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code uniqueWorkName} will be pruned.  If this method determines that new work
     * should NOT be run, then the entire chain will be considered a no-op.
     * <p>
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.  This is particularly important if you are using {@code APPEND} as your
     * {@link ExistingWorkPolicy}.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}
     * @param work The {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures that if there
     *             is pending work labelled with {@code uniqueWorkName}, it will be cancelled and
     *             the new work will run. {@code KEEP} will run the new sequence of work only if
     *             there is no pending work labelled with {@code uniqueWorkName}.  {@code APPEND}
     *             will create a new sequence of work if there is no existing work with
     *             {@code uniqueWorkName}; otherwise, {@code work} will be added as a child of all
     *             leaf nodes labelled with {@code uniqueWorkName}.
     * @return A {@link RemoteWorkContinuation} that allows further chaining
     */
    @NonNull
    public final RemoteWorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull OneTimeWorkRequest work) {
        return beginUniqueWork(uniqueWorkName, existingWorkPolicy, Collections.singletonList(work));
    }

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time.  For example, you may only want one sync
     * operation to be active.  If there is one pending, you can choose to let it run or replace it
     * with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this set of work.
     * <p>
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code uniqueWorkName} will be pruned.  If this method determines that new work
     * should NOT be run, then the entire chain will be considered a no-op.
     * <p>
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.  This is particularly important if you are using {@code APPEND} as your
     * {@link ExistingWorkPolicy}.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}; see below for more information
     * @param work One or more {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures that
     *             if there is pending work labelled with {@code uniqueWorkName}, it will be
     *             cancelled and the new work will run. {@code KEEP} will run the new sequence of
     *             work only if there is no pending work labelled with {@code uniqueWorkName}.
     *             {@code APPEND} will create a new sequence of work if there is no
     *             existing work with {@code uniqueWorkName}; otherwise, {@code work} will be added
     *             as a child of all leaf nodes labelled with {@code uniqueWorkName}.
     * @return A {@link RemoteWorkContinuation} that allows further chaining
     */
    @NonNull
    public abstract RemoteWorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work);

    /**
     * Enqueues the instance of {@link WorkContinuation} for background processing.
     *
     * @return A {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @NonNull
    public abstract ListenableFuture<List<WorkInfo>> getWorkInfos(@NonNull WorkQuery workQuery);

    /**
     * Updates progress information for a {@link ListenableWorker}.
     *
     * @param id   The {@link WorkRequest} id
     * @param data The progress {@link Data}
     * @return A {@link ListenableFuture} that can be used to determine when the setProgress
     * has completed.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract ListenableFuture<Void> setProgress(@NonNull UUID id, @NonNull Data data);

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
