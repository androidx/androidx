/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.unit

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.unit.Meter.Companion.centimeters
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.compose.unit.Meter.Companion.millimeters
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider
import com.android.extensions.xr.ShadowConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeterTest {
    private val UNIT_DENSITY = Density(density = 1.0f, fontScale = 1.0f)

    @Before
    fun setUp() {
        ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
            .setDefaultDpPerMeter(1f)
    }

    @Test
    fun meter_toDp() {
        assertThat(1.meters.toDp()).isEqualTo(1.dp)
    }

    @Test
    fun roundToPx_roundsToNearestPixel() {
        assertThat(1.meters.roundToPx(UNIT_DENSITY)).isEqualTo(1)
    }

    @Test
    fun roundToPx_roundsToNearestPixel_doubleDensity() {
        val DOUBLE_DENSITY = Density(density = 2.0f, fontScale = 2.0f)

        // Twice the density, twice the pixels.
        assertThat(1.meters.roundToPx(DOUBLE_DENSITY)).isEqualTo(2)
    }

    @Test
    fun dp_toMeter() {
        assertThat(10.dp.toMeter()).isEqualTo(Meter(10f))
        assertThat(Dp.Infinity.toMeter()).isEqualTo(Meter.Infinity)
        assertThat(Dp.Unspecified.toMeter()).isEqualTo(Meter.NaN)

        // Reference: "Hairline elements take up no space, but will draw a single pixel, independent
        // of the device's resolution and density."
        // https://developer.android.com/reference/kotlin/androidx/compose/ui/unit/Dp#Hairline()
        //
        // Currently, hairlines are exactly 0 meters.
        assertThat(Dp.Hairline.toMeter()).isEqualTo(Meter(0.0f))
    }

    @Test
    fun meter_toMm() {
        assertThat(22.meters.toMm()).isEqualTo(22000f)
    }

    @Test
    fun meter_toCm() {
        assertThat(11.meters.toCm()).isEqualTo(1100f)
    }

    @Test
    fun meter_toM() {
        assertThat(11.meters.toM()).isEqualTo(11f)
    }

    @Test
    fun meter_toPx() {
        assertThat(5.meters.toPx(UNIT_DENSITY)).isEqualTo(5f)
    }

    @Test
    fun meter_roundToPx() {
        assertThat(5.meters.roundToPx(UNIT_DENSITY)).isEqualTo(5)
    }

    @Test
    fun meter_todp() {
        assertThat(5.meters.toDp()).isEqualTo(5.dp)
    }

    @Test
    fun meter_isSpecified() {
        assertThat(5.meters.isSpecified).isTrue()
        assertThat(Meter.NaN.isSpecified).isFalse()
    }

    @Test
    fun meter_isFinite() {
        assertThat(5.meters.isFinite).isTrue()
        assertThat(Meter.Infinity.isFinite).isFalse()
    }

    @Test
    fun meter_fromIntMilli() {
        val test: Meter = 22.millimeters
        assertThat(test).isEqualTo(Meter(0.022000002f))
    }

    @Test
    fun meter_funFloatMilli() {
        val test = 22f.millimeters
        assertThat(test).isEqualTo(Meter(0.022000002f))
    }

    @Test
    fun meter_funDoubleMilli() {
        val test = 22.0.millimeters
        assertThat(test).isEqualTo(Meter(0.022000002f))
    }

    @Test
    fun meter_funIntCm() {
        val test = 22.centimeters
        assertThat(test).isEqualTo(Meter(0.22f))
    }

    @Test
    fun meter_funFloatCm() {
        val test = 22f.centimeters
        assertThat(test).isEqualTo(Meter(0.22f))
    }

    @Test
    fun meter_funDoubleCm() {
        val test = 22.0.centimeters
        assertThat(test).isEqualTo(Meter(0.22f))
    }

    @Test
    fun meter_funIntM() {
        val test = 22.meters
        assertThat(test).isEqualTo(Meter(22f))
    }

    @Test
    fun meter_funFloatM() {
        val test = 22f.meters
        assertThat(test).isEqualTo(Meter(22f))
    }

    @Test
    fun meter_funDoubleM() {
        val test = 22.0.meters
        assertThat(test).isEqualTo(Meter(22f))
    }

    @Test
    fun intTimesMeters() {
        val test: Meter = 22.meters
        assertThat(11 * test).isEqualTo(242.meters)
    }

    @Test
    fun floatTimesMeters() {
        val test: Meter = 22.meters
        assertThat(11f * test).isEqualTo(242.meters)
    }

    @Test
    fun doubleTimesMeters() {
        val test: Meter = 22.meters
        assertThat(11.0 * test).isEqualTo(242.meters)
    }

    @Test
    fun intDivMeters() {
        val test: Meter = 22.meters
        assertThat(test / 11).isEqualTo(2.meters)
    }

    @Test
    fun floatDivMeters() {
        val test: Meter = 22.meters
        assertThat(test / 11f).isEqualTo(2.meters)
    }

    @Test
    fun doubleDivMeters() {
        val test: Meter = 22.meters
        assertThat(test / 11.0).isEqualTo(2.meters)
    }

    @Test
    fun px_toMeter_toPx() {
        val density = Density(2.789f)
        assertThat(Meter.fromPixel(28.9f, density).toPx(density)).isWithin(1.0e-5f).of(28.9f)
    }

    @Test
    fun dpPerMeter_getterReevaluatedOnEachCall() {
        val extensions = XrExtensionsProvider.getXrExtensions()!!
        val shadowConfig = ShadowConfig.extract(extensions.config!!)

        shadowConfig.setDefaultDpPerMeter(100f)

        assertThat(1.meters.toDp()).isEqualTo(100.dp)

        shadowConfig.setDefaultDpPerMeter(500f)

        assertThat(1.meters.toDp()).isEqualTo(500.dp)
    }
}
