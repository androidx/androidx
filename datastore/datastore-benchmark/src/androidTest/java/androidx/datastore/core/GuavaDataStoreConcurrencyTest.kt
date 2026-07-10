/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.datastore.guava.GuavaDataStore
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuavaDataStoreConcurrencyTest {
    @get:Rule val tempFolder = TemporaryFolder()
    private val executorService = Executors.newFixedThreadPool(4)

    @After
    fun tearDown() {
        executorService.shutdown()
    }

    @Test
    fun testConcurrentUpdates_preservesOrder() {
        val file = tempFolder.newFile()
        val store =
            GuavaDataStore.Builder(serializer = StringSerializer, produceFile = { file })
                .setExecutor(executorService)
                .build()

        val numUpdates = 100
        val expectedBuilder = StringBuilder()
        val futures =
            List(numUpdates) { i ->
                expectedBuilder.append("$i,")
                store.updateDataAsync { current ->
                    // If strict FIFO is preserved the string should read "0,1,2,3,..."
                    "$current$i,"
                }
            }
        Futures.allAsList(futures).get()
        val expected = expectedBuilder.toString()
        val actual = store.getDataAsync().get()

        assertThat(actual).isEqualTo(expected)
    }

    /** A simple String serializer for the test. */
    private object StringSerializer : Serializer<String> {
        override val defaultValue: String = ""

        override suspend fun readFrom(input: InputStream): String {
            return try {
                input.readBytes().toString(Charset.defaultCharset())
            } catch (e: Exception) {
                defaultValue
            }
        }

        override suspend fun writeTo(t: String, output: OutputStream) {
            output.write(t.toByteArray(Charset.defaultCharset()))
        }
    }
}
