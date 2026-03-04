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

package androidx.xr.scenecore.runtime

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/** Provide the rendering implementation for [androidx.xr.scenecore.GltfModelNode] */
// TODO(b/481429599): Audit usage of LIBRARY_GROUP_PREFIX in SceneCore and migrate it over to
// LIBRARY_GROUP.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfModelNodeFeature {

    /**
     * The name of the node as defined in the glTF file. A null name is treated as an empty string.
     */
    public val name: String?

    /** The local pose (position and rotation) of the node relative to its immediate parent. */
    @get:MainThread @set:MainThread public var localPose: Pose

    /** The local scale of the node relative to its immediate parent. */
    @get:MainThread @set:MainThread public var localScale: Vector3

    /** The pose (position and rotation) of the node relative to the model's root node. */
    @get:MainThread @set:MainThread public var modelPose: Pose

    /** The scale of the node relative to the model's root node. */
    @get:MainThread @set:MainThread public var modelScale: Vector3

    /**
     * Sets a material override for a specific primitive of this node.
     *
     * @param material The material to use for the mesh.
     * @param primitiveIndex The zero-based index of the primitive of the mesh associated with the
     *   node.
     */
    @MainThread public fun setMaterialOverride(material: MaterialResource, primitiveIndex: Int)

    /**
     * Clears a material override for a specific primitive of this node.
     *
     * @param primitiveIndex The zero-based index of the primitive of the mesh associated with the
     *   node.
     */
    @MainThread public fun clearMaterialOverride(primitiveIndex: Int)

    /** Clears a material override for a all primitive indices of this node. */
    @MainThread public fun clearMaterialOverrides()
}
