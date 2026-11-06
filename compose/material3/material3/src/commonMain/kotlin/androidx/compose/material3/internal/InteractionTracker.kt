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

package androidx.compose.material3.internal

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Tracks which kinds of [Interaction]s are active on an [InteractionSource] from a [Modifier.Node],
 * like `collectIsFocusedAsState()` or `collectIsPressedAsState()` do in composition, but without
 * recomposing.
 *
 * Create it in [node] with the [kinds] to track (a combination of [Focus], [Press], [Hover] and
 * [Drag]), call [track] from [Modifier.Node.onAttach] and whenever the [InteractionSource] may have
 * changed, and read [isFocused], [isPressed], [isHovered] or [isDragged]. [onChanged] is called
 * whenever one of them changes. Tracking stops when [node] is detached, and starts over on the next
 * [track].
 *
 * This is a plain object rather than a delegated node, so it doesn't add a node nor a coroutine
 * scope: it collects in the [Modifier.Node.coroutineScope] of [node]. It's an abstract class rather
 * than taking a lambda so that the subclass is the only allocation, and it only allocates its list
 * of active interactions once a tracked interaction starts.
 */
internal abstract class InteractionTracker(
    private val node: Modifier.Node,
    private val kinds: Int,
) {
    private var interactionSource: InteractionSource? = null
    private var job: Job? = null
    private var activeInteractions: MutableList<Interaction>? = null
    private var activeKinds = 0

    val isFocused: Boolean
        get() = (activeKinds and Focus) != 0

    val isPressed: Boolean
        get() = (activeKinds and Press) != 0

    val isHovered: Boolean
        get() = (activeKinds and Hover) != 0

    val isDragged: Boolean
        get() = (activeKinds and Drag) != 0

    /** Called when [isFocused], [isPressed], [isHovered] or [isDragged] changes. */
    abstract fun onChanged()

    /** Starts tracking [interactionSource], unless it's already being tracked. */
    fun track(interactionSource: InteractionSource) {
        if (interactionSource === this.interactionSource && job?.isActive == true) return
        this.interactionSource = interactionSource
        job?.cancel()
        job =
            if (node.isAttached) {
                node.coroutineScope.launch {
                    interactionSource.interactions.collect { onInteraction(it) }
                }
            } else {
                null
            }
        // Interactions don't replay, so nothing is active until new ones are collected.
        activeInteractions?.clear()
        if (activeKinds != 0) {
            activeKinds = 0
            onChanged()
        }
    }

    private fun onInteraction(interaction: Interaction) {
        when (interaction) {
            is FocusInteraction.Unfocus -> activeInteractions?.remove(interaction.focus)
            is PressInteraction.Release -> activeInteractions?.remove(interaction.press)
            is PressInteraction.Cancel -> activeInteractions?.remove(interaction.press)
            is HoverInteraction.Exit -> activeInteractions?.remove(interaction.enter)
            is DragInteraction.Stop -> activeInteractions?.remove(interaction.start)
            is DragInteraction.Cancel -> activeInteractions?.remove(interaction.start)
            else -> {
                if ((kindOf(interaction) and kinds) == 0) return
                val active =
                    activeInteractions
                        ?: mutableListOf<Interaction>().also { activeInteractions = it }
                active.add(interaction)
            }
        }
        var newKinds = 0
        activeInteractions?.fastForEach { newKinds = newKinds or kindOf(it) }
        if (newKinds != activeKinds) {
            activeKinds = newKinds
            onChanged()
        }
    }

    companion object {
        const val Focus = 1
        const val Press = 1 shl 1
        const val Hover = 1 shl 2
        const val Drag = 1 shl 3

        private fun kindOf(interaction: Interaction): Int =
            when (interaction) {
                is FocusInteraction.Focus -> Focus
                is PressInteraction.Press -> Press
                is HoverInteraction.Enter -> Hover
                is DragInteraction.Start -> Drag
                else -> 0
            }
    }
}
