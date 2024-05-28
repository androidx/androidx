/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.synchronized
import androidx.compose.ui.createSynchronizedObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * Dispatcher with the ability to immediately perform (flush) all pending tasks.
 * Without a flush all tasks are dispatched in the dispatcher provided by [scope]
 */
@OptIn(InternalCoroutinesApi::class)
internal class FlushCoroutineDispatcher(
    scope: CoroutineScope
) : CoroutineDispatcher(), Delay {
    // Dispatcher should always be alive, even if Job is cancelled. Otherwise coroutines which
    // use this dispatcher won't be properly cancelled.
    // TODO replace it by scope.coroutineContext[CoroutineDispatcher] when it will be no longer experimental
    private val scope = CoroutineScope(scope.coroutineContext.minusKey(Job))
    private var immediateTasks = ArrayDeque<Runnable>()
    private val delayedTasks = ArrayDeque<Runnable>()
    private val tasksLock = createSynchronizedObject()
    private var immediateTasksSwap = ArrayDeque<Runnable>()
    @Volatile
    private var isPerformingRun = false
    private val runLock = createSynchronizedObject()
    
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasksLock) {
            immediateTasks.add(block)
        }
        scope.launch {
            performRun {
                val isTaskAlive = synchronized(tasksLock) {
                    immediateTasks.remove(block)
                }
                if (isTaskAlive) {
                    block.run()
                }
            }
        }
    }

    /**
     * Whether the dispatcher has any tasks scheduled or currently running.
     */
    fun hasTasks() = synchronized(tasksLock) {
        immediateTasks.isNotEmpty() || delayedTasks.isNotEmpty()
    } || isPerformingRun

    /**
     * Perform all scheduled tasks and wait for the tasks which are already
     * performing in the [scope]
     */
    fun flush() = performRun {
        // Run tasks until they're empty in order to executed even ones that are added by the tasks
        // pending at the start
        while (true) {
            synchronized(tasksLock) {
                if (immediateTasks.isEmpty())
                    return@performRun

                val tmp = immediateTasksSwap
                immediateTasksSwap = immediateTasks
                immediateTasks = tmp
            }

            immediateTasksSwap.forEach(Runnable::run)
            immediateTasksSwap.clear()
        }
    }

    // the lock is needed to be certain that all tasks will be completed after `flush` method
    private fun performRun(body: () -> Unit) = synchronized(runLock) {
        try {
            isPerformingRun = true
            body()
        } finally {
            isPerformingRun = false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val block = Runnable { continuation.resume(Unit, null) }
        synchronized(tasksLock) {
            delayedTasks.add(block)
        }
        val job = scope.launch {
            kotlinx.coroutines.delay(timeMillis)
            performRun {
                val isTaskAlive = synchronized(tasksLock) {
                    delayedTasks.remove(block)
                }
                if (isTaskAlive) {
                    block.run()
                }
            }
        }
        continuation.invokeOnCancellation {
            job.cancel()
            synchronized(tasksLock) {
                delayedTasks.remove(block)
            }
        }
    }
}
