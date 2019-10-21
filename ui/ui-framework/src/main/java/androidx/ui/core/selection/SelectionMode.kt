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

import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition

/**
 * The enum class allows user to decide the selection mode.
 */
enum class SelectionMode {
    /**
     * When selection handles are dragged across composables, selection extends by row, for example,
     * when the end selection handle is dragged down, upper rows will be selected first, and the
     * lower rows.
     */
    Vertical {
        override fun isSelected(
            bounds: PxBounds,
            start: PxPosition,
            end: PxPosition
        ): Boolean {
            // When the end of the selection is above the top of the composable, the composable is outside
            // of the selection range.
            if (end.y < bounds.top) return false

            // When the end of the selection is on the left of the composable, and not below the bottom
            // of composable, the composable is outside of the selection range.
            if (end.x < bounds.left && end.y < bounds.bottom) return false

            // When the start of the selection is below the bottom of the composable, the composable is
            // outside of the selection range.
            if (start.y >= bounds.bottom) return false

            // When the start of the selection is on the right of the composable, and not above the top
            // of the composable, the composable is outside of the selection range.
            if (start.x >= bounds.right && start.y >= bounds.top) return false

            return true
        }
    },

    /**
     * When selection handles are dragged across composables, selection extends by column, for example,
     * when the end selection handle is dragged to the right, left columns will be selected first,
     * and the right rows.
     */
    Horizontal {
        override fun isSelected(
            bounds: PxBounds,
            start: PxPosition,
            end: PxPosition
        ): Boolean {
            // When the end of the selection is on the left of the composable, the composable is outside of
            // the selection range.
            if (end.x < bounds.left) return false

            // When the end of the selection is on the top of the composable, and the not on the right
            // of the composable, the composable is outside of the selection range.
            if (end.y < bounds.top && end.x < bounds.right) return false

            // When the start of the selection is on the right of the composable, the composable is outside
            // of the selection range.
            if (start.x >= bounds.right) return false

            // When the start of the selection is below the composable, and not on the left of the
            // composable, the composable is outside of the selection range.
            if (start.y >= bounds.bottom && start.x >= bounds.left) return false

            return true
        }
    };

    // TODO(qqd) add function API docs
    /**
     * Decides if Composable which has [bounds], should be accepted by the selection and
     * change its selected state for a selection that starts at [start] and ends at [end].
     *
     * @param bounds Composable bounds
     * @param start
     * @param end
     */
    internal abstract fun isSelected(
        bounds: PxBounds,
        start: PxPosition,
        end: PxPosition
    ): Boolean
}
