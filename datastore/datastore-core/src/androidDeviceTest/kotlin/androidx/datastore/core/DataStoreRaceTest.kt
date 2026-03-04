/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.kruth.assertThat
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreRaceTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var testFile: File

    @Before
    fun setUp() {
        testFile = tmp.newFile()
    }

    @Test
    fun testCacheRaceCondition() = runTest {
        val serializer = TestingSerializer()

        val store = createDataStore(serializer = serializer, scope = backgroundScope)

        val firstValue = 1.toByte()
        val secondValue = 2.toByte()
        val expected = listOf(firstValue, secondValue)

        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(firstValue)

        // Hang on the next write (call to updateData()) to simulate race condition and wait for
        // the write to begin
        serializer.hangSubsequentWrites()
        val writerJob = launch { store.updateData { 2 } }
        serializer.awaitWriteStarted()

        val collected = mutableListOf<Byte>()
        val readerJob = launch { store.data.take(expected.size).toList(collected) }

        // Let write proceed and await processes to finish
        serializer.resumeWrite()
        writerJob.join()
        withTimeout(5.seconds) {
            // Without the fix, we pass the first assert of the value [1], and get stuck waiting and
            // timeout because value [2] is dropped due to the version is the old version, which is
            // not newer than the current version (incorrectly).
            readerJob.join()
        }

        // With the fix it should see [1, 2].
        assertThat(collected).isEqualTo(expected)
    }

    private fun <T> createDataStore(
        serializer: Serializer<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
        migrations: List<DataMigration<T>> = listOf(),
        scope: CoroutineScope,
        produceFile: () -> File = { testFile },
    ): DataStore<T> {
        return DataStoreFactory.create(
            serializer,
            corruptionHandler,
            migrations,
            scope,
            produceFile,
        )
    }

    private class TestingSerializer : Serializer<Byte> {
        override val defaultValue: Byte = 0

        private var writeRequest: CompletableDeferred<Unit>? = null
        private var writeContinuation: CompletableDeferred<Unit>? = null

        fun hangSubsequentWrites() {
            writeRequest = CompletableDeferred()
            writeContinuation = CompletableDeferred()
        }

        suspend fun awaitWriteStarted() {
            writeRequest!!.await()
        }

        fun resumeWrite() {
            writeContinuation!!.complete(Unit)
        }

        override suspend fun readFrom(input: InputStream): Byte {
            val read = input.read()
            if (read == -1) {
                return 0
            }
            return read.toByte()
        }

        override suspend fun writeTo(t: Byte, output: OutputStream) {
            writeRequest?.complete(Unit)
            writeContinuation?.await()
            output.write(t.toInt())
        }
    }
}
