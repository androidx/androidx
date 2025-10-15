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

import android.annotation.SuppressLint
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.core.viewtree.getParentOrViewTreeDisjointParent
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.xr.compose.R
import androidx.xr.compose.platform.LocalComposeXrOwners
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.platform.SpatialComposeScene
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.BodyPart
import androidx.xr.compose.subspace.LockDimensions
import androidx.xr.compose.subspace.LockingBehavior
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialBoxScope
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.recommendedSizeIfUnbounded
import androidx.xr.compose.subspace.node.SubspaceNodeApplier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.Config
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene

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
 * If this is the topmost Subspace in the compose hierarchy, its size will be determined by the
 * system's recommended content box. This provides a device-specific volume that represents a
 * comfortable, human-scale viewing area, making it the recommended way to create responsive spatial
 * layouts. See [ApplicationSubspace] for more detailed information and customization options for
 * this top-level behavior.
 *
 * If this is nested within another Subspace then it will lay out its content in the X and Y
 * directions according to the layout logic of its parent in 2D space. It will be constrained in the
 * Z direction according to the constraints imposed by its containing Subspace.
 *
 * This is a no-op and does not render anything in non-XR environments (i.e. Phone and Tablet).
 *
 * On XR devices that cannot currently render spatial UI, the Subspace will still create its scene
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
    // If not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    if (currentComposer.applier is SubspaceNodeApplier) {
        // We are already in a Subspace, so we can just render the content directly
        SpatialBox(content = content)
    } else if (LocalIsInApplicationSubspace.current) {
        PanelEmbeddedSubspace(content)
    } else {
        ApplicationSubspace(content = content)
    }
}

/**
 * Create a 3D area that the app can render spatial content into.
 *
 * ApplicationSubspace creates a Compose for XR's Spatial UI hierarchy (3D Scene Graph) in your
 * application's regular Compose UI tree. In this Subspace, You can use a @SubspaceComposable to
 * create 3D UI elements.
 *
 * Each call to ApplicationSubspace creates a new, independent Spatial UI hierarchy. It does **not**
 * inherit the spatial position, orientation, or scale of any parent ApplicationSubspace it is
 * nested within. Its position and scale are solely decided by the system's recommended position and
 * scale. To create an embedded Subspace within a SpatialPanel, Orbiter, SpatialPopup and etc, use
 * the [Subspace] instead.
 *
 * By default, this Subspace is automatically bounded by the system's recommended content box. This
 * box represents a comfortable, human-scale area in front of the user, sized to occupy a
 * significant portion of their view on any given device. Using this default is the suggested way to
 * create responsive spatial layouts that look great without hardcoding dimensions.
 * SubspaceModifiers like `SubspaceModifier.fillMaxSize` will expand to fill this recommended box.
 * This default can be overridden by applying a custom size-based modifier. For unbounded behavior,
 * set `allowUnboundedSubspace = true`.
 *
 * This composable is a no-op and does not render anything in non-XR environments (i.e., Phone and
 * Tablet).
 *
 * On XR devices that cannot currently render spatial UI, the ApplicationSubspace will still create
 * its scene and all of its internal state, even though nothing may be rendered. This is to ensure
 * that the state is maintained consistently in the spatial scene and to allow preparation for the
 * support of rendering spatial UI. State should be maintained by the compose runtime and events
 * that cause the compose runtime to lose state (app process killed or configuration change) will
 * also cause the ApplicationSubspace to lose its state.
 *
 * @param modifier The [SubspaceModifier] to be applied to the content of this Subspace.
 * @param allowUnboundedSubspace If true, the default recommended content box constraints will not
 *   be applied, allowing the Subspace to be infinite. Defaults to false, providing a safe, bounded
 *   space.
 * @param content The 3D content to render within this Subspace.
 */
@Composable
@ComposableOpenTarget(index = -1)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
public fun ApplicationSubspace(
    modifier: SubspaceModifier = SubspaceModifier,
    allowUnboundedSubspace: Boolean = false,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    // If not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    ApplicationSubspace(
        modifier = modifier,
        allowUnboundedSubspace = allowUnboundedSubspace,
        subspaceRootNode = LocalSubspaceRootNode.current,
        content = content,
    )
}

