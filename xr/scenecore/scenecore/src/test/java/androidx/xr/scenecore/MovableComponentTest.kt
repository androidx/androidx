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

import android.os.SystemClock
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakeRuntimePlane
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
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
import androidx.xr.scenecore.runtime.MoveEvent as RtMoveEvent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeMovableComponent
import androidx.xr.scenecore.testing.FakeScenePose
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.TestTimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

internal class FakeEntityMoveListener : EntityMoveListener {
    var onMoveStartedCount: Int = 0
        private set

    var onMoveUpdatedCount: Int = 0
        private set

    var onMoveEndedCount: Int = 0
        private set

    private var entity: Entity? = null
    private var initialInputRay: Ray? = null
    private var initialPose: Pose? = null
    private var initialScale: Float? = null
    private var initialParent: Entity? = null
    private var currentInputRay: Ray? = null
    private var currentPose: Pose? = null
    private var currentScale: Float? = null
    private var finalInputRay: Ray? = null
    private var finalPose: Pose? = null
    private var finalScale: Float? = null
    private var updatedParent: Entity? = null

    fun stateMatchesEvent(entity: Entity, event: MoveEvent): Boolean {
        when (event.moveState) {
            MoveEvent.MOVE_STATE_START ->
                return this.entity == entity &&
                    initialInputRay == event.initialInputRay &&
                    initialPose == event.previousPose &&
                    initialScale == event.previousScale &&
                    initialParent == event.initialParent
            MoveEvent.MOVE_STATE_ONGOING ->
                return this.entity == entity &&
                    currentInputRay == event.currentInputRay &&
                    currentPose == event.currentPose &&
                    currentScale == event.currentScale
            MoveEvent.MOVE_STATE_END ->
                return this.entity == entity &&
                    finalInputRay == event.currentInputRay &&
                    finalPose == event.currentPose &&
                    finalScale == event.currentScale &&
                    updatedParent == event.initialParent
        }
        return false
    }

    override fun onMoveStart(
        entity: Entity,
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Float,
        initialParent: Entity,
    ) {
        onMoveStartedCount++

        this.entity = entity
        this.initialInputRay = initialInputRay
        this.initialPose = initialPose
        this.initialScale = initialScale
        this.initialParent = initialParent
    }

    override fun onMoveUpdate(
        entity: Entity,
        currentInputRay: Ray,
        currentPose: Pose,
        currentScale: Float,
    ) {
        onMoveUpdatedCount++

        this.currentInputRay = currentInputRay
        this.currentPose = currentPose
        this.currentScale = currentScale
    }

