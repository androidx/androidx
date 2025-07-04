/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.ActivityPose
import androidx.xr.runtime.internal.HitTestResult
import androidx.xr.runtime.internal.HitTestResult.HitTestSurfaceType
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion.Companion.fromEulerAngles
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** This test should cover all the methods in [ActivityPose]. */
@RunWith(JUnit4::class)
class FakeActivityPoseTest {
    private lateinit var underTest: FakeActivityPose
    private lateinit var otherActivitySpace: FakeActivityPose

    @Before
    fun setUp() {
        underTest = FakeActivityPose()
        otherActivitySpace = FakeActivityPose()
    }

    @Test
    fun activitySpacePose_initialValueIsIdentity() {
        assertThat(underTest.activitySpacePose).isEqualTo(Pose.Identity)
    }

    @Test
    fun worldSpaceScale_initialValueIsOne() {
        assertThat(underTest.worldSpaceScale).isEqualTo(Vector3.One)
    }

    @Test
    fun activitySpaceScale_initialValueIsOne() {
        assertThat(underTest.activitySpaceScale).isEqualTo(Vector3.One)
    }

    @Test
    fun transformPoseTo_withSameActivitySpace_returnsTransformedPose() {
        val pose = Pose(Vector3(10f, 0f, 0f), fromEulerAngles(Vector3(0f, 0f, 90f)))

        assertThat(underTest.transformPoseTo(pose, underTest)).isEqualTo(pose)
    }

    @Test
    fun transformPoseTo_withDifferentActivitySpace_returnsTransformedPose() {
        val pose = Pose(Vector3(10f, 0f, 0f), fromEulerAngles(Vector3(0f, 0f, 90f)))

        assertThat(underTest.transformPoseTo(pose, otherActivitySpace)).isEqualTo(pose)
    }

    @Test
    fun hitTest_returnsEmptyHitTestResult() {
        val expectedValue =
            HitTestResult(null, null, HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN, 0f)

        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val filter = ActivityPose.HitTestFilter.SELF_SCENE

        assertThat(underTest.hitTest(origin, direction, filter).get()).isEqualTo(expectedValue)
    }

    @Test
    fun hitTest_returnsFakeHitTestResult() {
        val expectedValue =
            HitTestResult(
                Vector3(2f, 3f, 4f),
                Vector3(4f, 5f, 6f),
                HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE,
                7f,
            )

        underTest.hitTestResult = expectedValue

        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val filter = ActivityPose.HitTestFilter.SELF_SCENE

        assertThat(underTest.hitTest(origin, direction, filter).get()).isEqualTo(expectedValue)
    }
}
