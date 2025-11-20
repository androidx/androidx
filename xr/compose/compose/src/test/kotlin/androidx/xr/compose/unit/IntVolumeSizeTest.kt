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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider
import com.android.extensions.xr.ShadowConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntVolumeSizeTest {
    private val UNIT_DENSITY = Density(density = 1.0f, fontScale = 1.0f)

    @Before
    fun setUp() {
        ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
            .setDefaultDpPerMeter(1f)
    }

    @Test
    fun intVolumeSize_toString_returnsString() {
        val intVolumeSize = IntVolumeSize(0, 0, 0)

        val toString = intVolumeSize.toString()

        assertThat(toString).isEqualTo("IntVolumeSize(width=0, height=0, depth=0)")
    }

    @Test
    fun toDimensionsInMeters_returnsCorrectDimensions() {
        val intVolumeSize = IntVolumeSize(9, 9, 9)

        val dimensions = intVolumeSize.toDimensionsInMeters(UNIT_DENSITY)

        assertThat(dimensions.width).isWithin(0.0003f).of(9.0f)
        assertThat(dimensions.height).isWithin(0.0003f).of(9.0f)
        assertThat(dimensions.depth).isWithin(0.0003f).of(9.0f)
    }

    @Test
    fun toDimensionsInMeters_returnsCorrectDimensions_doubleDensity() {
        val intVolumeSize = IntVolumeSize(9, 9, 9)
        val DOUBLE_DENSITY = Density(density = 2.0f, fontScale = 2.0f)

        val dimensions = intVolumeSize.toDimensionsInMeters(DOUBLE_DENSITY)

        // When pixels are twice as dense, we expect the Meters equivalent to be half.
        assertThat(dimensions.width).isWithin(0.0002f).of(4.5f)
        assertThat(dimensions.height).isWithin(0.0002f).of(4.5f)
        assertThat(dimensions.depth).isWithin(0.0002f).of(4.5f)
    }

    @Test
    fun intVolumeSize_zero_returnsCorrectIntVolumeSize() {
        val intVolumeSize = IntVolumeSize.Zero

        assertThat(intVolumeSize).isEqualTo(IntVolumeSize(0, 0, 0))
    }

    @Test
    fun intVolumeSize_fromMeters_returnsCorrectIntVolumeSize() {
        val dimensions = FloatSize3d(9.0f, 9.0f, 9.0f)

        val intVolumeSize = dimensions.toIntVolumeSize(UNIT_DENSITY)

        assertThat(intVolumeSize).isEqualTo(IntVolumeSize(9, 9, 9))
    }

    @Test
    fun toDimensionsInMeters_andFromMeters_returnsCorrectIntVolumeSize() {
        val intVolumeSize = IntVolumeSize(10000, 10000, 10000)

        val dimensions = intVolumeSize.toDimensionsInMeters(UNIT_DENSITY)
        val fromMetersIntVolumeSize = dimensions.toIntVolumeSize(UNIT_DENSITY)

        assertThat(fromMetersIntVolumeSize).isEqualTo(IntVolumeSize(10000, 10000, 10000))
    }
}
