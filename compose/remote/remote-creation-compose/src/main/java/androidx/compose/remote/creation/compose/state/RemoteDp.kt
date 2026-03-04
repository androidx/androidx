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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Represents a Density-independent pixel (Dp) value.
 *
 * `RemoteDp` represents a Dp value that can be a constant, a named variable, or a dynamic
 * expression.
 */
@Stable
public class RemoteDp
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val value: RemoteFloat
) : BaseRemoteState<Dp>() {
    internal override val cacheKey: RemoteStateCacheKey
        get() = toPx().cacheKey

    internal enum class OperationKey {
        ToPx
    }

    override val constantValueOrNull: Dp?
        get() = value.constantValueOrNull?.dp

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx().writeToDocument(creationState)
    }

    /**
     * Function to convert this [RemoteDp] to a density-independent pixel value. It multiplies the
     * current float value by the screen\'s density.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toPx(density: RemoteDensity): RemoteFloat {
        return density.density * value
    }

    /** Converts a RemoteDp to a RemoteFloat Px using the [RemoteDensity]. */
    public fun toPx(): RemoteFloat {
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToPx, value),
        ) { creationState ->
            val density = creationState.remoteDensity
            (value * density.density).arrayForCreationState(creationState)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float {
        return toPx().getFloatIdForCreationState(creationState)
    }

    public companion object {
        /**
         * Creates a [RemoteDp] from a literal [Dp] value.
         *
         * @param value The [Dp] value.
         * @return A [RemoteDp] representing the constant Dp.
         */
        public operator fun invoke(value: Dp): RemoteDp = value.asRdp()

        /**
         * Creates a [RemoteDp] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteFloat]).
         * @return A [RemoteDp] referencing the ID.
         */
        internal fun createForId(id: RemoteFloat): RemoteDp = RemoteDp(id)

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
        @JvmStatic
        public fun createNamedRemoteDp(
            name: String,
            defaultValue: Dp,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteDp {
            return RemoteDp(
                RemoteFloat.createNamedRemoteFloat(
                    name = name,
                    defaultValue = defaultValue.value,
                    domain = domain,
                )
            )
        }
    }
}

/** Extension property to convert an [Int] to a [RemoteDp]. */
public val Int.rdp: RemoteDp
    get() {
        return RemoteDp(this.rf)
    }

/** Extension property to convert a [Float] to a [RemoteDp]. */
public val Float.rdp: RemoteDp
    get() {
        return RemoteDp(this.rf)
    }

/** Extension property to convert a [Dp] to a [RemoteDp]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Dp.asRdp(): RemoteDp {
    check(isSpecified) { "Dp conversion not possible for unspecified Dp" }
    return RemoteDp(this.value.rf)
}

/**
 * Remembers a named remote Dp expression.
 *
 * @param name The unique name for this remote Dp.
 * @param domain The domain of the named Dp (defaults to [RemoteState.Domain.User]).
 * @param content A lambda that provides the [RemoteDp] expression.
 * @return A [RemoteDp] representing the named remote Dp expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteDp(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    content: () -> RemoteDp,
): RemoteDp {
    return rememberNamedState(name, domain) {
        val remoteDp = content()
        RemoteDp(
            RemoteFloatExpression(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                val px = remoteDp.toPx()
                val initialValueId = px.getFloatIdForCreationState(creationState)
                val floatId =
                    creationState.document.addNamedFloat(domain.prefixed(name), initialValueId)
                floatArrayOf(floatId)
            }
        )
    }
}
