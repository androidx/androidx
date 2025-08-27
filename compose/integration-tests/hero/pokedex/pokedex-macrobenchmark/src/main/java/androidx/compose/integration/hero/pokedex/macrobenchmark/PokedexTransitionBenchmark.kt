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
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
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
class PokedexTransitionBenchmark(
    val compilationMode: CompilationMode,
    val enableSharedTransitionScope: Boolean,
    val enableSharedElementTransitions: Boolean,
) {
    val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val pokedexBenchmarkRuleChain: RuleChain =
        RuleChain.outerRule(PokedexDatabaseCleanupRule()).around(benchmarkRule)

    private val FirstPokemonToClickOn = "Ablazeon"
    private val SecondPokemonToClickOn = "Astrobat"

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun homeToDetailsTransition() {
        benchmarkRule.measureRepeated(
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics() + FrameTimingGfxInfoMetric(),
            compilationMode = compilationMode,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
            setupBlock = {
                val intent = Intent()
                intent.action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_COMPOSE_ACTIVITY"
                intent.putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
                intent.putExtra(
                    POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS,
                    enableSharedElementTransitions,
                )
                startActivityAndWait(intent)

                // Ablazeon always is the first pokemon in the grid
                device.waitOrThrow(Until.hasObject(By.text(FirstPokemonToClickOn)), 3_000)
                val content = device.findObjectOrThrow(By.res("PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
        ) {
            homeToDetailsAndBackAction(
                FirstPokemonToClickOn,
                waitForActiveTransitionStatus = true,
                waitForProgressBarAnimation = true,
                backButtonSelector = By.res("pokedexDetailsBack"),
            )
            // Wait until we're back on the pokedex list/home screen
            device.waitOrThrow(Until.hasObject(By.text(SecondPokemonToClickOn)), 1_000)
            homeToDetailsAndBackAction(
                SecondPokemonToClickOn,
                waitForActiveTransitionStatus = true,
                waitForProgressBarAnimation = true,
                backButtonSelector = By.res("pokedexDetailsBack"),
            )
        }
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun homeToDetailsTransitionViews() {
        benchmarkRule.measureRepeated(
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            metrics = defaultComposeScrollingMetrics() + FrameTimingGfxInfoMetric(),
            compilationMode = compilationMode,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
            setupBlock = {
                val intent = Intent()
                intent.action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_VIEWS_HOME_ACTIVITY"
                intent.putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
                intent.putExtra(
                    POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS,
                    enableSharedElementTransitions,
                )
                startActivityAndWait(intent)

                device.waitOrThrow(Until.hasObject(By.text(FirstPokemonToClickOn)), 3_000)
                val content =
                    device.findObjectOrThrow(By.res(POKEDEX_TARGET_PACKAGE_NAME, "PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
        ) {
            homeToDetailsAndBackAction(
                FirstPokemonToClickOn,
                waitForActiveTransitionStatus = false,
                waitForProgressBarAnimation = false,
                backButtonSelector = By.res(POKEDEX_TARGET_PACKAGE_NAME, "pokedexDetailsBack"),
            )
            device.waitOrThrow(Until.hasObject(By.text(SecondPokemonToClickOn)), 1_000)
            homeToDetailsAndBackAction(
                SecondPokemonToClickOn,
                waitForActiveTransitionStatus = false,
                waitForProgressBarAnimation = false,
                backButtonSelector = By.res(POKEDEX_TARGET_PACKAGE_NAME, "pokedexDetailsBack"),
            )
            device.waitOrThrow(Until.hasObject(By.text(SecondPokemonToClickOn)), 1_000)
        }
    }

    private fun MacrobenchmarkScope.homeToDetailsAndBackAction(
        pokemonName: String,
        backButtonSelector: BySelector,
        waitForActiveTransitionStatus: Boolean,
        waitForProgressBarAnimation: Boolean,
    ) {
        device.findObjectOrThrow(By.text(pokemonName)).click()
        device.waitForIdle()
        if (enableSharedElementTransitions && waitForActiveTransitionStatus) {
            device.waitForTransitionStatus(name = "details", active = true)
        }
        device.waitForTransitionStatus(name = "details", active = false)

        if (waitForProgressBarAnimation) {
            device.waitOrThrow(
                condition = Until.hasObject(By.res("progress-animation-active-true")),
                timeoutMillis = 1000,
            )
            device.waitOrThrow(Until.gone(By.res("progress-animation-active-true")), 1000)
        }

        device.findObjectOrThrow(backButtonSelector).click()
        device.waitForIdle()

        if (enableSharedElementTransitions && waitForActiveTransitionStatus) {
            device.waitForTransitionStatus("home", active = true)
        }
        device.waitForTransitionStatus("home", active = false)
    }

    private fun UiDevice.waitForTransitionStatus(
        name: String,
        active: Boolean,
        timeoutMs: Long = 2_000L,
    ) {
        waitOrThrow(Until.hasObject(By.text("pokedex-$name-transition-active-$active")), timeoutMs)
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
