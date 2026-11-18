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

package androidx.xr.scenecore.integration.tests

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.DEFAULT_MOVE_HANDLE_MARGIN
import androidx.xr.testutils.SnapshotXrSpace
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Device integration tests for [SurfaceEntity] interaction and spatial reform (CUJ R31:
 * b/573225293, b/583439191, b/581430543).
 *
 * Verified runtime behavior notes on `androidx-main`:
 * - [SurfaceEntity.create] enforces `Looper.getMainLooper().isCurrentThread` in
 *   `SpatialRenderingRuntime`, and `shape`, `stereoMode`, `edgeFeatheringParams`,
 *   `setSurfacePixelDimensions`, and `getSurface` are `@MainThread` SplitEngine calls, so only
 *   those calls run inside `sessionRule.onMainThread`. Pose, alpha, enabled state, component
 *   attach/detach, and assertions run on the test thread.
 * - [SurfaceEntity.create] defaults `parent` to `null`, so entities must be attached to
 *   `session.scene.activitySpace` (or another mounted ancestor) to enter the spatial scene graph.
 * - [ResizableComponent] attaches cleanly to [SurfaceEntity] (`ResizableComponentImpl.onAttach`
 *   configures `ReformOptions.ALLOW_RESIZE` and reads initial dimensions from `Shape.Quad`), and
 *   emits [ResizeEvent] callbacks (`START`/`ONGOING`/`END`), while the caller applies the updated
 *   size to `surfaceEntity.shape` on the main thread.
 * - SplitEngine `SurfaceEntity` CPM nodes (`stereo_surface_panel_entity_subspace_*`) do not emit
 *   `worldHalfExtents`, and Aura SpaceFlinger renders resize highlights in Impress rather than CPM
 *   `resizeAffordanceNodes`, so interaction targeting uses `SnapshotXrSpace.subspaceNodes` and
 *   known shape extents (plus `DEFAULT_MOVE_HANDLE_MARGIN`) relative to `subspace.parent`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SurfaceEntityInteractionAndReformTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun surfaceEntity_shapesStereoModesFeatheringPoseAlphaAndEnabled_updatesState() {
        val session = sessionRule.session
        val initialPose = Pose(Vector3(0.1f, 0.2f, -1.5f), Quaternion.Identity)
        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = initialPose,
                shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.6f, 0.9f)),
                stereoMode = SurfaceEntity.StereoMode.MONO,
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.CYAN)

        val initialSubspace = awaitSingleSurfaceSubspaceNode()
        assertThat(initialSubspace.isVisible).isTrue()
        assertThat(created.dimensions.width).isWithin(FLOAT_TOLERANCE).of(1.6f)
        assertThat(created.dimensions.height).isWithin(FLOAT_TOLERANCE).of(0.9f)
        assertThat(created.dimensions.depth).isWithin(FLOAT_TOLERANCE).of(0.0f)

        // Transition Quad -> Sphere and verify dimensions + active Surface frame submission.
        sessionRule.onMainThread { created.shape = SurfaceEntity.Shape.Sphere(2.0f) }
        assertThat(created.shape).isInstanceOf(SurfaceEntity.Shape.Sphere::class.java)
        assertThat(created.dimensions.width).isWithin(FLOAT_TOLERANCE).of(4.0f)
        assertThat(created.dimensions.height).isWithin(FLOAT_TOLERANCE).of(4.0f)
        assertThat(created.dimensions.depth).isWithin(FLOAT_TOLERANCE).of(4.0f)
        created.renderSolidColorFrame(Color.GREEN)

        // Transition Sphere -> Hemisphere + SIDE_BY_SIDE and verify dimensions + Surface frame.
        sessionRule.onMainThread {
            created.shape = SurfaceEntity.Shape.Hemisphere(1.5f)
            created.stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE
        }
        assertThat(created.shape).isInstanceOf(SurfaceEntity.Shape.Hemisphere::class.java)
        assertThat(created.dimensions.width).isWithin(FLOAT_TOLERANCE).of(3.0f)
        assertThat(created.dimensions.height).isWithin(FLOAT_TOLERANCE).of(3.0f)
        assertThat(created.dimensions.depth).isWithin(FLOAT_TOLERANCE).of(1.5f)
        created.renderSolidColorFrame(Color.MAGENTA)

        // Apply TOP_BOTTOM + RectangleFeather and render another frame while subspace stays
        // visible.
        val featherParams = SurfaceEntity.EdgeFeatheringParams.RectangleFeather(0.15f, 0.25f)
        sessionRule.onMainThread {
            created.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
            created.edgeFeatheringParams = featherParams
        }
        created.renderSolidColorFrame(Color.BLUE)
        assertThat(awaitSingleSurfaceSubspaceNode().isVisible).isTrue()

        val updatedPose = Pose(Vector3(-0.3f, 0.4f, -2.0f), Quaternion.fromEulerAngles(0f, 15f, 0f))
        created.setPose(updatedPose, Space.PARENT)
        created.setAlpha(0.5f)
        assertPose(created.getPose(Space.PARENT), updatedPose, FLOAT_TOLERANCE)
        assertThat(created.getAlpha()).isWithin(FLOAT_TOLERANCE).of(0.5f)

        val updatedTask =
            checkNotNull(
                waitForSpatialScene(
                    description = "SurfaceEntity CPM subspace node pose and alpha settled",
                    timeout = SCENE_TIMEOUT,
                    select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
                    until = { task ->
                        val activityNode = task?.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                        val subspace =
                            task?.subspaceNodes?.firstOrNull {
                                it.name.startsWith(SURFACE_SUBSPACE_PREFIX) && it.isVisible
                            }
                        if (activityNode == null || subspace == null) {
                            false
                        } else {
                            val localPos = activityNode.transformPointFromWorld(subspace.position)
                            val computedAlpha = readSubspaceAlpha(subspace)
                            (localPos - updatedPose.translation).length < CPM_POSE_TOLERANCE &&
                                computedAlpha != null &&
                                abs(computedAlpha - 0.5f) <= CPM_ALPHA_TOLERANCE
                        }
                    },
                )
            )
        val settledSubspace =
            updatedTask.subspaceNodes.first {
                it.name.startsWith(SURFACE_SUBSPACE_PREFIX) && it.isVisible
            }
        assertThat(readSubspaceAlpha(settledSubspace)).isWithin(CPM_ALPHA_TOLERANCE).of(0.5f)

        created.setEnabled(false)
        assertThat(created.isEnabled(includeParents = false)).isFalse()
        awaitSurfaceSubspaceCount(expectedCount = 0)

        created.setEnabled(true)
        assertThat(created.isEnabled(includeParents = false)).isTrue()
        val reenabledSubspace = awaitSingleSurfaceSubspaceNode()
        assertThat(reenabledSubspace.isVisible).isTrue()
    }

    @Test
    fun surfaceEntity_quad_interactableReceivesHoverAndClickEvents() {
        val session = sessionRule.session
        val events = Collections.synchronizedList(mutableListOf<InputEvent>())
        val exitLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = Pose(Vector3(0f, 0f, -1.5f)),
                shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.4f, 1.0f)),
                stereoMode = SurfaceEntity.StereoMode.MONO,
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.BLUE)

        val interactable =
            InteractableComponent.create(session = session) { event ->
                events.add(event)
                if (event.action == InputEvent.Action.HOVER_EXIT) {
                    exitLatch.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val interactableAdded = sessionRule.onMainThread { created.addComponent(interactable) }
        assertThat(interactableAdded).isTrue()

        val subspaceNode = awaitSingleSurfaceSubspaceNode()
        subspaceNode.performSpatialInteraction {
            pointAt(offset = Vector3.Zero)
            press()
            animate(durationMs = 150L) { fraction ->
                pointAt(offset = Vector3.lerp(Vector3.Zero, Vector3(0.25f, 0.1f, 0f), fraction))
            }
            release()
            pointAt(offset = Vector3(3.0f, 3.0f, 0f))
        }

        assertWithMessage("Timed out waiting for HOVER_EXIT on quad SurfaceEntity")
            .that(exitLatch.await(5, TimeUnit.SECONDS))
            .isTrue()

        val recordedActions = synchronized(events) { events.map { it.action } }
        assertThat(recordedActions)
            .containsAtLeast(
                InputEvent.Action.HOVER_ENTER,
                InputEvent.Action.DOWN,
                InputEvent.Action.MOVE,
                InputEvent.Action.UP,
                InputEvent.Action.HOVER_EXIT,
            )
        val downEvent =
            synchronized(events) { events.first { it.action == InputEvent.Action.DOWN } }
        val hitInfo = downEvent.hitInfoList.firstOrNull { it.inputEntity == created }
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo!!.hitPosition?.z).isWithin(0.2f).of(-1.5f)
    }

    @Test
    fun surfaceEntity_quad_disabledAndDetachedSuppressesInput() {
        val session = sessionRule.session
        val frontCount = AtomicInteger(0)
        val sentinelCount = AtomicInteger(0)
        val frontLatch1 = CountDownLatch(1)
        val frontLatch2 = CountDownLatch(1)
        val sentinelLatch1 = CountDownLatch(1)
        val sentinelLatch2 = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val (front, sentinel) =
            sessionRule.onMainThread {
                val f =
                    SurfaceEntity.create(
                        session = session,
                        pose = Pose(Vector3(0f, 0f, -1.2f)),
                        shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.2f, 1.2f)),
                        parent = session.scene.activitySpace,
                    )
                val s =
                    SurfaceEntity.create(
                        session = session,
                        pose = Pose(Vector3(0f, 0f, -1.8f)),
                        shape = SurfaceEntity.Shape.Quad(FloatSize2d(2.2f, 2.2f)),
                        parent = session.scene.activitySpace,
                    )
                f to s
            }
        front.renderSolidColorFrame(Color.GREEN)
        sentinel.renderSolidColorFrame(Color.RED)

        val frontInteractable =
            InteractableComponent.create(session = session) { event ->
                if (event.action == InputEvent.Action.DOWN) {
                    val count = frontCount.incrementAndGet()
                    if (count == 1) frontLatch1.countDown()
                    if (count == 2) frontLatch2.countDown()
                }
            }
        val sentinelInteractable =
            InteractableComponent.create(session = session) { event ->
                if (event.action == InputEvent.Action.DOWN) {
                    val count = sentinelCount.incrementAndGet()
                    if (count == 1) sentinelLatch1.countDown()
                    if (count == 2) sentinelLatch2.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val (frontAdded, sentinelAdded) =
            sessionRule.onMainThread {
                front.addComponent(frontInteractable) to sentinel.addComponent(sentinelInteractable)
            }
        assertThat(frontAdded).isTrue()
        assertThat(sentinelAdded).isTrue()

        val task = awaitSurfaceSubspaceCount(expectedCount = 2)

        // Phase 1: Both enabled -> front quad intercepts ray.
        findActivitySpaceNode(task).performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = Vector3(0f, 0f, -1.2f))
            select()
        }
        assertThat(frontLatch1.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(frontCount.get()).isEqualTo(1)
        assertThat(sentinelCount.get()).isEqualTo(0)

        // Phase 2: Disable front entity -> await CPM visibility update, then ray passes through.
        front.setEnabled(false)
        val taskAfterDisable = awaitSurfaceSubspaceCount(expectedCount = 1)
        findActivitySpaceNode(taskAfterDisable).performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = Vector3(0f, 0f, -1.2f))
            select()
        }
        assertThat(sentinelLatch1.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(frontCount.get()).isEqualTo(1)
        assertThat(sentinelCount.get()).isEqualTo(1)

        // Phase 3: Re-enable front entity -> front quad intercepts ray again.
        front.setEnabled(true)
        val taskAfterReenable = awaitSurfaceSubspaceCount(expectedCount = 2)
        findActivitySpaceNode(taskAfterReenable).performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = Vector3(0f, 0f, -1.2f))
            select()
        }
        assertThat(frontLatch2.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(frontCount.get()).isEqualTo(2)
        assertThat(sentinelCount.get()).isEqualTo(1)

        // Phase 4: Drain main-thread input before/after detaching InteractableComponent.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        sessionRule.onMainThread { front.removeComponent(frontInteractable) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        findActivitySpaceNode(taskAfterReenable).performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = Vector3(0f, 0f, -1.2f))
            select()
        }
        assertThat(sentinelLatch2.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(frontCount.get()).isEqualTo(2)
        assertThat(sentinelCount.get()).isEqualTo(2)
    }

    @Test
    fun surfaceEntity_sphere_interactableReceivesInwardRayInput() {
        val session = sessionRule.session
        val events = Collections.synchronizedList(mutableListOf<InputEvent>())
        val upLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val radius = 1.5f
        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = Pose.Identity,
                shape = SurfaceEntity.Shape.Sphere(radius),
                stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.MAGENTA)

        val interactable =
            InteractableComponent.create(session = session) { event ->
                events.add(event)
                if (event.action == InputEvent.Action.UP) {
                    upLatch.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val interactableAdded = sessionRule.onMainThread { created.addComponent(interactable) }
        assertThat(interactableAdded).isTrue()

        awaitSingleSurfaceSubspaceNode()
        val task = findHostTask()
        val inwardCenter = Vector3(0f, 0f, -radius)
        val inwardEnd = Vector3(0.35f, 0.15f, -radius)
        findActivitySpaceNode(task).performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = inwardCenter)
            press()
            animate(durationMs = 150L) { fraction ->
                pointAt(offset = Vector3.lerp(inwardCenter, inwardEnd, fraction))
            }
            release()
        }

        assertWithMessage("Timed out waiting for UP input event on Sphere SurfaceEntity")
            .that(upLatch.await(5, TimeUnit.SECONDS))
            .isTrue()

        val actions = synchronized(events) { events.map { it.action } }
        assertThat(actions)
            .containsAtLeast(
                InputEvent.Action.HOVER_ENTER,
                InputEvent.Action.DOWN,
                InputEvent.Action.MOVE,
                InputEvent.Action.UP,
            )
        val downEvent =
            synchronized(events) { events.first { it.action == InputEvent.Action.DOWN } }
        val hitInfo = downEvent.hitInfoList.firstOrNull { it.inputEntity == created }
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo!!.hitPosition?.z).isWithin(0.25f).of(-radius)
    }

    @Test
    fun surfaceEntity_hemisphere_interactableReceivesInwardRayAndExit() {
        val session = sessionRule.session
        val events = Collections.synchronizedList(mutableListOf<InputEvent>())
        val exitLatch = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)

        val radius = 1.5f
        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = Pose.Identity,
                shape = SurfaceEntity.Shape.Hemisphere(radius),
                stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.YELLOW)

        val interactable =
            InteractableComponent.create(session = session) { event ->
                events.add(event)
                if (event.action == InputEvent.Action.HOVER_EXIT) {
                    exitLatch.countDown()
                }
            }
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val interactableAdded = sessionRule.onMainThread { created.addComponent(interactable) }
        assertThat(interactableAdded).isTrue()

        awaitSingleSurfaceSubspaceNode()
        val task = findHostTask()
        val inwardCenter = Vector3(0f, 0f, -radius)
        val inwardEnd = Vector3(-0.3f, 0.15f, -radius)
        findActivitySpaceNode(task).performSpatialInteraction {
            moveOriginTo(Vector3.Zero)
            pointAt(offset = inwardCenter)
            press()
            animate(durationMs = 150L) { fraction ->
                pointAt(offset = Vector3.lerp(inwardCenter, inwardEnd, fraction))
            }
            release()
            // Hemisphere spans z <= 0, so pointing into +Z exits the surface collider cleanly.
            pointAt(offset = Vector3(0f, 0f, radius))
        }

        assertWithMessage("Timed out waiting for HOVER_EXIT on Hemisphere SurfaceEntity")
            .that(exitLatch.await(5, TimeUnit.SECONDS))
            .isTrue()

        val actions = synchronized(events) { events.map { it.action } }
        assertThat(actions)
            .containsAtLeast(
                InputEvent.Action.HOVER_ENTER,
                InputEvent.Action.DOWN,
                InputEvent.Action.MOVE,
                InputEvent.Action.UP,
                InputEvent.Action.HOVER_EXIT,
            )
        val downEvent =
            synchronized(events) { events.first { it.action == InputEvent.Action.DOWN } }
        val hitInfo = downEvent.hitInfoList.firstOrNull { it.inputEntity == created }
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo!!.hitPosition?.z).isWithin(0.25f).of(-radius)
    }

    @Test
    fun surfaceEntity_movableComponent_scaleInZFalse_updatesPoseNotScale() {
        val session = sessionRule.session
        val moveStartLatch = CountDownLatch(1)
        val moveEndLatch = CountDownLatch(1)
        val startScaleRef = AtomicReference<Float>()
        val endScaleRef = AtomicReference<Float>()

        session.scene.mainPanelEntity.setEnabled(false)

        val quadHeight = 1.0f
        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = Pose(Vector3(0f, 0f, -1.5f)),
                shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.2f, quadHeight)),
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.LTGRAY)

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)
        movableComponent.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveStart(
                    entity: Entity,
                    initialInputRay: Ray,
                    initialPose: Pose,
                    initialScale: Float,
                    initialParent: Entity,
                ) {
                    startScaleRef.set(initialScale)
                    moveStartLatch.countDown()
                }

                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    endScaleRef.set(finalScale)
                    moveEndLatch.countDown()
                }
            }
        )
        assertThat(created.addComponent(movableComponent)).isTrue()

        val movableNode = awaitSurfaceParentNode { it.isMovable }
        val initialTranslation = created.getPose(Space.ACTIVITY).translation
        val marginMeters =
            if (movableNode.worldHalfExtents != null) {
                DEFAULT_MOVE_HANDLE_MARGIN
            } else {
                (quadHeight / 2f) * movableNode.scale.y + DEFAULT_MOVE_HANDLE_MARGIN
            }

        movableNode.performSpatialInteraction {
            moveNodeBy(Vector3(0.35f, 0.15f, -0.3f), marginMeters = marginMeters)
        }

        assertThat(moveStartLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(moveEndLatch.await(5, TimeUnit.SECONDS)).isTrue()

        val distanceMoved =
            (created.getPose(Space.ACTIVITY).translation - initialTranslation).length
        assertThat(distanceMoved).isGreaterThan(0.1f)
        assertThat(created.getScale(Space.ACTIVITY)).isWithin(1e-2f).of(1.0f)
        assertThat(startScaleRef.get()).isNotNull()
        assertThat(endScaleRef.get()).isNotNull()
        assertThat(endScaleRef.get()!!).isWithin(1e-2f).of(startScaleRef.get()!!)

        created.removeComponent(movableComponent)
    }

    @Test
    fun surfaceEntity_movableComponent_scaleInZTrue_updatesPoseAndScale() {
        val session = sessionRule.session
        val moveStartLatch = CountDownLatch(1)
        val moveEndLatch = CountDownLatch(1)
        val startScaleRef = AtomicReference<Float>()
        val endScaleRef = AtomicReference<Float>()

        session.scene.mainPanelEntity.setEnabled(false)

        val quadHeight = 1.0f
        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = Pose(Vector3(0f, 0f, -1.5f)),
                shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, quadHeight)),
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.DKGRAY)

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = true)
        movableComponent.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveStart(
                    entity: Entity,
                    initialInputRay: Ray,
                    initialPose: Pose,
                    initialScale: Float,
                    initialParent: Entity,
                ) {
                    startScaleRef.set(initialScale)
                    moveStartLatch.countDown()
                }

                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    endScaleRef.set(finalScale)
                    moveEndLatch.countDown()
                }
            }
        )
        assertThat(created.addComponent(movableComponent)).isTrue()

        val movableNode = awaitSurfaceParentNode { it.isMovable }
        val initialZ = created.getPose(Space.ACTIVITY).translation.z
        val marginMeters =
            if (movableNode.worldHalfExtents != null) {
                DEFAULT_MOVE_HANDLE_MARGIN
            } else {
                (quadHeight / 2f) * movableNode.scale.y + DEFAULT_MOVE_HANDLE_MARGIN
            }

        movableNode.performSpatialInteraction {
            moveNodeBy(Vector3(0f, 0f, -0.6f), marginMeters = marginMeters)
        }

        assertWithMessage("Timed out waiting for MoveEvent.onMoveStart")
            .that(moveStartLatch.await(5, TimeUnit.SECONDS))
            .isTrue()
        assertWithMessage("Timed out waiting for MoveEvent.onMoveEnd")
            .that(moveEndLatch.await(5, TimeUnit.SECONDS))
            .isTrue()
        assertThat(created.getPose(Space.ACTIVITY).translation.z).isLessThan(initialZ - 0.15f)
        assertThat(created.getScale(Space.ACTIVITY)).isGreaterThan(1.05f)
        assertThat(startScaleRef.get()).isNotNull()
        assertThat(endScaleRef.get()).isNotNull()
        assertThat(endScaleRef.get()!!).isGreaterThan(startScaleRef.get()!!)

        created.removeComponent(movableComponent)
    }

    @Test
    fun surfaceEntity_resizableComponent_resizesQuadAndPreservesAspectRatio() {
        val session = sessionRule.session
        val resizeStates = Collections.synchronizedList(mutableListOf<ResizeEvent.ResizeState>())
        val endEventRef = AtomicReference<ResizeEvent>()
        val resizeEndLatch = CountDownLatch(1)
        lateinit var resizableComponent: ResizableComponent

        session.scene.mainPanelEntity.setEnabled(false)

        val initialWidth = 1.2f
        val initialHeight = 0.6f
        val expectedAspectRatio = initialWidth / initialHeight
        val created = sessionRule.onMainThread {
            SurfaceEntity.create(
                session = session,
                pose = Pose(Vector3(0f, 0f, -1.5f)),
                shape = SurfaceEntity.Shape.Quad(FloatSize2d(initialWidth, initialHeight)),
                parent = session.scene.activitySpace,
            )
        }
        created.renderSolidColorFrame(Color.WHITE)

        resizableComponent =
            ResizableComponent.create(
                session = session,
                minimumSize = FloatSize3d(0.4f, 0.4f, 0.0f),
                maximumSize = FloatSize3d(3.0f, 3.0f, 0.0f),
                resizeEventListener =
                    Consumer { event ->
                        resizeStates.add(event.resizeState)
                        if (event.resizeState == ResizeEvent.ResizeState.END) {
                            // Default mainThreadExecutor invokes this listener on the main thread.
                            created.shape =
                                SurfaceEntity.Shape.Quad(
                                    FloatSize2d(event.newSize.width, event.newSize.height)
                                )
                            resizableComponent.affordanceSize = event.newSize
                            endEventRef.set(event)
                            resizeEndLatch.countDown()
                        }
                    },
            )
        assertThat(created.addComponent(resizableComponent)).isTrue()
        // Set aspect-ratio lock and always-show overlay after attach so ResizableComponentImpl
        // has already initialized currentSize from Shape.Quad and has a non-null entity.
        resizableComponent.isFixedAspectRatioEnabled = true
        resizableComponent.isAlwaysShowOverlayEnabled = true

        val surfaceNode = awaitSurfaceParentNode { it.isVisible && it.isResizable }
        val cornerInSurface =
            Vector3(
                (initialWidth / 2f) + DEFAULT_MOVE_HANDLE_MARGIN,
                -((initialHeight / 2f) + DEFAULT_MOVE_HANDLE_MARGIN),
                0f,
            )
        val dragDelta = Vector3(0.25f, -0.05f, 0f)

        surfaceNode.performSpatialInteraction {
            pointAt(offset = cornerInSurface)
            press()
            animate(durationMs = 250L) { fraction ->
                pointAt(offset = cornerInSurface + (dragDelta * fraction))
            }
            release()
        }

        assertWithMessage("Timed out waiting for ResizeEvent.ResizeState.END on SurfaceEntity")
            .that(resizeEndLatch.await(5, TimeUnit.SECONDS))
            .isTrue()

        val states = synchronized(resizeStates) { resizeStates.toList() }
        assertThat(states)
            .containsAtLeast(ResizeEvent.ResizeState.START, ResizeEvent.ResizeState.END)
        val endEvent = endEventRef.get()
        assertThat(endEvent.entity).isEqualTo(created)
        assertThat(endEvent.newSize.width).isGreaterThan(initialWidth + 0.05f)
        assertThat(endEvent.newSize.height).isGreaterThan(initialHeight + 0.025f)
        val actualAspectRatio = endEvent.newSize.width / endEvent.newSize.height
        assertThat(actualAspectRatio).isWithin(0.05f).of(expectedAspectRatio)

        assertThat(created.dimensions.width).isWithin(FLOAT_TOLERANCE).of(endEvent.newSize.width)
        assertThat(created.dimensions.height).isWithin(FLOAT_TOLERANCE).of(endEvent.newSize.height)

        created.removeComponent(resizableComponent)
    }

    private fun readSubspaceAlpha(subspace: SnapshotXrSpace): Float? =
        subspace.properties["computedAlpha"]?.toFloatOrNull()
            ?: subspace.parent?.properties?.get("computedAlpha")?.toFloatOrNull()

    private fun awaitSingleSurfaceSubspaceNode(): SnapshotXrSpace {
        val task = awaitSurfaceSubspaceCount(expectedCount = 1)
        return task.subspaceNodes.first {
            it.name.startsWith(SURFACE_SUBSPACE_PREFIX) && it.isVisible
        }
    }

    private fun awaitSurfaceSubspaceCount(expectedCount: Int): SnapshotXrSpace =
        checkNotNull(
            waitForSpatialScene(
                description = "SurfaceEntity subspace node count == $expectedCount",
                timeout = SCENE_TIMEOUT,
                select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
                until = { task ->
                    task != null &&
                        task.subspaceNodes.count {
                            it.name.startsWith(SURFACE_SUBSPACE_PREFIX) && it.isVisible
                        } == expectedCount
                },
            )
        )

    private fun awaitSurfaceParentNode(predicate: (SnapshotXrSpace) -> Boolean): SnapshotXrSpace {
        val task =
            checkNotNull(
                waitForSpatialScene(
                    description = "SurfaceEntity parent reform node",
                    timeout = SCENE_TIMEOUT,
                    select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
                    until = { candidateTask ->
                        candidateTask?.subspaceNodes?.any { subspace ->
                            subspace.name.startsWith(SURFACE_SUBSPACE_PREFIX) &&
                                subspace.parent?.let(predicate) == true
                        } == true
                    },
                )
            )
        return task.subspaceNodes.firstNotNullOf { subspace ->
            if (subspace.name.startsWith(SURFACE_SUBSPACE_PREFIX)) {
                subspace.parent?.takeIf(predicate)
            } else {
                null
            }
        }
    }

    private fun findHostTask(): SnapshotXrSpace =
        checkNotNull(
            waitForSpatialScene(
                description = "active task space for ${sessionRule.activity}",
                timeout = SCENE_TIMEOUT,
                select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
                until = { it != null },
            )
        ) {
            "Expected active task space for ${sessionRule.activity}"
        }

    private fun findActivitySpaceNode(task: SnapshotXrSpace): SnapshotXrSpace =
        checkNotNull(task.findNodeByName(ACTIVITY_SPACE_NODE_NAME)) {
            "Expected $ACTIVITY_SPACE_NODE_NAME in task space"
        }

    private companion object {
        const val FLOAT_TOLERANCE = 1e-3f
        const val CPM_POSE_TOLERANCE = 5e-2f
        const val CPM_ALPHA_TOLERANCE = 5e-2f
        const val SURFACE_SUBSPACE_PREFIX = "stereo_surface_panel_entity_subspace_"
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        val SCENE_TIMEOUT = 5.seconds
    }
}
