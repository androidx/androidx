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
import androidx.test.filters.MediumTest
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ButtonBenchmark(private val buttonType: ButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = ButtonType.values()
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val buttonTestCaseFactory = { ButtonTestCase(buttonType) }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(buttonTestCaseFactory)
    }
}

internal class ButtonTestCase(
    private val buttonType: ButtonType
) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        when (buttonType) {
            ButtonType.FilledButton ->
                Button(onClick = { /* do something */ }) { Text("Button") }
            ButtonType.FilledTonalButton ->
                FilledTonalButton(onClick = { /* do something */ }) { Text("Filled Tonal Button") }
            ButtonType.OutlinedButton ->
                OutlinedButton(onClick = { /* do something */ }) { Text("Outlined Button") }
            ButtonType.ChildButton ->
                ChildButton(onClick = { /* do something */ }) { Text("Child Button") }
            ButtonType.CompactButton ->
                CompactButton(onClick = { /* do something */ }) { Text("Compact Button") }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

enum class ButtonType {
    FilledButton, FilledTonalButton, OutlinedButton, ChildButton, CompactButton
}
