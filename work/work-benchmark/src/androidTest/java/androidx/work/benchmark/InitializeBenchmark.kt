/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.benchmark

import android.content.Context
import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.impl.WorkDatabasePathHelper
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@LargeTest
class InitializeBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()
    private lateinit var databasePath: String
    private lateinit var context: Context
    private lateinit var executor: DispatchingExecutor
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var configuration: Configuration

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databasePath = WorkDatabasePathHelper.getDatabasePath(context).path

        // Use a DispatchingExecutor to avoid having to wait for all the tasks to be done
        // in the actual benchmark.
        executor = DispatchingExecutor()
        val serialExecutor = SerialExecutor(executor)

        taskExecutor = object : TaskExecutor {
            override fun postToMainThread(runnable: Runnable) {
                serialExecutor.execute(runnable)
            }

            override fun getMainThreadExecutor(): Executor {
                return serialExecutor
            }

            override fun executeOnBackgroundThread(runnable: Runnable) {
                serialExecutor.execute(runnable)
            }

            override fun getBackgroundExecutor(): SerialExecutor {
                return serialExecutor
            }
        }

        configuration = Configuration.Builder()
            .setTaskExecutor(executor)
            .setExecutor(executor)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }

    @Test
    fun initializeSimple() {
        benchmarkRule.measureRepeated {
            // Runs ForceStopRunnable
            val database = WorkDatabase.create(context, configuration.taskExecutor, false)
            WorkManagerImpl(context, configuration, taskExecutor, database)
            runWithTimingDisabled {
                executor.runAllCommands()
                database.close()
                context.deleteDatabase(databasePath)
            }
        }
    }

    @Test
    fun initializeWithWorkLeft() {
        val count = 20
        benchmarkRule.measureRepeated {
            val database = WorkDatabase.create(context, configuration.taskExecutor, false)
            runWithTimingDisabled {
                for (i in 0 until count) {
                    val request = OneTimeWorkRequestBuilder<NoOpWorker>()
                    val state =
                        if (i <= count - 2) WorkInfo.State.SUCCEEDED else WorkInfo.State.RUNNING
                    request.setInitialState(state)
                    database.workSpecDao().insertWorkSpec(request.build().workSpec)
                }
            }
            // Runs ForceStopRunnable
            WorkManagerImpl(context, configuration, taskExecutor, database)
            // Prune records for the next run.
            runWithTimingDisabled {
                executor.runAllCommands()
                with(database) {
                    workSpecDao().pruneFinishedWorkWithZeroDependentsIgnoringKeepForAtLeast()
                    close()
                }
                context.deleteDatabase(databasePath)
            }
        }
    }
}
