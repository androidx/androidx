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

@file:JvmName("ClickableModifierKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.ui.semantics.Role

// TODO provide an onClickLabel
/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * @param action will be called when user clicks on the element
 * @param enabled Controls the enabled state. When `false`, [action], and this modifier will appear
 *   disabled for accessibility services
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 */
public fun RemoteModifier.clickable(
    action: Action,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): RemoteModifier = clickableImpl(action = action, enabled = enabled, role = role?.toRemoteRole())

private fun Role.toRemoteRole(): RemoteRole =
    when (this) {
        Role.Button -> RemoteRole.Button
        Role.Checkbox -> RemoteRole.Checkbox
        Role.Switch -> RemoteRole.Switch
        Role.RadioButton -> RemoteRole.RadioButton
        Role.Tab -> RemoteRole.Tab
        Role.Image -> RemoteRole.Image
        Role.DropdownList -> RemoteRole.DropdownList
        else -> RemoteRole.Unknown
    }
