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

import androidx.annotation.RestrictTo
import androidx.datastore.handlers.NoOpCorruptionHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference

/**
 * Single process implementation of DataStore. This is NOT multi-process safe.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class, FlowPreview::class)
class SingleProcessDataStore<T>
/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    private val produceFile: () -> File,
    private val serializer: Serializer<T>,
    /**
     * The list of initialization tasks to perform. These tasks will be completed before any data
     * is published to the data and before any read-modify-writes execute in updateData.  If
     * any of the tasks fail, the tasks will be run again the next time data is collected or
     * updateData is called. Init tasks should not wait on results from data - this will
     * result in deadlock.
     */
    initTasksList: List<suspend (api: InitializerApi<T>) -> Unit> = emptyList(),
    private val corruptionHandler: CorruptionHandler<T> = NoOpCorruptionHandler<T>(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : DataStore<T> {

    override val data: Flow<T> = flow {
        val curChannel = downstreamChannel()
        actor.offer(Message.Read(curChannel))
        emitAll(curChannel.asFlow())
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        val ack = CompletableDeferred<T>()
        val dataChannel = downstreamChannel()
        val updateMsg = Message.Update<T>(transform, ack, dataChannel)

        actor.send(updateMsg)

        // If no read has succeeded yet, we need to wait on the result of the next read so we can
        // bubble exceptions up to the caller. Read exceptions are not bubbled up through ack.
        if (dataChannel.valueOrNull == null) {
            dataChannel.asFlow().first()
        }

        // Wait with same scope as the actor, so we're not waiting on a cancelled actor.
        return withContext(scope.coroutineContext) { ack.await() }
    }

    private val SCRATCH_SUFFIX = ".tmp"

    private val file: File by lazy {
        val file = produceFile()
        check(file.extension == serializer.fileExtension) {
            "File extension for file: $file does not match required extension for" +
                    " serializer: ${serializer.fileExtension}"
        }
        file
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

    private var initTasks: List<suspend (api: InitializerApi<T>) -> Unit>? =
        initTasksList.toList()

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

        /** Represents an update operation. */
        class Update<T>(
            val transform: suspend (t: T) -> T,
            /**
             * Used to signal (un)successful completion of the update to the caller.
             */
            val ack: CompletableDeferred<T>,
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
                    readAndInitOnce(msg.dataChannel)
                } catch (ex: Throwable) {
                    resetDataChannel(ex)
                    continue@messageConsumer
                }

                // We have successfully read data and sent it to downstreamChannel.

                if (msg is Message.Update) {
                    msg.ack.completeWith(
                        runCatching {
                            transformAndWrite(msg.transform, downstreamChannel())
                        }
                    )
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

    private suspend fun readAndInitOnce(dataChannel: ConflatedBroadcastChannel<T>) {
        if (dataChannel.valueOrNull != null) {
            // If we already have cached data, we don't try to read it again.
            return
        }

        val updateLock = Mutex()
        var initData = readDataOrHandleCorruption()

        var initializationComplete: Boolean = false

        // TODO(b/151635324): Consider using Context Element to throw an error on re-entrance.
        val api = object : InitializerApi<T> {
            override suspend fun updateData(transform: suspend (t: T) -> T): T {
                return updateLock.withLock() {
                    if (initializationComplete) {
                        throw IllegalStateException(
                            "InitializerApi.updateData should not be " +
                                    "called after initialization is complete."
                        )
                    }

                    val newData = transform(initData)
                    if (newData != initData) {
                        writeData(newData)
                        initData = newData
                    }

                    initData
                }
            }
        }

        initTasks?.forEach { it(api) }
        initTasks = null // Init tasks have run successfully, we don't need them anymore.
        updateLock.withLock {
            initializationComplete = true
        }

        dataChannel.offer(initData)
    }

    private suspend fun readDataOrHandleCorruption(): T {
        try {
            return readData()
        } catch (ex: CorruptionException) {

            val newData: T = corruptionHandler.handleCorruption(ex)

            try {
                writeData(newData)
            } catch (writeEx: IOException) {
                // If we fail to write the handled data, add the new exception as a suppressed
                // exception.
                ex.addSuppressed(writeEx)
                throw ex
            }

            // If we reach this point, we've successfully replaced the data on disk with newData.
            return newData
        }
    }

    private suspend fun readData(): T {
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

    private suspend fun transformAndWrite(
        transform: suspend (t: T) -> T,
        /**
         * This is the channel that contains the data that will be used for the transformation.
         * It *must* already have a value -- otherwise this will throw IllegalStateException.
         * Once the transformation is completed and data is durably persisted to disk, and the new
         * value will be offered to this channel.
         */
        updateDataChannel: ConflatedBroadcastChannel<T>
    ): T {
        val curData = updateDataChannel.value
        val newData = transform(curData)
        return if (curData == newData) {
            curData
        } else {
            writeData(newData)
            updateDataChannel.offer(newData)
            newData
        }
    }

    private fun writeData(newData: T) {
        file.createParentDirectories()

        val scratchFile = File(file.absolutePath + SCRATCH_SUFFIX)
        try {
            FileOutputStream(scratchFile).use { stream ->
                serializer.writeTo(newData, stream)
                stream.fd.sync()
                // TODO(b/151635324): fsync the directory, otherwise a badly timed crash could
                //  result in reverting to a previous state.
            }
            if (!scratchFile.renameTo(file)) {
                throw IOException("$scratchFile could not be renamed to $file")
            }
        } catch (ex: IOException) {
            if (scratchFile.exists()) {
                scratchFile.delete()
            }
            throw ex
        }
    }

    private fun File.createParentDirectories() {
        val parent: File? = canonicalFile.parentFile

        parent?.let {
            it.mkdirs()
            if (!it.isDirectory) {
                throw IOException("Unable to create parent directories of $this")
            }
        }
    }

    // Convenience function:
    @Suppress("NOTHING_TO_INLINE")
    private inline fun downstreamChannel(): ConflatedBroadcastChannel<T> {
        return downstreamChannel.get()
    }
}