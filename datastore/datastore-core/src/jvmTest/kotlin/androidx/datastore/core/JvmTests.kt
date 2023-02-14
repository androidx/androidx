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

import androidx.datastore.FileTestIO
import androidx.datastore.JavaIOFile
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class, FlowPreview::class)
@InternalCoroutinesApi
class DataMigrationInitializerTestFileTest :
    DataMigrationInitializerTest<JavaIOFile, IOException>(FileTestIO())

@OptIn(ExperimentalCoroutinesApi::class)
@InternalCoroutinesApi
class SingleProcessDatastoreJavaTest : SingleProcessDataStoreTest<JavaIOFile>(FileTestIO()) {

    @Test
    fun testMutatingDataStoreFails() = doTest {
        val dataStore = DataStoreFactory.create(
            serializer = ByteWrapperSerializer(),
            scope = dataStoreScope
        ) { testFile.file }

        assertThrows<IllegalStateException> {
            dataStore.updateData { input: ByteWrapper ->
                // mutating our wrapper causes us to fail
                input.byte = 123.toByte()
                input
            }
        }
    }

    @Test
    fun testClosingOutputStreamDoesntCloseUnderlyingStream() = doTest {
        val delegate = TestingSerializer()
        val serializer = object : Serializer<Byte> by delegate {
            override suspend fun writeTo(t: Byte, output: OutputStream) {
                delegate.writeTo(t, output)
                output.close() // This will be a no-op so the fd.sync() call will succeed.
            }
        }

        val dataStore = SingleProcessDataStore(
            FileStorage(serializer) { testFile.file }
        )

        // Shouldn't throw:
        dataStore.data.first()

        // Shouldn't throw:
        dataStore.updateData { it.inc() }
    }

    @Test
    fun testReadUnreadableFile() = doTest {
        testFile.file.setReadable(false)
        val result = runCatching {
            store.data.first()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf<IOException>()
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")
    }

    @Test
    fun testReadAfterTransientBadRead() = doTest {
        testFile.file.setReadable(false)

        assertThrows<IOException> { store.data.first() }.hasMessageThat()
            .contains("Permission denied")

        testFile.file.setReadable(true)
        assertThat(store.data.first()).isEqualTo(0)
    }

    // Mutable wrapper around a byte
    internal class ByteWrapperSerializer() : Serializer<ByteWrapper> {
        private val delegate = TestingSerializer()

        override val defaultValue = ByteWrapper(delegate.defaultValue)

        override suspend fun readFrom(input: InputStream): ByteWrapper {
            return ByteWrapper(delegate.readFrom(input))
        }

        override suspend fun writeTo(t: ByteWrapper, output: OutputStream) {
            delegate.writeTo(t.byte, output)
        }
    }

    // Mutable wrapper around a byte
    data class ByteWrapper(var byte: Byte)
}