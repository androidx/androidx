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

package androidx.compose.ui.node

import androidx.compose.ui.layout.registerOnLayoutRectChanged

/**
 * A [androidx.compose.ui.Modifier.Node] which receives a callback when the layout node is not
 * placed anymore.
 */
interface UnplacedStateAwareModifierNode : DelegatableNode {
    /**
     * This method is called when the layout was placed earlier, and is not placed anymore. It
     * happens when some of the parents still compose this child, but don't place a corresponding
     * [androidx.compose.ui.layout.Placeable], or when this child is completely removed from the
     * tree after being placed earlier. Not placed children are not drawn and doesn't receive
     * pointer input events.
     *
     * Those apis could be used to be notified when the layout is placed:
     * - [registerOnLayoutRectChanged]
     * - [GlobalPositionAwareModifierNode.onGloballyPositioned]
     * - [LayoutAwareModifierNode.onPlaced]
     * - Reacting on a non-null [androidx.compose.ui.layout.Placeable.PlacementScope.coordinates]
     *   from a [LayoutModifierNode] placement block.
     */
    fun onUnplaced()
}
