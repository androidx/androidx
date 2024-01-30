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

package androidx.work.testutils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.impl.Processor
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkLauncherImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.background.greedy.GreedyScheduler
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor

class TestEnv(
    val configuration: Configuration
) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val taskExecutor = WorkManagerTaskExecutor(configuration.taskExecutor)
    val db = WorkDatabase.create(
        context,
        taskExecutor.serialTaskExecutor, configuration.clock, true
    )
    val processor = Processor(context, configuration, taskExecutor, db)
}

fun GreedyScheduler(env: TestEnv, trackers: Trackers): GreedyScheduler {
    val launcher = WorkLauncherImpl(env.processor, env.taskExecutor)
    return GreedyScheduler(
        env.context, env.configuration, trackers,
        env.processor, launcher,
        env.taskExecutor
    )
}

fun WorkManager(env: TestEnv, schedulers: List<Scheduler>, trackers: Trackers) = WorkManagerImpl(
    env.context, env.configuration, env.taskExecutor, env.db, schedulers,
    env.processor, trackers
)
