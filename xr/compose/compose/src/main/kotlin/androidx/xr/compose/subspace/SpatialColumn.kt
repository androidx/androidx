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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialBiasAbsoluteAlignment
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

/**
 * A layout composable that arranges its children in a vertical sequence.
 *
 * For arranging children horizontally, see [SpatialRow].
 *
 * @param modifier Modifiers to apply to the layout.
 * @param horizontalAlignment The default horizontal alignment for child elements within the column.
 * @param depthAlignment The default depth alignment for child elements within the column.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param content The composable content to be laid out vertically.
 */
@Composable
@SubspaceComposable
public inline fun SpatialColumn(
    modifier: SubspaceModifier = SubspaceModifier,
    horizontalAlignment: SpatialAlignment.Horizontal = SpatialAlignment.CenterHorizontally,
    depthAlignment: SpatialAlignment.Depth = SpatialAlignment.CenterDepthwise,
    verticalArrangement: SpatialArrangement.Vertical = SpatialArrangement.Center,
    crossinline content: @Composable @SubspaceComposable SpatialColumnScope.() -> Unit,
) {
    val measurePolicy =
        spatialColumnMeasurePolicy(
            horizontalAlignment = horizontalAlignment,
            depthAlignment = depthAlignment,
            verticalArrangement = verticalArrangement,
        )

    SubspaceLayout(
        modifier = modifier,
        content = { SpatialColumnScopeInstance.content() },
        coreEntityName = "SpatialColumn",
        measurePolicy = measurePolicy,
    )
}

@PublishedApi
@Composable
internal fun spatialColumnMeasurePolicy(
    horizontalAlignment: SpatialAlignment.Horizontal,
    depthAlignment: SpatialAlignment.Depth,
    verticalArrangement: SpatialArrangement.Vertical,
): SubspaceMeasurePolicy =
    if (
        horizontalAlignment == SpatialAlignment.CenterHorizontally &&
            depthAlignment == SpatialAlignment.CenterDepthwise &&
            verticalArrangement == SpatialArrangement.Center
    ) {
        DefaultSpatialColumnMeasurePolicy
    } else {
        remember(horizontalAlignment, depthAlignment, verticalArrangement) {
            SpatialColumnMeasurePolicy(
                alignment = horizontalAlignment + depthAlignment,
                verticalArrangement = verticalArrangement,
            )
        }
    }

/**
 * A layout composable that arranges its children in a vertical sequence.
 *
 * For arranging children horizontally, see [SpatialRow].
 *
 * @param modifier Modifiers to apply to the layout.
 * @param alignment The default alignment for child elements within the column.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param content The composable content to be laid out vertically.
 */
@Composable
@SubspaceComposable
@Deprecated("Use SpatialColumn with horizontalAlignment and depthAlignment instead.")
public inline fun SpatialColumn(
    modifier: SubspaceModifier = SubspaceModifier,
    alignment: SpatialAlignment,
    verticalArrangement: SpatialArrangement.Vertical = SpatialArrangement.Center,
    crossinline content: @Composable @SubspaceComposable SpatialColumnScope.() -> Unit,
) {
    @Suppress("DEPRECATION")
    val measurePolicy =
        spatialColumnMeasurePolicy(alignment = alignment, verticalArrangement = verticalArrangement)

    SubspaceLayout(
        modifier = modifier,
        content = { SpatialColumnScopeInstance.content() },
        coreEntityName = "SpatialColumn",
        measurePolicy = measurePolicy,
    )
}

internal val DefaultSpatialColumnMeasurePolicy: SubspaceMeasurePolicy =
    SpatialColumnMeasurePolicy(
        alignment = SpatialAlignment.CenterHorizontally + SpatialAlignment.CenterDepthwise,
        verticalArrangement = SpatialArrangement.Center,
    )

@PublishedApi
@Composable
@Deprecated("Use SpatialColumn with horizontalAlignment and depthAlignment instead.")
internal fun spatialColumnMeasurePolicy(
    alignment: SpatialAlignment,
    verticalArrangement: SpatialArrangement.Vertical,
): SubspaceMeasurePolicy =
    if (alignment == SpatialAlignment.Center && verticalArrangement == SpatialArrangement.Center) {
        DefaultSpatialColumnMeasurePolicy
    } else {
        remember(alignment, verticalArrangement) {
            SpatialColumnMeasurePolicy(
                alignment = alignment,
                verticalArrangement = verticalArrangement,
            )
        }
    }

/**
 * Measure policy for [SpatialColumn] layouts. Handles the measurement and placement of children in
 * a vertical sequence.
 */
