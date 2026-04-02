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

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import kotlin.jvm.JvmInline

/** Type-safe reference for remote text resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @JvmInline public value class RcText(public val id: Int)

/** Type-safe reference for remote image resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcImage(public val id: Int)

/** Scope for building a path. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcPathScope {
    /** Moves the path to the specified position. */
    public fun moveTo(x: Float, y: Float)

    /** Adds a line to the specified position. */
    public fun lineTo(x: Float, y: Float)

    /** Adds a quadratic bezier curve. */
    public fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float)
}

/** Type-safe reference for remote path data. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcPath(public val id: Int) : RcPathScope {
    internal var writer: RemoteComposeWriter? = null

    internal constructor(id: Int, writer: RemoteComposeWriter?) : this(id) {
        this.writer = writer
    }

    /** Moves the path to the specified position. */
    override fun moveTo(x: Float, y: Float) {
        writer?.pathAppendMoveTo(id, x, y)
    }

    /** Adds a line to the specified position. */
    override fun lineTo(x: Float, y: Float) {
        writer?.pathAppendLineTo(id, x, y)
    }

    /** Adds a quadratic bezier curve. */
    override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        writer?.pathAppendQuadTo(id, x1, y1, x2, y2)
    }

    /** Closes the path. */
    public fun close() {
        writer?.pathAppendClose(id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcPath) return false
        return id == other.id
    }

    override fun hashCode(): Int = id
}

/** Type-safe reference for remote color variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColor(public val id: Int)

/**
 * Type-safe reference for remote float variables or expressions. This often encapsulates a
 * NaN-encoded ID for player-side evaluation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RcFloat {
    public var array: FloatArray = floatArrayOf()
    public var id: Float = Float.NaN
    public var animation: FloatArray? = null
    public var isEvaluated: Boolean = false
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

    /** Commits this expression to the document and returns an [RcFloat] using its ID. */
    public fun flush(): RcFloat {
        if (isEvaluated && id.isNaN()) return this
        val w = writer ?: return this
        id = w.floatExpression(array, animation)
        isEvaluated = true
        return this
    }

    /** Associates this [RcFloat] with a [RemoteComposeWriter] for expression evaluation. */
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

    /** Returns an [RcFloat] representing the absolute value of this expression. */
    public fun abs(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.ABS))

    /** Returns an [RcFloat] representing the square root of this expression. */
    public fun sqrt(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.SQRT))

    /** Returns an [RcFloat] representing the cosine of this expression. */
    public fun cos(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.COS))

    /** Returns an [RcFloat] representing the sine of this expression. */
    public fun sin(): RcFloat = RcFloat(writer, floatArrayOf(*toArray(), Rc.FloatExpression.SIN))

    /** Returns an [RcFloat] representing this expression raised to the power of [v]. */
    public fun pow(v: Float): RcFloat =
        RcFloat(writer, floatArrayOf(*toArray(), v, Rc.FloatExpression.POW))

    /** Returns an [RcFloat] representing this expression raised to the power of [v]. */
    public fun pow(v: RcFloat): RcFloat =
        RcFloat(writer ?: v.writer, floatArrayOf(*toArray(), *v.toArray(), Rc.FloatExpression.POW))

    /** Wraps this expression in an animation with the specified duration. */
    public fun anim(duration: Float = 1f): RcFloat {
        val w = writer ?: return this
        return RcFloat(w, array, id, w.anim(duration), isEvaluated)
    }

    /**
     * Converts this [RcFloat] to an [RcText] using the specified formatting.
     *
     * @param whole the number of digits before the decimal point
     * @param decimal the number of digits after the decimal point
     * @param flags formatting flags (see [Rc.TextFromFloat])
     */
    public fun format(whole: Int, decimal: Int, flags: Int): RcText {
        val w = writer ?: throw IllegalStateException("RcFloat must be associated with a writer")
        return RcText(w.createTextFromFloat(toFloat(), whole, decimal, flags))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcFloat) return false
        if (!array.contentEquals(other.array)) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        var result = array.contentHashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}

