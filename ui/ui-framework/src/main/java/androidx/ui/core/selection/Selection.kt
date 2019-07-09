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
import androidx.ui.engine.geometry.Rect

/**
 * Data class of Selection.
 */
data class Selection(
    /**
     * A box around the character at the start offset as Rect. This box' height is the line height,
     * and the width is the advance. Note: It is temporary to use Rect.
     */
    // TODO(qqd): After solving the problem of getting the coordinates of a character, figure out
    // what should the startOffset and endOffset should be.
    val startOffset: Rect,
    /**
     * A box around the character at the end offset as Rect. This box' height is the line height,
     * and the width is the advance. Note: It is temporary to use Rect.
     */
    val endOffset: Rect,
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
                startOffset = other.startOffset,
                startLayoutCoordinates = other.startLayoutCoordinates
            )
        }
        if (other.endLayoutCoordinates != null) {
            currentSelection = currentSelection.copy(
                endOffset = other.endOffset,
                endLayoutCoordinates = other.endLayoutCoordinates
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
