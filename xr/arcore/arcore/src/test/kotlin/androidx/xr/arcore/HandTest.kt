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
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntimeAnchor
import androidx.xr.runtime.testing.FakeRuntimeHand
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HandTest {

    private lateinit var xrResourcesManager: XrResourcesManager
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @get:Rule
    val grantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.SCENE_UNDERSTANDING",
            "android.permission.HAND_TRACKING",
        )

    @Before
    fun setUp() {
        xrResourcesManager = XrResourcesManager()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        FakeRuntimeAnchor.anchorsCreated = 0
    }

    @After
    fun tearDown() {
        xrResourcesManager.clear()
    }

    @Test
    fun left_returnsLeftHand() =
        createTestSessionAndRunTest(testDispatcher) {
            runTest(testDispatcher) {
                val perceptionManager = session.runtime.perceptionManager as FakePerceptionManager
                check(Hand.left(session) != null)
                check(Hand.left(session)!!.state.value.isActive == false)
                check(Hand.left(session)!!.state.value.handJoints.isEmpty())

                val leftRuntimeHand = perceptionManager.leftHand!! as FakeRuntimeHand
                val leftHandPose =
                    Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
                leftRuntimeHand.isActive = true
                leftRuntimeHand.handJoints = mapOf(HandJointType.PALM to leftHandPose)
                awaitNewCoreState(session, testScope)

                assertThat(Hand.left(session)!!.state.value.isActive).isEqualTo(true)
                assertThat(Hand.left(session)!!.state.value.handJoints)
                    .containsEntry(HandJointType.PALM, leftHandPose)
            }
        }

    @Test
    fun right_returnsRightHand() =
        createTestSessionAndRunTest(testDispatcher) {
            runTest(testDispatcher) {
                val perceptionManager = session.runtime.perceptionManager as FakePerceptionManager
                check(Hand.right(session) != null)
                check(Hand.right(session)!!.state.value.isActive == false)
                check(Hand.right(session)!!.state.value.handJoints.isEmpty())

                val rightRuntimeHand = perceptionManager.rightHand!! as FakeRuntimeHand
                val rightHandPose =
                    Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
                rightRuntimeHand.isActive = true
                rightRuntimeHand.handJoints = mapOf(HandJointType.PALM to rightHandPose)
                awaitNewCoreState(session, testScope)

                assertThat(Hand.right(session)!!.state.value.isActive).isEqualTo(true)
                assertThat(Hand.right(session)!!.state.value.handJoints)
                    .containsEntry(HandJointType.PALM, rightHandPose)
            }
        }

    @Test
    fun update_stateMachesRuntimeHand() = runBlocking {
        val runtimeHand = FakeRuntimeHand()
        val underTest = Hand(runtimeHand)
        check(underTest.state.value.isActive.equals(false))
        check(underTest.state.value.handJoints.isEmpty())

        runtimeHand.isActive = true
        val pose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        runtimeHand.handJoints = mapOf(HandJointType.PALM to pose)
        underTest.update()

        assertThat(underTest.state.value.isActive).isEqualTo(true)
        assertThat(underTest.state.value.handJoints).containsEntry(HandJointType.PALM, pose)
    }

    private fun createTestSessionAndRunTest(
        coroutineDispatcher: CoroutineDispatcher = StandardTestDispatcher(),
        testBody: () -> Unit,
    ) {
        ActivityScenario.launch(Activity::class.java).use {
            it.onActivity { activity ->
                session =
                    (Session.create(activity, coroutineDispatcher) as SessionCreateSuccess).session

                testBody()
            }
        }
    }

    /** Resumes and pauses the session just enough to emit a new CoreState. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitNewCoreState(session: Session, testScope: TestScope) {
        session.resume()
        testScope.advanceUntilIdle()
        session.pause()
    }
}
