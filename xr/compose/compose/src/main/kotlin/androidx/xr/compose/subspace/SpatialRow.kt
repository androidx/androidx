/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialBiasAlignment
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * A layout composable that arranges its children in a horizontal sequence. For arranging children
 * vertically, see [SpatialColumn].
 *
 * @param modifier Appearance modifiers to apply to this Composable.
 * @param verticalAlignment The default vertical alignment for child elements within the row.
 * @param depthAlignment The default depth alignment for child elements within the row.
 * @param horizontalArrangement The horizontal arrangement of the children.
 * @param content The composable content to be laid out horizontally in the row.
 */
@Composable
@SubspaceComposable
public inline fun SpatialRow(
    modifier: SubspaceModifier = SubspaceModifier,
    verticalAlignment: SpatialAlignment.Vertical = SpatialAlignment.CenterVertically,
    depthAlignment: SpatialAlignment.Depth = SpatialAlignment.CenterDepthwise,
    horizontalArrangement: SpatialArrangement.Horizontal = SpatialArrangement.Center,
    crossinline content: @Composable @SubspaceComposable SpatialRowScope.() -> Unit,
) {
    val measurePolicy =
        spatialRowMeasurePolicy(
            curveRadius = Dp.Infinity,
            verticalAlignment = verticalAlignment,
            depthAlignment = depthAlignment,
            horizontalArrangement = horizontalArrangement,
        )

    SubspaceLayout(
        modifier = modifier,
        content = { SpatialRowScopeInstance.content() },
        coreEntityName = "SpatialRow",
        measurePolicy = measurePolicy,
    )
}

/**
 * A layout composable that arranges its children in a curved horizontal sequence.
 *
 * @param modifier Appearance modifiers to apply to this Composable.
 * @param verticalAlignment The default vertical alignment for child elements within the row.
 * @param depthAlignment The default depth alignment for child elements within the row.
 * @param horizontalArrangement The horizontal arrangement of the children.
 * @param curveRadius Defines the curve of the row by specifying its radius in Dp. A larger radius
 *   creates a gentler curve (less curvature), while a smaller positive radius results in a sharper
 *   curve (more curvature). Using [Dp.Infinity] or a non-positive value (zero or negative) makes
 *   the row straight. When curved, row items are angled to follow the curve's path. This value is
 *   the radial distance in the polar coordinate system.
 * @param content The composable content to be laid out horizontally in the row.
 */
@Composable
@SubspaceComposable
public inline fun SpatialCurvedRow(
    modifier: SubspaceModifier = SubspaceModifier,
    verticalAlignment: SpatialAlignment.Vertical = SpatialAlignment.CenterVertically,
    depthAlignment: SpatialAlignment.Depth = SpatialAlignment.CenterDepthwise,
    horizontalArrangement: SpatialArrangement.Horizontal = SpatialArrangement.Center,
    curveRadius: Dp = SpatialCurvedRowDefaults.curveRadius,
    crossinline content: @Composable @SubspaceComposable SpatialRowScope.() -> Unit,
) {
    val measurePolicy =
        spatialRowMeasurePolicy(
            curveRadius = if (curveRadius > 0.dp) curveRadius else Dp.Infinity,
            verticalAlignment = verticalAlignment,
            depthAlignment = depthAlignment,
            horizontalArrangement = horizontalArrangement,
        )

    SubspaceLayout(
        modifier = modifier,
        content = { SpatialRowScopeInstance.content() },
        coreEntityName = "SpatialRow",
        measurePolicy = measurePolicy,
    )
}

internal val DefaultSpatialRowMeasurePolicy: SubspaceMeasurePolicy =
    SpatialRowMeasurePolicy(
        curveRadius = Dp.Infinity,
        alignment = SpatialAlignment.CenterVertically + SpatialAlignment.CenterDepthwise,
        horizontalArrangement = SpatialArrangement.Center,
    )

@PublishedApi
@Composable
internal fun spatialRowMeasurePolicy(
    curveRadius: Dp,
    verticalAlignment: SpatialAlignment.Vertical,
    depthAlignment: SpatialAlignment.Depth,
    horizontalArrangement: SpatialArrangement.Horizontal,
): SubspaceMeasurePolicy =
    if (
        curveRadius == Dp.Infinity &&
            verticalAlignment == SpatialAlignment.CenterVertically &&
            depthAlignment == SpatialAlignment.CenterDepthwise &&
            horizontalArrangement == SpatialArrangement.Center
    ) {
        DefaultSpatialRowMeasurePolicy
    } else {
        remember(curveRadius, verticalAlignment, depthAlignment, horizontalArrangement) {
            SpatialRowMeasurePolicy(
                curveRadius = curveRadius,
                alignment = verticalAlignment + depthAlignment,
                horizontalArrangement = horizontalArrangement,
            )
        }
    }

