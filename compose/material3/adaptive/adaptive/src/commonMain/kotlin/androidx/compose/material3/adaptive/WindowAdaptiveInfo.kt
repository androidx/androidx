/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowSizeClass

/**
 * Calculates and returns [WindowAdaptiveInfo] of the provided context. It's a convenient function
 * that uses the default [WindowSizeClass] constructor and the default [Posture] calculation
 * functions to retrieve [WindowSizeClass] and [Posture].
 *
 * Note that this function is meant to replace [currentWindowAdaptiveInfo] and support L and XL
 * width size classes by default.
 *
 * @return [WindowAdaptiveInfo] of the provided context
 */
@Composable
@Suppress("DEPRECATION")
fun currentWindowAdaptiveInfoV2(): WindowAdaptiveInfo =
    currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)

/**
 * Calculates and returns [WindowAdaptiveInfo] of the provided context. It's a convenient function
 * that uses the default [WindowSizeClass] constructor and the default [Posture] calculation
 * functions to retrieve [WindowSizeClass] and [Posture].
 *
 * @param supportLargeAndXLargeWidth `true` to support the large and extra-large window width size
 *   classes, which makes the returned [WindowSizeClass] be calculated based on the breakpoints that
 *   include large and extra-large widths.
 * @return [WindowAdaptiveInfo] of the provided context
 */
@Deprecated(
    message = "Please use V2 version of this function to support L and XL width size classes.",
    replaceWith = ReplaceWith("currentWindowAdaptiveInfoV2"),
    DeprecationLevel.WARNING,
)
@Composable
fun currentWindowAdaptiveInfo(supportLargeAndXLargeWidth: Boolean = false): WindowAdaptiveInfo {
    // Workaround (b/358626778): Directly using WindowInfo.containerDpSize breaks tests based on
    //   DeviceConfigurationOverride.ForcedSize. Those clients need to migrate to
    //   DeviceConfigurationOverride.WindowSize when its available.
    val windowSize =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.toSize().toDpSize() }
    return WindowAdaptiveInfo(
        windowSizeClass =
            if (supportLargeAndXLargeWidth) {
                WindowSizeClass.computeFromDpSizeV2(windowSize)
            } else {
                WindowSizeClass.computeFromDpSize(windowSize)
            },
        windowPosture = calculatePosture(),
    )
}

/**
 * Returns and automatically update the current window size in [DpSize].
 *
 * @return an [DpSize] that represents the current window size.
 */
@Deprecated(
    message = "Going to be removed in the next version. Prefer LocalWindowInfo instead",
    replaceWith = ReplaceWith("LocalWindowInfo.current.containerDpSize"),
    DeprecationLevel.WARNING,
)
@ExperimentalMaterial3AdaptiveApi
@Composable
fun currentWindowDpSize(): DpSize = LocalWindowInfo.current.containerDpSize

/**
 * Returns and automatically update the current window size. It's a convenient function of getting
 * [androidx.compose.ui.platform.WindowInfo.containerSize] from [LocalWindowInfo].
 *
 * @return an [IntSize] that represents the current window size.
 */
@Deprecated(
    message = "Prefer LocalWindowInfo instead",
    replaceWith = ReplaceWith("LocalWindowInfo.current.containerSize"),
    DeprecationLevel.WARNING,
)
@Composable
fun currentWindowSize(): IntSize = LocalWindowInfo.current.containerSize

/**
 * This class collects window info that affects adaptation decisions. An adaptive layout is supposed
 * to use the info from this class to decide how the layout is supposed to be adapted.
 *
 * @param windowSizeClass [WindowSizeClass] of the current window.
 * @param windowPosture [Posture] of the current window.
 * @constructor create an instance of [WindowAdaptiveInfo]
 */
@Immutable
class WindowAdaptiveInfo(val windowSizeClass: WindowSizeClass, val windowPosture: Posture) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WindowAdaptiveInfo) return false
        if (windowSizeClass != other.windowSizeClass) return false
        if (windowPosture != other.windowPosture) return false
        return true
    }

    override fun hashCode(): Int {
        var result = windowSizeClass.hashCode()
        result = 31 * result + windowPosture.hashCode()
        return result
    }

    override fun toString(): String {
        return "WindowAdaptiveInfo(windowSizeClass=$windowSizeClass, windowPosture=$windowPosture)"
    }
}

@Composable internal expect fun calculatePosture(): Posture
