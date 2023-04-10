/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.FileObserver
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Multi process implementation of DataStore. It is multi-process safe.
 */
internal class MultiProcessDataStore<T>(
    private val storage: Storage<T>,
    /**
     * The list of initialization tasks to perform. These tasks will be completed before any data
     * is published to the data and before any read-modify-writes execute in updateData.  If
     * any of the tasks fail, the tasks will be run again the next time data is collected or
     * updateData is called. Init tasks should not wait on results from data - this will
     * result in deadlock.
     */
    initTasksList: List<suspend (api: InitializerApi<T>) -> Unit> = emptyList(),
    private val corruptionHandler: CorruptionHandler<T> = NoOpCorruptionHandler<T>(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val produceFile: () -> File
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
         * If downstream flow has Data, we should start emitting from downstream flow as long as its
         * version is not stale compared to the version read from the shared counter when we enter
         * the flow.
         *
         * If Downstream flow is Final, the scope has been cancelled so the data store is no
         * longer usable. We should just propagate this exception.
         *
         * State always starts at null. null can transition to ReadException, Data or
         * Final. ReadException can transition to another ReadException, Data or Final.
         * Data can transition to another Data or Final. Final will not change.
         */
        // Only switch coroutine if sharedCounter is not initialized because initialization incurs
        // disk IO
        val latestVersionAtRead =
            if (lazySharedCounter.isInitialized()) sharedCounter.getValue() else
                withContext(scope.coroutineContext) {
                    sharedCounter.getValue()
                }
        val currentDownStreamFlowState = downstreamFlow.value

        if ((currentDownStreamFlowState !is Data) ||
            (currentDownStreamFlowState.version < latestVersionAtRead)
        ) {
            // We need to send a read request because we don't have data yet / cached data is stale.
            readActor.offer(Message.Read(currentDownStreamFlowState))
        }

        emitAll(
            downstreamFlow.dropWhile {
                if (currentDownStreamFlowState is Data<T>) {
                    // we need to drop until initTasks are completed and set to null, and data
                    // version >= the current version when entering flow
                    (it !is Data) || (it.version < latestVersionAtRead)
                } else if (currentDownStreamFlowState is Final<T>) {
                    // We don't need to drop Final values.
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
                        BUG_MESSAGE
                    )
                }
            }
        )
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        val ack = CompletableDeferred<T>()
        val currentDownStreamFlowState = downstreamFlow.value

        val updateMsg =
            Message.Update(transform, ack, currentDownStreamFlowState, coroutineContext)

        writeActor.offer(updateMsg)

        return ack.await()
    }

    private val LOCK_SUFFIX = ".lock"
    private val VERSION_SUFFIX = ".version"
    private val BUG_MESSAGE = "This is a bug in DataStore. Please file a bug at: " +
        "https://issuetracker.google.com/issues/new?component=907884&template=1466542"
    // TODO(b/255419657): update the shared lock IOException handling logic
    private val LOCK_ERROR_MESSAGE = "fcntl failed: EAGAIN"
    private val INVALID_VERSION = -1
    private var initTasks: List<suspend (api: InitializerApi<T>) -> Unit>? =
        initTasksList.toList()

    private val lazySharedCounter = lazy {
        SharedCounter.loadLib()
        SharedCounter.create {
            val versionFile = fileWithSuffix(VERSION_SUFFIX)
            versionFile.createIfNotExists()
            versionFile
        }
    }
    private val sharedCounter by lazySharedCounter

    private val threadLock = Mutex()
    private val storageConnection: StorageConnection<T> by lazy {
        storage.createConnection()
    }

    // file is protected rather than private to avoid requiring synthetic accessor for its usage in
    // the definition of FileObserver
    protected val file: File by lazy {
        produceFile()
    }

    private val lockFile: File by lazy {
        val lockFile = fileWithSuffix(LOCK_SUFFIX)
        lockFile.createIfNotExists()
        lockFile
    }

    private val fileObserver: FileObserver by lazy {
        @Suppress("DEPRECATION")
        object : FileObserver(file.canonicalFile.parent!!, FileObserver.MOVED_TO) {
            // It will be triggered by same-process-write as well. Shared memory version check will
            // prevent it from reading again. parameter `path` is relative to the observed directory
            override fun onEvent(event: Int, path: String?) {
                if ((downstreamFlow.value !is Final) && (file.name == path)) {
                    readActor.offer(Message.Read(downstreamFlow.value))
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val downstreamFlow = MutableStateFlow(UnInitialized as State<T>)

    private val writeActor = SimpleActor<Message.Update<T>>(
        scope = scope,
        onComplete = {
            it?.let {
                downstreamFlow.value = Final(it)
            }
            // We expect it to always be non-null but we will leave the alternative as a no-op
            // just in case.

            storageConnection.close()
        },
        onUndeliveredElement = { msg, ex ->
            msg.ack.completeExceptionally(
                ex ?: CancellationException(
                    "DataStore scope was cancelled before updateData could complete"
                )
            )
        }
    ) { msg ->
        handleUpdate(msg)
    }

    private val readActor = SimpleActor<Message.Read<T>>(
        scope = scope,
        onComplete = {
            // no more reads so stop listening to file changes
            fileObserver.stopWatching()
        },
        onUndeliveredElement = { _, _ -> }
    ) { msg ->
        handleRead(msg)
    }

    private suspend fun handleRead(read: Message.Read<T>) {
        when (val currentState = downstreamFlow.value) {
            is Data -> {
                readData()
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
        update.ack.completeWith(
            runCatching {
                var result: T
                when (val currentState = downstreamFlow.value) {
                    is Data -> {
                        // We are already initialized, we just need to perform the update
                        result = transformAndWrite(update.transform, update.callerContext)
                    }
                    is ReadException, is UnInitialized -> {
                        if (currentState === update.lastState) {
                            // we need to try to read again
                            readAndInitOrPropagateAndThrowFailure()

                            // We've successfully read, now we need to perform the update
                            result = transformAndWrite(update.transform, update.callerContext)
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
                result
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

    // It handles the read when data needs to be initialized.
    private suspend fun readAndInit() {
        initTaskLock.withLock() {
            // This should only be called if we don't already have cached data.
            if (downstreamFlow.value != UnInitialized && downstreamFlow.value !is ReadException) {
                // downstreamFlow.value is Data or Final, no need to readAndInit. As there are two
                // actors, we return here instead of throwing exception to properly handle the race
                // condition where one actor call `readAndInit()` after the other has completed
                // successfully.
                return
            }

            var initData: Data<T>
            if ((initTasks == null) || initTasks!!.isEmpty()) {
                initData = readDataOrHandleCorruption(hasWriteFileLock = false)
            } else {
                initData = getWriteFileLock {
                    val updateLock = Mutex()
                    var initializationComplete: Boolean = false
                    var currentData = readDataOrHandleCorruption(hasWriteFileLock = true).value

                    val api = object : InitializerApi<T> {
                        override suspend fun updateData(transform: suspend (t: T) -> T): T {
                            return updateLock.withLock() {
                                check(!initializationComplete) {
                                    "InitializerApi.updateData should not be called after " +
                                        "initialization is complete."
                                }

                                val newData = transform(currentData)
                                if (newData != currentData) {
                                    writeData(newData, updateCache = false)
                                    currentData = newData
                                }

                                currentData
                            }
                        }
                    }

                    initTasks?.forEach { it(api) }
                    // Init tasks have run successfully, we don't need them anymore.
                    initTasks = null
                    updateLock.withLock {
                        initializationComplete = true
                    }

                    // only to make compiler happy
                    currentData
                }
            }
            downstreamFlow.value = initData
            fileObserver.startWatching()
        }
    }

    // Only be called from `readAndInit`. State is UnInitialized or ReadException.
    private suspend fun readDataOrHandleCorruption(hasWriteFileLock: Boolean): Data<T> {
        try {
            if (hasWriteFileLock) {
                val data = readDataFromFileOrDefault()
                return Data(data, data.hashCode(), version = sharedCounter.getValue())
            } else {
                return tryGetReadFileLock {
                    val data = readDataFromFileOrDefault()
                    val version = if (it) sharedCounter.getValue() else INVALID_VERSION
                    Data(
                        data,
                        data.hashCode(),
                        version
                    )
                }
            }
        } catch (ex: CorruptionException) {
            var newData: T = corruptionHandler.handleCorruption(ex)
            var version: Int = INVALID_VERSION // should be overridden if write successfully

            try {
                doWithWriteFileLock(hasWriteFileLock) {
                    // Confirms the file is still corrupted before overriding
                    try {
                        newData = readDataFromFileOrDefault()
                        version = sharedCounter.getValue()
                    } catch (ignoredEx: CorruptionException) {
                        version = writeData(newData)
                    }
                    newData
                }
            } catch (writeEx: IOException) {
                // If we fail to write the handled data, add the new exception as a suppressed
                // exception.
                ex.addSuppressed(writeEx)
                throw ex
            }

            // If we reach this point, we've successfully replaced the data on disk with newData.
            return Data(newData, newData.hashCode(), version)
        }
    }

    private suspend fun doWithWriteFileLock(hasWriteFileLock: Boolean, block: suspend () -> T) {
        if (hasWriteFileLock) block() else getWriteFileLock { block() }
    }

    // It handles the read when the current state is Data
    private suspend fun readData(): T {
        // Check if the cached version matches with shared memory counter
        val currentState = downstreamFlow.value
        val version = sharedCounter.getValue()
        val cachedVersion = if (currentState is Data) currentState.version else INVALID_VERSION

        // Return cached value if cached version is latest
        if (currentState is Data && version == cachedVersion) {
            return currentState.value
        }
        val data = tryGetReadFileLock {
            val result = readDataFromFileOrDefault()
            Data(
                result,
                result.hashCode(),
                if (it) sharedCounter.getValue() else INVALID_VERSION
            )
        }
        downstreamFlow.value = data
        return data.value
    }

    // Caller is responsible for (try to) getting file lock. It reads from the file directly without
    // checking shared counter version and returns serializer default value if file is not found.
    private suspend fun readDataFromFileOrDefault(): T {
        return storageConnection.readData()
    }

    private suspend fun transformAndWrite(
        transform: suspend (t: T) -> T,
        callerContext: CoroutineContext
    ): T = getWriteFileLock {
        val curData = readDataFromFileOrDefault()
        val curDataAndHash = Data(curData, curData.hashCode(), /* unused */ version = 0)
        val newData = withContext(callerContext) { transform(curData) }

        // Check that curData has not changed...
        curDataAndHash.checkHashCode()

        if (curData != newData) {
            writeData(newData)
        }
        newData
    }.value

    // Write data to disk and return the corresponding version if succeed.
    internal suspend fun writeData(newData: T, updateCache: Boolean = true): Int {
        var newVersion: Int = 0

        // The code in `writeScope` is run synchronously, i.e. the newVersion isn't returned until
        // the code in `writeScope` completes.
        storageConnection.writeScope {
            // TODO(b/256242862): decide the long term solution to increment version after write to
            // scratch file. We used to increment version after scratch file write for performance
            // optimization for concurrent reads and failed writes.
            newVersion = sharedCounter.incrementAndGetValue()
            writeData(newData)
            if (updateCache) {
                downstreamFlow.value = Data(newData, newData.hashCode(), newVersion)
            }
        }

        return newVersion
    }

    private fun fileWithSuffix(suffix: String): File {
        return File(file.absolutePath + suffix)
    }

    private fun File.createIfNotExists() {
        createParentDirectories()
        if (!exists()) {
            createNewFile()
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

    private suspend fun getWriteFileLock(block: suspend () -> T): Data<T> {
        threadLock.withLock {
            FileOutputStream(lockFile).use { lockFileStream ->
                var lock: FileLock? = null
                try {
                    lock = lockFileStream.getChannel().lock(0L, Long.MAX_VALUE, /* shared= */ false)
                    val data = block()
                    return Data(data, data.hashCode(), sharedCounter.getValue())
                } finally {
                    lock?.release()
                }
            }
        }
    }

    private suspend fun tryGetReadFileLock(
        block: suspend (Boolean) -> Data<T>
    ): Data<T> {
        return threadLock.withTryLock<Data<T>> {
            if (it == false) {
                return block(false)
            }
            FileInputStream(lockFile).use { lockFileStream ->
                var lock: FileLock? = null
                try {
                    try {
                        lock = lockFileStream.getChannel().tryLock(
                            /* position= */ 0L,
                            /* size= */ Long.MAX_VALUE,
                            /* shared= */ true
                        )
                    } catch (ex: IOException) {
                        // TODO(b/255419657): Update the shared lock IOException handling logic for
                        // KMM.

                        // Some platforms / OS do not support shared lock and convert shared lock
                        // requests to exclusive lock requests. If the lock can't be acquired, it
                        // will throw an IOException with EAGAIN error, instead of returning null as
                        // specified in {@link FileChannel#tryLock}. We only continue if the error
                        // message is EAGAIN, otherwise just throw it.
                        if (ex.message?.startsWith(LOCK_ERROR_MESSAGE) != true) {
                            throw ex
                        }
                    }
                    return block(lock != null)
                } finally {
                    lock?.release()
                }
            }
        }
    }

    /**
     * Provide similar functionality of {@link kotlinx.coroutines.sync.Mutex#withLock} but don't
     * wait for the lock if unavailable, instead it passes a Boolean into the {@link action} lambda
     * to indicate if it is able to get the lock and run {@link action} immediately.
     */
    private inline fun <R> Mutex.withTryLock(owner: Any? = null, action: (Boolean) -> R): R {
        val locked: Boolean = tryLock(owner)
        try {
            return action(locked)
        } finally {
            if (locked) {
                unlock(owner)
            }
        }
    }

    internal companion object {
        internal val initTaskLock = Mutex()
    }
}