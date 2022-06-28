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

package androidx.datastore.core.okio

import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.use
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import okio.use

/**
 * OKIO implementation of the Storage interface, providing cross platform IO using the OKIO library.
 */
class OkioStorage<T>(
    private val fileSystem: FileSystem,
    private val serializer: OkioSerializer<T>,
    private val producePath: () -> Path
) : Storage<T> {
    private val canonicalPath by lazy {
        val path = producePath()
        check(path.isAbsolute) {
            "OkioStorage requires absolute paths, but did not get an absolute path from " +
                "producePath = $producePath, instead got $path"
        }
        path
    }

    override fun createConnection(): StorageConnection<T> {
        canonicalPath.toString().let { path ->
            synchronized(activeFilesLock) {
                check(!activeFiles.contains(path)) {
                    "There are multiple DataStores active for the same file: $path. You should " +
                        "either maintain your DataStore as a singleton or confirm that there is " +
                        "no two DataStore's active on the same file (by confirming that the scope" +
                        " is cancelled)."
                }
                activeFiles.add(path)
            }
        }
        return OkioStorageConnection(fileSystem, canonicalPath, serializer) {
            synchronized(activeFilesLock) {
                activeFiles.remove(canonicalPath.toString())
            }
        }
    }

    internal companion object {
        internal val activeFiles = mutableSetOf<String>()

        class Sync : SynchronizedObject()
        internal val activeFilesLock = Sync()
    }
}

internal class OkioStorageConnection<T>(
    private val fileSystem: FileSystem,
    private val path: Path,
    private val serializer: OkioSerializer<T>,
    private val onClose: () -> Unit
) : StorageConnection<T> {

    private val closed = AtomicBoolean(false)
    // TODO:(b/233402915) support multiple readers
    private val transactionMutex = Mutex()

    override suspend fun <R> readTransaction(
        block: suspend ReadScope<T>.() -> R
    ): R {
        checkNotClosed()

        val lock = transactionMutex.tryLock()
        try {
            OkioReadScope(fileSystem, path, serializer).use {
                return block(it)
            }
        } finally {
            if (lock) {
                transactionMutex.unlock()
            }
        }
    }

    override suspend fun writeTransaction(block: suspend WriteScope<T>.() -> Unit) {
        checkNotClosed()
        val parentDir = path.parent ?: error("must have a parent path")
        fileSystem.createDirectories(
            dir = parentDir,
            mustCreate = false
        )
        transactionMutex.withLock {
            val scratchPath = parentDir / "${path.name}.tmp"
            try {
                fileSystem.delete(
                    path = scratchPath,
                    mustExist = false
                )
                OkioWriteScope(fileSystem, scratchPath, serializer).use {
                    block(it)
                }
                if (fileSystem.exists(scratchPath)) {
                    fileSystem.atomicMove(scratchPath, path)
                }
            } catch (ex: IOException) {
                if (fileSystem.exists(scratchPath)) {
                    try {
                        fileSystem.delete(scratchPath)
                    } catch (e: IOException) {
                        // swallow failure to delete
                    }
                }
                throw ex
            }
        }
    }

    private fun checkNotClosed() {
        check(!closed.get()) { "StorageConnection has already been disposed." }
    }

    override fun close() {
        closed.set(true)
        onClose()
    }
}

internal open class OkioReadScope<T>(
    protected val fileSystem: FileSystem,
    protected val path: Path,
    protected val serializer: OkioSerializer<T>
) : ReadScope<T> {

    private var closed by atomic(false)

    override suspend fun readData(): T {
        checkClose()

        return try {
            fileSystem.read(
                file = path
            ) {
                serializer.readFrom(this)
            }
        } catch (ex: FileNotFoundException) {
            if (fileSystem.exists(path)) {
                throw ex
            }
            serializer.defaultValue
        }
    }

    override fun close() {
        closed = true
    }
    protected fun checkClose() {
        check(!closed) { "This scope has already been closed." }
    }
}

internal class OkioWriteScope<T>(
    fileSystem: FileSystem,
    path: Path,
    serializer: OkioSerializer<T>
) :
    OkioReadScope<T>(fileSystem, path, serializer), WriteScope<T> {

    override suspend fun writeData(value: T) {
        checkClose()
        val fileHandle = fileSystem.openReadWrite(path)
        fileHandle.use { handle ->
            handle.sink().buffer().use { sink ->
                serializer.writeTo(value, sink)
                handle.flush()
            }
        }
    }
}