/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.Context
import androidx.work.Configuration
import androidx.work.R
import androidx.work.impl.background.greedy.GreedyScheduler
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor

@JvmName("createWorkManager")
@JvmOverloads
fun WorkManagerImpl(
    context: Context,
    configuration: Configuration,
    workTaskExecutor: TaskExecutor = WorkManagerTaskExecutor(configuration.taskExecutor),
    workDatabase: WorkDatabase =
        WorkDatabase.create(
            context.applicationContext, workTaskExecutor.serialTaskExecutor,
            configuration.clock,
            context.resources.getBoolean(R.bool.workmanager_test_configuration)
        ),
    trackers: Trackers = Trackers(context.applicationContext, workTaskExecutor),
    processor: Processor = Processor(
        context.applicationContext, configuration, workTaskExecutor, workDatabase
    ),
    schedulersCreator: SchedulersCreator = ::createSchedulers
): WorkManagerImpl {
    val schedulers = schedulersCreator(
        context, configuration,
        workTaskExecutor, workDatabase, trackers, processor
    )
    return WorkManagerImpl(
        context.applicationContext, configuration, workTaskExecutor, workDatabase,
        schedulers, processor, trackers
    )
}

@JvmName("createTestWorkManager")
fun TestWorkManagerImpl(
    context: Context,
    configuration: Configuration,
    workTaskExecutor: TaskExecutor,
) = WorkManagerImpl(
    context, configuration, workTaskExecutor,
    WorkDatabase.create(context, workTaskExecutor.serialTaskExecutor, configuration.clock, true)
)

typealias SchedulersCreator = (
    context: Context,
    configuration: Configuration,
    workTaskExecutor: TaskExecutor,
    workDatabase: WorkDatabase,
    trackers: Trackers,
    processor: Processor
) -> List<Scheduler>

fun schedulers(vararg schedulers: Scheduler): SchedulersCreator =
    { _, _, _, _, _, _ -> schedulers.toList() }

private fun createSchedulers(
    context: Context,
    configuration: Configuration,
    workTaskExecutor: TaskExecutor,
    workDatabase: WorkDatabase,
    trackers: Trackers,
    processor: Processor,
): List<Scheduler> =
    listOf(
        Schedulers.createBestAvailableBackgroundScheduler(context, workDatabase, configuration),
        GreedyScheduler(
            context, configuration, trackers, processor,
            WorkLauncherImpl(processor, workTaskExecutor),
            workTaskExecutor
        ),
    )
