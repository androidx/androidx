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
@file:JvmName("RcTypes")

package androidx.compose.remote.creation.dsl

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.SoundExpression
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec.Companion.of
import androidx.compose.remote.creation.modifiers.CircleShape
import androidx.compose.remote.creation.modifiers.PathShape
import androidx.compose.remote.creation.modifiers.RectShape
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.modifiers.Shape

/** Type-safe reference for remote text resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcText internal constructor(internal val id: Int)

/** Type-safe reference for remote image resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcImage internal constructor(internal val id: Int)

/** Type-safe reference for a remote component. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcComponent internal constructor(internal val id: Int)

/**
 * Type-safe reference for an embedded audio resource registered via [RcScope.remoteSound]. Pass to
 * [RcScope.playSound] to trigger playback.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@JvmInline
public value class RcSound internal constructor(internal val id: Int)

/**
 * Type-safe reference for a sound synthesis expression registered via
 * [RcScope.remoteSoundExpression]. Pass to [RcScope.playSound] to trigger playback.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@JvmInline
public value class RcSoundExpression internal constructor(internal val id: Int)

/** Synthesis type for [RcScope.remoteSoundExpression]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@JvmInline
public value class RcSoundType internal constructor(internal val value: Int) {
    public companion object {
        /** A pure waveform tone defined by frequency (Hz), duration (s), and waveform kind. */
        public val Tone: RcSoundType = RcSoundType(SoundExpression.TYPE_TONE)
    }
}

/** Waveform shape for a [RcSoundType.Tone] sound expression. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@JvmInline
public value class RcWaveform internal constructor(internal val value: Float) {
    public companion object {
        /** Smooth sinusoidal wave (default). */
        public val Sine: RcWaveform = RcWaveform(SoundExpression.WAVEFORM_SINE)
        /** Hard square wave. */
        public val Square: RcWaveform = RcWaveform(SoundExpression.WAVEFORM_SQUARE)
        /** Sawtooth wave. */
        public val Sawtooth: RcWaveform = RcWaveform(SoundExpression.WAVEFORM_SAWTOOTH)
        /** Triangle wave. */
        public val Triangle: RcWaveform = RcWaveform(SoundExpression.WAVEFORM_TRIANGLE)
    }
}

/**
 * Static packed ARGB color literal (`0xAARRGGBB`).
 *
 * Construct from an ARGB `Int` or 32-bit `Long` hex literal (e.g., `RcColorValue(0xFFAA0000)`), or
 * with [rcColor] (`0xFFAA0000.rcColor()`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColorValue(@ColorInt internal val color: Int) {
    /**
     * Creates an [RcColorValue] from a 32-bit packed ARGB `Long` hex literal
     * (`0x00000000`..`0xFFFFFFFF`, e.g., `RcColorValue(0xFFAA0000)`).
     */
    public constructor(color: Long) : this(color.toInt())
}

/**
 * Converts a 32-bit packed ARGB `Long` hex literal (`0x00000000`..`0xFFFFFFFF`, e.g.
 * `0xFFAA0000.rcColor()`) to an [RcColorValue].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Long.rcColor(): RcColorValue = RcColorValue(this)

/** Converts a packed ARGB `Int` (`0xAARRGGBB`) to an [RcColorValue]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun @receiver:ColorInt Int.rcColor(): RcColorValue = RcColorValue(this)

/** Type-safe reference for remote color variables or expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColor internal constructor(internal val id: Int) {
    public companion object {
        /** Sentinel representing an unset or transparent remote color reference. */
        public val None: RcColor = RcColor(0)
    }
}

/** Type-safe reference for remote text-list resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextList internal constructor(internal val id: Float)

/** Type-safe reference for remote custom shaders. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcShader internal constructor(internal val id: Int) {
    public companion object {
        /** Clears any active shader when passed to [RcPaintScope.shader]. */
        public val None: RcShader = RcShader(0)
    }
}

