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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.action

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.compose.state.FallbackCreationState
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString

/** Run the named host action when invoked. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HostAction(
    public val name: RemoteString,
    public val type: Type = Type.INT,
    public var id: Int = -1,
) : Action {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public enum class Type(public val value: Int) {
        FLOAT(HostNamedActionOperation.FLOAT_TYPE),
        INT(HostNamedActionOperation.INT_TYPE),
        STRING(HostNamedActionOperation.STRING_TYPE),
        FLOAT_ARRAY(HostNamedActionOperation.FLOAT_ARRAY_TYPE),
        NONE(HostNamedActionOperation.NONE_TYPE),
    }

    // TODO: Add a RemoteFloatArray type and use it here!
    public constructor(
        name: RemoteString,
        value: RemoteFloat,
    ) : this(name, Type.FLOAT, value.getIdForCreationState(FallbackCreationState.state))

    public constructor(
        name: RemoteString,
        value: RemoteInt,
    ) : this(name, Type.INT, value.getIdForCreationState(FallbackCreationState.state))

    public constructor(
        name: RemoteString,
        value: RemoteString,
    ) : this(name, Type.STRING, value.getIdForCreationState(FallbackCreationState.state))

    override fun toRemoteAction(): androidx.compose.remote.creation.actions.Action {
        val constantValue = name.constantValue
        return if (constantValue != null) {
            HostAction(constantValue, type.ordinal, id)
        } else {
            HostAction(name.getIdForCreationState(FallbackCreationState.state), id)
        }
    }
}
