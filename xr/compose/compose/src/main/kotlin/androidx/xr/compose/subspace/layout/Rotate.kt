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

import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/**
 * Rotate a subspace element (i.e. Panel) in space. Parameter rotation angles are specified in
 * degrees. The rotations are applied with the order pitch, then yaw, then roll.
 *
 * @param pitch Rotation around the x-axis. The x-axis is the axis width is measured on.
 * @param yaw Rotation around the y-axis. The y-axis is the axis height is measured on.
 * @param roll Rotation around the z-axis. The z-axis is the axis depth is measured on.
 */
public fun SubspaceModifier.rotate(pitch: Float, yaw: Float, roll: Float): SubspaceModifier =
    this.then(RotationElement(pitch, yaw, roll))

/**
 * Rotate a subspace element (i.e. Panel) in space.
 *
 * @param axisAngle Vector representing the axis of rotation.
 * @param rotation Degrees of rotation.
 */
public fun SubspaceModifier.rotate(axisAngle: Vector3, rotation: Float): SubspaceModifier =
    this.then(RotationElement(axisAngle, rotation))

/**
 * Rotate a subspace element (i.e. Panel) in space.
 *
 * @param quaternion Quaternion describing the rotation.
 */
public fun SubspaceModifier.rotate(quaternion: Quaternion): SubspaceModifier =
    this.then(RotationElement(quaternion))

private class RotationElement(private val quaternion: Quaternion) :
    SubspaceModifierElement<RotationNode>() {

    public constructor(
        pitch: Float,
        yaw: Float,
        roll: Float,
    ) : this(Quaternion.fromEulerAngles(pitch, yaw, roll))

    public constructor(
        axisAngle: Vector3,
        rotation: Float,
    ) : this(Quaternion.fromAxisAngle(axisAngle, rotation))

    override fun create(): RotationNode = RotationNode(quaternion)

    override fun hashCode(): Int {
        return quaternion.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RotationElement) return false

        if (quaternion != other.quaternion) return false

        return true
    }

    override fun update(node: RotationNode) {
        node.quaternion = quaternion
    }
}

internal class RotationNode(public var quaternion: Quaternion) :
    SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: VolumeConstraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(Pose(translation = Vector3.Zero, rotation = quaternion))
        }
    }
}