/**
 * The current touch X position, as an [RcFloat] expression. This is in window/event pixels; scale
 * it into your drawing space (e.g. `touchPosX() * componentWidth() / windowWidth()`) when the
 * document is rendered at a different size than the window. Use it as the touch input expression of
 * [RcCanvasScope.addTouch].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun touchPosX(): RcFloat = RcFloat(null, floatArrayOf(Rc.Touch.POSITION_X))

/** The current touch Y position, as an [RcFloat] expression. See [touchPosX]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun touchPosY(): RcFloat = RcFloat(null, floatArrayOf(Rc.Touch.POSITION_Y))

/**
 * Type-safe reference for remote integer or long variables and expressions.
 *
 * Instances obtained from [RcScope.remoteInteger], [RcScope.remoteNamedInteger], or
 * [RcScope.remoteLong] support player-evaluated arithmetic (`+`, `-`, `*`, `/`, `%`, `min`, `max`,
 * bitwise ops, etc.).
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

    override fun equals(other: Any?): Boolean = other is RcInteger && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "RcInteger(id=$id)"
}

/** Convenience: build an integer expression from `Int op RcInteger`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Int.plus(other: RcInteger): RcInteger = other + this

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public operator fun Int.times(other: RcInteger): RcInteger = other * this

/**
 * Type-safe reference for a reusable text style registered via [RcScope.remoteTextStyle] and
 * applied via [RcScope.Text].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextStyle internal constructor(internal val id: Int)

/** Type-safe reference for remote bitmap fonts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBitmapFont internal constructor(internal val id: Int)

/**
 * Type-safe glyph descriptor for assembling a bitmap font via [RcScope.createBitmapFont].
 *
 * Construct instances using [bitmapFontGlyph].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcBitmapFontGlyph internal constructor(internal val glyph: BitmapFontData.Glyph) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcBitmapFontGlyph) return false
        val g = other.glyph
        return glyph.mChars == g.mChars &&
            glyph.mBitmapId == g.mBitmapId &&
            glyph.mMarginLeft == g.mMarginLeft &&
            glyph.mMarginTop == g.mMarginTop &&
            glyph.mMarginRight == g.mMarginRight &&
            glyph.mMarginBottom == g.mMarginBottom &&
            glyph.mBitmapWidth == g.mBitmapWidth &&
            glyph.mBitmapHeight == g.mBitmapHeight
    }

    override fun hashCode(): Int {
        var result = glyph.mChars?.hashCode() ?: 0
        result = 31 * result + glyph.mBitmapId
        result = 31 * result + glyph.mMarginLeft
        result = 31 * result + glyph.mMarginTop
        result = 31 * result + glyph.mMarginRight
        result = 31 * result + glyph.mMarginBottom
        result = 31 * result + glyph.mBitmapWidth
        result = 31 * result + glyph.mBitmapHeight
        return result
    }

    override fun toString(): String =
        "RcBitmapFontGlyph(chars=${glyph.mChars}, bitmapId=${glyph.mBitmapId}, " +
            "width=${glyph.mBitmapWidth}, height=${glyph.mBitmapHeight})"
}

/**
 * Builds an [RcBitmapFontGlyph] for [RcScope.createBitmapFont] whose image is [image].
 *
 * @param chars the character sequence this glyph renders
 * @param image the glyph's bitmap
 * @param width the bitmap width in pixels
 * @param height the bitmap height in pixels
 * @param marginLeft space before the bitmap, in pixels
 * @param marginTop space above the bitmap, in pixels
 * @param marginRight space after the bitmap, in pixels
 * @param marginBottom space below the bitmap, in pixels
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun bitmapFontGlyph(
    chars: String,
    image: RcImage,
    width: Int,
    height: Int,
    marginLeft: Int = 0,
    marginTop: Int = 0,
    marginRight: Int = 0,
    marginBottom: Int = 0,
): RcBitmapFontGlyph =
    RcBitmapFontGlyph(
        BitmapFontData.Glyph(
            chars,
            image.id,
            marginLeft.toShort(),
            marginTop.toShort(),
            marginRight.toShort(),
            marginBottom.toShort(),
            width.toShort(),
            height.toShort(),
        )
    )

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
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Int.rdp: RcDp
    get() = RcDp(this.toFloat())
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Float.rdp: RcDp
    get() = RcDp(this)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Double.rdp: RcDp
    get() = RcDp(this.toFloat())

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Int.rpx: RcPx
    get() = RcPx(this.toFloat())
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Float.rpx: RcPx
    get() = RcPx(this)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Double.rpx: RcPx
    get() = RcPx(this.toFloat())

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@get:JvmName("getRsp")
public val Int.rsp: RcSp
    get() = RcSp(this.toFloat())
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@get:JvmName("getRsp")
public val Float.rsp: RcSp
    get() = RcSp(this)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@get:JvmName("getRsp")
public val Double.rsp: RcSp
    get() = RcSp(this.toFloat())

/** Text alignment options for the remote player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextAlign internal constructor(internal val value: Int) {
    public companion object {
        public val Start: RcTextAlign = RcTextAlign(Rc.Text.ALIGN_START)
        public val Center: RcTextAlign = RcTextAlign(Rc.Text.ALIGN_CENTER)
        public val End: RcTextAlign = RcTextAlign(Rc.Text.ALIGN_END)
        public val Left: RcTextAlign = RcTextAlign(Rc.Text.ALIGN_LEFT)
        public val Right: RcTextAlign = RcTextAlign(Rc.Text.ALIGN_RIGHT)
        public val Justify: RcTextAlign = RcTextAlign(Rc.Text.ALIGN_JUSTIFY)
    }
}

/** Text overflow strategies for the remote player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextOverflow internal constructor(internal val value: Int) {
    public companion object {
        public val Clip: RcTextOverflow = RcTextOverflow(Rc.Text.OVERFLOW_CLIP)
        public val Visible: RcTextOverflow = RcTextOverflow(Rc.Text.OVERFLOW_VISIBLE)
        public val Ellipsis: RcTextOverflow = RcTextOverflow(Rc.Text.OVERFLOW_ELLIPSIS)
        public val StartEllipsis: RcTextOverflow = RcTextOverflow(Rc.Text.OVERFLOW_START_ELLIPSIS)
        public val MiddleEllipsis: RcTextOverflow = RcTextOverflow(Rc.Text.OVERFLOW_MIDDLE_ELLIPSIS)
    }
}

/** Content scaling strategies for images. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcContentScale internal constructor(internal val value: Int) {
    public companion object {
        public val Fit: RcContentScale = RcContentScale(Rc.ImageScale.FIT)
        public val Crop: RcContentScale = RcContentScale(Rc.ImageScale.CROP)
        public val FillBounds: RcContentScale = RcContentScale(Rc.ImageScale.FILL_BOUNDS)
        public val FillWidth: RcContentScale = RcContentScale(Rc.ImageScale.FILL_WIDTH)
        public val FillHeight: RcContentScale = RcContentScale(Rc.ImageScale.FILL_HEIGHT)
        public val Inside: RcContentScale = RcContentScale(Rc.ImageScale.INSIDE)
        public val None: RcContentScale = RcContentScale(Rc.ImageScale.NONE)
    }
}

/** Stroke cap styles for lines and paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcStrokeCap internal constructor(internal val value: Int) {
    public companion object {
        public val Butt: RcStrokeCap = RcStrokeCap(0)
        public val Round: RcStrokeCap = RcStrokeCap(1)
        public val Square: RcStrokeCap = RcStrokeCap(2)
    }
}

/** Stroke join styles for paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcStrokeJoin internal constructor(internal val value: Int) {
    public companion object {
        public val Miter: RcStrokeJoin = RcStrokeJoin(0)
        public val Round: RcStrokeJoin = RcStrokeJoin(1)
        public val Bevel: RcStrokeJoin = RcStrokeJoin(2)
    }
}

/** Paint styles for drawing shapes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcPaintStyle internal constructor(internal val value: Int) {
    public companion object {
        public val Fill: RcPaintStyle = RcPaintStyle(0)
        public val Stroke: RcPaintStyle = RcPaintStyle(1)
        public val FillAndStroke: RcPaintStyle = RcPaintStyle(2)
    }
}

/** Type-safe font-weight value; the underlying [value] is the standard CSS weight (100..900). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcFontWeight(public val value: Float) {
    /** Construct a dynamic font weight from a remote [RcFloat] variable or expression. */
    public constructor(value: RcFloat) : this(value.toFloat())

    public companion object {
        public val W100: RcFontWeight = RcFontWeight(100f)
        public val W200: RcFontWeight = RcFontWeight(200f)
        public val W300: RcFontWeight = RcFontWeight(300f)
        public val W400: RcFontWeight = RcFontWeight(400f)
        public val W500: RcFontWeight = RcFontWeight(500f)
        public val W600: RcFontWeight = RcFontWeight(600f)
        public val W700: RcFontWeight = RcFontWeight(700f)
        public val W800: RcFontWeight = RcFontWeight(800f)
        public val W900: RcFontWeight = RcFontWeight(900f)

        public val Thin: RcFontWeight = W100
        public val ExtraLight: RcFontWeight = W200
        public val Light: RcFontWeight = W300
        public val Normal: RcFontWeight = W400
        public val Medium: RcFontWeight = W500
        public val SemiBold: RcFontWeight = W600
        public val Bold: RcFontWeight = W700
        public val ExtraBold: RcFontWeight = W800
        public val Black: RcFontWeight = W900
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextTransformOp internal constructor(internal val value: Int) {
    public companion object {
        /** Converts the text to uppercase. */
        public val Uppercase: RcTextTransformOp =
            RcTextTransformOp(Rc.TextTransform.TEXT_TO_UPPERCASE)
        /** Converts the text to lowercase. */
        public val Lowercase: RcTextTransformOp =
            RcTextTransformOp(Rc.TextTransform.TEXT_TO_LOWERCASE)
        /** Makes the first character uppercase. */
        public val UppercaseFirstChar: RcTextTransformOp =
            RcTextTransformOp(Rc.TextTransform.TEXT_UPPERCASE_FIRST_CHAR)
        /** Converts the first letter of each word to uppercase. */
        public val Capitalize: RcTextTransformOp =
            RcTextTransformOp(Rc.TextTransform.TEXT_CAPITALIZE)
        /** Trims whitespace from the start and end of the text. */
        public val Trim: RcTextTransformOp = RcTextTransformOp(Rc.TextTransform.TEXT_TRIM)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDrawOnBitmapMode internal constructor(internal val value: Int) {
    public companion object {
        public val Clear: RcDrawOnBitmapMode = RcDrawOnBitmapMode(Rc.DrawOnBitmap.CLEAR_BITMAP)
        public val NoClear: RcDrawOnBitmapMode = RcDrawOnBitmapMode(Rc.DrawOnBitmap.DO_NOT_CLEAR)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBitmapTextMeasure internal constructor(internal val value: Int) {
    public companion object {
        public val Bottom: RcBitmapTextMeasure = RcBitmapTextMeasure(Rc.BitmapTextMeasure.BOTTOM)
        public val Height: RcBitmapTextMeasure = RcBitmapTextMeasure(Rc.BitmapTextMeasure.HEIGHT)
        public val Top: RcBitmapTextMeasure = RcBitmapTextMeasure(Rc.BitmapTextMeasure.TOP)
        public val Left: RcBitmapTextMeasure = RcBitmapTextMeasure(Rc.BitmapTextMeasure.LEFT)
        public val Right: RcBitmapTextMeasure = RcBitmapTextMeasure(Rc.BitmapTextMeasure.RIGHT)
        public val Width: RcBitmapTextMeasure = RcBitmapTextMeasure(Rc.BitmapTextMeasure.WIDTH)
    }
}

/** Font posture (upright vs. italic) for [RcScope.Text] and [RcScope.remoteTextStyle]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcFontStyle internal constructor(internal val value: Int) {
    public companion object {
        /** Upright (roman) glyph posture. */
        public val Normal: RcFontStyle = RcFontStyle(0)
        /** Italic or oblique glyph posture. */
        public val Italic: RcFontStyle = RcFontStyle(1)
    }
}

/** Blend mode selector for [RcPaintScope.blendMode] and [RcPaintScope.colorFilter]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBlendMode internal constructor(internal val value: Int) {
    public companion object {
        public val Clear: RcBlendMode = RcBlendMode(0)
        public val Src: RcBlendMode = RcBlendMode(1)
        public val Dst: RcBlendMode = RcBlendMode(2)
        public val SrcOver: RcBlendMode = RcBlendMode(3)
        public val DstOver: RcBlendMode = RcBlendMode(4)
        public val SrcIn: RcBlendMode = RcBlendMode(5)
        public val DstIn: RcBlendMode = RcBlendMode(6)
        public val SrcOut: RcBlendMode = RcBlendMode(7)
        public val DstOut: RcBlendMode = RcBlendMode(8)
        public val SrcAtop: RcBlendMode = RcBlendMode(9)
        public val DstAtop: RcBlendMode = RcBlendMode(10)
        public val Xor: RcBlendMode = RcBlendMode(11)
        public val Plus: RcBlendMode = RcBlendMode(12)
        public val Modulate: RcBlendMode = RcBlendMode(13)
        public val Screen: RcBlendMode = RcBlendMode(14)
        public val Overlay: RcBlendMode = RcBlendMode(15)
        public val Darken: RcBlendMode = RcBlendMode(16)
        public val Lighten: RcBlendMode = RcBlendMode(17)
        public val ColorDodge: RcBlendMode = RcBlendMode(18)
        public val ColorBurn: RcBlendMode = RcBlendMode(19)
        public val HardLight: RcBlendMode = RcBlendMode(20)
        public val SoftLight: RcBlendMode = RcBlendMode(21)
        public val Difference: RcBlendMode = RcBlendMode(22)
        public val Exclusion: RcBlendMode = RcBlendMode(23)
        public val Multiply: RcBlendMode = RcBlendMode(24)
        public val Hue: RcBlendMode = RcBlendMode(25)
        public val Saturation: RcBlendMode = RcBlendMode(26)
        public val Color: RcBlendMode = RcBlendMode(27)
        public val Luminosity: RcBlendMode = RcBlendMode(28)
        public val Add: RcBlendMode = RcBlendMode(30)
    }
}

/** Tiling mode for shader/texture sampling. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTileMode internal constructor(internal val value: Int) {
    public companion object {
        /** Replicate the edge sample. */
        public val Clamp: RcTileMode = RcTileMode(Rc.Texture.TILE_CLAMP.toInt())
        /** Mirror across each tile boundary. */
        public val Mirror: RcTileMode = RcTileMode(Rc.Texture.TILE_MIRROR.toInt())
        /** Repeat (wrap) the sample range. */
        public val Repeat: RcTileMode = RcTileMode(Rc.Texture.TILE_REPEAT.toInt())
        /** Sample as transparent outside the source bounds. */
        public val Decal: RcTileMode = RcTileMode(Rc.Texture.TILE_DECAL.toInt())
    }
}

/** Generic typeface family for [RcPaintScope.typeface]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcFontType internal constructor(internal val value: Int) {
    public companion object {
        public val Default: RcFontType = RcFontType(0)
        public val SansSerif: RcFontType = RcFontType(1)
        public val Serif: RcFontType = RcFontType(2)
        public val Monospace: RcFontType = RcFontType(3)
    }
}

/**
 * Stop behavior applied when a touch gesture ends in [RcCanvasScope.addTouch].
 *
 * `Notches*` modes consume a corresponding `notches` `FloatArray` describing the notches;
 * `Notches*Even` modes distribute uniform spacings driven by a count.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTouchStopMode internal constructor(internal val value: Int) {
    public companion object {
        /** Decelerate gradually after release. */
        public val Gently: RcTouchStopMode = RcTouchStopMode(Rc.Touch.STOP_GENTLY)
        /** Stop immediately on release. */
        public val Instantly: RcTouchStopMode = RcTouchStopMode(Rc.Touch.STOP_INSTANTLY)
        /** Snap to the nearest range endpoint. */
        public val Ends: RcTouchStopMode = RcTouchStopMode(Rc.Touch.STOP_ENDS)
        /** Snap to evenly-spaced notches across the range. */
        public val NotchesEven: RcTouchStopMode = RcTouchStopMode(Rc.Touch.STOP_NOTCHES_EVEN)
        /** Snap to notches at the given percentages of the range. */
        public val NotchesPercents: RcTouchStopMode =
            RcTouchStopMode(Rc.Touch.STOP_NOTCHES_PERCENTS)
        /** Snap to notches at the given absolute coordinates. */
        public val NotchesAbsolute: RcTouchStopMode =
            RcTouchStopMode(Rc.Touch.STOP_NOTCHES_ABSOLUTE)
        /** Set the value to the absolute touch position. */
        public val AbsolutePosition: RcTouchStopMode = RcTouchStopMode(Rc.Touch.STOP_ABSOLUTE_POS)
        /** Snap to evenly-spaced single-step notches. */
        public val NotchesSingleEven: RcTouchStopMode =
            RcTouchStopMode(Rc.Touch.STOP_NOTCHES_SINGLE_EVEN)
    }
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

