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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Abstract base class for all remote long representations. This class extends [RemoteState<Long>].
 */
@Stable
public open class RemoteLong
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val low: RemoteInt,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val high: RemoteInt,
) : BaseRemoteState<Long>() {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Long?
        get() {
            val l = low.constantValueOrNull ?: return null
            val h = high.constantValueOrNull ?: return null
            return (h.toLong() shl 32) or (l.toLong() and 0xFFFFFFFFL)
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal override val cacheKey: RemoteStateCacheKey
        get() = RemoteOperationCacheKey.create(RemoteLongOp.Emulated, low, high)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        throw UnsupportedOperationException("RemoteLong cannot be directly written to document yet")
    }

    /** Returns a new [RemoteLong] that evaluates to this [RemoteLong] plus [v]. */
    public operator fun plus(v: RemoteLong): RemoteLong {
        // Emulates 64-bit addition using 32-bit integer arithmetic (Multiple-precision arithmetic).
        //
        // The algorithm computes the lower 32-bit sum and manually detects carry-out by checking if
        // the unsigned sum is less than either unsigned operand. Since `RemoteInt` handles signed
        // values, it flips the sign bit using `xor Int.MIN_VALUE` to perform unsigned comparisons.
        // See: https://www.nayuki.io/page/unsigned-int-considered-harmful-for-java (Unsigned
        // comparison)
        val lowAdd = this.low + v.low
        val minVal = Int.MIN_VALUE.ri
        val carry = selectIfLt(lowAdd xor minVal, this.low xor minVal, 1.ri, 0.ri)
        val highAdd = this.high + v.high + carry
        return object : RemoteLong(lowAdd, highAdd) {
            override val cacheKey: RemoteStateCacheKey
                get() = RemoteOperationCacheKey.create(RemoteLongOp.Add, this@RemoteLong, v)
        }
    }

    /** Returns a new [RemoteLong] that evaluates to this [RemoteLong] minus [v]. */
    public operator fun minus(v: RemoteLong): RemoteLong {
        // Emulates 64-bit subtraction using 32-bit integer arithmetic (Multiple-precision
        // arithmetic).
        //
        // The algorithm computes the lower 32-bit difference and manually detects borrow-out by
        // checking if the first unsigned operand is less than the second unsigned operand. Similar
        // to
        // addition, unsigned comparison is achieved by flipping the sign bit using `xor
        // Int.MIN_VALUE`.
        // See: https://www.nayuki.io/page/unsigned-int-considered-harmful-for-java (Unsigned
        // comparison)
        val lowSub = this.low - v.low
        val minVal = Int.MIN_VALUE.ri
        val borrow = selectIfLt(this.low xor minVal, v.low xor minVal, 1.ri, 0.ri)
        val highSub = this.high - v.high - borrow
        return object : RemoteLong(lowSub, highSub) {
            override val cacheKey: RemoteStateCacheKey
                get() = RemoteOperationCacheKey.create(RemoteLongOp.Sub, this@RemoteLong, v)
        }
    }

    /** Returns a new [RemoteLong] that evaluates to this [RemoteLong] times [v]. */
    public operator fun times(v: RemoteLong): RemoteLong {
        // Emulates 64-bit multiplication using 32-bit integer arithmetic.
        //
        // To prevent 32-bit signed overflow during intermediate calculations, the 32-bit `low`
        // words are further split into 16-bit halves. It performs four 16x16-bit multiplications
        // and combines the partial products with appropriate shifts. This is an implementation of
        // standard long multiplication algorithms in software, commonly used when hardware
        // only supports 32-bit registers.
        // See: https://en.wikipedia.org/wiki/Multiplication_algorithm#Long_multiplication
        val mask = 0xFFFF.ri
        val a0 = this.low and mask
        val a1 = (this.low shr 16.ri) and mask
        val b0 = v.low and mask
        val b1 = (v.low shr 16.ri) and mask

        val m00 = a0 * b0
        val m00Carry = (m00 shr 16.ri) and mask

        val mid1 = (a1 * b0) + m00Carry
        val mid1Lo = mid1 and mask
        val mid1Hi = (mid1 shr 16.ri) and mask

        val mid2 = a0 * b1
        val mid2Lo = mid2 and mask
        val mid2Hi = (mid2 shr 16.ri) and mask

        val midLoSum = mid1Lo + mid2Lo
        val midLoSumCarry = (midLoSum shr 16.ri) and mask

        val highCross = mid1Hi + mid2Hi + midLoSumCarry
        val m11 = a1 * b1
        val upper32 = m11 + highCross

        val finalLow = this.low * v.low
        val finalHigh = (this.high * v.low) + (this.low * v.high) + upper32

        return object : RemoteLong(finalLow, finalHigh) {
            override val cacheKey: RemoteStateCacheKey
                get() = RemoteOperationCacheKey.create(RemoteLongOp.Mul, this@RemoteLong, v)
        }
    }

    /**
     * Returns a [RemoteInt] that evaluates to the truncating conversion of the lower 32 bits of the
     * [RemoteLong].
     */
    public fun toRemoteInt(): RemoteInt = low

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteLong] instance from a constant [Long] value. This value will be added as
         * a constant to the remote document.
         *
         * @param v The constant [Long] value.
         * @return A [MutableRemoteLong] representing the constant value.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(v: Long): RemoteLong {
            return MutableRemoteLong(v, cacheKey = RemoteConstantCacheKey(v)) { creationState ->
                creationState.document.addLong(v)
            }
        }

        /**
         * Creates a [RemoteLong] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteLong] referencing the ID.
         */
        internal fun createForId(id: Int): RemoteLong = MutableRemoteLong(id)

        /**
         * Creates a [RemoteLong] from low and high [RemoteInt]s.
         *
         * @param low The lower 32 bits.
         * @param high The upper 32 bits.
         * @return A [RemoteLong] representing the combination of low and high.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun fromLowHigh(low: RemoteInt, high: RemoteInt): RemoteLong {
            return RemoteLong(low, high)
        }

        /**
         * Creates a named [RemoteLong] with an initial value. Named remote longs can be set via
         * AndroidRemoteContext.setNamedLong.
         *
         * @param name The unique name for this remote long.
         * @param defaultValue The initial [Long] value for the named remote long.
         * @param domain The domain of the named long (defaults to [RemoteState.Domain.User]).
         * @return A [RemoteLong] representing the named long.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun createNamedRemoteLong(
            name: String,
            defaultValue: Long,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteLong {
            return MutableRemoteLong(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                creationState.document.addNamedLong(domain.prefixed(name), defaultValue)
            }
        }
    }
}

/**
 * A mutable implementation of [RemoteLong].
 *
 * @param constantValueOrNull A nullable value if this [MutableRemoteLong] is constant.
 */
