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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.internal.HandJointType
import androidx.xr.runtime.internal.Trackable as RuntimeTrackable
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntime
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeRuntimeHand
import androidx.xr.runtime.testing.FakeRuntimePlane
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerceptionStateExtenderTest {

    private lateinit var fakeRuntime: FakeRuntime
    private lateinit var timeSource: TestTimeSource
    private lateinit var underTest: PerceptionStateExtender

    @Before
    fun setUp() {
        fakeRuntime = FakeRuntimeFactory().createRuntime(Activity())
        timeSource = fakeRuntime.lifecycleManager.timeSource
        underTest = PerceptionStateExtender()
    }

    @After
    fun tearDown() {
        underTest.close()
    }

    @Test
    fun extend_notInitialized_throwsIllegalStateException(): Unit = runBlocking {
        val coreState = CoreState(timeSource.markNow())

        assertFailsWith<IllegalStateException> { underTest.extend(coreState) }
    }

    @Test
    fun extend_withOneState_addsAllTrackablesToTheCollection(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)

        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        val runtimeTrackable: RuntimeTrackable = FakeRuntimePlane()
        fakeRuntime.perceptionManager.addTrackable(runtimeTrackable)

        // act
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.trackables).hasSize(1)
        assertThat(convertTrackable(perceptionState.trackables.last())).isEqualTo(runtimeTrackable)
    }

    @Test
    fun extend_withTwoStates_updatesAllTrackablesInTheirCollection(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        fakeRuntime.perceptionManager.addTrackable(FakeRuntimePlane())
        underTest.extend(CoreState(timeSource.markNow()))

        timeSource += 10.milliseconds
        val timeMark = timeSource.markNow()
        fakeRuntime.perceptionManager.trackables.clear()
        val runtimeTrackable = FakeRuntimePlane()
        fakeRuntime.perceptionManager.addTrackable(runtimeTrackable)
        val coreState = CoreState(timeMark)

        // act
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.trackables).hasSize(1)
        assertThat(convertTrackable(perceptionState.trackables.last())).isEqualTo(runtimeTrackable)
    }

    @Test
    fun extend_withTwoStates_trackableStatusUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val runtimeTrackable = FakeRuntimePlane()
        fakeRuntime.perceptionManager.addTrackable(runtimeTrackable)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(
            coreState.perceptionState!!.trackables.last().state.value.trackingState ==
                TrackingState.Tracking
        )

        // act
        timeSource += 10.milliseconds
        runtimeTrackable.trackingState = TrackingState.Stopped
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.trackables.last().state.value.trackingState)
            .isEqualTo(TrackingState.Stopped)
    }

    @Test
    fun extend_withTwoStates_handStatesUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.leftHand != null)
        check(coreState.perceptionState!!.rightHand != null)
        check(coreState.perceptionState!!.leftHand!!.state.value.isActive == false)
        check(coreState.perceptionState!!.leftHand!!.state.value.handJoints.isEmpty())
        check(coreState.perceptionState!!.rightHand!!.state.value.isActive == false)
        check(coreState.perceptionState!!.rightHand!!.state.value.handJoints.isEmpty())

        // act
        timeSource += 10.milliseconds
        val leftRuntimeHand = fakeRuntime.perceptionManager.leftHand!! as FakeRuntimeHand
        val rightRuntimeHand = fakeRuntime.perceptionManager.rightHand!! as FakeRuntimeHand
        val leftHandPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        val rightHandPose = Pose(Vector3(3.0f, 2.0f, 1.0f), Quaternion(4.0f, 3.0f, 2.0f, 1.0f))
        leftRuntimeHand.isActive = true
        leftRuntimeHand.handJoints = mapOf(HandJointType.PALM to leftHandPose)
        rightRuntimeHand.isActive = true
        rightRuntimeHand.handJoints = mapOf(HandJointType.PALM to rightHandPose)
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.leftHand!!.state.value.isActive).isEqualTo(true)
        assertThat(coreState2.perceptionState!!.leftHand!!.state.value.handJoints)
            .containsEntry(HandJointType.PALM, leftHandPose)
        assertThat(coreState2.perceptionState!!.rightHand!!.state.value.isActive).isEqualTo(true)
        assertThat(coreState2.perceptionState!!.rightHand!!.state.value.handJoints)
            .containsEntry(HandJointType.PALM, rightHandPose)
    }

    @Test
    fun extend_perceptionStateMapSizeExceedsMax(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)

        // act
        underTest.extend(coreState)
        // make sure the perception state is added to the map at the beginning
        check(coreState.perceptionState != null)

        for (i in 1..PerceptionStateExtender.MAX_PERCEPTION_STATE_EXTENSION_SIZE) {
            timeSource += 10.milliseconds
            underTest.extend(CoreState(timeSource.markNow()))
        }

        // assert
        assertThat(coreState.perceptionState).isNull()
    }

    @Test
    fun close_cleanUpData(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        underTest.extend(coreState)
        // make sure the perception state is added to the map at the beginning
        check(coreState.perceptionState != null)

        // act
        underTest.close()

        // assert
        assertThat(coreState.perceptionState).isNull()
    }

    private fun convertTrackable(trackable: Trackable<Trackable.State>): RuntimeTrackable {
        return when (trackable) {
            is Plane -> trackable.runtimePlane
            else ->
                throw IllegalArgumentException("Unsupported trackable type: ${trackable.javaClass}")
        }
    }
}
