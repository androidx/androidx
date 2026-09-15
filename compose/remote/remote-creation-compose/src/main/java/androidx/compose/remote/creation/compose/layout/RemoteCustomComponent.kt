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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.managers.Custom.CustomProperty
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.BaseRemoteState
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
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
@Suppress("RestrictedApi")
public fun RemoteCustomComponent(
    name: String,
    modifier: RemoteModifier = RemoteModifier,
    properties: RemoteCustomPropertiesScope.() -> Unit = {},
) {
    RemoteCanvas(modifier = modifier) {
        remoteCanvas.internalCanvas.custom(
            config = name,
            modifier = modifier,
            properties = properties,
        )
    }
}

/** An entry representing a single custom property or return binding. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class CustomPropertyEntry {
    public abstract val id: Short
    public open val state: BaseRemoteState<*>?
        get() = null

    public abstract fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty
}

internal class IntPropertyEntry(
    override val id: Short,
    val value: Int,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.INT_PROP, value)
}

internal class ColorPropertyEntry(
    override val id: Short,
    val color: Color,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.COLOR_PROP, color.toArgb())
}

internal class FloatPropertyEntry(
    override val id: Short,
    val value: Float,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.FLOAT_PROP, value)
}

internal class StringPropertyEntry(
    override val id: Short,
    val value: String,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.STRING_PROP, creationState.document.addText(value))
}

internal class BooleanPropertyEntry(
    override val id: Short,
    val value: Boolean,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.INT_PROP, if (value) 1 else 0)
}

internal class RemoteFloatPropertyEntry(
    override val id: Short,
    override val state: RemoteFloat,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(
            id,
            CustomProperty.FLOAT_PROP,
            state.getFloatIdForCreationState(creationState),
        )
}

internal class RemoteStringPropertyEntry(
    override val id: Short,
    override val state: RemoteString,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.STRING_PROP, state.getIdForCreationState(creationState))
}

internal class RemoteIntPropertyEntry(
    override val id: Short,
    override val state: RemoteInt,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        if (state.hasConstantValue) {
            CustomProperty(id, CustomProperty.INT_PROP, state.constantValue)
        } else {
            CustomProperty(
                id,
                CustomProperty.INT_ID_PROP,
                state.getIdForCreationState(creationState),
            )
        }
}

internal class RemoteColorPropertyEntry(
    override val id: Short,
    override val state: RemoteColor,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        if (state.hasConstantValue) {
            CustomProperty(id, CustomProperty.COLOR_PROP, state.constantValue.toArgb())
        } else {
            CustomProperty(
                id,
                CustomProperty.COLOR_ID_PROP,
                state.getIdForCreationState(creationState),
            )
        }
}

internal class RemoteBooleanPropertyEntry(
    override val id: Short,
    override val state: RemoteBoolean,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        if (state.hasConstantValue) {
            CustomProperty(id, CustomProperty.INT_PROP, if (state.constantValue) 1 else 0)
        } else {
            CustomProperty(
                id,
                CustomProperty.INT_ID_PROP,
                state.toRemoteInt().getIdForCreationState(creationState),
            )
        }
}

internal class RemoteFloatReturnEntry(
    override val id: Short,
    override val state: RemoteFloat,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(
            id,
            CustomProperty.FLOAT_RETURN,
            state.getFloatIdForCreationState(creationState),
        )
}

internal class RemoteStringReturnEntry(
    override val id: Short,
    override val state: RemoteString,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.TEXT_RETURN, state.getIdForCreationState(creationState))
}

internal class RemoteIntReturnEntry(
    override val id: Short,
    override val state: RemoteInt,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.INT_RETURN, state.getIdForCreationState(creationState))
}

internal class RemoteColorReturnEntry(
    override val id: Short,
    override val state: RemoteColor,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(id, CustomProperty.COLOR_RETURN, state.getIdForCreationState(creationState))
}

internal class RemoteBooleanReturnEntry(
    override val id: Short,
    override val state: RemoteBoolean,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): CustomProperty =
        CustomProperty(
            id,
            CustomProperty.INT_RETURN,
            state.toRemoteInt().getIdForCreationState(creationState),
        )
}

/** Scope for configuring properties and return bindings of a [RemoteCustomComponent]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApi")
public class RemoteCustomPropertiesScope
public constructor(private val creationState: RemoteComposeCreationState? = null) {
    internal val entries = mutableListOf<CustomPropertyEntry>()

    internal val properties: List<CustomProperty>
        get() {
            val state =
                creationState ?: error("creationState required to evaluate properties eagerly")
            return List(entries.size) { entries[it].toCustomProperty(state) }
        }

    public constructor() : this(null)

    public fun property(id: Int, value: Int) {
        entries.add(IntPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: Color) {
        entries.add(ColorPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: Float) {
        entries.add(FloatPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: String) {
        entries.add(StringPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: Boolean) {
        entries.add(BooleanPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: RemoteFloat) {
        entries.add(RemoteFloatPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: RemoteDp) {
        property(id, value.toPx())
    }

    public fun property(id: Int, value: RemoteString) {
        entries.add(RemoteStringPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: RemoteInt) {
        entries.add(RemoteIntPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: RemoteColor) {
        entries.add(RemoteColorPropertyEntry(id.toShort(), value))
    }

    public fun property(id: Int, value: RemoteBoolean) {
        entries.add(RemoteBooleanPropertyEntry(id.toShort(), value))
    }

    public fun bindReturn(id: Int, state: RemoteString?) {
        if (state == null) return
        entries.add(RemoteStringReturnEntry(id.toShort(), state))
    }

    public fun bindReturn(id: Int, state: RemoteFloat?) {
        if (state == null) return
        entries.add(RemoteFloatReturnEntry(id.toShort(), state))
    }

    public fun bindReturn(id: Int, state: RemoteInt?) {
        if (state == null) return
        entries.add(RemoteIntReturnEntry(id.toShort(), state))
    }

    public fun bindReturn(id: Int, state: RemoteColor?) {
        if (state == null) return
        entries.add(RemoteColorReturnEntry(id.toShort(), state))
    }

    public fun bindReturn(id: Int, state: RemoteBoolean?) {
        if (state == null) return
        entries.add(RemoteBooleanReturnEntry(id.toShort(), state))
    }
}
