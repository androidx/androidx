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
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.MeshFeature

/** Test-only implementation of [androidx.xr.scenecore.runtime.MeshFeature] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeMeshFeature(
    nodeHolder: NodeHolder<*>,
    override val meshBoundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One),
) : FakeBaseRenderingFeature(nodeHolder), MeshFeature {

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        // Test stub.
    }

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        // no-op
    }

    override fun setReformAffordanceEnabled(
        entity: androidx.xr.scenecore.runtime.MeshEntity,
        enabled: Boolean,
        executor: java.util.concurrent.Executor,
        systemMovable: Boolean,
    ) {
        // Test stub.
    }
}
