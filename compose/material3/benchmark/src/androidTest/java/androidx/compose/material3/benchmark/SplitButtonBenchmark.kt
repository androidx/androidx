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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class SplitButtonTestCase(private val type: SplitButtonType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var trailingButtonChecked = mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        when (type) {
            SplitButtonType.Filled ->
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = { /* Do Nothing */ },
                        ) {
                            leadingContent()
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TrailingButton(
                            checked = trailingButtonChecked.value,
                            onCheckedChange = { /* Do Nothing */ },
                        ) {
                            trailingContent()
                        }
                    }
                )
            SplitButtonType.Tonal ->
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.TonalLeadingButton(
                            onClick = { /* Do Nothing */ },
                        ) {
                            leadingContent()
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TonalTrailingButton(
                            checked = trailingButtonChecked.value,
                            onCheckedChange = { /* Do Nothing */ },
                        ) {
                            trailingContent()
                        }
                    }
                )
            SplitButtonType.Elevated ->
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.ElevatedLeadingButton(
                            onClick = { /* Do Nothing */ },
                        ) {
                            leadingContent()
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.ElevatedTrailingButton(
                            checked = trailingButtonChecked.value,
                            onCheckedChange = { /* Do Nothing */ },
                        ) {
                            trailingContent()
                        }
                    }
                )
            SplitButtonType.Outlined ->
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.OutlinedLeadingButton(
                            onClick = { /* Do Nothing */ },
                        ) {
                            leadingContent()
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.OutlinedTrailingButton(
                            checked = trailingButtonChecked.value,
                            onCheckedChange = { /* Do Nothing */ },
                        ) {
                            trailingContent()
                        }
                    }
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        trailingButtonChecked.value = !trailingButtonChecked.value
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
    Filled,
    Tonal,
    Elevated,
    Outlined,
}
