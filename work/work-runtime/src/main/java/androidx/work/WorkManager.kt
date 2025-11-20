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

package androidx.work

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.WorkManager.Companion.getInstance
import androidx.work.WorkManager.Companion.initialize
import androidx.work.impl.WorkManagerImpl
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * WorkManager is the recommended library for persistent work. Scheduled work is guaranteed to
 * execute sometime after its [Constraints] are met. WorkManager allows observation of work status
 * and the ability to create complex chains of work.
 *
 * WorkManager uses an underlying job dispatching service when available based on the following
 * criteria:
 * * Uses JobScheduler for API 23+
 * * Uses a custom AlarmManager + BroadcastReceiver implementation for API 14-22
 *
 * All work must be done in a [ListenableWorker] class. A simple implementation, [Worker], is
 * recommended as the starting point for most developers. With the optional dependencies, you can
 * also use `CoroutineWorker` or `RxWorker`. All background work is given a maximum of ten minutes
 * to finish its execution. After this time has expired, the worker will be signalled to stop.
 *
 * There are two types of work supported by WorkManager: [OneTimeWorkRequest] and
 * [PeriodicWorkRequest]. You can enqueue requests using WorkManager as follows:
 * ```
 * WorkManager workManager = WorkManager.getInstance(Context);
 * workManager.enqueue(new OneTimeWorkRequest.Builder(FooWorker.class).build());
 * ```
 *
 * A [WorkRequest] has an associated id that can be used for lookups and observation as follows:
 * ```
 * WorkRequest request = new OneTimeWorkRequest.Builder(FooWorker.class).build();
 * workManager.enqueue(request);
 * LiveData<WorkInfo> status = workManager.getWorkInfoByIdLiveData(request.getId());
 * status.observe(...);
 * ```
 *
 * You can also use the id for cancellation:
 * ```
 * WorkRequest request = new OneTimeWorkRequest.Builder(FooWorker.class).build();
 * workManager.enqueue(request);
 * workManager.cancelWorkById(request.getId());
 * ```
 *
 * You can chain work as follows:
 * ```
 * WorkRequest request1 = new OneTimeWorkRequest.Builder(FooWorker.class).build();
 * WorkRequest request2 = new OneTimeWorkRequest.Builder(BarWorker.class).build();
 * WorkRequest request3 = new OneTimeWorkRequest.Builder(BazWorker.class).build();
 * workManager.beginWith(request1, request2).then(request3).enqueue();
 * ```
 *
 * Each call to [beginWith] returns a [WorkContinuation] upon which you can call
 * [WorkContinuation.then] with a single [OneTimeWorkRequest] or a list of [OneTimeWorkRequest] to
 * chain further work. This allows for creation of complex chains of work. For example, to create a
 * chain like this:
 * ```
 *            A
 *            |
 *      +----------+
 *      |          |
 *      B          C
 *      |
 *   +----+
 *   |    |
 *   D    E
 * ```
 *
 * you would enqueue them as follows:
 * ```
 * WorkContinuation continuation = workManager.beginWith(A);
 * continuation.then(B).then(D, E).enqueue();  // A is implicitly enqueued here
 * continuation.then(C).enqueue();
 * ```
 *
 * Work is eligible for execution when all of its prerequisites are complete. If any of its
 * prerequisites fail or are cancelled, the work will never run.
 *
 * WorkRequests can accept [Constraints], inputs (see [Data]), and backoff criteria. WorkRequests
 * can be tagged with human-readable Strings (see [WorkRequest.Builder.addTag]), and chains of work
 * can be given a uniquely-identifiable name (see [beginUniqueWork]).
 *
 * ### Initializing WorkManager
 *
 * By default, WorkManager auto-initializes itself using a built-in `ContentProvider`.
 * ContentProviders are created and run before the `Application` object, so this allows the
 * WorkManager singleton to be setup before your code can run in most cases. This is suitable for
 * most developers. However, you can provide a custom [Configuration] by using
 * [Configuration.Provider] or [WorkManager.initialize].
 *
 * ### Renaming and Removing ListenableWorker Classes
 *
 * Exercise caution in renaming classes derived from [ListenableWorker]s. WorkManager stores the
 * class name in its internal database when the [WorkRequest] is enqueued so it can later create an
 * instance of that worker when constraints are met. Unless otherwise specified in the WorkManager
 * [Configuration], this is done in the default [WorkerFactory] which tries to reflectively create
 * the ListenableWorker object. Therefore, renaming or removing these classes is dangerous - if
 * there is pending work with the given class, it will fail permanently if the class cannot be
 * found. If you are using a custom WorkerFactory, make sure you properly handle cases where the
 * class is not found so that your code does not crash.
 *
 * In case it is desirable to rename a class, implement a custom WorkerFactory that instantiates the
 * right ListenableWorker for the old class name.
 */
