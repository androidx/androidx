/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.findFollowingBreak
import androidx.compose.foundation.text.findPrecedingBreak
import androidx.compose.foundation.text.getParagraphBoundary
import androidx.compose.ui.text.TextRange

/**
 * Selection can be adjusted depends on context. For example, in touch mode dragging after a long
 * press adjusts selection by word. But selection by dragging handles is character precise
 * without adjustments. With a mouse, double-click selects by words and triple-clicks by paragraph.
 * @see [SelectionRegistrar.notifySelectionUpdate]
 */
internal fun interface SelectionAdjustment {

    /**
     * The callback function that is called once a new selection arrives, the return value of
     * this function will be the final adjusted [Selection].
     */
    fun adjust(layout: SelectionLayout): Selection

    companion object {
        /**
         * The selection adjustment that does nothing and directly return the input raw
         * selection range.
         */
        val None = SelectionAdjustment { layout ->
            Selection(
                start = layout.startInfo.anchorForOffset(layout.startInfo.rawStartHandleOffset),
                end = layout.endInfo.anchorForOffset(layout.endInfo.rawEndHandleOffset),
                handlesCrossed = layout.crossStatus == CrossStatus.CROSSED
            )
        }

        /**
         * The character based selection. It normally won't change the raw selection range except
         * when the input raw selection range is collapsed. In this case, it will almost
         * always make sure at least one character is selected.
         */
        val Character = SelectionAdjustment { layout ->
            None.adjust(layout).ensureAtLeastOneChar(layout)
        }

        /**
         * The word based selection adjustment. It will adjust the raw input selection such that
         * the selection boundary snap to the word boundary. It will always expand the raw input
         * selection range to the closest word boundary. If the raw selection is reversed, it
         * will always return a reversed selection, and vice versa.
         */
        val Word = SelectionAdjustment { layout ->
            adjustToBoundaries(layout) {
                textLayoutResult.getWordBoundary(it)
            }
        }

        /**
         * The paragraph based selection adjustment. It will adjust the raw input selection such
         * that the selection boundary snap to the paragraph boundary. It will always expand the
         * raw input selection range to the closest paragraph boundary. If the raw selection is
         * reversed, it will always return a reversed selection, and vice versa.
         */
        val Paragraph = SelectionAdjustment { layout ->
            adjustToBoundaries(layout) {
                inputText.getParagraphBoundary(it)
            }
        }

        /**
         * A special version of character based selection that accelerates the selection update
         * with word based selection. In short, it expands by word and shrinks by character.
         * Here is more details of the behavior:
         * 1. When previous selection is null, it will use word based selection.
         * 2. When the start/end offset has moved to a different line/Text, it will use word
         * based selection.
         * 3. When the selection is shrinking, it behave same as the character based selection.
         * Shrinking means that the start/end offset is moving in the direction that makes
         * selected text shorter.
         * 4. The selection boundary is expanding,
         *  a.if the previous start/end offset is not a word boundary, use character based
         * selection.
         *  b.if the previous start/end offset is a word boundary, use word based selection.
         *
         *  Notice that this selection adjustment assumes that when isStartHandle is true, only
         *  start handle is moving(or unchanged), and vice versa.
         */
        val CharacterWithWordAccelerate = SelectionAdjustment { layout ->
            val previousSelection = layout.previousSelection
                ?: return@SelectionAdjustment Word.adjust(layout)

            val previousAnchor: Selection.AnchorInfo
            val newAnchor: Selection.AnchorInfo
            val startAnchor: Selection.AnchorInfo
            val endAnchor: Selection.AnchorInfo

            if (layout.isStartHandle) {
                previousAnchor = previousSelection.start
                newAnchor = layout.updateSelectionBoundary(layout.startInfo, previousAnchor)
                startAnchor = newAnchor
                endAnchor = previousSelection.end
            } else {
                previousAnchor = previousSelection.end
                newAnchor = layout.updateSelectionBoundary(layout.endInfo, previousAnchor)
                startAnchor = previousSelection.start
                endAnchor = newAnchor
            }

            if (newAnchor == previousAnchor) {
                // This avoids some cases in BiDi where `layout.crossed` is incorrect.
                // In BiDi layout, a single character move gesture can result in the offset
                // changing a large amount when crossing over from LTR -> RTL or visa versa.
                // This can result in a layout which says it is crossed, but our new selection
                // is uncrossed. Instead, just re-use the old selection.
                // It also saves an allocation.
                previousSelection
            } else {
                val crossed = layout.crossStatus == CrossStatus.CROSSED ||
                    (layout.crossStatus == CrossStatus.COLLAPSED &&
                        startAnchor.offset > endAnchor.offset)
                Selection(startAnchor, endAnchor, crossed).ensureAtLeastOneChar(layout)
            }
        }
    }
}

