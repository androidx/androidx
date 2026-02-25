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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext.FLOAT_ANIMATION_DELTA_TIME
import androidx.compose.remote.core.RemoteContext.FLOAT_ANIMATION_TIME
import androidx.compose.remote.core.RemoteContext.FLOAT_CALENDAR_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_DAY_OF_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_WEEK_DAY
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression

/**
 * This is a collection of utilities that make RFloat class and allows kotlin float expressions to
 * be converted to remote compose RPM expressions
 */
public fun RemoteComposeWriter.rf(vararg elements: Float): RFloat {
    return RFloat(this, elements)
}

public fun RemoteComposeWriter.rf(v: Number): RFloat {
    if (v is RFloat) return v
    return RFloat(this, v.toFloat())
}

public operator fun Float.times(v: RFloat): RFloat {
    return RFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.MUL))
}

public operator fun Float.plus(v: RFloat): RFloat {
    return RFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.ADD))
}

public operator fun Float.minus(v: RFloat): RFloat {
    return RFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.SUB))
}

public operator fun Float.div(v: RFloat): RFloat {
    return RFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.DIV))
}

public operator fun Float.rem(v: RFloat): RFloat {
    return RFloat(v.writer, floatArrayOf(this, *v.array, Rc.FloatExpression.MOD))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open public class RFloat : Number {
    public var array: FloatArray = floatArrayOf()
    public var id: Float = 0f // if 0 it has not been sent
    public var writer: RemoteComposeWriter? = null
    public var animation: FloatArray? = null

    public constructor(writer: RemoteComposeWriter?, array: FloatArray) {
        this.array = array
        this.writer = writer
    }

    public constructor(writer: RemoteComposeWriter?, a: Float) {
        if (a.isNaN()) {
            id = a
        }
        this.array = floatArrayOf(a)
        this.writer = writer
    }

    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun toDouble(): Double {
        TODO("Not yet implemented")
    }

    public fun toArray(): FloatArray {
        return array
    }

    override fun toFloat(): Float {
        if (!id.isNaN()) {
            if (animation != null) {
                id = writer?.floatExpression(array, animation)!!
            } else {
                id = writer?.floatExpression(*array)!!
            }
        }
        return id
    }

    public fun flush(): RFloat {
        toFloat()
        return this
    }

    override fun toInt(): Int {
        TODO("Not yet implemented")
    }

    override fun toLong(): Long {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }

    @Deprecated(
        "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\nIf you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.\nSee https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration",
        ReplaceWith("this.toInt().toChar()"),
    )
    override fun toChar(): Char {
        return toInt().toChar()
    }

    public operator fun unaryMinus(): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), -1f, Rc.FloatExpression.MUL))
    }

    public operator fun unaryPlus(): RFloat {
        return this
    }

    public operator fun rem(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.MOD))
    }

    public operator fun rem(v: RFloat): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.MOD))
    }

    public fun min(v: RFloat): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.MIN))
    }

    public fun min(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.MIN))
    }

    public operator fun plus(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.ADD))
    }

    public operator fun plus(v: RFloat): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.ADD))
    }

    public operator fun plus(v: Number): RFloat {
        if (v is RFloat) {
            return plus(v)
        }
        return plus(v.toFloat())
    }

    public operator fun minus(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.SUB))
    }

    public operator fun minus(v: RFloat): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.SUB))
    }

    public operator fun minus(v: Number): RFloat {
        if (v is RFloat) {
            return minus(v)
        }
        return minus(v.toFloat())
    }

    public operator fun times(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.MUL))
    }

    public operator fun times(v: RFloat): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.MUL))
    }

    public operator fun div(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, Rc.FloatExpression.DIV))
    }

    public operator fun div(v: RFloat): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), *toArray(v), Rc.FloatExpression.DIV))
    }

    public operator fun get(index: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(index), Rc.FloatExpression.A_DEREF),
        )
    }

    public operator fun get(index: Int): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), index.toFloat(), Rc.FloatExpression.A_DEREF),
        )
    }

    public companion object {
        public operator fun invoke(float: Float, writer: RemoteComposeWriter? = null): RFloat {
            return RFloat(writer, floatArrayOf(float))
        }
    }

    public fun anim(
        duration: Float,
        type: Int = Rc.Animate.CUBIC_STANDARD,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RFloat {
        animation = writer?.anim(duration, type, spec, initialValue, wrap)
        this.flush()
        return this
    }

    public fun genTextId(
        before: Int = 2,
        after: Int = 1,
        flags: Int = Rc.TextFromFloat.PAD_AFTER_ZERO,
    ): Int {
        val w = writer
        if (w == null) {
            throw IllegalStateException("writer is null")
        }
        return w.createTextFromFloat(this.toFloat(), before, after, flags)
    }
}

