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
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY3
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY32
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY4
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_PERIOD_COMMA
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_SPACE_COMMA
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_UNDER_PERIOD
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.Utils.asNan
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.StringUtils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteFloat.OperationKey
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition

/**
 * Read-only supertype for all remote float representations.
 *
 * [RemoteFloat] represents a floating-point value that may be a constant, a named variable, or a
 * dynamic expression (such as an arithmetic operation). For writable float variables, see
 * [MutableRemoteFloat].
 *
 * Operations on [RemoteFloat] are evaluated dynamically on the remote player at runtime unless all
 * inputs are known constants, in which case constant folding evaluates the result immediately at
 * creation time (see [RemoteState]).
 */
@Stable
public open class RemoteFloat
@RememberInComposition
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Float?,
    cacheKey: RemoteStateCacheKey,
    /** Provides the RPN [FloatArray] expression for the given [RemoteComposeCreationState]. */
    internal open val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
) : BaseRemoteState<Float>(cacheKey) {

    init {
        if (constantValueOrNull?.isNaN() == true) {
            throw IllegalArgumentException("constantValueOrNull cannot be NaN")
        }
    }

    internal constructor(
        cacheKey: RemoteStateCacheKey
    ) : this(
        constantValueOrNull = null,
        cacheKey = cacheKey,
        arrayProvider = { FloatArray(0) },
    )

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = arrayForCreationState(creationState)
        // In case we have a single element array, check if the element is a variable id
        // (and not a 0-argument math operator like RAND); if it is an existing id, just
        // return this one, no need to create a new floatExpression.
        if (
            array.size == 1 && array[0].isNaN() && !AnimatedFloatExpression.isMathOperator(array[0])
        ) {
            return Utils.idFromNan(array[0])
        }

        return creationState.getOrPutFloatExpressionId(array, animation = null) {
            Utils.idFromNan(creationState.document.floatExpression(*array))
        }
    }

    internal enum class OperationKey(
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
                ToRemoteString -> formatToRemoteString(args, precedence)
                SelectIfLT -> args.formatSelect("<")
                SelectIfLE -> args.formatSelect("<=")
                SelectIfGT -> args.formatSelect(">")
                SelectIfGE -> args.formatSelect(">=")
                else -> formatCamelCaseFunction(args)
            }
        }

        override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
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
                    clamp(args[0] as RemoteFloat, args[1] as RemoteFloat, args[2] as RemoteFloat)
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
                        (args.getOrNull(1) as? RemoteInt)?.constantValueOrNull ?: 0,
                        (args.getOrNull(2) as? RemoteInt)?.constantValueOrNull ?: 0,
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
                        (args[1] as? FloatArrayRemoteState)?.floatArray
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

    /**
     * Returns the cached RPN [FloatArray] for [creationState], computing it if needed.
     *
     * @param creationState state holding the expression cache
     * @return RPN [FloatArray] for this [RemoteFloat]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun arrayForCreationState(creationState: RemoteComposeCreationState): FloatArray {
        return creationState.getOrPutFloatArray(cacheKey) { arrayProvider(creationState) }
    }

    /**
     * Returns `true` if this [RemoteFloat] already has a variable ID in [creationState].
     *
     * @param creationState state tracking written variable IDs
     */
    internal fun hasBeenWrittenToDoc(creationState: RemoteComposeCreationState): Boolean =
        creationState.remoteVariableToId.contains(cacheKey)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float {
        constantValueOrNull?.let {
            return it
        }
        val array = arrayForCreationState(creationState)
        if (array.size == 1) {
            return array[0]
        }
        return super.getFloatIdForCreationState(creationState)
    }

    /**
     * Converts this [RemoteFloat] to a [RemoteInt] by truncating any fractional part toward zero
     * (matching [Float.toInt]).
     *
     * Use [round] first to round to the nearest integer before converting.
     *
     * @return A [RemoteInt] that evaluates to the truncated integer value of this [RemoteFloat].
     */
    public fun toRemoteInt(): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(it.toInt())
        }
        return RemoteInt(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToInt, this),
        ) { creationState ->
            longArrayOf(getLongIdForCreationState(creationState))
        }
    }

    /**
     * Converts this [RemoteFloat] to a [RemoteString] using the default localized decimal format.
     *
     * @return A [RemoteString] representing the formatted float.
     */
    public fun toRemoteString(): RemoteString =
        toRemoteStringWithPadding(
            before = 40,
            after = 3,
            flags = SEPARATOR_COMMA_PERIOD or GROUPING_BY3 or PAD_AFTER_NONE or PAD_PRE_NONE,
        )

    /**
     * Formats this [RemoteFloat] as a [RemoteString] with integer padding options.
     *
     * @param before maximum digits before the decimal point
     * @param after maximum digits after the decimal point
     * @param flags formatting flags from [androidx.compose.remote.core.operations.TextFromFloat]
     * @param minimumIntegerDigits minimum integer digits before padding
     * @param formatWidth total target padded width, or `0` if unset
     * @param padCharacter character used for pre-padding (`' '` or `'0'`)
     * @return formatted [RemoteString]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteStringWithPadding(
        before: Int,
        after: Int,
        flags: Int,
        minimumIntegerDigits: Int = 0,
        formatWidth: Int = 0,
        padCharacter: Char = ' ',
    ): RemoteString {
        val padPre = minimumIntegerDigits > 1 || formatWidth > 0
        if (padPre) {
            val padWidth = if (formatWidth > 0) formatWidth else minimumIntegerDigits

            // Support scenarios where max digits was not restricted (before == 255)
            if (before == 255) {
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

                return (absValue.isLessThan(threshold.rf)).select(
                    toRemoteStringOptions(padWidth, after, flagsPadded),
                    unpadded,
                )
            }
        }

        return toRemoteStringOptions(before, after, flags)
    }

    /**
     * Formats this [RemoteFloat] as a [RemoteString] with explicit digit counts and [flags].
     *
     * @param before digits before the decimal point
     * @param after digits after the decimal point
     * @param flags formatting flags from [androidx.compose.remote.core.operations.TextFromFloat]
     * @return formatted [RemoteString]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteStringOptions(before: Int, after: Int, flags: Int): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(floatToString(it, before = before, after = after, flags = flags))
        }
        return MutableRemoteString(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(
                    OperationKey.ToRemoteString,
                    this,
                    before,
                    after,
                    flags,
                ),
            lazyRemoteString =
                object : LazyRemoteString {
                    override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                        return creationState.document.createTextFromFloat(
                            asNan(getIdForCreationState(creationState)),
                            before,
                            after,
                            flags,
                        )
                    }

                    override fun computeRequiredCodePointSet(
                        creationState: RemoteComposeCreationState
                    ): Set<String> {
                        val preFlags = flags and PAD_PRE_ZERO
                        val afterFlags = flags and PAD_AFTER_ZERO
                        if (after == 0) {
                            if (before == 1 || preFlags != PAD_PRE_SPACE) {
                                return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                            } else {
                                return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
                            }
                        }

                        // If flags is non-zero then we may pad with a space.
                        if (
                            (before == 1 && after == 1) ||
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

    /**
     * Boilerplate for implementing an unary operation.
     *
     * @param opCode The opcode to insert in the generated [FloatArray] if the source isn\'t a const
     *   int.
     * @param directEval When the source is a const int, this lambda will be called to evaluate the
     *   result directly.
     */
    internal fun unaryOp(
        op: OperationKey,
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

    /**
     * Returns a new [RemoteFloat] that evaluates to the negative of this [RemoteFloat].
     *
     * @return A new [RemoteFloat] representing `-this`.
     */
    public operator fun unaryMinus(): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(-it)
        }

        return binaryOp(this, -1f, OperationKey.UnaryMinus, directEval = { a, b -> a * b })
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [other].
     *
     * @param other The divisor [Float].
     * @return A new [RemoteFloat] representing `this % other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun rem(other: Float): RemoteFloat =
        binaryOp(this, other, OperationKey.Rem) { a, b -> a % b }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [other].
     *
     * @param other The divisor [RemoteFloat].
     * @return A new [RemoteFloat] representing `this % other`.
     */
    public operator fun rem(other: RemoteFloat): RemoteFloat =
        binaryOp(this, other, OperationKey.Rem) { a, b -> a % b }

    /**
     * Returns a new [RemoteFloat] that evaluates to the minimum of this [RemoteFloat] and [other].
     *
     * @param other The [Float] to compare with.
     * @return A new [RemoteFloat] representing the smaller of `this` and [other].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun min(other: Float): RemoteFloat =
        binaryOp(this, other, OperationKey.Min, directEval = { a, b -> kotlin.math.min(a, b) })

    /**
     * Returns a new [RemoteFloat] that evaluates to the minimum of this [RemoteFloat] and [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A new [RemoteFloat] representing the smaller of `this` and [other].
     */
    public fun min(other: RemoteFloat): RemoteFloat =
        binaryOp(this, other, OperationKey.Min) { a, b -> kotlin.math.min(a, b) }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [other].
     *
     * @param other The [Float] to add.
     * @return A new [RemoteFloat] representing `this + other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(other: Float): RemoteFloat {
        if (other == 0f) {
            return this
        }
        return binaryOp(this, other, OperationKey.Plus, directEval = { a, b -> a + b })
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [other].
     *
     * @param other The [RemoteFloat] to add.
     * @return A new [RemoteFloat] representing `this + other`.
     */
    public operator fun plus(other: RemoteFloat): RemoteFloat {
        other.constantValueOrNull?.let {
            return plus(it)
        }
        constantValueOrNull?.let {
            return other.plus(it)
        }
        return binaryOp(this, other, OperationKey.Plus) { a, b -> a + b }
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [other].
     *
     * @param other The [Float] to subtract.
     * @return A new [RemoteFloat] representing `this - other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun minus(other: Float): RemoteFloat {
        if (other == 0f) {
            return this
        }
        return binaryOp(this, other, OperationKey.Minus, directEval = { a, b -> a - b })
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [other].
     *
     * @param other The [RemoteFloat] to subtract.
     * @return A new [RemoteFloat] representing `this - other`.
     */
    public operator fun minus(other: RemoteFloat): RemoteFloat {
        other.constantValueOrNull?.let {
            return minus(it)
        }
        constantValueOrNull?.let {
            return (-other).plus(it)
        }
        return binaryOp(this, other, OperationKey.Minus) { a, b -> a - b }
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [other].
     *
     * @param other The [Float] to multiply by.
     * @return A new [RemoteFloat] representing `this * other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun times(other: Float): RemoteFloat {
        if (other == 0f) {
            return RemoteFloat(0f)
        }
        if (other == 1f) {
            return this
        }
        if (constantValueOrNull != null && constantValueOrNull == 1f) {
            return RemoteFloat(other)
        }
        return binaryOp(this, other, OperationKey.Times, directEval = { a, b -> a * b })
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [other].
     *
     * @param other The [RemoteFloat] to multiply by.
     * @return A new [RemoteFloat] representing `this * other`.
     */
    public operator fun times(other: RemoteFloat): RemoteFloat {
        other.constantValueOrNull?.let {
            return times(it)
        }
        constantValueOrNull?.let {
            return other.times(it)
        }
        return binaryOp(this, other, OperationKey.Times) { a, b -> a * b }
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] divided by [other].
     *
     * @param other The divisor [Float].
     * @return A new [RemoteFloat] representing `this / other`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun div(other: Float): RemoteFloat {
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return RemoteFloat(0f)
        }
        if (other == 1f) {
            return this
        }
        return binaryOp(this, other, OperationKey.Div, directEval = { a, b -> a / b })
    }

    /**
     * Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] divided by [other].
     *
     * @param other The divisor [RemoteFloat].
     * @return A new [RemoteFloat] representing `this / other`.
     */
    public operator fun div(other: RemoteFloat): RemoteFloat {
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return RemoteFloat(0f)
        }
        other.constantValueOrNull?.let {
            return div(it)
        }
        return binaryOp(this, other, OperationKey.Div) { aVal: Float, bVal: Float -> aVal / bVal }
    }

    /**
     * Returns a [RemoteFloat] that is a reference of this RemoteFloat.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     *
     * @param forceRemote If true, forces the creation of a remote reference even if the value is
     *   constant.
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createReference(forceRemote: Boolean = false): RemoteFloat {
        return RemoteFloat(
            constantValueOrNull = if (forceRemote) null else constantValueOrNull,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Reference, this, forceRemote),
        ) { creationState ->
            floatArrayOf(asNan(getIdForCreationState(creationState)))
        }
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this == other`.
     */
    public fun isEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareEQ,
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

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is not equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this != other`.
     */
    public fun isNotEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareNE,
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

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is less than the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this < other`.
     */
    public fun isLessThan(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareLT,
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

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is less than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this <= other`.
     */
    public fun isLessThanOrEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareLE,
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

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is greater than the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this > other`.
     */
    public fun isGreaterThan(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareGT,
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

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteFloat] is greater than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteFloat] to compare with.
     * @return A [RemoteBoolean] representing `this >= other`.
     */
    public fun isGreaterThanOrEqualTo(other: RemoteFloat): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareGE,
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
        public fun createForId(id: Float): RemoteFloat = RemoteFloat(id)

        /**
         * Creates a named [RemoteFloat] with an initial value. This allows referring to a float by
         * a symbolic name in the remote document. Named remote floats can be set via
         * AndroidRemoteContext.setNamedFloat.
         *
         * @param name The name of the remote float.
         * @param defaultValue The initial value of the remote float.
         * @param domain The domain of the named float (defaults to [RemoteState.Domain.User]). This
         *   helps avoid name collisions.
         * @return A [RemoteFloat] representing the named float.
         */
        @JvmStatic
        public fun createNamedRemoteFloat(
            name: String,
            defaultValue: Float,
            domain: RemoteState.Domain = RemoteState.Domain.User,
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
         * @param domain The domain of the named float (defaults to [RemoteState.Domain.User]). This
         *   helps avoid name collisions.
         * @return A [RemoteFloat] representing the named float.
         */
        @JvmStatic
        public fun createNamedRemoteFloat(
            name: String,
            defaultValue: RemoteFloat,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteFloat {
            return RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                val initialValueId = defaultValue.getFloatIdForCreationState(creationState)
                if (Utils.isVariable(initialValueId)) {
                    val floatId = creationState.document.floatExpression(initialValueId)
                    creationState.document.setFloatName(
                        Utils.idFromNan(floatId),
                        domain.prefixed(name),
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

internal fun floatToString(v: Float, before: Int, after: Int, flags: Int) =
    StringUtils.floatToString(
        v,
        before,
        after,
        when (flags and 12) {
            PAD_PRE_SPACE -> ' '
            PAD_PRE_NONE -> 0.toChar()
            PAD_PRE_ZERO -> '0'
            else -> ' '
        },
        when (flags and 3) {
            PAD_AFTER_SPACE -> ' '
            PAD_AFTER_NONE -> 0.toChar()
            PAD_AFTER_ZERO -> '0'
            else -> ' '
        },
        when (flags and (3 shl 6)) {
            SEPARATOR_PERIOD_COMMA -> StringUtils.SEPARATOR_PERIOD_COMMA
            SEPARATOR_COMMA_PERIOD -> StringUtils.SEPARATOR_COMMA_PERIOD
            SEPARATOR_SPACE_COMMA -> StringUtils.SEPARATOR_SPACE_COMMA
            SEPARATOR_UNDER_PERIOD -> StringUtils.SEPARATOR_UNDER_PERIOD
            else -> StringUtils.SEPARATOR_PERIOD_COMMA
        },
        when (flags and (3 shl 4)) {
            GROUPING_BY3 -> StringUtils.GROUPING_BY3
            GROUPING_BY4 -> StringUtils.GROUPING_BY4
            GROUPING_BY32 -> StringUtils.GROUPING_BY32
            else -> StringUtils.GROUPING_NONE
        },
        flags shr 8,
    )

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
    op: OperationKey,
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
    op: OperationKey,
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
            return RemoteFloatSelect(
                condition = a.condition,
                ifTrue = RemoteFloat(newTrue),
                ifFalse = RemoteFloat(newFalse),
                cacheKey = RemoteOperationCacheKey.create(op, a, b),
            )
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
    op: OperationKey,
    v: Float,
): FloatArray? {
    val addId = Utils.idFromNan(AnimatedFloatExpression.ADD)
    val subId = Utils.idFromNan(AnimatedFloatExpression.SUB)
    val mulId = Utils.idFromNan(AnimatedFloatExpression.MUL)
    val divId = Utils.idFromNan(AnimatedFloatExpression.DIV)
    return when (op) {
        OperationKey.Plus ->
            when (lastOp) {
                addId -> array.foldTrailingConstant({ it + v }, ::maybeTrimIfZero)
                subId -> array.foldTrailingConstant({ it - v }, ::maybeTrimIfZero)
                else -> null
            }
        OperationKey.Minus ->
            when (lastOp) {
                addId -> array.foldTrailingConstant({ it - v }, ::maybeTrimIfZero)
                subId -> array.foldTrailingConstant({ it + v }, ::maybeTrimIfZero)
                else -> null
            }
        OperationKey.Times ->
            when (lastOp) {
                mulId -> array.foldTrailingConstant({ it * v }, ::maybeTrimIfOne)
                divId -> array.foldTrailingConstant({ it / v }, ::maybeTrimIfOne)
                else -> null
            }
        OperationKey.Div ->
            when (lastOp) {
                mulId -> array.foldTrailingConstant({ it / v }, ::maybeTrimIfOne)
                divId -> array.foldTrailingConstant({ it * v }, ::maybeTrimIfOne)
                else -> null
            }
        OperationKey.UnaryMinus ->
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
    op: OperationKey,
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
    op: OperationKey,
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
                        floatArrayOf(a.getFloatIdForCreationState(creationState)),
                        floatArrayOf(b.getFloatIdForCreationState(creationState)),
                    )
                } else {
                    Pair(aArray, bArray)
                }

            val id =
                creationState.document.floatExpression(
                    *expressionGenerator(finalAArray, finalBArray)
                )
            longArrayOf(0x100000000 + Utils.idFromNan(id).toLong())
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
        return if (aConst < bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    if (ifTrue.cacheKey == ifFalse.cacheKey) {
        return ifTrue
    }

    return RemoteFloatSelect(
        condition = SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.LT),
        ifTrue = ifTrue,
        ifFalse = ifFalse,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfLT, a, b, ifTrue, ifFalse),
    )
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
        return if (aConst <= bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    if (ifTrue.cacheKey == ifFalse.cacheKey) {
        return ifTrue
    }

    return RemoteFloatSelect(
        condition = SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.LE),
        ifTrue = ifTrue,
        ifFalse = ifFalse,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfLE, a, b, ifTrue, ifFalse),
    )
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
        return if (aConst > bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    if (ifTrue.cacheKey == ifFalse.cacheKey) {
        return ifTrue
    }

    return RemoteFloatSelect(
        condition = SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.GT),
        ifTrue = ifTrue,
        ifFalse = ifFalse,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfGT, a, b, ifTrue, ifFalse),
    )
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
        return if (aConst >= bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    if (ifTrue.cacheKey == ifFalse.cacheKey) {
        return ifTrue
    }

    return RemoteFloatSelect(
        condition = SelectFloatCondition.FloatComparison(a, b, FloatComparisonOp.GE),
        ifTrue = ifTrue,
        ifFalse = ifFalse,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfGE, a, b, ifTrue, ifFalse),
    )
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
                OperationKey.DeltaFromReferenceInSeconds,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_FROM_NOW_SEC,
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
                OperationKey.DeltaFromReferenceInMinutes,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_FROM_NOW_MIN,
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
                OperationKey.DeltaFromReferenceInHours,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_FROM_NOW_HR,
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
                OperationKey.TimeOfReferenceInSeconds,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_IN_SEC,
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
                OperationKey.TimeOfReferenceInMinutes,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_IN_MIN,
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
                OperationKey.TimeOfReferenceInHours,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_IN_HR,
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
                OperationKey.DayOfMonthForReference,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_DAY_OF_MONTH,
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
                OperationKey.MonthOfYearForReference,
                referenceEpochMillis,
            ),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_MONTH_VALUE,
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
    val opKey = OperationKey.DayOfWeekForReference
    return RemoteFloat(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(opKey, referenceEpochMillis),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_DAY_OF_WEEK,
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
            RemoteOperationCacheKey.create(OperationKey.YearForReference, referenceEpochMillis),
    ) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_YEAR,
            )
        )
    }
}

