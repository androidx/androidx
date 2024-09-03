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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
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
class IconToggleButtonBenchmark(private val type: IconToggleButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = IconToggleButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val iconToggleButtonTestCaseFactory = { IconToggleButtonTestCase(type) }

    @Ignore
    @Test
    fun iconToggleButton_first_compose() {
        benchmarkRule.benchmarkFirstCompose(iconToggleButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconToggleButton_measure() {
        benchmarkRule.benchmarkFirstMeasure(iconToggleButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconToggleButton_layout() {
        benchmarkRule.benchmarkFirstLayout(iconToggleButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconToggleButton_draw() {
        benchmarkRule.benchmarkFirstDraw(iconToggleButtonTestCaseFactory)
    }

    @Test
    fun iconToggleButton_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(iconToggleButtonTestCaseFactory)
    }
}

internal class IconToggleButtonTestCase(private val type: IconToggleButtonType) :
    LayeredComposeTestCase(), ToggleableTestCase {

    private var state by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun MeasuredContent() {
        when (type) {
            IconToggleButtonType.IconToggleButton ->
                IconToggleButton(checked = state, onCheckedChange = { /* Do something! */ }) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.FilledIconToggleButton ->
                FilledIconToggleButton(checked = state, onCheckedChange = { /* Do something! */ }) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.FilledTonalIconToggleButton ->
                FilledTonalIconToggleButton(
                    checked = state,
                    onCheckedChange = { /* Do something! */ }
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.OutlinedIconToggleButton ->
                OutlinedIconToggleButton(
                    checked = state,
                    onCheckedChange = { /* Do something! */ }
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.IconToggleButtonExpressive ->
                IconToggleButton(
                    checked = state,
                    onCheckedChange = { /* Do something! */ },
                    shapes =
                        IconButtonShapes(
                            shape = IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape,
                            checkedShape = IconButtonDefaults.smallSquareShape
                        )
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.FilledIconToggleButtonExpressive ->
                FilledIconToggleButton(
                    checked = state,
                    onCheckedChange = { /* Do something! */ },
                    shapes =
                        IconButtonShapes(
                            shape = IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape,
                            checkedShape = IconButtonDefaults.smallSquareShape
                        )
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.FilledTonalIconToggleButtonExpressive ->
                FilledTonalIconToggleButton(
                    checked = state,
                    onCheckedChange = { /* Do something! */ },
                    shapes =
                        IconButtonShapes(
                            shape = IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape,
                            checkedShape = IconButtonDefaults.smallSquareShape
                        )
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconToggleButtonType.OutlinedIconToggleButtonExpressive ->
                OutlinedIconToggleButton(
                    checked = state,
                    onCheckedChange = { /* Do something! */ },
                    shapes =
                        IconButtonShapes(
                            shape = IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape,
                            checkedShape = IconButtonDefaults.smallSquareShape
                        )
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
        }
    }

    override fun toggleState() {
        state = !state
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class IconToggleButtonType {
    IconToggleButton,
    FilledIconToggleButton,
    FilledTonalIconToggleButton,
    OutlinedIconToggleButton,
    IconToggleButtonExpressive,
    FilledIconToggleButtonExpressive,
    FilledTonalIconToggleButtonExpressive,
    OutlinedIconToggleButtonExpressive
}
