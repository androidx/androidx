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
@file:JvmName("RcTypesKt")

package androidx.compose.remote.creation.dsl

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.SoundExpression
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec.Companion.of
import androidx.compose.remote.creation.modifiers.CircleShape
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

/** Type-safe reference for a raw inline PCM sound resource registered via [RcScope.addSound]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSound internal constructor(internal val id: Int)

/**
 * Type-safe reference for a sound synthesis expression registered via [RcScope.soundExpression].
 * Pass to [RcScope.playSound] to trigger playback.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSoundExpression internal constructor(internal val id: Int)

/**
 * Synthesis type for a [RcScope.soundExpression].
 *
 * Values mirror the NaN-encoded type constants in `SoundExpression.java` but the wire format is
 * owned by remote-core, so this value class is platform-independent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSoundType internal constructor(internal val value: Int) {
    public companion object {
        /** A pure waveform tone defined by frequency (Hz), duration (s), and waveform kind. */
        public val Tone: RcSoundType = RcSoundType(SoundExpression.TYPE_TONE)
    }
}

/** Waveform shape for a [RcSoundType.Tone] sound expression. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

/** Type-safe static packed ARGB color literal (`0xAARRGGBB`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColorValue(@ColorInt internal val id: Int)

/** Converts this packed ARGB `Long` literal (`0xAARRGGBB`) to an [RcColorValue]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Long.rcColor(): RcColorValue = RcColorValue(this.toInt())

/** Converts this packed ARGB `Int` literal (`0xAARRGGBB`) to an [RcColorValue]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Int.rcColor(): RcColorValue = RcColorValue(this)

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

/** Returns an [RcFloat] interpolated from [array] at [position]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun arraySpline(array: RcFloat, position: Float): RcFloat =
    RcFloat(array.writer, floatArrayOf(*array.toArray(), position, Rc.FloatExpression.A_SPLINE))

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
 * Type-safe glyph descriptor for assembling a bitmap font via [RcScope.createBitmapFont].
 *
 * Construct instances using [bitmapFontGlyph].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcBitmapFontGlyph internal constructor(internal val glyph: BitmapFontData.Glyph)

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

        /** Construct a weight from the raw CSS weight (100..900). */
        public fun of(value: Float): RcFontWeight = RcFontWeight(value)

        /** Construct a dynamic weight from a remote [RcFloat] variable or expression. */
        public fun of(value: RcFloat): RcFontWeight = RcFontWeight(value.toFloat())
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcFontStyle internal constructor(internal val value: Int) {
    public companion object {
        public val Normal: RcFontStyle = RcFontStyle(0)
        public val Italic: RcFontStyle = RcFontStyle(1)
    }
}

/**
 * Porter-Duff / SkBlendMode selector for [RcPaintScope.blendMode] and [RcPaintScope.colorFilter].
 * Wraps `PaintBundle.BLEND_MODE_*` constants.
 */
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

/** Tiling mode for shader/texture sampling. Wraps `Rc.Texture.TILE_*` constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTileMode internal constructor(internal val value: Short) {
    public companion object {
        /** Replicate the edge sample. */
        public val Clamp: RcTileMode = RcTileMode(Rc.Texture.TILE_CLAMP)
        /** Mirror across each tile boundary. */
        public val Mirror: RcTileMode = RcTileMode(Rc.Texture.TILE_MIRROR)
        /** Repeat (wrap) the sample range. */
        public val Repeat: RcTileMode = RcTileMode(Rc.Texture.TILE_REPEAT)
        /** Sample as transparent outside the source bounds. */
        public val Decal: RcTileMode = RcTileMode(Rc.Texture.TILE_DECAL)
    }
}

/** Typeface family for [RcPaintScope.typeface]. Wraps `RcPaint.FONT_TYPE_*` constants. */
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
 * Stop-behavior selector for the touch interaction emitted by [RcCanvasScope.addTouch]. Wraps
 * `Rc.Touch.STOP_*` opcodes.
 *
 * `Notches*` modes consume a corresponding `notches` `FloatArray` describing the notches;
 * `Notches*Even` modes are uniform spacings driven by a count.
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