/**
 * Create a Subspace that is rooted in the application space.
 *
 * This is used as the top-level Subspace within the context of the default task window. Nested
 * Subspaces should use their nearest Panel that contains the Subspace to determine the sizing
 * constraints and position of the Subspace.
 *
 * In the near future when HSM is spatialized, the Subspace should consider the app bounds when
 * determining its top-level constraints.
 *
 * TODO(b/419369273) Add test cases for activity to activity transitions and switching applications.
 */
@Composable
private fun ApplicationSubspace(
    modifier: SubspaceModifier,
    allowUnboundedSubspace: Boolean,
    subspaceRootNode: Entity? = LocalSubspaceRootNode.current,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val compositionContext = rememberCompositionContext()
    val subspaceRoot = remember { GroupEntity.create(session, "SubspaceRoot") }
    val scene by remember {
        session.scene.mainPanelEntity.setEnabled(false)
        disposableValueOf(
            SpatialComposeScene(
                lifecycleOwner = lifecycleOwner,
                context = context,
                jxrSession = session,
                parentCompositionContext = compositionContext,
                rootEntity = CoreGroupEntity(subspaceRoot),
            )
        ) {
            it.dispose()
            subspaceRoot.dispose()
            try {
                session.scene.mainPanelEntity.setEnabled(true)
            } catch (_: IllegalStateException) {
                // TODO(b/450063142) The shutdown order of Impress, SceneCore, and Compose should be
                //  fixed to avoid having to catch this exception here.
                // When this Composable is disposed, it's possible the Activity is already
                // being destroyed, which also destroys the underlying session. Accessing
                // `session.scene` would then throw an IllegalStateException, as checked
                // in `checkAndGetScene`. We can safely ignore this exception as the app
                // is tearing down and the main panel does not need to be re-enabled.
            }
        }
    }
    LaunchedEffect(subspaceRootNode) { subspaceRootNode?.let { subspaceRoot.parent = it } }

    scene.rootVolumeConstraints = remember { VolumeConstraints() }

    scene.setContent {
        CompositionLocalProvider(LocalIsInApplicationSubspace provides true) {
            val finalModifier =
                if (allowUnboundedSubspace) {
                    modifier
                } else {
                    modifier.then(SubspaceModifier.recommendedSizeIfUnbounded())
                }
            SpatialBox(modifier = finalModifier, content = content)
        }
    }
}

/*
 * Embedded Subspace whose parent is SpatialPanel.
 * This Subspace is constrained by the parent's constraints in width and height.
 */
