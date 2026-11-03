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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import android.graphics.Point
import android.os.Build
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.MemoryProfilingConfig
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.testutils.defaultComposeScrollingMetrics
import androidx.testutils.defaultMemoryMetrics
import androidx.testutils.findObjectOrThrow
import androidx.testutils.getStartupMetrics
import androidx.testutils.waitOrThrow
import androidx.tracing.trace
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Measures an image grid and viewer that use shared element transitions, with shared elements
 * enabled and disabled through `SharedContentConfig.isEnabled` (b/502677590).
 */
@LargeTest
@RunWith(Parameterized::class)
class SharedElementTransitionBenchmark(private val sharedElementsEnabled: Boolean) {
    private val benchmarkRule = MacrobenchmarkRule()

    private val imageServerRule = ImageMockServerRule(Images)

    // The image server must be running before the benchmark starts the app.
    @get:Rule val rules: RuleChain = RuleChain.outerRule(imageServerRule).around(benchmarkRule)

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /** Cold start until the first grid images are loaded. */
    @Test
    fun coldStartup() {
        measure(
            startupMode = StartupMode.COLD,
            metrics = getStartupMetrics() + gpuMemoryMetric(),
            setupBlock = { pressHome() },
        ) {
            startTargetActivity()
        }
    }

    /** Scrolls the grid down [ScrollCount] times, staying far from the end of the grid. */
    @Test
    fun scroll() {
        measure(
            startupMode = null,
            metrics = defaultComposeScrollingMetrics() + memoryMetrics(),
            setupBlock = {
                // A new process per iteration, so memory from earlier iterations isn't measured.
                killProcess()
                startTargetActivity()
            },
        ) {
            val grid = device.findObjectOrThrow(By.desc(ResultsGridDescription))
            val center = grid.visibleCenter
            // UiObject2.fling waits for Until.scrollFinished, which never completes for Compose
            // scrolling containers in the UiAutomator version used here, so drag instead.
            var firstVisibleItem = firstVisibleItemIndex()
            repeat(ScrollCount) { index ->
                trace("scroll $index") {
                    grid.drag(Point(center.x, center.y / 3))
                    waitForState(GridState)
                    val newFirstVisibleItem = firstVisibleItemIndex()
                    check(newFirstVisibleItem > firstVisibleItem) {
                        "Grid did not scroll: first visible item $newFirstVisibleItem"
                    }
                    firstVisibleItem = newFirstVisibleItem
                }
            }
        }
    }

    /** Opens the viewer from the grid and goes back, [TransitionCount] times. */
    @Test
    fun openViewer() {
        measure(
            startupMode = null,
            metrics = defaultComposeScrollingMetrics() + memoryMetrics(),
            setupBlock = {
                // A new process per iteration, so memory from earlier iterations isn't measured.
                killProcess()
                startTargetActivity()
            },
        ) {
            repeat(TransitionCount) { index ->
                trace("openViewer $index") {
                    device.findObjectOrThrow(By.desc("item-$index")).click()
                    waitForState("${ViewerStatePrefix}1")
                    device.pressBack()
                    waitForState(GridState)
                }
            }
        }
    }

    /**
     * Runs the benchmark. After the measured iterations, a heapprofd pass records ART and native
     * allocations (API 29+).
     */
    @OptIn(ExperimentalBenchmarkConfigApi::class, ExperimentalPerfettoCaptureApi::class)
    private fun measure(
        startupMode: StartupMode?,
        metrics: List<Metric>,
        setupBlock: MacrobenchmarkScope.() -> Unit,
        measureBlock: MacrobenchmarkScope.() -> Unit,
    ) {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = metrics,
            iterations = 10,
            experimentalConfig =
                ExperimentalConfig(
                    memoryProfilingConfig =
                        if (Build.VERSION.SDK_INT >= 29) {
                            MemoryProfilingConfig(
                                isSampleArtHeapEnabled = true,
                                isSampleNativeHeapEnabled = true,
                            )
                        } else {
                            null
                        }
                ),
            compilationMode = CompilationMode.Partial(),
            startupMode = startupMode,
            setupBlock = setupBlock,
            measureBlock = {
                measureBlock()
                imageServerRule.awaitImagesServed()
            },
        )
    }

    /** Default memory metrics plus GPU memory, where shared element transitions allocate layers. */
    private fun memoryMetrics(): List<Metric> = defaultMemoryMetrics() + gpuMemoryMetric()

    @OptIn(ExperimentalMetricApi::class)
    private fun gpuMemoryMetric(): Metric =
        MemoryUsageMetric(
            mode = MemoryUsageMetric.Mode.Max,
            subMetrics = listOf(MemoryUsageMetric.SubMetric.Gpu),
        )

    private fun MacrobenchmarkScope.startTargetActivity() {
        val intent = Intent()
        intent.action = Action
        // Without a startup mode the previous activity would be reused. Start a new one in each
        // iteration, so that the scroll position and the image cache are reset.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(SharedElementsEnabledExtra, sharedElementsEnabled)
        intent.putExtra(ImageBaseUrlExtra, imageServerRule.baseUrl)
        startActivityAndWait(intent)
        waitForState(GridState)
        imageServerRule.awaitImagesServed()
    }

    /**
     * Waits until the app reports [state]: the expected screen is shown, no transition is running,
     * and every requested image has loaded.
     */
    private fun waitForState(state: String) {
        trace("wait for $state") {
            device.waitOrThrow(Until.hasObject(By.desc(state)), TimeoutMs) {
                val current = device.findObject(By.descStartsWith(StatePrefix))?.contentDescription
                "Expected app state $state, but it is $current"
            }
        }
    }

    /**
     * Smallest index of the grid items on screen. Retries when UiAutomator returns nodes that the
     * lazy grid replaced before they were read.
     */
    private fun firstVisibleItemIndex(): Int {
        repeat(StaleRetries) {
            try {
                return device.findObjects(By.descStartsWith(ItemPrefix)).minOf {
                    it.contentDescription.removePrefix(ItemPrefix).toInt()
                }
            } catch (_: StaleObjectException) {}
        }
        error("Could not read the visible grid items")
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.SHARED_ELEMENT_TRANSITION_ACTIVITY"
        private const val SharedElementsEnabledExtra = "SHARED_ELEMENTS_ENABLED"
        private const val ImageBaseUrlExtra = "IMAGE_BASE_URL"
        private const val ResultsGridDescription = "results-grid"
        private const val ItemPrefix = "item-"

        // UI states reported by SharedElementTransitionActivity.
        private const val StatePrefix = "state-"
        private const val GridState = "state-grid"
        private const val ViewerStatePrefix = "state-viewer-"

        private const val TransitionCount = 3

        // About two screens per iteration; the grid has 200 items, so it never reaches the end.
        private const val ScrollCount = 6
        private const val TimeoutMs = 5_000L
        private const val StaleRetries = 3

        // Must match the images requested by SharedElementTransitionActivity.
        private const val ImageCount = 48
        private const val ImageWidthPx = 480
        private val AspectRatios = floatArrayOf(0.6f, 0.75f, 0.8f, 1f, 1.25f, 1.5f)
        private val Images =
            List(ImageCount) { id ->
                val aspectRatio = AspectRatios[id % AspectRatios.size]
                ImageSpec(id, ImageWidthPx, (ImageWidthPx / aspectRatio).roundToInt())
            }

        @Parameterized.Parameters(name = "sharedElementsEnabled={0}")
        @JvmStatic
        fun parameters(): List<Boolean> = listOf(true, false)
    }
}