/** Comparison operator for [RcScope.conditionalOperations]. Wraps `Rc.Condition.*` opcodes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcConditionOp internal constructor(internal val value: Byte) {
    public companion object {
        /** Equality (a == b). */
        public val Eq: RcConditionOp = RcConditionOp(Rc.Condition.EQ)
        /** Inequality (a != b). */
        public val Neq: RcConditionOp = RcConditionOp(Rc.Condition.NEQ)
        /** Strictly less than (a < b). */
        public val Lt: RcConditionOp = RcConditionOp(Rc.Condition.LT)
        /** Less than or equal (a <= b). */
        public val Lte: RcConditionOp = RcConditionOp(Rc.Condition.LTE)
        /** Strictly greater than (a > b). */
        public val Gt: RcConditionOp = RcConditionOp(Rc.Condition.GT)
        /** Greater than or equal (a >= b). */
        public val Gte: RcConditionOp = RcConditionOp(Rc.Condition.GTE)
    }
}

/** Represents a resolved logical condition between two float values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcCondition
internal constructor(
    internal val op: RcConditionOp,
    internal val a: RcFloat,
    internal val b: RcFloat,
)

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
 * Skip predicate for [RcScope.skip]. Wraps `Rc.Skip.*` opcodes.
 *
 * The companion `value` is interpreted in the player against either the runtime API level
 * (`IfApi*`) or the document profile bitmask (`IfProfile*`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcSkipKind internal constructor(internal val value: Short) {
    public companion object {
        /** Skip the block when the player API level is < value. */
        public val IfApiLessThan: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_LESS_THAN)
        /** Skip the block when the player API level is > value. */
        public val IfApiGreaterThan: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_GREATER_THAN)
        /** Skip the block when the player API level == value. */
        public val IfApiEqualTo: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_EQUAL_TO)
        /** Skip the block when the player API level != value. */
        public val IfApiNotEqualTo: RcSkipKind = RcSkipKind(Rc.Skip.IF_API_NOT_EQUAL_TO)
        /** Skip the block when the document profile bitmask includes value. */
        public val IfProfileIncludes: RcSkipKind = RcSkipKind(Rc.Skip.IF_PROFILE_INCLUDES)
        /** Skip the block when the document profile bitmask excludes value. */
        public val IfProfileExcludes: RcSkipKind = RcSkipKind(Rc.Skip.IF_PROFILE_EXCLUDES)
    }
}

/**
 * Time-attribute selector for [RcScope.timeAttribute]. Wraps `Rc.TimeAttributes.*` opcodes.
 *
 * The kinds prefixed `TimeFromArg*` consume one extra `Int` from the variadic `args` — a typed
 * wrapper API can encode that constraint in the future replacement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcTimeAttr internal constructor(internal val value: Short) {
    public companion object {
        /** (value − currentTimeMillis) × 1e-3 — seconds till `value`. */
        public val TimeFromNowSec: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_NOW_SEC)
        /** (value − currentTimeMillis) × 1e-3 / 60 — minutes till `value`. */
        public val TimeFromNowMin: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_NOW_MIN)
        /** (value − currentTimeMillis) × 1e-3 / 3600 — hours till `value`. */
        public val TimeFromNowHr: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_NOW_HR)
        /** (value − arg[0]) × 1e-3 — seconds elapsed since `arg[0]`. */
        public val TimeFromArgSec: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_ARG_SEC)
        /** (value − arg[0]) × 1e-3 / 60 — minutes elapsed since `arg[0]`. */
        public val TimeFromArgMin: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_ARG_MIN)
        /** (value − arg[0]) × 1e-3 / 3600 — hours elapsed since `arg[0]`. */
        public val TimeFromArgHr: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_ARG_HR)
        /** Second of the minute (0–59). */
        public val TimeInSec: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_IN_SEC)
        /** Minute of the hour (0–59). */
        public val TimeInMin: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_IN_MIN)
        /** Hour of the day (0–23). */
        public val TimeInHr: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_IN_HR)
        /** Day of the month (1–31). */
        public val TimeDayOfMonth: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_DAY_OF_MONTH)
        /** Month of the year (0–11). */
        public val TimeMonthValue: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_MONTH_VALUE)
        /** Day of the week (0–6). */
        public val TimeDayOfWeek: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_DAY_OF_WEEK)
        /** Year. */
        public val TimeYear: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_YEAR)
        /** (value − doc-load-time) × 1e-3 — seconds elapsed since the document loaded. */
        public val TimeFromLoadSec: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_FROM_LOAD_SEC)
        /** Day of the year (1–366). */
        public val TimeDayOfYear: RcTimeAttr = RcTimeAttr(Rc.TimeAttributes.TIME_DAY_OF_YEAR)
    }
}

