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
import androidx.benchmark.macro.TraceSectionMetric
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
    private val benchmarkRule = MacrobenchmarkRule()

    private val databaseCleanupRule = PokedexDatabaseCleanupRule()

    @get:Rule
    val pokedexBenchmarkRuleChain: RuleChain =
        RuleChain.outerRule(databaseCleanupRule).around(benchmarkRule)

    private val FirstPokemonToClickOn = "Ablazeon"
    private val SecondPokemonToClickOn = "Amphibyte"

    @OptIn(ExperimentalMetricApi::class)
    private val transitionDurationMetrics =
        listOf(
            TraceSectionMetric(
                "Pokedex Details Navigation Transition",
                label = "pokedexDetailsNavigationTransitionDuration",
                mode = TraceSectionMetric.Mode.Average,
            ),
            TraceSectionMetric(
                "Pokedex Details Navigation Transition",
                label = "pokedexDetailsNavigationTransitionDuration",
                mode = TraceSectionMetric.Mode.Count,
            ),
        )

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun homeToDetailsTransition() =
        benchmarkTransition(
            compilationMode,
            action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_COMPOSE_ACTIVITY",
            waitForProgressBarAnimation = true,
        )

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun homeToDetailsTransitionViews() =
        benchmarkTransition(
            compilationMode,
            action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_VIEWS_HOME_ACTIVITY",
            waitForProgressBarAnimation = false,
        )

    @OptIn(ExperimentalMetricApi::class)
    private fun benchmarkTransition(
        compilationMode: CompilationMode,
        action: String,
        waitForProgressBarAnimation: Boolean,
        enableSharedTransitionScope: Boolean = this.enableSharedTransitionScope,
        enableSharedElementTransitions: Boolean = this.enableSharedElementTransitions,
        iterations: Int = HeroMacrobenchmarkDefaults.ITERATIONS,
    ) =
        benchmarkRule.measureRepeated(
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            metrics =
                defaultComposeScrollingMetrics() +
                    FrameTimingGfxInfoMetric() +
                    transitionDurationMetrics,
            compilationMode = compilationMode,
            iterations = iterations,
            setupBlock = {
                device.pressHome()
                device.waitForIdle()
                databaseCleanupRule.deleteDatabaseFiles()

                val intent = Intent()
                intent.action = action
                intent.putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
                intent.putExtra(
                    POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS,
                    enableSharedElementTransitions,
                )
                startActivityAndWait(intent)

                device.waitOrThrow(Until.hasObject(By.text(FirstPokemonToClickOn)), 3_000)
                val content = device.findObjectOrThrow(byResContains("PokedexList"))
                // Set gesture margin to avoid triggering gesture navigation
                content.setGestureMargin(device.displayWidth / 5)
            },
        ) {
            homeToDetailsAndBackAction(
                FirstPokemonToClickOn,
                backButtonSelector = byResContains("pokedexDetailsBack"),
                waitForProgressBarAnimation = waitForProgressBarAnimation,
            )
            device.waitForIdle()
            homeToDetailsAndBackAction(
                SecondPokemonToClickOn,
                backButtonSelector = byResContains("pokedexDetailsBack"),
                waitForProgressBarAnimation = waitForProgressBarAnimation,
            )
        }

    private fun MacrobenchmarkScope.homeToDetailsAndBackAction(
        pokemonName: String,
        backButtonSelector: BySelector,
        waitForProgressBarAnimation: Boolean,
    ) {
        device.findObjectOrThrow(By.text(pokemonName)).click()
        device.waitForTransitionStatus(name = "details", active = false, 1500)

        if (waitForProgressBarAnimation) {
            device.waitOrThrow(
                Until.hasObject(byResContains("progress-animation-active-false")),
                2000,
            )
        }
        device.waitForIdle()

        device.findObjectOrThrow(backButtonSelector).click()

        device.waitForTransitionStatus("home", active = false, 1500)
        // Wait until we're back on the pokedex list/home screen
        device.waitOrThrow(Until.hasObject(By.text(pokemonName)), 1_000)
    }

    private fun UiDevice.waitForTransitionStatus(
        name: String,
        active: Boolean,
        timeoutMs: Long = 2_000L,
    ) {
        // TODO (b/439803128): Investigate why UiAutomator is not picking up the transition tag in
        //  the Compose benchmarks. We're using the lenient wait method for now.
        wait(Until.hasObject(By.text("pokedex-$name-transition-active-$active")), timeoutMs)
    }

    companion object {

        /**
         * Parameters for the benchmark. Uses abbreviations because of file length limit for
         * results. We use CompilationMode.Full() in CI to reduce the amount of benchmark
         * permutations. compilation = Compilation Mode eSTS = enableSharedTransitionScope eSET =
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
