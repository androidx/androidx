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

import androidx.ui.core.PxPosition
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import androidx.ui.painting.TextPainter
import androidx.ui.services.text_editing.TextSelection
import kotlin.math.max

/**
 *  This class processes the text selection for given selection start and end positions, and
 *  selection mode.
 */
internal class TextSelectionProcessor(
    /** The positions of the start and end of the selection in Text widget coordinate system.*/
    val selectionCoordinates: Pair<PxPosition, PxPosition>,
    /** The mode of selection. */
    val mode: SelectionMode,
    /** The lambda contains certain behavior when selection changes. Currently this is for changing
     * the selection used for drawing in Text widget. */
    var onSelectionChange: (TextSelection?) -> Unit = {},
    /** The TextPainter object from Text widget. */
    val textPainter: TextPainter
) {
    // TODO(qqd): Determine a set of coordinates around a character that we need.
    /**
     * The bounding box of the character at the start offset as Rect. The bounding box includes the
     * top, bottom, left, and right of the character. Note: It is temporary to use Rect.
     */
    // TODO(qqd): After solving the problem of getting the coordinates of a character, figure out
    // what should the startOffset and endOffset should be.
    internal var startOffset = Rect.zero
    /**
     * The bounding box of the character at the end offset as Rect. The bounding box includes the
     * top, bottom, left, and right of the character. Note: It is temporary to use Rect.
     */
    internal var endOffset = Rect.zero
    /**
     * A flag to check if the text widget contains the whole selection's start.
     */
    internal var containsWholeSelectionStart = false
    /**
     * A flag to check if the text widget contains the whole selection's end.
     */
    internal var containsWholeSelectionEnd = false
    /**
     *  A flag to check if the text widget is selected.
     */
    internal var isSelected = false

    /** The length of the text in text widget. */
    private val length = textPainter.text?.let { it.text.length } ?: 0

    init {
        processTextSelection()
    }

    /**
     * Process text selection.
     */
    private fun processTextSelection() {
        val startPx = selectionCoordinates.first
        val endPx = selectionCoordinates.second

        if (!mode.isSelected(textPainter, start = startPx, end = endPx)) {
            onSelectionChange(null)
            return
        }

        isSelected = true
        var (textSelectionStart, containsWholeSelectionStart) =
            getSelectionBorder(textPainter, startPx, true)
        var (textSelectionEnd, containsWholeSelectionEnd) =
            getSelectionBorder(textPainter, endPx, false)

        if (textSelectionStart.offset == textSelectionEnd.offset) {
            val wordBoundary = textPainter.getWordBoundary(textSelectionStart)
            textSelectionStart =
                TextPosition(wordBoundary.start, textSelectionStart.affinity)
            textSelectionEnd = TextPosition(wordBoundary.end, textSelectionEnd.affinity)
        } else {
            // Currently the implementation of selection is inclusive-inclusive which is a temporary
            // workaround, but inclusive-exclusive in Android. Thus before calling drawing selection
            // background, make the selection matches Android behaviour.
            textSelectionEnd =
                TextPosition(textSelectionEnd.offset + 1, TextAffinity.upstream)
        }

        onSelectionChange(TextSelection(textSelectionStart.offset, textSelectionEnd.offset))

        // Currently the implementation of selection is inclusive-inclusive which is a temporary
        // workaround, but inclusive-exclusive in Android. Thus make the selection end matches Crane
        // behaviour.
        textSelectionEnd =
            TextPosition(textSelectionEnd.offset - 1, TextAffinity.upstream)

        startOffset = textPainter.getBoundingBoxForTextPosition(textSelectionStart)
        endOffset = textPainter.getBoundingBoxForTextPosition(textSelectionEnd)

        this.containsWholeSelectionStart = containsWholeSelectionStart
        this.containsWholeSelectionEnd = containsWholeSelectionEnd
    }

    /**
     * This function gets the border of the text selection. Border means either start or end of the
     * selection.
     */
    private fun getSelectionBorder(
        textPainter: TextPainter,
        // This position is in Text widget coordinate system.
        position: PxPosition,
        isStart: Boolean
    ): Pair<TextPosition, Boolean> {
        // The text position of the border of selection. The default value is set to the beginning
        // of the text widget for the start border, and the very last position of the text widget
        // for the end border. If the widget contains the whole selection's border, this value will
        // be reset.
        var selectionBorder = TextPosition(
            offset = if (isStart) 0 else max(length - 1, 0),
            affinity = TextAffinity.upstream
        )
        // Flag to check if the widget contains the whole selection's border.
        var containsWholeSelectionBorder = false

        val top = 0.px
        val bottom = textPainter.height.px
        val left = 0.px
        val right = textPainter.width.px
        // If the current text widget contains the whole selection's border, then find the exact
        // text position of the border, and the flag checking  if the widget contains the whole
        // selection's border will be set to true.
        if (position.x >= left &&
            position.x < right &&
            position.y >= top &&
            position.y < bottom
        ) {
            val offset = Offset(position.x.value, position.y.value)
            // Constrain the position of the selection border to be within the text range of the
            // current widget.
            val constrainedSelectionBorderPosition =
                textPainter.getPositionForOffset(offset).offset.coerceIn(0, length - 1)
            selectionBorder = TextPosition(
                offset = constrainedSelectionBorderPosition,
                affinity = TextAffinity.upstream
            )
            containsWholeSelectionBorder = true
        }
        return Pair(selectionBorder, containsWholeSelectionBorder)
    }
}
