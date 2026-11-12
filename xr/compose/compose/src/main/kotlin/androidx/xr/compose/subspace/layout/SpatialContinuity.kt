/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.snapshotFlow
import androidx.xr.compose.platform.ComposeXrOwnerLocals
import androidx.xr.compose.platform.LocalComposeXrOwners
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.SessionSpatialConfiguration
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.animation.follow.FollowTarget
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Marks Spatial Continuity APIs as experimental. These APIs may change or be removed in the future.
 *
 * Any usage of a declaration annotated with `@ExperimentalSpatialContinuityApi` must be accepted
 * either by annotating that usage with `@OptIn(ExperimentalSpatialContinuityApi::class)` or by
 * propagating the annotation to the containing declaration.
 */
@RequiresOptIn(message = "This is an experimental API. It may be changed or removed in the future.")
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalSpatialContinuityApi

/**
 * Controls the spatial continuity target for a subspace composable associated via
 * [spatialContinuityTarget].
 *
 * Spatial continuity coordinates 3D placement across two types of spatial transitions for the
 * current [android.app.Activity]:
 * 1. **Outgoing placement**: Determines where subsequent content spawns in 3D space when this
 *    [android.app.Activity] launches a new [android.app.Activity] (e.g., via
 *    [android.content.Context.startActivity]) or when this [android.app.Activity] transitions from
 *    Full Space to Home Space (where the Activity's 2D window is placed in Home Space). By default,
 *    the system uses the [SpatialMainPanel]'s pose if present in the
 *    [androidx.xr.compose.spatial.Subspace], or the [androidx.xr.compose.spatial.Subspace] center
 *    otherwise. Calling [captureContinuity] sets the associated composable's pose as this launch
 *    target.
 * 2. **Incoming alignment**: Determines which part of this [android.app.Activity]'s
 *    [androidx.xr.compose.spatial.Subspace] is centered at the system's recommended pose when the
 *    system repositions this Activity (e.g., when re-centering via system quick settings, switching
 *    from Home Space to Full Space, or navigating back to this Activity from another Activity).
 *    When [captureContinuity] is called with `keepInFocus = true`, Compose for XR shifts the
 *    [androidx.xr.compose.spatial.Subspace] root so that the associated composable is centered at
 *    the system's recommended pose. When `keepInFocus = false`, the center of the
 *    [androidx.xr.compose.spatial.Subspace] is placed at the recommended pose instead.
 *
 * Create a separate [SpatialContinuityCapturer] for each composable. If a single instance is
 * attached to multiple [spatialContinuityTarget] modifiers, [captureContinuity] applies to the last
 * attached composable.
 *
 * Calling [captureContinuity] before this capturer is attached to a [spatialContinuityTarget]
 * modifier is a no-op and returns `false`.
 *
 * @sample androidx.xr.compose.samples.SpatialContinuitySample
 * @see androidx.xr.compose.subspace.layout.spatialContinuityTarget
 */
@ExperimentalSpatialContinuityApi
public class SpatialContinuityCapturer @RememberInComposition public constructor() {
    internal val nodes: MutableList<SpatialContinuityTargetNode> = mutableListOf()

    /**
     * Captures the associated subspace composable as the active spatial continuity target.
     *
     * If this capturer is attached to multiple [spatialContinuityTarget] modifiers, spatial
     * continuity is applied to the last attached composable. If the composable is outside the
     * user's field of view when an outgoing transition occurs, the system falls back to the user's
     * approximate gaze position. If the composable is removed from composition while active,
     * [resetContinuity] is executed to restore the default continuity behavior.
     *
     * @param keepInFocus whether Compose for XR automatically shifts this Activity's
     *   [androidx.xr.compose.spatial.Subspace] so that this composable is centered at the system's
     *   recommended pose on incoming space updates (re-centering, switching from Home Space to Full
     *   Space, or navigating back to this Activity). When `false`, this composable still serves as
     *   the outgoing launch target when starting a new Activity or transitioning to Home Space,
     *   while incoming space updates align the [androidx.xr.compose.spatial.Subspace] center with
     *   the recommended pose. Defaults to `true`.
     * @return `true` if spatial continuity was captured on an attached composable, or `false` if
     *   this [SpatialContinuityCapturer] is not currently attached to a [spatialContinuityTarget]
     *   modifier
     */
    @ExperimentalSpatialContinuityApi
    public fun captureContinuity(keepInFocus: Boolean = true): Boolean {
        val currentNode = nodes.lastOrNull() ?: return false
        return currentNode.captureContinuity(keepInFocus)
    }

    public companion object {
        /**
         * Resets spatial continuity to its default behavior: outgoing Activity launches and Home
         * Space transitions use the [SpatialMainPanel] (or the
         * [androidx.xr.compose.spatial.Subspace] center if [SpatialMainPanel] is not present), and
         * incoming space updates align the [androidx.xr.compose.spatial.Subspace] center with the
         * system's recommended pose. When chaining continuity across multiple Activities, call this
         * in a back handler if you want a previous Activity to restore its default placement.
         *
         * @sample androidx.xr.compose.samples.ResetSpatialContinuitySample
         * @param session the XR [Session] in which to reset spatial continuity
         */
        @ExperimentalSpatialContinuityApi
        @JvmStatic
        public fun resetContinuity(session: Session) {
            session.scene.keyEntity = session.scene.mainPanelEntity
        }
    }
}

