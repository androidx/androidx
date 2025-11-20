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

/**
 * A holder of the measured bounds.
 *
 * Based on [androidx.compose.ui.layout.LayoutCoordinates].
 */
public interface SubspaceLayoutCoordinates {
    /** The pose of this layout in the local coordinates space, with translation in pixels. */
    public val pose: Pose

    /**
     * The pose of this layout relative to its parent entity in the Compose hierarchy, with
     * translation in pixels.
     */
    public val poseInParentEntity: Pose

    /**
     * The pose of this layout relative to the root entity of the Compose for XR's hierarchy with
     * translation values in pixels.
     */
    public val poseInRoot: Pose

    /**
     * The coordinates of the immediate parent in the layout hierarchy.
     *
     * For a layout, this is its parent layout. For a modifier, this is the modifier that preceded
     * it, or the layout it is attached to if it is the first in the chain.
     *
     * Returns `null` only for the root of the hierarchy.
     */
    public val parentCoordinates: SubspaceLayoutCoordinates?

    /**
     * The coordinates of the nearest parent layout, skipping any intermediate modifiers.
     *
     * This is useful for positioning relative to the containing layout composable, irrespective of
     * any modifiers applied to it.
     *
     * Returns `null` only for the root of the hierarchy.
     */
    public val parentLayoutCoordinates: SubspaceLayoutCoordinates?

    /**
     * The size of this layout in the local coordinates space.
     *
     * This is also useful for providing the size of the node to the
     * [OnGloballyPositionedModifier][androidx.xr.compose.subspace.layout.OnGloballyPositionedNode].
     */
    public val size: IntVolumeSize
}

/** Returns information on pose, position and size. */
internal fun SubspaceLayoutCoordinates.toDebugString(): String = buildString {
    appendLine("pose: $pose")
    appendLine("poseInParentEntity: $poseInParentEntity")
    appendLine("size: $size")
}
