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

import androidx.compose.MutableState
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.core.toPx
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Offset
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import kotlin.math.max

internal class TextSelectionDelegate(
    private val selectionRange: MutableState<TextRange?>,
    private val layoutCoordinates: MutableState<LayoutCoordinates?>,
    private val textLayoutResult: TextLayoutResult
) : Selectable {

    override fun getSelection(
        startPosition: PxPosition,
        endPosition: PxPosition,
        containerLayoutCoordinates: LayoutCoordinates,
        longPress: Boolean
    ): Selection? {
        val layoutCoordinates = layoutCoordinates.value!!

        val relativePosition = containerLayoutCoordinates.childToLocal(
            layoutCoordinates, PxPosition.Origin
        )
        val startPx = startPosition - relativePosition
        val endPx = endPosition - relativePosition

        val selection = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            selectionCoordinates = Pair(startPx, endPx),
            layoutCoordinates = layoutCoordinates,
            wordSelectIfCollapsed = longPress
        )

        return if (selection == null) {
            selectionRange.value = null
            null
        } else {
            selectionRange.value = selection.toTextRange()
            return selection
        }
    }
}

/**
 * Return information about the current selection in the Text.
 *
 * @param textLayoutResult a result of the text layout.
 * @param selectionCoordinates The positions of the start and end of the selection in Text
 * composable coordinate system.
 * @param layoutCoordinates The [LayoutCoordinates] of the composable.
 * @param wordSelectIfCollapsed This flag is ignored if the selection offsets anchors point
 * different location. If the selection anchors point the same location and this is true, the
 * result selection will be adjusted to word boundary. Otherwise, the selection will be adjusted
 * to keep single character selected.
 *
 * @return [Selection] of the current composable, or null if the composable is not selected.
 */
internal fun getTextSelectionInfo(
    textLayoutResult: TextLayoutResult,
    selectionCoordinates: Pair<PxPosition, PxPosition>,
    layoutCoordinates: LayoutCoordinates,
    wordSelectIfCollapsed: Boolean
): Selection? {
    val startPosition = selectionCoordinates.first
    val endPosition = selectionCoordinates.second

    val bounds = PxBounds(
        Px.Zero,
        Px.Zero,
        textLayoutResult.size.width.toPx(),
        textLayoutResult.size.height.toPx()
    )

    val lastOffset = textLayoutResult.layoutInput.text.text.length

    val containsWholeSelectionStart =
        bounds.toRect().contains(Offset(startPosition.x.value, startPosition.y.value))

    val containsWholeSelectionEnd =
        bounds.toRect().contains(Offset(endPosition.x.value, endPosition.y.value))

    var rawStartOffset =
        if (containsWholeSelectionStart)
            textLayoutResult.getOffsetForPosition(startPosition).coerceIn(0, lastOffset)
        else
        // If the composable is selected, the start offset cannot be -1 for this composable. If the
        // final start offset is still -1, it means this composable is not selected.
            -1
    var rawEndOffset =
        if (containsWholeSelectionEnd)
            textLayoutResult.getOffsetForPosition(endPosition).coerceIn(0, lastOffset)
        else
        // If the composable is selected, the end offset cannot be -1 for this composable. If the
        // final end offset is still -1, it means this composable is not selected.
            -1

    val shouldProcessAsSinglecomposable =
        containsWholeSelectionStart && containsWholeSelectionEnd

    val (startOffset, endOffset, handlesCrossed) =
        if (shouldProcessAsSinglecomposable) {
            processAsSingleComposable(
                rawStartOffset = rawStartOffset,
                rawEndOffset = rawEndOffset,
                wordSelectIfCollapsed = wordSelectIfCollapsed,
                textLayoutResult = textLayoutResult
            )
        } else {
            processCrossComposable(
                startPosition = startPosition,
                endPosition = endPosition,
                rawStartOffset = rawStartOffset,
                rawEndOffset = rawEndOffset,
                lastOffset = lastOffset,
                bounds = bounds,
                containsWholeSelectionStart = containsWholeSelectionStart,
                containsWholeSelectionEnd = containsWholeSelectionEnd
            )
        }
    // nothing is selected
    if (startOffset == -1 && endOffset == -1) return null

    return Selection(
        start = Selection.AnchorInfo(
            coordinates = getSelectionHandleCoordinates(
                textLayoutResult = textLayoutResult,
                offset = startOffset,
                isStart = true,
                areHandlesCrossed = handlesCrossed
            ),
            direction = textLayoutResult.getBidiRunDirection(startOffset),
            offset = startOffset,
            layoutCoordinates = if (containsWholeSelectionStart) layoutCoordinates else null
        ),
        end = Selection.AnchorInfo(
            coordinates = getSelectionHandleCoordinates(
                textLayoutResult = textLayoutResult,
                offset = endOffset,
                isStart = false,
                areHandlesCrossed = handlesCrossed
            ),
            direction = textLayoutResult.getBidiRunDirection(max(endOffset - 1, 0)),
            offset = endOffset,
            layoutCoordinates = if (containsWholeSelectionEnd) layoutCoordinates else null
        ),
        handlesCrossed = handlesCrossed
    )
}

