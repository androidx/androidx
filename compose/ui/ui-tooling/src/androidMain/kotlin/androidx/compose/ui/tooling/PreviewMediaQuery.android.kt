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

import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SPEC_PREFIX = "spec:"

// Spec parameter keys (normalized lowercase for lookup)
private const val PARAM_WINDOW_POSTURE = "windowposture"
private const val PARAM_POINTER_PRECISION = "pointerprecision"
private const val PARAM_KEYBOARD_KIND = "keyboardkind"
private const val PARAM_HAS_CAMERA = "hascamera"
private const val PARAM_HAS_MICROPHONE = "hasmicrophone"
private const val PARAM_VIEWING_DISTANCE = "viewingdistance"
private const val PARAM_WIDTH = "width"
private const val PARAM_HEIGHT = "height"
private const val PARAM_DPI = "dpi"
private const val PARAM_ORIENTATION = "orientation"

// Spec parameter values
private const val POSTURE_TABLETOP = "tabletop"
private const val POSTURE_BOOK = "book"

private const val POINTER_PRECISION_FINE = "fine"
private const val POINTER_PRECISION_BLUNT = "blunt"
private const val POINTER_PRECISION_NONE = "none"

private const val KEYBOARD_KIND_PHYSICAL = "physical"
private const val KEYBOARD_KIND_NONE = "none"

private const val VIEWING_DISTANCE_FAR = "far"
private const val VIEWING_DISTANCE_MEDIUM = "medium"

private const val ORIENTATION_LANDSCAPE = "landscape"
private const val ORIENTATION_PORTRAIT = "portrait"

/**
 * Default preview spec density when `dpi` is omitted, matching Studio's `DeviceSpec.DEFAULT_DPI`.
 */
private const val DEFAULT_SPEC_DPI = 420

/** Android medium density baseline (`1dp == 1px` at 160 dpi). */
private const val BASELINE_DPI = 160

/** Default preview window width, matching `Devices.PHONE` (`spec:width=411dp,height=891dp`). */
internal val DEFAULT_PREVIEW_WIDTH = 411.dp

/** Default preview window height, matching `Devices.PHONE` (`spec:width=411dp,height=891dp`). */
internal val DEFAULT_PREVIEW_HEIGHT = 891.dp

/** Standard flat posture without active folding features intersecting the window. */
@OptIn(ExperimentalMediaQueryApi::class)
private val FlatPosture = UiMediaScope.WindowPosture(emptyList())

/**
 * Tabletop (laptop / flex-mode) posture where a horizontal hinge intersects the window.
 *
 * A half-opened horizontal fold divides the screen into top (viewing) and bottom (controls) halves.
 */
@OptIn(ExperimentalMediaQueryApi::class)
private val TabletopPosture =
    UiMediaScope.WindowPosture(
        listOf(
            UiMediaScope.WindowFold(
                state = UiMediaScope.FoldState.HalfOpened,
                orientation = UiMediaScope.FoldOrientation.Horizontal,
            )
        )
    )

/**
 * Book posture where a vertical hinge intersects the window.
 *
 * A half-opened vertical fold divides the screen into left and right halves (like an open book).
 */
@OptIn(ExperimentalMediaQueryApi::class)
private val BookPosture =
    UiMediaScope.WindowPosture(
        listOf(
            UiMediaScope.WindowFold(
                state = UiMediaScope.FoldState.HalfOpened,
                orientation = UiMediaScope.FoldOrientation.Vertical,
            )
        )
    )

/**
 * Resolves the preview spec density from [params]: defaults to [DEFAULT_SPEC_DPI] (`420`) when
 * `dpi` is omitted, or returns null when an explicit `dpi` key is not a positive integer.
 */
private fun parseDpi(params: Map<String, String>): Int? =
    if (PARAM_DPI in params) {
        params[PARAM_DPI]?.toIntOrNull()?.takeIf { it > 0 }
    } else {
        DEFAULT_SPEC_DPI
    }

