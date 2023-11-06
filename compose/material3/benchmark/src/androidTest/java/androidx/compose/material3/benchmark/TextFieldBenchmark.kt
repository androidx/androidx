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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TextFieldBenchmark(private val type: TextFieldType) {
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = TextFieldType.values()
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val textFieldTestCaseFactory = { TextFieldTestCase(type) }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(textFieldTestCaseFactory)
    }

    @Test
    fun enterText() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = textFieldTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

internal class TextFieldTestCase(
    private val type: TextFieldType
) : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: MutableState<String>

    @Composable
    override fun MeasuredContent() {
        state = remember { mutableStateOf("") }

        when (type) {
            TextFieldType.Filled ->
                TextField(
                    value = state.value,
                    onValueChange = {},
                )
            TextFieldType.Outlined ->
                OutlinedTextField(
                    value = state.value,
                    onValueChange = {},
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }

    override fun toggleState() {
        state.value = if (state.value.isEmpty()) "Lorem ipsum" else ""
    }
}

enum class TextFieldType {
    Filled, Outlined
}
