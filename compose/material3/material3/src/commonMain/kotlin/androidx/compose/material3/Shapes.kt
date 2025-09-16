/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.ShapeTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container corners, offering a range of roundedness from
 * square to fully circular.
 *
 * There are different sizes of shapes:
 * - Extra Small
 * - Small
 * - Medium
 * - Large, Large Increased
 * - Extra Large, Extra Large Increased
 * - Extra Extra Large
 *
 * You can customize the shape system for all components in the [MaterialTheme] or you can do it on
 * a per component basis.
 *
 * You can change the shape that a component has by overriding the shape parameter for that
 * component. For example, by default, buttons use the shape style “full.” If your product requires
 * a smaller amount of roundedness, you can override the shape parameter with a different shape
 * value like [MaterialTheme.shapes.small].
 *
 * To learn more about shapes, see
 * [Material Design shapes](https://m3.material.io/styles/shape/overview).
 *
 * @param extraSmall A shape style with 4 same-sized corners whose size are bigger than
 *   [RectangleShape] and smaller than [Shapes.small]. By default autocomplete menu, select menu,
 *   snackbars, standard menu, and text fields use this shape.
 * @param small A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.extraSmall] and smaller than [Shapes.medium]. By default chips use this shape.
 * @param medium A shape style with 4 same-sized corners whose size are bigger than [Shapes.small]
 *   and smaller than [Shapes.large]. By default cards and small FABs use this shape.
 * @param large A shape style with 4 same-sized corners whose size are bigger than [Shapes.medium]
 *   and smaller than [Shapes.extraLarge]. By default extended FABs, FABs, and navigation drawers
 *   use this shape.
 * @param extraLarge A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.large] and smaller than [CircleShape]. By default large FABs use this shape.
 * @param largeIncreased A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.medium] and smaller than [Shapes.extraLarge]. Slightly larger variant to
 *   [Shapes.large].
 * @param extraLargeIncreased A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.large] and smaller than [Shapes.extraExtraLarge]. Slightly larger variant to
 *   [Shapes.extraLarge].
 * @param extraExtraLarge A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.extraLarge] and smaller than [CircleShape].
 */