public operator fun Float.plus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.ADD))
}

public operator fun Float.minus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.SUB))
}

public operator fun Float.times(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.MUL))
}

public operator fun Float.div(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.DIV))
}

public operator fun Float.rem(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.MOD))
}

/** Returns an [RcFloat] representing the maximum of [a] and [b]. */
public fun max(a: RcFloat, b: RcFloat): RcFloat =
    RcFloat(a.writer ?: b.writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MAX))

/** Returns an [RcFloat] representing the maximum of [a] and [b]. */
public fun max(a: Float, b: RcFloat): RcFloat =
    RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MAX))

/** Returns an [RcFloat] representing the maximum of [a] and [b]. */
public fun max(a: RcFloat, b: Float): RcFloat =
    RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MAX))

/** Returns an [RcFloat] representing the minimum of [a] and [b]. */
public fun min(a: RcFloat, b: RcFloat): RcFloat =
    RcFloat(a.writer ?: b.writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MIN))

/** Returns an [RcFloat] representing the minimum of [a] and [b]. */
public fun min(a: Float, b: RcFloat): RcFloat =
    RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MIN))

/** Returns an [RcFloat] representing the minimum of [a] and [b]. */
public fun min(a: RcFloat, b: Float): RcFloat =
    RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MIN))

/** Returns an [RcFloat] representing the sign of [v]. */
public fun sign(v: RcFloat): RcFloat =
    RcFloat(v.writer, floatArrayOf(*v.toArray(), Rc.FloatExpression.SIGN))

/** Returns an [RcFloat] representing the maximum value in the [array]. */
public fun arrayMax(array: RcFloat): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), Rc.FloatExpression.A_MAX))

/** Returns an [RcFloat] representing the minimum value in the [array]. */
public fun arrayMin(array: RcFloat): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), Rc.FloatExpression.A_MIN))

/** Returns an [RcFloat] interpolated from [array] at [position]. */
public fun arraySpline(array: RcFloat, position: RcFloat): RcFloat =
    RcFloat(
        array.writer ?: position.writer,
        floatArrayOf(*array.toArray(), *position.toArray(), Rc.FloatExpression.A_SPLINE),
    )

/** Returns an [RcFloat] interpolated from [array] at [position]. */
public fun arraySpline(array: RcFloat, position: Float): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), position, Rc.FloatExpression.A_SPLINE))

/** Returns an [RcFloat] representing the current animation time. */
public fun animationTime(): RcFloat = RcFloat(null, floatArrayOf(Rc.Time.ANIMATION_TIME))

/** Returns an [RcFloat] representing the last touch event time. */
public fun touchTime(): RcFloat = RcFloat(null, floatArrayOf(Rc.Touch.TOUCH_EVENT_TIME))

/** Type-safe reference for remote integer/long variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcInteger(public val id: Long)

/** Type-safe reference for remote text styles. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextStyle(public val id: Int)

/** Type-safe dimension in Density-independent Pixels (dp). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDp(public val value: Float)

/** Type-safe dimension in raw pixels (px). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcPx(public val value: Float)

/** Type-safe dimension in Scalable Pixels (sp). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSp(public val value: Float)

/** Extension properties for unit-safe dimension creation. */
public val Int.rdp: RcDp
    get() = RcDp(this.toFloat())
public val Float.rdp: RcDp
    get() = RcDp(this)
public val Double.rdp: RcDp
    get() = RcDp(this.toFloat())

public val Int.rpx: RcPx
    get() = RcPx(this.toFloat())
public val Float.rpx: RcPx
    get() = RcPx(this)
public val Double.rpx: RcPx
    get() = RcPx(this.toFloat())

@get:JvmName("getRsp")
public val Int.rsp: RcSp
    get() = RcSp(this.toFloat())
@get:JvmName("getRsp")
public val Float.rsp: RcSp
    get() = RcSp(this)
@get:JvmName("getRsp")
public val Double.rsp: RcSp
    get() = RcSp(this.toFloat())
