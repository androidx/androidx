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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.platform.LocalCoreMainPanelEntity
import androidx.xr.compose.platform.LocalOpaqueEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.rememberComposeView
import androidx.xr.compose.subspace.rememberCorePanelEntity
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity

internal object ElevatedPanelDefaults {
    /** Default shape for a Spatial Panel. */
    internal val shape: SpatialShape = SpatialRoundedCornerShape(ZeroCornerSize)
}

/**
 * This is the base panel underlying the implementations of SpatialElevation, SpatialPopup, and
 * SpatialDialog. It allows creating a panel at a specific size and offset.
 */
@Composable
internal fun ElevatedPanel(
    elevation: Dp,
    contentSize: IntSize,
    shape: SpatialShape = ElevatedPanelDefaults.shape,
    contentOffset: Offset? = null,
    elevationTransitionSpec: @Composable Transition.Segment<Dp>.() -> FiniteAnimationSpec<Dp> = {
        spring()
    },
    content: @Composable () -> Unit,
) {
    val parentView = LocalView.current
    val zDepth by
        updateTransition(targetState = elevation, label = "restingLevelTransition").animateDp(
            transitionSpec = elevationTransitionSpec,
            label = "zDepth",
        ) { state ->
            state
        }
    var parentViewSize by remember { mutableStateOf(parentView.size) }
    DisposableEffect(parentView) {
        val listener =
            View.OnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
                parentViewSize = IntSize(right, bottom)
            }
        parentView.addOnLayoutChangeListener(listener)
        onDispose { parentView.removeOnLayoutChangeListener(listener) }
    }

    ElevatedPanel(
        contentSize = contentSize,
        shape = shape,
        pose =
            contentOffset?.let { rememberCalculatePose(it, parentViewSize, contentSize, zDepth) },
        content = content,
    )
}

/**
 * This is the base panel underlying the implementations of SpatialElevation, SpatialPopup, and
 * SpatialDialog. It allows creating a panel at a specific size and [Pose].
 */
@Composable
internal fun ElevatedPanel(
    contentSize: IntSize,
    shape: SpatialShape = ElevatedPanelDefaults.shape,
    pose: Pose? = null,
    content: @Composable () -> Unit,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val parentEntity = LocalCoreEntity.current ?: LocalCoreMainPanelEntity.current ?: return
    val view = rememberComposeView()
    val panelEntity =
        rememberCorePanelEntity(shape = shape) {
            PanelEntity.create(
                session = session,
                view = view,
                pixelDimensions = contentSize.run { IntSize2d(width, height) },
                name = "ElevatedPanel:${view.id}",
            )
        }
    // TODO(b/416093964): Does ElevatedPanel's alpha still need to be dependent on Pose?
    view.setContent {
        CompositionLocalProvider(LocalOpaqueEntity provides panelEntity) {
            Box(Modifier.alpha(if (pose == null) 0.0f else 1.0f)) { content() }
        }
    }

    LaunchedEffect(pose) {
        if (pose != null) {
            panelEntity.entity.setPose(pose)
        }
    }

    LaunchedEffect(contentSize) {
        val width = contentSize.width
        val height = contentSize.height

        panelEntity.size = IntVolumeSize(width = width, height = height, depth = 0)
    }

    LaunchedEffect(parentEntity) { panelEntity.entity.parent = parentEntity.entity }
}

/** A 3D vector where each coordinate is [Meter]s. */
internal data class MeterPosition(
    val x: Meter = 0.meters,
    val y: Meter = 0.meters,
    val z: Meter = 0.meters,
) {
    /**
     * Adds this [MeterPosition] to the [other] one.
     *
     * @param other the other [MeterPosition] to add.
     * @return a new [MeterPosition] representing the sum of the two positions.
     */
    public operator fun plus(other: MeterPosition) =
        MeterPosition(x = x + other.x, y = y + other.y, z = z + other.z)

    fun toVector3() = Vector3(x = x.toM(), y = y.toM(), z = z.toM())
}

internal val View.size
    get() = IntSize(width, height)
