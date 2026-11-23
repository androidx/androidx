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

import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.state.InternalRemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Stable

/** Visibility states for a remote component. */
public enum class RemoteVisibility(internal val value: Int) {
    /** The component is visible and takes up space in the layout. */
    Visible(Component.Visibility.VISIBLE),

    /** The component is invisible, but still takes up space in the layout. */
    Invisible(Component.Visibility.INVISIBLE),

    /** The component is hidden and does not take up any space in the layout. */
    Gone(Component.Visibility.GONE),
}

internal data class VisibilityModifier(public val visible: RemoteInt) : RemoteModifier.Element {
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element =
        with(this as InternalRemoteStateScope) {
            androidx.compose.remote.creation.modifiers.VisibilityModifier(visible.id)
        }
}

/**
 * Sets the visibility of the element.
 *
 * @param visible A [RemoteEnum] of [RemoteVisibility] indicating the visibility state.
 * @return a [RemoteModifier] that sets visibility to [visible]
 */
@Stable
public fun RemoteModifier.visibility(visible: RemoteEnum<RemoteVisibility>): RemoteModifier =
    visibility(visible.toRemoteInt { it.value.ri })

/**
 * Sets the visibility of the element.
 *
 * @param visible A [RemoteInt] indicating visibility (e.g. [RemoteVisibility.Visible],
 *   [RemoteVisibility.Invisible], or [RemoteVisibility.Gone]).
 * @return a [RemoteModifier] that sets visibility to [visible]
 */
@Stable
public fun RemoteModifier.visibility(visible: RemoteInt): RemoteModifier =
    then(VisibilityModifier(visible))