/** Comparison operator for [RcScope.ifTrue]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcConditionOp internal constructor(internal val value: Int) {
    public companion object {
        /** Equality (a == b). */
        public val Eq: RcConditionOp = RcConditionOp(Rc.Condition.EQ.toInt())
        /** Inequality (a != b). */
        public val Neq: RcConditionOp = RcConditionOp(Rc.Condition.NEQ.toInt())
        /** Strictly less than (a < b). */
        public val Lt: RcConditionOp = RcConditionOp(Rc.Condition.LT.toInt())
        /** Less than or equal (a <= b). */
        public val Lte: RcConditionOp = RcConditionOp(Rc.Condition.LTE.toInt())
        /** Strictly greater than (a > b). */
        public val Gt: RcConditionOp = RcConditionOp(Rc.Condition.GT.toInt())
        /** Greater than or equal (a >= b). */
        public val Gte: RcConditionOp = RcConditionOp(Rc.Condition.GTE.toInt())
    }
}

/** Represents a resolved logical condition between two float values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcCondition
internal constructor(
    internal val op: RcConditionOp,
    internal val a: RcFloat,
    internal val b: RcFloat,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcCondition) return false
        return op == other.op && a == other.a && b == other.b
    }

    override fun hashCode(): Int {
        var result = op.hashCode()
        result = 31 * result + a.hashCode()
        result = 31 * result + b.hashCode()
        return result
    }

    override fun toString(): String = "RcCondition(op=$op, a=$a, b=$b)"
}

/** Infix equality comparison. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.eq(other: RcFloat): RcCondition =
    RcCondition(RcConditionOp.Eq, this, other)

/** Infix inequality comparison. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.neq(other: RcFloat): RcCondition =
    RcCondition(RcConditionOp.Neq, this, other)

/** Infix less-than comparison. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.lt(other: RcFloat): RcCondition =
    RcCondition(RcConditionOp.Lt, this, other)

/** Infix less-than-or-equal comparison. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.lte(other: RcFloat): RcCondition =
    RcCondition(RcConditionOp.Lte, this, other)

/** Infix greater-than comparison. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.gt(other: RcFloat): RcCondition =
    RcCondition(RcConditionOp.Gt, this, other)

/** Infix greater-than-or-equal comparison. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.gte(other: RcFloat): RcCondition =
    RcCondition(RcConditionOp.Gte, this, other)

/** Infix equality comparison with raw Float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.eq(other: Float): RcCondition = eq(RcFloat(this.writer, other))

/** Infix inequality comparison with raw Float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.neq(other: Float): RcCondition = neq(RcFloat(this.writer, other))

/** Infix less-than comparison with raw Float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.lt(other: Float): RcCondition = lt(RcFloat(this.writer, other))

/** Infix less-than-or-equal comparison with raw Float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.lte(other: Float): RcCondition = lte(RcFloat(this.writer, other))

/** Infix greater-than comparison with raw Float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.gt(other: Float): RcCondition = gt(RcFloat(this.writer, other))

/** Infix greater-than-or-equal comparison with raw Float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public infix fun RcFloat.gte(other: Float): RcCondition = gte(RcFloat(this.writer, other))

/**
 * Skip predicate for [RcScope.skip].
 *
 * The companion `value` is interpreted in the player against either the runtime API level
 * (`IfApi*`) or the document profile bitmask (`IfProfile*`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSkipKind internal constructor(internal val value: Int) {
    public companion object {
        /** Skip the block when the player API level is < value. */
        public val IfApiLessThan: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_LESS_THAN.toInt())
        /** Skip the block when the player API level is > value. */
        public val IfApiGreaterThan: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_GREATER_THAN.toInt())
        /** Skip the block when the player API level == value. */
        public val IfApiEqualTo: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_EQUAL_TO.toInt())
        /** Skip the block when the player API level != value. */
        public val IfApiNotEqualTo: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_NOT_EQUAL_TO.toInt())
        /** Skip the block when the document profile bitmask includes value. */
        public val IfProfileIncludes: RcSkipKind = RcSkipKind(Rc.Skip.IF_PROFILE_INCLUDES.toInt())
        /** Skip the block when the document profile bitmask excludes value. */
        public val IfProfileExcludes: RcSkipKind = RcSkipKind(Rc.Skip.IF_PROFILE_EXCLUDES.toInt())
    }
}

