/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.benchmark.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.benchmark.RandomTextGenerator
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.awaitCancellation

@OptIn(ExperimentalComposeUiApi::class)
class BasicTextField2ToggleTextTestCase(
    private val textGenerator: RandomTextGenerator,
    private val textLength: Int,
    private val textNumber: Int,
    private val width: Dp,
    private val fontSize: TextUnit
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val states =
        List(textNumber) { TextFieldState(textGenerator.nextParagraph(length = textLength)) }

    @Composable
    override fun MeasuredContent() {
        for (state in states) {
            BasicTextField(
                state = state,
                textStyle = TextStyle(color = Color.Black, fontSize = fontSize),
                modifier = Modifier.background(color = Color.Cyan).requiredWidth(width)
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        Column(modifier = Modifier.width(width).verticalScroll(rememberScrollState())) {
            InterceptPlatformTextInput(
                interceptor = { _, _ -> awaitCancellation() },
                content = content
            )
        }
    }

    override fun toggleState() {
        states.forEach {
            it.setTextAndPlaceCursorAtEnd(textGenerator.nextParagraph(length = textLength))
        }
    }
}