    override fun onMoveEnd(
        entity: Entity,
        finalInputRay: Ray,
        finalPose: Pose,
        finalScale: Float,
        updatedParent: Entity?,
    ) {
        onMoveEndedCount++

        this.finalInputRay = finalInputRay
        this.finalPose = finalPose
        this.finalScale = finalScale
        this.updatedParent = updatedParent
    }
}

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MovableComponentTest {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var session: Session
    private lateinit var mFakeRuntime: FakePerceptionRuntime
    private lateinit var mFakeLifecycleManager: FakeLifecycleManager
    private lateinit var mFakePerceptionManager: FakePerceptionManager
    private lateinit var fakeActivitySpace: RtActivitySpace
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var timeSource: TestTimeSource
    private var mCurrentTimeMillis: Long = 1000000000L
    private var anchorEntityToDispose: AnchorEntity? = null

    private fun createSession() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val result = Session.create(activity, testDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        mFakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
        mFakeLifecycleManager = mFakeRuntime.lifecycleManager
        mFakePerceptionManager = mFakeRuntime.perceptionManager
        sceneRuntime = session.sceneRuntime
        timeSource = mFakeLifecycleManager.timeSource
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis)
    }

    private fun createCustomSession() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val result = Session.create(activity, testDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
        fakeActivitySpace = sceneRuntime.activitySpace
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
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

        val moveListener = FakeEntityMoveListener()
        val movableComponent =
            MovableComponent.createCustomMovable(
                session = session,
                scaleInZ = false,
                directExecutor(),
                moveListener,
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(movableComponent.rtMovableComponent)
            .isInstanceOf(FakeMovableComponent::class.java)

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        assertThat(rtMovableComponent.systemMovable).isFalse()
        assertThat(rtMovableComponent.scaleInZ).isFalse()
        assertThat(rtMovableComponent.userAnchorable).isFalse()
    }

    @Test
    fun addAutoMovableComponent_addsRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(movableComponent.rtMovableComponent)
            .isInstanceOf(FakeMovableComponent::class.java)

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        assertThat(rtMovableComponent.systemMovable).isTrue()
        assertThat(rtMovableComponent.scaleInZ).isFalse()
        assertThat(rtMovableComponent.userAnchorable).isFalse()
    }

    @Test
    fun addMovableAnchorableComponent_addsRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

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
        assertThat(movableComponent.rtMovableComponent)
            .isInstanceOf(FakeMovableComponent::class.java)

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        assertThat(rtMovableComponent.systemMovable).isTrue()
        assertThat(rtMovableComponent.scaleInZ).isFalse()
        assertThat(rtMovableComponent.userAnchorable).isTrue()
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

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(movableComponent.rtMovableComponent)
            .isInstanceOf(FakeMovableComponent::class.java)

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        assertThat(rtMovableComponent.systemMovable).isTrue()
        assertThat(rtMovableComponent.scaleInZ).isTrue()
        assertThat(rtMovableComponent.userAnchorable).isFalse()
    }

    @Test
    fun removeMovableComponent_removesRuntimeMovableComponent() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity.rtEntity?.getComponents()).hasSize(1)

        entity.removeComponent(movableComponent)
        assertThat(entity.rtEntity?.getComponents()).hasSize(0)
    }

    @Test
    fun movableComponent_canAttachOnlyOnce() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        val entity2 = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity2.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun movableComponent_setSizeInvokesRuntimeMovableComponentSetSize() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        val testSize = FloatSize3d(2f, 2f, 0f)
        movableComponent.size = testSize

        assertThat(movableComponent.size).isEqualTo(testSize)
        assertThat(movableComponent.rtMovableComponent)
            .isInstanceOf(FakeMovableComponent::class.java)

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        assertThat(rtMovableComponent.size).isEqualTo(testSize.toRtDimensions())
    }

    @Test
    fun movableComponent_addMoveListenerInvokesRuntimeMovableComponentAddMoveEventListener() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")

        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveListener = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener)

        var rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                fakeActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        // Simulates a move start event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)
        // Expects to receive a scenecore event.
        var expectedEvent = rtMoveEvent.toMoveEvent(session.scene.entityManager)

        assertThat(moveListener.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener.stateMatchesEvent(entity, expectedEvent)).isTrue()

        rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                fakeActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        // Simulates a move ongoing event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)
        // Expects to receive a scenecore event.
        expectedEvent = rtMoveEvent.toMoveEvent(session.scene.entityManager)

        assertThat(moveListener.onMoveUpdatedCount).isEqualTo(1)
        assertThat(moveListener.stateMatchesEvent(entity, expectedEvent)).isTrue()

        val fakeAnchorEntity = sceneRuntime.createAnchorEntity()
        rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_END,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                fakeActivitySpace,
                fakeAnchorEntity,
                disposedEntity = null,
            )

        // Simulates a move end event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)
        // Expects to receive a scenecore event.
        expectedEvent = rtMoveEvent.toMoveEvent(session.scene.entityManager)

        assertThat(moveListener.onMoveEndedCount).isEqualTo(1)
        assertThat(moveListener.stateMatchesEvent(entity, expectedEvent)).isTrue()
    }

    @Test
    fun movableComponent_addMultipleMoveEventListenersInvokesAllListeners() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")

        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveListener1 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener1)
        val moveListener2 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener2)

        val rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                fakeActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        // Simulates a move start event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)
        // Expects to receive a scenecore event.
        val expectedEvent = rtMoveEvent.toMoveEvent(session.scene.entityManager)

        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener1.stateMatchesEvent(entity, expectedEvent)).isTrue()
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.stateMatchesEvent(entity, expectedEvent)).isTrue()
    }

    @Test
    fun movableComponent_removeMoveEventListenerInvokesRuntimeRemoveMoveEventListener() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")

        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveListener1 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener1)
        val moveListener2 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener2)

        val rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                fakeActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
        // Simulates a move start event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)
        // Expects to receive a scenecore event.
        val expectedEvent = rtMoveEvent.toMoveEvent(session.scene.entityManager)

        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener1.stateMatchesEvent(entity, expectedEvent)).isTrue()
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.stateMatchesEvent(entity, expectedEvent)).isTrue()

        movableComponent.removeMoveListener(moveListener1)
        // Simulates a move start event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)

        // The first listener, which we removed, should not be called again.
        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(2)
        assertThat(moveListener2.stateMatchesEvent(entity, expectedEvent)).isTrue()

        movableComponent.removeMoveListener(moveListener2)
        // Simulates a move start event from runtime.
        rtMovableComponent.onMoveEvent(rtMoveEvent)

        // The listeners, now both removed, should have the same invocation counts.
        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(2)
    }

    @Test
    fun movablecomponent_canAttachAgainAfterDetach() {
        createCustomSession()
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        entity.removeComponent(movableComponent)
        assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun createMovableComponent_callsRuntimeCreateMovableComponent() {
        createCustomSession()

        val movableComponent = MovableComponent.createSystemMovable(session)
        val view = TextView(activity)
        val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        assertThat(movableComponent.rtMovableComponent)
            .isInstanceOf(FakeMovableComponent::class.java)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun customMovableComponent_invokesInitialListener() {
        createCustomSession()
        runTest(testDispatcher) {
            val entity = GroupEntity.create(session, "test")
            assertThat(entity).isNotNull()
            val moveListener = FakeEntityMoveListener()
            val movableComponent =
                MovableComponent.createCustomMovable(
                    session,
                    true,
                    testDispatcher.asExecutor(),
                    moveListener,
                )
            assertThat(entity.addComponent(movableComponent)).isTrue()

            val rtMoveEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_START,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    Pose(),
                    Pose(),
                    Vector3(1f, 1f, 1f),
                    Vector3(1f, 1f, 1f),
                    fakeActivitySpace,
                    updatedParent = null,
                    disposedEntity = null,
                )

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEvent)
            // Expects to receive a scenecore event.
            val moveEvent = rtMoveEvent.toMoveEvent(session.scene.entityManager)
            advanceUntilIdle()

            assertThat(moveListener.onMoveStartedCount).isEqualTo(1)
            assertThat(moveListener.stateMatchesEvent(entity, moveEvent)).isTrue()
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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)
            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            // Simulates a move end event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveEndEvent)

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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

            // Verify that runtime movable component has had plane pose set with non-null pose
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

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent
            // Simulates a move start event from runtime.
            rtMovableComponent.onMoveEvent(rtMoveStartEvent)

            // Verify that runtime movable component has had plane pose set with null pose
            assertThat(rtMovableComponent.setPlanePoseForMoveUpdatePoseCallCount).isEqualTo(1)
            assertThat(rtMovableComponent.lastPlanePose).isNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_unsupportedEntityType_throwsIllegalArgumentException() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            assertNotNull(session.scene.activitySpace.rtEntity).setPose(activitySpacePose)
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

            // Create a mock Entity that is not PanelEntity or GltfModelEntity
            val mockEntity = GroupEntity.create(session, "test")
            assertTrue(mockEntity.addComponent(movableComponent))

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * mockEntity.getScale()

            val rtMoveEndEvent =
                RtMoveEvent(
                    MoveEvent.MOVE_STATE_END,
                    Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                    Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                    proposedPose,
                    proposedPose,
                    entityScale,
                    entityScale,
                    checkNotNull(session.scene.activitySpace.rtEntity),
                    updatedParent = null,
                    disposedEntity = null,
                )

            val rtMovableComponent = movableComponent.rtMovableComponent as FakeMovableComponent

            // Expect an IllegalArgumentException because mockEntity is not a supported type.
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    rtMovableComponent.onMoveEvent(rtMoveEndEvent)
                }

            assertThat(exception.message)
                .isEqualTo(
                    "Movable component can be applied to either a PanelEntity or GltfModelEntity"
                )
        }
    }
}
