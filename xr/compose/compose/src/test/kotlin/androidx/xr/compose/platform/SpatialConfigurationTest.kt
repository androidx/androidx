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
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.unit.DpVolumeSize
import com.google.common.truth.Truth.assertThat
import java.lang.UnsupportedOperationException
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialConfigurationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun hasXrSpatialFeature_nonXr_isFalse() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Text(text = "${LocalSpatialConfiguration.current.hasXrSpatialFeature}")
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun requestFullSpaceMode_nonXr_throwsException() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                assertFailsWith<UnsupportedOperationException> {
                    LocalSpatialConfiguration.current.requestFullSpaceMode()
                }
            }
        }
    }

    @Test
    fun requestHomeSpaceMode_nonXr_throwsException() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                assertFailsWith<UnsupportedOperationException> {
                    LocalSpatialConfiguration.current.requestHomeSpaceMode()
                }
            }
        }
    }

    @Test
    fun requestModeChange_changesBounds() {
        var configuration: SpatialConfiguration? = null

        composeTestRule.setContent {
            TestSetup {
                configuration = LocalSpatialConfiguration.current
                if (configuration.bounds == DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity)) {
                    Text("Full")
                } else {
                    Text("Home")
                }
            }
        }

        composeTestRule.onNodeWithText("Full").assertExists()
        configuration?.requestHomeSpaceMode()
        composeTestRule.onNodeWithText("Home").assertExists()
        configuration?.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Full").assertExists()
    }

    @Test
    fun hasXrSpatialFeature_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            TestSetup { Text(text = "${LocalSpatialConfiguration.current.hasXrSpatialFeature}") }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun hasXrSpatialFeature_homeSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text(text = "${LocalSpatialConfiguration.current.hasXrSpatialFeature}")
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun bounds_homeSpaceMode_isPositiveAndNotMax() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                assertThat(LocalSpatialConfiguration.current.bounds.width).isNotEqualTo(Dp.Infinity)
                assertThat(LocalSpatialConfiguration.current.bounds.width).isGreaterThan(0.dp)
                assertThat(LocalSpatialConfiguration.current.bounds.height)
                    .isNotEqualTo(Dp.Infinity)
                assertThat(LocalSpatialConfiguration.current.bounds.height).isGreaterThan(0.dp)
                assertThat(LocalSpatialConfiguration.current.bounds.depth).isNotEqualTo(Dp.Infinity)
                assertThat(LocalSpatialConfiguration.current.bounds.depth).isGreaterThan(0.dp)
            }
        }
    }

    @Test
    fun bounds_fullSpaceMode_isMax() {
        composeTestRule.setContent {
            TestSetup {
                assertThat(LocalSpatialConfiguration.current.bounds.width).isEqualTo(Dp.Infinity)
                assertThat(LocalSpatialConfiguration.current.bounds.height).isEqualTo(Dp.Infinity)
                assertThat(LocalSpatialConfiguration.current.bounds.depth).isEqualTo(Dp.Infinity)
            }
        }
    }

    @Test
    fun bounds_nonXr_equalsViewSize() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                // 320x470 is the default screen size returned by the testing architecture.
                assertThat(LocalSpatialConfiguration.current.bounds.width).isEqualTo(320.dp)
                assertThat(LocalSpatialConfiguration.current.bounds.height).isEqualTo(470.dp)
                assertThat(LocalSpatialConfiguration.current.bounds.depth).isEqualTo(0.dp)
            }
        }
    }
}
