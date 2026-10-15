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

package androidx.testutils

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.runSingleSessionServer
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class TraceCounterMetricTest {

    private val captureInfo =
        Metric.CaptureInfo(
            targetPackageName = "androidx.compose.integration.hero.pokedex.macrobenchmark.target",
            testPackageName = "androidx.compose.integration.hero.pokedex.macrobenchmark",
            startupMode = StartupMode.COLD,
            apiLevel = 37,
        )

    @Test
    fun overallCounterMetrics_returnsExpectedMinMedianMaxLastAndCount() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api37_pokedex_scroll_counter", ".perfetto-trace")
        val metric =
            TraceCounterMetric(
                counterName = "pokedex:grid:items:active",
                label = "activeGridItems",
                modes =
                    listOf(
                        TraceCounterMetric.Mode.Min,
                        TraceCounterMetric.Mode.Median,
                        TraceCounterMetric.Mode.Max,
                        TraceCounterMetric.Mode.Last,
                        TraceCounterMetric.Mode.Count,
                    ),
            )

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertThat(measurements)
            .containsExactly(
                Metric.Measurement("activeGridItemsMin", 10.0),
                Metric.Measurement("activeGridItemsMedian", 12.0),
                Metric.Measurement("activeGridItemsMax", 18.0),
                Metric.Measurement("activeGridItemsLast", 12.0),
                Metric.Measurement("activeGridItemsCount", 358.0),
            )
    }

    @Test
    fun windowedCounterMetrics_byFlingSlice_returnsPerWindowStats() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api37_pokedex_scroll_counter", ".perfetto-trace")
        val metric =
            TraceCounterMetric(
                counterName = "pokedex:grid:items:active",
                label = "activeGridItems",
                modes =
                    listOf(
                        TraceCounterMetric.Mode.Min,
                        TraceCounterMetric.Mode.Median,
                        TraceCounterMetric.Mode.Max,
                        TraceCounterMetric.Mode.Last,
                        TraceCounterMetric.Mode.Count,
                    ),
                windowSliceName = "PokedexScrollBenchmark#fling%",
            )

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertThat(measurements)
            .containsExactly(
                Metric.Measurement("activeGridItemsWindow1Min", 10.0),
                Metric.Measurement("activeGridItemsWindow1Median", 12.0),
                Metric.Measurement("activeGridItemsWindow1Max", 18.0),
                Metric.Measurement("activeGridItemsWindow1Last", 12.0),
                Metric.Measurement("activeGridItemsWindow1Count", 110.0),
                Metric.Measurement("activeGridItemsWindow2Min", 12.0),
                Metric.Measurement("activeGridItemsWindow2Median", 13.0),
                Metric.Measurement("activeGridItemsWindow2Max", 14.0),
                Metric.Measurement("activeGridItemsWindow2Last", 12.0),
                Metric.Measurement("activeGridItemsWindow2Count", 80.0),
                Metric.Measurement("activeGridItemsWindow3Min", 10.0),
                Metric.Measurement("activeGridItemsWindow3Median", 11.0),
                Metric.Measurement("activeGridItemsWindow3Max", 14.0),
                Metric.Measurement("activeGridItemsWindow3Last", 12.0),
                Metric.Measurement("activeGridItemsWindow3Count", 88.0),
                Metric.Measurement("activeGridItemsWindow4Min", 12.0),
                Metric.Measurement("activeGridItemsWindow4Median", 13.0),
                Metric.Measurement("activeGridItemsWindow4Max", 14.0),
                Metric.Measurement("activeGridItemsWindow4Last", 12.0),
                Metric.Measurement("activeGridItemsWindow4Count", 80.0),
            )
    }

    @Test
    fun missingCounter_returnsZeroForCountAndEmptyForValueModes() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api37_pokedex_scroll_counter", ".perfetto-trace")
        val valueMetric =
            TraceCounterMetric(
                counterName = "pokedex:grid:items:nonexistent",
                label = "missingCounter",
                mode = TraceCounterMetric.Mode.Median,
            )
        val countMetric =
            TraceCounterMetric(
                counterName = "pokedex:grid:items:nonexistent",
                label = "missingCounter",
                mode = TraceCounterMetric.Mode.Count,
            )

        val (valueMeasurements, countMeasurements) =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                valueMetric.getMeasurements(captureInfo = captureInfo, traceSession = this) to
                    countMetric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertThat(valueMeasurements).isEmpty()
        assertThat(countMeasurements)
            .containsExactly(Metric.Measurement("missingCounterCount", 0.0))
    }
}
