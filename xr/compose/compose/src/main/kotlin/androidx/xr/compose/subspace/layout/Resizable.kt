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

import androidx.compose.ui.unit.Dp
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.IntVolumeSize

/**
 * Resize a subspace element (i.e. currently only affects Jetpack XR Entity Panels/Volumes) in
 * space.
 *
 * @param enabled - true if this composable should be resizable.
 * @param minimumSize - the smallest allowed dimensions for this composable.
 * @param maximumSize - the largest allowed dimensions for this composable.
 * @param maintainAspectRatio - true if the new size should maintain the same aspect ratio as the
 *   existing size.
 * @param onSizeChange - a callback to process the size change during resizing. This will only be
 *   called if [enabled] is true. If the callback returns false the default behavior of resizing
 *   this composable will be executed. If it returns true, it is the responsibility of the callback
 *   to process the event.
 */
public fun SubspaceModifier.resizable(
    enabled: Boolean = true,
    minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    maintainAspectRatio: Boolean = false,
    onSizeChange: (IntVolumeSize) -> Boolean = { false },
): SubspaceModifier =
    this.then(
        ResizableElement(enabled, minimumSize, maximumSize, maintainAspectRatio, onSizeChange)
    )

private class ResizableElement(
    private val enabled: Boolean,
    private val minimumSize: DpVolumeSize,
    private val maximumSize: DpVolumeSize,
    private val maintainAspectRatio: Boolean,
    private val onSizeChange: (IntVolumeSize) -> Boolean,
) : SubspaceModifierElement<ResizableNode>() {

    init {
        // TODO(b/345303299): Decide on implementation for min/max size bound checking against
        // current
        //  size.
        require(
            minimumSize.depth <= maximumSize.depth &&
                minimumSize.height <= maximumSize.height &&
                minimumSize.width <= maximumSize.width
        ) {
            "minimumSize must be less than or equal to maximumSize"
        }
    }

    override fun create(): ResizableNode =
        ResizableNode(enabled, minimumSize, maximumSize, maintainAspectRatio, onSizeChange)

    override fun update(node: ResizableNode) {
        node.enabled = enabled
        node.minimumSize = minimumSize
        node.maximumSize = maximumSize
        node.maintainAspectRatio = maintainAspectRatio
        node.onSizeChange = onSizeChange
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + maintainAspectRatio.hashCode()
        result = 31 * result + onSizeChange.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherElement = other as? ResizableNode ?: return false

        return enabled == otherElement.enabled &&
            minimumSize == otherElement.minimumSize &&
            maximumSize == otherElement.maximumSize &&
            maintainAspectRatio == otherElement.maintainAspectRatio &&
            onSizeChange === otherElement.onSizeChange
    }
}

internal class ResizableNode(
    internal var enabled: Boolean,
    internal var minimumSize: DpVolumeSize,
    internal var maximumSize: DpVolumeSize,
    internal var maintainAspectRatio: Boolean,
    internal var onSizeChange: (IntVolumeSize) -> Boolean,
) : SubspaceModifier.Node(), CoreEntityNode {
    override fun modifyCoreEntity(coreEntity: CoreEntity) {
        coreEntity.resizable?.updateState(this)
    }
}
