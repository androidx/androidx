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

package androidx.benchmark.perfetto

import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import perfetto.protos.TraceMetrics

@MediumTest
@RunWith(AndroidJUnit4::class)
class PerfettoTraceProcessorTest {
    @Test
    fun shellPath() {
        assumeTrue(isAbiSupported())
        val shellPath = PerfettoTraceProcessor.shellPath
        val out = Shell.executeScriptCaptureStdout("$shellPath --version")
        assertTrue("expect to get Perfetto version string, saw: $out", out.contains("Perfetto v"))
    }

    @Test
    fun getJsonMetrics_tracePathWithSpaces() {
        assumeTrue(isAbiSupported())
        assertFailsWith<IllegalArgumentException> {
            PerfettoTraceProcessor.runSingleSessionServer("/a b") {}
        }
    }

    @Test
    fun getJsonMetrics_metricWithSpaces() {
        assumeTrue(isAbiSupported())
        assertFailsWith<IllegalArgumentException> {
            PerfettoTraceProcessor.runSingleSessionServer(
                createTempFileFromAsset("api31_startup_cold", ".perfetto-trace").absolutePath
            ) {
                getTraceMetrics("a b")
            }
        }
    }

    @Test
    fun validateAbiNotSupportedBehavior() {
        assumeFalse(isAbiSupported())
        assertFailsWith<IllegalStateException> { PerfettoTraceProcessor.shellPath }

        assertFailsWith<IllegalStateException> {
            PerfettoTraceProcessor.runSingleSessionServer(
                createTempFileFromAsset("api31_startup_cold", ".perfetto-trace").absolutePath
            ) {
                getTraceMetrics("ignored_metric")
            }
        }
    }

    enum class QuerySlicesMode(val target: String?) {
        ValidPackage("androidx.benchmark.integration.macrobenchmark.target"),
        Unspecified(null),
        InvalidPackage("not.a.real.package")
    }

    @Test fun querySlices_validPackage() = validateQuerySlices(QuerySlicesMode.ValidPackage)

    @Test fun querySlices_invalidPackage() = validateQuerySlices(QuerySlicesMode.InvalidPackage)

    @Test fun querySlices_unspecified() = validateQuerySlices(QuerySlicesMode.Unspecified)

