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

package androidx.ui.rendering.paragraph

import androidx.ui.engine.geometry.Size

/**
 * Constraints for the text layout. The values are in pixels.
 */
data class TextConstraints(
    val minWidth: Float = 0.0f,
    val maxWidth: Float = Float.POSITIVE_INFINITY,
    val minHeight: Float = 0.0f,
    val maxHeight: Float = Float.POSITIVE_INFINITY
) {

    init {
        assert(minWidth.isFinite())
        assert(minHeight.isFinite())
    }

    /**
     * Returns the width that both satisfies the constraints and is as close as
     * possible to the given width.
     */
    private fun constrainWidth(width: Float = Float.POSITIVE_INFINITY): Float {
        return width.coerceIn(minWidth, maxWidth)
    }

    private fun constrainHeight(height: Float = Float.POSITIVE_INFINITY): Float {
        return height.coerceIn(minHeight, maxHeight)
    }

    /**
     * Returns the size that both satisfies the constraints and is as close as
     * possible to the given size.
     */
    internal fun constrain(size: Size): Size {
        return Size(constrainWidth(size.width), constrainHeight(size.height))
    }
}