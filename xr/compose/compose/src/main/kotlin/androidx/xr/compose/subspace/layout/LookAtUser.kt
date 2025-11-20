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

import androidx.xr.arcore.ArDevice
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.LayoutCoordinatesAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.subspace.node.invalidateMeasurement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.scene
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [SubspaceModifier] that forces the content to remain upright and will rotate on the y-axis that
 * the content faces the user at all times.
 *
 * A user of this API should configure the activity's Session object with
 * [Config.HeadTrackingMode.LAST_KNOWN] which requires android.permission.HEAD_TRACKING Android
 * permission be granted by the calling application. `session.configure( config =
 * session.config.copy(headTracking = Config.HeadTrackingMode.LAST_KNOWN) )`
 *
 * @param enabled true if this composable should always face the user.
 * @see lookAtUser modifier for making content that will tilt in all directions to face the user.
 */
public fun SubspaceModifier.billboard(enabled: Boolean = true): SubspaceModifier =
    this.then(SubspaceModifier.lookAtUser(enabled).gravityAligned())

/**
 * A [SubspaceModifier] that continuously rotates content so that it faces the user at all times.
 *
 * A user of this API should configure the activity's Session object with
 * [Config.HeadTrackingMode.LAST_KNOWN] which requires `android.permission.HEAD_TRACKING` Android
 * permission be granted by the calling application. `session.configure( config =
 * session.config.copy(headTracking = Config.HeadTrackingMode.LAST_KNOWN) )`
 *
 * @param enabled true if this composable should always face the user.
 * @param up Indicates which direction is "up" when orienting the content upright. LookAtUser makes
 *   the front of the content face the user. But this can be accomplished with the content sitting
 *   upright, upside-down or any rotation in-between. "up" addresses that ambiguity. By default, up
 *   is 0,1,0.
 * @see billboard modifier for making content that will generally face the user's direction but
 *   keeps the content in an upright position.
 */
public fun SubspaceModifier.lookAtUser(
    enabled: Boolean = true,
    up: Vector3 = Vector3(0f, 1f, 0f),
): SubspaceModifier = this.then(LookAtUserElement(enabled, up))

private class LookAtUserElement(private val enabled: Boolean, private val up: Vector3) :
    SubspaceModifierNodeElement<LookAtUserNode>() {
    override fun create(): LookAtUserNode = LookAtUserNode(enabled, up)

    override fun update(node: LookAtUserNode) {
        node.enabled = enabled
        node.up = up
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + up.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LookAtUserElement) return false
        if (enabled != other.enabled) return false
        if (up != other.up) return false
        return true
    }
}

internal class LookAtUserNode(var enabled: Boolean, var up: Vector3) :
    SubspaceModifier.Node(),
    SubspaceLayoutModifierNode,
    CoreEntityNode,
    LayoutCoordinatesAwareModifierNode,
    CompositionLocalConsumerSubspaceModifierNode {

    internal companion object {
        private const val ROTATION_THRESHOLD: Float = 1f
    }

    private var headPoseJob: Job? = null
    private var deltaRotation: Quaternion = Quaternion.Identity

    override fun CoreEntityScope.modifyCoreEntity() {}

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val placeable = measurable.measure(constraints)

        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(Pose(translation = Vector3.Zero, rotation = deltaRotation))
        }
    }

    override fun onLayoutCoordinates(coordinates: SubspaceLayoutCoordinates) {
        updateState()
    }

    /** Evaluates the enabled state and manages the headPoseJob accordingly */
    private fun updateState() {
        val session = checkNotNull(currentValueOf(LocalSession)) { "session must be initialized" }

        // If the headPoseJob is not running, and it should be, then run it.
        if (
            enabled &&
                headPoseJob?.isActive != true &&
                session.config.headTracking != Config.HeadTrackingMode.DISABLED
        ) {
            headPoseJob =
                coroutineScope.launch {
                    val arDevice = ArDevice.getInstance(session)
                    arDevice.state.collect { state -> updatePose(session, state) }
                }
        } else if (!enabled && headPoseJob?.isActive == true) {
            headPoseJob?.cancel()
            deltaRotation = Pose.Identity.rotation
            // TODO(b/460828333): invalidatePlacement() appears to be skipping valid nodes.
            // When this is resolved, revert this code to use invalidatePlacement() instead of
            // invalidateMeasurement().
            invalidateMeasurement()
        }
    }

    private suspend fun updatePose(session: Session, state: ArDevice.State) {
        val headPose =
            session.scene.perceptionSpace.transformPoseTo(
                state.devicePose,
                session.scene.activitySpace,
            )

        // Calculate totalTranslation by adding the pose translations of the
        // coreEntity and its parents, all the way up to the root node.
        var tempNode: CoreEntity? = coreEntity
        var totalTranslation = Pose.Identity.translation
        while (tempNode != null) {
            totalTranslation += tempNode.poseInMeters.translation
            tempNode = tempNode.parent
        }

        // Direction vector from entity to user.
        val targetVector = (headPose.translation - totalTranslation).toNormalized()

        // Calculate the rotation needed relative to the targetVector
        val targetRotation = Quaternion.fromLookTowards(targetVector, up)

        // Calculate angle difference to determine if head movement was significant.
        val rotationDelta = Quaternion.angle(targetRotation, coreEntity.poseInMeters.rotation)

        // Ignore very subtle head movements to avoid constant updating of entity.
        if (abs(rotationDelta) > ROTATION_THRESHOLD) {
            // Update just the rotation of the pose.
            deltaRotation = targetRotation
            invalidatePlacement()
        }
    }

    override fun onDetach() {
        headPoseJob?.cancel()
    }
}
