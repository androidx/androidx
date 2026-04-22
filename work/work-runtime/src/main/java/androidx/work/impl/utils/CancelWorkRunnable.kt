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
@file:JvmName("CancelWorkRunnable")

package androidx.work.impl.utils

import android.app.job.JobParameters
import androidx.work.Operation
import androidx.work.ScheduleEventListener
import androidx.work.impl.Schedulers
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.getAllDependentWork
import androidx.work.impl.model.getWorkInfos
import androidx.work.launchOperation
import java.util.UUID

/** Cancel work and its dependents and dispatch schedule events */
private fun cancel(workManagerImpl: WorkManagerImpl, workSpecId: String) {
    val toCancel = workManagerImpl.workDatabase.getWorkToCancel(workSpecId)
    val workSpecDao = workManagerImpl.workDatabase.workSpecDao()
    for (id in toCancel) {
        workSpecDao.setCancelledState(id)
    }
    val processor = workManagerImpl.processor
    processor.stopAndCancelWork(workSpecId, JobParameters.STOP_REASON_CANCELLED_BY_APP)
    for (scheduler in workManagerImpl.schedulers) {
        scheduler.cancel(workSpecId)
    }
    val scheduleEventListener = workManagerImpl.configuration.getScheduleEventListener()
    scheduleEventListener?.dispatchEvents(
        workManagerImpl.workTaskExecutor,
        workSpecDao.getWorkInfos(toCancel),
        ScheduleEventListener::onCancelled,
    )
}

private fun WorkDatabase.getWorkToCancel(workSpecId: String): List<String> {
    val workSpecDao = workSpecDao()
    val dependencyDao = dependencyDao()
    val toCancel = mutableListOf(workSpecId)
    toCancel.addAll(dependencyDao.getAllDependentWork(workSpecId))
    return toCancel.filter { id -> workSpecDao.getState(id)?.let { !it.isFinished } ?: false }
}

private fun reschedulePendingWorkers(workManagerImpl: WorkManagerImpl) {
    Schedulers.schedule(
        workManagerImpl.configuration,
        workManagerImpl.workDatabase,
        workManagerImpl.schedulers,
    )
}

/**
 * Cancels work for a specific id.
 *
 * @param id The id to cancel
 * @param workManagerImpl The [WorkManagerImpl] to use
 * @return A [Operation]
 */
public fun forId(id: UUID, workManagerImpl: WorkManagerImpl): Operation =
    launchOperation(
        tracer = workManagerImpl.configuration.tracer,
        label = "CancelWorkById",
        workManagerImpl.workTaskExecutor.serialTaskExecutor,
    ) {
        val workDatabase = workManagerImpl.workDatabase
        workDatabase.runInTransaction { cancel(workManagerImpl, id.toString()) }
        reschedulePendingWorkers(workManagerImpl)
    }

/**
 * Cancels work for a specific tag.
 *
 * @param tag The tag to cancel
 * @param workManagerImpl The [WorkManagerImpl] to use
 * @return A [Operation]
 */
public fun forTag(tag: String, workManagerImpl: WorkManagerImpl): Operation =
    launchOperation(
        tracer = workManagerImpl.configuration.tracer,
        label = "CancelWorkByTag_$tag",
        executor = workManagerImpl.workTaskExecutor.serialTaskExecutor,
    ) {
        val workDatabase = workManagerImpl.workDatabase
        workDatabase.runInTransaction {
            val workSpecDao = workDatabase.workSpecDao()
            val workSpecIds = workSpecDao.getUnfinishedWorkWithTag(tag)
            for (workSpecId in workSpecIds) {
                cancel(workManagerImpl, workSpecId)
            }
        }
        reschedulePendingWorkers(workManagerImpl)
    }

/**
 * Cancels work labelled with a specific name.
 *
 * @param name The name to cancel
 * @param workManagerImpl The [WorkManagerImpl] to use
 * @return A [Operation]
 */
public fun forName(name: String, workManagerImpl: WorkManagerImpl): Operation =
    launchOperation(
        tracer = workManagerImpl.configuration.tracer,
        label = "CancelWorkByName_$name",
        workManagerImpl.workTaskExecutor.serialTaskExecutor,
    ) {
        forNameInline(name, workManagerImpl)
        reschedulePendingWorkers(workManagerImpl)
    }

public fun forNameInline(name: String, workManagerImpl: WorkManagerImpl) {
    val workDatabase = workManagerImpl.workDatabase
    workDatabase.runInTransaction {
        val workSpecDao = workDatabase.workSpecDao()
        val workSpecIds = workSpecDao.getUnfinishedWorkWithName(name)
        for (workSpecId in workSpecIds) {
            cancel(workManagerImpl, workSpecId)
        }
    }
}

/**
 * Cancels all work.
 *
 * @param workManagerImpl The [WorkManagerImpl] to use
 * @return A [Operation] that cancels all work
 */
public fun forAll(workManagerImpl: WorkManagerImpl): Operation =
    launchOperation(
        tracer = workManagerImpl.configuration.tracer,
        label = "CancelAllWork",
        workManagerImpl.workTaskExecutor.serialTaskExecutor,
    ) {
        val workDatabase = workManagerImpl.workDatabase
        workDatabase.runInTransaction {
            val workSpecDao = workDatabase.workSpecDao()
            val workSpecIds = workSpecDao.getAllUnfinishedWork()
            for (workSpecId in workSpecIds) {
                cancel(workManagerImpl, workSpecId)
            }
            // Update the last cancelled time in Preference.
            PreferenceUtils(workDatabase)
                .setLastCancelAllTimeMillis(workManagerImpl.configuration.clock.currentTimeMillis())
        }
        // No need to call reschedule pending workers here as we just cancelled everything.
    }
