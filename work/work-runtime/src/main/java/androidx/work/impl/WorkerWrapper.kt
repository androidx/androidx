/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.impl

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.work.Clock
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.DirectExecutor
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.Logger
import androidx.work.WorkInfo
import androidx.work.WorkerExceptionInfo
import androidx.work.WorkerParameters
import androidx.work.impl.WorkerWrapper.Resolution.ResetWorkerStatus
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.model.DependencyDao
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.model.generationalId
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.safeAccept
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.utils.workForeground
import androidx.work.launchFuture
import androidx.work.logd
import androidx.work.loge
import androidx.work.logi
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import kotlin.collections.removeLast as removeLastKt
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * A runnable that looks up the [WorkSpec] from the database for a given id, instantiates its
 * Worker, and then calls it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WorkerWrapper internal constructor(builder: Builder) {
    val workSpec: WorkSpec = builder.workSpec
    private val appContext: Context = builder.appContext
    private val workSpecId: String = workSpec.id
    private val runtimeExtras: WorkerParameters.RuntimeExtras = builder.runtimeExtras

    private val builderWorker: ListenableWorker? = builder.worker
    private val workTaskExecutor: TaskExecutor = builder.workTaskExecutor

    private val configuration: Configuration = builder.configuration
    private val clock: Clock = configuration.clock
    private val foregroundProcessor: ForegroundProcessor = builder.foregroundProcessor
    private val workDatabase: WorkDatabase = builder.workDatabase
    private val workSpecDao: WorkSpecDao = workDatabase.workSpecDao()
    private val dependencyDao: DependencyDao = workDatabase.dependencyDao()
    private val tags: List<String> = builder.tags
    private val workDescription: String = createWorkDescription(tags)

    private val workerJob = Job()

    val workGenerationalId: WorkGenerationalId
        get() = workSpec.generationalId()

    fun launch(): ListenableFuture<Boolean> =
        launchFuture(workTaskExecutor.taskCoroutineDispatcher + Job()) {
            val resolution: Resolution =
                try {
                    // we're wrapping runWorker in separate job, so we can always run post
                    // processing
                    // without a fear of being cancelled.
                    withContext(workerJob) { runWorker() }
                } catch (workerStoppedException: WorkerStoppedException) {
                    ResetWorkerStatus(workerStoppedException.reason)
                } catch (e: CancellationException) {
                    // means that worker was self-cancelled, which we treat as failure
                    Resolution.Failed()
                } catch (throwable: Throwable) {
                    loge(TAG, throwable) { "Unexpected error in WorkerWrapper" }
                    Resolution.Failed()
                }
            workDatabase.runInTransaction(
                Callable {
                    when (resolution) {
                        is Resolution.Finished -> onWorkFinished(resolution.result)
                        is Resolution.Failed -> {
                            setFailed(resolution.result)
                            false
                        }
                        is ResetWorkerStatus -> resetWorkerStatus(resolution.reason)
                    }
                }
            )
        }

    private sealed class Resolution {
        class ResetWorkerStatus(val reason: Int = WorkInfo.STOP_REASON_NOT_STOPPED) : Resolution()

        class Failed(val result: ListenableWorker.Result = Failure()) : Resolution()

        class Finished(val result: ListenableWorker.Result) : Resolution()
    }

    private suspend fun runWorker(): Resolution {
        val isTracingEnabled = configuration.tracer.isEnabled()
        val traceTag = workSpec.traceTag
        if (isTracingEnabled && traceTag != null) {
            configuration.tracer.beginAsyncSection(
                traceTag,
                // Use hashCode() instead of a generational id given we want to allow concurrent
                // execution of Workers with the same name. Additionally `generation` is already
                // a part of the WorkSpec's hashCode.
                workSpec.hashCode()
            )
        }
        // Needed for nested transactions, such as when we're in a dependent work request when
        // using a SynchronousExecutor.
        val shouldExit =
            workDatabase.runInTransaction(
                Callable {
                    // Do a quick check to make sure we don't need to bail out in case this work is
                    // already
                    // running, finished, or is blocked.
                    if (workSpec.state !== WorkInfo.State.ENQUEUED) {
                        logd(TAG) {
                            "${workSpec.workerClassName} is not in ENQUEUED state. Nothing more to do"
                        }
                        return@Callable true
                    }

                    // Case 1:
                    // Ensure that Workers that are backed off are only executed when they are
                    // supposed to.
                    // GreedyScheduler can schedule WorkSpecs that have already been backed off
                    // because
                    // it is holding on to snapshots of WorkSpecs. So WorkerWrapper needs to
                    // determine
                    // if the ListenableWorker is actually eligible to execute at this point in
                    // time.

                    // Case 2:
                    // On API 23, we double scheduler Workers because JobScheduler prefers batching.
                    // So is the Work is periodic, we only need to execute it once per interval.
                    // Also potential bugs in the platform may cause a Job to run more than once.
                    if (workSpec.isPeriodic || workSpec.isBackedOff) {
                        val now = clock.currentTimeMillis()
                        if (now < workSpec.calculateNextRunTime()) {
                            Logger.get()
                                .debug(
                                    TAG,
                                    "Delaying execution for ${workSpec.workerClassName} because it is " +
                                        "being executed before schedule.",
                                )

                            // For AlarmManager implementation we need to reschedule this kind  of
                            // Work.
                            // This is not a problem for JobScheduler because we will only
                            // reschedule
                            // work if JobScheduler is unaware of a jobId.
                            return@Callable true
                        }
                    }
                    return@Callable false
                }
            )

        if (shouldExit) return ResetWorkerStatus()

        // Merge inputs.  This can be potentially expensive code, so this should not be done inside
        // a database transaction.
        val input: Data =
            if (workSpec.isPeriodic) {
                workSpec.input
            } else {
                val inputMergerFactory = configuration.inputMergerFactory
                val inputMergerClassName = workSpec.inputMergerClassName
                val inputMerger =
                    inputMergerFactory.createInputMergerWithDefaultFallback(inputMergerClassName)
                if (inputMerger == null) {
                    loge(TAG) { "Could not create Input Merger ${workSpec.inputMergerClassName}" }
                    return Resolution.Failed()
                }
                val inputs =
                    listOf(workSpec.input) + workSpecDao.getInputsFromPrerequisites(workSpecId)
                inputMerger.merge(inputs)
            }
        val params =
            WorkerParameters(
                UUID.fromString(workSpecId),
                input,
                tags,
                runtimeExtras,
                workSpec.runAttemptCount,
                workSpec.generation,
                configuration.executor,
                configuration.workerCoroutineContext,
                workTaskExecutor,
                configuration.workerFactory,
                WorkProgressUpdater(workDatabase, workTaskExecutor),
                WorkForegroundUpdater(workDatabase, foregroundProcessor, workTaskExecutor)
            )

        // Not always creating a worker here, as the WorkerWrapper.Builder can set a worker override
        // in test mode.
        val worker =
            builderWorker
                ?: try {
                    configuration.workerFactory.createWorkerWithDefaultFallback(
                        appContext,
                        workSpec.workerClassName,
                        params
                    )
                } catch (e: Throwable) {
                    loge(TAG) { "Could not create Worker ${workSpec.workerClassName}" }

                    configuration.workerInitializationExceptionHandler?.safeAccept(
                        WorkerExceptionInfo(workSpec.workerClassName, params, e),
                        TAG
                    )
                    return Resolution.Failed()
                }
        worker.setUsed()
        // we specifically use coroutineContext[Job] instead of workerJob
        // because it will be complete once withContext finishes.
        // This way if worker has successfully finished and then
        // interrupt() is called, then it is ignored, because
        // job is already completed.
        val job = coroutineContext[Job]!!

        // worker stopping is complicated process.
        // Historical behavior that we are trying to preserve is that
        // worker.onStopped is always called in case of stoppage since the worker is instantiated,
        // no matter if other methods such as startWork or getForegroundInfoAsync were called.
        //
        // Another important behavior is that worker should be marked as stopped before
        // calling .cancel() on the future returned from the startWork(). So the listeners of this
        // future could check what was the stop reason via `getStopReason()`, including listeners
        // that were added with the direct executor.
        // worker.stop() could be safely called multiple times, (only first one is effective),
        // and we rely on this property.
        // The completion listener below is for the cases when
        // 1. getForegroundInfoAsync / startWork weren't called yet at all
        // 2. when WorkerWrapper received stop signal when getForegroundInfoAsync() completed
        //    and startWork() hasn't been called yet.
        // 3. startWork's future was completed, but job was cancelled before we actually received
        //    a notification about future's completion. (it is the natural race between stop signal
        //    and future completion, that we can't avoid. In this case worker will be decided as
        //    stopped and re-enqueued for another attempt)
        job.invokeOnCompletion {
            if (it is WorkerStoppedException) {
                worker.stop(it.reason)
            }
            if (isTracingEnabled && traceTag != null) {
                configuration.tracer.endAsyncSection(traceTag, workSpec.hashCode())
            }
        }

        // Try to set the work to the running state.  Note that this may fail because another thread
        // may have modified the DB since we checked last at the top of this function.
        if (!trySetRunning()) {
            return ResetWorkerStatus()
        }

        if (job.isCancelled) {
            // doesn't matter job is cancelled anyway
            return ResetWorkerStatus()
        }

        val foregroundUpdater = params.foregroundUpdater
        val mainDispatcher = workTaskExecutor.getMainThreadExecutor().asCoroutineDispatcher()
        try {
            val result =
                withContext(mainDispatcher) {
                    workForeground(
                        appContext,
                        workSpec,
                        worker,
                        foregroundUpdater,
                        workTaskExecutor
                    )
                    logd(TAG) { "Starting work for ${workSpec.workerClassName}" }
                    // *important* we can't pass future around suspension points
                    // because we will lose cancellation, so we have to await
                    // right here on the main thread.
                    worker.startWork().awaitWithin(worker)
                }
            return Resolution.Finished(result)
        } catch (cancellation: CancellationException) {
            logi(TAG, cancellation) { "$workDescription was cancelled" }
            throw cancellation
        } catch (throwable: Throwable) {
            loge(TAG, throwable) { "$workDescription failed because it threw an exception/error" }
            configuration.workerExecutionExceptionHandler?.safeAccept(
                WorkerExceptionInfo(workSpec.workerClassName, params, throwable),
                TAG
            )
            return Resolution.Failed()
        }
    }

    private fun onWorkFinished(result: ListenableWorker.Result): Boolean {
        val state = workSpecDao.getState(workSpecId)
        workDatabase.workProgressDao().delete(workSpecId)
        return if (state == null) {
            // state can be null here with a REPLACE on beginUniqueWork().
            // Treat it as a failure, and rescheduleAndResolve() will
            // turn into a no-op. We still need to notify potential observers
            // holding on to wake locks on our behalf.
            false
        } else if (state === WorkInfo.State.RUNNING) {
            handleResult(result)
        } else if (!state.isFinished) {
            // counting this is stopped with unknown reason
            reschedule(WorkInfo.STOP_REASON_UNKNOWN)
        } else {
            false
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interrupt(stopReason: Int) {
        workerJob.cancel(WorkerStoppedException(stopReason))
    }

    private fun resetWorkerStatus(stopReason: Int): Boolean {
        val state = workSpecDao.getState(workSpecId)
        return if (state != null && !state.isFinished) {
            logd(TAG) {
                "Status for $workSpecId is $state; not doing any work and " +
                    "rescheduling for later execution"
            }
            // Set state to ENQUEUED again.
            // Reset scheduled state so it's picked up by background schedulers again.
            // We want to preserve time when work was enqueued so just explicitly set enqueued
            // instead using markEnqueuedState. Similarly, don't change any override time.
            workSpecDao.setState(WorkInfo.State.ENQUEUED, workSpecId)
            workSpecDao.setStopReason(workSpecId, stopReason)
            workSpecDao.markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET)
            true
        } else {
            logd(TAG) { "Status for $workSpecId is $state ; not doing any work" }
            false
        }
    }

    private fun handleResult(result: ListenableWorker.Result?): Boolean {
        return if (result is ListenableWorker.Result.Success) {
            logi(TAG) { "Worker result SUCCESS for $workDescription" }
            if (workSpec.isPeriodic) {
                resetPeriodic()
            } else {
                setSucceeded(result)
            }
        } else if (result is ListenableWorker.Result.Retry) {
            logi(TAG) { "Worker result RETRY for $workDescription" }
            reschedule(WorkInfo.STOP_REASON_NOT_STOPPED)
        } else {
            logi(TAG) { "Worker result FAILURE for $workDescription" }
            if (workSpec.isPeriodic) {
                resetPeriodic()
            } else {
                // we have here either failure or null
                setFailed(result ?: Failure())
            }
        }
    }

    private fun trySetRunning(): Boolean =
        workDatabase.runInTransaction(
            Callable {
                val currentState = workSpecDao.getState(workSpecId)
                if (currentState === WorkInfo.State.ENQUEUED) {
                    workSpecDao.setState(WorkInfo.State.RUNNING, workSpecId)
                    workSpecDao.incrementWorkSpecRunAttemptCount(workSpecId)
                    workSpecDao.setStopReason(workSpecId, WorkInfo.STOP_REASON_NOT_STOPPED)
                    true
                } else false
            }
        )

    @VisibleForTesting
    fun setFailed(result: ListenableWorker.Result): Boolean {
        iterativelyFailWorkAndDependents(workSpecId)
        val failure = result as Failure
        // Update Data as necessary.
        val output = failure.outputData
        workSpecDao.resetWorkSpecNextScheduleTimeOverride(
            workSpecId,
            workSpec.nextScheduleTimeOverrideGeneration
        )
        workSpecDao.setOutput(workSpecId, output)
        return false
    }

    private fun iterativelyFailWorkAndDependents(workSpecId: String) {
        val idsToProcess = mutableListOf(workSpecId)
        while (idsToProcess.isNotEmpty()) {
            val id = idsToProcess.removeLastKt()
            // Don't fail already cancelled work.
            if (workSpecDao.getState(id) !== WorkInfo.State.CANCELLED) {
                workSpecDao.setState(WorkInfo.State.FAILED, id)
            }
            idsToProcess.addAll(dependencyDao.getDependentWorkIds(id))
        }
    }

    private fun reschedule(stopReason: Int): Boolean {
        workSpecDao.setState(WorkInfo.State.ENQUEUED, workSpecId)
        workSpecDao.setLastEnqueueTime(workSpecId, clock.currentTimeMillis())
        workSpecDao.resetWorkSpecNextScheduleTimeOverride(
            workSpecId,
            workSpec.nextScheduleTimeOverrideGeneration
        )
        workSpecDao.markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET)
        workSpecDao.setStopReason(workSpecId, stopReason)
        return true
    }

    private fun resetPeriodic(): Boolean {
        // The system clock may have been changed such that the lastEnqueueTime was in the past.
        // Therefore we always use the current time to determine the next run time of a Worker.
        // This way, the Schedulers will correctly schedule the next instance of the
        // PeriodicWork in the future. This happens in calculateNextRunTime() in WorkSpec.
        workSpecDao.setLastEnqueueTime(workSpecId, clock.currentTimeMillis())
        workSpecDao.setState(WorkInfo.State.ENQUEUED, workSpecId)
        workSpecDao.resetWorkSpecRunAttemptCount(workSpecId)
        workSpecDao.resetWorkSpecNextScheduleTimeOverride(
            workSpecId,
            workSpec.nextScheduleTimeOverrideGeneration
        )
        workSpecDao.incrementPeriodCount(workSpecId)
        workSpecDao.markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET)
        return false
    }

    private fun setSucceeded(result: ListenableWorker.Result): Boolean {
        workSpecDao.setState(WorkInfo.State.SUCCEEDED, workSpecId)
        val success = result as ListenableWorker.Result.Success
        // Update Data as necessary.
        val output = success.outputData
        workSpecDao.setOutput(workSpecId, output)

        // Unblock Dependencies and set Period Start Time
        val currentTimeMillis = clock.currentTimeMillis()
        val dependentWorkIds = dependencyDao.getDependentWorkIds(workSpecId)
        for (dependentWorkId in dependentWorkIds) {
            if (
                workSpecDao.getState(dependentWorkId) === WorkInfo.State.BLOCKED &&
                    dependencyDao.hasCompletedAllPrerequisites(dependentWorkId)
            ) {
                logi(TAG) { "Setting status to enqueued for $dependentWorkId" }
                workSpecDao.setState(WorkInfo.State.ENQUEUED, dependentWorkId)
                workSpecDao.setLastEnqueueTime(dependentWorkId, currentTimeMillis)
            }
        }
        return false
    }

    private fun createWorkDescription(tags: List<String>) =
        "Work [ id=$workSpecId, tags={ ${tags.joinToString(",")} } ]"

    /** Builder class for [WorkerWrapper] */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder
    @SuppressLint("LambdaLast")
    constructor(
        context: Context,
        val configuration: Configuration,
        val workTaskExecutor: TaskExecutor,
        val foregroundProcessor: ForegroundProcessor,
        val workDatabase: WorkDatabase,
        val workSpec: WorkSpec,
        val tags: List<String>
    ) {
        val appContext: Context = context.applicationContext
        var worker: ListenableWorker? = null
        var runtimeExtras = WorkerParameters.RuntimeExtras()

        /**
         * @param runtimeExtras The [WorkerParameters.RuntimeExtras] for the worker; if this is
         *   `null`, it will be ignored and the default value will be retained.
         * @return The instance of [Builder] for chaining.
         */
        fun withRuntimeExtras(runtimeExtras: WorkerParameters.RuntimeExtras?): Builder {
            if (runtimeExtras != null) {
                this.runtimeExtras = runtimeExtras
            }
            return this
        }

        /**
         * @param worker The instance of [ListenableWorker] to be executed by [WorkerWrapper].
         *   Useful in the context of testing.
         * @return The instance of [Builder] for chaining.
         */
        @VisibleForTesting
        fun withWorker(worker: ListenableWorker): Builder {
            this.worker = worker
            return this
        }

        /** @return The instance of [WorkerWrapper]. */
        fun build(): WorkerWrapper {
            return WorkerWrapper(this)
        }
    }
}