/**
 * Time-attribute selector for [RcScope.timeAttribute].
 *
 * Attributes prefixed `TimeFromArg*` compare `variable` against the `referenceTime` parameter in
 * the 3-argument [RcScope.timeAttribute] overload.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTimeAttr internal constructor(internal val value: Int) {
    public companion object {
        /** (value − currentTimeMillis) × 1e-3 — seconds till `value`. */
        public val TimeFromNowSec: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_NOW_SEC.toInt())
        /** (value − currentTimeMillis) × 1e-3 / 60 — minutes till `value`. */
        public val TimeFromNowMin: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_NOW_MIN.toInt())
        /** (value − currentTimeMillis) × 1e-3 / 3600 — hours till `value`. */
        public val TimeFromNowHr: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_NOW_HR.toInt())
        /** (value − referenceTime) × 1e-3 — seconds elapsed since `referenceTime`. */
        public val TimeFromArgSec: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_ARG_SEC.toInt())
        /** (value − referenceTime) × 1e-3 / 60 — minutes elapsed since `referenceTime`. */
        public val TimeFromArgMin: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_ARG_MIN.toInt())
        /** (value − referenceTime) × 1e-3 / 3600 — hours elapsed since `referenceTime`. */
        public val TimeFromArgHr: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_ARG_HR.toInt())
        /** Second of the minute (0–59). */
        public val TimeInSec: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_IN_SEC.toInt())
        /** Minute of the hour (0–59). */
        public val TimeInMin: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_IN_MIN.toInt())
        /** Hour of the day (0–23). */
        public val TimeInHr: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_IN_HR.toInt())
        /** Day of the month (1–31). */
        public val TimeDayOfMonth: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_DAY_OF_MONTH.toInt())
        /** Month of the year (0–11). */
        public val TimeMonthValue: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_MONTH_VALUE.toInt())
        /** Day of the week (0–6). */
        public val TimeDayOfWeek: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_DAY_OF_WEEK.toInt())
        /** Year. */
        public val TimeYear: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_YEAR.toInt())
        /** (value − doc-load-time) × 1e-3 — seconds elapsed since the document loaded. */
        public val TimeFromLoadSec: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_FROM_LOAD_SEC.toInt())
        /** Day of the year (1–366). */
        public val TimeDayOfYear: RcTimeAttr =
            RcTimeAttr(Rc.TimeAttributes.TIME_DAY_OF_YEAR.toInt())
    }
}

/** Color attribute selector for [RcScope.colorAttribute]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColorAttr internal constructor(internal val value: Int) {
    public companion object {
        /** Hue component (HSV). */
        public val Hue: RcColorAttr = RcColorAttr(Rc.ColorAttribute.HUE.toInt())
        /** Saturation component (HSV). */
        public val Saturation: RcColorAttr = RcColorAttr(Rc.ColorAttribute.SATURATION.toInt())
        /** Brightness / value component (HSV). */
        public val Brightness: RcColorAttr = RcColorAttr(Rc.ColorAttribute.BRIGHTNESS.toInt())
        /** Red channel (sRGB). */
        public val Red: RcColorAttr = RcColorAttr(Rc.ColorAttribute.RED.toInt())
        /** Green channel (sRGB). */
        public val Green: RcColorAttr = RcColorAttr(Rc.ColorAttribute.GREEN.toInt())
        /** Blue channel (sRGB). */
        public val Blue: RcColorAttr = RcColorAttr(Rc.ColorAttribute.BLUE.toInt())
        /** Alpha channel. */
        public val Alpha: RcColorAttr = RcColorAttr(Rc.ColorAttribute.ALPHA.toInt())
    }
}

/**
 * Easing curve for [RcFloat.anim] and [Modifier.animationSpec].
 *
 * [CubicCustom] and [SplineCustom] require a control-point `spec` array passed alongside.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcAnimationCurve internal constructor(internal val value: Int) {
    public companion object {
        /** Standard cubic ease-in-out. */
        public val CubicStandard: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_STANDARD)
        /** Cubic ease that accelerates. */
        public val CubicAccelerate: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_ACCELERATE)
        /** Cubic ease that decelerates. */
        public val CubicDecelerate: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_DECELERATE)
        /** Linear (no easing). */
        public val CubicLinear: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_LINEAR)
        /** Cubic that recoils backward before accelerating forward. */
        public val CubicAnticipate: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_ANTICIPATE)
        /** Cubic that overshoots and settles back. */
        public val CubicOvershoot: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_OVERSHOOT)
        /** Caller-supplied cubic Bézier control points. */
        public val CubicCustom: RcAnimationCurve = RcAnimationCurve(Rc.Animate.CUBIC_CUSTOM)
        /** Caller-supplied monotonic spline control points. */
        public val SplineCustom: RcAnimationCurve = RcAnimationCurve(Rc.Animate.SPLINE_CUSTOM)
        /** Bouncing decay. */
        public val EaseOutBounce: RcAnimationCurve = RcAnimationCurve(Rc.Animate.EASE_OUT_BOUNCE)
        /** Elastic decay. */
        public val EaseOutElastic: RcAnimationCurve = RcAnimationCurve(Rc.Animate.EASE_OUT_ELASTIC)
    }
}

/** Border shape selector for [Modifier.border]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcBorderShape internal constructor(internal val value: Int) {
    public companion object {
        /** Rectangular outline. */
        public val Rectangle: RcBorderShape = RcBorderShape(ShapeType.RECTANGLE)
        /** Circular (pill shaped) outline (uses the smaller of width/height as diameter). */
        public val Circle: RcBorderShape = RcBorderShape(ShapeType.CIRCLE)
        /** Rounded-rectangle outline (uses the `roundedCorner` parameter). */
        public val RoundedRectangle: RcBorderShape = RcBorderShape(ShapeType.ROUNDED_RECTANGLE)
    }
}

/**
 * Haptic feedback kind for [RcScope.performHaptic].
 *
 * The player maps each platform-independent kind to its local haptic engine when supported.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcHaptic internal constructor(internal val value: Int) {
    public companion object {
        /** No haptic feedback should be performed. */
        public val NoHaptics: RcHaptic = RcHaptic(Rc.Haptic.NO_HAPTICS)
        /** A long press on an object. */
        public val LongPress: RcHaptic = RcHaptic(Rc.Haptic.LONG_PRESS)
        /** Pressed an on-screen virtual key. */
        public val VirtualKey: RcHaptic = RcHaptic(Rc.Haptic.VIRTUAL_KEY)
        /** Pressed a soft-keyboard key. */
        public val KeyboardTap: RcHaptic = RcHaptic(Rc.Haptic.KEYBOARD_TAP)
        /** Hour/minute tick of a clock. */
        public val ClockTick: RcHaptic = RcHaptic(Rc.Haptic.CLOCK_TICK)
        /** Context click on an object. */
        public val ContextClick: RcHaptic = RcHaptic(Rc.Haptic.CONTEXT_CLICK)
        /** Pressed a virtual or software keyboard key. */
        public val KeyboardPress: RcHaptic = RcHaptic(Rc.Haptic.KEYBOARD_PRESS)
        /** Released a soft-keyboard key. */
        public val KeyboardRelease: RcHaptic = RcHaptic(Rc.Haptic.KEYBOARD_RELEASE)
        /** Released an on-screen virtual key. */
        public val VirtualKeyRelease: RcHaptic = RcHaptic(Rc.Haptic.VIRTUAL_KEY_RELEASE)
        /** Selection/insertion handle move on a text field. */
        public val TextHandleMove: RcHaptic = RcHaptic(Rc.Haptic.TEXT_HANDLE_MOVE)
        /** Began a multi-touch gesture (e.g. on the soft keyboard). */
        public val GestureStart: RcHaptic = RcHaptic(Rc.Haptic.GESTURE_START)
        /** Ended a multi-touch gesture. */
        public val GestureEnd: RcHaptic = RcHaptic(Rc.Haptic.GESTURE_END)
        /** Confirmation / successful completion of an interaction. */
        public val Confirm: RcHaptic = RcHaptic(Rc.Haptic.CONFIRM)
        /** Rejection / failure of an interaction. */
        public val Reject: RcHaptic = RcHaptic(Rc.Haptic.REJECT)
        /** Toggled a switch into the on position. */
        public val ToggleOn: RcHaptic = RcHaptic(Rc.Haptic.TOGGLE_ON)
        /** Toggled a switch into the off position. */
        public val ToggleOff: RcHaptic = RcHaptic(Rc.Haptic.TOGGLE_OFF)
        /** A swipe/drag gesture passed an activation threshold. */
        public val GestureThresholdActivate: RcHaptic =
            RcHaptic(Rc.Haptic.GESTURE_THRESHOLD_ACTIVATE)
        /** A swipe/drag gesture cancelled by reversing past the threshold. */
        public val GestureThresholdDeactivate: RcHaptic =
            RcHaptic(Rc.Haptic.GESTURE_THRESHOLD_DEACTIVATE)
        /** Drag-and-drop target picked up. */
        public val DragStart: RcHaptic = RcHaptic(Rc.Haptic.DRAG_START)
        /** Switching between potential choices. */
        public val SegmentTick: RcHaptic = RcHaptic(Rc.Haptic.SEGMENT_TICK)
        /** Switching quickly between potential choices. */
        public val SegmentFrequentTick: RcHaptic = RcHaptic(Rc.Haptic.SEGMENT_FREQUENT_TICK)
    }
}