@Composable
private fun PanelEmbeddedSubspace(
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit
) {
    // If not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val compositionContext = rememberCompositionContext()
    val coreEntity = checkNotNull(LocalCoreEntity.current) { "CoreEntity unavailable for subspace" }
    // The subspace root node will be owned and manipulated by the containing composition, we need a
    // container that we can manipulate at the Subspace level in order to position the entire
    // subspace properly.
    val subspaceRootContainer by remember {
        disposableValueOf(
            CoreGroupEntity(GroupEntity.create(session, "SubspaceRootContainer")).apply {
                enabled = false
                parent = coreEntity
            }
        ) {
            it.dispose()
        }
    }
    val scene by remember {
        val subspaceRoot =
            CoreGroupEntity(GroupEntity.create(session, "SubspaceRoot")).apply {
                parent = subspaceRootContainer
            }
        disposableValueOf(
            SpatialComposeScene(
                lifecycleOwner = lifecycleOwner,
                context = context,
                jxrSession = session,
                parentCompositionContext = compositionContext,
                rootEntity = subspaceRoot,
            )
        ) {
            it.dispose()
            subspaceRoot.dispose()
        }
    }
    var subspaceContentPixelSize by remember { mutableStateOf(IntSize.Zero) }
    val parentSize = coreEntity.mutableSize.run { IntSize(width, height) }
    val density = LocalDensity.current
    val placeholderDpSize =
        subspaceContentPixelSize.run { with(density) { DpSize(width.toDp(), height.toDp()) } }
    val view = LocalView.current

    // Render a Spacer in a Layout such that the measurable passed to the 2D layout has the same
    // size as the content in the SubspaceLayout, but the SubspaceLayout gets the constraints
    // unaffected by its own size. This also triggers recomposition but prevents state reads in the
    // layout block.
    // This allows us to get the final 2D coordinates from the placement block (`layout{...}`) and
    // call `setPose` in the same frame, therefore it offers a better sync between the 3D pose and
    // the 2D layout pass.
    Layout(
        content = { Spacer(Modifier.size(placeholderDpSize.width, placeholderDpSize.height)) }
    ) { measurables, constraints ->
        // We set the scene content here so the 3D content has access to the 2D constraints.
        scene.setContent {
            SubspaceLayout(content = { SpatialBox(content = content) }) { subspaceMeasurables, _ ->
                val volumeConstraints = view.findVolumeConstraints()
                val placeables =
                    subspaceMeasurables.map {
                        it.measure(
                            VolumeConstraints(
                                minWidth = constraints.minWidth,
                                maxWidth = constraints.maxWidth,
                                minHeight = constraints.minHeight,
                                maxHeight = constraints.maxHeight,
                                minDepth = volumeConstraints?.minDepth ?: 0,
                                maxDepth = volumeConstraints?.maxDepth ?: Int.MAX_VALUE,
                            )
                        )
                    }
                val measuredContentVolume =
                    IntVolumeSize(
                            width = placeables.maxOf { it.measuredWidth },
                            height = placeables.maxOf { it.measuredHeight },
                            depth = placeables.maxOf { it.measuredDepth },
                        )
                        .apply { subspaceContentPixelSize = IntSize(width, height) }
                layout(
                    measuredContentVolume.width,
                    measuredContentVolume.height,
                    measuredContentVolume.depth,
                ) {
                    placeables.forEach { it.place(Pose.Identity) }
                }
            }
        }

        // We only expect one measurable here, which is the Spacer we added above. We don't actually
        // need to place the spacer though since we are just using it for size.
        val placeable = measurables[0].measure(constraints)
        val measuredPlaceholderSize = IntSize(placeable.width, placeable.height)
        layout(measuredPlaceholderSize.width, measuredPlaceholderSize.height) {
            // Here we determine the correct position for the 3D content and place the root node.
            // This ensures tighter coordination between the 2D and 3D placement. Note that this is
            // still imperfect as rendering is not explicitly synchronized.
            if (measuredPlaceholderSize != IntSize.Zero && parentSize != IntSize.Zero) {
                val contentOffset = coordinates?.positionInRoot() ?: return@layout
                val nextPose =
                    calculatePose(contentOffset, parentSize, measuredPlaceholderSize, density)
                subspaceRootContainer.poseInMeters = nextPose
                subspaceRootContainer.enabled = true
            }
        }
    }
}

/**
 * Controls the default distance (z-offset) between the content and the BodyPart.
 *
 * This offset is applied to push the content in front of the user, so their anchor point (e.g.,
 * head) is not inside the content. The values are in meters.
 */
private object UserSubspaceDefaults {
    @SuppressLint("PrimitiveInCollection")
    @OptIn(ExperimentalUserSubspaceApi::class)
    val OFFSETS: Map<BodyPart, Meter> = mapOf(BodyPart.Head to (-0.5f).meters)
}

