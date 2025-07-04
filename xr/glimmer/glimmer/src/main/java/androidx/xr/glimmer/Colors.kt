/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A set of named color parameters for a [GlimmerTheme].
 *
 * @property primary The primary color is an accent color used for brand expression. It should
 *   typically be used with text and icons for emphasis, or with borders to accentuate a particular
 *   component - it should not be used to fill surfaces.
 * @property secondary The secondary color is an accent color used for brand expression. It should
 *   typically be used with text and icons for emphasis, or with borders to accentuate a particular
 *   component - it should not be used to fill surfaces.
 * @property positive The positive color is used to indicate positive or affirmative actions. For
 *   example, the border of a confirmation button. It should not be used to fill surfaces.
 * @property negative The negative color is used to indicate negative actions. For example, the
 *   border of a cancel button. It should not be used to fill surfaces.
 * @property surface The surface color that affect surfaces of components, such as buttons, cards,
 *   and list items. This should be [Color.Black] to ensure maximum contrast.
 * @property outline Subtle color used for borders. This color helps to add contrast around
 *   components for accessibility purposes.
 * @property outlineVariant Utility color used for borders for decorative elements when strong
 *   contrast is not required.
 */
@Immutable
public class Colors(
    public val primary: Color = Color(0xFFA8C7FA),
    public val secondary: Color = Color(0xFF4C88E9),
    public val positive: Color = Color(0xFF4CE995),
    public val negative: Color = Color(0xFFF57084),
    public val surface: Color = Color.Black,
    public val outline: Color = Color(0xFF606460),
    public val outlineVariant: Color = Color(0xFF42434A),
) {

    /** Returns a copy of this Colors, optionally overriding some of the values. */
    public fun copy(
        primary: Color = this.primary,
        secondary: Color = this.secondary,
        positive: Color = this.positive,
        negative: Color = this.negative,
        surface: Color = this.surface,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
    ): Colors =
        Colors(
            primary = primary,
            secondary = secondary,
            positive = positive,
            negative = negative,
            surface = surface,
            outline = outline,
            outlineVariant = outlineVariant,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Colors) return false

        if (primary != other.primary) return false
        if (secondary != other.secondary) return false
        if (positive != other.positive) return false
        if (negative != other.negative) return false
        if (surface != other.surface) return false
        if (outline != other.outline) return false
        if (outlineVariant != other.outlineVariant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + secondary.hashCode()
        result = 31 * result + positive.hashCode()
        result = 31 * result + negative.hashCode()
        result = 31 * result + surface.hashCode()
        result = 31 * result + outline.hashCode()
        result = 31 * result + outlineVariant.hashCode()
        return result
    }

    override fun toString(): String {
        return "Colors(primary=$primary, secondary=$secondary, positive=$positive, negative=$negative, surface=$surface, outline=$outline, outlineVariant=$outlineVariant)"
    }
}
