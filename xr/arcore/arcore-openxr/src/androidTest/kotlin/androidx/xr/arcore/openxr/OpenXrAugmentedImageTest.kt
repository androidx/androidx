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

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.AugmentedImageDatabase
import androidx.xr.runtime.AugmentedImageDatabaseEntryMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class OpenXrAugmentedImageTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.arcore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private val augmentedImageId = 1L

    private lateinit var openXrRuntime: OpenXrRuntime
    private lateinit var xrResources: XrResources
    private lateinit var timeSource: OpenXrTimeSource
    private lateinit var underTest: OpenXrAugmentedImage

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        xrResources = XrResources(timeSource)
        underTest = OpenXrAugmentedImage(augmentedImageId)
        xrResources.addTrackable(augmentedImageId, underTest)
        xrResources.addUpdatable(underTest as Updatable)
    }

    @After
    fun tearDown() {
        xrResources.clear()
    }

    @Test
    fun update_updatesTrackingState() = initOpenXrRuntimeAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.trackingState.equals(TrackingState.PAUSED))

        underTest.update(xrTime)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_updatesCenterPose() = initOpenXrRuntimeAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.centerPose == Pose())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.centerPose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Test
    fun update_updatesExtents() = initOpenXrRuntimeAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.extents == FloatSize2d())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.extents).isEqualTo(FloatSize2d(1.0f, 2.0f))
    }

    private fun initOpenXrRuntimeAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            openXrRuntime = OpenXrRuntime(it, OpenXrPerceptionManager(timeSource), timeSource)
            openXrRuntime.initialize()
            openXrRuntime.resume()

            val augmentedImageDatabase = AugmentedImageDatabase()
            augmentedImageDatabase.addAugmentedImageDatabaseEntry(
                AugmentedImageDatabaseEntryMode.DYNAMIC,
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            )

            openXrRuntime.configure(Config(augmentedImageDatabase = augmentedImageDatabase))

            // Required to wait for the c++ polling event to finish
            runBlocking { openXrRuntime.update() }

            testBody()

            // Pause and stop the OpenXR runtime here in lieu of an @After method to ensure that the
            // calls to the OpenXR runtime are coming from the same thread.
            openXrRuntime.pause()
            openXrRuntime.destroy()
        }
    }
}
