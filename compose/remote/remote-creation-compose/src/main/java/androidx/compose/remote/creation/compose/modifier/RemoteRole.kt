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

import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.runtime.Immutable

/**
 * The type of user interface element. Accessibility services might use this to describe the element
 * or do customizations.
 *
 * @see RemoteSemanticsPropertyReceiver.role
 */
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
        /**
         * This element is a button control. Associated semantics properties and modifiers for
         * accessibility: [RemoteModifier.clickable], [RemoteSemanticsProperties.Enabled].
         */
        public val Button: RemoteRole = RemoteRole(0)

        /**
         * This element is a Checkbox which is a component that represents two states (checked /
         * unchecked). Associated semantics properties and modifiers for accessibility:
         * [RemoteModifier.clickable], [RemoteSemanticsProperties.StateDescription],
         * [RemoteSemanticsProperties.Enabled].
         */
        public val Checkbox: RemoteRole = RemoteRole(1)

        /**
         * This element is a Switch which is a two state toggleable component that provides on/off
         * like options. Associated semantics properties and modifiers for accessibility:
         * [RemoteModifier.clickable], [RemoteSemanticsProperties.StateDescription],
         * [RemoteSemanticsProperties.Enabled].
         */
        public val Switch: RemoteRole = RemoteRole(2)

        /**
         * This element is a RadioButton which is a component to represent two states, selected and
         * not selected. Associated semantics properties and modifiers for accessibility:
         * [RemoteModifier.clickable], [RemoteSemanticsProperties.StateDescription],
         * [RemoteSemanticsProperties.Enabled].
         */
        public val RadioButton: RemoteRole = RemoteRole(3)

        /**
         * This element is a Tab which represents a single page of content using a text label and/or
         * icon. Associated semantics properties and modifiers for accessibility:
         * [RemoteModifier.clickable], [RemoteSemanticsProperties.StateDescription],
         * [RemoteSemanticsProperties.Enabled].
         */
        public val Tab: RemoteRole = RemoteRole(4)

        /**
         * This element is an image. Associated semantics properties for accessibility:
         * [RemoteSemanticsProperties.ContentDescription].
         */
        public val Image: RemoteRole = RemoteRole(5)

        /**
         * This element is associated with a drop down menu or list. Associated semantics properties
         * and modifiers for accessibility: [RemoteModifier.clickable],
         * [RemoteSemanticsProperties.Enabled].
         */
        public val DropdownList: RemoteRole = RemoteRole(6)

        /**
         * This element is a value picker component allowing value navigation. Associated semantics
         * properties and modifiers for accessibility: [RemoteModifier.verticalScroll],
         * [RemoteSemanticsProperties.StateDescription], [RemoteSemanticsProperties.Enabled].
         */
        public val ValuePicker: RemoteRole = RemoteRole(7)

        /**
         * This element is a carousel displaying a scrollable collection of items. Associated
         * semantics properties and modifiers for accessibility: [RemoteModifier.horizontalScroll],
         * [RemoteSemanticsProperties.Enabled].
         */
        public val Carousel: RemoteRole = RemoteRole(8)
    }
}