/** A mutable implementation of [RemoteFloat]. It also implements [MutableRemoteState<Float>]. */
public class MutableRemoteFloat
@RememberInComposition
internal constructor(
    cacheKey: RemoteStateCacheKey,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Float,
) : RemoteFloat(cacheKey), MutableRemoteState<Float> {

    @RememberInComposition
    internal constructor(
        id: Int
    ) : this(cacheKey = RemoteStateIdKey(id), idProvider = { asNan(id) })

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Float?
        get() = null

    internal override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState ->
            // idProvider returns the allocated ID encoded as a NaN Float. We decode it
            // to a raw Int using idFromNan because getOrPutVariableId tracks integer IDs.
            // Caching this ID ensures that the same ID is shared when the variable is
            // used in expressions (via arrayProvider) and actions (via getIdForCreationState).
            val id =
                creationState.getOrPutVariableId(cacheKey) {
                    Utils.idFromNan(idProvider(creationState))
                }
            floatArrayOf(asNan(id))
        }

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromNan(idProvider(creationState))

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteFloat] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Float): MutableRemoteFloat =
            MutableRemoteFloat(Utils.idFromNan(id))

        /**
         * Creates a [MutableRemoteFloat] backed by a reserved float variable ID with no initial
         * value.
         *
         * @return A [MutableRemoteFloat] whose ID is allocated via `reserveFloatVariable()`.
         */
        @RememberInComposition
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createReserved(): MutableRemoteFloat =
            MutableRemoteFloat(
                cacheKey = RemoteStateInstanceKey(),
                idProvider = { creationState -> creationState.document.reserveFloatVariable() },
            )
    }
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
    MutableRemoteFloat(
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState -> creationState.document.floatExpression(initialValue) },
    )

