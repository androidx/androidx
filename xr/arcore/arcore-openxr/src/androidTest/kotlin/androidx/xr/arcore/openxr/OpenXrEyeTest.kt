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

package androidx.xr.arcore.openxr

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Config
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class OpenXrEyeTest {
    companion object {
        init {
            System.loadLibrary("androidx.xr.arcore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var openXrManager: OpenXrManager
    private lateinit var perceptionManager: OpenXrPerceptionManager

    @Test
    fun update_updatesCoarseTrackingState() =
        initOpenXrManagerAndRunTest(EyeTrackingMode.COARSE_TRACKING) {
            val underTestLeft: OpenXrEye = perceptionManager.leftEye as OpenXrEye
            val underTestRight: OpenXrEye = perceptionManager.rightEye as OpenXrEye
            val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.

            perceptionManager.updateEyes(xrTime)

            assertThat(underTestLeft.isOpen).isTrue()
            assertThat(underTestRight.isOpen).isTrue()
        }

    @Test
    fun update_updatesCoarsePose() =
        initOpenXrManagerAndRunTest(EyeTrackingMode.COARSE_TRACKING) {
            val underTestLeft: OpenXrEye = perceptionManager.leftEye as OpenXrEye
            val underTestRight: OpenXrEye = perceptionManager.rightEye as OpenXrEye
            val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.

            perceptionManager.updateEyes(xrTime)

            assertThat(underTestLeft.pose)
                .isEqualTo(Pose(Vector3(0.2f, 0.2f, 0.2f), Quaternion(0.1f, 0.1f, 0.1f, 0.1f)))
            assertThat(underTestRight.pose)
                .isEqualTo(Pose(Vector3(0.4f, 0.4f, 0.4f), Quaternion(0.3f, 0.3f, 0.3f, 0.3f)))
        }

    @Test
    fun update_updatesFineTrackingState() =
        initOpenXrManagerAndRunTest(EyeTrackingMode.FINE_TRACKING) {
            val underTestLeft: OpenXrEye = perceptionManager.leftEye as OpenXrEye
            val underTestRight: OpenXrEye = perceptionManager.rightEye as OpenXrEye
            val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.

            perceptionManager.updateEyes(xrTime)

            assertThat(underTestLeft.isOpen).isTrue()
            assertThat(underTestRight.isOpen).isTrue()
        }

    @Test
    fun update_updatesFinePose() =
        initOpenXrManagerAndRunTest(EyeTrackingMode.FINE_TRACKING) {
            val underTestLeft: OpenXrEye = perceptionManager.leftEye as OpenXrEye
            val underTestRight: OpenXrEye = perceptionManager.rightEye as OpenXrEye
            val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.

            perceptionManager.updateEyes(xrTime)

            assertThat(underTestLeft.pose)
                .isEqualTo(
                    Pose(
                        Vector3(0.22222f, 0.22222f, 0.22222f),
                        Quaternion(0.11111f, 0.11111f, 0.11111f, 0.11111f),
                    )
                )
            assertThat(underTestRight.pose)
                .isEqualTo(
                    Pose(
                        Vector3(0.44444f, 0.44444f, 0.44444f),
                        Quaternion(0.33333f, 0.33333f, 0.33333f, 0.33333f),
                    )
                )
        }

    private fun initOpenXrManagerAndRunTest(trackingMode: EyeTrackingMode, testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            perceptionManager = OpenXrPerceptionManager(timeSource)
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
            openXrManager.create()
            openXrManager.resume()
            openXrManager.configure(Config(eyeTracking = trackingMode))

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
