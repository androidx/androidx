/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams

/**
 * Used to provide a [PointSourceParams] to allow specifying the modified Composable as an audio
 * source. See [PointSourceParams] for more info on how to attach this object to a media source.
 *
 * PointSourceParams are used to configure a sound to be spatialized as a point in 3D space. This is
 * to override the default behavior where sound is played from the SpatialMainPanel.
 *
 * @sample androidx.xr.compose.samples.OnPointSourceParamsAvailableSample
 * @param onPointSourceParamsAvailable Will be called with a [PointSourceParams] once it is
 *   generated.
 */
public fun SubspaceModifier.onPointSourceParamsAvailable(
    onPointSourceParamsAvailable: (PointSourceParams) -> Unit
): SubspaceModifier = this.then(PointSourceElement(onPointSourceParamsAvailable))

private class PointSourceElement(private val onPointSourceParams: (PointSourceParams) -> Unit) :
    SubspaceModifierNodeElement<PointSourceNode>() {

    override fun create(): PointSourceNode = PointSourceNode(onPointSourceParams)

    override fun update(node: PointSourceNode) {
        node.onPointSourceParams = onPointSourceParams
    }

    override fun hashCode(): Int {
        return onPointSourceParams.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointSourceElement) return false

        return onPointSourceParams === other.onPointSourceParams
    }
}

// TODO(b/453766082) Add unit tests for the onPointSourceParamsAvailable modifier.
private class PointSourceNode(var onPointSourceParams: (PointSourceParams) -> Unit) :
    SubspaceModifier.Node(), CoreEntityNode {
    private var currentEntity: Entity? = null

    override fun CoreEntityScope.modifyCoreEntity() {
        coreEntity.onEntityAttached { entity ->
            if (currentEntity != entity) {
                currentEntity = entity
                onPointSourceParams(PointSourceParams(entity))
            }
        }
    }
}
