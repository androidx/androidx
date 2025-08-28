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

package androidx.compose.integration.hero.pokedex.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.common.macrobenchmark.HeroMacrobenchmarkDefaults
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexDatabaseCleanupRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PokedexScrollBenchmark(
    val compilationMode: CompilationMode,
    val enableSharedTransitionScope: Boolean,
    val enableSharedElementTransitions: Boolean,
) {
    val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val pokedexBenchmarkRuleChain: RuleChain =
        RuleChain.outerRule(PokedexDatabaseCleanupRule()).around(benchmarkRule)

    @Test
    fun scrollHomeCompose() =
        benchmarkScroll(
            action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_COMPOSE_ACTIVITY",
            setupBlock = {
                val searchCondition = Until.hasObject(By.res("Pokemon"))
                device.wait(searchCondition, 3_000)
                val content = device.findObject(By.res("PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
            measureBlock = { scrollActions(device.findObject(By.res("PokedexList"))) },
        )

    @Test
    fun scrollHomeViews() =
        benchmarkScroll(
            action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_VIEWS_HOME_ACTIVITY",
            setupBlock = {
                device.waitOrThrow(
                    Until.hasObject(By.res(POKEDEX_TARGET_PACKAGE_NAME, "cardView")),
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

    @OptIn(ExperimentalMetricApi::class)
    private fun benchmarkScroll(
        action: String,
        setupBlock: MacrobenchmarkScope.() -> Unit,
        measureBlock: MacrobenchmarkScope.() -> Unit,
    ) =
        benchmarkRule.measureRepeated(
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics() + FrameTimingGfxInfoMetric(),
            compilationMode = compilationMode,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
            setupBlock = {
                val intent = Intent()
                intent.action = action
                intent.putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
                intent.putExtra(
                    POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS,
                    enableSharedElementTransitions,
                )
                startActivityAndWait(intent)
                setupBlock()
            },
            measureBlock = measureBlock,
        )

    private fun MacrobenchmarkScope.scrollActions(content: UiObject2) {
        content.fling(Direction.DOWN)
        device.waitForIdle()
        content.fling(Direction.UP)
        device.waitForIdle()
        content.fling(Direction.DOWN)
        device.waitForIdle()
        content.fling(Direction.UP)
        device.waitForIdle()
    }

    companion object {
        /**
         * Parameters for the benchmark. Uses abbreviations because of file length limit for
         * results. compilation = Compilation Mode eSTS = enableSharedTransitionScope eSET =
         * enableSharedElementTransition
         */
        @Parameterized.Parameters(name = "compilation={0},eSTS={1},eSET={2}")
        @JvmStatic
        fun parameters(): List<Array<Any>> =
            createCompilationParams().flatMap { compilationMode ->
                PokedexSharedElementBenchmarkConfiguration.AllConfigurations.map { configuration ->
                    arrayOf(*compilationMode, *configuration.asBenchmarkArguments())
                }
            }
    }
}
