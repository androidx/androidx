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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.player.compose.embedded.modifier.ClickActionHandlers
import androidx.compose.remote.player.compose.embedded.modifier.rememberClickActionHandlers
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteColorAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach

/** Base interface for custom component property schemas. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CustomPropertyKey {
    /** The author-assigned property type ID. */
    public val id: Int
}

/**
 * Defines a float property schema encoding its author-assigned property type ID and default value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class FloatProperty(override val id: Int, public val default: Float = 0f) :
    CustomPropertyKey {
    public constructor(id: Short, default: Float = 0f) : this(id.toInt(), default)
}

/**
 * Defines an integer property schema encoding its author-assigned property type ID and default
 * value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class IntProperty(override val id: Int, public val default: Int = 0) :
    CustomPropertyKey {
    public constructor(id: Short, default: Int = 0) : this(id.toInt(), default)
}

/**
 * Defines a string property schema encoding its author-assigned property type ID and default value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class StringProperty(override val id: Int, public val default: String = "") :
    CustomPropertyKey {
    public constructor(id: Short, default: String = "") : this(id.toInt(), default)
}

/**
 * Defines a color property schema encoding its author-assigned property type ID and default value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class ColorProperty(
    override val id: Int,
    public val default: Color = Color.Unspecified,
) : CustomPropertyKey {
    public constructor(id: Short, default: Color = Color.Unspecified) : this(id.toInt(), default)
}

/** Defines a float return-channel property schema encoding its author-assigned property type ID. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class FloatReturnProperty(override val id: Int) : CustomPropertyKey {
    public constructor(id: Short) : this(id.toInt())
}

/** Defines a text return-channel property schema encoding its author-assigned property type ID. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class TextReturnProperty(override val id: Int) : CustomPropertyKey {
    public constructor(id: Short) : this(id.toInt())
}

/**
 * Interface for reading custom component properties on-demand using typed property keys as Compose
 * [State].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RcCustomPropertyReader {
    /** Returns `true` if [property] is present. */
    public fun hasProperty(property: CustomPropertyKey): Boolean

    /** Reads [property] reactively as a Compose [State]. */
    @Composable public fun floatState(property: FloatProperty): State<Float>

    /** Reads [property] reactively as a Compose [State]. */
    @Composable public fun intState(property: IntProperty): State<Int>

    /** Reads [property] reactively as a Compose [State]. */
    @Composable public fun textState(property: StringProperty): State<String>

    /** Reads [property] reactively as a Compose [State]. */
    @Composable public fun colorState(property: ColorProperty): State<Color>

    /** Returns a handler lambda that writes text back to return channel [property]. */
    public fun returnTextHandler(property: TextReturnProperty): (String) -> Unit

    /** Returns a handler lambda that writes a float back to return channel [property]. */
    public fun returnFloatHandler(property: FloatReturnProperty): (Float) -> Unit
}

