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
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.node.SubspaceNodeApplier
import androidx.xr.compose.subspace.spatialComposeView
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.toMeter
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity

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
 * A composable that creates an orbiter along the top or bottom edges of a view.
 *
 * Orbiters are floating elements that are typically used to control the content within spatial
 * panels and other entities that they're anchored to. They allow the content to have more space and
 * give users quick access to features like navigation without obstructing the main content.
 *
 * The size of the [Orbiter] is constrained by the dimensions of the parent spatial component it is
 * anchored to (e.g., a [androidx.xr.compose.subspace.SpatialPanel]). If it's not placed within a
 * specific spatial component, it defaults to the main window's size. Consequently, an [Orbiter]'s
 * content cannot be larger than its parent's dimensions.
 *
 * @param position The edge of the orbiter. Use [ContentEdge.Top] or [ContentEdge.Bottom].
 * @param offset The offset of the orbiter based on the outer edge of the orbiter.
 * @param offsetType The type of offset used for positioning the orbiter.
 * @param alignment The alignment of the orbiter. Use [Alignment.CenterHorizontally] or
 *   [Alignment.Start] or [Alignment.End].
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param elevation The z-direction elevation level of this Orbiter.
 * @param shouldRenderInNonSpatial In a non-spatial environment, if `true` the orbiter content is
 *   rendered as if the orbiter wrapper was not present and removed from the flow otherwise. In
 *   spatial environments, this flag is ignored.
 * @param content The content of the orbiter.
 *
 * Example:
 * ```
 * Orbiter(position = ContentEdge.Top, offset = 10.dp) {
 *   Text("This is a top edge Orbiter")
 * }
 * ```
 */
@Composable
@ComposableOpenTarget(index = -1)
@Deprecated(message = "Use an orbiter that takes an anchorPoint or a poseProvider.")
public fun Orbiter(
    position: ContentEdge.Horizontal,
    offset: Dp = 0.dp,
    offsetType: OrbiterOffsetType = OrbiterOffsetType.OuterEdge,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    shape: SpatialShape = OrbiterDefaults.Shape,
    elevation: Dp = OrbiterDefaults.Elevation,
    shouldRenderInNonSpatial: Boolean = true,
    content: @Composable @UiComposable () -> Unit,
) {
    if (
        !LocalSpatialCapabilities.current.isSpatialUiEnabled &&
            currentComposer.applier !is SubspaceNodeApplier
    ) {
        if (shouldRenderInNonSpatial) {
            content()
        }
        return
    }

    val density = LocalDensity.current

    Orbiter(
        poseProvider = { targetSize, layoutDirection, orbiterContentSize ->
            val anchorPoint =
                when (position) {
                    ContentEdge.Top ->
                        when (alignment) {
                            Alignment.Start -> OrbiterAnchorPoint.TopStart
                            Alignment.CenterHorizontally -> OrbiterAnchorPoint.Top
                            Alignment.End -> OrbiterAnchorPoint.TopEnd
                            AbsoluteAlignment.Left -> OrbiterAnchorPoint.Absolute.TopLeft
                            AbsoluteAlignment.Right -> OrbiterAnchorPoint.Absolute.TopRight
                            else -> throw IllegalArgumentException("Invalid alignment: $alignment")
                        }
                    ContentEdge.Bottom ->
                        when (alignment) {
                            Alignment.Start -> OrbiterAnchorPoint.BottomStart
                            Alignment.CenterHorizontally -> OrbiterAnchorPoint.Bottom
                            Alignment.End -> OrbiterAnchorPoint.BottomEnd
                            AbsoluteAlignment.Left -> OrbiterAnchorPoint.Absolute.BottomLeft
                            AbsoluteAlignment.Right -> OrbiterAnchorPoint.Absolute.BottomRight
                            else -> throw IllegalArgumentException("Invalid alignment: $alignment")
                        }
                    else -> throw IllegalArgumentException("Invalid position: $position")
                }
            val anchorVector =
                anchorPoint.calculateAnchorVector(
                    anchorHalfSize = targetSize.toMeterSize(density) / 2f,
                    layoutDirection = layoutDirection,
                    orbiterHalfSize = orbiterContentSize.toMeterSize(density) / 2f,
                )
            val verticalMultiplier = if (position == ContentEdge.Horizontal.Top) 1f else -1f
            val yOffset =
                when (offsetType) {
                    OrbiterOffsetType.Overlap -> -offset.toMeter().toM()
                    OrbiterOffsetType.InnerEdge -> offset.toMeter().toM()
                    OrbiterOffsetType.OuterEdge ->
                        offset.toMeter().toM() - orbiterContentSize.toMeterSize(density).height
                    else -> throw IllegalArgumentException("Invalid offsetType: $offsetType")
                } * verticalMultiplier
            val offsetVector = Vector3(x = 0f, y = yOffset, z = elevation.toMeter().toM())

            Pose(translation = anchorVector + offsetVector, rotation = Quaternion.Identity)
        },
        shape = shape,
        content = content,
    )
}

