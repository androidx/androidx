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

package androidx.compose.ui

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.IntSize

/**
 * Declares that this component blocks touch/pointer input and screen reader accessibility for
 * elements geometrically behind it.
 *
 * @sample androidx.compose.ui.samples.InteractionBarrierSample
 */
public fun Modifier.interactionBarrier(): Modifier = this then InteractionBarrierElement

private object InteractionBarrierElement : ModifierNodeElement<InteractionBarrierNode>() {
    override fun create(): InteractionBarrierNode = InteractionBarrierNode()

    override fun update(node: InteractionBarrierNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "interactionBarrier"
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other === this
}

internal class InteractionBarrierNode :
    Modifier.Node(), PointerInputModifierNode, SemanticsModifierNode {

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {}

    override fun onCancelPointerInput() {}

    // Empty implementation forces this LayoutNode to generate a SemanticsNode,
    // which is needed for accessibility pruning in SemanticsOwner.
    override fun SemanticsPropertyReceiver.applySemantics() {}
}
