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
    fun assertQuaternion_identicalQuaternion_passes() {
        val rotation = Quaternion.fromAxisAngle(Vector3.Up, 45f)

        assertQuaternion(rotation, rotation)
    }

    @Test
    fun assertQuaternion_antipodalQuaternions_passes() {
        val rotations =
            listOf(
                Quaternion.fromAxisAngle(Vector3.Up, 90f),
                Quaternion.fromAxisAngle(Vector3.Right, 180f),
                Quaternion.fromAxisAngle(Vector3(1f, 2f, -3f).toNormalized(), 67f),
            )

        for (rotation in rotations) {
            val antipodal = Quaternion(-rotation.x, -rotation.y, -rotation.z, -rotation.w)
            assertQuaternion(antipodal, rotation)
        }
    }

    @Test
    fun assertQuaternion_withinEpsilon_passes() {
        val expected = Quaternion.fromAxisAngle(Vector3.Up, 45f)
        val actual = Quaternion.fromAxisAngle(Vector3.Up, 45.05f)

        assertQuaternion(actual, expected, epsilon = 1e-3f)
    }

    @Test
    fun assertQuaternion_outsideEpsilon_throwsAssertionError() {
        val expected = Quaternion.fromAxisAngle(Vector3.Up, 30f)
        val actual = Quaternion.fromAxisAngle(Vector3.Up, 45f)

        assertThrows(AssertionError::class.java) {
            assertQuaternion(actual, expected, epsilon = 1e-3f)
        }
    }

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

        assertThrows(AssertionError::class.java) {
            assertPose(actual, expected, translationEpsilon = 1e-3f)
        }
    }

    @Test
    fun assertPose_looseTranslationEpsilon_keepsTightRotationEpsilon() {
        val expectedRotation = Quaternion.fromAxisAngle(Vector3.Up, 30f)
        val expected = Pose(Vector3(0f, 0f, -1.5f), expectedRotation)
        val actualTranslationDrifted = Pose(Vector3(0.02f, 0f, -1.5f), expectedRotation)
        val actualRotationDrifted =
            Pose(Vector3(0.02f, 0f, -1.5f), Quaternion.fromAxisAngle(Vector3.Up, 31f))

        // Translation drift of 0.02m passes with translationEpsilon = 0.05f while rotation is exact
        assertPose(actualTranslationDrifted, expected, translationEpsilon = 0.05f)

        // Loosening translationEpsilon to 0.05f must not mask a 1-degree rotation drift (~0.0087)
        assertThrows(AssertionError::class.java) {
            assertPose(actualRotationDrifted, expected, translationEpsilon = 0.05f)
        }
    }

    @Test
    fun assertPose_looseRotationEpsilon_keepsTightTranslationEpsilon() {
        val expectedTranslation = Vector3(0f, 0f, -1.5f)
        val expected = Pose(expectedTranslation, Quaternion.fromAxisAngle(Vector3.Up, 30f))
        val actualRotationDrifted =
            Pose(expectedTranslation, Quaternion.fromAxisAngle(Vector3.Up, 31f))
        val actualTranslationDrifted =
            Pose(Vector3(0.001f, 0f, -1.5f), Quaternion.fromAxisAngle(Vector3.Up, 31f))

        // 1-degree rotation drift (~0.0087) passes with rotationEpsilon = 0.02f while translation
        // is exact
        assertPose(actualRotationDrifted, expected, rotationEpsilon = 0.02f)

        // Loosening rotationEpsilon to 0.02f must not mask a 1mm translation drift
        assertThrows(AssertionError::class.java) {
            assertPose(actualTranslationDrifted, expected, rotationEpsilon = 0.02f)
        }
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
