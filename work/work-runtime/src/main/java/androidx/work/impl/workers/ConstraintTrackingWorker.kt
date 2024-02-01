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
package androidx.work.impl.workers

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.Logger
import androidx.work.WorkInfo.Companion.STOP_REASON_NOT_STOPPED
import androidx.work.WorkInfo.Companion.STOP_REASON_UNKNOWN
import androidx.work.WorkerExceptionInfo
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.ConstraintsState.ConstraintsNotMet
import androidx.work.impl.constraints.WorkConstraintsTracker
import androidx.work.impl.model.WorkSpec
import androidx.work.logd
import androidx.work.loge
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Is an implementation of a [androidx.work.Worker] that can delegate to a different
 * [androidx.work.Worker] when the constraints are met.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConstraintTrackingWorker(
    appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        return withContext(backgroundExecutor.asCoroutineDispatcher()) {
            setupAndRunConstraintTrackingWork()
        }
    }

    private suspend fun setupAndRunConstraintTrackingWork(): Result {
        val className = inputData.getString(ARGUMENT_CLASS_NAME)
        if (className.isNullOrEmpty()) {
            loge(TAG) { "No worker to delegate to." }
            return Result.failure()
        }
        val workManagerImpl = WorkManagerImpl.getInstance(applicationContext)
        // We need to know what the real constraints are for the delegate.
        val workSpec = workManagerImpl.workDatabase.workSpecDao().getWorkSpec(id.toString())
            ?: return Result.failure()
        val workConstraintsTracker = WorkConstraintsTracker(workManagerImpl.trackers)
        if (!workConstraintsTracker.areAllConstraintsMet(workSpec)) {
            logd(TAG) { "Constraints not met for delegate $className. Requesting retry." }
            return Result.retry()
        }
        logd(TAG) { "Constraints met for delegate $className" }
        val delegate = try {
            workerFactory.createWorkerWithDefaultFallback(
                applicationContext, className, workerParameters
            )
        } catch (e: Throwable) {
            logd(TAG) { "No worker to delegate to." }
            try {
                workManagerImpl.configuration.workerInitializationExceptionHandler?.accept(
                    WorkerExceptionInfo(className, workerParameters, e)
                )
            } catch (exception: Exception) {
                loge(TAG, exception) {
                    "Exception handler threw an exception"
                }
            }
            return Result.failure()
        }
        val mainThreadExecutor = workerParameters.taskExecutor.mainThreadExecutor
        return withContext(mainThreadExecutor.asCoroutineDispatcher()) {
            runWorker(delegate, workConstraintsTracker, workSpec)
        }
    }

    private suspend fun runWorker(
        delegate: ListenableWorker,
        workConstraintsTracker: WorkConstraintsTracker,
        workSpec: WorkSpec
    ): Result = coroutineScope {
        val atomicReason = AtomicInteger(STOP_REASON_NOT_STOPPED)
        // it is !!IMPORTANT!! to not have suspension points in-between startWork and .await() call
        // Because once we called startWork we should correctly guarantee to propagate cancellation
        // to the future and to call .stop() on the worker.
        val future = delegate.startWork()
        val constraintTrackingJob = launch {
            val reason = workConstraintsTracker.awaitConstraintsNotMet(workSpec)
            atomicReason.set(reason)
            future.cancel(true)
        }
        try {
            val result = future.await()
            result
        } catch (t: Throwable) {
            logd(TAG, t) { "Delegated worker ${delegate.javaClass} threw exception in startWork." }
            val constraintFailed = atomicReason.get() != STOP_REASON_NOT_STOPPED
            if (future.isCancelled && !delegate.isStopped && (constraintFailed || isStopped)) {
                val reason = when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> STOP_REASON_UNKNOWN
                    isStopped -> stopReason
                    else -> atomicReason.get()
                }
                delegate.stop(reason)
                Result.retry()
            } else Result.failure()
        } finally {
            constraintTrackingJob.cancel()
        }
    }
}

private suspend fun WorkConstraintsTracker.awaitConstraintsNotMet(workSpec: WorkSpec) =
    track(workSpec)
        .onEach { logd(TAG) { "Constraints changed for $workSpec" } }
        .filterIsInstance<ConstraintsNotMet>()
        .first()
        .reason

private val TAG = Logger.tagWithPrefix("ConstraintTrkngWrkr")

/**
 * The `className` of the [androidx.work.Worker] to delegate to.
 */
internal const val ARGUMENT_CLASS_NAME =
    "androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME"
