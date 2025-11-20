/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.view

import kotlin.math.roundToInt

internal object ExternalInputUtils {
    fun calculateGreaterZoom(
        currentZoom: Float,
        defaultZoom: Float,
        zoomLevels: List<Float>,
        maxAbsoluteZoom: Float,
    ): Float {
        val currentZoomLevel = (currentZoom / defaultZoom)
        // Jump to the next to next zoom level if the next zoom level is too close.
        // If no higher level is found, return the absolute max
        val zoomFactor =
            zoomLevels.firstOrNull { it > currentZoomLevel + 0.01f } ?: return maxAbsoluteZoom
        return zoomFactor * defaultZoom
    }

    fun calculateScroll(viewportDimension: Int, scrollFactor: Int): Int {
        return (viewportDimension.toFloat() / scrollFactor).roundToInt()
    }

    fun calculateSmallerZoom(
        currentZoom: Float,
        defaultZoom: Float,
        zoomLevels: List<Float>,
        minAbsoluteZoom: Float,
    ): Float {
        val currentZoomLevel = (currentZoom / defaultZoom)
        // Jump to the last to last zoom level if the last zoom level is too close.
        // If no smaller level is found, return the absolute min
        val zoomFactor =
            zoomLevels.reversed().firstOrNull { it < currentZoomLevel - 0.01f }
                ?: return minAbsoluteZoom
        return zoomFactor * defaultZoom
    }
}
