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

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.test.assertEquals

@MediumTest
@RunWith(AndroidJUnit4::class)
class StartupTimingQueryTest {
    private fun validateFixedTrace(
        @Suppress("SameParameterValue") api: Int,
        startupMode: StartupMode,
        expectedMetrics: StartupTimingQuery.SubMetrics?,
        tracePrefix: String = "api${api}_startup_${startupMode.name.lowercase(Locale.getDefault())}"
    ) {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset(prefix = tracePrefix, suffix = ".perfetto-trace")

        val startupSubMetrics = StartupTimingQuery.getFrameSubMetrics(
            absoluteTracePath = traceFile.absolutePath,
            captureApiLevel = api,
            targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
            testPackageName = "androidx.benchmark.integration.macrobenchmark.test",
            startupMode = startupMode
        )

        assertEquals(expected = expectedMetrics, actual = startupSubMetrics)
    }

    @Test
    fun fixedApi24Cold() = validateFixedTrace(
        api = 24,
        startupMode = StartupMode.COLD,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 264869885,
            timeToFullDisplayNs = 715406822,
            timelineRangeNs = 791231114368..791946521190
        )
    )

    @Test
    fun fixedApi24Warm() = validateFixedTrace(
        api = 24,
        startupMode = StartupMode.WARM,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 108563770,
            timeToFullDisplayNs = 581026583,
            timelineRangeNs = 800868511677..801449538260
        )
    )

    @Test
    fun fixedApi24Hot() = validateFixedTrace(
        api = 24,
        startupMode = StartupMode.HOT,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 35039927,
            timeToFullDisplayNs = 537343160,
            timelineRangeNs = 780778904571..781316247731
        )
    )

    @Test
    fun fixedApi31Cold() = validateFixedTrace(
        api = 31,
        startupMode = StartupMode.COLD,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 143980066,
            timeToFullDisplayNs = 620815843,
            timelineRangeNs = 186974938196632..186975559012475
        )
    )

    @Test
    fun fixedApi31Warm() = validateFixedTrace(
        api = 31,
        startupMode = StartupMode.WARM,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 62373965,
            timeToFullDisplayNs = 555968701,
            timelineRangeNs = 186982050780778..186982606749479
        )
    )

    @Test
    fun fixedApi31Hot() = validateFixedTrace(
        api = 31,
        startupMode = StartupMode.HOT,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 40534066,
            timeToFullDisplayNs = 542222554,
            timelineRangeNs = 186969441973689..186969984196243
        )
    )

    /**
     * Validate that StartupTimingQuery returns null and doesn't crash when process name truncated
     */
    @Test
    fun fixedApi29ColdProcessNameTruncated() = validateFixedTrace(
        api = 29,
        startupMode = StartupMode.COLD,
        tracePrefix = "api29_cold_startup_processname_truncated",
        expectedMetrics = null // process name is truncated, and we currently don't handle this
    )
}
