/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.work.impl.background.systemjob

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Configuration
import androidx.work.Logger
import androidx.work.impl.WorkDatabase
import androidx.work.impl.background.systemjob.SystemJobScheduler.getPendingJobs
import androidx.work.loge

internal const val WORKMANAGER_NAMESPACE = "androidx.work.systemjobscheduler"

// using SystemJobScheduler as tag for simplicity, because everything here is about
// SystemJobScheduler
private val TAG = Logger.tagWithPrefix("SystemJobScheduler")

internal val Context.wmJobScheduler: JobScheduler
    get() {
        val defaultJobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        return if (Build.VERSION.SDK_INT >= 34) {
            JobScheduler34.forNamespace(defaultJobScheduler)
        } else defaultJobScheduler
    }

@RequiresApi(34)
private object JobScheduler34 {
    fun forNamespace(jobScheduler: JobScheduler): JobScheduler {
        return jobScheduler.forNamespace(WORKMANAGER_NAMESPACE)
    }
}

private object JobScheduler21 {
    fun getAllPendingJobs(jobScheduler: JobScheduler): List<JobInfo> {
        return jobScheduler.allPendingJobs
    }
}

public val JobScheduler.safePendingJobs: List<JobInfo>?
    get() {
        return try {
            // Note: despite what the word "pending" and the associated Javadoc might imply, this is
            // actually a list of all unfinished jobs that JobScheduler knows about for the current
            // process.
            JobScheduler21.getAllPendingJobs(this)
        } catch (exception: Throwable) {
            // OEM implementation bugs in JobScheduler cause the app to crash. Avoid crashing.
            // see b/134028937
            loge(TAG, exception) { "getAllPendingJobs() is not reliable on this device." }
            null
        }
    }

internal fun createErrorMessage(
    context: Context,
    workDatabase: WorkDatabase,
    configuration: Configuration,
): String {
    val totalLimit = if (Build.VERSION.SDK_INT >= 31) 150 else 100
    val dbScheduledCount = workDatabase.workSpecDao().getScheduledWork().size
    val jobSchedulerStats =
        if (Build.VERSION.SDK_INT >= 34) {
            val namespacedScheduler = context.wmJobScheduler
            val allJobsInNamespace = namespacedScheduler.safePendingJobs
            if (allJobsInNamespace != null) {
                val pendingJobs = getPendingJobs(context, namespacedScheduler)
                val diff =
                    if (pendingJobs != null) allJobsInNamespace.size - pendingJobs.size else 0

                val nonWmJobsMessage =
                    when (diff) {
                        0 -> null
                        else -> "$diff of which are not owned by WorkManager"
                    }

                val defaultJobScheduler =
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                val wmJobsInDefault = getPendingJobs(context, defaultJobScheduler)?.size ?: 0

                val defaultNamespaceMessage =
                    when (wmJobsInDefault) {
                        0 -> null
                        else -> "$wmJobsInDefault from WorkManager in the default namespace"
                    }

                listOfNotNull(
                        "${allJobsInNamespace.size} jobs in \"$WORKMANAGER_NAMESPACE\" namespace",
                        nonWmJobsMessage,
                        defaultNamespaceMessage,
                    )
                    .joinToString(",\n")
            } else "<faulty JobScheduler failed to getPendingJobs>"
        } else {
            when (val pendingJobs = getPendingJobs(context, context.wmJobScheduler)) {
                null -> "<faulty JobScheduler failed to getPendingJobs>"
                else -> "${pendingJobs.size} jobs from WorkManager"
            }
        }

    return "JobScheduler $totalLimit job limit exceeded.\n" +
        "In JobScheduler there are $jobSchedulerStats.\n" +
        "There are $dbScheduledCount jobs tracked by WorkManager's database;\n" +
        "the Configuration limit is ${configuration.maxSchedulerLimit}."
}