/**
 * Parses a dimension value with a required `dp` or `px` suffix into a positive [Dp]. `px` values
 * are converted directly via `px * 160 / dpi` when [dpi] is valid.
 */
private fun parseDimensionDp(value: String?, dpi: Int?): Dp? {
    val dpValue =
        when {
            value == null -> null
            value.endsWith(DP_SUFFIX) -> value.removeSuffix(DP_SUFFIX).toFloatOrNull()
            value.endsWith(PX_SUFFIX) ->
                dpi?.let {
                    value.removeSuffix(PX_SUFFIX).toFloatOrNull()?.let { px ->
                        (px.toDouble() * BASELINE_DPI / it).toFloat()
                    }
                }
            else -> null
        }
    return dpValue?.takeIf { it.isFinite() && it > 0f }?.dp
}

/**
 * Preview-specific implementation of [UiMediaScope].
 *
 * Supplies parsed media query properties and resolves [windowWidth] and [windowHeight] dynamically
 * from `WindowInfo.containerDpSize` when positive, falling back to [deviceWidth] and
 * [deviceHeight]. When [reportDeviceWidth] or [reportDeviceHeight] is true, [deviceWidth] or
 * [deviceHeight] is reported directly instead of the container size: this is used on the axis along
 * which a preview is expanded (see [PreviewExpandSettings]), so that media queries see the size of
 * the device rather than the size of the expanded preview.
 */
@OptIn(ExperimentalMediaQueryApi::class)
internal class PreviewUiMediaScope(
    private val windowInfo: WindowInfo,
    override val windowPosture: UiMediaScope.WindowPosture = FlatPosture,
    override val pointerPrecision: UiMediaScope.PointerPrecision =
        UiMediaScope.PointerPrecision.Coarse,
    override val keyboardKind: UiMediaScope.KeyboardKind = UiMediaScope.KeyboardKind.Virtual,
    override val hasCamera: Boolean = true,
    override val hasMicrophone: Boolean = true,
    override val viewingDistance: UiMediaScope.ViewingDistance = UiMediaScope.ViewingDistance.Near,
    private val deviceWidth: Dp = DEFAULT_PREVIEW_WIDTH,
    private val deviceHeight: Dp = DEFAULT_PREVIEW_HEIGHT,
    private val reportDeviceWidth: Boolean = false,
    private val reportDeviceHeight: Boolean = false,
) : UiMediaScope {
    @get:FrequentlyChangingValue
    override val windowWidth: Dp
        get() =
            if (reportDeviceWidth) {
                deviceWidth
            } else {
                windowInfo.containerDpSize.width.takeIf { it > 0.dp } ?: deviceWidth
            }

    @get:FrequentlyChangingValue
    override val windowHeight: Dp
        get() =
            if (reportDeviceHeight) {
                deviceHeight
            } else {
                windowInfo.containerDpSize.height.takeIf { it > 0.dp } ?: deviceHeight
            }
}

/**
 * Parses the parameters of a preview device specification string (e.g.
 * `spec:width=411dp,height=891dp,...`) into a map. Keys and values are trimmed and lowercase.
 * Returns null if [deviceSpec] is null or not a `spec:` string.
 */
internal fun parseDeviceSpecParams(deviceSpec: String?): Map<String, String>? {
    if (deviceSpec.isNullOrEmpty() || !deviceSpec.startsWith(SPEC_PREFIX)) return null
    return deviceSpec
        .removePrefix(SPEC_PREFIX)
        .split(',')
        .mapNotNull {
            val parts = it.split('=', limit = 2)
            if (parts.size == 2) {
                parts[0].trim().lowercase() to parts[1].trim().lowercase()
            } else null
        }
        .toMap()
}

