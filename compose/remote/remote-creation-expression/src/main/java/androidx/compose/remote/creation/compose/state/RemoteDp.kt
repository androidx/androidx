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
import androidx.compose.remote.creation.compose.state.RemoteDp.Companion.createNamedRemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.runtime.Stable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public class RemoteDp
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val value: RemoteFloat,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<Float>(cacheKey) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String {
        val valKey = value.cacheKey
        if (valKey is RemoteOperationCacheKey && valKey.op == OperationKey.ToDp) {
            return "${valKey.args[0].toOperandString(100)}.toDp()"
        }
        return "${valKey.toOperandString(100)}.dp"
    }

    internal enum class OperationKey : RemoteOperation {
        Dp,
        ToPx,
        ToDp;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            return when (this) {
                Dp -> "${args[0].toOperandString(100)}.dp"
                ToPx -> "${args[0].toOperandString(100)}.toPx()"
                ToDp -> "${args[0].toOperandString(100)}.toDp()"
            }
        }

        override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
            return when (this) {
                Dp -> RemoteDp(args[0] as RemoteFloat)
                ToPx -> RemoteDp(args[0] as RemoteFloat).toPx()
                ToDp -> (args[0] as RemoteFloat).toRemoteDp().value
            }
        }
    }

    // AutoBoxing is acceptable here because constantValueOrNull is not expected to be on a
    // hot path, and it follows the pattern for all RemoteState subclasses.
    @get:Suppress("AutoBoxing")
    override val constantValueOrNull: Float?
        get() = value.constantValueOrNull

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    public fun toPx(density: RemoteDensity): RemoteFloat {
        return density.density * value
    }

    public fun toPx(): RemoteFloat {
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToPx, value),
        ) { creationState ->
            val density = creationState.remoteDensity
            (value * density.density).arrayForCreationState(creationState)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float {
        return toPx(creationState.remoteDensity).getFloatIdForCreationState(creationState)
    }

    public operator fun plus(other: RemoteDp): RemoteDp = RemoteDp(this.value + other.value)

    public operator fun minus(other: RemoteDp): RemoteDp = RemoteDp(this.value - other.value)

    public operator fun unaryMinus(): RemoteDp = RemoteDp(-this.value)

    public operator fun div(other: Float): RemoteDp = RemoteDp(this.value / other)

    public operator fun div(other: Int): RemoteDp = RemoteDp(this.value / other.toFloat())

    public operator fun div(other: RemoteFloat): RemoteDp = RemoteDp(this.value / other)

    public operator fun div(other: RemoteInt): RemoteDp =
        RemoteDp(this.value / other.toRemoteFloat())

    public operator fun div(other: RemoteDp): RemoteFloat = this.value / other.value

    public operator fun times(other: Float): RemoteDp = RemoteDp(this.value * other)

    public operator fun times(other: Int): RemoteDp = RemoteDp(this.value * other.toFloat())

    public operator fun times(other: RemoteFloat): RemoteDp = RemoteDp(this.value * other)

    public operator fun times(other: RemoteInt): RemoteDp =
        RemoteDp(this.value * other.toRemoteFloat())

    public fun isLessThan(other: RemoteDp): RemoteBoolean = this.value.isLessThan(other.value)

    public fun isLessThanOrEqualTo(other: RemoteDp): RemoteBoolean =
        this.value.isLessThanOrEqualTo(other.value)

    public fun isGreaterThan(other: RemoteDp): RemoteBoolean = this.value.isGreaterThan(other.value)

    public fun isGreaterThanOrEqualTo(other: RemoteDp): RemoteBoolean =
        this.value.isGreaterThanOrEqualTo(other.value)

    public fun isEqualTo(other: RemoteDp): RemoteBoolean = this.value.isEqualTo(other.value)

    public fun isNotEqualTo(other: RemoteDp): RemoteBoolean = this.value.isNotEqualTo(other.value)

    public companion object {
        /**
         * Creates a [RemoteDp] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteFloat]).
         * @return A [RemoteDp] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: RemoteFloat): RemoteDp = RemoteDp(id)

        public fun createNamedRemoteDp(
            name: String,
            defaultValue: Float,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteDp {
            return RemoteDp(
                createNamedRemoteFloat(
                    name = name,
                    defaultValue = defaultValue,
                    domain = domain,
                )
            )
        }

        public fun createNamedRemoteDp(
            name: String,
            domain: RemoteState.Domain = RemoteState.Domain.User,
            value: () -> RemoteDp,
        ): RemoteDp {
            val remoteDp = value()
            return RemoteDp(
                createNamedRemoteFloat(
                    name = name,
                    defaultValue = remoteDp.value,
                    domain = domain,
                )
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDp(value: RemoteFloat): RemoteDp =
    RemoteDp(value, RemoteOperationCacheKey.create(RemoteDp.OperationKey.Dp, value))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDp(value: Float): RemoteDp = RemoteDp(value.rf)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createNamedRemoteDp(
    name: String,
    defaultValue: Float,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteDp = RemoteDp.createNamedRemoteDp(name, defaultValue, domain)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createNamedRemoteDp(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> RemoteDp,
): RemoteDp = RemoteDp.createNamedRemoteDp(name, domain, value)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Int.rdp: RemoteDp
    get() {
        return RemoteDp(this.rf)
    }

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Float.rdp: RemoteDp
    get() {
        return RemoteDp(this.rf)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloat.asRemoteDp(): RemoteDp {
    return RemoteDp(this)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloat.toRemoteDp(): RemoteDp {
    // TODO: Optimize for constant values when value and density are constants
    return RemoteDp(
        RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(RemoteDp.OperationKey.ToDp, this),
        ) { creationState ->
            val density = creationState.remoteDensity
            (this / density.density).arrayForCreationState(creationState)
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RemoteDp, b: RemoteDp): RemoteDp = RemoteDp(min(a.value, b.value))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RemoteDp, b: RemoteDp): RemoteDp = RemoteDp(max(a.value, b.value))
