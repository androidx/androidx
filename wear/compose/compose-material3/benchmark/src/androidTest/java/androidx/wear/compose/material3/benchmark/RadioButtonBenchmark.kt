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

package androidx.wear.compose.material3.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RadioButtonBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val radioButtonTestCaseFactory = { RadioButtonTestCase(RadioButtonType.RadioButton) }

    private val splitRadioButtonTestCaseFactory = {
        RadioButtonTestCase(RadioButtonType.SplitRadioButton)
    }

    @Test
    fun radio_button_first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(radioButtonTestCaseFactory)
    }

    @Test
    fun split_radio_button_first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(splitRadioButtonTestCaseFactory)
    }
}

internal class RadioButtonTestCase(private val type: RadioButtonType) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        if (type == RadioButtonType.RadioButton) {
            RadioButton(selected = true, onSelect = { /* do something*/ }) {
                Text(text = "RadioButton")
            }
        } else {
            SplitRadioButton(
                selected = true,
                onSelectionClick = { /* do something */ },
                onContainerClick = { /* do something */ },
                selectionContentDescription = ""
            ) {
                Text(text = "SplitRadioButton")
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class RadioButtonType {
    RadioButton,
    SplitRadioButton
}