    private fun validateQuerySlices(mode: QuerySlicesMode) {
        // check known slice content is queryable
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            assertEquals(
                expected =
                    when (mode) {
                        QuerySlicesMode.InvalidPackage -> emptyList()
                        else ->
                            listOf(
                                Slice(name = "activityStart", ts = 186975009436431, dur = 29580628)
                            )
                    },
                actual = querySlices("activityStart", packageName = mode.target)
            )
            assertEquals(
                expected =
                    when (mode) {
                        QuerySlicesMode.InvalidPackage -> emptyList()
                        else ->
                            listOf(
                                Slice(name = "activityStart", ts = 186975009436431, dur = 29580628),
                                Slice(name = "activityResume", ts = 186975039764298, dur = 6570418)
                            )
                    },
                actual =
                    querySlices("activityStart", "activityResume", packageName = mode.target)
                        .sortedBy { it.ts }
            )
            assertEquals(
                expected =
                    when (mode) {
                        QuerySlicesMode.ValidPackage -> 7
                        QuerySlicesMode.Unspecified -> 127
                        QuerySlicesMode.InvalidPackage -> 0
                    },
                actual = querySlices("Lock contention %", packageName = mode.target).size
            )
        }
    }

    @Test
    fun query_syntaxError() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val error = assertFailsWith<IllegalStateException> { query("SYNTAX ERROR, PLEASE") }
            assertContains(
                charSequence = error.message!!,
                other = "syntax error",
                message = "expected 'syntax error', saw message : '''${error.message}'''"
            )
        }
    }

    @Test
    fun query() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            // raw list of maps
            assertEquals(
                expected =
                    listOf(
                        rowOf(
                            "name" to "activityStart",
                            "ts" to 186975009436431L,
                            "dur" to 29580628L
                        )
                    ),
                actual =
                    query("SELECT name,ts,dur FROM slice WHERE name LIKE \"activityStart\"")
                        .toList(),
            )

            // list of lists
            assertEquals(
                expected = listOf(listOf("activityStart", 186975009436431L, 29580628L)),
                actual =
                    query("SELECT name,ts,dur FROM slice WHERE name LIKE \"activityStart\"")
                        .map { listOf(it.string("name"), it.long("ts"), it.long("dur")) }
                        .toList(),
            )

            // multiple result query
            assertEquals(
                expected =
                    listOf(
                        listOf("activityStart", 186975009436431L, 29580628L),
                        listOf("activityResume", 186975039764298L, 6570418L)
                    ),
                actual =
                    query(
                            "SELECT name,ts,dur FROM slice WHERE" +
                                " name LIKE \"activityStart\" OR" +
                                " name LIKE \"activityResume\""
                        )
                        .map { listOf(it.string("name"), it.long("ts"), it.long("dur")) }
                        .toList(),
            )
        }
    }

    /** Validate parsing of bytes is possible */
    @Test
    fun queryBytes() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val query = "SELECT name,ts,dur FROM slice WHERE name LIKE \"activityStart\""
            val bytes = rawQuery(query)
            val queryResult = perfetto.protos.QueryResult.ADAPTER.decode(bytes)
            assertNull(queryResult.error, "no error expected")
            assertEquals(
                expected =
                    listOf(
                        rowOf(
                            "name" to "activityStart",
                            "ts" to 186975009436431L,
                            "dur" to 29580628L
                        )
                    ),
                actual = QueryResultIterator(queryResult).asSequence().toList(),
            )
        }
    }

    @Test
    fun query_includeModule() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        val startups =
            PerfettoTraceProcessor.runServer {
                loadTrace(PerfettoTrace(traceFile.absolutePath)) {
                    query(
                            """
                    INCLUDE PERFETTO MODULE android.startup.startups;

                    SELECT * FROM android_startups;
                """
                                .trimIndent()
                        )
                        .toList()
                }
            }
        // minimal validation, just verifying query worked
        assertEquals(1, startups.size)
        assertEquals(
            "androidx.benchmark.integration.macrobenchmark.target",
            startups.single().string("package")
        )
    }

    @Test
    fun queryMetricsJson() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val metrics = queryMetricsJson(listOf("android_startup"))
            assertTrue(metrics.contains("\"android_startup\": {"))
            assertTrue(metrics.contains("\"startup_type\": \"cold\","))
        }
    }

    @Test
    fun queryMetricsProtoBinary() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val metrics =
                TraceMetrics.ADAPTER.decode(queryMetricsProtoBinary(listOf("android_startup")))
            val startup = metrics.android_startup!!
            assertEquals(startup.startup.single().startup_type, "cold")
        }
    }

    @Test
    fun queryMetricsProtoText() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val metrics = queryMetricsProtoText(listOf("android_startup"))
            assertTrue(metrics.contains("android_startup {"))
            assertTrue(metrics.contains("startup_type: \"cold\""))
        }
    }

    @Test
    fun validatePerfettoTraceProcessorBinariesExist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffixes = listOf("aarch64")
        val entries = suffixes.map { "trace_processor_shell_$it" }.toSet()
        val assets = context.assets.list("") ?: emptyArray()
        assertTrue("Expected to find $entries", assets.toSet().containsAll(entries))
    }

    @Test
    fun runServerShouldHandleStartAndStopServer() {
        assumeTrue(isAbiSupported())

        // Check server is not running
        assertTrue(!isRunning())

        PerfettoTraceProcessor.runServer {
            // Check server is running
            assertTrue(isRunning())
        }

        // Check server is not running
        assertTrue(!isRunning())
    }

    @Test
    fun runServerWithNegativeTimeoutShouldStartAndStopServer() {
        assumeTrue(isAbiSupported())

        // Check server is not running
        assertTrue(!isRunning())

        PerfettoTraceProcessor.runServer((-1).milliseconds) {
            // Check server is running
            assertTrue(isRunning())
        }

        // Check server is not running
        assertTrue(!isRunning())
    }

    @Test
    fun runServerWithZeroTimeoutShouldStartAndStopServer() {
        assumeTrue(isAbiSupported())

        // Check server is not running
        assertTrue(!isRunning())

        PerfettoTraceProcessor.runServer((0).milliseconds) {
            // Check server is running
            assertTrue(isRunning())
        }

        // Check server is not running
        assertTrue(!isRunning())
    }

    @Test
    fun testParseTracesWithProcessTracks() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val slices = querySlices("launching:%", packageName = null)
            assertEquals(
                expected =
                    listOf(
                        Slice(
                            name =
                                "launching: androidx.benchmark.integration.macrobenchmark.target",
                            ts = 186974946587883,
                            dur = 137401159
                        )
                    ),
                slices
            )
        }
    }

    @LargeTest
    @Test
    fun parseLongTrace() {
        val traceFile =
            File.createTempFile("long_trace", ".trace", Outputs.dirUsableByAppAndShell).apply {
                var length = 0L
                val out = outputStream()
                while (length < 70 * 1024 * 1024) {
                    length +=
                        InstrumentationRegistry.getInstrumentation()
                            .context
                            .assets
                            .open("api31_startup_cold.perfetto-trace")
                            .copyTo(out)
                }
            }
        PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            // This would throw an exception if there is an error in the parsing.
            getTraceMetrics("android_startup")
        }
    }

    /**
     * This method will return true if the server status endpoint returns 200 (that is also the only
     * status code being returned).
     */
    private fun isRunning(): Boolean =
        try {
            val url = URL("http://localhost:${PerfettoTraceProcessor.PORT}/")
            with(url.openConnection() as HttpURLConnection) {
                return@with responseCode == 200
            }
        } catch (e: ConnectException) {
            false
        }
}
