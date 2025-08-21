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
package androidx.compose.remote.frontend.action

import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.frontend.state.FallbackCreationState
import androidx.compose.remote.frontend.state.MutableRemoteInt
import androidx.compose.remote.frontend.state.MutableRemoteString
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable

/** Run the named host action when invoked. */
class HostAction(val name: String, val type: Type = Type.INT, var id: Int = -1) : Action {

    enum class Type(val value: Int) {
        FLOAT(HostNamedActionOperation.FLOAT_TYPE),
        INT(HostNamedActionOperation.INT_TYPE),
        STRING(HostNamedActionOperation.STRING_TYPE),
        FLOAT_ARRAY(HostNamedActionOperation.FLOAT_ARRAY_TYPE),
        NONE(HostNamedActionOperation.NONE_TYPE),
    }

    // TODO: Add a RemoteFloatArray type and use it here!
    constructor(
        name: String,
        value: RemoteFloat,
        type: Type = Type.FLOAT,
    ) : this(name, type, value.getIdForCreationState(FallbackCreationState.state))

    constructor(
        name: String,
        value: MutableRemoteInt,
    ) : this(name, Type.INT, value.getIdForCreationState(FallbackCreationState.state))

    constructor(
        name: String,
        value: MutableRemoteString,
    ) : this(name, Type.STRING, value.getIdForCreationState(FallbackCreationState.state))

    override fun toRemoteAction(): androidx.compose.remote.creation.actions.Action {
        return HostAction(name, type.ordinal, id)
    }

    @Composable override fun toComposeUiAction(): () -> Unit = {}
}
