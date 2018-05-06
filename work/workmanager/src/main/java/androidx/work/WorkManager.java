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
import java.util.UUID;

/**
 * WorkManager is a library used to enqueue work that is guaranteed to execute after its constraints
 * are met.  WorkManager allows observation of work status and the ability to create complex chains
 * of work.
 * <p>
 * WorkManager uses an underlying job dispatching service when available based on the following
 * criteria:
 * <p><ul>
 * <li>Uses JobScheduler for API 23+
 * <li>For API 14-22
 * <ul>
 *   <li>If using Firebase JobDispatcher in the app and the optional Firebase dependency, uses
 *     Firebase JobDispatcher
 *   <li>Otherwise, uses a custom AlarmManager + BroadcastReceiver implementation
 * </ul></ul>
 * <p></p>All work must have a corresponding {@link Worker} to perform the computations.  Work is
 * performed in the background thread.
 *
 * <p>There are two types of work supported by WorkManager: {@link OneTimeWorkRequest} and
 * {@link PeriodicWorkRequest}.  You can enqueue requests using WorkManager as follows:
 *
 * <pre>
 * {@code
 * WorkManager workManager = WorkManager.getInstance();
 * workManager.enqueue(new OneTimeWorkRequest.Builder(FooWorker.class).build());}</pre>
 *
 * A {@link WorkRequest} has an associated id that can be used for lookups and observation as
 * follows:
 *
 * <pre>
 * {@code
 * WorkRequest request = new OneTimeWorkRequest.Builder(FooWorker.class).build();
 * workManager.enqueue(request);
 * LiveData<WorkStatus> status = workManager.getStatusById(request.getId());
 * status.observe(...);}</pre>
 *
 * You can also use the id for cancellation:
 *
 * <pre>
 * {@code
 * WorkRequest request = new OneTimeWorkRequest.Builder(FooWorker.class).build();
 * workManager.enqueue(request);
 * workManager.cancelWorkById(request.getId());}</pre>
 *
 * You can chain work as follows:
 *
 * <pre>
 * {@code
 * WorkRequest request1 = new OneTimeWorkRequest.Builder(FooWorker.class).build();
 * WorkRequest request2 = new OneTimeWorkRequest.Builder(BarWorker.class).build();
 * WorkRequest request3 = new OneTimeWorkRequest.Builder(BazWorker.class).build();
 * workManager.beginWith(request1, request2).then(request3).enqueue();}</pre>
 *
 * Each call to {@link #beginWith(OneTimeWorkRequest...)} or {@link #beginWith(List)} returns a
 * {@link WorkContinuation} upon which you can call
 * {@link WorkContinuation#then(OneTimeWorkRequest...)} or {@link WorkContinuation#then(List)} to
 * chain further work.  This allows for creation of complex chains of work.  For example, to create
 * a chain like this:
 *
 * <pre>
 *            A
 *            |
 *      +----------+
 *      |          |
 *      B          C
 *      |
 *   +----+
 *   |    |
 *   D    E             </pre>
 *
 * you would enqueue them as follows:
 *
 * <pre>
 * {@code
 * WorkContinuation continuation = workManager.beginWith(A);
 * continuation.then(B).then(D, E).enqueue();  // A is implicitly enqueued here
 * continuation.then(C).enqueue();}</pre>
 *
 * WorkRequests can accept {@link Constraints}, inputs (see {@link Data}), and backoff criteria.
 * WorkRequests can be tagged with human-readable Strings
 * (see {@link WorkRequest.Builder#addTag(String)}), and chains of work can be given a
 * uniquely-identifiable name (see
 * {@link #beginUniqueWork(String, ExistingWorkPolicy, OneTimeWorkRequest...)}).
 *
 * <p>By default, WorkManager runs its operations on a background thread.  If you are already
 * running on a background thread and have need for synchronous (blocking) calls to WorkManager, use
 * {@link #synchronous()} to access such methods.
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
    public abstract void cancelWorkById(@NonNull UUID id);

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
    public abstract LiveData<WorkStatus> getStatusById(@NonNull UUID id);

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
