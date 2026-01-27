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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString

/** Run the named host action when invoked. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HostAction(
    public val name: RemoteString,
    public val type: Type = Type.INT,
    public val id: Int = 0,
    public val value: RemoteState<*>? = null,
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
    public constructor(name: RemoteString, value: RemoteFloat) : this(name, Type.FLOAT, 0, value)

    public constructor(name: RemoteString, value: RemoteInt) : this(name, Type.INT, 0, value)

    public constructor(name: RemoteString, value: RemoteString) : this(name, Type.STRING, 0, value)

    public constructor(
        id: Int,
        name: RemoteString,
        value: RemoteString,
    ) : this(name, Type.STRING, id, value)

    override fun RemoteStateScope.toRemoteAction():
        androidx.compose.remote.creation.actions.Action {
        val valueId = value?.id ?: -1
        val constantValue = name.constantValueOrNull
        if (id != 0) {
            return HostAction(id, valueId)
        }
        return if (constantValue != null) {
            HostAction(constantValue, type.ordinal, valueId)
        } else {
            HostAction(name.id, valueId)
        }
    }
}
