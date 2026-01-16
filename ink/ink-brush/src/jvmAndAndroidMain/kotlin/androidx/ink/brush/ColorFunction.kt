/*
 * Copyright (C) 2025 The Android Open Source Project
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

/** A [ColorFunction] defines a mapping over colors. */
@ExperimentalInkCustomBrushApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi

// NotCloseable: Finalize is only used to free the native peer.
@Suppress("NotCloseable")
public abstract class ColorFunction private constructor(internal val nativePointer: Long) {

    /** Transforms the input color into a new color. */
    public abstract fun transformComposeColor(color: ComposeColor): ComposeColor

    /** Transforms the input color into a new color. */
    @ColorInt
    public fun transformColorIntArgb(@ColorInt colorIntArgb: Int): Int =
        transformComposeColor(ComposeColor(colorIntArgb)).toArgb()

    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        ColorFunctionNative.free(nativePointer)
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): ColorFunction =
            when (ColorFunctionNative.getParametersType(unownedNativePointer)) {
                0 -> OpacityMultiplier(unownedNativePointer)
                1 -> ReplaceColor(unownedNativePointer)
                else -> throw IllegalArgumentException("Invalid color function type")
            }
    }

    /** A [ColorFunction] that scales the color opacity by a specified multiplier. */
    public class OpacityMultiplier internal constructor(nativePointer: Long) :
        ColorFunction(nativePointer) {

        public constructor(
            @FloatRange(from = 0.0) multiplier: Float
        ) : this(ColorFunctionNative.createOpacityMultiplier(multiplier))

        @get:FloatRange(from = 0.0)
        public val multiplier: Float
            get() = ColorFunctionNative.getOpacityMultiplier(nativePointer)

        override fun transformComposeColor(color: ComposeColor): ComposeColor =
            color.copy(alpha = color.alpha * multiplier)

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is OpacityMultiplier) {
                return false
            }
            return multiplier == other.multiplier
        }

        override fun hashCode(): Int = multiplier.hashCode()

        override fun toString(): String = "ColorFunction.OpacityMultiplier($multiplier)"

        // Declared to make extension functions available.
        public companion object
    }

    /** A [ColorFunction] that ignores the input color and replaces it with the specified color. */
    public class ReplaceColor internal constructor(nativePointer: Long) :
        ColorFunction(nativePointer) {

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("HiddenTypeParameter") // Internal API.
        public val internalColor: ComposeColor =
            // Caching this because the native call is slow. Still doing the round-trip on
            // construction to
            // ensure this is exercised by tests and that deserialized color functions are
            // consistent with
            // newly constructed color functions.
            ComposeColor(ColorFunctionNative.computeReplaceColorLong(nativePointer).toULong())

        public val colorLong: Long
            @ColorLong get(): Long = internalColor.value.toLong()

        public val colorIntArgb: Int
            @ColorInt get(): Int = internalColor.toArgb()

        override fun transformComposeColor(color: ComposeColor): ComposeColor = this.internalColor

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is ReplaceColor) {
                return false
            }
            return internalColor.equals(other.internalColor)
        }

        override fun hashCode(): Int = internalColor.hashCode()

        override fun toString(): String = "ColorFunction.ReplaceColor($internalColor)"

        public companion object {

            @JvmStatic
            public fun withComposeColor(color: ComposeColor): ReplaceColor =
                ReplaceColor(
                    color.toColorInInkSupportedColorSpace().let { convertedColor ->
                        ColorFunctionNative.createReplaceColor(
                            convertedColor.red,
                            convertedColor.green,
                            convertedColor.blue,
                            convertedColor.alpha,
                            convertedColor.colorSpace.toInkColorSpaceId(),
                        )
                    }
                )

            @JvmStatic
            public fun withColorLong(@ColorLong colorLong: Long): ReplaceColor =
                ReplaceColor.withComposeColor(ComposeColor(colorLong.toULong()))

            @JvmStatic
            public fun withColorIntArgb(@ColorInt colorIntArgb: Int): ReplaceColor =
                ReplaceColor.withComposeColor(ComposeColor(colorIntArgb))
        }
    }
}

@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object ColorFunctionNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createOpacityMultiplier(multiplier: Float): Long

    @UsedByNative
    external fun createReplaceColor(
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
    ): Long

    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getParametersType(nativePointer: Long): Int

    @UsedByNative external fun getOpacityMultiplier(nativePointer: Long): Float

    @UsedByNative external fun computeReplaceColorLong(nativePointer: Long): Long
}
