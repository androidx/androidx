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

package androidx.compose.ui.tooling

import kotlin.math.roundToInt

// Spec parameter keys (normalized lowercase for lookup)
private const val PARAM_EXPAND = "expand"
private const val PARAM_MAX_HEIGHT = "maxheight"
private const val PARAM_MAX_WIDTH = "maxwidth"

// Spec parameter values
private const val EXPAND_VERTICAL = "vertical"
private const val EXPAND_HORIZONTAL = "horizontal"

/**
 * Default maximum size, in pixels, of the expanded axis of a preview, used when the device spec
 * does not specify a valid `maxHeight` / `maxWidth`.
 */
internal const val DEFAULT_EXPAND_MAX_SIZE_PX = 4096

/** Axis along which a preview grows to display all its scrollable content. */
internal enum class PreviewExpandAxis {
    VERTICAL,
    HORIZONTAL,
}

/**
 * Expansion settings of a preview, read from its device spec.
 *
 * @param axis the axis along which the preview grows to display all its scrollable content.
 * @param maxSizePx the maximum size, in pixels, of the preview along [axis].
 */
internal data class PreviewExpandSettings(val axis: PreviewExpandAxis, val maxSizePx: Int)

/**
 * Parses the expansion settings from a `spec:` device string, e.g.
 * `spec:parent=pixel_7,expand=vertical,maxHeight=1200dp`:
 * - `expand`: `vertical` or `horizontal` (case-insensitive), the axis along which the preview grows
 *   to display all its scrollable content.
 * - `maxHeight` (for `vertical`) / `maxWidth` (for `horizontal`): the maximum size of the expanded
 *   axis, as a number with a `dp` or `px` suffix. `dp` values are converted to pixels with
 *   [density]. The key that does not match the axis is ignored.
 *
 * Returns null when [deviceSpec] is not a `spec:` string or when `expand` is absent or invalid. An
 * absent or invalid maximum size falls back to [DEFAULT_EXPAND_MAX_SIZE_PX].
 */
internal fun parsePreviewExpandSettings(
    deviceSpec: String?,
    density: Float,
): PreviewExpandSettings? {
    val params = parseDeviceSpecParams(deviceSpec) ?: return null
    val axis =
        when (params[PARAM_EXPAND]) {
            EXPAND_VERTICAL -> PreviewExpandAxis.VERTICAL
            EXPAND_HORIZONTAL -> PreviewExpandAxis.HORIZONTAL
            else -> return null
        }
    val maxSizeKey =
        when (axis) {
            PreviewExpandAxis.VERTICAL -> PARAM_MAX_HEIGHT
            PreviewExpandAxis.HORIZONTAL -> PARAM_MAX_WIDTH
        }
    val maxSizePx = parseSizePx(params[maxSizeKey], density) ?: DEFAULT_EXPAND_MAX_SIZE_PX
    return PreviewExpandSettings(axis, maxSizePx)
}

/**
 * Parses a size with a required `dp` or `px` suffix (e.g. `1200dp`, `3000px`) into pixels. Returns
 * null if [value] is null, has no valid unit or number, or is smaller than one pixel.
 */
private fun parseSizePx(value: String?, density: Float): Int? {
    val px =
        when {
            value == null -> null
            value.endsWith(DP_SUFFIX) ->
                value.removeSuffix(DP_SUFFIX).toFloatOrNull()?.let { it * density }
            value.endsWith(PX_SUFFIX) -> value.removeSuffix(PX_SUFFIX).toFloatOrNull()
            else -> null
        } ?: return null
    if (!px.isFinite()) return null
    return px.roundToInt().takeIf { it >= 1 }
}
