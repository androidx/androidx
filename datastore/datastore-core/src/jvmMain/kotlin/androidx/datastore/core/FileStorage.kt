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

internal class FileStorage<T>(
    val produceFile: () -> File,
    private val serializer: Serializer<T>
) : Storage<T> {
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
    override suspend fun readData(): T {
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

    override suspend fun writeData(newData: T) {
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

    override fun onComplete() {
        synchronized(activeFilesLock) {
            activeFiles.remove(file.absolutePath)
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