/**
 * A composable that creates an orbiter along the start or end edges of a view.
 *
 * Orbiters are floating elements that are typically used to control the content within spatial
 * panels and other entities that they're anchored to. They allow the content to have more space and
 * give users quick access to features like navigation without obstructing the main content.
 *
 * The size of the [Orbiter] is constrained by the dimensions of the parent spatial component it is
 * anchored to (e.g., a [androidx.xr.compose.subspace.SpatialPanel]). If it's not placed within a
 * specific spatial component, it defaults to the main window's size. Consequently, an [Orbiter]'s
 * content cannot be larger than its parent's dimensions.
 *
 * @param position The edge of the orbiter. Use [ContentEdge.Start] or [ContentEdge.End].
 * @param offset The offset of the orbiter based on the outer edge of the orbiter.
 * @param offsetType The type of offset used for positioning the orbiter.
 * @param alignment The alignment of the orbiter. Use [Alignment.CenterVertically] or
 *   [Alignment.Top] or [Alignment.Bottom].
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param elevation The z-direction elevation level of this Orbiter.
 * @param shouldRenderInNonSpatial In a non-spatial environment, if `true` the orbiter content is
 *   rendered as if the orbiter wrapper was not present and removed from the flow otherwise. In
 *   spatial environments, this flag is ignored.
 * @param content The content of the orbiter.
 *
 * Example:
 * ```
 * Orbiter(position = ContentEdge.Start, offset = 10.dp) {
 *   Text("This is a start edge Orbiter")
 * }
 * ```
 */
@Composable
@ComposableOpenTarget(index = -1)
@Deprecated(message = "Use an orbiter that takes an anchorPoint or a poseProvider.")
public fun Orbiter(
    position: ContentEdge.Vertical,
    offset: Dp = 0.dp,
    offsetType: OrbiterOffsetType = OrbiterOffsetType.OuterEdge,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    shape: SpatialShape = OrbiterDefaults.Shape,
    elevation: Dp = OrbiterDefaults.Elevation,
    shouldRenderInNonSpatial: Boolean = true,
    content: @Composable @UiComposable () -> Unit,
) {
    if (
        !LocalSpatialCapabilities.current.isSpatialUiEnabled &&
            currentComposer.applier !is SubspaceNodeApplier
    ) {
        if (shouldRenderInNonSpatial) {
            content()
        }
        return
    }

    val density = LocalDensity.current

    Orbiter(
        poseProvider = { targetSize, layoutDirection, orbiterContentSize ->
            val anchorPoint =
                when (position) {
                    ContentEdge.Start ->
                        when (alignment) {
                            Alignment.Top -> OrbiterAnchorPoint.StartTop
                            Alignment.CenterVertically -> OrbiterAnchorPoint.Start
                            Alignment.Bottom -> OrbiterAnchorPoint.StartBottom
                            else -> throw IllegalArgumentException("Invalid alignment: $alignment")
                        }
                    ContentEdge.End ->
                        when (alignment) {
                            Alignment.Top -> OrbiterAnchorPoint.EndTop
                            Alignment.CenterVertically -> OrbiterAnchorPoint.End
                            Alignment.Bottom -> OrbiterAnchorPoint.EndBottom
                            else -> throw IllegalArgumentException("Invalid alignment: $alignment")
                        }
                    else -> throw IllegalArgumentException("Invalid position: $position")
                }
            val anchorVector =
                anchorPoint.calculateAnchorVector(
                    anchorHalfSize = targetSize.toMeterSize(density) / 2f,
                    layoutDirection = layoutDirection,
                    orbiterHalfSize = orbiterContentSize.toMeterSize(density) / 2f,
                )
            val sideMultiplier = if (position == ContentEdge.Vertical.End) 1f else -1f
            val xOffset =
                when (offsetType) {
                    OrbiterOffsetType.Overlap -> -offset.toMeter().toM()
                    OrbiterOffsetType.InnerEdge -> offset.toMeter().toM()
                    OrbiterOffsetType.OuterEdge ->
                        offset.toMeter().toM() - orbiterContentSize.toMeterSize(density).width
                    else -> throw IllegalArgumentException("Invalid offsetType: $offsetType")
                } * sideMultiplier
            val offsetVector =
                Vector3(
                    x = layoutDirection.multiplier * xOffset,
                    y = 0f,
                    z = elevation.toMeter().toM(),
                )

            Pose(translation = anchorVector + offsetVector, rotation = Quaternion.Identity)
        },
        shape = shape,
        content = content,
    )
}

