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

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.xr.compose.platform.LocalComposeXrOwners
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.platform.SpatialComposeScene
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getActivity
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialBoxScope
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.node.SubspaceNodeApplier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.android.awaitFrame

private val LocalIsInApplicationSubspace: ProvidableCompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        LocalCoreEntity.currentValue != null
    }

internal val LocalSubspaceRootNode: ProvidableCompositionLocal<Entity?> =
    compositionLocalWithComputedDefaultOf {
        LocalComposeXrOwners.currentValue?.subspaceRootNode
    }

/**
 * Create a 3D area that the app can render spatial content into.
 *
 * If this is the topmost [Subspace] in the compose hierarchy, its size will be determined by the
 * system's recommended content box. This provides a device-specific volume that represents a
 * comfortable, human-scale viewing area, making it the recommended way to create responsive spatial
 * layouts. See [ApplicationSubspace] for more detailed information and customization options for
 * this top-level behavior.
 *
 * If this is nested within another [Subspace] then it will lay out its content in the X and Y
 * directions according to the layout logic of its parent in 2D space. It will be constrained in the
 * Z direction according to the constraints imposed by its containing [Subspace].
 *
 * This is a no-op and does not render anything in non-XR environments (i.e. Phone and Tablet).
 *
 * On XR devices that cannot currently render spatial UI, the [Subspace] will still create its scene
 * and all of its internal state, even though nothing may be rendered. This is to ensure that the
 * state is maintained consistently in the spatial scene and to allow preparation for the support of
 * rendering spatial UI. State should be maintained by the compose runtime and events that cause the
 * compose runtime to lose state (app process killed or configuration change) will also cause the
 * Subspace to lose its state.
 *
 * @param content The 3D content to render within this Subspace.
 */
@Composable
@ComposableOpenTarget(index = -1)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
public fun Subspace(content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit) {
    val activity = LocalContext.current.getActivity() as? ComponentActivity ?: return

    // If not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    if (currentComposer.applier is SubspaceNodeApplier) {
        // We are already in a Subspace, so we can just render the content directly
        SpatialBox(content = content)
    } else if (LocalIsInApplicationSubspace.current) {
        NestedSubspace(activity, content)
    } else {
        ApplicationSubspace(activity = activity, constraints = null, content = content)
    }
}

/**
 * Create a 3D area that the app can render spatial content into with optional [VolumeConstraints].
 *
 * [ApplicationSubspace] should be used to create the topmost [Subspace] in your application's
 * spatial UI hierarchy. This composable will throw an [IllegalStateException] if it is used to
 * create a Subspace that is nested within another [Subspace] or [ApplicationSubspace]. For nested
 * 3D content areas, use the [Subspace] composable. The [ApplicationSubspace] will inherit its
 * position and scale from the system's recommended position and scale.
 *
 * By default, with no `constraints` provided, this Subspace is bounded by a recommended content
 * box. This box represents a comfortable, human-scale area in front of the user, sized to occupy a
 * significant portion of their view on any given device. Using this default is the suggested way to
 * create responsive spatial layouts that look great without hardcoding dimensions.
 *
 * This composable is a no-op and does not render anything in non-XR environments (i.e., Phone and
 * Tablet).
 *
 * On XR devices that cannot currently render spatial UI, the [ApplicationSubspace] will still
 * create its scene and all of its internal state, even though nothing may be rendered. This is to
 * ensure that the state is maintained consistently in the spatial scene and to allow preparation
 * for the support of rendering spatial UI. State should be maintained by the compose runtime and
 * events that cause the compose runtime to lose state (app process killed or configuration change)
 * will also cause the ApplicationSubspace to lose its state.
 *
 * @param constraints The volume constraints to apply to this [ApplicationSubspace]. If `null` (the
 *   default), the Subspace will be sized based on the system's recommended content box. This
 *   default provides a device-specific volume appropriate for comfortable viewing and interaction.
 * @param content The 3D content to render within this Subspace.
 */
@Composable
@ComposableOpenTarget(index = -1)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
public fun ApplicationSubspace(
    constraints: VolumeConstraints? = null,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    val activity = LocalContext.current.getActivity() as? ComponentActivity ?: return

    // If we are not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    if (currentComposer.applier is SubspaceNodeApplier) {
        // We are already in a Subspace, so we can just render the content directly
        SpatialBox(content = content)
    } else if (LocalIsInApplicationSubspace.current) {
        throw IllegalStateException("ApplicationSubspace cannot be nested within another Subspace.")
    } else {
        ApplicationSubspace(activity = activity, constraints = constraints, content = content)
    }
}

/**
 * Create a Subspace that is rooted in the application space.
 *
 * This is used as the top-level [Subspace] within the context of the default task window. Nested
 * Subspaces should use their nearest Panel that contains the [Subspace] to determine the sizing
 * constraints and position of the [Subspace].
 *
 * In the near future when HSM is spatialized, the Subspace should consider the app bounds when
 * determining its top-level constraints.
 *
 * TODO(b/419369273) Add test cases for activity to activity transitions and switching applications.
 */
