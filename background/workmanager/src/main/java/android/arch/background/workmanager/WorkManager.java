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

import android.arch.background.workmanager.impl.BaseWork;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

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
     * @param id The id of the {@link BaseWork}.
     * @return A {@link LiveData} of the status.
     */
    public abstract LiveData<Integer> getWorkStatus(String id);

    /**
     * Gets the output for a given work id.
     *
     * @param id The id of the {@link BaseWork}.
     * @return A {@link LiveData} of the output.
     */
    public abstract LiveData<Arguments> getOutput(String id);

    /**
     * Enqueues one or more items for background processing.
     *
     * @param work One or more {@link Work} to enqueue
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input work
     */
    public abstract WorkContinuation enqueue(Work... work);

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workerClasses One or more {@link Worker}s to enqueue; this is a convenience method
     *                      that makes a {@link Work} object with default arguments for each Worker
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workerClasses
     */
    @SuppressWarnings("unchecked")
    public abstract WorkContinuation enqueue(Class<? extends Worker>... workerClasses);

    /**
     * Enqueues one or more periodic work items for background processing.
     *
     * @param periodicWork One or more {@link PeriodicWork} to enqueue
     */
    public abstract void enqueue(PeriodicWork... periodicWork);

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
}

