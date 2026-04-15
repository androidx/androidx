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
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MeshEntity] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeMeshEntity(
    private val feature: MeshFeature? = null,
    private val executor: Executor? = null,
) : FakeEntity(), MeshEntity {
    override val meshBoundingBox: BoundingBox =
        feature?.meshBoundingBox ?: BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        feature?.setMaterial(material, subsetIndex)
    }

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        feature?.setBoneTransforms(transforms)
    }

    override fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean) {
        if (executor != null) {
            feature?.setReformAffordanceEnabled(this, enabled, executor, systemMovable)
        }
    }
}
