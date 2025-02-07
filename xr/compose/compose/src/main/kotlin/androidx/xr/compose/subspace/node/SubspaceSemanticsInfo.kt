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

public interface SubspaceSemanticsInfo {

    /** The unique ID of this semantics node. */
    public val semanticsId: Int

    /** The size of the bounding box for this node. */
    public val size: IntVolumeSize

    /** The pose of this node relative to its parent layout node in the Compose hierarchy. */
    public val pose: Pose

    /** The position of this node relative to the root of this Compose hierarchy, in pixels. */
    public val poseInRoot: Pose

    /**
     * The semantics configuration of this node.
     *
     * This includes all properties attached as modifiers to the current layout node.
     */
    public val config: SemanticsConfiguration

    /**
     * The children of this node in the semantics tree.
     *
     * The children are ordered in inverse hit test order (i.e., paint order).
     */
    public val semanticsChildren: List<SubspaceSemanticsInfo>

    /** The parent of this node in the semantics tree. */
    public val semanticsParent: SubspaceSemanticsInfo?

    /** Whether this node is the root of a semantics tree. */
    public val isRoot: Boolean
        get() = semanticsParent == null

    /** The [Entity] associated with this node. */
    public val semanticsEntity: Entity?

    /** The scale factor of this node relative to its parent. */
    public val scale: Float
        get() = semanticsEntity?.getScale() ?: 1f

    /** The components attached to this node by SubspaceLayoutNode update. */
    public val components: List<Component>?
        @Suppress("NullableCollection") get() = semanticsEntity?.getComponents()
}
