/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.core.graphics;

import android.os.Build;

import androidx.annotation.RequiresApi;

/**
 * Compat version of {@link android.graphics.BlendMode}, usages of {@link BlendModeCompat} will
 * map to {@link android.graphics.PorterDuff.Mode} wherever possible
 */
public enum BlendModeCompat {

    /**
     * {@usesMathJax}
     *
     * Destination pixels covered by the source are cleared to 0.
     *
     * <p>\(\alpha_{out} = 0\)</p>
     * <p>\(C_{out} = 0\)</p>
     */
    CLEAR,

    /**
     * {@usesMathJax}
     *
     * The source pixels replace the destination pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{src}\)</p>
     * <p>\(C_{out} = C_{src}\)</p>
     */
    SRC,

    /**
     * {@usesMathJax}
     *
     * The source pixels are discarded, leaving the destination intact.
     *
     * <p>\(\alpha_{out} = \alpha_{dst}\)</p>
     * <p>\(C_{out} = C_{dst}\)</p>
     */
    DST,

    /**
     * {@usesMathJax}
     *
     * The source pixels are drawn over the destination pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{src} + (1 - \alpha_{src}) * \alpha_{dst}\)</p>
     * <p>\(C_{out} = C_{src} + (1 - \alpha_{src}) * C_{dst}\)</p>
     */
    SRC_OVER,

    /**
     * {@usesMathJax}
     *
     * The source pixels are drawn behind the destination pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{dst} + (1 - \alpha_{dst}) * \alpha_{src}\)</p>
     * <p>\(C_{out} = C_{dst} + (1 - \alpha_{dst}) * C_{src}\)</p>
     */
    DST_OVER,

    /**
     * {@usesMathJax}
     *
     * Keeps the source pixels that cover the destination pixels,
     * discards the remaining source and destination pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{src} * \alpha_{dst}\)</p>
     * <p>\(C_{out} = C_{src} * \alpha_{dst}\)</p>
     */
    SRC_IN,

    /**
     * {@usesMathJax}
     *
     * Keeps the destination pixels that cover source pixels,
     *  discards the remaining source and destination pixels.
     * <p>\(\alpha_{out} = \alpha_{src} * \alpha_{dst}\)</p>
     * <p>\(C_{out} = C_{dst} * \alpha_{src}\)</p>
     */
    DST_IN,

    /**
     * {@usesMathJax}
     *
     * Keeps the source pixels that do not cover destination pixels.
     * Discards source pixels that cover destination pixels. Discards all
     * destination pixels.
     *
     * <p>\(\alpha_{out} = (1 - \alpha_{dst}) * \alpha_{src}\)</p>
     * <p>\(C_{out} = (1 - \alpha_{dst}) * C_{src}\)</p>
     */
    SRC_OUT,

    /**
     * {@usesMathJax}
     *
     * Keeps the destination pixels that are not covered by source pixels.
     * Discards destination pixels that are covered by source pixels. Discards all
     * source pixels.
     *
     * <p>\(\alpha_{out} = (1 - \alpha_{src}) * \alpha_{dst}\)</p>
     * <p>\(C_{out} = (1 - \alpha_{src}) * C_{dst}\)</p>
     */
    DST_OUT,

    /**
     * {@usesMathJax}
     *
     * Discards the source pixels that do not cover destination pixels.
     * Draws remaining source pixels over destination pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{dst}\)</p>
     * <p>\(C_{out} = \alpha_{dst} * C_{src} + (1 - \alpha_{src}) * C_{dst}\)</p>
     */
    SRC_ATOP,

    /**
     * {@usesMathJax}
     *
     * Discards the destination pixels that are not covered by source pixels.
     * Draws remaining destination pixels over source pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{src}\)</p>
     * <p>\(C_{out} = \alpha_{src} * C_{dst} + (1 - \alpha_{dst}) * C_{src}\)</p>
     */
    DST_ATOP,

    /**
     * {@usesMathJax}
     *
     * Discards the source and destination pixels where source pixels
     * cover destination pixels. Draws remaining source pixels.
     *
     * <p>
     *     \(\alpha_{out} = (1 - \alpha_{dst}) * \alpha_{src} + (1 - \alpha_{src}) * \alpha_{dst}\)
     * </p>
     * <p>\(C_{out} = (1 - \alpha_{dst}) * C_{src} + (1 - \alpha_{src}) * C_{dst}\)</p>
     */
    XOR,

