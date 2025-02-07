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
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.Plane
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
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
class OpenXrPlaneTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    private val planeId = 1L

    lateinit private var openXrManager: OpenXrManager
    lateinit private var xrResources: XrResources
    lateinit private var underTest: OpenXrPlane

    @Before
    fun setUp() {
        xrResources = XrResources()
        underTest =
            OpenXrPlane(planeId, Plane.Type.HorizontalUpwardFacing, OpenXrTimeSource(), xrResources)
        xrResources.addTrackable(planeId, underTest)
        xrResources.addUpdatable(underTest as Updatable)
    }

    @After
    fun tearDown() {
        xrResources.clear()
    }

    @Test
    fun createAnchor_addsAnchor() = initOpenXrManagerAndRunTest {
        check(xrResources.updatables.size == 3)
        check(xrResources.updatables.contains(underTest))

        val anchor = underTest.createAnchor(Pose())

        assertThat(xrResources.updatables)
            .containsExactly(
                underTest,
                xrResources.leftHand,
                xrResources.rightHand,
                anchor as Updatable
            )
    }

    @Test
    fun createAnchor_anchorResourcesExhausted_throwsException() = initOpenXrManagerAndRunTest {
        check(xrResources.updatables.size == 3)
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
        check(xrResources.updatables.size == 4)
        check(xrResources.updatables.contains(underTest))
        check(xrResources.updatables.contains(anchor as Updatable))

        anchor.detach()

        assertThat(xrResources.updatables)
            .containsExactly(underTest, xrResources.leftHand, xrResources.rightHand)
    }

    @Test
    fun update_updatesTrackingState() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.trackingState.equals(TrackingState.Paused))

        underTest.update(xrTime)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.Tracking)
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
        check(underTest.centerPose == Pose())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.extents).isEqualTo(Vector2(1.0f, 2.0f))
    }

    @Test
    fun update_updatesVertices() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.vertices.isEmpty())

        underTest.update(xrTime)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kVertices` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.vertices.size).isEqualTo(4)
        assertThat(underTest.vertices[0]).isEqualTo(Vector2(2.0f, 0.0f))
        assertThat(underTest.vertices[1]).isEqualTo(Vector2(2.0f, 2.0f))
        assertThat(underTest.vertices[2]).isEqualTo(Vector2(0.0f, 0.0f))
        assertThat(underTest.vertices[3]).isEqualTo(Vector2(0.0f, 2.0f))
    }

    @Test
    fun update_updatesSubsumedBy() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        val planeSubsumedId = 67890L
        val planeSubsumed: OpenXrPlane =
            OpenXrPlane(
                planeSubsumedId,
                Plane.Type.HorizontalUpwardFacing,
                OpenXrTimeSource(),
                xrResources,
            )
        xrResources.addTrackable(planeSubsumedId, planeSubsumed)
        xrResources.addUpdatable(planeSubsumed as Updatable)
        check(planeSubsumed.subsumedBy == null)
        check(xrResources.trackablesMap.containsKey(planeId))

        planeSubsumed.update(xrTime)

        assertThat(planeSubsumed.subsumedBy).isEqualTo(underTest)
    }

    @Test
    fun update_noSubsumedByPlanes_setsSubsumedByToNull() = initOpenXrManagerAndRunTest {
        val xrTime = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
        check(underTest.subsumedBy == null)

        underTest.update(xrTime)

        assertThat(underTest.subsumedBy).isNull()
    }

    @Test
    fun fromOpenXrType_withValidValue_convertsType() {
        val planeType: Plane.Type = Plane.Type.fromOpenXrType(0)

        assertThat(planeType).isEqualTo(Plane.Type.HorizontalDownwardFacing)
    }

    @Test
    fun fromOpenXrType_withInvalidValue_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { Plane.Type.fromOpenXrType(3) }
    }

    @Test
    fun fromOpenXrLabel_withValidValue_convertsLabel() {
        val planeLabel: Plane.Label = Plane.Label.fromOpenXrLabel(0)

        assertThat(planeLabel).isEqualTo(Plane.Label.Unknown)
    }

    @Test
    fun fromOpenXrLabel_withInvalidValue_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { Plane.Label.fromOpenXrLabel(5) }
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
