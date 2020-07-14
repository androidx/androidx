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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Class which guarantees single execution of blocks passed to [runInIsolation] by cancelling the
 * previous call. [runInIsolation] is backed by a [Mutex], which is fair, so concurrent callers of
 * [runInIsolation] will trigger in order, with the last call winning (by cancelling previous calls)
 *
 * Note: When the block is cancelled, the other scope (which called runInIsolation) is NOT cancelled
 */
internal class SingleRunner {
    private val mutex = Mutex()
    private var previous: CoroutineScope? = null

    suspend fun runInIsolation(block: suspend () -> Unit) {
        try {
            coroutineScope {
                mutex.withLock {
                    previous?.cancel(CancelIsolatedRunnerException(this@SingleRunner))
                    previous = this
                }
                try {
                    block()
                } finally {
                    mutex.withLock {
                        if (previous == this) {
                            previous = null
                        }
                    }
                }
            }
        } catch (cancelIsolatedRunner: CancelIsolatedRunnerException) {
            // gracefully cancel the scope w/o canceling the outer scope.
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
}
