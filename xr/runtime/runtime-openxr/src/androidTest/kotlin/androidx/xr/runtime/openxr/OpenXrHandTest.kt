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
import androidx.xr.runtime.internal.HandJointType
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
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
class OpenXrHandTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    lateinit private var openXrManager: OpenXrManager
    lateinit private var underTest: OpenXrHand

    @Before
    fun setUp() {
        underTest = OpenXrHand(isLeftHand = true)
    }

    @Test
    fun update_updatesActiveStatus() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(!underTest.isActive)

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.isActive).isTrue()
    }

    @Test
    fun update_updateHandJoints() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.handJoints.isEmpty())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.handJoints).hasSize(1)
        assertThat(underTest.handJoints[HandJointType.PALM]!!.rotation)
            .isEqualTo(Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        assertThat(underTest.handJoints[HandJointType.PALM]!!.translation)
            .isEqualTo(Vector3(5.0f, 6.0f, 7.0f))
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
