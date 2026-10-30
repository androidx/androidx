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
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.ui.semantics.Role
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteRoleTest {
    @Test
    fun toAccessibleRole_mapsToCoreRole() {
        assertThat(RemoteRole.Button.toAccessibleRole()).isEqualTo(AccessibleComponent.Role.BUTTON)
        assertThat(RemoteRole.Checkbox.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.CHECKBOX)
        assertThat(RemoteRole.Switch.toAccessibleRole()).isEqualTo(AccessibleComponent.Role.SWITCH)
        assertThat(RemoteRole.RadioButton.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.RADIO_BUTTON)
        assertThat(RemoteRole.Tab.toAccessibleRole()).isEqualTo(AccessibleComponent.Role.TAB)
        assertThat(RemoteRole.Image.toAccessibleRole()).isEqualTo(AccessibleComponent.Role.IMAGE)
        assertThat(RemoteRole.DropdownList.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.DROPDOWN_LIST)
        assertThat(RemoteRole.ValuePicker.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.PICKER)
        assertThat(RemoteRole.Carousel.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.CAROUSEL)
        assertThat(RemoteRole.Unknown.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.UNKNOWN)
    }

    @Test
    fun roleToString_returnsExpectedNames() {
        assertThat(RemoteRole.Button.toString()).isEqualTo("Button")
        assertThat(RemoteRole.Checkbox.toString()).isEqualTo("Checkbox")
        assertThat(RemoteRole.Switch.toString()).isEqualTo("Switch")
        assertThat(RemoteRole.RadioButton.toString()).isEqualTo("RadioButton")
        assertThat(RemoteRole.Tab.toString()).isEqualTo("Tab")
        assertThat(RemoteRole.Image.toString()).isEqualTo("Image")
        assertThat(RemoteRole.DropdownList.toString()).isEqualTo("DropdownList")
        assertThat(RemoteRole.ValuePicker.toString()).isEqualTo("ValuePicker")
        assertThat(RemoteRole.Carousel.toString()).isEqualTo("Carousel")
        assertThat(RemoteRole.Unknown.toString()).isEqualTo("Unknown")
    }

    @Test
    fun clickableAndCombinedClickable_acceptRemoteRole() {
        val clickableModifier =
            RemoteModifier.clickable(onClick = RemoteAction.Empty, role = RemoteRole.Checkbox)
        val clickableComposeRole =
            RemoteModifier.clickable(onClick = RemoteAction.Empty, role = Role.Checkbox)
        val clickableNullRole =
            RemoteModifier.clickable(onClick = RemoteAction.Empty, role = null as Role?)
        val combinedModifier =
            RemoteModifier.combinedClickable(
                onClick = RemoteAction.Empty,
                role = RemoteRole.Switch,
            )
        val combinedNullRole =
            RemoteModifier.combinedClickable(onClick = RemoteAction.Empty, role = null)
        assertThat(clickableModifier).isNotEqualTo(RemoteModifier)
        assertThat(clickableComposeRole).isEqualTo(clickableModifier)
        assertThat(clickableNullRole).isEqualTo(RemoteModifier)
        assertThat(combinedModifier).isNotEqualTo(RemoteModifier)
        assertThat(combinedNullRole).isEqualTo(RemoteModifier)
    }

    @Test
    fun roleConversions_allRolesMapCorrectly() {
        val mappings =
            listOf(
                Triple(Role.Button, RemoteRole.Button, AccessibleComponent.Role.BUTTON),
                Triple(Role.Checkbox, RemoteRole.Checkbox, AccessibleComponent.Role.CHECKBOX),
                Triple(Role.Switch, RemoteRole.Switch, AccessibleComponent.Role.SWITCH),
                Triple(
                    Role.RadioButton,
                    RemoteRole.RadioButton,
                    AccessibleComponent.Role.RADIO_BUTTON,
                ),
                Triple(Role.Tab, RemoteRole.Tab, AccessibleComponent.Role.TAB),
                Triple(Role.Image, RemoteRole.Image, AccessibleComponent.Role.IMAGE),
                Triple(
                    Role.DropdownList,
                    RemoteRole.DropdownList,
                    AccessibleComponent.Role.DROPDOWN_LIST,
                ),
                Triple(Role.ValuePicker, RemoteRole.ValuePicker, AccessibleComponent.Role.PICKER),
                Triple(Role.Carousel, RemoteRole.Carousel, AccessibleComponent.Role.CAROUSEL),
            )

        for ((composeRole, remoteRole, accessibleRole) in mappings) {
            val modWithRemoteRole =
                RemoteModifier.clickable(onClick = RemoteAction.Empty, role = remoteRole)
            val modWithComposeRole =
                RemoteModifier.clickable(onClick = RemoteAction.Empty, role = composeRole)
            assertThat(modWithComposeRole).isEqualTo(modWithRemoteRole)
            assertThat(remoteRole.toAccessibleRole()).isEqualTo(accessibleRole)
        }
    }
}
