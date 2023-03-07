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
import androidx.appactions.interaction.capabilities.core.impl.concurrent.ListenableFutureHelper
import com.google.common.util.concurrent.ListenableFuture

/**
 * An ListenableFuture-based interface of executing an action.
 */
fun interface ActionExecutorAsync<ArgumentT, OutputT> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val uiHandle: Any
        get() = this

    /**
     * Calls to execute the action.
     *
     * @param argument the argument for this action.
     * @return A ListenableFuture containing the ExecutionResult
     */
    fun execute(argument: ArgumentT): ListenableFuture<ExecutionResult<OutputT>>

    companion object {
        fun <ArgumentT, OutputT> ActionExecutor<ArgumentT, OutputT>
            .toActionExecutorAsync(): ActionExecutorAsync<ArgumentT, OutputT> =
            object : ActionExecutorAsync<ArgumentT, OutputT> {
                override val uiHandle = this@toActionExecutorAsync
                override fun execute(
                    argument: ArgumentT,
                ): ListenableFuture<ExecutionResult<OutputT>> =
                    ListenableFutureHelper.convertToListenableFuture("ActionExecutor#execute") {
                        this@toActionExecutorAsync.execute(argument)
                    }
            }
    }
}
