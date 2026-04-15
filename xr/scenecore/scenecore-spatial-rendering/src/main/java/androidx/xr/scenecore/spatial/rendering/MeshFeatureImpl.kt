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

package androidx.xr.scenecore.spatial.rendering

import androidx.annotation.MainThread
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.MeshFeature
import androidx.xr.scenecore.spatial.core.AndroidXrEntity
import com.android.extensions.xr.XrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import java.util.concurrent.Executor

internal class MeshFeatureImpl(
    impressApi: ImpressApi,
    splitEngineSubspaceManager: SplitEngineSubspaceManager,
    extensions: XrExtensions,
    private val impressNode: ImpressNode,
    override val meshBoundingBox: BoundingBox,
) : BaseRenderingFeature(impressApi, splitEngineSubspaceManager, extensions), MeshFeature {
    init {
        bindImpressNodeToSubspace("mesh_entity_subspace_", impressNode)
    }

    override fun setMaterial(material: MaterialResource, subsetIndex: Int) {
        impressApi.setCustomMeshNodeMaterial(
            impressNode,
            subsetIndex,
            (material as Material).nativeHandle,
        )
    }

    override fun setBoneTransforms(transforms: List<Matrix4>) {
        impressApi.setBoneTransforms(impressNode, transforms)
    }

    @MainThread
    override fun setReformAffordanceEnabled(
        entity: MeshEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    ) {
        impressApi.setCustomMeshReformAffordanceEnabled(impressNode, enabled, systemMovable)
        if (enabled) {
            subspace?.let { splitEngineSubspace ->
                splitEngineSubspace.subspaceNode?.listenForInput(executor) { inputEvent ->
                    splitEngineSubspaceManager.forwardInputEvent(
                        inputEvent,
                        splitEngineSubspace.subspaceId,
                    )
                    (entity as AndroidXrEntity).handleInputEvent(inputEvent)
                }
            }
        } else {
            subspace?.subspaceNode?.stopListeningForInput()
        }
    }
}
