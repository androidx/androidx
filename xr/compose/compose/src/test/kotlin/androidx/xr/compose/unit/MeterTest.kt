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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.runtime.Session
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeterTest {
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
    fun float_metersToPx() {
        assertThat(1f.metersToPx(session.scene.virtualPixelDensity)).isEqualTo(2000f)
    }

    @Test
    fun float_pxToMeters() {
        assertThat(2000f.pxToMeters(session.scene.virtualPixelDensity)).isEqualTo(1f)
    }

    @Test
    fun int_pxToMeters() {
        assertThat(2000.pxToMeters(session.scene.virtualPixelDensity)).isEqualTo(1f)
    }

    @Test
    fun float_roundMetersToPx() {
        assertThat(1.5f.roundMetersToPx(session.scene.virtualPixelDensity)).isEqualTo(3000)
    }

    @Test
    fun dp_toMeters() {
        assertThat(2000.dp.toMeters(UNIT_DENSITY, session.scene.virtualPixelDensity)).isEqualTo(1f)
    }

    @Test
    fun float_metersToDp() {
        assertThat(1f.metersToDp(UNIT_DENSITY, session.scene.virtualPixelDensity))
            .isEqualTo(2000.dp)
    }
}
