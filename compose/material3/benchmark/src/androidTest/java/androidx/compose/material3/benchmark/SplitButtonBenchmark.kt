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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedSplitButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledSplitButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSplitButton
import androidx.compose.material3.SplitButton
import androidx.compose.material3.Text
import androidx.compose.material3.TonalSplitButton
import androidx.compose.runtime.Composable
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
class SplitButtonBenchmark(private val type: SplitButtonType) {
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = SplitButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val splitButtonTestCaseFactory = { SplitButtonTestCase(type) }

    @Ignore
    @Test
    fun splitButton_first_compose() {
        benchmarkRule.benchmarkFirstCompose(splitButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun splitButton_measure() {
        benchmarkRule.benchmarkFirstMeasure(splitButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun splitButton_layout() {
        benchmarkRule.benchmarkFirstLayout(splitButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun splitButton_draw() {
        benchmarkRule.benchmarkFirstDraw(splitButtonTestCaseFactory)
    }

    @Test
    fun splitButton_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(splitButtonTestCaseFactory)
    }
}

internal class SplitButtonTestCase(private val type: SplitButtonType) : LayeredComposeTestCase() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun MeasuredContent() {
        when (type) {
            SplitButtonType.SplitButton ->
                SplitButton(
                    leadingButton = {
                        Button(onClick = { /* Do something! */ }) { Text("Button") }
                    },
                    trailingButton = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Localized description",
                            )
                        }
                    }
                )
            SplitButtonType.Filled ->
                FilledSplitButton(
                    onLeadingButtonClick = {},
                    checked = false,
                    onTrailingButtonClick = { /* Do Nothing */ },
                    leadingContent = { leadingContent() },
                    trailingContent = { trailingContent() },
                )
            SplitButtonType.Tonal ->
                TonalSplitButton(
                    onLeadingButtonClick = {},
                    checked = false,
                    onTrailingButtonClick = { /* Do Nothing */ },
                    leadingContent = { leadingContent() },
                    trailingContent = { trailingContent() },
                )
            SplitButtonType.Elevated ->
                ElevatedSplitButton(
                    onLeadingButtonClick = {},
                    checked = false,
                    onTrailingButtonClick = { /* Do Nothing */ },
                    leadingContent = { leadingContent() },
                    trailingContent = { trailingContent() },
                )
            SplitButtonType.Outlined ->
                OutlinedSplitButton(
                    onLeadingButtonClick = {},
                    checked = false,
                    onTrailingButtonClick = { /* Do Nothing */ },
                    leadingContent = { leadingContent() },
                    trailingContent = { trailingContent() },
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

@Composable
private fun leadingContent() {
    Icon(
        Icons.Outlined.Edit,
        contentDescription = "Localized description",
    )
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text("My Button")
}

@Composable
private fun trailingContent() {
    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Localized description")
}

enum class SplitButtonType {
    SplitButton,
    Filled,
    Tonal,
    Elevated,
    Outlined,
}