/**
 * Bit-flag set for [RcCanvasScope.drawTextAnchored] `flags`. Independent flags — combine with [or].
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
 * Bit-flag set for [RcCanvasScope.matrixFromPath] `flags`. Independent flags — combine with [or].
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
 * Formatting specification for [RcScope.createTextFromFloat] and [RcFloat.format].
 *
 * Use the [of] factory to combine padding, grouping, separator, and rounding options safely.
 *
 * ```
 * value.format(
 *     whole = 4,
 *     decimal = 2,
 *     spec = RcTextFromFloatSpec.of(
 *         padPre = RcTextFromFloatSpec.PadPre.Zero,
 *         grouping = RcTextFromFloatSpec.Grouping.By3,
 *         options = RcTextFromFloatSpec.Options.NegativeParens,
 *     ),
 * )
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextFromFloatSpec internal constructor(internal val bits: Int) {
    /** Pad style after the decimal point (mutually exclusive). */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmInline
    public value class PadAfter internal constructor(internal val value: Int) {
        public companion object {
            /** Pad past the point with spaces. */
            public val Space: PadAfter = PadAfter(TextFromFloat.PAD_AFTER_SPACE)
            /** Do not pad past the last digit. */
            public val None: PadAfter = PadAfter(TextFromFloat.PAD_AFTER_NONE)
            /** Pad past the last digit with `0`. */
            public val Zero: PadAfter = PadAfter(TextFromFloat.PAD_AFTER_ZERO)
        }
    }

    /** Pad style before the number (mutually exclusive). */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmInline
    public value class PadPre internal constructor(internal val value: Int) {
        public companion object {
            /** Pad before the number with spaces. */
            public val Space: PadPre = PadPre(TextFromFloat.PAD_PRE_SPACE)
            /** Do not pad before the number. */
            public val None: PadPre = PadPre(TextFromFloat.PAD_PRE_NONE)
            /** Pad before the number with `0`s. */
            public val Zero: PadPre = PadPre(TextFromFloat.PAD_PRE_ZERO)
        }
    }

    /** Digit-grouping style (mutually exclusive). */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmInline
    public value class Grouping internal constructor(internal val value: Int) {
        public companion object {
            /** No grouping, e.g. `1234567890.12`. */
            public val None: Grouping = Grouping(TextFromFloat.GROUPING_NONE)
            /** Group every 3 digits, e.g. `1,234,567,890.12`. */
            public val By3: Grouping = Grouping(TextFromFloat.GROUPING_BY3)
            /** Group every 4 digits, e.g. `12,3456,7890.12`. */
            public val By4: Grouping = Grouping(TextFromFloat.GROUPING_BY4)
            /** Group 3 then 2, e.g. `1,23,45,67,890.12`. */
            public val By32: Grouping = Grouping(TextFromFloat.GROUPING_BY32)
        }
    }

    /** Decimal/thousands separator style (mutually exclusive). */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmInline
    public value class Separator internal constructor(internal val value: Int) {
        public companion object {
            /** Comma group, period decimal: `123,456.12`. */
            public val CommaPeriod: Separator = Separator(TextFromFloat.SEPARATOR_COMMA_PERIOD)
            /** Period group, comma decimal: `123.456,12`. */
            public val PeriodComma: Separator = Separator(TextFromFloat.SEPARATOR_PERIOD_COMMA)
            /** Space group, comma decimal: `123 456,12`. */
            public val SpaceComma: Separator = Separator(TextFromFloat.SEPARATOR_SPACE_COMMA)
            /** Underscore group, period decimal: `123_456.12`. */
            public val UnderPeriod: Separator = Separator(TextFromFloat.SEPARATOR_UNDER_PERIOD)
        }
    }

    /** Numeric-presentation options (mutually exclusive). */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmInline
    public value class Options internal constructor(internal val value: Int) {
        public companion object {
            /** Default presentation. */
            public val None: Options = Options(TextFromFloat.OPTIONS_NONE)
            /** Render negative values with parentheses: `(890.12)`. */
            public val NegativeParens: Options = Options(TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES)
            /** Round (rather than clip) the last visible digit. */
            public val Rounding: Options = Options(TextFromFloat.OPTIONS_ROUNDING)
        }
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
 * Type-safe component identifier for [Modifier.componentId]. Wrapping prevents arbitrary ints from
 * being used as component IDs and keeps the IDs traceable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcComponentId internal constructor(internal val id: Int) {
    public companion object {
        /** Creates a component ID from a raw integer identifier. */
        public fun of(id: Int): RcComponentId = RcComponentId(id)
    }
}

/** Type-safe reference for remote macros defined via [RcScope.defineMacro]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public class RcMacro
internal constructor(
    internal val id: Int,
    internal val parameters: List<String> = emptyList(),
    internal val paramIds: IntArray = IntArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcMacro) return false
        return id == other.id &&
            parameters == other.parameters &&
            paramIds.contentEquals(other.paramIds)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + parameters.hashCode()
        result = 31 * result + paramIds.contentHashCode()
        return result
    }

    override fun toString(): String = "RcMacro(id=$id, parameters=$parameters)"
}

/**
 * Builder scope for supplying named arguments and content blocks when inflating an [RcMacro] via
 * [RcScope.inflate].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@RcDslMarker
public sealed interface RcMacroArgsScope {
    /** Binds parameter [name] to the dynamic [RcText] [value]. */
    public fun text(name: String, value: RcText)

    /** Binds parameter [name] to the static string [value]. */
    public fun text(name: String, value: String)

    /** Binds parameter [name] to the dynamic [RcFloat] [value]. */
    public fun float(name: String, value: RcFloat)

    /** Binds parameter [name] to the static float [value]. */
    public fun float(name: String, value: Float)

    /** Binds parameter [name] to the dynamic [RcInteger] [value]. */
    public fun int(name: String, value: RcInteger)

    /** Binds parameter [name] to the static integer [value]. */
    public fun int(name: String, value: Int)

    /** Binds parameter [name] to the dynamic [RcColor] [value]. */
    public fun color(name: String, value: RcColor)

    /** Binds parameter [name] to the static [RcColorValue] [value]. */
    public fun color(name: String, value: RcColorValue)

    /** Binds parameter [name] to a 32-bit packed ARGB `Long` hex literal [value]. */
    public fun color(name: String, value: Long): Unit = color(name, RcColorValue(value))

    /** Binds parameter [name] to the [RcImage] [value]. */
    public fun image(name: String, value: RcImage)

    /** Binds parameter [name] to an inline [RcScope] content block. */
    public fun block(name: String, content: RcScope.() -> Unit)
}

/** Type-safe reference for remote macro argument placeholders or blocks. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
@JvmInline
public value class RcMacroArg internal constructor(internal val paramId: Int) {
    /** Returns this macro argument placeholder as an [RcText] handle. */
    public fun asText(): RcText = RcText(paramId)

    /** Returns this macro argument placeholder as an [RcColor] handle. */
    public fun asColor(): RcColor = RcColor(paramId)

    /** Returns this macro argument placeholder as an [RcImage] handle. */
    public fun asImage(): RcImage = RcImage(paramId)

    /** Returns this macro argument placeholder as an [RcFloat] handle. */
    public fun asFloat(): RcFloat = RcFloat(Utils.asNan(paramId))

    /** Returns this macro argument placeholder as an [RcInteger] handle. */
    public fun asInteger(): RcInteger = RcInteger(0x100000000L + paramId.toLong())
}

/**
 * Type-safe reference for a remote key-value data map registered via [RcScope.remoteDataMap] and
 * queried via [RcScope.lookupText], [RcScope.lookupFloat], [RcScope.lookupInt], or
 * [RcScope.lookupBoolean].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDataMap internal constructor(internal val id: Int)

/** Builder scope for populating typed key-value entries in [RcScope.remoteDataMap]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcDataMapScope {
    /** Inserts a string entry under [key]. */
    public fun string(key: String, value: String)

    /** Inserts a 32-bit integer entry under [key]. */
    public fun int(key: String, value: Int)

    /** Inserts a 64-bit integer entry under [key]. */
    public fun long(key: String, value: Long)

    /** Inserts a floating-point entry under [key]. */
    public fun float(key: String, value: Float)

    /** Inserts a boolean entry under [key]. */
    public fun boolean(key: String, value: Boolean)
}

/** Type-safe reference for remote raw font files. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcFont internal constructor(internal val id: Int)

/**
 * 2D point holding [RcFloat] coordinates. Carrying [RcFloat] (not raw `Float`) means the same point
 * type works for both static positions and animated/expression-driven ones — `RcFloat` already
 * supports a writer-less literal mode.
 *
 * Construct via the [Float.rcAt] / [RcFloat.rcAt] infix builders for the common case.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcPoint internal constructor(public val x: RcFloat, public val y: RcFloat) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcPoint) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()

    override fun toString(): String = "RcPoint(x=$x, y=$y)"

    public companion object {
        /** Origin point `(0, 0)`. */
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
    /** Horizontal span `right - left` as an [RcFloat] expression. */
    public val width: RcFloat
        get() = right - left

    /** Vertical span `bottom - top` as an [RcFloat] expression. */
    public val height: RcFloat
        get() = bottom - top

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcRect) return false
        return left == other.left &&
            top == other.top &&
            right == other.right &&
            bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }

    override fun toString(): String = "RcRect(left=$left, top=$top, right=$right, bottom=$bottom)"

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

