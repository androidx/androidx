/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.compose.State
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.core.px
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextRange
import kotlin.math.max

internal class TextSelectionDelegate(
    private val internalSelection: State<TextRange?>,
    private val layoutCoordinates: State<LayoutCoordinates?>,
    private val textDelegate: TextDelegate
) : TextSelectionHandler {

    override fun getSelection(
        startPosition: PxPosition,
        endPosition: PxPosition,
        containerLayoutCoordinates: LayoutCoordinates,
        mode: SelectionMode
    ): Selection? {
        val layoutCoordinates = layoutCoordinates.value!!

        val relativePosition = containerLayoutCoordinates.childToLocal(
            layoutCoordinates, PxPosition.Origin
        )
        val startPx = startPosition - relativePosition
        val endPx = endPosition - relativePosition

        val selection = getTextSelectionInfo(
            textDelegate = textDelegate,
            mode = mode,
            selectionCoordinates = Pair(startPx, endPx),
            layoutCoordinates = layoutCoordinates
        )

        return if (selection == null) {
            internalSelection.value = null
            null
        } else {
            internalSelection.value = selection.toTextRange()
            return selection
        }
    }
}

/**
 * Return information about the current selection in the Text.
 *
 * @param textDelegate The TextDelegate object from Text composable.
 * @param mode The mode of selection.
 * @param selectionCoordinates The positions of the start and end of the selection in Text
 * composable coordinate system.
 */
internal fun getTextSelectionInfo(
    textDelegate: TextDelegate,
    mode: SelectionMode,
    selectionCoordinates: Pair<PxPosition, PxPosition>,
    layoutCoordinates: LayoutCoordinates
): Selection? {
    val startPx = selectionCoordinates.first
    val endPx = selectionCoordinates.second

    val bounds = PxBounds(Px.Zero, Px.Zero, textDelegate.width.px, textDelegate.height.px)
    if (!mode.isSelected(bounds, start = startPx, end = endPx)) {
        return null
    } else {
        var (textSelectionStart, containsWholeSelectionStart) = getSelectionBorder(
            textDelegate = textDelegate,
            position = startPx,
            isStart = true
        )

        var (textSelectionEnd, containsWholeSelectionEnd) = getSelectionBorder(
            textDelegate = textDelegate,
            position = endPx,
            isStart = false
        )

        if (textSelectionStart == textSelectionEnd) {
            val wordBoundary = textDelegate.getWordBoundary(textSelectionStart)
            textSelectionStart = wordBoundary.start
            textSelectionEnd = wordBoundary.end
        }

        return Selection(
            start = Selection.AnchorInfo(
                coordinates = getSelectionHandleCoordinates(
                    textDelegate = textDelegate,
                    offset = textSelectionStart,
                    isStart = true
                ),
                direction = textDelegate.getBidiRunDirection(textSelectionStart),
                offset = textSelectionStart,
                layoutCoordinates = if (containsWholeSelectionStart) layoutCoordinates else null
            ),
            end = Selection.AnchorInfo(
                coordinates = getSelectionHandleCoordinates(
                    textDelegate = textDelegate,
                    offset = textSelectionEnd,
                    isStart = false
                ),
                direction = textDelegate.getBidiRunDirection(Math.max(textSelectionEnd - 1, 0)),
                offset = textSelectionEnd,
                layoutCoordinates = if (containsWholeSelectionEnd) layoutCoordinates else null
            )
        )
    }
}

/**
 * This function gets the border of the text selection. Border means either start or end of the
 * selection.
 *
 * @param textDelegate TextDelegate instance
 * @param position
 * @param isStart true if called for selection start handle
 *
 * @return
 */
// TODO(qqd) describe function argument [position]
// TODO(qqd) describe what this function returns
private fun getSelectionBorder(
    textDelegate: TextDelegate,
    // This position is in Text composable coordinate system.
    position: PxPosition,
    isStart: Boolean
): Pair<Int, Boolean> {
    val textLength = textDelegate.text.text.length
    // The character offset of the border of selection. The default value is set to the
    // beginning of the text composable for the start border, and the very last character offset
    // of the text composable for the end border. If the composable contains the whole selection's
    // border, this value will be reset.
    var selectionBorder = if (isStart) 0 else max(textLength - 1, 0)
    // Flag to check if the composable contains the whole selection's border.
    var containsWholeSelectionBorder = false

    val top = 0.px
    val bottom = textDelegate.height.px
    val left = 0.px
    val right = textDelegate.width.px
    // If the current text composable contains the whole selection's border, then find the exact
    // character offset of the border, and the flag checking if the composable contains the whole
    // selection's border will be set to true.
    if (position.x >= left &&
        position.x < right &&
        position.y >= top &&
        position.y < bottom
    ) {
        // Constrain the character offset of the selection border to be within the text range
        // of the current composable.
        val constrainedSelectionBorderOffset =
            textDelegate.getOffsetForPosition(position).coerceIn(0, textLength - 1)
        selectionBorder = constrainedSelectionBorderOffset
        containsWholeSelectionBorder = true
    }
    return Pair(selectionBorder, containsWholeSelectionBorder)
}

/**
 *
 * @param textDelegate TextDelegate instance
 * @param offset
 * @param isStart true if called for selection start handle
 *
 * @return
 */
// TODO(qqd) describe function argument [offset]
// TODO(qqd) describe what this function returns
private fun getSelectionHandleCoordinates(
    textDelegate: TextDelegate,
    offset: Int,
    isStart: Boolean
): PxPosition {
    val line = textDelegate.getLineForOffset(offset)
    val offsetToCheck = if (isStart) offset else Math.max(offset - 1, 0)
    val bidiRunDirection = textDelegate.getBidiRunDirection(offsetToCheck)
    val paragraphDirection = textDelegate.getParagraphDirection(offset)

    val x = if (bidiRunDirection == paragraphDirection)
        textDelegate.getPrimaryHorizontal(offset)
    else
        textDelegate.getSecondaryHorizontal(offset)

    val y = textDelegate.getLineBottom(line)

    return PxPosition(x.px, y.px)
}