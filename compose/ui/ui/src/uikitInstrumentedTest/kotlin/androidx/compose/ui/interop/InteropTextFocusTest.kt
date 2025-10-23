/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import platform.UIKit.UITextField

class InteropTextFocusTest {
    @Test
    fun testFocusDropsAfterNativeViewFocused() = runUIKitInstrumentedTestWithInterop { overlay ->
        val nativeTextField = UITextField()
        val requester = FocusRequester()
        val positions = mutableListOf<Rect>()
        var isFocused = false
        setContent {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().onGloballyPositioned {
                positions.add(it.boundsInWindow())
            }) {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(requester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                )
                UIKitView(
                    factory = { nativeTextField },
                    modifier = Modifier.height(40.dp).fillMaxWidth(),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        requester.requestFocus()
        waitForIdle()

        assertTrue(isFocused)
        assertNotEquals(0.dp, keyboardHeight)

        positions.clear()

        nativeTextField.becomeFirstResponder()
        waitForIdle()

        assertTrue(nativeTextField.isFirstResponder)
        assertFalse(isFocused)

        requester.requestFocus()
        waitForIdle()

        assertFalse(nativeTextField.isFirstResponder)
        assertTrue(isFocused)

        assertTrue(
            actual = positions.all { it == positions.first() },
            message = "Keyboard frame should not change when refocus on native"
        )
    }
}