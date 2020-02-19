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
package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LastBaseline
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Path
import androidx.ui.graphics.PathOperation
import androidx.ui.graphics.Shape
import androidx.ui.graphics.addOutline
import androidx.ui.layout.AlignmentLineOffset
import androidx.ui.layout.Arrangement
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.layout.Spacer
import androidx.ui.material.BottomAppBar.FabConfiguration
import androidx.ui.material.surface.Surface
import androidx.ui.material.surface.primarySurface
import androidx.ui.semantics.Semantics
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import androidx.ui.unit.toPxSize
import kotlin.math.sqrt

/**
 * A TopAppBar displays information and actions relating to the current screen and is placed at the
 * top of the screen.
 *
 * This TopAppBar has slots for a title, navigation icon, and actions. Use the other TopAppBar
 * overload for a generic TopAppBar with no restriction on content.
 *
 * @sample androidx.ui.material.samples.SimpleTopAppBar
 *
 * @param title The title to be displayed in the center of the TopAppBar
 * @param navigationIcon The navigation icon displayed at the start of the TopAppBar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions The actions displayed at the end of the TopAppBar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param color The background color for the TopAppBar. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this TopAppBar to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this TopAppBar.
 * @param elevation the elevation of this TopAppBar.
 */
@Composable
fun TopAppBar(
    title: @Composable() () -> Unit,
    navigationIcon: @Composable() (() -> Unit)? = null,
    actions: @Composable() RowScope.() -> Unit = {},
    color: Color = MaterialTheme.colors().primarySurface,
    contentColor: Color = contentColorFor(color),
    elevation: Dp = TopAppBarElevation
) {
    AppBar(color, contentColor, elevation, RectangleShape) {
        if (navigationIcon == null) {
            Spacer(LayoutWidth(TitleInsetWithoutIcon))
        } else {
            // TODO: make this a row after b/148014745 is fixed
            Box(
                LayoutHeight.Fill + LayoutWidth(TitleInsetWithIcon),
                gravity = ContentGravity.CenterLeft,
                children = navigationIcon
            )
        }

        // TODO(soboleva): rework this once AlignmentLineOffset is a modifier
        Box(LayoutHeight.Fill + LayoutFlexible(1f), gravity = ContentGravity.BottomLeft) {
            val baselineOffset = with(DensityAmbient.current) { TitleBaselineOffset.toDp() }
            AlignmentLineOffset(alignmentLine = LastBaseline, after = baselineOffset) {
                Semantics(container = true) {
                    CurrentTextStyleProvider(value = MaterialTheme.typography().h6) {
                        Row {
                            title()
                        }
                    }
                }
            }
        }

        // TODO: remove box and center align row's children after b/148014745 is fixed
        Box(modifier = LayoutHeight.Fill, gravity = ContentGravity.CenterRight) {
            Row(arrangement = Arrangement.End, children = actions)
        }
    }
}

/**
 * A TopAppBar displays information and actions relating to the current screen and is placed at the
 * top of the screen.
 *
 * This TopAppBar has no pre-defined slots for content, allowing you to customize the layout of
 * content inside.
 *
 * @param color The background color for the TopAppBar. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this TopAppBar to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this TopAppBar.
 * @param elevation the elevation of this TopAppBar.
 * @param children the children of this TopAppBar.The default layout here is a [Row],
 * so content inside will be placed horizontally.
 */
@Composable
fun TopAppBar(
    color: Color = MaterialTheme.colors().primarySurface,
    contentColor: Color = contentColorFor(color),
    elevation: Dp = TopAppBarElevation,
    children: @Composable() RowScope.() -> Unit
) {
    AppBar(color, contentColor, elevation, RectangleShape, children)
}

