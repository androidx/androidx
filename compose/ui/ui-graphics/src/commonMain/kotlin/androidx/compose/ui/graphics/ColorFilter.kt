/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// TODO mark internal once https://youtrack.jetbrains.com/issue/KT-36695 is fixed
/* internal */ expect class NativeColorFilter

/**
 * Effect used to modify the color of each pixel drawn on a [Paint] that it is installed on
 */
@Immutable
open class ColorFilter internal constructor(internal val nativeColorFilter: NativeColorFilter) {

    companion object {
        /**
         * Creates a color filter that applies the blend mode given as the second
         * argument. The source color is the one given as the first argument, and the
         * destination color is the one from the layer being composited.
         *
         * The output of this filter is then composited into the background according
         * to the [Paint.blendMode], using the output of this filter as the source
         * and the background as the destination.
         *
         * @param color Color used to blend source content
         * @param blendMode BlendMode used when compositing the tint color to the destination
         */
        @Stable
        fun tint(color: Color, blendMode: BlendMode = BlendMode.SrcIn): ColorFilter =
            BlendModeColorFilter(color, blendMode)

        /**
         * Create a [ColorFilter] that transforms colors through a 4x5 color matrix. This filter can
         * be used to change the saturation of pixels, convert from YUV to RGB, etc.
         *
         * @param colorMatrix ColorMatrix used to transform pixel values when drawn
         */
        @Stable
        fun colorMatrix(colorMatrix: ColorMatrix): ColorFilter =
            ColorMatrixColorFilter(colorMatrix)

        /**
         * Create a [ColorFilter] that can be used to simulate simple lighting effects.
         * A lighting ColorFilter is defined by two parameters, one used to multiply the source
         * color and one used to add to the source color
         *
         * @param multiply Color used to multiply the source color when the color filter is applied.
         * @param add Color that will be added to the source color when the color filter is applied.
         */
        @Stable
        fun lighting(multiply: Color, add: Color): ColorFilter =
            LightingColorFilter(multiply, add)
    }
}

/**
 * Creates a color filter that applies the blend mode given as the second
 * argument. The source color is the one given as the first argument, and the
 * destination color is the one from the layer being composited.
 *
 * The output of this filter is then composited into the background according
 * to the [Paint.blendMode], using the output of this filter as the source
 * and the background as the destination.
 *
 * @param color Color used to blend source content
 * @param blendMode BlendMode used when compositing the tint color to the destination
 */
@Immutable
class BlendModeColorFilter internal constructor(
    val color: Color,
    val blendMode: BlendMode,
    nativeColorFilter: NativeColorFilter
) : ColorFilter(nativeColorFilter) {

    constructor(
        color: Color,
        blendMode: BlendMode
    ) : this(color, blendMode, actualTintColorFilter(color, blendMode))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlendModeColorFilter) return false

        if (color != other.color) return false
        if (blendMode != other.blendMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + blendMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "BlendModeColorFilter(color=$color, blendMode=$blendMode)"
    }
}

/**
 * Create a [ColorFilter] that transforms colors through a 4x5 color matrix. This filter can
 * be used to change the saturation of pixels, convert from YUV to RGB, etc.
 */
@Immutable
class ColorMatrixColorFilter internal constructor(
    private var colorMatrix: ColorMatrix?,
    nativeColorFilter: NativeColorFilter
) : ColorFilter(nativeColorFilter) {

    constructor(
        colorMatrix: ColorMatrix
    ) : this(colorMatrix, actualColorMatrixColorFilter(colorMatrix))

    /**
     * Copy the internal [ColorMatrix] into the provided [ColorMatrix] instance. By default
     * this creates a new [ColorMatrix] instance, however, consumers are encouraged to create
     * and re-use instances of [ColorMatrix]. This method returns the copied result for
     * convenience
     *
     * @param targetColorMatrix Optional [ColorMatrix] to copy contents into
     * @return the copied [ColorMatrix] instance
     */
    fun copyColorMatrix(targetColorMatrix: ColorMatrix = ColorMatrix()): ColorMatrix {
        val curMatrix = obtainColorMatrix()
        curMatrix.values.copyInto(targetColorMatrix.values)
        return targetColorMatrix
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorMatrixColorFilter) return false

        val colorMatrix = obtainColorMatrix()
        val otherColorMatrix = other.obtainColorMatrix()
        if (!colorMatrix.values.contentEquals(otherColorMatrix.values)) return false

        return true
    }

    private fun obtainColorMatrix(): ColorMatrix =
        // In the event that this ColorMatrixFilter was instantiated from a conversion
        // from the NativeColorFilter, lazily query the ColorMatrix here to avoid doing
        // a copy of ColorMatrix instances until this method is invoked
        colorMatrix ?: actualColorMatrixFromFilter(nativeColorFilter).also {
            colorMatrix = it
        }

    override fun hashCode(): Int {
        return colorMatrix?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "ColorMatrixColorFilter(colorMatrix=$colorMatrix)"
    }
}

/**
 * Create a [ColorFilter] that can be used to simulate simple lighting effects.
 * A lighting ColorFilter is defined by two parameters, one used to multiply the source
 * color and one used to add to the source color
 *
 * @param multiply Color used to multiply the source color when the color filter is applied.
 * @param add Color that will be added to the source color when the color filter is applied.
 */
@Immutable
class LightingColorFilter internal constructor(
    val multiply: Color,
    val add: Color,
    nativeColorFilter: NativeColorFilter
) : ColorFilter(nativeColorFilter) {

    constructor(
        multiply: Color,
        add: Color
    ) : this(multiply, add, actualLightingColorFilter(multiply, add))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LightingColorFilter) return false

        if (multiply != other.multiply) return false
        if (add != other.add) return false

        return true
    }

    override fun hashCode(): Int {
        var result = multiply.hashCode()
        result = 31 * result + add.hashCode()
        return result
    }

    override fun toString(): String {
        return "LightingColorFilter(multiply=$multiply, add=$add)"
    }
}

internal expect fun actualTintColorFilter(color: Color, blendMode: BlendMode): NativeColorFilter

internal expect fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): NativeColorFilter

internal expect fun actualLightingColorFilter(multiply: Color, add: Color): NativeColorFilter

internal expect fun actualColorMatrixFromFilter(filter: NativeColorFilter): ColorMatrix