/**
 * Marks Subspace APIs that are experimental and likely to change or be removed in the future.
 *
 * Any usage of a declaration annotated with `@ExperimentalUserSubspaceApi` must be accepted either
 * by annotating that usage with `@OptIn(ExperimentalUserSubspaceApi::class)` or by propagating the
 * annotation to the containing declaration.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an experimental API. It may be changed or removed in the future.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalUserSubspaceApi

/**
 * Create a user-centric 3D space that is ideal for spatial UI content that follows the user's given
 * body part with configurable following behaviors.
 *
 * Each call to `UserSubspace` creates a new, independent spatial UI hierarchy. It does **not**
 * inherit the spatial position, orientation, or scale of any parent `ApplicationSubspace` it is
 * nested within. Its position in the world is determined solely by its `lockTo` parameter.
 *
 * By default, this Subspace is automatically bounded by the system's recommended content box,
 * similar to [ApplicationSubspace]. When using BodyPart.Head as the lockTo target, this API
 * requires headtracking to not be disabled in the session configuration. If it is disabled, this
 * API will not return anything. The session configuration should resemble `session.configure(
 * config = session.config.copy(headTracking = Config.HeadTrackingMode.LAST_KNOWN) )`
 *
 * This composable is a no-op in non-XR environments (i.e., Phone and Tablet).
 *
 * ## Managing Spatial Overlap
 * Because each call to any kind of Subspace function creates an independent 3D scene, these spaces
 * are not aware of one another. This can lead to a phenomenon known as the "tunneling effect,"
 * where a moving `UserSubspace` (like a head-locked menu) can intersect with content in another
 * stationary Subspace. This overlap can cause jarring visual artifacts and z-depth ordering issues
 * (Z-fighting), creating a confusing user experience. A Subspace does not perform automatic
 * collision avoidance between these independent Subspaces. It is the developer's responsibility to
 * manage the layout and prevent these intersections or to introduce custom hit handling.
 *
 * ### Guidelines for Preventing Overlap:
 * 1. **Control Volume Size**: Carefully define the bounds of your Subspace instances. Instead of
 *    letting content fill the maximum recommended constraints, use sizing modifiers to create
 *    smaller, manageable content areas that are less likely to collide.
 * 2. **Use Strategic Offsets**: Use `SubspaceModifier.offset` to position a Subspace. For example,
 *    a head-locked menu can be offset to appear in the user's peripheral vision, reducing the
 *    chance it will collide with central content.Also, consider placing different Subspace
 *    instances at different depths. This ensures that if they overlap, their z-depth ordering will
 *    be clear and predictable. Note, however, that while the visual ordering may be clear, Jetpack
 *    XR doesn't guarantee predictable interaction behaviors between UI elements in separate,
 *    overlapping Subspaces.
 *
 * @param modifier The [SubspaceModifier] to be applied to the content of this Subspace.
 * @param lockTo Specifies a part of the body which the Subspace will be locked to.
 * @param lockDimensions A set of boolean flags to determine the dimensions of movement that are
 *   tracked. Possible tracking dimensions are: translationX, translationY, translationZ, rotationX,
 *   rotationY, and rotationZ. By default, all dimensions are tracked. Any dimensions not listed
 *   will not be tracked. For example if translationY is not listed, this means the content will not
 *   move as the user moves vertically up and down.
 * @param behavior determines how the UserSubspace follows the user. It can be made to move faster
 *   and be more responsive. The default is LockingBehavior.lazy().
 * @param allowUnboundedSubspace If true, the default recommended content box constraints will not
 *   be applied, allowing the Subspace to be infinite. Defaults to false, providing a safe, bounded
 *   space.
 * @param content The 3D content to render within this Subspace.
 */
// TODO(b/446871230): Add unit tests for UserSubspace.
@Composable
@ComposableOpenTarget(index = -1)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@ExperimentalUserSubspaceApi
public fun UserSubspace(
    modifier: SubspaceModifier = SubspaceModifier,
    lockTo: BodyPart = BodyPart.Head,
    lockDimensions: LockDimensions = LockDimensions.All,
    behavior: LockingBehavior = LockingBehavior.lazy(),
    allowUnboundedSubspace: Boolean = false,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    // If not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }

    if (session.config.headTracking == Config.HeadTrackingMode.DISABLED) {
        return
    }

    val userSubspaceRoot by remember {
        disposableValueOf(GroupEntity.create(session, "UserSubspaceRoot")) { it.dispose() }
    }
    SideEffect {
        session.scene.keyEntity?.getScale(relativeTo = Space.REAL_WORLD)?.let { scale ->
            userSubspaceRoot.setScale(scale)
        }
    }
    val userSubspaceRootNode by remember {
        disposableValueOf(CoreGroupEntity(userSubspaceRoot).apply { enabled = true }) {
            it.dispose()
        }
    }

    LaunchedEffect(behavior, lockTo, lockDimensions) {
        behavior.configure(
            session = session,
            trailingEntity = userSubspaceRootNode,
            lockTo = lockTo,
            lockDimensions = lockDimensions,
        )
    }

    // The content is wrapped in a SpatialBox and we move it slightly ahead of the `lockTo` body
    // part instead of the user being in the middle of it. But the user is still centered in the
    // Subspace.
    ApplicationSubspace(
        modifier = modifier,
        allowUnboundedSubspace = allowUnboundedSubspace,
        subspaceRootNode = userSubspaceRoot,
    ) {
        val subspaceOffset =
            checkNotNull(UserSubspaceDefaults.OFFSETS[lockTo]) {
                "No offset found for lockTo target."
            }
        SpatialBox(modifier = SubspaceModifier.offset(z = subspaceOffset.toDp()), content = content)
    }
}

