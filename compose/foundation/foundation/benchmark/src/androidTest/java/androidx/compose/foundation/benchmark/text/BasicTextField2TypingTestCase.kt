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

package androidx.compose.foundation.benchmark.text

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.awaitCancellation

/**
 * A single focused state-based [BasicTextField] whose input session is intercepted so the test can
 * drive the real [InputConnection] (the same object a soft keyboard talks to).
 */
@OptIn(ExperimentalComposeUiApi::class)
class BasicTextField2TypingTestCase(
    initialText: String,
    private val width: Dp,
    private val fontSize: TextUnit,
    singleLine: Boolean,
) : LayeredComposeTestCase() {
    val state = TextFieldState(initialText, TextRange(initialText.length))
    private val lineLimits =
        if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default
    private val focusRequester = FocusRequester()

    var inputConnection: InputConnection? = null
        private set

    @Composable
    override fun MeasuredContent() {
        BasicTextField(
            state = state,
            textStyle = TextStyle(color = Color.Black, fontSize = fontSize),
            lineLimits = lineLimits,
            // BasicTextField is autofillable by default. While an autofill session is active, every
            // text change sends the full text to the autofill service over binder, the same as
            // platform EditText. Opt out so that results do not depend on the device's autofill
            // service, and so that the benchmark loop does not fill the binder buffer.
            modifier =
                Modifier.width(width).focusRequester(focusRequester).semantics {
                    contentDataType = ContentDataType.None
                },
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        // Intercept bring-into-view requests above BasicTextField so they do not reach
        // AndroidComposeView's root BringIntoViewModifierNode, which calls
        // View.requestRectangleOnScreen (an IPC to WindowManagerService) on every cursor move.
        // BasicTextField's own internal scroll and bring-into-view responder still run.
        Column(modifier = Modifier.width(width).then(NoOpBringIntoViewElement)) {
            CompositionLocalProvider(
                // Disable the periodic cursor blink timer so background blink wakeups and draw
                // invalidations do not run during benchmark iterations.
                LocalCursorBlinkEnabled provides false,
                // The input session starts only when both the field and the window are focused.
                // Report the window as focused so the session starts immediately when the field
                // requests focus, without waiting for the test window to gain focus.
                LocalWindowInfo provides FocusedWindowInfo,
            ) {
                InterceptPlatformTextInput(
                    interceptor = { request, _ ->
                        // This does not start a platform input session or make a binder call.
                        // It does the opposite: the request is never passed on to the platform,
                        // so no IME connects to this field. Creating the InputConnection here
                        // gives the benchmark the object BasicTextField would hand to an IME, so
                        // the benchmark can call it directly.
                        inputConnection = request.createInputConnection(EditorInfo())
                        awaitCancellation()
                    },
                    content = content,
                )
            }
        }
    }

    fun requestFocus() {
        focusRequester.requestFocus()
    }
}

private object FocusedWindowInfo : WindowInfo {
    override val isWindowFocused: Boolean
        get() = true
}

private object NoOpBringIntoViewElement : ModifierNodeElement<NoOpBringIntoViewNode>() {
    override fun create(): NoOpBringIntoViewNode = NoOpBringIntoViewNode()

    override fun update(node: NoOpBringIntoViewNode) {}

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = other === this
}

private class NoOpBringIntoViewNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        // Stop propagation before AndroidComposeView's root BringIntoViewModifierNode.
    }
}
