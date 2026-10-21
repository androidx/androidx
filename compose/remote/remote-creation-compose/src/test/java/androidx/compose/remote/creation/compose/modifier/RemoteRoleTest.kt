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
import androidx.compose.remote.creation.compose.action.Action
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
        assertThat(RemoteRole.Unknown.toAccessibleRole())
            .isEqualTo(AccessibleComponent.Role.UNKNOWN)
    }

    @Test
    fun clickableAndCombinedClickable_acceptRemoteRole() {
        val clickableModifier =
            RemoteModifier.clickable(action = Action.Empty, role = RemoteRole.Checkbox)
        val clickableComposeRole =
            RemoteModifier.clickable(action = Action.Empty, role = Role.Checkbox)
        val clickableNullRole =
            RemoteModifier.clickable(action = Action.Empty, role = null as Role?)
        val combinedModifier =
            RemoteModifier.combinedClickable(onClick = Action.Empty, role = RemoteRole.Switch)
        val combinedNullRole = RemoteModifier.combinedClickable(onClick = Action.Empty, role = null)
        assertThat(clickableModifier).isNotEqualTo(RemoteModifier)
        assertThat(clickableComposeRole).isEqualTo(clickableModifier)
        assertThat(clickableNullRole).isEqualTo(RemoteModifier)
        assertThat(combinedModifier).isNotEqualTo(RemoteModifier)
        assertThat(combinedNullRole).isEqualTo(RemoteModifier)
    }
}
