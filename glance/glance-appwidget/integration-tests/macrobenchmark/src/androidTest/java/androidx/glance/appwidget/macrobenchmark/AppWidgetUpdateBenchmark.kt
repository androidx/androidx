/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.glance.appwidget.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.createStartupCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = 29)
@RunWith(Parameterized::class)
class AppWidgetUpdateBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode,
    useGlanceSession: Boolean,
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val appWidgetHostRule = AppWidgetHostRule(useSession = useGlanceSession)

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun initialUpdate() = benchmarkRule.measureRepeated(
        packageName = "androidx.glance.appwidget.macrobenchmark.target",
        metrics = listOf(
            TraceSectionMetric("appWidgetInitialUpdate"),
            TraceSectionMetric("GlanceAppWidget::update"),
        ),
        iterations = 100,
        compilationMode = compilationMode,
        startupMode = startupMode,
    ) {
        appWidgetHostRule.startHost()
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun appWidgetUpdate() = benchmarkRule.measureRepeated(
        packageName = "androidx.glance.appwidget.macrobenchmark.target",
        metrics = listOf(
            TraceSectionMetric("appWidgetUpdate"),
            TraceSectionMetric("GlanceAppWidget::update"),
        ),
        iterations = 100,
        compilationMode = compilationMode,
        startupMode = startupMode,
        setupBlock = {
            appWidgetHostRule.startHost()
            if (startupMode == StartupMode.COLD) killProcess()
        }
    ) {
        appWidgetHostRule.updateAppWidget()
    }

    companion object {
        @Parameterized.Parameters(name = "startup={0},compilation={1},useSession={2}")
        @JvmStatic
        fun parameters() =
            createStartupCompilationParams(
                startupModes = listOf(StartupMode.COLD, StartupMode.WARM),
                compilationModes = listOf(CompilationMode.DEFAULT)
            ).flatMap {
                listOf(
                    it + true,
                    it + false,
                )
            }
    }
}