/**
 * @receiver The selection layout. It is expected that its previousSelection is non-null
 */
private fun SelectionLayout.updateSelectionBoundary(
    info: SelectableInfo,
    previousSelectionAnchor: Selection.AnchorInfo
): Selection.AnchorInfo {
    val currentRawOffset =
        if (isStartHandle) info.rawStartHandleOffset
        else info.rawEndHandleOffset

    val currentSlot = if (isStartHandle) startSlot else endSlot
    if (currentSlot != info.slot) {
        // we are between Texts
        return info.anchorForOffset(currentRawOffset)
    }

    val currentRawLine by lazy(LazyThreadSafetyMode.NONE) {
        info.textLayoutResult.getLineForOffset(currentRawOffset)
    }

    val otherRawOffset =
        if (isStartHandle) info.rawEndHandleOffset
        else info.rawStartHandleOffset

    val anchorSnappedToWordBoundary by lazy(LazyThreadSafetyMode.NONE) {
        info.snapToWordBoundary(
            currentLine = currentRawLine,
            currentOffset = currentRawOffset,
            otherOffset = otherRawOffset,
            isStart = isStartHandle,
            crossed = crossStatus == CrossStatus.CROSSED
        )
    }

    if (info.selectableId != previousSelectionAnchor.selectableId) {
        // moved to an entirely new Text, use word based adjustment
        return anchorSnappedToWordBoundary
    }

    if (currentRawOffset == info.rawPreviousHandleOffset) {
        // no change in current handle, return the previous result unchanged
        return previousSelectionAnchor
    }

    val previousSelectionOffset = previousSelectionAnchor.offset
    val previousSelectionLine =
        info.textLayoutResult.getLineForOffset(previousSelectionOffset)

    if (currentRawLine != previousSelectionLine) {
        // line changed, use word based adjustment
        return anchorSnappedToWordBoundary
    }

    val previousSelectionWordBoundary =
        info.textLayoutResult.getWordBoundary(previousSelectionOffset)

    if (!info.isExpanding(currentRawOffset, isStartHandle)) {
        // we're shrinking, use the raw offset.
        return info.anchorForOffset(currentRawOffset)
    }

    if (previousSelectionOffset == previousSelectionWordBoundary.start ||
        previousSelectionOffset == previousSelectionWordBoundary.end
    ) {
        // We are expanding, and the previous offset was a word boundary,
        // so continue using word boundaries.
        return anchorSnappedToWordBoundary
    }

    // We're expanding, but our previousOffset was not at a word boundary. This means
    // we are adjusting a selection within a word already, so continue to do so.
    return info.anchorForOffset(currentRawOffset)
}

private fun SelectableInfo.isExpanding(
    currentRawOffset: Int,
    isStart: Boolean
): Boolean {
    if (rawPreviousHandleOffset == -1) {
        return true
    }
    if (currentRawOffset == rawPreviousHandleOffset) {
        return false
    }

    val crossed = rawCrossStatus == CrossStatus.CROSSED
    return if (isStart xor crossed) {
        currentRawOffset < rawPreviousHandleOffset
    } else {
        currentRawOffset > rawPreviousHandleOffset
    }
}

private fun SelectableInfo.snapToWordBoundary(
    currentLine: Int,
    currentOffset: Int,
    otherOffset: Int,
    isStart: Boolean,
    crossed: Boolean,
): Selection.AnchorInfo {
    val wordBoundary = textLayoutResult.getWordBoundary(currentOffset)

    // In the case where the target word crosses multiple lines due to hyphenation or
    // being too long, we use the line start/end to keep the adjusted offset at the
    // same line.
    val wordStartLine = textLayoutResult.getLineForOffset(wordBoundary.start)
    val start = if (wordStartLine == currentLine) {
        wordBoundary.start
    } else {
        textLayoutResult.getLineStart(currentLine)
    }

    val wordEndLine = textLayoutResult.getLineForOffset(wordBoundary.end)
    val end = if (wordEndLine == currentLine) {
        wordBoundary.end
    } else {
        textLayoutResult.getLineEnd(currentLine)
    }

    // If one of the word boundary is exactly same as the otherBoundaryOffset, we
    // can't snap to this word boundary since it will result in an empty selection
    // range.
    if (start == otherOffset) {
        return anchorForOffset(end)
    }
    if (end == otherOffset) {
        return anchorForOffset(start)
    }

    val resultOffset = if (isStart xor crossed) {
        // In this branch when:
        // 1. selection is updating the start offset, and selection is not reversed.
        // 2. selection is updating the end offset, and selection is reversed.
        if (currentOffset <= end) start else end
    } else {
        // In this branch when:
        // 1. selection is updating the end offset, and selection is not reversed.
        // 2. selection is updating the start offset, and selection is reversed.
        if (currentOffset >= start) end else start
    }

    return anchorForOffset(resultOffset)
}

