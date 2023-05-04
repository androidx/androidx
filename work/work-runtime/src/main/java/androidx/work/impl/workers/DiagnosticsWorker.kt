/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.work.Logger
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.generationalId
import androidx.work.impl.model.SystemIdInfoDao
import androidx.work.impl.model.WorkNameDao
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkTagDao
import java.util.concurrent.TimeUnit

internal class DiagnosticsWorker(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {
    override fun doWork(): Result {
        val workManager = WorkManagerImpl.getInstance(applicationContext)
        val database = workManager.workDatabase
        val workSpecDao = database.workSpecDao()
        val workNameDao = database.workNameDao()
        val workTagDao = database.workTagDao()
        val systemIdInfoDao = database.systemIdInfoDao()
        val startAt =
            workManager.configuration.clock.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val completed = workSpecDao.getRecentlyCompletedWork(startAt)
        val running = workSpecDao.getRunningWork()
        val enqueued = workSpecDao.getAllEligibleWorkSpecsForScheduling(
            Scheduler.MAX_GREEDY_SCHEDULER_LIMIT
        )
        if (completed.isNotEmpty()) {
            Logger.get().info(TAG, "Recently completed work:\n\n")
            Logger.get().info(
                TAG, workSpecRows(workNameDao, workTagDao, systemIdInfoDao, completed)
            )
        }
        if (running.isNotEmpty()) {
            Logger.get().info(TAG, "Running work:\n\n")
            Logger.get().info(TAG, workSpecRows(workNameDao, workTagDao, systemIdInfoDao, running))
        }
        if (enqueued.isNotEmpty()) {
            Logger.get().info(TAG, "Enqueued work:\n\n")
            Logger.get().info(TAG, workSpecRows(workNameDao, workTagDao, systemIdInfoDao, enqueued))
        }
        return Result.success()
    }
}

private val TAG = Logger.tagWithPrefix("DiagnosticsWrkr")

private fun workSpecRows(
    workNameDao: WorkNameDao,
    workTagDao: WorkTagDao,
    systemIdInfoDao: SystemIdInfoDao,
    workSpecs: List<WorkSpec>
) = buildString {
    val systemIdHeader = if (Build.VERSION.SDK_INT >= 23) "Job Id" else "Alarm Id"
    val header = "\n Id \t Class Name\t ${systemIdHeader}\t State\t Unique Name\t Tags\t"
    append(header)
    workSpecs.forEach { workSpec ->
        val systemId = systemIdInfoDao.getSystemIdInfo(workSpec.generationalId())?.systemId
        val names = workNameDao.getNamesForWorkSpecId(workSpec.id).joinToString(",")
        val tags = workTagDao.getTagsForWorkSpecId(workSpec.id).joinToString(",")
        append(workSpecRow(workSpec, names, systemId, tags))
    }
}

private fun workSpecRow(workSpec: WorkSpec, name: String, systemId: Int?, tags: String) =
    "\n${workSpec.id}\t ${workSpec.workerClassName}\t $systemId\t " +
        "${workSpec.state.name}\t $name\t $tags\t"
