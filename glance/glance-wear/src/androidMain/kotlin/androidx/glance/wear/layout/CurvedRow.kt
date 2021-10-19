/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:*www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.wear.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

/**
 * The alignment of a [CurvedRow]'s elements, with respect to its anchor angle. This specifies how
 * elements added to a [CurvedRow] should be laid out with respect to the [CurvedRow]'s anchor
 * angle.
 *
 * As an example, assume that the following diagrams are wrapped to an arc, and
 * each represents a [CurvedRow] element containing a single text element. The text
 * element's anchor angle is "0" for all cases.
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
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class AnchorType private constructor(private val value: Int) {
    public companion object {
        /**
         * Anchor at the start of the elements. This will cause elements added to a
         * [CurvedRow] to begin at the given anchor angle, and sweep around to the right.
         */
        public val Start: AnchorType = AnchorType(0)

        /**
         * Anchor at the center of the elements. This will cause the center of the
         * whole set of elements added to a [CurvedRow] to be pinned at the given anchor angle.
         */
        public val Center: AnchorType = AnchorType(1)

        /**
         * Anchor at the end of the elements. This will cause the set of elements
         * inside the [CurvedRow] to end at the specified anchor angle, i.e. all elements
         * should be to the left of anchor angle.
         */
        public val End: AnchorType = AnchorType(2)
    }
}

/**
 * How to lay down components when they are thinner than the [CurvedRow]. Similar to vertical
 * alignment in a Row.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class RadialAlignment private constructor(private val value: Int) {
    companion object {
        /**
         * Put the child closest to the center of the [CurvedRow], within the available space
         */
        val Inner = RadialAlignment(0)

        /**
         * Put the child in the middle point of the available space.
         */
        val Center = RadialAlignment(1)

        /**
         * Put the child farthest from the center of the [CurvedRow], within the available space
         */
        val Outer = RadialAlignment(2)
    }
}

internal class EmittableCurvedRow : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier

    var anchorDegrees: Float = 270f
    var anchorType: AnchorType = AnchorType.Center
    var radialAlignment: RadialAlignment = RadialAlignment.Center
}

internal class EmittableCurvedText : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var text: String = ""
    var textStyle: CurvedTextStyle? = null
}

/**
 * A curved container. This container will fill itself to a circle, which fits inside its parent
 * container, and all of its children will be placed on that circle. The parameters [anchorDegrees]
 * and [anchorType] can be used to specify where to draw children within this circle. Each
 * child will then be placed, one after the other, clockwise around the circle.
 *
 * While this container can hold any composable element, only those built specifically to work
 * inside of a CurvedRow container (e.g. [CurvedRowScope.CurvedText]) will adapt themselves to the
 * CurvedRow. Any other element will be drawn normally, at a tangent to the circle.
 *
 * @param modifier Modifiers for this container.
 * @param anchorDegrees The angle for the anchor in degrees, used with [anchorType] to determine
 *   where to draw children. Note that 0 degrees is the 3 o'clock position on a device, and the
 *   angle sweeps clockwise. Values do not have to be clamped to the range 0-360; values less
 *   than 0 degrees will sweep anti-clockwise (i.e. -90 degrees is equivalent to 270 degrees),
 *   and values >360 will be be placed at X mod 360 degrees.
 * @param anchorType Alignment of the contents of this container relative to [anchorDegrees].
 * @param radialAlignment specifies where to lay down children that are thinner than the
 *   CurvedRow, either closer to the center (INNER), apart from the center (OUTER) or in the middle
 *   point (CENTER).
 * @param content The content of this [CurvedRow].
 */
@Composable
public fun CurvedRow(
    modifier: GlanceModifier = GlanceModifier,
    anchorDegrees: Float = 270f,
    anchorType: AnchorType = AnchorType.Center,
    radialAlignment: RadialAlignment = RadialAlignment.Center,
    content: @Composable CurvedRowScope.() -> Unit
) {
    ComposeNode<EmittableCurvedRow, Applier>(
        factory = ::EmittableCurvedRow,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(anchorDegrees) { this.anchorDegrees = it }
            this.set(anchorType) { this.anchorType = it }
            this.set(radialAlignment) { this.radialAlignment = it }
        },
        content = { CurvedRowScope().content() }
    )
}

/**
 * Description of a text style for the [CurvedRowScope.CurvedText] composable.
 */
@Immutable
public class CurvedTextStyle(
    public val fontSize: TextUnit? = null,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextStyle

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

    override fun toString() =
        "TextStyle(size=$fontSize, fontWeight=$fontWeight, fontStyle=$fontStyle)"
}

/** A scope for elements which can only be contained within a [CurvedRow]. */
class CurvedRowScope {
    /**
     * A text element which will draw curved text. This is only valid as a direct descendant of a
     * [CurvedRow]
     *
     * @param text The text to render.
     * @param textStyle The style to use for the Text.
     */
    @Composable
    public fun CurvedText(
        text: String,
        modifier: GlanceModifier = GlanceModifier,
        textStyle: CurvedTextStyle? = null
    ) {
        ComposeNode<EmittableCurvedText, Applier>(
            factory = ::EmittableCurvedText,
            update = {
                this.set(text) { this.text = it }
                this.set(modifier) { this.modifier = it }
                this.set(textStyle) { this.textStyle = it }
            }
        )
    }
}