private fun interface BoundaryFunction {
    fun SelectableInfo.getBoundary(offset: Int): TextRange
}

private fun adjustToBoundaries(
    layout: SelectionLayout,
    boundaryFunction: BoundaryFunction,
): Selection {
    val crossed = layout.crossStatus == CrossStatus.CROSSED
    return Selection(
        start = layout.startInfo.anchorOnBoundary(
            crossed = crossed,
            isStart = true,
            slot = layout.startSlot,
            boundaryFunction = boundaryFunction,
        ),
        end = layout.endInfo.anchorOnBoundary(
            crossed = crossed,
            isStart = false,
            slot = layout.endSlot,
            boundaryFunction = boundaryFunction,
        ),
        handlesCrossed = crossed
    )
}

private fun SelectableInfo.anchorOnBoundary(
    crossed: Boolean,
    isStart: Boolean,
    slot: Int,
    boundaryFunction: BoundaryFunction,
): Selection.AnchorInfo {
    val offset = if (isStart) rawStartHandleOffset else rawEndHandleOffset

    if (slot != this.slot) {
        return anchorForOffset(offset)
    }

    val range = with(boundaryFunction) {
        getBoundary(offset)
    }

    return anchorForOffset(if (isStart xor crossed) range.start else range.end)
}

/**
 * This method adjusts the selection to one character respecting [String.findPrecedingBreak]
 * and [String.findFollowingBreak].
 */
internal fun Selection.ensureAtLeastOneChar(layout: SelectionLayout): Selection {
    // There already is at least one char in this selection, return this selection unchanged.
    if (!isCollapsed(layout)) {
        return this
    }

    // Exceptions where 0 char selection is acceptable:
    //   - The selection crosses multiple Texts, but is still collapsed.
    //       - In the same situation in a single Text, we usually select some whitespace.
    //         Since there is no whitespace to select, select nothing. Expanding the selection
    //         into any Texts in this case is likely confusing to the user
    //         as it is different functionality compared to single text.
    //   - The previous selection is null, indicating this is the start of a selection.
    //       - This allows a selection to start off as collapsed. This is necessary for
    //         Character adjustment to allow an initial collapsed selection, and then once a
    //         non-collapsed selection is started, this exception goes away.
    //   - There is no text to select at all, so you can't expand anywhere.
    val text = layout.currentInfo.inputText
    if (layout.size > 1 || layout.previousSelection == null || text.isEmpty()) {
        return this
    }

    return expandOneChar(layout)
}

/**
 * Precondition: the selection is empty.
 */
private fun Selection.expandOneChar(layout: SelectionLayout): Selection {
    val info = layout.currentInfo
    val text = info.inputText
    val offset = info.rawStartHandleOffset // start and end are the same, so either works

    // when the offset is at either boundary of the text,
    // expand the current handle one character into the text from the boundary.
    val lastOffset = text.length
    return when (offset) {
        0 -> {
            val followingBreak = text.findFollowingBreak(0)
            if (layout.isStartHandle) {
                copy(start = start.changeOffset(info, followingBreak), handlesCrossed = true)
            } else {
                copy(end = end.changeOffset(info, followingBreak), handlesCrossed = false)
            }
        }

        lastOffset -> {
            val precedingBreak = text.findPrecedingBreak(lastOffset)
            if (layout.isStartHandle) {
                copy(start = start.changeOffset(info, precedingBreak), handlesCrossed = false)
            } else {
                copy(end = end.changeOffset(info, precedingBreak), handlesCrossed = true)
            }
        }

        else -> {
            // In cases where offset is not along the boundary,
            // we will try to maintain the current cross handle states.
            val crossed = layout.previousSelection?.handlesCrossed == true
            val newOffset =
                if (layout.isStartHandle xor crossed) {
                    text.findPrecedingBreak(offset)
                } else {
                    text.findFollowingBreak(offset)
                }

            if (layout.isStartHandle) {
                copy(start = start.changeOffset(info, newOffset), handlesCrossed = crossed)
            } else {
                copy(end = end.changeOffset(info, newOffset), handlesCrossed = crossed)
            }
        }
    }
}

// update direction when we are changing the offset since it may be different
private fun Selection.AnchorInfo.changeOffset(
    info: SelectableInfo,
    newOffset: Int,
): Selection.AnchorInfo = copy(
    offset = newOffset,
    direction = info.textLayoutResult.getBidiRunDirection(newOffset)
)
