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
import androidx.compose.remote.creation.compose.modifier.UNSET_COMPONENT_ID
import androidx.compose.remote.creation.compose.modifier.prepareModifiers
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.document.RemoteCustomProperty
import androidx.compose.remote.document.mvf
import androidx.compose.runtime.Composable

internal class RemoteCustomComponentNode : RemoteComposeNode() {
    var name: String = ""
    var properties: RemoteCustomPropertiesScope.() -> Unit = {}

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(remoteCanvas)
        val writeModifiers = scope.prepareModifiers(modifier)
        val componentId =
            if (writeModifiers.componentId != UNSET_COMPONENT_ID) {
                writeModifiers.componentId
            } else {
                // TODO(b/590744731): allocateId() returns positive IDs that can collide with an
                // explicit componentId set on another component.
                creationState.document.allocateId()
            }
        val coreProperties = RemoteCustomPropertiesScope(creationState).apply(properties).properties
        creationState.document.startCustom(name, coreProperties, componentId) {
            writeModifiers(creationState.document)
        }
        renderChildren(creationState, remoteCanvas)
        creationState.document.endCustom()
    }
}

/**
 * Bridge exposing RemoteCompose Custom Components as `@RemoteComposable` components in the Compose
 * DSL.
 *
 * The [content] is recorded as child components of the Custom component, so a host plugin can
 * render them inside its own native implementation of [name] (e.g. as the label of a native button,
 * or the items of a native list).
 *
 * @param name Unique registered custom component name.
 * @param modifier High-level [RemoteModifier] to decorate the custom component.
 * @param content The remote children of the custom component.
 * @param properties Custom property definitions to pass to the custom component.
 */
// TODO(b/590744731): Address componentId collision with allocateId() before making this public.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@Suppress("ComposableLambdaParameterPosition")
public fun RemoteCustomComponent(
    name: String,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable @RemoteComposable () -> Unit = {},
    properties: RemoteCustomPropertiesScope.() -> Unit = {},
) {
    RemoteComposeNode(
        factory = ::RemoteCustomComponentNode,
        update = {
            set(name) { this.name = it }
            set(modifier) { this.modifier = it }
            set(properties) { this.properties = it }
        },
        content = content,
    )
}

/** An entry representing a single custom property or return binding. */
internal sealed class CustomPropertyEntry {
    abstract val id: Short
    open val state: RemoteValue<*>?
        get() = null

    abstract fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty
}

internal class IntPropertyEntry(
    override val id: Short,
    val value: Int,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(id, CustomProperty.INT_PROP, value)
}

internal class FloatPropertyEntry(
    override val id: Short,
    val value: Float,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(id, CustomProperty.FLOAT_PROP, value.mvf)
}

internal class StringPropertyEntry(
    override val id: Short,
    val value: String,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.STRING_PROP,
            creationState.document.addText(value).id,
        )
}

internal class BooleanPropertyEntry(
    override val id: Short,
    val value: Boolean,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(id, CustomProperty.INT_PROP, if (value) 1 else 0)
}

internal class RemoteFloatPropertyEntry(
    override val id: Short,
    override val state: RemoteFloat,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.FLOAT_PROP,
            creationState.getMaybeVariableFloat(state),
        )
}

internal class RemoteStringPropertyEntry(
    override val id: Short,
    override val state: RemoteString,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.STRING_PROP,
            creationState.getId(state),
        )
}

internal class RemoteIntPropertyEntry(
    override val id: Short,
    override val state: RemoteInt,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        if (state.hasConstantValue) {
            RemoteCustomProperty(id, CustomProperty.INT_PROP, state.constantValue)
        } else {
            RemoteCustomProperty(
                id,
                CustomProperty.INT_ID_PROP,
                creationState.getId(state),
            )
        }
}

internal class RemoteColorPropertyEntry(
    override val id: Short,
    override val state: RemoteColor,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        if (state.hasConstantValue) {
            RemoteCustomProperty(id, CustomProperty.COLOR_PROP, state.constantValue)
        } else {
            RemoteCustomProperty(
                id,
                CustomProperty.COLOR_ID_PROP,
                creationState.getId(state),
            )
        }
}

internal class RemoteBooleanPropertyEntry(
    override val id: Short,
    override val state: RemoteBoolean,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        if (state.hasConstantValue) {
            RemoteCustomProperty(id, CustomProperty.INT_PROP, if (state.constantValue) 1 else 0)
        } else {
            RemoteCustomProperty(
                id,
                CustomProperty.INT_ID_PROP,
                creationState.getId(state.toRemoteInt()),
            )
        }
}

internal class RemoteImageBitmapPropertyEntry(
    override val id: Short,
    override val state: RemoteImageBitmap,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.INT_PROP,
            creationState.getId(state),
        )
}

internal class RemoteFloatReturnEntry(
    override val id: Short,
    override val state: RemoteFloat,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.FLOAT_RETURN,
            creationState.getMaybeVariableFloat(state),
        )
}

internal class RemoteStringReturnEntry(
    override val id: Short,
    override val state: RemoteString,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.TEXT_RETURN,
            creationState.getId(state),
        )
}

internal class RemoteIntReturnEntry(
    override val id: Short,
    override val state: RemoteInt,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.INT_RETURN,
            creationState.getId(state),
        )
}

internal class RemoteColorReturnEntry(
    override val id: Short,
    override val state: RemoteColor,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.COLOR_RETURN,
            creationState.getId(state),
        )
}

internal class RemoteBooleanReturnEntry(
    override val id: Short,
    override val state: RemoteBoolean,
) : CustomPropertyEntry() {
    override fun toCustomProperty(creationState: RemoteComposeCreationState): RemoteCustomProperty =
        RemoteCustomProperty(
            id,
            CustomProperty.INT_RETURN,
            creationState.getId(state.toRemoteInt()),
        )
}

/** Scope for configuring properties and return bindings of a [RemoteCustomComponent]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("ComposableLambdaParameterPosition")
public class RemoteCustomPropertiesScope
public constructor(private val creationState: RemoteComposeCreationState? = null) {
    internal val entries = mutableListOf<CustomPropertyEntry>()

    internal val properties: List<RemoteCustomProperty>
        get() {
            val state =
                creationState ?: error("creationState required to evaluate properties eagerly")
            return List(entries.size) { entries[it].toCustomProperty(state) }
        }

    public constructor() : this(null)

    public fun property(id: Int, value: Int) {
        entries.add(IntPropertyEntry(id.toShort(), value))
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

    /**
     * Adds the bitmap [value] to the document and passes its id as an int property. A host plugin
     * can resolve it with `LocalRcImageLoader`.
     */
    public fun property(id: Int, value: RemoteImageBitmap) {
        entries.add(RemoteImageBitmapPropertyEntry(id.toShort(), value))
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
