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

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ChipBenchmark(private val type: ChipType) {

    companion object {
        @Parameterized.Parameters(name = "{0}") @JvmStatic fun parameters() = ChipType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val chipTestCaseFactory = { ChipTestCase(type) }

    @Ignore
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(chipTestCaseFactory)
    }

    @Ignore
    @Test
    fun chip_measure() {
        benchmarkRule.benchmarkFirstMeasure(chipTestCaseFactory)
    }

    @Ignore
    @Test
    fun chip_layout() {
        benchmarkRule.benchmarkFirstLayout(chipTestCaseFactory)
    }

    @Ignore
    @Test
    fun chip_draw() {
        benchmarkRule.benchmarkFirstDraw(chipTestCaseFactory)
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(chipTestCaseFactory)
    }
}

internal class ChipTestCase(private val type: ChipType) : LayeredComposeTestCase() {

    private var selected by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        when (type) {
            ChipType.Assist ->
                AssistChip(
                    onClick = { /* Do something! */ },
                    label = { Text("Assist Chip") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Localized description",
                            Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            ChipType.ElevatedAssist ->
                ElevatedAssistChip(
                    onClick = { /* Do something! */ },
                    label = { Text("Assist Chip") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Localized description",
                            Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            ChipType.Filter ->
                FilterChip(
                    selected = selected,
                    onClick = { selected = !selected },
                    label = { Text("Filter chip") },
                    leadingIcon =
                        if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Localized Description",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                )
            ChipType.Input ->
                InputChip(
                    selected = selected,
                    onClick = { selected = !selected },
                    label = { Text("Input Chip") },
                )
            ChipType.Suggestion ->
                SuggestionChip(
                    onClick = { /* Do something! */ },
                    label = { Text("Suggestion Chip") },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Localized description",
                            Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class ChipType {
    Assist,
    ElevatedAssist,
    Filter,
    Input,
    Suggestion,
}
