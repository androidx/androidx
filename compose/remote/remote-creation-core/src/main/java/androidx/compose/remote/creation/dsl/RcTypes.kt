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
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.RemoteComposeWriter
import kotlin.jvm.JvmInline

/** Type-safe reference for remote text resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcText internal constructor(internal val id: Int)

/** Type-safe reference for remote image resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcImage internal constructor(internal val id: Int)

/**
 * Type-safe reference for remote color variables or expressions.
 *
 * **Note:** `id` is `public val` rather than `internal val` because the legacy Java
 * `RcPaint.setColor(RcColorValue)` (one package above `/dsl/`, add-only per the project's API
 * constraint) reads it via `color.getId()`. Tried `@PublishedApi internal` to keep the Java
 * accessor public while restricting Kotlin source visibility, but `@JvmInline value class` accessor
 * projection doesn't generate the expected `getId()` instance method — see
 * https://youtrack.jetbrains.com/issue/KT-54604.
 *
 * Hiding fully would require updating `RcPaint.java` to call a different accessor. Until that
 * boundary opens, this class is the one B4 holdout (along with [RcColor]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColorValue(public val id: Int)

/** convert to RcColorValue */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Long.rcColor(): RcColorValue = RcColorValue(this.toInt())

/** convert to RcColorValue */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Int.rcColor(): RcColorValue = RcColorValue(this)

/**
 * Type-safe reference for remote color variables or expressions.
 *
 * Same B4-holdout situation as [RcColorValue] — kept `public val id` because
 * `RcPaint.setColor(RcColor)` reads it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColor(public val id: Int)

/** Type-safe reference for remote text-list resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextList internal constructor(internal val id: Float)

/** Type-safe reference for remote custom shaders. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcShader internal constructor(internal val id: Int)

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

/**
 * Type-safe reference for remote integer/long variables or expressions.
 *
 * Carries an optional [RemoteComposeWriter] reference so that arithmetic operators (`+`, `-`, `*`,
 * `/`, `%`, `min`, `max`, etc.) can build derived integer expressions via the writer's
 * `integerExpression(...)` API. Constructed integers from `remoteInteger(value)` carry the scope's
 * writer; standalone ones (rare) have a null writer and arithmetic fails fast.
 *
 * Not `@JvmInline` because value classes can only carry a single underlying value; the writer
 * reference forces a regular class allocation. The runtime cost is one heap allocation per
 * RcInteger — typically negligible compared to the wire write.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcInteger
internal constructor(internal val id: Long, internal val writer: RemoteComposeWriter? = null) {

    private fun requireWriter(): RemoteComposeWriter =
        writer
            ?: error(
                "RcInteger arithmetic requires a writer; use remoteInteger(...) inside an RcScope."
            )

    private fun expr(other: RcInteger, op: Long): RcInteger {
        val w = requireWriter()
        return RcInteger(w.integerExpression(id, other.id, op), w)
    }

    private fun expr(other: Int, op: Long): RcInteger {
        val w = requireWriter()
        return RcInteger(w.integerExpression(id, other.toLong(), op), w)
    }

    public operator fun unaryMinus(): RcInteger {
        val w = requireWriter()
        return RcInteger(w.integerExpression(id, Rc.IntegerExpression.L_NEG), w)
    }

    public operator fun plus(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_ADD)

    public operator fun plus(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_ADD)

    public operator fun minus(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_SUB)

    public operator fun minus(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_SUB)

    public operator fun times(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_MUL)

    public operator fun times(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_MUL)

    public operator fun div(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_DIV)

    public operator fun div(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_DIV)

    public operator fun rem(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_MOD)

    public operator fun rem(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_MOD)

    public fun min(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_MIN)

    public fun min(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_MIN)

    public fun max(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_MAX)

    public fun max(other: Int): RcInteger = expr(other, Rc.IntegerExpression.L_MAX)

    public fun abs(): RcInteger {
        val w = requireWriter()
        return RcInteger(w.integerExpression(id, Rc.IntegerExpression.L_ABS), w)
    }

    public fun sign(): RcInteger {
        val w = requireWriter()
        return RcInteger(w.integerExpression(id, Rc.IntegerExpression.L_SIGN), w)
    }

    /** Bitwise AND. */
    public infix fun and(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_AND)

    /** Bitwise OR. */
    public infix fun or(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_OR)

    /** Bitwise XOR. */
    public infix fun xor(other: RcInteger): RcInteger = expr(other, Rc.IntegerExpression.L_XOR)

    /** Arithmetic left shift. */
    public infix fun shl(bits: Int): RcInteger = expr(bits, Rc.IntegerExpression.L_SHL)

    /** Arithmetic right shift (sign-preserving). */
    public infix fun shr(bits: Int): RcInteger = expr(bits, Rc.IntegerExpression.L_SHR)

    /** Logical right shift (zero-fill). */
    public infix fun ushr(bits: Int): RcInteger = expr(bits, Rc.IntegerExpression.L_USHR)
}

