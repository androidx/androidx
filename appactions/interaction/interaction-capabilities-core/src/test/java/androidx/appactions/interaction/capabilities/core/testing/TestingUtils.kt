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

package androidx.appactions.interaction.capabilities.core.testing

import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.proto.FulfillmentResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** A wrapper being used for testing [CallbackInternal] */
class CallbackResponse
constructor(val fulfillmentResponse: FulfillmentResponse?, val errorStatus: ErrorStatusInternal?)

/**
 * A fake CallbackInternal instance being used for testing to receive the [CallbackResponse]
 * containing either [FulfillmentResponse] or [ErrorStatusInternal]
 */
class FakeCallbackInternal constructor(private val sendTimeoutMs: Long) : CallbackInternal {

    private val channel = Channel<CallbackResponse>(1)

    override fun onSuccess(fulfillmentResponse: FulfillmentResponse) {
        runBlocking {
            withTimeout(sendTimeoutMs) { channel.send(CallbackResponse(fulfillmentResponse, null)) }
        }
        channel.close()
    }

    override fun onError(errorStatus: ErrorStatusInternal) {
        runBlocking {
            withTimeout(sendTimeoutMs) { channel.send(CallbackResponse(null, errorStatus)) }
        }
        channel.close()
    }

    fun receiveResponse(): CallbackResponse = runBlocking {
        withTimeout(sendTimeoutMs) { channel.receive() }
    }
}
