/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:JvmName("RemoteFloatOperations")

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.easing.CubicEasing
import androidx.compose.remote.core.operations.utilities.easing.MonotonicSpline
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMap
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private const val FP_TO_RAD = 57.29578f // 180/PI
private const val FP_TO_DEG = 0.017453292f // 180/PI
private val PI_RF = Math.PI.toFloat().rf

private fun easeOutBounce(x: RemoteFloat): RemoteFloat {
    val n1 = 7.5625f.rf
    val d1 = 2.75f.rf

    return selectIfLt(
        x,
        0f.rf,
        0f.rf,
        selectIfLt(
            x,
            1f.rf / d1,
            1f.rf / (1f.rf + 1f.rf / d1) * (n1 * x * x + x),
            selectIfLt(
                x,
                2f.rf / d1,
                n1 * (x - 1.5f.rf / d1) * (x - 1.5f.rf / d1) + 0.75f.rf,
                selectIfLt(
                    x,
                    2.5f.rf / d1,
                    n1 * (x - 2.25f.rf / d1) * (x - 2.25f.rf / d1) + 0.9375f.rf,
                    selectIfLt(
                        x,
                        1f.rf,
                        n1 * (x - 2.625f.rf / d1) * (x - 2.625f.rf / d1) + 0.984375f.rf,
                        1f.rf,
                    ),
                ),
            ),
        ),
    )
}

