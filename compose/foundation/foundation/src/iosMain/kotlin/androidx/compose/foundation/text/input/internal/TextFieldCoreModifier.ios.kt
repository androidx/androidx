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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.selection.DefaultTextSelectionColors
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.LocalNativeTextInputContext

@OptIn(InternalComposeUiApi::class)
internal actual fun TextFieldCoreModifierNode.drawSelectionHighlight(
    scope: DrawScope,
    selection: TextRange,
    textLayoutResult: TextLayoutResult,
) {
    val usingNativeTextInput = currentValueOf(LocalNativeTextInputContext).usingNativeTextInput()
    // iOS handles selection drawing itself in native text input mode
    if (!usingNativeTextInput) {
        drawDefaultSelectionHighlight(scope, selection, textLayoutResult)
    }
}

@OptIn(InternalComposeUiApi::class)
internal actual fun TextFieldCoreModifierNode.drawCursor(
    scope: DrawScope,
    brush: Brush,
    showCursor: Boolean,
    cursorAnimation: CursorAnimationState?,
    textFieldSelectionState: TextFieldSelectionState,
) {
    val nativeTextInputContext = currentValueOf(LocalNativeTextInputContext)
    // iOS handles cursor drawing itself in native text input mode
    if (nativeTextInputContext.usingNativeTextInput()) {
        // iOS uses one color to draw the cursor and selection handles
        // If it's not user set, use the system default one
        val selectionColors = currentValueOf(LocalTextSelectionColors)
        nativeTextInputContext.updateNativeTextInputTintColor(
            selectionColors.handleColor.takeIf {
                it != DefaultTextSelectionColors.handleColor
            }
        )
    } else {
        drawDefaultCursor(scope, brush, showCursor, cursorAnimation, textFieldSelectionState)
    }
}