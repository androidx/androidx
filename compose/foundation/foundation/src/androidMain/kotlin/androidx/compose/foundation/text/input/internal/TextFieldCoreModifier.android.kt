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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

internal actual fun TextFieldCoreModifierNode.drawSelectionHighlight(
    scope: DrawScope,
    selection: TextRange,
    textLayoutResult: TextLayoutResult,
) = drawDefaultSelectionHighlight(scope, selection, textLayoutResult)

internal actual fun TextFieldCoreModifierNode.drawCursor(
    scope: DrawScope,
    brush: Brush,
    showCursor: Boolean,
    cursorAnimation: CursorAnimationState?,
    textFieldSelectionState: TextFieldSelectionState,
) = drawDefaultCursor(scope, brush, showCursor, cursorAnimation, textFieldSelectionState)
