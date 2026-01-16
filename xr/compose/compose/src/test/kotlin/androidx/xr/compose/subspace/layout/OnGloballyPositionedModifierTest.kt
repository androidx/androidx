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

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.toDp
import androidx.xr.compose.unit.IntVolumeSize
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [onGloballyPositioned] modifier. */
@RunWith(AndroidJUnit4::class)
class OnGloballyPositionedModifierTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun onGloballyPositioned_coordinates_positionIsSet() {
        var coordinates: SubspaceLayoutCoordinates? = null
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.offset(20.dp, 20.dp, 20.dp).onGloballyPositioned {
                        coordinates = it
                    }
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(coordinates)
        assertThat(coordinates.poseInRoot.translation.x.toDp()).isEqualTo(20.dp)
        assertThat(coordinates.poseInRoot.translation.y.toDp()).isEqualTo(20.dp)
        assertThat(coordinates.poseInRoot.translation.z.toDp()).isEqualTo(20.dp)
    }

    @Test
    fun onGloballyPositioned_coordinates_sizeIsSet() {
        var coordinates: SubspaceLayoutCoordinates? = null
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.size(100.dp).onGloballyPositioned { coordinates = it }
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(coordinates)
        assertThat(coordinates.size).isEqualTo(IntVolumeSize(100, 100, 100))
    }
}
