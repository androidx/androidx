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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.jvm.Synchronized
import kotlin.math.max

internal class MultiWidgetSelectionDelegate(
    override val selectableId: Long,
    private val coordinatesCallback: () -> LayoutCoordinates?,
    private val layoutResultCallback: () -> TextLayoutResult?
) : Selectable {

    private var _previousTextLayoutResult: TextLayoutResult? = null

    // previously calculated `lastVisibleOffset` for the `_previousTextLayoutResult`
    private var _previousLastVisibleOffset: Int = -1

    /**
     * TextLayoutResult is not expected to change repeatedly in a BasicText composable. At least
     * most TextLayoutResult changes would likely affect Selection logic in some way. Therefore,
     * this value only caches the last visible offset calculation for the latest seen
     * TextLayoutResult instance. Object equality check is not worth the extra calculation as
     * instance check is enough to accomplish whether a text layout has changed in a meaningful
     * way.
     */
    private val TextLayoutResult.lastVisibleOffset: Int
        @Synchronized get() {
            if (_previousTextLayoutResult !== this) {
                val lastVisibleLine = when {
                    !didOverflowHeight || multiParagraph.didExceedMaxLines -> lineCount - 1
                    else -> { // size.height < multiParagraph.height
                        var finalVisibleLine = getLineForVerticalPosition(size.height.toFloat())
                            .coerceAtMost(lineCount - 1)
                        // if final visible line's top is equal to or larger than text layout
                        // result's height, we need to check above lines one by one until we find
                        // a line that fits in boundaries.
                        while (
                            finalVisibleLine >= 0 &&
                            getLineTop(finalVisibleLine) >= size.height
                        ) finalVisibleLine--
                        finalVisibleLine.coerceAtLeast(0)
                    }
                }
                _previousLastVisibleOffset = getLineEnd(lastVisibleLine, true)
                _previousTextLayoutResult = this
            }
            return _previousLastVisibleOffset
        }

    override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder) {
        val layoutCoordinates = getLayoutCoordinates() ?: return
        val textLayoutResult = layoutResultCallback() ?: return

        val relativePosition =
            builder.containerCoordinates.localPositionOf(layoutCoordinates, Offset.Zero)
        val localPosition = builder.currentPosition - relativePosition
        val localPreviousHandlePosition = if (builder.previousHandlePosition.isUnspecified) {
            Offset.Unspecified
        } else {
            builder.previousHandlePosition - relativePosition
        }

        builder.appendSelectableInfo(
            textLayoutResult = textLayoutResult,
            localPosition = localPosition,
            previousHandlePosition = localPreviousHandlePosition,
            selectableId = selectableId,
        )
    }

    override fun getSelectAllSelection(): Selection? {
        val textLayoutResult = layoutResultCallback() ?: return null
        val start = 0
        val end = textLayoutResult.layoutInput.text.length

        return Selection(
            start = Selection.AnchorInfo(
                direction = textLayoutResult.getBidiRunDirection(start),
                offset = start,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = textLayoutResult.getBidiRunDirection(max(end - 1, 0)),
                offset = end,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
    }

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset {
        // Check if the selection handle's selectable is the current selectable.
        if (isStartHandle && selection.start.selectableId != this.selectableId ||
            !isStartHandle && selection.end.selectableId != this.selectableId
        ) {
            return Offset.Unspecified
        }

        if (getLayoutCoordinates() == null) return Offset.Unspecified

        val textLayoutResult = layoutResultCallback() ?: return Offset.Unspecified
        val offset = if (isStartHandle) selection.start.offset else selection.end.offset
        val coercedOffset = offset.coerceIn(0, textLayoutResult.lastVisibleOffset)
        return getSelectionHandleCoordinates(
            textLayoutResult = textLayoutResult,
            offset = coercedOffset,
            isStart = isStartHandle,
            areHandlesCrossed = selection.handlesCrossed
        )
    }

    override fun getLayoutCoordinates(): LayoutCoordinates? {
        val layoutCoordinates = coordinatesCallback()
        if (layoutCoordinates == null || !layoutCoordinates.isAttached) return null
        return layoutCoordinates
    }

    override fun textLayoutResult(): TextLayoutResult? {
        return layoutResultCallback()
    }

    override fun getText(): AnnotatedString {
        val textLayoutResult = layoutResultCallback() ?: return AnnotatedString("")
        return textLayoutResult.layoutInput.text
    }

    override fun getBoundingBox(offset: Int): Rect {
        val textLayoutResult = layoutResultCallback() ?: return Rect.Zero
        val textLength = textLayoutResult.layoutInput.text.length
        if (textLength < 1) return Rect.Zero
        return textLayoutResult.getBoundingBox(
            offset.coerceIn(0, textLength - 1)
        )
    }

    override fun getLineLeft(offset: Int): Float {
        val textLayoutResult = layoutResultCallback() ?: return -1f
        val line = textLayoutResult.getLineForOffset(offset)
        if (line >= textLayoutResult.lineCount) return -1f
        return textLayoutResult.getLineLeft(line)
    }

    override fun getLineRight(offset: Int): Float {
        val textLayoutResult = layoutResultCallback() ?: return -1f
        val line = textLayoutResult.getLineForOffset(offset)
        if (line >= textLayoutResult.lineCount) return -1f
        return textLayoutResult.getLineRight(line)
    }

    override fun getCenterYForOffset(offset: Int): Float {
        val textLayoutResult = layoutResultCallback() ?: return -1f
        val line = textLayoutResult.getLineForOffset(offset)
        if (line >= textLayoutResult.lineCount) return -1f
        val top = textLayoutResult.getLineTop(line)
        val bottom = textLayoutResult.getLineBottom(line)
        return ((bottom - top) / 2) + top
    }

    override fun getRangeOfLineContaining(offset: Int): TextRange {
        val textLayoutResult = layoutResultCallback() ?: return TextRange.Zero
        val visibleTextLength = textLayoutResult.lastVisibleOffset
        if (visibleTextLength < 1) return TextRange.Zero
        val line = textLayoutResult.getLineForOffset(offset.coerceIn(0, visibleTextLength - 1))
        return TextRange(
            start = textLayoutResult.getLineStart(line),
            end = textLayoutResult.getLineEnd(line, visibleEnd = true)
        )
    }

    override fun getLastVisibleOffset(): Int {
        val textLayoutResult = layoutResultCallback() ?: return 0
        return textLayoutResult.lastVisibleOffset
    }

    override fun getLineHeight(offset: Int): Float {
        val textLayoutResult = layoutResultCallback() ?: return 0f
        val textLength = textLayoutResult.layoutInput.text.length
        if (textLength < 1) return 0f
        val line = textLayoutResult.getLineForOffset(offset.coerceIn(0, textLength - 1))
        return textLayoutResult.multiParagraph.getLineHeight(line)
    }
}

/**
 * Appends a [SelectableInfo] to this [SelectionLayoutBuilder].
 *
 * @param textLayoutResult the [TextLayoutResult] for the selectable
 * @param localPosition the position of the current handle if not being dragged
 * or the drag position if it is
 * @param previousHandlePosition the position of the previous handle
 * @param selectableId the selectableId for the selectable
 */
internal fun SelectionLayoutBuilder.appendSelectableInfo(
    textLayoutResult: TextLayoutResult,
    localPosition: Offset,
    previousHandlePosition: Offset,
    selectableId: Long,
) {
    val bounds = Rect(
        0.0f,
        0.0f,
        textLayoutResult.size.width.toFloat(),
        textLayoutResult.size.height.toFloat()
    )

    val currentXDirection = getXDirection(localPosition, bounds)
    val currentYDirection = getYDirection(localPosition, bounds)

    fun otherDirection(anchor: Selection.AnchorInfo?): Direction = anchor
        ?.let { getDirectionById(it.selectableId, selectableId) }
        ?: resolve2dDirection(currentXDirection, currentYDirection)

    val otherDirection: Direction
    val startXHandleDirection: Direction
    val startYHandleDirection: Direction
    val endXHandleDirection: Direction
    val endYHandleDirection: Direction
    if (isStartHandle) {
        otherDirection = otherDirection(previousSelection?.end)
        startXHandleDirection = currentXDirection
        startYHandleDirection = currentYDirection
        endXHandleDirection = otherDirection
        endYHandleDirection = otherDirection
    } else {
        otherDirection = otherDirection(previousSelection?.start)
        startXHandleDirection = otherDirection
        startYHandleDirection = otherDirection
        endXHandleDirection = currentXDirection
        endYHandleDirection = currentYDirection
    }

    if (!isSelected(resolve2dDirection(currentXDirection, currentYDirection), otherDirection)) {
        return
    }

    val textLength = textLayoutResult.layoutInput.text.length
    val rawStartHandleOffset: Int
    val rawEndHandleOffset: Int
    if (isStartHandle) {
        rawStartHandleOffset = getOffsetForPosition(localPosition, textLayoutResult)
        rawEndHandleOffset = previousSelection?.end
            ?.getPreviousAdjustedOffset(selectableIdOrderingComparator, selectableId, textLength)
            ?: rawStartHandleOffset
    } else {
        rawEndHandleOffset = getOffsetForPosition(localPosition, textLayoutResult)
        rawStartHandleOffset = previousSelection?.start
            ?.getPreviousAdjustedOffset(selectableIdOrderingComparator, selectableId, textLength)
            ?: rawEndHandleOffset
    }

    val rawPreviousHandleOffset = if (previousHandlePosition.isUnspecified) -1 else {
        getOffsetForPosition(previousHandlePosition, textLayoutResult)
    }

    appendInfo(
        selectableId = selectableId,
        rawStartHandleOffset = rawStartHandleOffset,
        startXHandleDirection = startXHandleDirection,
        startYHandleDirection = startYHandleDirection,
        rawEndHandleOffset = rawEndHandleOffset,
        endXHandleDirection = endXHandleDirection,
        endYHandleDirection = endYHandleDirection,
        rawPreviousHandleOffset = rawPreviousHandleOffset,
        textLayoutResult = textLayoutResult,
    )
}

private fun Selection.AnchorInfo.getPreviousAdjustedOffset(
    selectableIdOrderingComparator: Comparator<Long>,
    currentSelectableId: Long,
    currentTextLength: Int
): Int {
    val compareResult = selectableIdOrderingComparator.compare(
        this.selectableId,
        currentSelectableId
    )

    return when {
        compareResult < 0 -> 0
        compareResult > 0 -> currentTextLength
        else -> offset
    }
}

private fun getXDirection(position: Offset, bounds: Rect): Direction = when {
    position.x < bounds.left -> Direction.BEFORE
    position.x > bounds.right -> Direction.AFTER
    else -> Direction.ON
}

private fun getYDirection(position: Offset, bounds: Rect): Direction = when {
    position.y < bounds.top -> Direction.BEFORE
    position.y > bounds.bottom -> Direction.AFTER
    else -> Direction.ON
}

private fun SelectionLayoutBuilder.getDirectionById(
    anchorSelectableId: Long,
    currentSelectableId: Long,
): Direction {
    val compareResult = selectableIdOrderingComparator.compare(
        anchorSelectableId,
        currentSelectableId
    )

    return when {
        compareResult < 0 -> Direction.BEFORE
        compareResult > 0 -> Direction.AFTER
        else -> Direction.ON
    }
}

/**
 * Returns true if either of the directions are [Direction.ON]
 * or if the directions are not both [Direction.BEFORE] or [Direction.AFTER].
 */
private fun isSelected(currentDirection: Direction, otherDirection: Direction): Boolean =
    currentDirection == Direction.ON || currentDirection != otherDirection

// map offsets above/below the text to 0/length respectively
private fun getOffsetForPosition(position: Offset, textLayoutResult: TextLayoutResult): Int = when {
    position.y <= 0f -> 0
    position.y >= textLayoutResult.multiParagraph.height -> textLayoutResult.layoutInput.text.length
    else -> textLayoutResult.getOffsetForPosition(position)
}
