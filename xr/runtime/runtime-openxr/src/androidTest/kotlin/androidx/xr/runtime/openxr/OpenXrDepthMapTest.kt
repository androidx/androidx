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

package androidx.xr.runtime.openxr

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
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
class OpenXrDepthMapTest {

    companion object {
        private const val LEFT_VIEW_INDEX: Int = 0
        private const val RIGHT_VIEW_INDEX: Int = 1

        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    lateinit private var timeSource: OpenXrTimeSource
    lateinit private var perceptionManager: OpenXrPerceptionManager
    lateinit private var openXrManager: OpenXrManager
    lateinit private var leftUnderTest: OpenXrDepthMap
    lateinit private var rightUnderTest: OpenXrDepthMap

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        perceptionManager = OpenXrPerceptionManager(timeSource)
        leftUnderTest = perceptionManager.depthMaps.get(LEFT_VIEW_INDEX) as OpenXrDepthMap
        rightUnderTest = perceptionManager.depthMaps.get(RIGHT_VIEW_INDEX) as OpenXrDepthMap
    }

    @Test
    fun configure_updatesWidth() = initOpenXrManagerAndRunTest {
        check(leftUnderTest.width == 0)
        check(rightUnderTest.width == 0)

        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.RAW_ONLY))

        // The value below comes from XR_DEPTH_CAMERA_RESOLUTION_80x80_ANDROID (width = 80, height =
        // 80) defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        assertThat(leftUnderTest.width).isEqualTo(80)
        assertThat(rightUnderTest.width).isEqualTo(80)
    }

    @Test
    fun configure_updatesHeight() = initOpenXrManagerAndRunTest {
        assertThat(leftUnderTest.height).isEqualTo(0)
        assertThat(rightUnderTest.height).isEqualTo(0)

        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.RAW_ONLY))

        // The value below comes from XR_DEPTH_CAMERA_RESOLUTION_80x80_ANDROID (width = 80, height =
        // 80) defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        assertThat(leftUnderTest.height).isEqualTo(80)
        assertThat(rightUnderTest.height).isEqualTo(80)
    }

    @Test
    fun configureRawOnly_thenUpdate_doesNotUpdateSmoothBuffers() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.RAW_ONLY))

        perceptionManager.update(xrTime)

        assertThat(leftUnderTest.smoothDepthMap).isNull()
        assertThat(leftUnderTest.smoothConfidenceMap).isNull()
        assertThat(rightUnderTest.smoothDepthMap).isNull()
        assertThat(rightUnderTest.smoothConfidenceMap).isNull()
    }

    @Test
    fun configureRawOnly_thenUpdate_updatesRawBuffers() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.RAW_ONLY))

        perceptionManager.update(xrTime)

        assertThat(leftUnderTest.rawDepthMap).isNotNull()
        assertThat(leftUnderTest.rawConfidenceMap).isNotNull()
        assertThat(rightUnderTest.rawDepthMap).isNotNull()
        assertThat(rightUnderTest.rawConfidenceMap).isNotNull()
        // The expected values of the raw depth and confidence buffers come from kTestRawDepthData
        // and kTestRawDepthConfidenceData in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        val expectedRawDepthMap: FloatBuffer = FloatBuffer.wrap(FloatArray(6400) { 8.0f })
        val expectedRawConfidenceMap: ByteBuffer = ByteBuffer.wrap(ByteArray(6400) { 100 })
        assertThat(leftUnderTest.rawDepthMap).isEqualTo(expectedRawDepthMap)
        assertThat(leftUnderTest.rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
        assertThat(rightUnderTest.rawDepthMap).isEqualTo(expectedRawDepthMap)
        assertThat(rightUnderTest.rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
    }

    @Test
    fun configureSmoothOnly_thenUpdate_doesNotUpdateRawBuffers() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.SMOOTH_ONLY))

        perceptionManager.update(xrTime)

        assertThat(leftUnderTest.rawDepthMap).isNull()
        assertThat(leftUnderTest.rawConfidenceMap).isNull()
        assertThat(rightUnderTest.rawDepthMap).isNull()
        assertThat(rightUnderTest.rawConfidenceMap).isNull()
    }

    @Test
    fun configureSmoothOnly_thenUpdate_updatesSmoothBuffers() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.SMOOTH_ONLY))

        perceptionManager.update(xrTime)

        assertThat(leftUnderTest.smoothDepthMap).isNotNull()
        assertThat(leftUnderTest.smoothConfidenceMap).isNotNull()
        assertThat(rightUnderTest.smoothDepthMap).isNotNull()
        assertThat(rightUnderTest.smoothConfidenceMap).isNotNull()
        // The expected values of the smooth depth and confidence buffers come from
        // kTestSmoothDepthData and kTestSmoothDepthConfidenceData in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        val expectedSmoothDepthMap: FloatBuffer = FloatBuffer.wrap(FloatArray(6400) { 10.0f })
        val expectedSmoothConfidenceMap: ByteBuffer =
            ByteBuffer.wrap(ByteArray(6400) { 200.toByte() })
        assertThat(leftUnderTest.smoothDepthMap).isEqualTo(expectedSmoothDepthMap)
        assertThat(leftUnderTest.smoothConfidenceMap).isEqualTo(expectedSmoothConfidenceMap)
        assertThat(rightUnderTest.smoothDepthMap).isEqualTo(expectedSmoothDepthMap)
        assertThat(rightUnderTest.smoothConfidenceMap).isEqualTo(expectedSmoothConfidenceMap)
    }

    @Test
    fun configureSmoothAndRaw_throwsConfigurationNotSupportedException() =
        initOpenXrManagerAndRunTest {
            assertThrows(ConfigurationNotSupportedException::class.java) {
                openXrManager.configure(
                    Config(depthEstimation = Config.DepthEstimationMode.SMOOTH_AND_RAW)
                )
            }
        }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity { activity ->
            openXrManager = OpenXrManager(activity, perceptionManager, timeSource)
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
