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
@file:JvmName("RcFloats")

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
public class RcFloat {
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

    internal fun toArray(): FloatArray {
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

    @JvmName("withWriter")
    internal fun withWriter(writer: RemoteComposeWriter): RcFloat {
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
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.MIN),
        )
    }

    public fun min(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.MIN))
    }

    /** Returns the larger of this expression and [v]. */
    public fun max(v: RcFloat): RcFloat {
        return RcFloat(
            writer ?: v.writer,
            floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.MAX),
        )
    }

    /** Returns the larger of this expression and [v]. */
    public fun max(v: Float): RcFloat {
        return RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.MAX))
    }

    public operator fun get(index: RcFloat): RcFloat {
        return RcFloat(
            writer ?: index.writer,
            floatArrayOf(*toArray(), *index.toArray(), Rc.FloatExpression.A_DEREF),
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
            floatArrayOf(*toArray(), index.toFloat(), Rc.FloatExpression.A_DEREF),
        )
    }

    public companion object {
        internal operator fun invoke(float: Float, writer: RemoteComposeWriter? = null): RcFloat {
            return RcFloat(writer, floatArrayOf(float))
        }
    }

    private fun anim(
        duration: Float,
        type: Int,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RcFloat {
        animation = writer?.anim(duration, type, spec, initialValue, wrap)
        this.flush()
        return this
    }

    /** Animates changes to this expression using a spring physics model. */
    public fun spring(
        stiffness: Float,
        damping: Float,
        stopThreshold: Float,
        boundaryMode: RcSpringBoundaryMode = RcSpringBoundaryMode.Overshoot,
    ): RcFloat {
        animation = writer?.spring(stiffness, damping, stopThreshold, boundaryMode.value)
        this.flush()
        return this
    }

    /**
     * Animates changes to this expression over [duration] seconds using [curve].
     *
     * [RcAnimationCurve.CubicCustom] and [RcAnimationCurve.SplineCustom] require a non-null [spec]
     * containing the control points; the other curves ignore [spec].
     */
    public fun anim(
        duration: Float,
        curve: RcAnimationCurve = RcAnimationCurve.CubicStandard,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RcFloat = anim(duration, curve.value, spec, initialValue, wrap)

    /** Animates changes to this expression over a dynamic [duration] (in seconds) using [curve]. */
    public fun anim(
        duration: RcFloat,
        curve: RcAnimationCurve = RcAnimationCurve.CubicStandard,
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
     * Converts this float expression to an [RcText] using the specified [RcTextFromFloatSpec]
     * formatting.
     *
     * @param whole the number of digits before the decimal point
     * @param decimal the number of digits after the decimal point
     * @param spec typed formatting specification
     */
    public fun format(
        whole: Int,
        decimal: Int = 0,
        spec: RcTextFromFloatSpec = RcTextFromFloatSpec.Default,
    ): RcText {
        val w = writer ?: throw IllegalStateException("RcFloat must be associated with a writer")
        return RcText(
            w.createTextFromFloat(toFloat(), whole.toShort(), decimal.toShort(), spec.bits)
        )
    }

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

internal fun arrayValue(array: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(array, *b.toArray(), Rc.FloatExpression.A_DEREF))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MAX))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MAX))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MAX),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MIN),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.POW))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.POW))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pow(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.POW),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sqrt(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.SQRT))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun abs(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.ABS))
}

