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

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.wear.compose.material3.tokens.ShapeKeyTokens
import androidx.wear.compose.material3.tokens.ShapeTokens

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container, offering a range of curved shapes (mostly
 * polygonal). The default [Shapes] theme for Material3 is rounded rectangles, with various degrees
 * of corner roundness:
 * - Extra Small
 * - Small
 * - Medium
 * - Large
 * - Extra Large
 *
 * You can customize the shape system for all components in the [MaterialTheme] or you can do it on
 * a per component basis by overriding the shape parameter for that component. For example, by
 * default, buttons use the shape style "large". If your product requires a smaller amount of
 * roundness, you can override the shape parameter with a different shape value like [Shapes.small].
 *
 * @param extraSmall By default, provides [ShapeDefaults.ExtraSmall], a [RoundedCornerShape] with
 *   4dp [CornerSize] (used by bundled Cards).
 * @param small By default, provides [ShapeDefaults.Small], a [RoundedCornerShape] with 8dp
 *   [CornerSize].
 * @param medium By default, provides [ShapeDefaults.Medium], a [RoundedCornerShape] with 16dp
 *   [CornerSize] (used by shape-shifting Buttons and rounded rectangle buttons).
 * @param large By default, provides [ShapeDefaults.Large], a [RoundedCornerShape] with 24dp
 *   [CornerSize] (used by Cards).
 * @param extraLarge By default, provides [ShapeDefaults.ExtraLarge], a [RoundedCornerShape] with
 *   32dp [CornerSize].
 */
// TODO(b/273226734) Review documentation with references to components that use the shape themes.
@Immutable
class Shapes(
    val extraSmall: CornerBasedShape = ShapeDefaults.ExtraSmall,
    val small: CornerBasedShape = ShapeDefaults.Small,
    val medium: CornerBasedShape = ShapeDefaults.Medium,
    val large: CornerBasedShape = ShapeDefaults.Large,
    val extraLarge: CornerBasedShape = ShapeDefaults.ExtraLarge,
) {
    /** Returns a copy of this Shapes, optionally overriding some of the values. */
    fun copy(
        extraSmall: CornerBasedShape = this.extraSmall,
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large,
        extraLarge: CornerBasedShape = this.extraLarge,
    ): Shapes =
        Shapes(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false
        if (extraSmall != other.extraSmall) return false
        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false
        if (extraLarge != other.extraLarge) return false
        return true
    }

    override fun hashCode(): Int {
        var result = extraSmall.hashCode()
        result = 31 * result + small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        result = 31 * result + extraLarge.hashCode()
        return result
    }

    override fun toString(): String {
        return "Shapes(" +
            "extraSmall=$extraSmall, " +
            "small=$small, " +
            "medium=$medium, " +
            "large=$large, " +
            "extraLarge=$extraLarge)"
    }
}

/** Contains the default values used by [Shapes] */
object ShapeDefaults {

    /** Extra small sized corner shape */
    val ExtraSmall = ShapeTokens.CornerExtraSmall

    /** Small sized corner shape */
    val Small = ShapeTokens.CornerSmall

    /** Medium sized corner shape */
    val Medium = ShapeTokens.CornerMedium

    /** Large sized corner shape */
    val Large = ShapeTokens.CornerLarge

    /** Extra large sized corner shape */
    val ExtraLarge = ShapeTokens.CornerExtraLarge
}

/**
 * Helper function for component shape tokens. Here is an example on how to use component color
 * tokens: ``MaterialTheme.shapes.fromToken(FabPrimarySmallTokens.ContainerShape)``
 */
internal fun Shapes.fromToken(value: ShapeKeyTokens): Shape {
    return when (value) {
        ShapeKeyTokens.CornerExtraSmall -> extraSmall
        ShapeKeyTokens.CornerSmall -> small
        ShapeKeyTokens.CornerMedium -> medium
        ShapeKeyTokens.CornerLarge -> large
        ShapeKeyTokens.CornerExtraLarge -> extraLarge
        ShapeKeyTokens.CornerFull -> ShapeTokens.CornerFull
        ShapeKeyTokens.CornerNone -> ShapeTokens.CornerNone
    }
}

/**
 * Converts a shape token key to the local shape provided by the theme The shape references the
 * [LocalShapes].
 */
internal val ShapeKeyTokens.value: Shape
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.fromToken(this)

/** CompositionLocal used to specify the default shapes for the surfaces. */
internal val LocalShapes = staticCompositionLocalOf { Shapes() }
