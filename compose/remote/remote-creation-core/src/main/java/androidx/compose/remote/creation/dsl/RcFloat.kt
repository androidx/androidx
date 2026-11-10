/*
 * Copyright 2026 The Android Open Source Project
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
@file:JvmName("RcFloatKt")

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext.FLOAT_ANIMATION_DELTA_TIME
import androidx.compose.remote.core.RemoteContext.FLOAT_ANIMATION_TIME
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.remoteContentEquals
import androidx.compose.remote.creation.remoteContentHashCode

/**
 * Type-safe reference for remote float variables or expressions. This often encapsulates a
 * NaN-encoded ID for player-side evaluation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open public class RcFloat {
    internal var array: FloatArray = floatArrayOf()
    internal var id: Float = 0f // if 0 it has not been sent
    internal var animation: FloatArray? = null
    internal var isEvaluated: Boolean = false
    internal var writer: RemoteComposeWriter? = null

    internal constructor(writer: RemoteComposeWriter?, array: FloatArray) {
        this.array = array
        this.writer = writer
    }

    internal constructor(writer: RemoteComposeWriter?, a: Float) {
        this.id = a
        this.isEvaluated = true
        this.array = floatArrayOf(a)
        this.writer = writer
    }

    internal constructor(
        writer: RemoteComposeWriter?,
        array: FloatArray,
        id: Float,
        animation: FloatArray?,
        isEvaluated: Boolean,
    ) {
        this.array = array
        this.writer = writer
        this.id = id
        this.animation = animation
        this.isEvaluated = isEvaluated
    }

    public constructor(a: Float) : this(null, a)

    public fun toArray(): FloatArray {
        return if (isEvaluated) floatArrayOf(id) else array
    }

    /** Commits this expression to the document and returns this instance bound to its ID. */
    public fun flush(): RcFloat {
        if (isEvaluated && id.isNaN()) return this
        val w = writer ?: return this
        id = w.floatExpression(array, animation)
        isEvaluated = true
        return this
    }

    public fun withWriter(writer: RemoteComposeWriter): RcFloat {
        if (this.writer == null) {
            this.writer = writer
        }
        return this
    }

    public fun toFloat(): Float {
        if (!isEvaluated) {
            if (array.size == 1 && animation == null) {
                id = array[0]
                isEvaluated = true
            } else {
                flush()
            }
        }
        return id
    }

    public operator fun unaryMinus(): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), -1f, Rc.FloatExpression.MUL))
    }

    public operator fun plus(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.ADD))
    }

    public operator fun plus(v: RcFloat): RcFloat {
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.ADD),
        )
    }

    public operator fun minus(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.SUB))
    }

    public operator fun minus(v: RcFloat): RcFloat {
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.SUB),
        )
    }

    public operator fun times(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.MUL))
    }

    public operator fun times(v: RcFloat): RcFloat {
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.MUL),
        )
    }

    public operator fun div(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.DIV))
    }

    public operator fun div(v: RcFloat): RcFloat {
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.DIV),
        )
    }

    public operator fun rem(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.MOD))
    }

    public operator fun rem(v: RcFloat): RcFloat {
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.MOD),
        )
    }

    public operator fun unaryPlus(): RcFloat {
        return this
    }

    public fun min(v: RcFloat): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.MIN))
    }

    public fun min(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.MIN))
    }

    public operator fun get(index: RcFloat): RcFloat {
        return RcFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(index), Rc.FloatExpression.A_DEREF),
        )
    }

    public operator fun set(index: RcFloat, value: RcFloat) {
        writer?.setArrayValue(Utils.idFromNan(this.id), index.toFloat(), value.toFloat())
    }

    public operator fun set(index: Int, value: RcFloat) {
        writer?.setArrayValue(Utils.idFromNan(this.id), index.toFloat(), value.toFloat())
    }

    public operator fun get(index: Int): RcFloat {
        return RcFloat(
            writer,
            floatArrayOf(*toArray(this), index.toFloat(), Rc.FloatExpression.A_DEREF),
        )
    }

    public companion object {
        public operator fun invoke(float: Float, writer: RemoteComposeWriter? = null): RcFloat {
            return RcFloat(writer, floatArrayOf(float))
        }
    }

    public fun anim(
        duration: Float,
        type: Int = Rc.Animate.CUBIC_STANDARD,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RcFloat {
        animation = writer?.anim(duration, type, spec, initialValue, wrap)
        this.flush()
        return this
    }

    /** support springs for animation */
    public fun spring(
        stiffness: Float,
        damping: Float,
        stopThreshold: Float,
        boundaryMode: Int,
    ): RcFloat {
        animation = writer?.spring(stiffness, damping, stopThreshold, boundaryMode)
        this.flush()
        return this
    }

    /**
     * Type-safe overload of [anim] that accepts a [RcAnimationCurve] instead of a raw
     * `Rc.Animate.*` opcode. `CubicCustom` and `SplineCustom` require a non-null [spec] containing
     * the control points; the others ignore [spec].
     */
    public fun anim(
        duration: Float,
        curve: RcAnimationCurve,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RcFloat = anim(duration, curve.value, spec, initialValue, wrap)

    public fun anim(
        duration: RcFloat,
        curve: RcAnimationCurve,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RcFloat = anim(duration.id, curve.value, spec, initialValue, wrap)

    /** Returns the absolute value of this expression. */
    public fun abs(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.ABS))

    /** Returns the square root of this expression. */
    public fun sqrt(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.SQRT))

    /** Returns the cosine of this expression. */
    public fun cos(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.COS))

    /** Returns the sine of this expression. */
    public fun sin(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.SIN))

    /** Returns this expression raised to the power of [v]. */
    public fun pow(v: Float): RcFloat =
        RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.POW))

    /** Returns this expression raised to the power of [v]. */
    public fun pow(v: RcFloat): RcFloat =
        RcFloat(writer ?: v.writer, floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.POW))

    /**
     * Converts this float expression to an [RcText] using the specified formatting.
     *
     * @param whole the number of digits before the decimal point
     * @param decimal the number of digits after the decimal point
     * @param flags formatting flags (see [Rc.TextFromFloat])
     */
    public fun format(whole: Int, decimal: Int = 0, flags: Int = 0): RcText {
        val w = writer ?: throw IllegalStateException("RcFloat must be associated with a writer")
        return RcText(w.createTextFromFloat(toFloat(), whole, decimal, flags))
    }

    /**
     * Converts this float expression to an [RcText] using the specified [RcTextFromFloatSpec]
     * formatting.
     *
     * @param whole the number of digits before the decimal point
     * @param decimal the number of digits after the decimal point
     * @param spec typed formatting specification
     */
    public fun format(whole: Int, decimal: Int = 0, spec: RcTextFromFloatSpec): RcText =
        format(whole, decimal, spec.bits)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcFloat) return false
        // array and animation hold NaN-encoded variable ids and operators, which contentEquals and
        // Float equality don't distinguish. `id` is a lazily populated cache of `array` +
        // `animation`, so excluding `id` keeps equality and hashCode invariant across flush().
        if (!array.remoteContentEquals(other.array)) return false
        if (!animation.remoteContentEquals(other.animation)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = array.remoteContentHashCode()
        result = 31 * result + animation.remoteContentHashCode()
        return result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayValue(array: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(array, *b.array, Rc.FloatExpression.A_DEREF))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.MAX))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.MAX))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.MAX))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.MIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.MIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.MIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.POW))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.POW))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.POW))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sqrt(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SQRT))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun abs(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ABS))
}