internal class SpatialColumnMeasurePolicy(
    private val alignment: SpatialAlignment,
    private val verticalArrangement: SpatialArrangement.Vertical,
) : SubspaceMeasurePolicy, SpatialRowColumnMeasurePolicy() {

    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        return measure(
            measurables = measurables,
            constraints = constraints,
            arrangementSpacingInt = verticalArrangement.spacing.roundToPx(),
            mainAxisMultiplier = MainAxisMultiplier.VerticalAxisMultiplier,
            subspaceMeasureScope = this,
        )
    }

    override val SubspacePlaceable.mainAxisSize: Int
        get() = measuredHeight

    override val SubspacePlaceable.crossAxisSize: Int
        get() = measuredWidth

    override val VolumeConstraints.mainAxisTargetSpace: Int
        get() = if (maxHeight != VolumeConstraints.INFINITY) maxHeight else minHeight

    override val VolumeConstraints.mainAxisMin: Int
        get() = minHeight

    override val VolumeConstraints.crossAxisMin: Int
        get() = minWidth

    override val VolumeConstraints.crossAxisMax: Int
        get() = maxWidth

    override fun arrangeMainAxisPositions(
        mainAxisLayoutSize: Int,
        childrenMainAxisSize: IntArray,
        mainAxisPositions: IntArray,
        subspaceMeasureScope: SubspaceMeasureScope,
    ) {
        with(verticalArrangement) {
            subspaceMeasureScope.arrange(
                totalSize = mainAxisLayoutSize,
                sizes = childrenMainAxisSize,
                outPositions = mainAxisPositions,
            )
        }
    }

    override fun getMainAxisOffset(
        contentSize: IntVolumeSize,
        containerSize: IntVolumeSize,
        layoutDirection: LayoutDirection,
    ): Int {
        // Each child will have its main-axis offset adjusted, based on extra space available and
        // the provided alignment. `mainAxisOffset` represents the top edge of the content in the
        // container space.
        return (alignment.verticalOffset(contentSize.height, containerSize.height) +
                containerSize.height / 2.0)
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
            minWidth = crossAxisMin,
            maxWidth = crossAxisMax,
            minHeight = mainAxisMin,
            maxHeight = mainAxisMax,
            minDepth = minDepth,
            maxDepth = maxDepth,
        )
    }

    override fun VolumeConstraints.plusMainAxis(addToMainAxis: Int): VolumeConstraints {
        return VolumeConstraints(
            minWidth = 0,
            maxWidth = maxWidth,
            minHeight = 0,
            maxHeight = maxHeight + addToMainAxis,
            minDepth = 0,
            maxDepth = maxDepth,
        )
    }

    override fun contentSize(
        mainAxisLayoutSize: Int,
        crossAxisSize: Int,
        depthSize: Int,
    ): IntVolumeSize {
        return IntVolumeSize(width = crossAxisSize, height = mainAxisLayoutSize, depth = depthSize)
    }

    override fun Density.getPose(
        resolvedMeasurable: ResolvedMeasurable,
        containerSize: IntVolumeSize,
        mainAxisOffset: Int,
        layoutDirection: LayoutDirection,
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
            resolvedMeasurable.horizontalOffset(
                width = crossAxisSize,
                space = containerSize.width,
                parentSpatialAlignment = alignment,
                layoutDirection = layoutDirection,
            )

        val depthPosition =
            resolvedMeasurable.depthOffset(
                depth = placeable.measuredDepth,
                space = containerSize.depth,
                parentSpatialAlignment = alignment,
            )

        val position =
            Vector3(
                x = crossAxisPosition.toFloat(),
                y = mainAxisPosition.toFloat(),
                z = depthPosition.toFloat(),
            )
        val orientation = Quaternion.Identity

        return Pose(position, orientation)
    }
}

/** Scope for customizing the layout of children within a [SpatialColumn]. */
@LayoutScopeMarker
public interface SpatialColumnScope {
    /**
     * Sizes the element's height proportionally to its [weight] relative to other weighted sibling
     * elements in the [SpatialColumn].
     *
     * The parent divides the remaining vertical space after measuring unweighted children and
     * distributes it according to the weights.
     *
     * If [fill] is true, the element will occupy its entire allocated height. Otherwise, it can be
     * smaller, potentially making the [SpatialColumn] smaller as unused space isn't redistributed.
     *
     * @param weight The proportional height for this element relative to other weighted siblings.
     *   Must be positive.
     * @param fill Whether the element should fill its entire allocated height.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): SubspaceModifier

    /**
     * Aligns the element within the [SpatialColumn] horizontally. This will override the horizontal
     * alignment value passed to the [SpatialColumn].
     *
     * @param alignment The horizontal alignment to apply.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Horizontal): SubspaceModifier

    /**
     * Aligns the element within the [SpatialColumn] depthwise. This will override the depth
     * alignment value passed to the [SpatialColumn].
     *
     * @param alignment The depth alignment to use for the element.
     * @return The modified [SubspaceModifier].
     */
    public fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier
}

/** Default implementation of the [SpatialColumnScope] interface. */
@PublishedApi
internal object SpatialColumnScopeInstance : SpatialColumnScope {
    override fun SubspaceModifier.weight(weight: Float, fill: Boolean): SubspaceModifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this then
            LayoutWeightElement(
                // Coerce Float.POSITIVE_INFINITY to Float.MAX_VALUE to avoid errors
                weight = weight.coerceAtMost(Float.MAX_VALUE),
                fill = fill,
            )
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Horizontal): SubspaceModifier {
        return this then RowColumnAlignElement(horizontalSpatialAlignment = alignment)
    }

    override fun SubspaceModifier.align(alignment: SpatialAlignment.Depth): SubspaceModifier {
        return this then RowColumnAlignElement(depthSpatialAlignment = alignment)
    }
}

private operator fun SpatialAlignment.Horizontal.plus(
    other: SpatialAlignment.Depth
): SpatialAlignment =
    when (this) {
        is SpatialBiasAlignment.Horizontal ->
            SpatialBiasAlignment(
                horizontalBias = horizontalBias,
                verticalBias = 0f,
                depthBias =
                    when (other) {
                        is SpatialBiasAlignment.Depth -> other.depthBias
                        else -> 0f
                    },
            )

        is SpatialBiasAbsoluteAlignment.Horizontal ->
            SpatialBiasAbsoluteAlignment(
                horizontalBias = horizontalBias,
                verticalBias = 0f,
                depthBias =
                    when (other) {
                        is SpatialBiasAlignment.Depth -> other.depthBias
                        else -> 0f
                    },
            )

        else -> SpatialBiasAlignment(0f, 0f, 0f)
    }
