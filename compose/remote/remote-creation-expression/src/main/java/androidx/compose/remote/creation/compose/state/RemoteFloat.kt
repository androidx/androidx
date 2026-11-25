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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY3
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.Utils.asNan
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.document.MaybeVariableFloat
import androidx.compose.remote.document.RemoteFloatId
import androidx.compose.remote.document.RemoteLongId
import androidx.compose.remote.document.floatArrayOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import kotlin.reflect.KClass

/**
 * Read-only supertype for all remote float representations.
 *
 * [RemoteFloat] represents a floating-point value that may be a constant, a named variable, or a
 * dynamic expression (such as an arithmetic operation). For writable float variables, see
 * [MutableRemoteFloat].
 *
 * Operations on [RemoteFloat] are evaluated dynamically on the remote player at runtime unless all
 * inputs are known constants, in which case constant folding evaluates the result immediately at
 * creation time (see [RemoteValue]).
 *
 * Comparing [RemoteFloat] values with `==` checks structural equality (matching constant values or
 * expression DAGs), whereas [MutableRemoteFloat] instances compare by identity; see [RemoteValue].
 * For runtime equality on the remote player, use [isEqualTo].
 */
@Stable
public sealed interface RemoteFloat : RemoteValue<Float> {
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Float?

    /**
     * Converts this [RemoteFloat] to a [RemoteInt] by truncating any fractional part toward zero
     * (matching [Float.toInt]).
     *
     * Use [round] first to round to the nearest integer before converting.
     *
     * @return A [RemoteInt] that evaluates to the truncated integer value of this [RemoteFloat].
     */
    public fun toRemoteInt(): RemoteInt

    /**
     * Converts this [RemoteFloat] to a [RemoteString] using the default localized decimal format.
     *
     * @return A [RemoteString] representing the formatted float.
     */
    public fun toRemoteString(): RemoteString

