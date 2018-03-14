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

import androidx.work.impl.WorkManagerImpl;

import java.util.Arrays;
import java.util.List;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public abstract class WorkManager {

    /**
     * Retrieves the singleton instance of {@link WorkManager}.
     *
     * @param context A {@link Context} object for configuration purposes.  Internally, this class
     *                will call {@link Context#getApplicationContext()}, so you may safely pass in
     *                any Context without risking a memory leak.
     * @return The singleton instance of {@link WorkManager}
     */
    public static synchronized WorkManager getInstance(Context context) {
        return WorkManagerImpl.getInstance(context);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param baseWork One or more {@link BaseWork} to enqueue
     */
    public final void enqueue(@NonNull BaseWork... baseWork) {
        enqueue(Arrays.asList(baseWork));
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param baseWork One or more {@link BaseWork} to enqueue
     */
    public abstract void enqueue(@NonNull List<? extends BaseWork> baseWork);

    /**
     * Begins a chain of {@link Work}, which can be enqueued together in the future using
     * {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link Work} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent {@link Work}
     */
    public final WorkContinuation beginWith(@NonNull Work...work) {
        return beginWith(Arrays.asList(work));
    }

    /**
     * Begins a chain of {@link Work}, which can be enqueued together in the future using
     * {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link Work} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent {@link Work}
     */
    public abstract WorkContinuation beginWith(@NonNull List<Work> work);

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain to be active at a given time.  For example, you may only want one sync operation to be
     * active.  If there is one pending, you can choose to let it run or replace it with your new
     * work.
     *
     * The {@code name} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code name} will be pruned.  If this method determines that new work should NOT be
     * run, then the entire chain will be considered a no-op.
     *
     * @param name A name which should uniquely label all the work in this chain
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}.
     * @param work One or more {@link Work} to enqueue. {@code REPLACE} ensures that
     *             if there is pending work labelled with {@code name}, it will be cancelled and the
     *             new work will run. {@code KEEP} will run the new sequence of work
     *             only if there is no pending work labelled with {@code name}.
     *             {@code APPEND} will create a new sequence of work if there is no
     *             existing work with {@code name}; otherwise, {@code work} will be added as a child
     *             of all leaf nodes labelled with {@code name}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public final WorkContinuation beginWithName(
            @NonNull String name,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull Work... work) {
        return beginWithName(name, existingWorkPolicy, Arrays.asList(work));
    }

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain to be active at a given time.  For example, you may only want one sync operation to be
     * active.  If there is one pending, you can choose to let it run or replace it with your new
     * work.
     *
     * The {@code name} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code name} will be pruned.  If this method determines that new work should NOT be
     * run, then the entire chain will be considered a no-op.
     *
     * @param name A name which should uniquely label all the work in this chain
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}.
     * @param work One or more {@link Work} to enqueue. {@code REPLACE} ensures that if there is
     *             pending work labelled with {@code name}, it will be cancelled and the new work
     *             will run. {@code KEEP} will run the new sequence of work only if there is no
     *             pending work labelled with {@code name}.  {@code APPEND} will create a new
     *             sequence of work if there is no existing work with {@code name}; otherwise,
     *             {@code work} will be added as a child of all leaf nodes labelled with
     *             {@code name}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public abstract WorkContinuation beginWithName(
            @NonNull String name,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<Work> work);

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
     * @param name The name used to identify the chain of work
     */
    public abstract void cancelAllWorkByName(@NonNull String name);

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
     * name.
     *
     * @param name The name used to identify the chain of work
     * @return A {@link LiveData} of the {@link WorkStatus} for work in the chain named {@code name}
     */
    public abstract LiveData<List<WorkStatus>> getStatusesByName(@NonNull String name);

    /**
     * Gets an object that gives access to blocking (synchronous) methods.
     *
     * @return A {@link BlockingWorkManager} object, which gives access to blocking
     *         (synchronous) methods
     */
    public abstract BlockingWorkManager blocking();
}
