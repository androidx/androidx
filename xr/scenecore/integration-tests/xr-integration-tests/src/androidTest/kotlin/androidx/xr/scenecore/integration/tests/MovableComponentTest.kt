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
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.xr.testutils.SnapshotXrSpace
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionScope
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Integration tests for [MovableComponent], driven by injected raycast input.
 *
 * Scenarios covered:
 * - Baseline system and custom movable drags on `mainPanelEntity` and [PanelEntity].
 * - [MovableComponent] depth movement along `-Z` with `scaleInZ = true` and `scaleInZ = false`.
 * - [GltfModelEntity] system and custom movable translation and input/move event-count parity
 *   (marked `@Ignore` under b/584908075 due to unskinned `footprint.glb` collider raycasting).
 * - Parent-child movable hierarchies and combined child [MovableComponent] + [ResizableComponent]
 *   ("Everything" panel CUJ).
 * - Movement prevention after `removeComponent(movableComponent)` or `entity.setEnabled(false)`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class MovableComponentTest {

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
    fun movableComponent_mainPanelEntity_systemMovable_isMovable() {
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread { activity.setContent { CenteredLabel("This is MainPanel") } }

        // 2. Create the MovableComponent (system movable) and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val movableComponent =
            MovableComponent.createSystemMovable(session = sessionRule.session, scaleInZ = false)
        movableComponent.addMoveListener(moveListener)
        assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()

        // 3. Wait for the system to report the main panel as movable before sampling the pose.
        val mainPanelNode = awaitMainPanelNode()
        val initialPosition = mainPanelEntity.getPose(Space.ACTIVITY).translation

        // 4. Grab the main panel by its move affordance and drag it.
        mainPanelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that the main panel pose was updated.
        awaitMoveEnd(moveListener)
        assertPanelHasMoved(mainPanelEntity, initialPosition)
    }

    @Test
    fun movableComponent_panelEntity_systemMovable_isMovable() {
        val panelName = "movablePanel"
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        val panelEntity = sessionRule.onMainThread {
            // 1. Create the PanelEntity under test, then hide the main panel so it does not
            // interfere with the spatial panel test.
            val panel = createSpatialPanel(panelName, "This is Spatial Panel")
            session.scene.mainPanelEntity.setEnabled(false)

            // 2. Create the MovableComponent (system movable) and attach it to the panel entity.
            val movableComponent =
                MovableComponent.createSystemMovable(session = session, scaleInZ = false)
            movableComponent.addMoveListener(moveListener)
            assertThat(panel.addComponent(movableComponent)).isTrue()
            panel
        }

        // 3. Wait for the system to report the panel as movable before sampling the pose.
        val panelNode = awaitPanelNode(panelName)
        val initialPosition = panelEntity.getPose(Space.ACTIVITY).translation

        // 4. Grab the panel by its move affordance and drag it.
        panelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that the panel entity pose was updated.
        awaitMoveEnd(moveListener)
        assertPanelHasMoved(panelEntity, initialPosition)
    }

    @Test
    fun movableComponent_mainPanelEntity_customMovable_isMovable() {
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread {
            activity.setContent { CenteredLabel("This is Custom Movable MainPanel") }
        }

        // 2. Create the MovableComponent (custom movable) and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val movableComponent =
            MovableComponent.createCustomMovable(
                session = sessionRule.session,
                scaleInZ = false,
                executor = null,
                entityMoveListener = moveListener,
            )
        assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()

        // 3. Wait for the system to report the main panel as movable before sampling the pose.
        val mainPanelNode = awaitMainPanelNode()
        val initialPosition = mainPanelEntity.getPose(Space.ACTIVITY).translation

        // 4. Grab the main panel by its move affordance and drag it.
        mainPanelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that every move callback fired and that the main
        // panel pose was updated by the listener.
        awaitMoveEnd(moveListener)
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveUpdateCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()
        assertPanelHasMoved(mainPanelEntity, initialPosition)
    }

    @Test
    fun movableComponent_panelEntity_customMovable_isMovable() {
        val panelName = "customMovablePanel"
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)

        val panelEntity = sessionRule.onMainThread {
            // 1. Create the PanelEntity under test, then hide the main panel so it does not
            // interfere with the spatial panel test.
            val panel = createSpatialPanel(panelName, "This is Custom Movable Spatial Panel")
            session.scene.mainPanelEntity.setEnabled(false)

            // 2. Create the MovableComponent (custom movable) and attach it to the panel entity.
            val movableComponent =
                MovableComponent.createCustomMovable(
                    session = session,
                    scaleInZ = false,
                    executor = null,
                    entityMoveListener = moveListener,
                )
            assertThat(panel.addComponent(movableComponent)).isTrue()
            panel
        }

        // 3. Wait for the system to report the panel as movable before sampling the pose.
        val panelNode = awaitPanelNode(panelName)
        val initialPosition = panelEntity.getPose(Space.ACTIVITY).translation

        // 4. Grab the panel by its move affordance and drag it.
        panelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that every move callback fired and that the panel
        // entity pose was updated by the listener.
        awaitMoveEnd(moveListener)
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveUpdateCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()
        assertPanelHasMoved(panelEntity, initialPosition)
    }

    @Test
    fun movableComponent_panelEntity_scaleInZTrue_scalesPanelAlongDepthMove() {
        val panelName = "scaleInZTruePanel"
        val session = sessionRule.session
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(panelName, "ScaleInZ True Panel")
        }
        val component =
            MovableComponent.createSystemMovable(session, scaleInZ = true).apply {
                size = panel.size.to3d()
                addMoveListener(moveListener)
            }
        assertThat(panel.addComponent(component)).isTrue()

        val panelNode = awaitPanelNode(panelName)
        val initialPose = panel.getPose(Space.ACTIVITY)
        val initialScale = panel.getScale(Space.ACTIVITY)

        panelNode.performSpatialInteraction { moveNodeBy(DEPTH_MOVE_AWAY_DELTA) }

        awaitMoveEnd(moveListener)
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveUpdateCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()

        waitUntil("panel to move away in -Z and scale up", MOVE_TIMEOUT) {
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
            createSpatialPanel(panelName, "ScaleInZ False Panel")
        }
        val component =
            MovableComponent.createSystemMovable(session, scaleInZ = false).apply {
                size = panel.size.to3d()
                addMoveListener(moveListener)
            }
        assertThat(panel.addComponent(component)).isTrue()

        val panelNode = awaitPanelNode(panelName)
        val initialPose = panel.getPose(Space.ACTIVITY)
        val initialScale = panel.getScale(Space.ACTIVITY)

        panelNode.performSpatialInteraction { moveNodeBy(DEPTH_MOVE_AWAY_DELTA) }

        awaitMoveEnd(moveListener)
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()

        waitUntil("panel to move away in -Z", MOVE_TIMEOUT) {
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

        awaitMoveEnd(moveListener)
        awaitLatch(
            inputListener.upLatch,
            "InteractableComponent did not receive Action.UP for GltfModelEntity",
        )

        waitUntil("GltfModelEntity to move", MOVE_TIMEOUT) {
            Vector3.distance(entity.getPose(Space.ACTIVITY).translation, initialPose.translation) >=
                MIN_GLTF_MOVE_DISTANCE_METERS
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

        awaitMoveEnd(moveListener)
        awaitLatch(inputListener.upLatch, "InteractableComponent did not receive Action.UP")
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveUpdateCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()

        waitUntil("Custom movable GltfModelEntity to move", MOVE_TIMEOUT) {
            Vector3.distance(entity.getPose(Space.ACTIVITY).translation, initialPose.translation) >=
                MIN_GLTF_MOVE_DISTANCE_METERS
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
                        name = parentName,
                        text = "Parent Movable Panel",
                        dimensions = HIERARCHY_PANEL_DIMENSIONS,
                        pose = HIERARCHY_PARENT_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdChild =
                    createSpatialPanel(
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

        val parentNode = awaitPanelNode(parentName)
        awaitPanelNode(childName)

        val initialParentActivityPose = parent.getPose(Space.ACTIVITY)
        val initialChildActivityPose = child.getPose(Space.ACTIVITY)
        val initialChildLocalPose = child.getPose(Space.PARENT)

        parentNode.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }

        awaitMoveEnd(parentMoveListener)

        waitUntil("parent and child to move together in ActivitySpace", MOVE_TIMEOUT) {
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
                        name = parentName,
                        text = "Parent Panel",
                        dimensions = HIERARCHY_PANEL_DIMENSIONS,
                        pose = HIERARCHY_PARENT_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdChild =
                    createSpatialPanel(
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

        awaitPanelNode(parentName)
        val initialChildNode =
            awaitPanelNode(
                panelName = childName,
                description = "movable & resizable spatial panel node named '$childName'",
            ) {
                it.isMovable && it.isResizable
            }

        val initialParentPose = parent.getPose(Space.ACTIVITY)
        val initialChildPose = child.getPose(Space.ACTIVITY)
        val initialChildSize = child.sizeInPixels

        // Step 1: Move child panel independently inward and upward inside Aura FOV; parent must
        // stay stationary.
        initialChildNode.performSpatialInteraction { moveNodeBy(CHILD_INWARD_MOVE_DELTA) }

        awaitMoveEnd(childMoveListener)

        waitUntil("child panel to move independently", MOVE_TIMEOUT) {
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
        // supports move + resize).
        val refreshedChildNode =
            awaitPanelNode(
                panelName = childName,
                description = "moved child panel '$childName' ready for resize",
            ) { node ->
                node.isMovable &&
                    node.isResizable &&
                    Vector3.distance(node.position, initialChildNode.position) > 0.05f
            }
        refreshedChildNode.performSpatialInteraction {
            resizeNodeBy(RESIZE_DELTA, alignment = SpatialAlignment.BottomRight)
        }

        awaitLatch(childResizeEnded, "Child resize listener did not receive ResizeState.END")

        waitUntil("child panel sizeInPixels to grow", MOVE_TIMEOUT) {
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
                        name = backstopName,
                        text = "Backstop",
                        dimensions = BACKSTOP_PANEL_DIMENSIONS,
                        pose = BACKSTOP_PANEL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdTarget =
                    createSpatialPanel(
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

        awaitPanelNode(
            panelName = backstopName,
            description = "spatial panel node named '$backstopName' with known extents",
        ) {
            true
        }
        val movableNode = awaitPanelNode(targetName)
        val initialPose = target.getPose(Space.ACTIVITY)

        // Verify moving works before removing MovableComponent.
        movableNode.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }
        awaitMoveEnd(moveListener)
        waitUntil("panel to move before removing MovableComponent", MOVE_TIMEOUT) {
            Vector3.distance(target.getPose(Space.ACTIVITY).translation, initialPose.translation) >=
                MIN_SMALL_MOVE_DISTANCE_METERS
        }

        target.removeComponent(movableComponent)

        val nonMovableNode =
            checkNotNull(
                waitForSpatialScene(
                    description = "panel without MovableComponent or DragBar",
                    timeout = SCENE_TIMEOUT,
                    pollInterval = SCENE_POLL_INTERVAL,
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

        awaitLatch(
            backstopInputListener.upLatch,
            "Backstop did not receive Action.UP arrival anchor after DragBar removal",
        )
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
                        name = backstopName,
                        text = "Backstop",
                        dimensions = BACKSTOP_PANEL_DIMENSIONS,
                        pose = BACKSTOP_PANEL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                val createdTarget =
                    createSpatialPanel(
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

        awaitPanelNode(
            panelName = backstopName,
            description = "spatial panel node named '$backstopName' with known extents",
        ) {
            true
        }
        val movableNodeSnapshot = awaitPanelNode(targetName)
        val initialPose = target.getPose(Space.ACTIVITY)

        target.setEnabled(false)

        waitForSpatialScene(
            description = "disabled panel hidden from CPM",
            timeout = SCENE_TIMEOUT,
            pollInterval = SCENE_POLL_INTERVAL,
            select = { scene ->
                scene.findTaskByActivity(sessionRule.activity)?.findNode {
                    it.name == targetName && it.isEmbeddedWindowLeash && it.isVisible
                }
            },
            until = { it == null },
        )

        backstopInputListener.resetEventCountsAndLatches()
        movableNodeSnapshot.performSpatialInteraction { moveNodeBy(SMALL_MOVE_DELTA) }

        awaitLatch(
            backstopInputListener.upLatch,
            "Backstop did not receive Action.UP when dragging disabled panel location",
        )
        // Short settle window after confirmed Action.UP arrival anchor to catch any late drift.
        assertThat(moveListener.moveStartedLatch.await(NO_EFFECT_SETTLE_MS, TimeUnit.MILLISECONDS))
            .isFalse()

        assertThat(moveListener.moveStartCount.get()).isEqualTo(0)
        assertPose(target.getPose(Space.ACTIVITY), initialPose, POSE_TOLERANCE)
    }

    /**
     * Creates a [PanelEntity] named [name] under [parent] (defaulting to
     * `session.scene.activitySpace`) that hosts a [ComposeView] displaying [text].
     *
     * Must be called on the main thread, i.e. inside `sessionRule.onMainThread { }`.
     */
    private fun XrSessionScope<*>.createSpatialPanel(
        name: String,
        text: String,
        dimensions: FloatSize2d = PANEL_DIMENSIONS,
        pose: Pose = PANEL_POSE,
        parent: Entity? = null,
    ): PanelEntity {
        val panelContentView =
            ComposeView(activity).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { CenteredLabel(text) }
            }

        val panelEntity =
            PanelEntity.create(
                session = session,
                view = panelContentView,
                dimensions = dimensions,
                name = name,
                pose = pose,
                parent = parent ?: session.scene.activitySpace,
            )

        // The panel content is hosted in an embedded window that does not inherit the activity's
        // ViewTree owners, so the ComposeView would fail to compose without them. PanelEntity wraps
        // a non-FrameLayout view in a new FrameLayout, so the owners have to be set on that root.
        val parentView: View = panelContentView.parent as? View ?: panelContentView

        parentView.setViewTreeLifecycleOwner(activity)
        parentView.setViewTreeViewModelStoreOwner(activity)
        parentView.setViewTreeSavedStateRegistryOwner(activity)

        return panelEntity
    }

    /**
     * Polls the spatial scene until the visible node backing the main panel is reported as movable
     * with known extents, and returns that node.
     *
     * Must be called from the instrumentation thread.
     */
    private fun awaitMainPanelNode(): SnapshotXrSpace =
        awaitSpatialNode(
            description = "movable $MAIN_PANEL_NODE_NAME node",
            selectNode = { task -> task.findNode { it.name == MAIN_PANEL_NODE_NAME } },
            predicate = { it.isMovable },
        )

    /**
     * Polls the spatial scene until the visible embedded window leash of the spatial panel named
     * [panelName] has known extents and satisfies [predicate] (defaulting to
     * [SnapshotXrSpace.isMovable]), and returns that node.
     *
     * Must be called from the instrumentation thread.
     */
    private fun awaitPanelNode(
        panelName: String,
        description: String = "movable spatial panel node named '$panelName'",
        predicate: (SnapshotXrSpace) -> Boolean = { it.isMovable },
    ): SnapshotXrSpace =
        awaitSpatialNode(
            description = description,
            selectNode = { task ->
                task.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
            },
            predicate = predicate,
        )

    /**
     * Waits for the UI to sync, then polls the activity task in the spatial scene until
     * [selectNode] returns a visible node with known extents that satisfies [predicate].
     *
     * The pointer aims at move or resize affordances based on node extents, so extents must be
     * populated before interacting. Must be called from the instrumentation thread since
     * `waitForIdleSync` and `waitForSpatialScene` block.
     */
    private fun awaitSpatialNode(
        description: String,
        selectNode: (SnapshotXrSpace) -> SnapshotXrSpace?,
        predicate: (SnapshotXrSpace) -> Boolean = { true },
    ): SnapshotXrSpace {
        // Let pending layout and component transactions submit before polling CPM, so the caller
        // samples its "before" state after the UI has synced.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return checkNotNull(
            waitForSpatialScene(
                description = description,
                timeout = SCENE_TIMEOUT,
                pollInterval = SCENE_POLL_INTERVAL,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.let(selectNode)
                },
                until = { node ->
                    node != null && node.isVisible && node.worldExtents != null && predicate(node)
                },
            )
        )
    }

    private fun waitForGltfSubspaceNode(): SnapshotXrSpace =
        checkNotNull(
            waitForSpatialScene(
                description = "visible glTF subspace node",
                timeout = SCENE_TIMEOUT,
                pollInterval = SCENE_POLL_INTERVAL,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name.startsWith(GLTF_ENTITY_SUBSPACE_PREFIX)
                    }
                },
                until = { node -> node != null && node.isVisible },
            )
        )

    private fun wakeGltfFootprintIntoIdle(
        gltfSubspaceNode: SnapshotXrSpace,
        inputListener: RecordingInputEventListener,
    ) {
        waitUntil("glTF initial hover to enter Opening", timeout = MOVE_TIMEOUT) {
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

    /**
     * Blocks the instrumentation thread until [listener] has reported the end of the move, and
     * fails on timeout.
     */
    private fun awaitMoveEnd(listener: RecordingMoveListener) {
        awaitLatch(listener.moveEnded, "Timed out waiting for the move to end")
    }

    /**
     * Blocks the instrumentation thread until [latch] counts down, failing with [failureMessage].
     */
    private fun awaitLatch(latch: CountDownLatch, failureMessage: String) {
        val signaled = latch.await(MOVE_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        assertWithMessage(failureMessage).that(signaled).isTrue()
    }

    /**
     * Polls until [panel] has moved at least [MIN_MOVE_DISTANCE_METERS] from [initialPosition] in
     * ActivitySpace.
     */
    private fun assertPanelHasMoved(panel: PanelEntity, initialPosition: Vector3) {
        waitUntil(
            description = "the panel pose to move beyond $MIN_MOVE_DISTANCE_METERS m",
            timeout = SCENE_TIMEOUT,
            pollInterval = SCENE_POLL_INTERVAL,
        ) {
            (panel.getPose(Space.ACTIVITY).translation - initialPosition).length >
                MIN_MOVE_DISTANCE_METERS
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
        /** Timeout applied when waiting for move or interaction callbacks and pose updates. */
        val MOVE_TIMEOUT = 5.seconds

        /** Timeout applied when polling the spatial scene. */
        val SCENE_TIMEOUT = 10.seconds

        /** Interval between two consecutive captures of the spatial scene. */
        val SCENE_POLL_INTERVAL = 100.milliseconds

        /** Settle window after arrival-anchor UP before verifying no delayed move occurred. */
        const val NO_EFFECT_SETTLE_MS = 300L

        const val BOUNDING_BOX_MODEL_ASSET = "BoundingBoxBlue.glb"
        const val GLTF_ENTITY_SUBSPACE_PREFIX = "gltf_entity_subspace_"

        val GLTF_CENTER = Vector3.Zero
        val GLTF_HALF_EXTENTS = FloatSize3d(0.5f, 0.5f, 0.5f)

        /** Size of the spatial panels created by `createSpatialPanel`. */
        val PANEL_DIMENSIONS = FloatSize2d(1.28f, 0.96f)
        val HIERARCHY_PANEL_DIMENSIONS = FloatSize2d(0.50f, 0.40f)
        val REMOVAL_PANEL_DIMENSIONS = FloatSize2d(1.0f, 0.7f)
        val BACKSTOP_PANEL_DIMENSIONS = FloatSize2d(1.6f, 1.4f)

        /** Initial pose of the spatial panels created by `createSpatialPanel`. */
        val PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))
        val BACKSTOP_PANEL_POSE = Pose(Vector3(0f, 0f, -0.3f))
        val HIERARCHY_PARENT_POSE = Pose(Vector3(-0.28f, 0f, 0.1f))
        val HIERARCHY_CHILD_LOCAL_POSE = Pose(Vector3(0.56f, 0f, 0f))

        /** Drag applied to the target node, expressed in the node's local space. */
        val MOVE_DELTA = Vector3(0.25f, 0.16f, 0f)
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

        /**
         * Lower bound for the distance the entity is expected to travel in ActivitySpace.
         *
         * [MOVE_DELTA] is expressed in the node's local space, so the resulting distance in
         * ActivitySpace also depends on the scale of the node while it is being dragged. The bound
         * is therefore kept well below `MOVE_DELTA.length` (~0.297m) rather than asserting an exact
         * distance.
         */
        val MIN_MOVE_DISTANCE_METERS = 0.5f * MOVE_DELTA.length
        const val MIN_GLTF_MOVE_DISTANCE_METERS = 0.25f
        const val MIN_SMALL_MOVE_DISTANCE_METERS = 0.12f
        const val MIN_DEPTH_MOVE_METERS = 0.25f
        const val MIN_SCALE_INCREASE = 0.05f
        const val SCALE_TOLERANCE = 1e-3f
        const val POSE_TOLERANCE = 1e-3f
    }
}

/**
 * An [EntityMoveListener] that records which callbacks were invoked, and counts down [moveEnded]
 * once the move has ended.
 *
 * When `applyMoveToEntity` is `true`, the listener writes the reported transform back to the entity
 * (required for custom movable components, whereas system movable components are moved by the
 * system itself).
 *
 * @param applyMoveToEntity whether the reported transform should be written back to the entity
 */
private class RecordingMoveListener(private val applyMoveToEntity: Boolean) : EntityMoveListener {
    private val moveStartFlag = AtomicBoolean(false)
    private val moveUpdateFlag = AtomicBoolean(false)
    private val moveEndFlag = AtomicBoolean(false)

    val moveStartCount = AtomicInteger(0)
    val moveEndCount = AtomicInteger(0)

    @Volatile var lastEndScale: Float = 1f
    @Volatile var moveStartedLatch = CountDownLatch(1)
    @Volatile var moveEnded = CountDownLatch(1)

    val onMoveStartCalled: Boolean
        get() = moveStartFlag.get()

    val onMoveUpdateCalled: Boolean
        get() = moveUpdateFlag.get()

    val onMoveEndCalled: Boolean
        get() = moveEndFlag.get()

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
        moveStartFlag.set(true)
        moveStartCount.incrementAndGet()
        moveStartedLatch.countDown()
    }

    override fun onMoveUpdate(
        entity: Entity,
        currentInputRay: Ray,
        currentPose: Pose,
        currentScale: Float,
    ) {
        moveUpdateFlag.set(true)
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
        lastEndScale = finalScale
        moveEndFlag.set(true)
        moveEndCount.incrementAndGet()
        moveEnded.countDown()
    }
}

/** The content shown by the main panel and by the spatial panels created by this test. */
@Composable
private fun CenteredLabel(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 32.sp, color = Color.Black)
    }
}