/**
 * Returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument
 * is greater than zero, -1.0f if the argument is less than zero.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sign(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.COPY_SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.COPY_SIGN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.COPY_SIGN),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun exp(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.EXP))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ceil(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.CEIL))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun floor(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.FLOOR))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun log(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.LOG))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun log2(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.LOG2))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ln(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.LN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun round(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.ROUND))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun inverse(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.INV))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun fraction(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.FRACT))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun square(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.SQUARE))
}

/** Returns the sine of [a] (in radians). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sin(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.SIN))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cos(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.COS))
}

/** Returns the tangent of [a] (in radians). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun tan(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.TAN))
}

/** Returns the arc sine of [a] in radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun asin(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.ASIN))
}

/** Returns the arc cosine of [a] in radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun acos(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.ACOS))
}

/** Returns the arc tangent of [a] in radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.ATAN))
}

/** Returns the angle `atan2(a, b)` in radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: RcFloat, b: Float): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.ATAN2))
}

/** Returns the angle `atan2(a, b)` in radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: Float, b: RcFloat): RcFloat {
    return RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.ATAN2))
}

/** Returns the angle `atan2(a, b)` in radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun atan2(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.ATAN2),
    )
}

/** Returns the cube root of [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cbrt(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.CBRT))
}

/** Returns [ifTrue] when [condition] > 0, otherwise [ifFalse]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ifElse(condition: RcFloat, ifTrue: RcFloat, ifFalse: RcFloat): RcFloat {
    return RcFloat(
        condition.writer ?: ifTrue.writer ?: ifFalse.writer,
        floatArrayOf(
            *ifFalse.toArray(),
            *ifTrue.toArray(),
            *condition.toArray(),
            Rc.FloatExpression.IFELSE,
        ),
    )
}

/** Converts [a] from radians to degrees. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toDegrees(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.DEG))
}

/** Converts [a] from degrees to radians. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toRadians(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.RAD))
}

/**
 * Refers to property [a] of the second particle in a dual-particle
 * [RcCanvasScope.particlesComparison].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun second(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), AnimatedFloatExpression.CMD2))
}

/**
 * Refers to property [a] of the first particle in a dual-particle
 * [RcCanvasScope.particlesComparison].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun first(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), AnimatedFloatExpression.CMD1))
}

/** Calculates a deterministic pseudo-random `0..1` number based on seed [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun noiseFrom(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.NOISE_FROM))
}

/** Returns the sum of the squares of [a] and [b] (`a * a + b * b`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun squareSum(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.SQUARE_SUM),
    )
}

/** Returns `1f` when [a] is greater than [b], and `0f` otherwise. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun step(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.STEP),
    )
}

/** Smoothly interpolates from `0` to `1` as [value] progresses between [min] and [max]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun smoothStep(value: RcFloat, min: RcFloat, max: RcFloat): RcFloat {
    return RcFloat(
        value.writer ?: min.writer ?: max.writer,
        floatArrayOf(
            *value.toArray(),
            *max.toArray(),
            *min.toArray(),
            Rc.FloatExpression.SMOOTH_STEP,
        ),
    )
}

/** Oscillates smoothly from `0` to [max] and back as [x] advances. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun pingPong(max: RcFloat, x: RcFloat): RcFloat {
    return RcFloat(
        x.writer ?: max.writer,
        floatArrayOf(*x.toArray(), *max.toArray(), Rc.FloatExpression.PINGPONG),
    )
}

/** Linearly interpolates `(1 - t) * x + t * y`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun lerp(x: RcFloat, y: RcFloat, t: RcFloat): RcFloat {
    return RcFloat(
        x.writer ?: y.writer ?: t.writer,
        floatArrayOf(*x.toArray(), *y.toArray(), *t.toArray(), Rc.FloatExpression.LERP),
    )
}

/** Returns `sqrt(a * a + b * b)` without intermediate overflow or underflow. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun hypot(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.HYPOT),
    )
}

/** Generates a random number in `[min, max]`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun random(min: RcFloat, max: RcFloat): RcFloat {
    return RcFloat(
        min.writer ?: max.writer,
        floatArrayOf(*min.toArray(), *max.toArray(), Rc.FloatExpression.RAND_IN_RANGE),
    )
}

/** Returns fused multiply-add `a * b + c`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun mad(a: RcFloat, b: RcFloat, c: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer ?: c.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), *c.toArray(), Rc.FloatExpression.MAD),
    )
}

internal fun toArray(a: RcFloat): FloatArray = a.toArray()

/** Clamps [value] between [min] and [max]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun clamp(min: RcFloat, max: RcFloat, value: RcFloat): RcFloat {
    return RcFloat(
        value.writer ?: min.writer ?: max.writer,
        floatArrayOf(*min.toArray(), *max.toArray(), *value.toArray(), Rc.FloatExpression.CLAMP),
    )
}

/** Evaluates a cubic Bézier curve with control points ([x1], [y1]) and ([x2], [y2]) at [value]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cubic(x1: RcFloat, x2: RcFloat, y1: RcFloat, y2: RcFloat, value: RcFloat): RcFloat {
    val writer = value.writer ?: x1.writer ?: x2.writer ?: y1.writer ?: y2.writer
    return RcFloat(
        writer,
        floatArrayOf(
            *x1.toArray(),
            *y1.toArray(),
            *x2.toArray(),
            *y2.toArray(),
            *value.toArray(),
            Rc.FloatExpression.CUBIC,
        ),
    )
}

/* ==================== Array operations ================== */

