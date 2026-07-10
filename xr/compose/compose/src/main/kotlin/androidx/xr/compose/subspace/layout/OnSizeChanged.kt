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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceMeasuredSizeAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.unit.IntVolumeSize

/**
 * Invokes `onSizeChanged` with the `IntVolumeSize` of the element when its size changes.
 *
 * This callback is executed after the measure pass when the SubspaceComposable's size is finalized.
 * It will be invoked at least once when the size becomes available and subsequently whenever the
 * size changes.
 *
 * For observing changes to position or orientation in addition to size, use
 * [SubspaceModifier.onGloballyPositioned] instead.
 *
 * @param onSizeChanged The callback that is invoked when the size changes. The [IntVolumeSize]
 *   parameter represents the new size of the layout in pixels.
 */
public fun SubspaceModifier.onSizeChanged(
    onSizeChanged: (size: IntVolumeSize) -> Unit
): SubspaceModifier = this then OnSizeChangedVolumeElement(onSizeChanged)

private class OnSizeChangedVolumeElement(val onSizeChanged: (IntVolumeSize) -> Unit) :
    SubspaceModifierNodeElement<OnSizeChangedNode>() {
    override fun create(): OnSizeChangedNode {
        return OnSizeChangedNode(onSizeChanged)
    }

    override fun update(node: OnSizeChangedNode) {
        node.update(onSizeChanged)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnSizeChangedVolumeElement) return false
        return onSizeChanged === other.onSizeChanged
    }

    override fun hashCode(): Int {
        return onSizeChanged.hashCode()
    }
}

private class OnSizeChangedNode(public var callback: (IntVolumeSize) -> Unit) :
    SubspaceModifier.Node(), SubspaceMeasuredSizeAwareModifierNode {

    private var lastSize: IntVolumeSize? = null

    internal fun update(callback: (IntVolumeSize) -> Unit) {
        this.callback = callback
        // This guarantees the new callback will be invoked immediately, even if
        // the size hasn't changed.
        lastSize = null
    }

    override fun onRemeasured(size: IntVolumeSize) {
        if (size != lastSize) {
            callback(size)
            lastSize = size
        }
    }
}
