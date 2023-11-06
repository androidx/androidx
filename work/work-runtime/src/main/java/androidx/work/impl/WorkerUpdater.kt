/*
 * Copyright 2022 The Android Open Source Project
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
@file:JvmName("WorkerUpdater")

package androidx.work.impl

import androidx.annotation.RestrictTo
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.Operation
import androidx.work.Operation.State.FAILURE
import androidx.work.WorkInfo
import androidx.work.WorkManager.UpdateResult
import androidx.work.WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN
import androidx.work.WorkRequest
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.EnqueueRunnable
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.wrapInConstraintTrackingWorkerIfNeeded
import com.google.common.util.concurrent.ListenableFuture

private fun updateWorkImpl(
    processor: Processor,
    workDatabase: WorkDatabase,
    configuration: Configuration,
    schedulers: List<Scheduler>,
    newWorkSpec: WorkSpec,
    tags: Set<String>
): UpdateResult {
    val workSpecId = newWorkSpec.id
    val oldWorkSpec = workDatabase.workSpecDao().getWorkSpec(workSpecId)
        ?: throw IllegalArgumentException("Worker with $workSpecId doesn't exist")
    if (oldWorkSpec.state.isFinished) return UpdateResult.NOT_APPLIED
    if (oldWorkSpec.isPeriodic xor newWorkSpec.isPeriodic) {
        val type = { spec: WorkSpec -> if (spec.isPeriodic) "Periodic" else "OneTime" }
        throw UnsupportedOperationException(
            "Can't update ${type(oldWorkSpec)} Worker to ${type(newWorkSpec)} Worker. " +
                "Update operation must preserve worker's type."
        )
    }
    val isEnqueued = processor.isEnqueued(workSpecId)
    if (!isEnqueued) schedulers.forEach { scheduler -> scheduler.cancel(workSpecId) }
    workDatabase.runInTransaction {
        val workSpecDao = workDatabase.workSpecDao()
        val workTagDao = workDatabase.workTagDao()

        // should keep state BLOCKING, preserving the chain, or possibly RUNNING
        // preserving run attempt count, to calculate back off correctly, and enqueue/override time
        val updatedSpec = newWorkSpec.copy(
            state = oldWorkSpec.state,
            runAttemptCount = oldWorkSpec.runAttemptCount,
            lastEnqueueTime = oldWorkSpec.lastEnqueueTime,
            generation = oldWorkSpec.generation + 1,
            periodCount = oldWorkSpec.periodCount,
            nextScheduleTimeOverride = oldWorkSpec.nextScheduleTimeOverride,
            nextScheduleTimeOverrideGeneration = oldWorkSpec.nextScheduleTimeOverrideGeneration
        ).apply {
            if (newWorkSpec.nextScheduleTimeOverrideGeneration == 1) {
                nextScheduleTimeOverride = newWorkSpec.nextScheduleTimeOverride
                nextScheduleTimeOverrideGeneration += 1
                // Other fields are left unchanged, so they can be used after override is cleared.
            }
        }

        workSpecDao.updateWorkSpec(wrapInConstraintTrackingWorkerIfNeeded(schedulers, updatedSpec))
        workTagDao.deleteByWorkSpecId(workSpecId)
        workTagDao.insertTags(workSpecId, tags)
        if (!isEnqueued) {
            workSpecDao.markWorkSpecScheduled(workSpecId, WorkSpec.SCHEDULE_NOT_REQUESTED_YET)
            workDatabase.workProgressDao().delete(workSpecId)
        }
    }
    if (!isEnqueued) Schedulers.schedule(configuration, workDatabase, schedulers)
    return if (isEnqueued) APPLIED_FOR_NEXT_RUN else UpdateResult.APPLIED_IMMEDIATELY
}

internal fun WorkManagerImpl.updateWorkImpl(
    workRequest: WorkRequest
): ListenableFuture<UpdateResult> {
    val future = SettableFuture.create<UpdateResult>()
    workTaskExecutor.serialTaskExecutor.execute {
        if (future.isCancelled) return@execute
        try {
            val result = updateWorkImpl(
                processor, workDatabase,
                configuration, schedulers, workRequest.workSpec, workRequest.tags
            )
            future.set(result)
        } catch (e: Throwable) {
            future.setException(e)
        }
    }
    return future
}

/**
 * Enqueue or update the work.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun WorkManagerImpl.enqueueUniquelyNamedPeriodic(
    name: String,
    workRequest: WorkRequest,
): Operation {
    val operation = OperationImpl()
    val enqueueNew = {
        val requests = listOf(workRequest)
        val continuation = WorkContinuationImpl(this, name, ExistingWorkPolicy.KEEP, requests)
        EnqueueRunnable(continuation, operation).run()
    }
    workTaskExecutor.serialTaskExecutor.execute {
        val workSpecDao = workDatabase.workSpecDao()
        val idAndStates = workSpecDao.getWorkSpecIdAndStatesForName(name)
        if (idAndStates.size > 1) {
            operation.failWorkTypeChanged("Can't apply UPDATE policy to the chains of work.")
            return@execute
        }
        val current = idAndStates.firstOrNull()
        if (current == null) {
            enqueueNew()
            return@execute
        }
        val spec = workSpecDao.getWorkSpec(current.id)
        if (spec == null) {
            operation.markState(
                FAILURE(
                    IllegalStateException("WorkSpec with ${current.id}, that matches a " +
                        "name \"$name\", wasn't found")
                )
            )
            return@execute
        }
        if (!spec.isPeriodic) {
            operation.failWorkTypeChanged("Can't update OneTimeWorker to Periodic Worker. " +
                "Update operation must preserve worker's type.")
            return@execute
        }
        if (current.state == WorkInfo.State.CANCELLED) {
            workSpecDao.delete(current.id)
            enqueueNew()
            return@execute
        }
        val newWorkSpec = workRequest.workSpec.copy(id = current.id)
        try {
            updateWorkImpl(
                processor, workDatabase, configuration, schedulers, newWorkSpec, workRequest.tags
            )
            operation.markState(Operation.SUCCESS)
        } catch (e: Throwable) {
            operation.markState(FAILURE(e))
        }
    }
    return operation
}

private fun OperationImpl.failWorkTypeChanged(message: String) = markState(
    FAILURE(UnsupportedOperationException(message))
)