@Composable
private fun ApplicationSubspace(
    activity: ComponentActivity,
    constraints: VolumeConstraints?,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val compositionContext = rememberCompositionContext()
    val subspaceRootNode = LocalSubspaceRootNode.current
    val scene by remember {
        session.scene.mainPanelEntity.setEnabled(false)
        val subspaceRoot = GroupEntity.create(session, "SubspaceRoot")
        subspaceRootNode?.let { subspaceRoot.parent = it }
        disposableValueOf(
            SpatialComposeScene(
                ownerActivity = activity,
                jxrSession = session,
                parentCompositionContext = compositionContext,
                rootEntity = CoreGroupEntity(subspaceRoot),
            )
        ) {
            it.dispose()
            subspaceRoot.dispose()
            session.scene.mainPanelEntity.setEnabled(true)
        }
    }

    val density = LocalDensity.current
    scene.rootVolumeConstraints =
        remember(constraints, density) {
            constraints
                ?: run {
                    val box: BoundingBox =
                        session.scene.activitySpace.recommendedContentBoxInFullSpace
                    val widthMeters = Meter(box.max.x - box.min.x)
                    val heightMeters = Meter(box.max.y - box.min.y)
                    val depthMeters = Meter(box.max.z - box.min.z)

                    VolumeConstraints(
                        minWidth = 0,
                        maxWidth = widthMeters.roundToPx(density),
                        minHeight = 0,
                        maxHeight = heightMeters.roundToPx(density),
                        minDepth = 0,
                        maxDepth = depthMeters.roundToPx(density),
                    )
                }
        }

    scene.setContent {
        CompositionLocalProvider(LocalIsInApplicationSubspace provides true) {
            SpatialBox(content = content)
        }
    }
}

@Composable
private fun NestedSubspace(
    activity: ComponentActivity,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val compositionContext = rememberCompositionContext()
    val coreEntity = checkNotNull(LocalCoreEntity.current) { "CoreEntity unavailable for subspace" }
    // The subspace root node will be owned and manipulated by the containing composition, we need a
    // container that we can manipulate at the Subspace level in order to position the entire
    // subspace properly.
    val subspaceRootContainer by remember {
        disposableValueOf(
            GroupEntity.create(session, "SubspaceRootContainer").apply {
                parent = coreEntity.entity
                setEnabled(false)
            }
        ) {
            it.dispose()
        }
    }
    val scene by remember {
        val subspaceRoot =
            GroupEntity.create(session, "SubspaceRoot").apply { parent = subspaceRootContainer }
        disposableValueOf(
            SpatialComposeScene(
                ownerActivity = activity,
                jxrSession = session,
                parentCompositionContext = compositionContext,
                rootEntity = CoreGroupEntity(subspaceRoot),
            )
        ) {
            it.dispose()
            subspaceRoot.dispose()
        }
    }
    var measuredSize by remember { mutableStateOf(IntVolumeSize.Zero) }
    var contentOffset by remember { mutableStateOf(Offset.Zero) }
    val viewSize = LocalView.current.size
    val density = LocalDensity.current

    LaunchedEffect(measuredSize, contentOffset, viewSize, density) {
        subspaceRootContainer.setPose(
            calculatePose(
                contentOffset,
                viewSize,
                measuredSize.run { IntSize(width, height) },
                density,
            )
        )
        // We need to wait for a single frame to ensure that the pose changes are batched to the
        // root container before we show it.
        if (!subspaceRootContainer.isEnabled(false) && awaitFrame() > 0) {
            subspaceRootContainer.setEnabled(true)
        }
    }

    Layout(modifier = Modifier.onGloballyPositioned { contentOffset = it.positionInRoot() }) {
        _,
        constraints ->
        scene.setContent {
            SubspaceLayout(content = { SpatialBox(content = content) }) { measurables, _ ->
                val placeables =
                    measurables.map {
                        it.measure(
                            VolumeConstraints(
                                minWidth = constraints.minWidth,
                                maxWidth = constraints.maxWidth,
                                minHeight = constraints.minHeight,
                                maxHeight = constraints.maxHeight,
                                // TODO(b/366564066) Nested Subspaces should get their depth
                                // constraints from
                                // the parent Subspace
                                minDepth = 0,
                                maxDepth = Int.MAX_VALUE,
                            )
                        )
                    }
                measuredSize =
                    IntVolumeSize(
                        width = placeables.maxOf { it.measuredWidth },
                        height = placeables.maxOf { it.measuredHeight },
                        depth = placeables.maxOf { it.measuredDepth },
                    )
                layout(measuredSize.width, measuredSize.height, measuredSize.depth) {
                    placeables.forEach { it.place(Pose.Identity) }
                    subspaceRootContainer.setPose(
                        calculatePose(
                            contentOffset,
                            viewSize,
                            measuredSize.run { IntSize(width, height) },
                            density,
                        )
                    )
                }
            }
        }

        layout(measuredSize.width, measuredSize.height) {}
    }
}
