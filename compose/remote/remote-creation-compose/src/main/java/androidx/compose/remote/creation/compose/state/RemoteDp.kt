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
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloatExpression
import androidx.compose.runtime.Stable

/**
 * Represents a Density-independent pixel (Dp) value.
 *
 * [RemoteDp] represents a Dp value that can be a constant, a named variable, or a dynamic
 * expression.
 */
@Stable
public class RemoteDp
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val value: RemoteFloat
) : BaseRemoteState<Float>(RemoteStateInstanceKey()) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val cacheKey: RemoteStateCacheKey
        get() = RemoteOperationCacheKey.create(OperationKey.ToPx, value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String {
        val valKey = value.cacheKey
        if (valKey is RemoteOperationCacheKey && valKey.op == OperationKey.ToDp) {
            return "${valKey.args[0].toOperandString(100)}.toDp()"
        }
        return "${valKey.toOperandString(100)}.dp"
    }

    internal enum class OperationKey : RemoteOperation {
        ToPx,
        ToDp;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            return when (this) {
                ToPx -> "${args[0].toOperandString(100)}.toPx()"
                ToDp -> "${args[0].toOperandString(100)}.toDp()"
            }
        }

        override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
            return when (this) {
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    /**
     * Converts this [RemoteDp] to pixels by multiplying the current float value by [density].
     *
     * @param density The [RemoteDensity] used to convert Dp to pixels.
     */
    public fun toPx(density: RemoteDensity): RemoteFloat {
        return density.density * value
    }

    /**
     * Converts this [RemoteDp] to a [RemoteFloat] pixel value using the current [RemoteDensity].
     */
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
        return toPx(creationState.remoteDensity).getFloatIdForCreationState(creationState)
    }

    /**
     * Adds two [RemoteDp] values together.
     *
     * @param other The [RemoteDp] to add to this value.
     */
    public operator fun plus(other: RemoteDp): RemoteDp = RemoteDp(this.value + other.value)

    /**
     * Subtracts [other] from this [RemoteDp].
     *
     * @param other The [RemoteDp] to subtract from this value.
     */
    public operator fun minus(other: RemoteDp): RemoteDp = RemoteDp(this.value - other.value)

    /** Negates this [RemoteDp], equivalent to multiplying by -1.0. */
    public operator fun unaryMinus(): RemoteDp = RemoteDp(-this.value)

    /**
     * Divides this [RemoteDp] by a scalar.
     *
     * @param other The scalar divisor.
     */
    public operator fun div(other: Float): RemoteDp = RemoteDp(this.value / other)

    /**
     * Divides this [RemoteDp] by a scalar.
     *
     * @param other The scalar divisor.
     */
    public operator fun div(other: Int): RemoteDp = RemoteDp(this.value / other.toFloat())

    /**
     * Divides this [RemoteDp] by a [RemoteFloat].
     *
     * @param other The [RemoteFloat] divisor.
     */
    public operator fun div(other: RemoteFloat): RemoteDp = RemoteDp(this.value / other)

    /**
     * Divides this [RemoteDp] by a [RemoteInt].
     *
     * @param other The [RemoteInt] divisor.
     */
    public operator fun div(other: RemoteInt): RemoteDp =
        RemoteDp(this.value / other.toRemoteFloat())

    /**
     * Divides this [RemoteDp] by another [RemoteDp] to produce a scalar ratio.
     *
     * @param other The [RemoteDp] divisor.
     */
    public operator fun div(other: RemoteDp): RemoteFloat = this.value / other.value

    /**
     * Multiplies this [RemoteDp] by a scalar.
     *
     * @param other The scalar multiplier.
     */
    public operator fun times(other: Float): RemoteDp = RemoteDp(this.value * other)

    /**
     * Multiplies this [RemoteDp] by a scalar.
     *
     * @param other The scalar multiplier.
     */
    public operator fun times(other: Int): RemoteDp = RemoteDp(this.value * other.toFloat())

    /**
     * Multiplies this [RemoteDp] by a [RemoteFloat].
     *
     * @param other The [RemoteFloat] multiplier.
     */
    public operator fun times(other: RemoteFloat): RemoteDp = RemoteDp(this.value * other)

    /**
     * Multiplies this [RemoteDp] by a [RemoteInt].
     *
     * @param other The [RemoteInt] multiplier.
     */
    public operator fun times(other: RemoteInt): RemoteDp =
        RemoteDp(this.value * other.toRemoteFloat())

    /**
     * Checks if this [RemoteDp] is less than [other].
     *
     * @param other The [RemoteDp] to compare with.
     */
    public fun isLessThan(other: RemoteDp): RemoteBoolean = this.value.isLessThan(other.value)

    /**
     * Checks if this [RemoteDp] is less than or equal to [other].
     *
     * @param other The [RemoteDp] to compare with.
     */
    public fun isLessThanOrEqualTo(other: RemoteDp): RemoteBoolean =
        this.value.isLessThanOrEqualTo(other.value)

    /**
     * Checks if this [RemoteDp] is greater than [other].
     *
     * @param other The [RemoteDp] to compare with.
     */
    public fun isGreaterThan(other: RemoteDp): RemoteBoolean = this.value.isGreaterThan(other.value)

    /**
     * Checks if this [RemoteDp] is greater than or equal to [other].
     *
     * @param other The [RemoteDp] to compare with.
     */
    public fun isGreaterThanOrEqualTo(other: RemoteDp): RemoteBoolean =
        this.value.isGreaterThanOrEqualTo(other.value)

    /**
     * Checks if this [RemoteDp] is equal to [other].
     *
     * @param other The [RemoteDp] to compare with.
     */
    public fun isEqualTo(other: RemoteDp): RemoteBoolean = this.value.isEqualTo(other.value)

    /**
     * Checks if this [RemoteDp] is not equal to [other].
     *
     * @param other The [RemoteDp] to compare with.
     */
    public fun isNotEqualTo(other: RemoteDp): RemoteBoolean = this.value.isNotEqualTo(other.value)

    public companion object {
        /**
         * Creates a [RemoteDp] from a [RemoteFloat].
         *
         * @param value The [RemoteFloat] value.
         * @return A [RemoteDp] representing the Dp.
         */
        public operator fun invoke(value: RemoteFloat): RemoteDp = RemoteDp(value)

        /**
         * Creates a [RemoteDp] from a literal [Float] value.
         *
         * @param value The [Float] value.
         * @return A [RemoteDp] representing the constant Dp.
         */
        public operator fun invoke(value: Float): RemoteDp = RemoteDp(value.rf)

        /**
         * Creates a [RemoteDp] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteFloat]).
         * @return A [RemoteDp] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: RemoteFloat): RemoteDp = RemoteDp(id)

        /**
         * Creates a named [RemoteDp] with an initial value.
         *
         * Named remote Dps can be set via AndroidRemoteContext.setNamedFloat.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param defaultValue The initial [Float] value for the named remote Dp.
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @return A [RemoteDp] representing the named Dp.
         */
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

        /**
         * Creates a named [RemoteDp] expression.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @param value A lambda that provides the [RemoteDp] expression.
         * @return A [RemoteDp] representing the named Dp expression.
         */
        public fun createNamedRemoteDp(
            name: String,
            domain: RemoteState.Domain = RemoteState.Domain.User,
            value: () -> RemoteDp,
        ): RemoteDp {
            val remoteDp = value()
            return RemoteDp(createNamedRemoteFloatExpression(name, domain) { remoteDp.value })
        }
    }
}

