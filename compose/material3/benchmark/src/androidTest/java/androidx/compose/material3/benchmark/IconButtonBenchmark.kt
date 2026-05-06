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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class IconButtonBenchmark(
    private val type: IconButtonType,
    private val rippleConfig: RippleConfigType,
) {

    companion object {
        @Parameterized.Parameters(name = "{0}, {1}")
        @JvmStatic
        fun parameters(): List<Array<Any>> {
            val result = mutableListOf<Array<Any>>()
            for (type in IconButtonType.values()) {
                for (rippleConfig in RippleConfigType.values()) {
                    result.add(arrayOf(type, rippleConfig))
                }
            }
            return result
        }
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val iconButtonTestCaseFactory = { IconButtonTestCase(type, rippleConfig) }

    @Ignore
    @Test
    fun iconButton_first_compose() {
        benchmarkRule.benchmarkFirstCompose(iconButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconButton_measure() {
        benchmarkRule.benchmarkFirstMeasure(iconButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconButton_layout() {
        benchmarkRule.benchmarkFirstLayout(iconButtonTestCaseFactory)
    }

    @Test
    fun iconButton_draw() {
        benchmarkRule.benchmarkFirstDraw(iconButtonTestCaseFactory)
    }

    @Test
    fun iconButton_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(iconButtonTestCaseFactory)
    }

    @Test
    fun iconButton_focus_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            iconButtonTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun iconButton_focus_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            iconButtonTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun iconButton_focus_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            iconButtonTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

internal class IconButtonTestCase(
    private val type: IconButtonType,
    private val rippleConfig: RippleConfigType,
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val focusRequester = FocusRequester()
    private val interactionSource = MutableInteractionSource()
    private lateinit var focusManager: FocusManager
    private lateinit var isFocused: State<Boolean>

    @Composable
    override fun MeasuredContent() {
        val modifier = Modifier.focusRequester(focusRequester)
        when (type) {
            IconButtonType.IconButton ->
                IconButton(
                    onClick = { /* Do something! */ },
                    modifier = modifier,
                    interactionSource = interactionSource,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconButtonType.FilledIconButton ->
                FilledIconButton(
                    onClick = { /* Do something! */ },
                    modifier = modifier,
                    interactionSource = interactionSource,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconButtonType.FilledTonalIconButton ->
                FilledTonalIconButton(
                    onClick = { /* Do something! */ },
                    modifier = modifier,
                    interactionSource = interactionSource,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
            IconButtonType.OutlinedIconButton ->
                OutlinedIconButton(
                    onClick = { /* Do something! */ },
                    modifier = modifier,
                    interactionSource = interactionSource,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
                }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        focusManager = LocalFocusManager.current
        isFocused = interactionSource.collectIsFocusedAsState()
        val themeConfig =
            when (rippleConfig) {
                RippleConfigType.Opacity -> RippleDefaults.OpacityFocusRippleThemeConfiguration
                RippleConfigType.InsetFocusRing ->
                    RippleDefaults.InsetFocusRingRippleThemeConfiguration
            }
        MaterialTheme {
            CompositionLocalProvider(LocalRippleThemeConfiguration provides themeConfig) {
                Column {
                    Box(Modifier.size(1.dp).focusable())
                    content()
                }
            }
        }
    }

    override fun toggleState() {
        if (isFocused.value) {
            focusManager.clearFocus()
        } else {
            focusRequester.requestFocus()
        }
    }
}

enum class IconButtonType {
    IconButton,
    FilledIconButton,
    FilledTonalIconButton,
    OutlinedIconButton,
}

enum class RippleConfigType {
    Opacity,
    InsetFocusRing,
}
