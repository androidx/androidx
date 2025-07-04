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
import androidx.xr.runtime.Config
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrHandTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var openXrManager: OpenXrManager
    private lateinit var underTest: OpenXrHand

    @Before
    fun setUp() {
        underTest = OpenXrHand(isLeftHand = true)
    }

    @Ignore(
        "b/425697141 - Requires HEAD_TRACKING permission which is not available on Android test runners."
    )
    @Test
    fun update_updatesActiveStatus() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.trackingState != TrackingState.TRACKING)

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Ignore(
        "b/425697141 - Requires HEAD_TRACKING permission which is not available on Android test runners."
    )
    @Test
    fun update_updateHandJoints() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.handJoints.isEmpty())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        val fetchedHandJoints = underTest.handJoints
        assertThat(fetchedHandJoints.size).isEqualTo(HandJointType.entries.size)
        for (jointType in HandJointType.entries) {
            val jointTypeIndex = jointType.ordinal.toFloat()
            assertThat(underTest.handJoints[jointType]!!.rotation)
                .isEqualTo(
                    Quaternion(
                        jointTypeIndex + 0.1f,
                        jointTypeIndex + 0.2f,
                        jointTypeIndex + 0.3f,
                        jointTypeIndex + 0.4f,
                    )
                )
            assertThat(underTest.handJoints[jointType]!!.translation)
                .isEqualTo(
                    Vector3(jointTypeIndex + 0.5f, jointTypeIndex + 0.6f, jointTypeIndex + 0.7f)
                )
        }
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            val perceptionManager = OpenXrPerceptionManager(timeSource)
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
            openXrManager.create()
            openXrManager.resume()
            openXrManager.configure(Config(handTracking = Config.HandTrackingMode.BOTH))

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
