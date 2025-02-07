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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialComposeScene
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getActivity
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialBoxScope
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreContentlessEntity
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ContentlessEntity

private val LocalIsInApplicationSubspace: ProvidableCompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        LocalCoreEntity.currentValue != null
    }

/**
 * Create a 3D area that the app can render spatial content into.
 *
 * If this is the topmost [Subspace] in the compose hierarchy then this will expand to fill all of
 * the available space and will not be bound by its containing window.
 *
 * If this is nested within another [Subspace] then it will lay out its content in the X and Y
 * directions according to the layout logic of its parent in 2D space. It will be constrained in the
 * Z direction according to the constraints imposed by its containing [Subspace].
 *
 * This is a no-op and does not render anything in non-XR environments (i.e. Phone and Tablet).
 *
 * @param content The 3D content to render within this Subspace.
 */
@Composable
public fun Subspace(content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit) {
    val activity = LocalContext.current.getActivity()

    // TODO(b/369446163) Test the case where a NestedSubspace could be created outside of a Panel.
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled && activity is ComponentActivity) {
        if (LocalIsInApplicationSubspace.current) {
            NestedSubspace(activity, content)
        } else {
            ApplicationSubspace(activity, content)
        }
    }
}

/**
 * Create a Subspace that is rooted in the application space.
 *
 * This is used as the top-level [Subspace] within the context of the default task window. Nested
 * Subspaces should use their nearest Panel that contains the [Subspace] to determine the sizing
 * constraints and position of the [Subspace].
 */
@Composable
private fun ApplicationSubspace(
    activity: ComponentActivity,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val compositionContext = rememberCompositionContext()
    val scene by remember {
        disposableValueOf(
            SpatialComposeScene(
                ownerActivity = activity,
                jxrSession = session,
                parentCompositionContext = compositionContext,
            )
        ) {
            it.dispose()
        }
    }

    DisposableEffect(session) {
        session.mainPanelEntity.setHidden(true)
        onDispose { session.mainPanelEntity.setHidden(false) }
    }

    SideEffect {
        scene.setContent {
            CompositionLocalProvider(LocalIsInApplicationSubspace provides true) {
                SpatialBox(content = content)
            }
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
    // subspace
    // properly.
    val subspaceRootContainer by remember {
        disposableValueOf(
            ContentlessEntity.create(session, "SubspaceRootContainer").apply {
                setParent(coreEntity.entity)
            }
        ) {
            it.dispose()
        }
    }
    val scene by remember {
        val subspaceRoot =
            ContentlessEntity.create(session, "SubspaceRoot").apply {
                setParent(subspaceRootContainer)
            }
        disposableValueOf(
            SpatialComposeScene(
                ownerActivity = activity,
                jxrSession = session,
                parentCompositionContext = compositionContext,
                rootEntity = CoreContentlessEntity(subspaceRoot),
            )
        ) {
            it.dispose()
            subspaceRoot.dispose()
        }
    }
    var measuredSize by remember { mutableStateOf(IntVolumeSize.Zero) }
    var contentOffset by remember { mutableStateOf(Offset.Zero) }
    val pose =
        rememberCalculatePose(
            contentOffset,
            LocalView.current.size,
            measuredSize.run { IntSize(width, height) },
        )

    LaunchedEffect(pose) { subspaceRootContainer.setPose(pose) }

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
                }
            }
        }

        layout(measuredSize.width, measuredSize.height) {}
    }
}
