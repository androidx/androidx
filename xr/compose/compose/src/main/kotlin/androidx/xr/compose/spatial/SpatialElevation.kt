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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastMap
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.platform.LocalCoreMainPanelEntity
import androidx.xr.compose.platform.LocalOpaqueEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.rememberComposeView
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.PanelEntity

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
    val parentEntity = LocalCoreEntity.current ?: LocalCoreMainPanelEntity.current ?: return
    val view = rememberComposeView()
    val panelEntity = remember {
        CorePanelEntity(
                PanelEntity.create(
                    session = session,
                    view = view,
                    pixelDimensions = IntSize2d(0, 0),
                    name = "SpatialElevation:${view.id}",
                )
            )
            .apply { enabled = false }
    }
    val parentView = LocalView.current
    var parentViewSize by remember { mutableStateOf(parentView.size) }
    val movableContent = remember {
        movableContentOf {
            CompositionLocalProvider(LocalOpaqueEntity provides panelEntity, content = content)
        }
    }

    DisposableEffect(panelEntity) { onDispose { panelEntity.dispose() } }
    DisposableEffect(parentView) {
        val listener =
            View.OnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
                parentViewSize = IntSize(right, bottom)
            }
        parentView.addOnLayoutChangeListener(listener)
        onDispose { parentView.removeOnLayoutChangeListener(listener) }
    }

    Layout(content = movableContent) { measurables, constraints ->
        val placeables = measurables.fastMap { it.measure(constraints) }
        val contentSize =
            placeables.fastFold(IntSize.Zero) { acc, placeable ->
                IntSize(
                    acc.width.coerceAtLeast(placeable.width),
                    acc.height.coerceAtLeast(placeable.height),
                )
            }

        layout(contentSize.width, contentSize.height) {
            coordinates?.positionInRoot()?.let {
                panelEntity.poseInMeters =
                    calculatePose(it, parentViewSize, contentSize, this@Layout, elevation)
            }
            panelEntity.parent = parentEntity
            panelEntity.size =
                IntVolumeSize(width = contentSize.width, height = contentSize.height, depth = 0)

            // Instead of placing the content here, set it as the panel's content
            view.setContent(movableContent)

            panelEntity.enabled = true
        }
    }
}
