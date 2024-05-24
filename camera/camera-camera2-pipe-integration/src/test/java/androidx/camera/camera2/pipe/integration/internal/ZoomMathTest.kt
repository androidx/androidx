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

import androidx.camera.camera2.pipe.integration.internal.ZoomMath.getLinearZoomFromZoomRatio
import androidx.camera.camera2.pipe.integration.internal.ZoomMath.getZoomRatioFromLinearZoom
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val CROP_REGION_TOLERANCE = 5f

class ZoomMathTest {
    private val minZoomRatio = 0.6f
    private val maxZoomRatio = 8f

    @Test
    fun getLinearZoomFromZoomRatio_zoomRatioIsMin_linearZoomIs0() {
        val linearZoom =
            getLinearZoomFromZoomRatio(
                zoomRatio = minZoomRatio,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )

        assertThat(linearZoom).isEqualTo(0f)
    }

    @Test
    fun getLinearZoomFromZoomRatio_zoomRatioIsMax_linearZoomIs1() {
        val linearZoom =
            getLinearZoomFromZoomRatio(
                zoomRatio = maxZoomRatio,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )

        assertThat(linearZoom).isEqualTo(1f)
    }

    @Test
    fun getLinearZoomFromZoomRatio_zoomUnsupported_linearZoomIs0() {
        // zoom unsupported means minZoomRatio = maxZoomRatio
        val linearZoom =
            getLinearZoomFromZoomRatio(zoomRatio = 1.0f, minZoomRatio = 1.0f, maxZoomRatio = 1.0f)

        assertThat(linearZoom).isEqualTo(0f)
    }

    @Test
    fun getZoomRatioFromLinearZoom_linearZoomIs0_zoomRatioIsMin() {
        val zoomRatio =
            getZoomRatioFromLinearZoom(
                linearZoom = 0f,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )

        assertThat(zoomRatio).isEqualTo(minZoomRatio)
    }

    @Test
    fun getZoomRatioFromLinearZoom_linearZoomIs1_zoomRatioIsMax() {
        val zoomRatio =
            getZoomRatioFromLinearZoom(
                linearZoom = 1.0f,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )

        assertThat(zoomRatio).isEqualTo(maxZoomRatio)
    }

    @Test
    fun getZoomRatioFromLinearZoom_zoomUnsupportedAndLinearZoom0_zoomRatioIsTheAllowedValue() {
        // zoom unsupported means minZoomRatio = maxZoomRatio
        val zoomRatio =
            getZoomRatioFromLinearZoom(linearZoom = 0f, minZoomRatio = 1.0f, maxZoomRatio = 1.0f)

        assertThat(zoomRatio).isEqualTo(1.0f)
    }

    @Test
    fun getZoomRatioFromLinearZoom_zoomUnsupportedAndLinearZoom0_5f_zoomRatioIsTheAllowedValue() {
        // zoom unsupported means minZoomRatio = maxZoomRatio
        val zoomRatio =
            getZoomRatioFromLinearZoom(linearZoom = 0.5f, minZoomRatio = 1.0f, maxZoomRatio = 1.0f)

        assertThat(zoomRatio).isEqualTo(1.0f)
    }

    @Test
    fun getZoomRatioFromLinearZoom_zoomUnsupportedAndLinearZoom1_zoomRatioIsTheAllowedValue() {
        // zoom unsupported means minZoomRatio = maxZoomRatio
        val zoomRatio =
            getZoomRatioFromLinearZoom(linearZoom = 1.0f, minZoomRatio = 1.0f, maxZoomRatio = 1.0f)

        assertThat(zoomRatio).isEqualTo(1.0f)
    }

    @Test
    fun getLinearZoomFromZoomRatio_getZoomRatioFromLinearZoomReturnsSameRatio() {
        val linearZoom =
            getLinearZoomFromZoomRatio(
                zoomRatio = 2f,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )

        val zoomRatio =
            getZoomRatioFromLinearZoom(
                linearZoom = linearZoom,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )

        assertThat(zoomRatio).isEqualTo(2f)
    }

    @Test
    fun linearZoomIs0_5f_cropWidthIsHalf() {
        val sensorRegionWidth = 10000f
        val minZoomCropWidth = sensorRegionWidth / minZoomRatio
        val maxZoomCropWidth = sensorRegionWidth / maxZoomRatio

        val zoomRatio =
            getZoomRatioFromLinearZoom(
                linearZoom = 0.5f,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )
        val cropWidth = sensorRegionWidth / zoomRatio

        assertThat(cropWidth)
            .isWithin(CROP_REGION_TOLERANCE)
            .of((minZoomCropWidth + maxZoomCropWidth) / 2)
    }

    @Test
    fun linearZoomIsIncreasedProgressively_cropWidthIsChangedLinearly() {
        val sensorRegionWidth = 10000f
        var previousCropWidth = sensorRegionWidth / minZoomRatio
        var previousCropWidthDiff = Float.NaN

        var linearZoom = 0.1f

        while (linearZoom < 1f) {
            val zoomRatio =
                getZoomRatioFromLinearZoom(
                    linearZoom = linearZoom,
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio
                )

            val cropWidth = sensorRegionWidth / zoomRatio
            val cropWidthDiff = previousCropWidth - cropWidth

            if (!previousCropWidthDiff.isNaN()) {
                assertThat(cropWidthDiff).isWithin(CROP_REGION_TOLERANCE).of(previousCropWidthDiff)
            }

            previousCropWidthDiff = cropWidthDiff
            previousCropWidth = cropWidth
            linearZoom += 0.1f
        }
    }
}