public class MutableRemoteLong
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Long?,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal override val cacheKey: RemoteStateCacheKey,
    low: RemoteInt =
        constantValueOrNull?.let { RemoteInt(it.toInt()) }
            ?: RemoteIntExpression(null, RemoteStateInstanceKey()) {
                throw UnsupportedOperationException("Cannot extract low from dynamic RemoteLong")
            },
    high: RemoteInt =
        constantValueOrNull?.let { RemoteInt((it shr 32).toInt()) }
            ?: RemoteIntExpression(null, RemoteStateInstanceKey()) {
                throw UnsupportedOperationException("Cannot extract high from dynamic RemoteLong")
            },
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteLong(low, high), MutableRemoteState<Long> {

    /**
     * Constructor for [MutableRemoteLong] that allows specifying an optional initial ID. If no ID
     * is provided, a new float variable ID is reserved.
     *
     * @param id An optional explicit ID for this mutable long. If `null`, a new ID is reserved.
     */
    internal constructor(
        id: Int
    ) : this(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id), idProvider = { id })

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    public override fun toString(): String {
        return "MutableRemoteLong@${this.hashCode()} =" + constantValueOrNull
    }

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteLong] instance.
         */
        public fun createMutable(initialValue: Long): MutableRemoteLong {
            return MutableRemoteLong(
                constantValueOrNull = null,
                cacheKey = RemoteStateInstanceKey(),
            ) { creationState ->
                creationState.document.addLong(initialValue)
            }
        }

        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteLong] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Int): MutableRemoteLong = MutableRemoteLong(id)
    }
}

/**
 * Factory composable for mutable remote long state.
 *
 * @param initialValue The initial [Long] value.
 * @return A [MutableRemoteLong] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberMutableRemoteLong(initialValue: Long): MutableRemoteLong {
    return remember { MutableRemoteLong.createMutable(initialValue) }
}

/** Factory composable for mutable remote long state. */
@Composable
@Deprecated("Use rememberMutableRemoteLong(value())")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRemoteLongValue(value: () -> Long): MutableRemoteLong =
    rememberMutableRemoteLong(value())

/**
 * Remembers a named remote long expression.
 *
 * @param name The unique name for this remote long.
 * @param domain The domain of the named long (defaults to [RemoteState.Domain.User]).
 * @param defaultValue The initial long value.
 * @return A [RemoteLong] representing the named remote long expression.
 */
@Composable
public fun rememberNamedRemoteLong(
    name: String,
    defaultValue: Long,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteLong {
    return rememberNamedState(name, domain) {
        RemoteLong.createNamedRemoteLong(name, defaultValue, domain)
    }
}

/** A Composable function to remember and provide a **named** mutable remote long value. */
@Composable
@Deprecated("Use rememberNamedRemoteLong(name, domain, defaultValue = content)")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRemoteLongValue(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> Long,
): RemoteLong {
    return rememberNamedState(name, domain) {
        val initial = value()
        MutableRemoteLong(
            constantValueOrNull = null,
            cacheKey = RemoteNamedCacheKey(domain, name),
        ) { creationState ->
            val id = creationState.document.addNamedLong(domain.prefixed(name), initial)
            creationState.document.setStringName(id, domain.prefixed(name))
            id
        }
    }
}

internal enum class RemoteLongOp {
    Emulated,
    Add,
    Sub,
    Mul,
}