/** Convenience: build an integer expression from `Int op RcInteger`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Int.plus(other: RcInteger): RcInteger = other + this

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Int.times(other: RcInteger): RcInteger = other * this

/** Type-safe reference for remote text styles. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextStyle internal constructor(internal val id: Int)

/** Type-safe reference for remote bitmap fonts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBitmapFont internal constructor(internal val id: Int)

/**
 * Internal helper: extract the writer-side raw int from an [RcInteger] handle. Centralises the
 * NaN-decoding so callers (e.g. the action scope) don't reach into the wrapper.
 */
internal fun RcInteger.toRawInt(): Int = (id % 0x100000000L).toInt()

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
    /** Makes the first character uppercase */
    UPPERCASE_FIRST_CAR(Rc.TextTransform.TEXT_UPPERCASE_FIRST_CHAR),
    /** converts to first letter of each word to upper case */
    TEXT_CAPITALIZE(Rc.TextTransform.TEXT_CAPITALIZE),
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class FontStyle(internal val value: Int) {
    Normal(0),
    Italic(1),
}

/**
 * Porter-Duff / SkBlendMode selector for [RcPaintScope.blendMode] and [RcPaintScope.colorFilter].
 * Wraps `PaintBundle.BLEND_MODE_*` constants.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcBlendMode(internal val value: Int) {
    Clear(0),
    Src(1),
    Dst(2),
    SrcOver(3),
    DstOver(4),
    SrcIn(5),
    DstIn(6),
    SrcOut(7),
    DstOut(8),
    SrcAtop(9),
    DstAtop(10),
    Xor(11),
    Plus(12),
    Modulate(13),
    Screen(14),
    Overlay(15),
    Darken(16),
    Lighten(17),
    ColorDodge(18),
    ColorBurn(19),
    HardLight(20),
    SoftLight(21),
    Difference(22),
    Exclusion(23),
    Multiply(24),
    Hue(25),
    Saturation(26),
    Color(27),
    Luminosity(28),
    Null(29),
    Add(30),
}

/** Tiling mode for shader/texture sampling. Wraps `Rc.Texture.TILE_*` constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcTileMode(internal val value: Short) {
    /** Replicate the edge sample. */
    Clamp(Rc.Texture.TILE_CLAMP),
    /** Mirror across each tile boundary. */
    Mirror(Rc.Texture.TILE_MIRROR),
    /** Repeat (wrap) the sample range. */
    Repeat(Rc.Texture.TILE_REPEAT),
    /** Sample as transparent outside the source bounds. */
    Decal(Rc.Texture.TILE_DECAL),
}