/**
 * Typed key-value property binding passed to [RcScope.CustomComponent] to configure or observe a
 * host-rendered custom component.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalRemoteCreationApi
public class RcCustomProperty {
    internal val mType: Short
    internal val mDataType: Short
    internal val mIntValue: Int
    internal val mFloatValue: Float
    internal val writer: RemoteComposeWriter?

    internal constructor(
        type: Short,
        dataType: Short,
        value: Int,
        writer: RemoteComposeWriter? = null,
    ) {
        mType = type
        mDataType = dataType
        mIntValue = value
        mFloatValue = 0f
        this.writer = writer
    }

    internal constructor(
        type: Short,
        dataType: Short,
        value: Float,
        writer: RemoteComposeWriter? = null,
    ) {
        mType = type
        mDataType = dataType
        mFloatValue = value
        mIntValue = 0
        this.writer = writer
    }

    internal constructor(type: Short, dataType: Short, value: RcFloat) {
        mType = type
        mDataType = dataType
        mFloatValue = value.toFloat()
        mIntValue = 0
        this.writer = value.writer
    }

    internal constructor(type: Short, dataType: Short, value: RcInteger) {
        mType = type
        mDataType = dataType
        mIntValue = value.toRawInt()
        mFloatValue = 0f
        this.writer = value.writer
    }

    /** Float handle associated with this property (or return channel). */
    public val floatValue: RcFloat
        get() = RcFloat(writer, mFloatValue)

    /** Text handle associated with this property (or return channel). */
    public val textValue: RcText
        get() = RcText(mIntValue)

    /** Integer handle associated with this property (or return channel). */
    public val intValue: RcInteger
        get() = RcInteger(0x100000000L + mIntValue.toLong(), writer)

    /** Color handle associated with this property (or return channel). */
    public val colorValue: RcColor
        get() = RcColor(mIntValue)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcCustomProperty) return false
        return mType == other.mType &&
            mDataType == other.mDataType &&
            mIntValue == other.mIntValue &&
            mFloatValue.toRawBits() == other.mFloatValue.toRawBits()
    }

    override fun hashCode(): Int {
        var result = mType.toInt()
        result = 31 * result + mDataType.toInt()
        result = 31 * result + mIntValue
        result = 31 * result + mFloatValue.toRawBits()
        return result
    }

    override fun toString(): String =
        "RcCustomProperty(type=$mType, dataType=$mDataType, intValue=$mIntValue, floatValue=$mFloatValue)"

    public companion object {
        internal const val INT_PROP: Short = 0
        internal const val FLOAT_PROP: Short = 1
        internal const val STRING_PROP: Short = 2
        internal const val FLOAT_RETURN: Short = 3
        internal const val TEXT_RETURN: Short = 4
        internal const val INT_RETURN: Short = 5
        internal const val COLOR_RETURN: Short = 6
        internal const val COLOR_ID_PROP: Short = 7
        internal const val COLOR_PROP: Short = 8
        internal const val INT_ID_PROP: Short = 9

        /** Creates a static integer custom property binding for key [type]. */
        public fun int(type: Int, value: Int): RcCustomProperty =
            RcCustomProperty(type.toShort(), INT_PROP, value)

        /** Creates a dynamic [RcInteger] custom property binding for key [type]. */
        public fun int(type: Int, value: RcInteger): RcCustomProperty =
            RcCustomProperty(type.toShort(), INT_ID_PROP, value)

        /** Creates a static float custom property binding for key [type]. */
        public fun float(type: Int, value: Float): RcCustomProperty =
            RcCustomProperty(type.toShort(), FLOAT_PROP, value)

        /** Creates a dynamic [RcFloat] custom property binding for key [type]. */
        public fun float(type: Int, value: RcFloat): RcCustomProperty =
            RcCustomProperty(type.toShort(), FLOAT_PROP, value)

        /** Creates a static [RcColorValue] custom property binding for key [type]. */
        public fun color(type: Int, value: RcColorValue): RcCustomProperty =
            RcCustomProperty(type.toShort(), COLOR_PROP, value.color)

        /** Creates a static color custom property binding from a 32-bit packed ARGB `Long`. */
        public fun color(type: Int, value: Long): RcCustomProperty =
            color(type, RcColorValue(value))

        /** Creates a dynamic [RcColor] custom property binding for key [type]. */
        public fun color(type: Int, value: RcColor): RcCustomProperty =
            RcCustomProperty(type.toShort(), COLOR_ID_PROP, value.id)

        /** Creates an [RcText] custom property binding for key [type]. */
        public fun text(type: Int, value: RcText): RcCustomProperty =
            RcCustomProperty(type.toShort(), STRING_PROP, value.id)
    }
}

/** Outline shape used by [Modifier.clip]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcShape internal constructor(internal val legacyShape: Shape) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcShape) return false
        val a = legacyShape
        val b = other.legacyShape
        return when {
            a is CircleShape && b is CircleShape -> true
            a is RectShape && b is RectShape -> true
            a is RoundedRectShape && b is RoundedRectShape ->
                a.topStart.toRawBits() == b.topStart.toRawBits() &&
                    a.topEnd.toRawBits() == b.topEnd.toRawBits() &&
                    a.bottomStart.toRawBits() == b.bottomStart.toRawBits() &&
                    a.bottomEnd.toRawBits() == b.bottomEnd.toRawBits()
            else -> a == b
        }
    }

    override fun hashCode(): Int =
        when (val s = legacyShape) {
            is CircleShape -> 1
            is RectShape -> 2
            is RoundedRectShape -> {
                var result = 3
                result = 31 * result + s.topStart.toRawBits()
                result = 31 * result + s.topEnd.toRawBits()
                result = 31 * result + s.bottomStart.toRawBits()
                result = 31 * result + s.bottomEnd.toRawBits()
                result
            }
            else -> s.hashCode()
        }

    override fun toString(): String =
        when (val s = legacyShape) {
            is CircleShape -> "RcShape.Circle"
            is RectShape -> "RcShape.Rectangle"
            is RoundedRectShape ->
                "RcShape.RoundedCorner(topStart=${s.topStart}, topEnd=${s.topEnd}, " +
                    "bottomEnd=${s.bottomEnd}, bottomStart=${s.bottomStart})"
            else -> "RcShape($s)"
        }

    public companion object {
        /** Axis-aligned rectangular clip outline. */
        public val Rectangle: RcShape = RcShape(RectShape(0f, 0f, 0f, 0f))

        /** Circular clip outline inscribed in the component bounds. */
        public val Circle: RcShape = RcShape(CircleShape())

        /** Creates a rounded-rectangle clip outline with uniform [radius] on all four corners. */
        public fun roundedCorner(radius: Float): RcShape =
            RcShape(RoundedRectShape(radius, radius, radius, radius))

        /** Creates a rounded-rectangle clip outline with uniform [radius] on all four corners. */
        public fun roundedCorner(radius: RcDp): RcShape =
            RcShape(RoundedRectShape(radius.value, radius.value, radius.value, radius.value))

        /** Creates a rounded-rectangle clip outline with uniform [radius] on all four corners. */
        public fun roundedCorner(radius: RcPx): RcShape =
            RcShape(RoundedRectShape(radius.value, radius.value, radius.value, radius.value))

        /** Creates a rounded-rectangle clip outline with per-corner radii. */
        public fun roundedCorner(
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
        ): RcShape = RcShape(RoundedRectShape(topStart, topEnd, bottomStart, bottomEnd))

        /** Creates a rounded-rectangle clip outline with per-corner [RcDp] radii. */
        public fun roundedCorner(
            topStart: RcDp = 0.rdp,
            topEnd: RcDp = 0.rdp,
            bottomEnd: RcDp = 0.rdp,
            bottomStart: RcDp = 0.rdp,
        ): RcShape =
            RcShape(
                RoundedRectShape(topStart.value, topEnd.value, bottomStart.value, bottomEnd.value)
            )

        /** Creates a rounded-rectangle clip outline with per-corner [RcPx] radii. */
        public fun roundedCorner(
            topStart: RcPx,
            topEnd: RcPx,
            bottomEnd: RcPx,
            bottomStart: RcPx,
        ): RcShape =
            RcShape(
                RoundedRectShape(topStart.value, topEnd.value, bottomStart.value, bottomEnd.value)
            )

        /**
         * Creates a clip outline from [path], a path previously added to the document. The path
         * coordinates are in the clipped component's content box, after any preceding padding.
         * Requires the experimental profile.
         */
        public fun path(path: RcPath): RcShape = RcShape(PathShape(path.id))
    }
}

