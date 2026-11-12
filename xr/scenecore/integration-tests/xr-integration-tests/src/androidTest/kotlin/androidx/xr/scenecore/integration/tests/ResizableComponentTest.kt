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
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionScope
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/568825233): Add integration test coverage for:
// 1. Resizing from the other corners of the resize affordance, and shrinking the panel.
// 2. ResizableComponent with minimumSize and maximumSize, isFixedAspectRatioEnabled and
//    isAutoHideContentWhileResizingEnabled.
// 3. The START, ONGOING and END resize events, and the pose of the panel after the resize.
// 4. ResizableComponent combined with MovableComponent, and attached to SurfaceEntity.
/**
 * Integration tests for [ResizableComponent], driven by injected raycast input.
 *
 * Each test attaches a [ResizableComponent] to a panel, drags a corner of the panel's resize
 * affordance outwards, and verifies that the panel has grown.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ResizableComponentTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun resizableComponent_mainPanelEntity_isResizable() {
        val resizeListener = RecordingResizeListener()

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread {
            activity.setContent { CenteredLabel("This is Resizable MainPanel") }
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        // 3. Create the ResizableComponent and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val resizableComponent =
            ResizableComponent.create(
                session = sessionRule.session,
                resizeEventListener = resizeListener,
            )
        assertThat(mainPanelEntity.addComponent(resizableComponent)).isTrue()

        // Wait for the system to report the main panel as resizable before sampling its size.
        val mainPanelNode =
            awaitResizableNode(sessionRule.activity, "main panel") { task ->
                task.findNode { it.name == MAIN_PANEL_NODE_NAME }
            }
        val initialSizeInPixels = mainPanelEntity.sizeInPixels

        // 4. Grab the main panel by a corner of its resize affordance and drag it outwards.
        mainPanelNode.performSpatialInteraction {
            resizeNodeBy(RESIZE_DELTA, alignment = RESIZE_CORNER)
        }

        // 5. Wait for the resize to end and verify that the main panel has grown.
        awaitResizeEnd(resizeListener)
        assertPanelHasGrown(mainPanelEntity, initialSizeInPixels)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun resizableComponent_panelEntity_isResizable() {
        val panelName = "resizablePanel"
        val resizeListener = RecordingResizeListener()

        // 1. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        val panelEntity = sessionRule.onMainThread {
            // 2. Create the PanelEntity under test, then hide the main panel so it does not
            // interfere with the spatial panel test.
            val panel = createSpatialPanel(panelName, "This is Resizable Spatial Panel")
            session.scene.mainPanelEntity.setEnabled(false)

            // 3. Create the ResizableComponent and attach it to the panel entity.
            val resizableComponent =
                ResizableComponent.create(
                    session = session,
                    resizeEventListener = resizeListener,
                )
            assertThat(panel.addComponent(resizableComponent)).isTrue()
            panel
        }

        // Wait for the system to report the panel as resizable before sampling its size.
        val panelNode =
            awaitResizableNode(sessionRule.activity, "spatial panel '$panelName'") { task ->
                task.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
            }
        val initialSizeInPixels = panelEntity.sizeInPixels

        // 4. Grab the panel by a corner of its resize affordance and drag it outwards.
        panelNode.performSpatialInteraction {
            resizeNodeBy(RESIZE_DELTA, alignment = RESIZE_CORNER)
        }

        // 5. Wait for the resize to end and verify that the panel has grown.
        awaitResizeEnd(resizeListener)
        assertPanelHasGrown(panelEntity, initialSizeInPixels)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun resizableComponent_activityPanelEntity_isResizable() {
        val panelName = "resizableActivityPanel"
        val resizeListener = RecordingResizeListener()

        // 1. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        try {
            val activityPanelEntity = sessionRule.onMainThread {
                // Embedding an activity in a panel requires the EMBED_ACTIVITY capability.
                assertThat(session.scene.spatialCapabilities)
                    .contains(SpatialCapability.EMBED_ACTIVITY)

                // 2. Create the ActivityPanelEntity under test, hide the main panel so it does not
                // interfere with the activity panel test, and launch an activity into the panel.
                val activityPanel =
                    ActivityPanelEntity.create(
                        session = session,
                        pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                        name = panelName,
                        pose = PANEL_POSE,
                        parent = session.scene.activitySpace,
                    )
                session.scene.mainPanelEntity.setEnabled(false)

                // Unlike the intent that XrSessionRule's ActivityScenarioRule launches the activity
                // under test with, this intent has no action and no category, so ActivityScenario
                // does not mistake the embedded activity for the activity under test.
                activityPanel.startActivity(Intent(activity, ComponentActivity::class.java))

                // 3. Create the ResizableComponent and attach it to the activity panel entity.
                val resizableComponent =
                    ResizableComponent.create(
                        session = session,
                        resizeEventListener = resizeListener,
                    )
                assertThat(activityPanel.addComponent(resizableComponent)).isTrue()
                activityPanel
            }

            // Wait for the embedded activity to be shown, and for the system to report the panel
            // as resizable, before sampling its size.
            val panelNode =
                awaitResizableNode(sessionRule.activity, "activity panel '$panelName'") { task ->
                    task.findNode { it.name == panelName }
                }
            val initialSizeInPixels = activityPanelEntity.sizeInPixels

            // 4. Grab the panel by a corner of its resize affordance and drag it outwards.
            panelNode.performSpatialInteraction {
                resizeNodeBy(RESIZE_DELTA, alignment = RESIZE_CORNER)
            }

            // 5. Wait for the resize to end and verify that the panel has grown.
            awaitResizeEnd(resizeListener)
            assertPanelHasGrown(activityPanelEntity, initialSizeInPixels)
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
     * Waits for the UI to sync, then polls the task of [activity] in the spatial scene until
     * [selectNode] returns a node that the system reports as resizable, and whose extents are
     * known, and returns that node.
     *
     * The pointer is aimed at the resize affordance based on the extents of the node, so they have
     * to be known before interacting with it. For instance, the extents of an [ActivityPanelEntity]
     * are only known once its activity has been launched.
     *
     * Blocks, so it must be called from the instrumentation thread.
     */
    private fun awaitResizableNode(
        activity: ComponentActivity,
        description: String,
        selectNode: (SpatialTask) -> SpatialNode?,
    ): SpatialNode {
        // Let pending layout and component transactions submit before polling CPM, so the caller
        // samples its "before" state after the UI has synced.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val node =
            SpatialSceneHelper.waitForSpatialScene(
                description = "a resizable $description",
                timeout = POLL_TIMEOUT,
                pollInterval = POLL_INTERVAL,
                select = { scene -> scene.findTaskByActivity(activity)?.let(selectNode) },
                until = { it != null && it.isResizable && it.worldExtents != null },
            )
        return checkNotNull(node)
    }

    /**
     * Finishes every activity of this process except [host], and blocks until they are destroyed.
     *
     * An [ActivityPanelEntity] only destroys its embedded activity when the entity itself is
     * destroyed, which happens after [host] has finished. If the embedded activity is still alive
     * when [host] finishes, it becomes the top activity of the task and crashes SystemUI.
     *
     * Must be called from the instrumentation thread before the test finishes.
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
     * Blocks the instrumentation thread until [listener] has reported the end of the resize, and
     * fails on timeout.
     */
    private fun awaitResizeEnd(listener: RecordingResizeListener) {
        val timeoutMillis = RESIZE_TIMEOUT.inWholeMilliseconds
        if (!listener.resizeEnded.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw AssertionError("Resize did not end, last state: ${listener.lastResizeState}")
        }
    }

    /**
     * Verifies that [panel] has grown beyond [initialSizeInPixels] in both dimensions.
     *
     * The size is polled because the size of the main panel is applied asynchronously, so it is not
     * necessarily up to date as soon as the resize has ended.
     */
    private fun assertPanelHasGrown(panel: PanelEntity, initialSizeInPixels: IntSize2d) {
        waitUntil(
            description = "the panel to grow beyond $initialSizeInPixels",
            timeout = POLL_TIMEOUT,
            pollInterval = POLL_INTERVAL,
        ) {
            val size = panel.sizeInPixels
            size.width > initialSizeInPixels.width && size.height > initialSizeInPixels.height
        }
    }

    private companion object {
        /** Timeout applied when waiting for the end of a resize to be reported by the component. */
        val RESIZE_TIMEOUT = 5.seconds

        /** Timeout applied when waiting for embedded activities to be destroyed. */
        val EMBEDDED_ACTIVITY_FINISH_TIMEOUT = 5.seconds

        /** Timeout applied when polling the spatial scene or the size of a panel. */
        val POLL_TIMEOUT = 10.seconds

        /** Interval between two consecutive polls. */
        val POLL_INTERVAL = 100.milliseconds

        /** Name of the task scoped node that backs the main panel. */
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"

        /** Size of the spatial panel and of the activity panel created by the tests. */
        val PANEL_PIXEL_DIMENSIONS = IntSize2d(1280, 960)

        /** Initial pose of the spatial panel and of the activity panel created by the tests. */
        val PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))

        /**
         * Corner of the resize affordance that is dragged. Only the corners of the affordance
         * resize the panel, its edges are used to move it.
         */
        val RESIZE_CORNER = SpatialAlignment.BottomRight

        /**
         * Drag applied to [RESIZE_CORNER], expressed in the node's local space. It points outwards
         * from the bottom right corner, so it is expected to grow the panel in both dimensions.
         */
        val RESIZE_DELTA = Vector3(0.2f, -0.2f, 0f)
    }
}

/**
 * A resize event listener that applies the size proposed at the end of a resize to the resized
 * [PanelEntity], and counts down [resizeEnded] once it has done so.
 *
 * [ResizableComponent] only proposes a new size and leaves it to the app to apply it. Only the
 * final size is applied, so the panel keeps its size while it is being resized.
 *
 * Events are delivered on the main thread, while [resizeEnded] and [lastResizeState] are read from
 * the instrumentation thread.
 */
private class RecordingResizeListener : Consumer<ResizeEvent> {
    val resizeEnded = CountDownLatch(1)

    /** State of the last received resize event, or null if no event has been received yet. */
    @Volatile
    var lastResizeState: ResizeEvent.ResizeState? = null
        private set

    override fun accept(event: ResizeEvent) {
        lastResizeState = event.resizeState
        if (event.resizeState == ResizeEvent.ResizeState.END) {
            (event.entity as? PanelEntity)?.size = event.newSize.to2d()
            resizeEnded.countDown()
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
