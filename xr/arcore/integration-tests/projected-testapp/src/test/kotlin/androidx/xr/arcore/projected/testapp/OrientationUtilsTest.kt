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

package androidx.xr.arcore.projected.testapp

import androidx.xr.runtime.math.Quaternion
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class OrientationUtilsTest {

    enum class OrientationTestCase(val quaternion: Quaternion, val expectedDescription: String) {
        // --- Standard Cases ---
        NORTH_LEVEL(Quaternion(0f, 0f, 0f, 1f), "Dir: N (0.0°) | Facing: LEVEL (0.0°)"),
        SOUTH_LEVEL(Quaternion(0f, 1f, 0f, 0f), "Dir: S (180.0°) | Facing: LEVEL (0.0°)"),
        EAST_LEVEL(
            Quaternion(0f, -0.70710678f, 0f, 0.70710678f),
            "Dir: E (90.0°) | Facing: LEVEL (0.0°)",
        ),
        WEST_LEVEL(
            Quaternion(0f, 0.70710678f, 0f, 0.70710678f),
            "Dir: W (270.0°) | Facing: LEVEL (0.0°)",
        ),

        // --- Singularity (Pole) Cases ---
        // Exactly looking straight UP (Gimbal Lock)
        PURE_UP(
            Quaternion(0.70710678f, 0f, 0f, 0.70710678f),
            "Dir: S (180.0°) | Facing: UP (90.0°)",
        ),
        // Exactly looking straight DOWN (Gimbal Lock)
        PURE_DOWN(
            Quaternion(-0.70710678f, 0f, 0f, 0.70710678f),
            "Dir: S (180.0°) | Facing: DOWN (-90.0°)",
        ),
        NORTH_DOWN_80(
            Quaternion(-0.6427876f, 0f, 0f, 0.7660444f),
            "Dir: N (0.0°) | Facing: DOWN (-80.0°)",
        ),
        SOUTH_DOWN_80(
            Quaternion(0f, -0.7660444f, -0.6427876f, 0f),
            "Dir: S (180.0°) | Facing: DOWN (-80.0°)",
        ),
        EAST_DOWN_80(
            Quaternion(-0.4545195f, -0.5416752f, -0.4545195f, 0.5416752f),
            "Dir: E (90.0°) | Facing: DOWN (-80.0°)",
        ),
        WEST_DOWN_80(
            Quaternion(0.4545195f, -0.5416752f, -0.4545195f, -0.5416752f),
            "Dir: W (270.0°) | Facing: DOWN (-80.0°)",
        ),
        NORTH_UP_80(
            Quaternion(0.6427876f, 0f, 0f, 0.7660444f),
            "Dir: N (0.0°) | Facing: UP (80.0°)",
        ),
        SOUTH_UP_80(
            Quaternion(0f, -0.7660444f, 0.6427876f, 0f),
            "Dir: S (180.0°) | Facing: UP (80.0°)",
        ),
        EAST_UP_80(
            Quaternion(0.4545195f, -0.5416752f, 0.4545195f, 0.5416752f),
            "Dir: E (90.0°) | Facing: UP (80.0°)",
        ),
        WEST_UP_80(
            Quaternion(-0.4545195f, -0.5416752f, 0.4545195f, -0.5416752f),
            "Dir: W (270.0°) | Facing: UP (80.0°)",
        ),
    }

    @Test
    fun testGetOrientationDescription(@TestParameter testCase: OrientationTestCase) {
        val description = getOrientationDescription(testCase.quaternion)
        assertThat(description).isEqualTo(testCase.expectedDescription)
    }
}
