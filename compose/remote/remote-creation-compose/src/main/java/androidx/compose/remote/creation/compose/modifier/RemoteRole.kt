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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** The type of user interface element. Accessibility services can use this to describe the node. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteRole internal constructor(internal val value: Int) {
    internal fun toAccessibleRole(): AccessibleComponent.Role =
        when (this) {
            Button -> AccessibleComponent.Role.BUTTON
            Checkbox -> AccessibleComponent.Role.CHECKBOX
            Switch -> AccessibleComponent.Role.SWITCH
            RadioButton -> AccessibleComponent.Role.RADIO_BUTTON
            Tab -> AccessibleComponent.Role.TAB
            Image -> AccessibleComponent.Role.IMAGE
            DropdownList -> AccessibleComponent.Role.DROPDOWN_LIST
            ValuePicker -> AccessibleComponent.Role.PICKER
            Carousel -> AccessibleComponent.Role.CAROUSEL
            else -> AccessibleComponent.Role.UNKNOWN
        }

    override fun toString(): String =
        when (this) {
            Button -> "Button"
            Checkbox -> "Checkbox"
            Switch -> "Switch"
            RadioButton -> "RadioButton"
            Tab -> "Tab"
            Image -> "Image"
            DropdownList -> "DropdownList"
            ValuePicker -> "ValuePicker"
            Carousel -> "Carousel"
            else -> "Unknown"
        }

    public companion object {
        /** An interactive element that initiates an action when clicked. */
        @Stable public val Button: RemoteRole = RemoteRole(0)

        /** A two-state toggleable element that can be checked or unchecked. */
        @Stable public val Checkbox: RemoteRole = RemoteRole(1)

        /** A toggleable switch providing an on/off state. */
        @Stable public val Switch: RemoteRole = RemoteRole(2)

        /** A selection element representing mutually exclusive options. */
        @Stable public val RadioButton: RemoteRole = RemoteRole(3)

        /** A tab representing a single selectable page or tab pane. */
        @Stable public val Tab: RemoteRole = RemoteRole(4)

        /** A graphical visual element or image. */
        @Stable public val Image: RemoteRole = RemoteRole(5)

        /** An element associated with selecting from a drop-down menu or list. */
        @Stable public val DropdownList: RemoteRole = RemoteRole(6)

        /** A selectable value picker component allowing forward/backward value navigation. */
        @Stable public val ValuePicker: RemoteRole = RemoteRole(7)

        /** A carousel element displaying a scrollable collection of items. */
        @Stable public val Carousel: RemoteRole = RemoteRole(8)

        internal val Unknown: RemoteRole = RemoteRole(-1)
    }
}
