/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import androidx.appactions.interaction.capabilities.core.impl.concurrent.ListenableFutureHelper
import com.google.common.util.concurrent.ListenableFuture

/**
 * An interface of executing the action.
 */
interface ActionExecutor<ArgumentT, OutputT> {
    /**
     * Calls to execute the action.
     *
     * @param argument the argument for this action.
     * @return the ExecutionResult
     */
    suspend fun execute(argument: ArgumentT): ExecutionResult<OutputT> {
        throw NotImplementedError()
    }

    /**
     * Calls to execute the action.
     *
     * @param argument the argument for this action.
     * @return A ListenableFuture containing the ExecutionResult
     */
    fun executeAsync(argument: ArgumentT): ListenableFuture<ExecutionResult<OutputT>> {
        return ListenableFutureHelper.convertToListenableFuture { execute(argument) }
    }
}
