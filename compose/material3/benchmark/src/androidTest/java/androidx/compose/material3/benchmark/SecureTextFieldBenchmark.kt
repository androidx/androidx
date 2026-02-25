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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.SecureTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class SecureTextFieldBenchmark(private val type: TextFieldType) {
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = TextFieldType.entries.toTypedArray()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val textFieldTestCaseFactory = { SecureTextFieldTestCase(type) }

    @Test
    fun focus_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            caseFactory = textFieldTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun focus_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            caseFactory = textFieldTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun focus_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            caseFactory = textFieldTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

internal class SecureTextFieldTestCase(private val type: TextFieldType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: TextFieldState
    private lateinit var isFocused: State<Boolean>
    private val interactionSource = MutableInteractionSource()

    private val focusRequester: FocusRequester = FocusRequester()
    private lateinit var focusManager: FocusManager

    @Composable
    override fun MeasuredContent() {
        state = rememberTextFieldState()

        val modifier = Modifier.focusRequester(focusRequester)
        when (type) {
            TextFieldType.Filled ->
                SecureTextField(
                    state = state,
                    modifier = modifier,
                    interactionSource = interactionSource,
                )

            TextFieldType.Outlined ->
                OutlinedSecureTextField(
                    state = state,
                    modifier = modifier,
                    interactionSource = interactionSource,
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        focusManager = LocalFocusManager.current
        isFocused = interactionSource.collectIsFocusedAsState()
        MaterialTheme {
            // Additional element that can steal focus
            Column {
                Box(Modifier.size(1.dp).focusable())
                content()
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
