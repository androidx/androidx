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

package androidx.xr.compose.subspace.node

import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Component
import androidx.xr.scenecore.Entity

/**
 * Provides semantic information about a node in the Subspace layout hierarchy.
 *
 * This interface defines the properties that are accessible for semantics-related purposes, such as
 * accessibility services and testing. Each `SubspaceSemanticsInfo` node represents a composable in
 * the UI tree that has semantics attached.
 */
public sealed interface SubspaceSemanticsInfo {

    /** The unique ID of this semantics node. */
    public val semanticsId: Int

    /** The dimensions of the 3D bounding box for this node. */
    public val size: IntVolumeSize

    /** The pose of this node relative to its parent layout node in the Compose hierarchy. */
    public val pose: Pose

    /** The pose of this node relative to the root of this Compose hierarchy, in pixels. */
    public val poseInRoot: Pose

    /**
     * The semantics configuration of this node.
     *
     * This includes all properties attached as modifiers to the current layout node.
     */
    public val semanticsConfiguration: SemanticsConfiguration

    /** The children of this node in the semantics tree. */
    public val childrenInfo: List<SubspaceSemanticsInfo>

    /** The parent of this node in the semantics tree. */
    public val parentInfo: SubspaceSemanticsInfo?

    /** Whether this node is the root of a semantics tree. */
    public val isRoot: Boolean
        get() = parentInfo == null

    /** The [Entity] associated with this node. */
    public val semanticsEntity: Entity?

    /** The scale factor of this node relative to its parent. */
    public val scale: Float
        get() = semanticsEntity?.getScale() ?: 1f

    /**
     * The list of components attached to this node by the Subspace layout update phase.
     *
     * Returns a collection of [Component] objects representing various capabilities or properties
     * (e.g., visual, interactive, or physical behaviors) attached to the [semanticsEntity]. Returns
     * `null` if there is no underlying `Entity`.
     */
    public val components: List<Component>?
        @Suppress("NullableCollection") get() = semanticsEntity?.getComponents()
}