    /**
     * {@usesMathJax}
     *
     * Adds the source pixels to the destination pixels and saturates
     * the result.
     *
     * <p>\(\alpha_{out} = max(0, min(\alpha_{src} + \alpha_{dst}, 1))\)</p>
     * <p>\(C_{out} = max(0, min(C_{src} + C_{dst}, 1))\)</p>
     */
    PLUS,

    /**
     * {@usesMathJax}
     *
     * Multiplies the source and destination pixels.
     *
     * <p>\(\alpha_{out} = \alpha_{src} * \alpha_{dst}\)</p>
     * <p>\(C_{out} = C_{src} * C_{dst}\)</p>
     *
     */
    MODULATE,

    /**
     * {@usesMathJax}
     *
     * Adds the source and destination pixels, then subtracts the
     * source pixels multiplied by the destination.
     * <p>\(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)</p>
     * <p>\(C_{out} = C_{src} + C_{dst} - C_{src} * C_{dst}\)</p>
     */
    SCREEN,

    /**
     * {@usesMathJax}
     *
     * Multiplies or screens the source and destination depending on the
     * destination color.
     *
     * <p>\(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)</p>
     * <p>\(\begin{equation}
     * C_{out} = \begin{cases} 2 * C_{src} * C_{dst} & 2 * C_{dst} \lt \alpha_{dst} \\
     * \alpha_{src} * \alpha_{dst} - 2 (\alpha_{dst} - C_{src}) (\alpha_{src} - C_{dst}) &
     * otherwise \end{cases}
     * \end{equation}\)</p>
     */
    OVERLAY,

    /**
     * {@usesMathJax}
     *
     * Retains the smallest component of the source and
     * destination pixels.
     * <p>\(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)</p>
     * <p>
     *     \(C_{out} =
     *     (1 - \alpha_{dst}) * C_{src} + (1 - \alpha_{src}) * C_{dst} + min(C_{src}, C_{dst})\)
     * </p>
     */
    DARKEN,

    /**
     * {@usesMathJax}
     *
     * Retains the largest component of the source and
     * destination pixel.
     * <p>\(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)</p>
     * <p>
     *     \(C_{out} =
     *      (1 - \alpha_{dst}) * C_{src} + (1 - \alpha_{src}) * C_{dst} + max(C_{src}, C_{dst})\)
     * </p>
     */
    LIGHTEN,

