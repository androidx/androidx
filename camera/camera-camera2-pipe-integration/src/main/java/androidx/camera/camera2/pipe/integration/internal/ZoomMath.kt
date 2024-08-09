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

package androidx.camera.camera2.pipe.integration.internal

import androidx.core.math.MathUtils
import kotlin.math.abs

/**
 * This class is used for containing the mathematical calculations for ZoomControl, mainly the
 * conversions between zoomRatio and linearZoom.
 *
 * The linearZoom is the percentage of zoom amount i.e. how much cropWidth is being used, so
 * linearZoom = 0.5 should represent the middle point of [minZoomCropWidth, maxZoomCropWidth] range.
 * But that does not mean it should be the same as (minZoom + maxZoom) / 2. For example, consider
 * the case where original cropWidth = 10000 for zoomRatio = 1.0f, minZoomRatio = 1.0f, maxZoomRatio
 * = 10.0f, zoomRatio = 5.5f may not represent linearZoom = 0.5 i.e. the half zoom amount. Here,
 * zoomRatio = 1.0f, cropWidth = 10000, zoomRatio = 5.5f, cropWidth = 1818.18 zoomRatio = 10.0f,
 * cropWidth = 1000 As observed, zoomRatio = 5.5f does not yield cropWidth = 5500 which would be the
 * actual zooming amount middle point.
 */
public object ZoomMath {
    public fun getLinearZoomFromZoomRatio(
        zoomRatio: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float
    ): Float {
        // if zoom is not supported i.e. minZoomRatio = maxZoomRatio, return 0
        if (areFloatsEqual(minZoomRatio, maxZoomRatio)) {
            return 0f
        }

        if (nearZero(zoomRatio)) {
            return 0f
        }

        if (areFloatsEqual(zoomRatio, maxZoomRatio)) {
            return 1f
        } else if (areFloatsEqual(zoomRatio, minZoomRatio)) {
            return 0f
        }

        /**
         * linearZoom should represent the percentage of zoom amount based on how much cropWidth is
         * visible.
         *
         * The original sensor region width is considered as 1.0f here as we only need the
         * linearZoom ratio, not the actual crop width.
         */
        val relativeCropWidth = 1.0f / zoomRatio
        val relativeCropWidthInMaxZoom = 1.0f / maxZoomRatio
        val relativeCropWidthInMinZoom = 1.0f / minZoomRatio

        val linearZoom =
            (relativeCropWidthInMinZoom - relativeCropWidth) /
                (relativeCropWidthInMinZoom - relativeCropWidthInMaxZoom)

        return MathUtils.clamp(linearZoom, 0f, 1.0f)
    }

    public fun getZoomRatioFromLinearZoom(
        linearZoom: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float
    ): Float {
        if (areFloatsEqual(linearZoom, 1.0f)) {
            return maxZoomRatio
        } else if (areFloatsEqual(linearZoom, 0f)) {
            return minZoomRatio
        }

        /**
         * This crop width is proportional to the real crop width. The real crop with = sensorWidth/
         * zoomRatio, but we need the ratio only so we can assume sensorWidth as 1.0f.
         */
        val relativeCropWidthInMaxZoom = 1.0f / maxZoomRatio
        val relativeCropWidthInMinZoom = 1.0f / minZoomRatio

        val cropWidth =
            relativeCropWidthInMinZoom -
                (relativeCropWidthInMinZoom - relativeCropWidthInMaxZoom) * linearZoom

        val ratio = 1.0f / cropWidth

        return MathUtils.clamp(ratio, minZoomRatio, maxZoomRatio)
    }

    private fun areFloatsEqual(num1: Float, num2: Float): Boolean {
        return nearZero(num1 - num2)
    }

    internal fun nearZero(num: Float): Boolean {
        return abs(num) < 2.0 * Math.ulp(abs(num))
    }
}
