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

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteDp.Companion.createNamedRemoteDp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Creates a [RemoteDp] from a literal [Dp] value.
 *
 * @param value The [Dp] value.
 * @return A [RemoteDp] representing the constant Dp.
 */
public fun RemoteDp(value: Dp): RemoteDp = value.asRdp()

/**
 * Creates a named [RemoteDp] with an initial value.
 *
 * Named remote Dps can be set via AndroidRemoteContext.setNamedFloat.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param defaultValue The initial [Dp] value for the named remote Dp.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteDp] representing the named Dp.
 */
public fun RemoteDp.Companion.createNamedRemoteDp(
    name: String,
    defaultValue: Dp,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteDp {
    check(defaultValue.isSpecified) { "Dp conversion not possible for unspecified Dp" }
    return createNamedRemoteDp(name, defaultValue.value, domain)
}

/**
 * Creates a named [RemoteDp] with an initial value.
 *
 * Named remote Dps can be set via AndroidRemoteContext.setNamedFloat.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param defaultValue The initial [Dp] value for the named remote Dp.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteDp] representing the named Dp.
 */
public fun createNamedRemoteDp(
    name: String,
    defaultValue: Dp,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteDp = RemoteDp.createNamedRemoteDp(name, defaultValue, domain)

/** Extension property to get the [Dp] value of this [RemoteDp], or null if dynamic. */
public val RemoteDp.asDpOrNull: Dp?
    get() = constantValueOrNull?.dp

/** Extension property to get the [Dp] value of this [RemoteDp]. */
public val RemoteDp.asDp: Dp
    get() = constantValue.dp

/** Extension function to convert a [Dp] to a [RemoteDp]. */
public fun Dp.asRdp(): RemoteDp {
    check(isSpecified) { "Dp conversion not possible for unspecified Dp" }
    return RemoteDp(this.value.rf)
}

/** Extension property to convert a [Dp] to a [RemoteDp]. */
public val Dp.rdp: RemoteDp
    get() = asRdp()

/**
 * Remembers a named remote Dp expression.
 *
 * @param name The unique name for this remote Dp.
 * @param domain The domain of the named Dp (defaults to [RemoteState.Domain.User]).
 * @param value A lambda that provides the [RemoteDp] expression.
 * @return A [RemoteDp] representing the named remote Dp expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteDp(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> RemoteDp,
): RemoteDp {
    return remember(name, domain) { createNamedRemoteDp(name, domain, value) }
}
