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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationStyleApi::class)
@LargeTest
@RunWith(Parameterized::class)
class StyledButtonBenchmark(private val type: StyledButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = StyledButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { StyledButtonTestCase(type) }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(testCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(testCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(testCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(testCaseFactory)
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(testCaseFactory)
    }

    @Test
    fun toggle_recomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            testCaseFactory,
            assertOneRecomposition = type == StyledButtonType.Material3Button,
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
internal class StyledButtonTestCase(private val type: StyledButtonType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private val enabled = mutableStateOf(true)

    @Composable
    override fun MeasuredContent() {
        val interactionSource = remember { MutableInteractionSource() }
        when (type) {
            StyledButtonType.Material3Button -> {
                Button(
                    onClick = {},
                    enabled = enabled.value,
                    interactionSource = interactionSource,
                ) {
                    Text("Button")
                }
            }

            StyledButtonType.StyledButton -> {
                val buttonStyle = Style {
                    val theme = MaterialTheme.LocalMaterialTheme.currentValue
                    background(theme.colorScheme.primary)
                    contentColor(theme.colorScheme.onPrimary)
                    textStyle(theme.typography.labelLarge)
                    shape(theme.shapes.medium)
                    contentPadding(8.dp)
                    minWidth(ButtonDefaults.MinWidth)
                    minHeight(ButtonDefaults.MinHeight)

                    disabled {
                        val innerTheme = MaterialTheme.LocalMaterialTheme.currentValue
                        background(innerTheme.colorScheme.secondary)
                    }
                }

                val state =
                    rememberUpdatedStyleState(interactionSource = interactionSource) {
                        it.isEnabled = enabled.value
                    }
                Row(
                    modifier =
                        Modifier.styleable(state, style = buttonStyle)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                            ) {}
                            .shadow(4.dp, shape = MaterialTheme.shapes.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Button")
                }
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        enabled.value = !enabled.value
    }
}

enum class StyledButtonType {
    Material3Button,
    StyledButton,
}
