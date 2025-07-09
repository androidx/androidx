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

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.node.SubspaceNodeApplier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d

/** Set the scrim alpha to 32% opacity across orbiters. */
private const val DEFAULT_SCRIM_ALPHA = 0x52000000

/** Contains default values used by Orbiters. */
public object OrbiterDefaults {

    /** Default shape for an Orbiter. */
    public val Shape: SpatialShape = SpatialRoundedCornerShape(ZeroCornerSize)

    /** Default elevation level for an Orbiter. */
    public val Elevation: Dp = SpatialElevationLevel.Level1
}

/**
 * A composable that creates an orbiter along the top or bottom edges of a view.
 *
 * Orbiters are floating elements that contain controls for spatial content. They allow the content
 * to have more space and give users quick access to features like navigation without obstructing
 * the main content.
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
 * Orbiter(position = OrbiterEdge.Top, offset = 10.dp) {
 *   Text("This is a top edge Orbiter")
 * }
 * ```
 */
@Composable
@ComposableOpenTarget(index = -1)
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
    Orbiter(
        OrbiterData(
            position = position,
            horizontalAlignment = alignment,
            offset = offset,
            offsetType = offsetType,
            shape = shape,
            elevation = elevation,
            shouldRenderInNonSpatial = shouldRenderInNonSpatial,
            content = content,
        )
    )
}

/**
 * A composable that creates an orbiter along the start or end edges of a view.
 *
 * Orbiters are floating elements that contain controls for spatial content. They allow the content
 * to have more space and give users quick access to features like navigation without obstructing
 * the main content.
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
 * Orbiter(position = OrbiterEdge.Start, offset = 10.dp) {
 *   Text("This is a start edge Orbiter")
 * }
 * ```
 */
@Composable
@ComposableOpenTarget(index = -1)
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
    Orbiter(
        OrbiterData(
            position = position,
            verticalAlignment = alignment,
            offset = offset,
            offsetType = offsetType,
            shape = shape,
            elevation = elevation,
            shouldRenderInNonSpatial = shouldRenderInNonSpatial,
            content = content,
        )
    )
}

@Composable
private fun Orbiter(data: OrbiterData) {
    // We use movableContentOf here to avoid recreating this content when the spatial capabilities
    // changes. This allows us to use the same orbiter content both in an orbiter when spatial
    // capabilities are granted and inline in a non-spatial environment in a way that retains the
    // orbiter content's internal state.
    val content = remember(data.content) { movableContentOf(data.content) }
    if (
        LocalSpatialCapabilities.current.isSpatialUiEnabled ||
            currentComposer.applier is SubspaceNodeApplier
    ) {
        PositionedOrbiter(data, content)
    } else if (data.shouldRenderInNonSpatial) {
        content()
    }
}

@Composable
internal fun PositionedOrbiter(data: OrbiterData, content: @Composable @UiComposable () -> Unit) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val density = LocalDensity.current
    val dialogManager = LocalDialogManager.current
    var contentSize: IntSize? by remember { mutableStateOf(null) }

    val parentEntity = LocalCoreEntity.current
    /**
     * Determine the reference panel size for Orbiter positioning.
     * 1. If parent entity is present, Orbiter is nested within a specific spatial component (e.g.,
     *    MainPanel, SpatialPanel) and uses its size.
     * 2. Otherwise, Orbiter is not explicitly parented within a Subspace()'s spatial entity. This
     *    occurs if Orbiter is used: a) Directly in `setContent { Orbiter(...) }` for a traditional
     *    2D Compose app. b) Inside a `Subspace { Orbiter(...) }` but not as a child of a CoreEntity
     *    provider. In these cases, Orbiter defaults to the main window's size, which are fetched
     *    and kept updated by getMainWindowSize().
     */
    val panelSize: IntVolumeSize = parentEntity?.mutableSize ?: getMainWindowSize(session)

    ElevatedPanel(
        contentSize = contentSize ?: IntSize.Zero,
        pose =
            contentSize?.let {
                rememberCalculatePose(
                    data.calculateOffset(panelSize.run { IntSize2d(width, height) }, it, density),
                    panelSize.run { IntSize(width, height) },
                    it,
                    data.elevation,
                )
            },
        shape = data.shape,
    ) {
        Box(
            modifier =
                Modifier.constrainTo(Constraints(0, panelSize.width, 0, panelSize.height))
                    .onSizeChanged { contentSize = it }
        ) {
            content()
        }
        if (dialogManager.isSpatialDialogActive.value) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(Color(DEFAULT_SCRIM_ALPHA)).pointerInput(
                        Unit
                    ) {
                        detectTapGestures { dialogManager.isSpatialDialogActive.value = false }
                    }
            ) {}
        }
    }
}

