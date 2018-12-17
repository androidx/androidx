/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.editable

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextDirection

/**
 * Represents the coordinates of the point in a selection, and the text direction at that point,
 * relative to top left of the [RenderEditable] that holds the selection.
 * @immutable
 *
 * @constructor Creates a description of a point in a text selection.
 * @param [point] argument must not be null. Coordinates of the lower left or lower right corner of
 *                the selection, relative to the top left of the [RenderEditable] object.
 * @param [direction] Direction of the text at this edge of the selection.
 */
data class TextSelectionPoint(val point: Offset, val direction: TextDirection?) {
    override fun toString(): String = when (direction) {
        TextDirection.LTR -> "$point-ltr"
        TextDirection.RTL -> "$point-rtl"
        else -> "$point"
    }
}