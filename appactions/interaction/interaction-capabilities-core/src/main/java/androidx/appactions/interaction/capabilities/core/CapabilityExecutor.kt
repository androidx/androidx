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

import androidx.annotation.RestrictTo
import androidx.concurrent.futures.await

/**
 * An interface of executing the action.
 *
 * Actions are executed asynchronously using Kotlin coroutines.
 * For a Future-based solution, see CapabilityExecutorAsync.
 */
fun interface CapabilityExecutor<ArgumentsT, OutputT> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val uiHandle: Any
        get() = this

    /**
     * Calls to execute the action.
     *
     * @param arguments the argument for this action.
     * @return the ExecutionResult
     */
    suspend fun onExecute(arguments: ArgumentsT): ExecutionResult<OutputT>
}

internal fun <ArgumentsT, OutputT>
    CapabilityExecutorAsync<ArgumentsT, OutputT>.toCapabilityExecutor():
    CapabilityExecutor<ArgumentsT, OutputT> = object : CapabilityExecutor<ArgumentsT, OutputT> {
    override val uiHandle = this@toCapabilityExecutor
    override suspend fun onExecute(arguments: ArgumentsT): ExecutionResult<OutputT> =
        this@toCapabilityExecutor.onExecute(arguments).await()
}