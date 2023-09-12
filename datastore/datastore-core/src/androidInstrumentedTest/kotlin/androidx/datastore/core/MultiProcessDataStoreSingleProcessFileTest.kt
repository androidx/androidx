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

import androidx.datastore.FileTestIO
import androidx.datastore.JavaIOFile
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@InternalCoroutinesApi
class MultiProcessDataStoreSingleProcessFileTest :
    MultiProcessDataStoreSingleProcessTest<JavaIOFile>(FileTestIO()) {
    override fun getJavaFile(file: JavaIOFile): File {
        return file.file
    }

    @Test
    fun testReadUnreadableFile() = runTest {
        // ensure the file exists by writing into it
        testFile.file.writeText("")
        testFile.file.setReadable(false)
        val result = runCatching {
            store.data.first()
        }

        Truth.assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        Truth.assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")
    }

    @Test
    fun testReadAfterTransientBadRead() = runTest {
        // ensure the file exists by writing into it
        testFile.file.writeText("")
        testFile.file.setReadable(false)

        assertThrows<IOException> { store.data.first() }.hasMessageThat()
            .contains("Permission denied")

        testFile.file.setReadable(true)
        Truth.assertThat(store.data.first()).isEqualTo(0)
    }

    @Test
    fun testMutatingDataStoreFails() = runTest {

        val dataStore = DataStoreImpl(
            storage = FileStorage(ByteWrapper.ByteWrapperSerializer(), {
                MultiProcessCoordinator(dataStoreScope.coroutineContext, it)
            }) { testFile.file },
            scope = dataStoreScope,
        )

        assertThrows<IllegalStateException> {
            dataStore.updateData { input: ByteWrapper ->
                // mutating our wrapper causes us to fail
                input.byte = 123.toByte()
                input
            }
        }
    }

    @Test
    fun stressTest() = runBlocking {
        val stressTestFile = getJavaFile(testIO.newTempFile(tempFolder))
        val testJob = Job()
        val testScope = CoroutineScope(
            Dispatchers.IO + testJob
        )
        val stressTestStore = DataStoreImpl<Int>(
            storage = FileStorage(
                object : Serializer<Int> {
                    override val defaultValue: Int
                        get() = 0

                    override suspend fun readFrom(input: InputStream): Int {
                        return input.reader(Charsets.UTF_8).use {
                            it.readText().toIntOrNull() ?: defaultValue
                        }
                    }

                    override suspend fun writeTo(t: Int, output: OutputStream) {
                        output.writer(Charsets.UTF_8).use {
                            it.write(t.toString())
                            it.flush()
                        }
                    }
                },
                coordinatorProducer = {
                    MultiProcessCoordinator(testScope.coroutineContext, it)
                },
                produceFile = { stressTestFile }
            ),
            scope = testScope,
            initTasksList = emptyList()
        )
        val limit = 1_000
        stressTestStore.updateData { 0 }
        val reader = async(Dispatchers.IO + testJob) {
            stressTestStore.data.scan(0) { prev, next ->
                check(next >= prev) {
                    "check failed: $prev / $next"
                }
                next
            }.take(limit - 200).collect() // we can drop some intermediate values, it is fine
        }
        val writer = async {
            repeat(limit) {
                stressTestStore.updateData {
                    it + 1
                }
            }
        }
        listOf(reader, writer).awaitAll()
        testJob.cancelAndJoin()
    }
}