/**
 * A composable that creates an orbiter along the edge of a spatial component (e.g.
 * [androidx.xr.compose.subspace.SpatialPanel]).
 *
 * Orbiters are floating elements that are typically used to control the content within spatial
 * panels and other entities that they're anchored to. They allow the content to have more space and
 * give users quick access to features like navigation without obstructing the main content.
 *
 * The size of the `Orbiter` is constrained by the dimensions of its spatial parent. The spatial
 * parent of the Orbiter is determined based on where the Orbiter is declared. When the orbiter is
 * declared within:
 * * A [Subspace], the nearest spatial component (e.g. [androidx.xr.compose.subspace.SpatialRow],
 *   [androidx.xr.compose.subspace.SpatialPanel]) is the spatial parent
 * * `setContent`, the main panel is the spatial parent
 *
 * @param anchorPoint The anchored position of the orbiter relative to the spatial component it is
 *   anchored to. [OrbiterAnchorPoint] is [LayoutDirection] aware.
 * @param offset The offset of the orbiter based on the outer edge of the orbiter.
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param content The content of the orbiter.
 */
@Composable
@ComposableOpenTarget(index = -1)
public fun Orbiter(
    anchorPoint: OrbiterAnchorPoint,
    offset: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, OrbiterDefaults.Elevation),
    shape: SpatialShape = OrbiterDefaults.Shape,
    content: @Composable @UiComposable () -> Unit,
) {
    val density = LocalDensity.current

    Orbiter(
        poseProvider = { targetSize, layoutDirection, orbiterContentSize ->
            val anchorVector =
                anchorPoint.calculateAnchorVector(
                    anchorHalfSize = targetSize.toMeterSize(density) / 2f,
                    layoutDirection = layoutDirection,
                    orbiterHalfSize = orbiterContentSize.toMeterSize(density) / 2f,
                )
            val offsetVector = offset.toMeterVector()

            Pose(translation = anchorVector + offsetVector, rotation = Quaternion.Identity)
        },
        shape = shape,
        content = content,
    )
}

/**
 * A composable that creates an orbiter along the edge of a spatial component (e.g.
 * [androidx.xr.compose.subspace.SpatialPanel]).
 *
 * Orbiters are floating elements that are typically used to control the content within spatial
 * panels and other entities that they're anchored to. They allow the content to have more space and
 * give users quick access to features like navigation without obstructing the main content.
 *
 * The size of the `Orbiter` is constrained by the dimensions of its spatial parent. The spatial
 * parent of the Orbiter is determined based on where the Orbiter is declared. When the orbiter is
 * declared within:
 * * A [Subspace], the nearest spatial component (e.g. [androidx.xr.compose.subspace.SpatialRow],
 *   [androidx.xr.compose.subspace.SpatialPanel]) is the spatial parent
 * * `setContent`, the main panel is the spatial parent
 *
 * Orbiters do not participate in their parent's layout and have no layout nodes in the containing
 * compose hierarchy.
 *
 * @param poseProvider A pose provider for calculating the offset pose of the orbiter relative to
 *   its spatial parent.
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param content The content of the orbiter.
 */
