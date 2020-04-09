/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.InternalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
@RunWith(JUnit4::class)
class SingleProcessDataStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var store: DataStore<Byte>
    private lateinit var serializer: TestingSerializer
    private lateinit var testFile: File
    private lateinit var testScope: TestCoroutineScope

    @Before
    fun setUp() {
        serializer = TestingSerializer()
        testFile = tmp.newFile()
        testScope = TestCoroutineScope(TestCoroutineDispatcher() + Job())
        store =
            SingleProcessDataStore<Byte>({ testFile }, serializer, scope = testScope)
    }

    @After
    fun cleanUp() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testReadNewMessage() = runBlockingTest {
        assertThat(store.dataFlow.first()).isEqualTo(0)
    }

    @Test
    fun testReadWithNewInstance() = runBlockingTest {
        // TODO(b/151635324): Change this to call updateData() once implemented.
        FileOutputStream(testFile).use { stream ->
            stream.write(1)
            stream.fd.sync()
        }

        val newStore = newDataStore(testFile)
        assertThat(newStore.dataFlow.first()).isEqualTo(1)
    }

    @Test
    fun testReadUnreadableFile() = runBlockingTest {
        testFile.setReadable(false)
        val result = runCatching {
            store.dataFlow.first()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")
    }

    @Test
    fun testReadAfterTransientBadRead() = runBlockingTest {
        testFile.setReadable(false)
        val result = runCatching {
            store.dataFlow.first()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")

        testFile.setReadable(true)
        assertThat(store.dataFlow.first()).isEqualTo(0)
    }

    @Test
    fun testScopeCancelledWithActiveFlow() = runBlockingTest {
        val collection = async {
            store.dataFlow.take(2).collect {
                // Do nothing, this will wait on another element which will never arrive
            }
        }

        testScope.cancel()

        assertThat(collection.isCompleted).isTrue()
        assertThat(collection.isActive).isFalse()
    }

    private fun newDataStore(
        file: File = tmp.newFile()
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            { file },
            serializer = serializer,
            scope = testScope
        )
    }

    private class TestingSerializer(
        override val defaultValue: Byte = 0,
        @Volatile var failingRead: Boolean = false
    ) : DataStore.Serializer<Byte> {
        override fun readFrom(input: InputStream): Byte {
            if (failingRead) {
                throw IOException("I was asked to fail on reads")
            }
            val read = input.read()
            if (read == -1) {
                return defaultValue
            }
            return read.toByte()
        }
    }
}
