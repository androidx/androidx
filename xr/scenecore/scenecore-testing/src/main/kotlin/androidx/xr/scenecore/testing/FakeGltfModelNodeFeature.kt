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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.MaterialResource

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfModelNodeFeature] */
// TODO(b/481429599): Audit usage of LIBRARY_GROUP_PREFIX in SceneCore and migrate it over to
// LIBRARY_GROUP.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeGltfModelNodeFeature(override val name: String? = "test_node") :
    GltfModelNodeFeature {

    override var localPose: Pose = Pose.Identity
    override var localScale: Vector3 = Vector3(1f, 1f, 1f)
    override var modelPose: Pose = Pose.Identity
    override var modelScale: Vector3 = Vector3(1f, 1f, 1f)

    public val materialOverrides: MutableMap<Int, MaterialResource> = mutableMapOf()

    override fun setMaterialOverride(material: MaterialResource, primitiveIndex: Int) {
        materialOverrides[primitiveIndex] = material
    }

    override fun clearMaterialOverride(primitiveIndex: Int) {
        materialOverrides.remove(primitiveIndex)
    }

    override fun clearMaterialOverrides() {
        materialOverrides.clear()
    }
}