/**
 * A `Custom` (host-extension) component from a document: a config name plus its properties.
 * `Custom` components have no built-in rendering — the host app supplies it, dispatched by
 * [config].
 *
 * Properties are resolved on-demand reactively via [floatState], [intState], [textState], and
 * [colorState].
 *
 * A `Custom` component may also host remote child components (see [childCount], [Child], [Children]
 * and [customChild]), letting a plugin place document content inside a native component, e.g. as
 * the label of a native button.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcCustomComponent
internal constructor(
    public val config: String,
    public val componentId: Int,
    private val rawProperties: List<Custom.CustomProperty>,
    private val remoteContext: RemoteContext,
    private val children: List<Component> = emptyList(),
    /**
     * Runs the component's single-click actions, or `null` if it has none. Only populated for
     * plugins that opt in via [CustomComposablePlugin.handlesClick].
     */
    public val onClick: (() -> Unit)? = null,
    /**
     * Runs the component's double-click actions, or `null` if it has none. Only populated for
     * plugins that opt in via [CustomComposablePlugin.handlesClick].
     */
    public val onDoubleClick: (() -> Unit)? = null,
    /**
     * Runs the component's long-click actions, or `null` if it has none. Only populated for plugins
     * that opt in via [CustomComposablePlugin.handlesClick].
     */
    public val onLongClick: (() -> Unit)? = null,
) : RcCustomPropertyReader {

    /** The number of remote child components hosted by this component. */
    public val childCount: Int
        get() = children.size

    /**
     * Renders the remote child at [index] in the current layout scope, or nothing if out of range.
     */
    @Composable
    public fun Child(index: Int, modifier: Modifier = Modifier) {
        val child = children.getOrNull(index) ?: return
        RcPlayerComponent(child, modifier)
    }

    /** Renders all remote children, in order, in the current layout scope. */
    @Composable
    public fun Children() {
        children.fastForEach { RcPlayerComponent(it) }
    }

    /**
     * Returns the remote child at [index] as an [RcCustomComponent] if it is itself a `Custom`
     * component (e.g. a named slot), otherwise `null` (including when [index] is out of range). Its
     * children can then be rendered directly via [Children] without dispatching it through the
     * plugin registry.
     */
    public fun customChild(index: Int): RcCustomComponent? {
        val child = children.getOrNull(index) as? Custom ?: return null
        val data = child.readData()
        val childConfig =
            data.config ?: if (data.configId != -1) remoteContext.getText(data.configId) else null
        @Suppress("UNCHECKED_CAST")
        val properties = data.properties as? List<Custom.CustomProperty> ?: emptyList()
        return RcCustomComponent(
            config = childConfig ?: "",
            componentId = child.componentId,
            rawProperties = properties,
            remoteContext = remoteContext,
            children = child.childrenComponents,
        )
    }

    /** Returns the raw [Custom.CustomProperty] for [property], if present. */
    public fun findProperty(property: CustomPropertyKey): Custom.CustomProperty? {
        val id = property.id
        for (i in 0 until rawProperties.size) {
            val prop = rawProperties[i]
            if (prop.mType.toInt() == id) return prop
        }
        return null
    }

    override fun hasProperty(property: CustomPropertyKey): Boolean = findProperty(property) != null

    @Composable
    override fun floatState(property: FloatProperty): State<Float> {
        val prop = findProperty(property) ?: return rememberUpdatedState(property.default)
        return rememberRemoteFloatAsState(prop.mFloatValue)
    }

    @Composable
    override fun intState(property: IntProperty): State<Int> {
        val prop = findProperty(property) ?: return rememberUpdatedState(property.default)
        return if (prop.mDataType == Custom.CustomProperty.INT_ID_PROP) {
            rememberRemoteIntAsState(prop.mIntValue)
        } else {
            rememberUpdatedState(prop.mIntValue)
        }
    }

    @Composable
    override fun textState(property: StringProperty): State<String> {
        val prop = findProperty(property) ?: return rememberUpdatedState(property.default)
        return rememberRemoteStringAsState(prop.mIntValue)
    }

    @Composable
    override fun colorState(property: ColorProperty): State<Color> {
        val prop = findProperty(property) ?: return rememberUpdatedState(property.default)
        return if (prop.mDataType == Custom.CustomProperty.COLOR_ID_PROP) {
            rememberRemoteColorAsState(prop.mIntValue)
        } else {
            rememberUpdatedState(Color(prop.mIntValue))
        }
    }

    override fun returnFloatHandler(property: FloatReturnProperty): (Float) -> Unit = { value ->
        val prop = findProperty(property)
        if (prop != null && prop.mDataType == Custom.CustomProperty.FLOAT_RETURN) {
            val targetId = Utils.idFromNan(prop.mFloatValue)
            remoteContext.overrideFloat(targetId, value)
        }
    }

    override fun returnTextHandler(property: TextReturnProperty): (String) -> Unit = { value ->
        val prop = findProperty(property)
        if (prop != null && prop.mDataType == Custom.CustomProperty.TEXT_RETURN) {
            val targetId = prop.mIntValue
            remoteContext.loadText(targetId, value)
        }
    }

    public val floatReturns: Set<Int>
        get() {
            val set = HashSet<Int>()
            for (i in 0 until rawProperties.size) {
                val prop = rawProperties[i]
                if (prop.mDataType == Custom.CustomProperty.FLOAT_RETURN) {
                    set.add(prop.mType.toInt())
                }
            }
            return set
        }

    public val textReturns: Set<Int>
        get() {
            val set = HashSet<Int>()
            for (i in 0 until rawProperties.size) {
                val prop = rawProperties[i]
                if (prop.mDataType == Custom.CustomProperty.TEXT_RETURN) {
                    set.add(prop.mType.toInt())
                }
            }
            return set
        }

    /** Eager map of float properties for backwards compatibility. */
    public val floats: Map<Int, Float>
        get() {
            val map = HashMap<Int, Float>()
            for (i in 0 until rawProperties.size) {
                val it = rawProperties[i]
                if (it.isFloat()) {
                    val value = it.mFloatValue
                    val resolved =
                        if (value.isNaN()) {
                            remoteContext.getFloat(Utils.idFromNan(value))
                        } else {
                            value
                        }
                    map[it.mType.toInt()] = resolved
                }
            }
            return map
        }

    /** Eager map of int properties for backwards compatibility. */
    public val ints: Map<Int, Int>
        get() {
            val map = HashMap<Int, Int>()
            for (i in 0 until rawProperties.size) {
                val it = rawProperties[i]
                if (!it.isFloat() && !it.isString()) {
                    map[it.mType.toInt()] = it.mIntValue
                }
            }
            return map
        }

    /** Eager map of text properties for backwards compatibility. */
    public val texts: Map<Int, String>
        get() {
            val map = HashMap<Int, String>()
            for (i in 0 until rawProperties.size) {
                val it = rawProperties[i]
                if (it.isString()) {
                    map[it.mType.toInt()] = remoteContext.getText(it.mIntValue) ?: ""
                }
            }
            return map
        }
}

