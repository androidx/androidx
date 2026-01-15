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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

internal interface DragAndDropHoverInteraction : Interaction {

    /**
     * An interaction representing a drag and drop hover event on a BasicTextField.
     *
     * @see androidx.compose.foundation.hoverable
     * @see Exit
     */
    class Enter : DragAndDropHoverInteraction

    /**
     * An interaction representing an [Enter] event being released on a BasicTextField.
     *
     * @property enter the source [Enter] interaction that is being released
     * @see Enter
     */
    class Exit(val enter: Enter) : DragAndDropHoverInteraction
}

/**
 * Subscribes to this [InteractionSource] and returns a [State] representing whether this component
 * is hovered by DragAndDrop or not.
 *
 * @return [State] representing whether this component is being hovered or not
 */
@Composable
internal fun InteractionSource.collectIsDragAndDropHoveredAsState(): State<Boolean> {
    val isHovered = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val hoverInteractions = mutableListOf<DragAndDropHoverInteraction.Enter>()
        interactions.collect { interaction ->
            when (interaction) {
                is DragAndDropHoverInteraction.Enter -> hoverInteractions.add(interaction)
                is DragAndDropHoverInteraction.Exit -> hoverInteractions.remove(interaction.enter)
            }
            isHovered.value = hoverInteractions.isNotEmpty()
        }
    }
    return isHovered
}
