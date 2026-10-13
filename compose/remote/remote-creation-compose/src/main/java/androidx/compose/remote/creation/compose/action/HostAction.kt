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
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.actions.HostAction as CreationHostAction
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString

/**
 * Creates an [Action] that triggers a named action on the host.
 *
 * Named actions are delivered to the `onNamedAction` callback of the Compose players. [name] may be
 * a constant or a dynamic [RemoteString]; a dynamic name is resolved to its current value when the
 * action runs.
 *
 * @param name The name of the action.
 */
public fun hostAction(name: RemoteString): Action = HostAction(name, HostAction.Type.NONE)

/**
 * Creates an [Action] that triggers a named action on the host with a float value.
 *
 * @param name The name of the action.
 * @param value The float value to pass with the action, delivered to the host as a [Float].
 */
public fun hostAction(name: RemoteString, value: RemoteFloat): Action = HostAction(name, value)

/**
 * Creates an [Action] that triggers a named action on the host with an int value.
 *
 * @param name The name of the action.
 * @param value The int value to pass with the action, delivered to the host as an [Int].
 */
public fun hostAction(name: RemoteString, value: RemoteInt): Action = HostAction(name, value)

/**
 * Creates an [Action] that triggers a named action on the host with a string value.
 *
 * @param name The name of the action.
 * @param value The string value to pass with the action, delivered to the host as a [String].
 */
public fun hostAction(name: RemoteString, value: RemoteString): Action = HostAction(name, value)

/** Run the named host action when invoked. */
internal class HostAction(
    public val name: RemoteString,
    public val type: Type = Type.NONE,
    public val value: RemoteState<*>? = null,
) : RemoteAction() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public enum class Type(public val value: Int) {
        FLOAT(HostNamedActionOperation.FLOAT_TYPE),
        INT(HostNamedActionOperation.INT_TYPE),
        STRING(HostNamedActionOperation.STRING_TYPE),
        FLOAT_ARRAY(HostNamedActionOperation.FLOAT_ARRAY_TYPE),
        NONE(HostNamedActionOperation.NONE_TYPE),
    }

    // TODO: Add a RemoteFloatArray type and use it here!
    public constructor(name: RemoteString, value: RemoteFloat) : this(name, Type.FLOAT, value)

    public constructor(name: RemoteString, value: RemoteInt) : this(name, Type.INT, value)

    public constructor(name: RemoteString, value: RemoteString) : this(name, Type.STRING, value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val valueId = value?.id ?: -1
        val constantName = name.constantValueOrNull
        if (constantName != null) {
            return CreationHostAction(constantName, type.value, valueId)
        }
        // A dynamic name is referenced by its text id, which players resolve to the current value
        // when the action runs. This keeps the action a named action (with its typed value)
        // rather than an id action keyed on an unstable, writer-allocated variable id.
        val nameId = name.id
        val typeValue = type.value
        return CreationAction { writer ->
            HostNamedActionOperation.apply(writer.buffer.buffer, nameId, typeValue, valueId)
        }
    }
}

/** Run the host action with a stable integer [id], and optional [metadata], when invoked. */
internal class HostIdAction(public val id: Int, public val metadata: RemoteString? = null) :
    RemoteAction() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val metadataId = metadata?.id
        val actionId = id
        return CreationAction { writer ->
            if (metadataId == null) {
                HostActionOperation.apply(writer.buffer.buffer, actionId)
            } else {
                HostActionMetadataOperation.apply(writer.buffer.buffer, actionId, metadataId)
            }
        }
    }
}
