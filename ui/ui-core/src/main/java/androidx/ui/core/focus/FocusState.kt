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

/**
 * Different states of the focus system.
 *
 * These are the most frequently used states. For more detailed states, refer to
 * [FocusDetailedState].
 *
 * [Focused]: A focusable component that is currently in focus.
 * [NotFocusable]: A focusable component that is currently not focusable. Eg. A disabled button.
 * [NotFocused]:  A focusable component that is not currently focused.
 */
enum class FocusState { Focused, NotFocusable, NotFocused }

/**
 * Different states of the focus system.
 * These are the detailed states used by the Focus Nodes.
 * If you need higher level states, eg [Focused][FocusState.Focused] or
 * [NotFocused][FocusState.NotFocused], use the states in [FocusState].
 *
 * [Active]: The focusable component is currently active (i.e. it receives key events).
 * [ActiveParent] : One of the descendants of the focusable component is [Active].
 * [Captured]: The focusable component is currently active (has focus), and is in a state where
 * it does not want to give up focus. (Eg. a text field with an invalid phone number).
 * [Disabled]: The focusable component is not currently focusable. (eg. A disabled button).
 * [Inactive]: The focusable component does not receive any key events. (ie it is not active,
 * nor are any of its descendants active).
 */
enum class FocusDetailedState { Active, ActiveParent, Captured, Disabled, Inactive }

/**
 * Converts a [FocusDetailedState] to a [FocusState].
 */
fun FocusDetailedState.focusState() = when (this) {
    FocusDetailedState.Captured,
    FocusDetailedState.Active -> FocusState.Focused
    FocusDetailedState.ActiveParent,
    FocusDetailedState.Inactive -> FocusState.NotFocused
    FocusDetailedState.Disabled -> FocusState.NotFocusable
}
