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

package androidx.benchmark.perfetto

import androidx.benchmark.Packages
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import perfetto.protos.DataSourceConfig
import perfetto.protos.FtraceConfig
import perfetto.protos.TraceConfig

@RunWith(AndroidJUnit4::class)
@SmallTest
class PerfettoConfigTest {
    @Test
    fun ftraceBasics() {
        val atraceApps = listOf(Packages.TEST)
        val ftraceDataSource =
            perfettoConfig(atraceApps = atraceApps, stackSamplingConfig = null).data_sources.first {
                it.config?.name == "linux.ftrace"
            }

        assertNotNull(ftraceDataSource)

        val ftraceConfig = ftraceDataSource.config?.ftrace_config
        assertNotNull(ftraceConfig)

        assertEquals(listOf(Packages.TEST), ftraceConfig.atrace_apps)

        assertTrue(ftraceConfig.atrace_categories.contains("view"))
        assertFalse(ftraceConfig.atrace_categories.contains("webview"))
        assertFalse(ftraceConfig.atrace_categories.contains("memory"))
    }

    @Test
    fun validateAndEncode() {
        // default config shouldn't throw
        perfettoConfig(atraceApps = listOf(Packages.TEST), stackSamplingConfig = null)
            .validateAndEncode()
    }

    @OptIn(androidx.benchmark.ExperimentalBenchmarkConfigApi::class)
    @Suppress("NewApi")
    @Test
    fun experimentalConfig_memoryProfilingConfig() {
        val memoryConfig =
            androidx.benchmark.MemoryProfilingConfig(
                isSampleArtHeapEnabled = true,
                isSampleNativeHeapEnabled = true,
            )
        val config = androidx.benchmark.ExperimentalConfig(memoryProfilingConfig = memoryConfig)
        assertNotNull(config.memoryProfilingConfig)
        assertTrue(config.memoryProfilingConfig!!.isSampleArtHeapEnabled)
        assertTrue(config.memoryProfilingConfig!!.isSampleNativeHeapEnabled)
        assertNotNull(androidx.benchmark.ExperimentalConfig())
    }