/**
 * Returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument
 * is greater than zero, -1.0f if the argument is less than zero.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sign(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.COPY_SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.COPY_SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.COPY_SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun exp(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.EXP))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ceil(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.CEIL))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun floor(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.FLOOR))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun log(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.LOG))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun log2(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.LOG2))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ln(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.LN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun round(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ROUND))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun inverse(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.INV))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun fraction(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.FRACT))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun square(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SQUARE))
}

/** Math.sin(a) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sin(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cos(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.COS))
}

/** Math.tan(a) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun tan(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.TAN))
}

/** Math.asin(a) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun asin(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ASIN))
}

/** Math.acos(a) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun acos(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ACOS))
}

/** atan(a) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ATAN))
}

/** atan2(a,b) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.ATAN2))
}

/** atan2(a,b) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.ATAN2))
}

/** atan2(a,b) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.ATAN2))
}

/** cube root */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cbrt(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.CBRT))
}

/** Returns [ifTrue] when [condition] > 0, otherwise [ifFalse]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ifElse(condition: RcFloat, ifTrue: RcFloat, ifFalse: RcFloat): RcFloat {
    return RcFloat(
        condition.writer ?: ifTrue.writer ?: ifFalse.writer,
        floatArrayOf(*ifFalse.array, *ifTrue.array, *condition.array, Rc.FloatExpression.IFELSE),
    )
}

/** convert radians to degrees */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toDeg(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.DEG))
}

