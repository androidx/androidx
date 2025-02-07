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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Pose

/**
 * Moves a subspace element (i.e. currently only affects Jetpack XR Entity Panels/Volumes) in space.
 * The order of the [SubspaceModifier]s is important. Please take note of this when using movable.
 * If you have the following modifier chain: SubspaceModifier.offset().size().movable(), the
 * modifiers will work as expected. If instead you have this modifier chain:
 * SubspaceModifier.size().offset().movable(), you will experience unexpected placement behavior
 * when using the movable modifier. In general, the offset modifier should be specified before the
 * size modifier, and the movable modifier should be specified last.
 *
 * @param enabled - true if this composable should be movable.
 * @param stickyPose - if enabled, the user specified position will be retained when the modifier is
 *   disabled or removed.
 * @param scaleWithDistance - true if this composable should scale in size when moved in depth. When
 *   this scaleWithDistance is enabled, the subspace element moved will grow or shrink. It will also
 *   maintain any explicit scale that it had before movement.
 * @param onPoseChange - a callback to process the pose change during movement, with translation in
 *   pixels. This will only be called if [enabled] is true. If the callback returns false the
 *   default behavior of moving this composable's subspace hierarchy will be executed. If it returns
 *   true, it is the responsibility of the callback to process the event. [PoseChangeEvent.pose]
 *   represents the new pose of the composable in the subspace with respect to its parent, with its
 *   translation being expressed in pixels.[PoseChangeEvent.scale] is how large the panel should be
 *   scaled as a result of its motion. This value will change with the panel's depth when
 *   'scaleWithDistance' is set, otherwise it will be locked in at 1.0 or at the value specified by
 *   the scale modifier. [PoseChangeEvent.size] is the IntVolumeSize value that will include the
 *   width, height and depth of the composable; that factors in shrinking or stretching due to
 *   [PoseChangeEvent.scale]
 */
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    stickyPose: Boolean = false,
    scaleWithDistance: Boolean = true,
    onPoseChange: (PoseChangeEvent) -> Boolean = { false },
): SubspaceModifier =
    this.then(MovableElement(enabled, onPoseChange, stickyPose, scaleWithDistance))

public class PoseChangeEvent(
    public var pose: Pose = Pose.Identity,
    public var scale: Float = 1.0F,
    public var size: IntVolumeSize = IntVolumeSize.Zero,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoseChangeEvent) return false

        if (pose != other.pose) return false
        if (scale != other.scale) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pose.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    override fun toString(): String {
        return "PoseChangeEvent(pose=$pose, scale=$scale, size=$size)"
    }
}

private class MovableElement(
    private val enabled: Boolean,
    private val onPoseChange: (PoseChangeEvent) -> Boolean,
    private val stickyPose: Boolean,
    private val scaleWithDistance: Boolean,
) : SubspaceModifierElement<MovableNode>() {

    override fun create(): MovableNode =
        MovableNode(
            enabled = enabled,
            stickyPose = stickyPose,
            onPoseChange = onPoseChange,
            scaleWithDistance = scaleWithDistance,
        )

    override fun update(node: MovableNode) {
        node.enabled = enabled
        node.onPoseChange = onPoseChange
        node.stickyPose = stickyPose
        node.scaleWithDistance = scaleWithDistance
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MovableElement) return false

        if (enabled != other.enabled) return false
        if (onPoseChange !== other.onPoseChange) return false
        if (stickyPose != other.stickyPose) return false
        if (scaleWithDistance != other.scaleWithDistance) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onPoseChange.hashCode()
        result = 31 * result + stickyPose.hashCode()
        result = 31 * result + scaleWithDistance.hashCode()
        return result
    }
}

internal class MovableNode(
    public var enabled: Boolean,
    public var stickyPose: Boolean,
    public var scaleWithDistance: Boolean,
    public var onPoseChange: (PoseChangeEvent) -> Boolean,
) : SubspaceModifier.Node(), CoreEntityNode {
    override fun modifyCoreEntity(coreEntity: CoreEntity) {
        coreEntity.movable?.updateState(this)
    }
}
