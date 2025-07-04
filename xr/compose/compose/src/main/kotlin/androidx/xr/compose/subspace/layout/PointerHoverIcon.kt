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
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.runtime.Session
import androidx.xr.scenecore.SpatialPointerComponent
import androidx.xr.scenecore.SpatialPointerIcon

/**
 * When present, this modifier defines how the pointer icon will be displayed when the spatial
 * pointer hovers over an element. Any child elements will inherit the pointer icon setting that
 * this modifier defines for their parent element upon hovering.
 *
 * @param icon The [SpatialPointerIcon] to be displayed on hover.
 * @return A [SubspaceModifier] that includes the hover icon behavior.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceModifier.pointerHoverIcon(icon: SpatialPointerIcon): SubspaceModifier =
    this.then(SpatialPointerHoverIconElement(icon))

private class SpatialPointerHoverIconElement(private val icon: SpatialPointerIcon) :
    SubspaceModifierNodeElement<SpatialPointerHoverIconNode>() {

    override fun create(): SpatialPointerHoverIconNode = SpatialPointerHoverIconNode(icon)

    override fun update(node: SpatialPointerHoverIconNode) {
        node.icon = icon
    }

    override fun hashCode(): Int {
        return icon.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialPointerHoverIconElement) return false

        return icon == other.icon
    }
}

internal class SpatialPointerHoverIconNode(internal var icon: SpatialPointerIcon) :
    SubspaceModifier.Node(), CompositionLocalConsumerSubspaceModifierNode, CoreEntityNode {

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Expected Session to be available." }

    /** Whether the SpatialPointerComponent is attached to the entity. */
    private var isComponentAttached: Boolean = false

    private val component: SpatialPointerComponent by lazy {
        SpatialPointerComponent.create(session)
    }

    override fun CoreEntityScope.modifyCoreEntity() {
        if (!isComponentAttached) {
            check(coreEntity.addComponent(component)) {
                "Could not add SpatialPointerComponent to Core Entity"
            }
            isComponentAttached = true
        }
        component.spatialPointerIcon = icon
    }

    override fun onDetach() {
        if (isComponentAttached) {
            coreEntity.removeComponent(component)
            isComponentAttached = false
        }
    }
}