// Suppressing Metalava checks for added abstract methods in WorkManager.
// WorkManager cannot be extended, because the constructor is marked @Restricted
@SuppressLint("AddedAbstractMethod")
public abstract class WorkManager internal constructor() {

    public companion object {
        /**
         * Retrieves the `default` singleton instance of [WorkManager].
         *
         * @return The singleton instance of [WorkManager]; this may be `null` in unusual
         *   circumstances where you have disabled automatic initialization and have failed to
         *   manually call [initialize].
         * @throws IllegalStateException If WorkManager is not initialized properly as per the
         *   exception message.
         */
        // `open` modifier was added to avoid errors in WorkManagerImpl:
        // "WorkManagerImpl cannot override <X> in WorkManager", even though methods are static
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        @Deprecated(
            message = "Use the overload receiving Context",
            replaceWith = ReplaceWith("WorkManager.getContext(context)"),
        )
        @JvmStatic
        public open fun getInstance(): WorkManager {
            @Suppress("DEPRECATION") val workManager: WorkManager? = WorkManagerImpl.getInstance()
            checkNotNull(workManager) {
                "WorkManager is not initialized properly.  The most " +
                    "likely cause is that you disabled WorkManagerInitializer in your manifest " +
                    "but forgot to call WorkManager#initialize in your Application#onCreate or a " +
                    "ContentProvider."
            }
            return workManager
        }

        /**
         * Retrieves the `default` singleton instance of [WorkManager].
         *
         * @param context A [Context] for on-demand initialization.
         * @return The singleton instance of [WorkManager]; this may be `null` in unusual
         *   circumstances where you have disabled automatic initialization and have failed to
         *   manually call [initialize].
         * @throws IllegalStateException If WorkManager is not initialized properly
         */
        // `open` modifier was added to avoid errors in WorkManagerImpl:
        // "WorkManagerImpl cannot override <X> in WorkManager", even though methods are static
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        @JvmStatic
        public open fun getInstance(context: Context): WorkManager {
            return WorkManagerImpl.getInstance(context)
        }

        /**
         * Used to do a one-time initialization of the [WorkManager] singleton with a custom
         * [Configuration]. By default, this method should not be called because WorkManager is
         * automatically initialized. To initialize WorkManager yourself, please follow these steps:
         * * Disable `androidx.work.WorkManagerInitializer` in your manifest.
         * * Invoke this method in `Application#onCreate` or a `ContentProvider`. Note that this
         *   method **must** be invoked in one of these two places or you risk getting a
         *   `NullPointerException` in [getInstance].
         *
         * This method throws an [IllegalStateException] when attempting to initialize in direct
         * boot mode.
         *
         * This method throws an exception if it is called multiple times.
         *
         * @param context A [Context] object for configuration purposes. Internally, this class will
         *   call [Context.getApplicationContext], so you may safely pass in any Context without
         *   risking a memory leak.
         * @param configuration The [Configuration] for used to set up WorkManager.
         * @see Configuration.Provider for on-demand initialization.
         */
        // `open` modifier was added to avoid errors in WorkManagerImpl:
        // "WorkManagerImpl cannot override <X> in WorkManager", even though methods are static
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        @JvmStatic
        public open fun initialize(context: Context, configuration: Configuration) {
            WorkManagerImpl.initialize(context, configuration)
        }

        /**
         * Provides a way to check if [WorkManager] is initialized in this process.
         *
         * @return `true` if [WorkManager] has been initialized in this process.
         */
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        @JvmStatic
        public open fun isInitialized(): Boolean = WorkManagerImpl.isInitialized()
    }

    /** The [Configuration] instance that [WorkManager] was initialized with. */
    public abstract val configuration: Configuration

