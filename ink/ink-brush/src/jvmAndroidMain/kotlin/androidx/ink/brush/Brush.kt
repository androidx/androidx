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

package androidx.ink.brush

import androidx.annotation.ColorInt
import androidx.annotation.ColorLong
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.toArgb
import androidx.ink.nativeloader.NativeLoader
import kotlin.Float
import kotlin.jvm.JvmStatic

/**
 * Defines how stroke inputs are interpreted to create the visual representation of a stroke.
 *
 * The type completely describes how inputs are used to create stroke meshes, and how those meshes
 * should be drawn by stroke renderers. In an analogous way to "font" and "font family", a [Brush]
 * can be considered an instance of a [BrushFamily] with a particular [color], [size], and an extra
 * parameter controlling visual fidelity, called [epsilon].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class Brush
internal constructor(
    /** The [BrushFamily] for this brush. See [StockBrushes] for available [BrushFamily] values. */
    public val family: BrushFamily,
    composeColor: ComposeColor,
    /**
     * The overall thickness of strokes created with a given brush, in the same units as the stroke
     * coordinate system. This must be at least as big as [epsilon].
     */
    @FloatRange(
        from = 0.0,
        fromInclusive = false,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false
    )
    public val size: Float,
    /**
     * The smallest distance for which two points should be considered visually distinct for stroke
     * generation geometry purposes. Effectively, it is the visual fidelity of strokes created with
     * this brush, where any (lack of) visual fidelity can be observed by a user the further zoomed
     * in they are on the stroke. Lower values of [epsilon] result in higher fidelity strokes at the
     * cost of somewhat higher memory usage. This value, like [size], is in the same units as the
     * stroke coordinate system. A size of 0.1 physical pixels at the default zoom level is a good
     * starting point that can tolerate a reasonable amount of zooming in with high quality visual
     * results.
     */
    @FloatRange(
        from = 0.0,
        fromInclusive = false,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false
    )
    public val epsilon: Float,
) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val composeColor: ComposeColor = composeColor.toColorInInkSupportedColorSpace()

    /**
     * The default color of a [Brush] is pure black. To set a custom color, use [withColorLong] or
     * [withColorIntArgb].
     */
    public constructor(
        family: BrushFamily,
        size: Float,
        epsilon: Float,
    ) : this(family, DEFAULT_COMPOSE_COLOR, size, epsilon)

    /**
     * The brush color as a [ColorLong], which can express colors in several different color spaces.
     * sRGB and Display P3 are supported; a color in any other color space will be converted to
     * Display P3.
     */
    public val colorLong: Long
        @ColorLong get(): Long = composeColor.value.toLong()

    /**
     * The brush color as a [ColorInt], which can only express colors in the sRGB color space. For
     * clients that want to support wide-gamut colors, use [colorLong].
     */
    public val colorInt: Int
        @ColorInt get(): Int = composeColor.toArgb()

    /** A handle to the underlying native [Brush] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nativePointer: Long =
        nativeCreateBrush(
            family.nativePointer,
            this.composeColor.red,
            this.composeColor.green,
            this.composeColor.blue,
            this.composeColor.alpha,
            this.composeColor.colorSpace.toInkColorSpaceId(),
            size,
            epsilon,
        )

    // Base implementation of copy() that all public versions call.
    private fun copy(family: BrushFamily, color: ComposeColor, size: Float, epsilon: Float): Brush {
        return if (
            family == this.family &&
                color == this.composeColor &&
                size == this.size &&
                epsilon == this.epsilon
        ) {
            // For a pure copy, return the same object, since it is immutable.
            this
        } else {
            Brush(family, color, size, epsilon)
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged. To change the color, use [copyWithColorLong] or [copyWithColorIntArgb].
     */
    @JvmSynthetic
    public fun copy(
        family: BrushFamily = this.family,
        size: Float = this.size,
        epsilon: Float = this.epsilon,
    ): Brush = copy(family, this.composeColor, size, epsilon)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged. The color is specified as a [ColorLong], which can encode several different color
     * spaces. sRGB and Display P3 are supported; a color in any other color space will be converted
     * to Display P3.
     *
     * Some libraries (notably Jetpack UI Graphics) use [ULong] for [ColorLong]s, so the caller must
     * call [ULong.toLong] on such a value before passing it to this method.
     */
    @JvmSynthetic
    public fun copyWithColorLong(
        family: BrushFamily = this.family,
        @ColorLong colorLong: Long,
        size: Float = this.size,
        epsilon: Float = this.epsilon,
    ): Brush = copy(family, ComposeColor(colorLong.toULong()), size, epsilon)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged. The color is specified as a [ColorInt], which is in the sRGB color space by
     * definition. Note that the [ColorInt] channel order puts alpha first (in the most significant
     * byte).
     *
     * Kotlin interprets integer literals greater than `0x7fffffff` as [Long]s, so callers that want
     * to specify a literal [ColorInt] with alpha >= 0x80 must call [Long.toInt] on the literal.
     */
    @JvmSynthetic
    public fun copyWithColorIntArgb(
        family: BrushFamily = this.family,
        @ColorInt colorIntArgb: Int,
        size: Float = this.size,
        epsilon: Float = this.epsilon,
    ): Brush = copy(family, ComposeColor(colorIntArgb), size, epsilon)

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a Brush. Kotlin developers, see [copy] method.
     */
    public fun toBuilder(): Builder =
        Builder().setFamily(family).setComposeColor(composeColor).setSize(size).setEpsilon(epsilon)

    /**
     * Builder for [Brush].
     *
     * Use Brush.Builder to construct a [Brush] with default values, overriding only as needed.
     */
    public class Builder {
        private var family: BrushFamily? = null
        private var composeColor: ComposeColor = DEFAULT_COMPOSE_COLOR

        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        private var size: Float? = null

        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        private var epsilon: Float? = null

        /**
         * Sets the [BrushFamily] for this brush. See [StockBrushes] for available [BrushFamily]
         * values.
         */
        public fun setFamily(family: BrushFamily): Builder {
            this.family = family
            return this
        }

        internal fun setComposeColor(color: ComposeColor): Builder {
            this.composeColor = color
            return this
        }

        /**
         * Sets the color using a [ColorLong], which can encode several different color spaces. sRGB
         * and Display P3 are supported; a color in any other color space will be converted to
         * Display P3.
         *
         * Some libraries (notably Jetpack UI Graphics) use [ULong] for [ColorLong]s, so the caller
         * must call [ULong.toLong] on such a value before passing it to this method.
         */
        public fun setColorLong(@ColorLong colorLong: Long): Builder {
            this.composeColor = ComposeColor(colorLong.toULong())
            return this
        }

        /**
         * Sets the color using a [ColorInt], which is in the sRGB color space by definition. Note
         * that the [ColorInt] channel order puts alpha first (in the most significant byte).
         *
         * Kotlin interprets integer literals greater than `0x7fffffff` as [Long]s, so Kotlin
         * callers that want to specify a literal [ColorInt] with alpha >= 0x80 must call
         * [Long.toInt] on the literal.
         */
        public fun setColorIntArgb(@ColorInt colorInt: Int): Builder {
            this.composeColor = ComposeColor(colorInt)
            return this
        }

        public fun setSize(
            @FloatRange(
                from = 0.0,
                fromInclusive = false,
                to = Double.POSITIVE_INFINITY,
                toInclusive = false,
            )
            size: Float
        ): Builder {
            this.size = size
            return this
        }

        public fun setEpsilon(
            @FloatRange(
                from = 0.0,
                fromInclusive = false,
                to = Double.POSITIVE_INFINITY,
                toInclusive = false,
            )
            epsilon: Float
        ): Builder {
            this.epsilon = epsilon
            return this
        }

        public fun build(): Brush =
            Brush(
                family =
                    checkNotNull(family) {
                        "brush family must be specified before calling build()"
                    },
                composeColor = composeColor,
                size = checkNotNull(size) { "brush size must be specified before calling build()" },
                epsilon =
                    checkNotNull(epsilon) {
                        "brush epsilon must be specified before calling build()"
                    },
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Brush) return false

        if (family != other.family) return false
        if (composeColor != other.composeColor) return false
        if (size != other.size) return false
        if (epsilon != other.epsilon) return false

        return true
    }

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies the same hashCode.
    override fun hashCode(): Int {
        var result = family.hashCode()
        result = 31 * result + composeColor.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + epsilon.hashCode()
        return result
    }

    override fun toString(): String {
        return "Brush(family=$family, color=$composeColor, size=$size, epsilon=$epsilon)"
    }

    /** Delete native Brush memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrush(nativePointer)
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeCreateBrush(
        familyNativePointer: Long,
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
        size: Float,
        epsilon: Float,
    ): Long

    /** Release the underlying memory allocated in [nativeCreateBrush]. */
    private external fun nativeFreeBrush(
        nativePointer: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    public companion object {
        init {
            NativeLoader.load()
        }

        private val DEFAULT_COMPOSE_COLOR = ComposeColor.Black

        /**
         * Returns a new [Brush] with the color specified by a [ColorLong], which can encode several
         * different color spaces. sRGB and Display P3 are supported; a color in any other color
         * space will be converted to Display P3.
         *
         * Some libraries (notably Jetpack UI Graphics) use [ULong] for [ColorLong]s, so the caller
         * must call [ULong.toLong] on such a value before passing it to this method.
         */
        @JvmStatic
        public fun withColorLong(
            family: BrushFamily,
            @ColorLong colorLong: Long,
            size: Float,
            epsilon: Float,
        ): Brush = Brush(family, ComposeColor(colorLong.toULong()), size, epsilon)

        /**
         * Returns a new [Brush] with the color specified by a [ColorInt], which is in the sRGB
         * color space by definition. Note that the [ColorInt] channel order puts alpha first (in
         * the most significant byte).
         *
         * Kotlin interprets integer literals greater than `0x7fffffff` as [Long]s, so callers that
         * want to specify a literal [ColorInt] with alpha >= 0x80 must call [Long.toInt] on the
         * literal.
         */
        @JvmStatic
        public fun withColorIntArgb(
            family: BrushFamily,
            @ColorInt colorIntArgb: Int,
            size: Float,
            epsilon: Float,
        ): Brush = Brush(family, ComposeColor(colorIntArgb), size, epsilon)

        /** Returns a new [Brush.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()
    }
}