/**
 * Measure policy for [SpatialRow] and [SpatialCurvedRow] layouts. Handles the measurement and
 * placement of children in a horizontal sequence, optionally along a curve.
 */
internal class SpatialRowMeasurePolicy(
    private val curveRadius: Dp,
    private val alignment: SpatialAlignment,
    private val horizontalArrangement: SpatialArrangement.Horizontal,
) : SubspaceMeasurePolicy, SpatialRowColumnMeasurePolicy() {

    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        return measure(
            measurables = measurables,
            constraints = constraints,
            arrangementSpacingInt = horizontalArrangement.spacing.roundToPx(),
            mainAxisMultiplier = MainAxisMultiplier.HorizontalAxisMultiplier,
            subspaceMeasureScope = this,
        )
    }

    override val SubspacePlaceable.mainAxisSize: Int
        get() = measuredWidth

    override val SubspacePlaceable.crossAxisSize: Int
        get() = measuredHeight

    override val VolumeConstraints.mainAxisTargetSpace: Int
        get() = if (maxWidth != VolumeConstraints.INFINITY) maxWidth else minWidth

    override val VolumeConstraints.mainAxisMin: Int
        get() = minWidth

    override val VolumeConstraints.crossAxisMin: Int
        get() = minHeight

    override val VolumeConstraints.crossAxisMax: Int
        get() = maxHeight

    override fun arrangeMainAxisPositions(
        mainAxisLayoutSize: Int,
        childrenMainAxisSize: IntArray,
        mainAxisPositions: IntArray,
        subspaceMeasureScope: SubspaceMeasureScope,
    ) {
        with(horizontalArrangement) {
            subspaceMeasureScope.arrange(
                totalSize = mainAxisLayoutSize,
                sizes = childrenMainAxisSize,
                layoutDirection = subspaceMeasureScope.layoutDirection,
                outPositions = mainAxisPositions,
            )
        }
    }

    override fun getMainAxisOffset(contentSize: IntVolumeSize, containerSize: IntVolumeSize): Int {
        // Each child will have its main-axis offset adjusted, based on extra space available and
        // the provided alignment. `mainAxisOffset` represents the left edge of the content
        // in the container space.
        return (alignment.horizontalOffset(contentSize.width, containerSize.width) -
                containerSize.width / 2.0)
            .fastRoundToInt()
    }

    override fun buildConstraints(
        mainAxisMin: Int,
        mainAxisMax: Int,
        crossAxisMin: Int,
        crossAxisMax: Int,
        minDepth: Int,
        maxDepth: Int,
    ): VolumeConstraints {
        return VolumeConstraints(
            minWidth = mainAxisMin,
            maxWidth = mainAxisMax,
            minHeight = crossAxisMin,
            maxHeight = crossAxisMax,
            minDepth = minDepth,
            maxDepth = maxDepth,
        )
    }

    override fun VolumeConstraints.plusMainAxis(addToMainAxis: Int): VolumeConstraints {
        return VolumeConstraints(
            minWidth = 0,
            maxWidth = maxWidth + addToMainAxis,
            minHeight = 0,
            maxHeight = maxHeight,
            minDepth = 0,
            maxDepth = maxDepth,
        )
    }

    override fun contentSize(
        mainAxisLayoutSize: Int,
        crossAxisSize: Int,
        depthSize: Int,
    ): IntVolumeSize {
        return IntVolumeSize(width = mainAxisLayoutSize, height = crossAxisSize, depth = depthSize)
    }

    override fun Density.getPose(
        resolvedMeasurable: ResolvedMeasurable,
        containerSize: IntVolumeSize,
        mainAxisOffset: Int,
    ): Pose {
        val mainAxisPosition = (resolvedMeasurable.mainAxisPosition ?: 0) + mainAxisOffset

        val placeable =
            checkNotNull(resolvedMeasurable.placeable) {
                "Placeable cannot be null when getPose is called. Measurement phase might have failed for this item."
            }

        // Set child's cross-axis position based on its desired size + the container's
        // size/alignment.
        val crossAxisSize = placeable.crossAxisSize
        val crossAxisPosition =
            resolvedMeasurable.verticalOffset(
                height = crossAxisSize,
                space = containerSize.height,
                parentSpatialAlignment = alignment,
            )

        val depthPosition =
            resolvedMeasurable.depthOffset(
                depth = placeable.measuredDepth,
                space = containerSize.depth,
                parentSpatialAlignment = alignment,
            )

        var position =
            Vector3(
                x = mainAxisPosition.toFloat(),
                y = crossAxisPosition.toFloat(),
                z = depthPosition.toFloat(),
            )
        var orientation = Quaternion.Identity

        if (curveRadius != Dp.Infinity) {
            val pixelsCurveRadius = curveRadius.toPx()
            // NOTE: Orientation needs to be computed first, otherwise position
            // gets overwritten with the new position which will lead to an
            // incorrect orientation calculation.
            orientation = getOrientationTangentToCircle(position, pixelsCurveRadius)
            position = getPositionOnCircle(position, pixelsCurveRadius)
        }

        return Pose(position, orientation)
    }

    /**
     * Calculates the 3D position of an item on a circle, used for curved [SpatialRow] layouts.
     * NOTE: This method is specifically for horizontal (row-like) curves.
     *
     * @param position The initial calculated 1D position along the curve's arc length (x-axis), and
     *   its y/z offsets.
     * @param radius The radius of the curve in pixels.
     * @return The adjusted [Vector3] position on the circle in 3D space.
     */
    // [radius], like [position], should be in pixels.
    private fun getPositionOnCircle(position: Vector3, radius: Float): Vector3 {
        // NOTE: This method is hard coded to work with rows.  Needs to be made
        // slightly more general to work with columns.
        val arclength = position.x // Signed, negative means arc extends to left.
        val theta = arclength / radius
        val x = radius * sin(theta)
        val y = position.y
        val z = radius * (1.0f - cos(theta)) + position.z // z offset from the chord

        return Vector3(x.toInt().toFloat(), y.toInt().toFloat(), z.toInt().toFloat())
    }

    /**
     * Calculates the orientation for an item to be tangent to a circle, for curved [SpatialRow].
     * NOTE: This method is specifically for horizontal (row-like) curves.
     *
     * @param position The initial calculated 1D position along the curve's arc length (x-axis).
     * @param radius The radius of the curve in pixels.
     * @return The [Quaternion] orientation to make the item tangent to the circle.
     */
    // [radius], like [position], should be in pixels.
    private fun getOrientationTangentToCircle(position: Vector3, radius: Float): Quaternion {
        // NOTE: This method is hard coded to work with rows.  Needs to be made
        // slightly more general to work with columns.
        val arclength = position.x // Signed, negative means arc extends to left.
        val theta = arclength / radius

        // We need to rotate by negative theta (clockwise) around the Y axis.
        val qX = 0.0f
        val qY = sin(-theta * 0.5f)
        val qZ = 0.0f
        val qW = cos(-theta * 0.5f)

        return Quaternion(qX, qY, qZ, qW)
    }
}

