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

internal class PokedexSharedElementBenchmarkConfiguration
private constructor(
    val enableSharedTransitionScope: Boolean,
    val enableSharedElementTransitions: Boolean,
) {
    init {
        check(enableSharedTransitionScope || !enableSharedElementTransitions) {
            "Shared element transitions require shared transition scope to be enabled"
        }
    }

    companion object {
        fun disableSharedTransitionAndElement() =
            PokedexSharedElementBenchmarkConfiguration(
                enableSharedTransitionScope = false,
                enableSharedElementTransitions = false,
            )

        fun enableSharedTransitionScope() =
            PokedexSharedElementBenchmarkConfiguration(
                enableSharedTransitionScope = true,
                enableSharedElementTransitions = false,
            )

        fun enableSharedElement() =
            PokedexSharedElementBenchmarkConfiguration(
                enableSharedTransitionScope = true,
                enableSharedElementTransitions = true,
            )

        val AllConfigurations =
            listOf(
                disableSharedTransitionAndElement(),
                enableSharedTransitionScope(),
                enableSharedElement(),
            )
    }
}

/**
 * Returns a list of arguments for the benchmark with the following structure: {
 * enableSharedTransitionScope, enableSharedElementTransitions }
 */
internal fun PokedexSharedElementBenchmarkConfiguration.asBenchmarkArguments() =
    arrayOf(enableSharedTransitionScope, enableSharedElementTransitions)
