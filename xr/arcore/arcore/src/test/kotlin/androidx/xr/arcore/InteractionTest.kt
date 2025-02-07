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

package androidx.xr.arcore

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.internal.HitResult as RuntimeHitResult
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntime
import androidx.xr.runtime.testing.FakeRuntimePlane
import com.google.common.truth.Truth.assertThat
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InteractionTest {

    private lateinit var session: Session
    private lateinit var activity: Activity
    private lateinit var timeSource: TestTimeSource
    private lateinit var perceptionStateExtender: PerceptionStateExtender
    private lateinit var perceptionManager: FakePerceptionManager

    @get:Rule
    val grantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.SCENE_UNDERSTANDING",
            "android.permission.HAND_TRACKING",
        )

    @Test
    fun hitTest_successWithOneHitResult() = createTestSessionAndRunTest {
        runTest {
            timeSource = (session.runtime as FakeRuntime).lifecycleManager.timeSource
            perceptionStateExtender =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            perceptionManager = perceptionStateExtender.perceptionManager as FakePerceptionManager
            val runtimePlane = FakeRuntimePlane()
            perceptionManager.addTrackable(runtimePlane)
            // Mock the behavior of session update.
            val timeMark = timeSource.markNow()
            val state = CoreState(timeMark)
            perceptionStateExtender.extend(state)
            check(state.perceptionState?.trackables?.size == 1)
            val expectedTrackable = state.perceptionState?.trackables?.first()
            val runtimeHitResult: RuntimeHitResult =
                RuntimeHitResult(
                    distance = 1f,
                    hitPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f)),
                    trackable = runtimePlane,
                )
            perceptionManager.addHitResult(runtimeHitResult)

            val hitResults = hitTest(session, Ray(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 1f)))

            assertThat(hitResults.size).isEqualTo(1)
            assertThat(hitResults[0].distance).isEqualTo(runtimeHitResult.distance)
            assertThat(hitResults[0].hitPose).isEqualTo(runtimeHitResult.hitPose)
            assertThat(hitResults[0].trackable).isEqualTo(expectedTrackable)
        }
    }

    private fun createTestSessionAndRunTest(testBody: () -> Unit) {
        ActivityScenario.launch(Activity::class.java).use {
            it.onActivity { activity ->
                session =
                    (Session.create(activity, StandardTestDispatcher()) as SessionCreateSuccess)
                        .session

                testBody()
            }
        }
    }
}
