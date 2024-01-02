/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class RangeSliderBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val sliderTestCaseFactory = { RangeSliderTestCase() }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(sliderTestCaseFactory)
    }

    @Test
    fun moveThumb() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = sliderTestCaseFactory
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class RangeSliderTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private lateinit var state: RangeSliderState

    @Composable
    override fun MeasuredContent() {
        state = remember { RangeSliderState(steps = 15) }

        RangeSlider(
            state = state,
            startThumb = {
                Spacer(
                    Modifier
                        .size(48.dp)
                        .background(color = MaterialTheme.colorScheme.primary)
                        .clip(CircleShape)
                        .shadow(10.dp, CircleShape)
                )
            })
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }

    override fun toggleState() {
        if (state.activeRangeStart == 0f) {
            state.activeRangeStart = .7f
        } else {
            state.activeRangeStart = 0f
        }
    }
}
