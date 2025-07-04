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

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.util.UUID
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
class OpenXrAnchorTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var openXrManager: OpenXrManager
    private lateinit var xrResources: XrResources
    private lateinit var underTest: OpenXrAnchor

    @Before
    fun setUp() {
        xrResources = XrResources()
        underTest = OpenXrAnchor(nativePointer = 1, xrResources = xrResources)
        xrResources.addUpdatable(underTest as Updatable)
    }

    @Test
    fun update_updatesPose() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.pose == Pose())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.pose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Test
    fun update_updatesTrackingState() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.trackingState == TrackingState.PAUSED)

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from the tracking state corresponding to `kLocationFlags` defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun persist_updatesUuidAndPersistenceState() = initOpenXrManagerAndRunTest {
        check(underTest.persistenceState == Anchor.PersistenceState.NOT_PERSISTED)
        check(underTest.uuid == null)

        underTest.persist()

        assertThat(underTest.persistenceState).isEqualTo(Anchor.PersistenceState.PENDING)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kUuid` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.uuid)
            .isEqualTo(UUID.fromString("01020304-0506-0708-090a-0b0c0d0e0f10"))
    }

    @Test
    fun persist_calledTwice_doesNotChangeUuidAndState() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        underTest.persist()
        underTest.update(xrTime)
        check(underTest.uuid != null)
        check(underTest.persistenceState == Anchor.PersistenceState.PERSISTED)

        underTest.persist()

        assertThat(underTest.persistenceState).isEqualTo(Anchor.PersistenceState.PERSISTED)
    }

    @Test
    fun update_updatesPersistenceState() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        underTest.persist()
        check(underTest.persistenceState == Anchor.PersistenceState.PENDING)

        underTest.update(xrTime)

        assertThat(underTest.persistenceState).isEqualTo(Anchor.PersistenceState.PERSISTED)
    }

    @Test
    fun detach_removesAnchorFromXrResources() = initOpenXrManagerAndRunTest {
        check(xrResources.updatables.contains(underTest))

        underTest.detach()

        assertThat(xrResources.updatables).doesNotContain(underTest)
    }

    @Test
    fun fromOpenXrPersistenceState_returnsCorrectPersistenceStateValues() {
        // XR_ANCHOR_PERSIST_STATE_PERSIST_NOT_REQUESTED_ANDROID
        assertThat(Anchor.PersistenceState.fromOpenXrPersistenceState(0))
            .isEqualTo(Anchor.PersistenceState.NOT_PERSISTED)
        // XR_ANCHOR_PERSIST_STATE_PERSIST_PENDING_ANDROID
        assertThat(Anchor.PersistenceState.fromOpenXrPersistenceState(1))
            .isEqualTo(Anchor.PersistenceState.PENDING)
        // XR_ANCHOR_PERSIST_STATE_PERSISTED_ANDROID
        assertThat(Anchor.PersistenceState.fromOpenXrPersistenceState(2))
            .isEqualTo(Anchor.PersistenceState.PERSISTED)
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            val perceptionManager = OpenXrPerceptionManager(timeSource)
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
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
