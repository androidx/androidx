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

package androidx.xr.compose.spatial

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.graphics.drawable.toDrawable
import androidx.xr.compose.R
import androidx.xr.compose.platform.LocalCoreMainPanelEntity
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.findNearestParentEntity
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.subspace.MAX_SAFE_PANEL_HEIGHT_PX
import androidx.xr.compose.subspace.MAX_SAFE_PANEL_WIDTH_PX
import androidx.xr.compose.subspace.SpatialComposeView
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.SpatialAbsoluteAlignment
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialBiasAbsoluteAlignment
import androidx.xr.compose.subspace.layout.SpatialBiasAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.node.SubspaceNodeApplier
import androidx.xr.compose.subspace.spatialComposeView
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.pxToMeters
import androidx.xr.compose.unit.toMeters
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PixelDensity
import androidx.xr.scenecore.scene

/** Set the scrim alpha to 32% opacity across orbiters. */
private const val DEFAULT_SCRIM_ALPHA = 0x52000000

/** Contains default values used by Orbiters. */
public object OrbiterDefaults {
    /** Default shape for an Orbiter. */
    public val Shape: SpatialShape = SpatialRoundedCornerShape(ZeroCornerSize)
    /** Default elevation level for an Orbiter. */
    public val Elevation: Dp = SpatialElevationLevel.Level1
}

private val EmptyContent: @Composable () -> Unit = {}

/**
 * A composable that creates an orbiter along the edge or center of a parent spatial component.
 *
 * Orbiters are floating elements that are typically used to control the content within spatial
 * panels and other entities that they're anchored to. They allow the content to have more space and
 * give users quick access to features like navigation without obstructing the main content.
 *
 * Sizing constraints depend on where the orbiter is declared:
 * * Within a [Subspace]: the nearest parent spatial component (e.g.,
 *   [androidx.xr.compose.subspace.SpatialPanel]) is the parent.
 * * Within `setContent`: the main panel is the parent.
 *
 * @sample androidx.xr.compose.samples.OrbiterBottomBarSample
 * @sample androidx.xr.compose.samples.OrbiterTopRailSample
 * @sample androidx.xr.compose.samples.OrbiterSideRailSample
 * @param position position of the orbiter relative to the parent spatial component, defined by its
 *   alignment, edge alignment, and offset
 * @param shape shape of the orbiter when rendered in 3D space
 * @param content content to display inside the orbiter
 */
