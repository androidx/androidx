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

package androidx.compose.foundation.benchmark.text

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.text.input.TextFieldState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TextFieldStateBenchmark(val textLength: Int) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0}")
        fun initParameters(): Array<Any> = arrayOf(10, 500)
    }

    private fun createTestState(): TextFieldState {
        return TextFieldState("A".repeat(textLength))
    }

    @Test
    fun edit_empty() {
        val state = createTestState()
        benchmarkRule.measureRepeated { state.edit {} }
    }

    @Test
    fun edit_noChangeAfterEdit() {
        val state = createTestState()
        val index = state.text.length / 2
        benchmarkRule.measureRepeated {
            state.edit {
                replace(index, index, "B")
                replace(index, index + 1, "")
            }
        }
    }

    @Test
    fun edit_singleReplace_noGapMove() {
        val state = createTestState()
        val index = state.text.length / 2
        benchmarkRule.measureRepeated {
            state.edit { replace(index, index, "B") }
            runWithMeasurementDisabled {
                // Delete the inserted text
                state.edit { replace(index, index + 1, "") }
            }
        }
    }

    @Test
    fun edit_singleReplace_gapMove() {
        val state = createTestState()
        val insertionIndex = state.text.length / 2
        val gapIndex = (state.text.length / 4) * 3
        benchmarkRule.measureRepeated {
            runWithMeasurementDisabled {
                // Move the gap to the gapIndex.
                state.edit { replace(gapIndex, gapIndex, "B") }
            }
            state.edit { replace(insertionIndex, insertionIndex, "B") }
            runWithMeasurementDisabled {
                // Delete the inserted text.
                state.edit {
                    replace(insertionIndex, insertionIndex + 1, "")
                    replace(gapIndex, gapIndex + 1, "")
                }
            }
        }
    }
}
