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
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.Session
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
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
    @get:Rule val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var session: Session

    @After
    fun tearDown() {
        resetSpatialInteraction()
        if (this::session.isInitialized) {
            // Some tests hide the main panel. It is backed by the task scoped
            // MainPanelAndTaskWindowLeashNode, which is shared with the other tests in this class,
            // so restore its visibility instead of leaking the hidden state.
            activityScenarioRule.scenario.onActivity {
                session.scene.mainPanelEntity.setEnabled(true)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun movableComponent_mainPanelEntity_systemMovable_isMovable() {
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)

        activityScenarioRule.scenario.onActivity { activity ->
            // 1. Set the Compose content of the main panel and create the XR session.
            activity.setContent { CenteredLabel("This is MainPanel") }
            session = createXrSession(activity)
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        activityScenarioRule.scenario.requestAndAwaitFullSpace(session)

        val mainPanelEntity = session.scene.mainPanelEntity
        activityScenarioRule.scenario.onActivity {
            // 3. Create the MovableComponent (system movable) and attach it to the main panel.
            val movableComponent =
                MovableComponent.createSystemMovable(session = session, scaleInZ = false)
            movableComponent.addMoveListener(moveListener)
            assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()
        }

        // Wait for the UI and the scene graph to sync before sampling the pose.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val initialPosition = activitySpacePosition(mainPanelEntity)

        // 4. Grab the main panel by its move affordance and drag it.
        findMainPanelNode().performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that the main panel pose was updated.
        awaitMoveEnd(moveListener)
        val distanceMoved = (activitySpacePosition(mainPanelEntity) - initialPosition).length
        assertThat(distanceMoved).isGreaterThan(MIN_MOVE_DISTANCE_METERS)
    }

    @Test
    @XrDeviceTest
    fun movableComponent_panelEntity_systemMovable_isMovable() {
        val panelName = "movablePanel"
        val moveListener = RecordingMoveListener(applyMoveToEntity = false)
        lateinit var panelEntity: PanelEntity

        activityScenarioRule.scenario.onActivity { activity ->
            // 1. Create the XR session. The main panel content is intentionally left unset because
            // the main panel is hidden below.
            session = createXrSession(activity)
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        activityScenarioRule.scenario.requestAndAwaitFullSpace(session)

        activityScenarioRule.scenario.onActivity { activity ->
            // 3. Hide the main panel so it does not interfere with the spatial panel test, then
            // create the PanelEntity under test.
            session.scene.mainPanelEntity.setEnabled(false)
            panelEntity = createSpatialPanel(activity, panelName, "This is Spatial Panel")

            // 4. Create the MovableComponent (system movable) and attach it to the panel entity.
            val movableComponent =
                MovableComponent.createSystemMovable(session = session, scaleInZ = false)
            movableComponent.addMoveListener(moveListener)
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()
        }

        // Wait for the UI and the scene graph to sync before sampling the pose.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val initialPosition = activitySpacePosition(panelEntity)

        // 5. Grab the panel by its move affordance and drag it.
        findPanelNode(panelName).performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 6. Wait for the move to end and verify that the panel entity pose was updated.
        awaitMoveEnd(moveListener)
        val distanceMoved = (activitySpacePosition(panelEntity) - initialPosition).length
        assertThat(distanceMoved).isGreaterThan(MIN_MOVE_DISTANCE_METERS)
    }

    @Test
    @XrDeviceTest
    fun movableComponent_mainPanelEntity_customMovable_isMovable() {
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)

        activityScenarioRule.scenario.onActivity { activity ->
            // 1. Set the Compose content of the main panel and create the XR session.
            activity.setContent { CenteredLabel("This is Custom Movable MainPanel") }
            session = createXrSession(activity)
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        activityScenarioRule.scenario.requestAndAwaitFullSpace(session)

        val mainPanelEntity = session.scene.mainPanelEntity
        activityScenarioRule.scenario.onActivity {
            // 3. Create the MovableComponent (custom movable) and attach it to the main panel.
            val movableComponent =
                MovableComponent.createCustomMovable(
                    session = session,
                    scaleInZ = false,
                    executor = null,
                    entityMoveListener = moveListener,
                )
            assertThat(mainPanelEntity.addComponent(movableComponent)).isTrue()
        }

        // Wait for the UI and the scene graph to sync before sampling the pose.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val initialPosition = activitySpacePosition(mainPanelEntity)

        // 4. Grab the main panel by its move affordance and drag it.
        findMainPanelNode().performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 5. Wait for the move to end and verify that every move callback fired and that the main
        // panel pose was updated by the listener.
        awaitMoveEnd(moveListener)
        val distanceMoved = (activitySpacePosition(mainPanelEntity) - initialPosition).length
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveUpdateCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()
        assertThat(distanceMoved).isGreaterThan(MIN_MOVE_DISTANCE_METERS)
    }

    @Test
    @XrDeviceTest
    fun movableComponent_panelEntity_customMovable_isMovable() {
        val panelName = "customMovablePanel"
        val moveListener = RecordingMoveListener(applyMoveToEntity = true)
        lateinit var panelEntity: PanelEntity

        activityScenarioRule.scenario.onActivity { activity ->
            // 1. Create the XR session. The main panel content is intentionally left unset because
            // the main panel is hidden below.
            session = createXrSession(activity)
        }

        // 2. Request Full Space Mode and wait for the transition to complete.
        activityScenarioRule.scenario.requestAndAwaitFullSpace(session)

        activityScenarioRule.scenario.onActivity { activity ->
            // 3. Hide the main panel so it does not interfere with the spatial panel test, then
            // create the PanelEntity under test.
            session.scene.mainPanelEntity.setEnabled(false)
            panelEntity =
                createSpatialPanel(activity, panelName, "This is Custom Movable Spatial Panel")

            // 4. Create the MovableComponent (custom movable) and attach it to the panel entity.
            val movableComponent =
                MovableComponent.createCustomMovable(
                    session = session,
                    scaleInZ = false,
                    executor = null,
                    entityMoveListener = moveListener,
                )
            assertThat(panelEntity.addComponent(movableComponent)).isTrue()
        }

        // Wait for the UI and the scene graph to sync before sampling the pose.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val initialPosition = activitySpacePosition(panelEntity)

        // 5. Grab the panel by its move affordance and drag it.
        findPanelNode(panelName).performSpatialInteraction { moveNodeBy(MOVE_DELTA) }

        // 6. Wait for the move to end and verify that every move callback fired and that the panel
        // entity pose was updated by the listener.
        awaitMoveEnd(moveListener)
        val distanceMoved = (activitySpacePosition(panelEntity) - initialPosition).length
        assertThat(moveListener.onMoveStartCalled).isTrue()
        assertThat(moveListener.onMoveUpdateCalled).isTrue()
        assertThat(moveListener.onMoveEndCalled).isTrue()
        assertThat(distanceMoved).isGreaterThan(MIN_MOVE_DISTANCE_METERS)
    }

    /**
     * Creates a [PanelEntity] named [name] that hosts a [ComposeView] displaying [text].
     *
     * Must be called on the main thread.
     */
    private fun createSpatialPanel(
        activity: ComponentActivity,
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
                dimensions = PANEL_DIMENSIONS,
                name = name,
                pose = PANEL_POSE,
                parent = session.scene.activitySpace,
            )

        // The panel content is hosted in an embedded window that does not inherit the activity's
        // ViewTree owners, so the ComposeView would fail to compose without them. PanelEntity wraps
        // a non-FrameLayout view in a new FrameLayout, so the owners have to be set on that root.
        val parentView: View =
            if (panelContentView.parent != null && panelContentView.parent is View)
                panelContentView.parent as View
            else panelContentView

        parentView.setViewTreeLifecycleOwner(activity)
        parentView.setViewTreeViewModelStoreOwner(activity)
        parentView.setViewTreeSavedStateRegistryOwner(activity)

        return panelEntity
    }

    /** Captures the spatial scene and returns the task of the instrumented package. */
    private fun captureTask(): SpatialTask {
        val scene = SpatialSceneHelper.captureSpatialScene()
        return checkNotNull(scene.findTaskByPackageName(packageName)) {
            "Expected an active spatial task for $packageName"
        }
    }

    /** Returns the node backing the main panel, from a freshly captured spatial scene. */
    private fun findMainPanelNode(): SpatialNode {
        val task = captureTask()
        return checkNotNull(task.findNodeByName(MAIN_PANEL_NODE_NAME) ?: task.taskWindowLeash) {
            "Expected an active $MAIN_PANEL_NODE_NAME for $packageName"
        }
    }

    /**
     * Returns the node backing the spatial panel named [panelName], from a freshly captured spatial
     * scene.
     */
    private fun findPanelNode(panelName: String): SpatialNode {
        return checkNotNull(
            captureTask().findNode {
                it.name == panelName && it.isMovable && it.isEmbeddedWindowLeash
            }
        ) {
            "Expected a movable spatial panel node named '$panelName' in $packageName"
        }
    }

    /** Reads the translation of [entity] in ActivitySpace on the main thread. */
    private fun activitySpacePosition(entity: Entity): Vector3 {
        lateinit var position: Vector3
        activityScenarioRule.scenario.onActivity {
            position = entity.getPose(Space.ACTIVITY).translation
        }
        return position
    }

    /** Blocks until [listener] has reported the end of the move, and fails on timeout. */
    private fun awaitMoveEnd(listener: RecordingMoveListener) {
        assertWithMessage("Timed out waiting for the move to end")
            .that(listener.moveLatch.await(MOVE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .isTrue()
    }

    private val packageName: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

    private companion object {
        /** Timeout applied when waiting for a move to be reported by the component. */
        const val MOVE_TIMEOUT_SECONDS = 5L

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
         * is therefore kept slightly below `MOVE_DELTA.length` (~0.297m) rather than asserting an
         * exact distance.
         */
        const val MIN_MOVE_DISTANCE_METERS = 0.25f
    }
}

/**
 * An [EntityMoveListener] that records which callbacks were invoked, and counts [moveLatch] down
 * once the move has ended.
 *
 * @param applyMoveToEntity whether the reported transform should be written back to the entity. A
 *   custom movable component only reports the move and leaves it to the app to apply it, whereas a
 *   system movable component is moved by the system itself.
 */
private class RecordingMoveListener(private val applyMoveToEntity: Boolean) : EntityMoveListener {
    val moveLatch: CountDownLatch = CountDownLatch(1)

    // These flags are written on the callback thread and read by the test thread only after
    // moveLatch has been counted down, which establishes the required happens-before edge.
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
        moveLatch.countDown()
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
