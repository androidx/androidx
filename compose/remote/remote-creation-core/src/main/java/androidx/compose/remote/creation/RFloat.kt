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
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAND
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1

public fun RemoteComposeWriter.rf(vararg elements: Float): RFloat {
    return RFloat(this, elements)
}

public fun RemoteComposeWriter.rf(v: Number): RFloat {
    if (v is RFloat) return v
    return RFloat(this, v.toFloat())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RFloat : Number {
    public var array: FloatArray = floatArrayOf()
    public var id: Float = 0f // if 0 it has not been sent
    public var writer: RemoteComposeWriter? = null

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
            id = writer?.floatExpression(*array)!!
        }
        return id
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

    public operator fun unaryPlus(): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), -1f, AnimatedFloatExpression.MUL))
    }

    public operator fun rem(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, AnimatedFloatExpression.MOD))
    }

    public operator fun rem(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(v), AnimatedFloatExpression.MOD),
        )
    }

    public fun min(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(v), AnimatedFloatExpression.MIN),
        )
    }

    public fun min(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, AnimatedFloatExpression.MIN))
    }

    public operator fun plus(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, AnimatedFloatExpression.ADD))
    }

    public operator fun plus(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(v), AnimatedFloatExpression.ADD),
        )
    }

    public operator fun minus(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, AnimatedFloatExpression.SUB))
    }

    public operator fun minus(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(v), AnimatedFloatExpression.SUB),
        )
    }

    public operator fun times(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, AnimatedFloatExpression.MUL))
    }

    public operator fun times(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(v), AnimatedFloatExpression.MUL),
        )
    }

    public operator fun div(v: Float): RFloat {
        return RFloat(writer, floatArrayOf(*toArray(this), v, AnimatedFloatExpression.DIV))
    }

    public operator fun div(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(this), *toArray(v), AnimatedFloatExpression.DIV),
        )
    }

    public operator fun get(v: RFloat): RFloat {
        return RFloat(
            writer,
            floatArrayOf(*toArray(v), *toArray(this), AnimatedFloatExpression.A_DEREF),
        )
    }

    public operator fun get(v: Int): RFloat {
        return RFloat(
            writer,
            floatArrayOf(v.toFloat(), *toArray(this), AnimatedFloatExpression.A_DEREF),
        )
    }

    public companion object {
        public operator fun invoke(float: Float, writer: RemoteComposeWriter? = null): RFloat {
            return RFloat(writer, floatArrayOf(float))
        }
    }
}

public fun toFloat(a: Number): Float {
    return when (a) {
        is RFloat -> a.id
        else -> a.toFloat()
    }
}

public fun max(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, AnimatedFloatExpression.MAX))
}

public fun max(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, AnimatedFloatExpression.MAX))
}

public fun max(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.MAX))
}

public fun min(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, AnimatedFloatExpression.MIN))
}

public fun min(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, AnimatedFloatExpression.MIN))
}

public fun min(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.MIN))
}

public fun pow(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, AnimatedFloatExpression.POW))
}

public fun pow(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, AnimatedFloatExpression.POW))
}

public fun pow(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.POW))
}

public fun sqrt(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.SQRT))
}

public fun abs(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.ABS))
}

public fun sign(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.SIGN))
}

public fun copySign(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, AnimatedFloatExpression.COPY_SIGN))
}

public fun copySign(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, AnimatedFloatExpression.COPY_SIGN))
}

public fun copySign(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.COPY_SIGN))
}

public fun exp(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.EXP))
}

public fun ceil(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.CEIL))
}

public fun floor(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.FLOOR))
}

public fun log(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.LOG))
}

public fun ln(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.LN))
}

public fun round(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.ROUND))
}

public fun sin(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.SIN))
}

public fun cos(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.COS))
}

public fun tan(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.TAN))
}

public fun asin(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.ASIN))
}

public fun acos(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.ACOS))
}

public fun atan(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.ATAN))
}

public fun atan2(a: RFloat, b: Float): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, b, AnimatedFloatExpression.ATAN2))
}

public fun atan2(a: Float, b: RFloat): RFloat {
    return RFloat(b.writer, floatArrayOf(a, *b.array, AnimatedFloatExpression.ATAN2))
}

public fun atan2(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.ATAN2))
}

public fun cbrt(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.CBRT))
}

public fun toDeg(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.DEG))
}

public fun toRad(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.RAD))
}

/** parameters can be float or RFloat. Coded this way to not require 8 versions returns a*b+c */
public fun mad(a: Number, b: Number, c: Number): RFloat {
    return RFloat(
        null,
        floatArrayOf(*(toArray(a)), *(toArray(b)), *(toArray(c)), AnimatedFloatExpression.MAD),
    )
}

/** parameters can be float or RFloat. Coded this way to not require 8 versions returns a*b+c */
public fun RemoteComposeWriter.ifElse(a: Number, b: Number, c: Number): RFloat {
    return RFloat(
        this,
        floatArrayOf(*(toArray(c)), *(toArray(b)), *(toArray(a)), AnimatedFloatExpression.IFELSE),
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

public fun clamp(min: Number, max: Number, value: Number): RFloat {
    return RFloat(
        null,
        floatArrayOf(
            *(toArray(min)),
            *(toArray(max)),
            *(toArray(value)),
            AnimatedFloatExpression.CLAMP,
        ),
    )
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

/** generate random number */
public fun RemoteComposeWriter.rand(): RFloat {
    return RFloat(this, RAND)
}

/** the index variable in the particle system */
public fun RemoteComposeWriter.index(): RFloat {
    return RFloat(this, VAR1)
}

/** The time in seconds relative to animation 0 at start of running */
public fun RemoteComposeWriter.animationTime(): RFloat {
    return RFloat(this, FLOAT_ANIMATION_TIME)
}

/** The time in seconds relative to animation 0 at start of running */
public fun RemoteComposeWriter.deltTime(): RFloat {
    return RFloat(this, FLOAT_ANIMATION_DELTA_TIME)
}