public fun toFloat(a: Number): Float {
    return when (a) {
        is RFloat -> a.id
        else -> a.toFloat()
    }
}

public fun arrayValue(array: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(array, *b.array, Rc.FloatExpression.A_DEREF))
}

public fun max(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.MAX))
}

public fun max(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.MAX))
}

public fun max(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.MAX))
}

public fun min(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.MIN))
}

public fun min(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.MIN))
}

public fun min(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.MIN))
}

public fun pow(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.POW))
}

public fun pow(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.POW))
}

public fun pow(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.POW))
}

public fun sqrt(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SQRT))
}

public fun abs(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ABS))
}

/**
 * Returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument
 * is greater than zero, -1.0f if the argument is less than zero.
 */
public fun sign(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SIGN))
}

public fun copySign(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.COPY_SIGN))
}

public fun copySign(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.COPY_SIGN))
}

public fun copySign(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.COPY_SIGN))
}

public fun exp(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.EXP))
}

public fun ceil(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.CEIL))
}

public fun floor(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.FLOOR))
}

public fun log(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.LOG))
}

public fun log2(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.LOG2))
}

public fun ln(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.LN))
}

public fun round(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ROUND))
}

public fun inverse(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.INV))
}

public fun fraction(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.FRACT))
}

public fun square(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SQUARE))
}

/** Math.sin(a) */
public fun sin(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.SIN))
}

public fun cos(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.COS))
}

/** Math.tan(a) */
public fun tan(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.TAN))
}

/** Math.asin(a) */
public fun asin(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ASIN))
}

/** Math.acos(a) */
public fun acos(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ACOS))
}

/** atan(a) */
public fun atan(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.ATAN))
}

/** atan2(a,b) */
public fun atan2(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, Rc.FloatExpression.ATAN2))
}

/** atan2(a,b) */
public fun atan2(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, Rc.FloatExpression.ATAN2))
}

/** atan2(a,b) */
public fun atan2(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.ATAN2))
}

/** cube root */
public fun cbrt(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.CBRT))
}

/** if (c) b else c */
public fun ifThenElse(a: RFloat, b: RFloat, c: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, *c.array, Rc.FloatExpression.IFELSE))
}

/** if (a) b else c */
public fun ifElse(a: RFloat, b: RFloat, c: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*c.array, *b.array, *a.array, Rc.FloatExpression.IFELSE))
}

/** convert radians to degrees */
public fun toDeg(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.DEG))
}

/** convert degrees to radians */
public fun toRad(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.RAD))
}

/** convert degrees to radians */
public fun second(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.CMD2))
}

/** convert degrees to radians */
public fun first(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.CMD1))
}

/** NOISE_FROM operator calculate a random 0..1 number based on a seed */
public fun noiseFrom(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.NOISE_FROM))
}

/** the sum of the square of two numbers */
public fun sqrSum(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.SQUARE_SUM))
}

/**  */
public fun step(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.STEP))
}

/** output goes smoothly from 0 to 1 as value goes from min to max */
public fun smoothStep(value: RFloat, min: RFloat, max: RFloat): RFloat {
    return RFloat(
        value.writer,
        floatArrayOf(*value.array, *max.array, *min.array, Rc.FloatExpression.SMOOTH_STEP),
    )
}

/** output goes smoothly from 0 to 1 as value goes from min to max */
public fun smoothStep(value: Number, min: Number, max: Number): RFloat {
    val valuer = value as? RFloat ?: RFloat(null, value.toFloat())
    val minr = min as? RFloat ?: RFloat(null, min.toFloat())
    val maxr = max as? RFloat ?: RFloat(null, max.toFloat())
    var writer = valuer.writer
    if (writer == null) {
        writer = minr.writer
    }
    if (writer == null) {
        writer = maxr.writer
    }
    if (writer == null) {
        throw IllegalStateException("one of the inputs must have a writer")
    }
    return RFloat(
        writer,
        floatArrayOf(*valuer.array, *maxr.array, *minr.array, Rc.FloatExpression.SMOOTH_STEP),
    )
}

