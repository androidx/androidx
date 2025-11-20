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
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Config
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrFaceTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    lateinit private var openXrManager: OpenXrManager
    lateinit private var underTest: OpenXrFace

    @Before
    fun setUp() {
        underTest = OpenXrFace()
    }

    @Test
    fun update_updatesTrackingStateToTracking() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.trackingState != TrackingState.TRACKING)

        underTest.update(xrTime)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_updatesBlendShapeAndConfidenceValues() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.

        underTest.update(xrTime)

        val fetchedConfidences = underTest.confidenceValues
        assertThat(fetchedConfidences.size)
            .isEqualTo(OpenXrFace.XR_FACE_REGION_CONFIDENCE_COUNT_ANDROID)
        val fetchedBlendShapes = underTest.blendShapeValues
        assertThat(fetchedBlendShapes.size).isEqualTo(OpenXrFace.XR_FACE_PARAMETER_COUNT_ANDROID)

        // Expected values of the confidence and blendShape arrays are defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        // and are equal to (Array index) / (Array length)
        for ((i, confidence) in fetchedConfidences.withIndex()) {
            assertThat(confidence)
                .isEqualTo(i.toFloat() / OpenXrFace.XR_FACE_REGION_CONFIDENCE_COUNT_ANDROID)
        }
        for ((i, blendShapeValue) in fetchedBlendShapes.withIndex()) {
            assertThat(blendShapeValue)
                .isEqualTo(i.toFloat() / OpenXrFace.XR_FACE_PARAMETER_COUNT_ANDROID)
        }
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            val perceptionManager = OpenXrPerceptionManager(timeSource)
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
            openXrManager.create()
            openXrManager.resume()

            // Configure twice because the stubs return false calibration the first time
            try {
                openXrManager.configure(Config(faceTracking = Config.FaceTrackingMode.USER))
            } catch (e: FaceTrackingNotCalibratedException) {
                openXrManager.configure(Config(faceTracking = Config.FaceTrackingMode.USER))
            }

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
