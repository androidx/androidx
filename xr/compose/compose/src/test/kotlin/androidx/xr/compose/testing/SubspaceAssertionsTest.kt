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

package androidx.xr.compose.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.runtime.math.Quaternion
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspaceAssertionsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun assertRotationIsEqualTo_withZeroTolerance_succeedsOnExactMatch() {
        val expectedRotation = Quaternion.fromEulerAngles(pitch = 45f, yaw = 0f, roll = 0f)
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.size(100.dp).rotate(expectedRotation).testTag("panel")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationIsEqualTo(expectedRotation, tolerance = 0.0f)
    }

    @Test
    fun assertRotationInRootIsEqualTo_withZeroTolerance_succeedsOnExactMatch() {
        val expectedRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 90f, roll = 0f)
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.size(100.dp).rotate(expectedRotation).testTag("panel")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.0f)
    }

    @Test
    fun assertRotationIsEqualTo_whenExceedingTolerance_throwsAssertionError() {
        val rotation = Quaternion.fromEulerAngles(pitch = 45f, yaw = 0f, roll = 0f)
        val differentRotation = Quaternion.fromEulerAngles(pitch = 50f, yaw = 0f, roll = 0f)
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp).rotate(rotation).testTag("panel")) {}
            }
        }

        assertFailsWith<AssertionError> {
            composeTestRule
                .onSubspaceNodeWithTag("panel")
                .assertRotationIsEqualTo(differentRotation, tolerance = 1.0f)
        }
    }
}
