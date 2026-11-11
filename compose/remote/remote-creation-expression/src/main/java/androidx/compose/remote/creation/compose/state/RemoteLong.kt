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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition

/**
 * Read-only supertype for all remote 64-bit integer representations.
 *
 * [RemoteLong] represents a 64-bit signed integer value that may be a constant, a named variable,
 * or a dynamic expression. For writable long variables, see [MutableRemoteLong].
 *
 * Operations on [RemoteLong] are evaluated dynamically on the remote player at runtime unless all
 * inputs are known constants, in which case constant folding evaluates the result immediately at
 * creation time (see [RemoteState]).
 */
@Stable
public open class RemoteLong
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val low: RemoteInt,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val high: RemoteInt,
    cacheKey: RemoteStateCacheKey =
        RemoteOperationCacheKey.create(RemoteLongOp.FromLowHigh, low, high),
    private val idProvider: ((creationState: RemoteComposeCreationState) -> Int)? = null,
) : BaseRemoteState<Long>(cacheKey) {

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Long?
        get() {
            val l = low.constantValueOrNull ?: return null
            val h = high.constantValueOrNull ?: return null
            return (h.toLong() shl 32) or (l.toLong() and 0xFFFFFFFFL)
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String = constantValueOrNull?.toString() ?: super.toDebugString()

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val provider =
            idProvider
                ?: throw UnsupportedOperationException(
                    "RemoteLong cannot be directly written to document yet"
                )
        return provider(creationState)
    }

    /**
     * Returns a new [RemoteLong] that evaluates to this [RemoteLong] plus [other].
     *
     * @param other The [RemoteLong] to add.
     * @return A new [RemoteLong] representing `this + other`.
     */
    public operator fun plus(other: RemoteLong): RemoteLong {
        // Emulates 64-bit addition using 32-bit integer arithmetic (Multiple-precision arithmetic).
        //
        // The algorithm computes the lower 32-bit sum and manually detects carry-out by checking if
        // the unsigned sum is less than either unsigned operand. Since `RemoteInt` handles signed
        // values, it flips the sign bit using `xor Int.MIN_VALUE` to perform unsigned comparisons.
        // See: https://www.nayuki.io/page/unsigned-int-considered-harmful-for-java (Unsigned
        // comparison)
        val lowAdd = this.low + other.low
        val minVal = Int.MIN_VALUE.ri
        val carry = selectIfLt(lowAdd xor minVal, this.low xor minVal, 1.ri, 0.ri)
        val highAdd = this.high + other.high + carry
        val key = RemoteOperationCacheKey.create(RemoteLongOp.Add, this@RemoteLong, other)
        return object : RemoteLong(lowAdd, highAdd, key) {}
    }

    /**
     * Returns a new [RemoteLong] that evaluates to this [RemoteLong] minus [other].
     *
     * @param other The [RemoteLong] to subtract.
     * @return A new [RemoteLong] representing `this - other`.
     */
    public operator fun minus(other: RemoteLong): RemoteLong {
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
        val lowSub = this.low - other.low
        val minVal = Int.MIN_VALUE.ri
        val borrow = selectIfLt(this.low xor minVal, other.low xor minVal, 1.ri, 0.ri)
        val highSub = this.high - other.high - borrow
        val key = RemoteOperationCacheKey.create(RemoteLongOp.Sub, this@RemoteLong, other)
        return object : RemoteLong(lowSub, highSub, key) {}
    }

    /**
     * Returns a new [RemoteLong] that evaluates to this [RemoteLong] times [other].
     *
     * @param other The [RemoteLong] to multiply by.
     * @return A new [RemoteLong] representing `this * other`.
     */
    public operator fun times(other: RemoteLong): RemoteLong {
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
        val b0 = other.low and mask
        val b1 = (other.low shr 16.ri) and mask

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

        val finalLow = this.low * other.low
        val finalHigh = (this.high * other.low) + (this.low * other.high) + upper32

        val key = RemoteOperationCacheKey.create(RemoteLongOp.Mul, this@RemoteLong, other)
        return object : RemoteLong(finalLow, finalHigh, key) {}
    }

    /**
     * Returns a [RemoteInt] that evaluates to the truncating conversion of the lower 32 bits of
     * this [RemoteLong].
     *
     * @return A [RemoteInt] representing the lower 32 bits of this [RemoteLong].
     */
    public fun toRemoteInt(): RemoteInt = low

    public companion object {
        /**
         * Creates a [RemoteLong] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteLong] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteLong = MutableRemoteLong(id)

        /**
         * Creates a [RemoteLong] from low and high [RemoteInt]s.
         *
         * @param low The lower 32 bits.
         * @param high The upper 32 bits.
         * @return A [RemoteLong] representing the combination of low and high.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromLowHigh(low: RemoteInt, high: RemoteInt): RemoteLong {
            return RemoteLong(low, high)
        }

        /**
         * Creates a named [RemoteLong] with an initial value. Named remote longs can be set via
         * AndroidRemoteContext.setNamedLong.
         *
         * @param name The unique name for this remote long.
         * @param defaultValue The initial [Long] value for the named remote long.
         * @param domain The domain of the named long (defaults to [Domain.User]).
         * @return A [RemoteLong] representing the named long.
         */
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
@RememberInComposition
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Long?,
    cacheKey: RemoteStateCacheKey,
    low: RemoteInt =
        constantValueOrNull?.let { RemoteInt(it.toInt()) }
            ?: RemoteInt(null, RemoteStateInstanceKey()) {
                throw UnsupportedOperationException("Cannot extract low from dynamic RemoteLong")
            },
    high: RemoteInt =
        constantValueOrNull?.let { RemoteInt((it shr 32).toInt()) }
            ?: RemoteInt(null, RemoteStateInstanceKey()) {
                throw UnsupportedOperationException("Cannot extract high from dynamic RemoteLong")
            },
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteLong(low, high, cacheKey), MutableRemoteState<Long> {

    /**
     * Constructor for [MutableRemoteLong] that allows specifying an optional initial ID. If no ID
     * is provided, a new float variable ID is reserved.
     *
     * @param id An optional explicit ID for this mutable long. If `null`, a new ID is reserved.
     */
    @RememberInComposition
    internal constructor(
        id: Int
    ) : this(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id), idProvider = { id })

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    public override fun toString(): String {
        return "MutableRemoteLong@${this.hashCode()} =" + constantValueOrNull
    }

    internal companion object {
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
 * Creates a [RemoteLong] from a constant [Long] [value].
 *
 * @param value The constant [Long] value.
 * @return A [RemoteLong] representing the constant long.
 */
public fun RemoteLong(value: Long): RemoteLong =
    RemoteLong(
        low = RemoteInt(value.toInt()),
        high = RemoteInt((value shr 32).toInt()),
        cacheKey = RemoteConstantCacheKey(value),
        idProvider = { creationState -> creationState.document.addLong(value) },
    )

/**
 * Creates a [MutableRemoteLong] initialized with [initialValue].
 *
 * @param initialValue The initial [Long] value.
 * @return A [MutableRemoteLong] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteLong(initialValue: Long): MutableRemoteLong =
    MutableRemoteLong(
        constantValueOrNull = initialValue,
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState -> creationState.document.addLong(initialValue) },
    )

internal enum class RemoteLongOp(val symbol: String? = null) : RemoteOperation {
    FromLowHigh,
    Add("+"),
    Sub("-"),
    Mul("*");

    override val precedence: Int
        get() =
            when (this) {
                Mul -> 4
                Add,
                Sub -> 3
                else -> 100
            }

    override fun toDebugString(args: List<RemoteStateCacheKey>): String {
        if (symbol != null && args.size == 2) {
            return args.formatOp(symbol, precedence)
        }
        return formatCamelCaseFunction(args)
    }

    override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
        return when (this) {
            FromLowHigh -> {
                val low = args[0] as RemoteInt
                val high = args[1] as RemoteInt
                object : RemoteLong(low, high) {}
            }
            Add -> (args[0] as RemoteLong) + (args[1] as RemoteLong)
            Sub -> (args[0] as RemoteLong) - (args[1] as RemoteLong)
            Mul -> (args[0] as RemoteLong) * (args[1] as RemoteLong)
        }
    }
}
