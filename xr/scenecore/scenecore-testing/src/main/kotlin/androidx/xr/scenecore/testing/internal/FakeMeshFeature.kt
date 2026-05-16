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

package androidx.xr.scenecore.testing.internal

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.MeshFeature
import androidx.xr.scenecore.runtime.NodeHolder
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MeshFeature] */
internal open class FakeMeshFeature(
    nodeHolder: NodeHolder<*>,
    override val meshBoundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One),
    /**
     * The list of materials applied to the mesh subsets. This can be inspected or modified in
     * tests.
     */
    internal val materials: MutableList<MaterialResource> = mutableListOf(),
) : FakeBaseRenderingFeature(nodeHolder), MeshFeature {

    /**
     * The number of bones in the skinned mesh. This can be set in tests to control the behavior of
     * [setBoneTransforms].
     */
    internal var boneCount: Int = 0

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        if (subsetIndex < materials.size) {
            materials[subsetIndex] = material
        }
    }

    private val _boneTransforms: MutableList<Matrix4> = mutableListOf()

    /**
     * The list of transforms applied to the bones in the skinned mesh. This can be inspected in
     * tests.
     */
    internal val boneTransforms: List<Matrix4>
        get() = _boneTransforms

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        _boneTransforms.clear()
        for (i in transforms.indices) {
            if (i < boneCount) {
                _boneTransforms.add(transforms[i])
            }
        }
    }

    /**
     * The [MeshEntity] that this feature is associated with, captured from
     * [setReformAffordanceEnabled].
     */
    internal var meshEntity: MeshEntity? = null
        private set

    /** Whether the reform affordance is enabled, captured from [setReformAffordanceEnabled]. */
    internal var reformAffordanceEnabled = false
        private set

    /** The [Executor] used for reform affordances, captured from [setReformAffordanceEnabled]. */
    internal var executor: Executor? = null
        private set

    /** Whether the entity is system movable, captured from [setReformAffordanceEnabled]. */
    internal var systemMovable = false
        private set

    override fun setReformAffordanceEnabled(
        entity: MeshEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    ) {
        meshEntity = entity
        reformAffordanceEnabled = enabled
        this.executor = executor
        this.systemMovable = systemMovable
    }

    override fun setColliderEnabled(enabled: Boolean) {
        // Test stub.
    }
}
