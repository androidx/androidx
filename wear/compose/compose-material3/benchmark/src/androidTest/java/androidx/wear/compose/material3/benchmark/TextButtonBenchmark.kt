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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class TextButtonBenchmark(private val textButtonType: TextButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = TextButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { TextButtonTestCase(textButtonType) }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(testCaseFactory)
    }
}

internal class TextButtonTestCase(private val textButtonType: TextButtonType) :
    LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        when (textButtonType) {
            TextButtonType.FilledTextButton ->
                TextButton(onClick = {}, colors = TextButtonDefaults.filledTextButtonColors()) {
                    Text("ABC")
                }
            TextButtonType.FilledTonalTextButton ->
                TextButton(
                    onClick = {},
                    colors = TextButtonDefaults.filledTonalTextButtonColors()
                ) {
                    Text("ABC")
                }
            TextButtonType.OutlinedTextButton ->
                TextButton(onClick = {}, colors = TextButtonDefaults.outlinedTextButtonColors()) {
                    Text("ABC")
                }
            TextButtonType.TextButton ->
                TextButton(onClick = {}, colors = TextButtonDefaults.textButtonColors()) {
                    Text("ABC")
                }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class TextButtonType {
    FilledTextButton,
    FilledTonalTextButton,
    OutlinedTextButton,
    TextButton
}
