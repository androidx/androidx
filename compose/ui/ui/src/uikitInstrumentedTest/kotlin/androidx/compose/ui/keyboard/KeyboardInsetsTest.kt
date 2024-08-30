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

package androidx.compose.ui.keyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.toDpRect
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardInsetsTest {
    @Test
    fun testBottomIMEInsets() = runUIKitInstrumentedTest {
        var contentFrame: DpRect? = null

        setContent {
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .onGloballyPositioned { coordinates ->
                    contentFrame = coordinates.boundsInWindow().toDpRect(density)
                }
            )
            LaunchedEffect(Unit) {
                showKeyboard(animated = false)
            }
        }

        assertEquals(screenSize.height - keyboardHeight, contentFrame?.height)
    }

    @Test
    fun testFocusableAboveKeyboardTextFieldLocation() = runUIKitInstrumentedTest {
        var textRectInWindow: DpRect? = null
        var textRectInRoot: DpRect? = null

        val focusRequester = FocusRequester()
        setContent({
            this.onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onGloballyPositioned { coordinates ->
                            textRectInWindow = coordinates.boundsInWindow().toDpRect(density)
                            textRectInRoot = coordinates.boundsInRoot().toDpRect(density)
                        }
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                showKeyboard(animated = false)
            }
        }

        assertEquals(screenSize.height - keyboardHeight, textRectInWindow?.bottom)
        assertEquals(screenSize.height, textRectInRoot?.bottom)

        focusRequester.freeFocus()
        hideKeyboard(animated = false)

        waitForIdle()
        assertEquals(screenSize.height, textRectInWindow?.bottom)
        assertEquals(screenSize.height, textRectInRoot?.bottom)
    }
}
