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
package androidx.datastore

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

@ObsoleteCoroutinesApi
@kotlinx.coroutines.ExperimentalCoroutinesApi
@FlowPreview
/**
 * Single process implementation of DataStore. This is NOT multi-process safe.
 */
class SingleProcessDataStore<T>(
    private val produceFile: () -> File,
    private val serializer: DataStore.Serializer<T>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : DataStore<T> {
    override val dataFlow: Flow<T> = flow {
        val curChannel = downstreamChannel()
        actor.offer(Message.Read(curChannel))
        // TODO(b/151635324): Currently, this will only emit the value read, and the flow will not
        //  be closed unless it encounters an exception. Once updateData() is implemented, this will
        //  emit future updates.
        emitAll(curChannel.asFlow())
    }

    /**
     * The external facing channel. The data flow emits the values from this channel.
     *
     * Once the read has completed successfully, downStreamChannel.get().value is the same as the
     * current on disk data. If the read fails, downStreamChannel will be closed with that cause,
     * and a new instance will be set in its place.
     */
    private val downstreamChannel: AtomicReference<ConflatedBroadcastChannel<T>> =
        AtomicReference(ConflatedBroadcastChannel())

    /** The actions for the actor. */
    private sealed class Message<T> {
        abstract val dataChannel: ConflatedBroadcastChannel<T>

        /**
         * Represents a read operation. If the data is already cached, this is a no-op. If data
         * has not been cached, it triggers a new read to the specified dataChannel.
         */
        class Read<T>(
            override val dataChannel: ConflatedBroadcastChannel<T>
        ) : Message<T>()
    }

    /**
     * Consumes messages. All state changes should happen within actor.
     */
    private val actor: SendChannel<Message<T>> = scope.actor(
        capacity = UNLIMITED
    ) {
        try {
            messageConsumer@ for (msg in channel) {
                if (msg.dataChannel.isClosedForSend) {
                    // The message was sent with an old, now closed, dataChannel. This means that
                    // our read failed.
                    continue@messageConsumer
                }

                try {
                    readOnce(msg.dataChannel)
                } catch (ex: Throwable) {
                    resetDataChannel(ex)
                }
            }
        } finally {
            // The scope has been cancelled. Cancel downstream in case there are any collectors
            // still active.
            downstreamChannel().cancel()
        }
    }

    private fun resetDataChannel(ex: Throwable) {
        val failedDataChannel = downstreamChannel.getAndSet(ConflatedBroadcastChannel())

        failedDataChannel.close(ex)
    }

    private suspend fun readOnce(dataChannel: ConflatedBroadcastChannel<T>) {
        if (dataChannel.valueOrNull != null) {
            // If we already have cached data, we don't try to read it again.
            return
        }

        dataChannel.offer(readData())
    }

    private suspend fun readData(): T {
        // TODO(b/151635324): consider caching produceFile result.
        val file = produceFile()
        try {
            FileInputStream(file).use { stream ->
                return serializer.readFrom(stream)
            }
        } catch (ex: FileNotFoundException) {
            if (file.exists()) {
                throw ex
            }
            return serializer.defaultValue
        }
    }

    // Convenience function:
    @Suppress("NOTHING_TO_INLINE")
    private inline fun downstreamChannel(): ConflatedBroadcastChannel<T> {
        return downstreamChannel.get()
    }
}