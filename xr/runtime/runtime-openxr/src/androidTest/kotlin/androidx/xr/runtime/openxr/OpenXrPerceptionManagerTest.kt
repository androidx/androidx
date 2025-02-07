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

package androidx.xr.runtime.openxr

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.HandJointType
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrPerceptionManagerTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }

        const val XR_TIME = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
    }

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    lateinit var openXrManager: OpenXrManager
    lateinit var underTest: OpenXrPerceptionManager

    @Before
    fun setUp() {
        underTest = OpenXrPerceptionManager(OpenXrTimeSource())
    }

    @After
    fun tearDown() {
        underTest.clear()
    }

    @Test
    fun createAnchor_returnsAnchorWithTheGivenPose() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        val pose = Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f))
        val anchor = underTest.createAnchor(pose)

        assertThat(anchor.pose).isEqualTo(pose)
    }

    @Test
    fun createAnchor_anchorLimitReached_throwsException() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)

        // Number of calls comes from 'kAnchorResourcesLimit' defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        repeat(5) { underTest.createAnchor(Pose()) }

        assertThrows(AnchorResourcesExhaustedException::class.java) {
            underTest.createAnchor(Pose())
        }
    }

    @Test
    fun detachAnchor_removesAnchorWhenItDetaches() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)

        val anchor = underTest.createAnchor(Pose())
        check(underTest.xrResources.updatables.contains(anchor as Updatable))

        anchor.detach()

        assertThat(underTest.xrResources.updatables).doesNotContain(anchor as Updatable)
    }

    @Test
    fun update_updatesTrackables() = initOpenXrManagerAndRunTest {
        // TODO: b/345314278 -- Add more meaningful tests once trackables are implemented properly
        // and a
        // fake perception library can be used mock trackables.
        underTest.update(XR_TIME)

        assertThat(underTest.trackables).hasSize(1)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat((underTest.trackables.first() as OpenXrPlane).centerPose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Test
    fun update_updatesHands() = initOpenXrManagerAndRunTest {
        check(underTest.xrResources.updatables.size == 2)
        check(!underTest.leftHand.isActive)
        check(!underTest.rightHand.isActive)

        underTest.update(XR_TIME)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.leftHand.isActive).isTrue()
        assertThat(underTest.leftHand.handJoints).hasSize(1)
        assertThat(underTest.leftHand.handJoints[HandJointType.PALM]!!.rotation)
            .isEqualTo(Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        assertThat(underTest.leftHand.handJoints[HandJointType.PALM]!!.translation)
            .isEqualTo(Vector3(5.0f, 6.0f, 7.0f))
        assertThat(underTest.rightHand.isActive).isTrue()
        assertThat(underTest.rightHand.handJoints).hasSize(1)
        assertThat(underTest.rightHand.handJoints[HandJointType.PALM]!!.rotation)
            .isEqualTo(Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        assertThat(underTest.rightHand.handJoints[HandJointType.PALM]!!.translation)
            .isEqualTo(Vector3(5.0f, 6.0f, 7.0f))
    }

    @Test
    fun hitTest_returnsHitResults() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)
        check(underTest.trackables.isNotEmpty())
        val trackable = underTest.trackables.first() as OpenXrPlane

        // TODO: b/345314278 -- Add more meaningful tests once trackables are implemented properly
        // and a
        // fake perception library can be used to mock trackables.
        val hitResults = underTest.hitTest(Ray(Vector3(4f, 3f, 2f), Vector3(2f, 1f, 0f)))

        assertThat(hitResults).hasSize(1)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(hitResults.first().hitPose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
        assertThat(hitResults.first().trackable).isEqualTo(trackable)
        assertThat(hitResults.first().distance).isEqualTo(5f) // sqrt((4-0)^2 + (3-0)^2 + (2-2)^2)
    }

    @Test
    fun getPersistedAnchorUuids_returnsStubUuid() = initOpenXrManagerAndRunTest {
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kUuid` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.getPersistedAnchorUuids())
            .containsExactly(UUID.fromString("01020304-0506-0708-090a-0b0c0d0e0f10"))
    }

    @Test
    fun loadAnchor_returnsAnchorWithGivenUuidAndPose() = initOpenXrManagerAndRunTest {
        // The stub doesn't care about the UUID, so we can use any UUID.
        val uuid = UUID.randomUUID()
        val anchor = underTest.loadAnchor(uuid)

        assertThat(anchor.uuid).isEqualTo(uuid)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(anchor.pose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Test
    fun loadAnchor_anchorLimitReached_throwsException() = initOpenXrManagerAndRunTest {
        // Number of calls comes from 'kAnchorResourcesLimit' defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        // The UUID is randomized because the manager will not create duplicate anchors for the same
        // UUID.
        repeat(5) { underTest.loadAnchor(UUID.randomUUID()) }

        assertThrows(AnchorResourcesExhaustedException::class.java) {
            underTest.loadAnchor(UUID.randomUUID())
        }
    }

    @Test
    fun loadAnchorFromNativePointer_returnsAnchorWithGivenNativePointer() =
        initOpenXrManagerAndRunTest {
            val anchor = underTest.loadAnchorFromNativePointer(123L) as OpenXrAnchor
            assertThat(anchor.nativePointer).isEqualTo(123L)

            // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time
            // being they
            // come from `kPose` defined in
            // //third_party/arcore/androidx/native/openxr/openxr_stub.cc
            assertThat(anchor.pose)
                .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
        }

    @Test
    fun unpersistAnchor_doesNotThrowIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.unpersistAnchor(UUID.randomUUID())
    }

    @Test
    fun clear_clearXrResources() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)
        underTest.createAnchor(Pose())
        check(underTest.trackables.isNotEmpty())
        check(underTest.xrResources.trackablesMap.isNotEmpty())
        check(underTest.xrResources.updatables.isNotEmpty())

        underTest.clear()

        assertThat(underTest.trackables).isEmpty()
        assertThat(underTest.xrResources.trackablesMap).isEmpty()
        assertThat(underTest.xrResources.updatables).isEmpty()
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            openXrManager = OpenXrManager(it, underTest, timeSource)
            openXrManager.create()
            openXrManager.resume()

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
