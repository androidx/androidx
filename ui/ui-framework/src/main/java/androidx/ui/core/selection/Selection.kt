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
import androidx.ui.text.style.TextDirection

/**
 * Data class of Selection.
 */
data class Selection(
    /**
     * The coordinates of the graphical position for selection start character offset.
     *
     * This graphical position is the point at the left bottom corner for LTR
     * character, or right bottom corner for RTL character.
     *
     * This coordinates is in child widget coordinates system.
     */
    val startCoordinates: PxPosition,
    /**
     * The coordinates of the graphical position for selection end character offset.
     *
     * This graphical position is the point at the left bottom corner for LTR
     * character, or right bottom corner for RTL character.
     *
     * This coordinates is in child widget coordinates system.
     */
    val endCoordinates: PxPosition,
    /**
     * Text direction of the starting character in selection.
     */
    val startDirection: TextDirection,
    /**
     * Text direction of the last character in selection.
     *
     * Note: The selection is inclusive-exclusive. But this is the text direction of the last
     * character of the selection.
     */
    val endDirection: TextDirection,
    /**
     * The layout coordinates of the child which contains the start of the selection. If the child
     * does not contain the start of the selection, this should be null.
     */
    val startLayoutCoordinates: LayoutCoordinates?,
    /**
     * The layout coordinates of the child which contains the end of the selection. If the child
     * does not contain the end of the selection, this should be null.
     */
    val endLayoutCoordinates: LayoutCoordinates?
) {
    internal fun merge(other: Selection): Selection {
        // TODO: combine two selections' contents with styles together.
        var currentSelection = this.copy()
        if (other.startLayoutCoordinates != null) {
            currentSelection = currentSelection.copy(
                startCoordinates = other.startCoordinates,
                startLayoutCoordinates = other.startLayoutCoordinates,
                startDirection = other.startDirection
            )
        }
        if (other.endLayoutCoordinates != null) {
            currentSelection = currentSelection.copy(
                endCoordinates = other.endCoordinates,
                endLayoutCoordinates = other.endLayoutCoordinates,
                endDirection = other.endDirection
            )
        }
        return currentSelection
    }
}

internal operator fun Selection?.plus(rhs: Selection?): Selection? {
    if (this == null) return rhs
    if (rhs == null) return this
    return merge(rhs)
}
