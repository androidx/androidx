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
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
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
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/561779675): Add integration test coverage for:
// 1. MovableComponent with scaleInZ = true, verifying that when the pointer drag includes Z-axis
//    translation (depth change), the entity's scale updates proportionally.
// 2. MovableComponent attached to ActivityPanelEntity and SurfaceEntity and GltfModelEntity (both
//    system and custom movable).
@RunWith(AndroidJUnit4::class)
@LargeTest
class MovableComponentTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun movableComponent_mainPanelEntity_systemMovable_isMovable() {
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread { activity.setContent { CenteredLabel("This is MainPanel") } }

        // 2. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        // 3. Create the MovableComponent (system movable) and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val movableComponent =
            MovableComponent.createSystemMovable(session = sessionRule.session, scaleInZ = false)
        movableComponent.addMoveListener(moveListener)
        assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()

        // Wait for the system to report the main panel as movable before sampling the pose.
        val mainPanelNode = awaitMainPanelNode(sessionRule.activity)
        val initialPosition = mainPanelEntity.getPose(Space.ACTIVITY).translation

        // 4. Grab the main panel by its move affordance and drag it.
        mainPanelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that the main panel pose was updated.
        awaitMoveEnd(moveListener)
        assertPanelHasMoved(mainPanelEntity, initialPosition)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun movableComponent_panelEntity_systemMovable_isMovable() {
        val panelName = "movablePanel"
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        // 1. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        val panelEntity = sessionRule.onMainThread {
            // 2. Create the PanelEntity under test, then hide the main panel so it does not
            // interfere with the spatial panel test.
            val panel = createSpatialPanel(panelName, "This is Spatial Panel")
            session.scene.mainPanelEntity.setEnabled(false)

            // 3. Create the MovableComponent (system movable) and attach it to the panel entity.
            val movableComponent =
                MovableComponent.createSystemMovable(session = session, scaleInZ = false)
            movableComponent.addMoveListener(moveListener)
            assertThat(panel.addComponent(movableComponent)).isTrue()
            panel
        }

        // Wait for the system to report the panel as movable before sampling the pose.
        val panelNode = awaitPanelNode(sessionRule.activity, panelName)
        val initialPosition = panelEntity.getPose(Space.ACTIVITY).translation

