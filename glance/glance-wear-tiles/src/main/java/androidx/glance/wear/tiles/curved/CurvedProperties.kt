/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.wear.tiles.curved

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider

/**
 * The alignment of a [CurvedRow]'s elements, with respect to its anchor angle. This specifies how
 * elements added to a [CurvedRow] should be laid out with respect to the [CurvedRow]'s anchor
 * angle.
 *
 * As an example, assume that the following diagrams are wrapped to an arc, and each represents a
 * [CurvedRow] element containing a single text element. The text element's anchor angle is "0" for
 * all cases.
 *
 * ```
 * AnchorType.Start:
 * -180                                0                                    180
 *                                     Hello World!
 *
 *
 * AnchorType.Center:
 * -180                                0                                    180
 *                                Hello World!
 *
 * AnchorType.End:
 * -180                                0                                    180
 *                          Hello World!
 * ```
 */
@JvmInline
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public value class AnchorType private constructor(private val value: Int) {
    public companion object {
        /**
         * Anchor at the start of the elements. This will cause elements added to a [CurvedRow] to
         * begin at the given anchor angle, and sweep around to the right.
         */
        public val Start: AnchorType = AnchorType(0)

        /**
         * Anchor at the center of the elements. This will cause the center of the whole set of
         * elements added to a [CurvedRow] to be pinned at the given anchor angle.
         */
        public val Center: AnchorType = AnchorType(1)

        /**
         * Anchor at the end of the elements. This will cause the set of elements inside the
         * [CurvedRow] to end at the specified anchor angle, i.e. all elements should be to the left
         * of anchor angle.
         */
        public val End: AnchorType = AnchorType(2)
    }
}

/**
 * How to lay down components when they are thinner than the [CurvedRow]. Similar to vertical
 * alignment in a Row.
 */
@JvmInline
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public value class RadialAlignment private constructor(private val value: Int) {
    public companion object {
        /** Put the child closest to the center of the [CurvedRow], within the available space */
        public val Inner: RadialAlignment = RadialAlignment(0)

        /** Put the child in the middle point of the available space. */
        public val Center: RadialAlignment = RadialAlignment(1)

        /** Put the child farthest from the center of the [CurvedRow], within the available space */
        public val Outer: RadialAlignment = RadialAlignment(2)
    }
}

/** Description of a text style for the [CurvedScope.curvedText] composable. */
@Immutable
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public class CurvedTextStyle(
    public val color: ColorProvider? = null,
    public val fontSize: TextUnit? = null,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurvedTextStyle) return false

        if (color != other.color) return false
        if (fontSize != other.fontSize) return false
        if (fontWeight != other.fontWeight) return false
        if (fontStyle != other.fontStyle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fontSize.hashCode()
        result = 31 * result + fontWeight.hashCode()
        result = 31 * result + fontStyle.hashCode()
        return result
    }

    override fun toString(): String =
        "TextStyle(size=$fontSize, fontWeight=$fontWeight, fontStyle=$fontStyle)"
}
