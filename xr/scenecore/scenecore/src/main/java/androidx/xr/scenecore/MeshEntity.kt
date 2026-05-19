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

import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.MeshEntity as RtMeshEntity

/**
 * A renderable entity in the scene graph that renders a [CustomMesh].
 *
 * A `MeshEntity` renders a `CustomMesh` using a list of [Materials][Material]. The number of
 * materials must match the number of subsets in the `CustomMesh`.
 *
 * @property mesh The [CustomMesh] resource rendered by this entity.
 * @property boneCount The number of bones used to animate the mesh. Must be between 0 and 255,
 *   inclusive. If 0, skinning is disabled. If non-zero, bone transforms can be set with
 *   [setBoneTransforms].
 */
public class MeshEntity
private constructor(
    rtMeshEntity: RtMeshEntity,
    entityRegistry: EntityRegistry,
    public val mesh: CustomMesh,
    private val _materials: MutableList<Material>,
    @IntRange(from = 0, to = 255) public val boneCount: Int,
) : Entity(rtMeshEntity, entityRegistry) {

    private val rtMeshEntity: RtMeshEntity
        get() = rtEntity as RtMeshEntity

    /** The list of materials used to render this entity's custom mesh. */
    public val materials: List<Material>
        get() = _materials.toList()

    /**
     * Sets a material for a mesh subset.
     *
     * @param material The new [Material] to apply to the mesh subset.
     * @param subsetIndex The zero-based index for the mesh subset. Default is the first subset of
     *   the mesh.
     * @throws IllegalArgumentException if `subsetIndex` is out of bounds for the number of subsets.
     */
    @MainThread
    @JvmOverloads
    public fun setMaterial(material: Material, subsetIndex: Int = 0) {
        checkNotDisposed()
        require(subsetIndex >= 0 && subsetIndex < _materials.size) {
            "Subset index $subsetIndex is out of bounds for the number of subsets (${_materials.size})."
        }
        _materials[subsetIndex] = material
        rtMeshEntity.setMaterial(material.material, subsetIndex)
    }

    /**
     * Sets the transforms for the bones in the skinned mesh.
     *
     * This function is used to animate meshes with skeletal animations (skinning). Each bone's
     * transform is represented by a [Matrix4] object. This can only be used if [boneCount] is
     * greater than zero.
     *
     * @param transforms A list of [Matrix4] objects representing the new bone transforms. The order
     *   in the list corresponds to the bone indices. The number of transforms can be less than
     *   [boneCount], in which case only the provided bones are updated. Any extra transforms beyond
     *   [boneCount] will be ignored.
     * @throws IllegalStateException if [boneCount] is zero.
     */
    @MainThread
    public fun setBoneTransforms(transforms: List<Matrix4>) {
        checkNotDisposed()
        check(boneCount > 0) {
            "MeshEntity must be created with a boneCount greater than 0 to set bone transforms."
        }
        rtMeshEntity.setBoneTransforms(transforms)
    }

    public companion object {
        /**
         * Creates a new [MeshEntity].
         *
         * @param session The session to use for creating the MeshEntity.
         * @param mesh The [CustomMesh] to render.
         * @param materials The list of [Materials][Material] to use for each subset of the mesh.
         *   The list must contain one material per mesh subset. Materials in the list must not be
         *   null.
         * @param boneCount The number of bones used to animate the mesh. Must be between 0 and 255,
         *   inclusive. If 0, skinning is disabled. If non-zero, bone transforms can be set with
         *   [setBoneTransforms].
         * @param pose The initial pose of the entity relative to its parent. Defaults to
         *   `Pose.Identity`.
         * @param parent Parent entity. Defaults to `null`. If `null`, the entity is created but not
         *   attached to the scene graph and will be invisible. When a parent entity (e.g.,
         *   [ActivitySpace] or any other [Entity] already present in the scene) is assigned later,
         *   the entity will remain invisible until you explicitly enable it by calling
         *   [Entity.setEnabled] (enabled=true). This allows for [Entity] pre-configuration before
         *   making it visible.
         * @return A new [MeshEntity].
         * @throws IllegalArgumentException if `boneCount` is not between 0 and 255, if the number
         *   of materials does not match the number of mesh subsets, or if any material in the list
         *   is null.
         */
        @MainThread
        @JvmOverloads
        @JvmStatic
        @Suppress("RestrictedApiAndroidX")
        public fun create(
            session: Session,
            mesh: CustomMesh,
            materials: List<Material>,
            @IntRange(from = 0, to = 255) boneCount: Int = 0,
            pose: Pose = Pose.Identity,
            parent: Entity? = null,
        ): MeshEntity {
            require(boneCount in 0..255) { "boneCount must be between 0 and 255, inclusive." }
            require(materials.size == mesh.subsets.size) {
                "The number of materials (${materials.size}) must match the number of mesh subsets (${mesh.subsets.size})."
            }
            require(!materials.contains(null as Material?)) {
                "Materials in the list must not be null."
            }
            val renderingRuntime = session.renderingRuntime
            val entityRegistry = session.scene.entityRegistry

            val materialResources = materials.map { it.material }
            val customMeshResource = mesh.getResource()

            val rtParent = parent?.rtEntity

            val rtEntity =
                renderingRuntime.createMeshEntity(
                    customMeshResource,
                    materialResources,
                    boneCount,
                    pose,
                    rtParent,
                )

            return MeshEntity(rtEntity, entityRegistry, mesh, materials.toMutableList(), boneCount)
                .also { it.parent = parent }
        }
    }
}
