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

package androidx.ui.core.focus

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier

/**
 * A [Modifier.Element] that wraps makes the modifiers on the right into a Focusable. Use a
 * different instance of [FocusModifier] for each focusable component.
 *
 * TODO(b/152528891): Write tests for [FocusModifier] after we finalize on the api (API
 * review tracked by b/152529882).
 */
interface FocusModifier : Modifier.Element {
    /**
     * The current focus state of the component wrapped by this [Modifier].
     */
    val focusDetailedState: FocusDetailedState

    /**
     * Use this function to request focus for this component. If the system grants focus to the
     * component wrapped by this [modifier][FocusModifier], its [state][focusDetailedState] will
     * be set to [Active][FocusDetailedState.Active].
     */
    fun requestFocus()

    /**
     * Use this function to send a request to capture the focus. If a component is captured, it's
     * [state][focusDetailedState] will be set to [Captured][FocusDetailedState.Captured]. When a
     * component is in this state, it holds onto focus until [freeFocus] is called. When a
     * component is in the [Captured][FocusDetailedState.Captured] state, all focus requests from
     * other components are declined.
     *
     * @return true if the focus was successfully captured. false otherwise.
     */
    fun captureFocus(): Boolean

    /**
     * Use this function to send a request to release focus when the component is in a
     * [Captured][FocusDetailedState.Captured] state.
     *
     * @return true if the focus was successfully released. false otherwise.
     */
    fun freeFocus(): Boolean
}

/**
 * Use this function to create an instance of [FocusModifier]. Adding a [FocusModifier] to a
 * [Composable] makes it focusable.
 */
@Composable
fun FocusModifier(): FocusModifier = remember { FocusModifierImpl(FocusDetailedState.Inactive) }

/**
 * This function returns the [FocusState] for the component wrapped by this [FocusModifier].
 */
val FocusModifier.focusState: FocusState get() = focusDetailedState.focusState()