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

package androidx.xr.compose.subspace.node

import androidx.xr.compose.subspace.layout.DelegatableSubspaceNode
import androidx.xr.compose.subspace.layout.SubspaceLayoutCoordinates
import androidx.xr.compose.unit.IntVolumeSize

/**
 * A [DelegatableSubspaceNode] whose [onPlaced] callback is invoked when the layout coordinates of
 * the layout node may have changed.
 *
 * @see [androidx.xr.compose.subspace.layout.onGloballyPositioned]
 */
public interface SubspaceLayoutAwareModifierNode : SubspaceMeasuredSizeAwareModifierNode {
    /**
     * [onPlaced] is called after the parent [androidx.xr.compose.subspace.layout.SubspaceModifier]
     * and parent layout has been placed and before child
     * [androidx.xr.compose.subspace.layout.SubspaceModifier] is placed. This allows child
     * [androidx.xr.compose.subspace.layout.SubspaceModifier] to adjust its own placement based on
     * where the parent is.
     *
     * @param coordinates The layout coordinates of the node.
     */
    public fun onPlaced(coordinates: SubspaceLayoutCoordinates) {}

    override fun onRemeasured(size: IntVolumeSize) {}
}
