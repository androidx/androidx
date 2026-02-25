/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.CLEAR_AND_SET
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.MERGE
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.SET
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text

internal data class SemanticsModifier(val mergeMode: Mode, val semantics: AccessibilitySemantics) :
    RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.SemanticsModifier(
            CoreSemantics().apply {
                mMode = mergeMode
                mTextId = semantics.text?.id ?: 0
                mContentDescriptionId = semantics.contentDescription?.id ?: 0
                mStateDescriptionId = semantics.stateDescription?.id ?: 0
                mEnabled = semantics.enabled ?: true
                mRole = fromRole(semantics.role)
            }
        )
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AccessibilitySemantics(
    public var contentDescription: RemoteString? = null,
    public var role: Role? = null,
    public var text: RemoteString? = null,
    public var stateDescription: RemoteString? = null,
    public var enabled: Boolean? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.clearAndSetSemantics(
    fn: AccessibilitySemantics.() -> Unit
): RemoteModifier = then(SemanticsModifier(CLEAR_AND_SET, AccessibilitySemantics().apply(fn)))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.semantics(
    mergeDescendants: Boolean = false,
    fn: AccessibilitySemantics.() -> Unit,
): RemoteModifier =
    then(
        SemanticsModifier(if (mergeDescendants) MERGE else SET, AccessibilitySemantics().apply(fn))
    )
