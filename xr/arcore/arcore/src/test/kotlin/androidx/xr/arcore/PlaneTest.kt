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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestPlane
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class PlaneTest {
    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
    }

    @Test
    fun constructor_convertsRuntimePlaneType() =
        runTest(testDispatcher) {
            val plane1 = TestPlane(PlaneType.HORIZONTAL_UPWARD_FACING, PlaneLabel.FLOOR)
            val plane2 = TestPlane(PlaneType.HORIZONTAL_DOWNWARD_FACING, PlaneLabel.CEILING)
            val plane3 = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(plane1, plane2, plane3)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            check(underTest.size == 3)
            advanceUntilIdle()

            assertThat(underTest.count { it.type == PlaneType.HORIZONTAL_UPWARD_FACING })
                .isEqualTo(1)
            assertThat(underTest.count { it.type == PlaneType.HORIZONTAL_DOWNWARD_FACING })
                .isEqualTo(1)
            assertThat(underTest.count { it.type == PlaneType.VERTICAL }).isEqualTo(1)
        }

    @Test
    fun constructor_convertsRuntimePlaneLabel() =
        runTest(testDispatcher) {
            val plane1 = TestPlane(PlaneType.VERTICAL, PlaneLabel.UNKNOWN)
            val plane2 = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            val plane3 = TestPlane(PlaneType.VERTICAL, PlaneLabel.FLOOR)
            val plane4 = TestPlane(PlaneType.VERTICAL, PlaneLabel.CEILING)
            val plane5 = TestPlane(PlaneType.VERTICAL, PlaneLabel.TABLE)
            arCoreTestRule.addTrackables(plane1, plane2, plane3, plane4, plane5)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            check(underTest.size == 5)
            advanceUntilIdle()

            assertThat(underTest.count { it.state.value.label == PlaneLabel.UNKNOWN }).isEqualTo(1)
            assertThat(underTest.count { it.state.value.label == PlaneLabel.WALL }).isEqualTo(1)
            assertThat(underTest.count { it.state.value.label == PlaneLabel.FLOOR }).isEqualTo(1)
            assertThat(underTest.count { it.state.value.label == PlaneLabel.CEILING }).isEqualTo(1)
            assertThat(underTest.count { it.state.value.label == PlaneLabel.TABLE }).isEqualTo(1)
        }

    @Test
    fun subscribe_collectReturnsPlane() =
        runTest(testDispatcher) {
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
            assertThat(underTest.single().type).isEqualTo(PlaneType.VERTICAL)
        }

    @Test
    fun subscribe_planeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Plane.subscribe(session) }
    }

    @Test
    fun createAnchor_usesGivenPose() =
        runTest(testDispatcher) {
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)

            advanceUntilIdle()

            var underTest: Plane? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.first() }
            }
            check(underTest != null)

            val anchorPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
            val expectedPose = underTest.state.value.centerPose.compose(anchorPose)
            val anchorResult = underTest.createAnchor(anchorPose)
            check(anchorResult is AnchorCreateSuccess)
            val anchorState = anchorResult.anchor.state.value

            assertThat(anchorState.pose.translation).isEqualTo(expectedPose.translation)
            assertThat(anchorState.pose.rotation).isEqualTo(expectedPose.rotation)
        }

    @Test
    fun createAnchor_anchorLimitReached_returnsAnchorResourcesExhaustedResult() =
        runTest(testDispatcher) {
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)

            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }

            repeat(arCoreTestRule.anchorResourceLimit) { underTest.single().createAnchor(Pose()) }

            assertThat(underTest.single().createAnchor(Pose()))
                .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
        }

    @Test
    fun createAnchor_planeTrackingDisabled_throwsIllegalStateException() =
        runTest(testDispatcher) {
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)

            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }

            activityController.pause()
            advanceUntilIdle()
            session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))
            activityController.resume()

            assertFailsWith<IllegalStateException> { underTest.single().createAnchor(Pose()) }
        }

    @Test
    fun update_trackingStateMatchesTestPlaneVisibility() =
        runTest(testDispatcher) {
            activityController.resume()
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.TRACKING)

            testPlane.isVisible = false
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun update_planeTrackingDisabled_trackingStops() =
        runTest(testDispatcher) {
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)

            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }

            activityController.pause()
            advanceUntilIdle()
            session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))
            activityController.resume()
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.STOPPED)
        }

    @Test
    fun update_centerPoseMatchesTestPlaneCenterPose() =
        runTest(testDispatcher) {
            activityController.resume()
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(Pose())

            val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
            testPlane.centerPose = newPose
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(newPose)
        }

    @Test
    fun update_extentsMatchesTestPlaneExtents() =
        runTest(testDispatcher) {
            activityController.resume()
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.extents).isEqualTo(FloatSize2d())

            val newExtents = FloatSize2d(3.0f, 4.0f)
            testPlane.extents = newExtents
            advanceUntilIdle()

            assertThat(underTest.single().state.value.extents).isEqualTo(newExtents)
        }

    @Test
    fun update_verticesMatchesTestPlaneVertices() =
        runTest(testDispatcher) {
            activityController.resume()
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.addTrackables(testPlane)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.vertices).isEqualTo(listOf<Vector2>())

            val newVertices = listOf(Vector2(3.0f, 4.0f), Vector2(5.0f, 6.0f))
            testPlane.vertices = newVertices
            advanceUntilIdle()

            assertThat(underTest.single().state.value.vertices).isEqualTo(newVertices)
        }

    @Test
    fun update_subsumedByMatchesTestPlaneSubsumedBy() =
        runTest(testDispatcher) {
            activityController.resume()
            val testPlane1 = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            val testPlane2 = TestPlane(PlaneType.HORIZONTAL_UPWARD_FACING, PlaneLabel.TABLE)
            arCoreTestRule.addTrackables(testPlane1, testPlane2)
            advanceUntilIdle()

            var underTest = emptyList<Plane>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            testPlane1.subsumedBy = testPlane2
            advanceUntilIdle()

            // XrResourcesManager stores Trackables in a ConcurrentHashMap so entries are unordered
            val subsumed = underTest.first { it.state.value.subsumedBy != null }
            val subsumer = underTest.first { it.state.value.subsumedBy == null }
            assertThat(subsumed.state.value.subsumedBy).isEqualTo(subsumer)
        }
}
