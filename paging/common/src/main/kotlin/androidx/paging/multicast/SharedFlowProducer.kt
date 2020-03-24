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

import androidx.paging.multicast.ChannelManager.Message.Dispatch.UpstreamFinished
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A flow collector that works with a [ChannelManager] to collect values from an upstream flow
 * and dispatch to the [sendUpsteamMessage] which then dispatches to downstream collectors.
 *
 * They work in sync such that this producer always expects an ack from the [ChannelManager] after
 * sending an event.
 *
 * Cancellation of the collection might be triggered by both this producer (e.g. upstream completes)
 * or the [ChannelManager] (e.g. all active collectors complete).
 */
internal class SharedFlowProducer<T>(
    private val scope: CoroutineScope,
    private val src: Flow<T>,
    private val sendUpsteamMessage: suspend (ChannelManager.Message.Dispatch<T>) -> Unit
) {
    private lateinit var collectionJob: Job

    /**
     * Starts the collection of the upstream flow.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        scope.launch {
            try {
                // launch again to track the collection job
                collectionJob = scope.launch {
                    try {
                        src.catch {
                            sendUpsteamMessage(ChannelManager.Message.Dispatch.Error(it))
                        }.collect {
                            val ack = CompletableDeferred<Unit>()
                            sendUpsteamMessage(
                                ChannelManager.Message.Dispatch.Value(
                                    it,
                                    ack
                                )
                            )
                            // suspend until at least 1 receives the new value
                            ack.await()
                        }
                    } catch (closed: ClosedSendChannelException) {
                        // ignore. if consumers are gone, it might close itself.
                    }
                }
                // wait until collection ends, either due to an error or ordered by the channel
                // manager
                collectionJob.join()
            } finally {
                // cleanup the channel manager so that downstreams can be closed if they are not
                // closed already and leftovers can be moved to a new producer if necessary.
                try {
                    sendUpsteamMessage(UpstreamFinished(this@SharedFlowProducer))
                } catch (closed: ClosedSendChannelException) {
                    // it might close before us, its fine.
                }
            }
        }
    }

    suspend fun cancelAndJoin() {
        collectionJob.cancelAndJoin()
    }

    fun cancel() {
        collectionJob.cancel()
    }
}
