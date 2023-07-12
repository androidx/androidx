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

package androidx.work.testing

import android.content.Context
import androidx.work.Configuration
import androidx.work.impl.Processor
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkLauncherImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor

internal fun createTestWorkManagerImpl(
    context: Context,
    configuration: Configuration,
    serialExecutor: SerialExecutor,
): WorkManagerImpl {
    val taskExecutor = object : TaskExecutor {
        val synchronousExecutor = SynchronousExecutor()
        override fun getMainThreadExecutor() = synchronousExecutor

        override fun getSerialTaskExecutor() = serialExecutor
    }
    return WorkManagerImpl(
        context = context,
        configuration = configuration,
        workTaskExecutor = taskExecutor,
        workDatabase = WorkDatabase.create(
            context, taskExecutor.serialTaskExecutor, configuration.clock, true),
        schedulersCreator = ::createTestSchedulers
    )
}

internal fun createTestWorkManagerImpl(
    context: Context,
    configuration: Configuration,
): WorkManagerImpl {
    val taskExecutor = WorkManagerTaskExecutor(configuration.taskExecutor)
    return WorkManagerImpl(
        context = context,
        configuration = configuration,
        workTaskExecutor = taskExecutor,
        workDatabase = WorkDatabase.create(
            context, taskExecutor.serialTaskExecutor, configuration.clock, true),
        schedulersCreator = ::createTestSchedulers
    )
}

internal val WorkManagerImpl.testDriver: TestDriver
    get() {
        return schedulers.find { it is TestScheduler } as? TestScheduler
            ?: throw IllegalStateException(
                "WorkManager is incorrectly initialized. " +
                    "Was WorkManagerTestInitHelper.initializeTestWorkManager* method called?"
            )
    }

@Suppress("UNUSED_PARAMETER")
private fun createTestSchedulers(
    context: Context,
    configuration: Configuration,
    workTaskExecutor: TaskExecutor,
    workDatabase: WorkDatabase,
    trackers: Trackers,
    processor: Processor,
): List<Scheduler> {
    val launcher = WorkLauncherImpl(processor, workTaskExecutor)
    return listOf<Scheduler>(TestScheduler(workDatabase, launcher, configuration.clock))
}