/**
 * A Subspace that does not match the scaling, alignment, and placement suggested by the system.
 * Instead it will align itself to gravity (perpendicular to the floor) and have a scale value equal
 * to the scale of the [androidx.xr.scenecore.ActivitySpace] of the application (1:1 with OpenXR
 * Unbounded Reference Space).
 *
 * [GravityAlignedSubspace] should be used to create a topmost Subspace in your application's
 * spatial UI hierarchy.
 *
 * By default, this Subspace is automatically bounded by the system's recommended content box. This
 * box represents a comfortable, human-scale area in front of the user, sized to occupy a
 * significant portion of their view on any given device. Using this default is the suggested way to
 * create responsive spatial layouts that look great without hardcoding dimensions.
 * SubspaceModifiers like SubspaceModifier.fillMaxSize will expand to fill this recommended box.
 * This default can be overridden by applying a custom size-based modifier. For unbounded behavior,
 * set `[allowUnboundedSubspace] = true`.
 *
 * @param modifier The [SubspaceModifier] to be applied to the content of this Subspace.
 * @param allowUnboundedSubspace If true, the default recommended content box constraints will not
 *   be applied, allowing the Subspace to be infinite. Defaults to false, providing a safe, bounded
 *   space.
 * @param content The 3D content to render within this Subspace.
 * @throws [IllegalStateException] - If the activity in which it is hosted is not a
 *   [ComponentActivity]
 *
 * A composable that performs no operation and renders nothing in non-XR environments (e.g., phones
 * and tablets).
 *
 * For conditionally rendering content based on the environment, see
 * [androidx.xr.compose.platform.SpatialConfiguration].
 *
 * TODO(b/431767697): Constraints should be a SubspaceModifier
 */
@Composable
@ComposableOpenTarget(index = -1)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun GravityAlignedSubspace(
    modifier: SubspaceModifier = SubspaceModifier,
    allowUnboundedSubspace: Boolean = false,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    // If we are not in XR, do nothing
    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    if (LocalIsInApplicationSubspace.current) {
        throw IllegalStateException(
            "GravityAlignedSubspace cannot be nested within another Subspace."
        )
    } else {
        ApplicationSubspace(
            modifier = modifier,
            allowUnboundedSubspace = allowUnboundedSubspace,
            subspaceRootNode = null,
            content = content,
        )
    }
}

/**
 * Traverses up the view hierarchy starting from the given [View] to find the first view or ancestor
 * that has the `compose_xr_panel_volume_constraints` tag set.
 *
 * @return The [VolumeConstraints] object if found, otherwise `null`.
 */
private fun View.findVolumeConstraints(): VolumeConstraints? {
    var current: View? = this
    while (current != null) {
        val constraints = current.getTag(R.id.compose_xr_panel_volume_constraints)
        if (constraints is VolumeConstraints) {
            return constraints
        }
        current = current.getParentOrViewTreeDisjointParent() as? View
    }
    // No constraints found in this branch of the hierarchy
    return null
}

/**
 * Creates an ApplicationSubspace that places its content at a real-world location represented by
 * [AnchorEntity].
 *
 * This is useful for placing UI elements on real-world surfaces or at specific spatial locations.
 * The visual stability of the anchored content depends on the underlying system's ability to track
 * the [AnchorEntity].
 *
 * [AnchoredSubspace] follows the same conventions as [ApplicationSubspace], including layout and
 * sizing behaviors. See [ApplicationSubspace] for more details.
 *
 * Note: For Creating, loading, and persisting anchors, please check
 * [androidx.xr.scenecore.AnchorEntity] for more information
 *
 * @param lockTo the real-world [AnchorEntity] to which this space will be attached. If the
 *   developer changes the anchor parameter then the subspace will be reanchored to the swapped
 *   anchor.
 * @param modifier The [SubspaceModifier] to be applied to this Subspace.
 * @param allowUnboundedSubspace If true, the default recommended content box constraints will not
 *   be applied, allowing the Subspace to be infinite. Defaults to false, providing a safe, bounded
 *   space.
 * @param content The content to render within this Subspace.
 * @sample androidx.xr.compose.samples.AnchoredSubspaceSample
 * @see [ApplicationSubspace]
 */
@Composable
@ComposableOpenTarget(index = -1)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
public fun AnchoredSubspace(
    lockTo: AnchorEntity,
    modifier: SubspaceModifier = SubspaceModifier,
    allowUnboundedSubspace: Boolean = false,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {

    if (!LocalSpatialConfiguration.current.hasXrSpatialFeature) return

    ApplicationSubspace(
        modifier = modifier,
        subspaceRootNode = lockTo,
        content = content,
        allowUnboundedSubspace = allowUnboundedSubspace,
    )
}
