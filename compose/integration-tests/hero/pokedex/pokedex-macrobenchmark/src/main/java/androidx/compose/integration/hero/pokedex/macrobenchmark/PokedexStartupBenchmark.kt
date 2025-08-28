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

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.common.macrobenchmark.HeroMacrobenchmarkDefaults
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexDatabaseCleanupRule
import androidx.test.filters.LargeTest
import androidx.testutils.createStartupCompilationParams
import androidx.testutils.measureStartup
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PokedexStartupBenchmark(
    private val startupMode: StartupMode,
    private val compilation: CompilationMode,
    private val enableSharedTransitionScope: Boolean,
    private val enableSharedElementTransitions: Boolean,
) {
    val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val pokedexBenchmarkRuleChain: RuleChain =
        RuleChain.outerRule(PokedexDatabaseCleanupRule()).around(benchmarkRule)

    private fun measureStartup(action: String) =
        benchmarkRule.measureStartup(
            compilationMode = compilation,
            startupMode = startupMode,
            packageName = POKEDEX_TARGET_PACKAGE_NAME,
            iterations = HeroMacrobenchmarkDefaults.ITERATIONS,
        ) {
            this.action = action
            this.putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
            this.putExtra(POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS, enableSharedElementTransitions)
        }

    @Test
    fun startupCompose() = measureStartup("$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_COMPOSE_ACTIVITY")

    @Test
    fun startupViews() = measureStartup("$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_VIEWS_HOME_ACTIVITY")

    companion object {
        /**
         * Parameters for the benchmark. Uses abbreviations because of file length limit for
         * results. startup = Startup Mode compilation = Compilation Mode eSTS =
         * enableSharedTransitionScope eSET = enableSharedElementTransition
         */
        @Parameterized.Parameters(name = "startup={0},compilation={1},eSTS={2},eSET={3}")
        @JvmStatic
        fun parameters() =
            createStartupCompilationParams().flatMap { compilationMode ->
                PokedexSharedElementBenchmarkConfiguration.AllConfigurations.map { configuration ->
                    arrayOf(*compilationMode, *configuration.asBenchmarkArguments())
                }
            }
    }
}
