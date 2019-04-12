/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.graphics

import androidx.annotation.AnyThread
import androidx.annotation.IntRange
import androidx.annotation.Size
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.ulp
import kotlin.math.withSign

/**
 * {@usesMathJax}
 *
 * A [ColorSpace] is used to identify a specific organization of colors.
 * Each color space is characterized by a [color model][Model] that defines
 * how a color value is represented (for instance the [RGB][Model.Rgb] color
 * model defines a color value as a triplet of numbers).
 *
 * Each component of a color must fall within a valid range, specific to each
 * color space, defined by [getMinValue] and [getMaxValue]
 * This range is commonly \([0..1]\). While it is recommended to use values in the
 * valid range, a color space always clamps input and output values when performing
 * operations such as converting to a different color space.
 *
 * ###Using color spaces
 *
 * This implementation provides a pre-defined set of common color spaces
 * described in the [Named] enum. To obtain an instance of one of the
 * pre-defined color spaces, simply invoke [get]:
 *
 *     val sRgb = ColorSpace.get(ColorSpace.Named.Srgb);
 *
 * The [get] method always returns the same instance for a given
 * name. Color spaces with an [RGB][Model.Rgb] color model can be safely
 * cast to [Rgb]. Doing so gives you access to more APIs to query various
 * properties of RGB color models: color gamut primaries, transfer functions,
 * conversions to and from linear space, etc. Please refer to [Rgb] for
 * more information.
 *
 * The documentation of [Named] provides a detailed description of the
 * various characteristics of each available color space.
 *
 * ###Color space conversions
 *
 * To allow conversion between color spaces, this implementation uses the CIE
 * XYZ profile connection space (PCS). Color values can be converted to and from
 * this PCS using [toXyz] and [fromXyz].
 *
 * For color space with a non-RGB color model, the white point of the PCS
 * *must be* the CIE standard illuminant D50. RGB color spaces use their
 * native white point (D65 for [sRGB][Named.Srgb] for instance and must
 * undergo [chromatic adaptation][Adaptation] as necessary.
 *
 * Since the white point of the PCS is not defined for RGB color space, it is
 * highly recommended to use the variants of the [connect]
 * method to perform conversions between color spaces. A color space can be
 * manually adapted to a specific white point using [adapt].
 * Please refer to the documentation of [RGB color spaces][Rgb] for more
 * information. Several common CIE standard illuminants are provided in this
 * class as reference (see [IlluminantD65] or [IlluminantD50]
 * for instance).
 *
 * Here is an example of how to convert from a color space to another:
 *
 *     // Convert from DCI-P3 to Rec.2020
 *     val dciP3 = ColorSpace.get(ColorSpace.Named.DciP3)
 *     val bt2020 = ColorSpace.get(ColorSpace.Named.BT2020)
 *     val connector = ColorSpace.connect(dciP3, bt2020)
 *
 *     val bt2020Values = connector.transform(p3r, p3g, p3b);
 *
 * You can easily convert to [sRGB][Named.Srgb] by omitting the second
 * parameter:
 *
 *     // Convert from DCI-P3 to sRGB
 *     val dciP3 = ColorSpace.get(ColorSpace.Named.DciP3)
 *     val connector = ColorSpace.connect(dciP3)
 *
 *     val sRGBValues = connector.transform(p3r, p3g, p3b);
 *
 * Conversions also work between color spaces with different color models:
 *
 *     // Convert from CIE L*a*b* (color model Lab) to Rec.709 (color model RGB)
 *     val cieLab = ColorSpace.get(ColorSpace.Named.CIE_LAB)
 *     val bt709 = ColorSpace.get(ColorSpace.Named.BT709)
 *     val connector = ColorSpace.connect( cieLab, bt709)
 *
 * ###Color spaces and multi-threading
 *
 * Color spaces and other related classes ([Connector] for instance)
 * are immutable and stateless. They can be safely used from multiple concurrent
 * threads.
 *
 * Public static methods provided by this class, such as [get]
 * and [connect], are also guaranteed to be thread-safe.
 *
 * @see get
 * @see Named
 * @see Model
 * @see Connector
 * @see Adaptation
 */
