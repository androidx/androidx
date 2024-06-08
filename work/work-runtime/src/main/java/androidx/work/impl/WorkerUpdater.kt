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
import androidx.work.WorkInfo
import androidx.work.WorkManager.UpdateResult
import androidx.work.WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN
import androidx.work.WorkRequest
import androidx.work.executeAsync
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.EnqueueRunnable
import androidx.work.impl.utils.wrapWorkSpecIfNeeded
import androidx.work.launchOperation
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
    val oldWorkSpec =
        workDatabase.workSpecDao().getWorkSpec(workSpecId)
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
        val updatedSpec =
            newWorkSpec
                .copy(
                    state = oldWorkSpec.state,
                    runAttemptCount = oldWorkSpec.runAttemptCount,
                    lastEnqueueTime = oldWorkSpec.lastEnqueueTime,
                    generation = oldWorkSpec.generation + 1,
                    periodCount = oldWorkSpec.periodCount,
                    nextScheduleTimeOverride = oldWorkSpec.nextScheduleTimeOverride,
                    nextScheduleTimeOverrideGeneration =
                        oldWorkSpec.nextScheduleTimeOverrideGeneration
                )
                .apply {
                    if (newWorkSpec.nextScheduleTimeOverrideGeneration == 1) {
                        nextScheduleTimeOverride = newWorkSpec.nextScheduleTimeOverride
                        nextScheduleTimeOverrideGeneration += 1
                        // Other fields are left unchanged, so they can be used after override is
                        // cleared.
                    }
                }

        workSpecDao.updateWorkSpec(wrapWorkSpecIfNeeded(schedulers, updatedSpec))
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
    return workTaskExecutor.serialTaskExecutor.executeAsync("updateWorkImpl") {
        updateWorkImpl(
            processor,
            workDatabase,
            configuration,
            schedulers,
            workRequest.workSpec,
            workRequest.tags
        )
    }
}

/** Enqueue or update the work. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun WorkManagerImpl.enqueueUniquelyNamedPeriodic(
    name: String,
    workRequest: WorkRequest,
): Operation =
    launchOperation(
        configuration.tracer,
        "enqueueUniquePeriodic_$name",
        workTaskExecutor.serialTaskExecutor
    ) {
        val enqueueNew = {
            val requests = listOf(workRequest)
            val continuation = WorkContinuationImpl(this, name, ExistingWorkPolicy.KEEP, requests)
            EnqueueRunnable.enqueue(continuation)
        }

        val workSpecDao = workDatabase.workSpecDao()
        val idAndStates = workSpecDao.getWorkSpecIdAndStatesForName(name)
        if (idAndStates.size > 1)
            throw UnsupportedOperationException("Can't apply UPDATE policy to the chains of work.")

        val current = idAndStates.firstOrNull()
        if (current == null) {
            enqueueNew()
            return@launchOperation
        }
        val spec =
            workSpecDao.getWorkSpec(current.id)
                ?: throw IllegalStateException(
                    "WorkSpec with ${current.id}, that matches a " + "name \"$name\", wasn't found"
                )

        if (!spec.isPeriodic)
            throw UnsupportedOperationException(
                "Can't update OneTimeWorker to Periodic Worker. " +
                    "Update operation must preserve worker's type."
            )

        if (current.state == WorkInfo.State.CANCELLED) {
            workSpecDao.delete(current.id)
            enqueueNew()
            return@launchOperation
        }
        val newWorkSpec = workRequest.workSpec.copy(id = current.id)
        updateWorkImpl(
            processor,
            workDatabase,
            configuration,
            schedulers,
            newWorkSpec,
            workRequest.tags
        )
    }