/** Typeface family for [RcPaintScope.typeface]. Wraps `RcPaint.FONT_TYPE_*` constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcFontType(internal val value: Int) {
    Default(0),
    SansSerif(1),
    Serif(2),
    Monospace(3),
}

/**
 * Stop-behavior selector for the touch interaction emitted by [RcCanvasScope.addTouch]. Wraps
 * `Rc.Touch.STOP_*` opcodes.
 *
 * `Notches*` modes consume a corresponding `notches` `FloatArray` describing the notches;
 * `Notches*Even` modes are uniform spacings driven by a count.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcTouchStopMode(internal val value: Int) {
    /** Decelerate gradually after release. */
    Gently(Rc.Touch.STOP_GENTLY),
    /** Stop immediately on release. */
    Instantly(Rc.Touch.STOP_INSTANTLY),
    /** Snap to the nearest range endpoint. */
    Ends(Rc.Touch.STOP_ENDS),
    /** Snap to evenly-spaced notches across the range. */
    NotchesEven(Rc.Touch.STOP_NOTCHES_EVEN),
    /** Snap to notches at the given percentages of the range. */
    NotchesPercents(Rc.Touch.STOP_NOTCHES_PERCENTS),
    /** Snap to notches at the given absolute coordinates. */
    NotchesAbsolute(Rc.Touch.STOP_NOTCHES_ABSOLUTE),
    /** Set the value to the absolute touch position. */
    AbsolutePosition(Rc.Touch.STOP_ABSOLUTE_POS),
    /** Snap to evenly-spaced single-step notches. */
    NotchesSingleEven(Rc.Touch.STOP_NOTCHES_SINGLE_EVEN),
}

// =====================================================================================
// Typed opcode enums (B1 from the type-safety proposal).
//
// Defined here so they can replace raw Byte/Short/Int parameters in a follow-up step.
// Not yet wired into the API — RcScope/Modifier signatures are unchanged.
//
// `internal val value` exposes the underlying opcode to RcScopeImpl (same module) so
// the future API replacement can call writer.<op>(typed.value) without conversion or
// reflection. The primitive type matches the Java constant (Byte/Short/Int) so no
// narrowing is required at call sites.
// =====================================================================================

/** Comparison operator for [RcScope.conditionalOperations]. Wraps `Rc.Condition.*` opcodes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcConditionOp(internal val value: Byte) {
    /** Equality (a == b). */
    Eq(Rc.Condition.EQ),
    /** Inequality (a != b). */
    Neq(Rc.Condition.NEQ),
    /** Strictly less than (a < b). */
    Lt(Rc.Condition.LT),
    /** Less than or equal (a <= b). */
    Lte(Rc.Condition.LTE),
    /** Strictly greater than (a > b). */
    Gt(Rc.Condition.GT),
    /** Greater than or equal (a >= b). */
    Gte(Rc.Condition.GTE),
}

/**
 * Skip predicate for [RcScope.skip] / [RcScope.beginSkip]. Wraps `Rc.Skip.*` opcodes.
 *
 * The companion `value` is interpreted in the player against either the runtime API level
 * (`IfApi*`) or the document profile bitmask (`IfProfile*`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcSkipKind(internal val value: Short) {
    /** Skip the block when the player API level is < value. */
    IfApiLessThan(Rc.Skip.IF_API_LESS_THAN),
    /** Skip the block when the player API level is > value. */
    IfApiGreaterThan(Rc.Skip.IF_API_GREATER_THAN),
    /** Skip the block when the player API level == value. */
    IfApiEqualTo(Rc.Skip.IF_API_EQUAL_TO),
    /** Skip the block when the player API level != value. */
    IfApiNotEqualTo(Rc.Skip.IF_API_NOT_EQUAL_TO),
    /** Skip the block when the document profile bitmask includes value. */
    IfProfileIncludes(Rc.Skip.IF_PROFILE_INCLUDES),
    /** Skip the block when the document profile bitmask excludes value. */
    IfProfileExcludes(Rc.Skip.IF_PROFILE_EXCLUDES),
}

