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

@file:Suppress("DEPRECATION", "KotlinRunTestResultUnused")

package androidx.xr.scenecore.testrule

import android.os.SystemClock
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.AnchorPlacement
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeScenePose
import androidx.xr.scenecore.testing.MovableComponentTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class TestRuleMovableComponentTest {

    internal class FakeEntityMoveListener : EntityMoveListener {
        var onMoveStartedCount: Int = 0
            private set

        var onMoveUpdatedCount: Int = 0
            private set

        var onMoveEndedCount: Int = 0
            private set

        internal var entity: Entity? = null
        internal var initialInputRay: Ray? = null
        internal var initialPose: Pose? = null
        internal var initialScale: Float? = null
        internal var initialParent: Entity? = null
        internal var currentInputRay: Ray? = null
        internal var currentPose: Pose? = null
        internal var currentScale: Float? = null
        internal var finalInputRay: Ray? = null
        internal var finalPose: Pose? = null
        internal var finalScale: Float? = null
        internal var updatedParent: Entity? = null

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

    @Rule @JvmField val sceneCoreTestRule = SceneCoreTestRule()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    @Suppress("DEPRECATION")
    private lateinit var fakePerceptionManager: androidx.xr.arcore.testing.FakePerceptionManager
    private lateinit var testDispatcher: TestDispatcher
    private val currentTimeMillis: Long = 1000000000L
    private var anchorSpaceToDispose: AnchorSpace? = null

    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    private fun createSession() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val result = runBlocking { Session.create(activity, testDispatcher) }
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val fakeRuntime =
            session.runtimes
                .filterIsInstance<androidx.xr.arcore.testing.FakePerceptionRuntime>()
                .first()
        fakePerceptionManager = fakeRuntime.perceptionManager
        SystemClock.setCurrentTimeMillis(currentTimeMillis)
    }

    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes

    @After
    fun tearDown() {
        anchorSpaceToDispose?.disposeInternal()
        anchorSpaceToDispose = null
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun addMovableComponent_addsRuntimeMovableComponent() {
        createSession()
        val entity = Entity.create(session, "test")
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
        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)

        assertThat(tester).isNotNull()
        assertThat(tester.isSystemMovable).isFalse()
        assertThat(tester.isScaleInZ).isFalse()
        assertThat(tester.isAnchorable).isFalse()
    }

    @Test
    fun addAutoMovableComponent_addsRuntimeMovableComponent() {
        createSession()
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)

        assertThat(tester).isNotNull()
        assertThat(tester.isSystemMovable).isTrue()
        assertThat(tester.isScaleInZ).isFalse()
        assertThat(tester.isAnchorable).isFalse()
    }

    @Test
    fun addMovableAnchorableComponent_addsRuntimeMovableComponent() {
        createSession()
        val entity = Entity.create(session, "test")
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
        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)

        assertThat(tester).isNotNull()
        assertThat(tester.isSystemMovable).isTrue()
        assertThat(tester.isScaleInZ).isFalse()
        assertThat(tester.isAnchorable).isTrue()
    }

    @Test
    fun createAnchorableWithEmptySet_throwsException() {
        createSession()
        assertFailsWith<IllegalArgumentException> {
            MovableComponent.createAnchorable(
                session = session,
                anchorPlacement = emptySet(),
                disposeParentOnReAnchor = false,
            )
        }
    }

    @Test
    fun addMovableComponentToAnchorSpace_returnsFalse() {
        createSession()
        val anchorEntity =
            AnchorSpace.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)
        assertThat(anchorEntity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(anchorEntity.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun addMovableComponentToActivitySpace_returnsFalse() {
        createSession()
        val activitySpace = session.scene.activitySpace
        assertThat(activitySpace).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(activitySpace.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun addMovableComponentDefaultArguments_addsRuntimeMovableComponentWithDefaults() {
        createSession()
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)

        assertThat(tester).isNotNull()
        assertThat(tester.isSystemMovable).isTrue()
        assertThat(tester.isScaleInZ).isTrue()
        assertThat(tester.isAnchorable).isFalse()
    }

    @Test
    fun removeMovableComponent_removesRuntimeMovableComponent() {
        createSession()
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity.getComponents()).contains(movableComponent)

        entity.removeComponent(movableComponent)
        assertThat(entity.getComponents()).doesNotContain(movableComponent)
    }

    @Test
    fun movableComponent_canAttachOnlyOnce() {
        createSession()
        val entity = Entity.create(session, "test")
        val entity2 = Entity.create(session, "test")
        assertThat(entity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity2.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun movableComponent_setSizeInvokesRuntimeMovableComponentSetSize() {
        createSession()
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        val testSize = FloatSize3d(2f, 2f, 0f)
        movableComponent.size = testSize

        assertThat(movableComponent.size).isEqualTo(testSize)
    }

    @Test
    fun movableComponent_addMoveListenerInvokesRuntimeMovableComponentAddMoveEventListener() {
        createSession()
        val entity = Entity.create(session, "test")

        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveListener = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener)

        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
        assertThat(tester).isNotNull()

        val startRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val initialPose = Pose()
        val initialScale = 1.0f
        val initialScaleVec = Vector3.One * initialScale
        val initialParent = session.scene.activitySpace

        // Simulates a move start event from runtime.
        tester.triggerOnMoveStart(startRay, initialPose, initialScaleVec, initialParent)
        ShadowLooper.idleMainLooper()

        assertThat(moveListener.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener.entity).isEqualTo(entity)
        assertThat(moveListener.initialInputRay).isEqualTo(startRay)
        assertThat(moveListener.initialPose).isEqualTo(initialPose)
        assertThat(moveListener.initialScale).isEqualTo(initialScale)
        assertThat(moveListener.initialParent).isEqualTo(initialParent)

        val ongoingRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f))
        val currentPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
        val currentScale = 2.0f
        val currentScaleVec = Vector3.One * currentScale

        // Simulates a move ongoing event from runtime.
        tester.triggerOnMoveUpdate(ongoingRay, currentPose, currentScaleVec)
        ShadowLooper.idleMainLooper()

        assertThat(moveListener.onMoveUpdatedCount).isEqualTo(1)
        assertThat(moveListener.currentInputRay).isEqualTo(ongoingRay)
        assertThat(moveListener.currentPose).isEqualTo(currentPose)
        assertThat(moveListener.currentScale).isEqualTo(currentScale)

        val entity2 = Entity.create(session, "test")
        val finalRay = Ray(Vector3(2f, 2f, 2f), Vector3(3f, 3f, 3f))
        val finalPose = Pose(Vector3(2f, 2f, 2f), Quaternion.Identity)
        val finalScale = 3.0f
        val finalScaleVec = Vector3.One * finalScale

        // Simulates a move end event from runtime.
        tester.triggerOnMoveEnd(finalRay, finalPose, finalScaleVec, entity2)
        ShadowLooper.idleMainLooper()

        assertThat(moveListener.onMoveEndedCount).isEqualTo(1)
        assertThat(moveListener.finalInputRay).isEqualTo(finalRay)
        assertThat(moveListener.finalPose).isEqualTo(finalPose)
        assertThat(moveListener.finalScale).isEqualTo(finalScale)
        assertThat(moveListener.updatedParent).isEqualTo(entity2)
    }

    @Test
    fun movableComponent_addMultipleMoveEventListenersInvokesAllListeners() {
        createSession()
        val entity = Entity.create(session, "test")

        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveListener1 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener1)
        val moveListener2 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener2)

        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
        assertThat(tester).isNotNull()

        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val pose = Pose()
        val scale = Vector3(1f, 1f, 1f)
        val initialParent = session.scene.activitySpace

        // Simulates a move start event from runtime.
        tester.triggerOnMoveStart(ray, pose, scale, initialParent)
        ShadowLooper.idleMainLooper()

        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(1)
    }

    @Test
    fun movableComponent_removeMoveEventListenerInvokesRuntimeRemoveMoveEventListener() {
        createSession()
        val entity = Entity.create(session, "test")

        assertThat(entity).isNotNull()

        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()

        val moveListener1 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener1)
        val moveListener2 = FakeEntityMoveListener()
        movableComponent.addMoveListener(directExecutor(), moveListener2)

        val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
        assertThat(tester).isNotNull()

        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val pose = Pose()
        val scale = Vector3(1f, 1f, 1f)
        val initialParent = session.scene.activitySpace

        // Simulates a move start event from runtime.
        tester.triggerOnMoveStart(ray, pose, scale, initialParent)
        ShadowLooper.idleMainLooper()

        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(1)

        movableComponent.removeMoveListener(moveListener1)

        // Must end and restart to trigger again due to stateful tester
        tester.triggerOnMoveUpdate(ray, pose, scale)
        tester.triggerOnMoveEnd(ray, pose, scale, null)

        // Simulates another move start event from runtime.
        tester.triggerOnMoveStart(ray, pose, scale, initialParent)
        ShadowLooper.idleMainLooper()

        // The first listener, which we removed, should not be called again.
        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(2)

        movableComponent.removeMoveListener(moveListener2)

        tester.triggerOnMoveUpdate(ray, pose, scale)
        tester.triggerOnMoveEnd(ray, pose, scale, null)

        // Simulates another move start event from runtime.
        tester.triggerOnMoveStart(ray, pose, scale, initialParent)
        ShadowLooper.idleMainLooper()

        // The listeners, now both removed, should have the same invocation counts.
        assertThat(moveListener1.onMoveStartedCount).isEqualTo(1)
        assertThat(moveListener2.onMoveStartedCount).isEqualTo(2)
    }

    @Test
    fun movableComponent_canAttachAgainAfterDetach() {
        createSession()
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        entity.removeComponent(movableComponent)
        assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun createMovableComponent_callsRuntimeCreateMovableComponent() {
        createSession()

        val movableComponent = MovableComponent.createSystemMovable(session)
        val view = TextView(activity)
        val panelEntity =
            PanelEntity.create(
                session,
                view,
                IntSize2d(720, 480),
                "test",
                parent = session.scene.activitySpace,
            )
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        assertThat(sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent))
            .isNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun customMovableComponent_invokesInitialListener() {
        createSession()
        runTest(testDispatcher) {
            val entity = Entity.create(session, "test")
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

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
            val pose = Pose()
            val scale = Vector3(1f, 1f, 1f)
            val initialParent = session.scene.activitySpace

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, pose, scale, initialParent)
            advanceUntilIdle()

            assertThat(moveListener.onMoveStartedCount).isEqualTo(1)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_updatesThePoseBasedOnPlanes() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 3 units because the activity space is 1 unit below the origin and the
            // plane is 2 units above it.
            val expectedPose = Pose(Vector3(1f, 3f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_nullParent_updatesThePoseBasedOnPlanes() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            panelEntity.parent = null
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 3 units because the activity space is 1 unit below the origin and the
            // plane is 2 units above it.
            val expectedPose = Pose(Vector3(1f, 3f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_withNonActivityParent_updatesPoseBasedOnPlanesAndParent() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val entityPose = Pose(Vector3(0f, -1f, 0f), Quaternion.Identity)
            val entity = Entity.create(session, "test", entityPose)
            (entity.rtScenePose as FakeScenePose).activitySpacePose = entityPose
            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            panelEntity.parent = entity
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, entity)
            ShadowLooper.idleMainLooper()

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 3 units because the activity space is 1 unit below the origin and the
            // plane is 2 units above it. Since the parent is 1 unit below the activity space, the
            // expected position should be 4 units above the parent.
            val expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_scaledParent_updatesThePoseBasedOnPlanes() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpaceScale = Vector3(2f, 2f, 2f)
            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.activitySpace.rtScenePose as FakeScenePose).activitySpaceScale =
                activitySpaceScale
            (session.scene.activitySpace.rtEntity as FakeActivitySpace).setScale(activitySpaceScale)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(.5f, .5f, .5f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()

            // The expected position should be 3 unit above the activity in order to rest on the
            // plane. It is 1.5 units because the activity space is 1 unit below the origin and
            // the plane is 2 units above it and the activity space is scaled by 2.
            val expectedPose = Pose(Vector3(.5f, 1.5f, .5f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_withinAnchorDistance_setsAnchorSpace() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            val expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)
            anchorSpaceToDispose = panelEntity.parent as AnchorSpace
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_withinAnchorDistanceAboveAnchor_anchorsEntityToPlane() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose =
                Pose(
                    Vector3(1f, 3f + MovableComponent.MAX_PLANE_ANCHOR_DISTANCE / 2f, 1f),
                    Quaternion.Identity,
                )
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            val expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)
            anchorSpaceToDispose = panelEntity.parent as AnchorSpace
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_withIncorrectPlaneType_doesNotCreateAnchor() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val anchorPlacement =
                AnchorPlacement.createForPlanes(
                    setOf(PlaneOrientation.VERTICAL),
                    setOf(PlaneSemanticType.WALL),
                )
            val movableComponent =
                MovableComponent.createAnchorable(session, setOf(anchorPlacement))
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // The expected position should be the proposed position from the reform event because
            // no suitable planes can be found.
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(ActivitySpace::class.java)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_withinAnchorDistanceAndScale_setsAnchorSpaceAndScales() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpaceScale = Vector3(2f, 2f, 2f)
            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.activitySpace.rtScenePose as FakeScenePose).activitySpaceScale =
                activitySpaceScale
            (session.scene.activitySpace.rtEntity as FakeActivitySpace).setScale(activitySpaceScale)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            val entityScale = Vector3.One * 5f
            panelEntity.setScale(entityScale)
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(.5f, .5f, .5f), Quaternion.Identity)
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            val expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.getScale())
                .isWithin(1e-5f)
                .of(activitySpaceScale.x * entityScale.x)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)
            anchorSpaceToDispose = panelEntity.parent as AnchorSpace
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createAnchorable_noPlanes_keepsProposedPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()

            // The expected position should be unchanged from the proposed event
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_validPlaneButNotTracking_keepsProposedPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.PAUSED,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()

            // The expected position should be unchanged from the proposed event
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_outsideExtents_keepsProposedPose() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(5f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()

            // The expected position should be unchanged from the proposed event
            val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_resetsToScenePoseAfterAnchoring() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)

            // Re-start a move to pull away from the anchor.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor, and it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_resetsAndScaleToScenePoseAfterAnchoring() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpaceScale = Vector3(2f, 2f, 2f)
            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.activitySpace.rtScenePose as FakeScenePose).activitySpaceScale =
                activitySpaceScale
            (session.scene.activitySpace.rtEntity as FakeActivitySpace).setScale(activitySpaceScale)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            val entityScale = Vector3.One * 5f
            panelEntity.setScale(entityScale)
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            var proposedPose = Pose(Vector3(.5f, .5f, .5f), Quaternion.Identity)
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)
            assertThat(panelEntity.getScale())
                .isWithin(1e-5f)
                .of(activitySpaceScale.x * entityScale.x)

            proposedPose = Pose(Vector3(2f, 8f, 2f), Quaternion.Identity)

            val entity = Entity.create(session, "test")

            // Re-start a move to pull away from the anchor.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, entity)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving to (2, 8, 2) relative to the anchor entity. This translates to (1, 4, 1)
            // relative to the activity space. This should pull the entity away from the anchor, and
            // it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)
            assertThat(panelEntity.getScale()).isWithin(1e-5f).of(entityScale.x)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_childOfEntity_resetsToActivityPoseAfterAnchoring() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            // Create a parent entity whose pose is below the activity space pose.
            val parentPose = Pose(Vector3(0f, -1f, 0f), Quaternion.Identity)
            val parentEntity: Entity = Entity.create(session, "test", parentPose)
            panelEntity.parent = parentEntity

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)

            // Re-start a move to pull away from the anchor.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor, and it should be reparented to the activity space not the original
            // parent.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_shouldDispose_disposesAnchorAfterUnparenting() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)

            // Re-start a move to pull away from the anchor.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor, and it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)

            // Verify that the anchor entity was disposed by checking that it is no longer in the
            // entity manager.
            assertThat(session.scene.entityRegistry.getEntitiesOfType(AnchorSpace::class.java).size)
                .isEqualTo(0)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_shouldDispose_doesNotDisposeIfAnchorHasChildren() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)
            val view = TextView(activity)

            val panelEntity =
                PanelEntity.create(
                    session,
                    view,
                    IntSize2d(720, 480),
                    "test",
                    parent = session.scene.activitySpace,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()

            var proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * panelEntity.getScale()
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Simulates a move start event from runtime.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor
            // which is (1, 3, 0) relative to the activity space, which results in an updated pose
            // of (0, 0, 1) relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion
            // represents a 90-degree rotation around the x-axis. Which is expected when the panel
            // is rotated into the plane's reference space.

            var expectedPose = Pose(Vector3(0f, 0f, 1f), Quaternion(-0.707f, 0f, 0f, 0.707f))
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isInstanceOf(AnchorSpace::class.java)

            // Cache anchor entity and give it a child
            val anchorEntity = panelEntity.parent
            val childEntity = Entity.create(session, "test", Pose.Identity)
            childEntity.parent = anchorEntity

            proposedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)

            // Re-start a move to pull away from the anchor.
            tester.triggerOnMoveStart(ray, proposedPose, entityScale, session.scene.activitySpace)
            ShadowLooper.idleMainLooper()
            // Simulates a move update event from runtime.
            tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
            ShadowLooper.idleMainLooper()
            // Simulates a move end event from runtime.
            tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
            ShadowLooper.idleMainLooper()

            // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away
            // from the anchor, and it should be reparented to the activity space.
            expectedPose = Pose(Vector3(1f, 4f, 1f), Quaternion.Identity)
            assertPose(panelEntity.getPose(), expectedPose)
            assertThat(panelEntity.parent).isEqualTo(session.scene.activitySpace)

            // Verify that the anchor entity has not been disposed by checking that it is still in
            // the entity manager.
            assertThat(session.scene.entityRegistry.getEntitiesOfType(AnchorSpace::class.java).size)
                .isEqualTo(1)
            anchorSpaceToDispose =
                session.scene.entityRegistry.getEntitiesOfType(AnchorSpace::class.java).first()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: b/494308962 Remove references to arcore-testing Fakes
    fun createAnchorable_unsupportedEntityType_throwsIllegalArgumentException() {
        createSession()
        runTest(testDispatcher) {
            activityController.create().start().resume()

            val activitySpacePose = Pose(Vector3(-1f, -1f, 0f), Quaternion.Identity)
            session.scene.activitySpace.rtEntity.setPose(activitySpacePose)
            (session.scene.perceptionSpace.rtScenePose as FakeScenePose).activitySpacePose =
                activitySpacePose.inverse

            val planeCenterPose = Pose(Vector3(0f, 2f, 0f), Quaternion.Identity)
            val plane =
                androidx.xr.arcore.testing.FakeRuntimePlane(
                    trackingState = TrackingState.TRACKING,
                    type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                    label = Plane.Label.FLOOR,
                    extents = FloatSize2d(5.0f, 5.0f),
                    centerPose = planeCenterPose,
                )
            fakePerceptionManager.addTrackable(plane)
            advanceUntilIdle()

            val movableComponent = MovableComponent.createAnchorable(session)

            // Create a mock Entity that is not a PanelEntity, GltfModelEntity, or MeshEntity.
            val mockEntity = Entity.create(session, "test")
            assertThat(mockEntity.addComponent(movableComponent)).isTrue()

            val proposedPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
            val entityScale = Vector3.One * mockEntity.getScale()
            val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))

            val tester = sceneCoreTestRule.createTester<MovableComponentTester>(movableComponent)
            assertThat(tester).isNotNull()

            // Expect an IllegalArgumentException because mockEntity is not a supported type.
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    // Must follow sequence: Start -> Update -> End
                    tester.triggerOnMoveStart(
                        ray,
                        proposedPose,
                        entityScale,
                        session.scene.activitySpace,
                    )
                    tester.triggerOnMoveUpdate(ray, proposedPose, entityScale)
                    tester.triggerOnMoveEnd(ray, proposedPose, entityScale, null)
                }

            assertThat(exception.message)
                .isEqualTo(
                    "Movable component can be applied to either a PanelEntity, GltfModelEntity, or MeshEntity"
                )
        }
    }
}
