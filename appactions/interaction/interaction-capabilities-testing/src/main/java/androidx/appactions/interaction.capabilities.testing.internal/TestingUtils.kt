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

import androidx.appactions.interaction.capabilities.core.ActionExecutorAsync
import androidx.appactions.interaction.capabilities.core.ExecutionResult.Companion.getDefaultInstance
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.proto.FulfillmentResponse

object TestingUtils {
    const val CB_TIMEOUT = 1000L

    fun buildActionCallback(future: SettableFutureWrapper<Boolean>): CallbackInternal {
        return object : CallbackInternal {
            override fun onSuccess(response: FulfillmentResponse) {
                future.set(true)
            }

            override fun onError(error: ErrorStatusInternal) {
                future.set(false)
            }
        }
    }

    fun buildActionCallbackWithFulfillmentResponse(
        future: SettableFutureWrapper<FulfillmentResponse>,
    ): CallbackInternal {
        return object : CallbackInternal {
            override fun onSuccess(response: FulfillmentResponse) {
                future.set(response)
            }

            override fun onError(error: ErrorStatusInternal) {
                future.setException(
                    IllegalStateException(
                        String.format(
                            "expected FulfillmentResponse, but got ErrorStatus=%s instead",
                            error
                        )
                    )
                )
            }
        }
    }

    fun buildErrorActionCallback(
        future: SettableFutureWrapper<ErrorStatusInternal>,
    ): CallbackInternal {
        return object : CallbackInternal {
            override fun onSuccess(response: FulfillmentResponse) {
                future.setException(
                    java.lang.IllegalStateException(
                        "expected ErrorStatus, but got FulfillmentResponse instead"
                    )
                )
            }

            override fun onError(error: ErrorStatusInternal) {
                future.set(error)
            }
        }
    }

    fun <ArgumentT, OutputT> createFakeActionExecutor(): ActionExecutorAsync<ArgumentT, OutputT> {
        return ActionExecutorAsync { _: ArgumentT ->
            Futures.immediateFuture(
                getDefaultInstance()
            )
        }
    }
}
