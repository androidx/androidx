/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.math.Pose
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSpatialAnnotationsApi::class)
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class OpenXrSpatialAnnotationTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.arcore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private val spatialAnnotationId = 1L

    private lateinit var openXrRuntime: OpenXrRuntime
    private lateinit var xrResources: XrResources
    private lateinit var timeSource: OpenXrTimeSource
    private lateinit var underTest: OpenXrSpatialAnnotation

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        xrResources = XrResources(timeSource)
        underTest =
            OpenXrSpatialAnnotation(spatialAnnotationId, SpatialAnnotationId.fromString("test_id"))
        xrResources.addTrackable(spatialAnnotationId, underTest)
        xrResources.addUpdatable(underTest as Updatable)
    }

    @After
    fun tearDown() {
        xrResources.clear()
    }

    @Test
    fun update_updatesTrackingState() = initOpenXrRuntimeAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds
        check(underTest.trackingState == TrackingState.PAUSED)

        underTest.update(xrTime)

        // TODO(b/542271442): Update assertions when the fake OpenXR stub returns functional
        // tracking state data using the Kotlin test API.
        // assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_updatesCenterPose() = initOpenXrRuntimeAndRunTest {
        val xrTime = 50L * 1_000_000
        check(underTest.centerPose == Pose())

        underTest.update(xrTime)

        // TODO(b/542271442): Assert against stub API returns
    }

    @Test
    fun update_updatesQuad() = initOpenXrRuntimeAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.quad == null)

        underTest.update(xrTime)

        // TODO(b/542271442): Assert against stub API returns
    }

    @Test
    fun alignment_returnsConfiguredAlignment() {
        val annotation =
            OpenXrSpatialAnnotation(
                spatialAnnotationId,
                SpatialAnnotationId.fromString("test_id"),
                SpatialAnnotationQuadAlignment.SCREEN,
            )
        assertThat(annotation.alignment).isEqualTo(SpatialAnnotationQuadAlignment.SCREEN)
    }

    private fun initOpenXrRuntimeAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            openXrRuntime = OpenXrRuntime(it, OpenXrPerceptionManager(timeSource), timeSource)
            openXrRuntime.initialize()
            openXrRuntime.resume()

            testBody()

            // Pause and stop the OpenXR runtime here in lieu of an @After method to ensure that the
            // calls to the OpenXR runtime are coming from the same thread.
            openXrRuntime.pause()
            openXrRuntime.destroy()
        }
    }
}
