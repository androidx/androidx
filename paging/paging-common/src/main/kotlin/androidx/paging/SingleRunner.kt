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

package androidx.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Class which guarantees single execution of blocks passed to [runInIsolation] by cancelling the
 * previous call. [runInIsolation] is backed by a [Mutex], which is fair, so concurrent callers of
 * [runInIsolation] will trigger in order, with the last call winning (by cancelling previous calls)
 *
 * When priorities are used, if the currently running block has a higher priority, the new one is
 * cancelled. If the currently running block has lower priority, currently running block is
 * cancelled.
 * If they have equal priority:
 *  * if cancelPreviousInEqualPriority == true, existing block is cancelled
 *  * if cancelPreviousInEqualPriority == false, new block is cancelled
 *
 * Note: When a block is cancelled, the outer scope (which called runInIsolation) is NOT cancelled.
 */
internal class SingleRunner(
    cancelPreviousInEqualPriority: Boolean = true
) {
    private val holder = Holder(this, cancelPreviousInEqualPriority)

    suspend fun runInIsolation(
        priority: Int = DEFAULT_PRIORITY,
        block: suspend () -> Unit
    ) {
        try {
            coroutineScope {
                val myJob = checkNotNull(coroutineContext[Job]) {
                    "Internal error. coroutineScope should've created a job."
                }
                val run = holder.tryEnqueue(
                    priority = priority,
                    job = myJob
                )
                if (run) {
                    try {
                        block()
                    } finally {
                        holder.onFinish(myJob)
                    }
                }
            }
        } catch (cancelIsolatedRunner: CancelIsolatedRunnerException) {
            // if i was cancelled by another caller to this SingleRunner, silently cancel me
            if (cancelIsolatedRunner.runner !== this@SingleRunner) {
                throw cancelIsolatedRunner
            }
        }
    }

    /**
     * Internal exception which is used to cancel previous instance of an isolated runner.
     * We use this special class so that we can still support regular cancelation coming from the
     * `block` but don't cancel its coroutine just to cancel the block.
     */
    private class CancelIsolatedRunnerException(val runner: SingleRunner) : CancellationException()

    private class Holder(
        private val singleRunner: SingleRunner,
        private val cancelPreviousInEqualPriority: Boolean
    ) {
        private val mutex = Mutex()
        private var previous: Job? = null
        private var previousPriority: Int = 0

        suspend fun tryEnqueue(
            priority: Int,
            job: Job
        ): Boolean {
            mutex.withLock {
                val prev = previous
                return if (prev == null ||
                    !prev.isActive ||
                    previousPriority < priority ||
                    (previousPriority == priority && cancelPreviousInEqualPriority)
                ) {
                    prev?.cancel(CancelIsolatedRunnerException(singleRunner))
                    prev?.join()
                    previous = job
                    previousPriority = priority
                    true
                } else {
                    false
                }
            }
        }

        suspend fun onFinish(job: Job) {
            mutex.withLock {
                if (job === previous) {
                    previous = null
                }
            }
        }
    }

    companion object {
        const val DEFAULT_PRIORITY = 0
    }
}
