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
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class OpenXrAugmentedObjectTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private val objectId = 1L

    private lateinit var openXrManager: OpenXrManager
    private lateinit var xrResources: XrResources
    private lateinit var underTest: OpenXrAugmentedObject

    @Before
    fun setUp() {
        xrResources = XrResources()
        underTest = OpenXrAugmentedObject(objectId, OpenXrTimeSource(), xrResources)
        xrResources.addTrackable(objectId, underTest)
        xrResources.addUpdatable(underTest as Updatable)
    }

    @After
    fun tearDown() {
        xrResources.clear()
    }

    @Test
    fun createAnchor_addsAnchor() = initOpenXrManagerAndRunTest {
        check(xrResources.updatables.size == 1)
        check(xrResources.updatables.contains(underTest))

        val anchor = underTest.createAnchor(Pose())

        assertThat(xrResources.updatables).contains(anchor as Updatable)
    }

    @Test
    fun createAnchor_anchorResourcesExhausted_throwsException() = initOpenXrManagerAndRunTest {
        check(xrResources.updatables.size == 1)
        check(xrResources.updatables.contains(underTest))

        // Number of calls comes from 'kAnchorResourcesLimit' defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        repeat(5) { underTest.createAnchor(Pose()) }

        assertThrows(AnchorResourcesExhaustedException::class.java) {
            underTest.createAnchor(Pose())
        }
    }

    @Test
    fun detachAnchor_removesAnchorWhenItDetaches() = initOpenXrManagerAndRunTest {
        val anchor = underTest.createAnchor(Pose())
        check(xrResources.updatables.size == 2)
        check(xrResources.updatables.contains(underTest))
        check(xrResources.updatables.contains(anchor as Updatable))

        anchor.detach()

        assertThat(xrResources.updatables).doesNotContain(anchor)
    }

    @Test
    fun update_updatesTrackingState() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.trackingState.equals(TrackingState.PAUSED))

        underTest.update(xrTime)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_updatesCenterPose() = initOpenXrManagerAndRunTest {
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
    fun update_updatesExtents() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.extents == Vector3())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.extents).isEqualTo(Vector3(1.0f, 2.0f, 3.0f))
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            val perceptionManager = OpenXrPerceptionManager(timeSource)
            openXrManager = OpenXrManager(it, perceptionManager, timeSource)
            openXrManager.create()
            openXrManager.resume()
            openXrManager.configure(
                Config(
                    augmentedObjectCategories =
                        listOf(
                            AugmentedObjectCategory.KEYBOARD,
                            AugmentedObjectCategory.MOUSE,
                            AugmentedObjectCategory.LAPTOP,
                        )
                )
            )

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
