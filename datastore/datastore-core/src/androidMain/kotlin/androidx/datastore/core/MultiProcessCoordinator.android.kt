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

@file:JvmName("MultiProcessCoordinatorKt") // Workaround for b/313964643

package androidx.datastore.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileLock
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class MultiProcessCoordinator(
    private val context: CoroutineContext,
    protected val file: File
) : InterProcessCoordinator {
    // TODO(b/269375542): the flow should `flowOn` the provided [context]
    override val updateNotifications: Flow<Unit> = MulticastFileObserver.observe(file)

    // run block with the exclusive lock
    override suspend fun <T> lock(block: suspend () -> T): T {
        inMemoryMutex.withLock {
            FileOutputStream(lockFile).use { lockFileStream ->
                var lock: FileLock? = null
                try {
                    lock = getExclusiveFileLockWithRetryIfDeadlock(lockFileStream)
                    return block()
                } finally {
                    lock?.release()
                }
            }
        }
    }

    // run block with an attempt to get the exclusive lock, still run even if
    // attempt fails. Pass a boolean to indicate if the attempt succeeds.
    @OptIn(ExperimentalContracts::class) // withTryLock
    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        inMemoryMutex.withTryLock<T> {
            if (it == false) {
                return block(false)
            }
            FileInputStream(lockFile).use { lockFileStream ->
                var lock: FileLock? = null
                try {
                    try {
                        lock =
                            lockFileStream
                                .getChannel()
                                .tryLock(
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
                        if (
                            (ex.message?.startsWith(LOCK_ERROR_MESSAGE) != true) &&
                                (ex.message?.startsWith(DEADLOCK_ERROR_MESSAGE) != true)
                        ) {
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

    // get the current version
    override suspend fun getVersion(): Int {
        // Only switch coroutine if sharedCounter is not initialized because initialization incurs
        // disk IO
        return withLazyCounter { it.getValue() }
    }

    // increment version and return the new one
    override suspend fun incrementAndGetVersion(): Int {
        // Only switch coroutine if sharedCounter is not initialized because initialization incurs
        // disk IO
        return withLazyCounter { it.incrementAndGetValue() }
    }

    private val LOCK_SUFFIX = ".lock"
    private val VERSION_SUFFIX = ".version"
    private val LOCK_ERROR_MESSAGE = "fcntl failed: EAGAIN"

    private val inMemoryMutex = Mutex()
    private val lockFile: File by lazy {
        val lockFile = fileWithSuffix(LOCK_SUFFIX)
        lockFile.createIfNotExists()
        lockFile
    }

    private val lazySharedCounter = lazy {
        SharedCounter.loadLib()
        SharedCounter.create {
            val versionFile = fileWithSuffix(VERSION_SUFFIX)
            versionFile.createIfNotExists()
            versionFile
        }
    }
    private val sharedCounter by lazySharedCounter

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

    /**
     * {@link SharedCounter} needs to be initialized in a separate coroutine so it does not violate
     * StrictMode policy in the main thread.
     */
    private suspend inline fun <T> withLazyCounter(
        crossinline block: suspend (SharedCounter) -> T
    ): T {
        return if (lazySharedCounter.isInitialized()) {
            block(sharedCounter)
        } else {
            withContext(context) { block(sharedCounter) }
        }
    }

    companion object {
        // Retry with exponential backoff to get file lock if it hits "Resource deadlock would
        // occur" error until the backoff reaches [MAX_WAIT_MILLIS].
        private suspend fun getExclusiveFileLockWithRetryIfDeadlock(
            lockFileStream: FileOutputStream
        ): FileLock {
            var backoff = INITIAL_WAIT_MILLIS
            while (backoff <= MAX_WAIT_MILLIS) {
                try {
                    return lockFileStream.getChannel().lock(0L, Long.MAX_VALUE, /* shared= */ false)
                } catch (ex: IOException) {
                    if (ex.message?.contains(DEADLOCK_ERROR_MESSAGE) != true) {
                        throw ex
                    }
                    delay(backoff)
                    backoff *= 2
                }
            }
            return lockFileStream.getChannel().lock(0L, Long.MAX_VALUE, /* shared= */ false)
        }

        private val DEADLOCK_ERROR_MESSAGE = "Resource deadlock would occur"
        private val INITIAL_WAIT_MILLIS: Long = 10
        private val MAX_WAIT_MILLIS: Long = 60000
    }
}

/**
 * Create a coordinator for multiple process use cases.
 *
 * @param context the coroutine context to be used by the [MultiProcessCoordinator] for IO
 *   operations.
 * @param file the File in which [DataStore] stores the data.
 */
@Suppress("StreamFiles")
fun createMultiProcessCoordinator(context: CoroutineContext, file: File): InterProcessCoordinator =
    MultiProcessCoordinator(context, file)