private fun easeOutElastic(x: RemoteFloat): RemoteFloat {
    val c4 = (2f.rf * PI_RF) / 3f.rf

    return selectIfLt(
        x,
        0f.rf,
        0f.rf,
        selectIfLt(
            x,
            1f.rf,
            pow(2f.rf, -10f.rf * x) * sin((x * 10f.rf - 0.75f.rf) * c4) + 1f.rf,
            1f.rf,
        ),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Max) { a, b -> max(a, b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Max) { a, b -> max(a, b) }

/**
 * Returns the greater of two [RemoteFloat] values.
 *
 * @param a The first [RemoteFloat] value.
 * @param b The second [RemoteFloat] value.
 * @return The larger of [a] and [b].
 */
public fun max(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Max) { a, b -> max(a, b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Min) { a, b -> min(a, b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Min) { a, b -> min(a, b) }

/**
 * Returns the smaller of two [RemoteFloat] values.
 *
 * @param a The first [RemoteFloat] value
 * @param b The second [RemoteFloat] value
 * @return A [RemoteFloat] representing the minimum of [a] and [b]
 */
public fun min(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Min) { a, b -> min(a, b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Pow) { a, b -> a.pow(b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Pow) { a, b -> a.pow(b) }

/**
 * Raises [a] to the power of [b].
 *
 * @param a The base value.
 * @param b The exponent value.
 * @return A [RemoteFloat] representing [a] raised to the power of [b].
 */
public fun pow(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Pow) { a, b -> a.pow(b) }

/** Returns the positive square root of the given [RemoteFloat]. */
public fun sqrt(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Sqrt) { a -> sqrt(a) }

/**
 * Returns the absolute value of the given [RemoteFloat].
 *
 * @param a The [RemoteFloat] whose absolute value is to be determined.
 * @return A [RemoteFloat] representing the absolute value of [a].
 */
public fun abs(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Abs) { a -> abs(a) }

/**
 * Returns the sign of the given [RemoteFloat] [a].
 *
 * @param a The value whose sign is to be returned.
 * @return 1.0 if the value is positive, -1.0 if it is negative, and 0.0 if it is zero.
 */
public fun sign(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Sign) { a -> sign(a) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.CopySign) { a, b -> Math.copySign(a, b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.CopySign) { a, b -> Math.copySign(a, b) }

/**
 * Returns the first floating-point argument with the sign of the second floating-point argument.
 *
 * @param a The value whose magnitude is used.
 * @param b The value whose sign is used.
 */
public fun copySign(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.CopySign) { a, b -> Math.copySign(a, b) }

/** Returns Euler's number `e` raised to the power of the given [RemoteFloat] value [a]. */
public fun exp(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Exp) { a -> exp(a) }

/**
 * Returns the smallest [RemoteFloat] that is greater than or equal to [a] and is a mathematical
 * integer.
 */
public fun ceil(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Ceil) { a -> ceil(a) }

/**
 * Returns the largest (closest to positive infinity) [RemoteFloat] value that is less than or equal
 * to [a] and is equal to a mathematical integer.
 */
public fun floor(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Floor) { a -> floor(a) }

/**
 * Computes the base-10 logarithm of the [RemoteFloat] [a].
 *
 * @param a The value whose logarithm is to be computed.
 */
public fun log(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Log) { a -> log10(a) }

public fun ln(a: RemoteFloat): RemoteFloat = a.unaryOp(RemoteFloat.OperationKey.Ln) { a -> ln(a) }

/**
 * Returns the value of the given [RemoteFloat] rounded to the nearest integer.
 *
 * @param a The [RemoteFloat] to be rounded.
 */
public fun round(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Round) { a -> Math.round(a).toFloat() }

/**
 * Computes the sine of the given [RemoteFloat].
 *
 * @param a The angle in radians.
 * @return The sine of [a].
 */
public fun sin(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Sin) { a -> sin(a) }

/**
 * Computes the cosine of the given [RemoteFloat] [a].
 *
 * @param a The value in radians whose cosine is to be computed.
 * @return The cosine of [a].
 */
public fun cos(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Cos) { a -> cos(a) }

/**
 * Computes the trigonometric tangent of an angle in radians.
 *
 * @param a The angle in radians.
 */
public fun tan(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Tan) { a -> tan(a) }

/**
 * Computes the arc sine of the given [RemoteFloat].
 *
 * @param a The value whose arc sine is to be computed.
 * @return The arc sine of [a] in radians.
 */
public fun asin(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Asin) { a -> asin(a) }

/**
 * Computes the arc cosine of the given [RemoteFloat].
 *
 * @param a The value whose arc cosine is to be computed.
 * @return The arc cosine of [a] in radians.
 */
public fun acos(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Acos) { a -> acos(a) }

/**
 * Computes the arc tangent of the given [RemoteFloat] value.
 *
 * @param a The value whose arc tangent is to be computed.
 * @return The arc tangent of [a], in radians, in the range of -pi/2 to pi/2.
 */
public fun atan(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Atan) { a -> atan(a) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Atan2) { a, b -> atan2(a, b) }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Atan2) { a, b -> atan2(a, b) }

/**
 * Returns the angle theta from the conversion of rectangular coordinates (b, a) to polar
 * coordinates (r, theta).
 *
 * @param a The y-coordinate
 * @param b The x-coordinate
 */
public fun atan2(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Atan2) { a, b -> atan2(a, b) }

/** Returns the cube root of a [RemoteFloat]. */
public fun cbrt(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Cbrt) { a -> cbrt(a) }

/**
 * Converts an angle measured in radians to an approximately equivalent angle measured in degrees.
 *
 * @param a An angle, in radians
 * @return The measurement of the angle [a] in degrees
 */
public fun toDeg(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.ToDeg) { a -> a * FP_TO_RAD }

/** Converts the given [RemoteFloat] value in degrees to radians. */
public fun toRad(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.ToRad) { a -> a * FP_TO_DEG }

/**
 * Computes [from] + ([to] - [from]) * [tween].
 *
 * @param from The [RemoteFloat] we're interpolating from, i.e. when [tween] is 0, lerp evaluates to
 *   [from]
 * @param to The [RemoteFloat] we're interpolating towards, i.e. when [tween] is 1, lerp evaluates
 *   to [to]
 * @param tween The ratio between [from] and [to] that controls the result.
 */
public fun lerp(from: RemoteFloat, to: RemoteFloat, tween: RemoteFloat): RemoteFloat {
    val constFrom = from.constantValueOrNull
    val constTo = to.constantValueOrNull
    val constTween = tween.constantValueOrNull
    if (constFrom != null && constTo != null && constTween != null) {
        return RemoteFloat(constFrom + (constTo - constFrom) * constTween)
    }

    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Lerp, from, to, tween),
    ) { creationState ->
        combineToFloatArray(creationState, arrayOf(from, to, tween), AnimatedFloatExpression.LERP)
    }
}

/**
 * Computes [from] + ([to] - [from]) * [tween].
 *
 * @param from The [RemoteFloat] we're interpolating from, i.e. when [tween] is 0, lerp evaluates to
 *   [from]
 * @param to The [RemoteFloat] we're interpolating towards, i.e. when [tween] is 1, lerp evaluates
 *   to [to]
 * @param tween The ratio between [from] and [to] that controls the result.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun lerp(from: Float, to: Float, tween: RemoteFloat): RemoteFloat {
    tween.constantValueOrNull?.let {
        return RemoteFloat(from + (to - from) * it)
    }

    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Lerp, from, to, tween),
        arrayProvider = { creationState ->
            floatArrayOf(
                from,
                to,
                *tween.arrayProvider(creationState),
                AnimatedFloatExpression.LERP,
            )
        },
    )
}

/**
 * Computes a multiply-add operation: [a] * [b] + [c].
 *
 * @param a The first multiplier
 * @param b The second multiplier
 * @param c The value to be added to the product of [a] and [b]
 */
public fun mad(a: RemoteFloat, b: RemoteFloat, c: RemoteFloat): RemoteFloat {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    val constC = c.constantValueOrNull
    if (constA != null && constB != null && constC != null) {
        return RemoteFloat(constA * constB + constC)
    }

    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Mad, a, b, c),
        arrayProvider = { creationState ->
            floatArrayOf(
                *(toArray(a, creationState)),
                *(toArray(b, creationState)),
                *(toArray(c, creationState)),
                AnimatedFloatExpression.MAD,
            )
        },
    )
}

/**
 * Restricts the given [value] to the range defined by [min] and [max].
 *
 * @param min The lower bound of the range
 * @param max The upper bound of the range
 * @param value The [RemoteFloat] value to be clamped
 */
public fun clamp(value: RemoteFloat, min: RemoteFloat, max: RemoteFloat): RemoteFloat {
    val constMin = min.constantValueOrNull
    val constMax = max.constantValueOrNull
    val constValue = value.constantValueOrNull
    if (constMin != null && constMax != null && constValue != null) {
        return if (constValue < constMin) {
            min
        } else if (constValue > constMax) {
            max
        } else {
            value
        }
    }
    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Clamp, min, max, value),
    ) { creationState ->
        combineToFloatArray(creationState, arrayOf(min, max, value), AnimatedFloatExpression.CLAMP)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun clamp(value: RemoteFloat, min: Float, max: Float): RemoteFloat {
    value.constantValueOrNull?.let {
        return if (it < min) {
            RemoteFloat(min)
        } else if (it > max) {
            RemoteFloat(max)
        } else {
            value
        }
    }

    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Clamp, min, max, value),
        arrayProvider = { creationState ->
            floatArrayOf(
                min,
                max,
                *value.arrayProvider(creationState),
                AnimatedFloatExpression.CLAMP,
            )
        },
    )
}