// TODO: Update new shape descriptions to list what components leverage them by default.
// TODO(b/368578382): Update 'increased' variant kdocs to reference design documentation.
@Immutable
class Shapes
@ExperimentalMaterial3ExpressiveApi
constructor(
    // Shapes None and Full are omitted as None is a RectangleShape and Full is a CircleShape.
    val extraSmall: CornerBasedShape = ShapeDefaults.ExtraSmall,
    val small: CornerBasedShape = ShapeDefaults.Small,
    val medium: CornerBasedShape = ShapeDefaults.Medium,
    val large: CornerBasedShape = ShapeDefaults.Large,
    val extraLarge: CornerBasedShape = ShapeDefaults.ExtraLarge,
    largeIncreased: CornerBasedShape = ShapeDefaults.LargeIncreased,
    extraLargeIncreased: CornerBasedShape = ShapeDefaults.ExtraLargeIncreased,
    extraExtraLarge: CornerBasedShape = ShapeDefaults.ExtraExtraLarge,
) {
    @ExperimentalMaterial3ExpressiveApi
    /**
     * A shape style with 4 same-sized corners whose size are bigger than [Shapes.medium] and
     * smaller than [Shapes.extraLarge]. Slightly larger variant to [Shapes.large].
     */
    val largeIncreased = largeIncreased

    @ExperimentalMaterial3ExpressiveApi
    /**
     * A shape style with 4 same-sized corners whose size are bigger than [Shapes.large] and smaller
     * than [Shapes.extraExtraLarge]. Slightly larger variant to [Shapes.extraLarge].
     */
    val extraLargeIncreased = extraLargeIncreased

    @ExperimentalMaterial3ExpressiveApi
    /**
     * A shape style with 4 same-sized corners whose size are bigger than [Shapes.extraLarge] and
     * smaller than [CircleShape].
     */
    val extraExtraLarge = extraExtraLarge

    /**
     * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
     * components, communicate state, and express brand.
     *
     * The shape scale defines the style of container corners, offering a range of roundedness from
     * square to fully circular.
     *
     * There are different sizes of shapes:
     * - Extra Small
     * - Small
     * - Medium
     * - Large, Large Increased
     * - Extra Large, Extra Large Increased
     * - Extra Extra Large
     *
     * You can customize the shape system for all components in the [MaterialTheme] or you can do it
     * on a per component basis.
     *
     * You can change the shape that a component has by overriding the shape parameter for that
     * component. For example, by default, buttons use the shape style “full.” If your product
     * requires a smaller amount of roundedness, you can override the shape parameter with a
     * different shape value like [MaterialTheme.shapes.small].
     *
     * To learn more about shapes, see
     * [Material Design shapes](https://m3.material.io/styles/shape/overview).
     *
     * @param extraSmall A shape style with 4 same-sized corners whose size are bigger than
     *   [RectangleShape] and smaller than [Shapes.small]. By default autocomplete menu, select
     *   menu, snackbars, standard menu, and text fields use this shape.
     * @param small A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.extraSmall] and smaller than [Shapes.medium]. By default chips use this shape.
     * @param medium A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.small] and smaller than [Shapes.large]. By default cards and small FABs use this
     *   shape.
     * @param large A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.medium] and smaller than [Shapes.extraLarge]. By default extended FABs, FABs, and
     *   navigation drawers use this shape.
     * @param extraLarge A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.large] and smaller than [CircleShape]. By default large FABs use this shape.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    constructor(
        extraSmall: CornerBasedShape = ShapeDefaults.ExtraSmall,
        small: CornerBasedShape = ShapeDefaults.Small,
        medium: CornerBasedShape = ShapeDefaults.Medium,
        large: CornerBasedShape = ShapeDefaults.Large,
        extraLarge: CornerBasedShape = ShapeDefaults.ExtraLarge,
    ) : this(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
        largeIncreased = ShapeDefaults.LargeIncreased,
        extraLargeIncreased = ShapeDefaults.ExtraLargeIncreased,
        extraExtraLarge = ShapeDefaults.ExtraExtraLarge,
    )

    /** Returns a copy of this Shapes, optionally overriding some of the values. */
    @ExperimentalMaterial3ExpressiveApi
    fun copy(
        extraSmall: CornerBasedShape = this.extraSmall,
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large,
        extraLarge: CornerBasedShape = this.extraLarge,
        largeIncreased: CornerBasedShape = this.largeIncreased,
        extraLargeIncreased: CornerBasedShape = this.extraLargeIncreased,
        extraExtraLarge: CornerBasedShape = this.extraExtraLarge,
    ): Shapes =
        Shapes(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
            largeIncreased = largeIncreased,
            extraLargeIncreased = extraLargeIncreased,
            extraExtraLarge = extraExtraLarge,
        )

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

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false
        if (extraSmall != other.extraSmall) return false
        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false
        if (extraLarge != other.extraLarge) return false
        if (largeIncreased != other.largeIncreased) return false
        if (extraLargeIncreased != other.extraLargeIncreased) return false
        if (extraExtraLarge != other.extraExtraLarge) return false
        return true
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun hashCode(): Int {
        var result = extraSmall.hashCode()
        result = 31 * result + small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        result = 31 * result + extraLarge.hashCode()
        result = 31 * result + largeIncreased.hashCode()
        result = 31 * result + extraLargeIncreased.hashCode()
        result = 31 * result + extraExtraLarge.hashCode()
        return result
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun toString(): String {
        return "Shapes(" +
            "extraSmall=$extraSmall, " +
            "small=$small, " +
            "medium=$medium, " +
            "large=$large, " +
            "largeIncreased=$largeIncreased, " +
            "extraLarge=$extraLarge, " +
            "extralargeIncreased=$extraLargeIncreased, " +
            "extraExtraLarge=$extraExtraLarge)"
    }

    /** Cached shapes used in components */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultButtonShapesCached: ButtonShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultToggleButtonShapesCached: ToggleButtonShapes? = null
    internal var defaultVerticalDragHandleShapesCached: DragHandleShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultIconToggleButtonShapesCached: IconToggleButtonShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultIconButtonShapesCached: IconButtonShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultInteractiveListItemShapesCached: InteractiveListItemShapes? = null
}

/** Contains the default values used by [Shapes] */
object ShapeDefaults {
    /** Extra small sized corner shape */
    val ExtraSmall: CornerBasedShape = ShapeTokens.CornerExtraSmall

    /** Small sized corner shape */
    val Small: CornerBasedShape = ShapeTokens.CornerSmall

    /** Medium sized corner shape */
    val Medium: CornerBasedShape = ShapeTokens.CornerMedium

    /** Large sized corner shape */
    val Large: CornerBasedShape = ShapeTokens.CornerLarge

    @ExperimentalMaterial3ExpressiveApi
    /** Large sized corner shape, slightly larger than [Large] */
    val LargeIncreased: CornerBasedShape = ShapeTokens.CornerLargeIncreased

    /** Extra large sized corner shape */
    val ExtraLarge: CornerBasedShape = ShapeTokens.CornerExtraLarge

    @ExperimentalMaterial3ExpressiveApi
    /** Extra large sized corner shape, slightly larger than [ExtraLarge] */
    val ExtraLargeIncreased: CornerBasedShape = ShapeTokens.CornerExtraLargeIncreased

    @ExperimentalMaterial3ExpressiveApi
    /** An extra extra large (XXL) sized corner shape */
    val ExtraExtraLarge: CornerBasedShape = ShapeTokens.CornerExtraExtraLarge

    // TODO(b/368578382): Update 'increased' variant kdocs to reference design documentation.
    /** A non-rounded corner size */
    internal val CornerNone: CornerSize = ShapeTokens.CornerValueNone

    /** An extra small rounded corner size */
    internal val CornerExtraSmall: CornerSize = ShapeTokens.CornerValueExtraSmall

    /** A small rounded corner size */
    internal val CornerSmall: CornerSize = ShapeTokens.CornerValueSmall

    /** A medium rounded corner size */
    internal val CornerMedium: CornerSize = ShapeTokens.CornerValueMedium

    /** A large rounded corner size */
    internal val CornerLarge: CornerSize = ShapeTokens.CornerValueLarge

    /** A large rounded corner size, slightly larger than [CornerLarge] */
    internal val CornerLargeIncreased: CornerSize = ShapeTokens.CornerValueLargeIncreased

    /** An extra large rounded corner size */
    internal val CornerExtraLarge: CornerSize = ShapeTokens.CornerValueExtraLarge

    /** An extra large rounded corner size, slightly larger than [CornerExtraLarge] */
    internal val CornerExtraLargeIncreased: CornerSize = ShapeTokens.CornerValueExtraLargeIncreased

    /** An extra extra large (XXL) rounded corner size */
    internal val CornerExtraExtraLarge: CornerSize = ShapeTokens.CornerValueExtraExtraLarge

    /** A fully rounded corner size */
    internal val CornerFull: CornerSize = CornerSize(100)
}

/** Helper function for component shape tokens. Used to grab the top values of a shape parameter. */
internal fun CornerBasedShape.top(
    bottomSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(bottomStart = bottomSize, bottomEnd = bottomSize)
}

/**
 * Helper function for component shape tokens. Used to grab the bottom values of a shape parameter.
 */
internal fun CornerBasedShape.bottom(
    topSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(topStart = topSize, topEnd = topSize)
}

/**
 * Helper function for component shape tokens. Used to grab the start values of a shape parameter.
 */
internal fun CornerBasedShape.start(
    endSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(topEnd = endSize, bottomEnd = endSize)
}

/** Helper function for component shape tokens. Used to grab the end values of a shape parameter. */
internal fun CornerBasedShape.end(
    startSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(topStart = startSize, bottomStart = startSize)
}

/**
 * Helper function for component shape tokens. Here is an example on how to use component color
 * tokens: ``MaterialTheme.shapes.fromToken(FabPrimarySmallTokens.ContainerShape)``
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun Shapes.fromToken(value: ShapeKeyTokens): Shape {
    return when (value) {
        ShapeKeyTokens.CornerExtraLarge -> extraLarge
        ShapeKeyTokens.CornerExtraLargeIncreased -> extraLargeIncreased
        ShapeKeyTokens.CornerExtraExtraLarge -> extraExtraLarge
        ShapeKeyTokens.CornerExtraLargeTop -> extraLarge.top()
        ShapeKeyTokens.CornerExtraSmall -> extraSmall
        ShapeKeyTokens.CornerExtraSmallTop -> extraSmall.top()
        ShapeKeyTokens.CornerFull -> CircleShape
        ShapeKeyTokens.CornerLarge -> large
        ShapeKeyTokens.CornerLargeIncreased -> largeIncreased
        ShapeKeyTokens.CornerLargeEnd -> large.end()
        ShapeKeyTokens.CornerLargeTop -> large.top()
        ShapeKeyTokens.CornerMedium -> medium
        ShapeKeyTokens.CornerNone -> RectangleShape
        ShapeKeyTokens.CornerSmall -> small
        ShapeKeyTokens.CornerLargeStart -> large.start()
    }
}

/**
 * Converts a shape token key to the local shape provided by the theme The color is subscribed to
 * [LocalShapes] changes
 */
internal val ShapeKeyTokens.value: Shape
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.fromToken(this)

/** CompositionLocal used to specify the default shapes for the surfaces. */
internal val LocalShapes = staticCompositionLocalOf { Shapes() }
