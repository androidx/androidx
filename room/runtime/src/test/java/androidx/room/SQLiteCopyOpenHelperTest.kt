/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room

import android.content.Context
import android.content.res.AssetManager
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@RunWith(JUnit4::class)
class SQLiteCopyOpenHelperTest {

    companion object {
        const val DB_NAME = "test.db"
        const val DB_VERSION = 0
    }

    @get:Rule
    val tempDirectory = TemporaryFolder()

    val context: Context = mock(Context::class.java)
    val assetManager: AssetManager = mock(AssetManager::class.java)
    val delegate: SupportSQLiteOpenHelper = mock(SQLiteCopyOpenHelper::class.java)
    val configuration: DatabaseConfiguration = mock(DatabaseConfiguration::class.java)

    @Test
    fun singleCopy() {
        val copyFile = tempDirectory.newFile("toCopy.db")
        writeDatabaseVersion(copyFile)
        setupMocks(tempDirectory.root, copyFile)

        val openHelper = createOpenHelper(copyFile)
        openHelper.writableDatabase
        openHelper.writableDatabase

        verify(delegate).databaseName
        verify(context).getDatabasePath(DB_NAME)
        verify(assetManager).open("toCopy.db")
        verifyNoMoreInteractions(configuration)

        assertEquals(1, getAndIncrementAccessCount(copyFile))
    }

    @Test
    fun singleCopy_multiInstance() {
        val copyFile = tempDirectory.newFile("toCopy.db")
        writeDatabaseVersion(copyFile)
        setupMocks(tempDirectory.root, copyFile)

        createOpenHelper(copyFile).writableDatabase
        createOpenHelper(copyFile).writableDatabase

        verify(delegate, times(2)).databaseName
        verify(context, times(2)).getDatabasePath(DB_NAME)
        verify(assetManager).open("toCopy.db")
        verifyNoMoreInteractions(configuration)

        assertEquals(1, getAndIncrementAccessCount(copyFile))
    }

    @Test
    fun singleCopy_multiThread() {
        val copyFile = tempDirectory.newFile("toCopy.db")
        writeDatabaseVersion(copyFile)
        setupMocks(tempDirectory.root, copyFile)

        val t1 = thread(name = "DB Thread A") {
            createOpenHelper(copyFile).writableDatabase
        }
        val t2 = thread(name = "DB Thread B") {
            createOpenHelper(copyFile).writableDatabase
        }

        t1.join()
        t2.join()

        verify(delegate, times(2)).databaseName
        verify(context, times(2)).getDatabasePath(DB_NAME)
        verify(assetManager).open("toCopy.db")
        verifyNoMoreInteractions(configuration)

        assertEquals(1, getAndIncrementAccessCount(copyFile))
    }

    @Test
    fun singleCopy_multiProcess() {
        val copyFile = tempDirectory.newFile("toCopy.db")
        writeDatabaseVersion(copyFile)

        val processes = List(2) { spawnCopyProcess(copyFile) }
        try {
            processes.forEach {
                val exited = it.waitFor(5, TimeUnit.SECONDS)
                if (!exited) {
                    throw TimeoutException("Timed Out waiting for copy process to finish.")
                }
                val exitCode = it.exitValue()
                if (exitCode != 0) {
                    throw IllegalStateException("Copy process exited with non-zero code. " +
                            "Code: $exitCode")
                }
            }
        } finally {
            processes.forEach { it.destroy() }
        }

        assertEquals(1, getAndIncrementAccessCount(copyFile))
    }

    @Test
    fun firstCopyFails_multiThread() {
        val copyFile = tempDirectory.newFile("toCopy.db")
        writeDatabaseVersion(copyFile)
        val openCount = AtomicInteger()
        setupMocks(tempDirectory.root, copyFile) {
            if (openCount.getAndIncrement() == 0) {
                throw IOException("Fake IO Error")
            }
        }

        val exceptions = mutableListOf<Exception>()
        val t1 = thread(name = "DB Thread A") {
            try {
                createOpenHelper(copyFile).writableDatabase
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }
        val t2 = thread(name = "DB Thread B") {
            try {
                createOpenHelper(copyFile).writableDatabase
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        t1.join()
        t2.join()

        verify(delegate, times(2)).databaseName
        verify(context, times(2)).getDatabasePath(DB_NAME)
        verify(assetManager, times(2)).open("toCopy.db")
        verifyNoMoreInteractions(configuration)

        assertEquals(2, getAndIncrementAccessCount(copyFile))
        assertEquals(1, exceptions.size)
    }

    internal fun setupMocks(tmpDir: File, copyFromFile: File, onAssetOpen: () -> Unit = {}) {
        `when`(delegate.databaseName).thenReturn(DB_NAME)
        `when`(context.getDatabasePath(DB_NAME)).thenReturn(File(tmpDir, DB_NAME))
        configuration::class.java.getField("multiInstanceInvalidation").let { field ->
            field.isAccessible = true
            field.setBoolean(configuration, true)
        }
        `when`(context.filesDir).thenReturn(tmpDir)
        `when`(context.assets).thenReturn(assetManager)
        `when`(assetManager.open(copyFromFile.name)).thenAnswer {
            getAndIncrementAccessCount(copyFromFile) // increment file access counter
            onAssetOpen()
            return@thenAnswer object : InputStream() {
                val delegate by lazy { FileInputStream(copyFromFile) }
                override fun read(): Int {
                    Thread.sleep(10) // simulate slow reading, as if this was a big file
                    return delegate.read()
                }
            }
        }
    }

    internal fun createOpenHelper(copyFromAssetFile: File) =
        SQLiteCopyOpenHelper(
            context,
            copyFromAssetFile.name,
            null,
            DB_VERSION,
            delegate
        ).apply { setDatabaseConfiguration(configuration) }

    // Writes sqlite user database version in a file, located at offset 60.
    private fun writeDatabaseVersion(file: File) {
        val buffer = ByteBuffer.allocate(4)
        RandomAccessFile(file, "rw").channel.use {
            buffer.putInt(DB_VERSION)
            buffer.rewind()
            it.write(buffer, 60)
        }
    }

    // Get and increment a made-up file access counter located at the first 4 bytes of a file.
    private fun getAndIncrementAccessCount(file: File): Int {
        val buffer = ByteBuffer.allocate(4)
        return RandomAccessFile(file, "rw").channel.use {
            it.lock()
            it.read(buffer, 0)
            buffer.rewind()
            val count = buffer.int
            buffer.rewind()
            buffer.putInt(count + 1)
            buffer.rewind()
            it.write(buffer, 0)
            return@use count
        }
    }

    // Spawns a new Java process to perform a copy using the open helper.
    private fun spawnCopyProcess(copyFromFile: File): Process {
        val javaBin = System.getProperty("java.home") +
                File.separator + "bin" + File.separator + "java"
        val classpath = System.getProperty("java.class.path")
        val mainClass = RoomCopyTestProcess::class.java.canonicalName
        val tmpDirPath = tempDirectory.root.absolutePath
        val copyFromFilePath = copyFromFile.absolutePath
        return ProcessBuilder(javaBin, "-cp", classpath, mainClass, tmpDirPath, copyFromFilePath)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectErrorStream(true)
            .start()
    }
}

// Main class that will run in a separate process from the test. Used to verify multi-process lock.
class RoomCopyTestProcess {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val tmpDir = File(args[0])
            val copyFromFile = File(args[1])
            val openHelper = SQLiteCopyOpenHelperTest().apply { setupMocks(tmpDir, copyFromFile) }
                .createOpenHelper(copyFromFile)
            openHelper.writableDatabase
        }
    }
}