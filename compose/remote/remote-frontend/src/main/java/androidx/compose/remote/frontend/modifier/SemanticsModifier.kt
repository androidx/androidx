/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.modifier

import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.CLEAR_AND_SET
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.MERGE
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.SET
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.state.FallbackCreationState
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString

data class SemanticsModifier(val mergeMode: Mode, val semantics: AccessibilitySemantics) :
    RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.SemanticsModifier(
            CoreSemantics().apply {
                mMode = mergeMode
                mTextId = semantics.text?.getIdForCreationState(FallbackCreationState.state) ?: 0
                mContentDescriptionId =
                    semantics.contentDescription?.getIdForCreationState(FallbackCreationState.state)
                        ?: 0
                mStateDescriptionId =
                    semantics.stateDescription?.getIdForCreationState(FallbackCreationState.state)
                        ?: 0
                mRole = fromRole(semantics.role)
            }
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        val properties: SemanticsPropertyReceiver.() -> Unit = {
            semantics.text?.value?.let { text = AnnotatedString(it) }
            semantics.role?.let { role = it }
            semantics.stateDescription?.value?.let { stateDescription = it }
            semantics.contentDescription?.value?.let { contentDescription = it }
        }

        return if (mergeMode == CLEAR_AND_SET) {
            clearAndSetSemantics(properties)
        } else {
            semantics(mergeDescendants = mergeMode == MERGE, properties)
        }
    }
}

private fun fromRole(role: Role?): AccessibleComponent.Role? {
    return when (role) {
        Role.RadioButton -> AccessibleComponent.Role.RADIO_BUTTON
        Role.DropdownList -> AccessibleComponent.Role.DROPDOWN_LIST
        Role.Button -> AccessibleComponent.Role.BUTTON
        Role.Checkbox -> AccessibleComponent.Role.CHECKBOX
        Role.Image -> AccessibleComponent.Role.IMAGE
        Role.Switch -> AccessibleComponent.Role.SWITCH
        Role.Tab -> AccessibleComponent.Role.TAB
        null -> null
        else -> AccessibleComponent.Role.UNKNOWN
    }
}

data class AccessibilitySemantics(
    var contentDescription: RemoteString? = null,
    var role: Role? = null,
    var text: RemoteString? = null,
    var stateDescription: RemoteString? = null,
)

fun RemoteModifier.clearAndSetSemantics(fn: AccessibilitySemantics.() -> Unit): RemoteModifier =
    then(SemanticsModifier(CLEAR_AND_SET, AccessibilitySemantics().apply(fn)))

fun RemoteModifier.semantics(
    mergeDescendants: Boolean = false,
    fn: AccessibilitySemantics.() -> Unit,
): RemoteModifier =
    then(
        SemanticsModifier(if (mergeDescendants) MERGE else SET, AccessibilitySemantics().apply(fn))
    )