/** output goes from 0 to max and back with x */
public fun pingPong(max: Number, x: Number): RFloat {
    val xr = x as? RFloat ?: RFloat(null, x.toFloat())
    val maxr = max as? RFloat ?: RFloat(null, max.toFloat())
    var writer = xr.writer
    if (writer == null) {
        writer = xr.writer
    }

    if (writer == null) {
        throw IllegalStateException("one of the inputs must have a writer")
    }
    return RFloat(writer, floatArrayOf(*xr.array, *maxr.array, Rc.FloatExpression.PINGPONG))
}

/** linear interpolation (1-t)*x+t*y; */
public fun lerp(x: RFloat, y: RFloat, t: RFloat): RFloat {
    return RFloat(x.writer, floatArrayOf(*x.array, *y.array, *t.array, Rc.FloatExpression.LERP))
}

/** linear interpolation (1-t)*x+t*y; */
public fun lerp(x: Number, y: Number, t: Number): RFloat {
    val xr = x as? RFloat ?: RFloat(null, x.toFloat())
    val yr = y as? RFloat ?: RFloat(null, y.toFloat())
    val tr = t as? RFloat ?: RFloat(null, t.toFloat())
    var writer = xr.writer
    if (writer == null) {
        writer = yr.writer
    }
    if (writer == null) {
        writer = tr.writer
    }
    if (writer == null) {
        throw IllegalStateException("one of the inputs must have a writer")
    }
    return RFloat(writer, floatArrayOf(*xr.array, *yr.array, *tr.array, Rc.FloatExpression.LERP))
}

/** Math.hypot */
public fun hypot(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.HYPOT))
}

/** random number in range */
public fun random(min: RFloat, max: RFloat): RFloat {
    return RFloat(
        min.writer,
        floatArrayOf(*min.array, *max.array, Rc.FloatExpression.RAND_IN_RANGE),
    )
}

/** parameters can be float or RFloat. Coded this way to not require 8 versions returns a*b+c */
public fun mad(a: Number, b: Number, c: Number): RFloat {
    return RFloat(
        null,
        floatArrayOf(*(toArray(a)), *(toArray(b)), *(toArray(c)), Rc.FloatExpression.MAD),
    )
}

/** parameters can be float or RFloat. Coded this way to not require 8 versions returns a*b+c */
public fun RemoteComposeWriter.ifElse(a: Number, b: Number, c: Number): RFloat {
    return RFloat(
        this,
        floatArrayOf(*(toArray(c)), *(toArray(b)), *(toArray(a)), Rc.FloatExpression.IFELSE),
    )
}

public fun toArray(a: RFloat): FloatArray {

    if (a.id.isNaN()) {
        return floatArrayOf(a.id)
    }
    return a.array
}

public fun toArray(a: Number): FloatArray {
    if (a is RFloat) {
        if (a.id.isNaN()) {
            return floatArrayOf(a.id)
        }
        return a.array
    }
    return floatArrayOf(a.toFloat())
}

/** clamp a value between min and max */
public fun clamp(min: Number, max: Number, value: RFloat): RFloat {
    return RFloat(
        value.writer,
        floatArrayOf(*(toArray(min)), *(toArray(max)), *(toArray(value)), Rc.FloatExpression.CLAMP),
    )
}

/** clamp a value between min and max */
public fun cubic(x1: Number, x2: Number, y1: Number, y2: Number, value: Number): RFloat {
    val writer =
        if (value is RFloat) value.writer
        else if (x1 is RFloat) x1.writer
        else if (x2 is RFloat) x2.writer
        else if (y1 is RFloat) y1.writer else if (y2 is RFloat) y2.writer else null
    if (writer == null) {
        throw IllegalStateException("one of the inputs must be an RFloat")
    }
    return RFloat(
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
public fun arrayMax(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_MAX))
}

/** The minimum value of an array */
public fun arrayMin(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_MIN))
}

/** the sum of the values of an array */
public fun arraySum(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_SUM))
}

/** the sum of the values of an array up to index */
public fun arraySum(a: RFloat, index: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *index.array, Rc.FloatExpression.A_SUM_UNTIL))
}

@Suppress("RestrictedApiAndroidX")
public fun arraySumXY(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, Rc.FloatExpression.A_SUM_XY))
}

@Suppress("RestrictedApiAndroidX")
public fun arraySumSqr(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_SUM_SQR))
}

/** the avg values of an array */
public fun arrayAvg(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_AVG))
}

/** the length of an array */
public fun arrayLength(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_LEN))
}

