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
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.RemoteComposeWriter
import kotlin.jvm.JvmInline

/** Type-safe reference for remote text resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @JvmInline public value class RcText(public val id: Int)

/** Type-safe reference for remote image resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcImage(public val id: Int)

/** Type-safe reference for remote color variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColorValue(public val id: Int)

/** Type-safe reference for remote color variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColor(public val id: Int)

/** Type-safe reference for remote color variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextList(public val id: Float)

/** Type-safe reference for remote custom shaders. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcShader(public val id: Int)

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
    public fun format(whole: Int, decimal: Int, flags: Int = 0): RcText {
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.plus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.ADD))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.minus(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.SUB))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.times(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.MUL))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.div(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.DIV))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Float.rem(v: RcFloat): RcFloat {
    return RcFloat(v.writer, floatArrayOf(this, *v.toArray(), Rc.FloatExpression.MOD))
}

/** Returns an [RcFloat] representing the maximum of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RcFloat, b: RcFloat): RcFloat =
    RcFloat(a.writer ?: b.writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MAX))

/** Returns an [RcFloat] representing the maximum of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: Float, b: RcFloat): RcFloat =
    RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MAX))

/** Returns an [RcFloat] representing the maximum of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RcFloat, b: Float): RcFloat =
    RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MAX))

/** Returns an [RcFloat] representing the minimum of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RcFloat, b: RcFloat): RcFloat =
    RcFloat(a.writer ?: b.writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MIN))

/** Returns an [RcFloat] representing the minimum of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: Float, b: RcFloat): RcFloat =
    RcFloat(b.writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MIN))

/** Returns an [RcFloat] representing the minimum of [a] and [b]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RcFloat, b: Float): RcFloat =
    RcFloat(a.writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MIN))

/** Returns an [RcFloat] representing the sign of [v]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun sign(v: RcFloat): RcFloat =
    RcFloat(v.writer, floatArrayOf(*v.toArray(), Rc.FloatExpression.SIGN))

/** Returns an [RcFloat] representing the maximum value in the [array]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayMax(array: RcFloat): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), Rc.FloatExpression.A_MAX))

/** Returns an [RcFloat] representing the minimum value in the [array]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arrayMin(array: RcFloat): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), Rc.FloatExpression.A_MIN))

/** Returns an [RcFloat] interpolated from [array] at [position]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySpline(array: RcFloat, position: RcFloat): RcFloat =
    RcFloat(
        array.writer ?: position.writer,
        floatArrayOf(*array.toArray(), *position.toArray(), Rc.FloatExpression.A_SPLINE),
    )

/** Returns an [RcFloat] interpolated from [array] at [position]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySpline(array: RcFloat, position: Float): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), position, Rc.FloatExpression.A_SPLINE))

/** Returns an [RcFloat] representing the current animation time. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun animationTime(): RcFloat = RcFloat(null, floatArrayOf(Rc.Time.ANIMATION_TIME))

/** Returns an [RcFloat] representing the last touch event time. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun touchTime(): RcFloat = RcFloat(null, floatArrayOf(Rc.Touch.TOUCH_EVENT_TIME))

/** Type-safe reference for remote integer/long variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcInteger(public val id: Long)

/** Type-safe reference for remote text styles. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextStyle(public val id: Int)

/** Type-safe reference for remote bitmap fonts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBitmapFont(public val id: Int)

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

/** Text alignment options for the remote player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcTextAlign(public val value: Int) {
    Start(Rc.Text.ALIGN_START),
    Center(Rc.Text.ALIGN_CENTER),
    End(Rc.Text.ALIGN_END),
    Left(Rc.Text.ALIGN_LEFT),
    Right(Rc.Text.ALIGN_RIGHT),
    Justify(Rc.Text.ALIGN_JUSTIFY),
}

/** Text overflow strategies for the remote player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcTextOverflow(public val value: Int) {
    Clip(Rc.Text.OVERFLOW_CLIP),
    Visible(Rc.Text.OVERFLOW_VISIBLE),
    Ellipsis(Rc.Text.OVERFLOW_ELLIPSIS),
    StartEllipsis(Rc.Text.OVERFLOW_START_ELLIPSIS),
    MiddleEllipsis(Rc.Text.OVERFLOW_MIDDLE_ELLIPSIS),
}

/** Content scaling strategies for images. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcContentScale(public val value: Int) {
    Fit(Rc.ImageScale.FIT),
    Crop(Rc.ImageScale.CROP),
    FillBounds(Rc.ImageScale.FILL_BOUNDS),
    FillWidth(Rc.ImageScale.FILL_WIDTH),
    FillHeight(Rc.ImageScale.FILL_HEIGHT),
    Inside(Rc.ImageScale.INSIDE),
    None(Rc.ImageScale.NONE),
}

/** Stroke cap styles for lines and paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcStrokeCap(public val value: Int) {
    Butt(0),
    Round(1),
    Square(2),
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcPaint.setStrokeCap(v: RcStrokeCap): RcPaint {
    return setStrokeCap(v.value)
}

/** Stroke join styles for paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcStrokeJoin(public val value: Int) {
    Miter(0),
    Round(1),
    Bevel(2),
}

public fun RcPaint.setStrokeJoin(v: RcStrokeJoin): RcPaint {
    return setStrokeJoin(v.value)
}

/** Paint styles for drawing shapes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcPaintStyle(public val value: Int) {
    Fill(0),
    Stroke(1),
    FillAndStroke(2),
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcPaint.setStyle(v: RcPaintStyle): RcPaint {
    return setStyle(v.value)
}

/** Common font weight values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RcFontWeight {
    public const val W100: Float = 100f
    public const val W200: Float = 200f
    public const val W300: Float = 300f
    public const val W400: Float = 400f
    public const val W500: Float = 500f
    public const val W600: Float = 600f
    public const val W700: Float = 700f
    public const val W800: Float = 800f
    public const val W900: Float = 900f

    public const val Thin: Float = W100
    public const val ExtraLight: Float = W200
    public const val Light: Float = W300
    public const val Normal: Float = W400
    public const val Medium: Float = W500
    public const val SemiBold: Float = W600
    public const val Bold: Float = W700
    public const val ExtraBold: Float = W800
    public const val Black: Float = W900
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcTextTransformOp(internal val value: Int) {
    /** Converts the text to uppercase. */
    UPPERCASE(Rc.TextTransform.TEXT_TO_UPPERCASE),
    /** Converts the text to lowercase. */
    LOWERCASE(Rc.TextTransform.TEXT_TO_LOWERCASE),
    /** Capitalizes the first letter of each word. */
    UPPERCASE_FIRST_CAR(Rc.TextTransform.TEXT_UPPERCASE_FIRST_CHAR),
    /** Trims whitespace from the start and end of the text. */
    TRIM(Rc.TextTransform.TEXT_TRIM),
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class DrawOnBitmapMode(internal val value: Int) {
    CLEAR(Rc.DrawOnBitmap.CLEAR_BITMAP),
    NO_CLEAR(Rc.DrawOnBitmap.DO_NOT_CLEAR),
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class BitmapTextMeasure(internal val value: Int) {
    BOTTOM(Rc.BitmapTextMeasure.BOTTOM),
    HEIGHT(Rc.BitmapTextMeasure.HEIGHT),
    TOP(Rc.BitmapTextMeasure.TOP),
    LEFT(Rc.BitmapTextMeasure.LEFT),
    RIGHT(Rc.BitmapTextMeasure.RIGHT),
    WIDTH(Rc.BitmapTextMeasure.WIDTH),
}