        // 4. Grab the panel by its move affordance and drag it.
        panelNode.performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that the panel entity pose was updated.
        awaitMoveEnd(moveListener)
        assertPanelHasMoved(panelEntity, initialPosition)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun movableComponent_mainPanelEntity_customMovable_isMovable() {
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread {
            activity.setContent { CenteredLabel("This is Custom Movable MainPanel") }
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        // 3. Create the MovableComponent (custom movable) and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val movableComponent =
            MovableComponent.createCustomMovable(
                session = sessionRule.session,
                scaleInZ = false,
                executor = null,
                entityMoveListener = moveListener,
            )
        assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()

        // Wait for the system to report the main panel as movable before sampling the pose.
        val mainPanelNode = awaitMainPanelNode(sessionRule.activity)
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
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun movableComponent_panelEntity_customMovable_isMovable() {
        val panelName = "customMovablePanel"
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)

        // 1. Request Full Space Mode and wait for the transition to complete.
        sessionRule.requestFullSpace()

        val panelEntity = sessionRule.onMainThread {
            // 2. Create the PanelEntity under test, then hide the main panel so it does not
            // interfere with the spatial panel test.
            val panel = createSpatialPanel(panelName, "This is Custom Movable Spatial Panel")
            session.scene.mainPanelEntity.setEnabled(false)

            // 3. Create the MovableComponent (custom movable) and attach it to the panel entity.
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

        // Wait for the system to report the panel as movable before sampling the pose.
        val panelNode = awaitPanelNode(sessionRule.activity, panelName)
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

    /**
     * Creates a [PanelEntity] named [name] under the activity space that hosts a [ComposeView]
     * displaying [text].
     *
     * Must be called on the main thread, i.e. inside `sessionRule.onMainThread { }`.
     */
    private fun XrSessionScope<*>.createSpatialPanel(name: String, text: String): PanelEntity {
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
                dimensions = PANEL_DIMENSIONS,
                name = name,
                pose = PANEL_POSE,
                parent = session.scene.activitySpace,
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
     * Polls the spatial scene until the node backing the main panel of [activity] is reported as
     * movable with known extents, and returns that node.
     *
     * Must be called from the instrumentation thread.
     */
    private fun awaitMainPanelNode(activity: ComponentActivity): SpatialNode =
        awaitMovableNode(activity, "$MAIN_PANEL_NODE_NAME node") { task ->
            task.findNode { it.name == MAIN_PANEL_NODE_NAME }
        }

    /**
     * Polls the spatial scene until the embedded window leash of the spatial panel named
     * [panelName] is reported as movable with known extents, and returns that node.
     *
     * Must be called from the instrumentation thread.
     */
    private fun awaitPanelNode(
        activity: ComponentActivity,
        panelName: String,
    ): SpatialNode =
        awaitMovableNode(activity, "spatial panel node named '$panelName'") { task ->
            task.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
        }

    /**
     * Waits for the UI to sync, then polls the task of [activity] in the spatial scene until
     * [selectNode] returns a node that the system reports as movable, and whose extents are known,
     * and returns that node.
     *
     * The pointer is aimed at the move affordance based on the extents of the node, so they have to
     * be known before interacting with it.
     *
     * Must be called from the instrumentation thread, since waitForIdleSync and waitForSpatialScene
     * both block.
     */
    private fun awaitMovableNode(
        activity: ComponentActivity,
        description: String,
        selectNode: (SpatialTask) -> SpatialNode?,
    ): SpatialNode {
        // Let pending layout and component transactions submit before polling CPM, so the caller
        // samples its "before" state after the UI has synced.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val node =
            waitForSpatialScene(
                description = "a movable $description",
                timeout = SCENE_TIMEOUT,
                pollInterval = SCENE_POLL_INTERVAL,
                select = { scene -> scene.findTaskByActivity(activity)?.let(selectNode) },
                until = { it != null && it.isMovable && it.worldExtents != null },
            )
        return checkNotNull(node)
    }

    /**
     * Blocks the instrumentation thread until [listener] has reported the end of the move, and
     * fails on timeout.
     */
    private fun awaitMoveEnd(listener: RecordingMoveListener) {
        val moveEnded =
            listener.moveEnded.await(MOVE_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        assertWithMessage("Timed out waiting for the move to end").that(moveEnded).isTrue()
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

    private companion object {
        /** Timeout applied when waiting for a move to be reported by the component. */
        val MOVE_TIMEOUT = 5.seconds

        /** Timeout applied when polling the spatial scene. */
        val SCENE_TIMEOUT = 10.seconds

        /** Interval between two consecutive captures of the spatial scene. */
        val SCENE_POLL_INTERVAL = 100.milliseconds

        /** Name of the task scoped node that backs the main panel. */
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"

        /** Size of the spatial panels created by `createSpatialPanel`. */
        val PANEL_DIMENSIONS = FloatSize2d(1.28f, 0.96f)

        /** Initial pose of the spatial panels created by `createSpatialPanel`. */
        val PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))

        /** Drag applied to the target node, expressed in the node's local space. */
        val MOVE_DELTA = Vector3(0.25f, 0.16f, 0f)

        /**
         * Lower bound for the distance the entity is expected to travel in ActivitySpace.
         *
         * [MOVE_DELTA] is expressed in the node's local space, so the resulting distance in
         * ActivitySpace also depends on the scale of the node while it is being dragged. The bound
         * is therefore kept well below `MOVE_DELTA.length` (~0.297m) rather than asserting an exact
         * distance.
         */
        val MIN_MOVE_DISTANCE_METERS = 0.5f * MOVE_DELTA.length
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
    val moveEnded = CountDownLatch(1)

    // These flags are written on the callback thread and read by the test thread only after
    // moveEnded.await() returns, which establishes the required happens-before edge.
    var onMoveStartCalled: Boolean = false
        private set

    var onMoveUpdateCalled: Boolean = false
        private set

    var onMoveEndCalled: Boolean = false
        private set

    override fun onMoveStart(
        entity: Entity,
        initialInputRay: Ray,
        initialPose: Pose,
        initialScale: Float,
        initialParent: Entity,
    ) {
        onMoveStartCalled = true
    }

    override fun onMoveUpdate(
        entity: Entity,
        currentInputRay: Ray,
        currentPose: Pose,
        currentScale: Float,
    ) {
        onMoveUpdateCalled = true
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
        onMoveEndCalled = true
        if (applyMoveToEntity) {
            entity.setPose(finalPose)
            entity.setScale(finalScale)
        }
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