    /**
     * Formats this [RemoteFloat] as a [RemoteString] with integer padding options.
     *
     * @param before maximum digits before the decimal point (stored as a short on the wire)
     * @param after maximum digits after the decimal point (stored as a short on the wire)
     * @param flags formatting flags from [androidx.compose.remote.core.operations.TextFromFloat]
     * @param minimumIntegerDigits minimum integer digits before padding
     * @param formatWidth total target padded width, or `0` if unset
     * @param padCharacter character used for pre-padding (`' '` or `'0'`)
     * @return formatted [RemoteString]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteStringWithPadding(
        before: Short,
        after: Short,
        flags: Int,
        minimumIntegerDigits: Int = 0,
        formatWidth: Int = 0,
        padCharacter: Char = ' ',
    ): RemoteString

    /**
     * Formats this [RemoteFloat] as a [RemoteString] with explicit digit counts and [flags].
     *
     * @param before digits before the decimal point (stored as a short on the wire)
     * @param after digits after the decimal point (stored as a short on the wire)
     * @param flags formatting flags from [androidx.compose.remote.core.operations.TextFromFloat]
     * @return formatted [RemoteString]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteStringOptions(before: Short, after: Short, flags: Int): RemoteString

    /**
     * Returns a new [RemoteFloat] that evaluates to the negative of this [RemoteFloat].
     *
     * @return A new [RemoteFloat] representing `-this`.
     */
    public operator fun unaryMinus(): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [other].
     *
     * @param other The divisor [Float].
     * @return A new [RemoteFloat] representing `this % other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public operator fun rem(other: Float): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [other].
     *
     * @param other The divisor [RemoteFloat].
     * @return A new [RemoteFloat] representing `this % other`.
     */
    public operator fun rem(other: RemoteFloat): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to the minimum of this [RemoteFloat] and [other].
     *
     * @param other The [Float] to compare with.
     * @return A new [RemoteFloat] representing the smaller of `this` and [other].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun min(other: Float): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to the minimum of this [RemoteFloat] and [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A new [RemoteFloat] representing the smaller of `this` and [other].
     */
    public fun min(other: RemoteFloat): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [other].
     *
     * @param other The [Float] to add.
     * @return A new [RemoteFloat] representing `this + other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public operator fun plus(other: Float): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [other].
     *
     * @param other The [RemoteFloat] to add.
     * @return A new [RemoteFloat] representing `this + other`.
     */
    public operator fun plus(other: RemoteFloat): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [other].
     *
     * @param other The [Float] to subtract.
     * @return A new [RemoteFloat] representing `this - other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public operator fun minus(other: Float): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [other].
     *
     * @param other The [RemoteFloat] to subtract.
     * @return A new [RemoteFloat] representing `this - other`.
     */
    public operator fun minus(other: RemoteFloat): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [other].
     *
     * @param other The [Float] to multiply by.
     * @return A new [RemoteFloat] representing `this * other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public operator fun times(other: Float): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [other].
     *
     * @param other The [RemoteFloat] to multiply by.
     * @return A new [RemoteFloat] representing `this * other`.
     */
    public operator fun times(other: RemoteFloat): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] divided by [other].
     *
     * @param other The divisor [Float].
     * @return A new [RemoteFloat] representing `this / other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public operator fun div(other: Float): RemoteFloat

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] divided by [other].
     *
     * @param other The divisor [RemoteFloat].
     * @return A new [RemoteFloat] representing `this / other`.
     */
    public operator fun div(other: RemoteFloat): RemoteFloat

    /**
     * Returns a [RemoteFloat] that is a reference of this RemoteFloat.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     *
     * @param forceRemote If true, forces the creation of a remote reference even if the value is
     *   constant.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createReference(forceRemote: Boolean = false): RemoteFloat

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this == other`.
     */
    public fun isEqualTo(other: RemoteFloat): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is not equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this != other`.
     */
    public fun isNotEqualTo(other: RemoteFloat): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is less than the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this < other`.
     */
    public fun isLessThan(other: RemoteFloat): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is less than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this <= other`.
     */
    public fun isLessThanOrEqualTo(other: RemoteFloat): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is greater than the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this > other`.
     */
    public fun isGreaterThan(other: RemoteFloat): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is greater than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this >= other`.
     */
    public fun isGreaterThanOrEqualTo(other: RemoteFloat): RemoteBoolean

    public companion object {
        internal fun formatAnimationType(type: Int): String =
            when (type) {
                CUBIC_STANDARD -> "CUBIC_STANDARD"
                CUBIC_ACCELERATE -> "CUBIC_ACCELERATE"
                CUBIC_DECELERATE -> "CUBIC_DECELERATE"
                CUBIC_LINEAR -> "CUBIC_LINEAR"
                CUBIC_ANTICIPATE -> "CUBIC_ANTICIPATE"
                CUBIC_OVERSHOOT -> "CUBIC_OVERSHOOT"
                CUBIC_CUSTOM -> "CUBIC_CUSTOM"
                SPLINE_CUSTOM -> "SPLINE_CUSTOM"
                EASE_OUT_BOUNCE -> "EASE_OUT_BOUNCE"
                EASE_OUT_ELASTIC -> "EASE_OUT_ELASTIC"
                else -> type.toString()
            }

        internal fun formatToRemoteString(
            args: List<RemoteStateCacheKey>,
            precedence: Int,
        ): String {
            if (args.size < 4) {
                return "${args[0].toOperandString(precedence)}.toRemoteString()"
            }
            val str = args[1].toDebugString()
            // Check if options match DefaultDecimalFormat (before=255/40, after=3|0, flags=533/21)
            val isDefault =
                (str == "255" || str == "40") &&
                    (args[2].toDebugString() == "3" || args[2].toDebugString() == "0") &&
                    (args[3].toDebugString() == "533" || args[3].toDebugString() == "21")
            return if (isDefault) {
                "${args[0].toOperandString(precedence)}.toRemoteString()"
            } else {
                val obj = args[0].toOperandString(precedence)
                val params = args.joinToDebugString(startIndex = 1)
                "$obj.toRemoteString($params)"
            }
        }

        internal fun isConstant(v: Float): Boolean {
            // Assume all NaNs are variables which are probably non-const.
            return !v.isNaN()
        }

        /**
         * Creates a [RemoteFloat] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteFloat] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(id: RemoteFloatId): RemoteFloat =
            RemoteFloat(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id.id)) { _ ->
                floatArrayOf(id)
            }

        /**
         * Creates a [RemoteFloat] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteFloat] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Float): RemoteFloat = RemoteFloat(id)

        /**
         * Creates a named [RemoteFloat] with an initial value. This allows referring to a float by
         * a symbolic name in the remote document. Named remote floats can be set via
         * AndroidRemoteContext.setNamedFloat.
         *
         * @param name The name of the remote float.
         * @param defaultValue The initial value of the remote float.
         * @param domain The domain of the named float (defaults to [RemoteValue.Domain.User]). This
         *   helps avoid name collisions.
         * @return A [RemoteFloat] representing the named float.
         */
        @JvmStatic
        public fun createNamedRemoteFloat(
            name: String,
            defaultValue: Float,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
        ): RemoteFloat {
            return RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                floatArrayOf(
                    creationState.document.addNamedFloat(domain.prefixed(name), defaultValue)
                )
            }
        }

        /**
         * Creates a named [RemoteFloat] with an initial [defaultValue]. This allows referring to a
         * float by a symbolic name in the remote document. Named remote floats can be set via
         * AndroidRemoteContext.setNamedFloat.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param defaultValue The initial [RemoteFloat] value or expression of the named float.
         * @param domain The domain of the named float (defaults to [RemoteValue.Domain.User]). This
         *   helps avoid name collisions.
         * @return A [RemoteFloat] representing the named float.
         */
        @JvmStatic
        public fun createNamedRemoteFloat(
            name: String,
            defaultValue: RemoteFloat,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
        ): RemoteFloat {
            return RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                val initialValueId = creationState.getFloatId(defaultValue)
                if (Utils.isVariable(initialValueId)) {
                    val floatId = creationState.document.addFloatExpression(initialValueId)
                    creationState.document.setNamedVariable(
                        floatId.id,
                        domain.prefixed(name),
                        NamedVariable.FLOAT_TYPE,
                    )
                    floatArrayOf(floatId)
                } else {
                    val floatId =
                        creationState.document.addNamedFloat(domain.prefixed(name), initialValueId)
                    floatArrayOf(floatId)
                }
            }
        }
    }
}

/**
 * Operation keys identifying [RemoteFloat] AST nodes in [RemoteOperationCacheKey].
 *
 * @property opCode [AnimatedFloatExpression] opcode emitted to RPN bytecode, or [Float.NaN] for
 *   operations without a single float expression opcode
 * @property precedence operator precedence controlling parentheses in [toDebugString]
 * @property symbol infix operator symbol used by [toDebugString], or `null` for non-infix ops
 */
internal enum class RemoteFloatOperationKey(
    internal val opCode: Float,
    override val precedence: Int = 100,
    internal val symbol: String? = null,
) : RemoteOperation {
    ToRemoteInt(Float.NaN),
    ToRemoteString(Float.NaN),
    UnaryMinus(AnimatedFloatExpression.MUL, 4),
    Plus(AnimatedFloatExpression.ADD, 2, "+"),
    Minus(AnimatedFloatExpression.SUB, 2, "-"),
    Times(AnimatedFloatExpression.MUL, 3, "*"),
    Div(AnimatedFloatExpression.DIV, 3, "/"),
    Rem(AnimatedFloatExpression.MOD, 3, "%"),
    Min(AnimatedFloatExpression.MIN),
    Max(AnimatedFloatExpression.MAX),
    Abs(AnimatedFloatExpression.ABS),
    Clamp(AnimatedFloatExpression.CLAMP),
    Reference(Float.NaN),
    SelectIfLT(AnimatedFloatExpression.IFELSE, 0),
    SelectIfLE(AnimatedFloatExpression.IFELSE, 0),
    SelectIfGT(AnimatedFloatExpression.IFELSE, 0),
    SelectIfGE(AnimatedFloatExpression.IFELSE, 0),
    ComparisonOp(AnimatedFloatExpression.IFELSE),
    Mad(AnimatedFloatExpression.MAD),
    Lerp(AnimatedFloatExpression.LERP),
    Floor(AnimatedFloatExpression.FLOOR),
    Ceil(AnimatedFloatExpression.CEIL),
    Log(AnimatedFloatExpression.LOG),
    Ln(AnimatedFloatExpression.LN),
    Rand(AnimatedFloatExpression.RAND),
    RandInRange(AnimatedFloatExpression.RAND_IN_RANGE),
    Round(AnimatedFloatExpression.ROUND),
    Sin(AnimatedFloatExpression.SIN),
    Cos(AnimatedFloatExpression.COS),
    Tan(AnimatedFloatExpression.TAN),
    Asin(AnimatedFloatExpression.ASIN),
    Acos(AnimatedFloatExpression.ACOS),
    Atan(AnimatedFloatExpression.ATAN),
    Atan2(AnimatedFloatExpression.ATAN2),
    Cbrt(AnimatedFloatExpression.CBRT),
    ToDeg(AnimatedFloatExpression.DEG),
    ToRad(AnimatedFloatExpression.RAD),
    Sqrt(AnimatedFloatExpression.SQRT),
    Exp(AnimatedFloatExpression.EXP),
    Pow(AnimatedFloatExpression.POW),
    CopySign(AnimatedFloatExpression.COPY_SIGN),
    Sign(AnimatedFloatExpression.SIGN),
    TimeOfReferenceInSeconds(Float.NaN),
    TimeOfReferenceInMinutes(Float.NaN),
    TimeOfReferenceInHours(Float.NaN),
    DeltaFromReferenceInSeconds(Float.NaN),
    DeltaFromReferenceInMinutes(Float.NaN),
    DeltaFromReferenceInHours(Float.NaN),
    DayOfMonthForReference(Float.NaN),
    MonthOfYearForReference(Float.NaN),
    DayOfWeekForReference(Float.NaN),
    YearForReference(Float.NaN),
    ToInt(Float.NaN),
    CompareEQ(Float.NaN, 1, "=="),
    CompareNE(Float.NaN, 1, "!="),
    CompareLT(Float.NaN, 1, "<"),
    CompareLE(Float.NaN, 1, "<="),
    CompareGT(Float.NaN, 1, ">"),
    CompareGE(Float.NaN, 1, ">="),
    Anim(Float.NaN) {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "animate(${args[0].toDebugString()})"
    },
    Cubic(AnimatedFloatExpression.CUBIC) {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "cubicEasing(${args.joinToDebugString()})"
    },
    Spline(AnimatedFloatExpression.A_SPLINE) {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "evalSpline(${args.joinToDebugString()})"
    },
    SplineLoop(AnimatedFloatExpression.A_SPLINE_LOOP) {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "evalSpline(${args.joinToDebugString()}, loop=true)"
    };

    override fun toDebugString(args: List<RemoteStateCacheKey>): String {
        if (symbol != null && args.size == 2) {
            return args.formatOp(symbol, precedence)
        }
        return when (this) {
            UnaryMinus -> "-${args[0].toOperandString(precedence)}"
            ToRemoteInt,
            ToInt -> "${args[0].toOperandString(precedence)}.toRemoteInt()"
            ToRemoteString -> RemoteFloat.formatToRemoteString(args, precedence)
            SelectIfLT -> args.formatSelect("<")
            SelectIfLE -> args.formatSelect("<=")
            SelectIfGT -> args.formatSelect(">")
            SelectIfGE -> args.formatSelect(">=")
            else -> formatCamelCaseFunction(args)
        }
    }

    override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
        return when (this) {
            Plus -> (args[0] as RemoteFloat) + (args[1] as RemoteFloat)
            Minus -> (args[0] as RemoteFloat) - (args[1] as RemoteFloat)
            Times -> (args[0] as RemoteFloat) * (args[1] as RemoteFloat)
            Div -> (args[0] as RemoteFloat) / (args[1] as RemoteFloat)
            Rem -> (args[0] as RemoteFloat) % (args[1] as RemoteFloat)
            UnaryMinus -> -(args[0] as RemoteFloat)
            Min -> min(args[0] as RemoteFloat, args[1] as RemoteFloat)
            Max -> max(args[0] as RemoteFloat, args[1] as RemoteFloat)
            Abs -> abs(args[0] as RemoteFloat)
            Clamp ->
                clamp(
                    value = args[2] as RemoteFloat,
                    min = args[0] as RemoteFloat,
                    max = args[1] as RemoteFloat,
                )
            SelectIfLT ->
                selectIfLt(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                    args[3] as RemoteFloat,
                )
            SelectIfLE ->
                selectIfLe(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                    args[3] as RemoteFloat,
                )
            SelectIfGT ->
                selectIfGt(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                    args[3] as RemoteFloat,
                )
            SelectIfGE ->
                selectIfGe(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                    args[3] as RemoteFloat,
                )
            Mad -> mad(args[0] as RemoteFloat, args[1] as RemoteFloat, args[2] as RemoteFloat)
            Lerp -> lerp(args[0] as RemoteFloat, args[1] as RemoteFloat, args[2] as RemoteFloat)
            Floor -> floor(args[0] as RemoteFloat)
            Ceil -> ceil(args[0] as RemoteFloat)
            Log -> log(args[0] as RemoteFloat)
            Ln -> ln(args[0] as RemoteFloat)
            Rand -> rand()
            RandInRange -> randRange(args[0] as RemoteFloat, args[1] as RemoteFloat)
            Round -> round(args[0] as RemoteFloat)
            Sin -> sin(args[0] as RemoteFloat)
            Cos -> cos(args[0] as RemoteFloat)
            Tan -> tan(args[0] as RemoteFloat)
            Asin -> asin(args[0] as RemoteFloat)
            Acos -> acos(args[0] as RemoteFloat)
            Atan -> atan(args[0] as RemoteFloat)
            Atan2 -> atan2(args[0] as RemoteFloat, args[1] as RemoteFloat)
            Cbrt -> cbrt(args[0] as RemoteFloat)
            ToDeg -> toDeg(args[0] as RemoteFloat)
            ToRad -> toRad(args[0] as RemoteFloat)
            Sqrt -> sqrt(args[0] as RemoteFloat)
            Exp -> exp(args[0] as RemoteFloat)
            Pow -> pow(args[0] as RemoteFloat, args[1] as RemoteFloat)
            CopySign -> copySign(args[0] as RemoteFloat, args[1] as RemoteFloat)
            Sign -> sign(args[0] as RemoteFloat)
            ToInt,
            ToRemoteInt -> (args[0] as RemoteFloat).toRemoteInt()
            ToRemoteString ->
                (args[0] as RemoteFloat).toRemoteStringOptions(
                    ((args.getOrNull(1) as? RemoteInt)?.constantValueOrNull ?: 0).toShort(),
                    ((args.getOrNull(2) as? RemoteInt)?.constantValueOrNull ?: 0).toShort(),
                    (args.getOrNull(3) as? RemoteInt)?.constantValueOrNull ?: 0,
                )
            CompareEQ -> (args[0] as RemoteFloat).isEqualTo(args[1] as RemoteFloat)
            CompareNE -> (args[0] as RemoteFloat).isNotEqualTo(args[1] as RemoteFloat)
            CompareLT -> (args[0] as RemoteFloat).isLessThan(args[1] as RemoteFloat)
            CompareLE -> (args[0] as RemoteFloat).isLessThanOrEqualTo(args[1] as RemoteFloat)
            CompareGT -> (args[0] as RemoteFloat).isGreaterThan(args[1] as RemoteFloat)
            CompareGE -> (args[0] as RemoteFloat).isGreaterThanOrEqualTo(args[1] as RemoteFloat)
            Reference ->
                (args[0] as RemoteFloat).createReference(
                    (args.getOrNull(1) as? RemoteBoolean)?.constantValueOrNull ?: false
                )
            DeltaFromReferenceInSeconds -> deltaFromReferenceInSeconds(args[0] as RemoteLong)
            DeltaFromReferenceInMinutes -> deltaFromReferenceInMinutes(args[0] as RemoteLong)
            DeltaFromReferenceInHours -> deltaFromReferenceInHours(args[0] as RemoteLong)
            TimeOfReferenceInSeconds -> timeOfReferenceInSeconds(args[0] as RemoteLong)
            TimeOfReferenceInMinutes -> timeOfReferenceInMinutes(args[0] as RemoteLong)
            TimeOfReferenceInHours -> timeOfReferenceInHours(args[0] as RemoteLong)
            DayOfMonthForReference -> dayOfMonthForReference(args[0] as RemoteLong)
            MonthOfYearForReference -> monthOfYearForReference(args[0] as RemoteLong)
            DayOfWeekForReference -> dayOfWeekForReference(args[0] as RemoteLong)
            YearForReference -> yearForReference(args[0] as RemoteLong)
            Anim ->
                animatedRemoteFloat(
                    args[0] as RemoteFloat,
                    (args[1] as? FloatArrayRemoteValue)?.floatArray
                        ?: (args[1].cacheKey as FloatArrayCacheKey).floatArray,
                )
            Cubic ->
                cubicEasing(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                    args[3] as RemoteFloat,
                    args[4] as RemoteFloat,
                )
            Spline ->
                evalSpline(
                    args[0] as RemoteFloatArray,
                    loop = false,
                    progress = args[1] as RemoteFloat,
                )
            SplineLoop ->
                evalSpline(
                    args[0] as RemoteFloatArray,
                    loop = true,
                    progress = args[1] as RemoteFloat,
                )
            ComparisonOp ->
                selectIfLt(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                    args[3] as RemoteFloat,
                )
        }
    }
}

internal fun RemoteFloat(
    constantValueOrNull: Float?,
    cacheKey: RemoteStateCacheKey,
    arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
): RemoteFloat = RemoteFloatImpl(constantValueOrNull, cacheKey, arrayProvider)

internal val RemoteFloat.arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
    get() = (this as BaseRemoteFloat).arrayProvider

/**
 * Returns the cached RPN [FloatArray] for [creationState], computing it if needed.
 *
 * @param creationState state holding the expression cache
 * @return RPN [FloatArray] for this [RemoteFloat]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloat.arrayForCreationState(
    creationState: RemoteComposeCreationState
): FloatArray = creationState.getOrPutFloatArray(cacheKey) { arrayProvider(creationState) }

/**
 * Returns `true` if this [RemoteFloat] already has a variable ID in [creationState].
 *
 * @param creationState state tracking written variable IDs
 */
internal fun RemoteFloat.hasBeenWrittenToDoc(creationState: RemoteComposeCreationState): Boolean =
    creationState.remoteVariableToId.contains(cacheKey)

/**
 * Boilerplate for implementing an unary operation.
 *
 * @param opCode The opcode to insert in the generated [FloatArray] if the source isn't a const int.
 * @param directEval When the source is a const int, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun RemoteFloat.unaryOp(
    op: RemoteFloatOperationKey,
    opCode: Float = op.opCode,
    directEval: (Float) -> Float,
): RemoteFloat {
    constantValueOrNull?.let {
        return RemoteFloat(directEval(it))
    }

    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, this),
    ) { creationState ->
        combineToFloatArray(creationState, arrayOf(this), opCode)
    }
}

internal abstract class BaseRemoteFloat(cacheKey: RemoteStateCacheKey) :
    BaseRemoteValue<Float>(cacheKey), RemoteFloat {
    override val equalityType: KClass<*>
        get() = RemoteFloat::class

    abstract val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray

    override fun toRemoteInt(): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(it.toInt())
        }
        return RemoteInt(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(RemoteFloatOperationKey.ToInt, this),
        ) { creationState ->
            longArrayOf(getLongId(creationState))
        }
    }

    override fun toRemoteString(): RemoteString =
        toRemoteStringWithPadding(
            before = 40,
            after = 3,
            flags = SEPARATOR_COMMA_PERIOD or GROUPING_BY3 or PAD_AFTER_NONE or PAD_PRE_NONE,
        )

    override fun toRemoteStringWithPadding(
        before: Short,
        after: Short,
        flags: Int,
        minimumIntegerDigits: Int,
        formatWidth: Int,
        padCharacter: Char,
    ): RemoteString {
        val padPre = minimumIntegerDigits > 1 || formatWidth > 0
        if (padPre) {
            val padWidth = if (formatWidth > 0) formatWidth else minimumIntegerDigits

            // Support scenarios where max digits was not restricted (before == 255)
            if (before.toInt() == 255) {
                val flagsWithoutPrePadding = flags and PAD_PRE_ZERO.inv()
                val flagsPreNone = flagsWithoutPrePadding or PAD_PRE_NONE
                val unpadded = toRemoteStringOptions(before, after, flagsPreNone)
                val isSpacePadded = formatWidth > 0 && padCharacter == ' '
                val flagsPadded =
                    flagsWithoutPrePadding or if (isSpacePadded) PAD_PRE_SPACE else PAD_PRE_ZERO

                var threshold = 1f
                for (i in 1 until padWidth) {
                    threshold *= 10f
                }

                val isNegative = this.isLessThan(0f.rf)
                val absValue = isNegative.select(-this, this)
                val paddedBefore = padWidth.coerceIn(0, Short.MAX_VALUE.toInt()).toShort()

                return (absValue.isLessThan(threshold.rf)).select(
                    toRemoteStringOptions(paddedBefore, after, flagsPadded),
                    unpadded,
                )
            }
        }

        return toRemoteStringOptions(before, after, flags)
    }

    override fun toRemoteStringOptions(before: Short, after: Short, flags: Int): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(floatToString(it, before = before, after = after, flags = flags))
        }
        return RemoteString(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(
                    RemoteFloatOperationKey.ToRemoteString,
                    this,
                    before,
                    after,
                    flags,
                ),
            lazyRemoteString =
                object : LazyRemoteString {
                    override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                        return creationState.document
                            .textFromFloat(
                                MaybeVariableFloat.variable(getId(creationState)),
                                before.toInt(),
                                after.toInt(),
                                flags,
                            )
                            .id
                    }

                    override fun computeRequiredCodePointSet(
                        creationState: RemoteComposeCreationState
                    ): Set<String> {
                        val preFlags = flags and PAD_PRE_ZERO
                        val afterFlags = flags and PAD_AFTER_ZERO
                        if (after.toInt() == 0) {
                            if (before.toInt() == 1 || preFlags != PAD_PRE_SPACE) {
                                return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                            } else {
                                return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
                            }
                        }

                        // If flags is non-zero then we may pad with a space.
                        if (
                            (before.toInt() == 1 && after.toInt() == 1) ||
                                (preFlags != PAD_PRE_SPACE && afterFlags != PAD_AFTER_SPACE)
                        ) {
                            return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".")
                        } else {
                            return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", " ")
                        }
                    }
                },
        )
    }

    override fun unaryMinus(): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(-it)
        }

        return binaryOp(
            this,
            -1f,
            RemoteFloatOperationKey.UnaryMinus,
            directEval = { a, b -> a * b },
        )
    }

    override fun rem(other: Float): RemoteFloat =
        binaryOp(this, other, RemoteFloatOperationKey.Rem) { a, b -> a % b }

    override fun rem(other: RemoteFloat): RemoteFloat =
        binaryOp(this, other, RemoteFloatOperationKey.Rem) { a, b -> a % b }

    override fun min(other: Float): RemoteFloat =
        binaryOp(
            this,
            other,
            RemoteFloatOperationKey.Min,
            directEval = { a, b -> kotlin.math.min(a, b) },
        )

    override fun min(other: RemoteFloat): RemoteFloat =
        binaryOp(this, other, RemoteFloatOperationKey.Min) { a, b -> kotlin.math.min(a, b) }

    override fun plus(other: Float): RemoteFloat {
        if (other == 0f) {
            return this
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Plus, directEval = { a, b -> a + b })
    }

    override fun plus(other: RemoteFloat): RemoteFloat {
        other.constantValueOrNull?.let {
            return plus(it)
        }
        constantValueOrNull?.let {
            return other.plus(it)
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Plus) { a, b -> a + b }
    }

    override fun minus(other: Float): RemoteFloat {
        if (other == 0f) {
            return this
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Minus, directEval = { a, b -> a - b })
    }

    override fun minus(other: RemoteFloat): RemoteFloat {
        other.constantValueOrNull?.let {
            return minus(it)
        }
        constantValueOrNull?.let {
            return (-other).plus(it)
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Minus) { a, b -> a - b }
    }

    override fun times(other: Float): RemoteFloat {
        if (other == 0f) {
            return RemoteFloat(0f)
        }
        if (other == 1f) {
            return this
        }
        if (constantValueOrNull != null && constantValueOrNull == 1f) {
            return RemoteFloat(other)
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Times, directEval = { a, b -> a * b })
    }

    override fun times(other: RemoteFloat): RemoteFloat {
        other.constantValueOrNull?.let {
            return times(it)
        }
        constantValueOrNull?.let {
            return other.times(it)
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Times) { a, b -> a * b }
    }

    override fun div(other: Float): RemoteFloat {
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return RemoteFloat(0f)
        }
        if (other == 1f) {
            return this
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Div, directEval = { a, b -> a / b })
    }

    override fun div(other: RemoteFloat): RemoteFloat {
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return RemoteFloat(0f)
        }
        other.constantValueOrNull?.let {
            return div(it)
        }
        return binaryOp(this, other, RemoteFloatOperationKey.Div) { aVal: Float, bVal: Float ->
            aVal / bVal
        }
    }

    override fun createReference(forceRemote: Boolean): RemoteFloat {
        return RemoteFloat(
            constantValueOrNull = if (forceRemote) null else constantValueOrNull,
            cacheKey =
                RemoteOperationCacheKey.create(
                    RemoteFloatOperationKey.Reference,
                    this,
                    forceRemote,
                ),
        ) { creationState ->
            floatArrayOf(asNan(getId(creationState)))
        }
    }

    override fun isEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteFloatOperationKey.CompareEQ,
            { a, b ->
                floatArrayOf(
                    1f,
                    0f,
                    *b,
                    *a,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.ABS,
                    AnimatedFloatExpression.IFELSE,
                )
            },
            FloatComparisonOp.EQ,
        ) { a, b ->
            a == b
        }

    override fun isNotEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteFloatOperationKey.CompareNE,
            { a, b ->
                floatArrayOf(
                    0f,
                    1f,
                    *b,
                    *a,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.ABS,
                    AnimatedFloatExpression.IFELSE,
                )
            },
            FloatComparisonOp.NE,
        ) { a, b ->
            a != b
        }

    override fun isLessThan(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteFloatOperationKey.CompareLT,
            { a, b ->
                floatArrayOf(
                    0f,
                    1f,
                    *b,
                    *a,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.IFELSE,
                )
            },
            FloatComparisonOp.LT,
        ) { a, b ->
            a < b
        }

    override fun isLessThanOrEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteFloatOperationKey.CompareLE,
            { a, b ->
                floatArrayOf(
                    1f,
                    0f,
                    *a,
                    *b,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.IFELSE,
                )
            },
            FloatComparisonOp.LE,
        ) { a, b ->
            a <= b
        }

    override fun isGreaterThan(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteFloatOperationKey.CompareGT,
            { a, b ->
                floatArrayOf(
                    0f,
                    1f,
                    *a,
                    *b,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.IFELSE,
                )
            },
            FloatComparisonOp.GT,
        ) { a, b ->
            a > b
        }

    override fun isGreaterThanOrEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteFloatOperationKey.CompareGE,
            { a, b ->
                floatArrayOf(
                    1f,
                    0f,
                    *b,
                    *a,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.IFELSE,
                )
            },
            FloatComparisonOp.GE,
        ) { a, b ->
            a >= b
        }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        writeFloatToDocument(creationState)

    override fun getFloatId(creationState: RemoteComposeCreationState): Float =
        floatIdForCreationState(creationState)
}

private class RemoteFloatImpl(
    override val constantValueOrNull: Float?,
    cacheKey: RemoteStateCacheKey,
    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
) : BaseRemoteFloat(cacheKey) {
    init {
        if (constantValueOrNull?.isNaN() == true) {
            throw IllegalArgumentException("constantValueOrNull cannot be NaN")
        }
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the source is a const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: Float,
    b: RemoteFloat,
    op: RemoteFloatOperationKey,
    opCode: Float = op.opCode,
    directEval: (Float, Float) -> Float,
): RemoteFloat {
    val bConst = b.constantValueOrNull
    if (!a.isNaN() && bConst != null) {
        return RemoteFloat(directEval(a, bConst))
    }

    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, a, b),
    ) { creationState ->
        combineToFloatArray(creationState, arrayOf(RemoteFloat(a), b), opCode)
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation. Supports peephole optmizations
 * potentially folding this operation into the previous one (e.g. folding several additions into
 * one).
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the source is a const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: RemoteFloat,
    b: Float,
    op: RemoteFloatOperationKey,
    opCode: Float = op.opCode,
    directEval: (Float, Float) -> Float,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    if (aConst != null && !b.isNaN()) {
        return RemoteFloat(directEval(aConst, b))
    }

    // 1. Principled Ternary Peephole: operate directly on the RemoteFloat AST
    if (a is RemoteFloatSelect && a.hasConstantBranches) {
        val newTrue = directEval(a.ifTrue.constantValueOrNull!!, b)
        val newFalse = directEval(a.ifFalse.constantValueOrNull!!, b)
        if (!newTrue.isNaN() && !newFalse.isNaN()) {
            return a.condition.createSelect(RemoteFloat(newTrue), RemoteFloat(newFalse))
        }
    }

    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, a, b),
    ) { creationState ->
        // 2. Trailing Constant Peephole (associativity): only evaluate aArray when needed
        val aArray = a.arrayForCreationState(creationState)
        val last = aArray.last()
        if (aArray.size > 2 && last.isNaN()) {
            foldTrailingConstantForOp(aArray, Utils.idFromNan(last), op, b)
                ?: combineToFloatArray(creationState, arrayOf(a), b, opCode)
        } else {
            combineToFloatArray(creationState, arrayOf(a), b, opCode)
        }
    }
}

private fun foldTrailingConstantForOp(
    array: FloatArray,
    lastOp: Int,
    op: RemoteFloatOperationKey,
    v: Float,
): FloatArray? {
    val addId = Utils.idFromNan(AnimatedFloatExpression.ADD)
    val subId = Utils.idFromNan(AnimatedFloatExpression.SUB)
    val mulId = Utils.idFromNan(AnimatedFloatExpression.MUL)
    val divId = Utils.idFromNan(AnimatedFloatExpression.DIV)
    return when (op) {
        RemoteFloatOperationKey.Plus ->
            when (lastOp) {
                addId -> array.foldTrailingConstant({ it + v }, ::maybeTrimIfZero)
                subId -> array.foldTrailingConstant({ it - v }, ::maybeTrimIfZero)
                else -> null
            }
        RemoteFloatOperationKey.Minus ->
            when (lastOp) {
                addId -> array.foldTrailingConstant({ it - v }, ::maybeTrimIfZero)
                subId -> array.foldTrailingConstant({ it + v }, ::maybeTrimIfZero)
                else -> null
            }
        RemoteFloatOperationKey.Times ->
            when (lastOp) {
                mulId -> array.foldTrailingConstant({ it * v }, ::maybeTrimIfOne)
                divId -> array.foldTrailingConstant({ it / v }, ::maybeTrimIfOne)
                else -> null
            }
        RemoteFloatOperationKey.Div ->
            when (lastOp) {
                mulId -> array.foldTrailingConstant({ it / v }, ::maybeTrimIfOne)
                divId -> array.foldTrailingConstant({ it * v }, ::maybeTrimIfOne)
                else -> null
            }
        RemoteFloatOperationKey.UnaryMinus ->
            when (lastOp) {
                mulId,
                divId -> array.foldTrailingConstant({ -it }, ::maybeTrimIfOne)
                else -> null
            }
        else -> null
    }
}

private inline fun FloatArray.foldTrailingConstant(
    update: (Float) -> Float,
    trim: (FloatArray) -> FloatArray = { it },
): FloatArray? {
    val idx = size - 2
    if (idx < 0 || get(idx).isNaN()) return null
    val copy = clone()
    val updatedValue = update(copy[idx])
    if (updatedValue.isNaN()) return null
    copy[idx] = updatedValue
    return trim(copy)
}

private fun maybeTrimIfZero(array: FloatArray) =
    if (array[array.size - 2] == 0f) {
        array.copyOfRange(0, array.size - 2)
    } else {
        array
    }

private fun maybeTrimIfOne(array: FloatArray) =
    if (array[array.size - 2] == 1f) {
        array.copyOfRange(0, array.size - 2)
    } else {
        array
    }

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: RemoteFloat,
    b: RemoteFloat,
    op: RemoteFloatOperationKey,
    opCode: Float = op.opCode,
    directEval: (Float, Float) -> Float,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteFloat(directEval(aConst, bConst))
    }

    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, a, b),
    ) { creationState ->
        combineToFloatArray(creationState, arrayOf(a, b), opCode)
    }
}

/**
 * Boilerplate for implementing a binary comparison operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param expressionGenerator Generator for the comparison expression [FloatArray] used when both
 *   sources aren\'t a const float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun comparisonOp(
    a: RemoteFloat,
    b: RemoteFloat,
    op: RemoteFloatOperationKey,
    expressionGenerator: (FloatArray, FloatArray) -> FloatArray,
    floatComparisonOp: FloatComparisonOp? = null,
    directEval: (Float, Float) -> Boolean,
): RemoteBoolean {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteBoolean(directEval(aConst, bConst))
    }

    return RemoteBoolean(
        RemoteInt(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(op, a, b),
        ) { creationState ->
            val aArray = a.arrayForCreationState(creationState)
            val bArray = b.arrayForCreationState(creationState)

            // A comparisonOp adds five op codes
            val combinedSize = aArray.size + bArray.size + 5
            val (finalAArray, finalBArray) =
                if (combinedSize > MAX_SAFE_FLOAT_ARRAY) { // Check if new array would exceed limit
                    Pair(
                        floatArrayOf(creationState.getFloatId(a)),
                        floatArrayOf(creationState.getFloatId(b)),
                    )
                } else {
                    Pair(aArray, bArray)
                }

            val id =
                creationState.document.addFloatExpression(
                    *expressionGenerator(finalAArray, finalBArray)
                )
            longArrayOf(0x100000000L + id.id.toLong())
        },
        floatComparison = floatComparisonOp?.let { SelectFloatCondition.FloatComparison(a, b, it) },
    )
}

/** Returns [ifTrue] if [a] < [b], otherwise returns [ifFalse]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLt(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst < bConst) ifTrue else ifFalse
    }
    return SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.LT)
        .createSelect(ifTrue, ifFalse)
}

/** Returns [ifTrue] if [a] <= [b], otherwise returns [ifFalse]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLe(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst <= bConst) ifTrue else ifFalse
    }
    return SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.LE)
        .createSelect(ifTrue, ifFalse)
}

/** Returns [ifTrue] if [a] > [b], otherwise returns [ifFalse]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGt(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst > bConst) ifTrue else ifFalse
    }
    return SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.GT)
        .createSelect(ifTrue, ifFalse)
}

/** Returns [ifTrue] if [a] >= [b], otherwise returns [ifFalse]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGe(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst >= bConst) ifTrue else ifFalse
    }
    return SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.GE)
        .createSelect(ifTrue, ifFalse)
}

/**
 * The difference between now and [referenceEpochMillis] in seconds.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the difference between now and [referenceEpochMillis]
 *   in seconds.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun deltaFromReferenceInSeconds(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.DeltaFromReferenceInSeconds,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_FROM_NOW_SEC.toInt(),
            )
        )
    }
}

/**
 * The difference between now and [referenceEpochMillis] in minutes.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the difference between now and [referenceEpochMillis]
 *   in minutes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun deltaFromReferenceInMinutes(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.DeltaFromReferenceInMinutes,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_FROM_NOW_MIN.toInt(),
            )
        )
    }
}

/**
 * The difference between now and [referenceEpochMillis] in hours.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return @return A [RemoteFloat] that evaluates to the difference between now and
 *   [referenceEpochMillis] in hours.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun deltaFromReferenceInHours(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.DeltaFromReferenceInHours,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_FROM_NOW_HR.toInt(),
            )
        )
    }
}

/**
 * The time of day for [referenceEpochMillis] in seconds (0-59).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the time of day for [referenceEpochMillis] in seconds
 *   (0-59).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun timeOfReferenceInSeconds(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.TimeOfReferenceInSeconds,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_IN_SEC.toInt(),
            )
        )
    }
}

/**
 * The time of day for [referenceEpochMillis] in minutes (0-59).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the time of day for [referenceEpochMillis] in minutes
 *   (0-59).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun timeOfReferenceInMinutes(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.TimeOfReferenceInMinutes,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_IN_MIN.toInt(),
            )
        )
    }
}

/**
 * The time of day for [referenceEpochMillis] in hours (0-23).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the time of day for [referenceEpochMillis] in hours
 *   (0-23).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun timeOfReferenceInHours(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.TimeOfReferenceInHours,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_IN_HR.toInt(),
            )
        )
    }
}

/**
 * The day of month for [referenceEpochMillis] (1-31).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the day of month for [referenceEpochMillis] (1-31).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun dayOfMonthForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.DayOfMonthForReference,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_DAY_OF_MONTH.toInt(),
            )
        )
    }
}

/**
 * The month of year for [referenceEpochMillis] (0-11).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the month of year for [referenceEpochMillis] (0-11).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun monthOfYearForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.MonthOfYearForReference,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_MONTH_VALUE.toInt(),
            )
        )
    }
}

/**
 * The day of week for [referenceEpochMillis] (0-16).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the day of week for [referenceEpochMillis] (0-16).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun dayOfWeekForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    val opKey = RemoteFloatOperationKey.DayOfWeekForReference
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(opKey, referenceEpochMillis),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_DAY_OF_WEEK.toInt(),
            )
        )
    }
}

/**
 * The year of [referenceEpochMillis].
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the year of [referenceEpochMillis].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun yearForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.YearForReference,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                RemoteLongId(creationState.getId(referenceEpochMillis)),
                TimeAttribute.TIME_YEAR.toInt(),
            )
        )
    }
}

internal fun interface FloatIdProvider {
    fun provide(creationState: RemoteComposeCreationState): RemoteFloatId
}

/**
 * A mutable implementation of [RemoteFloat]. It also implements [MutableRemoteValue<Float>].
 *
 * Distinct [MutableRemoteFloat] instances represent separate mutable variables and compare by
 * identity with `==`.
 */
@Stable
public sealed interface MutableRemoteFloat : RemoteFloat, MutableRemoteValue<Float> {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteFloat] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Float): MutableRemoteFloat =
            MutableRemoteFloatImpl(Utils.idFromNan(id))

        /**
         * Creates a [MutableRemoteFloat] backed by a reserved float variable ID with no initial
         * value.
         *
         * @return A [MutableRemoteFloat] whose ID is allocated via `allocateFloatId()`.
         */
        @RememberInComposition
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createReserved(): MutableRemoteFloat =
            MutableRemoteFloatImpl(
                cacheKey = RemoteStateInstanceKey(),
                idProvider = { creationState -> creationState.document.allocateFloatId() },
            )
    }
}

private class MutableRemoteFloatImpl(
    cacheKey: RemoteStateCacheKey,
    private val idProvider: FloatIdProvider,
) : BaseRemoteFloat(cacheKey), MutableRemoteFloat {
    constructor(id: Int) : this(cacheKey = RemoteStateIdKey(id), idProvider = { RemoteFloatId(id) })

    override val constantValueOrNull: Float?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState ->
            // Caching this ID ensures that the same ID is shared when the variable is
            // used in expressions (via arrayProvider) and actions (via getId).
            val id =
                creationState.getOrPutVariableId(cacheKey) { idProvider.provide(creationState).id }
            floatArrayOf(RemoteFloatId(id))
        }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider.provide(creationState).id
}

/**
 * Creates a [RemoteFloat] from a constant [Float] [value].
 *
 * @param value The constant [Float] value.
 * @return A [RemoteFloat] representing the constant float.
 */
public fun RemoteFloat(value: Float): RemoteFloat =
    RemoteFloat(
        constantValueOrNull = if (RemoteFloat.isConstant(value)) value else null,
        cacheKey =
            if (RemoteFloat.isConstant(value)) {
                RemoteConstantCacheKey(value)
            } else {
                RemoteStateIdKey(Utils.idFromNan(value))
            },
        arrayProvider = { _ -> floatArrayOf(value) },
    )

/**
 * Creates a [RemoteFloat] from a lambda evaluated within [RemoteComposeCreationState].
 *
 * @param cacheKey The [RemoteStateCacheKey] for caching this expression.
 * @param value A lambda evaluated within [RemoteComposeCreationState] that provides the
 *   [RemoteFloat].
 * @return A [RemoteFloat] representing the dynamic float expression.
 */
@RememberInComposition
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloat(
    cacheKey: RemoteStateCacheKey = RemoteStateInstanceKey(),
    value: RemoteComposeCreationState.() -> RemoteFloat,
): RemoteFloat =
    RemoteFloat(
        constantValueOrNull = null,
        cacheKey = cacheKey,
        arrayProvider = { creationState ->
            value(creationState).arrayForCreationState(creationState)
        },
    )

/**
 * Creates a [MutableRemoteFloat] initialized with [initialValue].
 *
 * @param initialValue The initial [Float] value.
 * @return A [MutableRemoteFloat] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteFloat(initialValue: Float): MutableRemoteFloat =
    MutableRemoteFloatImpl(
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState -> creationState.document.addMutableFloat(initialValue) },
    )

/**
 * Creates a [MutableRemoteFloat] initialized with [initialValue].
 *
 * @param initialValue The initial [RemoteFloat] value or expression.
 * @return A [MutableRemoteFloat] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteFloat(initialValue: RemoteFloat): MutableRemoteFloat =
    MutableRemoteFloatImpl(
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState ->
            creationState.document.addFloatExpression(
                *initialValue.arrayForCreationState(creationState)
            )
        },
    )

/**
 * Creates an animated [RemoteFloat] combining an [input] [RemoteFloat] with an animation definition
 * ([FloatArray]).
 */
internal fun animatedRemoteFloat(input: RemoteFloat, anim: FloatArray): RemoteFloat =
    RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.Anim,
                input,
                FloatArrayCacheKey(anim),
            ),
    ) { creationState ->
        val array = input.arrayForCreationState(creationState)
        val id =
            creationState.getOrPutFloatExpressionId(array, anim) {
                creationState.document.addAnimatedFloat(array, anim)
            }
        floatArrayOf(asNan(id))
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_STANDARD: Int = 1
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_ACCELERATE: Int = 2
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_DECELERATE: Int = 3
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_LINEAR: Int = 4
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_ANTICIPATE: Int = 5
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_OVERSHOOT: Int = 6
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_CUSTOM: Int = 11
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val SPLINE_CUSTOM: Int = 12
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val EASE_OUT_BOUNCE: Int = 13
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val EASE_OUT_ELASTIC: Int = 14

/** Describes the type of animation RemoteCompose applies to an animated value. */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            CUBIC_STANDARD,
            CUBIC_ACCELERATE,
            CUBIC_DECELERATE,
            CUBIC_LINEAR,
            CUBIC_ANTICIPATE,
            CUBIC_OVERSHOOT,
            CUBIC_CUSTOM,
            SPLINE_CUSTOM,
            EASE_OUT_BOUNCE,
            EASE_OUT_ELASTIC,
        ]
)
internal annotation class AnimationType

/** Extension property to convert an [Int] to a [RemoteFloat]. */
public val Int.rf: RemoteFloat
    get() = RemoteFloat(this.toFloat())

/** Extension property to convert a [Float] to a [RemoteFloat]. */
public val Float.rf: RemoteFloat
    get() = RemoteFloat(this)
