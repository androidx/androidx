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

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider
import com.android.extensions.xr.ShadowConfig
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DpVolumeSizeTest {
    @Before
    fun setUp() {
        ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
            .setDefaultDpPerMeter(1f)
    }

    @Test
    fun dpVolumeSize_isCreated() {
        val dpVolumeSize = DpVolumeSize(0.dp, 0.dp, 0.dp)

        assertNotNull(dpVolumeSize)
    }

    @Test
    fun dpVolumeSize_toString_returnsString() {
        val dpVolumeSize = DpVolumeSize(0.dp, 0.dp, 0.dp)

        val toString = dpVolumeSize.toString()

        assertThat(toString).isEqualTo("DpVolumeSize(width=0.0.dp, height=0.0.dp, depth=0.0.dp)")
    }

    @Test
    fun toDimensionsInMeter_returnsCorrectDimensions() {
        val dpVolumeSize = DpVolumeSize(1.dp, 1.dp, 1.dp)

        val dimensions = dpVolumeSize.toDimensionsInMeters()

        assertThat(dimensions).isEqualTo(FloatSize3d(1f, 1f, 1f))
    }

    @Test
    fun dpVolumeSize_fromMeters_returnsCorrectDpVolumeSize() {
        val dpVolumeSize = FloatSize3d(1f, 1f, 1f).toDpVolumeSize()

        assertThat(dpVolumeSize).isEqualTo(DpVolumeSize(1.dp, 1.dp, 1.dp))
    }

    @Test
    fun dpVolumeSize_zero_returnsCorrectDpVolumeSize() {
        val zero = DpVolumeSize.Zero

        assertThat(zero).isEqualTo(DpVolumeSize(0f.dp, 0f.dp, 0f.dp))
    }

    @Test
    fun toDimensionsInMeters_andFromMeters_returnsCorrectDpVolumeSize() {
        val testDpVolumeSize = DpVolumeSize(1111.11f.dp, 1111.11f.dp, 1111.11f.dp)

        val dimensions = testDpVolumeSize.toDimensionsInMeters()
        val fromMetersDpVolumeSize = dimensions.toDpVolumeSize()

        assertThat(fromMetersDpVolumeSize)
            .isEqualTo(DpVolumeSize(1111.11f.dp, 1111.11f.dp, 1111.11f.dp))
    }
}