@Composable
@ComposableOpenTarget(index = -1)
public fun Orbiter(
    poseProvider: OrbiterPoseProvider,
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

    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val parentView = LocalView.current
    @Suppress("DEPRECATION") val localId = currentCompositeKeyHash
    val context = LocalContext.current
    val compositionContext = rememberCompositionContext()
    val parentEntity: CoreEntity? = findNearestParentEntity()

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
            )
        }

    SideEffect {
        holder.parentEntity = parentEntity
        holder.poseProvider = poseProvider
        holder.shape = shape
        holder.content = movableContent
    }
}

/** Calculates the [Pose] of an [Orbiter] in 3D space relative to its spatial parent. */
public fun interface OrbiterPoseProvider {
    /**
     * Calculate the [Pose] of the [Orbiter].
     *
     * @param anchorSize The size of the [Orbiter]'s anchor target in pixels.
     * @param layoutDirection The layout direction of the [Orbiter].
     * @param orbiterContentSize The size of the [Orbiter]'s content in pixels.
     * @return The [Pose] of the [Orbiter] in meters.
     */
    public fun calculatePose(
        anchorSize: IntSize,
        layoutDirection: LayoutDirection,
        orbiterContentSize: IntSize,
    ): Pose
}

/**
 * Represents an anchor point for an [Orbiter] relative to its target.
 *
 * Each anchor point defines a specific location on the boundary of the target where the orbiter can
 * be attached. The anchor points are sensitive to [LayoutDirection], ensuring that concepts like
 * "start" and "end" are correctly interpreted in both LTR and RTL contexts.
 *
 * For layout direction-agnostic positioning, use the [Absolute] variants.
 */
public sealed class OrbiterAnchorPoint private constructor() {

    internal abstract fun calculateAnchorVector(
        anchorHalfSize: FloatSize2d,
        layoutDirection: LayoutDirection,
        orbiterHalfSize: FloatSize2d,
    ): Vector3