object BottomAppBar {
    /**
     * Configuration for a [FloatingActionButton] in a [BottomAppBar].
     *
     * This is state that is usually passed down to BottomAppBar by [Scaffold] or by another
     * scaffold-like component that is aware of [FloatingActionButton] existence and its size and
     * position.
     *
     * When cutoutShape is provided in BottomAppBar, a cutout / notch will be 'carved' into the
     * BottomAppBar based on FabConfiguration provided, with some extra space on all sides.
     *
     * If you use BottomAppBar with [Scaffold], a typical cutout for
     * FAB may look like:
     *
     * @sample androidx.ui.material.samples.SimpleBottomAppBarCutoutWithScaffold
     *
     * @param fabSize the size of the FAB that will be shown on top of BottomAppBar
     * @param fabTopLeftPosition the top left coordinate of the [FloatingActionButton] on the
     * screen for BottomAppBar to carve a right cutout if desired
     * @param fabDockedPosition the docked position of the [FloatingActionButton] in the
     * [BottomAppBar]
     */
    @Immutable
    data class FabConfiguration(
        internal val fabSize: IntPxSize,
        internal val fabTopLeftPosition: PxPosition,
        internal val fabDockedPosition: FabDockedPosition
    )

    /**
     * The possible positions for a [FloatingActionButton] docked to a [BottomAppBar].
     *
     * The layout of icons within the [BottomAppBar] will depend on the chosen position of the
     * [FloatingActionButton].
     */
    enum class FabDockedPosition {
        /**
         * Positioned in the center of the [BottomAppBar]. A minimum of one and a maximum of two
         * additional actions can be placed on the right side of the bar, and navigation icon will
         * be placed on the left side.
         */
        Center,
        /**
         * Positioned at the end of the [BottomAppBar]. A maximum of four additional
         * actions will be shown on the left side of the bar and there should be no navigation icon.
         */
        End
    }
}

/**
 * A BottomAppBar displays actions relating to the current screen and is placed at the bottom of
 * the screen. It can also optionally display a [FloatingActionButton], which is either overlaid
 * on top of the BottomAppBar, or inset, carving a cutout in the BottomAppBar.
 *
 * See [BottomAppBar anatomy](https://material.io/components/app-bars-bottom/#anatomy) for the
 * recommended content depending on the [FloatingActionButton] position.
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBar
 *
 * @param color The background color for the BottomAppBar. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this BottomAppBar to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this BottomAppBar.
 * @param fabConfiguration The [FabConfiguration] that controls where the [FloatingActionButton]
 * is placed inside the BottomAppBar. This is used both to determine the cutout position for
 * BottomAppBar (if cutoutShape is non-null) and to choose proper layout for BottomAppBar. If
 * null, BottomAppBar will show no cutout and no-FAB layout of icons.
 * @param cutoutShape the shape of the cutout that will be added to the BottomAppBar - this
 * should typically be the same shape used inside the [FloatingActionButton]. This shape will be
 * drawn with an offset around all sides. If null, where will be no cutout.
 * @param children the children of this BottomAppBar. The default layout here is a [Row],
 * so content inside will be placed horizontally.
 */
@Composable
fun BottomAppBar(
    color: Color = MaterialTheme.colors().primarySurface,
    contentColor: Color = contentColorFor(color),
    fabConfiguration: FabConfiguration? = null,
    cutoutShape: Shape? = null,
    children: @Composable() RowScope.() -> Unit
) {
    val shape = if (cutoutShape == null || fabConfiguration == null) {
        RectangleShape
    } else {
        BottomAppBarCutoutShape(
            cutoutShape,
            fabConfiguration.fabTopLeftPosition,
            fabConfiguration.fabSize.toPxSize()
        )
    }
    AppBar(color, contentColor, BottomAppBarElevation, shape) {
        // TODO: remove box and inline row's children after b/148014745 is fixed
        Box(LayoutSize.Fill, gravity = ContentGravity.Center) {
            Row(LayoutWidth.Fill, children = children)
        }
    }
}

