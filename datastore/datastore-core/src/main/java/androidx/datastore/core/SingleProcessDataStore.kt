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
package androidx.datastore.core

import androidx.annotation.GuardedBy
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Represents the current state of the DataStore.
 */
private sealed class State<T>

private object UnInitialized : State<Any>()

/**
 * A read from disk has succeeded, value represents the current on disk state.
 */
private class Data<T>(val value: T, val hashCode: Int) : State<T>() {
    fun checkHashCode() {
        check(value.hashCode() == hashCode) {
            "Data in DataStore was mutated but DataStore is only compatible with Immutable types."
        }
    }
}

/**
 * A read from disk has failed. ReadException is the exception that was thrown.
 */
private class ReadException<T>(val readException: Throwable) : State<T>()

/**
 * The scope has been cancelled. This DataStore cannot process any new reads or writes.
 */
private class Final<T>(val finalException: Throwable) : State<T>()

/**
 * Single process implementation of DataStore. This is NOT multi-process safe.
 */
internal class SingleProcessDataStore<T>(
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
        /**
         * If downstream flow is UnInitialized, no data has been read yet, we need to trigger a new
         * read then start emitting values once we have seen a new value (or exception).
         *
         * If downstream flow has a ReadException, there was an exception last time we tried to read
         * data. We need to trigger a new read then start emitting values once we have seen a new
         * value (or exception).
         *
         * If downstream flow has Data, we should just start emitting from downstream flow.
         *
         * If Downstream flow is Final, the scope has been cancelled so the data store is no
         * longer usable. We should just propagate this exception.
         *
         * State always starts at null. null can transition to ReadException, Data or
         * Final. ReadException can transition to another ReadException, Data or Final.
         * Data can transition to another Data or Final. Final will not change.
         */

        val currentDownStreamFlowState = downstreamFlow.value

        if (currentDownStreamFlowState !is Data) {
            // We need to send a read request because we don't have data yet.
            actor.offer(Message.Read(currentDownStreamFlowState))
        }

        emitAll(
            downstreamFlow.dropWhile {
                if (currentDownStreamFlowState is Data<T> ||
                    currentDownStreamFlowState is Final<T>
                ) {
                    // We don't need to drop any Data or Final values.
                    false
                } else {
                    // we need to drop the last seen state since it was either an exception or
                    // wasn't yet initialized. Since we sent a message to actor, we *will* see a
                    // new value.
                    it === currentDownStreamFlowState
                }
            }.map {
                when (it) {
                    is ReadException<T> -> throw it.readException
                    is Final<T> -> throw it.finalException
                    is Data<T> -> it.value
                    is UnInitialized -> error(
                        "This is a bug in DataStore. Please file a bug at: " +
                            "https://issuetracker.google.com/issues/new?" +
                            "component=907884&template=1466542"
                    )
                }
            }
        )
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        /**
         * The states here are the same as the states for reads. Additionally we send an ack that
         * the actor *must* respond to (even if it is cancelled).
         */
        val ack = CompletableDeferred<T>()
        val currentDownStreamFlowState = downstreamFlow.value

        val updateMsg =
            Message.Update(transform, ack, currentDownStreamFlowState, coroutineContext)

        actor.offer(updateMsg)

        return ack.await()
    }

    private val SCRATCH_SUFFIX = ".tmp"

    private val file: File by lazy {
        val file = produceFile()

        file.absolutePath.let {
            synchronized(activeFilesLock) {
                check(!activeFiles.contains(it)) {
                    "There are multiple DataStores active for the same file: $file. You should " +
                        "either maintain your DataStore as a singleton or confirm that there is " +
                        "no two DataStore's active on the same file (by confirming that the scope" +
                        " is cancelled)."
                }
                activeFiles.add(it)
            }
        }

        file
    }

    @Suppress("UNCHECKED_CAST")
    private val downstreamFlow = MutableStateFlow(UnInitialized as State<T>)

    private var initTasks: List<suspend (api: InitializerApi<T>) -> Unit>? =
        initTasksList.toList()

    /** The actions for the actor. */
    private sealed class Message<T> {
        abstract val lastState: State<T>?

        /**
         * Represents a read operation. If the data is already cached, this is a no-op. If data
         * has not been cached, it triggers a new read to the specified dataChannel.
         */
        class Read<T>(
            override val lastState: State<T>?
        ) : Message<T>()

        /** Represents an update operation. */
        class Update<T>(
            val transform: suspend (t: T) -> T,
            /**
             * Used to signal (un)successful completion of the update to the caller.
             */
            val ack: CompletableDeferred<T>,
            override val lastState: State<T>?,
            val callerContext: CoroutineContext
        ) : Message<T>()
    }

    private val actor = SimpleActor<Message<T>>(
        scope = scope,
        onComplete = {
            it?.let {
                downstreamFlow.value = Final(it)
            }
            // We expect it to always be non-null but we will leave the alternative as a no-op
            // just in case.

            synchronized(activeFilesLock) {
                activeFiles.remove(file.absolutePath)
            }
        },
        onUndeliveredElement = { msg, ex ->
            if (msg is Message.Update) {
                // TODO(rohitsat): should we instead use scope.ensureActive() to get the original
                //  cancellation cause? Should we instead have something like
                //  UndeliveredElementException?
                msg.ack.completeExceptionally(
                    ex ?: CancellationException(
                        "DataStore scope was cancelled before updateData could complete"
                    )
                )
            }
        }
    ) { msg ->
        when (msg) {
            is Message.Read -> {
                handleRead(msg)
            }
            is Message.Update -> {
                handleUpdate(msg)
            }
        }
    }

    private suspend fun handleRead(read: Message.Read<T>) {
        when (val currentState = downstreamFlow.value) {
            is Data -> {
                // We already have data so just return...
            }
            is ReadException -> {
                if (currentState === read.lastState) {
                    readAndInitOrPropagateFailure()
                }

                // Someone else beat us but also failed. The collector has already
                // been signalled so we don't need to do anything.
            }
            UnInitialized -> {
                readAndInitOrPropagateFailure()
            }
            is Final -> error("Can't read in final state.") // won't happen
        }
    }

    private suspend fun handleUpdate(update: Message.Update<T>) {
        // All branches of this *must* complete ack either successfully or exceptionally.
        // We must *not* throw an exception, just propagate it to the ack.
        update.ack.completeWith(
            runCatching {

                when (val currentState = downstreamFlow.value) {
                    is Data -> {
                        // We are already initialized, we just need to perform the update
                        transformAndWrite(update.transform, update.callerContext)
                    }
                    is ReadException, is UnInitialized -> {
                        if (currentState === update.lastState) {
                            // we need to try to read again
                            readAndInitOrPropagateAndThrowFailure()

                            // We've successfully read, now we need to perform the update
                            transformAndWrite(update.transform, update.callerContext)
                        } else {
                            // Someone else beat us to read but also failed. We just need to
                            // signal the writer that is waiting on ack.
                            // This cast is safe because we can't be in the UnInitialized
                            // state if the state has changed.
                            throw (currentState as ReadException).readException
                        }
                    }

                    is Final -> throw currentState.finalException // won't happen
                }
            }
        )
    }

    private suspend fun readAndInitOrPropagateAndThrowFailure() {
        try {
            readAndInit()
        } catch (throwable: Throwable) {
            downstreamFlow.value = ReadException(throwable)
            throw throwable
        }
    }

    private suspend fun readAndInitOrPropagateFailure() {
        try {
            readAndInit()
        } catch (throwable: Throwable) {
            downstreamFlow.value = ReadException(throwable)
        }
    }

    private suspend fun readAndInit() {
        // This should only be called if we don't already have cached data.
        check(downstreamFlow.value == UnInitialized || downstreamFlow.value is ReadException)

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

        downstreamFlow.value = Data(initData, initData.hashCode())
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

    // downstreamFlow.value must be successfully set to data before calling this
    private suspend fun transformAndWrite(
        transform: suspend (t: T) -> T,
        callerContext: CoroutineContext
    ): T {
        // value is not null or an exception because we must have the value set by now so this cast
        // is safe.
        val curDataAndHash = downstreamFlow.value as Data<T>
        curDataAndHash.checkHashCode()

        val curData = curDataAndHash.value
        val newData = withContext(callerContext) { transform(curData) }

        // Check that curData has not changed...
        curDataAndHash.checkHashCode()

        return if (curData == newData) {
            curData
        } else {
            writeData(newData)
            downstreamFlow.value = Data(newData, newData.hashCode())
            newData
        }
    }

    /**
     * Internal only to prevent creation of synthetic accessor function. Do not call this from
     * outside this class.
     */
    internal suspend fun writeData(newData: T) {
        file.createParentDirectories()

        val scratchFile = File(file.absolutePath + SCRATCH_SUFFIX)
        try {
            FileOutputStream(scratchFile).use { stream ->
                serializer.writeTo(newData, UncloseableOutputStream(stream))
                stream.fd.sync()
                // TODO(b/151635324): fsync the directory, otherwise a badly timed crash could
                //  result in reverting to a previous state.
            }

            if (!scratchFile.renameTo(file)) {
                throw IOException(
                    "Unable to rename $scratchFile." +
                        "This likely means that there are multiple instances of DataStore " +
                        "for this file. Ensure that you are only creating a single instance of " +
                        "datastore for this file."
                )
            }
        } catch (ex: IOException) {
            if (scratchFile.exists()) {
                scratchFile.delete() // Swallow failure to delete
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

    // Wrapper on FileOutputStream to prevent closing the underlying OutputStream.
    private class UncloseableOutputStream(val fileOutputStream: FileOutputStream) : OutputStream() {

        override fun write(b: Int) {
            fileOutputStream.write(b)
        }

        override fun write(b: ByteArray) {
            fileOutputStream.write(b)
        }

        override fun write(bytes: ByteArray, off: Int, len: Int) {
            fileOutputStream.write(bytes, off, len)
        }

        override fun close() {
            // We will not close the underlying FileOutputStream until after we're done syncing
            // the fd. This is useful for things like b/173037611.
        }

        override fun flush() {
            fileOutputStream.flush()
        }
    }

    internal companion object {
        /**
         * Active files should contain the absolute path for which there are currently active
         * DataStores. A DataStore is active until the scope it was created with has been
         * cancelled. Files aren't added to this list until the first read/write because the file
         * path is computed asynchronously.
         */
        @GuardedBy("activeFilesLock")
        internal val activeFiles = mutableSetOf<String>()

        internal val activeFilesLock = Any()
    }
}
