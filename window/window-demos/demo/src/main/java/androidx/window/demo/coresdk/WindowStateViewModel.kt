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

package androidx.window.demo.coresdk

import android.graphics.Rect
import android.view.Surface.ROTATION_0
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Represents the display configuration, including rotations, bounds, and callback source. */
@Immutable
data class WindowState(
    val name: String,
    val timeStamp: Date = Date(),
    val applicationDisplayRotation: Int = ROTATION_0,
    val activityDisplayRotation: Int = ROTATION_0,
    val applicationDisplayBounds: Rect = Rect(),
    val activityDisplayBounds: Rect = Rect(),
    val callbackDetails: String = "",
    val isDetailsExpanded: Boolean = false,
) {
    /**
     * Compares the core state properties of this WindowState with another object.
     *
     * @param other the object to compare with.
     * @return `true` if the core state properties are the same, `false` otherwise.
     */
    fun isSameState(other: Any?): Boolean =
        (other is WindowState) &&
            applicationDisplayRotation == other.applicationDisplayRotation &&
            activityDisplayRotation == other.activityDisplayRotation &&
            applicationDisplayBounds == other.applicationDisplayBounds &&
            activityDisplayBounds == other.activityDisplayBounds
}

/**
 * Manages the state of window events.
 *
 * This ViewModel maintains a list of [WindowState] objects, representing different window
 * configurations over time. It provides methods to add new states, update existing ones, clear the
 * list, and handle user interactions with the UI.
 *
 * @param initStates an optional initial list of [WindowState] objects to populate the ViewModel
 *   with upon creation. Defaults to an empty list.
 * @property windowStates the current list of window states, this is exposed for the UI to observe.
 */
class WindowStateViewModel(initStates: List<WindowState> = emptyList()) : ViewModel() {
    private val _states = MutableStateFlow(initStates)
    val windowStates: StateFlow<List<WindowState>> = _states.asStateFlow()

    /** Clears all window states from the list. */
    fun clearWindowStates() = _states.update { emptyList() }

    /** Adds a new window state to the beginning of the list. */
    fun onWindowStateCallback(state: WindowState) =
        _states.update { listOf(state) + it } // Prepend to the list.

    /** Updates the latest window state if it's different from the current latest state. */
    fun updateLatestWindowState(state: WindowState) =
        _states.update { if (state.isSameState(it.firstOrNull())) it else listOf(state) + it }

    /** Toggles the expanded state of a window state at the given index. */
    fun onWindowStateItemClick(index: Int) =
        _states.update { states -> states.updateAtIndex(index) { it.toggleExpand() } }
}

/**
 * Toggles the expanded state of a WindowState.
 *
 * @return a new [WindowState] with the `isDetailsExpanded` property toggled.
 */
private fun WindowState.toggleExpand(): WindowState = copy(isDetailsExpanded = !isDetailsExpanded)

/**
 * Updates an item at a specific index in a list using the provided transform function.
 *
 * @param index the index of the item to update.
 * @param transform the function to apply to the item at the specified index.
 * @return a new list with the item at the specified index updated.
 */
private fun <T> List<T>.updateAtIndex(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { idx, value ->
        if (idx == index) transform(value) else value
    }