/**
 * Creates a named [RemoteDp] with an initial value.
 *
 * Named remote Dps can be set via AndroidRemoteContext.setNamedFloat.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param defaultValue The initial [Float] value for the named remote Dp.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteDp] representing the named Dp.
 */
public fun createNamedRemoteDp(
    name: String,
    defaultValue: Float,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteDp = RemoteDp.createNamedRemoteDp(name, defaultValue, domain)

/**
 * Creates a named [RemoteDp] expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @param value A lambda that provides the [RemoteDp] expression.
 * @return A [RemoteDp] representing the named Dp expression.
 */
public fun createNamedRemoteDp(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> RemoteDp,
): RemoteDp = RemoteDp.createNamedRemoteDp(name, domain, value)

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

/** Converts this [RemoteFloat] to a [RemoteDp] directly (1:1) */
public fun RemoteFloat.asRemoteDp(): RemoteDp {
    return RemoteDp(this)
}

/** Converts this [RemoteFloat] representing pixels to a [RemoteDp] by dividing by density. */
public fun RemoteFloat.toRemoteDp(): RemoteDp {
    // TODO: Optimize for constant values when value and density are constants
    return RemoteDp(
        RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(RemoteDp.OperationKey.ToDp, this),
        ) { creationState ->
            val density = creationState.remoteDensity
            (this / density.density).arrayForCreationState(creationState)
        }
    )
}

/**
 * Returns the smaller of two [RemoteDp] values.
 *
 * @param a The first [RemoteDp] value to compare.
 * @param b The second [RemoteDp] value to compare.
 */
public fun min(a: RemoteDp, b: RemoteDp): RemoteDp = RemoteDp(min(a.value, b.value))

/**
 * Returns the greater of two [RemoteDp] values.
 *
 * @param a The first [RemoteDp] value to compare.
 * @param b The second [RemoteDp] value to compare.
 */
public fun max(a: RemoteDp, b: RemoteDp): RemoteDp = RemoteDp(max(a.value, b.value))
