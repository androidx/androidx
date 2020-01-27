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
import androidx.ui.geometry.Offset
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import androidx.ui.unit.Px
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.toPx
import androidx.ui.unit.toRect
import kotlin.math.max

internal class TextSelectionDelegate(
    private val selectionRange: MutableState<TextRange?>,
    private val layoutCoordinates: MutableState<LayoutCoordinates?>,
    private val textLayoutResult: MutableState<TextLayoutResult?>
) : Selectable {

    override fun getSelection(
        startPosition: PxPosition,
        endPosition: PxPosition,
        containerLayoutCoordinates: LayoutCoordinates,
        longPress: Boolean,
        previousSelection: Selection?,
        isStartHandle: Boolean
    ): Selection? {
        val layoutCoordinates = layoutCoordinates.value
        if (layoutCoordinates == null || !layoutCoordinates.isAttached) return null
        val textLayoutResult = textLayoutResult.value ?: return null

        val relativePosition = containerLayoutCoordinates.childToLocal(
            layoutCoordinates, PxPosition.Origin
        )
        val startPx = startPosition - relativePosition
        val endPx = endPosition - relativePosition

        val selection = getTextSelectionInfo(
            textLayoutResult = textLayoutResult,
            selectionCoordinates = Pair(startPx, endPx),
            layoutCoordinates = layoutCoordinates,
            wordBasedSelection = longPress,
            previousSelection = previousSelection,
            isStartHandle = isStartHandle
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
 * @param wordBasedSelection This flag is ignored if the selection handles are being dragged. If
 * the selection is modified by long press and drag gesture, the result selection will be
 * adjusted to word based selection. Otherwise, the selection will be adjusted to character based
 * selection.
 * @param previousSelection previous selection result
 * @param isStartHandle true if the start handle is being dragged
 *
 * @return [Selection] of the current composable, or null if the composable is not selected.
 */
internal fun getTextSelectionInfo(
    textLayoutResult: TextLayoutResult,
    selectionCoordinates: Pair<PxPosition, PxPosition>,
    layoutCoordinates: LayoutCoordinates,
    wordBasedSelection: Boolean,
    previousSelection: Selection? = null,
    isStartHandle: Boolean = true
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

    return getRefinedSelectionInfo(
        rawStartOffset = rawStartOffset,
        rawEndOffset = rawEndOffset,
        containsWholeSelectionStart = containsWholeSelectionStart,
        containsWholeSelectionEnd = containsWholeSelectionEnd,
        startPosition = startPosition,
        endPosition = endPosition,
        bounds = bounds,
        textLayoutResult = textLayoutResult,
        lastOffset = lastOffset,
        layoutCoordinates = layoutCoordinates,
        wordBasedSelection = wordBasedSelection,
        previousSelection = previousSelection,
        isStartHandle = isStartHandle
    )
}

/**
 * This method refines the selection info by processing the initial raw selection info.
 *
 * @param rawStartOffset unprocessed start offset calculated directly from input position
 * @param rawEndOffset unprocessed end offset calculated directly from input position
 * @param containsWholeSelectionStart a flag to check if current composable contains the overall
 * selection start
 * @param containsWholeSelectionEnd a flag to check if current composable contains the overall
 * selection end
 * @param startPosition graphical position of the start of the selection, in composable's
 * coordinates.
 * @param endPosition graphical position of the end of the selection, in composable's coordinates.
 * @param bounds bounds of the current composable
 * @param textLayoutResult a result of the text layout.
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param layoutCoordinates The [LayoutCoordinates] of the composable.
 * @param wordBasedSelection This flag is ignored if the selection handles are being dragged. If
 * the selection is modified by long press and drag gesture, the result selection will be
 * adjusted to word based selection. Otherwise, the selection will be adjusted to character based
 * selection.
 * @param previousSelection previous selection result
 * @param isStartHandle true if the start handle is being dragged
 *
 * @return [Selection] of the current composable, or null if the composable is not selected.
 */
private fun getRefinedSelectionInfo(
    rawStartOffset: Int,
    rawEndOffset: Int,
    containsWholeSelectionStart: Boolean,
    containsWholeSelectionEnd: Boolean,
    startPosition: PxPosition,
    endPosition: PxPosition,
    bounds: PxBounds,
    textLayoutResult: TextLayoutResult,
    lastOffset: Int,
    layoutCoordinates: LayoutCoordinates,
    wordBasedSelection: Boolean,
    previousSelection: Selection? = null,
    isStartHandle: Boolean = true
): Selection? {
    val shouldProcessAsSinglecomposable =
        containsWholeSelectionStart && containsWholeSelectionEnd

    var (startOffset, endOffset, handlesCrossed) =
        if (shouldProcessAsSinglecomposable) {
            processAsSingleComposable(
                rawStartOffset = rawStartOffset,
                rawEndOffset = rawEndOffset,
                previousSelection = previousSelection,
                isStartHandle = isStartHandle,
                lastOffset = lastOffset
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

    // If under long press, update the selection to word-based.
    if (wordBasedSelection) {
        val (start, end) = updateWordBasedSelection(
            textLayoutResult = textLayoutResult,
            startOffset = startOffset,
            endOffset = endOffset,
            handlesCrossed = handlesCrossed
        )
        startOffset = start
        endOffset = end
    }

    return getAssembledSelectionInfo(
        startOffset = startOffset,
        endOffset = endOffset,
        containsWholeSelectionStart = containsWholeSelectionStart,
        containsWholeSelectionEnd = containsWholeSelectionEnd,
        handlesCrossed = handlesCrossed,
        layoutCoordinates = layoutCoordinates,
        textLayoutResult = textLayoutResult
    )
}

/**
 * [Selection] contains a lot of parameters. It looks more clean to assemble an object of this
 * class in a separate method.
 *
 * @param startOffset the final start offset to be returned.
 * @param endOffset the final end offset to be returned.
 * @param containsWholeSelectionStart a flag to check if current composable contains the overall
 * selection start
 * @param containsWholeSelectionEnd a flag to check if current composable contains the overall
 * selection end
 * @param handlesCrossed true if the selection handles are crossed
 * @param layoutCoordinates The [LayoutCoordinates] of the composable.
 * @param textLayoutResult a result of the text layout.
 *
 * @return an assembled object of [Selection] using the offered selection info.
 */
private fun getAssembledSelectionInfo(
    startOffset: Int,
    endOffset: Int,
    containsWholeSelectionStart: Boolean,
    containsWholeSelectionEnd: Boolean,
    handlesCrossed: Boolean,
    layoutCoordinates: LayoutCoordinates,
    textLayoutResult: TextLayoutResult
): Selection {
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
 * different location. If the selection anchors point the same location and this is true, the
 * result selection will be adjusted to word boundary. Otherwise, the selection will be adjusted
 * to keep single character selected.
 * @param previousSelection previous selection result
 * @param isStartHandle true if the start handle is being dragged
 * @param lastOffset last offset of the text. It's actually the length of the text.
 *
 * @return the final startOffset, endOffset of the selection, and if the start and end are
 * crossed each other.
 */
private fun processAsSingleComposable(
    rawStartOffset: Int,
    rawEndOffset: Int,
    previousSelection: Selection?,
    isStartHandle: Boolean,
    lastOffset: Int
): Triple<Int, Int, Boolean> {
    var startOffset = rawStartOffset
    var endOffset = rawEndOffset
    if (startOffset == endOffset) {
        val textRange = ensureAtLeastOneChar(
            offset = rawStartOffset,
            lastOffset = lastOffset,
            previousSelection = previousSelection,
            isStartHandle = isStartHandle
        )
        startOffset = textRange.start
        endOffset = textRange.end
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
 * This method returns the adjusted word-based start and end offset of the selection.
 *
 * @param textLayoutResult a result of the text layout.
 * @param startOffset start offset to be snapped to a word.
 * @param endOffset end offset to be snapped to a word.
 * @param handlesCrossed true if the selection handles are crossed
 *
 * @return the adjusted word-based start and end offset of the selection.
 */
private fun updateWordBasedSelection(
    textLayoutResult: TextLayoutResult,
    startOffset: Int,
    endOffset: Int,
    handlesCrossed: Boolean
): Pair<Int, Int> {
    val maxOffset = textLayoutResult.layoutInput.text.text.length - 1
    val startWordBoundary = textLayoutResult.getWordBoundary(startOffset.coerceIn(0, maxOffset))
    val endWordBoundary = textLayoutResult.getWordBoundary(endOffset.coerceIn(0, maxOffset))

    // If handles are not crossed, start should be snapped to the start of the word containing the
    // start offset, and end should be snapped to the end of the word containing the end offset.
    // If handles are crossed, start should be snapped to the end of the word containing the start
    // offset, and end should be snapped to the start of the word containing the end offset.
    val start = if (handlesCrossed) startWordBoundary.end else startWordBoundary.start
    val end = if (handlesCrossed) endWordBoundary.start else endWordBoundary.end

    return Pair(start, end)
}
/**
 * This method adjusts the raw start and end offset and bounds the selection to one character. The
 * logic of bounding evaluates the last selection result, which handle is being dragged, and if
 * selection reaches the boundary.
 *
 * @param offset unprocessed start and end offset calculated directly from input position, in
 * this case start and offset equals to each other.
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param previousSelection previous selection result
 * @param isStartHandle true if the start handle is being dragged
 *
 * @return the adjusted [TextRange].
 */
private fun ensureAtLeastOneChar(
    offset: Int,
    lastOffset: Int,
    previousSelection: Selection?,
    isStartHandle: Boolean
): TextRange {
    var newStartOffset = offset
    var newEndOffset = offset

    // If the start and end offset are at the same character, and it's not the
    // initial selection, then bound to at least one character.
    previousSelection?.let {
        if (isStartHandle) {
            newStartOffset =
                if (it.handlesCrossed) {
                    if (newEndOffset == 0 || it.start.offset == newEndOffset + 1) {
                        newEndOffset + 1
                    } else {
                        newEndOffset - 1
                    }
                } else {
                    if (newEndOffset == lastOffset || it.start.offset == newEndOffset - 1) {
                        newEndOffset - 1
                    } else {
                        newEndOffset + 1
                    }
                }
        } else {
            newEndOffset =
                if (it.handlesCrossed) {
                    if (newStartOffset == lastOffset || it.end.offset == newStartOffset - 1) {
                        newStartOffset - 1
                    } else {
                        newStartOffset + 1
                    }
                } else {
                    if (newStartOffset == 0 || it.end.offset == newStartOffset + 1) {
                        newStartOffset + 1
                    } else {
                        newStartOffset - 1
                    }
                }
        }
    }
    return TextRange(newStartOffset, newEndOffset)
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