/**
 * Time-attribute selector for [RcScope.timeAttribute]. Wraps `Rc.TimeAttributes.*` opcodes.
 *
 * The kinds prefixed `TimeFromArg*` consume one extra `Int` from the variadic `args` — a typed
 * wrapper API can encode that constraint in the future replacement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcTimeAttr(internal val value: Short) {
    /** (value − currentTimeMillis) × 1e-3 — seconds till`value`. */
    TimeFromNowSec(Rc.TimeAttributes.TIME_FROM_NOW_SEC),
    /** (value − currentTimeMillis) × 1e-3 / 60 — minutes till `value`. */
    TimeFromNowMin(Rc.TimeAttributes.TIME_FROM_NOW_MIN),
    /** (value − currentTimeMillis) × 1e-3 / 3600 — hours till `value`. */
    TimeFromNowHr(Rc.TimeAttributes.TIME_FROM_NOW_HR),
    /** (value − arg[0]) × 1e-3 — seconds elapsed since `arg[0]`. */
    TimeFromArgSec(Rc.TimeAttributes.TIME_FROM_ARG_SEC),
    /** (value − arg[0]) × 1e-3 / 60 — minutes elapsed since `arg[0]`. */
    TimeFromArgMin(Rc.TimeAttributes.TIME_FROM_ARG_MIN),
    /** (value − arg[0]) × 1e-3 / 3600 — hours elapsed since `arg[0]`. */
    TimeFromArgHr(Rc.TimeAttributes.TIME_FROM_ARG_HR),
    /** Second of the minute (0–59). */
    TimeInSec(Rc.TimeAttributes.TIME_IN_SEC),
    /** Minute of the hour (0–59). */
    TimeInMin(Rc.TimeAttributes.TIME_IN_MIN),
    /** Hour of the day (0–23). */
    TimeInHr(Rc.TimeAttributes.TIME_IN_HR),
    /** Day of the month (1–31). */
    TimeDayOfMonth(Rc.TimeAttributes.TIME_DAY_OF_MONTH),
    /** Month of the year (0–11). */
    TimeMonthValue(Rc.TimeAttributes.TIME_MONTH_VALUE),
    /** Day of the week (0–6). */
    TimeDayOfWeek(Rc.TimeAttributes.TIME_DAY_OF_WEEK),
    /** Year. */
    TimeYear(Rc.TimeAttributes.TIME_YEAR),
    /** (value − doc-load-time) × 1e-3 — seconds elapsed since the document loaded. */
    TimeFromLoadSec(Rc.TimeAttributes.TIME_FROM_LOAD_SEC),
    /** Day of the year (1–366). */
    TimeDayOfYear(Rc.TimeAttributes.TIME_DAY_OF_YEAR),
}

/** Color attribute selector for [RcScope.getColorAttribute]. Wraps `Rc.ColorAttribute.*`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcColorAttr(internal val value: Short) {
    /** Hue component (HSV). */
    Hue(Rc.ColorAttribute.HUE),
    /** Saturation component (HSV). */
    Saturation(Rc.ColorAttribute.SATURATION),
    /** Brightness / value component (HSV). */
    Brightness(Rc.ColorAttribute.BRIGHTNESS),
    /** Red channel (sRGB). */
    Red(Rc.ColorAttribute.RED),
    /** Green channel (sRGB). */
    Green(Rc.ColorAttribute.GREEN),
    /** Blue channel (sRGB). */
    Blue(Rc.ColorAttribute.BLUE),
    /** Alpha channel. */
    Alpha(Rc.ColorAttribute.ALPHA),
}

/**
 * Easing curve for [RcFloat.anim] and [Modifier.animationSpec]. Wraps `Rc.Animate.*`.
 *
 * `CubicCustom` and `SplineCustom` require a control-point spec passed alongside; a typed wrapper
 * API can require those parameters in the future replacement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcAnimationCurve(internal val value: Int) {
    /** Standard cubic ease-in-out. */
    CubicStandard(Rc.Animate.CUBIC_STANDARD),
    /** Cubic ease that accelerates. */
    CubicAccelerate(Rc.Animate.CUBIC_ACCELERATE),
    /** Cubic ease that decelerates. */
    CubicDecelerate(Rc.Animate.CUBIC_DECELERATE),
    /** Linear (no easing). */
    CubicLinear(Rc.Animate.CUBIC_LINEAR),
    /** Cubic that recoils backward before accelerating forward. */
    CubicAnticipate(Rc.Animate.CUBIC_ANTICIPATE),
    /** Cubic that overshoots and settles back. */
    CubicOvershoot(Rc.Animate.CUBIC_OVERSHOOT),
    /** Caller-supplied cubic Bézier control points. */
    CubicCustom(Rc.Animate.CUBIC_CUSTOM),
    /** Caller-supplied monotonic spline control points. */
    SplineCustom(Rc.Animate.SPLINE_CUSTOM),
    /** Bouncing decay. */
    EaseOutBounce(Rc.Animate.EASE_OUT_BOUNCE),
    /** Elastic decay. */
    EaseOutElastic(Rc.Animate.EASE_OUT_ELASTIC),
}

