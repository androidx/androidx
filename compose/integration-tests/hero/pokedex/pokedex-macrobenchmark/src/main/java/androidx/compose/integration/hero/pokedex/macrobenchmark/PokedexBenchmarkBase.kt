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

package androidx.compose.integration.hero.pokedex.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_API_URL
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.Compose.POKEDEX_START_DESTINATION
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexDatabaseCleanupRule
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexMockServerRule
import org.junit.Rule
import org.junit.rules.RuleChain

abstract class PokedexBenchmarkBase {
    val benchmarkRule = MacrobenchmarkRule()
    val mockServerRule = PokedexMockServerRule()
    internal val databaseCleanupRule = PokedexDatabaseCleanupRule()

    @get:Rule
    val pokedexBenchmarkRuleChain: RuleChain =
        RuleChain.outerRule(databaseCleanupRule).around(mockServerRule).around(benchmarkRule)

    fun Intent.configure(
        action: String,
        enableSharedTransitionScope: Boolean,
        enableSharedElementTransitions: Boolean,
        startDestination: String? = null,
    ): Intent =
        this.apply {
            setAction(action)
            putExtra(POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE, enableSharedTransitionScope)
            putExtra(POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS, enableSharedElementTransitions)
            if (startDestination != null) {
                putExtra(POKEDEX_START_DESTINATION, startDestination)
            }
            putExtra(POKEDEX_API_URL, mockServerRule.url.toString())
        }
}