/**
 * Creates a [MutableRemoteFloat] initialized with [initialValue].
 *
 * @param initialValue The initial [RemoteFloat] value or expression.
 * @return A [MutableRemoteFloat] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteFloat(initialValue: RemoteFloat): MutableRemoteFloat =
    MutableRemoteFloat(
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState ->
            creationState.document.floatExpression(
                *initialValue.arrayForCreationState(creationState)
            )
        },
    )

/** Comparison operators for evaluating [RemoteFloat] select conditions. */
internal enum class FloatComparisonOp {
    LT,
    LE,
    GT,
    GE,
    EQ,
    NE,
}

/** Condition determining branch selection in a [RemoteFloatSelect]. */
internal sealed class SelectFloatCondition {
    /**
     * Builds the [FloatArray] bytecode for selecting between [ifFalse] and [ifTrue].
     *
     * @param creationState creation state used to resolve variable IDs
     * @param ifFalse value selected when the condition evaluates to false
     * @param ifTrue value selected when the condition evaluates to true
     * @return float array encoding the selection expression in RPN
     */
    abstract fun buildFloatArray(
        creationState: RemoteComposeCreationState,
        ifFalse: RemoteFloat,
        ifTrue: RemoteFloat,
    ): FloatArray

    /**
     * Direct comparison between two [RemoteFloat] instances.
     *
     * @property a left-hand operand
     * @property b right-hand operand
     * @property op comparison operator
     */
    data class FloatComparison(val a: RemoteFloat, val b: RemoteFloat, val op: FloatComparisonOp) :
        SelectFloatCondition() {
        override fun buildFloatArray(
            creationState: RemoteComposeCreationState,
            ifFalse: RemoteFloat,
            ifTrue: RemoteFloat,
        ): FloatArray =
            when (op) {
                FloatComparisonOp.LT ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.GT ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, a, b),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.LE ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, a, b),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.GE ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.EQ ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.ABS,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.NE ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.ABS,
                        AnimatedFloatExpression.IFELSE,
                    )
            }
    }

    /**
     * Boolean condition wrapping a [RemoteBoolean].
     *
     * @property bool boolean expression driving the branch selection
     */
    data class BooleanCondition(val bool: RemoteBoolean) : SelectFloatCondition() {
        override fun buildFloatArray(
            creationState: RemoteComposeCreationState,
            ifFalse: RemoteFloat,
            ifTrue: RemoteFloat,
        ): FloatArray =
            combineToFloatArray(
                creationState,
                arrayOf(ifFalse, ifTrue, bool.intValue.toRemoteFloat()),
                AnimatedFloatExpression.IFELSE,
            )
    }
}

