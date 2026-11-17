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

import android.app.Activity
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
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
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.SpatialDeviceType
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialPointerType
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.Timeout
import org.junit.runner.RunWith

// TODO(b/569762387): Add integration test coverage for subgraph event propagation from descendant
// entities that do not have their own InteractableComponent attached.
/**
 * Integration tests for [InteractableComponent], driven by injected raycast input.
 *
 * Covers baseline input delivery on [androidx.xr.scenecore.MainPanelEntity], [PanelEntity], and
 * [ActivityPanelEntity], attach/detach/reattach lifecycles on [PanelEntity] and [GltfModelEntity],
 * full hover/click sequences, `LEFT`/`RIGHT` pointer handedness isolation and hit coordinates,
 * [Entity.setEnabled] suppression/restoration, and coexistence with [MovableComponent] and
 * [ResizableComponent].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class InteractableComponentTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    // Ensure embedded activities are finished outside the Timeout rule window before sessionRule
    // destroys the host activity (b/584286729).
    @get:Rule(order = 1)
    val embeddedActivityCleanupRule =
        object : ExternalResource() {
            override fun after() {
                finishActivitiesOtherThan(sessionRule.activity)
            }
        }

    @get:Rule(order = 2) val timeout: Timeout = Timeout.seconds(30)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun interactableComponent_mainPanelEntity_receivesInputEvents() {
        val inputListener = RecordingInputEventListener()

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread {
            activity.setContent { CenteredLabel("This is Interactable MainPanel") }
        }

        // 2. Create the InteractableComponent and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val interactableComponent =
            InteractableComponent.create(
                session = sessionRule.session,
                inputEventListener = inputListener,
            )
        assertThat(mainPanelEntity.addComponent(interactableComponent)).isTrue()

        // Wait for the main panel node to be ready to receive input in the spatial scene.
        val mainPanelNode =
            waitTaskNode("main panel") { task ->
                task.findNode { it.name == MAIN_PANEL_NODE_NAME }
            }

        // 3. Aim at the main panel, press, drag across its center, and release.
        performDragInteraction(mainPanelNode)

        // 4. Wait for the input sequence to complete and verify the received InputEvents.
        inputListener.waitForActions(
            description = "main panel hover, press, drag, and release",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.DOWN,
            InputEvent.Action.MOVE,
            InputEvent.Action.UP,
        )
        assertInputEvents(inputListener.events, expectedEntity = mainPanelEntity)
    }

    @Test
    fun interactableComponent_panelEntity_receivesInputEvents() {
        val panelName = "interactablePanel"
        val inputListener = RecordingInputEventListener()
        val session = sessionRule.session

        // 1. Hide the main panel so it does not interfere with the spatial panel test, then create
        // the PanelEntity under test.
        session.scene.mainPanelEntity.setEnabled(false)
        val panelEntity = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, text = "This is Interactable Spatial Panel")
        }

        // 2. Create the InteractableComponent and attach it to the panel entity.
        val interactableComponent =
            InteractableComponent.create(
                session = session,
                inputEventListener = inputListener,
            )
        assertThat(panelEntity.addComponent(interactableComponent)).isTrue()

        // Wait for the spatial panel node to be ready to receive input in the spatial scene.
        val panelNode =
            waitTaskNode("spatial panel '$panelName'") { task ->
                task.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
            }

        // 3. Aim at the panel, press, drag across its center, and release.
        performDragInteraction(panelNode)

        // 4. Wait for the input sequence to complete and verify the received InputEvents.
        inputListener.waitForActions(
            description = "panel hover, press, drag, and release",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.DOWN,
            InputEvent.Action.MOVE,
            InputEvent.Action.UP,
        )
        assertInputEvents(inputListener.events, expectedEntity = panelEntity)
    }

    @Test
    fun interactableComponent_activityPanelEntity_receivesInputEvents() {
        val panelName = "interactableActivityPanel"
        val inputListener = RecordingInputEventListener()
        val session = sessionRule.session

        // Embedding an activity in a panel requires the EMBED_ACTIVITY capability.
        assertThat(session.scene.spatialCapabilities).contains(SpatialCapability.EMBED_ACTIVITY)

        session.scene.mainPanelEntity.setEnabled(false)
        val activityPanelEntity = sessionRule.onMainThread {
            val activityPanel =
                ActivityPanelEntity.create(
                    session = session,
                    pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                    name = panelName,
                    pose = PANEL_POSE,
                    parent = session.scene.activitySpace,
                )
            // Unlike the intent that XrSessionRule's ActivityScenarioRule launches the activity
            // under test with, this intent has no action and no category, so ActivityScenario
            // does not mistake the embedded activity for the activity under test.
            activityPanel.startActivity(Intent(activity, ComponentActivity::class.java))
            activityPanel
        }

        // Create the InteractableComponent and attach it to the activity panel entity.
        val interactableComponent =
            InteractableComponent.create(
                session = session,
                inputEventListener = inputListener,
            )
        assertThat(activityPanelEntity.addComponent(interactableComponent)).isTrue()

        // Wait for the embedded activity to be shown and ready to receive input in the spatial
        // scene.
        val panelNode =
            waitTaskNode("activity panel '$panelName'") { task ->
                task.findNode { it.name == panelName }
            }

        // Aim at the activity panel, press, drag across its center, and release.
        performDragInteraction(panelNode)

        // Wait for the input sequence to complete and verify the received InputEvents.
        inputListener.waitForActions(
            description = "activity panel hover, press, drag, and release",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.DOWN,
            InputEvent.Action.MOVE,
            InputEvent.Action.UP,
        )
        assertInputEvents(inputListener.events, expectedEntity = activityPanelEntity)
    }

    @Test
    fun interactableComponent_panelEntity_toggleAttachDetachReattach_controlsEventDelivery() {
        val foregroundListener = RecordingInputEventListener()
        val anchorListener = RecordingInputEventListener()
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        val (fgPanel, anchorPanel) =
            sessionRule.onMainThread {
                val fg =
                    createSpatialPanel(
                        name = FOREGROUND_PANEL_NAME,
                        dimensions = PANEL_DIMENSIONS,
                        pose = FOREGROUND_POSE,
                    )
                val anchor =
                    createSpatialPanel(
                        name = ANCHOR_PANEL_NAME,
                        dimensions = ANCHOR_PANEL_DIMENSIONS,
                        pose = ANCHOR_POSE,
                    )
                fg to anchor
            }

        val interactable = InteractableComponent.create(session, foregroundListener)
        val anchorInteractable = InteractableComponent.create(session, anchorListener)
        assertThat(fgPanel.addComponent(interactable)).isTrue()
        assertThat(anchorPanel.addComponent(anchorInteractable)).isTrue()
        assertThat(fgPanel.getComponentsOfType(InteractableComponent::class.java))
            .containsExactly(interactable)

        val foregroundNode = waitVisiblePanelNode(FOREGROUND_PANEL_NAME)
        val anchorNode = waitVisiblePanelNode(ANCHOR_PANEL_NAME)

        // Phase 1: Attached component receives full hover + click lifecycle events.
        foregroundNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            movePointerBy(Vector3(0.05f, 0.04f, 0f))
            movePointerBy(Vector3(0.05f, 0.04f, 0f))
            select()
            // Sweep off the left edge (away from anchorPanel on the right) to trigger HOVER_EXIT
            // cleanly.
            movePointerBy(Vector3(-(PANEL_DIMENSIONS.width + 0.5f), 0f, 0f))
        }
        foregroundListener.waitForActions(
            description = "attached panel hover and click lifecycle",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.HOVER_MOVE,
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
            InputEvent.Action.HOVER_EXIT,
        )
        val phase1PanelEvents = foregroundListener.events
        assertLifecycleOrder(events = phase1PanelEvents, hasHoverMove = true)
        assertInBoundsHitEvents(
            events = phase1PanelEvents,
            expectedEntity = fgPanel,
            expectedPlaneZ = FOREGROUND_POSE.translation.z,
        )

        // Phase 2: Detach InteractableComponent and verify no events arrive on the panel, gated by
        // an arrival-anchor click on the adjacent panel plus settle window.
        fgPanel.removeComponent(interactable)
        assertThat(fgPanel.getComponentsOfType(InteractableComponent::class.java)).isEmpty()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        foregroundListener.clear()
        anchorListener.clear()
        val unexpectedAfterDetach = foregroundListener.armNextEventLatch()

        foregroundNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        anchorNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        anchorListener.waitForActions(
            description = "arrival anchor click after panel detach",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        waitNegativeSettleWindow(unexpectedAfterDetach)
        assertWithMessage("Expected no InputEvents after detaching InteractableComponent")
            .that(foregroundListener.events)
            .isEmpty()

        // Phase 3: Re-attach the same InteractableComponent instance and verify delivery resumes.
        assertThat(fgPanel.addComponent(interactable)).isTrue()
        assertThat(fgPanel.getComponentsOfType(InteractableComponent::class.java))
            .containsExactly(interactable)

        foregroundListener.clear()
        foregroundNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        foregroundListener.waitForActions(
            description = "re-attached panel click",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        assertInBoundsHitEvents(
            events = foregroundListener.events,
            expectedEntity = fgPanel,
            expectedPlaneZ = FOREGROUND_POSE.translation.z,
        )
    }

    @Test
    fun interactableComponent_gltfModelEntity_hoverClickAndToggleAttachDetachReattach() {
        val gltfListener = RecordingInputEventListener()
        val anchorListener = RecordingInputEventListener()
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        val (modelEntity, anchorPanel) =
            sessionRule.onMainThread {
                val model = GltfModel.create(session, Paths.get("models", BOUNDING_BOX_MODEL_ASSET))
                val entity =
                    GltfModelEntity.create(
                        session = session,
                        model = model,
                        pose = GLTF_POSE,
                        parent = session.scene.activitySpace,
                    )
                val anchor =
                    createSpatialPanel(
                        name = ANCHOR_PANEL_NAME,
                        dimensions = ANCHOR_PANEL_DIMENSIONS,
                        pose = ANCHOR_POSE_FAR,
                    )
                entity to anchor
            }

        val interactable = InteractableComponent.create(session, gltfListener)
        // TODO(b/586576069): Remove main-thread wrapper once GltfModelEntity.addComponent runs
        // Impress JNI nSetGltfModelColliderEnabled on the main thread.
        val modelAdded = sessionRule.onMainThread { modelEntity.addComponent(interactable) }
        assertThat(modelAdded).isTrue()
        assertThat(anchorPanel.addComponent(InteractableComponent.create(session, anchorListener)))
            .isTrue()

        val gltfNode = waitVisibleSubspaceEntityNode(GLTF_SUBSPACE_PREFIX)
        val anchorNode = waitVisiblePanelNode(ANCHOR_PANEL_NAME)

        // Phase 1: Full hover lifecycle (HOVER_ENTER -> HOVER_MOVE -> HOVER_EXIT) and click
        // (DOWN -> UP) on GltfModelEntity. BoundingBoxBlue.glb is a solid double-sided unit cube
        // centered at local (0, 0, 0), so offsets within halfExtents reliably intersect.
        gltfNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Right,
        ) {
            pointAt(offset = GLTF_BOUNDS_CENTER)
            movePointerBy(Vector3(0.05f, 0.03f, 0f))
            movePointerBy(Vector3(0.05f, 0.03f, 0f))
            select()
            // Sweep off the left edge (away from anchorPanel on the right) to emit HOVER_EXIT.
            movePointerBy(Vector3(-(GLTF_HALF_EXTENT + 0.6f), 0f, 0f))
        }

        gltfListener.waitForActions(
            description = "glTF hover and click lifecycle",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.HOVER_MOVE,
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
            InputEvent.Action.HOVER_EXIT,
        )

        val phase1Events = gltfListener.events
        assertLifecycleOrder(events = phase1Events, hasHoverMove = true)
        assertInBoundsHitEvents(events = phase1Events, expectedEntity = modelEntity)

        // Phase 2: Detach InteractableComponent (disables glTF collider) and confirm no events are
        // delivered after arrival anchor + settle window.
        // TODO(b/586576069): Remove main-thread wrapper once GltfModelEntity.removeComponent runs
        // Impress JNI nSetGltfModelColliderEnabled on the main thread.
        sessionRule.onMainThread { modelEntity.removeComponent(interactable) }

        gltfListener.clear()
        anchorListener.clear()
        val unexpectedAfterDetach = gltfListener.armNextEventLatch()

        gltfNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Right,
        ) {
            pointAt(offset = GLTF_BOUNDS_CENTER)
            select()
        }
        anchorNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Right,
        ) {
            pointAt()
            select()
        }
        anchorListener.waitForActions(
            description = "arrival anchor click after glTF detach",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        waitNegativeSettleWindow(unexpectedAfterDetach)
        assertWithMessage("Expected no InputEvents after detaching InteractableComponent from glTF")
            .that(gltfListener.events)
            .isEmpty()

        // Phase 3: Re-attach the same InteractableComponent (re-enables glTF collider) and confirm
        // click delivery resumes.
        // TODO(b/586576069): Remove main-thread wrapper once GltfModelEntity.addComponent runs
        // Impress JNI nSetGltfModelColliderEnabled on the main thread.
        val modelReadded = sessionRule.onMainThread { modelEntity.addComponent(interactable) }
        assertThat(modelReadded).isTrue()

        gltfListener.clear()
        gltfNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Right,
        ) {
            pointAt(offset = GLTF_BOUNDS_CENTER)
            select()
            movePointerBy(Vector3(-(GLTF_HALF_EXTENT + 0.6f), 0f, 0f))
        }
        gltfListener.waitForActions(
            description = "re-attached glTF click and exit",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
            InputEvent.Action.HOVER_EXIT,
        )
        assertInBoundsHitEvents(events = gltfListener.events, expectedEntity = modelEntity)
    }

    @Test
    fun interactableComponent_multiplePointers_distinguishesLeftAndRightHandednessAndHitCoordinates() {
        val inputListener = RecordingInputEventListener()
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        val createdPanel = sessionRule.onMainThread {
            createSpatialPanel(
                name = MULTI_POINTER_PANEL_NAME,
                dimensions = MULTI_POINTER_PANEL_DIMENSIONS,
                pose = CENTER_PANEL_POSE,
            )
        }
        assertThat(createdPanel.addComponent(InteractableComponent.create(session, inputListener)))
            .isTrue()
        val panelNode = waitVisiblePanelNode(MULTI_POINTER_PANEL_NAME)

        // Phase 1: Hands + Left pointer aimed at the left half of the panel. Sweep off-panel after
        // select() so post-UP HOVER_EXIT completes before Phase 2.
        panelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt(offset = LEFT_POINTER_OFFSET)
            select()
            movePointerBy(Vector3(-(MULTI_POINTER_PANEL_DIMENSIONS.width + 0.5f), 0f, 0f))
        }
        inputListener.waitForActions(
            description = "Hands/Left DOWN, UP, and post-UP HOVER_EXIT",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
            InputEvent.Action.HOVER_EXIT,
        )
        val leftEvents = inputListener.events
        assertWithMessage(
                "Expected only Hands events in Phase 1; received=${inputListener.formatEvents()}"
            )
            .that(leftEvents.map { it.source }.distinct())
            .containsExactly(InputEvent.Source.HANDS)
        assertWithMessage(
                "Expected only LEFT pointer events in Phase 1; received=${inputListener.formatEvents()}"
            )
            .that(leftEvents.map { it.pointerType }.distinct())
            .containsExactly(InputEvent.Pointer.LEFT)
        assertInBoundsHitEvents(
            events = leftEvents,
            expectedEntity = createdPanel,
            expectedPlaneZ = CENTER_PANEL_POSE.translation.z,
        )
        val leftDownHit =
            leftEvents.first { it.action == InputEvent.Action.DOWN }.hitInfoList.first()
        assertVector3(
            actual = checkNotNull(leftDownHit.hitPosition),
            expected = CENTER_PANEL_POSE.translation + LEFT_POINTER_OFFSET,
            epsilon = HIT_POSITION_EPSILON,
        )

        // Phase 2: Hands + Right pointer aimed at the right half of the panel.
        inputListener.clear()
        panelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Right,
        ) {
            pointAt(offset = RIGHT_POINTER_OFFSET)
            select()
            movePointerBy(Vector3(MULTI_POINTER_PANEL_DIMENSIONS.width + 0.5f, 0f, 0f))
        }
        inputListener.waitForActions(
            description = "Hands/Right DOWN, UP, and post-UP HOVER_EXIT",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
            InputEvent.Action.HOVER_EXIT,
        )
        val rightEvents = inputListener.events
        assertWithMessage(
                "Expected only Hands events in Phase 2; received=${inputListener.formatEvents()}"
            )
            .that(rightEvents.map { it.source }.distinct())
            .containsExactly(InputEvent.Source.HANDS)
        assertWithMessage(
                "Expected only RIGHT pointer events in Phase 2; received=${inputListener.formatEvents()}"
            )
            .that(rightEvents.map { it.pointerType }.distinct())
            .containsExactly(InputEvent.Pointer.RIGHT)
        assertInBoundsHitEvents(
            events = rightEvents,
            expectedEntity = createdPanel,
            expectedPlaneZ = CENTER_PANEL_POSE.translation.z,
        )
        val rightDownHit =
            rightEvents.first { it.action == InputEvent.Action.DOWN }.hitInfoList.first()
        assertVector3(
            actual = checkNotNull(rightDownHit.hitPosition),
            expected = CENTER_PANEL_POSE.translation + RIGHT_POINTER_OFFSET,
            epsilon = HIT_POSITION_EPSILON,
        )
    }

    @Test
    fun interactableComponent_entitySetEnabled_suppressesAndRestoresInputEvents() {
        val foregroundListener = RecordingInputEventListener()
        val anchorListener = RecordingInputEventListener()
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        val (fgPanel, anchorPanel) =
            sessionRule.onMainThread {
                val fg =
                    createSpatialPanel(
                        name = ENABLE_TOGGLE_PANEL_NAME,
                        dimensions = PANEL_DIMENSIONS,
                        pose = FOREGROUND_POSE,
                    )
                val anchor =
                    createSpatialPanel(
                        name = ANCHOR_PANEL_NAME,
                        dimensions = ANCHOR_PANEL_DIMENSIONS,
                        pose = ANCHOR_POSE,
                    )
                fg to anchor
            }

        assertThat(fgPanel.addComponent(InteractableComponent.create(session, foregroundListener)))
            .isTrue()
        assertThat(anchorPanel.addComponent(InteractableComponent.create(session, anchorListener)))
            .isTrue()

        val enabledPanelNode = waitVisiblePanelNode(ENABLE_TOGGLE_PANEL_NAME)
        val anchorNode = waitVisiblePanelNode(ANCHOR_PANEL_NAME)

        // Phase 1: Enabled entity receives input events.
        enabledPanelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        foregroundListener.waitForActions(
            description = "initially enabled panel click and hover exit",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
            InputEvent.Action.HOVER_EXIT,
        )

        // Phase 2: Disable entity (`setEnabled(false)`), wait for CPM visibility to clear, and
        // verify no events are delivered while disabled.
        fgPanel.setEnabled(false)
        assertThat(fgPanel.isEnabled(includeParents = true)).isFalse()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForSpatialScene(
            description = "panel node '$ENABLE_TOGGLE_PANEL_NAME' hidden in CPM",
            select = { scene ->
                scene.findTaskByActivity(sessionRule.activity)?.findNode {
                    it.name == ENABLE_TOGGLE_PANEL_NAME && it.isEmbeddedWindowLeash && it.isVisible
                }
            },
            until = { it == null },
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        foregroundListener.clear()
        anchorListener.clear()
        val unexpectedWhileDisabled = foregroundListener.armNextEventLatch()

        enabledPanelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        anchorNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        anchorListener.waitForActions(
            description = "arrival anchor click while foreground panel disabled",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        waitNegativeSettleWindow(unexpectedWhileDisabled)
        assertWithMessage(
                "Expected no InputEvents while entity is disabled; received=${foregroundListener.formatEvents()}"
            )
            .that(foregroundListener.events)
            .isEmpty()

        // Phase 3: Re-enable entity (`setEnabled(true)`), wait for CPM visibility, and confirm
        // input event delivery is restored.
        fgPanel.setEnabled(true)
        assertThat(fgPanel.isEnabled(includeParents = true)).isTrue()
        val reenabledPanelNode = waitVisiblePanelNode(ENABLE_TOGGLE_PANEL_NAME)

        foregroundListener.clear()
        reenabledPanelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        foregroundListener.waitForActions(
            description = "re-enabled panel click",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        assertInBoundsHitEvents(
            events = foregroundListener.events,
            expectedEntity = fgPanel,
            expectedPlaneZ = FOREGROUND_POSE.translation.z,
        )
    }

    @Test
    fun interactableComponent_panelWithMovableAndResizable_preservesInputAfterMoveAndResize() {
        val panelListener = RecordingInputEventListener()
        val moveEndedLatch = CountDownLatch(1)
        val resizeEndedLatch = CountDownLatch(1)
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        val createdPanel = sessionRule.onMainThread {
            createSpatialPanel(
                name = COMBINED_PANEL_NAME,
                dimensions = COMBINED_PANEL_DIMENSIONS,
                pose = COMBINED_PANEL_POSE,
            )
        }

        val panelInteractable = InteractableComponent.create(session, panelListener)
        val panelMovable = MovableComponent.createSystemMovable(session, scaleInZ = false)
        panelMovable.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    moveEndedLatch.countDown()
                }
            }
        )
        val panelResizable =
            ResizableComponent.create(
                session = session,
                minimumSize = FloatSize3d(0.4f, 0.4f, 0f),
                maximumSize = FloatSize3d(2.0f, 2.0f, 0f),
            ) { resizeEvent ->
                if (resizeEvent.resizeState == ResizeEvent.ResizeState.END) {
                    createdPanel.size = resizeEvent.newSize.to2d()
                    resizeEndedLatch.countDown()
                }
            }

        assertThat(createdPanel.addComponent(panelInteractable)).isTrue()
        assertThat(createdPanel.addComponent(panelMovable)).isTrue()
        assertThat(createdPanel.addComponent(panelResizable)).isTrue()
        val initialPanelPose = createdPanel.getPose(Space.ACTIVITY)

        val panelNode = waitVisiblePanelNode(COMBINED_PANEL_NAME) { it.isMovable && it.isResizable }

        // Phase 1: Hover + click center of the PanelEntity with Movable + Resizable attached,
        // validating HitInfo (inputEntity, hitPosition, invertible Activity->local transform).
        panelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            movePointerBy(POST_RESIZE_HIT_OFFSET)
            select()
        }
        panelListener.waitForActions(
            description = "combined PanelEntity hover and click",
            InputEvent.Action.HOVER_ENTER,
            InputEvent.Action.HOVER_MOVE,
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        val panelPhase1Events = panelListener.events
        assertInBoundsHitEvents(
            events = panelPhase1Events,
            expectedEntity = createdPanel,
            expectedPlaneZ = initialPanelPose.translation.z,
        )
        val initialDownHit =
            panelPhase1Events.first { it.action == InputEvent.Action.DOWN }.hitInfoList.first()
        val initialHitPosition = checkNotNull(initialDownHit.hitPosition)
        assertVector3(
            actual = initialHitPosition,
            expected = initialPanelPose.translation + POST_RESIZE_HIT_OFFSET,
            epsilon = HIT_POSITION_EPSILON,
        )

        // Phase 2: Move the panel via its MovableComponent affordance and verify subsequent
        // InteractableComponent hits report the updated ActivitySpace position.
        panelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA_X) }
        assertWithMessage("Timed out waiting for MovableComponent onMoveEnd")
            .that(moveEndedLatch.await(INPUT_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS))
            .isTrue()
        waitUntil(description = "moved panel ActivitySpace pose to settle") {
            createdPanel.getPose(Space.ACTIVITY).translation.x >
                initialPanelPose.translation.x + 0.05f
        }
        val movedPose = createdPanel.getPose(Space.ACTIVITY)
        val movedPanelNode =
            waitVisiblePanelNode(COMBINED_PANEL_NAME) {
                (it.position - panelNode.position).length > 0.05f
            }

        panelListener.clear()
        movedPanelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt()
            select()
        }
        panelListener.waitForActions(
            description = "moved combined PanelEntity click",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        val movedEvents = panelListener.events
        assertInBoundsHitEvents(
            events = movedEvents,
            expectedEntity = createdPanel,
            expectedPlaneZ = movedPose.translation.z,
        )
        val movedDownHit =
            movedEvents.first { it.action == InputEvent.Action.DOWN }.hitInfoList.first()
        assertPose(
            actual = movedDownHit.transform.inverse.toPose(),
            expected = movedPose,
            epsilon = HIT_TRANSFORM_EPSILON,
        )
        assertThat(checkNotNull(movedDownHit.hitPosition).x)
            .isGreaterThan(initialHitPosition.x + 0.05f)

        // Phase 3: Resize the panel via its ResizableComponent affordance and verify subsequent
        // InteractableComponent hits still land cleanly with valid ActivitySpace transform/hitInfo.
        movedPanelNode.performSpatialInteraction {
            resizeNodeBy(delta = RESIZE_DELTA, alignment = SpatialAlignment.BottomRight)
        }
        assertWithMessage("Timed out waiting for ResizableComponent ResizeState.END")
            .that(resizeEndedLatch.await(INPUT_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS))
            .isTrue()
        waitUntil(description = "resized panel size to settle") {
            createdPanel.size.width > COMBINED_PANEL_DIMENSIONS.width + 0.1f &&
                createdPanel.size.height > COMBINED_PANEL_DIMENSIONS.height + 0.05f
        }
        val resizedPose = createdPanel.getPose(Space.ACTIVITY)
        val movedExtents = checkNotNull(movedPanelNode.worldExtents)
        val resizedPanelNode =
            waitVisiblePanelNode(COMBINED_PANEL_NAME) {
                val extents = it.worldExtents ?: return@waitVisiblePanelNode false
                extents.x > movedExtents.x + 0.1f && extents.y > movedExtents.y + 0.05f
            }

        panelListener.clear()
        resizedPanelNode.performSpatialInteraction(
            deviceType = SpatialDeviceType.Hands,
            pointerType = SpatialPointerType.Left,
        ) {
            pointAt(offset = POST_RESIZE_HIT_OFFSET)
            select()
        }
        panelListener.waitForActions(
            description = "resized combined PanelEntity click",
            InputEvent.Action.DOWN,
            InputEvent.Action.UP,
        )
        val resizedEvents = panelListener.events
        assertInBoundsHitEvents(
            events = resizedEvents,
            expectedEntity = createdPanel,
            expectedPlaneZ = resizedPose.translation.z,
        )
        val resizedDownHit =
            resizedEvents.first { it.action == InputEvent.Action.DOWN }.hitInfoList.first()
        assertPose(
            actual = resizedDownHit.transform.inverse.toPose(),
            expected = resizedPose,
            epsilon = HIT_TRANSFORM_EPSILON,
        )
        assertVector3(
            actual = checkNotNull(resizedDownHit.hitPosition),
            expected = resizedPose.translation + POST_RESIZE_HIT_OFFSET,
            epsilon = HIT_POSITION_EPSILON,
        )
    }

    /** Creates a spatial [PanelEntity] backed by a simple centered [TextView]. Must run on Main. */
    private fun createSpatialPanel(
        name: String,
        text: String = name,
        dimensions: FloatSize2d = PANEL_DIMENSIONS,
        pose: Pose = PANEL_POSE,
    ): PanelEntity {
        val textView =
            TextView(sessionRule.activity).apply {
                this.text = text
                gravity = Gravity.CENTER
                setTextColor(AndroidColor.WHITE)
                setBackgroundColor(AndroidColor.DKGRAY)
            }
        return PanelEntity.create(
            session = sessionRule.session,
            view = textView,
            dimensions = dimensions,
            name = name,
            pose = pose,
            parent = sessionRule.session.scene.activitySpace,
        )
    }

    /**
     * Polls the task of [sessionRule]'s activity in the spatial scene until [selectNode] returns a
     * node with known extents, and returns that node.
     *
     * Blocks, so it must be called from the instrumentation thread.
     */
    private fun waitTaskNode(
        description: String,
        selectNode: (SpatialTask) -> SpatialNode?,
    ): SpatialNode {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return checkNotNull(
            waitForSpatialScene(
                description = "an interactable $description",
                timeout = POLL_TIMEOUT,
                pollInterval = POLL_INTERVAL,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.let(selectNode)
                },
                until = { it != null && it.worldExtents != null },
            )
        )
    }

    /**
     * Polls CPM until an embedded panel node named [panelName] is visible and matches [predicate].
     */
    private fun waitVisiblePanelNode(
        panelName: String,
        predicate: (SpatialNode) -> Boolean = { true },
    ): SpatialNode =
        waitTaskNode(description = "panel node '$panelName' visible in CPM") { task ->
            task.findNode {
                it.name == panelName && it.isEmbeddedWindowLeash && it.isVisible && predicate(it)
            }
        }

    /**
     * Polls CPM until a subspace node starting with [subspacePrefix] (`gltf_entity_subspace_*`) is
     * visible and returns its parent entity node.
     */
    private fun waitVisibleSubspaceEntityNode(subspacePrefix: String): SpatialNode {
        return checkNotNull(
            waitForSpatialScene(
                description = "subspace parent node for '$subspacePrefix' visible in CPM",
                select = { scene ->
                    val subspace =
                        scene.findTaskByActivity(sessionRule.activity)?.subspaceNodes?.firstOrNull {
                            it.name.startsWith(subspacePrefix) && it.isVisible
                        }
                    subspace?.parent?.takeIf { it.isVisible }
                },
                until = { it != null },
            )
        )
    }

    /**
     * Aims the pointer ray at [node], presses, drags across its center from [DRAG_START_OFFSET] to
     * [DRAG_END_OFFSET], and releases.
     *
     * Blocks, so it must be called from the instrumentation thread.
     */
    private fun performDragInteraction(node: SpatialNode) {
        node.performSpatialInteraction {
            pointAt(offset = DRAG_START_OFFSET)
            press()
            animate { fraction ->
                pointAt(offset = Vector3.lerp(DRAG_START_OFFSET, DRAG_END_OFFSET, fraction))
            }
            release()
        }
    }

    /**
     * Finishes every activity of this process except [host], and blocks until they are destroyed.
     *
     * An [ActivityPanelEntity] only destroys its embedded activity when the entity itself is
     * destroyed, which happens after [host] has finished. If the embedded activity is still alive
     * when [host] finishes, it becomes the top activity of the task and crashes SystemUI.
     *
     * Must be called from the instrumentation thread before XrSessionRule finishes [host].
     */
    private fun finishActivitiesOtherThan(host: Activity) {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        fun aliveActivities(): List<Activity> =
            Stage.values()
                .filter { it != Stage.DESTROYED }
                .flatMap { monitor.getActivitiesInStage(it) }
                .filter { it !== host }

        val hadOtherActivities = sessionRule.onMainThread {
            aliveActivities().also { activities -> activities.forEach { it.finish() } }.isNotEmpty()
        }
        if (!hadOtherActivities) return
        waitUntil(
            description = "embedded activities to be destroyed",
            timeout = EMBEDDED_ACTIVITY_FINISH_TIMEOUT,
            pollInterval = POLL_INTERVAL,
        ) {
            sessionRule.onMainThread { aliveActivities().isEmpty() }
        }
    }

    /**
     * Waits for an unexpected event over [NEGATIVE_SETTLE_WINDOW_MS], returning early if delivered.
     */
    private fun waitNegativeSettleWindow(unexpectedEventLatch: CountDownLatch) {
        unexpectedEventLatch.await(NEGATIVE_SETTLE_WINDOW_MS, TimeUnit.MILLISECONDS)
    }

    /**
     * Verifies that [events] contain the expected hover/press/drag/release action sequence and that
     * the press, drag and release events report [expectedEntity] as the hit entity.
     */
    private fun assertInputEvents(events: List<InputEvent>, expectedEntity: Entity) {
        val actions = events.map { it.action }
        assertThat(actions)
            .containsAtLeast(
                InputEvent.Action.HOVER_ENTER,
                InputEvent.Action.DOWN,
                InputEvent.Action.MOVE,
                InputEvent.Action.UP,
            )
            .inOrder()

        val downEvent = events.first { it.action == InputEvent.Action.DOWN }
        assertThat(downEvent.hitInfoList).isNotEmpty()
        val hitInfo = downEvent.hitInfoList.first()
        assertThat(hitInfo.inputEntity).isEqualTo(expectedEntity)
        assertThat(hitInfo.hitPosition).isNotNull()

        // The first hit entity is guaranteed to stay the same during DOWN -> MOVE -> UP.
        val pressedEvents = events.filter {
            it.action == InputEvent.Action.MOVE || it.action == InputEvent.Action.UP
        }
        for (event in pressedEvents) {
            assertThat(event.hitInfoList).isNotEmpty()
            assertThat(event.hitInfoList.first().inputEntity).isEqualTo(expectedEntity)
        }
    }

    /**
     * Asserts causal ordering of a full hover + click lifecycle (`HOVER_ENTER` -> `DOWN` -> `UP` ->
     * `HOVER_EXIT`), plus `HOVER_MOVE` strictly within `[HOVER_ENTER, HOVER_EXIT)` when expected.
     */
    private fun assertLifecycleOrder(events: List<InputEvent>, hasHoverMove: Boolean) {
        val enterIndex = events.indexOfFirst { it.action == InputEvent.Action.HOVER_ENTER }
        val downIndex = events.indexOfFirst { it.action == InputEvent.Action.DOWN }
        val upIndex = events.indexOfFirst { it.action == InputEvent.Action.UP }
        val exitIndex = events.indexOfLast { it.action == InputEvent.Action.HOVER_EXIT }

        assertWithMessage("Expected HOVER_ENTER, DOWN, UP, HOVER_EXIT in $events")
            .that(enterIndex)
            .isAtLeast(0)
        assertThat(downIndex).isGreaterThan(enterIndex)
        assertThat(upIndex).isGreaterThan(downIndex)
        assertThat(exitIndex).isGreaterThan(upIndex)

        if (hasHoverMove) {
            val moveIndex = events.indexOfFirst { it.action == InputEvent.Action.HOVER_MOVE }
            assertWithMessage("Expected HOVER_MOVE during hover window in $events")
                .that(moveIndex)
                .isGreaterThan(enterIndex)
            assertThat(moveIndex).isLessThan(exitIndex)
        }
    }

    /**
     * Validates [InputEvent.HitInfo] (`inputEntity`, non-null `hitPosition`, invertible non-zero
     * `transform`, and local/plane Z sanity when [expectedPlaneZ] is known) for all in-bounds hit
     * actions (`HOVER_ENTER`, `HOVER_MOVE`, `DOWN`, `MOVE`, `UP`).
     */
    private fun assertInBoundsHitEvents(
        events: List<InputEvent>,
        expectedEntity: Entity,
        expectedPlaneZ: Float? = null,
    ) {
        val hitEvents = events.filter {
            it.action == InputEvent.Action.HOVER_ENTER ||
                it.action == InputEvent.Action.HOVER_MOVE ||
                it.action == InputEvent.Action.DOWN ||
                it.action == InputEvent.Action.MOVE ||
                it.action == InputEvent.Action.UP
        }
        assertWithMessage("Expected at least one hit InputEvent for $expectedEntity")
            .that(hitEvents)
            .isNotEmpty()

        for (event in hitEvents) {
            assertThat(event.direction.length).isGreaterThan(0f)
            val hitInfo = event.hitInfoList.firstOrNull()
            assertWithMessage(
                    "Expected closest HitInfo referencing $expectedEntity on ${event.action}, " +
                        "got ${event.hitInfoList}"
                )
                .that(hitInfo?.inputEntity)
                .isEqualTo(expectedEntity)

            val resolvedHit = checkNotNull(hitInfo)
            val hitPos =
                checkNotNull(resolvedHit.hitPosition) {
                    "Expected non-null hitPosition on ${event.action}"
                }
            assertThat(resolvedHit.transform).isNotEqualTo(Matrix4.Zero)
            assertThat(resolvedHit.transform.isTrs).isTrue()

            if (expectedPlaneZ != null) {
                assertThat(hitPos.z).isWithin(HIT_POSITION_EPSILON).of(expectedPlaneZ)
                val localHit = (resolvedHit.transform * Matrix4.fromTranslation(hitPos)).translation
                assertThat(localHit.z).isWithin(HIT_POSITION_EPSILON).of(0f)
            }
        }
    }

    /**
     * An [InputEvent] listener that records all received events in order and provides wait helpers.
     *
     * Events are delivered on the main thread, while [events] and wait helpers are read from the
     * instrumentation thread.
     */
    private class RecordingInputEventListener : Consumer<InputEvent> {
        private val _events = CopyOnWriteArrayList<InputEvent>()
        @Volatile private var nextEventLatch: CountDownLatch? = null

        val events: List<InputEvent>
            get() = _events.toList()

        override fun accept(event: InputEvent) {
            _events.add(event)
            nextEventLatch?.countDown()
        }

        fun clear() {
            _events.clear()
            nextEventLatch = null
        }

        fun armNextEventLatch(): CountDownLatch {
            return CountDownLatch(1).also { nextEventLatch = it }
        }

        fun waitForActions(description: String, vararg requiredActions: InputEvent.Action) {
            if (requiredActions.isEmpty()) return
            waitForCondition(description) { recorded ->
                var nextRequiredIndex = 0
                for (event in recorded) {
                    if (event.action == requiredActions[nextRequiredIndex]) {
                        nextRequiredIndex++
                        if (nextRequiredIndex == requiredActions.size) {
                            return@waitForCondition true
                        }
                    }
                }
                false
            }
        }

        fun formatEvents(): String =
            _events.joinToString(prefix = "[", postfix = "]") {
                "${it.source}/${it.pointerType}/${it.action}(hits=${it.hitInfoList.map { h -> h.inputEntity }})"
            }

        fun waitForCondition(description: String, predicate: (List<InputEvent>) -> Boolean) {
            try {
                waitUntil(description = description, timeout = INPUT_TIMEOUT) {
                    predicate(_events.toList())
                }
            } catch (e: AssertionError) {
                throw AssertionError("${e.message}; receivedEvents=${formatEvents()}", e)
            }
        }
    }

    private companion object {
        /** Timeout applied when waiting for injected input sequences to complete. */
        val INPUT_TIMEOUT = 5.seconds

        /** Timeout applied when waiting for embedded activities to be destroyed. */
        val EMBEDDED_ACTIVITY_FINISH_TIMEOUT = 5.seconds

        /** Timeout applied when polling the spatial scene. */
        val POLL_TIMEOUT = 10.seconds

        /** Interval between two consecutive polls. */
        val POLL_INTERVAL = 100.milliseconds

        const val NEGATIVE_SETTLE_WINDOW_MS = 300L

        /** Name of the task scoped node that backs the main panel. */
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"

        const val FOREGROUND_PANEL_NAME = "InteractableForegroundPanel"
        const val ANCHOR_PANEL_NAME = "InteractableAnchorPanel"
        const val MULTI_POINTER_PANEL_NAME = "InteractableMultiPointerPanel"
        const val ENABLE_TOGGLE_PANEL_NAME = "InteractableEnableTogglePanel"
        const val COMBINED_PANEL_NAME = "InteractableCombinedPanel"

        const val BOUNDING_BOX_MODEL_ASSET = "BoundingBoxBlue.glb"
        val GLTF_BOUNDS_CENTER = Vector3.Zero
        const val GLTF_HALF_EXTENT = 0.5f
        const val GLTF_SUBSPACE_PREFIX = "gltf_entity_subspace_"

        /** Size of the spatial panel and of the activity panel created by baseline tests. */
        val PANEL_PIXEL_DIMENSIONS = IntSize2d(1280, 960)

        /**
         * Initial pose of the spatial panel and of the activity panel created by baseline tests.
         */
        val PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))

        /** Starting offset of the pointer drag relative to the panel center, in local meters. */
        val DRAG_START_OFFSET = Vector3(-0.1f, 0f, 0f)

        /** Ending offset of the pointer drag relative to the panel center, in local meters. */
        val DRAG_END_OFFSET = Vector3(0.1f, 0f, 0f)

        val PANEL_DIMENSIONS = FloatSize2d(0.6f, 0.5f)
        val ANCHOR_PANEL_DIMENSIONS = FloatSize2d(0.5f, 0.5f)
        val MULTI_POINTER_PANEL_DIMENSIONS = FloatSize2d(1.0f, 0.6f)
        val COMBINED_PANEL_DIMENSIONS = FloatSize2d(0.8f, 0.6f)

        val FOREGROUND_POSE = Pose(Vector3(-0.35f, 0f, -1.2f))
        val ANCHOR_POSE = Pose(Vector3(0.45f, 0f, -1.2f))
        val ANCHOR_POSE_FAR = Pose(Vector3(0.95f, 0f, -1.2f))
        val CENTER_PANEL_POSE = Pose(Vector3(0f, 0f, -1.2f))
        val GLTF_POSE = Pose(Vector3(-0.35f, 0f, -1.5f))
        val COMBINED_PANEL_POSE = Pose(Vector3(-0.65f, 0f, -1.2f))

        val MOVE_DELTA_X = Vector3(0.25f, 0f, 0f)
        val RESIZE_DELTA = Vector3(0.25f, -0.20f, 0f)
        val LEFT_POINTER_OFFSET = Vector3(-0.20f, 0f, 0f)
        val RIGHT_POINTER_OFFSET = Vector3(0.20f, 0f, 0f)
        val POST_RESIZE_HIT_OFFSET = Vector3(0.05f, 0.04f, 0f)
        const val HIT_POSITION_EPSILON = 0.05f
        const val HIT_TRANSFORM_EPSILON = 0.05f
    }
}

/** The content shown by the main panel in the baseline test. */
@Composable
private fun CenteredLabel(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 32.sp, color = Color.Black)
    }
}
