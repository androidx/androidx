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
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.MeshEntity as RtMeshEntity

/**
 * A renderable entity in the scene graph that renders a [CustomMesh].
 *
 * A `MeshEntity` renders a `CustomMesh` using a list of [Materials][Material]. The number of
 * materials must match the number of subsets in the `CustomMesh`.
 *
 * @property mesh The [CustomMesh] resource rendered by this entity.
 * @property boneCount The number of bones used to animate the mesh. If 0, skinning is disabled.
 */
@ExperimentalCustomMeshApi
public class MeshEntity
private constructor(
    rtEntity: RtMeshEntity,
    entityRegistry: EntityRegistry,
    public val mesh: CustomMesh,
    private val _materials: MutableList<Material>,
    public val boneCount: Int,
) : BaseEntity<RtMeshEntity>(rtEntity, entityRegistry) {

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
        rtEntity!!.setMaterial(material.material, subsetIndex)
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
         * @param boneCount The number of bones used to animate the mesh. If 0, skinning is
         *   disabled.
         * @param pose The initial pose of the entity relative to its parent. Defaults to
         *   `Pose.Identity`.
         * @param parent Parent entity. If `null`, the entity is created but not attached to the
         *   scene graph and will not be visible until a parent is set. The default value is
         *   [Scene]'s [ActivitySpace].
         * @return A new [MeshEntity].
         * @throws IllegalArgumentException if the number of materials does not match the number of
         *   mesh subsets, or if any material in the list is null.
         */
        @MainThread
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            mesh: CustomMesh,
            materials: List<Material>,
            boneCount: Int = 0,
            pose: Pose = Pose.Identity,
            parent: Entity? = session.scene.activitySpace,
        ): MeshEntity {
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

            val rtParent =
                if (parent != null && parent !is BaseEntity<*>) {
                    androidx.xr.runtime.XrLog.warn(
                        "The provided parent is not a BaseEntity. The MeshEntity will " +
                            "be created without a parent."
                    )
                    null
                } else {
                    (parent as? BaseEntity<*>)?.rtEntity
                }

            val rtEntity =
                renderingRuntime.createMeshEntity(
                    customMeshResource,
                    materialResources,
                    boneCount,
                    pose,
                    rtParent,
                )

            return MeshEntity(rtEntity, entityRegistry, mesh, materials.toMutableList(), boneCount)
        }
    }
}
