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
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.MutableRemoteState
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.isLiteral

/** Update a value on click. */
internal class ValueChangeAction<T>(
    public val remoteValue: MutableRemoteState<T>,
    public val updatedValue: RemoteState<T>,
) : Action {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        return if (remoteValue is MutableRemoteInt) {
            updatedValue as RemoteInt
            val array = updatedValue.arrayForCreationState(this)

            if (array.isLiteral()) {
                ValueIntegerChange(remoteValue.id, array[0].toInt())
            } else {
                // TODO validate why these are direct ids as a Long.
                ValueIntegerExpressionChange(remoteValue.longId, updatedValue.longId)
            }
        } else if (remoteValue is MutableRemoteFloat) {
            updatedValue as RemoteFloat
            ValueFloatExpressionChange(remoteValue.id, updatedValue.id)
        } else if (remoteValue is RemoteString) {
            updatedValue as RemoteString
            ValueStringChange(remoteValue.id, updatedValue.constantValue)
        } else {
            TODO("println unsupported type in ValueChange $remoteValue")
        }
    }
}

internal class ValueFloatChangeAction(
    public val value: MutableRemoteFloat,
    public val updatedValue: Float,
) : Action {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val id = value.id
        return ValueFloatChange(id, updatedValue)
    }
}

internal class ValueFloatDpChangeAction(
    public val value: RemoteDp,
    public val updatedValue: Float,
) : Action {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val id = value.value.id
        return ValueFloatChange(id, updatedValue)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ValueChange(value: RemoteDp, updatedValue: Float): Action {
    return ValueFloatDpChangeAction(value, updatedValue)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ValueChange(value: RemoteDp, updatedValue: Int): Action {
    return ValueFloatDpChangeAction(value, updatedValue.toFloat())
}

/**
 * Creates an [Action] that updates the value of a [MutableRemoteState] to a new [RemoteState].
 *
 * @param remoteState The mutable remote state to be updated.
 * @param updatedValue The new remote state value to apply.
 * @return An [Action] representing the value change.
 */
public fun <T> ValueChange(
    remoteState: MutableRemoteState<T>,
    updatedValue: RemoteState<T>,
): Action = ValueChangeAction(remoteState, updatedValue)
