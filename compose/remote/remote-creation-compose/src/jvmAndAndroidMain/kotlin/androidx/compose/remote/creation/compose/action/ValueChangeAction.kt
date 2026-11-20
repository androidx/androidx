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

package androidx.compose.remote.creation.compose.action

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.compose.state.InternalRemoteStateScope
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.MutableRemoteValue
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.arrayForCreationState
import androidx.compose.remote.creation.compose.state.creationState
import androidx.compose.remote.creation.compose.state.isLiteral

/** Update a value on click. */
internal class ValueChangeAction<T>(
    public val remoteValue: MutableRemoteValue<T>,
    public val updatedValue: RemoteValue<T>,
) : RemoteAction {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction =
        with(this as InternalRemoteStateScope) {
            val actualMutable = creationState.asEncodedMutable(remoteValue)
            val actualValue = creationState.asEncoded(updatedValue)

            if (actualMutable is MutableRemoteInt) {
                actualValue as RemoteInt
                val array = actualValue.arrayForCreationState(creationState)

                if (array.isLiteral()) {
                    ValueIntegerChange(actualMutable.id, array[0].toInt())
                } else {
                    // TODO validate why these are direct ids as a Long.
                    ValueIntegerExpressionChange(actualMutable.longId, actualValue.longId)
                }
            } else if (actualMutable is MutableRemoteFloat) {
                actualValue as RemoteFloat
                ValueFloatExpressionChange(actualMutable.id, actualValue.id)
            } else if (actualMutable is RemoteString) {
                actualValue as RemoteString
                ValueStringChange(actualMutable.id, actualValue.constantValue)
            } else {
                TODO("println unsupported type in ValueChange $actualMutable")
            }
        }
}

internal class ValueFloatChangeAction(
    public val value: MutableRemoteFloat,
    public val updatedValue: Float,
) : RemoteAction {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction =
        with(this as InternalRemoteStateScope) { ValueFloatChange(value.id, updatedValue) }
}

/**
 * Creates an [RemoteAction] that updates the value of a [MutableRemoteValue] to a new
 * [RemoteValue].
 *
 * Example of toggling a [MutableRemoteValue] boolean value on click:
 *
 * @sample androidx.compose.remote.creation.compose.samples.ClickableValueChangeSample
 * @param remoteValue The mutable remote value to be updated.
 * @param updatedValue The new remote value to apply.
 * @return An [RemoteAction] representing the value change.
 */
public fun <T> valueChangeAction(
    remoteValue: MutableRemoteValue<T>,
    updatedValue: RemoteValue<T>,
): RemoteAction = ValueChangeAction(remoteValue, updatedValue)
