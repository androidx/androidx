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

import androidx.annotation.RestrictTo
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.scenecore.PointSourceParams

/**
 * Used to provide a [PointSourceParams] to allow specifying the modified Composable as an audio
 * source.
 *
 * PointSourceParams are used to configure a sound to be spatialized as a point in 3D space.
 *
 * @param onPointSourceParams - Will be called with a [PointSourceParams] once it is generated. This
 *   will generally only execute one time, but may be executed again if the modifier chain is
 *   updated with significant insertions and/or deletions, causing this modifier to be recreated. It
 *   is recommended to order this modifier before conditional modifiers in a chain to maintain one
 *   time execution, or to handle re-execution appropriately.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceModifier.onPointSourceParams(
    onPointSourceParams: (PointSourceParams) -> Unit
): SubspaceModifier = this.then(PointSourceElement(onPointSourceParams))

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

private class PointSourceNode(internal var onPointSourceParams: (PointSourceParams) -> Unit) :
    SubspaceModifier.Node(), CoreEntityNode {
    private var hasInvoked = false

    override fun CoreEntityScope.modifyCoreEntity() {
        if (!hasInvoked) {
            onPointSourceParams(PointSourceParams(coreEntity.entity))
            hasInvoked = true
        }
    }
}