/**
 * A type-safe plugin for rendering custom components in Remote Compose.
 *
 * @param T The strongly-typed data payload extracted from [RcCustomComponent].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CustomComposablePlugin<T : Any> {
    /**
     * The config name key identifying custom components handled by this plugin (e.g.
     * `"support:edit-text"`).
     */
    public val name: String

    /**
     * Extracts strongly-typed data [T] from [component] if supported. Returns `null` if this plugin
     * does not handle [component] or if required properties are missing.
     */
    @Composable public fun extract(component: RcCustomComponent): T?

    /** Renders the custom component UI using the extracted data [data]. */
    @Composable public fun Content(data: T, component: RcCustomComponent, modifier: Modifier)

    /**
     * Whether this plugin dispatches the component's clicks itself via [RcCustomComponent.onClick],
     * [RcCustomComponent.onDoubleClick], and [RcCustomComponent.onLongClick] (e.g. from a native
     * button). When `true`, the player does not apply the component's click modifiers to the
     * wrapping layout.
     */
    public val handlesClick: Boolean
        get() = false
}

/**
 * A registry of [CustomComposablePlugin] instances used to dispatch rendering for custom
 * components.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CustomPluginRegistry(private val plugins: Map<String, CustomComposablePlugin<*>>) {
    public constructor(
        vararg plugins: CustomComposablePlugin<*>
    ) : this(
        HashMap<String, CustomComposablePlugin<*>>().apply {
            for (i in 0 until plugins.size) {
                val p = plugins[i]
                put(p.name, p)
            }
        }
    )

    public constructor(
        plugins: List<CustomComposablePlugin<*>>
    ) : this(
        HashMap<String, CustomComposablePlugin<*>>().apply {
            for (i in 0 until plugins.size) {
                val p = plugins[i]
                put(p.name, p)
            }
        }
    )

    /**
     * Renders [component] using the matching plugin in this registry for `component.config`.
     *
     * @return `true` if a plugin handled [component], `false` otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    @Composable
    public fun Render(component: RcCustomComponent, modifier: Modifier = Modifier) {
        val plugin = plugins[component.config] as? CustomComposablePlugin<Any> ?: return
        val data = plugin.extract(component) ?: return
        plugin.Content(data, component, modifier)
    }

    /** Whether the plugin registered for [config] handles clicks itself. */
    internal fun handlesClick(config: String): Boolean = plugins[config]?.handlesClick == true
}

/**
 * Composition local for the [CustomPluginRegistry] used to render [Custom] components. Provided by
 * [RcPlayer]'s `customPlugins` parameter.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRcCustomPlugins: ProvidableCompositionLocal<CustomPluginRegistry?> =
    compositionLocalOf {
        null
    }

/** Resolves the config name of a [Custom] component. */
@Composable
private fun rememberCustomConfig(layout: Custom): String {
    val data = layout.readData()
    return if (data.configId != -1) {
        rememberRemoteStringAsState(data.configId).value
    } else {
        data.config ?: ""
    }
}

/**
 * Returns the click handlers for [layout] if its plugin handles clicks itself (see
 * [CustomComposablePlugin.handlesClick]), otherwise `null`. When non-null, the player must not also
 * apply the component's click modifiers, and each individual handler on [ClickActionHandlers] is
 * `null` if the component has no actions for that gesture.
 */
@Composable
internal fun rememberCustomClickHandlers(layout: Custom): ClickActionHandlers? {
    val customPlugins = LocalRcCustomPlugins.current ?: return null
    val config = rememberCustomConfig(layout)
    if (!customPlugins.handlesClick(config)) return null
    val clickOps =
        layout.componentModifiers.list.fastFilter {
            it is ClickModifierOperation || it is MultiClickModifier
        }
    return rememberClickActionHandlers(clickOps)
}

/** Renders a [Custom] component by delegating to the host's [LocalRcCustomPlugins] registry. */
@Composable
internal fun RcPlayerCustom(
    layout: Custom,
    modifier: Modifier,
    clickHandlers: ClickActionHandlers? = null,
) {
    val remoteContext = LocalRemoteContext.current
    val customPlugins = LocalRcCustomPlugins.current

    val data = layout.readData()
    val config = rememberCustomConfig(layout)

    @Suppress("UNCHECKED_CAST")
    val properties = data.properties as? List<Custom.CustomProperty> ?: emptyList()

    Box(modifier = modifier) {
        val component =
            RcCustomComponent(
                config = config,
                componentId = layout.componentId,
                rawProperties = properties,
                remoteContext = remoteContext,
                children = layout.childrenComponents,
                onClick = clickHandlers?.onClick,
                onDoubleClick = clickHandlers?.onDoubleClick,
                onLongClick = clickHandlers?.onLongClick,
            )
        customPlugins?.Render(component, modifier = Modifier)
    }
}

private fun Custom.CustomProperty.isFloat(): Boolean =
    mDataType == Custom.CustomProperty.FLOAT_PROP || mDataType == Custom.CustomProperty.FLOAT_RETURN

private fun Custom.CustomProperty.isString(): Boolean =
    mDataType == Custom.CustomProperty.STRING_PROP || mDataType == Custom.CustomProperty.TEXT_RETURN
