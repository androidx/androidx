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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.DefaultTextSelectionColors
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.uikit.LocalNativeTextInputContext

@OptIn(InternalComposeUiApi::class)
internal actual fun Modifier.textFieldCursor(
    state: LegacyTextFieldState,
    value: TextFieldValue,
    offsetMapping: OffsetMapping,
    cursorBrush: Brush,
    showCursor: Boolean,
): Modifier = composed {
    val nativeInputContext = LocalNativeTextInputContext.current
    val usingNativeTextInput = nativeInputContext.usingNativeTextInput()
    // iOS handles cursor drawing itself in native text input mode

    val selectionColors = LocalTextSelectionColors.current
    LaunchedEffect(selectionColors) {
        // iOS uses one color to draw the cursor and selection handles
        // If it's not user set, use the system default one
        nativeInputContext.updateNativeTextInputTintColor(
            selectionColors.handleColor.takeIf {
                it != DefaultTextSelectionColors.handleColor
            }
        )
    }

    if (usingNativeTextInput) this else cursor(state, value, offsetMapping, cursorBrush, showCursor)
}

@OptIn(InternalComposeUiApi::class)
internal actual fun Modifier.textFieldDraw(
    state: LegacyTextFieldState,
    value: TextFieldValue,
    offsetMapping: OffsetMapping,
): Modifier = composed {
    val nativeInputContext = LocalNativeTextInputContext.current
    val usingNativeTextInput = nativeInputContext.usingNativeTextInput()

    // iOS handles selection drawing itself in native text input mode
    if (usingNativeTextInput) {
        this.drawBehind {
            state.layoutResult?.let { layoutResult ->
                drawIntoCanvas { canvas ->
                    // Still need this for text rendering
                    TextPainter.paint(canvas, layoutResult.value)
                }
            }
        }
    } else {
        defaultTextFieldDraw(state, value, offsetMapping)
    }
}