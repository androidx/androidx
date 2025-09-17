/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * A [androidx.compose.ui.Modifier.Node] which receives various callbacks in response to local
 * changes in layout.
 *
 * This is the [androidx.compose.ui.Modifier.Node] equivalent of
 * [androidx.compose.ui.layout.OnRemeasuredModifier] and
 * [androidx.compose.ui.layout.OnPlacedModifier]
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.OnSizeChangedSample
 * @sample androidx.compose.ui.samples.OnPlaced
 * @sample androidx.compose.ui.samples.LayoutAwareModifierNodeSample
 */
interface LayoutAwareModifierNode : DelegatableNode {
    /**
     * [onPlaced] is called after the parent [LayoutModifier] and parent layout has been placed and
     * before child [LayoutModifier] is placed. This allows child [LayoutModifier] to adjust its own
     * placement based on where the parent is.
     *
     * If you only need to access the current [LayoutCoordinates] at a single point in time from
     * outside this method, use [requireLayoutCoordinates].
     */
    fun onPlaced(coordinates: LayoutCoordinates) {}

    /**
     * This method is called when the layout content is remeasured. The most common usage is
     * [onSizeChanged].
     */
    fun onRemeasured(size: IntSize) {}
}

// TODO(b/309776096): Make it public
internal interface OnUnplacedModifierNode : DelegatableNode {
    fun onUnplaced()
}
