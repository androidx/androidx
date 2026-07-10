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
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.testing.internal.FakeMeshEntity as InternalFakeMeshEntity
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MeshEntity] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeMeshEntity
internal constructor(
    private val feature: FakeMeshFeature,
    private val executor: Executor? = null,
    fakeInternal: InternalFakeMeshEntity,
) : FakeEntity(fakeInternal = fakeInternal), MeshEntity {

    public constructor(
        feature: FakeMeshFeature,
        executor: Executor? = null,
    ) : this(feature, executor, InternalFakeMeshEntity(feature.fakeInternal, executor))

    override val meshBoundingBox: BoundingBox
        get() = feature.meshBoundingBox

    /**
     * The list of materials applied to the mesh subsets. This can be inspected or modified in
     * tests.
     */
    public val materials: List<MaterialResource>
        get() = feature.materials

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        feature.setMaterial(material, subsetIndex)
    }

    /**
     * The list of transforms applied to the bones in the skinned mesh. This can be inspected in
     * tests.
     */
    public val boneTransforms: List<Matrix4>
        get() = feature.boneTransforms

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        feature.setBoneTransforms(transforms)
    }

    /** Whether the reform affordance is enabled, captured from [setReformAffordanceEnabled]. */
    public val reformAffordanceEnabled: Boolean
        get() = feature.reformAffordanceEnabled

    /** Whether the entity is system movable, captured from [setReformAffordanceEnabled]. */
    public val systemMovable: Boolean
        get() = feature.systemMovable

    override fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean) {
        if (executor != null) {
            feature.setReformAffordanceEnabled(this, enabled, executor, systemMovable)
        }
    }

    override fun setColliderEnabled(enabled: Boolean) {
        feature?.setColliderEnabled(enabled)
    }
}
