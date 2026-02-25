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
import androidx.kruth.assertThat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.GeospatialMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.arcore.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrRuntimeTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.arcore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var underTest: OpenXrRuntime

    private lateinit var openXrManager: OpenXrManager

    @Test
    fun getPreferredBlendMode_returnsBlendMode() = initOpenXrRuntimeAndRunTest {
        // Result comes from `kBlendModes` defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        assertThat(underTest.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.ADDITIVE)
    }

    @Test
    fun isSupported_geospatialVpsAndGps_returnsTrue() = initOpenXrRuntimeAndRunTest {
        // Result comes from //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        assertThat(underTest.isSupported(GeospatialMode.VPS_AND_GPS)).isTrue()
    }

    private fun initOpenXrRuntimeAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            val perceptionManager = OpenXrPerceptionManager(timeSource)
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
            underTest = OpenXrRuntime(openXrManager, perceptionManager)
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