/**
 * Border / dynamicBorder shape selector for [Modifier.border] and [Modifier.dynamicBorder]. Wraps
 * `ShapeType.*` constants from the layout core.
 *
 * Named `RcBorderShape` to avoid colliding with the existing
 * `androidx.compose.remote.creation.modifiers.Shape` class used by [Modifier.clip].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcBorderShape(internal val value: Int) {
    /** Rectangular outline. */
    Rectangle(ShapeType.RECTANGLE),
    /** Circular (pill shaped) outline (uses the smaller of width/height as diameter). */
    Circle(ShapeType.CIRCLE),
    /** Rounded-rectangle outline (uses the `roundedCorner` parameter). */
    RoundedRectangle(ShapeType.ROUNDED_RECTANGLE),
}

/**
 * Haptic feedback kind for [RcScope.performHaptic]. Wraps `Rc.Haptic.*` constants.
 *
 * Values mirror `android.view.HapticFeedbackConstants` but the wire format is owned by
 * remote-creation-core, so this enum is platform-independent — the player chooses how (or whether)
 * to map each kind to a local haptic engine.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcHaptic(internal val value: Int) {
    /** No haptic feedback should be performed. */
    NoHaptics(Rc.Haptic.NO_HAPTICS),
    /** A long press on an object. */
    LongPress(Rc.Haptic.LONG_PRESS),
    /** Pressed an on-screen virtual key. */
    VirtualKey(Rc.Haptic.VIRTUAL_KEY),
    /** Pressed a soft-keyboard key. */
    KeyboardTap(Rc.Haptic.KEYBOARD_TAP),
    /** Hour/minute tick of a clock. */
    ClockTick(Rc.Haptic.CLOCK_TICK),
    /** Context click on an object. */
    ContextClick(Rc.Haptic.CONTEXT_CLICK),
    /** P a virtual or software keyboard key. */
    KeyboardPress(Rc.Haptic.KEYBOARD_PRESS),
    /** Released a soft-keyboard key. */
    KeyboardRelease(Rc.Haptic.KEYBOARD_RELEASE),
    /** Released an on-screen virtual key. */
    VirtualKeyRelease(Rc.Haptic.VIRTUAL_KEY_RELEASE),
    /** Selection/insertion handle move on a text field. */
    TextHandleMove(Rc.Haptic.TEXT_HANDLE_MOVE),
    /** Began a multi-touch gesture (e.g. on the soft keyboard). */
    GestureStart(Rc.Haptic.GESTURE_START),
    /** Ended a multi-touch gesture. */
    GestureEnd(Rc.Haptic.GESTURE_END),
    /** Confirmation / successful completion of an interaction. */
    Confirm(Rc.Haptic.CONFIRM),
    /** Rejection / failure of an interaction. */
    Reject(Rc.Haptic.REJECT),
    /** Toggled a switch into the on position. */
    ToggleOn(Rc.Haptic.TOGGLE_ON),
    /** Toggled a switch into the off position. */
    ToggleOff(Rc.Haptic.TOGGLE_OFF),
    /** A swipe/drag gesture passed an activation threshold. */
    GestureThresholdActivate(Rc.Haptic.GESTURE_THRESHOLD_ACTIVATE),
    /** A swipe/drag gesture cancelled by reversing past the threshold. */
    GestureThresholdDeactivate(Rc.Haptic.GESTURE_THRESHOLD_DEACTIVATE),
    /** Drag-and-drop target picked up. */
    DragStart(Rc.Haptic.DRAG_START),
    /** Switching between potential choices. */
    SegmentTick(Rc.Haptic.SEGMENT_TICK),
    /** Switching quickly between potential choices. */
    SegmentFrequentTick(Rc.Haptic.SEGMENT_FREQUENT_TICK),
}