/**
 * Parses a preview device specification string (e.g. `spec:width=411dp,height=891dp,...`) or
 * returns a default [UiMediaScope] for Phone / Default device if omitted or not a `spec:` string.
 *
 * Uses full [UiMediaScope] property names: `windowPosture`, `pointerPrecision`, `keyboardKind`,
 * `hasCamera`, `hasMicrophone`, and `viewingDistance`.
 *
 * The device window dimensions come from the `width` and `height` keys in [deviceSpec] when present
 * (`dp` or `px`, with `px` converted directly via `dpi` or `420` when omitted, without snapping
 * arbitrary DPI values to Android density buckets), or from [fallbackWidth] and [fallbackHeight]
 * otherwise, swapping axes when `orientation` (`landscape` or `portrait`) disagrees with the raw
 * aspect ratio. When [reportDeviceWidth] or [reportDeviceHeight] is true, that device dimension is
 * reported as the window size instead of the size of the container.
 */
@OptIn(ExperimentalMediaQueryApi::class)
internal fun createPreviewUiMediaScope(
    windowInfo: WindowInfo,
    deviceSpec: String? = null,
    fallbackWidth: Dp = DEFAULT_PREVIEW_WIDTH,
    fallbackHeight: Dp = DEFAULT_PREVIEW_HEIGHT,
    reportDeviceWidth: Boolean = false,
    reportDeviceHeight: Boolean = false,
): UiMediaScope {
    val params =
        parseDeviceSpecParams(deviceSpec)
            ?: return PreviewUiMediaScope(
                windowInfo = windowInfo,
                deviceWidth = fallbackWidth,
                deviceHeight = fallbackHeight,
                reportDeviceWidth = reportDeviceWidth,
                reportDeviceHeight = reportDeviceHeight,
            )

    val dpi = parseDpi(params)
    val rawWidth = parseDimensionDp(params[PARAM_WIDTH], dpi) ?: fallbackWidth
    val rawHeight = parseDimensionDp(params[PARAM_HEIGHT], dpi) ?: fallbackHeight
    val swapDimensions =
        when (params[PARAM_ORIENTATION]) {
            ORIENTATION_LANDSCAPE -> rawWidth < rawHeight
            ORIENTATION_PORTRAIT -> rawWidth > rawHeight
            else -> false
        }
    return PreviewUiMediaScope(
        windowInfo = windowInfo,
        windowPosture =
            when (params[PARAM_WINDOW_POSTURE]) {
                POSTURE_TABLETOP -> TabletopPosture
                POSTURE_BOOK -> BookPosture
                else -> FlatPosture
            },
        pointerPrecision =
            when (params[PARAM_POINTER_PRECISION]) {
                POINTER_PRECISION_FINE -> UiMediaScope.PointerPrecision.Fine
                POINTER_PRECISION_BLUNT -> UiMediaScope.PointerPrecision.Blunt
                POINTER_PRECISION_NONE -> UiMediaScope.PointerPrecision.None
                else -> UiMediaScope.PointerPrecision.Coarse
            },
        keyboardKind =
            when (params[PARAM_KEYBOARD_KIND]) {
                KEYBOARD_KIND_PHYSICAL -> UiMediaScope.KeyboardKind.Physical
                KEYBOARD_KIND_NONE -> UiMediaScope.KeyboardKind.None
                else -> UiMediaScope.KeyboardKind.Virtual
            },
        hasCamera = params[PARAM_HAS_CAMERA]?.toBooleanStrictOrNull() ?: true,
        hasMicrophone = params[PARAM_HAS_MICROPHONE]?.toBooleanStrictOrNull() ?: true,
        viewingDistance =
            when (params[PARAM_VIEWING_DISTANCE]) {
                VIEWING_DISTANCE_FAR -> UiMediaScope.ViewingDistance.Far
                VIEWING_DISTANCE_MEDIUM -> UiMediaScope.ViewingDistance.Medium
                else -> UiMediaScope.ViewingDistance.Near
            },
        deviceWidth = if (swapDimensions) rawHeight else rawWidth,
        deviceHeight = if (swapDimensions) rawWidth else rawHeight,
        reportDeviceWidth = reportDeviceWidth,
        reportDeviceHeight = reportDeviceHeight,
    )
}
