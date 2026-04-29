/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.xr.arcore.ArDevice
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.LocalSubspaceRootNode
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.subspace.node.invalidatePlacement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [SubspaceModifier] that continuously rotates content so that it faces the user at all times.
 *
 * A user of this API should configure the activity's Session object with
 * [DeviceTrackingMode.SPATIAL] which requires `android.permission.HEAD_TRACKING` Android permission
 * be granted by the calling application. `session.configure( config =
 * session.config.copy(deviceTracking = DeviceTrackingMode.SPATIAL) )`
 *
 * This modifier might not work as expected when used on content within a
 * [androidx.xr.compose.spatial.FollowingSubspace].
 *
 * The preceding rotate modifiers will be disregarded because this modifier will override them. But
 * the rotate after the `rotateToLookAtUser` modifier will be respected.
 *
 * To achieve a "billboard" effect—where the content rotates to face the user on the Y-axis while
 * remaining upright and aligned with gravity—combine this with [gravityAligned].
 *
 * @sample androidx.xr.compose.samples.RotateToLookAtUserBillboardSample
 * @sample androidx.xr.compose.samples.RotateToLookAtUserWithUpVectorSample
 * @sample androidx.xr.compose.samples.RotateToLookAtUserUnderParentContainerSample
 * @param upDirection Defines the reference "up" direction for the content's orientation. Pointing
 *   the content's forward vector at the user leaves the rotation around that axis (roll) undefined;
 *   this vector resolves that ambiguity. The default is Vector3.Up, which corresponds to the up
 *   direction of the ActivitySpace.
 */
// TODO(b/461808266): RotateToLookAtUser and FollowingSubspace not compatible with each other
// TODO(b/487087894): [Moohan Emulator] ARCore ArDevice emit identity pose until user moves
public fun SubspaceModifier.rotateToLookAtUser(
    upDirection: Vector3 = Vector3.Up
): SubspaceModifier = this.then(RotateToLookAtUserElement(upDirection))

private class RotateToLookAtUserElement(private val upDirection: Vector3) :
    SubspaceModifierNodeElement<RotateToLookAtUserNode>() {
    override fun create(): RotateToLookAtUserNode = RotateToLookAtUserNode(upDirection)

    override fun update(node: RotateToLookAtUserNode) {
        node.upDirection = upDirection
    }

    override fun hashCode(): Int = upDirection.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RotateToLookAtUserElement) return false
        return (upDirection == other.upDirection)
    }
}

internal class RotateToLookAtUserNode(var upDirection: Vector3) :
    SubspaceModifier.Node(),
    SubspaceLayoutModifierNode,
    SubspaceLayoutAwareModifierNode,
    CompositionLocalConsumerSubspaceModifierNode {
    private lateinit var session: Session
    private lateinit var arDevice: ArDevice
    private var headPoseJob: Job? = null
    private var currentHeadPose: Pose = Pose()

    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    @Suppress("RestrictedApiAndroidX")
    override fun onAttach() {
        super.onAttach()
        // Initialize the Session and ArDevice once when the node is attached
        session =
            checkNotNull(currentValueOf(LocalSession)) {
                "LocalSession must be available during onAttach."
            }

        if (session.config.deviceTracking == DeviceTrackingMode.DISABLED) {
            return
        }
        arDevice = ArDevice.getInstance(session)
    }

    // Launching coroutineScope in onAttach throws IllegalStateException
    // Deferring to onPlaced ensures the node is attached to a valid Owner
    override fun onPlaced(coordinates: SubspaceLayoutCoordinates) {
        manageHeadPoseJob()
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val placeable: SubspacePlaceable = measurable.measure(constraints = constraints)

        return layout(width = placeable.width, height = placeable.height, depth = placeable.depth) {
            // Get the pose of the root node in the ActivitySpace.
            // Transform from Root space to Activity space (dst_From_src notation).
            val activitySpaceFromRoot: Pose =
                currentValueOf(LocalSubspaceRootNode)?.getPose(relativeTo = Space.ACTIVITY)
                    ?: Pose.Identity

            // Get the pose of the node in the Compose root space.
            // Transform from Node space to root space.
            val rootFromNodePixels: Pose = coordinates?.poseInRoot ?: Pose.Identity

            // Convert the node's pose in Compose root from pixels to meters.
            val rootFromNodeMeters: Pose =
                rootFromNodePixels.convertPixelsToMeters(
                    density = this@RotateToLookAtUserNode.density
                )

            // Chain (compose) the transforms: Node -> Root -> Activity.
            val activitySpaceFromNode: Pose =
                activitySpaceFromRoot.compose(other = rootFromNodeMeters)

            // Extract Payloads in Activity Space.
            val nodeActivitySpaceTranslation: Vector3 = activitySpaceFromNode.translation
            val headActivitySpaceTranslation: Vector3 = currentHeadPose.translation

            // Calculate the vector pointing "from" the node "to" the head
            val nodeToHeadDirection: Vector3 =
                headActivitySpaceTranslation - nodeActivitySpaceTranslation

            // Calculate Target Rotation:
            // This is the absolute rotation the node needs in ActivitySpace.
            val activitySpaceFromTargetNodeRotation: Quaternion =
                Quaternion.fromLookTowards(forward = nodeToHeadDirection, up = upDirection)

            // Calculate Local Delta Rotation:
            // We know: activitySpaceFromTargetNodeRotation = activitySpaceFromNode.rotation *
            // localRotationOffset
            // To isolate localRotationOffset, multiply both sides by the inverse of the parent
            // rotation.
            val nodeFromActivitySpaceRotation: Quaternion = activitySpaceFromNode.rotation.inverse
            val localRotationOffset: Quaternion =
                nodeFromActivitySpaceRotation * activitySpaceFromTargetNodeRotation

            // Place the measured content using the new local rotation offset.
            placeable.place(pose = Pose(translation = Vector3.Zero, rotation = localRotationOffset))
        }
    }

    private fun manageHeadPoseJob() {
        if (headPoseJob?.isActive == true) return
        headPoseJob =
            coroutineScope.launch { arDevice.state.collect { state -> updatePose(state) } }
    }

    private fun updatePose(state: ArDevice.State) {
        currentHeadPose =
            session.scene.perceptionSpace.transformPoseTo(
                state.devicePose,
                session.scene.activitySpace,
            )
        invalidatePlacement()
    }

    override fun onDetach() {
        super.onDetach()
        headPoseJob?.cancel()
    }
}
