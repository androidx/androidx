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

import androidx.compose.Immutable
import androidx.ui.core.LayoutCoordinates
import androidx.ui.text.TextRange
import androidx.ui.text.style.TextDirection
import androidx.ui.unit.PxPosition

/**
 * Information about the current Selection.
 */
@Immutable
data class Selection(
    /**
     * Information about the start of the selection.
     */
    val start: AnchorInfo,

    /**
     * Information about the end of the selection.
     */
    val end: AnchorInfo,
    /**
     * The flag to show that the selection handles are dragged across each other. After selection
     * is initialized, if user drags one handle to cross the other handle, this is true, otherwise
     * it's false.
     */
    // If selection happens in single widget, checking [TextRange.start] > [TextRange.end] is
    // enough.
    // But when selection happens across multiple widgets, this value needs more complicated
    // calculation. To avoid repeated calculation, making it as a flag is cheaper.
    val handlesCrossed: Boolean = false
) {
    /**
     * Contains information about an anchor (start/end) of selection.
     */
    @Immutable
    data class AnchorInfo(
        /**
         * The coordinates of the graphical position for selection character offset.
         *
         * This graphical position is the point at the left bottom corner for LTR
         * character, or right bottom corner for RTL character.
         *
         * This coordinates is in child composable coordinates system.
         */
        val coordinates: PxPosition,

        /**
         * Text direction of the character in selection edge.
         */
        val direction: TextDirection,

        /**
         * Character offset for the selection edge. This offset is within individual child text
         * composable.
         */
        val offset: Int,
        /**
         * The layout coordinates of the child which contains the whole selection. If the child
         * does not contain the end of the selection, this should be null.
         */
        val layoutCoordinates: LayoutCoordinates?
    )

    fun merge(other: Selection?): Selection {
        if (other == null) return this

        var selection = this
        if (handlesCrossed) selection = selection.copy(start = other.start)
        else selection = selection.copy(end = other.end)

        return selection
    }

    /**
     * Returns the selection offset information as a [TextRange]
     */
    fun toTextRange(): TextRange {
        return TextRange(start.offset, end.offset)
    }
}