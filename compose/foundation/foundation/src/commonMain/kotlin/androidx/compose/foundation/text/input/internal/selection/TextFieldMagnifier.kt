/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.coerceIn
import androidx.compose.foundation.text.input.internal.fromTextLayoutToCore
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue

internal abstract class TextFieldMagnifierNode :
    DelegatingNode(), OnGloballyPositionedModifier, DrawModifierNode, SemanticsModifierNode {

    abstract fun update(
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        textLayoutState: TextLayoutState,
        visible: Boolean
    )

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {}

    override fun ContentDrawScope.draw() {}

    override fun SemanticsPropertyReceiver.applySemantics() {}
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
internal expect fun textFieldMagnifierNode(
    textFieldState: TransformedTextFieldState,
    textFieldSelectionState: TextFieldSelectionState,
    textLayoutState: TextLayoutState,
    visible: Boolean
): TextFieldMagnifierNode

internal fun calculateSelectionMagnifierCenterAndroid(
    textFieldState: TransformedTextFieldState,
    selectionState: TextFieldSelectionState,
    textLayoutState: TextLayoutState,
    magnifierSize: IntSize
): Offset {
    // state read of currentDragPosition so that we always recompose on drag position changes
    val localDragPosition = selectionState.handleDragPosition

    // Do not show the magnifier if origin position is already Unspecified.
    // Never show the magnifier in an empty text field.
    if (localDragPosition.isUnspecified || textFieldState.visualText.isEmpty()) {
        return Offset.Unspecified
    }

    val selection = textFieldState.visualText.selection
    val textOffset =
        when (selectionState.draggingHandle) {
            null -> return Offset.Unspecified
            Handle.Cursor,
            Handle.SelectionStart -> selection.start
            Handle.SelectionEnd -> selection.end
        }

    // If the text hasn't been laid out yet, don't show the modifier.
    val layoutResult = textLayoutState.layoutResult ?: return Offset.Unspecified

    val dragX = localDragPosition.x
    val line = layoutResult.getLineForOffset(textOffset)
    val lineStart = layoutResult.getLineLeft(line)
    val lineEnd = layoutResult.getLineRight(line)
    val lineMin = minOf(lineStart, lineEnd)
    val lineMax = maxOf(lineStart, lineEnd)
    val centerX = dragX.coerceIn(lineMin, lineMax)

    // Hide the magnifier when dragged too far (outside the horizontal bounds of how big the
    // magnifier actually is). See
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/Editor.java;l=5228-5231;drc=2fdb6bd709be078b72f011334362456bb758922c
    // Also check whether magnifierSize is calculated. A platform magnifier instance is not
    // created until it's requested for the first time. So the size will only be calculated after we
    // return a specified offset from this function.
    // It is very unlikely that this behavior would cause a flicker since magnifier immediately
    // shows up where the pointer is being dragged. The pointer needs to drag further than the half
    // of magnifier's width to hide by the following logic.
    if (
        magnifierSize != IntSize.Zero && (dragX - centerX).absoluteValue > magnifierSize.width / 2
    ) {
        return Offset.Unspecified
    }

    // Center vertically on the current line.
    val top = layoutResult.getLineTop(line)
    val bottom = layoutResult.getLineBottom(line)
    val centerY = ((bottom - top) / 2) + top

    var offset = Offset(centerX, centerY)
    textLayoutState.textLayoutNodeCoordinates
        ?.takeIf { it.isAttached }
        ?.let { innerCoordinates -> offset = offset.coerceIn(innerCoordinates.visibleBounds()) }
    return textLayoutState.fromTextLayoutToCore(offset)
}
