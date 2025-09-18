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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.action

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.frontend.state.FallbackCreationState
import androidx.compose.remote.frontend.state.MutableRemoteFloat
import androidx.compose.remote.frontend.state.MutableRemoteInt
import androidx.compose.remote.frontend.state.MutableRemoteState
import androidx.compose.remote.frontend.state.MutableRemoteString
import androidx.compose.remote.frontend.state.RemoteDp
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteInt
import androidx.compose.remote.frontend.state.RemoteState
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.remote.frontend.state.isLiteral
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp

// TODO fix up types after RemoteType refactor
/** Update a value on click. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ValueChangeAction<T>(
    public val remoteValue: MutableRemoteState<T>,
    public val updatedValue: RemoteState<T>,
) : androidx.compose.remote.frontend.action.Action {
    public override fun toRemoteAction(): Action {
        return if (remoteValue is MutableRemoteInt) {
            updatedValue as RemoteInt
            val array = updatedValue.arrayForCreationState(FallbackCreationState.state)

            if (array.isLiteral()) {
                ValueIntegerChange(
                    remoteValue.getIdForCreationState(FallbackCreationState.state),
                    array[0].toInt(),
                )
            } else {
                // TODO validate why these are direct ids as a Long.
                ValueIntegerExpressionChange(
                    remoteValue.getIdForCreationState(FallbackCreationState.state).toLong(),
                    updatedValue.getIdForCreationState(FallbackCreationState.state).toLong(),
                )
            }
        } else if (remoteValue is MutableRemoteFloat) {
            updatedValue as RemoteFloat
            ValueFloatExpressionChange(
                remoteValue.getIdForCreationState(FallbackCreationState.state),
                updatedValue.getIdForCreationState(FallbackCreationState.state),
            )
        } else if (remoteValue is RemoteString) {
            updatedValue as RemoteString
            ValueStringChange(
                remoteValue.getIdForCreationState(FallbackCreationState.state),
                updatedValue.value,
            )
        } else {
            TODO("println unsupported type in ValueChange $remoteValue")
        }
    }

    @Composable
    public override fun toComposeUiAction(): () -> Unit {
        return {
            println("Updating $remoteValue to $updatedValue")
            remoteValue.value = updatedValue.value
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ValueFloatChangeAction(
    public val value: MutableState<RemoteFloat>,
    public val updatedValue: Float,
) : androidx.compose.remote.frontend.action.Action {
    public override fun toRemoteAction(): Action {
        val id = Utils.idFromNan(value.value.internalAsFloat())
        return ValueFloatChange(id, updatedValue)
    }

    @Composable
    public override fun toComposeUiAction(): () -> Unit {
        return { println("Updating RemoteFloat $value to $updatedValue") }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ValueFloatDpChangeAction(public val value: RemoteDp, public val updatedValue: Float) :
    androidx.compose.remote.frontend.action.Action {
    public override fun toRemoteAction(): Action {
        val id = Utils.idFromNan(value.value.internalAsFloat())
        return ValueFloatChange(id, updatedValue)
    }

    @Composable
    public override fun toComposeUiAction(): () -> Unit {
        return { println("Updating RemoteFloat $value to $updatedValue") }
    }
}

public fun ValueChange(
    value: MutableRemoteFloat,
    updatedValue: Float,
): androidx.compose.remote.frontend.action.Action {
    return ValueChangeAction<Float>(value, RemoteFloat(updatedValue))
}

public fun ValueChange(
    value: MutableRemoteFloat,
    updatedValue: RemoteFloat,
): androidx.compose.remote.frontend.action.Action {
    return ValueChangeAction<Float>(value, updatedValue)
}

public fun ValueChange(
    value: RemoteDp,
    updatedValue: Float,
): androidx.compose.remote.frontend.action.Action {
    return ValueFloatDpChangeAction(value, updatedValue)
}

public fun ValueChange(
    value: RemoteDp,
    updatedValue: Int,
): androidx.compose.remote.frontend.action.Action {
    return ValueFloatDpChangeAction(value, updatedValue.toFloat())
}

public fun ValueChange(
    value: RemoteDp,
    updatedValue: Dp,
): androidx.compose.remote.frontend.action.Action {
    return ValueFloatDpChangeAction(value, updatedValue.value)
}

public fun ValueChange(remoteState: MutableRemoteInt, updatedValue: Int): ValueChangeAction<Int> =
    ValueChangeAction<Int>(remoteState, MutableRemoteInt(mutableIntStateOf(updatedValue)))

public fun ValueChange(
    remoteState: MutableRemoteInt,
    updatedValue: RemoteInt,
): ValueChangeAction<Int> = ValueChangeAction<Int>(remoteState, updatedValue)

public fun ValueChange(
    remoteState: MutableRemoteString,
    updatedValue: String,
): ValueChangeAction<String> =
    ValueChangeAction<String>(remoteState, MutableRemoteString(mutableStateOf(updatedValue)))
