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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeNode
import androidx.compose.remote.creation.compose.state.RemoteBoolean.Companion.createNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteLong.Companion.createNamedRemoteLong
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

// ==============================================================================================
// RemoteBoolean Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteBoolean] initialized with [defaultValue].
 *
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [Boolean] value
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteBoolean] bound to the named boolean state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteBoolean(
    name: String,
    defaultValue: Boolean,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteBoolean {
    return remember(name, domain) { createNamedRemoteBoolean(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteString Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteString] initialized with [defaultValue].
 *
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [String] value
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteString] bound to the named string state
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteString(
    name: String,
    defaultValue: String,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteString {
    return remember(name, domain) { createNamedRemoteString(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteLong Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteLong] initialized with [defaultValue].
 *
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [Long] value
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteLong] bound to the named long state
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteLong(
    name: String,
    defaultValue: Long,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteLong {
    return remember(name, domain) { createNamedRemoteLong(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteEnum Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteEnum] initialized with [defaultValue].
 *
 * The enum value is stored in the remote document as a named integer holding the enum entry's
 * [Enum.ordinal]. Hosts updating this value by [name] should supply the target entry's ordinal.
 *
 * @param T the [Enum] type represented by this state
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [Enum] value
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteEnum] backed by the named integer ordinal state
 */
@Composable
@RemoteComposable
public inline fun <reified T : Enum<T>> rememberNamedRemoteEnum(
    name: String,
    defaultValue: T,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteEnum<T> {
    return rememberNamedRemoteEnum(name, defaultValue, enumEntries(), domain)
}

/**
 * Remembers a named [RemoteEnum] using the given [enumEntries].
 *
 * The enum value is stored in the remote document as a named integer holding the enum entry's
 * [Enum.ordinal]. Hosts updating this value by [name] should supply the target entry's ordinal.
 *
 * @param T the [Enum] type represented by this state
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [Enum] value
 * @param enumEntries [EnumEntries] containing all values of [T]
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteEnum] backed by the named integer ordinal state
 */
@Composable
@RemoteComposable
public fun <T : Enum<T>> rememberNamedRemoteEnum(
    name: String,
    defaultValue: T,
    enumEntries: EnumEntries<T>,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteEnum<T> {
    return remember(name, domain) {
        RemoteEnum(createNamedRemoteInt(name, defaultValue.ordinal, domain), enumEntries)
    }
}

// ==============================================================================================
// RemoteFloat Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteFloat] initialized with [defaultValue].
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextDynamicFontSizeSample
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [RemoteFloat] constant or expression
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteFloat] bound to the named float state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteFloat(
    name: String,
    defaultValue: RemoteFloat,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteFloat {
    return remember(name, domain) { createNamedRemoteFloat(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteColor Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteColor] initialized with [defaultValue].
 *
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [RemoteColor] constant or expression
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteColor] bound to the named color state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteColor(
    name: String,
    defaultValue: RemoteColor,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteColor =
    remember(name, domain) { RemoteColor.createNamedRemoteColor(name, defaultValue, domain) }

// ==============================================================================================
// RemoteDp Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteDp] initialized with [defaultValue].
 *
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [RemoteDp] constant or expression
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteDp] bound to the named Dp state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteDp(
    name: String,
    defaultValue: RemoteDp,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteDp {
    return remember(name, domain) { RemoteDp.createNamedRemoteDp(name, domain) { defaultValue } }
}

// ==============================================================================================
// RemoteImageBitmap Composables
// ==============================================================================================

/**
 * Remembers a [RemoteImageBitmap] loaded from [url].
 *
 * @param url URL where the bitmap can be fetched
 * @return [RemoteImageBitmap] representing the image at [url]
 */
@RemoteComposable
@Composable
public fun rememberRemoteImageBitmap(url: String): RemoteImageBitmap {
    return remember(url) { RemoteImageBitmap(url) }
}

/**
 * Remembers a named [RemoteImageBitmap] loaded from [url].
 *
 * @param name unique name identifying this state within its [domain]
 * @param url URL where the bitmap can be fetched
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteImageBitmap] bound to the named bitmap state loaded from [url]
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    url: String,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteImageBitmap {
    return remember(name, domain) {
        RemoteImageBitmap.createNamedRemoteImageBitmap(name, url, domain)
    }
}

// ==============================================================================================
// RemoteInt Composables
// ==============================================================================================

/**
 * Remembers a named [RemoteInt] initialized with [defaultValue].
 *
 * @param name unique name identifying this state within its [domain]
 * @param defaultValue initial [Int] value
 * @param domain domain for the named state, defaulting to [RemoteValue.Domain.User]
 * @return [RemoteInt] bound to the named integer state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteInt(
    name: String,
    defaultValue: Int,
    domain: RemoteValue.Domain = RemoteValue.Domain.User,
): RemoteInt {
    return remember(name, domain) { createNamedRemoteInt(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteValue Hoisting Composables
// ==============================================================================================

internal class RemoteHoistNode : RemoteComposeNode() {
    var states: Array<out RemoteValue<*>> = emptyArray()

    override val supportsComponentToken: Boolean
        get() = false

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        for (state in states) {
            creationState.getId(state)
        }
    }
}

/**
 * Explicitly registers one or more [RemoteValue] expressions to be hoisted and evaluated at the
 * current container level in the document hierarchy.
 *
 * This ensures the operations underlying the given values (such as string formatting or
 * mathematical operations) are emitted at this container level, so that they are evaluated whenever
 * this container is rendered, rather than being trapped inside a conditionally hidden child
 * container (such as an inactive branch of a
 * [androidx.compose.remote.creation.compose.layout.RemoteAllStatesLayout]).
 *
 * @param values The [RemoteValue] expressions to hoist to the current container scope.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun Hoist(vararg values: RemoteValue<*>) {
    RemoteComposeNode(factory = ::RemoteHoistNode, update = { set(values) { this.states = it } })
}
