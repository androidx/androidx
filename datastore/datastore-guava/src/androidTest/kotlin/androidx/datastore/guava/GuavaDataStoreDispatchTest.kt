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

package androidx.datastore.guava

import android.os.Looper
import androidx.datastore.core.Serializer
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuavaDataStoreDispatchTest {

    @get:Rule val tmpFolder = TemporaryFolder()

    private lateinit var testFile: File
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    @Before
    fun setUp() {
        testFile = tmpFolder.newFile()
    }

    @After
    fun tearDown() {
        backgroundExecutor.shutdown()
    }

    @Test
    fun datastoreOperations_dispatchToBackgroundThread() {
        // This lambda will throw if its blocking code is ever called on the main thread.
        val produceFileThatThrowsOnMainThread: () -> File = {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw IllegalStateException(
                    "Blocking file production cannot run on the main thread!"
                )
            }
            testFile
        }
        val dataStore =
            GuavaDataStore.Builder(
                    serializer = StringSerializer,
                    produceFile = produceFileThatThrowsOnMainThread,
                )
                .setExecutor(backgroundExecutor)
                .build()

        runBlocking(Dispatchers.Main.immediate) {
            // This call should not throw, as `produceFile` will be invoked on the background
            // thread.
            val initialData = dataStore.getDataAsync().get()
            assertThat(initialData).isEqualTo("default")

            // This call should also not throw
            val updatedData =
                dataStore
                    .updateDataAsync {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            throw IllegalStateException(
                                "Update transform cannot run on the main thread!"
                            )
                        }
                        "new value"
                    }
                    .get()
            assertThat(updatedData).isEqualTo("new value")

            // Assert the updated data was persisted correctly
            val finalData = dataStore.getDataAsync().get()
            assertThat(finalData).isEqualTo("new value")
        }
    }

    /** A simple String serializer for the test. */
    private object StringSerializer : Serializer<String> {
        override val defaultValue = "default"

        override suspend fun readFrom(input: InputStream): String {
            val bytes = input.readBytes()
            // If the file is empty (no bytes read), return the default value.
            if (bytes.isEmpty()) {
                return defaultValue
            }
            return bytes.toString(Charsets.UTF_8)
        }

        override suspend fun writeTo(t: String, output: OutputStream) =
            output.write(t.toByteArray())
    }
}
