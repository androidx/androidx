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

package androidx.datastore.core

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Multi process implementation of DataStore. It is multi-process safe. */
internal class DataStoreImpl<T>(
    private val storage: Storage<T>,
    /**
     * The list of initialization tasks to perform. These tasks will be completed before any data is
     * published to the data and before any read-modify-writes execute in updateData. If any of the
     * tasks fail, the tasks will be run again the next time data is collected or updateData is
     * called. Init tasks should not wait on results from data - this will result in deadlock.
     */
    initTasksList: List<suspend (api: InitializerApi<T>) -> Unit> = emptyList(),
    /**
     * The handler of [CorruptionException]s when they are thrown during reads or writes. It
     * produces the new data to replace the corrupted data on disk. By default it is a no-op which
     * simply throws the exception and does not produce new data.
     */
    private val corruptionHandler: CorruptionHandler<T> = NoOpCorruptionHandler(),
    private val scope: CoroutineScope = CoroutineScope(ioDispatcher() + SupervisorJob())
) : DataStore<T> {

    /**
     * Shared flow responsible for observing [InterProcessCoordinator] for file changes. Each
     * downstream [data] flow collects on this [kotlinx.coroutines.flow.SharedFlow] to ensure we
     * observe the [InterProcessCoordinator] when there is an active collection on the [data].
     */
    private val updateCollection =
        flow<Unit> {
                // deferring 1 flow so we can create coordinator lazily just to match existing
                // behavior.
                // also wait for initialization to complete before watching update events.
                readAndInit.awaitComplete()
                coordinator.updateNotifications.conflate().collect {
                    val currentState = inMemoryCache.currentState
                    if (currentState !is Final) {
                        // update triggered reads should always wait for lock
                        readDataAndUpdateCache(requireLock = true)
                    }
                }
            }
            .shareIn(
                scope = scope,
                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeout = Duration.ZERO,
                        replayExpiration = Duration.ZERO
                    ),
                replay = 0
            )

    /**
     * The actual values of DataStore. This is exposed in the API via [data] to be able to combine
     * its lifetime with IPC update collection ([updateCollection]).
     */
    private val internalDataFlow: Flow<T> = flow {
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
         * If Downstream flow is Final, the scope has been cancelled so the data store is no longer
         * usable. We should just propagate this exception.
         *
         * State always starts at null. null can transition to ReadException, Data or Final.
         * ReadException can transition to another ReadException, Data or Final. Data can transition
         * to another Data or Final. Final will not change.
         */
        // the first read should not be blocked by ongoing writes, so it can be dirty read. If it is
        // a unlocked read, the same value might be emitted to the flow again
        val startState = readState(requireLock = false)
        when (startState) {
            is Data<T> -> emit(startState.value)
            is UnInitialized -> error(BUG_MESSAGE)
            is ReadException<T> -> throw startState.readException
            // TODO(b/273990827): decide the contract of accessing when state is Final
            is Final -> return@flow
        }

        emitAll(
            inMemoryCache.flow
                .takeWhile {
                    // end the flow if we reach the final value
                    it !is Final
                }
                .dropWhile { it is Data && it.version <= startState.version }
                .map {
                    when (it) {
                        is ReadException<T> -> throw it.readException
                        is Data<T> -> it.value
                        is Final<T>,
                        is UnInitialized -> error(BUG_MESSAGE)
                    }
                }
        )
    }

    override val data: Flow<T> = channelFlow {
        val updateCollector =
            launch(start = CoroutineStart.LAZY) {
                updateCollection.collect {
                    // collect it infinitely so it keeps running as long as the data flow is active.
                }
            }
        internalDataFlow
            .onStart { updateCollector.start() }
            .onCompletion { updateCollector.cancel() }
            .collect { send(it) }
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        val parentContextElement = coroutineContext[UpdatingDataContextElement.Companion.Key]
        parentContextElement?.checkNotUpdating(this)
        val childContextElement =
            UpdatingDataContextElement(parent = parentContextElement, instance = this)
        return withContext(childContextElement) {
            val ack = CompletableDeferred<T>()
            val currentDownStreamFlowState = inMemoryCache.currentState
            val updateMsg =
                Message.Update(transform, ack, currentDownStreamFlowState, coroutineContext)
            writeActor.offer(updateMsg)
            ack.await()
        }
    }

    // cache is only set by the reads who have file lock, so cache always has stable data
    private val inMemoryCache = DataStoreInMemoryCache<T>()

    private val readAndInit = InitDataStore(initTasksList)

    // TODO(b/269772127): make this private after we allow multiple instances of DataStore on the
    //  same file
    private val storageConnectionDelegate = lazy { storage.createConnection() }
    internal val storageConnection by storageConnectionDelegate
    private val coordinator: InterProcessCoordinator by lazy { storageConnection.coordinator }

    private val writeActor =
        SimpleActor<Message.Update<T>>(
            scope = scope,
            onComplete = {
                // We expect it to always be non-null but we will leave the alternative as a no-op
                // just in case.
                it?.let { inMemoryCache.tryUpdate(Final(it)) }
                // don't try to close storage connection if it was not created in the first place.
                if (storageConnectionDelegate.isInitialized()) {
                    storageConnection.close()
                }
            },
            onUndeliveredElement = { msg, ex ->
                msg.ack.completeExceptionally(
                    ex
                        ?: CancellationException(
                            "DataStore scope was cancelled before updateData could complete"
                        )
                )
            }
        ) { msg ->
            handleUpdate(msg)
        }

    private suspend fun readState(requireLock: Boolean): State<T> =
        withContext(scope.coroutineContext) {
            if (inMemoryCache.currentState is Final) {
                // if state is Final, just return it
                inMemoryCache.currentState
            } else {
                try {
                    // make sure we initialize properly before reading from file.
                    readAndInitOrPropagateAndThrowFailure()
                } catch (throwable: Throwable) {
                    // init or read failed, it is already updated in the cached value
                    // so we don't need to do anything.
                    return@withContext ReadException(throwable, -1)
                }
                // after init, try to read again. If the init run for this block, it won't re-read
                // the file and use cache, so this is an OK call to make wrt performance.
                readDataAndUpdateCache(requireLock)
            }
        }

    private suspend fun handleUpdate(update: Message.Update<T>) {
        update.ack.completeWith(
            runCatching {
                val result: T
                when (val currentState = inMemoryCache.currentState) {
                    is Data -> {
                        // We are already initialized, we just need to perform the update
                        result = transformAndWrite(update.transform, update.callerContext)
                    }
                    is ReadException,
                    is UnInitialized -> {
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
        val preReadVersion = coordinator.getVersion()
        try {
            readAndInit.runIfNeeded()
        } catch (throwable: Throwable) {
            inMemoryCache.tryUpdate(ReadException(throwable, preReadVersion))
            throw throwable
        }
    }

    /**
     * Reads the file and updates the cache unless current cached value is Data and its version is
     * equal to the latest version, or it is unable to get lock.
     *
     * Calling this method when state is UnInitialized is a bug and this method will throw if that
     * happens.
     */
    private suspend fun readDataAndUpdateCache(requireLock: Boolean): State<T> {
        // Check if the cached version matches with shared memory counter
        val currentState = inMemoryCache.currentState
        // should not call this without initialization first running
        check(currentState !is UnInitialized) { BUG_MESSAGE }
        val latestVersion = coordinator.getVersion()
        val cachedVersion = if (currentState is Data) currentState.version else -1

        // Return cached value if cached version is latest
        if (currentState is Data && latestVersion == cachedVersion) {
            return currentState
        }
        val (newState, acquiredLock) =
            if (requireLock) {
                coordinator.lock {
                    try {
                        readDataOrHandleCorruption(hasWriteFileLock = true)
                    } catch (ex: Throwable) {
                        ReadException<T>(ex, coordinator.getVersion())
                    } to true
                }
            } else {
                coordinator.tryLock { locked ->
                    try {
                        readDataOrHandleCorruption(locked)
                    } catch (ex: Throwable) {
                        ReadException<T>(
                            ex,
                            if (locked) coordinator.getVersion() else cachedVersion
                        )
                    } to locked
                }
            }
        if (acquiredLock) {
            inMemoryCache.tryUpdate(newState)
        }
        return newState
    }

    // Caller is responsible for (try to) getting file lock. It reads from the file directly without
    // checking shared counter version and returns serializer default value if file is not found.
    private suspend fun readDataFromFileOrDefault(): T {
        return storageConnection.readData()
    }

    private suspend fun transformAndWrite(
        transform: suspend (t: T) -> T,
        callerContext: CoroutineContext
    ): T =
        coordinator.lock {
            val curData = readDataOrHandleCorruption(hasWriteFileLock = true)
            val newData = withContext(callerContext) { transform(curData.value) }

            // Check that curData has not changed...
            curData.checkHashCode()

            if (curData.value != newData) {
                writeData(newData, updateCache = true)
            }
            newData
        }

    // Write data to disk and return the corresponding version if succeed.
    internal suspend fun writeData(newData: T, updateCache: Boolean): Int {
        var newVersion = 0

        // The code in `writeScope` is run synchronously, i.e. the newVersion isn't returned until
        // the code in `writeScope` completes.
        storageConnection.writeScope {
            // update version before write to file to avoid the case where if update version after
            // file write, the process can crash after file write but before version increment, so
            // the readers might skip reading forever because the version isn't changed
            newVersion = coordinator.incrementAndGetVersion()
            writeData(newData)
            if (updateCache) {
                inMemoryCache.tryUpdate(Data(newData, newData.hashCode(), newVersion))
            }
        }

        return newVersion
    }

    private suspend fun readDataOrHandleCorruption(hasWriteFileLock: Boolean): Data<T> {
        try {
            return if (hasWriteFileLock) {
                val data = readDataFromFileOrDefault()
                Data(data, data.hashCode(), version = coordinator.getVersion())
            } else {
                val preLockVersion = coordinator.getVersion()
                coordinator.tryLock { locked ->
                    val data = readDataFromFileOrDefault()
                    val version = if (locked) coordinator.getVersion() else preLockVersion
                    Data(data, data.hashCode(), version)
                }
            }
        } catch (ex: CorruptionException) {
            var newData: T = corruptionHandler.handleCorruption(ex)
            var version: Int // initialized inside the try block

            try {
                doWithWriteFileLock(hasWriteFileLock) {
                    // Confirms the file is still corrupted before overriding
                    try {
                        newData = readDataFromFileOrDefault()
                        version = coordinator.getVersion()
                    } catch (ignoredEx: CorruptionException) {
                        version = writeData(newData, updateCache = true)
                    }
                }
            } catch (writeEx: Throwable) {
                // If we fail to write the handled data, add the new exception as a suppressed
                // exception.
                ex.addSuppressed(writeEx)
                throw ex
            }

            // If we reach this point, we've successfully replaced the data on disk with newData.
            return Data(newData, newData.hashCode(), version)
        }
    }

    @OptIn(ExperimentalContracts::class)
    private suspend fun <R> doWithWriteFileLock(
        hasWriteFileLock: Boolean,
        block: suspend () -> R
    ): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return if (hasWriteFileLock) {
            block()
        } else {
            coordinator.lock { block() }
        }
    }

    private inner class InitDataStore(
        initTasksList: List<suspend (api: InitializerApi<T>) -> Unit>
    ) : RunOnce() {
        // cleaned after initialization is complete
        private var initTasks: List<suspend (api: InitializerApi<T>) -> Unit>? =
            initTasksList.toList()

        override suspend fun doRun() {
            val initData =
                if ((initTasks == null) || initTasks!!.isEmpty()) {
                    // if there are no init tasks, we can directly read
                    readDataOrHandleCorruption(hasWriteFileLock = false)
                } else {
                    // if there are init tasks, we need to obtain a lock to ensure migrations
                    // run as 1 chunk
                    coordinator.lock {
                        val updateLock = Mutex()
                        var initializationComplete = false
                        var currentData = readDataOrHandleCorruption(hasWriteFileLock = true).value

                        val api =
                            object : InitializerApi<T> {
                                override suspend fun updateData(transform: suspend (t: T) -> T): T {
                                    return updateLock.withLock {
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
                        updateLock.withLock { initializationComplete = true }
                        // only to make compiler happy
                        Data(
                            value = currentData,
                            hashCode = currentData.hashCode(),
                            version = coordinator.getVersion()
                        )
                    }
                }
            inMemoryCache.tryUpdate(initData)
        }
    }

    companion object {
        private const val BUG_MESSAGE =
            "This is a bug in DataStore. Please file a bug at: " +
                "https://issuetracker.google.com/issues/new?component=907884&template=1466542"
    }
}

/**
 * Helper class that executes [doRun] up to 1 time to completion. If it fails, it will be retried in
 * the next [runIfNeeded] call.
 */
internal abstract class RunOnce {
    private val runMutex = Mutex()
    private val didRun = CompletableDeferred<Unit>()

    protected abstract suspend fun doRun()

    suspend fun awaitComplete() = didRun.await()

    suspend fun runIfNeeded() {
        if (didRun.isCompleted) return
        runMutex.withLock {
            if (didRun.isCompleted) return
            doRun()
            didRun.complete(Unit)
        }
    }
}

/**
 * [CoroutineContext.Element] that is added to the coroutineContext when [DataStore.updateData] is
 * called to detect nested calls. b/241760537 (see: [DataStoreImpl.updateData])
 *
 * It is OK for different DataStore instances to nest updateData calls, they just cannot be on the
 * same DataStore. To track these instances, each [UpdatingDataContextElement] holds a reference to
 * a parent.
 */
internal class UpdatingDataContextElement(
    private val parent: UpdatingDataContextElement?,
    private val instance: DataStoreImpl<*>
) : CoroutineContext.Element {

    companion object {
        internal val NESTED_UPDATE_ERROR_MESSAGE =
            """
                Calling updateData inside updateData on the same DataStore instance is not supported
                since updates made in the parent updateData call will not be visible to the nested
                updateData call. See https://issuetracker.google.com/issues/241760537 for details.
            """
                .trimIndent()

        internal object Key : CoroutineContext.Key<UpdatingDataContextElement>
    }

    /** Checks the given [candidate] is not currently in a [DataStore.updateData] block. */
    fun checkNotUpdating(candidate: DataStore<*>) {
        if (instance === candidate) {
            error(NESTED_UPDATE_ERROR_MESSAGE)
        }
        // check the parent if it exists to detect nested calls between [DataStore] instances.
        parent?.checkNotUpdating(candidate)
    }

    override val key: CoroutineContext.Key<*>
        get() = Key
}
