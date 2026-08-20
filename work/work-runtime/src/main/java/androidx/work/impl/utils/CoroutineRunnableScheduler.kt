/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.impl.utils

import androidx.annotation.RestrictTo
import androidx.work.RunnableScheduler
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A helper utility to schedule delayed runnables using Kotlin Coroutines on the [TaskExecutor]'s
 * task coroutine dispatcher.
 *
 * This utility allows scheduling tasks with a delay without blocking threads, and supports
 * cancelling pending schedules.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CoroutineRunnableScheduler(private val taskExecutor: TaskExecutor) :
    RunnableScheduler {
    private val coroutineScope: CoroutineScope = taskExecutor.coroutineScope
    private val dispatcher: CoroutineDispatcher = taskExecutor.taskCoroutineDispatcher
    private val jobs = mutableMapOf<Runnable, Job>()
    private val lock = Any()

    override fun scheduleWithDelay(delayInMillis: Long, runnable: Runnable) {
        synchronized(lock) {
            // Cancel any existing job for this runnable
            jobs.remove(runnable)?.cancel()

            val job =
                coroutineScope.launch(dispatcher) {
                    val thisJob = coroutineContext[Job]!!
                    try {
                        delay(delayInMillis)
                        runnable.run()
                    } finally {
                        synchronized(lock) {
                            // Only remove from map if it hasn't been overwritten by a new
                            // reschedule
                            if (jobs[runnable] === thisJob) {
                                jobs.remove(runnable)
                            }
                        }
                    }
                }
            jobs[runnable] = job
        }
    }

    override fun cancel(runnable: Runnable) {
        synchronized(lock) { jobs.remove(runnable)?.cancel() }
    }
}
