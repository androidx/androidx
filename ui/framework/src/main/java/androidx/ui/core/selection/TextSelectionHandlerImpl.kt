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

import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import androidx.ui.painting.TextPainter
import androidx.ui.services.text_editing.TextSelection
import kotlin.math.max
import kotlin.math.min

/**
 * A class contains all the logic for text selection.
 */
internal class TextSelectionHandlerImpl(
    val textPainter: TextPainter,
    val layoutCoordinates: LayoutCoordinates?,
    var onSelectionChange: (TextSelection?) -> Unit = {}
) : TextSelectionHandler {

    /** Last TextPosition of the text in text widget. */
    private val lastTextPosition: Int
    /** Bounding box of the text widget. */
    private val box: Rect

    init {
        lastTextPosition = textPainter.text?.let { it.text.length - 1 } ?: 0
        box = Rect(
            top = 0f,
            bottom = textPainter.height,
            left = 0f,
            right = textPainter.width
        )
    }

    // Get selection for the start and end coordinates pair.
    override fun getSelection(
        selectionCoordinates: Pair<PxPosition, PxPosition>,
        containerLayoutCoordinates: LayoutCoordinates,
        mode: SelectionMode
    ): Selection? {
        val relativePosition = containerLayoutCoordinates.childToLocal(
            layoutCoordinates!!, PxPosition.Origin
        )
        val startPx = selectionCoordinates.first - relativePosition
        val endPx = selectionCoordinates.second - relativePosition

        if (!mode.isSelected(box, start = startPx, end = endPx)) {
            onSelectionChange(null)
            return null
        }

        var textSelectionStart = getSelectionBorder(startPx, true).first
        var startLayoutCoordinates = getSelectionBorder(startPx, true).second

        var textSelectionEnd = getSelectionBorder(endPx, false).first
        var endLayoutCoordinates = getSelectionBorder(endPx, false).second

        if (textSelectionStart.offset == textSelectionEnd.offset) {
            val wordBoundary = textPainter.getWordBoundary(textSelectionStart)
            textSelectionStart =
                TextPosition(wordBoundary.start, textSelectionStart.affinity)
            textSelectionEnd = TextPosition(wordBoundary.end, textSelectionEnd.affinity)
        } else {
            // Currently on Android, selection end is the offset after last character.
            // But when dragging happens, current Crane Text Selection end is the offset
            // of the last character. Thus before calling drawing selection background,
            // make the selection end matches Android behaviour.
            textSelectionEnd =
                TextPosition(textSelectionEnd.offset + 1, TextAffinity.upstream)
        }

        onSelectionChange(TextSelection(textSelectionStart.offset, textSelectionEnd.offset))

        // In Crane Text Selection, the selection end should be the last character, thus
        // make the selection end matches Crane behaviour.
        textSelectionEnd =
            TextPosition(textSelectionEnd.offset - 1, TextAffinity.upstream)

        return Selection(
            startOffset =
            textPainter.getBoundingBoxForTextPosition(textSelectionStart),
            endOffset =
            textPainter.getBoundingBoxForTextPosition(textSelectionEnd),
            startLayoutCoordinates = startLayoutCoordinates,
            endLayoutCoordinates = endLayoutCoordinates
        )
    }

    /**
     * This function gets the border of the text selection. Border means either start or end of the
     * selection.
     */
    private fun getSelectionBorder(
        position: PxPosition,
        isStart: Boolean
    ): Pair<TextPosition, LayoutCoordinates?> {
        // The text position of the border of selection. The default value is set to the beginning
        // of the text widget for the start border, and the very last position of the text widget
        // for the end border. If the widget contains the whole selection's border, this value will
        // be reset.
        var selectionBorder = TextPosition(
            offset = if (isStart) 0 else lastTextPosition,
            affinity = TextAffinity.upstream
        )
        // This LayoutCoordinates is for the widget which contains the whole selection's border. If
        // the current widget does not contain the whole selection's border, this default null value
        // will be returned.
        var selectionBorderLayoutCoordinates: LayoutCoordinates? = null

        // If the current text widget contains the whole selection's border, then find the exact
        // text position of the border, and the LayoutCoordinates of the current widget will be
        // returned.
        if (position.x >= box.left.px &&
            position.x <= box.right.px &&
            position.y >= box.top.px &&
            position.y <= box.bottom.px
        ) {
            val offset = Offset(position.x.value, position.y.value)
            // Constrain the position of the selection border to be within the text range of the
            // current widget.
            val constrainedSelectionBorderPosition =
                min(max(textPainter.getPositionForOffset(offset).offset, 0), lastTextPosition)
            selectionBorder = TextPosition(
                offset = constrainedSelectionBorderPosition,
                affinity = TextAffinity.upstream
            )
            selectionBorderLayoutCoordinates = layoutCoordinates
        }
        return Pair(selectionBorder, selectionBorderLayoutCoordinates)
    }
}
