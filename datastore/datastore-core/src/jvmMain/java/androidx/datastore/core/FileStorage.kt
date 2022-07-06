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

import androidx.annotation.GuardedBy
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FileStorage<T>(
    private val serializer: Serializer<T>,
    private val produceFile: () -> File
) : Storage<T> {

    override fun createConnection(): StorageConnection<T> {
        val file = produceFile()

        synchronized(activeFilesLock) {
            val path = file.absolutePath
            check(!activeFiles.contains(path)) {
                "There are multiple DataStores active for the same file: $path. You should " +
                    "either maintain your DataStore as a singleton or confirm that there is " +
                    "no two DataStore's active on the same file (by confirming that the scope" +
                    " is cancelled)."
            }
            activeFiles.add(path)
        }

        return FileStorageConnection(file, serializer) {
            synchronized(activeFilesLock) {
                activeFiles.remove(file.absolutePath)
            }
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

internal class FileStorageConnection<T>(
    private val file: File,
    private val serializer: Serializer<T>,
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
            return FileReadScope(file, serializer).use {
                block(it)
            }
        } finally {
            if (lock) {
                transactionMutex.unlock()
            }
        }
    }

    override suspend fun writeTransaction(block: suspend WriteScope<T>.() -> Unit) {
        checkNotClosed()
        file.createParentDirectories()

        transactionMutex.withLock {
            val scratchFile = File(file.absolutePath + ".tmp")
            try {
                FileWriteScope(scratchFile, serializer).use {
                    block(it)
                }
                if (scratchFile.exists() && !scratchFile.renameTo(file)) {
                    throw IOException(
                        "Unable to rename $scratchFile. " +
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
    }

    public override fun close() {
        closed.set(true)
        onClose()
    }

    private fun checkNotClosed() {
        check(!closed.get()) { "StorageConnection has already been disposed." }
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
}

internal open class FileReadScope<T>(
    protected val file: File,
    protected val serializer: Serializer<T>
) : ReadScope<T> {

    private val closed = AtomicBoolean(false)

    override suspend fun readData(): T {
        checkNotClosed()
        return try {
            FileInputStream(file).use { stream ->
                serializer.readFrom(stream)
            }
        } catch (ex: FileNotFoundException) {
            if (file.exists()) {
                throw ex
            }
            serializer.defaultValue
        }
    }

    override fun close() {
        closed.set(true)
    }
    protected fun checkNotClosed() {
        check(!closed.get()) { "This scope has already been closed." }
    }
}

internal class FileWriteScope<T>(file: File, serializer: Serializer<T>) :
    FileReadScope<T>(file, serializer), WriteScope<T> {

    override suspend fun writeData(value: T) {
        checkNotClosed()
        val fos = FileOutputStream(file)
        fos.use { stream ->
            serializer.writeTo(value, UncloseableOutputStream(stream))
            stream.fd.sync()
            // TODO(b/151635324): fsync the directory, otherwise a badly timed crash could
            //  result in reverting to a previous state.
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
}