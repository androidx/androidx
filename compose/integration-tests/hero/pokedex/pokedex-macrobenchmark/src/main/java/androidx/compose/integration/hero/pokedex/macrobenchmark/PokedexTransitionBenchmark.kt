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
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.common.macrobenchmark.HeroMacrobenchmarkDefaults
import androidx.compose.integration.hero.pokedex.macrobenchmark.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS
import androidx.compose.integration.hero.pokedex.macrobenchmark.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE
import androidx.compose.integration.hero.pokedex.macrobenchmark.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import kotlin.IllegalArgumentException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PokedexTransitionBenchmark(
    val compilationMode: CompilationMode,
    val enableSharedTransitionScope: Boolean,
    val enableSharedElementTransitions: Boolean,
) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun homeToDetailsTransition() {
        benchmarkRule.measureRepeated(
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics() + FrameTimingGfxInfoMetric(),
            compilationMode = compilationMode,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
            setupBlock = {
                // Start out by deleting any existing data
                resetPokedexDatabase()

                val intent = Intent()
                intent.action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_COMPOSE_ACTIVITY"
                intent.putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
                intent.putExtra(
                    POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS,
                    enableSharedElementTransitions,
                )
                startActivityAndWait(intent)

                // Ablazeon always is the first pokemon in the grid
                val searchCondition = Until.hasObject(By.res("Ablazeon_card"))
                device.wait(searchCondition, 3_000)
                val content = device.findObject(By.res("PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
        ) {
            homeToDetailsAndBackAction("Ablazeon")
            device.waitForIdle()
            homeToDetailsAndBackAction("Anglark")
        }
    }

    private fun MacrobenchmarkScope.homeToDetailsAndBackAction(pokemonName: String) {
        val list = device.findObject(By.res("PokedexList"))
        val pokemonCard = list.findObject(By.res("${pokemonName}_card"))
        pokemonCard.click()

        if (enableSharedElementTransitions) {
            waitForTransitionStatus("details", active = true)
            waitForTransitionStatus("details", active = false)
        }

        device.waitOrThrow(Until.hasObject(By.res("progress-animation-active-true")), 1000)
        device.waitOrThrow(Until.gone(By.res("progress-animation-active-true")), 1000)

        device.findObject(By.res("pokedexDetailsBack")).click()

        if (enableSharedElementTransitions) {
            waitForTransitionStatus("home", active = true)
        }
        waitForTransitionStatus("home", active = false)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.waitForTransitionStatus(
        name: String,
        active: Boolean,
        timeoutMs: Long = 2_000L,
    ) {
        val transitionElementName = "pokedex-$name-transition-active-$active"
        val waitForPokedexDetailsTransitionResult: Boolean? =
            device.wait(Until.hasObject(By.res(transitionElementName)), timeoutMs)
        if (waitForPokedexDetailsTransitionResult == null) {
            Log.d(
                "PokedexTransitionBenchmark",
                "Waited for $transitionElementName, did not appear after $timeoutMs ms." +
                    "Dumping window hierarchy.",
            )
            device.dumpWindowHierarchy(System.out)
            throw IllegalArgumentException(
                "Waited for $transitionElementName, did not appear" + " after $timeoutMs ms."
            )
        }
    }

    companion object {

        /**
         * Parameters for the benchmark. Uses abbreviations because of file length limit for
         * results. compilation = Compilation Mode. We use CompilationMode.Full() in CI to reduce
         * the amount of benchmark permutations. eSTS = enableSharedTransitionScope eSET =
         * enableSharedElementTransition
         */
        @Parameterized.Parameters(name = "compilation={0},eSTS={1},eSET={2}")
        @JvmStatic
        fun parameters() =
            createCompilationParams(compilationModes = listOf(CompilationMode.Full())).flatMap {
                compilationMode ->
                PokedexSharedElementBenchmarkConfiguration.AllConfigurations.map { configuration ->
                    arrayOf(*compilationMode, *configuration.asBenchmarkArguments())
                }
            }
    }
}

private fun <U> UiDevice.waitOrThrow(condition: SearchCondition<U>, timeout: Long): U =
    requireNotNull(wait(condition, timeout))
