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

private const val DP_SUFFIX = "dp"

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

private fun parseDp(value: String?): Dp? = value?.removeSuffix(DP_SUFFIX)?.toFloatOrNull()?.dp

/**
 * Preview-specific implementation of [UiMediaScope].
 *
 * Supplies parsed media query properties and resolves [windowWidth] and [windowHeight] dynamically
 * from `WindowInfo.containerDpSize` when positive, falling back to the configured default
 * dimensions.
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
    private val defaultWidth: Dp = 411.dp,
    private val defaultHeight: Dp = 891.dp,
) : UiMediaScope {
    @get:FrequentlyChangingValue
    override val windowWidth: Dp
        get() = windowInfo.containerDpSize.width.takeIf { it > 0.dp } ?: defaultWidth

    @get:FrequentlyChangingValue
    override val windowHeight: Dp
        get() = windowInfo.containerDpSize.height.takeIf { it > 0.dp } ?: defaultHeight
}

/**
 * Parses a preview device specification string (e.g. `spec:width=411dp,height=891dp,...`) or
 * returns a default [UiMediaScope] for Phone / Default device if omitted or not a `spec:` string.
 *
 * Uses full [UiMediaScope] property names: `windowPosture`, `pointerPrecision`, `keyboardKind`,
 * `hasCamera`, `hasMicrophone`, and `viewingDistance`.
 */
@OptIn(ExperimentalMediaQueryApi::class)
internal fun createPreviewUiMediaScope(
    windowInfo: WindowInfo,
    deviceSpec: String? = null,
): UiMediaScope {
    if (deviceSpec.isNullOrEmpty() || !deviceSpec.startsWith(SPEC_PREFIX)) {
        return PreviewUiMediaScope(windowInfo)
    }

    val params =
        deviceSpec
            .removePrefix(SPEC_PREFIX)
            .split(',')
            .mapNotNull {
                val parts = it.split('=', limit = 2)
                if (parts.size == 2) {
                    parts[0].trim().lowercase() to parts[1].trim().lowercase()
                } else null
            }
            .toMap()

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
        defaultWidth = parseDp(params[PARAM_WIDTH]) ?: 411.dp,
        defaultHeight = parseDp(params[PARAM_HEIGHT]) ?: 891.dp,
    )
}
