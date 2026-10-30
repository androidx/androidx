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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.CLEAR_AND_SET
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.MERGE
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode.SET
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.SemanticsModifier as CreationSemanticsModifier
import androidx.compose.ui.semantics.Role

/**
 * [RemoteSemanticsPropertyKey] is the infrastructure for setting key/value pairs inside semantics
 * block in a type-safe way. Each key has one particular statically defined value type T.
 */
public class RemoteSemanticsPropertyKey<T>
internal constructor(
    /** The name of the property. Should be the same as the constant from which it is accessed. */
    public val name: String
) {
    override fun toString(): String {
        return "RemoteSemanticsPropertyKey: $name"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteSemanticsPropertyKey<*>) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

/**
 * [RemoteSemanticsPropertyReceiver] is the scope provided by semantics {} blocks, letting you set
 * key/value pairs primarily via extension functions.
 */
public interface RemoteSemanticsPropertyReceiver {
    /**
     * Sets the semantics property [key] to [value].
     *
     * @param key The [RemoteSemanticsPropertyKey] to set.
     * @param value The value to associate with [key].
     */
    public operator fun <T> set(key: RemoteSemanticsPropertyKey<T>, value: T?)

    @Suppress("HiddenAbstractMethodInInterface")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun <T> get(key: RemoteSemanticsPropertyKey<T>): T?
}

/** General semantics properties, mainly used for accessibility and testing. */
public object RemoteSemanticsProperties {
    /** Semantics property key for [RemoteSemanticsPropertyReceiver.contentDescription]. */
    public val ContentDescription: RemoteSemanticsPropertyKey<RemoteString> =
        RemoteSemanticsPropertyKey("ContentDescription")

    /** Semantics property key for [RemoteSemanticsPropertyReceiver.role]. */
    public val Role: RemoteSemanticsPropertyKey<Role> = RemoteSemanticsPropertyKey("Role")

    /** Semantics property key for [RemoteSemanticsPropertyReceiver.remoteRole]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val RemoteRole: RemoteSemanticsPropertyKey<RemoteRole> =
        RemoteSemanticsPropertyKey("RemoteRole")

    /** Semantics property key for [RemoteSemanticsPropertyReceiver.text]. */
    public val Text: RemoteSemanticsPropertyKey<RemoteString> = RemoteSemanticsPropertyKey("Text")

    /** Semantics property key for [RemoteSemanticsPropertyReceiver.stateDescription]. */
    public val StateDescription: RemoteSemanticsPropertyKey<RemoteString> =
        RemoteSemanticsPropertyKey("StateDescription")

    /** Semantics property key for [RemoteSemanticsPropertyReceiver.enabled]. */
    public val Enabled: RemoteSemanticsPropertyKey<Boolean> = RemoteSemanticsPropertyKey("Enabled")
}

/**
 * Developer-set content description of the semantics node, for use in testing, accessibility and
 * similar use cases.
 */
public var RemoteSemanticsPropertyReceiver.contentDescription: RemoteString?
    get() = get(RemoteSemanticsProperties.ContentDescription)
    set(value) {
        set(RemoteSemanticsProperties.ContentDescription, value)
    }

/** The type of user interface element. Accessibility services can use this to describe the node. */
public var RemoteSemanticsPropertyReceiver.role: Role?
    get() = get(RemoteSemanticsProperties.Role)
    set(value) {
        set(RemoteSemanticsProperties.Role, value)
    }

/** The type of user interface element. Accessibility services can use this to describe the node. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public var RemoteSemanticsPropertyReceiver.remoteRole: RemoteRole?
    get() = get(RemoteSemanticsProperties.RemoteRole)
    set(value) {
        set(RemoteSemanticsProperties.RemoteRole, value)
    }

/** Text content for the semantics node. */
public var RemoteSemanticsPropertyReceiver.text: RemoteString?
    get() = get(RemoteSemanticsProperties.Text)
    set(value) {
        set(RemoteSemanticsProperties.Text, value)
    }

/** Description of the state for the semantics node. */
public var RemoteSemanticsPropertyReceiver.stateDescription: RemoteString?
    get() = get(RemoteSemanticsProperties.StateDescription)
    set(value) {
        set(RemoteSemanticsProperties.StateDescription, value)
    }

/** Whether the component is enabled. */
public var RemoteSemanticsPropertyReceiver.enabled: Boolean
    get() = get(RemoteSemanticsProperties.Enabled) ?: true
    set(value) {
        set(RemoteSemanticsProperties.Enabled, value)
    }

internal data class SemanticsModifier(
    val mergeMode: Mode,
    val properties: Map<RemoteSemanticsPropertyKey<*>, Any?>,
) : RemoteModifier.Element {
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return CreationSemanticsModifier(
            CoreSemantics().apply {
                mMode = mergeMode
                mTextId = (properties[RemoteSemanticsProperties.Text] as? RemoteString)?.id ?: 0
                mContentDescriptionId =
                    (properties[RemoteSemanticsProperties.ContentDescription] as? RemoteString)?.id
                        ?: 0
                mStateDescriptionId =
                    (properties[RemoteSemanticsProperties.StateDescription] as? RemoteString)?.id
                        ?: 0
                mEnabled = properties[RemoteSemanticsProperties.Enabled] as? Boolean ?: true
                mRole =
                    (properties[RemoteSemanticsProperties.RemoteRole] as? RemoteRole)
                        ?.toAccessibleRole()
                        ?: fromRole(properties[RemoteSemanticsProperties.Role] as? Role)
            }
        )
    }
}

