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
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class StartupTimingQueryTest {
    private fun validateFixedTrace(
        @Suppress("SameParameterValue") api: Int,
        startupMode: StartupMode,
        expectedMetrics: StartupTimingQuery.SubMetrics
    ) {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset(
            prefix = "api${api}_startup_${startupMode.name.lowercase(Locale.getDefault())}",
            suffix = ".perfetto-trace"
        )

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
            timeToInitialDisplayNs = 358237760,
            timeToFullDisplayNs = 784769167,
            timelineRange = 269543917431669..269544702200836
        )
    )

    @Test
    fun fixedApi24Warm() = validateFixedTrace(
        api = 24,
        startupMode = StartupMode.WARM,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 135008333,
            timeToFullDisplayNs = 598500833,
            timelineRange = 268757401479247..268757999980080
        )
    )

    @Test
    fun fixedApi24Hot() = validateFixedTrace(
        api = 24,
        startupMode = StartupMode.HOT,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 54248802,
            timeToFullDisplayNs = 529336511,
            timelineRange = 268727533977218..268728063313729
        )
    )

    @Test
    fun fixedApi31Cold() = validateFixedTrace(
        api = 31,
        startupMode = StartupMode.COLD,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 137401159,
            timeToFullDisplayNs = 612424592,
            timelineRange = 186974946587883..186975559012475
        )
    )

    @Test
    fun fixedApi31Warm() = validateFixedTrace(
        api = 31,
        startupMode = StartupMode.WARM,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 55378859,
            timeToFullDisplayNs = 546599533,
            timelineRange = 186982060149946..186982606749479
        )
    )

    @Test
    fun fixedApi31Hot() = validateFixedTrace(
        api = 31,
        startupMode = StartupMode.HOT,
        StartupTimingQuery.SubMetrics(
            timeToInitialDisplayNs = 42757609,
            timeToFullDisplayNs = 537651148,
            timelineRange = 186969446545095..186969984196243
        )
    )
}