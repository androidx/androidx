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
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PointerCaptureComponent
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Device integration tests for [PointerCaptureComponent] lifecycle, state transitions, and captured
 * [InputEvent] delivery (b/581430543).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialPointerAndCaptureComponentTest {

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
    fun pointerCaptureComponent_attachDuplicateAndReattachLifecycle() {
        val session = sessionRule.session
        val panelA =
            createTestPanel(
                name = "capture_panel_a",
                pose = Pose(Vector3(-0.6f, 0f, -2.0f), Quaternion.Identity),
            )
        val panelB =
            createTestPanel(
                name = "capture_panel_b",
                pose = Pose(Vector3(0.6f, 0f, -2.0f), Quaternion.Identity),
            )

        val observedStates = CopyOnWriteArrayList<PointerCaptureComponent.PointerCaptureState>()
        val attachLatch = CountDownLatch(1)
        val stoppedLatch = CountDownLatch(1)
        val executor = Executor { it.run() }

        val capture1 =
            PointerCaptureComponent.create(
                session,
                executor,
                { state ->
                    observedStates.add(state)
                    if (
                        state == PointerCaptureComponent.PointerCaptureState.ACTIVE ||
                            state == PointerCaptureComponent.PointerCaptureState.PAUSED
                    ) {
                        attachLatch.countDown()
                    }
                    if (state == PointerCaptureComponent.PointerCaptureState.STOPPED) {
                        stoppedLatch.countDown()
                    }
                },
                {},
            )
        val capture2 = PointerCaptureComponent.create(session, executor, {}, {})

        assertThat(panelA.addComponent(capture1)).isTrue()

        // A second PointerCaptureComponent on the same entity, or reusing capture1 elsewhere, must
        // be rejected while capture1 is attached.
        assertThat(panelA.addComponent(capture2)).isFalse()
        assertThat(panelB.addComponent(capture1)).isFalse()

        assertWithMessage(
                "PointerCaptureComponent did not enter ACTIVE/PAUSED; observed=$observedStates"
            )
            .that(attachLatch.await(5000L, TimeUnit.MILLISECONDS))
            .isTrue()

        // Detaching clears the active pointer capture registration and allows re-attachment.
        panelA.removeComponent(capture1)
        assertWithMessage(
                "PointerCaptureComponent did not transition to STOPPED; observed=$observedStates"
            )
            .that(stoppedLatch.await(5000L, TimeUnit.MILLISECONDS))
            .isTrue()
        assertThat(observedStates.last())
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.STOPPED)

        assertThat(panelB.addComponent(capture1)).isTrue()

        // A fresh PointerCaptureComponent attaches cleanly once capture1 is removed (reusing
        // rejected capture2 directly is tracked in b/584961273).
        val capture3 = PointerCaptureComponent.create(session, executor, {}, {})
        assertThat(panelA.addComponent(capture3)).isTrue()
    }

    // TODO(b/584961273): Remove @Ignore once rejected attach no longer latches attachedEntity.
    @Ignore(
        "b/584961273: PointerCaptureComponent.onAttach sets attachedEntity before " +
            "addComponent succeeds, permanently blocking reuse after a rejected attach"
    )
    @Test
    fun pointerCaptureComponent_rejectedDuplicateAttach_canStillAttachAfterPreviousRemoved() {
        val session = sessionRule.session
        val panelA =
            createTestPanel(
                name = "capture_rejected_reuse_panel",
                pose = Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
            )
        val executor = Executor { it.run() }
        val capture1 = PointerCaptureComponent.create(session, executor, {}, {})
        val capture2 = PointerCaptureComponent.create(session, executor, {}, {})

        assertThat(panelA.addComponent(capture1)).isTrue()
        assertThat(panelA.addComponent(capture2)).isFalse()

        panelA.removeComponent(capture1)
        assertThat(panelA.addComponent(capture2)).isTrue()
    }

    @Test
    fun pointerCaptureComponent_mainPanelEntity_receivesCapturedInputAndStopsOnDetach() {
        val session = sessionRule.session
        val mainPanel = session.scene.mainPanelEntity
        sessionRule.onMainThread {
            val view =
                TextView(activity).apply {
                    text = "pointer_capture_main_panel"
                    setBackgroundColor(Color.DKGRAY)
                    setTextColor(Color.WHITE)
                }
            activity.setContentView(view)
        }
        mainPanel.setEnabled(true)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val (mainPanelNode, activitySpaceNode) =
            checkNotNull(
                waitForSpatialScene(
                    description = "visible main panel and activity space root SpatialNodes",
                    select = { scene ->
                        val task = scene.findTaskByActivity(sessionRule.activity)
                        val panel =
                            (task?.findNodeByName(MAIN_PANEL_NODE_NAME) ?: task?.taskWindowLeash)
                                ?.takeIf { it.isVisible && it.worldExtents != null }
                        val spaceRoot = task?.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                        if (panel != null && spaceRoot != null) panel to spaceRoot else null
                    },
                    until = { it != null },
                )
            ) {
                "Expected visible main panel and activity space root SpatialNodes"
            }

        val observedStates = CopyOnWriteArrayList<PointerCaptureComponent.PointerCaptureState>()
        val capturedEvents = CopyOnWriteArrayList<InputEvent>()
        val activeLatch = CountDownLatch(1)
        val stoppedLatch = CountDownLatch(1)

        val captureComponent =
            PointerCaptureComponent.create(
                session,
                Executor { it.run() },
                { state ->
                    observedStates.add(state)
                    if (state == PointerCaptureComponent.PointerCaptureState.ACTIVE) {
                        activeLatch.countDown()
                    }
                    if (state == PointerCaptureComponent.PointerCaptureState.STOPPED) {
                        stoppedLatch.countDown()
                    }
                },
                { event -> capturedEvents.add(event) },
            )

        assertThat(mainPanel.addComponent(captureComponent)).isTrue()
        assertWithMessage(
                "PointerCaptureComponent did not transition to ACTIVE; observed=$observedStates"
            )
            .that(activeLatch.await(5000L, TimeUnit.MILLISECONDS))
            .isTrue()

        // Pointer capture intercepts rays aimed at empty background outside Android window bounds.
        activitySpaceNode.performSpatialInteraction {
            moveOriginTo(Vector3(2.5f, 0f, 0.5f))
            pointAt(offset = Vector3(2.5f, 0f, 0f))
            press()
            pointAt(offset = Vector3(2.7f, 0.1f, 0f))
            release()
        }

        waitUntil(
            description = "PointerCaptureComponent to receive InputEvents through Action.UP",
            timeout = 5000.milliseconds,
            pollInterval = 50.milliseconds,
        ) {
            capturedEvents.any { it.action == InputEvent.Action.UP }
        }

        val eventsSnapshot = capturedEvents.toList()
        assertThat(eventsSnapshot.map { it.action })
            .containsAtLeast(
                InputEvent.Action.DOWN,
                InputEvent.Action.MOVE,
                InputEvent.Action.UP,
            )
            .inOrder()
        val downEvent = eventsSnapshot.first { it.action == InputEvent.Action.DOWN }
        // xr_inject_input_cli drops the deviceType argument and synthesizes HANDS events on current
        // platform builds; accept either HANDS or CONTROLLER.
        assertThat(downEvent.source)
            .isIn(listOf(InputEvent.Source.HANDS, InputEvent.Source.CONTROLLER))
        for (event in eventsSnapshot) {
            assertThat(event.direction.length).isGreaterThan(0f)
            assertThat(event.timestamp).isAtLeast(0L)
        }

        mainPanel.removeComponent(captureComponent)
        assertWithMessage(
                "PointerCaptureComponent did not transition to STOPPED on detach; " +
                    "observed=$observedStates"
            )
            .that(stoppedLatch.await(5000L, TimeUnit.MILLISECONDS))
            .isTrue()
        assertThat(observedStates.last())
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.STOPPED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        capturedEvents.clear()

        val postDetachUpLatch = CountDownLatch(1)
        val arrivalInteractable =
            InteractableComponent.create(
                session,
                Executor { it.run() },
                Consumer { event ->
                    if (event.action == InputEvent.Action.UP) {
                        postDetachUpLatch.countDown()
                    }
                },
            )
        assertThat(mainPanel.addComponent(arrivalInteractable)).isTrue()

        activitySpaceNode.performSpatialInteraction {
            moveOriginTo(Vector3(2.5f, 0f, 0.5f))
            pointAt(offset = Vector3(2.5f, 0f, 0f))
            select()
        }
        mainPanelNode.performSpatialInteraction {
            pointAt()
            select()
        }
        assertWithMessage(
                "Temporary InteractableComponent did not receive Action.UP after detaching " +
                    "PointerCaptureComponent"
            )
            .that(postDetachUpLatch.await(5000L, TimeUnit.MILLISECONDS))
            .isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(capturedEvents).isEmpty()
    }

    private fun createTestPanel(name: String, pose: Pose): PanelEntity = sessionRule.onMainThread {
        val view = TextView(activity).apply { text = name }
        PanelEntity.create(
            session,
            view,
            FloatSize2d(0.8f, 0.8f),
            name,
            pose = pose,
            parent = session.scene.activitySpace,
        )
    }

    private companion object {
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"
    }
}
