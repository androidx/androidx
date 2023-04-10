/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * Configure component to be hoverable via pointer enter/exit events.
 *
 * @sample androidx.compose.foundation.samples.HoverableSample
 *
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [HoverInteraction.Enter] when this element is being hovered.
 * @param enabled Controls the enabled state. When `false`, hover events will be ignored.
 */
fun Modifier.hoverable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
) = this then if (enabled) HoverableElement(interactionSource) else Modifier

private class HoverableElement(
    private val interactionSource: MutableInteractionSource
) : ModifierNodeElement<HoverableNode>() {
    override fun create() = HoverableNode(interactionSource)

    override fun update(node: HoverableNode) = node.apply {
        updateInteractionSource(interactionSource)
    }

    override fun hashCode(): Int {
        return 31 * interactionSource.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HoverableElement) return false
        if (other.interactionSource != interactionSource) return false
        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "hoverable"
        properties["interactionSource"] = interactionSource
    }
}

private class HoverableNode(
    private var interactionSource: MutableInteractionSource
) : PointerInputModifierNode, Modifier.Node() {
    private var hoverInteraction: HoverInteraction.Enter? = null

    fun updateInteractionSource(interactionSource: MutableInteractionSource) {
        if (this.interactionSource != interactionSource) {
            tryEmitExit()
            // b/273699888 TODO: Define behavior if there is an ongoing hover
            this.interactionSource = interactionSource
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Main) {
            when (pointerEvent.type) {
                PointerEventType.Enter -> coroutineScope.launch { emitEnter() }
                PointerEventType.Exit -> coroutineScope.launch { emitExit() }
            }
        }
    }

    override fun onCancelPointerInput() {
        tryEmitExit()
    }

    override fun onDetach() {
        tryEmitExit()
    }

    suspend fun emitEnter() {
        if (hoverInteraction == null) {
            val interaction = HoverInteraction.Enter()
            interactionSource.emit(interaction)
            hoverInteraction = interaction
        }
    }

    suspend fun emitExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource.emit(interaction)
            hoverInteraction = null
        }
    }

    fun tryEmitExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource.tryEmit(interaction)
            hoverInteraction = null
        }
    }
}
