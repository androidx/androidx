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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.MeshFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.testing.FakeRenderingRuntime.FakeKhronosPbrMaterial
import androidx.xr.scenecore.testing.FakeRenderingRuntime.FakeWaterMaterial
import androidx.xr.scenecore.testing.internal.FakeMeshEntity as InternalFakeMeshEntity
import androidx.xr.scenecore.testing.internal.FakeMeshFeature as InternalFakeMeshFeature
import androidx.xr.scenecore.testing.internal.FakeRenderingRuntime.FakeKhronosPbrMaterial as InternalFakeKhronosPbrMaterial
import androidx.xr.scenecore.testing.internal.FakeRenderingRuntime.FakeWaterMaterial as InternalFakeWaterMaterial
import androidx.xr.scenecore.testing.internal.FakeResource as InternalFakeResource
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MeshFeature] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeMeshFeature
internal constructor(
    nodeHolder: NodeHolder<*>,
    override val meshBoundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One),
    initialMaterials: List<MaterialResource> = emptyList(),
    internal var fakeInternal: InternalFakeMeshFeature,
) : FakeBaseRenderingFeature(nodeHolder), MeshFeature {

    public constructor(
        nodeHolder: NodeHolder<*>,
        meshBoundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One),
        initialMaterials: List<MaterialResource> = emptyList(),
    ) : this(
        nodeHolder,
        meshBoundingBox,
        initialMaterials,
        InternalFakeMeshFeature(nodeHolder, meshBoundingBox),
    )

    internal val materialWrappers = mutableMapOf<Any, MaterialResource>()

    init {
        initialMaterials.forEach { material ->
            val internalMaterial =
                when (material) {
                    is FakeResource -> material.fakeInternal
                    is FakeWaterMaterial -> material.fakeInternal
                    is FakeKhronosPbrMaterial -> material.fakeInternal
                    else -> throw IllegalArgumentException("Unsupported material type")
                }

            fakeInternal.materials.add(internalMaterial)

            materialWrappers[internalMaterial] = material
        }
    }

    /**
     * The list of materials applied to the mesh subsets. This can be inspected or modified in
     * tests.
     */
    public val materials: List<MaterialResource>
        get() =
            fakeInternal.materials.map { internalMaterial ->
                materialWrappers.getOrPut(internalMaterial) {
                    when (internalMaterial) {
                        is InternalFakeResource -> FakeResource(internalMaterial.token)
                        is InternalFakeWaterMaterial ->
                            FakeWaterMaterial(internalMaterial.isAlphaMapVersion)
                        is InternalFakeKhronosPbrMaterial ->
                            FakeKhronosPbrMaterial(internalMaterial.spec)
                        else ->
                            throw IllegalArgumentException(
                                "Unknown internal material type: $internalMaterial"
                            )
                    }
                }
            }

    /**
     * The number of bones in the skinned mesh. This can be set in tests to control the behavior of
     * [setBoneTransforms].
     */
    public var boneCount: Int
        get() = fakeInternal.boneCount
        set(value) {
            fakeInternal.boneCount = value
        }

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        if (subsetIndex < materials.size) {
            val internalMaterial =
                when (material) {
                    is FakeResource -> material.fakeInternal
                    is FakeWaterMaterial -> material.fakeInternal
                    is FakeKhronosPbrMaterial -> material.fakeInternal
                    else -> throw IllegalArgumentException("Unsupported material type")
                }

            materialWrappers[internalMaterial] = material

            fakeInternal.setMaterial(internalMaterial, subsetIndex)
        }
    }

    /**
     * The list of transforms applied to the bones in the skinned mesh. This can be inspected in
     * tests.
     */
    public val boneTransforms: List<Matrix4>
        get() = fakeInternal.boneTransforms

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        fakeInternal.setBoneTransforms(transforms)
    }

    internal val meshEntityWrapper: MutableMap<InternalFakeMeshEntity, FakeMeshEntity> =
        mutableMapOf()

    /**
     * The [MeshEntity] that this feature is associated with, captured from
     * [setReformAffordanceEnabled].
     */
    public val meshEntity: MeshEntity?
        get() = fakeInternal.meshEntity?.let { meshEntityWrapper[it] }

    /** Whether the reform affordance is enabled, captured from [setReformAffordanceEnabled]. */
    public val reformAffordanceEnabled: Boolean
        get() = fakeInternal.reformAffordanceEnabled

    /** The [Executor] used for reform affordances, captured from [setReformAffordanceEnabled]. */
    public val executor: Executor?
        get() = fakeInternal.executor

    /** Whether the entity is system movable, captured from [setReformAffordanceEnabled]. */
    public val systemMovable: Boolean
        get() = fakeInternal.systemMovable

    override fun setReformAffordanceEnabled(
        entity: MeshEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    ) {
        val internalEntity = (entity as FakeMeshEntity).fakeInternal as InternalFakeMeshEntity
        meshEntityWrapper[internalEntity] = entity
        fakeInternal.setReformAffordanceEnabled(internalEntity, enabled, executor, systemMovable)
    }

    override fun setColliderEnabled(enabled: Boolean) {
        // Test stub.
    }
}