    @OptIn(androidx.benchmark.ExperimentalBenchmarkConfigApi::class)
    @Suppress("NewApi")
    @Test
    fun perfettoConfig_memoryProfiling() {
        val memoryConfig =
            androidx.benchmark.MemoryProfilingConfig(
                isSampleArtHeapEnabled = true,
                isSampleNativeHeapEnabled = true,
            )
        val perfettoConfig =
            perfettoConfig(
                atraceApps = listOf(Packages.TEST),
                stackSamplingConfig = null,
                memoryProfilingConfig = memoryConfig,
                memoryProfilingPackages = listOf(Packages.TEST),
            )
        assertNotNull(perfettoConfig)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val heapprofdDataSource =
                perfettoConfig.data_sources.firstOrNull { it.config?.name == "android.heapprofd" }
            assertNotNull(heapprofdDataSource)
            assertEquals(
                listOf("com.android.art", "libc.malloc"),
                heapprofdDataSource.config?.heapprofd_config?.heaps,
            )
            assertEquals(
                listOf(Packages.TEST),
                heapprofdDataSource.config?.heapprofd_config?.process_cmdline,
            )
        } else {
            val heapprofdDataSource =
                perfettoConfig.data_sources.firstOrNull { it.config?.name == "android.heapprofd" }
            assertNull(heapprofdDataSource)
        }
        assertTrue(perfettoConfig.data_sources.any { it.config?.name == "linux.ftrace" })
        assertTrue(perfettoConfig.data_sources.any { it.config?.name == "linux.process_stats" })
        perfettoConfig.validateAndEncode()
    }

    @OptIn(androidx.benchmark.ExperimentalBenchmarkConfigApi::class)
    @Suppress("NewApi")
    @Test
    fun perfettoConfig_memoryProfiling_artHeapOnly() {
        val memoryConfig =
            androidx.benchmark.MemoryProfilingConfig(
                isSampleArtHeapEnabled = true,
                isSampleNativeHeapEnabled = false,
            )
        val perfettoConfig =
            perfettoConfig(
                atraceApps = listOf(Packages.TEST),
                stackSamplingConfig = null,
                memoryProfilingConfig = memoryConfig,
                memoryProfilingPackages = listOf(Packages.TEST),
            )
        assertNotNull(perfettoConfig)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val heapprofdDataSource =
                perfettoConfig.data_sources.firstOrNull { it.config?.name == "android.heapprofd" }
            assertNotNull(heapprofdDataSource)
            assertEquals(
                listOf("com.android.art"),
                heapprofdDataSource.config?.heapprofd_config?.heaps,
            )
        } else {
            val heapprofdDataSource =
                perfettoConfig.data_sources.firstOrNull { it.config?.name == "android.heapprofd" }
            assertNull(heapprofdDataSource)
        }
        assertTrue(perfettoConfig.data_sources.any { it.config?.name == "linux.ftrace" })
    }

    @OptIn(androidx.benchmark.ExperimentalBenchmarkConfigApi::class)
    @Suppress("NewApi")
    @Test
    fun perfettoConfig_memoryProfiling_nativeHeapOnly() {
        val memoryConfig =
            androidx.benchmark.MemoryProfilingConfig(
                isSampleArtHeapEnabled = false,
                isSampleNativeHeapEnabled = true,
            )
        val perfettoConfig =
            perfettoConfig(
                atraceApps = listOf(Packages.TEST),
                stackSamplingConfig = null,
                memoryProfilingConfig = memoryConfig,
                memoryProfilingPackages = listOf(Packages.TEST),
            )
        assertNotNull(perfettoConfig)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val heapprofdDataSource =
                perfettoConfig.data_sources.firstOrNull { it.config?.name == "android.heapprofd" }
            assertNotNull(heapprofdDataSource)
            assertEquals(
                listOf("libc.malloc"),
                heapprofdDataSource.config?.heapprofd_config?.heaps,
            )
        } else {
            val heapprofdDataSource =
                perfettoConfig.data_sources.firstOrNull { it.config?.name == "android.heapprofd" }
            assertNull(heapprofdDataSource)
        }
        assertTrue(perfettoConfig.data_sources.any { it.config?.name == "linux.ftrace" })
    }

    @OptIn(androidx.benchmark.ExperimentalBenchmarkConfigApi::class)
    @Suppress("NewApi")
    @Test
    fun memoryProfilingConfig_requiresAtLeastOneHeap() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                androidx.benchmark.MemoryProfilingConfig(
                    isSampleArtHeapEnabled = false,
                    isSampleNativeHeapEnabled = false,
                )
            }
        assertTrue(
            exception.message!!.contains(
                "At least one of isSampleArtHeapEnabled or isSampleNativeHeapEnabled must be enabled"
            )
        )
    }

    @OptIn(androidx.benchmark.ExperimentalBenchmarkConfigApi::class)
    @Suppress("NewApi")
    @Test
    fun perfettoConfig_memoryProfiling_requiresNonEmptyPackages() {
        val memoryConfig =
            androidx.benchmark.MemoryProfilingConfig(
                isSampleArtHeapEnabled = true,
                isSampleNativeHeapEnabled = false,
            )
        val exception =
            assertFailsWith<IllegalArgumentException> {
                perfettoConfig(
                    atraceApps = listOf(Packages.TEST),
                    stackSamplingConfig = null,
                    memoryProfilingConfig = memoryConfig,
                    memoryProfilingPackages = emptyList(),
                )
            }
        assertTrue(exception.message!!.contains("memoryProfilingPackages must not be empty"))
    }

    @Test
    fun validateAndEncode_invalidAtraceCategories() {
        val invalidConfig =
            TraceConfig(
                buffers =
                    listOf(
                        TraceConfig.BufferConfig(
                            size_kb = 16384,
                            fill_policy = TraceConfig.BufferConfig.FillPolicy.RING_BUFFER,
                        )
                    ),
                data_sources =
                    listOf(
                        TraceConfig.DataSource(
                            config =
                                DataSourceConfig(
                                    name = "linux.ftrace",
                                    target_buffer = 0,
                                    ftrace_config =
                                        FtraceConfig(atrace_categories = listOf("bad_category")),
                                )
                        )
                    ),
            )
        val exception = assertFailsWith<IllegalStateException> { invalidConfig.validateAndEncode() }
        assertTrue(exception.message!!.contains("bad_category"))
    }

    @SdkSuppress(maxSdkVersion = 27)
    @Test
    fun validateAndEncode_invalidWildcard() {
        val invalidConfig =
            TraceConfig(
                buffers =
                    listOf(
                        TraceConfig.BufferConfig(
                            size_kb = 16384,
                            fill_policy = TraceConfig.BufferConfig.FillPolicy.RING_BUFFER,
                        )
                    ),
                data_sources =
                    listOf(
                        TraceConfig.DataSource(
                            config =
                                DataSourceConfig(
                                    name = "linux.ftrace",
                                    target_buffer = 0,
                                    ftrace_config =
                                        FtraceConfig(
                                            atrace_categories = listOf("view"),
                                            atrace_apps = listOf("*"),
                                        ),
                                )
                        )
                    ),
            )
        val exception = assertFailsWith<IllegalStateException> { invalidConfig.validateAndEncode() }
        assertEquals(
            expected = "Support for wildcard (*) app matching in atrace added in API 28",
            actual = exception.message,
        )
    }

    @SdkSuppress(maxSdkVersion = 23)
    @Test
    fun validateAndEncode_invalidLength() {
        val invalidConfig =
            perfettoConfig(
                atraceApps =
                    listOf(
                        "0123456789",
                        "0123456789",
                        "0123456789",
                        "0123456789",
                        "0123456789",
                        "0123456789",
                        "0123456789",
                        "0123456789",
                        "0123456789",
                    ),
                stackSamplingConfig = null,
            )
        val exception = assertFailsWith<IllegalStateException> { invalidConfig.validateAndEncode() }
        assertEquals(
            expected =
                "Unable to trace package list (\"0123456789,0123456789,0123456789," +
                    "0123456789,0123456789,0123456789,0123456789,0123456789,0123456789\").length" +
                    " = 98 > 91 chars, which is the limit before API 24",
            actual = exception.message,
        )
    }
}
