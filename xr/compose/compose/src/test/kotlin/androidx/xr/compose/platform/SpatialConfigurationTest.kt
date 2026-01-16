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

package androidx.xr.compose.platform

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialConfigurationTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val hasXrSpatialFeatureText = "Has XR Spatial Feature"

    @Test
    fun hasXrSpatialFeature_nonXr_isFalse() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent {
            if (LocalSpatialConfiguration.current.hasXrSpatialFeature) {
                Text(hasXrSpatialFeatureText)
            }
        }

        composeTestRule.onNodeWithText(hasXrSpatialFeatureText).assertDoesNotExist()
    }

    @Test
    fun requestFullSpaceMode_nonXr_throwsException() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent {
            assertFailsWith<UnsupportedOperationException> {
                @Suppress("DEPRECATION") LocalSpatialConfiguration.current.requestFullSpaceMode()
            }
        }
    }

    @Test
    fun requestHomeSpaceMode_nonXr_throwsException() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent {
            assertFailsWith<UnsupportedOperationException> {
                @Suppress("DEPRECATION") LocalSpatialConfiguration.current.requestHomeSpaceMode()
            }
        }
    }

    @Test
    fun requestModeChange_changesBounds() {
        var configuration: SpatialConfiguration? = null

        composeTestRule.setContent {
            configuration = LocalSpatialConfiguration.current
            if (configuration.bounds == DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity)) {
                Text("Full")
            } else {
                Text("Home")
            }
        }

        composeTestRule.onNodeWithText("Full").assertExists()
        composeTestRule.runOnIdle { @Suppress("DEPRECATION") configuration?.requestHomeSpaceMode() }
        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.runOnIdle { @Suppress("DEPRECATION") configuration?.requestFullSpaceMode() }
        composeTestRule.onNodeWithText("Full").assertExists()
    }

    @Test
    fun hasXrSpatialFeature_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            if (LocalSpatialConfiguration.current.hasXrSpatialFeature) {
                Text(hasXrSpatialFeatureText)
            }
        }

        composeTestRule.onNodeWithText(hasXrSpatialFeatureText).assertExists()
    }

    @Test
    fun hasXrSpatialFeature_homeSpaceMode_returnsTrue() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            if (LocalSpatialConfiguration.current.hasXrSpatialFeature) {
                Text(hasXrSpatialFeatureText)
            }
        }

        composeTestRule.onNodeWithText(hasXrSpatialFeatureText).assertExists()
    }

    @Test
    fun bounds_homeSpaceMode_isPositiveAndNotMax() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        var bounds: DpVolumeSize? = null
        composeTestRule.setContent { bounds = LocalSpatialConfiguration.current.bounds }
        composeTestRule.waitForIdle()

        val bounds1 = checkNotNull(bounds)
        assertThat(bounds1.width).isNotEqualTo(Dp.Infinity)
        assertThat(bounds1.width).isGreaterThan(0.dp)
        assertThat(bounds1.height).isNotEqualTo(Dp.Infinity)
        assertThat(bounds1.height).isGreaterThan(0.dp)
        assertThat(bounds1.depth).isNotEqualTo(Dp.Infinity)
        assertThat(bounds1.depth).isGreaterThan(0.dp)
    }

    @Test
    fun bounds_fullSpaceMode_isMax() {
        var bounds: DpVolumeSize? = null
        composeTestRule.setContent { bounds = LocalSpatialConfiguration.current.bounds }
        composeTestRule.waitForIdle()

        val bounds1 = checkNotNull(bounds)
        assertThat(bounds1.width).isEqualTo(Dp.Infinity)
        assertThat(bounds1.height).isEqualTo(Dp.Infinity)
        assertThat(bounds1.depth).isEqualTo(Dp.Infinity)
    }

    @Test
    fun bounds_nonXr_equalsViewSize() {
        composeTestRule.activity.disableXr()

        var bounds: DpVolumeSize? = null
        composeTestRule.setContent { bounds = LocalSpatialConfiguration.current.bounds }
        composeTestRule.waitForIdle()

        // 320x470 is the default screen size returned by the testing architecture.
        val bounds1 = checkNotNull(bounds)
        assertThat(bounds1.width).isEqualTo(320.dp)
        assertThat(bounds1.height).isEqualTo(470.dp)
        assertThat(bounds1.depth).isEqualTo(0.dp)
    }
}
