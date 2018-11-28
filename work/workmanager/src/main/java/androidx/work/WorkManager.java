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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager is a library used to enqueue deferrable work that is guaranteed to execute sometime
 * after its {@link Constraints} are met.  WorkManager allows observation of work status and the
 * ability to create complex chains of work.
 * <p>
 * WorkManager uses an underlying job dispatching service when available based on the following
 * criteria:
 * <p><ul>
 * <li>Uses JobScheduler for API 23+
 * <li>Uses a custom AlarmManager + BroadcastReceiver implementation for API 14-22</ul>
 * <p>
 * All work must be done in a {@link ListenableWorker} class.  A simple implementation,
 * {@link Worker}, is recommended as the starting point for most developers.  With the optional
 * dependencies, you can also use {@code CoroutineWorker} or {@code RxWorker}.
 * <p>
 * There are two types of work supported by WorkManager: {@link OneTimeWorkRequest} and
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
 * LiveData<WorkInfo> status = workManager.getWorkInfoByIdLiveData(request.getId());
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
 * Each call to {@link #beginWith(OneTimeWorkRequest)} or {@link #beginWith(List)} returns a
 * {@link WorkContinuation} upon which you can call
 * {@link WorkContinuation#then(OneTimeWorkRequest)} or {@link WorkContinuation#then(List)} to
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
 * Work is eligible for execution when all of its prerequisites are complete.  If any of its
 * prerequisites fail or are cancelled, the work will never run.
 * <p>
 * WorkRequests can accept {@link Constraints}, inputs (see {@link Data}), and backoff criteria.
 * WorkRequests can be tagged with human-readable Strings
 * (see {@link WorkRequest.Builder#addTag(String)}), and chains of work can be given a
 * uniquely-identifiable name (see
 * {@link #beginUniqueWork(String, ExistingWorkPolicy, OneTimeWorkRequest)}).
 *
 * <p>
 * <b>Manually initializing WorkManager</b>
 * <p>
 * You can manually initialize WorkManager and provide a custom {@link Configuration} for it.
 * Please see {@link #initialize(Context, Configuration)}.
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
     * <li>Disable {@code androidx.work.impl.WorkManagerInitializer} in your manifest.
     * <li>Invoke this method in {@code Application#onCreate} or a {@code ContentProvider}. Note
     * that this method <b>must</b> be invoked in one of these two places or you risk getting a
     * {@code NullPointerException} in {@link #getInstance()}.
     * </ul></p>
     * <p>
     * This method throws an exception if it is called multiple times.
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
     * @return An {@link Operation} that can be used to determine when the enqueue has completed
     */
    @NonNull
    public final Operation enqueue(@NonNull WorkRequest workRequest) {
        return enqueue(Collections.singletonList(workRequest));
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param requests One or more {@link WorkRequest} to enqueue
     * @return An {@link Operation} that can be used to determine when the enqueue has completed
     */
    @NonNull
    public abstract Operation enqueue(@NonNull List<? extends WorkRequest> requests);

    /**
     * Begins a chain with one or more {@link OneTimeWorkRequest}s, which can be enqueued together
     * in the future using {@link WorkContinuation#enqueue()}.
     * <p>
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}
     */
    public final @NonNull WorkContinuation beginWith(@NonNull OneTimeWorkRequest work) {
        return beginWith(Collections.singletonList(work));
    }

    /**
     * Begins a chain with one or more {@link OneTimeWorkRequest}s, which can be enqueued together
     * in the future using {@link WorkContinuation#enqueue()}.
     * <p>
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.
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
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public final @NonNull WorkContinuation beginUniqueWork(
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
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public abstract @NonNull WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work);


    /**
     * This method allows you to enqueue {@code work} requests to a uniquely-named
     * {@link WorkContinuation}, where only one continuation of a particular name can be active at
     * a time. For example, you may only want one sync operation to be active. If there is one
     * pending, you can choose to let it run or replace it with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link WorkContinuation}.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}; see below for more information
     * @param work The {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures that if there
     *             is pending work labelled with {@code uniqueWorkName}, it will be cancelled and
     *             the new work will run. {@code KEEP} will run the new OneTimeWorkRequests only if
     *             there is no pending work labelled with {@code uniqueWorkName}.  {@code APPEND}
     *             will append the OneTimeWorkRequests as leaf nodes labelled with
     *             {@code uniqueWorkName}.
     * @return An {@link Operation} that can be used to determine when the enqueue has completed
     */
    @NonNull
    public Operation enqueueUniqueWork(
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
     * {@link WorkContinuation}, where only one continuation of a particular name can be active at
     * a time. For example, you may only want one sync operation to be active. If there is one
     * pending, you can choose to let it run or replace it with your new work.
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link WorkContinuation}.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}
     * @param work {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures
     *                     that if there is pending work labelled with {@code uniqueWorkName}, it
     *                     will be cancelled and the new work will run. {@code KEEP} will run the
     *                     new OneTimeWorkRequests only if there is no pending work labelled with
     *                     {@code uniqueWorkName}. {@code APPEND} will append the
     *                     OneTimeWorkRequests as leaf nodes labelled with {@code uniqueWorkName}.
     * @return An {@link Operation} that can be used to determine when the enqueue has completed
     */
    @NonNull
    public abstract Operation enqueueUniqueWork(
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
     * @return An {@link Operation} that can be used to determine when the enqueue has completed
     */
    @NonNull
    public abstract Operation enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork);

    /**
     * Cancels work with the given id if it isn't finished.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.  Upon cancellation,
     * {@link ListenableWorker#onStopped()} will be invoked for any affected workers.
     *
     * @param id The id of the work
     * @return An {@link Operation} that can be used to determine when the cancelWorkById has
     * completed
     */
    public abstract @NonNull Operation cancelWorkById(@NonNull UUID id);

    /**
     * Cancels all unfinished work with the given tag.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.  Upon cancellation,
     * {@link ListenableWorker#onStopped()} will be invoked for any affected workers.
     *
     * @param tag The tag used to identify the work
     * @return An {@link Operation} that can be used to determine when the cancelAllWorkByTag has
     * completed
     */
    public abstract @NonNull Operation cancelAllWorkByTag(@NonNull String tag);

    /**
     * Cancels all unfinished work in the work chain with the given name.  Note that cancellation is
     * a best-effort policy and work that is already executing may continue to run.  Upon
     * cancellation, {@link ListenableWorker#onStopped()} will be invoked for any affected workers.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return An {@link Operation} that can be used to determine when the cancelUniqueWork has
     * completed
     */
    public abstract @NonNull Operation cancelUniqueWork(@NonNull String uniqueWorkName);

    /**
     * Cancels all unfinished work.  <b>Use this method with extreme caution!</b>  By invoking it,
     * you will potentially affect other modules or libraries in your codebase.  It is strongly
     * recommended that you use one of the other cancellation methods at your disposal.
     * <p>
     * Upon cancellation, {@link ListenableWorker#onStopped()} will be invoked for any affected
     * workers.
     *
     * @return An {@link Operation} that can be used to determine when the cancelAllWork has
     * completed
     */
    public abstract @NonNull Operation cancelAllWork();

    /**
     * Prunes all eligible finished work from the internal database.  Eligible work must be finished
     * ({@link WorkInfo.State#SUCCEEDED}, {@link WorkInfo.State#FAILED}, or
     * {@link WorkInfo.State#CANCELLED}), with zero unfinished dependents.
     * <p>
     * <b>Use this method with caution</b>; by invoking it, you (and any modules and libraries in
     * your codebase) will no longer be able to observe the {@link WorkInfo} of the pruned work.
     * You do not normally need to call this method - WorkManager takes care to auto-prune its work
     * after a sane period of time.  This method also ignores the
     * {@link OneTimeWorkRequest.Builder#keepResultsForAtLeast(long, TimeUnit)} policy.
     *
     * @return An {@link Operation} that can be used to determine when the pruneWork has
     * completed
     */
    public abstract @NonNull Operation pruneWork();

    /**
     * Gets a {@link LiveData} of the last time all work was cancelled.  This method is intended for
     * use by library and module developers who have dependent data in their own repository that
     * must be updated or deleted in case someone cancels their work without their prior knowledge.
     *
     * @return A {@link LiveData} of the timestamp ({@code System#getCurrentTimeMillis()}) when
     *         {@link #cancelAllWork()} was last invoked; this timestamp may be {@code 0L} if this
     *         never occurred
     */
    public abstract @NonNull LiveData<Long> getLastCancelAllTimeMillisLiveData();

    /**
     * Gets a {@link ListenableFuture} of the last time all work was cancelled.  This method is
     * intended for use by library and module developers who have dependent data in their own
     * repository that must be updated or deleted in case someone cancels their work without
     * their prior knowledge.
     *
     * @return A {@link ListenableFuture} of the timestamp ({@code System#getCurrentTimeMillis()})
     *         when {@link #cancelAllWork()} was last invoked; this timestamp may be {@code 0L} if
     *         this never occurred
     */
    public abstract @NonNull ListenableFuture<Long> getLastCancelAllTimeMillis();

    /**
     * Gets a {@link LiveData} of the {@link WorkInfo} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link LiveData} of the {@link WorkInfo} associated with {@code id}; note that
     *         this {@link WorkInfo} may be {@code null} if {@code id} is not known to
     *         WorkManager.
     */
    public abstract @NonNull LiveData<WorkInfo> getWorkInfoByIdLiveData(@NonNull UUID id);

    /**
     * Gets a {@link ListenableFuture} of the {@link WorkInfo} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link ListenableFuture} of the {@link WorkInfo} associated with {@code id};
     * note that this {@link WorkInfo} may be {@code null} if {@code id} is not known to
     * WorkManager
     */
    public abstract @NonNull ListenableFuture<WorkInfo> getWorkInfoById(@NonNull UUID id);

    /**
     * Gets a {@link LiveData} of the {@link WorkInfo} for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A {@link LiveData} list of {@link WorkInfo} for work tagged with {@code tag}
     */
    public abstract @NonNull LiveData<List<WorkInfo>> getWorkInfosByTagLiveData(
            @NonNull String tag);

    /**
     * Gets a {@link ListenableFuture} of the {@link WorkInfo} for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A {@link ListenableFuture} list of {@link WorkInfo} for work tagged with
     * {@code tag}
     */
    public abstract @NonNull ListenableFuture<List<WorkInfo>> getWorkInfosByTag(
            @NonNull String tag);

    /**
     * Gets a {@link LiveData} of the {@link WorkInfo} for all work in a work chain with a given
     * unique name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A {@link LiveData} of the {@link WorkInfo} for work in the chain named
     *         {@code uniqueWorkName}
     */
    public abstract @NonNull LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(
            @NonNull String uniqueWorkName);

    /**
     * Gets a {@link ListenableFuture} of the {@link WorkInfo} for all work in a work chain
     * with a given unique name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A {@link ListenableFuture} of the {@link WorkInfo} for work in the chain named
     *         {@code uniqueWorkName}
     */
    public abstract @NonNull ListenableFuture<List<WorkInfo>> getWorkInfosForUniqueWork(
            @NonNull String uniqueWorkName);

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected WorkManager() {
    }
}
