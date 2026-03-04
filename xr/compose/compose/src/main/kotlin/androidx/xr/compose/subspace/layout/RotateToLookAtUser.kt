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
import androidx.xr.runtime.XrLog
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
 * [DeviceTrackingMode.SPATIAL_LAST_KNOWN] which requires `android.permission.HEAD_TRACKING` Android
 * permission be granted by the calling application. `session.configure( config =
 * session.config.copy(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN) )`
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
// TODO(b/461808266): LookAtUser and FollowingSubspace not compatible with each other
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

    override fun onAttach() {
        super.onAttach()
        // Initialize the Session and ArDevice once when the node is attached
        session =
            checkNotNull(currentValueOf(LocalSession)) {
                "LocalSession must be available during onAttach."
            }

        if (session.config.deviceTracking == DeviceTrackingMode.DISABLED) {
            XrLog.warn("Head tracking must be enabled in the Session config to use LookAtUser.")
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
        val placeable = measurable.measure(constraints)

        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            // Calculate the node's current position in activity space.
            val rootActivitySpaceTransformation =
                currentValueOf(LocalSubspaceRootNode)?.getPose(Space.ACTIVITY) ?: Pose.Identity
            val nodePoseInRoot = coordinates?.poseInRoot ?: Pose.Identity
            val currentActivitySpaceTransformation =
                rootActivitySpaceTransformation.compose(nodePoseInRoot)
            val currentActivitySpaceRotation = currentActivitySpaceTransformation.rotation
            val currentActivitySpaceTranslation =
                currentActivitySpaceTransformation.translation.convertPixelsToMeters(
                    this@RotateToLookAtUserNode.density
                )

            // Calculate the desired forward vector in activity space, pointing from
            // the node to the user.
            val targetVector = currentHeadPose.translation - currentActivitySpaceTranslation
            // Calculate the desired rotation of the node in activity space based on the desired
            // forward and up vectors.
            val goalActivitySpaceRotation: Quaternion =
                Quaternion.fromLookTowards(targetVector, upDirection)
            // Determine the local rotation that must be applied to the node to achieve the desired
            // rotation in activity space.
            val newLocalRotation = currentActivitySpaceRotation.inverse * goalActivitySpaceRotation
            // Place the measured content using the new local rotation, which will orient the
            // content so that if faces the user.
            placeable.place(Pose(translation = Vector3.Zero, rotation = newLocalRotation))
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
