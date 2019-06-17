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
import androidx.ui.painting.TextPainter

/**
 * The enum class allows user to decide the selection mode.
 */
enum class SelectionMode {
    /**
     * When selection handles are dragged across widgets, selection extends by row, for example,
     * when the end selection handle is dragged down, upper rows will be selected first, and the
     * lower rows.
     */
    Vertical {
        override fun isSelected(
            textPainter: TextPainter,
            start: PxPosition,
            end: PxPosition
        ): Boolean {
            val top = 0.px
            val bottom = textPainter.height.px
            val left = 0.px
            val right = textPainter.width.px

            // When the end of the selection is above the top of the widget, the widget is outside
            // of the selection range.
            if (end.y < top) return false

            // When the end of the selection is on the left of the widget, and not below the bottom
            // of widget, the widget is outside of the selection range.
            if (end.x < left && end.y < bottom) return false

            // When the start of the selection is below the bottom of the widget, the widget is
            // outside of the selection range.
            if (start.y >= bottom) return false

            // When the start of the selection is on the right of the widget, and not above the top
            // of the widget, the widget is outside of the selection range.
            if (start.x >= right && start.y >= top) return false

            return true
        }
    },

    /**
     * When selection handles are dragged across widgets, selection extends by column, for example,
     * when the end selection handle is dragged to the right, left columns will be selected first,
     * and the right rows.
     */
    Horizontal {
        override fun isSelected(
            textPainter: TextPainter,
            start: PxPosition,
            end: PxPosition
        ): Boolean {
            val top = 0.px
            val bottom = textPainter.height.px
            val left = 0.px
            val right = textPainter.width.px

            // When the end of the selection is on the left of the widget, the widget is outside of
            // the selection range.
            if (end.x < left) return false

            // When the end of the selection is on the top of the widget, and the not on the right
            // of the widget, the widget is outside of the selection range.
            if (end.y < top && end.x < right) return false

            // When the start of the selection is on the right of the widget, the widget is outside
            // of the selection range.
            if (start.x >= right) return false

            // When the start of the selection is below the widget, and not on the left of the
            // widget, the widget is outside of the selection range.
            if (start.y >= bottom && start.x >= left) return false

            return true
        }
    };

    internal abstract fun isSelected(
        textPainter: TextPainter,
        start: PxPosition,
        end: PxPosition
    ): Boolean
}
