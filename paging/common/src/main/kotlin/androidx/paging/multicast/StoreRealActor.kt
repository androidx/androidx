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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple actor implementation abstracting away Coroutine.actor since it is deprecated.
 * It also enforces a 0 capacity buffer.
 */
internal abstract class StoreRealActor<T>(
    scope: CoroutineScope
) {
    private val inboundChannel = Channel<Any?>(capacity = Channel.RENDEZVOUS)
    private val closeCompleted = CompletableDeferred<Unit>()
    private val didClose = AtomicBoolean(false)

    init {
        inboundChannel.consumeAsFlow()
            .onEach { msg ->
                if (msg === CLOSE_TOKEN) {
                    doClose()
                } else {
                    @Suppress("UNCHECKED_CAST")
                    handle(msg as T)
                }
            }.onCompletion {
                doClose()
            }
            .launchIn(scope)
    }

    private fun doClose() {
        if (didClose.compareAndSet(false, true)) {
            try {
                onClosed()
            } finally {
                inboundChannel.close()
                closeCompleted.complete(Unit)
            }
        }
    }

    open fun onClosed() = Unit

    abstract suspend fun handle(msg: T)

    suspend fun send(msg: T) {
        inboundChannel.send(msg)
    }

    suspend fun close() {
        // using a custom token to close so that we can gracefully close the downstream
        inboundChannel.send(CLOSE_TOKEN)
        // wait until close is done done
        closeCompleted.await()
    }

    companion object {
        val CLOSE_TOKEN = Any()
    }
}
