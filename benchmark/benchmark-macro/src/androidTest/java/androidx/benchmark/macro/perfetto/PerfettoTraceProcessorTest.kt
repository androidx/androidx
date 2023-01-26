/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.Shell
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PerfettoTraceProcessorTest {

    @Test
    fun shellPath() {
        assumeTrue(isAbiSupported())
        val shellPath = PerfettoTraceProcessor.shellPath
        val out = Shell.executeScriptCaptureStdout("$shellPath --version")
        assertTrue(
            "expect to get Perfetto version string, saw: $out",
            out.contains("Perfetto v")
        )
    }

    @Test
    fun getJsonMetrics_tracePathWithSpaces() {
        assumeTrue(isAbiSupported())
        assertFailsWith<IllegalArgumentException> {
            PerfettoTraceProcessor.runServer("/a b") { }
        }
    }

    @Test
    fun getJsonMetrics_metricWithSpaces() {
        assumeTrue(isAbiSupported())
        assertFailsWith<IllegalArgumentException> {
            PerfettoTraceProcessor.runServer(
                createTempFileFromAsset(
                    "api31_startup_cold",
                    ".perfetto-trace"
                ).absolutePath
            ) {
                getTraceMetrics("a b")
            }
        }
    }

    @Test
    fun validateAbiNotSupportedBehavior() {
        assumeFalse(isAbiSupported())
        assertFailsWith<IllegalStateException> {
            PerfettoTraceProcessor.shellPath
        }

        assertFailsWith<IllegalStateException> {
            PerfettoTraceProcessor.runServer(
                createTempFileFromAsset(
                    "api31_startup_cold",
                    ".perfetto-trace"
                ).absolutePath
            ) {
                getTraceMetrics("ignored_metric")
            }
        }
    }

    @Test
    fun querySlices() {
        // check known slice content is queryable
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runServer(traceFile.absolutePath) {

            assertEquals(
                expected = listOf(
                    Slice(
                        name = "activityStart",
                        ts = 186975009436431,
                        dur = 29580628
                    )
                ),
                actual = querySlices("activityStart")
            )
            assertEquals(
                expected = listOf(
                    Slice(
                        name = "activityStart",
                        ts = 186975009436431,
                        dur = 29580628
                    ),
                    Slice(
                        name = "activityResume",
                        ts = 186975039764298,
                        dur = 6570418
                    )
                ),
                actual = querySlices("activityStart", "activityResume")
                    .sortedBy { it.ts }
            )
        }
    }

    @Test
    fun validateTraceProcessorBinariesExist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffixes = listOf("aarch64")
        val entries = suffixes.map { "trace_processor_shell_$it" }.toSet()
        val assets = context.assets.list("") ?: emptyArray()
        assertTrue(
            "Expected to find $entries",
            assets.toSet().containsAll(entries)
        )
    }

    @Test
    fun runServerShouldHandleStartAndStopServer() {
        assumeTrue(isAbiSupported())

        // This method will return true if the server status endpoint returns 200 (that is also
        // the only status code being returned).
        fun isRunning(): Boolean = try {
            val url = URL("http://localhost:${PerfettoTraceProcessor.PORT}/")
            with(url.openConnection() as HttpURLConnection) {
                return@with responseCode == 200
            }
        } catch (e: ConnectException) {
            false
        }

        // Check server is not running
        assertTrue(!isRunning())

        PerfettoTraceProcessor.runServer {

            // Check server is running
            assertTrue(isRunning())
        }

        // Check server is not running
        assertTrue(!isRunning())
    }
}