// TODO: consider exposing this in the shape package, for a generic cutout shape - might be useful
// for custom components.
/**
 * A [Shape] that represents a bottom app bar with a cutout. The cutout drawn will be [cutoutShape]
 * increased in size by [BottomAppBarCutoutOffset] on all sides.
 */
private data class BottomAppBarCutoutShape(
    val cutoutShape: Shape,
    val fabPosition: PxPosition,
    val fabSize: PxSize
) : Shape {

    override fun createOutline(size: PxSize, density: Density): Outline {
        val boundingRectangle = Path().apply {
            addRect(Rect.fromLTRB(0f, 0f, size.width.value, size.height.value))
        }
        val path = Path().apply {
            addCutoutShape(density)
            // Subtract this path from the bounding rectangle
            op(boundingRectangle, this, PathOperation.difference)
        }
        return Outline.Generic(path)
    }

    /**
     * Adds the filled [cutoutShape] to the [Path]. The path can the be subtracted from the main
     * rectangle path used for the app bar, to create the resulting cutout shape.
     */
    private fun Path.addCutoutShape(density: Density) {
        // The gap on all sides between the FAB and the cutout
        val cutoutOffset = with(density) { BottomAppBarCutoutOffset.toPx() }

        val cutoutSize = PxSize(
            width = fabSize.width + (cutoutOffset * 2),
            height = fabSize.height + (cutoutOffset * 2)
        )

        val cutoutStartX = fabPosition.x.value - cutoutOffset.value
        val cutoutEndX = cutoutStartX + cutoutSize.width.value

        val cutoutRadius = cutoutSize.height.value / 2f
        // Shift the cutout up by half its height, so only the bottom half of the cutout is actually
        // cut into the app bar
        val cutoutStartY = -cutoutRadius

        addOutline(cutoutShape.createOutline(cutoutSize, density))
        shift(Offset(cutoutStartX, cutoutStartY))

        // TODO: consider exposing the custom cutout shape instead of just replacing circle shapes?
        if (cutoutShape == CircleShape) {
            val edgeRadius = with(density) { BottomAppBarRoundedEdgeRadius.toPx().value }
            // TODO: possibly support providing a custom vertical offset?
            addRoundedEdges(cutoutStartX, cutoutEndX, cutoutRadius, edgeRadius, 0f)
        }
    }

    /**
     * Adds rounded edges to the [Path] representing a circular cutout in a BottomAppBar.
     *
     * Adds a curve for the left and right edges, with a straight line drawn between them - this
     * combined with the cutout shape results in the overall cutout path that can be subtracted
     * from the bounding rect of the app bar.
     *
     * @param cutoutStartPosition the absolute start position of the cutout
     * @param cutoutEndPosition the absolute end position of the cutout
     * @param cutoutRadius the radius of the cutout's circular edge - for a typical circular FAB
     * this will just be the radius of the circular cutout, but in the case of an extended FAB, we
     * can model this as two circles on either side attached to a rectangle.
     * @param roundedEdgeRadius how far from the points where the cutout intersects with the app bar
     * should the rounded edges be drawn to.
     * @param verticalOffset how far the app bar is from the center of the cutout circle
     */
    private fun Path.addRoundedEdges(
        cutoutStartPosition: Float,
        cutoutEndPosition: Float,
        cutoutRadius: Float,
        roundedEdgeRadius: Float,
        verticalOffset: Float
    ) {
        // Where the cutout intersects with the app bar, as if the cutout is not vertically aligned
        // with the app bar, the intersect will not be equal to the radius of the circle.
        val appBarInterceptOffset = calculateCutoutCircleYIntercept(cutoutRadius, verticalOffset)
        val appBarInterceptStartX = cutoutStartPosition + (cutoutRadius + appBarInterceptOffset)
        val appBarInterceptEndX = cutoutEndPosition - (cutoutRadius + appBarInterceptOffset)

        // How far the control point is away from the cutout intercept. We set this to be as small
        // as possible so that we have the most 'rounded' curve.
        val controlPointOffset = 1f

        // How far the control point is away from the center of the radius of the cutout
        val controlPointRadiusOffset = appBarInterceptOffset - controlPointOffset

        // The coordinates offset from the center of the radius of the cutout, where we should
        // draw the curve to
        val (curveInterceptXOffset, curveInterceptYOffset) = calculateRoundedEdgeIntercept(
            controlPointRadiusOffset,
            verticalOffset,
            cutoutRadius
        )

        // Convert the offset relative to the center of the cutout circle into an absolute
        // coordinate, by adding the radius of the shape to get a pure relative offset from the
        // leftmost edge, and then positioning it next to the cutout
        val curveInterceptStartX = cutoutStartPosition + (curveInterceptXOffset + cutoutRadius)
        val curveInterceptEndX = cutoutEndPosition - (curveInterceptXOffset + cutoutRadius)

        // Convert the curveInterceptYOffset which is relative to the center of the cutout, to an
        // absolute position
        val curveInterceptY = curveInterceptYOffset - verticalOffset

        // Where the rounded edge starts
        val roundedEdgeStartX = appBarInterceptStartX - roundedEdgeRadius
        val roundedEdgeEndX = appBarInterceptEndX + roundedEdgeRadius

        moveTo(roundedEdgeStartX, 0f)
        quadraticBezierTo(
            appBarInterceptStartX - controlPointOffset,
            0f,
            curveInterceptStartX,
            curveInterceptY
        )
        lineTo(curveInterceptEndX, curveInterceptY)
        quadraticBezierTo(appBarInterceptEndX + controlPointOffset, 0f, roundedEdgeEndX, 0f)
        close()
    }
}

