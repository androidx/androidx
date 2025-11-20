/*
 * Copyright (C) 2024-2025 The Android Open Source Project
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
import androidx.ink.nativeloader.UsedByNative
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
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class Brush
private constructor(
    /** A handle to the underlying native [Brush] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,

    /** The [BrushFamily] for this brush. See [StockBrushes] for available [BrushFamily] values. */
    public val family: BrushFamily,
) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("HiddenTypeParameter") // Internal API.
    public val internalColor: ComposeColor =
        // Caching this because the native call is slow. Still doing the round-trip on construction
        // to
        // ensure this is exercised by tests and that deserialized brushes are consistent with newly
        // constructed brushes.
        ComposeColor(BrushNative.computeComposeColorLong(nativePointer).toULong())

    /**
     * The overall thickness of strokes created with a given brush, in the same units as the stroke
     * coordinate system. This must be at least as big as [epsilon].
     */
    @get:FloatRange(
        from = 0.0,
        fromInclusive = false,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false,
    )
    public val size: Float
        get() = BrushNative.getSize(nativePointer)

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
    @get:FloatRange(
        from = 0.0,
        fromInclusive = false,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false,
    )
    public val epsilon: Float
        get() = BrushNative.getEpsilon(nativePointer)

    internal constructor(
        family: BrushFamily,
        composeColor: ComposeColor,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        size: Float,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        epsilon: Float,
    ) : this(
        composeColor.toColorInInkSupportedColorSpace().let { convertedColor ->
            BrushNative.create(
                family.nativePointer,
                convertedColor.red,
                convertedColor.green,
                convertedColor.blue,
                convertedColor.alpha,
                convertedColor.colorSpace.toInkColorSpaceId(),
                size,
                epsilon,
            )
        },
        family,
    )

    /**
     * The default color of a [Brush] is pure black. To set a custom color, use
     * [createWithColorLong] or [createWithColorIntArgb].
     */
    public constructor(
        family: BrushFamily,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        size: Float,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        epsilon: Float,
    ) : this(family, DEFAULT_COMPOSE_COLOR, size, epsilon)

    /**
     * The brush color as a [ColorLong], which can express colors in several different color spaces.
     * sRGB and Display P3 are supported; a color in any other color space will be converted to
     * Display P3.
     */
    public val colorLong: Long
        @ColorLong get(): Long = internalColor.value.toLong()

    /**
     * The brush color as a [ColorInt], which can only express colors in the sRGB color space. For
     * clients that want to support wide-gamut colors, use [colorLong].
     */
    public val colorIntArgb: Int
        @ColorInt get(): Int = internalColor.toArgb()

    // Base implementation of copy() that all public versions call.
    private fun copy(family: BrushFamily, color: ComposeColor, size: Float, epsilon: Float): Brush {
        return if (
            family == this.family &&
                color == this.internalColor &&
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
    @JvmOverloads
    public fun copy(
        family: BrushFamily = this.family,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        size: Float = this.size,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        epsilon: Float = this.epsilon,
    ): Brush = copy(family, this.internalColor, size, epsilon)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged. The color is specified as a [ColorLong], which can encode several different color
     * spaces. sRGB and Display P3 are supported; a color in any other color space will be converted
     * to Display P3.
     *
     * Some libraries (notably Jetpack UI Graphics) use [ULong] for [ColorLong]s, so the caller must
     * call [ULong.toLong] on such a value before passing it to this method.
     */
    @JvmOverloads
    public fun copyWithColorLong(
        @ColorLong colorLong: Long,
        family: BrushFamily = this.family,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        size: Float = this.size,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
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
    @JvmOverloads
    public fun copyWithColorIntArgb(
        @ColorInt colorIntArgb: Int,
        family: BrushFamily = this.family,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        size: Float = this.size,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        epsilon: Float = this.epsilon,
    ): Brush = copy(family, ComposeColor(colorIntArgb), size, epsilon)

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a Brush. Kotlin developers, see [copy] method.
     */
    public fun toBuilder(): Builder =
        Builder().setFamily(family).setComposeColor(internalColor).setSize(size).setEpsilon(epsilon)

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
        public fun setColorIntArgb(@ColorInt colorIntArgb: Int): Builder {
            this.composeColor = ComposeColor(colorIntArgb)
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
        if (internalColor != other.internalColor) return false
        if (size != other.size) return false
        if (epsilon != other.epsilon) return false

        return true
    }

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies the same hashCode.
    override fun hashCode(): Int {
        var result = family.hashCode()
        result = 31 * result + internalColor.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + epsilon.hashCode()
        return result
    }

    override fun toString(): String {
        return "Brush(family=$family, color=$internalColor, size=$size, epsilon=$epsilon)"
    }

    /** Delete native Brush memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushNative.free(nativePointer)
    }

    public companion object {
        private val DEFAULT_COMPOSE_COLOR = ComposeColor.Black

        /**
         * Construct a [BrushPaint] from an unowned heap-allocated native pointer to a C++
         * `BrushPaint`. Kotlin wrapper objects nested under the [BrushPaint] are initialized
         * similarly using their own [wrapNative] methods, passing those pointers to newly
         * copy-constructed heap-allocated objects. That avoids the need to call Kotlin constructors
         * for those objects from C++.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @OptIn(ExperimentalInkCustomBrushApi::class)
        public fun wrapNative(unownedNativePointer: Long): Brush {
            return Brush(
                unownedNativePointer,
                BrushFamily.wrapNative(BrushNative.newCopyOfBrushFamily(unownedNativePointer)),
            )
        }

        /**
         * Returns a new [Brush] with the color specified by a [ColorLong], which can encode several
         * different color spaces. sRGB and Display P3 are supported; a color in any other color
         * space will be converted to Display P3.
         *
         * Some libraries (notably Jetpack UI Graphics) use [ULong] for [ColorLong]s, so the caller
         * must call [ULong.toLong] on such a value before passing it to this method.
         */
        @JvmStatic
        public fun createWithColorLong(
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
        public fun createWithColorIntArgb(
            family: BrushFamily,
            @ColorInt colorIntArgb: Int,
            size: Float,
            epsilon: Float,
        ): Brush = Brush(family, ComposeColor(colorIntArgb), size, epsilon)

        /** Returns a new [Brush.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()
    }
}

/** Singleton wrapper around native JNI calls. */
@UsedByNative
private object BrushNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative
    external fun create(
        familyNativePointer: Long,
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
        size: Float,
        epsilon: Float,
    ): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun computeComposeColorLong(nativePointer: Long): Long

    /** This is a callback used by computeComposeColorLong. */
    @UsedByNative
    @JvmStatic
    fun composeColorLongFromComponents(
        colorSpaceId: Int,
        redGammaCorrected: Float,
        greenGammaCorrected: Float,
        blueGammaCorrected: Float,
        alpha: Float,
    ): Long =
        ComposeColor(
                redGammaCorrected,
                greenGammaCorrected,
                blueGammaCorrected,
                alpha,
                colorSpace = composeColorSpaceFromInkColorSpaceId(colorSpaceId),
            )
            .value
            .toLong()

    @UsedByNative external fun getSize(nativePointer: Long): Float

    @UsedByNative external fun getEpsilon(nativePointer: Long): Float

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushFamily` for the pointed-at
     * native `Brush`.
     */
    @UsedByNative external fun newCopyOfBrushFamily(nativePointer: Long): Long
}
