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

import androidx.xr.compose.spatial.LocalSubspaceRootNode
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Space

/**
 * A [SubspaceModifier] that forces the content to remain upright, aligned with gravity by isolating
 * the element's yaw by cancelling parent pitch and roll.
 *
 * This modifier calculates the necessary counter-rotation to ignore the pitch and roll of its
 * parent entity's total world transformation. It only affects the rotation and not the translation.
 * This is useful for UI elements like labels or billboards that should always stay level,
 * regardless of the orientation of the object they are attached to. It effectively isolates the
 * **yaw** (rotation around the vertical Y-axis).
 */
public fun SubspaceModifier.gravityAligned(): SubspaceModifier = this.then(GravityAlignedElement)

private object GravityAlignedElement : SubspaceModifierNodeElement<GravityAlignedNode>() {
    override fun create(): GravityAlignedNode = GravityAlignedNode()

    override fun update(node: GravityAlignedNode) {}

    override fun hashCode(): Int = "GravityAligned".hashCode()

    override fun equals(other: Any?): Boolean = other === this
}

internal class GravityAlignedNode :
    SubspaceModifier.Node(),
    SubspaceLayoutModifierNode,
    CompositionLocalConsumerSubspaceModifierNode {

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val placeable = measurable.measure(constraints)

        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            // Gravity Aligned Calculation:
            // 1. Determine the object's desired "forward" direction in World space, but projected
            // onto the horizontal (XZ) plane.
            // 2. Create a goal world rotation that points in this new horizontal direction.
            // 3. Convert this goal world rotation back into a local rotation relative to the parent
            // layout.
            // 4. Apply this new local rotation to the placeable.
            // The "World" here in variables names refer to SceneCore's [Space.REAL_WORLD] which is
            // the global coordinate space, unscaled and gravity aligned at the root of the scene
            // graph of the activity.
            val rootWorldRotation =
                currentValueOf(LocalSubspaceRootNode)?.getPose(Space.REAL_WORLD)?.rotation
                    ?: Quaternion.Identity
            val nodePoseInRoot = coordinates?.poseInRoot?.rotation ?: Quaternion.Identity
            val currentWorldRotation = rootWorldRotation * nodePoseInRoot

            // Calculate the node's current forward vector in the World space.
            val tiltedWorldForward = currentWorldRotation * Vector3.Forward

            // Project the forward vector onto the horizontal (XZ) plane by zeroing out the Y.
            // This removes any pitch and roll, leaving a vector perfectly level with the ground.
            val levelForwardTarget = Vector3(tiltedWorldForward.x, 0f, tiltedWorldForward.z)

            val goalWorldRotation: Quaternion =
                // Check for a DEGENERATE CASE: if the object is looking almost straight up or down,
                // the projected forward vector will be near zero length.
                if (levelForwardTarget.lengthSquared < 1e-6f) {
                    // DEGENERATE CASE: the yaw is undefined. The current forward vector is pointing
                    // straight up or down. Fallback to using the parent layout node's world yaw as
                    // a stable reference.
                    val parentPoseInRoot =
                        coordinates?.parentCoordinates?.poseInRoot?.rotation ?: Quaternion.Identity
                    val parentWorldRotation = rootWorldRotation * parentPoseInRoot
                    val parentWorldForward = parentWorldRotation * Vector3.Forward
                    val projectedParentForward =
                        Vector3(parentWorldForward.x, 0f, parentWorldForward.z)

                    if (projectedParentForward.lengthSquared < 1e-6f) {
                        // ULTIMATE FALLBACK: if the parent is also looking straight up or down.
                        Quaternion.Identity
                    } else {
                        // Create a rotation from the default forward to the parent's projected
                        // forward.
                        Quaternion.fromRotation(Vector3.Forward, projectedParentForward)
                    }
                } else {
                    // COMMON CASE: create a rotation that turns the default forward vector
                    // to face the new, level target direction.
                    Quaternion.fromRotation(Vector3.Forward, levelForwardTarget)
                }

            // Calculate the correction needed to go from the current rotation to the goal rotation.
            // This works because placement in the modifier chain is compositional.
            val newLocalRotation = currentWorldRotation.inverse * goalWorldRotation

            // Place the measured content using the new gravity-aligned local rotation.
            // The translation is zero as this modifier only affects the rotation.
            placeable.place(Pose(translation = Vector3.Zero, rotation = newLocalRotation))
        }
    }
}