/**
 * Bit-flag set for [RcScope.drawTextAnchored] `flags`. Independent flags — combine with [or].
 *
 * ```
 * drawTextAnchored(text, x, y, panX, panY,
 *     flags = RcTextAnchorFlags.MeasureEveryTime or RcTextAnchorFlags.BaselineRelative)
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextAnchorFlags internal constructor(internal val bits: Int) {
    /** Union with another flag set. */
    public infix fun or(other: RcTextAnchorFlags): RcTextAnchorFlags =
        RcTextAnchorFlags(bits or other.bits)

    /** True if every bit in [other] is also set here. */
    public operator fun contains(other: RcTextAnchorFlags): Boolean =
        (bits and other.bits) == other.bits

    public companion object {
        /** No flags set. */
        public val None: RcTextAnchorFlags = RcTextAnchorFlags(0)
        /** Text is right-to-left. */
        public val TextRtl: RcTextAnchorFlags = RcTextAnchorFlags(DrawTextAnchored.ANCHOR_TEXT_RTL)
        /** Use monospace measurement (every glyph the same advance). */
        public val MonospaceMeasure: RcTextAnchorFlags =
            RcTextAnchorFlags(DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE)
        /** Force re-measure each frame instead of caching (off by default — costs CPU). */
        public val MeasureEveryTime: RcTextAnchorFlags =
            RcTextAnchorFlags(DrawTextAnchored.MEASURE_EVERY_TIME)
        /** Anchor on the baseline rather than the cap-height box. */
        public val BaselineRelative: RcTextAnchorFlags =
            RcTextAnchorFlags(DrawTextAnchored.BASELINE_RELATIVE)
    }
}

/**
 * Bit-flag set for [RcScope.matrixFromPath] `flags`. Independent flags — combine with [or].
 *
 * Maps directly to the SkPathMeasure flags used by the player.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcMatrixFromPathFlags internal constructor(internal val bits: Int) {
    /** Union with another flag set. */
    public infix fun or(other: RcMatrixFromPathFlags): RcMatrixFromPathFlags =
        RcMatrixFromPathFlags(bits or other.bits)

    /** True if every bit in [other] is also set here. */
    public operator fun contains(other: RcMatrixFromPathFlags): Boolean =
        (bits and other.bits) == other.bits

    public companion object {
        /** No flags set (no matrix is written). */
        public val None: RcMatrixFromPathFlags = RcMatrixFromPathFlags(0)
        /** Write a translation matrix tracking the path position. */
        public val Position: RcMatrixFromPathFlags =
            RcMatrixFromPathFlags(MatrixFromPath.POSITION_MATRIX_FLAG)
        /** Write a rotation matrix tracking the path tangent. */
        public val Tangent: RcMatrixFromPathFlags =
            RcMatrixFromPathFlags(MatrixFromPath.TANGENT_MATRIX_FLAG)
        /** Common shorthand: position + tangent. */
        public val PositionAndTangent: RcMatrixFromPathFlags =
            RcMatrixFromPathFlags(
                MatrixFromPath.POSITION_MATRIX_FLAG or MatrixFromPath.TANGENT_MATRIX_FLAG
            )
    }
}

