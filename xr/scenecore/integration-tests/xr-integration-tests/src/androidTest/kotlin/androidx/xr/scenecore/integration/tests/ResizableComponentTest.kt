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
import androidx.xr.runtime.Session
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
import androidx.xr.testutils.SpatialScene
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
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

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    @XrDeviceTest
    fun resizableComponent_mainPanelEntity_isResizable() = runTestWithSession { activity, session ->
        val resizeListener = RecordingResizeListener()

        // 1. Set the Compose content of the main panel.
        activity.setContent { CenteredLabel("This is Resizable MainPanel") }

        // 2. Request Full Space Mode and wait for the transition to complete.
        session.scene.requestFullSpaceAndAwait()

        // 3. Create the ResizableComponent and attach it to the main panel.
        val mainPanelEntity = session.scene.mainPanelEntity
        val resizableComponent =
            ResizableComponent.create(session = session, resizeEventListener = resizeListener)
        assertThat(mainPanelEntity.addComponent(resizableComponent)).isTrue()

        // Wait for the system to report the main panel as resizable before sampling its size.
        val mainPanelNode =
            awaitResizableNode(activity, "main panel") { task ->
                task.findNodeByName(MAIN_PANEL_NODE_NAME) ?: task.taskWindowLeash
            }
        val initialSizeInPixels = mainPanelEntity.sizeInPixels
        try {
            // 4. Grab the main panel by a corner of its resize affordance and drag it outwards.
            withContext(Dispatchers.IO) {
                mainPanelNode.performSpatialInteraction {
                    resizeNodeBy(RESIZE_DELTA, alignment = RESIZE_CORNER)
                }
            }

            // 5. Wait for the resize to end and verify that the main panel has grown.
            awaitResizeEnd(resizeListener)
            assertPanelHasGrown(mainPanelEntity, initialSizeInPixels)
        } finally {
            // Restore the main panel size before closing the session, since it is backed by the
            // task-scoped window leash node.
            mainPanelEntity.sizeInPixels = initialSizeInPixels
        }
    }

    @Test
    @XrDeviceTest
    fun resizableComponent_panelEntity_isResizable() = runTestWithSession { activity, session ->
        val panelName = "resizablePanel"
        val resizeListener = RecordingResizeListener()

        // 1. Request Full Space Mode and wait for the transition to complete.
        session.scene.requestFullSpaceAndAwait()

        // 2. Hide the main panel so it does not interfere with the spatial panel test, then create
        // the PanelEntity under test.
        session.scene.mainPanelEntity.setEnabled(false)
        try {
            val panelEntity =
                createSpatialPanel(activity, session, panelName, "This is Resizable Spatial Panel")

            // 3. Create the ResizableComponent and attach it to the panel entity.
            val resizableComponent =
                ResizableComponent.create(session = session, resizeEventListener = resizeListener)
            assertThat(panelEntity.addComponent(resizableComponent)).isTrue()

            // Wait for the system to report the panel as resizable before sampling its size.
            val panelNode =
                awaitResizableNode(activity, "spatial panel '$panelName'") { task ->
                    task.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
                }
            val initialSizeInPixels = panelEntity.sizeInPixels

            // 4. Grab the panel by a corner of its resize affordance and drag it outwards.
            withContext(Dispatchers.IO) {
                panelNode.performSpatialInteraction {
                    resizeNodeBy(RESIZE_DELTA, alignment = RESIZE_CORNER)
                }
            }

            // 5. Wait for the resize to end and verify that the panel has grown.
            awaitResizeEnd(resizeListener)
            assertPanelHasGrown(panelEntity, initialSizeInPixels)
        } finally {
            // Restore main panel visibility before closing the session, since it is backed by the
            // task-scoped window leash node.
            session.scene.mainPanelEntity.setEnabled(true)
        }
    }

    @Test
    @XrDeviceTest
    fun resizableComponent_activityPanelEntity_isResizable() =
        runTestWithSession { activity, session ->
            val panelName = "resizableActivityPanel"
            val resizeListener = RecordingResizeListener()

            // 1. Request Full Space Mode and wait for the transition to complete.
            session.scene.requestFullSpaceAndAwait()
            // Embedding an activity in a panel requires the EMBED_ACTIVITY capability.
            assertThat(session.scene.spatialCapabilities).contains(SpatialCapability.EMBED_ACTIVITY)

            // 2. Hide the main panel so it does not interfere with the activity panel test, then
            // create the ActivityPanelEntity under test and launch an activity into it.
            session.scene.mainPanelEntity.setEnabled(false)
            try {
                val activityPanelEntity =
                    ActivityPanelEntity.create(
                        session = session,
                        pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                        name = panelName,
                        pose = PANEL_POSE,
                        parent = session.scene.activitySpace,
                    )
                // Unlike the intent launched by runTestWithSession, this intent has no action and
                // no category, so ActivityScenario does not mistake the embedded activity for the
                // activity under test.
                activityPanelEntity.startActivity(Intent(activity, ComponentActivity::class.java))

                // 3. Create the ResizableComponent and attach it to the activity panel entity.
                val resizableComponent =
                    ResizableComponent.create(
                        session = session,
                        resizeEventListener = resizeListener,
                    )
                assertThat(activityPanelEntity.addComponent(resizableComponent)).isTrue()

                // Wait for the embedded activity to be shown, and for the system to report the
                // panel as resizable, before sampling its size.
                val panelNode =
                    awaitResizableNode(activity, "activity panel '$panelName'") { task ->
                        task.findNode { it.name == panelName }
                    }
                val initialSizeInPixels = activityPanelEntity.sizeInPixels

                // 4. Grab the panel by a corner of its resize affordance and drag it outwards.
                withContext(Dispatchers.IO) {
                    panelNode.performSpatialInteraction {
                        resizeNodeBy(RESIZE_DELTA, alignment = RESIZE_CORNER)
                    }
                }

                // 5. Wait for the resize to end and verify that the panel has grown.
                awaitResizeEnd(resizeListener)
                assertPanelHasGrown(activityPanelEntity, initialSizeInPixels)
            } finally {
                // Restore main panel visibility before closing the session, since it is backed by
                // the task-scoped window leash node.
                session.scene.mainPanelEntity.setEnabled(true)
            }
        }

    /**
     * Creates a [PanelEntity] named [name] that hosts a [ComposeView] displaying [text].
     *
     * Must be called on the main thread.
     */
    private fun createSpatialPanel(
        activity: ComponentActivity,
        session: Session,
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
     * Polls the task of [activity] in the spatial scene until [selectNode] returns a node that the
     * system reports as resizable, and whose extents are known, and returns that node.
     *
     * The pointer is aimed at the resize affordance based on the extents of the node, so they have
     * to be known before interacting with it. For instance, the extents of an [ActivityPanelEntity]
     * are only known once its activity has been launched.
     */
    private suspend fun awaitResizableNode(
        activity: ComponentActivity,
        description: String,
        selectNode: (SpatialTask) -> SpatialNode?,
    ): SpatialNode {
        val node =
            pollUntil(
                description = "a resizable $description",
                select = { captureSpatialScene().findTaskByActivity(activity)?.let(selectNode) },
                until = { it != null && it.isResizable && it.worldExtents != null },
            )
        return checkNotNull(node)
    }

    /** Suspends until [listener] has reported the end of the resize, and fails on timeout. */
    private suspend fun awaitResizeEnd(listener: RecordingResizeListener) {
        withTimeoutOrNull(RESIZE_TIMEOUT) { listener.resizeEnded.await() }
            ?: throw AssertionError("Resize did not end, last state: ${listener.lastResizeState}")
    }

    /**
     * Verifies that [panel] has grown beyond [initialSizeInPixels] in both dimensions.
     *
     * The size is polled because the size of the main panel is applied asynchronously, so it is not
     * necessarily up to date as soon as the resize has ended.
     */
    private suspend fun assertPanelHasGrown(panel: PanelEntity, initialSizeInPixels: IntSize2d) {
        pollUntil(
            description = "the panel to grow beyond $initialSizeInPixels",
            select = { panel.sizeInPixels },
            until = { size ->
                size.width > initialSizeInPixels.width && size.height > initialSizeInPixels.height
            },
        )
    }

    /** Captures the spatial scene off the main thread, since the capture blocks while polling. */
    private suspend fun captureSpatialScene(): SpatialScene =
        withContext(Dispatchers.IO) { SpatialSceneHelper.captureSpatialScene() }

    /**
     * Repeatedly evaluates [select] until [until] holds for its result, and returns that result.
     *
     * @throws AssertionError if [until] does not hold within [POLL_TIMEOUT].
     */
    private suspend fun <T> pollUntil(
        description: String,
        select: suspend () -> T,
        until: (T) -> Boolean,
    ): T {
        val start = TimeSource.Monotonic.markNow()
        while (true) {
            val value = select()
            if (until(value)) return value
            if (start.elapsedNow() >= POLL_TIMEOUT) {
                throw AssertionError("Timed out waiting for $description, last value: $value")
            }
            delay(POLL_INTERVAL)
        }
    }

    private companion object {
        /** Timeout applied when waiting for the end of a resize to be reported by the component. */
        val RESIZE_TIMEOUT = 5.seconds

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
 * [PanelEntity], and completes [resizeEnded] once it has done so.
 *
 * [ResizableComponent] only proposes a new size and leaves it to the app to apply it. Only the
 * final size is applied, so the panel keeps its size while it is being resized.
 */
private class RecordingResizeListener : Consumer<ResizeEvent> {
    val resizeEnded = CompletableDeferred<Unit>()

    /** State of the last received resize event, or null if no event has been received yet. */
    var lastResizeState: ResizeEvent.ResizeState? = null
        private set

    override fun accept(event: ResizeEvent) {
        lastResizeState = event.resizeState
        if (event.resizeState == ResizeEvent.ResizeState.END) {
            (event.entity as? PanelEntity)?.size = event.newSize.to2d()
            resizeEnded.complete(Unit)
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
