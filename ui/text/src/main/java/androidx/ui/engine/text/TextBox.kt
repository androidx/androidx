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
package androidx.ui.engine.text

import androidx.ui.engine.geometry.Rect
import androidx.ui.toStringAsFixed

/**
 * A rectangle enclosing a run of text.
 *
 * This is similar to [Rect] but includes an inherent [TextDirection].
 */
data class TextBox(
    /**
     * The left edge of the text box, irrespective of direction.
     * To get the leading edge (which may depend on the [direction]), consider [start].
     */
    val left: Float,
    /** The top edge of the text box. */
    val top: Float,
    /**
     * The right edge of the text box, irrespective of direction.
     * To get the trailing edge (which may depend on the [direction]), consider [end].
     */
    val right: Float,
    /** The bottom edge of the text box. */
    val bottom: Float,
    /** The direction in which text inside this box flows. */
    val direction: TextDirection
) {

    /** Returns a rect of the same size as this box. */
    fun toRect(): Rect {
        return Rect.fromLTRB(left, top, right, bottom)
    }

    /**
     * The [left] edge of the box for left-to-right text; the [right] edge of the box for right-to-left text.
     * See also:
     *  * [direction], which specifies the text direction.
     */
    fun start(): Float {
        return if ((direction == TextDirection.Ltr)) left else right
    }

    /**
     * The [right] edge of the box for left-to-right text; the [left] edge of the box for right-to-left text.
     * See also:
     *  * [direction], which specifies the text direction.
     */
    fun end(): Float {
        return if ((direction == TextDirection.Ltr)) right else left
    }

    override fun toString(): String {
        return "TextBox.fromLTRBD(${left.toStringAsFixed(1)}, ${top.toStringAsFixed(1)}, " +
            "${right.toStringAsFixed(1)}, ${bottom.toStringAsFixed(1)}, $direction)"
    }

    companion object {
        fun fromLTRBD(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            direction: TextDirection
        ): TextBox {
            return TextBox(left, top, right, bottom, direction)
        }
    }
}
