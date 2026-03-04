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

package androidx.xr.arcore.projected

import android.app.Activity
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedRuntimeTest {

    private lateinit var underTest: ProjectedRuntime

    private lateinit var activity: Activity
    private lateinit var perceptionManager: ProjectedPerceptionManager
    private lateinit var timeSource: ProjectedTimeSource

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).get()
        timeSource = ProjectedTimeSource()
        perceptionManager = ProjectedPerceptionManager(timeSource)

        underTest =
            ProjectedRuntime(
                ProjectedManager(activity, perceptionManager, timeSource, Dispatchers.IO),
                perceptionManager,
            )
    }

    @Test
    fun isSupported_supportedModes_returnsTrue() {
        for (mode in ProjectedRuntime.SUPPORTED_CONFIG_MODES) {
            assertThat(underTest.isSupported(mode)).isTrue()
        }
    }

    @Test
    fun isSupported_unsupportedModes_returnsFalse() {
        val unsupportedModes =
            listOf(
                PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                HandTrackingMode.BOTH,
                DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW,
                AnchorPersistenceMode.LOCAL,
                FaceTrackingMode.BLEND_SHAPES,
                EyeTrackingMode.COARSE_TRACKING,
                EyeTrackingMode.FINE_TRACKING,
            )

        for (mode in unsupportedModes) {
            assertThat(underTest.isSupported(mode)).isFalse()
        }
    }
}