@AnyThread
abstract class ColorSpace internal constructor(
    /**
     * Returns the name of this color space. The name is never null
     * and contains always at least 1 character.
     *
     * Color space names are recommended to be unique but are not
     * guaranteed to be. There is no defined format but the name usually
     * falls in one of the following categories:
     *
     *  * Generic names used to identify color spaces in non-RGB
     * color models. For instance: [Generic L*a*b*][Named.CieLab].
     *  * Names tied to a particular specification. For instance:
     * [sRGB IEC61966-2.1][Named.Srgb] or
     * [SMPTE ST 2065-1:2012 ACES][Named.Aces].
     *  * Ad-hoc names, often generated procedurally or by the user
     * during a calibration workflow. These names often contain the
     * make and model of the display.
     *
     * Because the format of color space names is not defined, it is
     * not recommended to programmatically identify a color space by its
     * name alone. Names can be used as a first approximation.
     *
     * It is however perfectly acceptable to display color space names to
     * users in a UI, or in debuggers and logs. When displaying a color space
     * name to the user, it is recommended to add extra information to avoid
     * ambiguities: color model, a representation of the color space's gamut,
     * white point, etc.
     *
     * @return A non-null String of length >= 1
     */
    val name: String,

    /**
     * Return the color model of this color space.
     *
     * @return A non-null [Model]
     *
     * @see Model
     * @see componentCount
     */
    val model: Model,

    /**
     * Returns the ID of this color space. Positive IDs match the color
     * spaces enumerated in [Named]. A negative ID indicates a
     * color space created by calling one of the public constructors.
     *
     * @return An integer between [MinId] and [MaxId]
     */
    @IntRange(from = MinId.toLong(), to = MaxId.toLong())
    id: Int
) {
    /**
     * Returns the ID of this color space. Positive IDs match the color
     * spaces enumerated in [Named]. A negative ID indicates a
     * color space created by calling one of the public constructors.
     *
     * @return An integer between [MinId] and [MaxId]
     */
    @IntRange(from = MinId.toLong(), to = MaxId.toLong())
    var id: Int = id
        internal set

    /**
     * Returns the number of components that form a color value according
     * to this color space's color model.
     *
     * @return An integer between 1 and 4
     *
     * @see Model
     * @see model
     */
    val componentCount: Int
        @IntRange(from = 1, to = 4)
        get() = model.componentCount

    /**
     * Returns whether this color space is a wide-gamut color space.
     * An RGB color space is wide-gamut if its gamut entirely contains
     * the [sRGB][Named.Srgb] gamut and if the area of its gamut is
     * 90% of greater than the area of the [NTSC][Named.Ntsc1953]
     * gamut.
     *
     * @return True if this color space is a wide-gamut color space,
     * false otherwise
     */
    abstract val isWideGamut: Boolean

    /**
     *
     * Indicates whether this color space is the sRGB color space or
     * equivalent to the sRGB color space.
     *
     * A color space is considered sRGB if it meets all the following
     * conditions:
     *
     *  * Its color model is [Model.Rgb].
     *  *
     * Its primaries are within 1e-3 of the true
     * [sRGB][Named.Srgb] primaries.
     *
     *  *
     * Its white point is within 1e-3 of the CIE standard
     * illuminant [D65][IlluminantD65].
     *
     *  * Its opto-electronic transfer function is not linear.
     *  * Its electro-optical transfer function is not linear.
     *  * Its transfer functions yield values within 1e-3 of [Named.Srgb].
     *  * Its range is \([0..1]\).
     *
     *
     * This method always returns true for [Named.Srgb].
     *
     * @return True if this color space is the sRGB color space (or a
     * close approximation), false otherwise
     */
    open val isSrgb: Boolean
        get() = false

    /**
     * {@usesMathJax}
     * List of common, named color spaces. A corresponding instance of
     * [ColorSpace] can be obtained by calling [ColorSpace.get]:
     *
     *     val cs = ColorSpace.get(ColorSpace.Named.DciP3);
     *
     * The properties of each color space are described below (see [sRGB][Srgb]
     * for instance). When applicable, the color gamut of each color space is compared
     * to the color gamut of sRGB using a CIE 1931 xy chromaticity diagram. This diagram
     * shows the location of the color space's primaries and white point.
     *
     * @see ColorSpace.get
     */
    enum class Named(internal val colorSpace: ColorSpace) {
        // NOTE: Do NOT change the order of the enum
        /**
         * [RGB][ColorSpace.Rgb] color space sRGB standardized as IEC 61966-2.1:1999.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.640</td><td>0.300</td><td>0.150</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.330</td><td>0.600</td><td>0.060</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">sRGB IEC61966-2.1</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{sRGB} = \begin{cases} 12.92 \times C_{linear} & C_{linear} \lt 0.0031308 \\\
         *             1.055 \times C_{linear}^{\frac{1}{2.4}} - 0.055 & C_{linear} \ge 0.0031308 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{sRGB}}{12.92} & C_{sRGB} \lt 0.04045 \\\
         *             \left( \frac{C_{sRGB} + 0.055}{1.055} \right) ^{2.4} & C_{sRGB} \ge 0.04045 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_srgb.png"></img>
         * <figcaption style="text-align: center;">sRGB</figcaption>
         */
        Srgb(ColorSpace.Rgb(
            "sRGB IEC61966-2.1",
            SrgbPrimaries,
            IlluminantD65,
            SrgbTransferParameters,
            id = 0
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space sRGB standardized as IEC 61966-2.1:1999.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.640</td><td>0.300</td><td>0.150</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.330</td><td>0.600</td><td>0.060</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">sRGB IEC61966-2.1 (Linear)</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(C_{sRGB} = C_{linear}\)</td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(C_{linear} = C_{sRGB}\)</td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_srgb.png"></img>
         * <figcaption style="text-align: center;">sRGB</figcaption>
         */
        LinearSrgb(ColorSpace.Rgb(
            "sRGB IEC61966-2.1 (Linear)",
            SrgbPrimaries,
            IlluminantD65,
            1.0,
            0.0f, 1.0f,
            id = 1
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space scRGB-nl standardized as IEC 61966-2-2:2003.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.640</td><td>0.300</td><td>0.150</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.330</td><td>0.600</td><td>0.060</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">scRGB-nl IEC 61966-2-2:2003</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{scRGB} = \begin{cases} sign(C_{linear}) 12.92 \times \left| C_{linear} \right| &
         *             \left| C_{linear} \right| \lt 0.0031308 \\\
         *             sign(C_{linear}) 1.055 \times \left| C_{linear} \right| ^{\frac{1}{2.4}} - 0.055 &
         *             \left| C_{linear} \right| \ge 0.0031308 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}sign(C_{scRGB}) \frac{\left| C_{scRGB} \right|}{12.92} &
         *             \left| C_{scRGB} \right| \lt 0.04045 \\\
         *             sign(C_{scRGB}) \left( \frac{\left| C_{scRGB} \right| + 0.055}{1.055} \right) ^{2.4} &
         *             \left| C_{scRGB} \right| \ge 0.04045 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([-0.799..2.399[\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_scrgb.png"></img>
         * <figcaption style="text-align: center;">Extended sRGB (orange) vs sRGB (white)</figcaption>
         */
        ExtendedSrgb(ColorSpace.Rgb(
            "scRGB-nl IEC 61966-2-2:2003",
            SrgbPrimaries,
            IlluminantD65, null,
            { x -> absRcpResponse(x, 1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045, 2.4) },
            { x -> absResponse(x, 1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045, 2.4) },
            -0.799f, 2.399f, null, // FIXME: Use SrgbTransferParameters
            id = 2
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space scRGB standardized as IEC 61966-2-2:2003.
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.640</td><td>0.300</td><td>0.150</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.330</td><td>0.600</td><td>0.060</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">scRGB IEC 61966-2-2:2003</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(C_{scRGB} = C_{linear}\)</td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(C_{linear} = C_{scRGB}\)</td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([-0.5..7.499[\)</td></tr>
         * </table>
         *
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_scrgb.png"></img>
         * <figcaption style="text-align: center;">Extended sRGB (orange) vs sRGB (white)</figcaption>
         */
        LinearExtendedSrgb(ColorSpace.Rgb(
            "scRGB IEC 61966-2-2:2003",
            SrgbPrimaries,
            IlluminantD65,
            1.0,
            -0.5f, 7.499f,
            id = 3)),

        /**
         * [RGB][ColorSpace.Rgb] color space BT.709 standardized as Rec. ITU-R BT.709-5.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.640</td><td>0.300</td><td>0.150</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.330</td><td>0.600</td><td>0.060</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Rec. ITU-R BT.709-5</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{BT709} = \begin{cases} 4.5 \times C_{linear} & C_{linear} \lt 0.018 \\\
         *             1.099 \times C_{linear}^{\frac{1}{2.2}} - 0.099 & C_{linear} \ge 0.018 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{BT709}}{4.5} & C_{BT709} \lt 0.081 \\\
         *             \left( \frac{C_{BT709} + 0.099}{1.099} \right) ^{2.2} & C_{BT709} \ge 0.081 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_bt709.png"></img>
         * <figcaption style="text-align: center;">BT.709</figcaption>
         */
        Bt709(ColorSpace.Rgb(
            "Rec. ITU-R BT.709-5",
            floatArrayOf(0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f),
            IlluminantD65,
            Rgb.TransferParameters(1 / 1.099, 0.099 / 1.099, 1 / 4.5, 0.081, 1 / 0.45),
            id = 4
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space BT.2020 standardized as Rec. ITU-R BT.2020-1.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.708</td><td>0.170</td><td>0.131</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.292</td><td>0.797</td><td>0.046</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Rec. ITU-R BT.2020-1</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{BT2020} = \begin{cases} 4.5 \times C_{linear} & C_{linear} \lt 0.0181 \\\
         *             1.0993 \times C_{linear}^{\frac{1}{2.2}} - 0.0993 & C_{linear} \ge 0.0181 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{BT2020}}{4.5} & C_{BT2020} \lt 0.08145 \\\
         *             \left( \frac{C_{BT2020} + 0.0993}{1.0993} \right) ^{2.2} & C_{BT2020} \ge 0.08145 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_bt2020.png"></img>
         * <figcaption style="text-align: center;">BT.2020 (orange) vs sRGB (white)</figcaption>
         */
        Bt2020(ColorSpace.Rgb(
            "Rec. ITU-R BT.2020-1",
            floatArrayOf(0.708f, 0.292f, 0.170f, 0.797f, 0.131f, 0.046f),
            IlluminantD65,
            Rgb.TransferParameters(1 / 1.0993, 0.0993 / 1.0993, 1 / 4.5, 0.08145, 1 / 0.45),
            id = 5
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space DCI-P3 standardized as SMPTE RP 431-2-2007.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.680</td><td>0.265</td><td>0.150</td><td>0.314</td></tr>
         *     <tr><td>y</td><td>0.320</td><td>0.690</td><td>0.060</td><td>0.351</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">SMPTE RP 431-2-2007 DCI (P3)</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">N/A</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(C_{P3} = C_{linear}^{\frac{1}{2.6}}\)</td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(C_{linear} = C_{P3}^{2.6}\)</td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_dci_p3.png"></img>
         * <figcaption style="text-align: center;">DCI-P3 (orange) vs sRGB (white)</figcaption>
         */
        DciP3(ColorSpace.Rgb(
            "SMPTE RP 431-2-2007 DCI (P3)",
            floatArrayOf(0.680f, 0.320f, 0.265f, 0.690f, 0.150f, 0.060f),
            floatArrayOf(0.314f, 0.351f),
            2.6,
            0.0f, 1.0f,
            id = 6
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space Display P3 based on SMPTE RP 431-2-2007 and IEC 61966-2.1:1999.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.680</td><td>0.265</td><td>0.150</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.320</td><td>0.690</td><td>0.060</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Display P3</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{DisplayP3} = \begin{cases} 12.92 \times C_{linear} & C_{linear} \lt 0.0030186 \\\
         *             1.055 \times C_{linear}^{\frac{1}{2.4}} - 0.055 & C_{linear} \ge 0.0030186 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{DisplayP3}}{12.92} & C_{sRGB} \lt 0.04045 \\\
         *             \left( \frac{C_{DisplayP3} + 0.055}{1.055} \right) ^{2.4} & C_{sRGB} \ge 0.04045 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_display_p3.png"></img>
         * <figcaption style="text-align: center;">Display P3 (orange) vs sRGB (white)</figcaption>
         */
        DisplayP3(ColorSpace.Rgb(
            "Display P3",
            floatArrayOf(0.680f, 0.320f, 0.265f, 0.690f, 0.150f, 0.060f),
            IlluminantD65,
            SrgbTransferParameters,
            id = 7
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space NTSC, 1953 standard.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.67</td><td>0.21</td><td>0.14</td><td>0.310</td></tr>
         *     <tr><td>y</td><td>0.33</td><td>0.71</td><td>0.08</td><td>0.316</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">NTSC (1953)</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">C</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{BT709} = \begin{cases} 4.5 \times C_{linear} & C_{linear} \lt 0.018 \\\
         *             1.099 \times C_{linear}^{\frac{1}{2.2}} - 0.099 & C_{linear} \ge 0.018 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{BT709}}{4.5} & C_{BT709} \lt 0.081 \\\
         *             \left( \frac{C_{BT709} + 0.099}{1.099} \right) ^{2.2} & C_{BT709} \ge 0.081 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_ntsc_1953.png"></img>
         * <figcaption style="text-align: center;">NTSC 1953 (orange) vs sRGB (white)</figcaption>
         */
        Ntsc1953(ColorSpace.Rgb(
            "NTSC (1953)",
            Ntsc1953Primaries,
            IlluminantC,
            Rgb.TransferParameters(1 / 1.099, 0.099 / 1.099, 1 / 4.5, 0.081, 1 / 0.45),
            id = 8
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space SMPTE C.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.630</td><td>0.310</td><td>0.155</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.340</td><td>0.595</td><td>0.070</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">SMPTE-C RGB</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{BT709} = \begin{cases} 4.5 \times C_{linear} & C_{linear} \lt 0.018 \\\
         *             1.099 \times C_{linear}^{\frac{1}{2.2}} - 0.099 & C_{linear} \ge 0.018 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{BT709}}{4.5} & C_{BT709} \lt 0.081 \\\
         *             \left( \frac{C_{BT709} + 0.099}{1.099} \right) ^{2.2} & C_{BT709} \ge 0.081 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_smpte_c.png"></img>
         * <figcaption style="text-align: center;">SMPTE-C (orange) vs sRGB (white)</figcaption>
         */
        SmpteC(ColorSpace.Rgb(
            "SMPTE-C RGB",
            floatArrayOf(0.630f, 0.340f, 0.310f, 0.595f, 0.155f, 0.070f),
            IlluminantD65,
            Rgb.TransferParameters(1 / 1.099, 0.099 / 1.099, 1 / 4.5, 0.081, 1 / 0.45),
            id = 9
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space Adobe RGB (1998).
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.64</td><td>0.21</td><td>0.15</td><td>0.3127</td></tr>
         *     <tr><td>y</td><td>0.33</td><td>0.71</td><td>0.06</td><td>0.3290</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Adobe RGB (1998)</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D65</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(C_{RGB} = C_{linear}^{\frac{1}{2.2}}\)</td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(C_{linear} = C_{RGB}^{2.2}\)</td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_adobe_rgb.png"></img>
         * <figcaption style="text-align: center;">Adobe RGB (orange) vs sRGB (white)</figcaption>
         */
        AdobeRgb(ColorSpace.Rgb(
            "Adobe RGB (1998)",
            floatArrayOf(0.64f, 0.33f, 0.21f, 0.71f, 0.15f, 0.06f),
            IlluminantD65,
            2.2,
            0.0f, 1.0f,
            id = 10
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space ProPhoto RGB standardized as ROMM RGB ISO 22028-2:2013.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.7347</td><td>0.1596</td><td>0.0366</td><td>0.3457</td></tr>
         *     <tr><td>y</td><td>0.2653</td><td>0.8404</td><td>0.0001</td><td>0.3585</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">ROMM RGB ISO 22028-2:2013</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D50</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{ROMM} = \begin{cases} 16 \times C_{linear} & C_{linear} \lt 0.001953 \\\
         *             C_{linear}^{\frac{1}{1.8}} & C_{linear} \ge 0.001953 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(\begin{equation}
         *             C_{linear} = \begin{cases}\frac{C_{ROMM}}{16} & C_{ROMM} \lt 0.031248 \\\
         *             C_{ROMM}^{1.8} & C_{ROMM} \ge 0.031248 \end{cases}
         *             \end{equation}\)
         *         </td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([0..1]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_pro_photo_rgb.png"></img>
         * <figcaption style="text-align: center;">ProPhoto RGB (orange) vs sRGB (white)</figcaption>
         */
        ProPhotoRgb(ColorSpace.Rgb(
            "ROMM RGB ISO 22028-2:2013",
            floatArrayOf(0.7347f, 0.2653f, 0.1596f, 0.8404f, 0.0366f, 0.0001f),
            IlluminantD50,
            Rgb.TransferParameters(1.0, 0.0, 1 / 16.0, 0.031248, 1.8),
            id = 11
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space ACES standardized as SMPTE ST 2065-1:2012.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.73470</td><td>0.00000</td><td>0.00010</td><td>0.32168</td></tr>
         *     <tr><td>y</td><td>0.26530</td><td>1.00000</td><td>-0.07700</td><td>0.33767</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">SMPTE ST 2065-1:2012 ACES</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D60</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(C_{ACES} = C_{linear}\)</td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(C_{linear} = C_{ACES}\)</td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([-65504.0, 65504.0]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_aces.png"></img>
         * <figcaption style="text-align: center;">ACES (orange) vs sRGB (white)</figcaption>
         */
        Aces(ColorSpace.Rgb(
            "SMPTE ST 2065-1:2012 ACES",
            floatArrayOf(0.73470f, 0.26530f, 0.0f, 1.0f, 0.00010f, -0.0770f),
            IlluminantD60,
            1.0,
            -65504.0f, 65504.0f,
            id = 12
        )),

        /**
         * [RGB][ColorSpace.Rgb] color space ACEScg standardized as Academy S-2014-004.
         *
         * <table summary="Color space definition">
         *     <tr>
         *         <th>Chromaticity</th><th>Red</th><th>Green</th><th>Blue</th><th>White point</th>
         *     </tr>
         *     <tr><td>x</td><td>0.713</td><td>0.165</td><td>0.128</td><td>0.32168</td></tr>
         *     <tr><td>y</td><td>0.293</td><td>0.830</td><td>0.044</td><td>0.33767</td></tr>
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Academy S-2014-004 ACEScg</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D60</td></tr>
         *     <tr>
         *         <td>Opto-electronic transfer function (OETF)</td>
         *         <td colspan="4">\(C_{ACEScg} = C_{linear}\)</td>
         *     </tr>
         *     <tr>
         *         <td>Electro-optical transfer function (EOTF)</td>
         *         <td colspan="4">\(C_{linear} = C_{ACEScg}\)</td>
         *     </tr>
         *     <tr><td>Range</td><td colspan="4">\([-65504.0, 65504.0]\)</td></tr>
         * </table>
         *
         * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_acescg.png"></img>
         * <figcaption style="text-align: center;">ACEScg (orange) vs sRGB (white)</figcaption>
         */
        Acescg(ColorSpace.Rgb(
            "Academy S-2014-004 ACEScg",
            floatArrayOf(0.713f, 0.293f, 0.165f, 0.830f, 0.128f, 0.044f),
            IlluminantD60,
            1.0,
            -65504.0f, 65504.0f,
            id = 13
        )),

        /**
         * [XYZ][Model.Xyz] color space CIE XYZ. This color space assumes standard
         * illuminant D50 as its white point.
         *
         * <table summary="Color space definition">
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Generic XYZ</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D50</td></tr>
         *     <tr><td>Range</td><td colspan="4">\([-2.0, 2.0]\)</td></tr>
         * </table>
         */
        CieXyz(Xyz(
            "Generic XYZ",
            id = 14
        )),

        /**
         * [Lab][Model.Lab] color space CIE L*a*b*. This color space uses CIE XYZ D50
         * as a profile conversion space.
         *
         * <table summary="Color space definition">
         *     <tr><th>Property</th><th colspan="4">Value</th></tr>
         *     <tr><td>Name</td><td colspan="4">Generic L*a*b*</td></tr>
         *     <tr><td>CIE standard illuminant</td><td colspan="4">D50</td></tr>
         *     <tr><td>Range</td><td colspan="4">\(L: [0.0, 100.0], a: [-128, 128], b: [-128, 128]\)</td></tr>
         * </table>
         */
        CieLab(ColorSpace.Lab(
            "Generic L*a*b*",
            id = 15
        ));

        // Update the initialization block next to #get(Named) when adding new values

        companion object {
            init {
                // Make sure the IDs match the ordinal index of each element. They aren't known
                // at construction time, so this fix-up corrects that.
                Named.values().forEachIndexed { index, named ->
                    named.colorSpace.id = index
                }
            }

            // See static initialization block next to #get(Named)
            internal val ColorSpaces = Named.values().map { it.colorSpace }.toList()
        }
    }

    /**
     * A render intent determines how a [connector][ColorSpace.Connector]
     * maps colors from one color space to another. The choice of mapping is
     * important when the source color space has a larger color gamut than the
     * destination color space.
     *
     * @see ColorSpace.connect
     */
    enum class RenderIntent {
        /**
         * Compresses the source gamut into the destination gamut.
         * This render intent affects all colors, inside and outside
         * of destination gamut. The goal of this render intent is
         * to preserve the visual relationship between colors.
         *
         * This render intent is currently not
         * implemented and behaves like [Relative].
         */
        Perceptual,

        /**
         * Similar to the [Absolute] render intent, this render
         * intent matches the closest color in the destination gamut
         * but makes adjustments for the destination white point.
         */
        Relative,

        /**
         * Attempts to maintain the relative saturation of colors
         * from the source gamut to the destination gamut, to keep
         * highly saturated colors as saturated as possible.
         *
         * This render intent is currently not
         * implemented and behaves like [Relative].
         */
        Saturation,

        /**
         * Colors that are in the destination gamut are left unchanged.
         * Colors that fall outside of the destination gamut are mapped
         * to the closest possible color within the gamut of the destination
         * color space (they are clipped).
         */
        Absolute
    }

    /**
     * List of adaptation matrices that can be used for chromatic adaptation
     * using the von Kries transform. These matrices are used to convert values
     * in the CIE XYZ space to values in the LMS space (Long Medium Short).
     *
     * Given an adaptation matrix \(A\), the conversion from XYZ to
     * LMS is straightforward:
     *
     * $$\left[ \begin{array}{c} L\\ M\\ S \end{array} \right] =
     * A \left[ \begin{array}{c} X\\ Y\\ Z \end{array} \right]$$
     *
     * The complete von Kries transform \(T\) uses a diagonal matrix
     * noted \(D\) to perform the adaptation in LMS space. In addition
     * to \(A\) and \(D\), the source white point \(W1\) and the destination
     * white point \(W2\) must be specified:
     *
     * $$\begin{align*}
     * \left[ \begin{array}{c} L_1\\ M_1\\ S_1 \end{array} \right] &=
     * A \left[ \begin{array}{c} W1_X\\ W1_Y\\ W1_Z \end{array} \right] \\\
     * \left[ \begin{array}{c} L_2\\ M_2\\ S_2 \end{array} \right] &=
     * A \left[ \begin{array}{c} W2_X\\ W2_Y\\ W2_Z \end{array} \right] \\\
     * D &= \left[ \begin{matrix} \frac{L_2}{L_1} & 0 & 0 \\\
     * 0 & \frac{M_2}{M_1} & 0 \\\
     * 0 & 0 & \frac{S_2}{S_1} \end{matrix} \right] \\\
     * T &= A^{-1}.D.A
     * \end{align*}$$
     *
     * As an example, the resulting matrix \(T\) can then be used to
     * perform the chromatic adaptation of sRGB XYZ transform from D65
     * to D50:
     *
     * $$sRGB_{D50} = T.sRGB_{D65}$$
     *
     * @see ColorSpace.Connector
     * @see ColorSpace.connect
     */
    enum class Adaptation constructor(internal val transform: FloatArray) {
        /**
         * Bradford chromatic adaptation transform, as defined in the
         * CIECAM97s color appearance model.
         */
        Bradford(floatArrayOf(
            0.8951f, -0.7502f, 0.0389f,
            0.2664f, 1.7135f, -0.0685f,
            -0.1614f, 0.0367f, 1.0296f
        )),

        /**
         * von Kries chromatic adaptation transform.
         */
        VonKries(floatArrayOf(
            0.40024f, -0.22630f, 0.00000f,
            0.70760f, 1.16532f, 0.00000f,
            -0.08081f, 0.04570f, 0.91822f
        )),

        /**
         * CIECAT02 chromatic adaption transform, as defined in the
         * CIECAM02 color appearance model.
         */
        Ciecat02(floatArrayOf(
            0.7328f, -0.7036f, 0.0030f,
            0.4296f, 1.6975f, 0.0136f,
            -0.1624f, 0.0061f, 0.9834f
        ))
    }

    /**
     * A color model is required by a [ColorSpace] to describe the
     * way colors can be represented as tuples of numbers. A common color
     * model is the [RGB][Rgb] color model which defines a color
     * as represented by a tuple of 3 numbers (red, green and blue).
     */
    enum class Model constructor(
        /**
         * Returns the number of components for this color model.
         *
         * @return An integer between 1 and 4
         */
        @IntRange(from = 1, to = 4)
        val componentCount: Int
    ) {
        /**
         * The RGB model is a color model with 3 components that
         * refer to the three additive primiaries: red, green
         * andd blue.
         */
        Rgb(3),
        /**
         * The XYZ model is a color model with 3 components that
         * are used to model human color vision on a basic sensory
         * level.
         */
        Xyz(3),
        /**
         * The Lab model is a color model with 3 components used
         * to describe a color space that is more perceptually
         * uniform than XYZ.
         */
        Lab(3),
        /**
         * The CMYK model is a color model with 4 components that
         * refer to four inks used in color printing: cyan, magenta,
         * yellow and black (or key). CMYK is a subtractive color
         * model.
         */
        Cmyk(4)
    }

    init { // ColorSpace init
        if (name.isEmpty()) {
            throw IllegalArgumentException("The name of a color space cannot be null and " +
                    "must contain at least 1 character")
        }

        if (id < MinId || id > MaxId) {
            throw IllegalArgumentException("The id must be between $MinId and $MaxId")
        }
    }

    /**
     * Returns the minimum valid value for the specified component of this
     * color space's color model.
     *
     * @param component The index of the component
     * @return A floating point value less than [getMaxValue]
     *
     * @see getMaxValue
     * @see Model.componentCount
     */
    abstract fun getMinValue(@IntRange(from = 0, to = 3) component: Int): Float

    /**
     * Returns the maximum valid value for the specified component of this
     * color space's color model.
     *
     * @param component The index of the component
     * @return A floating point value greater than [getMinValue]
     *
     * @see getMinValue
     * @see Model.componentCount
     */
    abstract fun getMaxValue(@IntRange(from = 0, to = 3) component: Int): Float

    /**
     * Converts a color value from this color space's model to
     * tristimulus CIE XYZ values. If the color model of this color
     * space is not [RGB][Model.Rgb], it is assumed that the
     * target CIE XYZ space uses a [D50][IlluminantD50]
     * standard illuminant.
     *
     * This method is a convenience for color spaces with a model
     * of 3 components ([RGB][Model.Rgb] or [Model.Lab]
     * for instance). With color spaces using fewer or more components,
     * use [toXyz] instead.
     *
     * @param r The first component of the value to convert from (typically R in RGB)
     * @param g The second component of the value to convert from (typically G in RGB)
     * @param b The third component of the value to convert from (typically B in RGB)
     * @return A new array of 3 floats, containing tristimulus XYZ values
     *
     * @see toXyz
     * @see fromXyz
     */
    @Size(3)
    fun toXyz(r: Float, g: Float, b: Float): FloatArray {
        return toXyz(floatArrayOf(r, g, b))
    }

    /**
     * Converts a color value from this color space's model to
     * tristimulus CIE XYZ values. If the color model of this color
     * space is not [RGB][Model.Rgb], it is assumed that the
     * target CIE XYZ space uses a [D50][IlluminantD50]
     * standard illuminant.
     *
     * The specified array's length  must be at least
     * equal to to the number of color components as returned by
     * [Model.componentCount].
     *
     * @param v An array of color components containing the color space's
     * color value to convert to XYZ, and large enough to hold
     * the resulting tristimulus XYZ values
     * @return The array passed in parameter
     *
     * @see toXyz
     * @see fromXyz
     */
    @Size(min = 3)
    abstract fun toXyz(@Size(min = 3) v: FloatArray): FloatArray

    /**
     * Converts tristimulus values from the CIE XYZ space to this
     * color space's color model.
     *
     * @param x The X component of the color value
     * @param y The Y component of the color value
     * @param z The Z component of the color value
     * @return A new array whose size is equal to the number of color
     * components as returned by [Model.componentCount]
     *
     * @see fromXyz
     * @see toXyz
     */
    @Size(min = 3)
    fun fromXyz(x: Float, y: Float, z: Float): FloatArray {
        val xyz = FloatArray(model.componentCount)
        xyz[0] = x
        xyz[1] = y
        xyz[2] = z
        return fromXyz(xyz)
    }

    /**
     * Converts tristimulus values from the CIE XYZ space to this color
     * space's color model. The resulting value is passed back in the specified
     * array.
     *
     * The specified array's length  must be at least equal to
     * to the number of color components as returned by
     * [Model.componentCount], and its first 3 values must
     * be the XYZ components to convert from.
     *
     * @param v An array of color components containing the XYZ values
     * to convert from, and large enough to hold the number
     * of components of this color space's model
     * @return The array passed in parameter
     *
     * @see fromXyz
     * @see toXyz
     */
    @Size(min = 3)
    abstract fun fromXyz(@Size(min = 3) v: FloatArray): FloatArray

    /**
     * Returns a string representation of the object. This method returns
     * a string equal to the value of:
     *
     *     "$name "(id=$id, model=$model)"
     *
     * For instance, the string representation of the [sRGB][Named.Srgb]
     * color space is equal to the following value:
     *
     *     sRGB IEC61966-2.1 (id=0, model=RGB)
     *
     * @return A string representation of the object
     */
    override fun toString(): String {
        return "$name (id=$id, model=$model)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val that = other as ColorSpace

        if (id != that.id) return false

        return if (name != that.name) false else model == that.model
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + id
        return result
    }

    /**
     * Implementation of the CIE XYZ color space. Assumes the white point is D50.
     */
    private class Xyz internal constructor(
        name: String,
        @IntRange(from = MinId.toLong(), to = MaxId.toLong()) id: Int
    ) : ColorSpace(name, Model.Xyz, id) {

        override val isWideGamut: Boolean
            get() = true

        override fun getMinValue(@IntRange(from = 0, to = 3) component: Int): Float {
            return -2.0f
        }

        override fun getMaxValue(@IntRange(from = 0, to = 3) component: Int): Float {
            return 2.0f
        }

        override fun toXyz(@Size(min = 3) v: FloatArray): FloatArray {
            v[0] = clamp(v[0])
            v[1] = clamp(v[1])
            v[2] = clamp(v[2])
            return v
        }

        override fun fromXyz(@Size(min = 3) v: FloatArray): FloatArray {
            v[0] = clamp(v[0])
            v[1] = clamp(v[1])
            v[2] = clamp(v[2])
            return v
        }

        private fun clamp(x: Float): Float {
            return x.coerceIn(-2f, 2f)
        }
    }

    /**
     * Implementation of the CIE L*a*b* color space. Its PCS is CIE XYZ
     * with a white point of D50.
     */
    @AnyThread
    private class Lab internal constructor(
        name: String,
        @IntRange(from = MinId.toLong(), to = MaxId.toLong()) id: Int
    ) : ColorSpace(name, Model.Lab, id) {

        override val isWideGamut: Boolean
            get() = true

        override fun getMinValue(@IntRange(from = 0, to = 3) component: Int): Float {
            return if (component == 0) 0.0f else -128.0f
        }

        override fun getMaxValue(@IntRange(from = 0, to = 3) component: Int): Float {
            return if (component == 0) 100.0f else 128.0f
        }

        override fun toXyz(@Size(min = 3) v: FloatArray): FloatArray {
            v[0] = v[0].coerceIn(0.0f, 100.0f)
            v[1] = v[1].coerceIn(-128.0f, 128.0f)
            v[2] = v[2].coerceIn(-128.0f, 128.0f)

            val fy = (v[0] + 16.0f) / 116.0f
            val fx = fy + (v[1] * 0.002f)
            val fz = fy - (v[2] * 0.005f)
            val x = if (fx > D) fx * fx * fx else (1.0f / B) * (fx - C)
            val y = if (fy > D) fy * fy * fy else (1.0f / B) * (fy - C)
            val z = if (fz > D) fz * fz * fz else (1.0f / B) * (fz - C)

            v[0] = x * IlluminantD50Xyz[0]
            v[1] = y * IlluminantD50Xyz[1]
            v[2] = z * IlluminantD50Xyz[2]

            return v
        }

        override fun fromXyz(@Size(min = 3) v: FloatArray): FloatArray {
            val x = v[0] / IlluminantD50Xyz[0]
            val y = v[1] / IlluminantD50Xyz[1]
            val z = v[2] / IlluminantD50Xyz[2]

            val fx = if (x > A) x.pow(1f / 3f) else B * x + C
            val fy = if (y > A) y.pow(1f / 3f) else B * y + C
            val fz = if (z > A) z.pow(1f / 3f) else B * z + C

            val l = 116.0f * fy - 16.0f
            val a = 500.0f * (fx - fy)
            val b = 200.0f * (fy - fz)

            v[0] = l.coerceIn(0.0f, 100.0f)
            v[1] = a.coerceIn(-128.0f, 128.0f)
            v[2] = b.coerceIn(-128.0f, 128.0f)

            return v
        }

        companion object {
            private const val A = 216.0f / 24389.0f
            private const val B = 841.0f / 108.0f
            private const val C = 4.0f / 29.0f
            private const val D = 6.0f / 29.0f
        }
    }

    /**
     * {@usesMathJax}
     *
     * An RGB color space is an additive color space using the
     * [RGB][Model.Rgb] color model (a color is therefore represented
     * by a tuple of 3 numbers).
     *
     * A specific RGB color space is defined by the following properties:
     *
     *  * Three chromaticities of the red, green and blue primaries, which
     * define the gamut of the color space.
     *  * A white point chromaticity that defines the stimulus to which
     * color space values are normalized (also just called "white").
     *  * An opto-electronic transfer function, also called opto-electronic
     * conversion function or often, and approximately, gamma function.
     *  * An electro-optical transfer function, also called electo-optical
     * conversion function or often, and approximately, gamma function.
     *  * A range of valid RGB values (most commonly \([0..1]\)).
     *
     * The most commonly used RGB color space is [sRGB][Named.Srgb].
     *
     * ### Primaries and white point chromaticities
     *
     * In this implementation, the chromaticity of the primaries and the white
     * point of an RGB color space is defined in the CIE xyY color space. This
     * color space separates the chromaticity of a color, the x and y components,
     * and its luminance, the Y component. Since the primaries and the white
     * point have full brightness, the Y component is assumed to be 1 and only
     * the x and y components are needed to encode them.
     *
     * For convenience, this implementation also allows to define the
     * primaries and white point in the CIE XYZ space. The tristimulus XYZ values
     * are internally converted to xyY.
     *
     * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_srgb.png"></img>
     * <figcaption style="text-align: center;">sRGB primaries and white point</figcaption>
     *
     * ### Transfer functions
     *
     * A transfer function is a color component conversion function, defined as
     * a single variable, monotonic mathematical function. It is applied to each
     * individual component of a color. They are used to perform the mapping
     * between linear tristimulus values and non-linear electronic signal value.
     *
     * The *opto-electronic transfer function* (OETF or OECF) encodes
     * tristimulus values in a scene to a non-linear electronic signal value.
     * An OETF is often expressed as a power function with an exponent between
     * 0.38 and 0.55 (the reciprocal of 1.8 to 2.6).
     *
     * The *electro-optical transfer function* (EOTF or EOCF) decodes
     * a non-linear electronic signal value to a tristimulus value at the display.
     * An EOTF is often expressed as a power function with an exponent between
     * 1.8 and 2.6.
     *
     * Transfer functions are used as a compression scheme. For instance,
     * linear sRGB values would normally require 11 to 12 bits of precision to
     * store all values that can be perceived by the human eye. When encoding
     * sRGB values using the appropriate OETF (see [sRGB][Named.Srgb] for
     * an exact mathematical description of that OETF), the values can be
     * compressed to only 8 bits precision.
     *
     * When manipulating RGB values, particularly sRGB values, it is safe
     * to assume that these values have been encoded with the appropriate
     * OETF (unless noted otherwise). Encoded values are often said to be in
     * "gamma space". They are therefore defined in a non-linear space. This
     * in turns means that any linear operation applied to these values is
     * going to yield mathematically incorrect results (any linear interpolation
     * such as gradient generation for instance, most image processing functions
     * such as blurs, etc.).
     *
     * To properly process encoded RGB values you must first apply the
     * EOTF to decode the value into linear space. After processing, the RGB
     * value must be encoded back to non-linear ("gamma") space. Here is a
     * formal description of the process, where \(f\) is the processing
     * function to apply:
     *
     * $$RGB_{out} = OETF(f(EOTF(RGB_{in})))$$
     *
     * If the transfer functions of the color space can be expressed as an
     * ICC parametric curve as defined in ICC.1:2004-10, the numeric parameters
     * can be retrieved from [transferParameters]. This can
     * be useful to match color spaces for instance.
     *
     * Some RGB color spaces, such as [Named.Aces] and
     * [scRGB][Named.LinearExtendedSrgb], are said to be linear because
     * their transfer functions are the identity function: \(f(x) = x\).
     * If the source and/or destination are known to be linear, it is not
     * necessary to invoke the transfer functions.
     *
     * ### Range
     *
     * Most RGB color spaces allow RGB values in the range \([0..1]\). There
     * are however a few RGB color spaces that allow much larger ranges. For
     * instance, [scRGB][Named.ExtendedSrgb] is used to manipulate the
     * range \([-0.5..7.5]\) while [ACES][Named.Aces] can be used throughout
     * the range \([-65504, 65504]\).
     *
     * <img style="display: block; margin: 0 auto;" src="{@docRoot}reference/android/images/graphics/colorspace_scrgb.png"></img>
     * <figcaption style="text-align: center;">Extended sRGB and its large range</figcaption>
     *
     * ### Converting between RGB color spaces
     *
     * Conversion between two color spaces is achieved by using an intermediate
     * color space called the profile connection space (PCS). The PCS used by
     * this implementation is CIE XYZ. The conversion operation is defined
     * as such:
     *
     * $$RGB_{out} = OETF(T_{dst}^{-1} \cdot T_{src} \cdot EOTF(RGB_{in}))$$
     *
     * Where \(T_{src}\) is the [RGB to XYZ transform][getTransform]
     * of the source color space and \(T_{dst}^{-1}\) the
     * [XYZ to RGB transform][getInverseTransform] of the destination color space.
     *
     * Many RGB color spaces commonly used with electronic devices use the
     * standard illuminant [D65][IlluminantD65]. Care must be take however
     * when converting between two RGB color spaces if their white points do not
     * match. This can be achieved by either calling
     * [adapt] to adapt one or both color spaces to
     * a single common white point. This can be achieved automatically by calling
     * [ColorSpace.connect], which also handles
     * non-RGB color spaces.
     *
     * To learn more about the white point adaptation process, refer to the
     * documentation of [Adaptation].
     */
    @AnyThread
    class Rgb
    /**
     * Creates a new RGB color space using a specified set of primaries
     * and a specified white point.
     *
     * The primaries and white point can be specified in the CIE xyY space
     * or in CIE XYZ. The length of the arrays depends on the chosen space:
     *
     * <table summary="Parameters length">
     *     <tr><th>Space</th><th>Primaries length</th><th>White point length</th></tr>
     *     <tr><td>xyY</td><td>6</td><td>2</td></tr>
     *     <tr><td>XYZ</td><td>9</td><td>3</td></tr>
     * </table>
     *
     * When the primaries and/or white point are specified in xyY, the Y component
     * does not need to be specified and is assumed to be 1.0. Only the xy components
     * are required.
     *
     * @param name Name of the color space, cannot be null, its length must be >= 1
     * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
     * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
     * @param transform Computed transform matrix that converts from RGB to XYZ, or
     * `null` to compute it from `primaries` and `whitePoint`.
     * @param oetf Opto-electronic transfer function, cannot be null
     * @param eotf Electro-optical transfer function, cannot be null
     * @param min The minimum valid value in this color space's RGB range
     * @param max The maximum valid value in this color space's RGB range
     * @param transferParameters Parameters for the transfer functions
     * @param id ID of this color space as an integer between [MinId] and [MaxId]
     *
     * @throws IllegalArgumentException If any of the following conditions is met:
     *  * The name is null or has a length of 0.
     *  * The primaries array is null or has a length that is neither 6 or 9.
     *  * The white point array is null or has a length that is neither 2 or 3.
     *  * The OETF is null or the EOTF is null.
     *  * The minimum valid value is >= the maximum valid value.
     *  * The ID is not between [MinId] and [MaxId].
     *
     * @see get
     */
    internal constructor(
        @Size(min = 1) name: String,
        @Size(min = 6, max = 9) primaries: FloatArray,
        @Size(min = 2, max = 3) whitePoint: FloatArray,
        @Size(9) transform: FloatArray?,
        oetf: (Double) -> Double,
        eotf: (Double) -> Double,
        private val min: Float,
        private val max: Float,
        /**
         * Returns the parameters used by the [electro-optical][eotf]
         * and [opto-electronic][oetf] transfer functions. If the transfer
         * functions do not match the ICC parametric curves defined in ICC.1:2004-10
         * (section 10.15), this method returns null.
         *
         * See [TransferParameters] for a full description of the transfer
         * functions.
         *
         * @return An instance of [TransferParameters] or null if this color
         * space's transfer functions do not match the equation defined in
         * [TransferParameters]
         */
        val transferParameters: TransferParameters?,
        @IntRange(from = MinId.toLong(), to = MaxId.toLong()) id: Int
    ) : ColorSpace(name, Model.Rgb, id) {

        internal val whitePoint: FloatArray
        internal val primaries: FloatArray
        internal val transform: FloatArray
        internal val inverseTransform: FloatArray
        internal val oetfOrig = oetf

        /**
         * Returns the opto-electronic transfer function (OETF) of this color space.
         * The inverse function is the electro-optical transfer function (EOTF) returned
         * by [eotf]. These functions are defined to satisfy the following
         * equality for \(x \in [0..1]\):
         *
         * $$OETF(EOTF(x)) = EOTF(OETF(x)) = x$$
         *
         * For RGB colors, this function can be used to convert from linear space
         * to "gamma space" (gamma encoded). The terms gamma space and gamma encoded
         * are frequently used because many OETFs can be closely approximated using
         * a simple power function of the form \(x^{\frac{1}{\gamma}}\) (the
         * approximation of the [sRGB][Named.Srgb] OETF uses \(\gamma=2.2\)
         * for instance).
         *
         * @return A transfer function that converts from linear space to "gamma space"
         *
         * @see eotf
         * @see Rgb.transferParameters
         */
        val oetf: (Double) -> Double = { x -> oetfOrig(x).coerceIn(min.toDouble(), max.toDouble()) }

        internal val eotfOrig = eotf

        /**
         * Returns the electro-optical transfer function (EOTF) of this color space.
         * The inverse function is the opto-electronic transfer function (OETF)
         * returned by [getOetf]. These functions are defined to satisfy the
         * following equality for \(x \in [0..1]\):
         *
         * $$OETF(EOTF(x)) = EOTF(OETF(x)) = x$$
         *
         * For RGB colors, this function can be used to convert from "gamma space"
         * (gamma encoded) to linear space. The terms gamma space and gamma encoded
         * are frequently used because many EOTFs can be closely approximated using
         * a simple power function of the form \(x^\gamma\) (the approximation of the
         * [sRGB][Named.Srgb] EOTF uses \(\gamma=2.2\) for instance).
         *
         * @return A transfer function that converts from "gamma space" to linear space
         *
         * @see oetf
         * @see Rgb.transferParameters
         */
        val eotf: (Double) -> Double = { x -> eotfOrig(x.coerceIn(min.toDouble(), max.toDouble())) }

        override val isWideGamut: Boolean
        override val isSrgb: Boolean

        init {
            if (primaries.size != 6 && primaries.size != 9) {
                throw IllegalArgumentException(("The color space's primaries must be " +
                        "defined as an array of 6 floats in xyY or 9 floats in XYZ"))
            }

            if (whitePoint.size != 2 && whitePoint.size != 3) {
                throw IllegalArgumentException(("The color space's white point must be " +
                        "defined as an array of 2 floats in xyY or 3 float in XYZ"))
            }

            if (min >= max) {
                throw IllegalArgumentException("Invalid range: min=$min, max=$max; min must " +
                        "be strictly < max")
            }
            this.whitePoint = xyWhitePoint(whitePoint)
            this.primaries = xyPrimaries(primaries)

            if (transform == null) {
                this.transform = computeXYZMatrix(this.primaries, this.whitePoint)
            } else {
                if (transform.size != 9) {
                    throw IllegalArgumentException(("Transform must have 9 entries! Has " +
                            "${transform.size}"))
                }
                this.transform = transform
            }
            inverseTransform = inverse3x3(this.transform)

            // A color space is wide-gamut if its area is >90% of NTSC 1953 and
            // if it entirely contains the Color space definition in xyY
            isWideGamut = isWideGamut(this.primaries, min, max)
            isSrgb = isSrgb(this.primaries, this.whitePoint, oetf, eotf, min, max, id)
        }

        /**
         * Returns the non-adapted CIE xyY white point of this color space as
         * a new array of 2 floats. The Y component is assumed to be 1 and is
         * therefore not copied into the destination. The x and y components
         * are written in the array at positions 0 and 1 respectively.
         *
         * @return A new non-null array of 2 floats
         */
        @Size(2)
        fun getWhitePoint(): FloatArray = whitePoint.clone()

        /**
         * Returns the primaries of this color space as a new array of 6 floats.
         * The Y component is assumed to be 1 and is therefore not copied into
         * the destination. The x and y components of the first primary are
         * written in the array at positions 0 and 1 respectively.
         *
         * @return A new non-null array of 2 floats
         *
         * @see getWhitePoint
         */
        @Size(6)
        fun getPrimaries(): FloatArray = primaries.clone()

        /**
         * Returns the transform of this color space as a new array. The
         * transform is used to convert from RGB to XYZ (with the same white
         * point as this color space). To connect color spaces, you must first
         * [adapt][ColorSpace.adapt] them to the
         * same white point.
         *
         * It is recommended to use [ColorSpace.connect]
         * to convert between color spaces.
         *
         * @return A new array of 9 floats
         *
         * @see getInverseTransform
         */
        @Size(9)
        fun getTransform(): FloatArray = transform.clone()

        /**
         * Returns the inverse transform of this color space as a new array.
         * The inverse transform is used to convert from XYZ to RGB (with the
         * same white point as this color space). To connect color spaces, you
         * must first [adapt][ColorSpace.adapt] them
         * to the same white point.
         *
         * It is recommended to use [ColorSpace.connect]
         * to convert between color spaces.
         *
         * @return A new array of 9 floats
         *
         * @see getTransform
         */
        @Size(9)
        fun getInverseTransform(): FloatArray = inverseTransform.clone()

        /**
         * {@usesMathJax}
         *
         * Defines the parameters for the ICC parametric curve type 4, as
         * defined in ICC.1:2004-10, section 10.15.
         *
         * The EOTF is of the form:
         *
         * \(\begin{equation}
         * Y = \begin{cases}c X + f & X \lt d \\\
         * \left( a X + b \right) ^{g} + e & X \ge d \end{cases}
         * \end{equation}\)
         *
         * The corresponding OETF is simply the inverse function.
         *
         * The parameters defined by this class form a valid transfer
         * function only if all the following conditions are met:
         *
         *  * No parameter is a [Not-a-Number][Double.isNaN]
         *  * \(d\) is in the range \([0..1]\)
         *  * The function is not constant
         *  * The function is positive and increasing
         */
        class TransferParameters(
            /** Value \(a\) in the equation of the EOTF described above.  */
            val a: Double,
            /** Value \(b\) in the equation of the EOTF described above.  */
            val b: Double,
            /** Value \(c\) in the equation of the EOTF described above.  */
            val c: Double,
            /** Value \(d\) in the equation of the EOTF described above.  */
            val d: Double,
            /** Value \(e\) in the equation of the EOTF described above.  */
            val e: Double,
            /** Value \(f\) in the equation of the EOTF described above.  */
            val f: Double,
            /** Value \(g\) in the equation of the EOTF described above.  */
            val g: Double
        ) {
            /**
             * Defines the parameters for the ICC parametric curve type 3, as
             * defined in ICC.1:2004-10, section 10.15.
             *
             * The EOTF is of the form:
             *
             * \(\begin{equation}
             * Y = \begin{cases}c X & X \lt d \\\
             * \left( a X + b \right) ^{g} & X \ge d \end{cases}
             * \end{equation}\)
             *
             * This constructor is equivalent to setting  \(e\) and \(f\) to 0.
             *
             * @param a The value of \(a\) in the equation of the EOTF described above
             * @param b The value of \(b\) in the equation of the EOTF described above
             * @param c The value of \(c\) in the equation of the EOTF described above
             * @param d The value of \(d\) in the equation of the EOTF described above
             * @param g The value of \(g\) in the equation of the EOTF described above
             *
             * @throws IllegalArgumentException If the parameters form an invalid transfer function
             */
            constructor(a: Double, b: Double, c: Double, d: Double, g: Double) :
                    this(a, b, c, d, 0.0, 0.0, g)

            init {
                if (a.isNaN() || b.isNaN() || c.isNaN() || d.isNaN() || e.isNaN() || f.isNaN() ||
                    g.isNaN()) {
                    throw IllegalArgumentException("Parameters cannot be NaN")
                }

                // Next representable float after 1.0
                // We use doubles here but the representation inside our native code is often floats
                if (!(d >= 0.0 && d <= 1.0f + 1f.ulp)) {
                    throw IllegalArgumentException("Parameter d must be in the range [0..1], was " +
                            "$d")
                }

                if (d == 0.0 && (a == 0.0 || g == 0.0)) {
                    throw IllegalArgumentException(
                        "Parameter a or g is zero, the transfer function is constant"
                    )
                }

                if (d >= 1.0 && c == 0.0) {
                    throw IllegalArgumentException(
                        "Parameter c is zero, the transfer function is constant"
                    )
                }

                if ((a == 0.0 || g == 0.0) && c == 0.0) {
                    throw IllegalArgumentException("Parameter a or g is zero," +
                            " and c is zero, the transfer function is constant")
                }

                if (c < 0.0) {
                    throw IllegalArgumentException("The transfer function must be increasing")
                }

                if (a < 0.0 || g < 0.0) {
                    throw IllegalArgumentException(("The transfer function must be " +
                            "positive or increasing"))
                }
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) {
                    return true
                }
                if (other == null || javaClass != other.javaClass) {
                    return false
                }

                val that = other as TransferParameters

                if (that.a.compareTo(a) != 0) return false
                if (that.b.compareTo(b) != 0) return false
                if (that.c.compareTo(c) != 0) return false
                if (that.d.compareTo(d) != 0) return false
                if (that.e.compareTo(e) != 0) return false
                return if (that.f.compareTo(f) != 0) false else that.g.compareTo(g) == 0
            }

            override fun hashCode(): Int {
                var temp: Long = a.toBits()
                var result: Int = (temp xor (temp.ushr(32))).toInt()
                temp = b.toBits()
                result = 31 * result + (temp xor (temp.ushr(32))).toInt()
                temp = c.toBits()
                result = 31 * result + (temp xor (temp.ushr(32))).toInt()
                temp = d.toBits()
                result = 31 * result + (temp xor (temp.ushr(32))).toInt()
                temp = e.toBits()
                result = 31 * result + (temp xor (temp.ushr(32))).toInt()
                temp = f.toBits()
                result = 31 * result + (temp xor (temp.ushr(32))).toInt()
                temp = g.toBits()
                result = 31 * result + (temp xor (temp.ushr(32))).toInt()
                return result
            }
        }

        /**
         * Creates a new RGB color space using a 3x3 column-major transform matrix.
         * The transform matrix must convert from the RGB space to the profile connection
         * space CIE XYZ.
         *
         * The range of the color space is imposed to be \([0..1]\).
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param toXYZ 3x3 column-major transform matrix from RGB to the profile
         * connection space CIE XYZ as an array of 9 floats, cannot be null
         * @param oetf Opto-electronic transfer function, cannot be null
         * @param eotf Electro-optical transfer function, cannot be null
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * The OETF is null or the EOTF is null.
         *  * The minimum valid value is >= the maximum valid value.
         *
         * @see get
         */
        constructor(
            @Size(min = 1) name: String,
            @Size(9) toXYZ: FloatArray,
            oetf: (Double) -> Double,
            eotf: (Double) -> Double
        ) : this(
            name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), null,
            oetf, eotf, 0.0f, 1.0f, null, MinId
        )

        /**
         * Creates a new RGB color space using a specified set of primaries
         * and a specified white point.
         *
         * The primaries and white point can be specified in the CIE xyY space
         * or in CIE XYZ. The length of the arrays depends on the chosen space:
         *
         * <table summary="Parameters length">
         *     <tr><th>Space</th><th>Primaries length</th><th>White point length</th></tr>
         *     <tr><td>xyY</td><td>6</td><td>2</td></tr>
         *     <tr><td>XYZ</td><td>9</td><td>3</td></tr>
         * </table>
         *
         * When the primaries and/or white point are specified in xyY, the Y component
         * does not need to be specified and is assumed to be 1.0. Only the xy components
         * are required.
         *
         * The ID, returned by [id], of an object created by
         * this constructor is always [MinId].
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
         * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
         * @param oetf Opto-electronic transfer function, cannot be null
         * @param eotf Electro-optical transfer function, cannot be null
         * @param min The minimum valid value in this color space's RGB range
         * @param max The maximum valid value in this color space's RGB range
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * The primaries array is null or has a length that is neither 6 or 9.
         *  * The white point array is null or has a length that is neither 2 or 3.
         *  * The OETF is null or the EOTF is null.
         *  * The minimum valid value is >= the maximum valid value.
         *
         * @see get
         */
        constructor(
            @Size(min = 1) name: String,
            @Size(min = 6, max = 9) primaries: FloatArray,
            @Size(min = 2, max = 3) whitePoint: FloatArray,
            oetf: (Double) -> Double,
            eotf: (Double) -> Double,
            min: Float,
            max: Float
        ) : this(name, primaries, whitePoint, null, oetf, eotf, min, max, null, MinId)

        /**
         * Creates a new RGB color space using a 3x3 column-major transform matrix.
         * The transform matrix must convert from the RGB space to the profile connection
         * space CIE XYZ.
         *
         * The range of the color space is imposed to be \([0..1]\).
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param toXYZ 3x3 column-major transform matrix from RGB to the profile
         * connection space CIE XYZ as an array of 9 floats, cannot be null
         * @param function Parameters for the transfer functions
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * Gamma is negative.
         *
         * @see get
         */
        constructor(
            @Size(min = 1) name: String,
            @Size(9) toXYZ: FloatArray,
            function: TransferParameters
        ) : this(name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), function, MinId)

        /**
         * Creates a new RGB color space using a specified set of primaries
         * and a specified white point.
         *
         * The primaries and white point can be specified in the CIE xyY space
         * or in CIE XYZ. The length of the arrays depends on the chosen space:
         *
         * <table summary="Parameters length">
         *     <tr><th>Space</th><th>Primaries length</th><th>White point length</th></tr>
         *     <tr><td>xyY</td><td>6</td><td>2</td></tr>
         *     <tr><td>XYZ</td><td>9</td><td>3</td></tr>
         * </table>
         *
         * When the primaries and/or white point are specified in xyY, the Y component
         * does not need to be specified and is assumed to be 1.0. Only the xy components
         * are required.
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
         * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
         * @param function Parameters for the transfer functions
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * The primaries array is null or has a length that is neither 6 or 9.
         *  * The white point array is null or has a length that is neither 2 or 3.
         *  * The transfer parameters are invalid.
         *
         * @see get
         */
        constructor(
            @Size(min = 1) name: String,
            @Size(min = 6, max = 9) primaries: FloatArray,
            @Size(min = 2, max = 3) whitePoint: FloatArray,
            function: TransferParameters
        ) : this(name, primaries, whitePoint, function, MinId)

        /**
         * Creates a new RGB color space using a specified set of primaries
         * and a specified white point.
         *
         * The primaries and white point can be specified in the CIE xyY space
         * or in CIE XYZ. The length of the arrays depends on the chosen space:
         *
         * <table summary="Parameters length">
         *     <tr><th>Space</th><th>Primaries length</th><th>White point length</th></tr>
         *     <tr><td>xyY</td><td>6</td><td>2</td></tr>
         *     <tr><td>XYZ</td><td>9</td><td>3</td></tr>
         * </table>
         *
         * When the primaries and/or white point are specified in xyY, the Y component
         * does not need to be specified and is assumed to be 1.0. Only the xy components
         * are required.
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
         * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
         * @param function Parameters for the transfer functions
         * @param id ID of this color space as an integer between [MinId] and [MaxId]
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * The primaries array is null or has a length that is neither 6 or 9.
         *  * The white point array is null or has a length that is neither 2 or 3.
         *  * The ID is not between [MinId] and [MaxId].
         *  * The transfer parameters are invalid.
         *
         * @see get
         */
        internal constructor(
            @Size(min = 1) name: String,
            @Size(min = 6, max = 9) primaries: FloatArray,
            @Size(min = 2, max = 3) whitePoint: FloatArray,
            function: TransferParameters,
            @IntRange(from = MinId.toLong(), to = MaxId.toLong()) id: Int
        ) : this(name, primaries, whitePoint, null,
            if (function.e == 0.0 && function.f == 0.0) { x ->
                rcpResponse(x, function.a, function.b, function.c, function.d, function.g)
            } else { x ->
                rcpResponse(
                    x, function.a, function.b, function.c, function.d, function.e,
                    function.f, function.g
                )
            },
            if (function.e == 0.0 && function.f == 0.0) { x ->
                response(x, function.a, function.b, function.c, function.d, function.g)
            } else { x ->
                response(
                    x, function.a, function.b, function.c, function.d, function.e,
                    function.f, function.g
                )
            },
            0.0f, 1.0f, function, id
        )

        /**
         * Creates a new RGB color space using a 3x3 column-major transform matrix.
         * The transform matrix must convert from the RGB space to the profile connection
         * space CIE XYZ.
         *
         * The range of the color space is imposed to be \([0..1]\).
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param toXYZ 3x3 column-major transform matrix from RGB to the profile
         * connection space CIE XYZ as an array of 9 floats, cannot be null
         * @param gamma Gamma to use as the transfer function
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * Gamma is negative.
         *
         * @see get
         */
        constructor(
            @Size(min = 1) name: String,
            @Size(9) toXYZ: FloatArray,
            gamma: Double
        ) : this(name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), gamma, 0.0f, 1.0f, MinId)

        /**
         * Creates a new RGB color space using a specified set of primaries
         * and a specified white point.
         *
         * The primaries and white point can be specified in the CIE xyY space
         * or in CIE XYZ. The length of the arrays depends on the chosen space:
         *
         * <table summary="Parameters length">
         *     <tr><th>Space</th><th>Primaries length</th><th>White point length</th></tr>
         *     <tr><td>xyY</td><td>6</td><td>2</td></tr>
         *     <tr><td>XYZ</td><td>9</td><td>3</td></tr>
         * </table>
         *
         * When the primaries and/or white point are specified in xyY, the Y component
         * does not need to be specified and is assumed to be 1.0. Only the xy components
         * are required.
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
         * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
         * @param gamma Gamma to use as the transfer function
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * The primaries array is null or has a length that is neither 6 or 9.
         *  * The white point array is null or has a length that is neither 2 or 3.
         *  * Gamma is negative.
         *
         * @see get
         */
        constructor(
            @Size(min = 1) name: String,
            @Size(min = 6, max = 9) primaries: FloatArray,
            @Size(min = 2, max = 3) whitePoint: FloatArray,
            gamma: Double
        ) : this(name, primaries, whitePoint, gamma, 0.0f, 1.0f, MinId)

        /**
         * Creates a new RGB color space using a specified set of primaries
         * and a specified white point.
         *
         * The primaries and white point can be specified in the CIE xyY space
         * or in CIE XYZ. The length of the arrays depends on the chosen space:
         *
         * <table summary="Parameters length">
         *     <tr><th>Space</th><th>Primaries length</th><th>White point length</th></tr>
         *     <tr><td>xyY</td><td>6</td><td>2</td></tr>
         *     <tr><td>XYZ</td><td>9</td><td>3</td></tr>
         * </table>
         *
         * When the primaries and/or white point are specified in xyY, the Y component
         * does not need to be specified and is assumed to be 1.0. Only the xy components
         * are required.
         *
         * @param name Name of the color space, cannot be null, its length must be >= 1
         * @param primaries RGB primaries as an array of 6 (xy) or 9 (XYZ) floats
         * @param whitePoint Reference white as an array of 2 (xy) or 3 (XYZ) floats
         * @param gamma Gamma to use as the transfer function
         * @param min The minimum valid value in this color space's RGB range
         * @param max The maximum valid value in this color space's RGB range
         * @param id ID of this color space as an integer between [MinId] and [MaxId]
         *
         * @throws IllegalArgumentException If any of the following conditions is met:
         *  * The name is null or has a length of 0.
         *  * The primaries array is null or has a length that is neither 6 or 9.
         *  * The white point array is null or has a length that is neither 2 or 3.
         *  * The minimum valid value is >= the maximum valid value.
         *  * The ID is not between [MinId] and [MaxId].
         *  * Gamma is negative.
         *
         * @see get
         */
        internal constructor(
            @Size(min = 1) name: String,
            @Size(min = 6, max = 9) primaries: FloatArray,
            @Size(min = 2, max = 3) whitePoint: FloatArray,
            gamma: Double,
            min: Float,
            max: Float,
            @IntRange(from = MinId.toLong(), to = MaxId.toLong()) id: Int
        ) : this(
            name, primaries, whitePoint, null,
            if (gamma == 1.0) DoubleIdentity
            else { x -> (if (x < 0.0) 0.0 else x).pow(1.0 / gamma) },
            if (gamma == 1.0) DoubleIdentity
            else { x -> (if (x < 0.0) 0.0 else x).pow(gamma) },
            min, max, TransferParameters(1.0, 0.0, 0.0, 0.0, gamma), id
        )

        /**
         * Creates a copy of the specified color space with a new transform.
         *
         * @param colorSpace The color space to create a copy of
         */
        internal constructor(
            colorSpace: Rgb,
            @Size(9) transform: FloatArray,
            @Size(min = 2, max = 3) whitePoint: FloatArray
        ) : this(
            colorSpace.name, colorSpace.primaries, whitePoint, transform,
            colorSpace.oetfOrig, colorSpace.eotfOrig, colorSpace.min, colorSpace.max,
            colorSpace.transferParameters, MinId
        )

        /**
         * Copies the non-adapted CIE xyY white point of this color space in
         * specified array. The Y component is assumed to be 1 and is therefore
         * not copied into the destination. The x and y components are written
         * in the array at positions 0 and 1 respectively.
         *
         * @param whitePoint The destination array, cannot be null, its length
         * must be >= 2
         *
         * @return The destination array passed as a parameter
         *
         * @see getWhitePoint
         */
        @Size(min = 2)
        fun getWhitePoint(@Size(min = 2) whitePoint: FloatArray): FloatArray {
            whitePoint[0] = this.whitePoint[0]
            whitePoint[1] = this.whitePoint[1]
            return whitePoint
        }

        /**
         * Copies the primaries of this color space in specified array. The Y
         * component is assumed to be 1 and is therefore not copied into the
         * destination. The x and y components of the first primary are written
         * in the array at positions 0 and 1 respectively.
         *
         * @param primaries The destination array, cannot be null, its length
         * must be >= 6
         *
         * @return The destination array passed as a parameter
         *
         * @see getPrimaries
         */
        @Size(min = 6)
        fun getPrimaries(@Size(min = 6) primaries: FloatArray): FloatArray {
            return this.primaries.copyInto(primaries)
        }

        /**
         * Copies the transform of this color space in specified array. The
         * transform is used to convert from RGB to XYZ (with the same white
         * point as this color space). To connect color spaces, you must first
         * [adapt][ColorSpace.adapt] them to the
         * same white point.
         *
         * It is recommended to use [ColorSpace.connect]
         * to convert between color spaces.
         *
         * @param transform The destination array, cannot be null, its length
         * must be >= 9
         *
         * @return The destination array passed as a parameter
         *
         * @see getInverseTransform
         */
        @Size(min = 9)
        fun getTransform(@Size(min = 9) transform: FloatArray): FloatArray {
            return this.transform.copyInto(transform)
        }

        /**
         * Copies the inverse transform of this color space in specified array.
         * The inverse transform is used to convert from XYZ to RGB (with the
         * same white point as this color space). To connect color spaces, you
         * must first [adapt][ColorSpace.adapt] them
         * to the same white point.
         *
         * It is recommended to use [ColorSpace.connect]
         * to convert between color spaces.
         *
         * @param inverseTransform The destination array, cannot be null, its length
         * must be >= 9
         *
         * @return The destination array passed as a parameter
         *
         * @see getTransform
         */
        @Size(min = 9)
        fun getInverseTransform(@Size(min = 9) inverseTransform: FloatArray): FloatArray {
            return this.inverseTransform.copyInto(inverseTransform)
        }

        override fun getMinValue(component: Int): Float {
            return min
        }

        override fun getMaxValue(component: Int): Float {
            return max
        }

        /**
         * Decodes an RGB value to linear space. This is achieved by
         * applying this color space's electro-optical transfer function
         * to the supplied values.
         *
         * Refer to the documentation of [ColorSpace.Rgb] for
         * more information about transfer functions and their use for
         * encoding and decoding RGB values.
         *
         * @param r The red component to decode to linear space
         * @param g The green component to decode to linear space
         * @param b The blue component to decode to linear space
         * @return A new array of 3 floats containing linear RGB values
         *
         * @see toLinear
         * @see fromLinear
         */
        @Size(3)
        fun toLinear(r: Float, g: Float, b: Float): FloatArray {
            return toLinear(floatArrayOf(r, g, b))
        }

        /**
         * Decodes an RGB value to linear space. This is achieved by
         * applying this color space's electro-optical transfer function
         * to the first 3 values of the supplied array. The result is
         * stored back in the input array.
         *
         * Refer to the documentation of [ColorSpace.Rgb] for
         * more information about transfer functions and their use for
         * encoding and decoding RGB values.
         *
         * @param v A non-null array of non-linear RGB values, its length
         * must be at least 3
         * @return The specified array
         *
         * @see toLinear
         * @see fromLinear
         */
        @Size(min = 3)
        fun toLinear(@Size(min = 3) v: FloatArray): FloatArray {
            v[0] = eotf(v[0].toDouble()).toFloat()
            v[1] = eotf(v[1].toDouble()).toFloat()
            v[2] = eotf(v[2].toDouble()).toFloat()
            return v
        }

        /**
         * Encodes an RGB value from linear space to this color space's
         * "gamma space". This is achieved by applying this color space's
         * opto-electronic transfer function to the supplied values.
         *
         * Refer to the documentation of [ColorSpace.Rgb] for
         * more information about transfer functions and their use for
         * encoding and decoding RGB values.
         *
         * @param r The red component to encode from linear space
         * @param g The green component to encode from linear space
         * @param b The blue component to encode from linear space
         * @return A new array of 3 floats containing non-linear RGB values
         *
         * @see fromLinear
         * @see toLinear
         */
        @Size(3)
        fun fromLinear(r: Float, g: Float, b: Float): FloatArray {
            return fromLinear(floatArrayOf(r, g, b))
        }

        /**
         * Encodes an RGB value from linear space to this color space's
         * "gamma space". This is achieved by applying this color space's
         * opto-electronic transfer function to the first 3 values of the
         * supplied array. The result is stored back in the input array.
         *
         * Refer to the documentation of [ColorSpace.Rgb] for
         * more information about transfer functions and their use for
         * encoding and decoding RGB values.
         *
         * @param v A non-null array of linear RGB values, its length
         * must be at least 3
         * @return A new array of 3 floats containing non-linear RGB values
         *
         * @see fromLinear
         * @see toLinear
         */
        @Size(min = 3)
        fun fromLinear(@Size(min = 3) v: FloatArray): FloatArray {
            v[0] = oetf(v[0].toDouble()).toFloat()
            v[1] = oetf(v[1].toDouble()).toFloat()
            v[2] = oetf(v[2].toDouble()).toFloat()
            return v
        }

        @Size(min = 3)
        override fun toXyz(@Size(min = 3) v: FloatArray): FloatArray {
            v[0] = eotf(v[0].toDouble()).toFloat()
            v[1] = eotf(v[1].toDouble()).toFloat()
            v[2] = eotf(v[2].toDouble()).toFloat()
            return mul3x3Float3(transform, v)
        }

        @Size(min = 3)
        override fun fromXyz(@Size(min = 3) v: FloatArray): FloatArray {
            mul3x3Float3(inverseTransform, v)
            v[0] = oetf(v[0].toDouble()).toFloat()
            v[1] = oetf(v[1].toDouble()).toFloat()
            v[2] = oetf(v[2].toDouble()).toFloat()
            return v
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            if (!super.equals(other)) return false

            val rgb = other as Rgb

            if (rgb.min.compareTo(min) != 0) return false
            if (rgb.max.compareTo(max) != 0) return false
            if (!(whitePoint contentEquals rgb.whitePoint)) return false
            if (!(primaries contentEquals rgb.primaries)) return false
            if (transferParameters != null) {
                return transferParameters == rgb.transferParameters
            } else if (rgb.transferParameters == null) {
                return true
            }

            return if (oetfOrig != rgb.oetfOrig) false else eotfOrig == rgb.eotfOrig
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + whitePoint.contentHashCode()
            result = 31 * result + primaries.contentHashCode()
            result = 31 * result + (if (min != +0.0f) min.toBits() else 0)
            result = 31 * result + (if (max != +0.0f) max.toBits() else 0)
            result = (31 * result +
                    if (transferParameters != null) transferParameters.hashCode() else 0)
            if (transferParameters == null) {
                result = 31 * result + oetfOrig.hashCode()
                result = 31 * result + eotfOrig.hashCode()
            }
            return result
        }

        companion object {
            private val DoubleIdentity: (Double) -> Double = { d -> d }

            /**
             * Computes whether a color space is the sRGB color space or at least
             * a close approximation.
             *
             * @param primaries The set of RGB primaries in xyY as an array of 6 floats
             * @param whitePoint The white point in xyY as an array of 2 floats
             * @param OETF The opto-electronic transfer function
             * @param EOTF The electro-optical transfer function
             * @param min The minimum value of the color space's range
             * @param max The minimum value of the color space's range
             * @param id The ID of the color space
             * @return True if the color space can be considered as the sRGB color space
             *
             * @see isSrgb
             */
            internal fun isSrgb(
                @Size(6) primaries: FloatArray,
                @Size(2) whitePoint: FloatArray,
                OETF: (Double) -> Double,
                EOTF: (Double) -> Double,
                min: Float,
                max: Float,
                @IntRange(from = MinId.toLong(), to = MaxId.toLong()) id: Int
            ): Boolean {
                if (id == 0) return true
                if (!ColorSpace.compare(primaries, SrgbPrimaries)) {
                    return false
                }
                if (!ColorSpace.compare(whitePoint, IlluminantD65)) {
                    return false
                }

                if (min != 0.0f) return false
                if (max != 1.0f) return false

                // We would have already returned true if this was SRGB itself, so
                // it is safe to reference it here.
                val srgb = Named.Srgb.colorSpace as ColorSpace.Rgb

                var x = 0.0
                while (x <= 1.0) {
                    if (!compare(x, OETF, srgb.oetfOrig)) return false
                    if (!compare(x, EOTF, srgb.eotfOrig)) return false
                    x += 1 / 255.0
                }

                return true
            }

            private fun compare(
                point: Double,
                a: (Double) -> Double,
                b: (Double) -> Double
            ): Boolean {
                val rA = a(point)
                val rB = b(point)
                return abs(rA - rB) <= 1e-3
            }

            /**
             * Computes whether the specified CIE xyY or XYZ primaries (with Y set to 1) form
             * a wide color gamut. A color gamut is considered wide if its area is &gt; 90%
             * of the area of NTSC 1953 and if it contains the sRGB color gamut entirely.
             * If the conditions above are not met, the color space is considered as having
             * a wide color gamut if its range is larger than [0..1].
             *
             * @param primaries RGB primaries in CIE xyY as an array of 6 floats
             * @param min The minimum value of the color space's range
             * @param max The minimum value of the color space's range
             * @return True if the color space has a wide gamut, false otherwise
             *
             * @see isWideGamut
             * @see area
             */
            internal fun isWideGamut(
                @Size(6) primaries: FloatArray,
                min: Float,
                max: Float
            ): Boolean {
                return (((area(primaries) / area(Ntsc1953Primaries) > 0.9f && contains(
                    primaries,
                    SrgbPrimaries
                ))) || (min < 0.0f && max > 1.0f))
            }

            /**
             * Computes the area of the triangle represented by a set of RGB primaries
             * in the CIE xyY space.
             *
             * @param primaries The triangle's vertices, as RGB primaries in an array of 6 floats
             * @return The area of the triangle
             *
             * @see isWideGamut
             */
            private fun area(@Size(6) primaries: FloatArray): Float {
                val rx = primaries[0]
                val ry = primaries[1]
                val gx = primaries[2]
                val gy = primaries[3]
                val bx = primaries[4]
                val by = primaries[5]
                val det = rx * gy + ry * bx + gx * by - gy * bx - ry * gx - rx * by
                val r = 0.5f * det
                return if (r < 0.0f) -r else r
            }

            /**
             * Computes the cross product of two 2D vectors.
             *
             * @param ax The x coordinate of the first vector
             * @param ay The y coordinate of the first vector
             * @param bx The x coordinate of the second vector
             * @param by The y coordinate of the second vector
             * @return The result of a x b
             */
            private fun cross(ax: Float, ay: Float, bx: Float, by: Float): Float {
                return ax * by - ay * bx
            }

            /**
             * Decides whether a 2D triangle, identified by the 6 coordinates of its
             * 3 vertices, is contained within another 2D triangle, also identified
             * by the 6 coordinates of its 3 vertices.
             *
             * In the illustration below, we want to test whether the RGB triangle
             * is contained within the triangle XYZ formed by the 3 vertices at
             * the "+" locations.
             *
             *
             *                                     Y     .
             *                                 .   +    .
             *                                  .     ..
             *                                   .   .
             *                                    . .
             *                                     .  G
             *                                     *
             *                                    * *
             *                                  **   *
             *                                 *      **
             *                                *         *
             *                              **           *
             *                             *              *
             *                            *                *
             *                          **                  *
             *                         *                     *
             *                        *                       **
             *                      **                          *   R    ...
             *                     *                             *  .....
             *                    *                         ***** ..
             *                  **              ************       .   +
             *              B  *    ************                    .   X
             *           ......*****                                 .
             *     ......    .                                        .
             *             ..
             *        +   .
             *      Z    .
             *
             * RGB is contained within XYZ if all the following conditions are true
             * (with "x" the cross product operator):
             *
             *   -->  -->
             *   GR x RX >= 0
             *   -->  -->
             *   RX x BR >= 0
             *   -->  -->
             *   RG x GY >= 0
             *   -->  -->
             *   GY x RG >= 0
             *   -->  -->
             *   RB x BZ >= 0
             *   -->  -->
             *   BZ x GB >= 0
             *
             * @param p1 The enclosing triangle
             * @param p2 The enclosed triangle
             * @return True if the triangle p1 contains the triangle p2
             *
             * @see isWideGamut
             */
            private fun contains(@Size(6) p1: FloatArray, @Size(6) p2: FloatArray): Boolean {
                // Translate the vertices p1 in the coordinates system
                // with the vertices p2 as the origin
                val p0 = floatArrayOf(
                    p1[0] - p2[0], p1[1] - p2[1],
                    p1[2] - p2[2], p1[3] - p2[3],
                    p1[4] - p2[4], p1[5] - p2[5]
                )
                // Check the first vertex of p1
                if ((cross(p0[0], p0[1], p2[0] - p2[4], p2[1] - p2[5]) < 0 ||
                            cross(p2[0] - p2[2], p2[1] - p2[3], p0[0], p0[1]) < 0)) {
                    return false
                }
                // Check the second vertex of p1
                if ((cross(p0[2], p0[3], p2[2] - p2[0], p2[3] - p2[1]) < 0 ||
                            cross(p2[2] - p2[4], p2[3] - p2[5], p0[2], p0[3]) < 0)) {
                    return false
                }
                // Check the third vertex of p1
                return !(cross(p0[4], p0[5], p2[4] - p2[2], p2[5] - p2[3]) < 0 ||
                        cross(p2[4] - p2[0], p2[5] - p2[1], p0[4], p0[5]) < 0)
            }

            /**
             * Computes the primaries  of a color space identified only by
             * its RGB->XYZ transform matrix. This method assumes that the
             * range of the color space is [0..1].
             *
             * @param toXYZ The color space's 3x3 transform matrix to XYZ
             * @return A new array of 6 floats containing the color space's
             * primaries in CIE xyY
             */
            @Size(6)
            internal fun computePrimaries(@Size(9) toXYZ: FloatArray): FloatArray {
                val r = mul3x3Float3(toXYZ, floatArrayOf(1.0f, 0.0f, 0.0f))
                val g = mul3x3Float3(toXYZ, floatArrayOf(0.0f, 1.0f, 0.0f))
                val b = mul3x3Float3(toXYZ, floatArrayOf(0.0f, 0.0f, 1.0f))

                val rSum = r[0] + r[1] + r[2]
                val gSum = g[0] + g[1] + g[2]
                val bSum = b[0] + b[1] + b[2]

                return floatArrayOf(
                    r[0] / rSum, r[1] / rSum,
                    g[0] / gSum, g[1] / gSum,
                    b[0] / bSum, b[1] / bSum
                )
            }

            /**
             * Computes the white point of a color space identified only by
             * its RGB->XYZ transform matrix. This method assumes that the
             * range of the color space is [0..1].
             *
             * @param toXYZ The color space's 3x3 transform matrix to XYZ
             * @return A new array of 2 floats containing the color space's
             * white point in CIE xyY
             */
            @Size(2)
            internal fun computeWhitePoint(@Size(9) toXYZ: FloatArray): FloatArray {
                val w = mul3x3Float3(toXYZ, floatArrayOf(1.0f, 1.0f, 1.0f))
                val sum = w[0] + w[1] + w[2]
                return floatArrayOf(w[0] / sum, w[1] / sum)
            }

            /**
             * Converts the specified RGB primaries point to xyY if needed. The primaries
             * can be specified as an array of 6 floats (in CIE xyY) or 9 floats
             * (in CIE XYZ). If no conversion is needed, the input array is copied.
             *
             * @param primaries The primaries in xyY or XYZ
             * @return A new array of 6 floats containing the primaries in xyY
             */
            @Size(6)
            internal fun xyPrimaries(
                @Size(min = 6, max = 9) primaries: FloatArray
            ): FloatArray {
                val xyPrimaries = FloatArray(6)

                // XYZ to xyY
                if (primaries.size == 9) {
                    var sum: Float = primaries[0] + primaries[1] + primaries[2]
                    xyPrimaries[0] = primaries[0] / sum
                    xyPrimaries[1] = primaries[1] / sum

                    sum = primaries[3] + primaries[4] + primaries[5]
                    xyPrimaries[2] = primaries[3] / sum
                    xyPrimaries[3] = primaries[4] / sum

                    sum = primaries[6] + primaries[7] + primaries[8]
                    xyPrimaries[4] = primaries[6] / sum
                    xyPrimaries[5] = primaries[7] / sum
                } else {
                    primaries.copyInto(xyPrimaries, endIndex = 6)
                }

                return xyPrimaries
            }

            /**
             * Converts the specified white point to xyY if needed. The white point
             * can be specified as an array of 2 floats (in CIE xyY) or 3 floats
             * (in CIE XYZ). If no conversion is needed, the input array is copied.
             *
             * @param whitePoint The white point in xyY or XYZ
             * @return A new array of 2 floats containing the white point in xyY
             */
            @Size(2)
            internal fun xyWhitePoint(@Size(min = 2, max = 3) whitePoint: FloatArray): FloatArray {
                val xyWhitePoint = FloatArray(2)

                // XYZ to xyY
                if (whitePoint.size == 3) {
                    val sum = whitePoint[0] + whitePoint[1] + whitePoint[2]
                    xyWhitePoint[0] = whitePoint[0] / sum
                    xyWhitePoint[1] = whitePoint[1] / sum
                } else {
                    whitePoint.copyInto(xyWhitePoint, endIndex = 2)
                }

                return xyWhitePoint
            }

            /**
             * Computes the matrix that converts from RGB to XYZ based on RGB
             * primaries and a white point, both specified in the CIE xyY space.
             * The Y component of the primaries and white point is implied to be 1.
             *
             * @param primaries The RGB primaries in xyY, as an array of 6 floats
             * @param whitePoint The white point in xyY, as an array of 2 floats
             * @return A 3x3 matrix as a new array of 9 floats
             */
            @Size(9)
            internal fun computeXYZMatrix(
                @Size(6) primaries: FloatArray,
                @Size(2) whitePoint: FloatArray
            ): FloatArray {
                val rx = primaries[0]
                val ry = primaries[1]
                val gx = primaries[2]
                val gy = primaries[3]
                val bx = primaries[4]
                val by = primaries[5]
                val wx = whitePoint[0]
                val wy = whitePoint[1]

                val oneRxRy = (1 - rx) / ry
                val oneGxGy = (1 - gx) / gy
                val oneBxBy = (1 - bx) / by
                val oneWxWy = (1 - wx) / wy

                val rxRy = rx / ry
                val gxGy = gx / gy
                val bxBy = bx / by
                val wxWy = wx / wy

                val byNumerator =
                    (oneWxWy - oneRxRy) * (gxGy - rxRy) - (wxWy - rxRy) * (oneGxGy - oneRxRy)
                val byDenominator =
                    (oneBxBy - oneRxRy) * (gxGy - rxRy) - (bxBy - rxRy) * (oneGxGy - oneRxRy)
                val bY = byNumerator / byDenominator
                val gY = (wxWy - rxRy - bY * (bxBy - rxRy)) / (gxGy - rxRy)
                val rY = 1f - gY - bY

                val rYRy = rY / ry
                val gYGy = gY / gy
                val bYBy = bY / by

                return floatArrayOf(
                    rYRy * rx, rY, rYRy * (1f - rx - ry),
                    gYGy * gx, gY, gYGy * (1f - gx - gy),
                    bYBy * bx, bY, bYBy * (1f - bx - by)
                )
            }
        }
    }

    /**
     * {@usesMathJax}
     *
     * A connector transforms colors from a source color space to a destination
     * color space.
     *
     * A source color space is connected to a destination color space using the
     * color transform \(C\) computed from their respective transforms noted
     * \(T_{src}\) and \(T_{dst}\) in the following equation:
     *
     * $$C = T^{-1}_{dst} . T_{src}$$
     *
     * The transform \(C\) shown above is only valid when the source and
     * destination color spaces have the same profile connection space (PCS).
     * We know that instances of [ColorSpace] always use CIE XYZ as their
     * PCS but their white points might differ. When they do, we must perform
     * a chromatic adaptation of the color spaces' transforms. To do so, we
     * use the von Kries method described in the documentation of [Adaptation],
     * using the CIE standard illuminant [D50][ColorSpace.IlluminantD50]
     * as the target white point.
     *
     * Example of conversion from [sRGB][Named.Srgb] to
     * [DCI-P3][Named.DciP3]:
     *
     *     val connector = ColorSpace.connect(
     *         ColorSpace.get(ColorSpace.Named.SRGB),
     *         ColorSpace.get(ColorSpace.Named.DCI_P3));
     *     val p3 = connector.transform(1.0f, 0.0f, 0.0f);
     *     // p3 contains { 0.9473, 0.2740, 0.2076 }
     *
     * @see Adaptation
     * @see ColorSpace.adapt
     * @see ColorSpace.adapt
     * @see ColorSpace.connect
     * @see ColorSpace.connect
     * @see ColorSpace.connect
     * @see ColorSpace.connect
     */
    @AnyThread
    open class Connector
    /**
     * To connect between color spaces, we might need to use adapted transforms.
     * This should be transparent to the user so this constructor takes the
     * original source and destinations (returned by the getters), as well as
     * possibly adapted color spaces used by transform().
     */
    internal constructor(
        /**
         * Returns the source color space this connector will convert from.
         *
         * @return A non-null instance of [ColorSpace]
         *
         * @see destination
         */
        val source: ColorSpace,
        /**
         * Returns the destination color space this connector will convert to.
         *
         * @return A non-null instance of [ColorSpace]
         *
         * @see source
         */
        val destination: ColorSpace,
        private val transformSource: ColorSpace,
        private val transformDestination: ColorSpace,
        /**
         * Returns the render intent this connector will use when mapping the
         * source color space to the destination color space.
         *
         * @return A non-null [RenderIntent]
         *
         * @see RenderIntent
         */
        val renderIntent: RenderIntent,
        @Size(3) private val transform: FloatArray?
    ) {
        /**
         * Creates a new connector between a source and a destination color space.
         *
         * @param source The source color space, cannot be null
         * @param destination The destination color space, cannot be null
         * @param intent The render intent to use when compressing gamuts
         */
        internal constructor(
            source: ColorSpace,
            destination: ColorSpace,
            intent: RenderIntent
        ) : this(
            source, destination,
            if (source.model == Model.Rgb) adapt(source, IlluminantD50Xyz) else source,
            if (destination.model == Model.Rgb) {
                adapt(destination, IlluminantD50Xyz)
            } else {
                destination
            },
            intent,
            computeTransform(source, destination, intent)
        ) {
        }

        /**
         * Transforms the specified color from the source color space
         * to a color in the destination color space. This convenience
         * method assumes a source color model with 3 components
         * (typically RGB). To transform from color models with more than
         * 3 components, such as [CMYK][Model.Cmyk], use
         * [transform] instead.
         *
         * @param r The red component of the color to transform
         * @param g The green component of the color to transform
         * @param b The blue component of the color to transform
         * @return A new array of 3 floats containing the specified color
         * transformed from the source space to the destination space
         *
         * @see transform
         */
        @Size(3)
        fun transform(r: Float, g: Float, b: Float): FloatArray {
            return transform(floatArrayOf(r, g, b))
        }

        /**
         * Transforms the specified color from the source color space
         * to a color in the destination color space.
         *
         * @param v A non-null array of 3 floats containing the value to transform
         * and that will hold the result of the transform
         * @return The v array passed as a parameter, containing the specified color
         * transformed from the source space to the destination space
         *
         * @see transform
         */
        @Size(min = 3)
        open fun transform(@Size(min = 3) v: FloatArray): FloatArray {
            val xyz = transformSource.toXyz(v)
            if (transform != null) {
                xyz[0] *= transform[0]
                xyz[1] *= transform[1]
                xyz[2] *= transform[2]
            }
            return transformDestination.fromXyz(xyz)
        }

        /**
         * Optimized connector for RGB->RGB conversions.
         */
        internal class RgbConnector internal constructor(
            private val mSource: ColorSpace.Rgb,
            private val mDestination: ColorSpace.Rgb,
            intent: RenderIntent
        ) : Connector(mSource, mDestination, mSource, mDestination, intent, null) {
            private val mTransform: FloatArray

            init {
                mTransform = computeTransform(mSource, mDestination, intent)
            }

            override fun transform(@Size(min = 3) v: FloatArray): FloatArray {
                v[0] = mSource.eotf(v[0].toDouble()).toFloat()
                v[1] = mSource.eotf(v[1].toDouble()).toFloat()
                v[2] = mSource.eotf(v[2].toDouble()).toFloat()
                mul3x3Float3(mTransform, v)
                v[0] = mDestination.oetf(v[0].toDouble()).toFloat()
                v[1] = mDestination.oetf(v[1].toDouble()).toFloat()
                v[2] = mDestination.oetf(v[2].toDouble()).toFloat()
                return v
            }

            /**
             * Computes the color transform that connects two RGB color spaces.
             *
             * We can only connect color spaces if they use the same profile
             * connection space. We assume the connection space is always
             * CIE XYZ but we maye need to perform a chromatic adaptation to
             * match the white points. If an adaptation is needed, we use the
             * CIE standard illuminant D50. The unmatched color space is adapted
             * using the von Kries transform and the [Adaptation.Bradford]
             * matrix.
             *
             * @param source The source color space, cannot be null
             * @param destination The destination color space, cannot be null
             * @param intent The render intent to use when compressing gamuts
             * @return An array of 9 floats containing the 3x3 matrix transform
             */
            @Size(9)
            private fun computeTransform(
                source: ColorSpace.Rgb,
                destination: ColorSpace.Rgb,
                intent: RenderIntent
            ): FloatArray {
                if (compare(source.whitePoint, destination.whitePoint)) {
                    // RGB->RGB using the PCS of both color spaces since they have the same
                    return mul3x3(destination.inverseTransform, source.transform)
                } else {
                    // RGB->RGB using CIE XYZ D50 as the PCS
                    var transform = source.transform
                    var inverseTransform = destination.inverseTransform

                    val srcXYZ = xyYToXyz(source.whitePoint)
                    val dstXYZ = xyYToXyz(destination.whitePoint)

                    if (!compare(source.whitePoint, IlluminantD50)) {
                        val srcAdaptation = chromaticAdaptation(
                            Adaptation.Bradford.transform, srcXYZ,
                            IlluminantD50Xyz.copyOf()
                        )
                        transform = mul3x3(srcAdaptation, source.transform)
                    }

                    if (!compare(destination.whitePoint, IlluminantD50)) {
                        val dstAdaptation = chromaticAdaptation(
                            Adaptation.Bradford.transform, dstXYZ,
                            IlluminantD50Xyz.copyOf()
                        )
                        inverseTransform =
                            inverse3x3(mul3x3(dstAdaptation, destination.transform))
                    }

                    if (intent == RenderIntent.Absolute) {
                        transform = mul3x3Diag(
                            floatArrayOf(
                                srcXYZ[0] / dstXYZ[0],
                                srcXYZ[1] / dstXYZ[1],
                                srcXYZ[2] / dstXYZ[2]
                            ), transform
                        )
                    }

                    return mul3x3(inverseTransform, transform)
                }
            }
        }

        companion object {
            /**
             * Computes an extra transform to apply in XYZ space depending on the
             * selected rendering intent.
             */
            internal fun computeTransform(
                source: ColorSpace,
                destination: ColorSpace,
                intent: RenderIntent
            ): FloatArray? {
                if (intent != RenderIntent.Absolute) return null

                val srcRGB = source.model == Model.Rgb
                val dstRGB = destination.model == Model.Rgb

                if (srcRGB && dstRGB) return null

                if (srcRGB || dstRGB) {
                    val rgb = (if (srcRGB) source else destination) as ColorSpace.Rgb
                    val srcXYZ = if (srcRGB) xyYToXyz(rgb.whitePoint) else IlluminantD50Xyz
                    val dstXYZ = if (dstRGB) xyYToXyz(rgb.whitePoint) else IlluminantD50Xyz
                    return floatArrayOf(
                        srcXYZ[0] / dstXYZ[0],
                        srcXYZ[1] / dstXYZ[1],
                        srcXYZ[2] / dstXYZ[2]
                    )
                }

                return null
            }

            /**
             * Returns the identity connector for a given color space.
             *
             * @param source The source and destination color space
             * @return A non-null connector that does not perform any transform
             *
             * @see ColorSpace.connect
             */
            internal fun identity(source: ColorSpace): Connector {
                return object : Connector(source, source, RenderIntent.Relative) {
                    override fun transform(@Size(min = 3) v: FloatArray): FloatArray {
                        return v
                    }
                }
            }
        }
    }

    companion object { // ColorSpace companion object
        /**
         * Standard CIE 1931 2° illuminant A, encoded in xyY.
         * This illuminant has a color temperature of 2856K.
         */
        val IlluminantA = floatArrayOf(0.44757f, 0.40745f)
        /**
         * Standard CIE 1931 2° illuminant B, encoded in xyY.
         * This illuminant has a color temperature of 4874K.
         */
        val IlluminantB = floatArrayOf(0.34842f, 0.35161f)
        /**
         * Standard CIE 1931 2° illuminant C, encoded in xyY.
         * This illuminant has a color temperature of 6774K.
         */
        val IlluminantC = floatArrayOf(0.31006f, 0.31616f)
        /**
         * Standard CIE 1931 2° illuminant D50, encoded in xyY.
         * This illuminant has a color temperature of 5003K. This illuminant
         * is used by the profile connection space in ICC profiles.
         */
        val IlluminantD50 = floatArrayOf(0.34567f, 0.35850f)
        /**
         * Standard CIE 1931 2° illuminant D55, encoded in xyY.
         * This illuminant has a color temperature of 5503K.
         */
        val IlluminantD55 = floatArrayOf(0.33242f, 0.34743f)
        /**
         * Standard CIE 1931 2° illuminant D60, encoded in xyY.
         * This illuminant has a color temperature of 6004K.
         */
        val IlluminantD60 = floatArrayOf(0.32168f, 0.33767f)
        /**
         * Standard CIE 1931 2° illuminant D65, encoded in xyY.
         * This illuminant has a color temperature of 6504K. This illuminant
         * is commonly used in RGB color spaces such as sRGB, BT.209, etc.
         */
        val IlluminantD65 = floatArrayOf(0.31271f, 0.32902f)
        /**
         * Standard CIE 1931 2° illuminant D75, encoded in xyY.
         * This illuminant has a color temperature of 7504K.
         */
        val IlluminantD75 = floatArrayOf(0.29902f, 0.31485f)
        /**
         * Standard CIE 1931 2° illuminant E, encoded in xyY.
         * This illuminant has a color temperature of 5454K.
         */
        val IlluminantE = floatArrayOf(0.33333f, 0.33333f)

        /**
         * The minimum ID value a color space can have.
         *
         * @see id
         */
        const val MinId = -1 // Do not change

        /**
         * The maximum ID value a color space can have.
         *
         * @see id
         */
        const val MaxId = 63 // Do not change, used to encode in longs

        internal val SrgbPrimaries = floatArrayOf(0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f)
        internal val Ntsc1953Primaries = floatArrayOf(0.67f, 0.33f, 0.21f, 0.71f, 0.14f, 0.08f)
        internal val IlluminantD50Xyz = floatArrayOf(0.964212f, 1.0f, 0.825188f)

        internal val SrgbTransferParameters =
            Rgb.TransferParameters(1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045, 2.4)

        /**
         *
         * Connects two color spaces to allow conversion from the source color
         * space to the destination color space. If the source and destination
         * color spaces do not have the same profile connection space (CIE XYZ
         * with the same white point), they are chromatically adapted to use the
         * CIE standard illuminant [D50][IlluminantD50] as needed.
         *
         *
         * If the source and destination are the same, an optimized connector
         * is returned to avoid unnecessary computations and loss of precision.
         *
         *
         * Colors are mapped from the source color space to the destination color
         * space using the [perceptual][RenderIntent.Perceptual] render intent.
         *
         * @param source The color space to convert colors from
         * @param destination The color space to convert colors to
         * @return A non-null connector between the two specified color spaces
         *
         * @see connect
         * @see connect
         * @see connect
         */
        fun connect(source: ColorSpace, destination: ColorSpace): Connector {
            return connect(source, destination, RenderIntent.Perceptual)
        }

        /**
         * Connects two color spaces to allow conversion from the source color
         * space to the destination color space. If the source and destination
         * color spaces do not have the same profile connection space (CIE XYZ
         * with the same white point), they are chromatically adapted to use the
         * CIE standard illuminant [D50][IlluminantD50] as needed.
         *
         * If the source and destination are the same, an optimized connector
         * is returned to avoid unnecessary computations and loss of precision.
         *
         * @param source The color space to convert colors from
         * @param destination The color space to convert colors to
         * @param intent The render intent to map colors from the source to the destination
         * @return A non-null connector between the two specified color spaces
         * @see connect
         */
        fun connect(
            source: ColorSpace,
            destination: ColorSpace,
            intent: RenderIntent
        ): Connector {
            if (source == destination) return Connector.identity(source)

            return if (source.model == Model.Rgb && destination.model == Model.Rgb) {
                Connector.RgbConnector(source as Rgb, destination as Rgb, intent)
            } else Connector(source, destination, intent)
        }

        /**
         * Connects the specified color spaces to sRGB.
         * If the source color space does not use CIE XYZ D65 as its profile
         * connection space, the two spaces are chromatically adapted to use the
         * CIE standard illuminant [D50][IlluminantD50] as needed.
         *
         * If the source is the sRGB color space, an optimized connector
         * is returned to avoid unnecessary computations and loss of precision.
         *
         * @param source The color space to convert colors from
         * @param intent The render intent to map colors from the source to the destination
         * @return A non-null connector between the specified color space and sRGB
         *
         * @see connect
         * @see connect
         * @see connect
         */
        @JvmOverloads
        fun connect(source: ColorSpace, intent: RenderIntent = RenderIntent.Perceptual): Connector {
            if (source.isSrgb) return Connector.identity(source)

            return if (source.model == Model.Rgb) {
                Connector.RgbConnector(source as Rgb, get(Named.Srgb) as Rgb, intent)
            } else Connector(source, get(Named.Srgb), intent)
        }

        /**
         * Performs the chromatic adaptation of a color space from its native
         * white point to the specified white point. If the specified color space
         * does not have an [RGB][Model.Rgb] color model, or if the color
         * space already has the target white point, the color space is returned
         * unmodified.
         *
         * The chromatic adaptation is performed using the von Kries method
         * described in the documentation of [Adaptation].
         *
         * The color space returned by this method always has
         * an ID of [MinId].
         *
         * @param colorSpace The color space to chromatically adapt
         * @param whitePoint The new white point
         * @param adaptation The adaptation matrix
         * @return A new color space if the specified color space has an RGB
         * model and a white point different from the specified white
         * point; the specified color space otherwise
         * @see Adaptation
         * @see adapt
         */
        @JvmOverloads
        fun adapt(
            colorSpace: ColorSpace,
            @Size(min = 2, max = 3) whitePoint: FloatArray,
            adaptation: Adaptation = Adaptation.Bradford
        ): ColorSpace {
            if (colorSpace.model == Model.Rgb) {
                val rgb = colorSpace as ColorSpace.Rgb
                if (compare(rgb.whitePoint, whitePoint)) return colorSpace

                val xyz = if (whitePoint.size == 3) whitePoint.copyOf()
                else xyYToXyz(whitePoint)
                val adaptationTransform = chromaticAdaptation(
                    adaptation.transform,
                    xyYToXyz(rgb.whitePoint), xyz
                )
                val transform = mul3x3(adaptationTransform, rgb.transform)

                return ColorSpace.Rgb(rgb, transform, whitePoint)
            }
            return colorSpace
        }

        /**
         * Returns an instance of [ColorSpace] whose ID matches the
         * specified ID.
         *
         * This method always returns the same instance for a given ID.
         *
         * This method is thread-safe.
         *
         * @param index An integer ID between [MinId] and [MaxId]
         * @return A non-null [ColorSpace] instance
         * @throws IllegalArgumentException If the ID does not match the ID of one of the
         * [named color spaces][Named]
         */
        internal operator fun get(
            @IntRange(from = MinId.toLong(), to = MaxId.toLong()) index: Int
        ): ColorSpace {
            if (index < 0 || index >= Named.values().size) {
                throw IllegalArgumentException(
                    "Invalid ID, must be in the range [0..${Named.values().size}])")
            }
            return Named.ColorSpaces[index]
        }

        /**
         * Returns an instance of [ColorSpace] identified by the specified
         * name. The list of names provided in the [Named] enum gives access
         * to a variety of common RGB color spaces.
         *
         * This method always returns the same instance for a given name.
         *
         * This method is thread-safe.
         *
         * @param name The name of the color space to get an instance of
         * @return A non-null [ColorSpace] instance
         */
        operator fun get(name: Named): ColorSpace {
            return Named.ColorSpaces[name.ordinal]
        }

        /**
         * Returns a [Named] instance of [ColorSpace] that matches
         * the specified RGB to CIE XYZ transform and transfer functions. If no
         * instance can be found, this method returns null.
         *
         * The color transform matrix is assumed to target the CIE XYZ space
         * a [D50][IlluminantD50] standard illuminant.
         *
         * @param toXYZD50 3x3 column-major transform matrix from RGB to the profile
         * connection space CIE XYZ as an array of 9 floats, cannot be null
         * @param function Parameters for the transfer functions
         * @return A non-null [ColorSpace] if a match is found, null otherwise
         */
        fun match(
            @Size(9) toXYZD50: FloatArray,
            function: Rgb.TransferParameters
        ): ColorSpace? {
            for (colorSpace in Named.ColorSpaces) {
                if (colorSpace.model == Model.Rgb) {
                    val rgb = adapt(colorSpace, IlluminantD50Xyz) as ColorSpace.Rgb
                    if ((compare(toXYZD50, rgb.transform) && compare(
                            function,
                            rgb.transferParameters
                        ))
                    ) {
                        return colorSpace
                    }
                }
            }

            return null
        }

        // Reciprocal piecewise gamma response
        internal fun rcpResponse(x: Double, a: Double, b: Double, c: Double, d: Double, g: Double):
                Double {
            return if (x >= d * c) (x.pow(1.0 / g) - b) / a else x / c
        }

        // Piecewise gamma response
        internal fun response(x: Double, a: Double, b: Double, c: Double, d: Double, g: Double):
                Double {
            return if (x >= d) (a * x + b).pow(g) else c * x
        }

        // Reciprocal piecewise gamma response
        internal fun rcpResponse(
            x: Double,
            a: Double,
            b: Double,
            c: Double,
            d: Double,
            e: Double,
            f: Double,
            g: Double
        ): Double {
            return if (x >= d * c) ((x - e).pow(1.0 / g) - b) / a else (x - f) / c
        }

        // Piecewise gamma response
        internal fun response(
            x: Double,
            a: Double,
            b: Double,
            c: Double,
            d: Double,
            e: Double,
            f: Double,
            g: Double
        ): Double {
            return if (x >= d) (a * x + b).pow(g) + e else c * x + f
        }

        // Reciprocal piecewise gamma response, encoded as sign(x).f(abs(x)) for color
        // spaces that allow negative values
        internal fun absRcpResponse(
            x: Double,
            a: Double,
            b: Double,
            c: Double,
            d: Double,
            g: Double
        ): Double {
            return rcpResponse(if (x < 0.0) -x else x, a, b, c, d, g).withSign(x)
        }

        // Piecewise gamma response, encoded as sign(x).f(abs(x)) for color spaces that
        // allow negative values
        internal fun absResponse(x: Double, a: Double, b: Double, c: Double, d: Double, g: Double):
                Double {
            return response(if (x < 0.0) -x else x, a, b, c, d, g).withSign(x)
        }

        /**
         * Compares two sets of parametric transfer functions parameters with a precision of 1e-3.
         *
         * @param a The first set of parameters to compare
         * @param b The second set of parameters to compare
         * @return True if the two sets are equal, false otherwise
         */
        private fun compare(a: Rgb.TransferParameters, b: Rgb.TransferParameters?): Boolean {
            return (b != null &&
                    abs(a.a - b.a) < 1e-3 &&
                    abs(a.b - b.b) < 1e-3 &&
                    abs(a.c - b.c) < 1e-3 &&
                    abs(a.d - b.d) < 2e-3 && // Special case for variations in sRGB OETF/EOTF

                    abs(a.e - b.e) < 1e-3 &&
                    abs(a.f - b.f) < 1e-3 &&
                    abs(a.g - b.g) < 1e-3)
        }

        /**
         * Compares two arrays of float with a precision of 1e-3.
         *
         * @param a The first array to compare
         * @param b The second array to compare
         * @return True if the two arrays are equal, false otherwise
         */
        internal fun compare(a: FloatArray, b: FloatArray): Boolean {
            if (a === b) return true
            for (i in a.indices) {
                // TODO: do we need the compareTo() here? Isn't the abs sufficient?
                if (a[i].compareTo(b[i]) != 0 && abs(a[i] - b[i]) > 1e-3f) return false
            }
            return true
        }

        /**
         * Inverts a 3x3 matrix. This method assumes the matrix is invertible.
         *
         * @param m A 3x3 matrix as a non-null array of 9 floats
         * @return A new array of 9 floats containing the inverse of the input matrix
         */
        @Size(9)
        internal fun inverse3x3(@Size(9) m: FloatArray): FloatArray {
            val a = m[0]
            val b = m[3]
            val c = m[6]
            val d = m[1]
            val e = m[4]
            val f = m[7]
            val g = m[2]
            val h = m[5]
            val i = m[8]

            val xA = e * i - f * h
            val xB = f * g - d * i
            val xC = d * h - e * g

            val det = a * xA + b * xB + c * xC

            val inverted = FloatArray(m.size)
            inverted[0] = xA / det
            inverted[1] = xB / det
            inverted[2] = xC / det
            inverted[3] = (c * h - b * i) / det
            inverted[4] = (a * i - c * g) / det
            inverted[5] = (b * g - a * h) / det
            inverted[6] = (b * f - c * e) / det
            inverted[7] = (c * d - a * f) / det
            inverted[8] = (a * e - b * d) / det
            return inverted
        }

        /**
         * Multiplies two 3x3 matrices, represented as non-null arrays of 9 floats.
         *
         * @param lhs 3x3 matrix, as a non-null array of 9 floats
         * @param rhs 3x3 matrix, as a non-null array of 9 floats
         * @return A new array of 9 floats containing the result of the multiplication
         * of rhs by lhs
         */
        @Size(9)
        internal fun mul3x3(@Size(9) lhs: FloatArray, @Size(9) rhs: FloatArray):
                FloatArray {
            val r = FloatArray(9)
            r[0] = lhs[0] * rhs[0] + lhs[3] * rhs[1] + lhs[6] * rhs[2]
            r[1] = lhs[1] * rhs[0] + lhs[4] * rhs[1] + lhs[7] * rhs[2]
            r[2] = lhs[2] * rhs[0] + lhs[5] * rhs[1] + lhs[8] * rhs[2]
            r[3] = lhs[0] * rhs[3] + lhs[3] * rhs[4] + lhs[6] * rhs[5]
            r[4] = lhs[1] * rhs[3] + lhs[4] * rhs[4] + lhs[7] * rhs[5]
            r[5] = lhs[2] * rhs[3] + lhs[5] * rhs[4] + lhs[8] * rhs[5]
            r[6] = lhs[0] * rhs[6] + lhs[3] * rhs[7] + lhs[6] * rhs[8]
            r[7] = lhs[1] * rhs[6] + lhs[4] * rhs[7] + lhs[7] * rhs[8]
            r[8] = lhs[2] * rhs[6] + lhs[5] * rhs[7] + lhs[8] * rhs[8]
            return r
        }

        /**
         * Multiplies a vector of 3 components by a 3x3 matrix and stores the
         * result in the input vector.
         *
         * @param lhs 3x3 matrix, as a non-null array of 9 floats
         * @param rhs Vector of 3 components, as a non-null array of 3 floats
         * @return The array of 3 passed as the rhs parameter
         */
        @Size(min = 3)
        internal fun mul3x3Float3(
            @Size(9) lhs: FloatArray,
            @Size(min = 3) rhs: FloatArray
        ): FloatArray {
            val r0 = rhs[0]
            val r1 = rhs[1]
            val r2 = rhs[2]
            rhs[0] = lhs[0] * r0 + lhs[3] * r1 + lhs[6] * r2
            rhs[1] = lhs[1] * r0 + lhs[4] * r1 + lhs[7] * r2
            rhs[2] = lhs[2] * r0 + lhs[5] * r1 + lhs[8] * r2
            return rhs
        }

        /**
         * Multiplies a diagonal 3x3 matrix lhs, represented as an array of 3 floats,
         * by a 3x3 matrix represented as an array of 9 floats.
         *
         * @param lhs Diagonal 3x3 matrix, as a non-null array of 3 floats
         * @param rhs 3x3 matrix, as a non-null array of 9 floats
         * @return A new array of 9 floats containing the result of the multiplication
         * of rhs by lhs
         */
        @Size(9)
        internal fun mul3x3Diag(
            @Size(3) lhs: FloatArray,
            @Size(9) rhs: FloatArray
        ): FloatArray {
            return floatArrayOf(
                lhs[0] * rhs[0], lhs[1] * rhs[1], lhs[2] * rhs[2],
                lhs[0] * rhs[3], lhs[1] * rhs[4], lhs[2] * rhs[5],
                lhs[0] * rhs[6], lhs[1] * rhs[7], lhs[2] * rhs[8]
            )
        }

        /**
         * Converts a value from CIE xyY to CIE XYZ. Y is assumed to be 1 so the
         * input xyY array only contains the x and y components.
         *
         * @param xyY The xyY value to convert to XYZ, cannot be null, length must be 2
         * @return A new float array of length 3 containing XYZ values
         */
        @Size(3)
        internal fun xyYToXyz(@Size(2) xyY: FloatArray): FloatArray {
            return floatArrayOf(xyY[0] / xyY[1], 1.0f, (1f - xyY[0] - xyY[1]) / xyY[1])
        }

        /**
         * Computes the chromatic adaptation transform from the specified
         * source white point to the specified destination white point.
         *
         * The transform is computed using the von Kries method, described
         * in more details in the documentation of [Adaptation]. The
         * [Adaptation] enum provides different matrices that can be
         * used to perform the adaptation.
         *
         * @param matrix The adaptation matrix
         * @param srcWhitePoint The white point to adapt from, *will be modified*
         * @param dstWhitePoint The white point to adapt to, *will be modified*
         * @return A 3x3 matrix as a non-null array of 9 floats
         */
        @Size(9)
        internal fun chromaticAdaptation(
            @Size(9) matrix: FloatArray,
            @Size(3) srcWhitePoint: FloatArray,
            @Size(3) dstWhitePoint: FloatArray
        ): FloatArray {
            val srcLMS = mul3x3Float3(matrix, srcWhitePoint)
            val dstLMS = mul3x3Float3(matrix, dstWhitePoint)
            // LMS is a diagonal matrix stored as a float[3]
            val LMS =
                floatArrayOf(dstLMS[0] / srcLMS[0], dstLMS[1] / srcLMS[1], dstLMS[2] / srcLMS[2])
            return mul3x3(inverse3x3(matrix), mul3x3Diag(LMS, matrix))
        }
    }
}
