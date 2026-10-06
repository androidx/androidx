/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(
    ExperimentalMetricApi::class,
    ExperimentalBenchmarkConfigApi::class,
    ExperimentalPerfettoCaptureApi::class,
)

package androidx.compose.integration.hero.pokedex.macrobenchmark

import android.content.Intent
import android.util.DisplayMetrics
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.MemoryProfilingConfig
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.compose.integration.hero.common.macrobenchmark.HeroMacrobenchmarkDefaults
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.findObjectOrThrow
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.waitOrThrow
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.testutils.CpuFrequencyChangeMetric
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import androidx.testutils.defaultMemoryMetrics
import androidx.tracing.Trace
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PokedexScrollBenchmark(
    val compilationMode: CompilationMode,
    val enableSharedTransitionScope: Boolean,
    val enableSharedElementTransitions: Boolean,
) : PokedexBenchmarkBase() {
    @Test
    fun scrollHomeCompose() =
        benchmarkScroll(
            action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_COMPOSE_ACTIVITY",
            setupBlock = {
                device.waitForIdle()
                device.waitOrThrow(Until.hasObject(By.res("Ablazeon_card")), 3_000)
                val content = device.findObjectOrThrow(By.res("PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
            measureBlock = { scrollActions(device.findObjectOrThrow(By.res("PokedexList"))) },
        )

    @Test
    fun scrollHomeViews() =
        benchmarkScroll(
            action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_VIEWS_HOME_ACTIVITY",
            setupBlock = {
                device.waitForIdle()
                // Wait until we have content loaded
                device.waitOrThrow(
                    Until.hasObject(By.res(POKEDEX_TARGET_PACKAGE_NAME, "name")),
                    3_000,
                )
                val content =
                    device.findObjectOrThrow(By.res(POKEDEX_TARGET_PACKAGE_NAME, "PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
            measureBlock = {
                scrollActions(
                    device.findObjectOrThrow(By.res(POKEDEX_TARGET_PACKAGE_NAME, "PokedexList"))
                )
            },
        )

    @OptIn(ExperimentalMetricApi::class, ExperimentalBenchmarkConfigApi::class)
    private fun benchmarkScroll(
        action: String,
        enableScrollbar: Boolean = true,
        setupBlock: MacrobenchmarkScope.() -> Unit,
        measureBlock: MacrobenchmarkScope.() -> Unit,
    ) {

        benchmarkRule.measureRepeated(
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            metrics =
                defaultComposeScrollingMetrics() +
                    FrameTimingGfxInfoMetric() +
                    CpuFrequencyChangeMetric() +
                    defaultMemoryMetrics(),
            compilationMode = compilationMode,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
            experimentalConfig =
                ExperimentalConfig(
                    memoryProfilingConfig =
                        MemoryProfilingConfig(
                            isSampleArtHeapEnabled = true,
                            isSampleNativeHeapEnabled = true,
                        )
                ),
            setupBlock = {
                // Start off by killing the existing process. After previous iterations, the
                // activity might be running, and we wouldn't launch our setup activity as the
                // process is already active.
                killProcess()
                databaseCleanupRule.deleteDatabaseFiles()
                cacheCleanupRule.deleteCacheFiles()

                val intent = Intent()
                intent.configure(
                    action = action,
                    enableSharedTransitionScope = enableSharedTransitionScope,
                    enableSharedElementTransitions = enableSharedElementTransitions,
                    enableScrollbar = enableScrollbar,
                )
                startActivityAndWait(intent)
                setupBlock()
            },
            measureBlock = measureBlock,
        )
    }

    private fun MacrobenchmarkScope.scrollActions(content: UiObject2) {
        // Perform down flings (scrolling toward the bottom of the list) with the default fling
        // speed, and up flings (scrolling back toward the top) with a lower speed. Injected input
        // event velocity varies across gestures, so a lower up-fling speed prevents the return
        // fling from hitting the top bound of the list.
        val downSpeed = (FLING_SPEED_DP_PER_SECOND * targetDisplayDensity).roundToInt()
        val upSpeed = (downSpeed * OPPOSING_DIRECTION_FLING_FACTOR).roundToInt()
        fun flingAndWaitForIdle(direction: Direction, speed: Int) {
            trace("PokedexScrollBenchmark#fling($direction, speed=$speed)") {
                val canStillScroll = content.fling(direction, speed)
                Trace.setCounter(
                    "pokedex:scroll:canStillScroll",
                    if (canStillScroll) 1 else 0,
                )
                device.waitForIdle()
            }
        }
        flingAndWaitForIdle(Direction.DOWN, downSpeed)
        flingAndWaitForIdle(Direction.UP, upSpeed)
        flingAndWaitForIdle(Direction.DOWN, downSpeed)
        flingAndWaitForIdle(Direction.UP, upSpeed)
    }

    /** Density of the instrumentation's target context, in DP. */
    private val MacrobenchmarkScope.targetDisplayDensity: Float
        get() {
            val uiContext = instrumentation.targetContext
            val densityDpi = uiContext.resources.configuration.densityDpi
            return densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        }

    companion object {
        /** The fling speed used for flings, in dp per second. Copied from [UiObject2]. */
        private const val FLING_SPEED_DP_PER_SECOND = 7_500

        /**
         * Slows [Direction.UP] flings so they do not scroll past the start of the list when
         * injected [android.view.MotionEvent] timing varies.
         */
        private const val OPPOSING_DIRECTION_FLING_FACTOR = 0.85f

        /**
         * Parameters for the benchmark. Uses abbreviations because of file length limit for
         * results. We use CompilationMode.Full() in CI to reduce the amount of benchmark
         * permutations. compilation = Compilation Mode eSTS = enableSharedTransitionScope eSET =
         * enableSharedElementTransition
         */
        @Parameterized.Parameters(name = "compilation={0},eSTS={1},eSET={2}")
        @JvmStatic
        fun parameters(): List<Array<Any>> =
            createCompilationParams(compilationModes = listOf(CompilationMode.Full())).flatMap {
                compilationMode ->
                PokedexSharedElementBenchmarkConfiguration.AllConfigurations.map { configuration ->
                    arrayOf(*compilationMode, *configuration.asBenchmarkArguments())
                }
            }
    }
}

internal fun <R> trace(sectionName: String, block: () -> R): R =
    try {
        Trace.beginSection(sectionName)
        block()
    } finally {
        Trace.endSection()
    }
