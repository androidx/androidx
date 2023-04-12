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

import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.FulfillmentResult
import androidx.appactions.interaction.proto.FulfillmentResponse
import kotlinx.coroutines.CompletableDeferred

/**
 * A fake CallbackInternal instance being used for testing to receive the [FulfillmentResult]
 * containing either [FulfillmentResponse] or [ErrorStatusInternal]
 */
class FakeCallbackInternal constructor(
    private val timeoutMs: Long = TestingUtils.CB_TIMEOUT,
) : CallbackInternal {

    private val completer = CompletableDeferred<FulfillmentResult>()

    override fun onSuccess(fulfillmentResponse: FulfillmentResponse) {
        completer.complete(FulfillmentResult(fulfillmentResponse))
    }

    override fun onError(errorStatus: ErrorStatusInternal) {
        completer.complete(FulfillmentResult(errorStatus))
    }

    fun receiveResponse(): FulfillmentResult = with(TestingUtils) {
        completer.awaitSync(timeoutMs)
    }
}