/**
 * This method takes unprocessed selection information as input, and calculates the selection
 * range and check if the selection handles are crossed, for selection with both start and end
 * are in the current composable.
 *
 * @param rawStartOffset unprocessed start offset calculated directly from input position
 * @param rawEndOffset unprocessed end offset calculated directly from input position
 * @param wordSelectIfCollapsed This flag is ignored if the selection offsets anchors point
 * different location. If the selection anchors point the same location and this is true, the
 * result selection will be adjusted to word boundary. Otherwise, the selection will be adjusted
 * to keep single character selected.
 * @param textLayoutResult a result of the text layout.
 *
 * @return the final startOffset, endOffset of the selection, and if the start and end are
 * crossed each other.
 */
private fun processAsSingleComposable(
    rawStartOffset: Int,
    rawEndOffset: Int,
    wordSelectIfCollapsed: Boolean,
    textLayoutResult: TextLayoutResult
): Triple<Int, Int, Boolean> {
    var startOffset = rawStartOffset
    var endOffset = rawEndOffset
    if (startOffset == endOffset) {
        if (wordSelectIfCollapsed) {
            // If the start and end offset are at the same character, and it's the initial
            // selection, then select a word.
            val wordBoundary = textLayoutResult.getWordBoundary(startOffset)
            startOffset = wordBoundary.start
            endOffset = wordBoundary.end
        } else {
            // If the start and end offset are at the same character, and it's not the
            // initial selection, then bound to at least one character.
            endOffset = startOffset + 1
        }
    }
    // Check if the start and end handles are crossed each other.
    val areHandlesCrossed = startOffset > endOffset
    return Triple(startOffset, endOffset, areHandlesCrossed)
}

/**
 * This method takes unprocessed selection information as input, and calculates the selection
 * range for current composable, and check if the selection handles are crossed, for selection with
 * the start and end are in different composables.
 *
 * @param startPosition graphical position of the start of the selection, in composable's
 * coordinates.
 * @param endPosition graphical position of the end of the selection, in composable's coordinates.
 * @param rawStartOffset unprocessed start offset calculated directly from input position
 * @param rawEndOffset unprocessed end offset calculated directly from input position
 * @param lastOffset the last offset of the text in current composable
 * @param bounds the bounds of the composable
 * @param containsWholeSelectionStart flag to check if the current composable contains the start of
 * the selection
 * @param containsWholeSelectionEnd flag to check if the current composable contains the end of the
 * selection
 *
 * @return the final startOffset, endOffset of the selection, and if the start and end handles are
 * crossed each other.
 */
private fun processCrossComposable(
    startPosition: PxPosition,
    endPosition: PxPosition,
    rawStartOffset: Int,
    rawEndOffset: Int,
    lastOffset: Int,
    bounds: PxBounds,
    containsWholeSelectionStart: Boolean,
    containsWholeSelectionEnd: Boolean
): Triple<Int, Int, Boolean> {
    val handlesCrossed = SelectionMode.Vertical.areHandlesCrossed(
        bounds = bounds,
        start = startPosition,
        end = endPosition)
    val isSelected = SelectionMode.Vertical.isSelected(
        bounds = bounds,
        start = if (handlesCrossed) endPosition else startPosition,
        end = if (handlesCrossed) startPosition else endPosition
    )
    var startOffset = if (isSelected && !containsWholeSelectionStart) {
        // If the composable is selected but the start is not in the composable, bound to the border
        // of the text in the composable.
        if (handlesCrossed) max(lastOffset, 0) else 0
    } else {
        // This else branch means (isSelected && containsWholeSelectionStart || !isSelected). If the
        // composable is not selected, the final offset will still be -1, if the composable contains
        // the start, the final offset has already been calculated earlier.
        rawStartOffset
    }
    var endOffset = if (isSelected && !containsWholeSelectionEnd) {
        // If the composable is selected but the end is not in the composable, bound to the border
        // of the text in the composable.
        if (handlesCrossed) 0 else max(lastOffset, 0)
    } else {
        // The same as startOffset.
        rawEndOffset
    }
    return Triple(startOffset, endOffset, handlesCrossed)
}

/**
 * This method returns the graphical position where the selection handle should be based on the
 * offset and other information.
 *
 * @param textLayoutResult a result of the text layout.
 * @param offset character offset to be calculated
 * @param isStart true if called for selection start handle
 * @param areHandlesCrossed true if the selection handles are crossed
 *
 * @return the graphical position where the selection handle should be.
 */
private fun getSelectionHandleCoordinates(
    textLayoutResult: TextLayoutResult,
    offset: Int,
    isStart: Boolean,
    areHandlesCrossed: Boolean
): PxPosition {
    val line = textLayoutResult.getLineForOffset(offset)
    val offsetToCheck =
        if (isStart && !areHandlesCrossed || !isStart && areHandlesCrossed) offset
        else max(offset - 1, 0)
    val bidiRunDirection = textLayoutResult.getBidiRunDirection(offsetToCheck)
    val paragraphDirection = textLayoutResult.getParagraphDirection(offset)

    val x = textLayoutResult.getHorizontalPosition(
        offset = offset,
        usePrimaryDirection = bidiRunDirection == paragraphDirection
    )
    val y = textLayoutResult.getLineBottom(line)

    return PxPosition(x, y)
}
