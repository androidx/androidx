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

import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/**
 * A holder of the measured bounds.
 *
 * Based on [androidx.compose.ui.layout.LayoutCoordinates].
 */
public interface SubspaceLayoutCoordinates {
    /** The pose of this layout in the local coordinates space, with translation in pixels. */
    public val pose: Pose

    /**
     * The pose of this layout relative to the root entity of the Compose hierarchy, with
     * translation in pixels.
     */
    public val poseInRoot: Pose

    /**
     * The pose of this layout relative to its parent entity in the Compose hierarchy, with
     * translation in pixels.
     */
    public val poseInParentEntity: Pose

    /** The position of the pose. */
    @Deprecated("Use pose.position instead", ReplaceWith("pose.position"))
    public val position: Vector3
        get() = pose.translation

    /**
     * The position of this layout relative to the root entity of the Compose hierarchy.
     *
     * For testing only.
     */
    @Deprecated("Use poseInRoot.position instead", ReplaceWith("poseInRoot.position"))
    public val positionInRoot: Vector3
        get() = poseInRoot.translation

    /** The position of this layout relative to its parent entity in the Compose hierarchy. */
    @Deprecated(
        "Use poseInParentEntity.position instead",
        ReplaceWith("poseInParentEntity.position")
    )
    public val positionInParentEntity: Vector3
        get() = poseInParentEntity.translation

    /** The rotation of the pose. */
    @Deprecated("Use pose.rotation instead", ReplaceWith("pose.rotation"))
    public val rotation: Quaternion
        get() = pose.rotation

    /**
     * The rotation of this layout relative to the root entity of the Compose hierarchy.
     *
     * For testing only.
     */
    @Deprecated("Use poseInRoot.rotation instead", ReplaceWith("poseInRoot.rotation"))
    public val rotationInRoot: Quaternion
        get() = poseInRoot.rotation

    /** The rotation of this layout relative to its parent entity in the Compose hierarchy. */
    @Deprecated(
        "Use poseInParentEntity.rotation instead",
        ReplaceWith("poseInParentEntity.rotation")
    )
    public val rotationInParentEntity: Quaternion
        get() = poseInParentEntity.rotation

    /**
     * The size of this layout in the local coordinates space.
     *
     * This is also useful for providing the size of the node to the [OnGloballyPositionedModifier].
     */
    public val size: IntVolumeSize
}

/** Returns information on pose, position and size. */
public fun SubspaceLayoutCoordinates.toDebugString(): String = buildString {
    appendLine("pose: $pose")
    appendLine("poseInParentEntity: $poseInParentEntity")
    appendLine("size: $size")
}
