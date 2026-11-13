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
import android.view.View
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionScope
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/569762387): Add integration test coverage for:
// 1. Hover exit (HOVER_EXIT) when the pointer ray moves off the entity, and detaching
//    InteractableComponent to verify no further InputEvents are delivered.
// 2. Subgraph event propagation from descendant entities that do not have their own
//    InteractableComponent attached.
// 3. InteractableComponent attached to GltfModelEntity and SurfaceEntity, and combined with
//    MovableComponent and ResizableComponent.
// 4. Concurrent / interleaved multi-pointer interactions (e.g., LEFT and RIGHT pointers).
/**
 * Integration tests for [InteractableComponent], driven by injected raycast input.
 *
 * Each test attaches an [InteractableComponent] to a panel, performs a pointer hover, press, drag,
 * and release across the panel, and verifies that the expected [InputEvent] sequence and hit
 * information are delivered to the listener.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InteractableComponentTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun interactableComponent_mainPanelEntity_receivesInputEvents() {
        val inputListener = RecordingInputEventListener()

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread {
            activity.setContent { CenteredLabel("This is Interactable MainPanel") }
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        // 3. Create the InteractableComponent and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val interactableComponent =
            InteractableComponent.create(
                session = sessionRule.session,
                inputEventListener = inputListener,
            )
        assertThat(mainPanelEntity.addComponent(interactableComponent)).isTrue()

        // Wait for the main panel node to be ready to receive input in the spatial scene.
        val mainPanelNode =
            awaitInteractableNode(sessionRule.activity, "main panel") { task ->
                task.findNode { it.name == MAIN_PANEL_NODE_NAME }
            }

        // 4. Aim at the main panel, press, drag across its center, and release.
        performDragInteraction(mainPanelNode)

        // 5. Wait for the input sequence to complete and verify the received InputEvents.
        awaitInputEnd(inputListener)
        assertInputEvents(inputListener.events, expectedEntity = mainPanelEntity)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun interactableComponent_panelEntity_receivesInputEvents() {
        val panelName = "interactablePanel"
        val inputListener = RecordingInputEventListener()

        // 1. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        val panelEntity = sessionRule.onMainThread {
            // 2. Hide the main panel so it does not interfere with the spatial panel test, then
            // create the PanelEntity under test.
            session.scene.mainPanelEntity.setEnabled(false)
            val panel = createSpatialPanel(panelName, "This is Interactable Spatial Panel")

            // 3. Create the InteractableComponent and attach it to the panel entity.
            val interactableComponent =
                InteractableComponent.create(
                    session = session,
                    inputEventListener = inputListener,
                )
            assertThat(panel.addComponent(interactableComponent)).isTrue()
            panel
        }

        // Wait for the spatial panel node to be ready to receive input in the spatial scene.
        val panelNode =
            awaitInteractableNode(sessionRule.activity, "spatial panel '$panelName'") { task ->
                task.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
            }

        // 4. Aim at the panel, press, drag across its center, and release.
        performDragInteraction(panelNode)

        // 5. Wait for the input sequence to complete and verify the received InputEvents.
        awaitInputEnd(inputListener)
        assertInputEvents(inputListener.events, expectedEntity = panelEntity)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun interactableComponent_activityPanelEntity_receivesInputEvents() {
        val panelName = "interactableActivityPanel"
        val inputListener = RecordingInputEventListener()

        // 1. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()
        // Embedding an activity in a panel requires the EMBED_ACTIVITY capability.
        val spatialCapabilities = sessionRule.session.scene.spatialCapabilities
        assume().that(spatialCapabilities).contains(SpatialCapability.EMBED_ACTIVITY)

        try {
            val activityPanelEntity = sessionRule.onMainThread {
                // 2. Hide the main panel so it does not interfere with the activity panel test,
                // then create the ActivityPanelEntity under test and launch an activity into it.
                session.scene.mainPanelEntity.setEnabled(false)
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

                // 3. Create the InteractableComponent and attach it to the activity panel entity.
                val interactableComponent =
                    InteractableComponent.create(
                        session = session,
                        inputEventListener = inputListener,
                    )
                assertThat(activityPanel.addComponent(interactableComponent)).isTrue()
                activityPanel
            }

            // Wait for the embedded activity to be shown and ready to receive input in the spatial
            // scene.
            val panelNode =
                awaitInteractableNode(sessionRule.activity, "activity panel '$panelName'") { task ->
                    task.findNode { it.name == panelName }
                }

            // 4. Aim at the activity panel, press, drag across its center, and release.
            performDragInteraction(panelNode)

            // 5. Wait for the input sequence to complete and verify the received InputEvents.
            awaitInputEnd(inputListener)
            assertInputEvents(inputListener.events, expectedEntity = activityPanelEntity)
        } finally {
            // No public API closes the embedded activity before the host finishes; finish it
            // directly.
            finishActivitiesOtherThan(sessionRule.activity)
        }
    }

    /**
     * Creates a [PanelEntity] named [name] that hosts a [ComposeView] displaying [text].
     *
     * Must be called inside `sessionRule.onMainThread { }`.
     */
    private fun XrSessionScope<ComponentActivity>.createSpatialPanel(
        name: String,
        text: String,
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
                pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                name = name,
                pose = PANEL_POSE,
                parent = session.scene.activitySpace,
            )

        // The panel content is hosted in an embedded window that does not inherit the activity's
        // ViewTree owners, so the ComposeView would fail to compose without them. PanelEntity wraps
        // a non-FrameLayout view in a new FrameLayout, so the owners have to be set on that root.
        val rootView: View = panelContentView.parent as? View ?: panelContentView
        rootView.setViewTreeLifecycleOwner(activity)
        rootView.setViewTreeViewModelStoreOwner(activity)
        rootView.setViewTreeSavedStateRegistryOwner(activity)

        return panelEntity
    }

    /**
     * Polls the task of [activity] in the spatial scene until [selectNode] returns a node that has
     * known extents, and returns that node.
     *
     * The pointer ray is aimed at the node based on its extents, so its extents have to be ready
     * before interacting with it. For instance, the surface and extents of an [ActivityPanelEntity]
     * are only populated once its embedded activity has been launched.
     *
     * Blocks, so it must be called from the instrumentation thread.
     */
    private fun awaitInteractableNode(
        activity: ComponentActivity,
        description: String,
        selectNode: (SpatialTask) -> SpatialNode?,
    ): SpatialNode {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val node =
            waitForSpatialScene(
                description = "an interactable $description",
                timeout = POLL_TIMEOUT,
                pollInterval = POLL_INTERVAL,
                select = { scene -> scene.findTaskByActivity(activity)?.let(selectNode) },
                until = { it != null && it.worldExtents != null },
            )
        return checkNotNull(node)
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

        sessionRule.onMainThread { aliveActivities().forEach { it.finish() } }
        waitUntil(
            description = "embedded activities to be destroyed",
            timeout = EMBEDDED_ACTIVITY_FINISH_TIMEOUT,
            pollInterval = POLL_INTERVAL,
        ) {
            sessionRule.onMainThread { aliveActivities().isEmpty() }
        }
    }

    /**
     * Blocks the instrumentation thread until [listener] has recorded an [InputEvent.Action.UP]
     * event, and fails on timeout.
     */
    private fun awaitInputEnd(listener: RecordingInputEventListener) {
        val timeoutMillis = INPUT_TIMEOUT.inWholeMilliseconds
        if (!listener.inputEnded.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw AssertionError(
                "Input sequence did not end with UP, received actions: " +
                    "${listener.events.map { it.action }}"
            )
        }
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

    private companion object {
        /** Timeout applied when waiting for the input sequence to complete. */
        val INPUT_TIMEOUT = 5.seconds

        /** Timeout applied when waiting for embedded activities to be destroyed. */
        val EMBEDDED_ACTIVITY_FINISH_TIMEOUT = 5.seconds

        /** Timeout applied when polling the spatial scene. */
        val POLL_TIMEOUT = 10.seconds

        /** Interval between two consecutive polls. */
        val POLL_INTERVAL = 100.milliseconds

        /** Name of the task scoped node that backs the main panel. */
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"

        /** Size of the spatial panel and of the activity panel created by the tests. */
        val PANEL_PIXEL_DIMENSIONS = IntSize2d(1280, 960)

        /** Initial pose of the spatial panel and of the activity panel created by the tests. */
        val PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))

        /** Starting offset of the pointer drag relative to the panel center, in local meters. */
        val DRAG_START_OFFSET = Vector3(-0.1f, 0f, 0f)

        /** Ending offset of the pointer drag relative to the panel center, in local meters. */
        val DRAG_END_OFFSET = Vector3(0.1f, 0f, 0f)
    }
}

/**
 * An [InputEvent] listener that records all received events in order and counts down [inputEnded]
 * once an [InputEvent.Action.UP] event is received.
 *
 * Events are delivered on the main thread, while [events] and [inputEnded] are read from the
 * instrumentation thread.
 */
private class RecordingInputEventListener : Consumer<InputEvent> {
    private val _events = CopyOnWriteArrayList<InputEvent>()

    val events: List<InputEvent>
        get() = _events.toList()

    val inputEnded = CountDownLatch(1)

    override fun accept(event: InputEvent) {
        _events.add(event)
        if (event.action == InputEvent.Action.UP) {
            inputEnded.countDown()
        }
    }
}

/** The content shown by the main panel and by the spatial panel created by this test. */
@Composable
private fun CenteredLabel(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 32.sp, color = Color.Black)
    }
}