/** Component enter/exit visibility animation preset for [Modifier.animationSpec]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcEnterExitAnimation internal constructor(internal val value: Int) {
    internal fun toCore(): AnimationSpec.ANIMATION = AnimationSpec.intToAnimation(value)

    public companion object {
        /** Fade in from transparent to opaque. */
        public val FadeIn: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.FADE_IN.ordinal)
        /** Fade out from opaque to transparent. */
        public val FadeOut: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.FADE_OUT.ordinal)
        /** Slide horizontally from/to the left edge. */
        public val SlideLeft: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.SLIDE_LEFT.ordinal)
        /** Slide horizontally from/to the right edge. */
        public val SlideRight: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.SLIDE_RIGHT.ordinal)
        /** Slide vertically from/to the top edge. */
        public val SlideTop: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.SLIDE_TOP.ordinal)
        /** Slide vertically from/to the bottom edge. */
        public val SlideBottom: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.SLIDE_BOTTOM.ordinal)
        /** Rotate during visibility transition. */
        public val Rotate: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.ROTATE.ordinal)
        /** Dissolve using a particle effect. */
        public val Particle: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.PARTICLE.ordinal)
        /**
         * Execute a custom visibility animation defined via [RcScope.defineVisibilityAnimation].
         */
        public val Custom: RcEnterExitAnimation =
            RcEnterExitAnimation(AnimationSpec.ANIMATION.CUSTOM.ordinal)
    }
}

/**
 * Sequencing order of an enter/exit animation relative to layout motion in
 * [Modifier.animationSpec].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcAnimationSequence internal constructor(internal val value: Int) {
    internal fun toCore(): AnimationSpec.SEQUENCE = AnimationSpec.intToSequence(value)

    public companion object {
        /** Run simultaneously with layout motion. */
        public val Concurrent: RcAnimationSequence =
            RcAnimationSequence(AnimationSpec.SEQUENCE.CONCURRENT.ordinal)
        /** Run before layout motion begins. */
        public val Before: RcAnimationSequence =
            RcAnimationSequence(AnimationSpec.SEQUENCE.BEFORE.ordinal)
        /** Run after layout motion completes. */
        public val After: RcAnimationSequence =
            RcAnimationSequence(AnimationSpec.SEQUENCE.AFTER.ordinal)
    }
}

/** Horizontal alignment along the arc for [RcCanvasScope.drawTextOnCircle]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcCircleTextAlignment internal constructor(internal val value: Int) {
    internal fun toCore(): DrawTextOnCircle.Alignment = DrawTextOnCircle.Alignment.fromInt(value)

    public companion object {
        /** Align text start at `startAngle`. */
        public val Start: RcCircleTextAlignment =
            RcCircleTextAlignment(DrawTextOnCircle.Alignment.START.ordinal)
        /** Center text around `startAngle`. */
        public val Center: RcCircleTextAlignment =
            RcCircleTextAlignment(DrawTextOnCircle.Alignment.CENTER.ordinal)
        /** Align text end at `startAngle`. */
        public val End: RcCircleTextAlignment =
            RcCircleTextAlignment(DrawTextOnCircle.Alignment.END.ordinal)
    }
}

/** Radial placement relative to the circle circumference for [RcCanvasScope.drawTextOnCircle]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcCircleTextPlacement internal constructor(internal val value: Int) {
    internal fun toCore(): DrawTextOnCircle.Placement = DrawTextOnCircle.Placement.fromInt(value)

    public companion object {
        /** Place text outside the circle circumference. */
        public val Outside: RcCircleTextPlacement =
            RcCircleTextPlacement(DrawTextOnCircle.Placement.OUTSIDE.ordinal)
        /** Place text inside the circle circumference. */
        public val Inside: RcCircleTextPlacement =
            RcCircleTextPlacement(DrawTextOnCircle.Placement.INSIDE.ordinal)
    }
}

/** Semantic UI role announced by assistive technologies in [RcSemanticsScope.role]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcRole internal constructor(internal val value: Int) {
    public companion object {
        /** Standard clickable button. */
        public val Button: RcRole = RcRole(AccessibleComponent.Role.BUTTON.ordinal)
        /** Two-state or tri-state checkbox. */
        public val Checkbox: RcRole = RcRole(AccessibleComponent.Role.CHECKBOX.ordinal)
        /** Two-state on/off switch. */
        public val Switch: RcRole = RcRole(AccessibleComponent.Role.SWITCH.ordinal)
        /** Mutually exclusive radio button. */
        public val RadioButton: RcRole = RcRole(AccessibleComponent.Role.RADIO_BUTTON.ordinal)
        /** Selectable tab within a tab bar. */
        public val Tab: RcRole = RcRole(AccessibleComponent.Role.TAB.ordinal)
        /** Graphic or illustration. */
        public val Image: RcRole = RcRole(AccessibleComponent.Role.IMAGE.ordinal)
        /** Expandable dropdown list trigger. */
        public val DropdownList: RcRole = RcRole(AccessibleComponent.Role.DROPDOWN_LIST.ordinal)
        /** Value picker control. */
        public val Picker: RcRole = RcRole(AccessibleComponent.Role.PICKER.ordinal)
        /** Scrollable carousel container. */
        public val Carousel: RcRole = RcRole(AccessibleComponent.Role.CAROUSEL.ordinal)
        /** Unspecified or custom role. */
        public val Unknown: RcRole = RcRole(AccessibleComponent.Role.UNKNOWN.ordinal)
    }
}

/** Semantic tree merge behavior for [RcSemanticsScope.mode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSemanticsMode internal constructor(internal val value: Int) {
    public companion object {
        /** Adds or overrides semantic properties on this node without merging descendants. */
        public val Set: RcSemanticsMode = RcSemanticsMode(AccessibleComponent.Mode.SET.ordinal)
        /** Clears descendant semantics and sets only the properties declared on this node. */
        public val ClearAndSet: RcSemanticsMode =
            RcSemanticsMode(AccessibleComponent.Mode.CLEAR_AND_SET.ordinal)
        /** Merges descendant semantic properties into this node. */
        public val Merge: RcSemanticsMode = RcSemanticsMode(AccessibleComponent.Mode.MERGE.ordinal)
    }
}

/** Texture sampling filter mode for [RcPaintScope.textureShader]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextureFilterMode internal constructor(internal val value: Int) {
    public companion object {
        /** Uses the player's default texture sampling filter. */
        public val Default: RcTextureFilterMode =
            RcTextureFilterMode(Rc.Texture.FILTER_DEFAULT.toInt())
        /** Nearest-neighbor point sampling without interpolation. */
        public val Nearest: RcTextureFilterMode =
            RcTextureFilterMode(Rc.Texture.FILTER_NEAREST.toInt())
        /** Bilinear interpolation across adjacent texels. */
        public val Linear: RcTextureFilterMode =
            RcTextureFilterMode(Rc.Texture.FILTER_LINEAR.toInt())
    }
}

/** Behavior flags for [RcScope.particlesComparison]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcParticleComparisonFlags internal constructor(internal val value: Int) {
    public companion object {
        /** Default particle comparison behavior with no modifier flags set. */
        public val None: RcParticleComparisonFlags = RcParticleComparisonFlags(0)
    }
}

/**
 * Measurement dimension and flag selector for [RcScope.textMeasure]. Combine base dimensions with
 * modifier flags such as [MonospaceWidth] or [FontHeight] using [or].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTextMeasureMode internal constructor(internal val value: Int) {
    /** Combines this measurement mode with a modifier flag. */
    public infix fun or(other: RcTextMeasureMode): RcTextMeasureMode =
        RcTextMeasureMode(value or other.value)

    public companion object {
        /** Measures the horizontal width of the text bounds. */
        public val Width: RcTextMeasureMode = RcTextMeasureMode(TextMeasure.MEASURE_WIDTH)
        /** Measures the vertical height of the text bounds. */
        public val Height: RcTextMeasureMode = RcTextMeasureMode(TextMeasure.MEASURE_HEIGHT)
        /** Measures the left bound offset of the text. */
        public val Left: RcTextMeasureMode = RcTextMeasureMode(TextMeasure.MEASURE_LEFT)
        /** Measures the right bound offset of the text. */
        public val Right: RcTextMeasureMode = RcTextMeasureMode(TextMeasure.MEASURE_RIGHT)
        /** Measures the top bound offset of the text relative to the baseline. */
        public val Top: RcTextMeasureMode = RcTextMeasureMode(TextMeasure.MEASURE_TOP)
        /** Measures the bottom bound offset of the text relative to the baseline. */
        public val Bottom: RcTextMeasureMode = RcTextMeasureMode(TextMeasure.MEASURE_BOTTOM)
        /** Modifier flag to measure glyphs using uniform monospace advance width. */
        public val MonospaceWidth: RcTextMeasureMode =
            RcTextMeasureMode(TextMeasure.MEASURE_MONOSPACE_FLAG)
        /**
         * Modifier flag to measure vertical bounds using full font metrics instead of ink bounds.
         */
        public val FontHeight: RcTextMeasureMode =
            RcTextMeasureMode(TextMeasure.MEASURE_MAX_HEIGHT_FLAG)
    }
}