/**
 * Associates this composable with [spatialContinuityCapturer] so its 3D pose can be designated as
 * the spatial continuity target.
 *
 * When activated via [SpatialContinuityCapturer.captureContinuity], this composable's pose governs:
 * - Where a new [android.app.Activity] launched from the current Activity spawns in 3D space.
 * - Where the Activity's 2D window is placed when transitioning from Full Space to Home Space.
 * - Which composable is centered at the system's recommended pose when re-centering, transitioning
 *   from Home Space to Full Space, or navigating back to the current Activity (when `keepInFocus`
 *   is `true`).
 *
 * For example, if a user moves a panel and launches another Activity from a button on that panel,
 * calling [SpatialContinuityCapturer.captureContinuity] before
 * [android.content.Context.startActivity] ensures the newly launched Activity spawns at that
 * panel's location.
 *
 * Note: When combining [spatialContinuityTarget] with [movable] using [MovePolicy.anchor], avoid
 * placing additional panels in the same Activity, as secondary panel orientations may not align
 * with the anchor if the Activity is recreated. Spatial continuity has no effect on children of a
 * [androidx.xr.compose.spatial.Subspace] that uses a [FollowTarget].
 *
 * @sample androidx.xr.compose.samples.SpatialContinuitySample
 * @param spatialContinuityCapturer the [SpatialContinuityCapturer] that controls spatial continuity
 *   for this composable
 */
@ExperimentalSpatialContinuityApi
public fun SubspaceModifier.spatialContinuityTarget(
    spatialContinuityCapturer: SpatialContinuityCapturer
): SubspaceModifier = this.then(SpatialContinuityTargetElement(spatialContinuityCapturer))

@OptIn(ExperimentalSpatialContinuityApi::class)
internal data class SpatialContinuityTargetElement(
    val spatialContinuityCapturer: SpatialContinuityCapturer
) : SubspaceModifierNodeElement<SpatialContinuityTargetNode>() {
    override fun create(): SpatialContinuityTargetNode =
        SpatialContinuityTargetNode(spatialContinuityCapturer)

    override fun update(node: SpatialContinuityTargetNode) {
        node.spatialContinuityCapturer = spatialContinuityCapturer
    }
}

@OptIn(ExperimentalSpatialContinuityApi::class)
internal class SpatialContinuityTargetNode(spatialContinuityCapturer: SpatialContinuityCapturer) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    SubspaceLayoutAwareModifierNode {

    internal val coreEntity: CoreEntity
        get() =
            checkNotNull(layoutNode?.coreEntity) {
                "SpatialContinuityTargetNode must be attached to a SubspaceLayoutNode to access the CoreEntity."
            }

    internal var spatialContinuityCapturer: SpatialContinuityCapturer = spatialContinuityCapturer
        set(newCapturer) {
            if (field != newCapturer) {
                field.nodes -= this
                field = newCapturer
                if (isAttached && this !in newCapturer.nodes) {
                    newCapturer.nodes += this
                }
            }
        }

    internal var keepInFocus: Boolean = false
    private var attachedSession: Session? = null
    private var recommendedPoseJob: Job? = null

    override fun onAttach() {
        if (this !in spatialContinuityCapturer.nodes) {
            spatialContinuityCapturer.nodes += this
        }
    }

    internal fun captureContinuity(keepInFocus: Boolean): Boolean {
        if (!isAttached) return false
        val session = currentValueOf(LocalSession) ?: return false
        attachedSession = session
        this.keepInFocus = keepInFocus
        coreEntity.setAsKeyEntity(session)
        return true
    }

    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {
        if (recommendedPoseJob?.isActive == true) return
        val session = currentValueOf(LocalSession) ?: return
        attachedSession = session
        val locals = currentValueOf(LocalComposeXrOwners)
        val config = locals.spatialConfiguration as? SessionSpatialConfiguration ?: return

        // The system recommendedPose can be updated when moving between activities, switching
        // spatial modes or when the OS is re-centered. When this occurs, move the subspaceRootNode
        // such that the keyEntity will appear at the recommended pose.
        recommendedPoseJob = coroutineScope.launch {
            snapshotFlow { config.recommendedPose }
                .collect { recommendedPose ->
                    if (keepInFocus && coreEntity.isKeyEntity(session)) {
                        alignKeyEntityWithRecommendedPose(recommendedPose, session, locals)
                    }
                }
        }
    }

    private fun alignKeyEntityWithRecommendedPose(
        recommendedPose: Pose,
        session: Session,
        locals: ComposeXrOwnerLocals,
    ) {
        val keyEntity = session.scene.keyEntity ?: return
        val subspaceRootNode = locals.subspaceRootNode ?: return
        val activityFromRoot = subspaceRootNode.getPose(Space.ACTIVITY)
        val activityFromKeyEntity = keyEntity.getPose(Space.ACTIVITY)

        // Pose of keyEntity relative to subspaceRootNode.
        val rootFromKeyEntity = activityFromRoot.inverse.compose(activityFromKeyEntity)

        // Position subspaceRootNode such that keyEntity aligns with recommendedPose.
        val targetFromRoot = recommendedPose.compose(rootFromKeyEntity.inverse)

        subspaceRootNode.setPose(pose = targetFromRoot, relativeTo = Space.ACTIVITY)
    }

    override fun onDetach() {
        spatialContinuityCapturer.nodes -= this
        recommendedPoseJob?.cancel()
        recommendedPoseJob = null
        keepInFocus = false

        val session = attachedSession ?: return
        if (coreEntity.isKeyEntity(session)) {
            SpatialContinuityCapturer.resetContinuity(session)
        }
        attachedSession = null
    }
}
