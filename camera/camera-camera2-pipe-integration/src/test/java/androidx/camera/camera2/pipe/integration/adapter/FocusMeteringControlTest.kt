/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.graphics.Rect
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.util.Rational
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
public class FocusMeteringControlTest {
    @Test
    public fun meteringRegionsFromMeteringPoint_fovAspectRatioEqualToCropAspectRatio() {
        val meteringPoint = FakeMeteringPointFactory().createPoint(0.0f, 0.0f)
        val meteringRectangles = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint),
            Rect(0, 0, 800, 600),
            Rational(4, 3)
        )
        assertThat(meteringRectangles.size).isEqualTo(1)
        // Aspect ratio of crop region is same as default aspect ratio. So no padding is needed
        // along width or height. However only the bottom right quadrant of the metering rectangle
        // will fit inside the crop region.
        val expectedMeteringRectangle = MeteringRectangle(
            0, 0, 60, 45, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles[0]).isEqualTo(expectedMeteringRectangle)

        val meteringPoint1 = FakeMeteringPointFactory().createPoint(0.5f, 0.5f)
        val meteringRectangles1 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint1),
            Rect(0, 0, 800, 600),
            Rational(4, 3)
        )
        assertThat(meteringRectangles1.size).isEqualTo(1)
        // Aspect ratio of crop region is same as default aspect ratio. So no padding is needed
        // along width or height. The metering region will completely fit inside the crop region.
        val expectedMeteringRectangle1 = MeteringRectangle(
            340, 255, 120, 90, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles1[0]).isEqualTo(expectedMeteringRectangle1)

        val meteringPoint2 = FakeMeteringPointFactory().createPoint(1f, 1f)
        val meteringRectangles2 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint2),
            Rect(0, 0, 800, 600),
            Rational(4, 3)
        )
        assertThat(meteringRectangles2.size).isEqualTo(1)
        // Aspect ratio of crop region is same as default aspect ratio. So no padding is needed
        // along width or height. However only the top left quadrant of the metering rectangle
        // will fit inside the crop region.
        val expectedMeteringRectangle2 = MeteringRectangle(
            740, 555, 60, 45, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles2[0]).isEqualTo(expectedMeteringRectangle2)
    }

    @Test
    public fun meteringRegionsFromMeteringPoint_fovAspectRatioGreaterThanCropAspectRatio() {
        val meteringPoint = FakeMeteringPointFactory().createPoint(0.0f, 0.0f)
        val meteringRectangles = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint),
            Rect(0, 0, 400, 400),
            Rational(4, 3)
        )
        assertThat(meteringRectangles.size).isEqualTo(1)
        // Default aspect ratio is greater than the aspect ratio of the crop region. So we need
        // to add some padding at the top.
        val expectedMeteringRectangle = MeteringRectangle(
            0, 20, 30, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles[0]).isEqualTo(expectedMeteringRectangle)

        val meteringPoint1 = FakeMeteringPointFactory().createPoint(0.5f, 0.5f)
        val meteringRectangles1 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint1),
            Rect(0, 0, 400, 400),
            Rational(4, 3)
        )
        assertThat(meteringRectangles1.size).isEqualTo(1)
        val expectedMeteringRectangle1 = MeteringRectangle(
            170, 170, 60, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles1[0]).isEqualTo(expectedMeteringRectangle1)

        val meteringPoint2 = FakeMeteringPointFactory().createPoint(1f, 1f)
        val meteringRectangles2 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint2),
            Rect(0, 0, 400, 400),
            Rational(4, 3)
        )
        assertThat(meteringRectangles2.size).isEqualTo(1)
        // Default aspect ratio is greater than the aspect ratio of the crop region. So we need
        // to add some padding at the bottom.
        val expectedMeteringRectangle2 = MeteringRectangle(
            370, 320, 30, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles2[0]).isEqualTo(expectedMeteringRectangle2)
    }

    @Test
    public fun meteringRegionsFromMeteringPoint_fovAspectRatioLessThanCropAspectRatio() {
        val meteringPoint = FakeMeteringPointFactory().createPoint(0.0f, 0.0f)
        val meteringRectangles = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint),
            Rect(0, 0, 400, 400),
            Rational(3, 4)
        )
        assertThat(meteringRectangles.size).isEqualTo(1)
        val expectedMeteringRectangle = MeteringRectangle(
            20, 0, 60, 30, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles[0]).isEqualTo(expectedMeteringRectangle)

        val meteringPoint1 = FakeMeteringPointFactory().createPoint(0.5f, 0.5f)
        val meteringRectangles1 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint1),
            Rect(0, 0, 400, 400),
            Rational(3, 4)
        )
        assertThat(meteringRectangles1.size).isEqualTo(1)
        val expectedMeteringRectangle1 = MeteringRectangle(
            170, 170, 60, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles1[0]).isEqualTo(expectedMeteringRectangle1)

        val meteringPoint2 = FakeMeteringPointFactory().createPoint(1f, 1f)
        val meteringRectangles2 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint2),
            Rect(0, 0, 400, 400),
            Rational(3, 4)
        )
        assertThat(meteringRectangles2.size).isEqualTo(1)
        val expectedMeteringRectangle2 = MeteringRectangle(
            320, 370, 60, 30, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles2[0]).isEqualTo(expectedMeteringRectangle2)
    }
}