/** Returns the maximum element of array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayMax(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_MAX))
}

/** Returns the minimum element of array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayMin(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_MIN))
}

/** Returns the sum of all elements in array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySum(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_SUM))
}

/** Returns the sum of elements in array [a] up to [index]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySum(a: RcFloat, index: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: index.writer,
        floatArrayOf(*a.toArray(), *index.toArray(), Rc.FloatExpression.A_SUM_UNTIL),
    )
}

/** Returns the dot product (sum of element-wise products) of arrays [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySumXY(a: RcFloat, b: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: b.writer,
        floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.A_SUM_XY),
    )
}

/** Returns the sum of the squares of all elements in array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySumSqr(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_SUM_SQR))
}

/** Returns the arithmetic mean of all elements in array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayAvg(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_AVG))
}

/** Returns the element count of array [a]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayLength(a: RcFloat): RcFloat {
    return RcFloat(a.writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_LEN))
}

/** Interpolates array [array] as a spline at normalized static [position] (`0f..1f`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySpline(array: RcFloat, position: Float): RcFloat {
    return RcFloat(
        array.writer,
        floatArrayOf(*array.toArray(), position, Rc.FloatExpression.A_SPLINE),
    )
}

/** Interpolates array [a] as a spline at normalized dynamic [pos] (`0f..1f`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySpline(a: RcFloat, pos: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: pos.writer,
        floatArrayOf(*a.toArray(), *pos.toArray(), Rc.FloatExpression.A_SPLINE),
    )
}

/** Interpolates array [a] as a looping spline at normalized dynamic [pos] (`0f..1f`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun splineLoop(a: RcFloat, pos: RcFloat): RcFloat {
    return RcFloat(
        a.writer ?: pos.writer,
        floatArrayOf(*a.toArray(), *pos.toArray(), Rc.FloatExpression.A_SPLINE_LOOP),
    )
}

/** generate random number */
internal fun RemoteComposeWriter.rand(): RcFloat {
    return RcFloat(this, Rc.FloatExpression.RAND)
}

/** the index variable in the particle system */
internal fun RemoteComposeWriter.index(): RcFloat {
    return RcFloat(this, Rc.FloatExpression.VAR1)
}

/** The time in seconds relative to animation 0 at start of running */
internal fun RemoteComposeWriter.animationTime(): RcFloat {
    return RcFloat(this, FLOAT_ANIMATION_TIME)
}

/** Returns the frame delta time in seconds. */
internal fun RemoteComposeWriter.deltaTime(): RcFloat {
    return RcFloat(this, FLOAT_ANIMATION_DELTA_TIME)
}

/** First expression parameter variable (`VAR1`). */
internal val RemoteComposeWriter.var1: RcFloat
    get() = RcFloat(this, floatArrayOf(Rc.FloatExpression.VAR1))

/** Creates an [RcFloat] from raw RPN expression elements bound to this [RemoteComposeWriter]. */
internal fun RemoteComposeWriter.rf(vararg elements: Float): RcFloat {
    return RcFloat(this, elements)
}

internal fun RemoteComposeWriter.rf(v: Number): RcFloat {
    return RcFloat(this, v.toFloat())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.times(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.MUL))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.plus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.ADD))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.minus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.SUB))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.div(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.DIV))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.rem(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.MOD))
}
