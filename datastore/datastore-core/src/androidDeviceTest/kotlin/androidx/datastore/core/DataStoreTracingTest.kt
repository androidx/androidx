/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import androidx.kruth.assertThat
import androidx.test.core.app.ApplicationProvider
import androidx.tracing.TraceDriver
import androidx.tracing.Tracer
import androidx.tracing.wire.TraceSink
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.appendingSink
import okio.buffer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout

class DataStoreTracingTest {
    @get:Rule val timeout = Timeout(10, TimeUnit.SECONDS)

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var testDataStoreFile: File
    private lateinit var traceFile: File
    private lateinit var dataStoreScope: TestScope
    private lateinit var driver: TraceDriver
    private lateinit var tracer: Tracer

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        testDataStoreFile = tmp.newFile()
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
        val context: Context = ApplicationProvider.getApplicationContext()
        traceFile = File(context.getExternalFilesDir(null), "datastore-test-trace.txt")
        driver =
            androidx.tracing.wire.TraceDriver(
                context = context,
                sink =
                    TraceSink(
                        sequenceId = 1,
                        bufferedSink = traceFile.appendingSink().buffer(),
                        coroutineContext = dataStoreScope.coroutineContext,
                    ),
                isEnabled = true,
            )
        tracer = driver.tracer
    }

    @Test
    fun testWriteTrace() = runTest {
        val store =
            DataStoreFactory.createWithTracing(
                storage =
                    FileStorage(
                        serializer = TestingSerializer(),
                        produceFile = { testDataStoreFile },
                    ),
                tracer = tracer,
                scope = dataStoreScope,
            )

        // Collect initial trace file size so we can confirm it was written to during this test.
        val initialTraceSize = traceFile.length()
        val expectedByte = 123.toByte()

        assertThat(store.updateData { expectedByte }).isEqualTo(expectedByte)
        assertThat(store.data.first()).isEqualTo(expectedByte)

        // Close the tracing driver so a trace is outputted.
        driver.close()

        // We manually checked the content of the perfetto trace, but can also confirm a
        // trace was added to the file here.
        assertThat(traceFile.length()).isGreaterThan(initialTraceSize)
    }
}