    /**
     * {@usesMathJax}
     *
     * Makes destination brighter to reflect source.
     *     \(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)
     * </p>
     * <p>
     *      \begin{equation}
     *      C_{out} =
     *      \begin{cases}
     *          C_{src} * (1 - \alpha_{dst}) & C_{dst} = 0 \\
     *          C_{src} + \alpha_{dst}*(1 - \alpha_{src}) & C_{src} = \alpha_{src} \\
     *          \alpha_{src} * min(\alpha_{dst}, C_{dst} * \alpha_{src}/(\alpha_{src} - C_{src}))
     *              + C_{src} *(1 - \alpha_{dst} + \alpha_{dst}*(1 - \alpha_{src}) & otherwise
     *      \end{cases}
     *      \end{equation}
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    COLOR_DODGE,

    /**
     * {@usesMathJax}
     *
     * Makes destination darker to reflect source.
     *
     * <p>
     *     \(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)
     * </p>
     * <p>
     *     \begin{equation}
     *     C_{out} =
     *     \begin{cases}
     *         C_{dst} + C_{src}*(1 - \alpha_{dst}) & C_{dst} = \alpha_{dst} \\
     *         \alpha_{dst}*(1 - \alpha_{src}) & C_{src} = 0 \\
     *         \alpha_{src}*(\alpha_{dst} - min(\alpha_{dst}, (\alpha_{dst}
     *         - C_{dst})*\alpha_{src}/C_{src}))
     *         + C_{src} * (1 - \alpha_{dst}) + \alpha_{dst}*(1-\alpha_{src}) & otherwise
     *     \end{cases}
     *     \end{equation}
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    COLOR_BURN,

    /**
     * {@usesMathJax}
     *
     * Makes destination lighter or darker, depending on source.
     * <p>
     *     \(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)
     * </p>
     * <p>
     *     \begin{equation}
     *      C_{out} =
     *      \begin{cases}
     *           2*C_{src}*C_{dst} & C_{src}*(1-\alpha_{dst}) + C_{dst}*(1-\alpha_{src}) + 2*C_{src}
     *              \leq \alpha_{src} \\
     *           \alpha_{src}*\alpha_{dst}- 2*(\alpha_{dst} - C_{dst})*(\alpha_{src} - C_{src})
     *              & otherwise
     *      \end{cases}
     *      \end{equation}
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    HARD_LIGHT,

    /**
     * {@usesMathJax}
     *
     * Makes destination lighter or darker, depending on source.
     * <p>
     *     Where
     *       \begin{equation}
     *       m =
     *          \begin{cases}
     *              C_{dst} / \alpha_{dst} & \alpha_{dst} \gt 0 \\
     *              0 & otherwise
     *          \end{cases}
     *       \end{equation}
     * </p>
     * <p>
     *       \begin{equation}
     *       g =
     *          \begin{cases}
     *              (16 * m * m + 4 * m) * (m - 1) + 7 * m & 4 * C_{dst} \leq \alpha_{dst} \\
     *              \sqrt m - m & otherwise
     *          \end{cases}
     *       \end{equation}
     * </p>
     * <p>
     *       \begin{equation}
     *       f =
     *          \begin{cases}
     *              C_{dst} * (\alpha_{src} + (2 * C_{src} - \alpha_{src}) * (1 - m))
     *                  & 2 * C_{src} \leq \alpha_{src} \\
     *              C_{dst} * \alpha_{src} + \alpha_{dst} * (2 * C_{src} - \alpha_{src}) * g
     *                  & otherwise
     *          \end{cases}
     *       \end{equation}
     * </p>
     * <p>
     *       \begin{equation}
     *          \alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}
     *       \end{equation}
     *       \begin{equation}
     *          C_{out} = C_{src} / \alpha_{dst} + C_{dst} / \alpha_{src} + f
     *       \end{equation}
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    SOFT_LIGHT,

    /**
     * {@usesMathJax}
     *
     * Subtracts darker from lighter with higher contrast.
     * <p>
     *     \begin{equation}
     *          \alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}
     *     \end{equation}
     * </p>
     * <p>
     *     \begin{equation}
     *           C_{out} = C_{src} + C_{dst} - 2 * min(C_{src}
     *                       * \alpha_{dst}, C_{dst} * \alpha_{src})
     *     \end{equation}
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    DIFFERENCE,

    /**
     * {@usesMathJax}
     *
     * Subtracts darker from lighter with lower contrast.
     * <p>
     *     \begin{equation}
     *          \alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}
     *     \end{equation}
     * </p>
     * <p>
     *     \begin{equation}
     *          C_{out} = C_{src} + C_{dst} - 2 * C_{src} * C_{dst}
     *     \end{equation}
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    EXCLUSION,

    /**
     * {@usesMathJax}
     *
     * Multiplies the source and destination pixels.
     * <p>\(\alpha_{out} = \alpha_{src} + \alpha_{dst} - \alpha_{src} * \alpha_{dst}\)</p>
     * <p>\(C_{out} =
     *      C_{src} * (1 - \alpha_{dst}) + C_{dst} * (1 - \alpha_{src}) + (C_{src} * C_{dst})\)
     * </p>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    MULTIPLY,

    /**
     * Replaces hue of destination with hue of source, leaving saturation
     * and luminosity unchanged.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    HUE,

    /**
     * Replaces saturation of destination saturation hue of source, leaving hue and
     * luminosity unchanged.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    SATURATION,

    /**
     * Replaces hue and saturation of destination with hue and saturation of source,
     * leaving luminosity unchanged.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    COLOR,

    /**
     * Replaces luminosity of destination with luminosity of source, leaving hue and
     * saturation unchanged.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    LUMINOSITY
}