internal typealias SealedFloatCondition = SelectFloatCondition

/**
 * Select expression choosing between [ifTrue] and [ifFalse] based on [condition].
 *
 * @property condition selection condition, either direct float comparison or a boolean
 * @property ifTrue value returned when [condition] evaluates to true
 * @property ifFalse value returned when [condition] evaluates to false
 */
internal class RemoteFloatSelect(
    val condition: SelectFloatCondition,
    val ifTrue: RemoteFloat,
    val ifFalse: RemoteFloat,
    cacheKey: RemoteStateCacheKey,
) :
    RemoteFloat(
        constantValueOrNull = null,
        cacheKey = cacheKey,
        arrayProvider = { creationState ->
            condition.buildFloatArray(creationState, ifFalse, ifTrue)
        },
    ) {
    val hasConstantBranches: Boolean
        get() = ifTrue.constantValueOrNull != null && ifFalse.constantValueOrNull != null
}

/**
 * Creates an animated [RemoteFloat] combining an [input] [RemoteFloat] with an animation definition
 * ([FloatArray]).
 */
internal fun animatedRemoteFloat(input: RemoteFloat, anim: FloatArray): RemoteFloat =
    RemoteFloat(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(OperationKey.Anim, input, FloatArrayCacheKey(anim)),
    ) { creationState ->
        val array = input.arrayForCreationState(creationState)
        val id =
            creationState.getOrPutFloatExpressionId(array, anim) {
                Utils.idFromNan(creationState.document.floatExpression(array, anim))
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

/**
 * Converts a [Number] to a [FloatArray]. If the number is a [RemoteFloat], its
 * `arrayForCreationState` is used. Otherwise, it\'s converted to a single-element [FloatArray].
 *
 * @param a The number to convert.
 * @return A [FloatArray] representation of the number.
 */
internal fun toArray(a: Number): FloatArray =
    when (a) {
        is Float -> floatArrayOf(a)
        else -> floatArrayOf(a.toFloat())
    }

/**
 * Converts a [RemoteFloat] to a [FloatArray] using a specific [RemoteComposeCreationState].
 *
 * @param a The [RemoteFloat] to convert.
 * @param creationState The [RemoteComposeCreationState] to use for conversion.
 * @return A [FloatArray] representation of [a].
 */
internal fun toArray(a: RemoteFloat, creationState: RemoteComposeCreationState): FloatArray =
    a.arrayForCreationState(creationState)

/**
 * Checks if a given float represents a time variable.
 *
 * @param fl The float to check.
 * @return `true` if the float is a time variable, `false` otherwise.
 */
internal fun isTimeVar(fl: Float): Boolean {
    return RemoteContext.isTime(fl)
}

/**
 * Converts a [FloatArray] expression to a human-readable string representation. It includes labels
 * for NaN values, which typically represent IDs.
 *
 * @param array The [FloatArray] representing the expression.
 * @return A string representation of the expression.
 */
internal fun toString(array: FloatArray): String {
    val labels = arrayOfNulls<String>(array.size)
    for (i in array.indices) {
        if (array[i].isNaN()) {
            labels[i] = "[" + Utils.idStringFromNan(array[i]) + "]"
        }
    }
    return AnimatedFloatExpression.toString(array, labels)
}

/**
 * Constructs a floatArray that either inlines or references the contents of [remoteFloats] followed
 * by [extras]. Inlining is preferred as long as the resulting array length is less than
 * [MAX_SAFE_FLOAT_ARRAY].
 */
internal fun combineToFloatArray(
    creationState: RemoteComposeCreationState,
    remoteFloats: Array<RemoteFloat>,
    vararg extras: Float,
): FloatArray {
    var totalSizeInline = extras.size
    val totalSizeReference = extras.size + remoteFloats.size
    val arrays =
        Array<FloatArray>(remoteFloats.size) { i ->
            val remoteFloat = remoteFloats[i]
            // If remoteFloat has already been written to the document then use a reference
            // rather than inlining the expression. This results in smaller documents.
            if (remoteFloat.hasBeenWrittenToDoc(creationState)) {
                totalSizeInline += 1
                floatArrayOf(remoteFloat.getFloatIdForCreationState(creationState))
            } else {
                val array = remoteFloat.arrayForCreationState(creationState)
                totalSizeInline += array.size
                array
            }
        }

    val combinedArray: FloatArray
    var idx = 0

    if (totalSizeInline > MAX_SAFE_FLOAT_ARRAY) {
        // Add references for the RemoteFloat values.
        combinedArray = FloatArray(totalSizeReference)
        for (i in remoteFloats.indices) {
            combinedArray[i] = remoteFloats[i].getFloatIdForCreationState(creationState)
        }
        idx = remoteFloats.size
    } else {
        // Inline the RemoteFloat arrays.
        combinedArray = FloatArray(totalSizeInline)
        for (array in arrays) {
            array.copyInto(
                combinedArray,
                destinationOffset = idx,
                startIndex = 0,
                endIndex = array.size,
            )
            idx += array.size
        }
    }

    for (extra in extras) {
        combinedArray[idx++] = extra
    }

    return combinedArray
}

private const val MAX_SAFE_FLOAT_ARRAY = 30

/** An inline value class representing a reference to a remote float. */
@JvmInline internal value class RemoteFloatReference(private val v: Float)

/** Extension property to convert an [Int] to a [RemoteFloat]. */
public val Int.rf: RemoteFloat
    get() = RemoteFloat(this.toFloat())

/** Extension property to convert a [Float] to a [RemoteFloat]. */
public val Float.rf: RemoteFloat
    get() = RemoteFloat(this)

/** Extension function to get either a Float ID or a Float literal from a [Number]. */
internal fun Number.getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float =
    when (this) {
        is Float -> this
        else -> toFloat()
    }
