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

package androidx.compose.foundation.text

import androidx.collection.mutableObjectListOf
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.mutableIntStateOf

internal class LinkStateInteractionSourceObserver {
    private val Focused = 0b001
    private val Hovered = 0b010
    private val Pressed = 0b100
    private val interactionState = mutableIntStateOf(0b000)

    suspend fun collectInteractionsForLinks(interactionSource: InteractionSource) {
        val interactions = mutableObjectListOf<Interaction>()
        interactionSource.interactions.collect { interaction ->
            var state = 0
            when (interaction) {
                is HoverInteraction.Enter,
                is FocusInteraction.Focus,
                is PressInteraction.Press -> {
                    interactions.add(interaction)
                }
                is HoverInteraction.Exit -> interactions.remove(interaction.enter)
                is FocusInteraction.Unfocus -> interactions.remove(interaction.focus)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
            }
            interactions.forEach {
                when (it) {
                    is HoverInteraction.Enter -> state = state or Hovered
                    is FocusInteraction.Focus -> state = state or Focused
                    is PressInteraction.Press -> state = state or Pressed
                }
            }
            interactionState.intValue = state
        }
    }

    val isFocused
        get() = interactionState.intValue and Focused != 0

    val isHovered
        get() = interactionState.intValue and Hovered != 0

    val isPressed
        get() = interactionState.intValue and Pressed != 0
}
