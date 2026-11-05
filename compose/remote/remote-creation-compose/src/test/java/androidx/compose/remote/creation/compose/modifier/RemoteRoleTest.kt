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
    }

    @Test
    fun clickableAndCombinedClickable_acceptRemoteRole() {
        val clickableDefault = RemoteModifier.clickable(onClick = RemoteAction.None)
        val clickableButton =
            RemoteModifier.clickable(onClick = RemoteAction.None, role = RemoteRole.Button)
        val clickableModifier =
            RemoteModifier.clickable(onClick = RemoteAction.None, role = RemoteRole.Checkbox)
        val clickableNullRole = RemoteModifier.clickable(onClick = RemoteAction.None, role = null)

        val combinedDefault = RemoteModifier.combinedClickable(onClick = RemoteAction.None)
        val combinedButton =
            RemoteModifier.combinedClickable(onClick = RemoteAction.None, role = RemoteRole.Button)
        val combinedModifier =
            RemoteModifier.combinedClickable(
                onClick = RemoteAction.None,
                role = RemoteRole.Switch,
            )
        val combinedNullRole =
            RemoteModifier.combinedClickable(onClick = RemoteAction.None, role = null)

        // Default role is null, so empty action produces RemoteModifier (no SemanticsModifier
        // added)
        assertThat(clickableDefault).isEqualTo(RemoteModifier)
        assertThat(clickableButton).isNotEqualTo(RemoteModifier)
        assertThat(clickableModifier).isNotEqualTo(RemoteModifier)
        assertThat(clickableNullRole).isEqualTo(RemoteModifier)

        assertThat(combinedDefault).isEqualTo(RemoteModifier)
        assertThat(combinedButton).isNotEqualTo(RemoteModifier)
        assertThat(combinedModifier).isNotEqualTo(RemoteModifier)
        assertThat(combinedNullRole).isEqualTo(RemoteModifier)
    }
}
