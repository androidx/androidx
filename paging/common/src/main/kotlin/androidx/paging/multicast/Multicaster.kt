/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging.multicast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform

/**
 * Like a publish, shares 1 upstream value with multiple downstream receiver.
 * It has one store specific behavior where upstream flow is suspended until at least 1 downstream
 * flow emits the value to ensure we don't abuse the upstream flow of downstream cannot keep up.
 */
internal class Multicaster<T>(
    /**
     * The [CoroutineScope] to use for upstream subscription
     */
    private val scope: CoroutineScope,
    /**
     * The buffer size that is used only if the upstream has not complete yet.
     * Defaults to 0.
     */
    bufferSize: Int = 0,
    /**
     * Source function to create a new flow when necessary.
     */
    private val source: Flow<T>,

    /**
     * If true, downstream is never closed by the multicaster unless upstream throws an error.
     * Instead, it is kept open and if a new downstream shows up that causes us to restart the flow,
     * it will receive values as well.
     */
    private val piggybackingDownstream: Boolean = false,
    /**
     * Called when upstream dispatches a value.
     */
    private val onEach: suspend (T) -> Unit,

    private val keepUpstreamAlive: Boolean = false
) {

    private val channelManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ChannelManager(
            scope = scope,
            bufferSize = bufferSize,
            upstream = source,
            piggybackingDownstream = piggybackingDownstream,
            onEach = onEach,
            keepUpstreamAlive = keepUpstreamAlive
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow = flow<T> {
        val channel = Channel<ChannelManager.Message.Dispatch.Value<T>>(Channel.UNLIMITED)
        val subFlow = channel.consumeAsFlow()
            .onStart {
                channelManager.addDownstream(channel)
            }
            .transform {
                emit(it.value)
                it.delivered.complete(Unit)
            }.onCompletion {
                channelManager.removeDownstream(channel)
            }
        emitAll(subFlow)
    }

    suspend fun close() {
        channelManager.close()
    }
}
