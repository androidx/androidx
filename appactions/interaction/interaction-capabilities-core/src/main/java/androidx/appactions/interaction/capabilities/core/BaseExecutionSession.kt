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

package androidx.appactions.interaction.capabilities.core

import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture

/** Base interface for ExecutionSession of all verticals. */
interface BaseExecutionSession<ArgumentsT, OutputT> {
    /**
     * Implement any initialization logic.
     *
     * This method is called once, before any other listeners are invoked.
     */
    fun onCreate(sessionConfig: SessionConfig) {}

    /**
     * Called when all arguments are finalized.
     *
     * @param arguments the [ArgumentsT] instance containing data for fulfillment.
     * @return an [ExecutionResult] instance.
     */
    suspend fun onExecute(arguments: ArgumentsT): ExecutionResult<OutputT> {
        return onExecuteAsync(arguments).await()
    }

    /**
     * Called when all arguments are finalized.
     *
     * @param arguments the Argument instance containing data for fulfillment.
     * @return a [ListenableFuture] containing an [ExecutionResult] instance.
     */
    fun onExecuteAsync(arguments: ArgumentsT): ListenableFuture<ExecutionResult<OutputT>> {
        return Futures.immediateFuture(ExecutionResult.Builder<OutputT>().build())
    }

    /** Implement any cleanup logic. This method is called some time after the session finishes. */
    fun onDestroy() {}
}
