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

package androidx.web.integration.macrobenchmark

import android.content.ComponentName
import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.web.WebFeature
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "androidx.web.testapp"
private const val BENCHMARK_ACTIVITY = "androidx.web.testapp.BenchmarkActivity"
private const val EXTRA_USE_WEB_SURFACE = "use_web_surface"
private const val LOAD_TIMEOUT_MS = 10_000L
private const val STEPS_PER_SWIPE = 50

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class WebSurfaceVsWebViewBenchmark {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        assumeTrue(WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT))
        assumeTrue(WebFeature.isFeatureSupported(WebFeature.WEB_SURFACE))
    }

    @Test fun scroll_webSurface() = measureScroll(useWebSurface = true)

    @Test fun scroll_androidView() = measureScroll(useWebSurface = false)

    private fun measureScroll(useWebSurface: Boolean) {
        var segments = emptyArray<Point>()
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(
                    FrameTimingMetric(),
                    TraceSectionMetric("Record View#draw()", TraceSectionMetric.Mode.Sum),
                    TraceSectionMetric("Record View#draw()", TraceSectionMetric.Mode.Average),
                    TraceSectionMetric(
                        "AndroidOwner:measureAndLayout",
                        TraceSectionMetric.Mode.Sum,
                    ),
                    TraceSectionMetric(
                        "AndroidOwner:measureAndLayout",
                        TraceSectionMetric.Mode.Average,
                    ),
                ),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                val intent =
                    Intent().apply {
                        component = ComponentName(PACKAGE_NAME, BENCHMARK_ACTIVITY)
                        putExtra(EXTRA_USE_WEB_SURFACE, useWebSurface)
                    }
                startActivityAndWait(intent)
                check(device.wait(Until.hasObject(By.text("Benchmark Item #1")), LOAD_TIMEOUT_MS)) {
                    "Timed out waiting for benchmark page content to render"
                }
                val contentBounds = device.findObject(By.res("android", "content")).visibleBounds
                val centerX = contentBounds.centerX()
                // Start inside the web content in the lower half of the viewport, then continuously
                // drag up and down within the scroll range without lifting the finger or waiting
                // for idle so measured frames only capture active scrolling with web content on
                // screen.
                val startY = contentBounds.top + (contentBounds.height() * 0.75f).toInt()
                val topY = contentBounds.top + (contentBounds.height() * 0.30f).toInt()
                val bottomY = contentBounds.top + (contentBounds.height() * 0.70f).toInt()
                segments =
                    arrayOf(
                        Point(centerX, startY),
                        Point(centerX, topY),
                        Point(centerX, bottomY),
                        Point(centerX, topY),
                        Point(centerX, bottomY),
                        Point(centerX, topY),
                        Point(centerX, bottomY),
                    )
                device.waitForIdle()
            },
        ) {
            check(device.swipe(segments, STEPS_PER_SWIPE)) { "Failed to inject scroll swipe" }
        }
    }
}
