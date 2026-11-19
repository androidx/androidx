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
import kotlin.reflect.KClass

/**
 * Represents a density-independent pixel (Dp) value.
 *
 * [RemoteDp] represents a Dp value that may be a constant, a named variable, or a dynamic
 * expression. Operations on [RemoteDp] are evaluated dynamically on the remote player at runtime
 * unless all inputs are known constants, in which case constant folding evaluates the result
 * immediately at creation time (see [RemoteState]).
 *
 * Comparing [RemoteDp] values with `==` checks structural equality (matching constant values or
 * expression DAGs); see [RemoteState]. For runtime equality on the remote player, use [isEqualTo].
 */
@Stable
public sealed interface RemoteDp : RemoteState<Float> {
    // AutoBoxing is acceptable here because constantValueOrNull is not expected to be on a
    // hot path, and it follows the pattern for all RemoteState subclasses.
    @get:Suppress("AutoBoxing") override val constantValueOrNull: Float?

    /** The underlying [RemoteFloat] value in Dp. */
    public val value: RemoteFloat

    /**
     * Converts this [RemoteDp] to pixels by multiplying the current float value by [density].
     *
     * @param density The [RemoteDensity] used to convert Dp to pixels.
     * @return A [RemoteFloat] representing this value in pixels.
     */
    public fun toPx(density: RemoteDensity): RemoteFloat

    /**
     * Converts this [RemoteDp] to a [RemoteFloat] pixel value using the current [RemoteDensity].
     *
     * @return A [RemoteFloat] representing this value in pixels.
     */
    public fun toPx(): RemoteFloat

    /**
     * Returns a new [RemoteDp] that evaluates to this [RemoteDp] plus [other].
     *
     * @param other The [RemoteDp] to add to this value.
     * @return A [RemoteDp] representing `this + other`.
     */
    public operator fun plus(other: RemoteDp): RemoteDp

    /**
     * Returns a new [RemoteDp] that evaluates to this [RemoteDp] minus [other].
     *
     * @param other The [RemoteDp] to subtract from this value.
     * @return A [RemoteDp] representing `this - other`.
     */
    public operator fun minus(other: RemoteDp): RemoteDp

    /**
     * Returns a new [RemoteDp] that evaluates to the negation of this [RemoteDp].
     *
     * @return A [RemoteDp] representing `-this`.
     */
    public operator fun unaryMinus(): RemoteDp

    /**
     * Divides this [RemoteDp] by a scalar [Float].
     *
     * @param other The scalar divisor.
     * @return A [RemoteDp] representing `this / other`.
     */
    public operator fun div(other: Float): RemoteDp

    /**
     * Divides this [RemoteDp] by a scalar [Int].
     *
     * @param other The scalar divisor.
     * @return A [RemoteDp] representing `this / other`.
     */
    public operator fun div(other: Int): RemoteDp

    /**
     * Divides this [RemoteDp] by a [RemoteFloat].
     *
     * @param other The [RemoteFloat] divisor.
     * @return A [RemoteDp] representing `this / other`.
     */
    public operator fun div(other: RemoteFloat): RemoteDp

    /**
     * Divides this [RemoteDp] by a [RemoteInt].
     *
     * @param other The [RemoteInt] divisor.
     * @return A [RemoteDp] representing `this / other`.
     */
    public operator fun div(other: RemoteInt): RemoteDp

    /**
     * Divides this [RemoteDp] by another [RemoteDp] to produce a unitless [RemoteFloat] ratio.
     *
     * @param other The [RemoteDp] divisor.
     * @return A [RemoteFloat] representing the ratio `this / other`.
     */
    public operator fun div(other: RemoteDp): RemoteFloat

    /**
     * Multiplies this [RemoteDp] by a scalar [Float].
     *
     * @param other The scalar multiplier.
     * @return A [RemoteDp] representing `this * other`.
     */
    public operator fun times(other: Float): RemoteDp

