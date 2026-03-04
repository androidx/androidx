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

import androidx.datastore.core.handlers.ReThrowCorruptionHandler
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
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
    private val corruptionHandler: CorruptionHandler<T> = ReThrowCorruptionHandler(),
    private val context: CoroutineContext = ioDispatcher() + SupervisorJob(),
    private val tracer: DataStoreTracer? = null,
) : CurrentDataProviderStore<T> {

    private val scope: CoroutineScope =
        CoroutineScope(context + SupervisorJob(parent = context[Job]))

    /**
     * The actual values of DataStore. This is exposed in the API via [data] to be able to combine
     * its lifetime with IPC update collection ([updateCollection]).
     */
    override val data: Flow<T> = flow {
        // Ensure the DataStore's context hasn't been cancelled before starting
        context.ensureActive()
        // Get new token to pass the trace along to the child spans in `
        // readAndInitOrPropagateAndThrowFailure` and `readAndUpdateCache`.
        val token = captureTraceToken(tracer)
        val startState = readState(requireLock = false, token = token)

        when (startState) {
            is Data<T> -> emit(startState.value)
            is UnInitialized -> error(BUG_MESSAGE)
            is ReadException<T> -> throw startState.readException
            // TODO(b/273990827): decide the contract of accessing when state is Final
            is Final -> return@flow
            is NoValueDataState -> error(BUG_MESSAGE)
        }

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
        emitAll(
            inMemoryCache.flow
                .onStart { incrementCollector() }
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
                        is UnInitialized,
                        is NoValueDataState -> error(BUG_MESSAGE)
                    }
                }
                .onCompletion { decrementCollector() }
        )
    }

    override suspend fun currentData(): T {
        context.ensureActive()
        val token = captureTraceToken(tracer)
        when (val startState = readState(requireLock = false, token = token)) {
            is Data<T> -> return startState.value
            is UnInitialized -> error(BUG_MESSAGE)
            is ReadException<T> -> throw startState.readException
            // TODO(b/273990827): decide the contract of accessing when state is Final
            is Final -> throw startState.finalException
            is NoValueDataState -> error(BUG_MESSAGE)
        }
    }

    private val collectorMutex = Mutex()
    private var collectorCounter = 0
    /**
     * Job responsible for observing [InterProcessCoordinator] for file changes. Each downstream
     * [data] flow collects on this [kotlinx.coroutines.Job] to ensure we observe the
     * [InterProcessCoordinator] when there is an active collection on the [data].
     */
    private var collectorJob: Job? = null

    private suspend fun incrementCollector() {
        collectorMutex.withLock {
            if (++collectorCounter == 1) {
                collectorJob =
                    scope.launch {
                        readAndInit.awaitComplete()
                        coordinator.updateNotifications.conflate().collect {
                            val currentState = inMemoryCache.currentState
                            if (currentState !is Final) {
                                // update triggered reads should always wait for lock
                                readDataAndUpdateCache(requireLock = true)
                            }
                        }
                    }
            }
        }
    }

    private suspend fun decrementCollector() {
        collectorMutex.withLock {
            if (--collectorCounter == 0) {
                collectorJob?.cancel()
                collectorJob = null
            }
        }
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        context.ensureActive()
        return trace(tracer = tracer, name = "DataStore.updateData") {
            // Get new token as we will have a new span under `JDS.updateData` after `withContext`
            val token = captureTraceToken(tracer)
            val parentContextElement = coroutineContext[UpdatingDataContextElement.Companion.Key]
            parentContextElement?.checkNotUpdating(this)
            val childContextElement =
                UpdatingDataContextElement(parent = parentContextElement, instance = this)
            withContext(childContextElement) {
                val ack = CompletableDeferred<T>()
                val currentDownStreamFlowState = inMemoryCache.currentState
                // Skip passing the actual cached value if it's Data since Message.Update doesn't
                // rely on the cached value. This enables potentially earlier GC.
                val enqueueState: State<T> =
                    if (currentDownStreamFlowState is Data) {
                        NoValueDataState(currentDownStreamFlowState.version)
                    } else {
                        currentDownStreamFlowState
                    }

                val updateMsg =
                    Message.Update(transform, ack, enqueueState, coroutineContext, token)
                writeActor.offer(updateMsg)
                ack.await()
            }
        }
    }

    // cache is only set by the reads who have file lock, so cache always has stable data
    private val inMemoryCache = DataStoreInMemoryCache<T>()

    private val readAndInit = InitDataStore(initTasksList, scope.coroutineContext)

    // TODO(b/269772127): make this private after we allow multiple instances of DataStore on the
    //  same file
    private val storageConnectionDelegate = lazy { storage.createConnection() }
    internal val storageConnection by storageConnectionDelegate
    private val coordinatorDelegate = lazy { storageConnection.coordinator }
    private val coordinator: InterProcessCoordinator by coordinatorDelegate

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
            },
        ) { msg ->
            handleUpdate(msg)
        }

    // Run on caller's coroutine until there is disk I/O needed.
    private suspend fun readState(requireLock: Boolean, token: DataStoreTraceToken?): State<T> {
        if (inMemoryCache.currentState is Final) {
            // if state is Final, just return it
            return inMemoryCache.currentState
        } else {
            try {
                // make sure we initialize properly before reading from file.
                readAndInitOrPropagateAndThrowFailure(token)
            } catch (throwable: Throwable) {
                // init or read failed, it is already updated in the cached value
                // so we don't need to do anything.
                return ReadException(throwable, -1)
            }
            // after init, try to read again. If the init run for this block, it won't re-read
            // the file and use cache, so this is an OK call to make wrt performance.
            return readDataAndUpdateCache(requireLock)
        }
    }

    private suspend fun handleUpdate(update: Message.Update<T>) {
        trace(tracer = tracer, name = "DataStore.handleUpdate", token = update.token) {
            // Get new token as we are under a new span ("DataStore.handleUpdate")
            val handleUpdateToken = captureTraceToken(tracer)
            update.ack.completeWith(
                runCatching {
                    // Combine caller and datastore context keys. Since we add contexts in the order
                    // "caller + datastore context", we'll have all the keys from the datastore
                    // context,
                    // and all keys in the caller context that were not present in the datastore
                    // context.
                    withContext(update.callerContext + coroutineContext) {
                        val result: T
                        when (val currentState = inMemoryCache.currentState) {
                            is Data -> {
                                // We are already initialized, we just need to perform the update
                                result =
                                    transformAndWrite(
                                        update.transform,
                                        update.callerContext,
                                        handleUpdateToken,
                                    )
                            }
                            is ReadException,
                            is UnInitialized -> {
                                if (currentState === update.lastState) {
                                    // we need to try to read again
                                    readAndInitOrPropagateAndThrowFailure(handleUpdateToken)

                                    // We've successfully read, now we need to perform the update
                                    result =
                                        transformAndWrite(
                                            update.transform,
                                            update.callerContext,
                                            handleUpdateToken,
                                        )
                                } else {
                                    // Someone else beat us to read but also failed. We just need to
                                    // signal the writer that is waiting on ack.
                                    // This cast is safe because we can't be in the UnInitialized
                                    // state if the state has changed.
                                    throw (currentState as ReadException).readException
                                }
                            }

                            is Final -> throw currentState.finalException // won't happen
                            is NoValueDataState -> error(BUG_MESSAGE) // won't happen
                        }
                        result
                    }
                }
            )
        }
    }

    // [readAndInit] will dispatch the work to the dispatcher DataStore's CoroutineScope if needed.
    private suspend fun readAndInitOrPropagateAndThrowFailure(token: DataStoreTraceToken?) {
        trace(
            tracer = tracer,
            name = "DataStore.readAndInitOrPropagateAndThrowFailure",
            token = token,
        ) {
            // Initialization needs to run on DataStore's scope instead of caller's scope
            val preReadVersion =
                if (coordinatorDelegate.isInitialized()) {
                    coordinator.getVersion()
                } else {
                    // Get new token as we are under a new span
                    // ("DataStore.readAndInitOrPropagateAndThrowFailure")
                    val readAndInitOrPropagateAndThrowFailureToken = captureTraceToken(tracer)
                    withContext(scope.coroutineContext) {
                        trace(
                            tracer = tracer,
                            name = "DataStore.getCoordinatorVersion",
                            token = readAndInitOrPropagateAndThrowFailureToken,
                        ) {
                            coordinator.getVersion()
                        }
                    }
                }
            try {
                readAndInit.runIfNeeded()
            } catch (throwable: Throwable) {
                inMemoryCache.tryUpdate(ReadException(throwable, preReadVersion))
                throw throwable
            }
        }
    }

    /**
     * Reads the file and updates the cache unless current cached value is Data and its version is
     * equal to the latest version, or it is unable to get lock.
     *
     * Calling this method when state is UnInitialized is a bug and this method will throw if that
     * happens.
     *
     * Run on caller's thread unless there is disk I/O needed - then it'd run on the dispatcher from
     * DataStore's CoroutineScope.
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
        val getVersion: suspend (Boolean) -> (Int) = { hasLock ->
            if (hasLock) {
                coordinator.getVersion()
            } else {
                cachedVersion
            }
        }
        val (newState, acquiredLock) =
            if (requireLock) {
                coordinator.lock {
                    try {
                        readDataOrHandleCorruption(hasWriteFileLock = true, getVersion = getVersion)
                    } catch (ex: Throwable) {
                        ReadException<T>(ex, getVersion.invoke(requireLock))
                    } to true
                }
            } else {
                coordinator.tryLock { locked ->
                    try {
                        readDataOrHandleCorruption(locked, getVersion)
                    } catch (ex: Throwable) {
                        ReadException<T>(ex, getVersion.invoke(locked))
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
        return trace(tracer = tracer, name = "DataStore.readDataFromFileOrDefault") {
            storageConnection.readData()
        }
    }

    private suspend fun transformAndWrite(
        transform: suspend (t: T) -> T,
        callerContext: CoroutineContext,
        token: DataStoreTraceToken?,
    ): T =
        coordinator.lock {
            trace(tracer = tracer, name = "DataStore.transformAndWrite", token = token) {
                val curData =
                    readDataOrHandleCorruption(
                        hasWriteFileLock = true,
                        getVersion = { coordinator.getVersion() },
                    )
                // Get new token as we are under a new span ("DataStore.transformAndWrite")
                val transformAndWriteToken = captureTraceToken(tracer)
                val newData =
                    withContext(callerContext) {
                        trace(
                            tracer = tracer,
                            name = "DataStore.transform",
                            token = transformAndWriteToken,
                        ) {
                            transform(curData.value)
                        }
                    }

                // Check that curData has not changed...
                curData.checkHashCode()

                if (curData.value != newData) {
                    writeData(newData, updateCache = true)
                }
                newData
            }
        }

    // Write data to disk and return the corresponding version if succeed.
    internal suspend fun writeData(newData: T, updateCache: Boolean): Int {
        var newVersion = 0

        // The code in `writeScope` is run synchronously, i.e. the newVersion isn't returned until
        // the code in `writeScope` completes.
        storageConnection.writeScope {
            trace(tracer = tracer, name = "DataStore.writeData") {
                // update version before write to file to avoid the case where if update version
                // after file write, the process can crash after file write but before version
                // increment, so
                // the readers might skip reading forever because the version isn't changed
                newVersion = coordinator.incrementAndGetVersion()
                writeData(newData)
                if (updateCache) {
                    inMemoryCache.tryUpdate(Data(newData, newData.hashCode(), newVersion))
                }
            }
        }

        return newVersion
    }

    // Run on DataStore's CoroutineScope for I/O
    private suspend fun readDataOrHandleCorruption(
        hasWriteFileLock: Boolean,
        getVersion: suspend (Boolean) -> (Int),
    ): Data<T> {
        // Get new token as we are under a new span after `withContext`.
        val readOrHandleCorruptionToken = captureTraceToken(tracer)
        return withContext(scope.coroutineContext) {
            trace(
                tracer = tracer,
                name = "DataStore.readDataOrHandleCorruption",
                token = readOrHandleCorruptionToken,
            ) {
                try {
                    if (hasWriteFileLock) {
                        val data = readDataFromFileOrDefault()
                        Data(data, data.hashCode(), version = getVersion.invoke(hasWriteFileLock))
                    } else {
                        coordinator.tryLock { locked ->
                            val data = readDataFromFileOrDefault()
                            // The cached version is provided via the param is the last version that
                            // was
                            // successfully read and cached by this datastore instance.
                            Data(data, data.hashCode(), getVersion.invoke(locked))
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
                                version = getVersion.invoke(hasWriteFileLock)
                            } catch (ignoredEx: CorruptionException) {
                                version = writeData(newData, updateCache = true)
                            }
                        }
                    } catch (writeEx: Throwable) {
                        // If we fail to write the handled data, add the new exception as a
                        // suppressed
                        // exception.
                        ex.addSuppressed(writeEx)
                        throw ex
                    }

                    // If we reach this point, we've successfully replaced the data on disk with
                    // newData.
                    Data(newData, newData.hashCode(), version)
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    @Suppress(
        "LEAKED_IN_PLACE_LAMBDA",
        "WRONG_INVOCATION_KIND",
    ) // https://youtrack.jetbrains.com/issue/KT-29963
    private suspend fun <R> doWithWriteFileLock(
        hasWriteFileLock: Boolean,
        block: suspend () -> R,
    ): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return if (hasWriteFileLock) {
            block()
        } else {
            coordinator.lock { block() }
        }
    }

    // Run on DataStore's CoroutineScope for I/O
    private inner class InitDataStore(
        initTasksList: List<suspend (api: InitializerApi<T>) -> Unit>,
        val context: CoroutineContext,
    ) : RunOnce() {
        // cleaned after initialization is complete
        private var initTasks: List<suspend (api: InitializerApi<T>) -> Unit>? =
            initTasksList.toList()

        override suspend fun doRun() {
            // Capture new token due to call to `withContext`.
            val readAndInitToken = captureTraceToken(tracer)
            withContext(context) {
                trace(
                    tracer = tracer,
                    name = "DataStore.InitDataStore.doRun",
                    token = readAndInitToken,
                ) {
                    val getVersion: suspend (Boolean) -> (Int) = {
                        // We don't have the cached value case during initialization, so we just get
                        // the
                        // version from the coordinator.
                        coordinator.getVersion()
                    }
                    val initData =
                        if ((initTasks == null) || initTasks!!.isEmpty()) {
                            // if there are no init tasks, we can directly read
                            readDataOrHandleCorruption(
                                hasWriteFileLock = false,
                                getVersion = getVersion,
                            )
                        } else {
                            // if there are init tasks, we need to obtain a lock to ensure
                            // migrations
                            // run as 1 chunk
                            coordinator.lock {
                                val updateLock = Mutex()
                                var initializationComplete = false
                                var currentData =
                                    readDataOrHandleCorruption(
                                            hasWriteFileLock = true,
                                            getVersion = getVersion,
                                        )
                                        .value

                                val api =
                                    object : InitializerApi<T> {
                                        override suspend fun updateData(
                                            transform: suspend (t: T) -> T
                                        ): T {
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
                                    version = coordinator.getVersion(),
                                )
                            }
                        }
                    inMemoryCache.tryUpdate(initData)
                }
            }
        }
    }

    companion object {
        internal const val BUG_MESSAGE =
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
    private val instance: DataStoreImpl<*>,
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
