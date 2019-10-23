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
import androidx.ui.text.TextRange
import androidx.ui.text.style.TextDirection

/**
 * Information about the current Selection.
 */
data class Selection(
    /**
     * Information about the start of the selection.
     */
    val start: AnchorInfo,

    /**
     * Information about the end of the selection.
     */
    val end: AnchorInfo
) {
    /**
     * Contains information about an anchor (start/end) of selection.
     */
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

    // TODO(qqd): add tests, important
    fun merge(other: Selection?): Selection {
        if (other == null) return this

        // TODO(qqd): combine two selections' contents with styles together.
        var selection = this

        other.start.layoutCoordinates?.let {
            selection = selection.copy(start = other.start)
        }

        other.end.layoutCoordinates?.let {
            selection = selection.copy(end = other.end)
        }

        return selection
    }

    /**
     * Returns the selection offset information as a [TextRange]
     */
    fun toTextRange(): TextRange {
        return TextRange(start.offset, end.offset)
    }
}