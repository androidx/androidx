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
import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.ArDevice as RuntimeArDevice
import androidx.xr.arcore.runtime.AugmentedImage as RuntimeAugmentedImage
import androidx.xr.arcore.runtime.AugmentedObject as RuntimeAugmentedObject
import androidx.xr.arcore.runtime.Depth as RuntimeDepth
import androidx.xr.arcore.runtime.Face as RuntimeFace
import androidx.xr.arcore.runtime.Geospatial as RuntimeGeospatial
import androidx.xr.arcore.runtime.Hand as RuntimeHand
import androidx.xr.arcore.runtime.HandJointType
import androidx.xr.arcore.runtime.Plane as RuntimePlane
import androidx.xr.arcore.runtime.RenderViewpoint as RuntimeRenderViewpoint
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.HAND_TRACKING
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.UUID
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class XrResourcesManagerTest {

    private lateinit var underTest: XrResourcesManager
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

        shadowOf(activity.application).grantPermissions(HAND_TRACKING)

        activityController.create().start().resume()

        session =
            (Session.create(context = activity, coroutineContext = testDispatcher)
                    as SessionCreateSuccess)
                .session
        underTest =
            session.stateExtenders
                .filterIsInstance<PerceptionStateExtender>()
                .first()
                .xrResourcesManager
    }

    @After
    fun tearDown() {
        underTest.clear()
    }

    @Test
    fun initiateHands_setsAvailableHands() {
        val leftRuntimeHand = StubRuntimeHand()
        val rightRuntimeHand = StubRuntimeHand()
        underTest.initiateHands(leftRuntimeHand, rightRuntimeHand)

        assertThat(underTest.leftHand).isNotNull()
        assertThat(underTest.leftHand!!.runtimeHand).isEqualTo(leftRuntimeHand)
        assertThat(underTest.rightHand).isNotNull()
        assertThat(underTest.rightHand!!.runtimeHand).isEqualTo(rightRuntimeHand)
    }

    @Test
    fun initiateEyes_setsAvailableEyes() {
        val leftRuntimeEye = StubRuntimeEye()
        val rightRuntimeEye = StubRuntimeEye()
        underTest.initiateEyes(leftRuntimeEye, rightRuntimeEye)

        assertThat(underTest.leftEye).isNotNull()
        assertThat(underTest.leftEye!!.runtimeEye).isEqualTo(leftRuntimeEye)
        assertThat(underTest.rightEye).isNotNull()
        assertThat(underTest.rightEye!!.runtimeEye).isEqualTo(rightRuntimeEye)
    }

    @Test
    fun initiateEyes_setsWithNull() {
        underTest.initiateEyes(leftRuntimeEye = null, rightRuntimeEye = null)

        assertThat(underTest.leftEye).isNull()
        assertThat(underTest.rightEye).isNull()
    }

    @Test
    fun initiateHands_setsWithNull() {
        underTest.initiateHands(leftRuntimeHand = null, rightRuntimeHand = null)

        assertThat(underTest.leftHand).isNull()
        assertThat(underTest.rightHand).isNull()
    }

    @Test
    fun initiateArDevice_setsArDeviceAndRenderViewpoints() {
        val runtimeArDevice = StubRuntimeArDevice()
        val leftRuntimeRenderViewpoint = StubRuntimeRenderViewpoint()
        val rightRuntimeRenderViewpoint = StubRuntimeRenderViewpoint()
        val monoRuntimeRenderViewpoint = StubRuntimeRenderViewpoint()
        underTest.initiateArDeviceAndRenderViewpoints(
            runtimeArDevice,
            leftRuntimeRenderViewpoint,
            rightRuntimeRenderViewpoint,
            monoRuntimeRenderViewpoint,
        )

        assertThat(underTest.arDevice.runtimeArDevice).isEqualTo(runtimeArDevice)
        assertThat(underTest.leftRenderViewpoint!!.state.value.pose)
            .isEqualTo(leftRuntimeRenderViewpoint.pose)
        assertThat(underTest.rightRenderViewpoint!!.state.value.pose)
            .isEqualTo(rightRuntimeRenderViewpoint.pose)
        assertThat(underTest.monoRenderViewpoint!!.state.value.pose)
            .isEqualTo(monoRuntimeRenderViewpoint.pose)
    }

    @Test
    fun initiateFace_setsAvailableFace() {
        val userRuntimeFace = StubRuntimeFace()

        underTest.initiateFace(userRuntimeFace)

        assertThat(underTest.userFace!!.runtimeFace).isEqualTo(userRuntimeFace)
    }

    @Test
    fun addUpdatable_addsUpdatable() =
        runTest(testDispatcher) {
            val perceptionManager = session.perceptionRuntime.perceptionManager
            val anchor = Anchor(perceptionManager.createAnchor(Pose()), underTest)
            check(underTest.updatables.isEmpty())

            underTest.addUpdatable(anchor)

            assertThat(underTest.updatables).containsExactly(anchor)
        }

    @Test
    fun removeUpdatable_removesUpdatable() =
        runTest(testDispatcher) {
            val perceptionManager = session.perceptionRuntime.perceptionManager
            val anchor = Anchor(perceptionManager.createAnchor(Pose()), underTest)
            underTest.addUpdatable(anchor)
            check(underTest.updatables.contains(anchor))
            check(underTest.updatables.size == 1)

            underTest.removeUpdatable(anchor)

            assertThat(underTest.updatables).isEmpty()
        }

    @Test
    fun clear_clearAllUpdatables() =
        runTest(testDispatcher) {
            val perceptionManager = session.perceptionRuntime.perceptionManager
            val runtimeAnchor = perceptionManager.createAnchor(Pose())
            val runtimeAnchor2 = perceptionManager.createAnchor(Pose())
            val anchor = Anchor(runtimeAnchor, underTest)
            val anchor2 = Anchor(runtimeAnchor2, underTest)
            underTest.addUpdatable(anchor)
            underTest.addUpdatable(anchor2)
            check(underTest.updatables.isNotEmpty())

            underTest.clear()

            assertThat(underTest.updatables).isEmpty()
        }

    @Test
    fun syncTrackables_replacesExistingTrackables() {
        val runtimePlane1 = StubRuntimePlane()
        val runtimePlane2 = StubRuntimePlane()
        val runtimePlane3 = StubRuntimePlane()
        underTest.syncTrackables(listOf(runtimePlane1, runtimePlane2))
        check(underTest.trackablesMap[runtimePlane1] != null)
        check(underTest.trackablesMap[runtimePlane2] != null)
        check(underTest.trackablesMap[runtimePlane3] == null)

        underTest.syncTrackables(listOf(runtimePlane2, runtimePlane3))

        assertThat(underTest.trackablesMap[runtimePlane1]).isNull()
        assertThat(underTest.trackablesMap[runtimePlane2]).isNotNull()
        assertThat(underTest.trackablesMap[runtimePlane3]).isNotNull()
    }

    @Test
    fun syncTrackables_handlesAugmentedObjects() {
        val runtimeAugmentedObject1 = StubRuntimeAugmentedObject()
        val runtimeAugmentedObject2 = StubRuntimeAugmentedObject()
        val runtimeAugmentedObject3 = StubRuntimeAugmentedObject()

        underTest.syncTrackables(listOf(runtimeAugmentedObject1, runtimeAugmentedObject2))

        assertThat(underTest.trackablesMap[runtimeAugmentedObject1]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeAugmentedObject2]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeAugmentedObject3]).isNull()
    }

    @Test
    fun syncTrackables_handlesAugmentedImages() {
        val runtimeAugmentedImage1 = StubRuntimeAugmentedImage()
        val runtimeAugmentedImage2 = StubRuntimeAugmentedImage()
        val runtimeAugmentedImage3 = StubRuntimeAugmentedImage()

        underTest.syncTrackables(listOf(runtimeAugmentedImage1, runtimeAugmentedImage2))

        assertThat(underTest.trackablesMap[runtimeAugmentedImage1]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeAugmentedImage2]).isNotNull()
        assertThat(underTest.trackablesMap[runtimeAugmentedImage3]).isNull()
    }

    @Test
    fun clear_clearsAllTrackables() {
        val runtimePlane = StubRuntimePlane()
        underTest.syncTrackables(listOf(runtimePlane))
        check(underTest.trackablesMap.isNotEmpty())

        underTest.clear()

        assertThat(underTest.trackablesMap).isEmpty()
    }

    @Test
    fun update_anchorDetached_andNotUpdated() =
        runTest(testDispatcher) {
            val runtimePlane = StubRuntimePlane()
            val anchor = Anchor(runtimePlane.createAnchor(Pose()), underTest)
            anchor.detach()
            check(underTest.anchorsToDetachQueue.contains(anchor))

            underTest.update()

            assertThat(underTest.anchorsToDetachQueue).isEmpty()
        }

    @Test
    fun update_geospatialUpdated() =
        runTest(testDispatcher) {
            val runtimeGeospatial = StubRuntimeGeospatial()
            underTest.initiateGeospatial(runtimeGeospatial)
            underTest.update()
            check(underTest.geospatial.state.value == GeospatialState.NOT_RUNNING)

            runtimeGeospatial.state = RuntimeGeospatial.State.RUNNING
            underTest.update()

            assertThat(underTest.geospatial.state.value).isEqualTo(GeospatialState.RUNNING)
        }

    @Test
    fun update_updatesDepths() =
        runTest(testDispatcher) {
            val runtimeDepth = StubRuntimeDepth()
            underTest.initiateDepths(runtimeDepth, null, null)
            underTest.update()
            check(underTest.leftDepth != null)
            check(underTest.leftDepth!!.state.value.width == 0)
            val expectedWidth = 100

            runtimeDepth.width = expectedWidth
            underTest.update()
            underTest.leftDepth!!.update()

            assertThat(underTest.leftDepth!!.state.value.width).isEqualTo(expectedWidth)
        }

    private class StubRuntimeAnchor : RuntimeAnchor {
        override val pose = Pose()
        override val trackingState = TrackingState.TRACKING
        override val persistenceState = RuntimeAnchor.PersistenceState.NOT_PERSISTED
        override val uuid: UUID = UUID.randomUUID()

        override fun detach() {}

        override fun persist() {}
    }

    private class StubRuntimeArDevice : RuntimeArDevice {
        override val devicePose = Pose()
        override val trackingState = TrackingState.TRACKING
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private class StubRuntimeRenderViewpoint : RuntimeRenderViewpoint {
        override val pose = Pose()
        override val fieldOfView = androidx.xr.runtime.FieldOfView(0f, 0f, 0f, 0f)
    }

    private class StubRuntimeHand : RuntimeHand {
        val bufferSize = HandJointType.entries.size * 7 * Float.SIZE_BYTES
        override val trackingState = TrackingState.TRACKING
        override val handJointsBuffer: FloatBuffer = ByteBuffer.allocate(bufferSize).asFloatBuffer()
    }

    private class StubRuntimeEye : androidx.xr.arcore.runtime.Eye {
        override val isOpen = true
        override val pose = Pose()
        override val trackingState = TrackingState.TRACKING
    }

    private class StubRuntimePlane : RuntimePlane {
        override val type = RuntimePlane.Type.VERTICAL
        override val label = RuntimePlane.Label.WALL
        override val centerPose = Pose()
        override val extents = FloatSize2d()
        override val subsumedBy = null
        override val vertices: List<Vector2> = emptyList()
        override val trackingState = TrackingState.TRACKING

        override fun createAnchor(pose: Pose): RuntimeAnchor = StubRuntimeAnchor()
    }

    private class StubRuntimeAugmentedObject : RuntimeAugmentedObject {
        override val category = AugmentedObjectCategory.UNKNOWN
        override val centerPose = Pose()
        override val extents = FloatSize3d()
        override val trackingState = TrackingState.TRACKING
    }

    private class StubRuntimeAugmentedImage : RuntimeAugmentedImage {
        override val index = 0
        override val centerPose = Pose()
        override val extents = FloatSize2d()
        override val trackingState = TrackingState.TRACKING
    }

    private class StubRuntimeFace : RuntimeFace {
        override val isValid = true
        override val blendShapeValues = FloatArray(0)
        override val confidenceValues = FloatArray(0)
        override val centerPose = null
        override val mesh = null
        override val noseTipPose = null
        override val foreheadLeftPose = null
        override val foreheadRightPose = null
        override val trackingState = TrackingState.TRACKING
    }

    private class StubRuntimeDepth : RuntimeDepth {
        override var width = 0
        override var height = 0
        override val rawDepthMap: FloatBuffer? = null
        override val rawConfidenceMap: ByteBuffer? = null
        override val smoothDepthMap: FloatBuffer? = null
        override val smoothConfidenceMap: ByteBuffer? = null
    }

    private class StubRuntimeGeospatial : RuntimeGeospatial {
        override var state = RuntimeGeospatial.State.NOT_RUNNING

        override fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose = Pose()

        override fun createGeospatialPoseFromPose(
            pose: Pose
        ): RuntimeGeospatial.GeospatialPoseResult =
            RuntimeGeospatial.GeospatialPoseResult(
                GeospatialPose(0.0, 0.0, 0.0, Quaternion()),
                0.0,
                0.0,
                0.0,
            )

        override fun createAnchor(
            latitude: Double,
            longitude: Double,
            altitude: Double,
            eastUpSouthQuaternion: Quaternion,
        ) = StubRuntimeAnchor()

        override suspend fun createAnchorOnSurface(
            latitude: Double,
            longitude: Double,
            altitudeAboveSurface: Double,
            eastUpSouthQuaternion: Quaternion,
            surface: RuntimeGeospatial.Surface,
        ) = StubRuntimeAnchor()

        override suspend fun checkVpsAvailability(latitude: Double, longitude: Double) =
            VpsAvailabilityUnavailable()
    }
}
