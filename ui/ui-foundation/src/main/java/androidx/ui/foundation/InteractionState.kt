/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.State
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue

/**
 * InteractionState represents a [Set] of [Interaction]s present on a given component. This
 * allows you to build higher level components comprised of lower level interactions such as
 * [Clickable] and [androidx.ui.foundation.gestures.draggable], and react to [Interaction]
 * changes driven by these components in one place.
 *
 * Creating an [InteractionState] and passing it to these lower level interactions will cause a
 * recomposition when there are changes to the state of [Interaction], such as when a [Clickable]
 * becomes [Interaction.Pressed].
 *
 * @sample androidx.ui.foundation.samples.InteractionStateSample
 */
class InteractionState : State<Set<Interaction>> {
    override var value: Set<Interaction> by mutableStateOf(emptySet())
        private set

    /**
     * Adds the provided [interaction] to this InteractionState.
     * Since InteractionState represents a [Set], duplicate [interaction]s will not be added, and
     * hence will not cause a recomposition.
     */
    fun addInteraction(interaction: Interaction) {
        if (interaction !in this) value = value + interaction
    }

    /**
     * Removes the provided [interaction], if it is present, from this InteractionState.
     */
    fun removeInteraction(interaction: Interaction) {
        if (interaction in this) value = value - interaction
    }

    /**
     * @return whether the provided [interaction] exists inside this InteractionState.
     */
    operator fun contains(interaction: Interaction): Boolean = value.contains(interaction)
}
