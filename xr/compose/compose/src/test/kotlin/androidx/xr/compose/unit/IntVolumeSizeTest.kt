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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntVolumeSizeTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()
    private lateinit var session: Session

    @Before
    fun setUp() {
        session = composeTestRule.configureFakeSession(defaultDpPerMeter = 2000f)
    }

    @Test
    fun intVolumeSize_toString_returnsString() {
        val intVolumeSize = IntVolumeSize(0, 0, 0)

        val toString = intVolumeSize.toString()

        assertThat(toString).isEqualTo("IntVolumeSize(width=0, height=0, depth=0)")
    }

    @Test
    fun toDimensionsInMeters_returnsCorrectDimensions() {
        val intVolumeSize = IntVolumeSize(2000, 2000, 2000)

        val dimensions = intVolumeSize.toDimensionsInMeters(session.scene.virtualPixelDensity)

        assertThat(dimensions.width).isWithin(0.0003f).of(1.0f)
        assertThat(dimensions.height).isWithin(0.0003f).of(1.0f)
        assertThat(dimensions.depth).isWithin(0.0003f).of(1.0f)
    }

    @Test
    fun intVolumeSize_zero_returnsCorrectIntVolumeSize() {
        val intVolumeSize = IntVolumeSize.Zero

        assertThat(intVolumeSize).isEqualTo(IntVolumeSize(0, 0, 0))
    }

    @Test
    fun intVolumeSize_fromMeters_returnsCorrectIntVolumeSize() {
        val dimensions = FloatSize3d(1.0f, 1.0f, 1.0f)

        val intVolumeSize = dimensions.toIntVolumeSize(session.scene.virtualPixelDensity)

        assertThat(intVolumeSize).isEqualTo(IntVolumeSize(2000, 2000, 2000))
    }

    @Test
    fun toDimensionsInMeters_andFromMeters_returnsCorrectIntVolumeSize() {
        val intVolumeSize = IntVolumeSize(1000, 1000, 1000)

        val dimensions = intVolumeSize.toDimensionsInMeters(session.scene.virtualPixelDensity)
        val fromMetersIntVolumeSize = dimensions.toIntVolumeSize(session.scene.virtualPixelDensity)

        assertThat(fromMetersIntVolumeSize).isEqualTo(IntVolumeSize(1000, 1000, 1000))
    }
}