    /**
     * Enqueues one item for background processing.
     *
     * @param request The [WorkRequest] to enqueue
     * @return An [Operation] that can be used to determine when the enqueue has completed
     */
    public fun enqueue(request: WorkRequest): Operation {
        return enqueue(listOf(request))
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param requests One or more [WorkRequest] to enqueue
     * @return An [Operation] that can be used to determine when the enqueue has completed
     */
    public abstract fun enqueue(requests: List<WorkRequest>): Operation

    /**
     * Begins a chain with one or more [OneTimeWorkRequest]s, which can be enqueued together in the
     * future using [WorkContinuation.enqueue].
     *
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.
     *
     * @param request One or more [OneTimeWorkRequest] to start a chain of work
     * @return A [WorkContinuation] that allows for further chaining of dependent
     *   [OneTimeWorkRequest]
     */
    public fun beginWith(request: OneTimeWorkRequest): WorkContinuation {
        return beginWith(listOf(request))
    }

    /**
     * Begins a chain with one or more [OneTimeWorkRequest]s, which can be enqueued together in the
     * future using [WorkContinuation.enqueue].
     *
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run.
     *
     * @param requests One or more [OneTimeWorkRequest] to start a chain of work
     * @return A [WorkContinuation] that allows for further chaining of dependent
     *   [OneTimeWorkRequest]
     */
    public abstract fun beginWith(requests: List<OneTimeWorkRequest>): WorkContinuation

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time. For example, you may only want one sync
     * operation to be active. If there is one pending, you can choose to let it run or replace it
     * with your new work.
     *
     * The `uniqueWorkName` uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with `uniqueWorkName` will be pruned. If this method determines that new work should NOT
     * be run, then the entire chain will be considered a no-op.
     *
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run. This is particularly important if you are using `APPEND` as your
     * [ExistingWorkPolicy].
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An [ExistingWorkPolicy]
     * @param request The [OneTimeWorkRequest] to enqueue. `REPLACE` ensures that if there is
     *   pending work labelled with `uniqueWorkName`, it will be cancelled and the new work will
     *   run. `KEEP` will run the new sequence of work only if there is no pending work labelled
     *   with `uniqueWorkName`. `APPEND` will create a new sequence of work if there is no existing
     *   work with `uniqueWorkName`; otherwise, `work` will be added as a child of all leaf nodes
     *   labelled with `uniqueWorkName`.
     * @return A [WorkContinuation] that allows further chaining
     */
    public fun beginUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    ): WorkContinuation {
        return beginUniqueWork(uniqueWorkName, existingWorkPolicy, listOf(request))
    }

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time. For example, you may only want one sync
     * operation to be active. If there is one pending, you can choose to let it run or replace it
     * with your new work.
     *
     * The `uniqueWorkName` uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with `uniqueWorkName` will be pruned. If this method determines that new work should NOT
     * be run, then the entire chain will be considered a no-op.
     *
     * If any work in the chain fails or is cancelled, all of its dependent work inherits that state
     * and will never run. This is particularly important if you are using `APPEND` as your
     * [ExistingWorkPolicy].
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An [ExistingWorkPolicy]; see below for more information
     * @param requests One or more [OneTimeWorkRequest] to enqueue. `REPLACE` ensures that if there
     *   is pending work labelled with `uniqueWorkName`, it will be cancelled and the new work will
     *   run. `KEEP` will run the new sequence of work only if there is no pending work labelled
     *   with `uniqueWorkName`. `APPEND` will create a new sequence of work if there is no existing
     *   work with `uniqueWorkName`; otherwise, `work` will be added as a child of all leaf nodes
     *   labelled with `uniqueWorkName`.
     * @return A [WorkContinuation] that allows further chaining
     */
    public abstract fun beginUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    ): WorkContinuation

    /**
     * This method allows you to enqueue `work` requests to a uniquely-named [WorkContinuation],
     * where only one continuation of a particular name can be active at a time. For example, you
     * may only want one sync operation to be active. If there is one pending, you can choose to let
     * it run or replace it with your new work.
     *
     * The `uniqueWorkName` uniquely identifies this [WorkContinuation].
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An [ExistingWorkPolicy]; see below for more information
     * @param request The [OneTimeWorkRequest]s to enqueue. `REPLACE` ensures that if there is
     *   pending work labelled with `uniqueWorkName`, it will be cancelled and the new work will
     *   run. `KEEP` will run the new OneTimeWorkRequests only if there is no pending work labelled
     *   with `uniqueWorkName`. `APPEND` will append the OneTimeWorkRequests as leaf nodes labelled
     *   with `uniqueWorkName`.
     * @return An [Operation] that can be used to determine when the enqueue has completed
     */
    public open fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    ): Operation {
        return enqueueUniqueWork(uniqueWorkName, existingWorkPolicy, listOf(request))
    }

    /**
     * This method allows you to enqueue `work` requests to a uniquely-named [WorkContinuation],
     * where only one continuation of a particular name can be active at a time. For example, you
     * may only want one sync operation to be active. If there is one pending, you can choose to let
     * it run or replace it with your new work.
     *
     * The `uniqueWorkName` uniquely identifies this [WorkContinuation].
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An [ExistingWorkPolicy]
     * @param requests [OneTimeWorkRequest]s to enqueue. `REPLACE` ensures that if there is pending
     *   work labelled with `uniqueWorkName`, it will be cancelled and the new work will run. `KEEP`
     *   will run the new OneTimeWorkRequests only if there is no pending work labelled with
     *   `uniqueWorkName`. `APPEND` will append the OneTimeWorkRequests as leaf nodes labelled with
     *   `uniqueWorkName`.
     * @return An [Operation] that can be used to determine when the enqueue has completed
     */
    public abstract fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    ): Operation

    /**
     * This method allows you to enqueue a uniquely-named [PeriodicWorkRequest], where only one
     * PeriodicWorkRequest of a particular name can be active at a time. For example, you may only
     * want one sync operation to be active. If there is one pending, you can choose to let it run
     * or replace it with your new work.
     *
     * The `uniqueWorkName` uniquely identifies this PeriodicWorkRequest.
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingPeriodicWorkPolicy An [ExistingPeriodicWorkPolicy]
     * @param request A [PeriodicWorkRequest] to enqueue. `REPLACE` ensures that if there is pending
     *   work labelled with `uniqueWorkName`, it will be cancelled and the new work will run. `KEEP`
     *   will run the new PeriodicWorkRequest only if there is no pending work labelled with
     *   `uniqueWorkName`.
     * @return An [Operation] that can be used to determine when the enqueue has completed
     */
    public abstract fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Operation

    /**
     * Cancels work with the given id if it isn't finished. Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run. Upon cancellation,
     * [ListenableFuture] returned by [ListenableWorker.startWork] will be cancelled. Also
     * [ListenableWorker.onStopped] will be invoked for any affected workers.
     *
     * @param id The id of the work
     * @return An [Operation] that can be used to determine when the cancelWorkById has completed
     */
    public abstract fun cancelWorkById(id: UUID): Operation

    /**
     * Cancels all unfinished work with the given tag. Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run. Upon cancellation,
     * [ListenableFuture] returned by [ListenableWorker.startWork] will be cancelled. Also
     * [ListenableWorker.onStopped] will be invoked for any affected workers.
     *
     * @param tag The tag used to identify the work
     * @return An [Operation] that can be used to determine when the cancelAllWorkByTag has
     *   completed
     */
    public abstract fun cancelAllWorkByTag(tag: String): Operation

    /**
     * Cancels all unfinished work in the work chain with the given name. Note that cancellation is
     * a best-effort policy and work that is already executing may continue to run. Upon
     * cancellation, [ListenableFuture] returned by [ListenableWorker.startWork] will be cancelled.
     * Also [ListenableWorker.onStopped] will be invoked for any affected workers.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return An [Operation] that can be used to determine when the cancelUniqueWork has completed
     */
    public abstract fun cancelUniqueWork(uniqueWorkName: String): Operation

    /**
     * Cancels all unfinished work. **Use this method with extreme caution!** By invoking it, you
     * will potentially affect other modules or libraries in your codebase. It is strongly
     * recommended that you use one of the other cancellation methods at your disposal.
     *
     * Upon cancellation, [ListenableFuture] returned by [ListenableWorker.startWork] will be
     * cancelled. Also [ListenableWorker.onStopped] will be invoked for any affected workers.
     *
     * @return An [Operation] that can be used to determine when the cancelAllWork has completed
     */
    public abstract fun cancelAllWork(): Operation

    /**
     * Creates a [PendingIntent] which can be used to cancel a [WorkRequest] with the given `id`.
     *
     * @param id The [WorkRequest] id.
     * @return The [PendingIntent] that can be used to cancel the [WorkRequest].
     */
    public abstract fun createCancelPendingIntent(id: UUID): PendingIntent

    /**
     * Prunes all eligible finished work from the internal database. Eligible work must be finished
     * ([WorkInfo.State.SUCCEEDED], [WorkInfo.State.FAILED], or [WorkInfo.State.CANCELLED]), with
     * zero unfinished dependents.
     *
     * **Use this method with caution**; by invoking it, you (and any modules and libraries in your
     * codebase) will no longer be able to observe the [WorkInfo] of the pruned work. You do not
     * normally need to call this method - WorkManager takes care to auto-prune its work after a
     * sane period of time. This method also ignores the
     * [OneTimeWorkRequest.Builder.keepResultsForAtLeast] policy.
     *
     * @return An [Operation] that can be used to determine when the pruneWork has completed
     */
    public abstract fun pruneWork(): Operation

    /**
     * Gets a [LiveData] of the last time all work was cancelled. This method is intended for use by
     * library and module developers who have dependent data in their own repository that must be
     * updated or deleted in case someone cancels their work without their prior knowledge.
     *
     * @return A [LiveData] of the timestamp (`System#getCurrentTimeMillis()`) when [cancelAllWork]
     *   was last invoked; this timestamp may be `0L` if this never occurred
     */
    public abstract fun getLastCancelAllTimeMillisLiveData(): LiveData<Long>

    /**
     * Gets a [ListenableFuture] of the last time all work was cancelled. This method is intended
     * for use by library and module developers who have dependent data in their own repository that
     * must be updated or deleted in case someone cancels their work without their prior knowledge.
     *
     * @return A [ListenableFuture] of the timestamp (`System#getCurrentTimeMillis()`) when
     *   [cancelAllWork] was last invoked; this timestamp may be `0L` if this never occurred
     */
    public abstract fun getLastCancelAllTimeMillis(): ListenableFuture<Long>

    /**
     * Gets a [LiveData] of the [WorkInfo] for a given work id.
     *
     * @param id The id of the work
     * @return A [LiveData] of the [WorkInfo] associated with `id`; note that this [WorkInfo] may be
     *   `null` if `id` is not known to WorkManager.
     */
    public abstract fun getWorkInfoByIdLiveData(id: UUID): LiveData<WorkInfo?>

    /**
     * Gets a [Flow] of the [WorkInfo] for a given work id.
     *
     * @param id The id of the work
     * @return A [Flow] of the [WorkInfo] associated with `id`; note that this [WorkInfo] may be
     *   `null` if `id` is not known to WorkManager.
     */
    public abstract fun getWorkInfoByIdFlow(id: UUID): Flow<WorkInfo?>

    /**
     * Gets a [ListenableFuture] of the [WorkInfo] for a given work id.
     *
     * @param id The id of the work
     * @return A [ListenableFuture] of the [WorkInfo] associated with `id`; note that this
     *   [WorkInfo] may be `null` if `id` is not known to WorkManager
     */
    public abstract fun getWorkInfoById(id: UUID): ListenableFuture<WorkInfo?>

    /**
     * Gets a [LiveData] of the [WorkInfo] for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A [LiveData] list of [WorkInfo] for work tagged with `tag`
     */
    public abstract fun getWorkInfosByTagLiveData(tag: String): LiveData<List<WorkInfo>>

    /**
     * Gets a [Flow] of the [WorkInfo] for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A [Flow] list of [WorkInfo] for work tagged with `tag`
     */
    public abstract fun getWorkInfosByTagFlow(tag: String): Flow<List<WorkInfo>>

    /**
     * Gets a [ListenableFuture] of the [WorkInfo] for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A [ListenableFuture] list of [WorkInfo] for work tagged with `tag`
     */
    public abstract fun getWorkInfosByTag(tag: String): ListenableFuture<List<WorkInfo>>

    /**
     * Gets a [LiveData] of the [WorkInfo] for all work in a work chain with a given unique name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A [LiveData] of the [WorkInfo] for work in the chain named `uniqueWorkName`
     */
    public abstract fun getWorkInfosForUniqueWorkLiveData(
        uniqueWorkName: String
    ): LiveData<List<WorkInfo>>

    /**
     * Gets a [Flow] of the [WorkInfo] for all work in a work chain with a given unique name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A [Flow] of the [WorkInfo] for work in the chain named `uniqueWorkName`
     */
    public abstract fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>>

    /**
     * Gets a [ListenableFuture] of the [WorkInfo] for all work in a work chain with a given unique
     * name.
     *
     * @param uniqueWorkName The unique name used to identify the chain of work
     * @return A [ListenableFuture] of the [WorkInfo] for work in the chain named `uniqueWorkName`
     */
    public abstract fun getWorkInfosForUniqueWork(
        uniqueWorkName: String
    ): ListenableFuture<List<WorkInfo>>

    /**
     * Gets the [LiveData] of the [List] of [WorkInfo] for all work referenced by the [WorkQuery]
     * specification.
     *
     * @param workQuery The work query specification
     * @return A [LiveData] of the [List] of [WorkInfo] for work referenced by this [WorkQuery].
     */
    public abstract fun getWorkInfosLiveData(workQuery: WorkQuery): LiveData<List<WorkInfo>>

    /**
     * Gets the [Flow] of the [List] of [WorkInfo] for all work referenced by the [WorkQuery]
     * specification.
     *
     * @param workQuery The work query specification
     * @return A [Flow] of the [List] of [WorkInfo] for work referenced by this [WorkQuery].
     */
    public abstract fun getWorkInfosFlow(workQuery: WorkQuery): Flow<List<WorkInfo>>

    /**
     * Gets the [ListenableFuture] of the [List] of [WorkInfo] for all work referenced by the
     * [WorkQuery] specification.
     *
     * @param workQuery The work query specification
     * @return A [ListenableFuture] of the [List] of [WorkInfo] for work referenced by this
     *   [WorkQuery].
     */
    public abstract fun getWorkInfos(workQuery: WorkQuery): ListenableFuture<List<WorkInfo>>

    /**
     * Updates the work with the new specification. A [WorkRequest] passed as parameter must have an
     * id set with [WorkRequest.Builder.setId] that matches an id of the previously enqueued work.
     *
     * It preserves enqueue time, e.g. if a work was enqueued 3 hours ago and had 6 hours long
     * initial delay, after the update it would be still eligible for run in 3 hours, assuming that
     * initial delay wasn't updated.
     *
     * If the work being updated is currently running the returned ListenableFuture will be
     * completed with [UpdateResult.APPLIED_FOR_NEXT_RUN]. In this case the current run won't be
     * interrupted and will continue to rely on previous state of the request, e.g. using old
     * constraints, tags etc. However, on the next run, e.g. retry of one-time Worker or another
     * iteration of periodic worker, the new worker specification will be used.
     *
     * If the one time work that is updated is already finished the returned ListenableFuture will
     * be completed with [UpdateResult.NOT_APPLIED].
     *
     * If update can be applied immediately, e.g. the updated work isn't currently running, the
     * returned ListenableFuture will be completed with [UpdateResult.APPLIED_IMMEDIATELY].
     *
     * If the work with the given id (`request.getId()`) doesn't exist the returned ListenableFuture
     * will be completed exceptionally with [IllegalArgumentException].
     *
     * Worker type can't be changed, [OneTimeWorkRequest] can't be updated to [PeriodicWorkRequest]
     * and otherwise, the returned ListenableFuture will be completed with
     * [IllegalArgumentException].
     *
     * @param request the new specification for the work.
     * @return a [ListenableFuture] that will be successfully completed if the update was
     *   successful. The future will be completed with an exception if the work is already running
     *   or finished.
     */
    // consistent with already existent method like getWorkInfos() in WorkManager
    @Suppress("AsyncSuffixFuture")
    public abstract fun updateWork(request: WorkRequest): ListenableFuture<UpdateResult>

    /** An enumeration of results for [WorkManager.updateWork] method. */
    public enum class UpdateResult {
        /** An update wasn't applied, because `Worker` has already finished. */
        NOT_APPLIED,

        /**
         * An update was successfully applied immediately, meaning the updated work wasn't currently
         * running in the moment of the request. See [UpdateResult.APPLIED_FOR_NEXT_RUN] for the
         * case of running worker.
         */
        APPLIED_IMMEDIATELY,

        /**
         * An update was successfully applied, but the worker being updated was running. This run
         * isn't interrupted and will continue to rely on previous state of the request, e.g. using
         * old constraints, tags etc. However, on the next run, e.g. retry of one-time Worker or
         * another iteration of periodic worker, the new worker specification. will be used.
         */
        APPLIED_FOR_NEXT_RUN,
    }
}
