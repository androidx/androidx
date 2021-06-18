package androidx.datastore.core

import androidx.annotation.GuardedBy
import kotlinx.io.IOException
import kotlinx.io.asInput
import kotlinx.io.asOutput
import java.io.*

internal class AndroidStorage<T>(produceFile: () -> File, val codec: Codec<T>): Storage<T> {

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
                return codec.readFrom(stream.asInput())
            }
        } catch (ex: FileNotFoundException) {
            if (file.exists()) {
                throw ex
            }
            return codec.defaultValue
        }
    }

    override suspend fun writeData(newData: T) {
        file.createParentDirectories()

        val scratchFile = File(file.absolutePath + SCRATCH_SUFFIX)
        try {
            FileOutputStream(scratchFile).use { stream ->
                // serializer.writeTo(newData, UncloseableOutputStream(stream))
                val output = UncloseableOutputStream(stream).asOutput()
                codec.writeTo(newData, output)

                // TODO(lukhnos): THIS MUST BE DONE HERE!!!
                output.flush()

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

    override fun complete() {
        synchronized(activeFilesLock) {
            activeFiles.remove(file.absolutePath)
        }
    }
}