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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Plane as RuntimePlane
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeLifecycleManager
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntimeAnchor
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeRuntimePlane
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class PlaneTest {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var xrResourcesManager: XrResourcesManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        xrResourcesManager = XrResourcesManager()

        val shadowApplication = shadowOf(activity.application)
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }
        FakeRuntimeFactory.hasCreatePermission = true
        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        xrResourcesManager.lifecycleManager = session.runtime.lifecycleManager

        FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @Test
    fun constructor_convertsRuntimePlaneType() {
        val plane1 =
            Plane(
                FakeRuntimePlane(type = RuntimePlane.Type.HORIZONTAL_UPWARD_FACING),
                xrResourcesManager,
            )
        val plane2 =
            Plane(
                FakeRuntimePlane(type = RuntimePlane.Type.HORIZONTAL_DOWNWARD_FACING),
                xrResourcesManager,
            )
        val plane3 = Plane(FakeRuntimePlane(type = RuntimePlane.Type.VERTICAL), xrResourcesManager)

        assertThat(plane1.type).isEqualTo(Plane.Type.HORIZONTAL_UPWARD_FACING)
        assertThat(plane2.type).isEqualTo(Plane.Type.HORIZONTAL_DOWNWARD_FACING)
        assertThat(plane3.type).isEqualTo(Plane.Type.VERTICAL)
    }

    @Test
    fun constructor_convertsRuntimePlaneLabel() {
        val plane1 = Plane(FakeRuntimePlane(label = RuntimePlane.Label.UNKNOWN), xrResourcesManager)
        val plane2 = Plane(FakeRuntimePlane(label = RuntimePlane.Label.WALL), xrResourcesManager)
        val plane3 = Plane(FakeRuntimePlane(label = RuntimePlane.Label.FLOOR), xrResourcesManager)
        val plane4 = Plane(FakeRuntimePlane(label = RuntimePlane.Label.CEILING), xrResourcesManager)
        val plane5 = Plane(FakeRuntimePlane(label = RuntimePlane.Label.TABLE), xrResourcesManager)

        assertThat(plane1.state.value.label).isEqualTo(Plane.Label.UNKNOWN)
        assertThat(plane2.state.value.label).isEqualTo(Plane.Label.WALL)
        assertThat(plane3.state.value.label).isEqualTo(Plane.Label.FLOOR)
        assertThat(plane4.state.value.label).isEqualTo(Plane.Label.CEILING)
        assertThat(plane5.state.value.label).isEqualTo(Plane.Label.TABLE)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun subscribe_collectReturnsPlane() =
        runTest(testDispatcher) {
            val perceptionManager = session.runtime.perceptionManager as FakePerceptionManager
            val runtimePlane = FakeRuntimePlane()
            perceptionManager.addTrackable(runtimePlane)
            activityController.resume()
            advanceUntilIdle()
            activityController.pause()
            var underTest = emptyList<Plane>()

            val job =
                backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    underTest = Plane.subscribe(session).first().toList()
                }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
            assertThat(underTest.first().runtimePlane).isEqualTo(runtimePlane)
            job.cancel()
        }

    @Test
    fun subscribe_planeTrackingDisabled_throwsIllegalStateException() {
        val configureResult = session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Plane.subscribe(session) }
    }

    @Test
    fun createAnchor_usesGivenPose() {
        val runtimePlane = FakeRuntimePlane()
        (session.runtime.perceptionManager as FakePerceptionManager).addTrackable(runtimePlane)
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap.values.first() as Plane
        val pose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))

        val anchorResult = underTest.createAnchor(pose)

        assertThat(anchorResult).isInstanceOf(AnchorCreateSuccess::class.java)
        val anchor = (anchorResult as AnchorCreateSuccess).anchor
        assertThat(anchor.state.value.pose).isEqualTo(pose)
    }

    @Test
    fun createAnchor_anchorLimitReached_returnsAnchorResourcesExhaustedResult() {
        val runtimePlane = FakeRuntimePlane()
        (session.runtime.perceptionManager as FakePerceptionManager).addTrackable(runtimePlane)
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap.values.first() as Plane

        repeat(FakeRuntimeAnchor.ANCHOR_RESOURCE_LIMIT) {
            val result = underTest.createAnchor(Pose())
        }

        assertThat(underTest.createAnchor(Pose()))
            .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
    }

    @Test
    fun createAnchor_planeTrackingDisabled_throwsIllegalStateException() {
        val runtimePlane = FakeRuntimePlane()
        (session.runtime.perceptionManager as FakePerceptionManager).addTrackable(runtimePlane)
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap.values.first() as Plane
        session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { underTest.createAnchor(Pose()) }
    }

    @Test
    fun update_trackingStateMatchesRuntime() = runBlocking {
        val runtimePlane = FakeRuntimePlane()
        runtimePlane.trackingState = TrackingState.STOPPED
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap[runtimePlane] as Plane
        check(underTest.state.value.trackingState == TrackingState.STOPPED)

        runtimePlane.trackingState = TrackingState.TRACKING
        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_centerPoseMatchesRuntime() = runBlocking {
        val runtimePlane = FakeRuntimePlane()
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap[runtimePlane] as Plane
        check(
            (underTest.state.value.centerPose ==
                Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1.0f)))
        )

        val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        runtimePlane.centerPose = newPose
        underTest.update()

        assertThat(underTest.state.value.centerPose).isEqualTo(newPose)
    }

    @Test
    fun update_extentsMatchesRuntime() = runBlocking {
        val runtimePlane = FakeRuntimePlane()
        val extents = Vector2(1.0f, 2.0f)
        runtimePlane.extents = extents
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap[runtimePlane] as Plane
        underTest.update()
        check(underTest.state.value.extents == extents)

        val newExtents = Vector2(3.0f, 4.0f)
        runtimePlane.extents = newExtents
        underTest.update()

        assertThat(underTest.state.value.extents).isEqualTo(newExtents)
    }

    @Test
    fun update_verticesMatchesRuntime() = runBlocking {
        val runtimePlane = FakeRuntimePlane()
        val vertices = listOf(Vector2(1.0f, 2.0f), Vector2(3.0f, 4.0f))
        runtimePlane.vertices = vertices
        xrResourcesManager.syncTrackables(listOf(runtimePlane))
        val underTest = xrResourcesManager.trackablesMap[runtimePlane] as Plane
        underTest.update()
        assertThat(underTest.state.value.vertices).isEqualTo(vertices)

        val newVertices = listOf(Vector2(3.0f, 4.0f), Vector2(5.0f, 6.0f))
        runtimePlane.vertices = newVertices
        underTest.update()

        assertThat(underTest.state.value.vertices).isEqualTo(newVertices)
    }

    @Test
    fun update_subsumedByMatchesRuntime() = runBlocking {
        val runtimePlane = FakeRuntimePlane()
        val subsumedByRuntimePlane = FakeRuntimePlane()
        (session.runtime.perceptionManager as FakePerceptionManager).addTrackable(runtimePlane)
        (session.runtime.perceptionManager as FakePerceptionManager).addTrackable(
            subsumedByRuntimePlane
        )
        xrResourcesManager.syncTrackables(listOf(runtimePlane, subsumedByRuntimePlane))
        xrResourcesManager.update()
        val underTest = xrResourcesManager.trackablesMap[runtimePlane] as Plane
        val subsumingPlaneWrapper =
            xrResourcesManager.trackablesMap[subsumedByRuntimePlane] as Plane
        check(underTest.state.value.subsumedBy == null)

        runtimePlane.subsumedBy = subsumedByRuntimePlane
        xrResourcesManager.update()

        assertThat(underTest.state.value.subsumedBy).isNotNull()
        assertThat(underTest.state.value.subsumedBy).isEqualTo(subsumingPlaneWrapper)
        assertThat(underTest.state.value.subsumedBy!!.runtimePlane)
            .isEqualTo(subsumedByRuntimePlane)
    }

    @Test
    fun labelToString_returnsCorrectString() {
        assertThat(Plane.Label.WALL.toString()).isEqualTo("WALL")
        assertThat(Plane.Label.FLOOR.toString()).isEqualTo("FLOOR")
        assertThat(Plane.Label.CEILING.toString()).isEqualTo("CEILING")
        assertThat(Plane.Label.TABLE.toString()).isEqualTo("TABLE")
        assertThat(Plane.Label.UNKNOWN.toString()).isEqualTo("UNKNOWN")
    }

    @Test
    fun typeToString_returnsCorrectString() {
        assertThat(Plane.Type.HORIZONTAL_UPWARD_FACING.toString())
            .isEqualTo("HORIZONTAL_UPWARD_FACING")
        assertThat(Plane.Type.HORIZONTAL_DOWNWARD_FACING.toString())
            .isEqualTo("HORIZONTAL_DOWNWARD_FACING")
        assertThat(Plane.Type.VERTICAL.toString()).isEqualTo("VERTICAL")
    }
}