@Composable
@ComposableOpenTarget(index = -1)
public fun Orbiter(
    position: OrbiterPosition,
    shape: SpatialShape = OrbiterDefaults.Shape,
    content: @Composable @UiComposable () -> Unit,
) {
    val movableContent = remember { movableContentOf(content) }
    if (
        currentComposer.applier !is SubspaceNodeApplier &&
            !LocalSpatialCapabilities.current.isSpatialUiEnabled
    ) {
        movableContent()
        return
    }

    val layoutDirection = LocalLayoutDirection.current
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val parentView = LocalView.current
    val localId = currentCompositeKeyHashCode
    val context = LocalContext.current
    val compositionContext = rememberCompositionContext()
    val parentEntity: CoreEntity? = findNearestParentEntity()
    val density = LocalDensity.current

    val pixelDensity = session.scene.virtualPixelDensity

    val poseProvider =
        remember(position, layoutDirection, density) {
            OrbiterPoseProvider { targetSize, orbiterContentSize ->
                val spatialAlignment = position.alignment
                val offset = position.offset

                val baseAlignmentOffset =
                    spatialAlignment.align(
                        size =
                            IntVolumeSize(
                                width = orbiterContentSize.width,
                                height = orbiterContentSize.height,
                                depth = 0,
                            ),
                        space =
                            IntVolumeSize(
                                width = targetSize.width,
                                height = targetSize.height,
                                depth = 0,
                            ),
                        layoutDirection = layoutDirection,
                    )
                val baseAlignmentVector =
                    Vector3(
                        x = baseAlignmentOffset.x.pxToMeters(pixelDensity),
                        y = baseAlignmentOffset.y.pxToMeters(pixelDensity),
                        z = baseAlignmentOffset.z.pxToMeters(pixelDensity),
                    )

                val horizontalBias =
                    when (spatialAlignment) {
                        is SpatialBiasAlignment -> spatialAlignment.horizontalBias
                        is SpatialBiasAbsoluteAlignment -> spatialAlignment.horizontalBias
                        else -> 0f
                    }
                val verticalBias =
                    when (spatialAlignment) {
                        is SpatialBiasAlignment -> spatialAlignment.verticalBias
                        is SpatialBiasAbsoluteAlignment -> spatialAlignment.verticalBias
                        else -> 0f
                    }

                val resolvedHorizontalBias =
                    when (spatialAlignment) {
                        is SpatialBiasAlignment -> {
                            if (layoutDirection == LayoutDirection.Ltr) horizontalBias
                            else -horizontalBias
                        }

                        is SpatialBiasAbsoluteAlignment -> horizontalBias
                        else -> 0f
                    }

                // SpatialAlignment positions the orbiter completely inside the parent.
                // We use the OrbiterPosition.EdgeAlignment to determine how far outward to shift:
                // - Inside (0f): No shift, orbiter remains inside the parent.
                // - Center (1f): Shifted by half its size, so its center rests on the parent's
                // edge.
                // - Outside (2f): Shifted by full size, so it sits completely outside the parent.
                var xEdgeOffset = 0f
                var yEdgeOffset = 0f
                val orbiterHalfSize = orbiterContentSize.toMeterSize(pixelDensity) / 2f

                val horizontalEdgeOffsetMultiplier =
                    when (position.horizontalEdgeAlignment) {
                        OrbiterPosition.EdgeAlignment.Outside -> 2f
                        OrbiterPosition.EdgeAlignment.Inside -> 0f
                        else -> 1f // Center
                    }

                val verticalEdgeOffsetMultiplier =
                    when (position.verticalEdgeAlignment) {
                        OrbiterPosition.EdgeAlignment.Outside -> 2f
                        OrbiterPosition.EdgeAlignment.Inside -> 0f
                        else -> 1f // Center
                    }

                if (resolvedHorizontalBias == -1f || resolvedHorizontalBias == 1f) {
                    xEdgeOffset =
                        resolvedHorizontalBias *
                            orbiterHalfSize.width *
                            horizontalEdgeOffsetMultiplier
                }
                if (verticalBias == -1f || verticalBias == 1f) {
                    yEdgeOffset =
                        verticalBias * orbiterHalfSize.height * verticalEdgeOffsetMultiplier
                }

                val edgeOffsetVector = Vector3(x = xEdgeOffset, y = yEdgeOffset, z = 0f)
                val userOffsetVector =
                    offset.toMeterVector(density = density, pixelDensity = pixelDensity)
                val resolvedUserOffsetVector =
                    if (spatialAlignment is SpatialBiasAbsoluteAlignment) {
                        userOffsetVector
                    } else {
                        Vector3(
                            x = userOffsetVector.x * layoutDirection.multiplier,
                            y = userOffsetVector.y,
                            z = userOffsetVector.z,
                        )
                    }

                Pose(
                    translation = baseAlignmentVector + edgeOffsetVector + resolvedUserOffsetVector,
                    rotation = Quaternion.Identity,
                )
            }
        }

    val holder =
        remember(parentView) {
            SpatialOrbiter(
                context = context,
                parentView = parentView,
                compositionContext = compositionContext,
                session = session,
                localId = localId,
                initialPoseProvider = poseProvider,
                initialShape = shape,
                pixelDensity = pixelDensity,
            )
        }
    SideEffect {
        holder.parentEntity = parentEntity
        holder.poseProvider = poseProvider
        holder.shape = shape
        holder.content = movableContent
    }
}