/**
 * Bit-packed format spec for [RcScope.createTextFromFloat] / [RcFloat.format].
 *
 * Unlike the OR-able flag sets above, `TextFromFloat` packs five mutually-exclusive sub-fields into
 * one Int, plus two independent boolean flags. Use the [of] factory to build a value safely; raw
 * `or` of arbitrary constants would silently corrupt the sub-field encoding.
 *
 * ```
 * Float.format(whole = 4, decimal = 2,
 *     spec = RcTextFromFloatSpec.of(
 *         padPre = RcTextFromFloatSpec.PadPre.Zero,
 *         grouping = RcTextFromFloatSpec.Grouping.By3,
 *         options = RcTextFromFloatSpec.Options.NegativeParens))
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextFromFloatSpec internal constructor(internal val bits: Int) {
    /** Pad style after the decimal point (mutually exclusive). */
    public enum class PadAfter(internal val value: Int) {
        /** Pad past the point with spaces. */
        Space(TextFromFloat.PAD_AFTER_SPACE),
        /** Do not pad past the last digit. */
        None(TextFromFloat.PAD_AFTER_NONE),
        /** Pad past the last digit with `0`. */
        Zero(TextFromFloat.PAD_AFTER_ZERO),
    }

    /** Pad style before the number (mutually exclusive). */
    public enum class PadPre(internal val value: Int) {
        /** Pad before the number with spaces. */
        Space(TextFromFloat.PAD_PRE_SPACE),
        /** Do not pad before the number. */
        None(TextFromFloat.PAD_PRE_NONE),
        /** Pad before the number with `0`s. */
        Zero(TextFromFloat.PAD_PRE_ZERO),
    }

    /** Digit-grouping style (mutually exclusive). */
    public enum class Grouping(internal val value: Int) {
        /** No grouping, e.g. `1234567890.12`. */
        None(TextFromFloat.GROUPING_NONE),
        /** Group every 3 digits, e.g. `1,234,567,890.12`. */
        By3(TextFromFloat.GROUPING_BY3),
        /** Group every 4 digits, e.g. `12,3456,7890.12`. */
        By4(TextFromFloat.GROUPING_BY4),
        /** Group 3 then 2, e.g. `1,23,45,67,890.12`. */
        By32(TextFromFloat.GROUPING_BY32),
    }

    /** Decimal/thousands separator style (mutually exclusive). */
    public enum class Separator(internal val value: Int) {
        /** Comma group, period decimal: `123,456.12`. */
        CommaPeriod(TextFromFloat.SEPARATOR_COMMA_PERIOD),
        /** Period group, comma decimal: `123.456,12`. */
        PeriodComma(TextFromFloat.SEPARATOR_PERIOD_COMMA),
        /** Space group, comma decimal: `123 456,12`. */
        SpaceComma(TextFromFloat.SEPARATOR_SPACE_COMMA),
        /** Underscore group, period decimal: `123_456.12`. */
        UnderPeriod(TextFromFloat.SEPARATOR_UNDER_PERIOD),
    }

    /** Numeric-presentation options (mutually exclusive). */
    public enum class Options(internal val value: Int) {
        /** Default presentation. */
        None(TextFromFloat.OPTIONS_NONE),
        /** Render negative values with parentheses: `(890.12)`. */
        NegativeParens(TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES),
        /** Round (rather than clip) the last visible digit. */
        Rounding(TextFromFloat.OPTIONS_ROUNDING),
    }

    public companion object {
        /** Default spec: spaces, no grouping, comma-period separator. */
        public val Default: RcTextFromFloatSpec = RcTextFromFloatSpec(0)

        /**
         * Builder. Each parameter is mutually exclusive within its sub-field; cross-field
         * combinations are always safe.
         *
         * @param legacy When true, ignore [grouping] / [separator] and use legacy mode.
         * @param fullFidelity When true, ignore all formatting and emit the raw float.
         */
        public fun of(
            padAfter: PadAfter = PadAfter.Space,
            padPre: PadPre = PadPre.Space,
            grouping: Grouping = Grouping.None,
            separator: Separator = Separator.CommaPeriod,
            options: Options = Options.None,
            legacy: Boolean = false,
            fullFidelity: Boolean = false,
        ): RcTextFromFloatSpec {
            var bits =
                padAfter.value or padPre.value or grouping.value or separator.value or options.value
            if (legacy) bits = bits or TextFromFloat.LEGACY_MODE
            if (fullFidelity) bits = bits or TextFromFloat.FULL_FORMAT
            return RcTextFromFloatSpec(bits)
        }
    }
}

// =====================================================================================
// Type-safe value classes (B5 / B10 from the type-safety proposal).
//
// All marked `internal constructor` so callers cannot wrap arbitrary numbers; they go
// through named factory methods or extension properties. Backing primitives are
// internal so the future API replacement can read them without exposing them to apps.
// =====================================================================================

/**
 * Type-safe boolean reference for the player. Distinct from [RcInteger] so that `boolean → numeric`
 * arithmetic doesn't compile silently.
 *
 * Wraps the same NaN-encoded handle as [RcInteger] but carries boolean intent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBool internal constructor(internal val handle: Long)

/** Internal helper to extract the writer-side raw int from a [RcBool] handle. */
internal fun RcBool.toRawInt(): Int = (handle % 0x100000000L).toInt()