/** Color attribute selector for [RcScope.getColorAttribute]. Wraps `Rc.ColorAttribute.*`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcColorAttr internal constructor(internal val value: Short) {
    public companion object {
        /** Hue component (HSV). */
        public val Hue: RcColorAttr = RcColorAttr(Rc.ColorAttribute.HUE)
        /** Saturation component (HSV). */
        public val Saturation: RcColorAttr = RcColorAttr(Rc.ColorAttribute.SATURATION)
        /** Brightness / value component (HSV). */
        public val Brightness: RcColorAttr = RcColorAttr(Rc.ColorAttribute.BRIGHTNESS)
        /** Red channel (sRGB). */
        public val Red: RcColorAttr = RcColorAttr(Rc.ColorAttribute.RED)
        /** Green channel (sRGB). */
        public val Green: RcColorAttr = RcColorAttr(Rc.ColorAttribute.GREEN)
        /** Blue channel (sRGB). */
        public val Blue: RcColorAttr = RcColorAttr(Rc.ColorAttribute.BLUE)
        /** Alpha channel. */
        public val Alpha: RcColorAttr = RcColorAttr(Rc.ColorAttribute.ALPHA)
    }
}

/**
 * Easing curve for [RcFloat.anim] and [Modifier.animationSpec]. Wraps `Rc.Animate.*`.
 *
 * `CubicCustom` and `SplineCustom` require a control-point spec passed alongside; a typed wrapper
 * API can require those parameters in the future replacement.
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

/**
 * Border / dynamicBorder shape selector for [Modifier.border] and [Modifier.dynamicBorder]. Wraps
 * `ShapeType.*` constants from the layout core.
 */
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
 * Haptic feedback kind for [RcScope.performHaptic]. Wraps `Rc.Haptic.*` constants.
 *
 * Values mirror `android.view.HapticFeedbackConstants` but the wire format is owned by
 * remote-creation-core, so this value class is platform-independent — the player chooses how (or
 * whether) to map each kind to a local haptic engine.
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
 * Bit-packed format spec for [RcScope.createTextFromFloat] / [RcFloat.format].
 *
 * Unlike the OR-able flag sets above, `TextFromFloat` packs five mutually-exclusive sub-fields into
 * one Int, plus two independent boolean flags. Use the [of] factory to build a value safely; raw
 * `or` of arbitrary constants would silently corrupt the sub-field encoding.
 *
 * ```
 * value.format(whole = 4, decimal = 2,
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

/** Type-safe reference for remote animation specs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcAnimationSpec internal constructor(internal val id: Int)

/** Type-safe reference for remote macros. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcMacro internal constructor(internal val id: Int)

/** Type-safe reference for remote macro argument placeholders or blocks. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcMacroArg internal constructor(internal val paramId: Int) {
    /** Returns this macro argument placeholder as an [RcText] handle. */
    public fun asText(): RcText = RcText(paramId)

    /** Returns this macro argument placeholder as an [RcColor] handle. */
    public fun asColor(): RcColor = RcColor(paramId)

    /** Returns this macro argument placeholder as an [RcImage] handle. */
    public fun asImage(): RcImage = RcImage(paramId)
}

/** Type-safe reference for remote key-value data maps. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDataMap internal constructor(internal val id: Int)

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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("NoByteOrShort")
public class RcCustomProperty {
    internal val mType: Short
    internal val mDataType: Short
    internal val mIntValue: Int
    internal val mFloatValue: Float

    internal constructor(type: Short, dataType: Short, value: Int) {
        mType = type
        mDataType = dataType
        mIntValue = value
        mFloatValue = 0f
    }

    internal constructor(type: Short, dataType: Short, value: Float) {
        mType = type
        mDataType = dataType
        mFloatValue = value
        mIntValue = 0
    }

    internal constructor(type: Short, dataType: Short, value: RcFloat) {
        mType = type
        mDataType = dataType
        mFloatValue = value.toFloat()
        mIntValue = 0
    }

    internal constructor(type: Short, dataType: Short, value: RcInteger) {
        mType = type
        mDataType = dataType
        mIntValue = value.toRawInt()
        mFloatValue = 0f
    }

    public fun getFloatValue(): RcFloat {
        return RcFloat(mFloatValue)
    }

    public fun getStringValue(): RcText {
        return RcText(mIntValue)
    }

    public fun getIntValue(): RcInteger {
        return RcInteger(mIntValue.toLong())
    }

    public fun getColorValue(): RcColor {
        return RcColor(mIntValue)
    }

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

        public fun int(type: Short, value: Int): RcCustomProperty =
            RcCustomProperty(type, INT_PROP, value)

        public fun int(type: Int, value: Int): RcCustomProperty =
            RcCustomProperty(type.toShort(), INT_PROP, value)

        public fun int(type: Short, value: RcInteger): RcCustomProperty =
            RcCustomProperty(type, INT_ID_PROP, value.toRawInt())

        public fun int(type: Int, value: RcInteger): RcCustomProperty =
            RcCustomProperty(type.toShort(), INT_ID_PROP, value.toRawInt())

        public fun float(type: Short, value: Float): RcCustomProperty =
            RcCustomProperty(type, FLOAT_PROP, value)

        public fun float(type: Int, value: Float): RcCustomProperty =
            RcCustomProperty(type.toShort(), FLOAT_PROP, value)

        public fun float(type: Short, value: RcFloat): RcCustomProperty =
            RcCustomProperty(type, FLOAT_PROP, value)

        public fun float(type: Int, value: RcFloat): RcCustomProperty =
            RcCustomProperty(type.toShort(), FLOAT_PROP, value)

        public fun color(type: Short, value: RcColorValue): RcCustomProperty =
            RcCustomProperty(type, COLOR_PROP, value.id)

        public fun color(type: Int, value: RcColorValue): RcCustomProperty =
            RcCustomProperty(type.toShort(), COLOR_PROP, value.id)

        public fun color(type: Short, value: RcColor): RcCustomProperty =
            RcCustomProperty(type, COLOR_ID_PROP, value.id)

        public fun color(type: Int, value: RcColor): RcCustomProperty =
            RcCustomProperty(type.toShort(), COLOR_ID_PROP, value.id)

        public fun text(type: Short, value: RcText): RcCustomProperty =
            RcCustomProperty(type, STRING_PROP, value.id)

        public fun text(type: Int, value: RcText): RcCustomProperty =
            RcCustomProperty(type.toShort(), STRING_PROP, value.id)

        public fun returnFloat(type: Short, scope: RcScope): RcCustomProperty {
            val id = (scope as RcScopeImpl).writer.createFloatId()
            return RcCustomProperty(type, FLOAT_RETURN, id)
        }

        public fun returnFloat(type: Int, scope: RcScope): RcCustomProperty =
            returnFloat(type.toShort(), scope)

        public fun returnText(type: Short, scope: RcScope): RcCustomProperty {
            val id = (scope as RcScopeImpl).writer.nextId()
            return RcCustomProperty(type, TEXT_RETURN, id)
        }

        public fun returnText(type: Int, scope: RcScope): RcCustomProperty =
            returnText(type.toShort(), scope)

        public fun returnInt(type: Short, scope: RcScope): RcCustomProperty {
            val id = (scope as RcScopeImpl).writer.nextId()
            return RcCustomProperty(type, INT_RETURN, id)
        }

        public fun returnInt(type: Int, scope: RcScope): RcCustomProperty =
            returnInt(type.toShort(), scope)

        public fun returnColor(type: Short, scope: RcScope): RcCustomProperty {
            val id = (scope as RcScopeImpl).writer.nextId()
            return RcCustomProperty(type, COLOR_RETURN, id)
        }

        public fun returnColor(type: Int, scope: RcScope): RcCustomProperty =
            returnColor(type.toShort(), scope)
    }
}

/** Outline shape used by [Modifier.clip]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcShape internal constructor(internal val legacyShape: Shape) {
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
            topStart: Float = 0f,
            topEnd: Float = 0f,
            bottomEnd: Float = 0f,
            bottomStart: Float = 0f,
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
            topStart: RcPx = 0.rpx,
            topEnd: RcPx = 0.rpx,
            bottomEnd: RcPx = 0.rpx,
            bottomStart: RcPx = 0.rpx,
        ): RcShape =
            RcShape(
                RoundedRectShape(topStart.value, topEnd.value, bottomStart.value, bottomEnd.value)
            )
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
