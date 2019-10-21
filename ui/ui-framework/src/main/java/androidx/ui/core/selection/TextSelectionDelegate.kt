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
import androidx.ui.text.style.TextDirection
import kotlin.math.max

internal class TextSelectionDelegate(
    private val internalSelection: State<TextRange?>,
    private val layoutCoordinates: State<LayoutCoordinates?>,
    private val textDelegate: TextDelegate
) : TextSelectionHandler {

    override fun getSelection(
        selectionCoordinates: Pair<PxPosition, PxPosition>,
        containerLayoutCoordinates: LayoutCoordinates,
        mode: SelectionMode
    ): Selection? {
        val layoutCoordinates = layoutCoordinates.value!!

        val relativePosition = containerLayoutCoordinates.childToLocal(
            layoutCoordinates, PxPosition.Origin
        )
        val startPx = selectionCoordinates.first - relativePosition
        val endPx = selectionCoordinates.second - relativePosition

        val textSelectionInfo = getTextSelectionInfo(
            textDelegate = textDelegate,
            mode = mode,
            selectionCoordinates = Pair(startPx, endPx)
        )

        return if (textSelectionInfo == null) {
            internalSelection.value = null
            null
        } else {
            val textRange = textSelectionInfo.toTextRange()
            internalSelection.value = textRange

            // TODO(qqd): Determine a set of coordinates around a character that we
            //  need.
            Selection(
                startCoordinates = textSelectionInfo.start.coordinate,
                endCoordinates = textSelectionInfo.end.coordinate,
                startOffset = textRange.start,
                endOffset = textRange.end,
                startDirection = textSelectionInfo.start.direction,
                endDirection = textSelectionInfo.end.direction,
                startLayoutCoordinates = if (textSelectionInfo.start.containsWholeSelection) {
                    layoutCoordinates
                } else {
                    null
                },
                endLayoutCoordinates = if (textSelectionInfo.end.containsWholeSelection) {
                    layoutCoordinates
                } else {
                    null
                }
            )
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
    selectionCoordinates: Pair<PxPosition, PxPosition>
): TextSelectionInfo? {
    val startPx = selectionCoordinates.first
    val endPx = selectionCoordinates.second

    val bounds = PxBounds(Px.Zero, Px.Zero, textDelegate.width.px, textDelegate.height.px)
    if (!mode.isSelected(bounds, start = startPx, end = endPx)) {
        return null
    } else {
        var (textSelectionStart, containsWholeSelectionStart) =
            getSelectionBorder(textDelegate, startPx, true)
        var (textSelectionEnd, containsWholeSelectionEnd) =
            getSelectionBorder(textDelegate, endPx, false)

        if (textSelectionStart == textSelectionEnd) {
            val wordBoundary = textDelegate.getWordBoundary(textSelectionStart)
            textSelectionStart = wordBoundary.start
            textSelectionEnd = wordBoundary.end
        }

        return TextSelectionInfo(
            start = TextSelectionEdgeInfo(
                coordinate = getSelectionHandleCoordinates(
                    textDelegate = textDelegate,
                    offset = textSelectionStart,
                    isStart = true
                ),
                containsWholeSelection = containsWholeSelectionStart,
                direction = textDelegate.getBidiRunDirection(textSelectionStart),
                offset = textSelectionStart
            ),
            end = TextSelectionEdgeInfo(
                coordinate = getSelectionHandleCoordinates(
                    textDelegate = textDelegate,
                    offset = textSelectionEnd,
                    isStart = false
                ),
                containsWholeSelection = containsWholeSelectionEnd,
                direction = textDelegate.getBidiRunDirection(Math.max(textSelectionEnd - 1, 0)),
                offset = textSelectionEnd
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

/**
 * Contains information about the current selection on a Text.
 */
internal data class TextSelectionInfo(
    /**
     * Provides information for selection start.
     */
    val start: TextSelectionEdgeInfo,

    /**
     * Provides information for selection end.
     */
    val end: TextSelectionEdgeInfo
) {
    /**
     * Returns the selection offset information as a [TextRange]
     */
    fun toTextRange(): TextRange {
        return TextRange(start.offset, end.offset)
    }
}

/**
 * Contains information about an edge (start/end) of text selection
 */
internal data class TextSelectionEdgeInfo(
    /**
     * The coordinates of the graphical position for selection character offset.
     *
     * This graphical position is the point at the left bottom corner for LTR
     * character, or right bottom corner for RTL character.
     *
     * This coordinates is in child composable coordinates system.
     */
    val coordinate: PxPosition,

    /**
     * Text direction of the character in selection edge.
     */
    val direction: TextDirection,

    /**
     * A flag to check if the text composable contains the whole selection's edge.
     */
    val containsWholeSelection: Boolean,

    /**
     * Character offset for the selection edge.
     */
    val offset: Int
)