    /**
     * Multiplies this [RemoteDp] by a scalar [Int].
     *
     * @param other The scalar multiplier.
     * @return A [RemoteDp] representing `this * other`.
     */
    public operator fun times(other: Int): RemoteDp

    /**
     * Multiplies this [RemoteDp] by a [RemoteFloat].
     *
     * @param other The [RemoteFloat] multiplier.
     * @return A [RemoteDp] representing `this * other`.
     */
    public operator fun times(other: RemoteFloat): RemoteDp

    /**
     * Multiplies this [RemoteDp] by a [RemoteInt].
     *
     * @param other The [RemoteInt] multiplier.
     * @return A [RemoteDp] representing `this * other`.
     */
    public operator fun times(other: RemoteInt): RemoteDp

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteDp]
     * is less than the evaluated value of [other].
     *
     * @param other The [RemoteDp] to compare with.
     * @return A [RemoteBoolean] representing `this < other`.
     */
    public fun isLessThan(other: RemoteDp): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteDp]
     * is less than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteDp] to compare with.
     * @return A [RemoteBoolean] representing `this <= other`.
     */
    public fun isLessThanOrEqualTo(other: RemoteDp): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteDp]
     * is greater than the evaluated value of [other].
     *
     * @param other The [RemoteDp] to compare with.
     * @return A [RemoteBoolean] representing `this > other`.
     */
    public fun isGreaterThan(other: RemoteDp): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteDp]
     * is greater than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteDp] to compare with.
     * @return A [RemoteBoolean] representing `this >= other`.
     */
    public fun isGreaterThanOrEqualTo(other: RemoteDp): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteDp]
     * is equal to the evaluated value of [other].
     *
     * @param other The [RemoteDp] to compare with.
     * @return A [RemoteBoolean] representing `this == other`.
     */
    public fun isEqualTo(other: RemoteDp): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteDp]
     * is not equal to the evaluated value of [other].
     *
     * @param other The [RemoteDp] to compare with.
     * @return A [RemoteBoolean] representing `this != other`.
     */
    public fun isNotEqualTo(other: RemoteDp): RemoteBoolean

    public companion object {
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

private class RemoteDpImpl(override val value: RemoteFloat, cacheKey: RemoteStateCacheKey) :
    BaseRemoteState<Float>(cacheKey), RemoteDp {
    override val equalityType: KClass<*>
        get() = RemoteDp::class

    override val constantValueOrNull: Float?
        get() = value.constantValueOrNull

    override fun toPx(density: RemoteDensity): RemoteFloat = density.density * value

    override fun toPx(): RemoteFloat {
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(DpOperationKey.ToPx, value),
        ) { creationState ->
            val density = creationState.remoteDensity
            (value * density.density).arrayForCreationState(creationState)
        }
    }

    override fun plus(other: RemoteDp): RemoteDp = RemoteDp(value + other.value)

    override fun minus(other: RemoteDp): RemoteDp = RemoteDp(value - other.value)

    override fun unaryMinus(): RemoteDp = RemoteDp(-value)

    override fun div(other: Float): RemoteDp = RemoteDp(value / other)

    override fun div(other: Int): RemoteDp = RemoteDp(value / other.toFloat())

    override fun div(other: RemoteFloat): RemoteDp = RemoteDp(value / other)

    override fun div(other: RemoteInt): RemoteDp = RemoteDp(value / other.toRemoteFloat())

    override fun div(other: RemoteDp): RemoteFloat = value / other.value

    override fun times(other: Float): RemoteDp = RemoteDp(value * other)

    override fun times(other: Int): RemoteDp = RemoteDp(value * other.toFloat())

    override fun times(other: RemoteFloat): RemoteDp = RemoteDp(value * other)

    override fun times(other: RemoteInt): RemoteDp = RemoteDp(value * other.toRemoteFloat())

    override fun isLessThan(other: RemoteDp): RemoteBoolean = value.isLessThan(other.value)

    override fun isLessThanOrEqualTo(other: RemoteDp): RemoteBoolean =
        value.isLessThanOrEqualTo(other.value)

    override fun isGreaterThan(other: RemoteDp): RemoteBoolean = value.isGreaterThan(other.value)

    override fun isGreaterThanOrEqualTo(other: RemoteDp): RemoteBoolean =
        value.isGreaterThanOrEqualTo(other.value)

    override fun isEqualTo(other: RemoteDp): RemoteBoolean = value.isEqualTo(other.value)

    override fun isNotEqualTo(other: RemoteDp): RemoteBoolean = value.isNotEqualTo(other.value)

    override fun toString(): String {
        val valKey = value.cacheKey
        if (valKey is RemoteOperationCacheKey && valKey.op == DpOperationKey.ToDp) {
            return "${valKey.args[0].toOperandString(100)}.toDp()"
        }
        return "${valKey.toOperandString(100)}.dp"
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    override fun getFloatId(creationState: RemoteComposeCreationState): Float {
        return creationState.getFloatId(toPx(creationState.remoteDensity))
    }
}

private enum class DpOperationKey : RemoteOperation {
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

    override fun reconstruct(args: List<RemoteState<*>>): RemoteState<*> {
        return when (this) {
            Dp -> RemoteDp(args[0] as RemoteFloat)
            ToPx -> RemoteDp(args[0] as RemoteFloat).toPx()
            ToDp -> (args[0] as RemoteFloat).toRemoteDp().value
        }
    }
}

/**
 * Creates a [RemoteDp] from a [RemoteFloat].
 *
 * @param value The [RemoteFloat] that holds the actual Dp value.
 * @return A [RemoteDp] wrapping the given [RemoteFloat].
 */
public fun RemoteDp(value: RemoteFloat): RemoteDp =
    RemoteDpImpl(value, RemoteOperationCacheKey.create(DpOperationKey.Dp, value))

/**
 * Creates a [RemoteDp] with a literal [Float] value.
 *
 * @param value The literal float value in Dp.
 * @return A [RemoteDp] representing the constant Dp.
 */
public fun RemoteDp(value: Float): RemoteDp = RemoteDp(value.rf)

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

/**
 * Wraps this [RemoteFloat] directly as a [RemoteDp] without density conversion (`1:1`).
 *
 * @return A [RemoteDp] wrapping this [RemoteFloat] scalar value.
 */
public fun RemoteFloat.asRemoteDp(): RemoteDp {
    return RemoteDp(this)
}

/**
 * Converts this [RemoteFloat] pixel value to a [RemoteDp] by dividing by the current density.
 *
 * @return A [RemoteDp] representing this pixel value in Dp.
 */
public fun RemoteFloat.toRemoteDp(): RemoteDp {
    // TODO: Optimize for constant values when value and density are constants
    return RemoteDp(
        RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(DpOperationKey.ToDp, this),
        ) { creationState ->
            val density = creationState.remoteDensity
            (this / density.density).arrayForCreationState(creationState)
        }
    )
}

/**
 * Returns a [RemoteDp] that evaluates to the smaller of [a] and [b].
 *
 * @param a The first [RemoteDp] value to compare.
 * @param b The second [RemoteDp] value to compare.
 * @return A [RemoteDp] representing the smaller of [a] and [b].
 */
public fun min(a: RemoteDp, b: RemoteDp): RemoteDp = RemoteDp(min(a.value, b.value))

/**
 * Returns a [RemoteDp] that evaluates to the greater of [a] and [b].
 *
 * @param a The first [RemoteDp] value to compare.
 * @param b The second [RemoteDp] value to compare.
 * @return A [RemoteDp] representing the larger of [a] and [b].
 */
public fun max(a: RemoteDp, b: RemoteDp): RemoteDp = RemoteDp(max(a.value, b.value))
