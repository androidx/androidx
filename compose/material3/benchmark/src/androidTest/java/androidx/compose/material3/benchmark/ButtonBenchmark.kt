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

import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ButtonBenchmark(private val type: ButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}") @JvmStatic fun parameters() = ButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val buttonTestCaseFactory = { ButtonTestCase(type) }

    @Ignore
    @Test
    fun button_first_compose() {
        benchmarkRule.benchmarkFirstCompose(buttonTestCaseFactory)
    }

    @Ignore
    @Test
    fun button_measure() {
        benchmarkRule.benchmarkFirstMeasure(buttonTestCaseFactory)
    }

    @Ignore
    @Test
    fun button_layout() {
        benchmarkRule.benchmarkFirstLayout(buttonTestCaseFactory)
    }

    @Ignore
    @Test
    fun button_draw() {
        benchmarkRule.benchmarkFirstDraw(buttonTestCaseFactory)
    }

    @Test
    fun button_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(buttonTestCaseFactory)
    }
}

internal class ButtonTestCase(private val type: ButtonType) : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        when (type) {
            ButtonType.FilledButton -> Button(onClick = { /* Do something! */ }) { Text("Button") }
            ButtonType.ElevatedButton ->
                ElevatedButton(onClick = { /* Do something! */ }) { Text("Elevated Button") }
            ButtonType.FilledTonalButton ->
                FilledTonalButton(onClick = { /* Do something! */ }) { Text("Filled Tonal Button") }
            ButtonType.OutlinedButton ->
                OutlinedButton(onClick = { /* Do something! */ }) { Text("Outlined Button") }
            ButtonType.TextButton ->
                TextButton(onClick = { /* Do something! */ }) { Text("Text Button") }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class ButtonType {
    FilledButton,
    ElevatedButton,
    FilledTonalButton,
    OutlinedButton,
    TextButton
}
