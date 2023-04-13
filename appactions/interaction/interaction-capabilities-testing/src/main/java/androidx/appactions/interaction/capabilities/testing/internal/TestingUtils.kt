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

package androidx.appactions.interaction.capabilities.testing.internal

import androidx.appactions.interaction.capabilities.core.CapabilityExecutorAsync
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object TestingUtils {
    // use this timeout for things that should take negligible time.
    const val CB_TIMEOUT = 1000L

    // use this timeout for waiting an arbitrary period of time.
    const val BLOCKING_TIMEOUT = 300L

    fun <ArgumentsT, OutputT> createFakeCapabilityExecutor():
        CapabilityExecutorAsync<ArgumentsT, OutputT> {
        return CapabilityExecutorAsync { _: ArgumentsT ->
            Futures.immediateFuture(
                ExecutionResult.Builder<OutputT>().build()
            )
        }
    }

    /** Blocks the current thread until the Deferred is completed, or times out. */
    fun <T> Deferred<T>.awaitSync(timeoutMs: Long = CB_TIMEOUT): T =
        runBlocking {
            withTimeout(timeoutMs) {
                this@awaitSync.await()
            }
        }
}
