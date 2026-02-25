/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.MainThread
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfModelNodeFeature as RtGltfModelNode

/**
 * [GltfModelNode] represents a node in a [GltfModelEntity].
 *
 * The lifecycle of a glTF node is tied to the glTF model itself. Trying to use the [GltfModelNode]
 * after the [GltfModelEntity] is disposed will throw an [IllegalStateException].
 */
public class GltfModelNode
internal constructor(
    private val modelEntity: GltfModelEntity,
    private val rtFeature: RtGltfModelNode,
    /** The index of this node in the flattened list of child nodes of the glTF model. */
    public val index: Int,
    /** The name of the node as defined in the glTF file. Returns `null` if the node has no name. */
    public val name: String?,
) {
    /** The [Pose] of this node relative to its immediate parent [GltfModelNode]. */
    @get:MainThread
    @set:MainThread
    public var localPose: Pose
        get() {
            modelEntity.checkNotDisposed()
            return rtFeature.localPose
        }
        set(value) {
            modelEntity.checkNotDisposed()
            rtFeature.localPose = value
        }

    /** The scale of this node relative to its immediate parent [GltfModelNode]. */
    @get:MainThread
    @set:MainThread
    public var localScale: Vector3
        get() {
            modelEntity.checkNotDisposed()
            return rtFeature.localScale
        }
        set(value) {
            modelEntity.checkNotDisposed()
            rtFeature.localScale = value
        }

    /** The [Pose] of this node relative to the [GltfModelEntity] root. */
    @get:MainThread
    @set:MainThread
    public var modelPose: Pose
        get() {
            modelEntity.checkNotDisposed()
            return rtFeature.modelPose
        }
        set(value) {
            modelEntity.checkNotDisposed()
            rtFeature.modelPose = value
        }

    /** The scale of this node relative to the [GltfModelEntity] root. */
    @get:MainThread
    @set:MainThread
    public var modelScale: Vector3
        get() {
            modelEntity.checkNotDisposed()
            return rtFeature.modelScale
        }
        set(value) {
            modelEntity.checkNotDisposed()
            rtFeature.modelScale = value
        }

    /**
     * Sets a material override for a primitive of a node within the glTF graph.
     *
     * The override is then applied to a primitive of that node's mesh at the specified
     * [primitiveIndex].
     *
     * @param material The new [Material] to apply to the primitive.
     * @param primitiveIndex The zero-based index for the primitive of the specified node, as
     *   defined in the glTF graph. Default is the first primitive of that node.
     * @throws IllegalArgumentException if the provided [Material] is invalid.
     * @throws IllegalStateException if the node does not have a mesh.
     * @throws IndexOutOfBoundsException if the [primitiveIndex] is out of bounds.
     * @see clearMaterialOverride
     */
    @JvmOverloads
    @MainThread
    public fun setMaterialOverride(material: Material, primitiveIndex: Int = 0) {
        modelEntity.checkNotDisposed()
        rtFeature.setMaterialOverride(material.material, primitiveIndex)
    }

    /**
     * Clears a previously set material override for a specific primitive of a node's mesh within
     * the glTF graph.
     *
     * If no override was previously set for that primitive, this call has no effect.
     *
     * @param primitiveIndex The zero-based index for the primitive of the specified node, as
     *   defined in the glTF graph. Default is the first primitive of that node.
     * @throws IllegalStateException if the node does not have a mesh.
     * @throws IndexOutOfBoundsException if the [primitiveIndex] is out of bounds.
     * @see setMaterialOverride
     */
    @JvmOverloads
    @MainThread
    public fun clearMaterialOverride(primitiveIndex: Int = 0) {
        modelEntity.checkNotDisposed()
        rtFeature.clearMaterialOverride(primitiveIndex)
    }
}
