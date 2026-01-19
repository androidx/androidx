/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation3.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import org.junit.Rule

class NavDisplayBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactory = {
        object : LayeredComposeTestCase(), ToggleableTestCase {
            var keyCount = 0
            val backStack = mutableStateListOf(keyCount++)

            @Composable
            override fun MeasuredContent() {
                NavDisplay(
                    backStack,
                    entryProvider = { key -> NavEntry(key) { Box(Modifier.fillMaxSize()) } },
                )
            }

            override fun toggleState() {
                backStack.add(keyCount++)
            }
        }
    }

    /** Measure the time taken to compose the NavDisplay from scratch. */
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(caseFactory)
    }

    /**
     * Measure the time taken to recompose the NavDisplay when the back stack state gets toggled.
     */
    @Test
    fun toggleState_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(caseFactory, assertOneRecomposition = false)
    }
}
