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

package androidx.xr.scenecore

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimePlane
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.AnchorEntity as RtAnchorEntity
import androidx.xr.scenecore.runtime.AnchorPlacement as RtAnchorPlacement
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.MovableComponent as RtMovableComponent
import androidx.xr.scenecore.runtime.MoveEvent as RtMoveEvent
import androidx.xr.scenecore.runtime.MoveEventListener as RtMoveEventListener
import androidx.xr.scenecore.runtime.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic as RtPlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType as RtPlaneType
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeMovableComponent
import androidx.xr.scenecore.testing.FakeScenePose
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import kotlin.test.assertFailsWith
import kotlin.time.TestTimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MovableComponentTest {
    private val fakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private val mockSceneRuntime = mock<SceneRuntime>()
    private lateinit var mSceneRuntime: FakeSceneRuntime
    private lateinit var session: Session
    private lateinit var mFakeRuntime: FakePerceptionRuntime
    private lateinit var mFakeLifecycleManager: FakeLifecycleManager
    private lateinit var mFakePerceptionManager: FakePerceptionManager
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockGroupEntity = mock<RtEntity>()
    private val mockAnchorEntity = mock<RtAnchorEntity>()
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var timeSource: TestTimeSource
    private var mCurrentTimeMillis: Long = 1000000000L
    private var anchorEntityToDispose: AnchorEntity? = null

    object MockitoHelper {
        // use this in place of captor.capture() if you are trying to capture an argument that is
        // not nullable
        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
    }

    private fun createSession() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val result = Session.create(activity, testDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        mFakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
        mFakeLifecycleManager = mFakeRuntime.lifecycleManager
        mFakePerceptionManager = mFakeRuntime.perceptionManager
        mSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()
        timeSource = mFakeLifecycleManager.timeSource
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
    }

    private fun createCustomSession() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        whenever(mockSceneRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockSceneRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockSceneRuntime.headActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockSceneRuntime.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockSceneRuntime.createGroupEntity(any(), any(), any()))
            .thenReturn(mockGroupEntity)
        whenever(mockSceneRuntime.createAnchorEntity()).thenReturn(mockAnchorEntity)
        whenever(mockAnchorEntity.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        session =
            Session(
                activity,
                runtimes =
                    listOf(fakePerceptionRuntimeFactory.createRuntime(activity), mockSceneRuntime),
            )
        session.configure(Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        mFakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
        mFakeLifecycleManager = mFakeRuntime.lifecycleManager
        mFakePerceptionManager = mFakeRuntime.perceptionManager
        timeSource = mFakeLifecycleManager.timeSource
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
    }

    @After
    fun tearDown() {
        anchorEntityToDispose?.dispose()
        anchorEntityToDispose = null
    }

    @Test
    fun addMovableComponent_addsRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockEntityMoveListener = mock<EntityMoveListener>()

        val movableComponent =
            MovableComponent.createCustomMovable(
                session = session,
                scaleInZ = false,
                directExecutor(),
                mockEntityMoveListener,
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockSceneRuntime)
            .createMovableComponent(systemMovable = false, scaleInZ = false, userAnchorable = false)
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun addAutoMovableComponent_addsRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockSceneRuntime)
            .createMovableComponent(systemMovable = true, scaleInZ = false, userAnchorable = false)
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun addMovableAnchorableComponent_addsRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockAnchorPlacement = mock<RtAnchorPlacement>()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        whenever(
                mockSceneRuntime.createAnchorPlacementForPlanes(
                    setOf(RtPlaneType.HORIZONTAL),
                    setOf(RtPlaneSemantic.WALL),
                )
            )
            .thenReturn(mockAnchorPlacement)

        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                setOf(PlaneOrientation.HORIZONTAL),
                setOf(PlaneSemanticType.WALL),
            )

        val movableComponent =
            MovableComponent.createAnchorable(
                session = session,
                anchorPlacement = setOf(anchorPlacement),
                disposeParentOnReAnchor = false,
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockSceneRuntime)
            .createMovableComponent(systemMovable = true, scaleInZ = false, userAnchorable = true)
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun createAnchorableWithEmptySet_throwsException() {
        createCustomSession()
        assertFailsWith<IllegalArgumentException> {
            MovableComponent.createAnchorable(
                session = session,
                anchorPlacement = emptySet(),
                disposeParentOnReAnchor = false,
            )
        }
    }

    @Test
    fun addMovableComponentToAnchorEntity_returnsFalse() {
        createCustomSession()
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)
        assertThat(anchorEntity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(anchorEntity.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun addMovableComponentToActivitySpace_returnsFalse() {
        createCustomSession()
        val activitySpace = session.scene.activitySpace
        assertThat(activitySpace).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(activitySpace.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun addMovableComponentDefaultArguments_addsRuntimeMovableComponentWithDefaults() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockSceneRuntime)
            .createMovableComponent(systemMovable = true, scaleInZ = true, userAnchorable = false)
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun removeMovableComponent_removesRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        entity.removeComponent(movableComponent)
        verify(mockGroupEntity).removeComponent(any())
    }

    @Test
    fun movableComponent_canAttachOnlyOnce() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        val entity2 = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity2.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun movableComponent_setSizeInvokesRuntimeMovableComponentSetSize() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        val testSize = FloatSize3d(2f, 2f, 0f)
        movableComponent.size = testSize

        assertThat(movableComponent.size).isEqualTo(testSize)
        verify(mockRtMovableComponent).size = any()
    }

    @Test
    fun movableComponent_addMoveListenerInvokesRuntimeMovableComponentAddMoveEventListener() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockEntityMoveListener = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener)

        val captor: ArgumentCaptor<RtMoveEventListener> =
            ArgumentCaptor.forClass(RtMoveEventListener::class.java)

        verify(mockRtMovableComponent).addMoveEventListener(any(), MockitoHelper.capture(captor))
        val rtMoveEventListener = captor.value
        var rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())

        rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveUpdate(any(), any(), any(), any())

        rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_END,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                mockAnchorEntity,
                disposedEntity = null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveEnd(any(), any(), any(), any(), any())
    }

    @Test
    fun movableComponent_addMultipleMoveEventListenersInvokesAllListeners() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockEntityMoveListener = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener)
        val mockEntityMoveListener2 = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener2)

        val captor: ArgumentCaptor<RtMoveEventListener> =
            ArgumentCaptor.forClass(RtMoveEventListener::class.java)

        verify(mockRtMovableComponent).addMoveEventListener(any(), MockitoHelper.capture(captor))
        val rtMoveEventListener = captor.value
        val rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2).onMoveStart(any(), any(), any(), any(), any())
    }

    @Test
    fun movableComponent_removeMoveEventListenerInvokesRuntimeRemoveMoveEventListener() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockEntityMoveListener = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener)
        val mockEntityMoveListener2 = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener2)

        val captor: ArgumentCaptor<RtMoveEventListener> =
            ArgumentCaptor.forClass(RtMoveEventListener::class.java)

        verify(mockRtMovableComponent).addMoveEventListener(any(), MockitoHelper.capture(captor))
        val rtMoveEventListener = captor.value
        val rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2).onMoveStart(any(), any(), any(), any(), any())

        movableComponent.removeMoveListener(mockEntityMoveListener)
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        // The first listener, which we removed, should not be called again.
        verify(mockEntityMoveListener, times(1)).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2, times(2)).onMoveStart(any(), any(), any(), any(), any())

        movableComponent.removeMoveListener(mockEntityMoveListener2)
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        // The listeners, now both removed, should have the same invocation counts.
        verify(mockEntityMoveListener, times(1)).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2, times(2)).onMoveStart(any(), any(), any(), any(), any())
    }

    @Test
    fun movablecomponent_canAttachAgainAfterDetach() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        entity.removeComponent(movableComponent)
        assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun createMovableComponent_callsRuntimeCreateMovableComponent() {
        createCustomSession()
        whenever(mockSceneRuntime.createMovableComponent(any(), any(), any())).thenReturn(mock())

        val movableComponent = MovableComponent.createSystemMovable(session)
        val view = TextView(activity)
        val mockRtPanelEntity = mock<RtPanelEntity>()
        whenever(
                mockSceneRuntime.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockRtPanelEntity)
        whenever(mockRtPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        verify(mockSceneRuntime).createMovableComponent(any(), any(), any())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun customMovableComponent_invokesInitialListener() {
        createCustomSession()
        runTest(testDispatcher) {
            val entity = GroupEntity.create(session, "test")
            assertThat(entity).isNotNull()
            val mockRtMovableComponent = mock<RtMovableComponent>()
            whenever(mockSceneRuntime.createMovableComponent(any(), any(), any()))
                .thenReturn(mockRtMovableComponent)
            whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
            val mockEntityMoveListener = mock<EntityMoveListener>()
            val movableComponent =
                MovableComponent.createCustomMovable(
                    session,
                    true,
                    testDispatcher.asExecutor(),
                    mockEntityMoveListener,
                )
            assertThat(entity.addComponent(movableComponent)).isTrue()

            val captor: ArgumentCaptor<RtMoveEventListener> =
                ArgumentCaptor.forClass(RtMoveEventListener::class.java)

            verify(mockRtMovableComponent)
                .addMoveEventListener(any(), MockitoHelper.capture(captor))
            val rtMoveEventListener = captor.value
            val rtMoveEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    Pose(),
                    Pose(),
                    Vector3(1f, 1f, 1f),
                    Vector3(1f, 1f, 1f),
                    mockActivitySpace,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveEvent)
            advanceUntilIdle()

            verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_updatesThePoseBasedOnPlanes() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 3 units because the activity space is 1 unit below of the origin and the
            // plane is 2 units above it.
            val expectedPose = Pose(Vector3(1f, 3f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_nullParent_updatesThePoseBasedOnPlanes() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            panelEntity.parent = null
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 3 units because the activity space is 1 unit below of the origin and the
            // plane is 2 units above it.
            val expectedPose = Pose(Vector3(1f, 3f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_withNonActivityParent_updatesPoseBasedOnPlanesAndParent() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val groupEntityPose = Pose(Vector3(0f, -1f, 0f), Quaternion.Identity)
            val groupEntity = GroupEntity.create(session, "test", groupEntityPose)
            (groupEntity.rtScenePose as FakeScenePose).activitySpacePose = groupEntityPose
            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            panelEntity.parent = groupEntity
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    groupEntity.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 3 units because the activity space is 1 unit below of the origin and the
            // plane is 2 units above it. Since the parent is 1 unit below the activity space, the
            // expected position should be 4 units above the parent.
            val expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_scaledParent_updatesThePoseBasedOnPlanes() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpaceScale = Vector3(2f, 2f, 2f)
            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.activitySpace.rtScenePose as FakeScenePose).activitySpaceScale =
                activitySpaceScale
            (session.scene.activitySpace.rtEntity!! as FakeActivitySpace).setScale(
                activitySpaceScale
            )
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(.5f, .5f, .5f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 1.5 units because the activity space is 1 unit below of the origin and
            // the plane is 2 units above it and the activity space is scaled by 2.
            val expectedPose = Pose(Vector3(.5f, 1.5f, .5f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_withinAnchorDistance_setsAnchorEntity() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            val rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            val expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)
            anchorEntityToDispose = panelEntity.parent as AnchorEntity
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_withinAnchorDistanceAboveAnchor_resetsPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            // Put the proposed position at 2 + half the MIN_PLANE_ANCHOR_DISTANCE above the origin.
            // So it would be right above the plane.
            val proposedPose =
                Pose(
                    Vector3(1f, 3f + MovableComponent.MAX_PLANE_ANCHOR_DISTANCE / 2f, 1f),
                    Quaternion.Identity,
                )
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            val rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            val expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)
            anchorEntityToDispose = panelEntity.parent as AnchorEntity
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_withIncorrectPlaneType_doesNotCreateAnchor() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val anchorPlacement =
                AnchorPlacement.createForPlanes(
                    setOf(PlaneOrientation.VERTICAL),
                    setOf(PlaneSemanticType.WALL),
                )
            val movableComponent =
                MovableComponent.createAnchorable(session, setOf(anchorPlacement))
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            val rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // The expected position should be the proposed position from the reform event because
            // no suitable planes can be found.
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(ActivitySpace::class.java)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_withinAnchorDistanceAndScale_setsAnchorEntityAndScales() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpaceScale = Vector3(2f, 2f, 2f)
            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.activitySpace.rtScenePose as FakeScenePose).activitySpaceScale =
                activitySpaceScale
            (session.scene.activitySpace.rtEntity!! as FakeActivitySpace).setScale(
                activitySpaceScale
            )
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            val entityScale = Vector3.One * 5f
            panelEntity.setScale(entityScale)
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(.5f, .5f, .5f), Quaternion.Identity)
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            val rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.
            val expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.getScale()).isEqualTo(activitySpaceScale.x * entityScale.x)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)
            anchorEntityToDispose = panelEntity.parent as AnchorEntity
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_noPlanes_keepsProposedPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be unchanged from the proposed event
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_validPlaneButNotTracking_keepsProposedPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.PAUSED,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be unchanged from the proposed event
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_outsideExtents_keepsProposedPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(5f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // The expected position should be unchanged from the proposed event
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_resetsToScenePoseAfterAnchoring() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            var rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor and it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_resetsAndScaleToScenePoseAfterAnchoring() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpaceScale = Vector3(2f, 2f, 2f)
            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.activitySpace.rtScenePose as FakeScenePose).activitySpaceScale =
                activitySpaceScale
            (session.scene.activitySpace.rtEntity!! as FakeActivitySpace).setScale(
                activitySpaceScale
            )
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            val entityScale = Vector3.One * 5f
            panelEntity.setScale(entityScale)
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            var proposedPose = Pose(Vector3(.5f, .5f, .5f), Quaternion.Identity)
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            var rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated
            // pose(0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)
            assertThat(panelEntity.getScale()).isEqualTo(activitySpaceScale.x * entityScale.x)

            proposedPose = Pose(Vector3(2f, 8f, 2f), Quaternion.Identity)
            rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    panelEntity.rtEntity!!.parent!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving to (2, 8, 2) relative to the anchor entity. This translates to (1, 4, 1)
            // relative to the activity space.This should pull the entity away from the anchor and
            // it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)
            assertThat(panelEntity.getScale()).isEqualTo(entityScale.x)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_childOfEntity_resetsToActivityPoseAfterAnchoring() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            // Create a parent entity whose pose is below the activity space pose.
            val parentPose = Pose(Vector3(0f, -1f, 0f), Quaternion.Identity)
            val parentEntity: Entity = GroupEntity.create(session, "test", parentPose)
            panelEntity.parent = parentEntity

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            var rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor and it should be reparented to the activity space not the original
            // parent..
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_shouldDispose_disposesAnchorAfterUnparenting() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            var rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor and it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)

            // Verify that the anchor entity was disposed by checking that it is no longer in the
            // entity manager.
            assertThat(session.scene.entityManager.getEntitiesOfType(AnchorEntity::class.java).size)
                .isEqualTo(0)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_shouldDispose_doeNotDisposeIfAnchorHasChildren() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            var rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)
            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space. which results in an updated pose
            // of (0, 0, 1) relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90 rotation around the x-axis Which is expected when the panel is
            // rotated into the plane's reference space.
            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorEntity::class.java)

            // Cache anchor entity and give it a child
            val anchorEntity = panelEntity.parent
            val childEntity = GroupEntity.create(session, "test", Pose.Identity)
            childEntity.parent = anchorEntity

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveEndEvent)

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor and it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertThat(panelEntity.getPose()).isEqualTo(expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)

            // Verify that the anchor entity has not been disposed by checking that it is still in
            // the entity manager.
            assertThat(session.scene.entityManager.getEntitiesOfType(AnchorEntity::class.java).size)
                .isEqualTo(1)
            anchorEntityToDispose =
                session.scene.entityManager.getEntitiesOfType(AnchorEntity::class.java).first()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_nearPlane_callsSetPlanePoseWithNonNullPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // Verify that runtime movable component has had plane pose set with non-null pose
            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            assertThat(rtMovableComponent.setPlanePoseForMoveUpdatePoseCallCount).isEqualTo(1)
            assertThat(rtMovableComponent.lastPlanePose).isNotNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_awayFromPlane_callsSetPlanePoseWithNonNullPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity!!.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            mFakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val rtMoveEventListener =
                (movableComponent.rtMovableComponent as FakeMovableComponent)
                    .moveEventListenersMap
                    .entries
                    .first()
                    .key

            // Put the proposed position at 5 above the origin. so it is far away from the plane.
            val proposedPose = Pose(Vector3(1f, 5f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val rtMoveStartEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    session.scene.activitySpace.rtEntity!!,
                    updatedParent = null,
                    disposedEntity = null,
                )

            rtMoveEventListener.onMoveEvent(rtMoveStartEvent)

            // Verify that runtime movable component has had plane pose set with null pose
            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            assertThat(rtMovableComponent.setPlanePoseForMoveUpdatePoseCallCount).isEqualTo(1)
            assertThat(rtMovableComponent.lastPlanePose).isNull()
        }
    }
}