/**
 * Represents the position of an [Orbiter] in relation to the parent, defined by its alignment, edge
 * alignment, and offset. The edge alignment can be configured for both vertical bounds (i.e., the
 * top or bottom edges of a panel) and horizontal bounds (i.e., the start or end sides of the panel)
 * depending on the specified alignment.
 *
 * TopStart horizontalEdgeAlignment = EdgeAlignment.Outside and verticalEdgeAlignment =
 * EdgeAlignment.Outside, Left-to-Right (LTR):
 * ```
 * +---------+
 * | Orbiter |
 * +---------+------------------------+
 *           |                        |
 *           |                        |
 *           |      SpatialPanel      |
 *           |                        |
 *           |                        |
 *           +------------------------+
 * ```
 *
 * TopStart horizontalEdgeAlignment = EdgeAlignment.Outside, verticalEdgeAlignment =
 * EdgeAlignment.Inside, Left-to-Right (LTR):
 * ```
 * +---------+------------------------+
 * | Orbiter |                        |
 * +---------+                        |
 *           |      SpatialPanel      |
 *           |                        |
 *           |                        |
 *           +------------------------+
 * ```
 *
 * TopStart horizontalEdgeAlignment = EdgeAlignment.Inside, verticalEdgeAlignment =
 * EdgeAlignment.Outside, Left-to-Right (LTR):
 * ```
 *     +---------+
 *     | Orbiter |
 *     +---------+--------------+
 *     |                        |
 *     |      SpatialPanel      |
 *     |                        |
 *     |                        |
 *     +------------------------+
 * ```
 *
 * TopStart horizontalEdgeAlignment = EdgeAlignment.Inside and verticalEdgeAlignment =
 * EdgeAlignment.Inside, Left-to-Right (LTR):
 * ```
 * +---------+--------------+
 * | Orbiter |              |
 * +---------+              |
 * |      SpatialPanel      |
 * |                        |
 * |                        |
 * +------------------------+
 * ```
 *
 * TopStart horizontalEdgeAlignment = EdgeAlignment.Center and verticalEdgeAlignment =
 * EdgeAlignment.Center, Left-to-Right (LTR):
 * ```
 * +---------+
 * | Orbiter |------------------+
 * +---------+                  |
 *     |                        |
 *     |      SpatialPanel      |
 *     |                        |
 *     |                        |
 *     +------------------------+
 * ```
 *
 * BottomCenter verticalEdgeAlignment = EdgeAlignment.Center:
 * ```
 * +------------------------+
 * |                        |
 * |                        |
 * |      SpatialPanel      |
 * |                        |
 * |      +---------+       |
 * +------| Orbiter |-------+
 *        +---------+
 * ```
 */
