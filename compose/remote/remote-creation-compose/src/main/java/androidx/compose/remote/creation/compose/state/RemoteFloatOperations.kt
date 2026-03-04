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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
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

public fun max(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Max) { a, b -> max(a, b) }

public fun max(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Max) { a, b -> max(a, b) }

public fun max(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Max) { a, b -> max(a, b) }

public fun min(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Min) { a, b -> min(a, b) }

public fun min(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Min) { a, b -> min(a, b) }

public fun min(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Min) { a, b -> min(a, b) }

public fun pow(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Pow) { a, b -> a.pow(b) }

public fun pow(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Pow) { a, b -> a.pow(b) }

public fun pow(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Pow) { a, b -> a.pow(b) }

public fun sqrt(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Sqrt) { a -> sqrt(a) }

public fun abs(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Abs) { a -> abs(a) }

public fun sign(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Sign) { a -> sign(a) }

public fun copySign(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.CopySign) { a, b -> Math.copySign(a, b) }

public fun copySign(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.CopySign) { a, b -> Math.copySign(a, b) }

public fun copySign(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.CopySign) { a, b -> Math.copySign(a, b) }

public fun exp(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Exp) { a -> exp(a) }

public fun ceil(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Ceil) { a -> ceil(a) }

public fun floor(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Floor) { a -> floor(a) }

public fun log(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Log) { a -> log10(a) }

public fun ln(a: RemoteFloat): RemoteFloat = a.unaryOp(RemoteFloat.OperationKey.Ln) { a -> ln(a) }

public fun round(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Round) { a -> Math.round(a).toFloat() }

public fun sin(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Sin) { a -> sin(a) }

public fun cos(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Cos) { a -> cos(a) }

public fun tan(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Tan) { a -> tan(a) }

public fun asin(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Asin) { a -> asin(a) }

public fun acos(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Acos) { a -> acos(a) }

public fun atan(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Atan) { a -> atan(a) }

public fun atan2(a: RemoteFloat, b: Float): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Atan2) { a, b -> atan2(a, b) }

public fun atan2(a: Float, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Atan2) { a, b -> atan2(a, b) }

public fun atan2(a: RemoteFloat, b: RemoteFloat): RemoteFloat =
    binaryOp(a, b, RemoteFloat.OperationKey.Atan2) { a, b -> atan2(a, b) }

public fun cbrt(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.Cbrt) { a -> cbrt(a) }

public fun toDeg(a: RemoteFloat): RemoteFloat =
    a.unaryOp(RemoteFloat.OperationKey.ToDeg) { a -> a * FP_TO_RAD }

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
 * parameters can be float or RemoteFloat. Coded this way to not require 8 versions returns a*b+c
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

public fun clamp(min: RemoteFloat, max: RemoteFloat, value: RemoteFloat): RemoteFloat {
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

public fun clamp(min: Float, max: Float, value: RemoteFloat): RemoteFloat {
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
