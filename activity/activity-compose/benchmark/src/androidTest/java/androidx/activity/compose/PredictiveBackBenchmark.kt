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

package androidx.activity.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.recomposeAssertHadChanges
import androidx.compose.ui.Modifier
import kotlin.test.Test
import org.junit.Rule

class PredictiveBackBenchmark {
    @get:Rule val rule = ComposeBenchmarkRule()

    @Test
    fun benchmarkEnableSwitch() =
        rule.toggleStateBenchmarkRecompose({ PredictiveBackTestCase(initial = true) })

    @Test
    fun benchmarkDisableSwitch() =
        rule.toggleStateBenchmarkRecompose({ PredictiveBackTestCase(initial = false) })

    fun <T> ComposeBenchmarkRule.toggleStateBenchmarkRecompose(
        caseFactory: () -> T,
        assertOneRecomposition: Boolean = true,
        requireRecomposition: Boolean = true,
    ) where T : ComposeTestCase, T : ToggleableTestCase {
        runBenchmarkFor(caseFactory) {
            runOnUiThread { doFramesUntilNoChangesPending() }
            measureRepeatedOnUiThread {
                runWithMeasurementDisabled { getTestCase().toggleState() }
                if (requireRecomposition) {
                    recomposeAssertHadChanges()
                } else {
                    recompose()
                }
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
                runWithMeasurementDisabled {
                    getTestCase().toggleState()
                    doFramesUntilNoChangesPending()
                }
            }
        }
    }

    private class PredictiveBackTestCase(private val initial: Boolean) :
        ComposeTestCase, ToggleableTestCase {
        var enabled by mutableStateOf(initial)

        @Composable
        override fun Content() {
            PredictiveBackHandler(enabled) { it.collect {} }

            Box(Modifier.fillMaxSize())
        }

        override fun toggleState() {
            enabled = !enabled
        }
    }
}