/**
 * Helper to make the following equations easier to read
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun square(x: Float) = x * x

/**
 * Returns the relative y intercept for a circle with the given [cutoutRadius] and [verticalOffset]
 *
 * Returns the leftmost intercept, so this will be a negative number that when added to the circle's
 * absolute origin will give the absolute position of the left intercept, where the circle meets
 * the app bar.
 *
 * Explanation:
 * First construct the equation for a circle with given radius and vertical offset:
 * x^2 + (y-verticalOffset)^2 = radius^2
 *
 * We want to find the y intercept where the cutout hits the top edge of the bottom app bar, so
 * rearrange and set y to 0:
 *
 * x^2 = radius^2 - (0-verticalOffset)^2
 *
 * We are only interested in the left most (negative x) solution as we mirror this for the right
 * edge later.
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun calculateCutoutCircleYIntercept(
    cutoutRadius: Float,
    verticalOffset: Float
): Float {
    return -sqrt(square(cutoutRadius) - square(verticalOffset))
}

// TODO: Consider extracting this into the shape package / similar, might be useful for cutouts in
// general.
/**
 * For a given control point on a quadratic bezier curve, calculates the required intercept
 * point to create a smooth curve between the rounded edges near the cutout, and the actual curve
 * that is part of the cutout.
 *
 * This returns the relative offset from the center of a circle with radius that is half the
 * height of the cutout.
 *
 * Explanation and derivation comes from the Flutter team: https://goo.gl/Ufzrqn
 *
 * @param controlPointX the horizontal offset of the control point from the center of the circle
 * @param verticalOffset the vertical offset of the top edge of the app bar from the center of the
 * circle. I.e, if this is 2f, then the top edge of the app bar is 2f below the center. If 0f, the
 * top edge of the app bar is in centered inside the circle.
 * @param radius the radius of the circle - essentially the 'depth' of the cutout
 */
