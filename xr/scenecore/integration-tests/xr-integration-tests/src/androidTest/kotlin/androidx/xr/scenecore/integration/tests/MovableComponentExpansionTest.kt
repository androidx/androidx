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

import android.os.SystemClock
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Expanded SceneCore integration tests for [MovableComponent], covering scenarios outside
 * [MovableComponentTest]: `scaleInZ` depth scaling, [GltfModelEntity] footprint dragging and
 * event-count parity, parent-child movable/resizable ("Everything") panel hierarchies, and movement
 * prevention after component removal or entity disablement.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class MovableComponentExpansionTest {

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
    fun movableComponent_panelEntity_scaleInZTrue_scalesPanelAlongDepthMove() {
        val panelName = "scaleInZTruePanel"
        val session = sessionRule.session
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(activity, this.session, panelName, "ScaleInZ True Panel")
        }
        val component =
            MovableComponent.createSystemMovable(session, scaleInZ = true).apply {
                size = panel.size.to3d()
                addMoveListener(moveListener)
            }
        assertThat(panel.addComponent(component)).isTrue()

        val panelNode = waitForMovablePanelNode(panelName)
        val initialPose = panel.getPose(Space.ACTIVITY)
        val initialScale = panel.getScale(Space.ACTIVITY)

        panelNode.performSpatialInteraction { moveNodeBy(DEPTH_MOVE_AWAY_DELTA) }

        assertWithMessage("Move listener did not receive onMoveEnd")
            .that(moveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        assertThat(moveListener.onMoveStartCalled.get()).isTrue()
        assertThat(moveListener.onMoveUpdateCalled.get()).isTrue()
        assertThat(moveListener.onMoveEndCalled.get()).isTrue()

        waitUntil("panel to move away in -Z and scale up", INTERACTION_TIMEOUT) {
            val pose = panel.getPose(Space.ACTIVITY)
            val scale = panel.getScale(Space.ACTIVITY)
            pose.translation.z < initialPose.translation.z - MIN_DEPTH_MOVE_METERS &&
                scale > initialScale + MIN_SCALE_INCREASE
        }

        val finalScale = panel.getScale(Space.ACTIVITY)
        assertThat(finalScale).isGreaterThan(initialScale + MIN_SCALE_INCREASE)
        assertThat(moveListener.lastEndScale).isGreaterThan(initialScale + MIN_SCALE_INCREASE)
    }

    @Test
    fun movableComponent_panelEntity_scaleInZFalse_preservesScaleAlongDepthMove() {
        val panelName = "scaleInZFalseDepthPanel"
        val session = sessionRule.session
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(activity, this.session, panelName, "ScaleInZ False Panel")
        }
        val component =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = panel.size.to3d()
                addMoveListener(moveListener)
            }
        assertThat(panel.addComponent(component)).isTrue()

        val panelNode = waitForMovablePanelNode(panelName)
        val initialPose = panel.getPose(Space.ACTIVITY)
        val initialScale = panel.getScale(Space.ACTIVITY)

        panelNode.performSpatialInteraction { moveNodeBy(DEPTH_MOVE_AWAY_DELTA) }

        assertWithMessage("Move listener did not receive onMoveEnd")
            .that(moveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        assertThat(moveListener.onMoveStartCalled.get()).isTrue()
        assertThat(moveListener.onMoveEndCalled.get()).isTrue()

        waitUntil("panel to move away in -Z", INTERACTION_TIMEOUT) {
            panel.getPose(Space.ACTIVITY).translation.z <
                initialPose.translation.z - MIN_DEPTH_MOVE_METERS
        }

        val finalScale = panel.getScale(Space.ACTIVITY)
        assertThat(finalScale).isWithin(SCALE_TOLERANCE).of(initialScale)
        assertThat(moveListener.lastEndScale).isWithin(SCALE_TOLERANCE).of(initialScale)
    }

    // TODO(b/584908075): Remove @Ignore once SplitEngineMeshCollider footprint.glb raycasting is
    // fixed.
    @Ignore(
        "b/584908075: SplitEngineMeshCollider raycasting on unskinned footprint.glb blocks MovableComponent translation"
    )
    @Test
    fun movableComponent_gltfModelEntity_systemMovable_movesEntityWithMatchingEventCounts() {
        val session = sessionRule.session
        val activity = sessionRule.activity
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)
        val inputListener = RecordingInputEventListener()

        session.scene.mainPanelEntity.setEnabled(false)
        val entity = sessionRule.onMainThread {
            val loadedModel =
                GltfModel.create(this.session, Paths.get("models", BOUNDING_BOX_MODEL_ASSET))
            GltfModelEntity.create(
                session = this.session,
                model = loadedModel,
                pose = PANEL_POSE,
                parent = this.session.scene.activitySpace,
            )
        }

        val movable =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = GLTF_HALF_EXTENTS * 2f
                addMoveListener(moveListener)
            }
        assertThat(entity.addComponent(movable)).isTrue()

        val interactable =
            InteractableComponent.create(
                session = session,
                executor = activity.mainExecutor,
                inputEventListener = inputListener,
            )
        assertThat(entity.addComponent(interactable)).isTrue()

        val gltfSubspaceNode = waitForGltfSubspaceNode()
        val initialPose = entity.getPose(Space.ACTIVITY)

        wakeGltfFootprintIntoIdle(gltfSubspaceNode, inputListener)
        inputListener.resetEventCountsAndLatches()

        gltfSubspaceNode.performSpatialInteraction {
            moveOriginTo(GLTF_FOOTPRINT_RAY_ORIGIN_LOCAL)
            pointAt(offset = GLTF_FOOTPRINT_RAY_TARGET_LOCAL)
            press()
            animate { fraction ->
                val dragDelta = GLTF_MOVE_DELTA * fraction
                moveOriginTo(GLTF_FOOTPRINT_RAY_ORIGIN_LOCAL + dragDelta)
                pointAt(offset = GLTF_FOOTPRINT_RAY_TARGET_LOCAL + dragDelta)
            }
            release()
        }

        assertWithMessage("Move listener did not receive onMoveEnd for GltfModelEntity")
            .that(moveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        assertWithMessage("InteractableComponent did not receive Action.UP for GltfModelEntity")
            .that(inputListener.upLatch.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()

        waitUntil("GltfModelEntity to move", INTERACTION_TIMEOUT) {
            Vector3.distance(entity.getPose(Space.ACTIVITY).translation, initialPose.translation) >=
                MIN_MOVE_DISTANCE_METERS
        }

        // R14 non-double-counting requirement: released footprint pinches match MoveEvent counts.
        assertThat(moveListener.moveStartCount.get()).isEqualTo(1)
        assertThat(moveListener.moveEndCount.get()).isEqualTo(1)
        assertThat(inputListener.downCount.get()).isEqualTo(1)
        assertThat(inputListener.upCount.get()).isEqualTo(1)
        assertThat(inputListener.upCount.get()).isEqualTo(moveListener.moveEndCount.get())
    }

    // TODO(b/584908075): Remove @Ignore once SplitEngineMeshCollider raycasting is fixed.
    @Ignore(
        "b/584908075: SplitEngineMeshCollider raycasting on unskinned footprint.glb blocks MovableComponent translation"
    )
    @Test
    fun movableComponent_gltfModelEntity_customMovable_updatesPoseWithoutDoubleCounting() {
        val session = sessionRule.session
        val activity = sessionRule.activity
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)
        val inputListener = RecordingInputEventListener()

        session.scene.mainPanelEntity.setEnabled(false)
        val entity = sessionRule.onMainThread {
            val loadedModel =
                GltfModel.create(this.session, Paths.get("models", BOUNDING_BOX_MODEL_ASSET))
            GltfModelEntity.create(
                session = this.session,
                model = loadedModel,
                pose = PANEL_POSE,
                parent = this.session.scene.activitySpace,
            )
        }

        val movable =
            MovableComponent.createCustomMovable(
                    session = session,
                    scaleInZ = false,
                    executor = activity.mainExecutor,
                    entityMoveListener = moveListener,
                )
                .apply { size = GLTF_HALF_EXTENTS * 2f }
        assertThat(entity.addComponent(movable)).isTrue()

        val interactable =
            InteractableComponent.create(
                session = session,
                executor = activity.mainExecutor,
                inputEventListener = inputListener,
            )
        assertThat(entity.addComponent(interactable)).isTrue()

        val gltfSubspaceNode = waitForGltfSubspaceNode()
        val initialPose = entity.getPose(Space.ACTIVITY)

        wakeGltfFootprintIntoIdle(gltfSubspaceNode, inputListener)
        inputListener.resetEventCountsAndLatches()

        gltfSubspaceNode.performSpatialInteraction {
            moveOriginTo(GLTF_FOOTPRINT_RAY_ORIGIN_LOCAL)
            pointAt(offset = GLTF_FOOTPRINT_RAY_TARGET_LOCAL)
            press()
            animate { fraction ->
                val dragDelta = GLTF_MOVE_DELTA * fraction
                moveOriginTo(GLTF_FOOTPRINT_RAY_ORIGIN_LOCAL + dragDelta)
                pointAt(offset = GLTF_FOOTPRINT_RAY_TARGET_LOCAL + dragDelta)
            }
            release()
        }

        assertWithMessage("Custom MoveListener did not receive onMoveEnd")
            .that(moveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        assertWithMessage("InteractableComponent did not receive Action.UP")
            .that(inputListener.upLatch.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        assertThat(moveListener.onMoveStartCalled.get()).isTrue()
        assertThat(moveListener.onMoveUpdateCalled.get()).isTrue()
        assertThat(moveListener.onMoveEndCalled.get()).isTrue()

        waitUntil("Custom movable GltfModelEntity to move", INTERACTION_TIMEOUT) {
            Vector3.distance(entity.getPose(Space.ACTIVITY).translation, initialPose.translation) >=
                MIN_MOVE_DISTANCE_METERS
        }

        assertThat(moveListener.moveStartCount.get()).isEqualTo(1)
        assertThat(moveListener.moveEndCount.get()).isEqualTo(1)
        assertThat(inputListener.downCount.get()).isEqualTo(1)
        assertThat(inputListener.upCount.get()).isEqualTo(1)
        assertThat(inputListener.upCount.get()).isEqualTo(moveListener.moveEndCount.get())
    }

    @Test
    fun movableComponent_parentAndChildMovable_movingParentMovesChildInActivitySpace() {
        val parentName = "parentMovablePanel"
        val childName = "everythingChildPanel"
        val session = sessionRule.session
        val parentMoveListener = RecordingMoveListener(applyMoveToEntity = false)

        session.scene.mainPanelEntity.setEnabled(false)
        val (parent, child) =
            sessionRule.onMainThread {
                val createdParent =
                    createSpatialPanel(
                        activity = activity,
                        session = this.session,
                        name = parentName,
                        text = "Parent Movable Panel",
                        dimensions = HIERARCHY_PANEL_DIMENSIONS,
                        pose = HIERARCHY_PARENT_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdChild =
                    createSpatialPanel(
                        activity = activity,
                        session = this.session,
                        name = childName,
                        text = "Everything Child Panel",
                        dimensions = HIERARCHY_PANEL_DIMENSIONS,
                        pose = HIERARCHY_CHILD_LOCAL_POSE,
                        parent = createdParent,
                    )
                createdParent to createdChild
            }

        val parentMovable =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = parent.size.to3d()
                addMoveListener(parentMoveListener)
            }
        assertThat(parent.addComponent(parentMovable)).isTrue()

        val childMovable =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = child.size.to3d()
            }
        assertThat(child.addComponent(childMovable)).isTrue()

        val parentNode = waitForMovablePanelNode(parentName)
        waitForMovablePanelNode(childName)

        val initialParentActivityPose = parent.getPose(Space.ACTIVITY)
        val initialChildActivityPose = child.getPose(Space.ACTIVITY)
        val initialChildLocalPose = child.getPose(Space.PARENT)

        parentNode.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }

        assertWithMessage("Parent move listener did not receive onMoveEnd")
            .that(parentMoveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()

        waitUntil("parent and child to move together in ActivitySpace", INTERACTION_TIMEOUT) {
            Vector3.distance(
                parent.getPose(Space.ACTIVITY).translation,
                initialParentActivityPose.translation,
            ) >= MIN_SMALL_MOVE_DISTANCE_METERS &&
                Vector3.distance(
                    child.getPose(Space.ACTIVITY).translation,
                    initialChildActivityPose.translation,
                ) >= MIN_SMALL_MOVE_DISTANCE_METERS
        }

        val finalChildLocalPose = child.getPose(Space.PARENT)
        assertPose(finalChildLocalPose, initialChildLocalPose, POSE_TOLERANCE)
    }

    @Test
    fun movableComponent_childMovableAndResizable_movingChildKeepsParentStationary() {
        val parentName = "parentStationaryPanel"
        val childName = "everythingMovableResizableChild"
        val session = sessionRule.session
        val parentMoveListener = RecordingMoveListener(applyMoveToEntity = false)
        val childMoveListener = RecordingMoveListener(applyMoveToEntity = false)
        val childResizeEnded = CountDownLatch(1)

        session.scene.mainPanelEntity.setEnabled(false)
        val (parent, child) =
            sessionRule.onMainThread {
                val createdParent =
                    createSpatialPanel(
                        activity = activity,
                        session = this.session,
                        name = parentName,
                        text = "Parent Panel",
                        dimensions = HIERARCHY_PANEL_DIMENSIONS,
                        pose = HIERARCHY_PARENT_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdChild =
                    createSpatialPanel(
                        activity = activity,
                        session = this.session,
                        name = childName,
                        text = "Everything Panel",
                        dimensions = HIERARCHY_PANEL_DIMENSIONS,
                        pose = HIERARCHY_CHILD_LOCAL_POSE,
                        parent = createdParent,
                    )
                createdParent to createdChild
            }

        val parentMovable =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = parent.size.to3d()
                addMoveListener(parentMoveListener)
            }
        assertThat(parent.addComponent(parentMovable)).isTrue()

        val childMovable =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = child.size.to3d()
                addMoveListener(childMoveListener)
            }
        assertThat(child.addComponent(childMovable)).isTrue()

        var childResizableComponent: ResizableComponent? = null
        val childResizable =
            ResizableComponent.create(
                    session = session,
                    resizeEventListener =
                        Consumer { event: ResizeEvent ->
                            if (event.resizeState == ResizeEvent.ResizeState.END) {
                                (event.entity as? PanelEntity)?.size = event.newSize.to2d()
                                childMovable.size = event.newSize
                                childResizableComponent?.affordanceSize = event.newSize
                                childResizeEnded.countDown()
                            }
                        },
                )
                .apply { affordanceSize = child.size.to3d() }
        childResizableComponent = childResizable
        assertThat(child.addComponent(childResizable)).isTrue()

        waitForMovablePanelNode(parentName)
        val initialChildNode = waitForMovableAndResizablePanelNode(childName)

        val initialParentPose = parent.getPose(Space.ACTIVITY)
        val initialChildPose = child.getPose(Space.ACTIVITY)
        val initialChildSize = child.sizeInPixels

        // Step 1: Move child panel independently inward and upward inside Aura FOV; parent must
        // stay stationary.
        initialChildNode.performSpatialInteraction { moveNodeBy(CHILD_INWARD_MOVE_DELTA) }

        assertWithMessage("Child move listener did not receive onMoveEnd")
            .that(childMoveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()

        waitUntil("child panel to move independently", INTERACTION_TIMEOUT) {
            Vector3.distance(
                child.getPose(Space.ACTIVITY).translation,
                initialChildPose.translation,
            ) >= MIN_SMALL_MOVE_DISTANCE_METERS
        }

        // Settle window after confirmed child move arrival to catch any stray parent drift.
        assertThat(
                parentMoveListener.moveStartedLatch.await(
                    NO_EFFECT_SETTLE_MS,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isFalse()
        assertThat(parentMoveListener.moveStartCount.get()).isEqualTo(0)
        assertPose(parent.getPose(Space.ACTIVITY), initialParentPose, POSE_TOLERANCE)

        // Step 2: Wait for CPM node to reflect Step 1 move before resizing it ("Everything"
        // supports
        // move + resize).
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val refreshedChildNode =
            checkNotNull(
                waitForSpatialScene(
                    description = "moved child panel '$childName' ready for resize",
                    timeout = INTERACTION_TIMEOUT,
                    select = { scene ->
                        scene.findTaskByActivity(sessionRule.activity)?.findNode {
                            it.name == childName && it.isEmbeddedWindowLeash
                        }
                    },
                    until = { node ->
                        node != null &&
                            node.isVisible &&
                            node.isMovable &&
                            node.isResizable &&
                            node.worldExtents != null &&
                            Vector3.distance(node.position, initialChildNode.position) > 0.05f
                    },
                )
            )
        refreshedChildNode.performSpatialInteraction {
            resizeNodeBy(RESIZE_DELTA, alignment = SpatialAlignment.BottomRight)
        }

        assertWithMessage("Child resize listener did not receive ResizeState.END")
            .that(childResizeEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()

        waitUntil("child panel sizeInPixels to grow", INTERACTION_TIMEOUT) {
            val updatedSize = child.sizeInPixels
            updatedSize.width > initialChildSize.width &&
                updatedSize.height > initialChildSize.height
        }
    }

    @Test
    fun movableComponent_removedComponent_preventsFurtherMovement() {
        val targetName = "removableMovablePanel"
        val backstopName = "removalBackstopPanel"
        val session = sessionRule.session
        val activity = sessionRule.activity
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)
        val backstopInputListener = RecordingInputEventListener()

        session.scene.mainPanelEntity.setEnabled(false)
        val (backstop, target) =
            sessionRule.onMainThread {
                val createdBackstop =
                    createSpatialPanel(
                        activity = this.activity,
                        session = this.session,
                        name = backstopName,
                        text = "Backstop",
                        dimensions = BACKSTOP_PANEL_DIMENSIONS,
                        pose = BACKSTOP_PANEL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdTarget =
                    createSpatialPanel(
                        activity = this.activity,
                        session = this.session,
                        name = targetName,
                        text = "Removable Movable Panel",
                        dimensions = REMOVAL_PANEL_DIMENSIONS,
                        pose = PANEL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                createdBackstop to createdTarget
            }

        assertThat(
                backstop.addComponent(
                    InteractableComponent.create(
                        session = session,
                        executor = activity.mainExecutor,
                        inputEventListener = backstopInputListener,
                    )
                )
            )
            .isTrue()

        val movableComponent =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = target.size.to3d()
                addMoveListener(moveListener)
            }
        assertThat(target.addComponent(movableComponent)).isTrue()

        waitForPanelNodeWithExtents(backstopName)
        val movableNode = waitForMovablePanelNode(targetName)
        val initialPose = target.getPose(Space.ACTIVITY)

        // Verify moving works before removing MovableComponent.
        movableNode.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }
        assertWithMessage("Initial move did not end")
            .that(moveListener.moveEnded.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        waitUntil("panel to move before removing MovableComponent", INTERACTION_TIMEOUT) {
            Vector3.distance(target.getPose(Space.ACTIVITY).translation, initialPose.translation) >=
                MIN_SMALL_MOVE_DISTANCE_METERS
        }

        target.removeComponent(movableComponent)

        val nonMovableNode =
            checkNotNull(
                waitForSpatialScene(
                    description = "panel without MovableComponent or DragBar",
                    timeout = INTERACTION_TIMEOUT,
                    select = { scene ->
                        scene.findTaskByActivity(sessionRule.activity)?.findNode {
                            it.name == targetName && it.isEmbeddedWindowLeash
                        }
                    },
                    until = { node ->
                        node != null &&
                            !node.isMovable &&
                            node.worldExtents != null &&
                            node.findNode { it.name.contains("DragBar") } == null
                    },
                )
            )

        val poseAfterRemoval = target.getPose(Space.ACTIVITY)
        moveListener.resetMoveStartedLatch()
        backstopInputListener.resetEventCountsAndLatches()

        // Drag at the edge where DragBar previously existed; without DragBar the ray hits the
        // backstop behind it (arrival anchor) and the panel must remain stationary.
        nonMovableNode.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }

        assertWithMessage("Backstop did not receive Action.UP arrival anchor after DragBar removal")
            .that(backstopInputListener.upLatch.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        // Short settle window after confirmed Action.UP arrival anchor to catch any late drift.
        assertThat(moveListener.moveStartedLatch.await(NO_EFFECT_SETTLE_MS, TimeUnit.MILLISECONDS))
            .isFalse()

        assertThat(moveListener.moveStartCount.get()).isEqualTo(1)
        assertPose(target.getPose(Space.ACTIVITY), poseAfterRemoval, POSE_TOLERANCE)
    }

    @Test
    fun movableComponent_disabledEntity_preventsFurtherMovement() {
        val targetName = "disabledMovablePanel"
        val backstopName = "disabledBackstopPanel"
        val session = sessionRule.session
        val activity = sessionRule.activity
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)
        val backstopInputListener = RecordingInputEventListener()

        session.scene.mainPanelEntity.setEnabled(false)
        val (backstop, target) =
            sessionRule.onMainThread {
                val createdBackstop =
                    createSpatialPanel(
                        activity = this.activity,
                        session = this.session,
                        name = backstopName,
                        text = "Backstop",
                        dimensions = BACKSTOP_PANEL_DIMENSIONS,
                        pose = BACKSTOP_PANEL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdTarget =
                    createSpatialPanel(
                        activity = this.activity,
                        session = this.session,
                        name = targetName,
                        text = "Disabled Movable Panel",
                        dimensions = REMOVAL_PANEL_DIMENSIONS,
                        pose = PANEL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                createdBackstop to createdTarget
            }

        assertThat(
                backstop.addComponent(
                    InteractableComponent.create(
                        session = session,
                        executor = activity.mainExecutor,
                        inputEventListener = backstopInputListener,
                    )
                )
            )
            .isTrue()

        val movableComponent =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = target.size.to3d()
                addMoveListener(moveListener)
            }
        assertThat(target.addComponent(movableComponent)).isTrue()

        waitForPanelNodeWithExtents(backstopName)
        val movableNodeSnapshot = waitForMovablePanelNode(targetName)
        val initialPose = target.getPose(Space.ACTIVITY)

        target.setEnabled(false)

        waitForSpatialScene(
            description = "disabled panel hidden from CPM",
            timeout = INTERACTION_TIMEOUT,
            select = { scene ->
                scene.findTaskByActivity(sessionRule.activity)?.findNode {
                    it.name == targetName && it.isEmbeddedWindowLeash && it.isVisible
                }
            },
            until = { it == null },
        )

        backstopInputListener.resetEventCountsAndLatches()
        movableNodeSnapshot.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }

        assertWithMessage(
                "Backstop did not receive Action.UP when dragging disabled panel location"
            )
            .that(backstopInputListener.upLatch.await(INTERACTION_TIMEOUT_SEC, TimeUnit.SECONDS))
            .isTrue()
        // Short settle window after confirmed Action.UP arrival anchor to catch any late drift.
        assertThat(moveListener.moveStartedLatch.await(NO_EFFECT_SETTLE_MS, TimeUnit.MILLISECONDS))
            .isFalse()

        assertThat(moveListener.moveStartCount.get()).isEqualTo(0)
        assertPose(target.getPose(Space.ACTIVITY), initialPose, POSE_TOLERANCE)
    }

    private fun waitForPanelNodeWithExtents(panelName: String): SpatialNode =
        checkNotNull(
            waitForSpatialScene(
                description = "panel '$panelName' with known extents",
                timeout = INTERACTION_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == panelName && it.isEmbeddedWindowLeash
                    }
                },
                until = { node -> node != null && node.isVisible && node.worldExtents != null },
            )
        )

    private fun waitForMovablePanelNode(panelName: String): SpatialNode =
        checkNotNull(
            waitForSpatialScene(
                description = "movable panel '$panelName' with known extents",
                timeout = INTERACTION_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == panelName && it.isEmbeddedWindowLeash
                    }
                },
                until = { node ->
                    node != null && node.isVisible && node.isMovable && node.worldExtents != null
                },
            )
        )

    private fun waitForMovableAndResizablePanelNode(panelName: String): SpatialNode =
        checkNotNull(
            waitForSpatialScene(
                description = "movable & resizable panel '$panelName' with known extents",
                timeout = INTERACTION_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == panelName && it.isEmbeddedWindowLeash
                    }
                },
                until = { node ->
                    node != null &&
                        node.isVisible &&
                        node.isMovable &&
                        node.isResizable &&
                        node.worldExtents != null
                },
            )
        )

    private fun waitForGltfSubspaceNode(): SpatialNode =
        checkNotNull(
            waitForSpatialScene(
                description = "visible glTF subspace node",
                timeout = INTERACTION_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name.startsWith(GLTF_ENTITY_SUBSPACE_PREFIX)
                    }
                },
                until = { node -> node != null && node.isVisible },
            )
        )

    private fun wakeGltfFootprintIntoIdle(
        gltfSubspaceNode: SpatialNode,
        inputListener: RecordingInputEventListener,
    ) {
        waitUntil("glTF initial hover to enter Opening", timeout = INTERACTION_TIMEOUT) {
            if (inputListener.hoverEnterLatch.count > 0) {
                gltfSubspaceNode.performSpatialInteraction { pointAt(offset = GLTF_CENTER) }
            }
            inputListener.hoverEnterLatch.count == 0L
        }
        gltfSubspaceNode.performSpatialInteraction {
            // TODO(b/584908075): replace wake dwell sleeps with latch-gated waits once un-ignored.
            SystemClock.sleep(GLTF_WAKE_PRE_HOVER_SLEEP_MS)
            pointAt(offset = GLTF_WAKE_OFFSET)
            SystemClock.sleep(GLTF_OPENING_SETTLE_MS)
            moveOriginTo(GLTF_HOVER_AWAY_ORIGIN)
            pointAt(offset = GLTF_HOVER_AWAY_TARGET)
            SystemClock.sleep(GLTF_HOVER_AWAY_SETTLE_MS)
        }
    }

    private fun createSpatialPanel(
        activity: ComponentActivity,
        session: androidx.xr.runtime.Session,
        name: String,
        text: String,
        dimensions: FloatSize2d = PANEL_DIMENSIONS,
        pose: Pose = PANEL_POSE,
        parent: Entity = session.scene.activitySpace,
    ): PanelEntity {
        val panelContentView =
            ComposeView(activity).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = text, fontSize = 32.sp, color = Color.Black)
                    }
                }
            }
        val panelEntity =
            PanelEntity.create(
                session = session,
                view = panelContentView,
                dimensions = dimensions,
                name = name,
                pose = pose,
                parent = parent,
            )
        // PanelEntity wraps non-FrameLayout views in a new FrameLayout whose embedded window does
        // not inherit the activity's ViewTree owners, so ComposeView requires setting them on that
        // root view.
        val parentView: View =
            if (panelContentView.parent != null && panelContentView.parent is View) {
                panelContentView.parent as View
            } else {
                panelContentView
            }
        parentView.setViewTreeLifecycleOwner(activity)
        parentView.setViewTreeViewModelStoreOwner(activity)
        parentView.setViewTreeSavedStateRegistryOwner(activity)
        return panelEntity
    }

    private class RecordingMoveListener(private val applyMoveToEntity: Boolean) :
        EntityMoveListener {
        val onMoveStartCalled = AtomicBoolean(false)
        val onMoveUpdateCalled = AtomicBoolean(false)
        val onMoveEndCalled = AtomicBoolean(false)
        val moveStartCount = AtomicInteger(0)
        val moveEndCount = AtomicInteger(0)

        @Volatile var lastEndPose: Pose? = null
        @Volatile var lastEndScale: Float = 1f
        @Volatile var moveStartedLatch = CountDownLatch(1)
        @Volatile var moveEnded = CountDownLatch(1)

        fun resetMoveStartedLatch() {
            moveStartedLatch = CountDownLatch(1)
        }

        override fun onMoveStart(
            entity: Entity,
            initialInputRay: Ray,
            initialPose: Pose,
            initialScale: Float,
            initialParent: Entity,
        ) {
            onMoveStartCalled.set(true)
            moveStartCount.incrementAndGet()
            moveStartedLatch.countDown()
        }

        override fun onMoveUpdate(
            entity: Entity,
            currentInputRay: Ray,
            currentPose: Pose,
            currentScale: Float,
        ) {
            onMoveUpdateCalled.set(true)
            if (applyMoveToEntity) {
                entity.setPose(currentPose)
                entity.setScale(currentScale)
            }
        }

        override fun onMoveEnd(
            entity: Entity,
            finalInputRay: Ray,
            finalPose: Pose,
            finalScale: Float,
            updatedParent: Entity?,
        ) {
            if (applyMoveToEntity) {
                entity.setPose(finalPose)
                entity.setScale(finalScale)
            }
            lastEndPose = finalPose
            lastEndScale = finalScale
            onMoveEndCalled.set(true)
            moveEndCount.incrementAndGet()
            moveEnded.countDown()
        }
    }

    private class RecordingInputEventListener : Consumer<InputEvent> {
        val downCount = AtomicInteger(0)
        val upCount = AtomicInteger(0)
        @Volatile var hoverEnterLatch = CountDownLatch(1)
        @Volatile var upLatch = CountDownLatch(1)

        fun resetEventCountsAndLatches() {
            downCount.set(0)
            upCount.set(0)
            hoverEnterLatch = CountDownLatch(1)
            upLatch = CountDownLatch(1)
        }

        override fun accept(event: InputEvent) {
            when (event.action) {
                InputEvent.Action.HOVER_ENTER -> hoverEnterLatch.countDown()
                InputEvent.Action.DOWN -> downCount.incrementAndGet()
                InputEvent.Action.UP -> {
                    upCount.incrementAndGet()
                    upLatch.countDown()
                }
                else -> Unit
            }
        }
    }

    private companion object {
        const val BOUNDING_BOX_MODEL_ASSET = "BoundingBoxBlue.glb"
        const val GLTF_ENTITY_SUBSPACE_PREFIX = "gltf_entity_subspace_"

        val GLTF_CENTER = Vector3.Zero
        val GLTF_HALF_EXTENTS = FloatSize3d(0.5f, 0.5f, 0.5f)
        val PANEL_DIMENSIONS = FloatSize2d(1.28f, 0.96f)
        val HIERARCHY_PANEL_DIMENSIONS = FloatSize2d(0.50f, 0.40f)
        val REMOVAL_PANEL_DIMENSIONS = FloatSize2d(1.0f, 0.7f)
        val BACKSTOP_PANEL_DIMENSIONS = FloatSize2d(1.6f, 1.4f)

        val PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))
        val BACKSTOP_PANEL_POSE = Pose(Vector3(0f, 0f, -0.3f))
        val HIERARCHY_PARENT_POSE = Pose(Vector3(-0.28f, 0f, 0.1f))
        val HIERARCHY_CHILD_LOCAL_POSE = Pose(Vector3(0.56f, 0f, 0f))

        val SMALL_MOVE_DELTA = Vector3(0.15f, 0.10f, 0f)
        val CHILD_INWARD_MOVE_DELTA = Vector3(-0.14f, 0.10f, 0f)
        val DEPTH_MOVE_AWAY_DELTA = Vector3(0f, 0f, -0.60f)
        val RESIZE_DELTA = Vector3(0.2f, -0.2f, 0f)

        val GLTF_MOVE_DELTA = Vector3(0.22f, 0.14f, 0f)
        val GLTF_WAKE_OFFSET = Vector3(0.03f, 0f, 0f)
        val GLTF_HOVER_AWAY_ORIGIN = Vector3(1.2f, 0f, 0.8f)
        val GLTF_HOVER_AWAY_TARGET = Vector3(1.2f, 0f, 0f)
        // Front-rim rays cross the unskinned footprint.glb skirt in z in [0.500, 0.5375] without
        // hitting model_node_ (b/584908075).
        val GLTF_FOOTPRINT_RAY_ORIGIN_LOCAL = Vector3(0f, 0.45f, 0.532f)
        val GLTF_FOOTPRINT_RAY_TARGET_LOCAL = Vector3(0f, -0.95f, 0.508f)

        const val GLTF_WAKE_PRE_HOVER_SLEEP_MS = 250L
        const val GLTF_OPENING_SETTLE_MS = 650L
        const val GLTF_HOVER_AWAY_SETTLE_MS = 150L

        const val MIN_MOVE_DISTANCE_METERS = 0.25f
        const val MIN_SMALL_MOVE_DISTANCE_METERS = 0.12f
        const val MIN_DEPTH_MOVE_METERS = 0.25f
        const val MIN_SCALE_INCREASE = 0.05f
        const val SCALE_TOLERANCE = 1e-3f
        const val POSE_TOLERANCE = 1e-3f

        const val INTERACTION_TIMEOUT_SEC = 5L
        val INTERACTION_TIMEOUT = 5.seconds
        const val NO_EFFECT_SETTLE_MS = 300L
    }
}
