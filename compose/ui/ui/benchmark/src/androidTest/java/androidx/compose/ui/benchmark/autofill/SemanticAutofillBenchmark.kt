/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.benchmark.autofill

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkCompose
import androidx.compose.ui.ComposeUiFlags.isSemanticAutofillEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class SemanticAutofillBenchmark(private val isAutofillEnabled: Boolean) {
    @OptIn(ExperimentalComposeUiApi::class)
    private val previousFlagValue = isSemanticAutofillEnabled

    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(MicrobenchmarkConfig(traceAppTagEnabled = true))

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Autofill enabled = {0}")
        fun data(): Collection<Array<Any>> {
            val testCases = mutableListOf<Array<Any>>()
            // Add a `false` parameter here and run locally to compare Autofill off vs on
            for (isAutofillEnabled in listOf(true)) {
                testCases.add(arrayOf(isAutofillEnabled))
            }
            return testCases
        }
    }

    @Test
    fun runChangingAutofillTextBenchmark() {
        @OptIn(ExperimentalComposeUiApi::class)
        isSemanticAutofillEnabled = isAutofillEnabled

        benchmarkRule.toggleStateBenchmarkCompose(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    private lateinit var state: MutableState<Boolean>

                    @Composable
                    override fun Content() {
                        state = remember { mutableStateOf(false) }
                        ChangingAutofillText(state = state)
                    }

                    override fun toggleState() {
                        state.value = !state.value
                    }
                }
            }
        )

        @OptIn(ExperimentalComposeUiApi::class)
        isSemanticAutofillEnabled = previousFlagValue
    }

    @Test
    fun runRemovableAutofillTextBenchmark() {
        @OptIn(ExperimentalComposeUiApi::class)
        isSemanticAutofillEnabled = isAutofillEnabled

        benchmarkRule.toggleStateBenchmarkCompose(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    private lateinit var state: MutableState<Boolean>

                    @Composable
                    override fun Content() {
                        state = remember { mutableStateOf(false) }
                        RemovableAutofillText(state = state)
                    }

                    override fun toggleState() {
                        state.value = !state.value
                    }
                }
            }
        )

        @OptIn(ExperimentalComposeUiApi::class)
        isSemanticAutofillEnabled = previousFlagValue
    }

    @Test
    fun runChangingAutofillFocusBenchmark() {
        @OptIn(ExperimentalComposeUiApi::class)
        isSemanticAutofillEnabled = isAutofillEnabled

        benchmarkRule.toggleStateBenchmarkCompose(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    private lateinit var state: MutableState<Boolean>

                    @Composable
                    override fun Content() {
                        state = remember { mutableStateOf(false) }
                        ChangingAutofillFocus(state = state)
                    }

                    override fun toggleState() {
                        state.value = !state.value
                    }
                }
            }
        )

        @OptIn(ExperimentalComposeUiApi::class)
        isSemanticAutofillEnabled = previousFlagValue
    }
}