/** Scope for customizing the layout of children within a [SpatialRow]. */
@LayoutScopeMarker
public interface SpatialRowScope {
    /**
     * Sizes the element's width proportionally to its [weight] relative to other weighted sibling
     * elements in the [SpatialRow].
     *
     * The parent divides the remaining horizontal space after measuring unweighted children and
     * distributes it according to the weights.
     *
     * If [fill] is true, the element will occupy its entire allocated width. Otherwise, it can be
     * smaller, potentially making the [SpatialRow] smaller as unused space isn't redistributed.
     *
     * @param weight The proportional width for this element relative to other weighted siblings.
     *   Must be positive.
     * @param fill Whether the element should fill its entire allocated width.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): SubspaceModifier

    /**
     * Aligns the element vertically within the [SpatialRow], overriding the row's default vertical
     * alignment.
     *
     * @param alignment The vertical alignment to apply.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Vertical): SubspaceModifier

    /**
     * Aligns the element depthwise within the [SpatialRow], overriding the row's default depth
     * alignment.
     *
     * @param alignment The depth alignment to apply.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier
}

/** Contains the default values used by [SpatialCurvedRow]. */
public object SpatialCurvedRowDefaults {
    /** Default curve radius used by [SpatialCurvedRow]. */
    public val curveRadius: Dp = 825.dp
}

/** Default implementation of the [SpatialRowScope] interface. */
@PublishedApi
internal object SpatialRowScopeInstance : SpatialRowScope {
    override fun SubspaceModifier.weight(weight: Float, fill: Boolean): SubspaceModifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this then
            LayoutWeightElement(
                // Coerce Float.POSITIVE_INFINITY to Float.MAX_VALUE to avoid errors
                weight = weight.coerceAtMost(Float.MAX_VALUE),
                fill = fill,
            )
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Vertical): SubspaceModifier {
        return this then RowColumnAlignElement(verticalSpatialAlignment = alignment)
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier {
        return this then RowColumnAlignElement(depthSpatialAlignment = alignment)
    }
}

private operator fun SpatialAlignment.Vertical.plus(
    other: SpatialAlignment.Depth
): SpatialAlignment =
    when (this) {
        is SpatialBiasAlignment.Vertical ->
            SpatialBiasAlignment(
                horizontalBias = 0f,
                verticalBias = bias,
                depthBias =
                    when (other) {
                        is SpatialBiasAlignment.Depth -> other.bias
                        else -> 0f
                    },
            )

        else -> SpatialBiasAlignment(0f, 0f, 0f)
    }