/** treat the array as a spline and get a value 0 = start 1 = end */
public fun arraySpline(a: RFloat, pos: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *pos.array, Rc.FloatExpression.A_SPLINE))
}

/** treat the array as a spline that loops and get a value 0 = start 1 = start & end */
public fun splineLoop(a: RFloat, pos: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *pos.array, Rc.FloatExpression.A_SPLINE_LOOP))
}

/** hours run from Midnight=0 quantized to Hours 0-23 */
public fun RemoteComposeWriter.Hour(): RFloat {
    return RFloat(this, FLOAT_TIME_IN_HR)
}

/** minutes run from Midnight=0 quantized to minutes 0..1439 */
public fun RemoteComposeWriter.Minutes(): RFloat {
    return RFloat(this, FLOAT_TIME_IN_MIN)
}

/** seconds run from Midnight=0 quantized to seconds hour 0..3599 */
public fun RemoteComposeWriter.Seconds(): RFloat {
    return RFloat(this, FLOAT_TIME_IN_SEC)
}

/** CONTINUOUS_SEC is seconds from midnight looping every hour 0-3600 */
public fun RemoteComposeWriter.ContinuousSec(): RFloat {
    return RFloat(this, FLOAT_CONTINUOUS_SEC)
}

/** ID_OFFSET_TO_UTC is the offset from UTC in sec (typically / 3600f) */
public fun RemoteComposeWriter.UtcOffset(): RFloat {
    return RFloat(this, FLOAT_OFFSET_TO_UTC)
}

/** DAY OF THE WEEK 1-7. 1 = Monday */
public fun RemoteComposeWriter.DayOfWeek(): RFloat {
    return RFloat(this, FLOAT_WEEK_DAY)
}

/** Moth of Year quantized to MONTHS 1-12. 1 = January */
public fun RemoteComposeWriter.Month(): RFloat {
    return RFloat(this, FLOAT_CALENDAR_MONTH)
}

/** DAY OF THE MONTH 1-31 */
public fun RemoteComposeWriter.DayOfMonth(): RFloat {
    return RFloat(this, FLOAT_DAY_OF_MONTH)
}

/** Width */
public fun RemoteComposeWriter.ComponentWidth(): RFloat {
    return RFloat(this, addComponentWidthValue())
}

/** Height */
public fun RemoteComposeWriter.ComponentHeight(): RFloat {
    return RFloat(this, addComponentHeightValue())
}

/** Content Width */
public fun RemoteComposeWriter.ComponentContentWidth(): RFloat {
    return RFloat(this, addComponentContentWidthValue())
}

/** Content Height */
public fun RemoteComposeWriter.ComponentContentHeight(): RFloat {
    return RFloat(this, addComponentContentHeightValue())
}

/** X */
public fun RemoteComposeWriter.ComponentX(): RFloat {
    return RFloat(this, addComponentXValue())
}

/** Y */
public fun RemoteComposeWriter.ComponentY(): RFloat {
    return RFloat(this, addComponentYValue())
}

/** ROOT X */
public fun RemoteComposeWriter.ComponentRootX(): RFloat {
    return RFloat(this, addComponentRootXValue())
}

/** ROOT Y */
public fun RemoteComposeWriter.ComponentRootY(): RFloat {
    return RFloat(this, addComponentRootYValue())
}

/** generate random number */
public fun RemoteComposeWriter.rand(): RFloat {
    return RFloat(this, Rc.FloatExpression.RAND)
}

/** the index variable in the particle system */
public fun RemoteComposeWriter.index(): RFloat {
    return RFloat(this, Rc.FloatExpression.VAR1)
}

/** The time in seconds relative to animation 0 at start of running */
public fun RemoteComposeWriter.animationTime(): RFloat {
    return RFloat(this, FLOAT_ANIMATION_TIME)
}

/** The time in seconds relative to animation 0 at start of running */
public fun RemoteComposeWriter.deltaTime(): RFloat {
    return RFloat(this, FLOAT_ANIMATION_DELTA_TIME)
}

/** The width of the document on screen */
public fun RemoteComposeWriter.windowWidth(): RFloat {
    return RFloat(this, Rc.System.WINDOW_WIDTH)
}

/** The height of the document on screen */
public fun RemoteComposeWriter.windowHeight(): RFloat {
    return RFloat(this, Rc.System.WINDOW_WIDTH)
}

public val RemoteComposeWriter.var1: RFloat
    get() = RFloat(this, floatArrayOf(Rc.FloatExpression.VAR1))
