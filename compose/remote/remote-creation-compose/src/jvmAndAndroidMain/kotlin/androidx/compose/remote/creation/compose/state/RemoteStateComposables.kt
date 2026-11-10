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

@file:JvmName("RemoteStateKt")

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
 * Remembers a named remote boolean expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param initialValue The initial [Boolean] value.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteBoolean] instance representing the named expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteBoolean(
    name: String,
    initialValue: Boolean,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteBoolean {
    return remember(name, domain) { createNamedRemoteBoolean(name, initialValue, domain) }
}

// ==============================================================================================
// RemoteString Composables
// ==============================================================================================

/**
 * Remembers a named remote string expression.
 *
 * @param name The unique name for this remote string.
 * @param defaultValue The initial [String] value for this remote string.
 * @param domain The domain of the named string (defaults to [RemoteState.Domain.User]).
 * @return A [RemoteString] representing the named remote string expression.
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteString(
    name: String,
    defaultValue: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteString {
    return remember(name, domain) { createNamedRemoteString(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteLong Composables
// ==============================================================================================

/**
 * Remembers a named remote long expression.
 *
 * @param name The unique name for this remote long.
 * @param defaultValue The initial long value.
 * @param domain The domain of the named long (defaults to [RemoteState.Domain.User]).
 * @return A [RemoteLong] representing the named remote long expression.
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteLong(
    name: String,
    defaultValue: Long,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteLong {
    return remember(name, domain) { createNamedRemoteLong(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteEnum Composables
// ==============================================================================================

/**
 * Remembers a named remote enum expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param initialValue The initial [Enum] value.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteEnum] instance representing the named expression.
 */
@Composable
@RemoteComposable
public inline fun <reified T : Enum<T>> rememberNamedRemoteEnum(
    name: String,
    initialValue: T,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteEnum<T> {
    return rememberNamedRemoteEnum(name, initialValue, enumEntries(), domain)
}

/**
 * Remembers a named [RemoteEnum] expression using the given [enumEntries].
 *
 * @param name unique name identifying this state within its [domain]
 * @param initialValue initial [Enum] value
 * @param enumEntries [EnumEntries] containing all values of [T]
 * @param domain domain for the named state, defaulting to [RemoteState.Domain.User]
 * @return a [RemoteEnum] backed by the named state
 */
@Composable
@RemoteComposable
public fun <T : Enum<T>> rememberNamedRemoteEnum(
    name: String,
    initialValue: T,
    enumEntries: EnumEntries<T>,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteEnum<T> {
    return remember(name, domain) {
        RemoteEnum(createNamedRemoteInt(name, initialValue.ordinal, domain), enumEntries)
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
 * @param domain domain for the named state, defaulting to [RemoteState.Domain.User]
 * @return [RemoteFloat] bound to the named float state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteFloat(
    name: String,
    defaultValue: RemoteFloat,
    domain: RemoteState.Domain = RemoteState.Domain.User,
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
 * @param domain domain for the named state, defaulting to [RemoteState.Domain.User]
 * @return [RemoteColor] bound to the named color state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteColor(
    name: String,
    defaultValue: RemoteColor,
    domain: RemoteState.Domain = RemoteState.Domain.User,
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
 * @param domain domain for the named state, defaulting to [RemoteState.Domain.User]
 * @return [RemoteDp] bound to the named Dp state
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteDp(
    name: String,
    defaultValue: RemoteDp,
    domain: RemoteState.Domain = RemoteState.Domain.User,
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
 * @param domain domain for the named state, defaulting to [RemoteState.Domain.User]
 * @return [RemoteImageBitmap] bound to the named bitmap state loaded from [url]
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    url: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteImageBitmap {
    return remember(name, domain) {
        RemoteImageBitmap.createNamedRemoteImageBitmap(name, url, domain)
    }
}

// ==============================================================================================
// RemoteInt Composables
// ==============================================================================================

/**
 * Remembers a named remote integer expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @param defaultValue The initial [Int] value.
 * @return A [RemoteInt] instance representing the named expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteInt(
    name: String,
    defaultValue: Int,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteInt {
    return remember(name, domain) { createNamedRemoteInt(name, defaultValue, domain) }
}

// ==============================================================================================
// RemoteState Hoisting Composables
// ==============================================================================================

internal class RemoteHoistNode : RemoteComposeNode() {
    var states: Array<out RemoteState<*>> = emptyArray()

    override val supportsComponentToken: Boolean
        get() = false

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        for (state in states) {
            if (state is BaseRemoteState<*>) {
                state.getIdForCreationState(creationState)
            }
        }
    }
}

/**
 * Explicitly registers one or more [RemoteState] expressions to be hoisted and evaluated at the
 * current container level in the document hierarchy.
 *
 * This ensures the operations underlying the given states (such as string formatting or
 * mathematical operations) are emitted at this container level, so that they are evaluated whenever
 * this container is rendered, rather than being trapped inside a conditionally hidden child
 * container (such as an inactive branch of a RemoteStateLayout).
 *
 * @param states The remote state expressions to hoist to the current container scope.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun Hoist(vararg states: RemoteState<*>) {
    RemoteComposeNode(factory = ::RemoteHoistNode, update = { set(states) { this.states = it } })
}
