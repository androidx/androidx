/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.spatial.rendering

import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.MaterialResource

internal class GltfModelNodeFeatureImpl(
    private val impressApi: ImpressApi,
    private val impressNode: ImpressNode,
    private val modelRootNode: ImpressNode,
    name: String?,
) : GltfModelNodeFeature {

    override val name: String? = name?.ifEmpty { null }

    private val activeMaterialOverrides = mutableSetOf<Int>()

    override var localPose: Pose
        get() = impressApi.getImpressNodeLocalTransform(impressNode).pose
        set(value) {
            val currentScale = impressApi.getImpressNodeLocalTransform(impressNode).scale
            val newMatrix = Matrix4.fromTrs(value.translation, value.rotation, currentScale)
            impressApi.setImpressNodeLocalTransform(impressNode, newMatrix)
            impressApi.scheduleGltfReskinning(modelRootNode)
        }

    override var localScale: Vector3
        get() = impressApi.getImpressNodeLocalTransform(impressNode).scale
        set(value) {
            val currentPose = impressApi.getImpressNodeLocalTransform(impressNode).pose
            val newMatrix = Matrix4.fromTrs(currentPose.translation, currentPose.rotation, value)
            impressApi.setImpressNodeLocalTransform(impressNode, newMatrix)
            impressApi.scheduleGltfReskinning(modelRootNode)
        }

    override var modelPose: Pose
        get() = impressApi.getImpressNodeRelativeTransform(impressNode, modelRootNode).pose
        set(value) {
            val currentScale =
                impressApi.getImpressNodeRelativeTransform(impressNode, modelRootNode).scale
            val newMatrix = Matrix4.fromTrs(value.translation, value.rotation, currentScale)
            impressApi.setImpressNodeRelativeTransform(impressNode, modelRootNode, newMatrix)
            impressApi.scheduleGltfReskinning(modelRootNode)
        }

    override var modelScale: Vector3
        get() = impressApi.getImpressNodeRelativeTransform(impressNode, modelRootNode).scale
        set(value) {
            val currentPose =
                impressApi.getImpressNodeRelativeTransform(impressNode, modelRootNode).pose
            val newMatrix = Matrix4.fromTrs(currentPose.translation, currentPose.rotation, value)
            impressApi.setImpressNodeRelativeTransform(impressNode, modelRootNode, newMatrix)
            impressApi.scheduleGltfReskinning(modelRootNode)
        }

    override fun setMaterialOverride(material: MaterialResource, primitiveIndex: Int) {
        val nativeMaterial = (material as Material).nativeHandle
        impressApi.setGltfModelNodeMaterialOverride(impressNode, nativeMaterial, primitiveIndex)
        activeMaterialOverrides.add(primitiveIndex)
    }

    override fun clearMaterialOverride(primitiveIndex: Int) {
        impressApi.clearGltfModelNodeMaterialOverride(impressNode, primitiveIndex)
        activeMaterialOverrides.remove(primitiveIndex)
    }

    override fun clearMaterialOverrides() {
        for (index in activeMaterialOverrides) {
            impressApi.clearGltfModelNodeMaterialOverride(impressNode, index)
        }
        activeMaterialOverrides.clear()
    }
}
