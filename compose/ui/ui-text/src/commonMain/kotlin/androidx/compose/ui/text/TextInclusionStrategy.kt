/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Rect

/**
 * The text inclusion strategy used by [Paragraph.getRangeForRect], it specifies when a range of
 * text is inside the given rect based on the geometric relation between the text range's bounding
 * box and the given rect.
 *
 * @see Paragraph.getRangeForRect
 */
fun interface TextInclusionStrategy {
    /**
     * Returns true if this [TextInclusionStrategy] considers the text range's [textBounds] to be
     * inside the given [rect].
     *
     * @param textBounds the bounding box of a range of the text.
     * @param rect a rectangle area.
     */
    fun isIncluded(textBounds: Rect, rect: Rect): Boolean

    companion object {
        /**
         * The [TextInclusionStrategy] that includes the text range whose bounds has any overlap
         * with the given rect.
         */
        val AnyOverlap = TextInclusionStrategy { textBounds, rect -> textBounds.overlaps(rect) }

        /**
         * The [TextInclusionStrategy] that includes the text range whose bounds is completely
         * contained by the given rect.
         */
        val ContainsAll = TextInclusionStrategy { textBounds, rect ->
            !rect.isEmpty &&
                textBounds.left >= rect.left &&
                textBounds.right <= rect.right &&
                textBounds.top >= rect.top &&
                textBounds.bottom <= rect.bottom
        }

        /**
         * The [TextInclusionStrategy] that includes the text range whose bounds' center is
         * contained by the given rect.
         */
        val ContainsCenter = TextInclusionStrategy { textBounds, rect ->
            rect.contains(textBounds.center)
        }
    }
}
