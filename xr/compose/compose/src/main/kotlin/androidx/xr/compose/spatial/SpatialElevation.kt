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

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.xr.compose.R
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.findNearestParentEntity
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.spatialComposeView
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene

/**
 * Composable that creates a panel in 3D space when spatialization is enabled.
 *
 * [SpatialElevation] elevates content in-place. It uses the source position and constraints to
 * determine the size and placement of the elevated panel while reserving space for the original
 * element within the layout.
 *
 * In non-spatial environments, the content is rendered normally without elevation.
 *
 * SpatialElevation does not support a [content] lambda that has a width or height of zero.
 *
 * @param elevation the desired elevation level for the panel in spatial environments.
 * @param content the composable content to be displayed within the elevated panel.
 */
@Composable
public fun SpatialElevation(
    elevation: Dp = SpatialElevationLevel.Level0,
    content: @Composable () -> Unit,
) {
    val movableContent = remember { movableContentOf(content) }
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        LayoutSpatialElevation(elevation, movableContent)
    } else {
        movableContent()
    }
}

@Composable
private fun LayoutSpatialElevation(elevation: Dp, content: @Composable () -> Unit) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val context = LocalContext.current
    val compositionContext = rememberCompositionContext()
    @Suppress("DEPRECATION") val localId = currentCompositeKeyHash

    /**
     * Determine the reference panel size for the SpatialElevation positioning.
     * 1. If parent entity is present, [SpatialElevation] is nested within a specific
     *    [androidx.xr.compose.subspace.SpatialPanel] and uses its size.
     * 2. Otherwise, [SpatialElevation] is not explicitly parented within a Subspace()'s spatial
     *    entity. This occurs if [SpatialElevation] is used directly in `setContent {
     *    SpatialElevation(...) }`.
     *
     * Unlike [Orbiter], [SpatialElevation] may only be used in a 2D context (i.e. in a
     * [androidx.xr.compose.subspace.SpatialPanel] or in `setContent`).
     */
    val parentView = LocalView.current
    val parentEntity = findNearestParentEntity()

    val holder =
        remember(parentView) {
            SpatialElevationRenderer(
                context = context,
                parentView = parentView,
                compositionContext = compositionContext,
                session = session,
                localId = localId,
            )
        }

    DisposableEffect(parentView) {
        val listener =
            View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                val newWidth = right - left
                val newHeight = bottom - top
                holder.parentViewSize = IntSize(newWidth, newHeight)
            }

        parentView.addOnLayoutChangeListener(listener)

        onDispose { parentView.removeOnLayoutChangeListener(listener) }
    }

    Layout { _, constraints ->
        holder.measureSynchronously(constraints)
        val size = holder.measuredContentSize

        layout(size.width, size.height) {
            holder.alpha = if (coordinates?.boundsInWindow()?.isEmpty == false) 1f else 0f
            coordinates?.positionInRoot()?.let {
                holder.poseInMeters =
                    calculatePose(it, holder.parentViewSize, size, this@Layout, elevation)
            }
        }
    }

    SideEffect {
        holder.parentEntity = parentEntity
        holder.content = content
    }
}

private val EmptyContent: @Composable () -> Unit = {}

/**
 * Manages the lifecycle and synchronization of a Compose-based 2D UI surface projected into 3D
 * space.
 *
 * This renderer acts as a bridge between the standard Compose layout system and the XR Runtime. It
 * hosts a [ComposeView] (rendering the [content]) which is attached to a [CorePanelEntity] in the
 * 3D scene.
 */
private class SpatialElevationRenderer(
    private var context: Context,
    private var parentView: View,
    private var compositionContext: CompositionContext,
    private var session: Session,
    private var localId: Int,
) : RememberObserver {
    var measuredContentSize: IntSize by mutableStateOf(IntSize.Zero)
        private set

    private var incomingConstraints by mutableStateOf(Constraints())
    var parentViewSize: IntSize = parentView.size

    var content: @Composable () -> Unit by mutableStateOf(EmptyContent)
    var parentEntity: CoreEntity? = null
        set(value) {
            if (field != value) {
                field = value
                panelEntity?.parent = value
            }
        }

    var alpha: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                panelEntity?.alpha = value
            }
        }

    var poseInMeters: Pose = Pose.Identity
        set(value) {
            if (field != value) {
                field = value
                panelEntity?.poseInMeters = value
            }
        }

    private var view: ComposeView? = null
    private var panelEntity: CorePanelEntity? = null

    override fun onRemembered() {
        val view = spatialComposeView(parentView, context, compositionContext, localId)
        this.view = view

        panelEntity =
            CorePanelEntity(
                    PanelEntity.create(
                        session = session,
                        view = view,
                        pixelDimensions = IntSize2d(0, 0),
                        name = "SpatialElevation:${view.id}",
                        parent = session.scene.activitySpace,
                    )
                )
                .apply {
                    enabled = false
                    view.setTag(R.id.compose_xr_local_view_entity, this)
                }

        view.setContent {
            Layout(content = content) { measurables, _ ->
                val placeables = measurables.fastMap { it.measure(incomingConstraints) }
                val contentSize =
                    placeables.fastFold(IntSize.Zero) { acc, placeable ->
                        IntSize(
                            acc.width.coerceAtLeast(placeable.width),
                            acc.height.coerceAtLeast(placeable.height),
                        )
                    }

                measuredContentSize = contentSize
                layout(contentSize.width, contentSize.height) {
                    panelEntity?.parent = parentEntity
                    panelEntity?.size =
                        IntVolumeSize(
                            width = contentSize.width,
                            height = contentSize.height,
                            depth = 0,
                        )
                    panelEntity?.enabled = true

                    placeables.fastForEach { it.place(0, 0) }
                }
            }
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

    fun measureSynchronously(constraints: Constraints) {
        incomingConstraints = constraints
        val view = this.view ?: return

        if (!view.isAttachedToWindow) {
            return
        }

        val widthSpec = toMeasureSpec(constraints.maxWidth, constraints.minWidth)
        val heightSpec = toMeasureSpec(constraints.maxHeight, constraints.minHeight)

        view.measure(widthSpec, heightSpec)

        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        view.layout(0, 0, width, height)
    }

    private fun toMeasureSpec(max: Int, min: Int): Int {
        return when {
            max == Constraints.Infinity ->
                View.MeasureSpec.makeMeasureSpec(min, View.MeasureSpec.UNSPECIFIED)

            min == max -> View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.EXACTLY)
            else -> View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST)
        }
    }
}
