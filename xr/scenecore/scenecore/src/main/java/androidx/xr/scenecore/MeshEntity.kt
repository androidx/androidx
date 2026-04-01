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
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.MeshEntity as RtMeshEntity

/**
 * A renderable entity in the scene graph that renders a CustomMesh.
 *
 * A `MeshEntity` renders a [CustomMesh] using a list of [Material]s. The number of materials must
 * match the number of subsets in the CustomMesh.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeshEntity
private constructor(
    rtEntity: RtMeshEntity,
    entityRegistry: EntityRegistry,
    public val mesh: CustomMesh,
    private val _materials: MutableList<Material>,
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
     */
    @MainThread
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
         * @param materials The list of [Material]s to use for each subset of the mesh. The list
         *   must contain one material per mesh subset.
         * @param boneCount the number of bones. If zero, skinning will be disabled.
         * @param pose The initial pose of the entity relative to its parent. Defaults to
         *   `Pose.Identity`.
         * @return A new [MeshEntity].
         */
        @MainThread
        public fun create(
            session: Session,
            mesh: CustomMesh,
            materials: List<Material>,
            boneCount: Int = 0,
            pose: Pose = Pose.Identity,
        ): MeshEntity {
            val renderingRuntime = session.renderingRuntime
            val entityRegistry = session.scene.entityRegistry
            val scene = session.scene

            val materialResources = materials.map { it.material }
            val customMeshResource = mesh.getResource()

            // Default parent to activitySpace if available
            val parentEntity = scene.activitySpace

            val rtEntity =
                renderingRuntime.createMeshEntity(
                    customMeshResource,
                    materialResources,
                    boneCount,
                    pose,
                    parentEntity.rtEntity,
                )

            return MeshEntity(rtEntity, entityRegistry, mesh, materials.toMutableList())
        }
    }
}