private fun fromRole(role: Role?): AccessibleComponent.Role? {
    return when (role) {
        Role.RadioButton -> AccessibleComponent.Role.RADIO_BUTTON
        Role.DropdownList -> AccessibleComponent.Role.DROPDOWN_LIST
        Role.Button -> AccessibleComponent.Role.BUTTON
        Role.Checkbox -> AccessibleComponent.Role.CHECKBOX
        Role.Image -> AccessibleComponent.Role.IMAGE
        Role.Switch -> AccessibleComponent.Role.SWITCH
        Role.Tab -> AccessibleComponent.Role.TAB
        null -> null
        else -> AccessibleComponent.Role.UNKNOWN
    }
}

/**
 * Scope provided by semantics {} blocks, letting you set key/value pairs primarily via extension
 * functions.
 */
internal class AccessibilitySemantics : RemoteSemanticsPropertyReceiver {
    internal val props: MutableMap<RemoteSemanticsPropertyKey<*>, Any?> = mutableMapOf()

    override fun <T> set(key: RemoteSemanticsPropertyKey<T>, value: T?) {
        props[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: RemoteSemanticsPropertyKey<T>): T? {
        return props[key] as T?
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessibilitySemantics) return false
        return props == other.props
    }

    override fun hashCode(): Int {
        return props.hashCode()
    }
}

/**
 * Clears the semantics of all descendants and sets new semantics.
 *
 * @param properties A lambda to configure the semantics.
 */
public fun RemoteModifier.clearAndSetSemantics(
    properties: RemoteSemanticsPropertyReceiver.() -> Unit
): RemoteModifier =
    then(SemanticsModifier(CLEAR_AND_SET, AccessibilitySemantics().apply(properties).props.toMap()))

/**
 * Adds semantics to the node.
 *
 * @param mergeDescendants Whether to merge the semantics of all descendants into this node.
 * @param properties A lambda to configure the semantics.
 */
public fun RemoteModifier.semantics(
    mergeDescendants: Boolean = false,
    properties: RemoteSemanticsPropertyReceiver.() -> Unit,
): RemoteModifier =
    then(
        SemanticsModifier(
            if (mergeDescendants) MERGE else SET,
            AccessibilitySemantics().apply(properties).props.toMap(),
        )
    )