/**
 * Cubic easing function similar to CSS cubic-bezier, given two control point x1,y1 and x2,y2 the
 * function is defined as: f(x) = (1-x)^3 * x1 + 3 * (1-x)^2 * x * y1 + 3 * (1-x) * x^2 * y2 + x^3 *
 * y2
 *
 * @param x1 the x-coordinate of the first control point
 * @param y1 the y-coordinate of the first control point
 * @param x2 the x-coordinate of the second control point
 * @param y2 the y-coordinate of the second control point
 * @param progress A value between 0 and 1
 * @return The computed cubic easing for the given [progress]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cubicEasing(
    x1: RemoteFloat,
    y1: RemoteFloat,
    x2: RemoteFloat,
    y2: RemoteFloat,
    progress: RemoteFloat,
): RemoteFloat {
    if (
        x1.hasConstantValue &&
            y1.hasConstantValue &&
            x2.hasConstantValue &&
            y2.hasConstantValue &&
            progress.hasConstantValue
    ) {
        val easing = CubicEasing()
        easing.setup(x1.constantValue, y1.constantValue, x2.constantValue, y2.constantValue)
        return RemoteFloat(easing.get(progress.constantValue))
    }

    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Cubic, x1, y1, x2, y2, progress),
    ) { creationState ->
        combineToFloatArray(
            creationState,
            arrayOf(x1, y1, x2, y2, progress),
            AnimatedFloatExpression.CUBIC,
        )
    }
}

/**
 * @param controlPoints The [RemoteFloatArray] that defines the spline control points
 * @param loop Whether or not the last control point should be treated as looping round to the first
 *   one
 * @param progress A value between 0 and 1
 * @return The computed spline for the given [progress]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun evalSpline(
    controlPoints: RemoteFloatArray,
    loop: Boolean,
    progress: RemoteFloat,
): RemoteFloat {
    if (controlPoints.hasConstantValue && progress.hasConstantValue) {
        val constants = controlPoints.constantValue
        if (constants.fastAll { it.hasConstantValue }) {
            val spline =
                MonotonicSpline(null, constants.fastMap { it.constantValue }.toFloatArray())
            var p = progress.constantValue
            if (loop) {
                p -= p.toInt().toFloat()
                if (p < 0) p += 1f
            }
            return RemoteFloat(spline.getPos(p))
        }
    }

    return RemoteFloatExpression(
        constantValueOrNull = null,
        cacheKey =
            RemoteOperationCacheKey.create(
                if (loop) RemoteFloat.OperationKey.SplineLoop else RemoteFloat.OperationKey.Spline,
                controlPoints,
                progress,
            ),
    ) { creationState ->
        floatArrayOf(
            controlPoints.getFloatIdForCreationState(creationState),
            progress.getFloatIdForCreationState(creationState),
            if (loop) AnimatedFloatExpression.A_SPLINE_LOOP else AnimatedFloatExpression.A_SPLINE,
        )
    }
}

/**
 * Calculates the interpolated RemoteFloat given a progress value from 0.0 to 1.0.
 *
 * @param progress A RemoteFloat progressing from 0.0 to 1.0.
 * @param initialValue The starting value of the animation.
 * @param targetValue The target value to animate towards.
 * @param type The type of animation curve to apply.
 * @param spec Parameters for custom curves (e.g., [x1, y1, x2, y2] for CUBIC_CUSTOM).
 * @param wrap Optional wrapping bound (e.g., 360 for angles) to ensure shortest-path interpolation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interpolateRemoteFloat(
    progress: RemoteFloat,
    initialValue: RemoteFloat,
    targetValue: RemoteFloat,
    @AnimationType type: Int = CUBIC_STANDARD,
    spec: RemoteFloatArray? = null,
    wrap: RemoteFloat? = null,
): RemoteFloat {
    val easedProgress =
        when (type) {
            CUBIC_STANDARD -> cubicEasing(0.4f.rf, 0.0f.rf, 0.2f.rf, 1f.rf, progress)
            CUBIC_ACCELERATE -> cubicEasing(0.4f.rf, 0.05f.rf, 0.8f.rf, 0.7f.rf, progress)
            CUBIC_DECELERATE -> cubicEasing(0.0f.rf, 0.0f.rf, 0.2f.rf, 0.95f.rf, progress)
            CUBIC_LINEAR -> progress
            CUBIC_ANTICIPATE -> cubicEasing(0.36f.rf, 0f.rf, 0.66f.rf, -0.56f.rf, progress)
            CUBIC_OVERSHOOT -> cubicEasing(0.34f.rf, 1.56f.rf, 0.64f.rf, 1f.rf, progress)
            CUBIC_CUSTOM -> {
                if (spec != null && (spec.constantValueOrNull?.size ?: 0) >= 4) {
                    cubicEasing(spec[0], spec[1], spec[2], spec[3], progress)
                } else {
                    throw IllegalArgumentException("spec is either null or its size isn't 4")
                }
            }
            SPLINE_CUSTOM -> {
                if (spec != null) {
                    evalSpline(spec, loop = false, progress)
                } else {
                    progress
                }
            }
            EASE_OUT_BOUNCE -> easeOutBounce(progress)
            EASE_OUT_ELASTIC -> easeOutElastic(progress)
            else -> progress
        }

    var diff = targetValue - initialValue
    if (wrap != null) {
        // shortest path interpolation: diff = (((diff + wrap/2) % wrap + wrap) % wrap) - wrap/2
        val halfWrap = wrap / 2f.rf
        diff = ((diff + halfWrap) % wrap + wrap) % wrap - halfWrap
    }

    return initialValue + diff * easedProgress
}

/**
 * Returns a [RemoteFloat] which applies an animation based on the value of [rf].
 *
 * @param rf The [RemoteFloat] which the animation is keyed from
 * @param duration The duration of the animation in seconds
 * @param type The type of animation
 * @param spec The parameters of the animation if any
 * @param initialValue The initial value if it animates to a start
 * @param wrap If not [Float.NaN], then all animations will be computed modulo this value. For
 *   example, if the animation is for an angle, wrap=360 means that an angle of 355 would animate
 *   to 5.
 * @return A [RemoteFloat] based on [rf] but with an animation applied to it
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun animateRemoteFloat(
    rf: RemoteFloat,
    duration: Float = 1f,
    @AnimationType type: Int = CUBIC_STANDARD,
    spec: FloatArray? = null,
    initialValue: Float = Float.NaN,
    wrap: Float = Float.NaN,
): RemoteFloat {
    val anim = RemoteComposeBuffer.packAnimation(duration, type, spec, initialValue, wrap)
    return AnimatedRemoteFloat(rf, anim)
}

/**
 * Returns a [RemoteFloat] which applies an animation based on the result of [content].
 *
 * @param duration The duration of the animation in seconds
 * @param type The type of animation
 * @param spec The parameters of the animation if any
 * @param initialValue The initial value if it animates to a start
 * @param wrap If not [Float.NaN], then all animations will be computed modulo this value. For
 *   example, if the animation is for an angle, wrap=360 means that an angle of 355 would animate
 *   to 5.
 * @param content Callback that provides a [RemoteFloat] upon which the animation is based
 * @return A [RemoteFloat] based on the result of [content] but with an animation applied to it
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun animateRemoteFloat(
    duration: Float = 1f,
    @AnimationType type: Int = CUBIC_STANDARD,
    spec: FloatArray? = null,
    initialValue: Float = Float.NaN,
    wrap: Float = Float.NaN,
    content: () -> RemoteFloat,
): RemoteFloat {
    return animateRemoteFloat(content(), duration, type, spec, initialValue, wrap)
}
