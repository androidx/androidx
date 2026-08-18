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

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.managers.Custom.CustomProperty
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Bridge exposing RemoteCompose Custom Components as `@RemoteComposable` components in the Compose
 * DSL.
 *
 * @param modifier High-level [RemoteModifier] to decorate the custom component container.
 * @param name Unique registered custom component name.
 * @param properties Custom property definitions to pass to the custom component.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@SuppressLint("RestrictedApi")
public fun RemoteCustomComponent(
    name: String,
    modifier: RemoteModifier = RemoteModifier,
    properties: RemoteCustomPropertiesScope.() -> Unit = {},
) {
    val creationState = LocalRemoteComposeCreationState.current
    RemoteCanvas {
        val recordingModifier = remoteCanvas.toRecordingModifier(modifier)
        val writer = remoteCanvas.document
        if (recordingModifier.componentId == -1) {
            recordingModifier.componentId(writer.nextId())
        }
        val scope = RemoteCustomPropertiesScope(creationState).apply(properties)
        writer.startCustom(recordingModifier, name, scope.properties)
        writer.endCustom()
    }
}

/** Scope for configuring properties and return bindings of a [RemoteCustomComponent]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApi")
public class RemoteCustomPropertiesScope
internal constructor(private val creationState: RemoteComposeCreationState) {
    internal val properties = mutableListOf<CustomProperty>()

    public fun property(id: Int, value: Int) {
        properties.add(CustomProperty(id.toShort(), CustomProperty.INT_PROP, value))
    }

    public fun property(id: Int, color: Color) {
        property(id, color.toArgb())
    }

    public fun property(id: Int, value: RemoteFloat) {
        with(creationState) {
            properties.add(CustomProperty(id.toShort(), CustomProperty.FLOAT_PROP, value.floatId))
        }
    }

    public fun property(id: Int, value: RemoteDp) {
        property(id, value.toPx())
    }

    public fun property(id: Int, value: RemoteString) {
        with(creationState) {
            properties.add(CustomProperty(id.toShort(), CustomProperty.STRING_PROP, value.id))
        }
    }

    public fun bindReturn(id: Int, state: RemoteString?) {
        if (state == null) return
        with(creationState) {
            properties.add(CustomProperty(id.toShort(), CustomProperty.TEXT_RETURN, state.id))
        }
    }

    public fun bindReturn(id: Int, state: RemoteFloat?) {
        if (state == null) return
        with(creationState) {
            properties.add(CustomProperty(id.toShort(), CustomProperty.FLOAT_RETURN, state.floatId))
        }
    }
}
