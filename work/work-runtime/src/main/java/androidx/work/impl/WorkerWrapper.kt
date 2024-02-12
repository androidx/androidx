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
import androidx.annotation.WorkerThread
import androidx.work.Clock
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.Logger
import androidx.work.WorkInfo
import androidx.work.WorkerExceptionInfo
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.impl.background.systemalarm.RescheduleReceiver
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.model.DependencyDao
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.model.generationalId
import androidx.work.impl.utils.PackageManagerHelper
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.futures.SettableFuture
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * A runnable that looks up the [WorkSpec] from the database for a given id, instantiates
 * its Worker, and then calls it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WorkerWrapper internal constructor(builder: Builder) : Runnable {
    val workSpec: WorkSpec = builder.workSpec
    private val appContext: Context = builder.appContext
    private val workSpecId: String = workSpec.id
    private val runtimeExtras: WorkerParameters.RuntimeExtras = builder.runtimeExtras

    private var worker: ListenableWorker? = builder.worker
    private val workTaskExecutor: TaskExecutor = builder.workTaskExecutor

    private val configuration: Configuration = builder.configuration
    private val clock: Clock = configuration.clock
    private val foregroundProcessor: ForegroundProcessor = builder.foregroundProcessor
    private val workDatabase: WorkDatabase = builder.workDatabase
    private val workSpecDao: WorkSpecDao = workDatabase.workSpecDao()
    private val dependencyDao: DependencyDao = workDatabase.dependencyDao()
    private val tags: List<String> = builder.tags
    private var workDescription: String? = null

    private val _future: SettableFuture<Boolean> = SettableFuture.create()

    private val workerResultFuture: SettableFuture<ListenableWorker.Result> =
        SettableFuture.create()

    @Volatile
    private var interrupted = WorkInfo.STOP_REASON_NOT_STOPPED

    val workGenerationalId: WorkGenerationalId
        get() = workSpec.generationalId()
    val future: ListenableFuture<Boolean>
        get() = _future

    @WorkerThread
    override fun run() {
        workDescription = createWorkDescription(tags)
        runWorker()
    }

    private fun runWorker() {
        if (tryCheckForInterruptionAndResolve()) {
            return
        }

        // Needed for nested transactions, such as when we're in a dependent work request when
        // using a SynchronousExecutor.
        val shouldExit = workDatabase.runInTransaction(Callable {
            // Do a quick check to make sure we don't need to bail out in case this work is already
            // running, finished, or is blocked.
            if (workSpec.state !== WorkInfo.State.ENQUEUED) {
                resolveIncorrectStatus()
                logd(TAG) {
                    "${workSpec.workerClassName} is not in ENQUEUED state. Nothing more to do"
                }
                return@Callable true
            }

            // Case 1:
            // Ensure that Workers that are backed off are only executed when they are supposed to.
            // GreedyScheduler can schedule WorkSpecs that have already been backed off because
            // it is holding on to snapshots of WorkSpecs. So WorkerWrapper needs to determine
            // if the ListenableWorker is actually eligible to execute at this point in time.

            // Case 2:
            // On API 23, we double scheduler Workers because JobScheduler prefers batching.
            // So is the Work is periodic, we only need to execute it once per interval.
            // Also potential bugs in the platform may cause a Job to run more than once.
            if (workSpec.isPeriodic || workSpec.isBackedOff) {
                val now = clock.currentTimeMillis()
                if (now < workSpec.calculateNextRunTime()) {
                    Logger.get().debug(
                        TAG,
                        "Delaying execution for ${workSpec.workerClassName} because it is " +
                            "being executed before schedule.",
                    )

                    // For AlarmManager implementation we need to reschedule this kind  of Work.
                    // This is not a problem for JobScheduler because we will only reschedule
                    // work if JobScheduler is unaware of a jobId.
                    resolve(true)
                    return@Callable true
                }
            }
            return@Callable false
        })

        if (shouldExit) return

        // Merge inputs.  This can be potentially expensive code, so this should not be done inside
        // a database transaction.
        val input: Data = if (workSpec.isPeriodic) {
            workSpec.input
        } else {
            val inputMergerFactory = configuration.inputMergerFactory
            val inputMergerClassName = workSpec.inputMergerClassName
            val inputMerger =
                inputMergerFactory.createInputMergerWithDefaultFallback(inputMergerClassName)
            if (inputMerger == null) {
                loge(TAG) { "Could not create Input Merger ${workSpec.inputMergerClassName}" }
                setFailedAndResolve(Failure())
                return
            }
            val inputs = listOf(workSpec.input) + workSpecDao.getInputsFromPrerequisites(workSpecId)
            inputMerger.merge(inputs)
        }
        val params = WorkerParameters(
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
        val worker = worker
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
                setFailedAndResolve(Failure())
                return
            }
        worker.setUsed()
        this.worker = worker

        // Try to set the work to the running state.  Note that this may fail because another thread
        // may have modified the DB since we checked last at the top of this function.
        if (trySetRunning()) {
            if (tryCheckForInterruptionAndResolve()) {
                return
            }
            val foregroundUpdater = params.foregroundUpdater
            val mainDispatcher = workTaskExecutor.getMainThreadExecutor().asCoroutineDispatcher()
            val future = launchFuture(mainDispatcher + Job()) {
                workForeground(appContext, workSpec, worker, foregroundUpdater, workTaskExecutor)
                logd(TAG) { "Starting work for ${workSpec.workerClassName}" }
                worker.startWork().await()
            }
            workerResultFuture.setFuture(future)
            // Avoid synthetic accessors.
            val workDescription = workDescription
            workerResultFuture.addListener({
                var result: ListenableWorker.Result = Failure()
                try {
                    // If the ListenableWorker returns a null result treat it as a failure.
                    val futureResult = workerResultFuture.get()
                    result = if (futureResult == null) {
                        loge(TAG) {
                            workSpec.workerClassName +
                                " returned a null result. Treating it as a failure."
                        }
                        Failure()
                    } else {
                        logd(TAG) { "${workSpec.workerClassName} returned a $futureResult." }
                        futureResult
                    }
                } catch (exception: CancellationException) {
                    // Cancellations need to be treated with care here because innerFuture
                    // cancellations will bubble up, and we need to gracefully handle that.
                    logi(TAG, exception) { "$workDescription was cancelled" }
                } catch (exception: Exception) {
                    val exceptionToReport = if (exception is ExecutionException) {
                        exception.cause ?: exception
                    } else {
                        exception
                    }
                    loge(TAG, exceptionToReport) {
                        "$workDescription failed because it threw an exception/error"
                    }
                    configuration.workerExecutionExceptionHandler?.safeAccept(
                        WorkerExceptionInfo(workSpec.workerClassName, params, exceptionToReport),
                        TAG
                    )
                } finally {
                    onWorkFinished(result)
                }
            }, workTaskExecutor.getSerialTaskExecutor())
        } else {
            resolveIncorrectStatus()
        }
    }

    private fun onWorkFinished(result: ListenableWorker.Result) {
        if (!tryCheckForInterruptionAndResolve()) {
            workDatabase.runInTransaction {
                val state = workSpecDao.getState(workSpecId)
                workDatabase.workProgressDao().delete(workSpecId)
                if (state == null) {
                    // state can be null here with a REPLACE on beginUniqueWork().
                    // Treat it as a failure, and rescheduleAndResolve() will
                    // turn into a no-op. We still need to notify potential observers
                    // holding on to wake locks on our behalf.
                    resolve(false)
                } else if (state === WorkInfo.State.RUNNING) {
                    handleResult(result)
                } else if (!state.isFinished) {
                    // counting this is stopped with unknown reason
                    interrupted = WorkInfo.STOP_REASON_UNKNOWN
                    rescheduleAndResolve()
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interrupt(stopReason: Int) {
        interrupted = stopReason
        // Resolve WorkerWrapper's future so we do the right thing and setup a reschedule
        // if necessary. mInterrupted is always true here, we don't really care about the return
        // value.
        tryCheckForInterruptionAndResolve()
        // Propagate the cancellations to the inner future.
        workerResultFuture.cancel(true)
        // Worker can be null if run() hasn't been called yet
        // only call stop if it wasn't completed normally.
        val worker = worker
        if (worker != null && workerResultFuture.isCancelled()) {
            worker.stop(stopReason)
        } else {
            logd(TAG) { "WorkSpec $workSpec is already done. Not interrupting." }
        }
    }

    private fun resolveIncorrectStatus() {
        val status = workSpecDao.getState(workSpecId)
        if (status === WorkInfo.State.RUNNING) {
            logd(TAG) {
                "Status for $workSpecId is RUNNING; not doing any work and " +
                    "rescheduling for later execution"
            }
            resolve(true)
        } else {
            logd(TAG) { "Status for $workSpecId is $status ; not doing any work" }
            resolve(false)
        }
    }

    private fun tryCheckForInterruptionAndResolve(): Boolean {
        // Interruptions can happen when:
        // An explicit cancel* signal
        // A change in constraint, which causes WorkManager to stop the Worker.
        // Worker exceeding a 10 min execution window.
        // One scheduler completing a Worker, and telling other Schedulers to cleanup.
        if (interrupted != WorkInfo.STOP_REASON_NOT_STOPPED) {
            logd(TAG) { "Work interrupted for $workDescription" }
            val currentState = workSpecDao.getState(workSpecId)
            if (currentState == null) {
                // This can happen because of a beginUniqueWork(..., REPLACE, ...).  Notify the
                // listeners so we can clean up any wake locks, etc.
                resolve(false)
            } else {
                resolve(!currentState.isFinished)
            }
            return true
        }
        return false
    }

    private fun resolve(needsReschedule: Boolean) {
        workDatabase.runInTransaction {
            // IMPORTANT: We are using a transaction here as to ensure that we have some guarantees
            // about the state of the world before we disable RescheduleReceiver.

            // Check to see if there is more work to be done. If there is no more work, then
            // disable RescheduleReceiver. Using a transaction here, as there could be more than
            // one thread looking at the list of eligible WorkSpecs.
            val hasUnfinishedWork = workDatabase.workSpecDao().hasUnfinishedWork()
            if (!hasUnfinishedWork) {
                PackageManagerHelper.setComponentEnabled(
                    appContext, RescheduleReceiver::class.java, false
                )
            }
            if (needsReschedule) {
                // Set state to ENQUEUED again.
                // Reset scheduled state so it's picked up by background schedulers again.
                // We want to preserve time when work was enqueued so just explicitly set enqueued
                // instead using markEnqueuedState. Similarly, don't change any override time.
                workSpecDao.setState(WorkInfo.State.ENQUEUED, workSpecId)
                workSpecDao.setStopReason(workSpecId, interrupted)
                workSpecDao.markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET)
            }
        }
        _future.set(needsReschedule)
    }

    private fun handleResult(result: ListenableWorker.Result?) {
        if (result is ListenableWorker.Result.Success) {
            logi(TAG) { "Worker result SUCCESS for $workDescription" }
            if (workSpec.isPeriodic) {
                resetPeriodicAndResolve()
            } else {
                setSucceededAndResolve(result)
            }
        } else if (result is ListenableWorker.Result.Retry) {
            logi(TAG) { "Worker result RETRY for $workDescription" }
            rescheduleAndResolve()
        } else {
            logi(TAG) { "Worker result FAILURE for $workDescription" }
            if (workSpec.isPeriodic) {
                resetPeriodicAndResolve()
            } else {
                // we have here either failure or null
                setFailedAndResolve(result ?: Failure())
            }
        }
    }

    private fun trySetRunning(): Boolean = workDatabase.runInTransaction(
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
    fun setFailedAndResolve(result: ListenableWorker.Result) {
        resolve(false) {
            iterativelyFailWorkAndDependents(workSpecId)
            val failure = result as Failure
            // Update Data as necessary.
            val output = failure.outputData
            workSpecDao.resetWorkSpecNextScheduleTimeOverride(
                workSpecId,
                workSpec.nextScheduleTimeOverrideGeneration
            )
            workSpecDao.setOutput(workSpecId, output)
        }
    }

    private fun iterativelyFailWorkAndDependents(workSpecId: String) {
        val idsToProcess = mutableListOf(workSpecId)
        while (idsToProcess.isNotEmpty()) {
            val id = idsToProcess.removeLast()
            // Don't fail already cancelled work.
            if (workSpecDao.getState(id) !== WorkInfo.State.CANCELLED) {
                workSpecDao.setState(WorkInfo.State.FAILED, id)
            }
            idsToProcess.addAll(dependencyDao.getDependentWorkIds(id))
        }
    }

    private fun rescheduleAndResolve() {
        resolve(true) {
            workSpecDao.setState(WorkInfo.State.ENQUEUED, workSpecId)
            workSpecDao.setLastEnqueueTime(workSpecId, clock.currentTimeMillis())
            workSpecDao.resetWorkSpecNextScheduleTimeOverride(
                workSpecId,
                workSpec.nextScheduleTimeOverrideGeneration
            )
            workSpecDao.markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET)
        }
    }

    private fun resetPeriodicAndResolve() {
        resolve(false) {
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
        }
    }

    private fun setSucceededAndResolve(result: ListenableWorker.Result) {
        resolve(false) {
            workSpecDao.setState(WorkInfo.State.SUCCEEDED, workSpecId)
            val success = result as ListenableWorker.Result.Success
            // Update Data as necessary.
            val output = success.outputData
            workSpecDao.setOutput(workSpecId, output)

            // Unblock Dependencies and set Period Start Time
            val currentTimeMillis = clock.currentTimeMillis()
            val dependentWorkIds = dependencyDao.getDependentWorkIds(workSpecId)
            for (dependentWorkId in dependentWorkIds) {
                if (workSpecDao.getState(dependentWorkId) === WorkInfo.State.BLOCKED &&
                    dependencyDao.hasCompletedAllPrerequisites(dependentWorkId)
                ) {
                    logi(TAG) { "Setting status to enqueued for $dependentWorkId" }
                    workSpecDao.setState(WorkInfo.State.ENQUEUED, dependentWorkId)
                    workSpecDao.setLastEnqueueTime(dependentWorkId, currentTimeMillis)
                }
            }
        }
    }

    private fun resolve(reschedule: Boolean, block: () -> Unit) {
        try {
            workDatabase.runInTransaction(block)
        } finally {
            resolve(reschedule)
        }
    }

    private fun createWorkDescription(tags: List<String>) =
        "Work [ id=$workSpecId, tags={ ${tags.joinToString(",")} } ]"

    /**
     * Builder class for [WorkerWrapper]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder @SuppressLint("LambdaLast") constructor(
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
         * @param runtimeExtras The [WorkerParameters.RuntimeExtras] for the worker;
         * if this is `null`, it will be ignored and the default value
         * will be retained.
         * @return The instance of [Builder] for chaining.
         */
        fun withRuntimeExtras(runtimeExtras: WorkerParameters.RuntimeExtras?): Builder {
            if (runtimeExtras != null) {
                this.runtimeExtras = runtimeExtras
            }
            return this
        }

        /**
         * @param worker The instance of [ListenableWorker] to be executed by
         * [WorkerWrapper]. Useful in the context of testing.
         * @return The instance of [Builder] for chaining.
         */
        @VisibleForTesting
        fun withWorker(worker: ListenableWorker): Builder {
            this.worker = worker
            return this
        }

        /**
         * @return The instance of [WorkerWrapper].
         */
        fun build(): WorkerWrapper {
            return WorkerWrapper(this)
        }
    }
}

private val TAG = Logger.tagWithPrefix("WorkerWrapper")