private fun getWindowBoundsInPixels(session: Session): IntSize2d =
    session.activity.window.decorView.run { IntSize2d(width, height) }

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
    var panelSize by
        remember(session) {
            val initialPixelDimensions = getWindowBoundsInPixels(session)
            mutableStateOf(
                IntVolumeSize(initialPixelDimensions.width, initialPixelDimensions.height, 0)
            )
        }

    val mainView = session.activity.window.decorView

    DisposableEffect(Unit) {
        val listener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val newSize =
                    getWindowBoundsInPixels(session).run { IntVolumeSize(width, height, 0) }
                if (panelSize != newSize) {
                    panelSize = newSize
                }
            }
        mainView.addOnLayoutChangeListener(listener)

        onDispose { mainView.removeOnLayoutChangeListener(listener) }
    }

    return panelSize
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

internal data class OrbiterData(
    val position: ContentEdge,
    val verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    val horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    val offset: Dp,
    val offsetType: OrbiterOffsetType,
    val content: @Composable () -> Unit,
    val shape: SpatialShape,
    val elevation: Dp = OrbiterDefaults.Elevation,
    val shouldRenderInNonSpatial: Boolean = true,
)

/**
 * Calculates the offset that should be applied to the orbiter given its settings, the panel size,
 * and the size of the orbiter content, using the specified density to convert Dp to pixels.
 */
private fun OrbiterData.calculateOffset(
    viewSize: IntSize2d,
    contentSize: IntSize,
    density: Density,
): Offset {

    if (position is ContentEdge.Vertical) {
        val y = verticalAlignment.align(contentSize.height, viewSize.height)

        val xOffset: Float =
            when (offsetType) {
                OrbiterOffsetType.OuterEdge -> -offset.toPx(density)
                OrbiterOffsetType.InnerEdge -> -contentSize.width - offset.toPx(density)
                OrbiterOffsetType.Overlap -> -contentSize.width + offset.toPx(density)
                else -> error("Unexpected OrbiterOffsetType: $offsetType")
            }

        val x: Float =
            when (position) {
                ContentEdge.Start -> xOffset
                ContentEdge.End -> viewSize.width - contentSize.width - xOffset
                else -> error("Unexpected OrbiterEdge: $position")
            }
        return Offset(x, y.toFloat())
    } else {
        // It should be fine to use LTR layout direction here since we can use placeRelative to
        // adjust
        val x = horizontalAlignment.align(contentSize.width, viewSize.width, LayoutDirection.Ltr)

        val yOffset: Float =
            when (offsetType) {
                OrbiterOffsetType.OuterEdge -> -offset.toPx(density)
                OrbiterOffsetType.InnerEdge -> -contentSize.height - offset.toPx(density)
                OrbiterOffsetType.Overlap -> -contentSize.height + offset.toPx(density)
                else -> error("Unexpected OrbiterOffsetType: $offsetType")
            }

        val y: Float =
            when (position) {
                ContentEdge.Top -> yOffset
                ContentEdge.Bottom -> viewSize.height - contentSize.height - yOffset
                else -> error("Unexpected OrbiterEdge: $position")
            }
        return Offset(x.toFloat(), y)
    }
}

private fun Dp.toPx(density: Density): Float = with(density) { toPx() }
