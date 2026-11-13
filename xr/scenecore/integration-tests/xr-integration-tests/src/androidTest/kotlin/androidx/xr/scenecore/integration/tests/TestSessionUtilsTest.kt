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

package androidx.xr.scenecore.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TestSessionUtilsTest {

    @get:Rule val timeout: Timeout = Timeout.seconds(10)

    @Test
    fun assertPose_identicalPose_passes() {
        val translation = Vector3(0.25f, 1.5f, -2.0f)
        val rotation = Quaternion.fromAxisAngle(Vector3.Up, 45f)

        assertPose(Pose(translation, rotation), Pose(translation, rotation))
    }

    @Test
    fun assertPose_antipodalQuaternions_passes() {
        val translation = Vector3(0.25f, 1.5f, -2.0f)
        val rotations =
            listOf(
                Quaternion.fromAxisAngle(Vector3.Up, 90f),
                Quaternion.fromAxisAngle(Vector3.Right, 180f),
                Quaternion.fromAxisAngle(Vector3(1f, 2f, -3f).toNormalized(), 67f),
            )

        for (rotation in rotations) {
            val antipodal = Quaternion(-rotation.x, -rotation.y, -rotation.z, -rotation.w)
            assertPose(Pose(translation, antipodal), Pose(translation, rotation))
        }
    }

    @Test
    fun assertPose_differentRotation_throwsAssertionError() {
        val translation = Vector3(0f, 0f, -1.5f)
        val expected = Pose(translation, Quaternion.fromAxisAngle(Vector3.Up, 30f))
        val actual = Pose(translation, Quaternion.fromAxisAngle(Vector3.Up, 45f))

        assertThrows(AssertionError::class.java) { assertPose(actual, expected) }
    }

    @Test
    fun assertPose_translationMismatchOutsideEpsilon_throwsAssertionError() {
        val rotation = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val expected = Pose(Vector3(0f, 0f, -1.5f), rotation)
        val actual = Pose(Vector3(0.01f, 0f, -1.5f), rotation)

        assertThrows(AssertionError::class.java) { assertPose(actual, expected, epsilon = 1e-3f) }
    }

    @Test
    fun assertPose_scaledPositiveAndAntipodalQuaternions_passes() {
        val translation = Vector3(0.1f, 0.2f, -0.3f)
        assertPose(
            Pose(translation, Quaternion(0f, 0f, 0f, 2f)),
            Pose(translation, Quaternion.Identity),
        )
        assertPose(
            Pose(translation, Quaternion(0f, 0f, 0f, -2f)),
            Pose(translation, Quaternion.Identity),
        )
    }

    @Test
    fun assertPose_nonFiniteComponents_throwsAssertionError() {
        val validPose = Pose(Vector3.Zero, Quaternion.Identity)
        val nanRotationPose = Pose(Vector3.Zero, Quaternion(Float.NaN, 0f, 0f, 1f))
        val infRotationPose = Pose(Vector3.Zero, Quaternion(Float.POSITIVE_INFINITY, 0f, 0f, 1f))
        val nanTranslationPose = Pose(Vector3(Float.NaN, 0f, 0f), Quaternion.Identity)

        assertThrows(AssertionError::class.java) { assertPose(nanRotationPose, validPose) }
        assertThrows(AssertionError::class.java) { assertPose(infRotationPose, validPose) }
        assertThrows(AssertionError::class.java) { assertPose(nanTranslationPose, validPose) }
    }
}