/** convert degrees to radians */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toRad(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.RAD))
}

/**
 * Refers to property [a] of the second particle in a dual-particle [RcScope.particlesComparison].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun second(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.CMD2))
}

/**
 * Refers to property [a] of the first particle in a dual-particle [RcScope.particlesComparison].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun first(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.CMD1))
}

/** Calculates a deterministic pseudo-random `0..1` number based on seed [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun noiseFrom(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.NOISE_FROM))
}

/** Returns the sum of the squares of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sqrSum(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.SQUARE_SUM))
}

/** Returns `1f` when [a] is greater than [b], and `0f` otherwise. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun step(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.STEP))
}

/** output goes smoothly from 0 to 1 as value goes from min to max */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun smoothStep(value: RcFloat, min: RcFloat, max: RcFloat): RcFloat {

    var writer = value.writer
    if (writer == null) {
        writer = min.writer
    }
    if (writer == null) {
        writer = max.writer
    }
    if (writer == null) {
        throw IllegalStateException("one of the inputs must have a writer")
    }
    return RcFloat(
        writer,
        floatArrayOf(*value.array, *max.array, *min.array, Rc.FloatExpression.SMOOTH_STEP),
    )
}

/** output goes from 0 to max and back with x */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pingPong(max: RcFloat, x: RcFloat): RcFloat {

    var writer = x.writer
    if (writer == null) {
        writer = max.writer
    }

    if (writer == null) {
        throw IllegalStateException("one of the inputs must have a writer")
    }
    return RcFloat(writer, floatArrayOf(*x.array, *max.array, Rc.FloatExpression.PINGPONG))
}

/** linear interpolation (1-t)*x+t*y; */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun lerp(x: RcFloat, y: RcFloat, t: RcFloat): RcFloat {
    var writer = x.writer
    if (writer == null) {
        writer = y.writer
    }
    if (writer == null) {
        writer = t.writer
    }
    if (writer == null) {
        throw IllegalStateException("one of the inputs must have a writer")
    }
    return RcFloat(writer, floatArrayOf(*x.array, *y.array, *t.array, Rc.FloatExpression.LERP))
}

/** Math.hypot */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun hypot(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.HYPOT))
}

/** random number in range */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun random(min: RcFloat, max: RcFloat): RcFloat {
    return RcFloat(
        min.writer,
        floatArrayOf(*min.array, *max.array, Rc.FloatExpression.RAND_IN_RANGE),
    )
}