    /**
     * Attach the [Orbiter] at the start of the top edge of the content. Start is Left if the layout
     * direction is LTR and Right if the layout direction is RTL.
     */
    public object TopStart : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = -layoutDirection.multiplier * (anchorHalfSize.width - orbiterHalfSize.width),
                y = anchorHalfSize.height + orbiterHalfSize.height,
                z = 0f,
            )
    }

    /** Attach the [Orbiter] at the center of the top edge of the content. */
    public object Top : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 = Vector3(x = 0f, y = anchorHalfSize.height + orbiterHalfSize.height, z = 0f)
    }

    /**
     * Attach the [Orbiter] at the end of the top edge of the content. End is Right if the layout
     * direction is LTR and Left if the layout direction is RTL.
     */
    public object TopEnd : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = layoutDirection.multiplier * (anchorHalfSize.width - orbiterHalfSize.width),
                y = anchorHalfSize.height + orbiterHalfSize.height,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the top of the end edge of the content. End is Right if the layout
     * direction is LTR and Left if the layout direction is RTL.
     */
    public object EndTop : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = layoutDirection.multiplier * (anchorHalfSize.width + orbiterHalfSize.width),
                y = anchorHalfSize.height - orbiterHalfSize.height,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the center of the end edge of the content. End is Right if the layout
     * direction is LTR and Left if the layout direction is RTL.
     */
    public object End : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = layoutDirection.multiplier * (anchorHalfSize.width + orbiterHalfSize.width),
                y = 0f,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the bottom of the end edge of the content. End is Right if the layout
     * direction is LTR and Left if the layout direction is RTL.
     */
    public object EndBottom : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = layoutDirection.multiplier * (anchorHalfSize.width + orbiterHalfSize.width),
                y = -anchorHalfSize.height + orbiterHalfSize.height,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the start of the bottom edge of the content. Start is Left if the
     * layout direction is LTR and Right if the layout direction is RTL.
     */
    public object BottomStart : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = -layoutDirection.multiplier * (anchorHalfSize.width - orbiterHalfSize.width),
                y = -anchorHalfSize.height - orbiterHalfSize.height,
                z = 0f,
            )
    }

    /** Attach the [Orbiter] at the center of the bottom edge of the content. */
    public object Bottom : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 = Vector3(x = 0f, y = -anchorHalfSize.height - orbiterHalfSize.height, z = 0f)
    }

    /**
     * Attach the [Orbiter] at the end of the bottom edge of the content. End is Right if the layout
     * direction is LTR and Left if the layout direction is RTL.
     */
    public object BottomEnd : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = layoutDirection.multiplier * (anchorHalfSize.width - orbiterHalfSize.width),
                y = -anchorHalfSize.height - orbiterHalfSize.height,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the top of the start edge of the content. Start is Left if the layout
     * direction is LTR and Right if the layout direction is RTL.
     */
    public object StartTop : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = -layoutDirection.multiplier * (anchorHalfSize.width + orbiterHalfSize.width),
                y = anchorHalfSize.height - orbiterHalfSize.height,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the center of the start edge of the content. Start is Left if the
     * layout direction is LTR and Right if the layout direction is RTL.
     */
    public object Start : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = -layoutDirection.multiplier * (anchorHalfSize.width + orbiterHalfSize.width),
                y = 0f,
                z = 0f,
            )
    }

    /**
     * Attach the [Orbiter] at the bottom of the start edge of the content. Start is Left if the
     * layout direction is LTR and Right if the layout direction is RTL.
     */
    public object StartBottom : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 =
            Vector3(
                x = -layoutDirection.multiplier * (anchorHalfSize.width + orbiterHalfSize.width),
                y = -anchorHalfSize.height + orbiterHalfSize.height,
                z = 0f,
            )
    }

    /**
     * This is not exposed as part of the public API and is intended to prevent exhaustive when
     * statements.
     */
    private object Unused : OrbiterAnchorPoint() {
        override fun calculateAnchorVector(
            anchorHalfSize: FloatSize2d,
            layoutDirection: LayoutDirection,
            orbiterHalfSize: FloatSize2d,
        ): Vector3 = Vector3.Zero
    }

    /** Provides anchor points that are not affected by [LayoutDirection]. */
    public sealed class Absolute : OrbiterAnchorPoint() {
        /** Attach the [Orbiter] at the left of the top edge of the content. */
        public object TopLeft : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = -anchorHalfSize.width + orbiterHalfSize.width,
                    y = anchorHalfSize.height + orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the right of the top edge of the content. */
        public object TopRight : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = anchorHalfSize.width - orbiterHalfSize.width,
                    y = anchorHalfSize.height + orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the top of the right edge of the content. */
        public object RightTop : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = anchorHalfSize.width + orbiterHalfSize.width,
                    y = anchorHalfSize.height - orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the center of the right edge of the content. */
        public object Right : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 = Vector3(x = anchorHalfSize.width + orbiterHalfSize.width, y = 0f, z = 0f)
        }

        /** Attach the [Orbiter] at the bottom of the right edge of the content. */
        public object RightBottom : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = anchorHalfSize.width + orbiterHalfSize.width,
                    y = -anchorHalfSize.height + orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the left of the bottom edge of the content. */
        public object BottomLeft : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = -anchorHalfSize.width + orbiterHalfSize.width,
                    y = -anchorHalfSize.height - orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the right of the bottom edge of the content. */
        public object BottomRight : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = anchorHalfSize.width - orbiterHalfSize.width,
                    y = -anchorHalfSize.height - orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the top of the left edge of the content. */
        public object LeftTop : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = -anchorHalfSize.width - orbiterHalfSize.width,
                    y = anchorHalfSize.height - orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /** Attach the [Orbiter] at the center of the left edge of the content. */
        public object Left : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 = Vector3(x = -anchorHalfSize.width - orbiterHalfSize.width, y = 0f, z = 0f)
        }

        /** Attach the [Orbiter] at the bottom of the left edge of the content. */
        public object LeftBottom : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 =
                Vector3(
                    x = -anchorHalfSize.width - orbiterHalfSize.width,
                    y = -anchorHalfSize.height + orbiterHalfSize.height,
                    z = 0f,
                )
        }

        /**
         * This is not exposed as part of the public API and is intended to prevent exhaustive when
         * statements.
         */
        private object Unused : Absolute() {
            override fun calculateAnchorVector(
                anchorHalfSize: FloatSize2d,
                layoutDirection: LayoutDirection,
                orbiterHalfSize: FloatSize2d,
            ): Vector3 = Vector3.Zero
        }
    }
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
                    detectTapGestures { dialogManager.isSpatialDialogActive.value = false }
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
        val listener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val newSize =
                    getWindowBoundsInPixels(context).run { IntVolumeSize(width, height, 0) }
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
    private var localId: Int,
    initialPoseProvider: OrbiterPoseProvider,
    initialShape: SpatialShape,
) : RememberObserver {
    private var view: ComposeView? = null
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
                    PanelEntity.create(
                        session = session,
                        parent = null,
                        view = view,
                        pixelDimensions = IntSize2d(0, 0),
                        name = "Orbiter:${view.id}",
                    )
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
            val constraints = Constraints(maxWidth = panelSize.width, maxHeight = panelSize.height)
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
                            anchorSize = IntSize(constraints.maxWidth, constraints.maxHeight),
                            layoutDirection =
                                parentEntity?.layout?.layoutDirection ?: LayoutDirection.Ltr,
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

/** An enum that represents the edges of a view where an orbiter can be placed. */
public sealed interface ContentEdge {
    public class Horizontal private constructor(private val displayName: String) : ContentEdge {
        public companion object {
            /** Positioning constant to place an orbiter above the content's top edge. */
            public val Top: Horizontal = Horizontal("Top")

            /** Positioning constant to place an orbiter below the content's bottom edge. */
            public val Bottom: Horizontal = Horizontal("Bottom")
        }

        /** Returns the string representation of the edge. */
        override fun toString(): String {
            return displayName
        }
    }

    /** Represents vertical edges (start or end). */
    public class Vertical private constructor(private val displayName: String) : ContentEdge {
        public companion object {
            /**
             * Positioning constant to place an orbiter at the start of the content's starting edge.
             */
            public val Start: Vertical = Vertical("Start")

            /** Positioning constant to place an orbiter at the end of the content's ending edge. */
            public val End: Vertical = Vertical("End")
        }

        /** Returns the string representation of the edge. */
        override fun toString(): String {
            return displayName
        }
    }

    public companion object {
        /** The top edge. */
        public val Top: Horizontal = Horizontal.Top

        /** The bottom edge. */
        public val Bottom: Horizontal = Horizontal.Bottom

        /** The start edge. */
        public val Start: Vertical = Vertical.Start

        /** The end edge. */
        public val End: Vertical = Vertical.End
    }
}

/** Represents the type of offset used for positioning an orbiter. */
@JvmInline
public value class OrbiterOffsetType private constructor(private val value: Int) {
    public companion object {
        /** The edge of the orbiter that is facing away from the content element. */
        public val OuterEdge: OrbiterOffsetType = OrbiterOffsetType(0)

        /** The edge of the orbiter that is directly facing the content element. */
        public val InnerEdge: OrbiterOffsetType = OrbiterOffsetType(1)

        public val Overlap: OrbiterOffsetType = OrbiterOffsetType(2)
    }
}

private val LayoutDirection.multiplier: Float
    get() = if (this == LayoutDirection.Ltr) 1f else -1f

private fun IntSize.toMeterSize(density: Density): FloatSize2d =
    FloatSize2d(
        width = Meter.fromPixel(width.toFloat(), density).toM(),
        height = Meter.fromPixel(height.toFloat(), density).toM(),
    )

private fun DpVolumeOffset.toMeterVector(): Vector3 =
    Vector3(x = x.toMeter().toM(), y = y.toMeter().toM(), z = z.toMeter().toM())