public sealed class OrbiterPosition
private constructor(
    internal val alignment: SpatialAlignment,
    internal val horizontalEdgeAlignment: EdgeAlignment,
    internal val verticalEdgeAlignment: EdgeAlignment,
    internal val offset: DpVolumeOffset,
) {
    /**
     * Center of the parent (non-edge).
     *
     * @param offset manual offset applied to the orbiter. This offset will automatically adjust the
     *   horizontal offset according to the layout direction: when the layout direction is LTR,
     *   positive x offsets will move the content to the right and when the layout direction is RTL,
     *   positive x offsets will move the content to the left.
     */
    public class Center(
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation)
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.Center,
            horizontalEdgeAlignment = EdgeAlignment.Center,
            verticalEdgeAlignment = EdgeAlignment.Center,
            offset = offset,
        )

    /**
     * Top-Start edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the start edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the top edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied. This
     *   offset will automatically adjust the horizontal offset according to the layout direction:
     *   when the layout direction is LTR, positive x offsets will move the content to the right and
     *   when the layout direction is RTL, positive x offsets will move the content to the left.
     */
    public class TopStart(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.TopStart,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Top-Center edge alignment.
     *
     * @param verticalEdgeAlignment boundary offset behavior relative to the top edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the [verticalEdgeAlignment]'s offset
     *   is applied. This offset will automatically adjust the horizontal offset according to the
     *   layout direction: when the layout direction is LTR, positive x offsets will move the
     *   content to the right and when the layout direction is RTL, positive x offsets will move the
     *   content to the left.
     */
    public class TopCenter(
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.TopCenter,
            horizontalEdgeAlignment = EdgeAlignment.Center,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Top-End edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the end edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the top edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied. This
     *   offset will automatically adjust the horizontal offset according to the layout direction:
     *   when the layout direction is LTR, positive x offsets will move the content to the right and
     *   when the layout direction is RTL, positive x offsets will move the content to the left.
     */
    public class TopEnd(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.TopEnd,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Center-Start edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the start edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the [horizontalEdgeAlignment]'s
     *   offset is applied. This offset will automatically adjust the horizontal offset according to
     *   the layout direction: when the layout direction is LTR, positive x offsets will move the
     *   content to the right and when the layout direction is RTL, positive x offsets will move the
     *   content to the left.
     */
    public class CenterStart(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.CenterStart,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = EdgeAlignment.Center,
            offset = offset,
        )

    /**
     * Center-End edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the end edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the [horizontalEdgeAlignment]'s
     *   offset is applied. This offset will automatically adjust the horizontal offset according to
     *   the layout direction: when the layout direction is LTR, positive x offsets will move the
     *   content to the right and when the layout direction is RTL, positive x offsets will move the
     *   content to the left.
     */
    public class CenterEnd(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.CenterEnd,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = EdgeAlignment.Center,
            offset = offset,
        )

    /**
     * Bottom-Start edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the start edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the bottom edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied. This
     *   offset will automatically adjust the horizontal offset according to the layout direction:
     *   when the layout direction is LTR, positive x offsets will move the content to the right and
     *   when the layout direction is RTL, positive x offsets will move the content to the left.
     */
    public class BottomStart(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.BottomStart,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Bottom-Center edge alignment.
     *
     * @param verticalEdgeAlignment boundary offset behavior relative to the bottom edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the [verticalEdgeAlignment]'s offset
     *   is applied. This offset will automatically adjust the horizontal offset according to the
     *   layout direction: when the layout direction is LTR, positive x offsets will move the
     *   content to the right and when the layout direction is RTL, positive x offsets will move the
     *   content to the left.
     */
    public class BottomCenter(
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.BottomCenter,
            horizontalEdgeAlignment = EdgeAlignment.Center,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Bottom-End edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the end edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the bottom edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied. This
     *   offset will automatically adjust the horizontal offset according to the layout direction:
     *   when the layout direction is LTR, positive x offsets will move the content to the right and
     *   when the layout direction is RTL, positive x offsets will move the content to the left.
     */
    public class BottomEnd(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAlignment.BottomEnd,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Top-Left absolute edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the left edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the top edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied without
     *   considering layout direction
     */
    public class TopLeft(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAbsoluteAlignment.TopLeft,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Top-Right absolute edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the right edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the top edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied without
     *   considering layout direction
     */
    public class TopRight(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAbsoluteAlignment.TopRight,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Center-Left absolute edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the left edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the [horizontalEdgeAlignment]'s
     *   offset is applied without considering layout direction
     */
    public class CenterLeft(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAbsoluteAlignment.CenterLeft,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = EdgeAlignment.Center,
            offset = offset,
        )

    /**
     * Center-Right absolute edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the right edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the [horizontalEdgeAlignment]'s
     *   offset is applied without considering layout direction
     */
    public class CenterRight(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAbsoluteAlignment.CenterRight,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = EdgeAlignment.Center,
            offset = offset,
        )

    /**
     * Bottom-Left absolute edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the left edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the bottom edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied without
     *   considering layout direction
     */
    public class BottomLeft(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAbsoluteAlignment.BottomLeft,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Bottom-Right absolute edge alignment.
     *
     * @param horizontalEdgeAlignment boundary offset behavior relative to the right edge of the
     *   parent's bounds
     * @param verticalEdgeAlignment boundary offset behavior relative to the bottom edge of the
     *   parent's bounds
     * @param offset manual offset applied to the orbiter after the edge offset is applied without
     *   considering layout direction
     */
    public class BottomRight(
        horizontalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        verticalEdgeAlignment: EdgeAlignment = EdgeAlignment.Outside,
        offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    ) :
        OrbiterPosition(
            alignment = SpatialAbsoluteAlignment.BottomRight,
            horizontalEdgeAlignment = horizontalEdgeAlignment,
            verticalEdgeAlignment = verticalEdgeAlignment,
            offset = offset,
        )

    /**
     * Specifies how the [Orbiter] is aligned relative to its parent's layout boundary.
     *
     * Note: Calculates alignment based on the parent's rectangular layout bounds, not its visual
     * shape or rounded corners. Use a custom offset in [DpVolumeOffset] to align with curved or
     * non-rectangular contours.
     */
    @JvmInline
    public value class EdgeAlignment private constructor(private val value: Int) {
        public companion object {
            /**
             * Positions the [Orbiter] fully outside the parent's layout boundary.
             *
             * The orbiter does not overlap the parent panel's layout bounds. For example, a side
             * rail will sit completely to the side of the parent.
             *
             * ```
             *           +------------------------+
             *           |                        |
             * +---------+                        |
             * | Orbiter |      SpatialPanel      |
             * +---------+                        |
             *           |                        |
             *           +------------------------+
             * ```
             */
            public val Outside: EdgeAlignment = EdgeAlignment(0)

            /**
             * Positions the [Orbiter] fully inside the parent's layout boundary.
             *
             * The orbiter completely overlaps the parent panel's layout bounds, functioning as an
             * overlay.
             *
             * ```
             * +------------------------+
             * |      SpatialPanel      |
             * +-----------+            |
             * |  Orbiter  |            |
             * +-----------+            |
             * |                        |
             * +------------------------+
             * ```
             */
            public val Inside: EdgeAlignment = EdgeAlignment(1)

            /**
             * Centers the [Orbiter] directly on the parent's layout boundary line.
             *
             * The boundary line bisects the orbiter, placing it half-inside and half-outside the
             * parent.
             *
             * ```
             *       +------------------------+
             *       |                        |
             * +-----|-----+                  |
             * |  Orbiter  | SpatialPanel     |
             * +-----|-----+                  |
             *       |                        |
             *       +------------------------+
             * ```
             */
            public val Center: EdgeAlignment = EdgeAlignment(2)
        }
    }
}

/** Calculates the [Pose] of an [Orbiter] in 3D space relative to its spatial parent. */
private fun interface OrbiterPoseProvider {
    /**
     * Calculate the [Pose] of the [Orbiter].
     *
     * @param anchorSize The size of the [Orbiter]'s anchor target in pixels.
     * @param orbiterContentSize The size of the [Orbiter]'s content in pixels.
     * @return The [Pose] of the [Orbiter] in meters.
     */
    fun calculatePose(anchorSize: IntSize, orbiterContentSize: IntSize): Pose
}

@Composable
private fun PanelScrim() {
    val view = LocalView.current
    val dialogManager = LocalDialogManager.current
    val isDialogActive = dialogManager.isSpatialDialogActive.value
    if (isDialogActive) {
        Box(
            modifier =
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures { /* Prevent clicks to compose */ }
                }
        )
    }
    SideEffect {
        view.foreground =
            if (isDialogActive) {
                DEFAULT_SCRIM_ALPHA.toDrawable()
            } else {
                Color.TRANSPARENT.toDrawable()
            }
    }
}

private fun getWindowBoundsInPixels(context: Context): IntSize2d =
    (context as Activity).window.decorView.run { IntSize2d(width, height) }

/**
 * Provides the dimensions of the Android main window.
 *
 * Remembers and provides the size of the main window. It initializes the size from the main window
 * and keeps it updated by listening to layout changes on the decorView.
 *
 * The "main window" refers to the top-level window of an Android activity. It's the 2D Android
 * equivalent concept to the Android XR’s main panel.
 */
@Composable
private fun getMainWindowSize(session: Session): IntVolumeSize {
    val context = LocalContext.current
    var panelSize by
        remember(session) {
            val initialPixelDimensions = getWindowBoundsInPixels(context)
            mutableStateOf(
                IntVolumeSize(initialPixelDimensions.width, initialPixelDimensions.height, 0)
            )
        }
    val mainView = (context as Activity).window.decorView
    DisposableEffect(Unit) {
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val newSize = getWindowBoundsInPixels(context).run { IntVolumeSize(width, height, 0) }
            if (panelSize != newSize) {
                panelSize = newSize
            }
        }
        mainView.addOnLayoutChangeListener(listener)
        onDispose { mainView.removeOnLayoutChangeListener(listener) }
    }
    return panelSize
}

/**
 * A helper class that manages the lifecycle and composition of an Orbiter.
 *
 * It implements [RememberObserver] to tie the creation and disposal of the necessary infrastructure
 * ([ComposeView] and [CorePanelEntity]) to the lifecycle of the composable that uses it.
 *
 * @param context The Android [Context] used to create the internal [ComposeView].
 * @param parentView The parent Android [View] used to establish View Tree ownership (Lifecycle,
 *   ViewModel, etc.).
 * @param compositionContext The [CompositionContext] of the parent composable to link the new
 *   composition tree.
 * @param session The active XR [Session] required for creating the [PanelEntity].
 * @param localId A unique ID used for saving/restoring state within the Orbiter's composition.
 * @param initialPoseProvider The initial Pose provider for the `SpatialOrbiter`.
 * @param initialShape The initial SpatialShape of the `SpatialOrbiter`.
 */
private class SpatialOrbiter(
    private var context: Context,
    private var parentView: View,
    private var compositionContext: CompositionContext,
    private var session: Session,
    private var pixelDensity: PixelDensity,
    private var localId: Long,
    initialPoseProvider: OrbiterPoseProvider,
    initialShape: SpatialShape,
) : RememberObserver {
    private var view: SpatialComposeView? = null
    private var panelEntity: CorePanelEntity? = null
    var content: @Composable () -> Unit by mutableStateOf(EmptyContent)
    var poseProvider: OrbiterPoseProvider by mutableStateOf(initialPoseProvider)
    var shape: SpatialShape by mutableStateOf(initialShape)
    var parentEntity: CoreEntity? = null
        set(value) {
            if (field != value) {
                field = value
                panelEntity?.parent = value
            }
        }

    override fun onRemembered() {
        val view = spatialComposeView(parentView, context, compositionContext, localId)
        this.view = view
        panelEntity =
            CorePanelEntity(
                    pixelDensity = pixelDensity,
                    entity =
                        PanelEntity.create(
                            session = session,
                            parent = null,
                            view = view,
                            pixelDimensions = IntSize2d(0, 0),
                            name = "Orbiter:${view.id}",
                        ),
                )
                .apply {
                    this.enabled = false
                    view.setTag(R.id.compose_xr_local_view_entity, this)
                }
        view.setContent {
            val panelSize: IntVolumeSize =
                if (parentEntity == LocalCoreMainPanelEntity.current) {
                    getMainWindowSize(session)
                } else {
                    parentEntity?.mutableSize ?: IntVolumeSize.Zero
                }
            val anchorSize = IntSize(panelSize.width, panelSize.height)
            val constraints =
                safeConstraints(
                    maxWidth = panelSize.width.coerceAtMost(MAX_SAFE_PANEL_WIDTH_PX),
                    maxHeight = panelSize.height.coerceAtMost(MAX_SAFE_PANEL_HEIGHT_PX),
                )
            Layout(content = content) { measurables, _ ->
                val placeables = measurables.fastMap { it.measure(constraints) }
                val contentSize =
                    placeables.fastFold(IntSize.Zero) { acc, placeable ->
                        IntSize(
                            acc.width.coerceAtLeast(placeable.width),
                            acc.height.coerceAtLeast(placeable.height),
                        )
                    }
                layout(contentSize.width, contentSize.height) {
                    placeables.fastForEach { it.place(0, 0) }
                    panelEntity?.size = IntVolumeSize(contentSize.width, contentSize.height, 0)
                    val pose =
                        poseProvider.calculatePose(
                            anchorSize = anchorSize,
                            orbiterContentSize = contentSize,
                        )
                    panelEntity?.poseInMeters = pose
                    panelEntity?.parent = parentEntity
                    panelEntity?.setShape(shape, this@Layout)
                    panelEntity?.enabled = true
                }
            }
            // The scrim needs to be after the content so that it can capture input.
            PanelScrim()
        }
    }

    override fun onForgotten() {
        panelEntity?.dispose()
        view?.disposeComposition()
    }

    override fun onAbandoned() {
        // No-op. If resources were created during 'init' (constructor),
        // they should be released here since onRemembered() was never called.
    }
}

/**
 * Maximum dimension safely representable along both axes in [Constraints].
 *
 * [Constraints] packs dimensions into 31 bits across both axes. Dimensions up to 32,766 use at most
 * 15 bits each (30 bits total), ensuring they fit without overflowing the bit budget.
 */
private const val MAX_SUPPORTED_CONSTRAINT_DIMENSION = 32766

/**
 * Creates a [Constraints] with the specified [maxWidth] and [maxHeight], gracefully falling back to
 * clamped dimensions if the values exceed what 2D Compose can pack into 31 bits.
 */
private fun safeConstraints(maxWidth: Int, maxHeight: Int): Constraints {
    val nonNegativeWidth = maxWidth.coerceAtLeast(0)
    val nonNegativeHeight = maxHeight.coerceAtLeast(0)
    return try {
        Constraints(maxWidth = nonNegativeWidth, maxHeight = nonNegativeHeight)
    } catch (e: IllegalArgumentException) {
        val clampedWidth = nonNegativeWidth.coerceAtMost(MAX_SUPPORTED_CONSTRAINT_DIMENSION)
        val clampedHeight = nonNegativeHeight.coerceAtMost(MAX_SUPPORTED_CONSTRAINT_DIMENSION)
        Constraints(maxWidth = clampedWidth, maxHeight = clampedHeight)
    }
}

private val LayoutDirection.multiplier: Float
    get() = if (this == LayoutDirection.Ltr) 1f else -1f

private fun IntSize.toMeterSize(pixelDensity: PixelDensity): FloatSize2d =
    FloatSize2d(width = width.pxToMeters(pixelDensity), height = height.pxToMeters(pixelDensity))

private fun DpVolumeOffset.toMeterVector(density: Density, pixelDensity: PixelDensity): Vector3 =
    Vector3(
        x = x.toMeters(density, pixelDensity),
        y = y.toMeters(density, pixelDensity),
        z = z.toMeters(density, pixelDensity),
    )
