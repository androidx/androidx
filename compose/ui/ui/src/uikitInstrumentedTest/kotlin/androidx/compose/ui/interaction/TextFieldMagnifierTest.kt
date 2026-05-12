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

package androidx.compose.ui.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.findFirstDescendant
import androidx.compose.ui.test.utils.isLoupeView
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
class TextFieldMagnifierTest {

    private val params = listOf<TextFieldComposableFactory>(
        { fr -> BFT(fr) },
        { fr -> BFT2(fr) }
    )

    @Test
    fun testMagnifierShownOnTouchAndHold() = runUIKitInstrumentedTest(
        params = params
    ) { factory ->
        val focusRequester = FocusRequester()

        setContent {
            Column {
                Box(Modifier.height(200.dp).fillMaxWidth())
                factory(focusRequester)
            }
        }

        focusRequester.requestFocus()

        waitForIdle()

        findNodeWithTag("textField").touchDown()

        waitUntil {
            findFirstDescendant { it.isLoupeView } != null
        }
    }

    @Test
    fun testMagnifierHidesOnLift() = runUIKitInstrumentedTest(
        params = params
    ) { factory ->
        val focusRequester = FocusRequester()

        setContent {
            Column {
                Box(Modifier.height(200.dp).fillMaxWidth())
                factory(focusRequester)
            }
        }

        focusRequester.requestFocus()

        waitForIdle()

        val touch = findNodeWithTag("textField").touchDown()

        waitUntil {
            findFirstDescendant { it.isLoupeView } != null
        }

        touch.up()

        waitUntil {
            findFirstDescendant { it.isLoupeView } == null
        }
    }

    @Test
    fun testMagnifierHidesOnDragOutsideTextField() = runUIKitInstrumentedTest(
        params = params
    ) { factory ->
        val focusRequester = FocusRequester()

        setContent {
            Column {
                Box(Modifier.height(200.dp).fillMaxWidth())
                factory(focusRequester)
            }
        }

        focusRequester.requestFocus()

        waitForIdle()

        val touch = findNodeWithTag("textField").touchDown()

        waitUntil {
            findFirstDescendant { it.isLoupeView } != null
        }

        touch.dragBy(dy = 100.dp, duration = 0.1.seconds)

        waitUntil {
            findFirstDescendant { it.isLoupeView } == null
        }
    }

    private val textValue = "TEXT"
    private val keyboardOptions = KeyboardOptions(
        platformImeOptions = PlatformImeOptions {
            usingNativeTextInput(false)
        }
    )
    private fun modifier(focusRequester: FocusRequester): Modifier = Modifier
        .testTag("textField")
        .height(40.dp)
        .fillMaxWidth()
        .focusRequester(focusRequester)

    @Composable
    private fun BFT(
        focusRequester: FocusRequester
    ) = BasicTextField(
        textValue,
        onValueChange = {},
        keyboardOptions = keyboardOptions,
        modifier = modifier(focusRequester)
    )

    @Composable
    private fun BFT2(
        focusRequester: FocusRequester
    ) {
        val state = remember { TextFieldState(textValue) }
        BasicTextField(state, keyboardOptions = keyboardOptions, modifier = modifier(focusRequester))
    }
}

private typealias TextFieldComposableFactory = @Composable (FocusRequester) -> Unit