/** Boundary reflection behavior for [RcFloat.spring]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSpringBoundaryMode internal constructor(internal val value: Int) {
    public companion object {
        /** Allows the spring to overshoot target bounds freely without reflection. */
        public val Overshoot: RcSpringBoundaryMode = RcSpringBoundaryMode(0)
        /** Reflects velocity when crossing the lower bound (`0`). */
        public val BounceStart: RcSpringBoundaryMode = RcSpringBoundaryMode(1)
        /** Reflects velocity when crossing the upper bound (`1`). */
        public val BounceEnd: RcSpringBoundaryMode = RcSpringBoundaryMode(2)
        /** Reflects velocity at both the lower (`0`) and upper (`1`) bounds. */
        public val BounceBoth: RcSpringBoundaryMode = RcSpringBoundaryMode(3)
    }
}

/**
 * Keyboard shown by a text input editor; see [RcScope.TextInputLayout]. Mirrors Compose's
 * KeyboardType.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcKeyboardType internal constructor(internal val value: Int) {
    public companion object {
        public val Text: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_TEXT)
        public val Ascii: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_ASCII)
        public val Number: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_NUMBER)
        public val Phone: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_PHONE)
        public val Uri: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_URI)
        public val Email: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_EMAIL)
        public val Password: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_PASSWORD)
        public val NumberPassword: RcKeyboardType =
            RcKeyboardType(TextInputLayout.KEYBOARD_NUMBER_PASSWORD)
        public val Decimal: RcKeyboardType = RcKeyboardType(TextInputLayout.KEYBOARD_DECIMAL)
    }
}

/**
 * Action key of a text input editor; see [RcScope.TextInputLayout]. Mirrors Compose's ImeAction.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcImeAction internal constructor(internal val value: Int) {
    public companion object {
        public val Default: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_DEFAULT)
        public val None: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_NONE)
        public val Go: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_GO)
        public val Search: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_SEARCH)
        public val Send: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_SEND)
        public val Previous: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_PREVIOUS)
        public val Next: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_NEXT)
        public val Done: RcImeAction = RcImeAction(TextInputLayout.IME_ACTION_DONE)
    }
}

/** Characters a text input editor accepts; see [RcScope.TextInputLayout]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcInputFilter internal constructor(internal val value: Int) {
    public companion object {
        public val None: RcInputFilter = RcInputFilter(TextInputLayout.FILTER_NONE)
        public val Digits: RcInputFilter = RcInputFilter(TextInputLayout.FILTER_DIGITS)
        public val Decimal: RcInputFilter = RcInputFilter(TextInputLayout.FILTER_DECIMAL)
        public val Alphanumeric: RcInputFilter = RcInputFilter(TextInputLayout.FILTER_ALPHANUMERIC)
        public val Hexadecimal: RcInputFilter = RcInputFilter(TextInputLayout.FILTER_HEXADECIMAL)
    }
}

/** Automatic capitalization of a text input editor; see [RcScope.TextInputLayout]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcCapitalization internal constructor(internal val flag: Int) {
    public companion object {
        public val None: RcCapitalization = RcCapitalization(0)
        public val Characters: RcCapitalization =
            RcCapitalization(TextInputLayout.FLAG_CAPITALIZE_CHARACTERS)
        public val Words: RcCapitalization = RcCapitalization(TextInputLayout.FLAG_CAPITALIZE_WORDS)
        public val Sentences: RcCapitalization =
            RcCapitalization(TextInputLayout.FLAG_CAPITALIZE_SENTENCES)
    }
}

/**
 * What the string of a text input holds, driving autofill and keyboard suggestions (Android
 * autofill hints, iOS text content types, web autocomplete); see [RcScope.TextInputLayout].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcContentType internal constructor(internal val value: Int) {
    public companion object {
        public val None: RcContentType = RcContentType(TextInputLayout.CONTENT_TYPE_NONE)
        public val PersonName: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_PERSON_NAME)
        public val PersonFirstName: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_PERSON_FIRST_NAME)
        public val PersonLastName: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_PERSON_LAST_NAME)
        public val EmailAddress: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_EMAIL_ADDRESS)
        public val PhoneNumber: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_PHONE_NUMBER)
        public val Username: RcContentType = RcContentType(TextInputLayout.CONTENT_TYPE_USERNAME)
        public val Password: RcContentType = RcContentType(TextInputLayout.CONTENT_TYPE_PASSWORD)
        public val NewPassword: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_NEW_PASSWORD)
        public val OneTimeCode: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_ONE_TIME_CODE)
        public val PostalAddress: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_POSTAL_ADDRESS)
        public val PostalCode: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_POSTAL_CODE)
        public val CreditCardNumber: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_CREDIT_CARD_NUMBER)
        public val CreditCardExpirationDate: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_CREDIT_CARD_EXPIRATION_DATE)
        public val CreditCardSecurityCode: RcContentType =
            RcContentType(TextInputLayout.CONTENT_TYPE_CREDIT_CARD_SECURITY_CODE)
    }
}

/** Trigger condition for horizontal marquee scrolling in [Modifier.marquee]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcMarqueeAnimationMode internal constructor(internal val value: Int) {
    public companion object {
        /** Starts marquee scrolling as soon as content overflows. */
        public val Immediately: RcMarqueeAnimationMode = RcMarqueeAnimationMode(0)
        /** Scrolls marquee content only while the component holds focus. */
        public val WhileFocused: RcMarqueeAnimationMode = RcMarqueeAnimationMode(1)
    }
}

/**
 * Type-safe handle to a custom visibility animation registered via
 * [RcScope.defineVisibilityAnimation] and referenced in [Modifier.animationSpec].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcVisibilityAnimation internal constructor(internal val id: Int) {
    public companion object {
        /** Sentinel indicating no custom visibility animation function is attached. */
        public val None: RcVisibilityAnimation = RcVisibilityAnimation(-1)
    }
}

/** Transform mode applied to each stamped shape in [RcPathEffect.pathDash]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcStampedPathStyle internal constructor(internal val value: Int) {
    public companion object {
        /** Translates the shape to each stamped position without rotating it. */
        public val Translate: RcStampedPathStyle = RcStampedPathStyle(0)
        /** Rotates the shape to align with the path tangent at each stamped position. */
        public val Rotate: RcStampedPathStyle = RcStampedPathStyle(1)
        /** Smoothly bends the shape geometry along the curvature of the path. */
        public val Morph: RcStampedPathStyle = RcStampedPathStyle(2)
    }
}

/**
 * Type-safe stroke/contour path effect installed on a paint via [RcPaintScope.pathEffect].
 *
 * Construct instances with [dash], [discrete], [pathDash], [sum], or [chain].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcPathEffect internal constructor(internal val data: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcPathEffect) return false
        if (data.size != other.data.size) return false
        for (i in data.indices) {
            if (data[i].toRawBits() != other.data[i].toRawBits()) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for (v in data) {
            result = 31 * result + v.toRawBits()
        }
        return result
    }

    override fun toString(): String = "RcPathEffect(size=${data.size})"

    public companion object {
        /** Draws alternating on/off dash segments defined by [intervals] starting at [phase]. */
        public fun dash(intervals: FloatArray, phase: Float = 0f): RcPathEffect =
            RcPathEffect(PaintPathEffects.dash(phase, *intervals))

        /**
         * Breaks contours into segments of [segmentLength] and randomly offsets vertices by up to
         * [deviation].
         */
        public fun discrete(
            @FloatRange(from = 0.0, fromInclusive = false) segmentLength: Float,
            @FloatRange(from = 0.0) deviation: Float,
        ): RcPathEffect = RcPathEffect(PaintPathEffects.discrete(segmentLength, deviation))

        /**
         * Stamps [shape] along stroked contours every [advance] units starting at [phase] using
         * [style].
         */
        public fun pathDash(
            shape: RcPath,
            @FloatRange(from = 0.0, fromInclusive = false) advance: Float,
            phase: Float = 0f,
            style: RcStampedPathStyle = RcStampedPathStyle.Translate,
        ): RcPathEffect =
            RcPathEffect(PaintPathEffects.pathDash(shape.id, advance, phase, style.value))

        /** Draws both [first] and [second] independently over the same path. */
        public fun sum(first: RcPathEffect, second: RcPathEffect): RcPathEffect =
            RcPathEffect(PaintPathEffects.sum(first.data, second.data))

        /** Applies [inner] first, then applies [outer] to the resulting geometry. */
        public fun chain(outer: RcPathEffect, inner: RcPathEffect): RcPathEffect =
            RcPathEffect(PaintPathEffects.compose(outer.data, inner.data))
    }
}