private val TAG = Logger.tagWithPrefix("WorkerWrapper")

// copy of await() function but with specific cancellation propagation.
// it is needed that we specifically want to call .stop() on worker itself before
// calling cancel() of the future.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend fun <T> ListenableFuture<T>.awaitWithin(worker: ListenableWorker): T {
    try {
        if (isDone) return getUninterruptibly(this)
    } catch (e: ExecutionException) {
        // ExecutionException is the only kind of exception that can be thrown from a gotten
        // Future, other than CancellationException. Cancellation is propagated upward so that
        // the coroutine running this suspend function may process it.
        // Any other Exception showing up here indicates a very fundamental bug in a
        // Future implementation.
        throw e.nonNullCause()
    }

    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        addListener(ToContinuation(this, cont), DirectExecutor.INSTANCE)
        cont.invokeOnCancellation {
            if (it is WorkerStoppedException) {
                worker.stop(it.reason)
            }
            cancel(false)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WorkerStoppedException(val reason: Int) : CancellationException()

private class ToContinuation<T>(
    val futureToObserve: ListenableFuture<T>,
    val continuation: CancellableContinuation<T>
) : Runnable {
    override fun run() {
        if (futureToObserve.isCancelled) {
            continuation.cancel()
        } else {
            try {
                continuation.resumeWith(Result.success(getUninterruptibly(futureToObserve)))
            } catch (e: ExecutionException) {
                // ExecutionException is the only kind of exception that can be thrown from a gotten
                // Future. Anything else showing up here indicates a very fundamental bug in a
                // Future implementation.
                continuation.resumeWithException(e.nonNullCause())
            }
        }
    }
}

private fun <V> getUninterruptibly(future: Future<V>): V {
    var interrupted = false
    try {
        while (true) {
            try {
                return future.get()
            } catch (e: InterruptedException) {
                interrupted = true
            }
        }
    } finally {
        if (interrupted) {
            Thread.currentThread().interrupt()
        }
    }
}

private fun ExecutionException.nonNullCause(): Throwable {
    return this.cause!!
}
