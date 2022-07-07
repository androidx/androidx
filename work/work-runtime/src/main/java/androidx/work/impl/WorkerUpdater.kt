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

import androidx.work.Configuration
import androidx.work.WorkManager.UpdateResult
import androidx.work.WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN
import androidx.work.WorkRequest
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture

private fun updateWorkImpl(
    processor: Processor,
    workDatabase: WorkDatabase,
    configuration: Configuration,
    schedulers: List<Scheduler>,
    workRequest: WorkRequest
): UpdateResult {
    val workSpecId = workRequest.id.toString()
    val oldWorkSpec = workDatabase.workSpecDao().getWorkSpec(workSpecId)
        ?: throw IllegalArgumentException("Worker with $workSpecId doesn't exist")
    if (oldWorkSpec.state.isFinished) return UpdateResult.NOT_APPLIED
    val newWorkSpec = workRequest.workSpec
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
        // preserving run attempt count, to calculate back off correctly
        val updatedSpec = newWorkSpec.copy(
            state = oldWorkSpec.state,
            runAttemptCount = oldWorkSpec.runAttemptCount,
            lastEnqueueTime = oldWorkSpec.lastEnqueueTime,
        )
        workSpecDao.updateWorkSpec(updatedSpec)
        workTagDao.deleteByWorkSpecId(workSpecId)
        workTagDao.insertTags(workRequest)
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
            val result = updateWorkImpl(processor, workDatabase,
                configuration, schedulers, workRequest)
            future.set(result)
        } catch (e: Throwable) {
            future.setException(e)
        }
    }
    return future
}
