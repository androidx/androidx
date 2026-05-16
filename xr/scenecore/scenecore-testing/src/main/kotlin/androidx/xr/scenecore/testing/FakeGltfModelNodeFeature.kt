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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.testing.internal.FakeGltfModelNodeFeature as InternalFakeGltfModelNodeFeature

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfModelNodeFeature] */
// TODO(b/481429599): Audit usage of LIBRARY_GROUP_PREFIX in SceneCore and migrate it over to
// LIBRARY_GROUP.
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeGltfModelNodeFeature
internal constructor(
    override val name: String? = "test_node",
    internal val fakeInternal: InternalFakeGltfModelNodeFeature,
) : GltfModelNodeFeature {

    public constructor(
        name: String? = "test_node"
    ) : this(name, InternalFakeGltfModelNodeFeature(name))

    override var localPose: Pose
        get() = fakeInternal.localPose
        set(value) {
            fakeInternal.localPose = value
        }

    override var localScale: Vector3
        get() = fakeInternal.localScale
        set(value) {
            fakeInternal.localScale = value
        }

    override var modelPose: Pose
        get() = fakeInternal.modelPose
        set(value) {
            fakeInternal.modelPose = value
        }

    override var modelScale: Vector3
        get() = fakeInternal.modelScale
        set(value) {
            fakeInternal.modelScale = value
        }

    public val materialOverrides: MutableMap<Int, MaterialResource>
        get() = fakeInternal.materialOverrides

    override fun setMaterialOverride(material: MaterialResource, primitiveIndex: Int) {
        fakeInternal.setMaterialOverride(material, primitiveIndex)
    }

    override fun clearMaterialOverride(primitiveIndex: Int) {
        fakeInternal.clearMaterialOverride(primitiveIndex)
    }

    override fun clearMaterialOverrides() {
        fakeInternal.clearMaterialOverrides()
    }
}
