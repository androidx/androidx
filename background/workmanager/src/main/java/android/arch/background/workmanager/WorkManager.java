/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.lifecycle.LiveData;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public abstract class WorkManager {

    /**
     * Retrieves the singleton instance of {@link WorkManager}.
     *
     * @return The singleton instance of {@link WorkManager}
     */
    public static synchronized WorkManager getInstance() {
        return WorkManagerImpl.getInstance();
    }

    /**
     * Gets the {@link BaseWork.WorkStatus} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link LiveData} of the status
     */
    public abstract LiveData<Integer> getStatusForId(@NonNull String id);

    /**
     * Gets the output for a given work id.
     *
     * @param id The id of the work
     * @return A {@link LiveData} of the output
     */
    public abstract LiveData<Arguments> getOutput(@NonNull String id);

    /**
     * Enqueues one or more items for background processing.
     *
     * @param work One or more {@link Work} to enqueue
     */
    public abstract void enqueue(@NonNull Work... work);

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workerClasses One or more {@link Worker}s to enqueue; this is a convenience method
     *                      that makes a {@link Work} object with default arguments for each Worker
     */
    @SuppressWarnings("unchecked")
    public abstract void enqueue(@NonNull Class<? extends Worker>... workerClasses);

    /**
     * Starts a chain of work, which can be enqueued together in the future using
     * {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link Work} to enqueue in the future
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input work
     */
    public abstract WorkContinuation createWith(@NonNull Work...work);

    /**
     * This method allows you to create unique chains of work for situations where you only want one
     * chain to be active at a given time.  For example, you may only want one sync operation to be
     * active.  If there is one pending, you can choose to let it run or replace it with your new
     * work.
     *
     * All work in this chain will be automatically tagged with {@code tag} if it isn't already.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code tag} will be pruned.  If this method determines that new work should NOT be
     * run, then the entire chain will be considered a no-op.
     *
     * @param tag A tag which should uniquely label all the work in this chain
     * @param existingWorkPolicy One of {@code REPLACE_EXISTING_WORK} or {@code KEEP_EXISTING_WORK}.
     *                           {@code REPLACE_EXISTING_WORK} ensures that if there is pending work
     *                           labelled with {@code tag}, it will be cancelled and the new work
     *                           will run.  {@code KEEP_EXISTING_WORK} will run the new sequence of
     *                           work only if there is no pending work labelled with {@code tag}.
     * @param work One or more {@link Work} to enqueue
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public abstract WorkContinuation createWithUniqueTag(
            @NonNull String tag, @ExistingWorkPolicy int existingWorkPolicy, @NonNull Work... work);

    /**
     * Enqueues one or more periodic work items for background processing.
     *
     * @param periodicWork One or more {@link PeriodicWork} to enqueue
     */
    public abstract void enqueue(@NonNull PeriodicWork... periodicWork);

    /**
     * Cancels work with the given id, regardless of the current state of the work.  Note that
     * cancellation is a best-effort policy and work that is already executing may continue to run.
     *
     * @param id The id of the work
     */
    public abstract void cancelWorkForId(@NonNull String id);

    /**
     * Cancels all work with the given tag, regardless of the current state of the work.
     * Note that cancellation is a best-effort policy and work that is already executing may
     * continue to run.
     *
     * @param tag The tag used to identify the work
     */
    public abstract void cancelAllWorkWithTag(@NonNull String tag);

    /**
     * Prunes the database of all non-pending work.  Any work that has cancelled, failed, or
     * succeeded that is not part of a pending chain of work will be deleted.  This includes all
     * outputs stored in the database.
     */
    public abstract void pruneDatabase();

    @Retention(SOURCE)
    @IntDef({REPLACE_EXISTING_WORK, KEEP_EXISTING_WORK})
    public @interface ExistingWorkPolicy {
    }

    public static final int REPLACE_EXISTING_WORK = 0;
    public static final int KEEP_EXISTING_WORK = 1;
}