/**
 * Opaque token returned by [RcScope.beginSkip] and consumed by [RcScope.endSkip]. Replaces the raw
 * `Int` offset that previously had to be passed by the caller, preventing accidentally swapping
 * skip-block boundaries.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSkipToken internal constructor(internal val offset: Int)

/**
 * Type-safe component identifier for [Modifier.componentId]. Wrapping prevents arbitrary ints from
 * being used as component IDs and keeps the IDs traceable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcComponentId(public val id: Int)

/**
 * Type-safe font-weight value (added alongside the existing [RcFontWeight] object of `Float`
 * constants — that object is preserved for backward compatibility).
 *
 * Use [RcWeight.Normal], [RcWeight.Bold], etc. to construct a weight; the underlying [value] is the
 * standard CSS weight (100..900).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcWeight internal constructor(internal val value: Float) {
    public companion object {
        public val Thin: RcWeight = RcWeight(100f)
        public val ExtraLight: RcWeight = RcWeight(200f)
        public val Light: RcWeight = RcWeight(300f)
        public val Normal: RcWeight = RcWeight(400f)
        public val Medium: RcWeight = RcWeight(500f)
        public val SemiBold: RcWeight = RcWeight(600f)
        public val Bold: RcWeight = RcWeight(700f)
        public val ExtraBold: RcWeight = RcWeight(800f)
        public val Black: RcWeight = RcWeight(900f)

        /** Construct a weight from the raw CSS weight (100..900). */
        public fun of(value: Float): RcWeight = RcWeight(value)
    }
}

/**
 * 2D point holding [RcFloat] coordinates. Carrying [RcFloat] (not raw `Float`) means the same point
 * type works for both static positions and animated/expression-driven ones — `RcFloat` already
 * supports a writer-less literal mode.
 *
 * Construct via the [Float.rcAt] / [RcFloat.rcAt] infix builders for the common case.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcPoint internal constructor(public val x: RcFloat, public val y: RcFloat) {
    public companion object {
        public val Zero: RcPoint = RcPoint(RcFloat(0f), RcFloat(0f))
    }
}

/** Build a point from two static `Float` coordinates. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun Float.rcAt(y: Float): RcPoint = RcPoint(RcFloat(this), RcFloat(y))

/** Build a point from two animated [RcFloat] coordinates. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.rcAt(y: RcFloat): RcPoint = RcPoint(this, y)

/**
 * Axis-aligned rectangle with `left`, `top`, `right`, `bottom` as [RcFloat] values. Same reasoning
 * as [RcPoint] — RcFloat-typed coordinates handle both static and animated cases through the
 * existing writer-less RcFloat literal path.
 *
 * Use the named factory methods on the companion to avoid argument-order bugs in the four-float
 * drawing APIs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcRect
internal constructor(
    public val left: RcFloat,
    public val top: RcFloat,
    public val right: RcFloat,
    public val bottom: RcFloat,
) {
    public val width: RcFloat
        get() = right - left

    public val height: RcFloat
        get() = bottom - top

    public companion object {
        /** Rectangle by static left/top/right/bottom. */
        public fun ltrb(left: Float, top: Float, right: Float, bottom: Float): RcRect =
            RcRect(RcFloat(left), RcFloat(top), RcFloat(right), RcFloat(bottom))

        /** Rectangle by animated left/top/right/bottom. */
        public fun ltrb(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat): RcRect =
            RcRect(left, top, right, bottom)

        /** Rectangle by static top-left corner and size. */
        public fun xywh(x: Float, y: Float, width: Float, height: Float): RcRect =
            ltrb(x, y, x + width, y + height)

        /** Rectangle by animated top-left corner and size. */
        public fun xywh(x: RcFloat, y: RcFloat, width: RcFloat, height: RcFloat): RcRect =
            ltrb(x, y, x + width, y + height)

        /** Rectangle centered at [center] with the given static size. */
        public fun centered(center: RcPoint, width: Float, height: Float): RcRect {
            val halfW = width / 2f
            val halfH = height / 2f
            return RcRect(center.x - halfW, center.y - halfH, center.x + halfW, center.y + halfH)
        }

        /** Rectangle centered at [center] with the given animated size. */
        public fun centered(center: RcPoint, width: RcFloat, height: RcFloat): RcRect {
            val halfW = width / 2f
            val halfH = height / 2f
            return RcRect(center.x - halfW, center.y - halfH, center.x + halfW, center.y + halfH)
        }
    }
}
