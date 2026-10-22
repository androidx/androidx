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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString

@Composable
internal fun Modifier.semantics(op: CoreSemantics): Modifier {
    val contentDescription =
        op.mContentDescriptionId.takeIf { it != 0 }?.let { rememberRemoteStringAsState(it).value }
    val text = op.mTextId.takeIf { it != 0 }?.let { rememberRemoteStringAsState(it).value }

    if (contentDescription == null && text == null && op.mRole == null) return this

    val properties: SemanticsPropertyReceiver.() -> Unit = {
        if (contentDescription != null) {
            this.contentDescription = contentDescription
        }

        if (text != null) {
            this.text = AnnotatedString(text)
        }

        op.mRole?.let { this.role = it.toComposeRole() }
    }
    return when (op.mMode) {
        AccessibleComponent.Mode.SET -> this.semantics(properties = properties)
        AccessibleComponent.Mode.CLEAR_AND_SET -> this.clearAndSetSemantics(properties = properties)
        AccessibleComponent.Mode.MERGE ->
            this.semantics(mergeDescendants = true, properties = properties)
    }
}

private fun AccessibleComponent.Role.toComposeRole(): Role {
    return when (this) {
        AccessibleComponent.Role.BUTTON -> Role.Button
        AccessibleComponent.Role.CHECKBOX -> Role.Checkbox
        AccessibleComponent.Role.SWITCH -> Role.Switch
        AccessibleComponent.Role.RADIO_BUTTON -> Role.RadioButton
        AccessibleComponent.Role.TAB -> Role.Tab
        AccessibleComponent.Role.IMAGE -> Role.Image
        AccessibleComponent.Role.DROPDOWN_LIST -> Role.DropdownList
        // No direct Compose Role equivalents; Button is the closest interactive fallback.
        AccessibleComponent.Role.PICKER -> Role.Button
        AccessibleComponent.Role.CAROUSEL -> Role.Button
        else -> Role.Button
    }
}
