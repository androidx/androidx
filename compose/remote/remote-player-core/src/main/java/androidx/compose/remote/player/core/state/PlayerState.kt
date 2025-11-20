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
package androidx.compose.remote.player.core.state

import androidx.annotation.RestrictTo

/**
 * Represents the state of a remote compose player.
 *
 * The player state is a collection of named values, where each value is an [RcValue]. The state can
 * be updated by providing an edit function that modifies a mutable view of the state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PlayerState {
    /**
     * Returns the current snapshot of values in the player state.
     *
     * @return A map of names to [RcValue] instances representing the current state.
     */
    public val values: Map<String, RcValue>

    /**
     * Updates the player state with the given edit function.
     *
     * The edit function receives a [MapScope] which provides a mutable view of the state.
     * Modifications made to the [MapScope] will be reflected in the player state.
     *
     * @param edit A consumer that accepts a [MapScope] for modifying the player state.
     */
    public fun updateState(edit: MapScope.() -> Unit)

    /**
     * A scope for modifying the player state.
     *
     * This class provides a mutable view of the player state, allowing values to be set or updated.
     */
    public class MapScope(public val values: MutableMap<String, RcValue>) {
        /**
         * Set a value.
         *
         * @param name
         * @param value
         */
        public fun setUserLocalValue(name: String, value: RcValue) {
            values.put(name, value)
        }
    }

    /**
     * A map representing a snapshot of the player state.
     *
     * This class holds an immutable map of names to [RcValue] instances.
     */
    public class PlayerStateMap(public val values: Map<String, RcValue>) {
        public companion object {
            @JvmStatic public fun empty(): PlayerStateMap = PlayerStateMap(mapOf())
        }
    }
}

/**
 * Applies the current state to the given [StateUpdater].
 *
 * This method iterates through the values in the player state and sets each value on the provided
 * [StateUpdater] based on its type.
 *
 * @param stateUpdater The [StateUpdater] to apply the state to.
 */
internal fun PlayerState.applyTo(stateUpdater: StateUpdater) {
    values.forEach { name, rcValue ->
        when (rcValue) {
            is RcLong -> {
                stateUpdater.setNamedLong(name, rcValue.value)
            }
            is RcFloat -> {
                stateUpdater.setUserLocalFloat(name, rcValue.value)
            }
            is RcString -> {
                stateUpdater.setUserLocalString(name, rcValue.value)
            }
            is RcInt -> {
                stateUpdater.setUserLocalInt(name, rcValue.value)
            }
            is RcColor -> {
                stateUpdater.setUserLocalColor(name, rcValue.value)
            }
            is RcBitmap -> {
                stateUpdater.setUserLocalBitmap(name, rcValue.value)
            }
        }
    }
}
