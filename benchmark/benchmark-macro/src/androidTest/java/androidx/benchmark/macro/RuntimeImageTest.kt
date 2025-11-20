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

package androidx.benchmark.macro

import androidx.benchmark.DeviceInfo
import androidx.benchmark.json.BenchmarkData.TestResult.SingleMetricResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 31)
@RunWith(AndroidJUnit4::class)
class RuntimeImageTest {
    private val className = RuntimeImageTest::class.java.name
    private val iterCount = 3

    private fun captureRecyclerViewListStartupMetrics(
        testName: String
    ): Map<String, SingleMetricResult> =
        macrobenchmarkWithStartupMode(
                uniqueName = "${className}_$testName",
                className = className,
                testName = testName,
                packageName = Packages.TARGET,
                metrics = listOf(ArtMetric()),
                compilationMode = CompilationMode.None(),
                iterations = iterCount,
                experimentalConfig = null,
                startupMode = StartupMode.COLD,
                setupBlock = {},
                measureBlock = {
                    startActivityAndWait {
                        it.setPackage(packageName)
                        it.action =
                            "androidx.benchmark.integration.macrobenchmark.target.RECYCLER_VIEW"
                        it.putExtra("ITEM_COUNT", 5)
                    }
                    if (iteration != iterCount - 1) {
                        // For every iter but last we wait for the runtime image flush. Subsequent
                        // iterations will then be able to observe runtime image presence via class
                        // loading counts. We at least save the delay on last iteration.
                        DeviceInfo.sleepToAwaitRuntimeImageFlush()
                    }
                },
            )
            .metrics

    @LargeTest
    @Test
    fun classLoadCount() {
        assumeFalse(
            "Test requires that we don't poison the Runtime image",
            DeviceInfo.poisonTheRuntimeImage,
        )
        assumeTrue("Test requires runtime image support", DeviceInfo.supportsRuntimeImages)
        assumeTrue("Test requires class load tracing", DeviceInfo.supportsClassLoadTracing)

        val testName = RuntimeImageTest::classLoadCount.name
        val results = captureRecyclerViewListStartupMetrics(testName)

        val classLoadCount = results["artClassLoadCount"]!!.runs

        // observed >700 in practice, lower threshold used to be resilient
        assertTrue(
            classLoadCount.all { it > 500 },
            "too few class loads seen, observed: $classLoadCount",
        )
    }
}
