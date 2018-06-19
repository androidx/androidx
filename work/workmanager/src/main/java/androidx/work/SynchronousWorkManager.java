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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Blocking methods for {@link WorkManager} operations.  These methods are expected to be called
 * from a background thread.
 */
public interface SynchronousWorkManager {

    /**
     * Enqueues one or more {@link WorkRequest} in a synchronous fashion. This method is expected to
     * be called from a background thread and, upon successful execution, you can rely on that the
     * work has been enqueued.
     *
     * @param workRequest The Array of {@link WorkRequest}
     */
    @WorkerThread
    void enqueueSync(@NonNull WorkRequest... workRequest);

    /**
     * Enqueues the List of {@link WorkRequest} in a synchronous fashion. This method is expected to
     * be called from a background thread and, upon successful execution, you can rely on that the
     * work has been enqueued.
     *
     * @param workRequest The List of {@link WorkRequest}
     */
    @WorkerThread
    void enqueueSync(@NonNull List<? extends WorkRequest> workRequest);

    /**
     * This method allows you to synchronously enqueue a uniquely-named {@link PeriodicWorkRequest},
     * where only one PeriodicWorkRequest of a particular name can be active at a time.  For
     * example, you may only want one sync operation to be active.  If there is one pending, you can
     * choose to let it run or replace it with your new work.
     *
     * This method is expected to be called from a background thread.
     *
     * The {@code uniqueWorkName} uniquely identifies this PeriodicWorkRequest.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingPeriodicWorkPolicy An {@link ExistingPeriodicWorkPolicy}
     * @param periodicWork A {@link PeriodicWorkRequest} to enqueue. {@code REPLACE} ensures that if
     *                     there is pending work labelled with {@code uniqueWorkName}, it will be
     *                     cancelled and the new work will run. {@code KEEP} will run the new
     *                     PeriodicWorkRequest only if there is no pending work labelled with
     *                     {@code uniqueWorkName}.
     */
    @WorkerThread
    void enqueueUniquePeriodicWorkSync(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork);

    /**
     * Cancels work with the given id in a synchronous fashion if it isn't finished.  Note that
     * cancellation is dependent on timing (for example, the work could have completed in a
     * different thread just as you issue this call).  Use {@link #getStatusByIdSync(UUID)} to
     * find out the actual state of the work after this call.  This method is expected to be called
     * from a background thread.
     *
     * @param id The id of the work
     */
    @WorkerThread
    void cancelWorkByIdSync(@NonNull UUID id);

    /**
     * Cancels all unfinished work with the given tag in a synchronous fashion.  Note that
     * cancellation is dependent on timing (for example, the work could have completed in a
     * different thread just as you issue this call).  Use {@link #getStatusByIdSync(UUID)} to
     * find out the actual state of the work after this call.  This method is expected to be called
     * from a background thread.
     *
     * @param tag The tag used to identify the work
     */
    @WorkerThread
    void cancelAllWorkByTagSync(@NonNull String tag);

    /**
     * Cancels all unfinished work in the work chain with the given name in a synchronous fashion.
     * Note that cancellation is dependent on timing (for example, the work could have completed in
     * a different thread just as you issue this call).  Use {@link #getStatusByIdSync(UUID)} to
     * find out the actual state of the work after this call.  This method is expected to be called
     * from a background thread.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     */
    @WorkerThread
    void cancelUniqueWorkSync(@NonNull String uniqueWorkName);

    /**
     * Cancels all unfinished work in a synchronous fashion.  <b>Use this method with extreme
     * caution!</b>  By invoking it, you will potentially affect other modules or libraries in your
     * codebase.  It is strongly recommended that you use one of the other cancellation methods at
     * your disposal.
     */
    @WorkerThread
    void cancelAllWorkSync();

    /**
     * Gets the timestamp of the last time all work was cancelled in a synchronous fashion.  This
     * method is intended for use by library and module developers who have dependent data in their
     * own repository that must be updated or deleted in case someone cancels their work without
     * their prior knowledge.
     *
     * @return The timestamp in milliseconds when a method that cancelled all work was last invoked;
     *         this timestamp may be {@code 0L} if this never occurred.
     */
    @WorkerThread
    long getLastCancelAllTimeMillisSync();

    /**
     * Prunes all eligible finished work from the internal database in a synchronous fashion.
     * Eligible work must be finished ({@link State#SUCCEEDED}, {@link State#FAILED}, or
     * {@link State#CANCELLED}), with zero unfinished dependents.
     * <p>
     * <b>Use this method with caution</b>; by invoking it, you (and any modules and libraries in
     * your codebase) will no longer be able to observe the {@link WorkStatus} of the pruned work.
     * You do not normally need to call this method - WorkManager takes care to auto-prune its work
     * after a sane period of time.  This method also ignores the
     * {@link OneTimeWorkRequest.Builder#keepResultsForAtLeast(long, TimeUnit)} policy.
     */
    @WorkerThread
    void pruneWorkSync();

    /**
     * Gets the {@link WorkStatus} of a given work id in a synchronous fashion.  This method is
     * expected to be called from a background thread.
     *
     * @param id The id of the work
     * @return A {@link WorkStatus} associated with {@code id}, or {@code null} if {@code id} is not
     *         known to WorkManager
     */
    @WorkerThread
    @Nullable WorkStatus getStatusByIdSync(@NonNull UUID id);

    /**
     * Gets the {@link WorkStatus} for all work with a given tag in a synchronous fashion.  This
     * method is expected to be called from a background thread.
     *
     * @param tag The tag of the work
     * @return A list of {@link WorkStatus} for work tagged with {@code tag}
     */
    @WorkerThread
    @NonNull List<WorkStatus> getStatusesByTagSync(@NonNull String tag);

    /**
     * Gets the {@link WorkStatus} for all work for the chain of work with a given unique name in a
     * synchronous fashion.  This method is expected to be called from a background thread.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A list of {@link WorkStatus} for work in the chain named {@code uniqueWorkName}
     */
    @WorkerThread
    @NonNull List<WorkStatus> getStatusesForUniqueWorkSync(@NonNull String uniqueWorkName);
}
