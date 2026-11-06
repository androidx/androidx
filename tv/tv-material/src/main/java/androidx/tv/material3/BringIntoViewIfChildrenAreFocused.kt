/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.unit.IntSize

internal fun Modifier.bringIntoViewIfChildrenAreFocused(): Modifier =
    this.then(BringIntoViewIfChildrenAreFocusedElement)

private object BringIntoViewIfChildrenAreFocusedElement :
    ModifierNodeElement<BringIntoViewIfChildrenAreFocusedNode>() {
    override fun create(): BringIntoViewIfChildrenAreFocusedNode =
        BringIntoViewIfChildrenAreFocusedNode()

    override fun update(node: BringIntoViewIfChildrenAreFocusedNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "bringIntoViewIfChildrenAreFocused"
    }

    override fun hashCode(): Int = "bringIntoViewIfChildrenAreFocused".hashCode()

    override fun equals(other: Any?): Boolean = other === this
}

private class BringIntoViewIfChildrenAreFocusedNode :
    Modifier.Node(), BringIntoViewModifierNode, LayoutAwareModifierNode {
    private var myRect: Rect = Rect.Zero

    override fun onRemeasured(size: IntSize) {
        myRect = Rect(Offset.Zero, Offset(size.width.toFloat(), size.height.toFloat()))
    }

    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        // The container is not expected to be scrollable. Hence, the child is
        // already in view with respect to the container. Pass the current rectangle
        // to the parent and ignore the child rectangle received.
        bringIntoView { myRect }
    }
}