/** parameters can be float or RcFloat. Coded this way to not require 8 versions returns a*b+c */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun mad(a: RcFloat, b: RcFloat, c: RcFloat): RcFloat {
    return RcFloat(
        null,
        floatArrayOf(*(toArray(a)), *(toArray(b)), *(toArray(c)), Rc.FloatExpression.MAD),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toArray(a: RcFloat): FloatArray {

    if (a.id.isNaN()) {
        return floatArrayOf(a.id)
    }
    return a.array
}

/** clamp a value between min and max */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun clamp(min: RcFloat, max: RcFloat, value: RcFloat): RcFloat {
    return RcFloat(
        value.writer,
        floatArrayOf(*(toArray(min)), *(toArray(max)), *(toArray(value)), Rc.FloatExpression.CLAMP),
    )
}

/** Evaluates a cubic Bézier curve with control points ([x1], [y1]) and ([x2], [y2]) at [value]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cubic(x1: RcFloat, x2: RcFloat, y1: RcFloat, y2: RcFloat, value: RcFloat): RcFloat {
    val writer =
        if (value.writer != null) value.writer
        else if (x1.writer != null) x1.writer
        else if (x2.writer != null) x2.writer
        else if (y1.writer != null) y1.writer else if (y2.writer != null) y2.writer else null
    if (writer == null) {
        throw IllegalStateException("one of the inputs must be an RcFloat")
    }
    return RcFloat(
        writer,
        floatArrayOf(
            *(toArray(x1)),
            *(toArray(y1)),
            *(toArray(x2)),
            *(toArray(y2)),
            *(toArray(value)),
            Rc.FloatExpression.CUBIC,
        ),
    )
}

/* ==================== Array operations ================== */

/** maximum value of an array */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayMax(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_MAX))
}

/** The minimum value of an array */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayMin(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_MIN))
}

/** the sum of the values of an array */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySum(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_SUM))
}

/** the sum of the values of an array up to index */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySum(a: RcFloat, index: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *index.array, Rc.FloatExpression.A_SUM_UNTIL))
}

/** Returns the dot product (sum of element-wise products) of arrays [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySumXY(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.A_SUM_XY))
}

/** Returns the sum of the squares of all elements in array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySumSqr(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_SUM_SQR))
}

/** the avg values of an array */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayAvg(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_AVG))
}

/** the length of an array */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayLength(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_LEN))
}

/** treat the array as a spline and get a value 0 = start 1 = end */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySpline(a: RcFloat, pos: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *pos.array, Rc.FloatExpression.A_SPLINE))
}

/** treat the array as a spline that loops and get a value 0 = start 1 = start & end */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun splineLoop(a: RcFloat, pos: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.array, *pos.array, Rc.FloatExpression.A_SPLINE_LOOP))
}

/** generate random number */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeWriter.rand(): RcFloat {
    return RcFloat(this, Rc.FloatExpression.RAND)
}

/** the index variable in the particle system */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeWriter.index(): RcFloat {
    return RcFloat(this, Rc.FloatExpression.VAR1)
}

/** The time in seconds relative to animation 0 at start of running */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeWriter.animationTime(): RcFloat {
    return RcFloat(this, FLOAT_ANIMATION_TIME)
}

/** Returns the frame delta time in seconds. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeWriter.deltaTime(): RcFloat {
    return RcFloat(this, FLOAT_ANIMATION_DELTA_TIME)
}

/** First expression parameter variable (`VAR1`). */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteComposeWriter.var1: RcFloat
    get() = RcFloat(this, floatArrayOf(Rc.FloatExpression.VAR1))

/** Creates an [RcFloat] from raw RPN expression elements bound to this [RemoteComposeWriter]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeWriter.rf(vararg elements: Float): RcFloat {
    return RcFloat(this, elements)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeWriter.rf(v: Number): RcFloat {
    return RcFloat(this, v.toFloat())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.times(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.MUL))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.plus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.ADD))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.minus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.SUB))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.div(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.DIV))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.rem(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.MOD))
}
