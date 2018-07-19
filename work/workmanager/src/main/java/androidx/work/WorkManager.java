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
import java.util.concurrent.TimeUnit;

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
     * @return The singleton instance of {@link WorkManager}; this may be {@code null} in unusual
     *         circumstances where you have disabled automatic initialization and have failed to
     *         manually call {@link #initialize(Context, Configuration)}.
     * @throws IllegalStateException If WorkManager is not initialized properly.  This is most
     *         likely because you disabled the automatic initialization but forgot to manually
     *         call {@link WorkManager#initialize(Context, Configuration)}.
     */
    public static @NonNull WorkManager getInstance() {
        WorkManager workManager = WorkManagerImpl.getInstance();
        if (workManager == null) {
            throw new IllegalStateException("WorkManager is not initialized properly.  The most "
                    + "likely cause is that you disabled WorkManagerInitializer in your manifest "
                    + "but forgot to call WorkManager#initialize in your Application#onCreate or a "
                    + "ContentProvider.");
        } else {
            return workManager;
        }
    }

    /**
     * Used to do a one-time initialization of the {@link WorkManager} singleton with a custom
     * {@link Configuration}.  By default, this method should not be called because WorkManager is
     * automatically initialized.  To initialize WorkManager yourself, please follow these steps:
     * <p><ul>
     * <li>Disable {@code androidx.work.impl.WorkManagerInitializer} in your manifest
     * <li>In {@code Application#onCreate} or a {@code ContentProvider}, call this method before
     * calling {@link WorkManager#getInstance()}
     * </ul></p>
     * This method has no effect if WorkManager is already initialized.
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
     * @param workRequests One or more {@link WorkRequest} to enqueue
     */
    public final void enqueue(@NonNull WorkRequest... workRequests) {
        enqueue(Arrays.asList(workRequests));
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workRequests One or more {@link WorkRequest} to enqueue
     */
    public abstract void enqueue(@NonNull List<? extends WorkRequest> workRequests);

    /**
     * Begins a chain of {@link OneTimeWorkRequest}, which can be enqueued together in the future
     * using {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}
     */
    public final @NonNull WorkContinuation beginWith(@NonNull OneTimeWorkRequest...work) {
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
    public abstract @NonNull WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work);

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time.  For example, you may only want one sync
     * operation to be active.  If there is one pending, you can choose to let it run or replace it
     * with your new work.
     *
     * The {@code uniqueWorkName} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code uniqueWorkName} will be pruned.  If this method determines that new work
     * should NOT be run, then the entire chain will be considered a no-op.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}
     * @param work One or more {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures that
     *             if there is pending work labelled with {@code uniqueWorkName}, it will be
     *             cancelled and the new work will run. {@code KEEP} will run the new sequence of
     *             work only if there is no pending work labelled with {@code uniqueWorkName}.
     *             {@code APPEND} will create a new sequence of work if there is no
     *             existing work with {@code uniqueWorkName}; otherwise, {@code work} will be added
     *             as a child of all leaf nodes labelled with {@code uniqueWorkName}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public final @NonNull WorkContinuation beginUniqueWork(
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
     * The {@code uniqueWorkName} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code uniqueWorkName} will be pruned.  If this method determines that new work
     * should NOT be run, then the entire chain will be considered a no-op.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}
     * @param work One or more {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures that
     *             if there is pending work labelled with {@code uniqueWorkName}, it will be
     *             cancelled and the new work will run. {@code KEEP} will run the new sequence of
     *             work only if there is no pending work labelled with {@code uniqueWorkName}.
     *             {@code APPEND} will create a new sequence of work if there is no
     *             existing work with {@code uniqueWorkName}; otherwise, {@code work} will be added
     *             as a child of all leaf nodes labelled with {@code uniqueWorkName}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public abstract @NonNull WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work);

    /**
     * This method allows you to enqueue a uniquely-named {@link PeriodicWorkRequest}, where only
     * one PeriodicWorkRequest of a particular name can be active at a time.  For example, you may
     * only want one sync operation to be active.  If there is one pending, you can choose to let it
     * run or replace it with your new work.
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
    public abstract void enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork);

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
     * Cancels all unfinished work.  <b>Use this method with extreme caution!</b>  By invoking it,
     * you will potentially affect other modules or libraries in your codebase.  It is strongly
     * recommended that you use one of the other cancellation methods at your disposal.
     */
    public abstract void cancelAllWork();

    /**
     * Prunes all eligible finished work from the internal database.  Eligible work must be finished
     * ({@link State#SUCCEEDED}, {@link State#FAILED}, or {@link State#CANCELLED}), with zero
     * unfinished dependents.
     * <p>
     * <b>Use this method with caution</b>; by invoking it, you (and any modules and libraries in
     * your codebase) will no longer be able to observe the {@link WorkStatus} of the pruned work.
     * You do not normally need to call this method - WorkManager takes care to auto-prune its work
     * after a sane period of time.  This method also ignores the
     * {@link OneTimeWorkRequest.Builder#keepResultsForAtLeast(long, TimeUnit)} policy.
     */
    public abstract void pruneWork();

    /**
     * Gets a {@link LiveData} of the last time all work was cancelled.  This method is intended for
     * use by library and module developers who have dependent data in their own repository that
     * must be updated or deleted in case someone cancels their work without their prior knowledge.
     *
     * @return A {@link LiveData} of the timestamp in milliseconds when method that cancelled all
     *         work was last invoked; this timestamp may be {@code 0L} if this never occurred.
     */
    public abstract @NonNull LiveData<Long> getLastCancelAllTimeMillis();

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link LiveData} of the {@link WorkStatus} associated with {@code id}; note that
     *         this {@link WorkStatus} may be {@code null} if {@code id} is not known to
     *         WorkManager.
     */
    public abstract @NonNull LiveData<WorkStatus> getStatusById(@NonNull UUID id);

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A {@link LiveData} list of {@link WorkStatus} for work tagged with {@code tag}
     */
    public abstract @NonNull LiveData<List<WorkStatus>> getStatusesByTag(@NonNull String tag);

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for all work in a work chain with a given
     * unique name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A {@link LiveData} of the {@link WorkStatus} for work in the chain named
     *         {@code uniqueWorkName}
     */
    public abstract @NonNull LiveData<List<WorkStatus>> getStatusesForUniqueWork(
            @NonNull String uniqueWorkName);

    /**
     * Gets an object that gives access to synchronous methods.
     *
     * @return A {@link SynchronousWorkManager} object, which gives access to synchronous methods
     */
    public abstract @NonNull SynchronousWorkManager synchronous();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected WorkManager() {
    }
}
