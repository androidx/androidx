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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.kruth.assertThat
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType
import androidx.xr.scenecore.runtime.ScenePose
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakePerceptionSpaceScenePoseTest {
    lateinit var underTest: FakePerceptionSpaceScenePose

    @Before
    fun setUp() {
        underTest = FakePerceptionSpaceScenePose()

        // Default values
        assertThat(underTest.activitySpacePose).isEqualTo(Pose())
        assertThat(underTest.activitySpaceScale).isEqualTo(Vector3.One)
    }

    @Test
    fun hitTest_returnsEmptyHitTestResult() {
        val expectedValue =
            HitTestResult(null, null, HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN, 0f)

        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val filter = ScenePose.HitTestFilter.SELF_SCENE

        runBlocking {
            Truth.assertThat(underTest.hitTest(origin, direction, filter)).isEqualTo(expectedValue)
        }
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
        val filter = ScenePose.HitTestFilter.SELF_SCENE

        runBlocking {
            Truth.assertThat(underTest.hitTest(origin, direction, filter)).isEqualTo(expectedValue)
        }
    }
}
