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

package androidx.xr.compose.unit

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DpVolumeSizeTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val UNIT_DENSITY = Density(density = 1.0f, fontScale = 1.0f)
    private lateinit var session: Session

    @Before
    fun setUp() {
        session = composeTestRule.configureFakeSession()
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
        val dpVolumeSize = DpVolumeSize(1000.dp, 1000.dp, 1000.dp)

        val dimensions =
            dpVolumeSize.toDimensionsInMeters(UNIT_DENSITY, session.scene.virtualPixelDensity)

        assertThat(dimensions).isEqualTo(FloatSize3d(0.5f, 0.5f, 0.5f))
    }

    @Test
    fun dpVolumeSize_fromMeters_returnsCorrectDpVolumeSize() {
        val dpVolumeSize =
            FloatSize3d(0.5f, 0.5f, 0.5f)
                .toDpVolumeSize(UNIT_DENSITY, session.scene.virtualPixelDensity)

        assertThat(dpVolumeSize).isEqualTo(DpVolumeSize(1000.dp, 1000.dp, 1000.dp))
    }

    @Test
    fun dpVolumeSize_zero_returnsCorrectDpVolumeSize() {
        val zero = DpVolumeSize.Zero

        assertThat(zero).isEqualTo(DpVolumeSize(0f.dp, 0f.dp, 0f.dp))
    }

    @Test
    fun toDimensionsInMeters_andFromMeters_returnsCorrectDpVolumeSize() {
        val testDpVolumeSize = DpVolumeSize(1111.11f.dp, 1111.11f.dp, 1111.11f.dp)

        val dimensions =
            testDpVolumeSize.toDimensionsInMeters(UNIT_DENSITY, session.scene.virtualPixelDensity)
        val fromMetersDpVolumeSize =
            dimensions.toDpVolumeSize(UNIT_DENSITY, session.scene.virtualPixelDensity)

        assertThat(fromMetersDpVolumeSize)
            .isEqualTo(DpVolumeSize(1111.11f.dp, 1111.11f.dp, 1111.11f.dp))
    }

    @Test
    fun toDimensionsInMetersAndFromMeters_whenInfinite_returnsCorrectDpVolumeSize() {
        val testDpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity)

        val floatSize3d =
            testDpVolumeSize.toDimensionsInMeters(UNIT_DENSITY, session.scene.virtualPixelDensity)
        val fromMetersDpVolumeSize =
            floatSize3d.toDpVolumeSize(UNIT_DENSITY, session.scene.virtualPixelDensity)

        assertThat(floatSize3d)
            .isEqualTo(
                FloatSize3d(
                    Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY,
                )
            )
        assertThat(fromMetersDpVolumeSize)
            .isEqualTo(DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity))
    }
}