@Suppress("UnnecessaryVariable")
internal fun calculateRoundedEdgeIntercept(
    controlPointX: Float,
    verticalOffset: Float,
    radius: Float
): Pair<Float, Float> {
    val a = controlPointX
    val b = verticalOffset
    val r = radius

    // expands to a2b2r2 + b4r2 - b2r4
    val discriminant = square(b) * square(r) * (square(a) + square(b) - square(r))
    val divisor = square(a) + square(b)
    // the '-b' part of the quadratic solution
    val bCoefficient = a * square(r)

    // Two solutions for the x coordinate relative to the midpoint of the circle
    val xSolutionA = (bCoefficient - sqrt(discriminant)) / divisor
    val xSolutionB = (bCoefficient + sqrt(discriminant)) / divisor

    // Get y coordinate from r2 = x2 + y2 -> y2 = r2 - x2
    val ySolutionA = sqrt(square(r) - square(xSolutionA))
    val ySolutionB = sqrt(square(r) - square(xSolutionB))

    // If the vertical offset is 0, the vertical center of the circle lines up with the top edge of
    // the bottom app bar, so both solutions are identical.
    // If the vertical offset is not 0, there are two distinct solutions: one that will meet in the
    // top half of the circle, and one that will meet in the bottom half of the circle. As the app
    // bar is always on the bottom edge of the circle, we are always interested in the bottom half
    // solution. To calculate which is which, it depends on whether the vertical offset is positive
    // or negative.
    val (xSolution, ySolution) = if (b > 0) {
        // When the offset is positive, the top edge of the app bar is below the center of the
        // circle. The largest solution will be the one closest to the bottom of the circle, so we
        // pick that.
        if (ySolutionA > ySolutionB) xSolutionA to ySolutionA else xSolutionB to ySolutionB
    } else {
        // When the offset is negative, the top edge of the app bar is above the center of the
        // circle. The smallest solution will be the one closest to the top of the circle, so we
        // pick that.
        if (ySolutionA < ySolutionB) xSolutionA to ySolutionA else xSolutionB to ySolutionB
    }

    // If the calculated x coordinate is further away from the origin than the control point, the
    // curve will fold back on itself. In this scenario, we actually join the circle above the
    // center, so invert the y coordinate.
    val adjustedYSolution = if (xSolution < controlPointX) -ySolution else ySolution
    return xSolution to adjustedYSolution
}

/**
 * An empty App Bar that expands to the parent's width.
 *
 * For an App Bar that follows Material spec guidelines to be placed on the top of the screen, see
 * [TopAppBar].
 */
@Composable
private fun AppBar(
    color: Color,
    contentColor: Color,
    elevation: Dp,
    shape: Shape,
    children: @Composable() RowScope.() -> Unit
) {
    Surface(color = color, contentColor = contentColor, elevation = elevation, shape = shape) {
        Row(
            LayoutWidth.Fill + LayoutPadding(
                start = AppBarHorizontalPadding,
                end = AppBarHorizontalPadding
            ) + LayoutHeight(AppBarHeight),
            arrangement = Arrangement.SpaceBetween,
            children = children
        )
    }
}

private val AppBarHeight = 56.dp
// TODO: this should probably be part of the touch target of the start and end icons, clarify this
private val AppBarHorizontalPadding = 4.dp
// Start inset for the title when there is no navigation icon provided
private val TitleInsetWithoutIcon = 16.dp - AppBarHorizontalPadding
// Start inset for the title when there is a navigation icon provided
private val TitleInsetWithIcon = 72.dp - AppBarHorizontalPadding
// The baseline distance for the title from the bottom of the app bar
private val TitleBaselineOffset = 20.sp

private val BottomAppBarElevation = 8.dp
// TODO: clarify elevation in surface mapping - spec says 0.dp but it appears to have an
//  elevation overlay applied in dark theme examples.
private val TopAppBarElevation = 4.dp

// The gap on all sides between the FAB and the cutout
private val BottomAppBarCutoutOffset = 8.dp
// How far from the notch the rounded edges start
private val BottomAppBarRoundedEdgeRadius = 4.dp
