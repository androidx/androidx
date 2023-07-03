/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.compose.material3

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container, offering a range of
 * curved shapes (mostly polygonal). The default [Shapes] theme for Material3 is rounded rectangles,
 * with various degrees of corner roundness:
 *
 * - None
 * - Extra Small
 * - Small
 * - Medium
 * - Large
 * - Full
 *
 * You can customize the shape system for all components in the [MaterialTheme] or you can do it
 * on a per component basis by overriding the shape parameter for that
 * component. For example, by default, buttons use the shape style “full.” If your product requires
 * a smaller amount of roundness, you can override the shape parameter with a different shape
 * value like [Shapes.small].
 *
 * TODO(b/273226734) Review documentation with references to components that use the shape themes.
 *
 * @param none By default, provides [ShapeDefaults.None], which is [RectangleShape]
 * @param extraSmall By default, provides [ShapeDefaults.ExtraSmall], a [RoundedCornerShape]
 * with 4dp [CornerSize] (used by bundled Cards).
 * @param small By default, provides [ShapeDefaults.Small], a [RoundedCornerShape]
 * with 8dp [CornerSize].
 * @param medium By default, provides [ShapeDefaults.Medium], a [RoundedCornerShape] with
 * 16dp [CornerSize] (used by shape-shifting Buttons and rounded rectangle buttons).
 * @param large By default, provides [ShapeDefaults.Large], a [RoundedCornerShape]
 * with 24dp [CornerSize] (used by Cards).
 * @param extraLarge By default, provides [ShapeDefaults.ExtraLarge], a
 * [RoundedCornerShape] with 32dp [CornerSize].
 * @param full By default, provides [ShapeDefaults.Full], a Stadium shape with
 * 50% rounded [CornerSize] (used by Button).
 */
@Immutable
class Shapes(
    val none: Shape = ShapeDefaults.None,
    val extraSmall: Shape = ShapeDefaults.ExtraSmall,
    val small: Shape = ShapeDefaults.Small,
    val medium: Shape = ShapeDefaults.Medium,
    val large: Shape = ShapeDefaults.Large,
    val extraLarge: Shape = ShapeDefaults.ExtraLarge,
    val full: Shape = ShapeDefaults.Full,
) {
    /** Returns a copy of this Shapes, optionally overriding some of the values. */
    fun copy(
        none: Shape = this.none,
        extraSmall: Shape = this.extraSmall,
        small: Shape = this.small,
        medium: Shape = this.medium,
        large: Shape = this.large,
        extraLarge: Shape = this.extraLarge,
        full: Shape = this.full
    ): Shapes = Shapes(
        none = none,
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
        full = full,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false
        if (none != other.none) return false
        if (extraSmall != other.extraSmall) return false
        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false
        if (extraLarge != other.extraLarge) return false
        if (full != other.full) return false
        return true
    }

    override fun hashCode(): Int {
        var result = none.hashCode()
        result = 31 * result + extraSmall.hashCode()
        result = 31 * result + small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        result = 31 * result + extraLarge.hashCode()
        result = 31 * result + full.hashCode()
        return result
    }

    override fun toString(): String {
        return "Shapes(" +
            "none=$none, " +
            "extraSmall=$extraSmall, " +
            "small=$small, " +
            "medium=$medium, " +
            "large=$large, " +
            "extraLarge=$extraLarge, " +
            "full=$full)"
    }
}

/**
 * Contains the default values used by [Shapes]
 */
object ShapeDefaults {
    /** None provides a RectangleShape */
    val None = RectangleShape

    /** Extra small sized corner shape */
    val ExtraSmall = RoundedCornerShape(corner = CornerSize(4.dp))

    /** Small sized corner shape */
    val Small = RoundedCornerShape(corner = CornerSize(8.dp))

    /** Medium sized corner shape */
    val Medium = RoundedCornerShape(corner = CornerSize(16.dp))

    /** Large sized corner shape */
    val Large = RoundedCornerShape(corner = CornerSize(26.dp))

    /** Extra large sized corner shape */
    val ExtraLarge = RoundedCornerShape(corner = CornerSize(32.dp))

    /** Full provides a stadium-shape with 50% rounded corners */
    val Full = RoundedCornerShape(corner = CornerSize(50))
}

/** CompositionLocal used to specify the default shapes for the surfaces. */
internal val LocalShapes = staticCompositionLocalOf { Shapes() }
