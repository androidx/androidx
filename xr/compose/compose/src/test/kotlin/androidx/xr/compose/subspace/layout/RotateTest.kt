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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [rotate] modifier. */
@RunWith(AndroidJUnit4::class)
class RotateTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun rotation_canApplySingleRotation() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").rotate(90.0f, 0.0f, 0.0f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(Quaternion(0.70710677f, 0.0f, 0.0f, 0.70710677f))
    }

    @Test
    fun rotation_canRotationAcrossTwoAxis() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").rotate(Vector3(0.0f, 1.0f, 1.0f), 90.0f)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, 0.49999997f, 0.49999997f, 0.70710677f))
    }
}
