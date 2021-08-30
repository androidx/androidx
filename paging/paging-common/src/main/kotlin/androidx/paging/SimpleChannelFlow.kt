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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.internal.FusibleFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * This is a simplified channelFlow implementation as a temporary measure until channel flow
 * leaves experimental state.
 *
 * The exact same implementation is not possible due to [FusibleFlow] being an internal API. To
 * get close to that implementation, internally we use a [Channel.RENDEZVOUS] channel and use a
 * [buffer] ([Channel.BUFFERED]) operator on the resulting Flow. This gives us a close behavior
 * where the default is buffered and any followup buffer operation will result in +1 value being
 * produced.
 */
internal fun <T> simpleChannelFlow(
    block: suspend SimpleProducerScope<T>.() -> Unit
): Flow<T> {
    return flow {
        coroutineScope {
            val channel = Channel<T>(capacity = Channel.RENDEZVOUS)
            val producer = launch {
                try {
                    // run producer in a separate inner scope to ensure we wait for its children
                    // to finish, in case it does more launches inside.
                    coroutineScope {
                        val producerScopeImpl = SimpleProducerScopeImpl(
                            scope = this,
                            channel = channel,
                        )
                        producerScopeImpl.block()
                    }
                    channel.close()
                } catch (t: Throwable) {
                    channel.close(t)
                }
            }
            for (item in channel) {
                emit(item)
            }
            // in case channel closed before producer completes, cancel the producer.
            producer.cancel()
        }
    }.buffer(Channel.BUFFERED)
}

internal interface SimpleProducerScope<T> : CoroutineScope, SendChannel<T> {
    val channel: SendChannel<T>
    suspend fun awaitClose(block: () -> Unit)
}

internal class SimpleProducerScopeImpl<T>(
    scope: CoroutineScope,
    override val channel: SendChannel<T>,
) : SimpleProducerScope<T>, CoroutineScope by scope, SendChannel<T> by channel {
    override suspend fun awaitClose(block: () -> Unit) {
        try {
            val job = checkNotNull(coroutineContext[Job]) {
                "Internal error, context should have a job."
            }
            suspendCancellableCoroutine<Unit> { cont ->
                job.invokeOnCompletion {
                    cont.resume(Unit)
                }
            }
        } finally {
            block()
        }
    }
}