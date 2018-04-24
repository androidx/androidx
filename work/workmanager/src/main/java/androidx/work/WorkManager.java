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

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.impl.WorkManagerImpl;

import java.util.Arrays;
import java.util.List;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public abstract class WorkManager {

    /**
     * Retrieves the {@code default} singleton instance of {@link WorkManager}.
     *
     * @return The singleton instance of {@link WorkManager}
     */
    public static WorkManager getInstance() {
        return WorkManagerImpl.getInstance();
    }

    /**
     * Used to do a one-time initialization of the {@link WorkManager} singleton with the default
     * configuration.
     *
     * @param context A {@link Context} object for configuration purposes. Internally, this class
     *                will call {@link Context#getApplicationContext()}, so you may safely pass in
     *                any Context without risking a memory leak.
     * @param configuration The {@link Configuration} for used to set up WorkManager.
     */
    public static void initialize(@NonNull Context context, @NonNull Configuration configuration) {
        WorkManagerImpl.initialize(context, configuration);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workRequest One or more {@link WorkRequest} to enqueue
     */
    public final void enqueue(@NonNull WorkRequest... workRequest) {
        enqueue(Arrays.asList(workRequest));
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param baseWork One or more {@link WorkRequest} to enqueue
     */
    public abstract void enqueue(@NonNull List<? extends WorkRequest> baseWork);

    /**
     * Begins a chain of {@link OneTimeWorkRequest}, which can be enqueued together in the future
     * using {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}
     */
    public final WorkContinuation beginWith(@NonNull OneTimeWorkRequest...work) {
        return beginWith(Arrays.asList(work));
    }

    /**
     * Begins a chain of {@link OneTimeWorkRequest}, which can be enqueued together in the future
     * using {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}
     */
    public abstract WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work);

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time.  For example, you may only want one sync
     * operation to be active.  If there is one pending, you can choose to let it run or replace it
     * with your new work.
     *
     * The {@code name} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code name} will be pruned.  If this method determines that new work should NOT
     * be run, then the entire chain will be considered a no-op.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}.
     * @param work One or more {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures that
     *             if there is pending work labelled with {@code name}, it will be cancelled and the
     *             new work will run. {@code KEEP} will run the new sequence of work
     *             only if there is no pending work labelled with {@code name}.
     *             {@code APPEND} will create a new sequence of work if there is no
     *             existing work with {@code name}; otherwise, {@code work} will be added as a
     *             child of all leaf nodes labelled with {@code name}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public final WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull OneTimeWorkRequest... work) {
        return beginUniqueWork(uniqueWorkName, existingWorkPolicy, Arrays.asList(work));
    }

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time.  For example, you may only want one sync
     * operation to be active.  If there is one pending, you can choose to let it run or replace it
     * with your new work.
     *
     * The {@code name} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code name} will be pruned.  If this method determines that new work should NOT be
     * run, then the entire chain will be considered a no-op.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}.
     * @param work One or more {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures
     *             that if there is pending work labelled with {@code name}, it will be cancelled
     *             and the new work will run.
     *             {@code KEEP} will run the new sequence of work only if there is no
     *             pending work labelled with {@code name}.
     *             {@code APPEND} will create a new sequence of work if there is no existing work
     *             with {@code name}; otherwise, {@code work} will be added as a child of all
     *             leaf nodes labelled with {@code name}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public abstract WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work);

    /**
     * Cancels work with the given id if it isn't finished.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param id The id of the work
     */
    public abstract void cancelWorkById(@NonNull String id);

    /**
     * Cancels all unfinished work with the given tag.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param tag The tag used to identify the work
     */
    public abstract void cancelAllWorkByTag(@NonNull String tag);

    /**
     * Cancels all unfinished work in the work chain with the given name.  Note that cancellation is
     * a best-effort policy and work that is already executing may continue to run.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     */
    public abstract void cancelUniqueWork(@NonNull String uniqueWorkName);

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link LiveData} of the {@link WorkStatus} associated with {@code id}
     */
    public abstract LiveData<WorkStatus> getStatusById(@NonNull String id);

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A {@link LiveData} list of {@link WorkStatus} for work tagged with {@code tag}
     */
    public abstract LiveData<List<WorkStatus>> getStatusesByTag(@NonNull String tag);

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for all work in a work chain with a given
     * unique name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A {@link LiveData} of the {@link WorkStatus} for work in the chain named
     *         {@code uniqueWorkName}
     */
    public abstract LiveData<List<WorkStatus>> getStatusesForUniqueWork(
            @NonNull String uniqueWorkName);

    /**
     * Gets an object that gives access to synchronous methods.
     *
     * @return A {@link SynchronousWorkManager} object, which gives access to synchronous methods
     */
    public abstract SynchronousWorkManager synchronous();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected WorkManager() {
    }
}
