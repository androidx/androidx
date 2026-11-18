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
import android.graphics.Color as AndroidColor
import android.view.Gravity
import android.view.View
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.ExperimentalSurfaceEntityPixelDimensionsApi
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.DEFAULT_MOVE_HANDLE_MARGIN
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Integration tests for [ResizableComponent], driven by injected raycast input.
 *
 * Scenarios covered:
 * - Baseline resize on `mainPanelEntity`, [PanelEntity], and [ActivityPanelEntity].
 * - Detaching and re-attaching [ResizableComponent] blocks then restores corner resize.
 * - Multi-corner shrink (`BottomRight`) and outward grow (`TopLeft`) updates [PanelEntity.size],
 *   [PanelEntity.sizeInPixels], and preserves [PanelEntity.getPose].
 * - [ResizableComponent.isFixedAspectRatioEnabled] locking aspect ratio during asymmetric drag,
 *   unlocking when toggled off, dynamic portrait (`< 1.0`) to landscape (`> 1.0`) re-lock while
 *   attached via [ResizableComponent.affordanceSize], pre-attach lock before [Entity.addComponent]
 *   on non-square [PanelEntity] and quad [SurfaceEntity], `mainPanelEntity` async `WindowManager`
 *   grow/shrink preservation, and proportional `minimumEntitySize` (`0.80x`) / `maximumEntitySize`
 *   (`1.25x`) clamping with recovery.
 * - [ResizableComponent.minimumEntitySize] and [ResizableComponent.maximumEntitySize] clamp both
 *   initial-bounds drags and dynamic updates while attached.
 * - [ResizableComponent.isAutoHideContentWhileResizingEnabled] hides panel alpha during active
 *   resize drags only when enabled, and restores alpha on completion.
 * - Quad [SurfaceEntity] with [ResizableComponent] supports corner affordance resizing, dispatches
 *   [ResizeEvent] states, and respects min/max size constraints.
 * - [GltfModelEntity] [ResizableComponent] attach/detach, bounds, and listener lifecycle (marked
 *   `@Ignore` under b/582911034 because Impress-internal affordances are not exposed as CPM resize
 *   handles).
 * - Combined [MovableComponent] and [ResizableComponent] on the same [PanelEntity] ("Everything"
 *   panel CUJ).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class ResizableComponentTest {

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
    fun resizableComponent_mainPanelEntity_isResizable() {
        val resizeListener = RecordingResizeListener()

        // 1. Set the Compose content of the main panel.
        sessionRule.onMainThread {
            activity.setContent { CenteredLabel("This is Resizable MainPanel") }
        }

        // 2. Create the ResizableComponent and attach it to the main panel.
        val mainPanelEntity = sessionRule.session.scene.mainPanelEntity
        val resizableComponent =
            ResizableComponent.create(
                session = sessionRule.session,
                resizeEventListener = resizeListener,
            )
        assertThat(mainPanelEntity.addComponent(resizableComponent)).isTrue()

        // 3. Wait for the system to report the main panel as resizable before sampling its size.
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
    fun resizableComponent_panelEntity_isResizable() {
        val panelName = "resizablePanel"
        val resizeListener = RecordingResizeListener()

        val panelEntity = sessionRule.onMainThread {
            // 1. Create the PanelEntity under test, then hide the main panel so it does not
            // interfere with the spatial panel test.
            val panel = createSpatialPanel(panelName, "This is Resizable Spatial Panel")
            session.scene.mainPanelEntity.setEnabled(false)

            // 2. Create the ResizableComponent and attach it to the panel entity.
            val resizableComponent =
                ResizableComponent.create(
                    session = session,
                    resizeEventListener = resizeListener,
                )
            assertThat(panel.addComponent(resizableComponent)).isTrue()
            panel
        }

        // 3. Wait for the system to report the panel as resizable before sampling its size.
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
    fun resizableComponent_activityPanelEntity_isResizable() {
        val panelName = "resizableActivityPanel"
        val resizeListener = RecordingResizeListener()

        try {
            val activityPanelEntity = sessionRule.onMainThread {
                // Embedding an activity in a panel requires the EMBED_ACTIVITY capability.
                assertThat(session.scene.spatialCapabilities)
                    .contains(SpatialCapability.EMBED_ACTIVITY)

                // 1. Create the ActivityPanelEntity under test, hide the main panel so it does not
                // interfere with the activity panel test, and launch an activity into the panel.
                val activityPanel =
                    ActivityPanelEntity.create(
                        session = session,
                        pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                        name = panelName,
                        pose = COMPOSE_PANEL_POSE,
                        parent = session.scene.activitySpace,
                    )
                session.scene.mainPanelEntity.setEnabled(false)

                // Unlike the intent that XrSessionRule's ActivityScenarioRule launches the activity
                // under test with, this intent has no action and no category, so ActivityScenario
                // does not mistake the embedded activity for the activity under test.
                activityPanel.startActivity(Intent(activity, ComponentActivity::class.java))

                // 2. Create the ResizableComponent and attach it to the activity panel entity.
                val resizableComponent =
                    ResizableComponent.create(
                        session = session,
                        resizeEventListener = resizeListener,
                    )
                assertThat(activityPanel.addComponent(resizableComponent)).isTrue()
                activityPanel
            }

            // 3. Wait for the embedded activity to be shown, and for the system to report the panel
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
            sessionRule.finishActivitiesOtherThan()
        }
    }

    @Test
    fun detachAndReattachResizableComponent_blocksThenRestoresCornerResize() {
        val panelName = "detach_reattach_resizable_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, pixelSize = DEFAULT_PANEL_PX)
        }
        val component =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(panel.addComponent(component)).isTrue()

        val initialSize = panel.size
        val initialPx = panel.sizeInPixels
        val initialPose = panel.getPose(Space.ACTIVITY)

        waitForResizablePanelNode(panelName, expectedSize = initialSize)

        // Detach ResizableComponent and verify CPM removes resize capability + handles.
        panel.removeComponent(component)
        assertThat(panel.getComponentsOfType(ResizableComponent::class.java)).isEmpty()

        val detachedNode =
            checkNotNull(
                waitForSpatialScene(
                    description = "CPM resize capability cleared after detach",
                    timeout = SCENE_TIMEOUT,
                    select = { scene ->
                        scene.findTaskByActivity(sessionRule.activity)?.findNode {
                            it.name == panelName &&
                                it.isEmbeddedWindowLeash &&
                                it.isVisible &&
                                it.worldExtents != null &&
                                !it.isResizable &&
                                it.resizeAffordanceNodes.isEmpty()
                        }
                    },
                    until = { it != null },
                )
            )

        // Attempt corner resize at the detached node's BottomRight corner.
        detachedNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.25f, -0.20f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertNoResizeAcrossSettleWindow(
            panel = panel,
            resizeListener = resizeListener,
            expectedSize = initialSize,
            expectedPixelSize = initialPx,
            expectedPose = initialPose,
        )

        // Re-attach the same ResizableComponent and verify corner resizing works again.
        assertThat(panel.addComponent(component)).isTrue()
        assertThat(panel.getComponentsOfType(ResizableComponent::class.java))
            .containsExactly(component)

        val reattachedNode = waitForResizablePanelNode(panelName, expectedSize = initialSize)

        reattachedNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.25f, -0.20f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("Resize END delivered after re-attaching component") {
            resizeListener.hasEnded()
        }

        val afterSize = panel.size
        val afterPx = panel.sizeInPixels
        assertThat(resizeListener.snapshotStates())
            .containsAtLeast(
                ResizeEvent.ResizeState.START,
                ResizeEvent.ResizeState.ONGOING,
                ResizeEvent.ResizeState.END,
            )
        assertThat(afterSize.width).isGreaterThan(initialSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(afterSize.height).isGreaterThan(initialSize.height + MIN_RESIZE_DELTA_METERS)
        assertThat(afterPx.width).isGreaterThan(initialPx.width)
        assertThat(afterPx.height).isGreaterThan(initialPx.height)
    }

    @Test
    fun cornerHandles_shrinkFromBottomRightAndGrowFromTopLeft_updatesSizeAndExtents() {
        val panelName = "multi_corner_shrink_grow_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, pixelSize = LARGE_PANEL_PX)
        }
        val component =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(panel.addComponent(component)).isTrue()

        val initialSize = panel.size
        val initialPx = panel.sizeInPixels
        val initialPose = panel.getPose(Space.ACTIVITY)

        val initialNode = waitForResizablePanelNode(panelName, expectedSize = initialSize)

        // Phase 1: Shrink inward from BottomRight (-X = left, +Y = up), proportional to panel size
        // because SpaceFlinger doubles corner drag deltas relative to panel center (avoiding a 0x0
        // proposedSize that aborts the gesture per b/567260003).
        initialNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(-initialSize.width * 0.22f, initialSize.height * 0.22f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("BottomRight shrink resize END event received") { resizeListener.hasEnded() }
        val shrinkStates = resizeListener.snapshotStates()
        assertThat(shrinkStates)
            .containsAtLeast(
                ResizeEvent.ResizeState.START,
                ResizeEvent.ResizeState.ONGOING,
                ResizeEvent.ResizeState.END,
            )

        val shrunkSize = panel.size
        val shrunkPx = panel.sizeInPixels
        val shrunkPose = panel.getPose(Space.ACTIVITY)
        assertThat(shrunkSize.width).isLessThan(initialSize.width - MIN_RESIZE_DELTA_METERS)
        assertThat(shrunkSize.height).isLessThan(initialSize.height - MIN_RESIZE_DELTA_METERS)
        assertThat(shrunkPx.width).isLessThan(initialPx.width)
        assertThat(shrunkPx.height).isLessThan(initialPx.height)
        assertPose(shrunkPose, initialPose, POSE_EPSILON)

        // Phase 2: Wait for CPM worldExtents to reflect the shrunk panel size before growing
        // outward from TopLeft (-X = left, +Y = up).
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForResizablePanelNode(panelName, expectedSize = shrunkSize).performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(-shrunkSize.width * 0.30f, shrunkSize.height * 0.30f, 0f),
                alignment = SpatialAlignment.TopLeft,
            )
        }

        waitUntil("TopLeft grow resize END event received") { resizeListener.hasEnded() }
        val growStates = resizeListener.snapshotStates()
        assertThat(growStates)
            .containsAtLeast(
                ResizeEvent.ResizeState.START,
                ResizeEvent.ResizeState.ONGOING,
                ResizeEvent.ResizeState.END,
            )

        val grownSize = panel.size
        val grownPx = panel.sizeInPixels
        val grownPose = panel.getPose(Space.ACTIVITY)
        assertThat(grownSize.width).isGreaterThan(shrunkSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownSize.height).isGreaterThan(shrunkSize.height + MIN_RESIZE_DELTA_METERS)
        assertThat(grownPx.width).isGreaterThan(shrunkPx.width)
        assertThat(grownPx.height).isGreaterThan(shrunkPx.height)
        assertPose(grownPose, shrunkPose, POSE_EPSILON)
    }

    @Test
    fun fixedAspectRatio_maintainsAspectRatioAndTogglesOff() {
        val panelName = "fixed_aspect_ratio_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, pixelSize = DEFAULT_PANEL_PX)
        }
        val component =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(panel.addComponent(component)).isTrue()
        // Configure fixedAspectRatio after attach so ResizableComponentImpl resolves the initial
        // aspect ratio from the attached panel's size.
        component.isFixedAspectRatioEnabled = true
        assertThat(component.isFixedAspectRatioEnabled).isTrue()

        val initialSize = panel.size
        val initialAspect = initialSize.width / initialSize.height

        // Asymmetric drag: strong rightward X (+0.32m), small downward Y (-0.06m).
        // With fixed aspect ratio enabled, both width and height scale proportionally.
        waitForResizablePanelNode(panelName, expectedSize = initialSize).performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.32f, -0.06f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("Fixed-aspect resize END received") { resizeListener.hasEnded() }
        val lockedSize = panel.size
        assertThat(lockedSize.width).isGreaterThan(initialSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(lockedSize.height).isGreaterThan(initialSize.height + 0.03f)
        assertWithMessage(
                "Expected aspect ratio to stay near $initialAspect, was ${lockedSize.width / lockedSize.height}"
            )
            .that(abs((lockedSize.width / lockedSize.height) - initialAspect))
            .isLessThan(ASPECT_RATIO_TOLERANCE)

        // Toggle fixedAspectRatio off (true -> false) and repeat an asymmetric drag.
        resizeListener.clear()
        component.isFixedAspectRatioEnabled = false
        assertThat(component.isFixedAspectRatioEnabled).isFalse()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        waitForResizablePanelNode(panelName, expectedSize = lockedSize).performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.32f, -0.04f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("Unlocked-aspect resize END received") { resizeListener.hasEnded() }
        val unlockedSize = panel.size
        assertThat(unlockedSize.width).isGreaterThan(lockedSize.width + 0.15f)
        val unlockedAspect = unlockedSize.width / unlockedSize.height
        assertWithMessage(
                "Expected unlocked aspect ratio ($unlockedAspect) to widen beyond $initialAspect"
            )
            .that(unlockedAspect)
            .isGreaterThan(initialAspect + 0.15f)
    }

    @Test
    fun minimumAndMaximumEntitySize_clampsResizeWithinBounds() {
        val panelName = "min_max_bounds_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, pixelSize = DEFAULT_PANEL_PX)
        }
        // Compute bounds relative to runtime initialSize so expectations stay independent of device
        // defaultPixelsPerMeter density.
        val initialSize = panel.size
        val minBounds = FloatSize3d(initialSize.width * 0.8f, initialSize.height * 0.8f, 0f)
        val maxBounds = FloatSize3d(initialSize.width * 1.25f, initialSize.height * 1.25f, 0f)
        val component =
            ResizableComponent.create(
                session = session,
                minimumSize = minBounds,
                maximumSize = maxBounds,
                resizeEventListener = resizeListener,
            )
        assertThat(component.minimumEntitySize).isEqualTo(minBounds)
        assertThat(component.maximumEntitySize).isEqualTo(maxBounds)
        assertThat(panel.addComponent(component)).isTrue()

        // Phase 1: Overshoot maximumEntitySize (drag 60% larger while cap is 1.25x).
        waitForResizablePanelNode(panelName, expectedSize = initialSize).performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(initialSize.width * 0.6f, -initialSize.height * 0.6f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("Max-clamped resize END received") { resizeListener.hasEnded() }
        val maxEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize
        val afterMaxSize = panel.size
        assertThat(afterMaxSize.width).isGreaterThan(initialSize.width + 0.05f)
        assertThat(afterMaxSize.width).isAtMost(maxBounds.width + BOUND_CLAMP_TOLERANCE)
        assertThat(afterMaxSize.height).isAtMost(maxBounds.height + BOUND_CLAMP_TOLERANCE)
        assertThat(maxEndSize.width).isAtMost(maxBounds.width + BOUND_CLAMP_TOLERANCE)
        assertThat(maxEndSize.height).isAtMost(maxBounds.height + BOUND_CLAMP_TOLERANCE)
        assertThat(afterMaxSize.width).isAtLeast(maxBounds.width - 0.04f)

        // Phase 2: Wait for CPM worldExtents to reflect afterMaxSize, then overshoot
        // minimumEntitySize (drag sharply inward below 0.8x).
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForResizablePanelNode(panelName, expectedSize = afterMaxSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(-initialSize.width * 0.65f, initialSize.height * 0.65f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Min-clamped resize END received") { resizeListener.hasEnded() }
        val minEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize
        val afterMinSize = panel.size
        assertThat(afterMinSize.width).isLessThan(afterMaxSize.width - 0.05f)
        assertThat(afterMinSize.width).isAtLeast(minBounds.width - BOUND_CLAMP_TOLERANCE)
        assertThat(afterMinSize.height).isAtLeast(minBounds.height - BOUND_CLAMP_TOLERANCE)
        assertThat(minEndSize.width).isAtLeast(minBounds.width - BOUND_CLAMP_TOLERANCE)
        assertThat(minEndSize.height).isAtLeast(minBounds.height - BOUND_CLAMP_TOLERANCE)
        assertThat(afterMinSize.width).isAtMost(minBounds.width + 0.04f)

        // Phase 3: Update minimumEntitySize dynamically while attached (raise floor to 0.95x).
        val raisedMinBounds = FloatSize3d(initialSize.width * 0.95f, initialSize.height * 0.95f, 0f)
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForResizablePanelNode(panelName, expectedSize = afterMinSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(initialSize.width * 0.35f, -initialSize.height * 0.35f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }
        waitUntil("Re-grow before dynamic minimum update END received") {
            resizeListener.hasEnded()
        }
        val regrownSize = panel.size
        assertThat(regrownSize.width).isGreaterThan(raisedMinBounds.width + 0.05f)

        resizeListener.clear()
        component.minimumEntitySize = raisedMinBounds
        assertThat(component.minimumEntitySize).isEqualTo(raisedMinBounds)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        waitForResizablePanelNode(panelName, expectedSize = regrownSize).performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(-initialSize.width * 0.45f, initialSize.height * 0.45f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }
        waitUntil("Raised-min clamped resize END received") { resizeListener.hasEnded() }
        val afterRaisedMinSize = panel.size
        assertThat(afterRaisedMinSize.width).isLessThan(regrownSize.width - 0.04f)
        assertThat(afterRaisedMinSize.width)
            .isAtLeast(raisedMinBounds.width - BOUND_CLAMP_TOLERANCE)
        assertThat(afterRaisedMinSize.height)
            .isAtLeast(raisedMinBounds.height - BOUND_CLAMP_TOLERANCE)
        assertThat(afterRaisedMinSize.width).isAtMost(raisedMinBounds.width + 0.04f)
    }

    @Test
    fun autoHideContentWhileResizing_hidesAlphaDuringActiveDragOnlyWhenEnabled() {
        val panelName = "auto_hide_content_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, pixelSize = DEFAULT_PANEL_PX)
        }
        val component =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(component.isAutoHideContentWhileResizingEnabled).isTrue()
        assertThat(panel.addComponent(component)).isTrue()
        val initialSize = panel.size

        // ResizableComponentImpl auto-hides entity content during resize by calling
        // NodeTransaction.setAlpha directly on the CPM Node rather than Entity.setAlpha, so
        // PanelEntity.getAlpha() stays unchanged and CPM requestedAlpha is the observable signal.
        // Phase 1: Default autoHideContent = true hides panel requestedAlpha while pressed.
        waitForResizablePanelNode(panelName, expectedSize = initialSize).performSpatialInteraction {
            pointAtResizeAffordance(alignment = SpatialAlignment.BottomRight)
            press()
            animate(durationMs = DRAG_DURATION_MS) { fraction ->
                pointAtResizeAffordance(
                    offset = Vector3(0.18f * fraction, -0.14f * fraction, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }
            waitForSpatialScene(
                description = "Panel requestedAlpha drops near zero during active resize drag",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == panelName && it.isEmbeddedWindowLeash
                    }
                },
                until = { node ->
                    node != null && node.requestedAlphaOrDefault(1f) <= ALPHA_HIDDEN_THRESHOLD
                },
            )
            release()
        }

        waitUntil("Panel resize END received after release") { resizeListener.hasEnded() }
        waitForSpatialScene(
            description = "Panel requestedAlpha restored after release",
            timeout = SCENE_TIMEOUT,
            select = { scene ->
                scene.findTaskByActivity(sessionRule.activity)?.findNode {
                    it.name == panelName && it.isEmbeddedWindowLeash
                }
            },
            until = { node ->
                node != null && node.requestedAlphaOrDefault(0f) >= ALPHA_VISIBLE_THRESHOLD
            },
        )
        val phase1Size = panel.size

        // Phase 2: Disable autoHideContent and confirm alpha stays 1.0 throughout active drag.
        resizeListener.clear()
        component.isAutoHideContentWhileResizingEnabled = false
        assertThat(component.isAutoHideContentWhileResizingEnabled).isFalse()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        waitForResizablePanelNode(panelName, expectedSize = phase1Size).performSpatialInteraction {
            pointAtResizeAffordance(alignment = SpatialAlignment.BottomRight)
            press()
            animate(durationMs = DRAG_DURATION_MS) { fraction ->
                pointAtResizeAffordance(
                    offset = Vector3(0.16f * fraction, -0.12f * fraction, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }
            waitUntil("Resize ONGOING received while autoHideContent is false") {
                resizeListener.hasOngoing()
            }
            assertAlphaRemainsVisibleAcrossSettleWindow(panelName)
            release()
        }

        waitUntil("Phase 2 resize END received") { resizeListener.hasEnded() }
        val finalNode =
            waitForSpatialScene(
                description = "Panel requestedAlpha visible after Phase 2 resize END",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == panelName && it.isEmbeddedWindowLeash
                    }
                },
                until = { node ->
                    node != null && node.requestedAlphaOrDefault(0f) >= ALPHA_VISIBLE_THRESHOLD
                },
            )
        assertThat(finalNode).isNotNull()
    }

    @OptIn(ExperimentalSurfaceEntityPixelDimensionsApi::class)
    @Test
    fun surfaceEntity_resizableComponent_resizesQuadSurfaceAndRespectsConstraints() {
        val minSize = FloatSize3d(0.85f, 0.68f, 0f)
        val maxSize = FloatSize3d(1.15f, 0.92f, 0f)
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val surface = sessionRule.onMainThread {
            val entity =
                SurfaceEntity.create(
                    session = this.session,
                    pose = SPATIAL_PANEL_POSE,
                    shape = SurfaceEntity.Shape.Quad(SURFACE_QUAD_SIZE),
                    parent = this.session.scene.activitySpace,
                )
            entity.setSurfacePixelDimensions(SURFACE_PIXEL_DIMENSIONS)
            val androidSurface = entity.getSurface()
            val canvas = androidSurface.lockHardwareCanvas()
            try {
                canvas.drawColor(AndroidColor.CYAN)
            } finally {
                androidSurface.unlockCanvasAndPost(canvas)
            }
            entity
        }

        val component =
            ResizableComponent.create(
                session = session,
                minimumSize = minSize,
                maximumSize = maxSize,
                resizeEventListener = resizeListener,
            )
        assertThat(component.minimumEntitySize).isEqualTo(minSize)
        assertThat(component.maximumEntitySize).isEqualTo(maxSize)
        assertThat(surface.addComponent(component)).isTrue()
        val initialDimensions = surface.dimensions

        // Phase 1: Target the BottomRight corner affordance computed from Quad extents plus
        // DEFAULT_MOVE_HANDLE_MARGIN (SplitEngine SurfaceEntity CPM nodes do not expose
        // worldHalfExtents or CPM child resizeAffordanceNodes on Aura SpaceFlinger) and drag
        // outward past maximumSize.
        val growSurfaceNode = waitForResizableSurfaceNode()
        val growCornerLocal =
            Vector3(
                (initialDimensions.width / 2f) + DEFAULT_MOVE_HANDLE_MARGIN,
                -((initialDimensions.height / 2f) + DEFAULT_MOVE_HANDLE_MARGIN),
                0f,
            )
        val growDelta = Vector3(0.25f, -0.20f, 0f)

        growSurfaceNode.performSpatialInteraction {
            pointAt(offset = growCornerLocal)
            press()
            animate(durationMs = DRAG_DURATION_MS) { fraction ->
                pointAt(offset = growCornerLocal + (growDelta * fraction))
            }
            release()
        }

        waitUntil("SurfaceEntity Phase 1 resize END received") { resizeListener.hasEnded() }
        val phase1States = resizeListener.snapshotStates()
        assertThat(phase1States)
            .containsAtLeast(ResizeEvent.ResizeState.START, ResizeEvent.ResizeState.END)
        val phase1EndSize = checkNotNull(resizeListener.lastEndEvent()).newSize
        assertThat(phase1EndSize.width).isWithin(0.06f).of(maxSize.width)
        assertThat(phase1EndSize.height).isWithin(0.06f).of(maxSize.height)
        assertThat(phase1EndSize.width).isAtMost(maxSize.width + BOUND_CLAMP_TOLERANCE)
        assertThat(phase1EndSize.height).isAtMost(maxSize.height + BOUND_CLAMP_TOLERANCE)
        assertThat(phase1EndSize.width).isAtLeast(minSize.width - BOUND_CLAMP_TOLERANCE)
        assertThat(phase1EndSize.height).isAtLeast(minSize.height - BOUND_CLAMP_TOLERANCE)

        val grownShape = surface.shape
        val grownDimensions = surface.dimensions
        assertThat(grownShape).isInstanceOf(SurfaceEntity.Shape.Quad::class.java)
        assertThat(grownDimensions.width)
            .isGreaterThan(initialDimensions.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownDimensions.height)
            .isGreaterThan(initialDimensions.height + MIN_RESIZE_DELTA_METERS)
        assertThat(grownDimensions.width).isWithin(0.06f).of(maxSize.width)
        assertThat(grownDimensions.height).isWithin(0.06f).of(maxSize.height)
        assertThat(grownDimensions.width).isAtMost(maxSize.width + BOUND_CLAMP_TOLERANCE)
        assertThat(grownDimensions.height).isAtMost(maxSize.height + BOUND_CLAMP_TOLERANCE)

        // Phase 2: Target the updated BottomRight corner affordance and drag inward past
        // minimumSize to verify lower-bound clamping.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        resizeListener.clear()
        val shrinkSurfaceNode = waitForResizableSurfaceNode()
        val shrinkCornerLocal =
            Vector3(
                (grownDimensions.width / 2f) + DEFAULT_MOVE_HANDLE_MARGIN,
                -((grownDimensions.height / 2f) + DEFAULT_MOVE_HANDLE_MARGIN),
                0f,
            )
        val shrinkDelta = Vector3(-0.35f, 0.28f, 0f)

        shrinkSurfaceNode.performSpatialInteraction {
            pointAt(offset = shrinkCornerLocal)
            press()
            animate(durationMs = DRAG_DURATION_MS) { fraction ->
                pointAt(offset = shrinkCornerLocal + (shrinkDelta * fraction))
            }
            release()
        }

        waitUntil("SurfaceEntity Phase 2 resize END received") { resizeListener.hasEnded() }
        val phase2States = resizeListener.snapshotStates()
        assertThat(phase2States)
            .containsAtLeast(ResizeEvent.ResizeState.START, ResizeEvent.ResizeState.END)
        val phase2EndSize = checkNotNull(resizeListener.lastEndEvent()).newSize
        assertThat(phase2EndSize.width).isWithin(0.06f).of(minSize.width)
        assertThat(phase2EndSize.height).isWithin(0.06f).of(minSize.height)
        assertThat(phase2EndSize.width).isAtLeast(minSize.width - BOUND_CLAMP_TOLERANCE)
        assertThat(phase2EndSize.height).isAtLeast(minSize.height - BOUND_CLAMP_TOLERANCE)
        assertThat(phase2EndSize.width).isAtMost(maxSize.width + BOUND_CLAMP_TOLERANCE)
        assertThat(phase2EndSize.height).isAtMost(maxSize.height + BOUND_CLAMP_TOLERANCE)

        val shrunkShape = surface.shape
        val shrunkDimensions = surface.dimensions
        assertThat(shrunkShape).isInstanceOf(SurfaceEntity.Shape.Quad::class.java)
        assertThat(shrunkDimensions.width)
            .isLessThan(grownDimensions.width - MIN_RESIZE_DELTA_METERS)
        assertThat(shrunkDimensions.height)
            .isLessThan(grownDimensions.height - MIN_RESIZE_DELTA_METERS)
        assertThat(shrunkDimensions.width).isWithin(0.06f).of(minSize.width)
        assertThat(shrunkDimensions.height).isWithin(0.06f).of(minSize.height)
        assertThat(shrunkDimensions.width).isAtLeast(minSize.width - BOUND_CLAMP_TOLERANCE)
        assertThat(shrunkDimensions.height).isAtLeast(minSize.height - BOUND_CLAMP_TOLERANCE)
    }

    @Ignore(
        "b/582911034: Impress-internal glTF resize affordances are not exposed as CPM resize handles for device drag targeting"
    )
    @Test
    fun gltfModelEntity_supportsResizableComponentLifecycleAndBounds() {
        val initialPose = Pose(Vector3(0f, -0.25f, -1.5f), Quaternion.Identity)
        val session = sessionRule.session
        val activity = sessionRule.activity
        val resizeListener = RecordingResizeListener(applyResizeToEntity = false)
        val secondaryListener = Consumer<ResizeEvent> {}

        session.scene.mainPanelEntity.setEnabled(false)
        val gltfEntity = sessionRule.onMainThread {
            val model =
                GltfModel.create(this.session, Paths.get(GLTF_DIR, BOUNDING_BOX_MODEL_ASSET))
            GltfModelEntity.create(
                session = this.session,
                model = model,
                pose = initialPose,
                parent = this.session.scene.activitySpace,
            )
        }

        val component =
            ResizableComponent.create(
                    session = session,
                    minimumSize = FloatSize3d(0.25f, 0.25f, 0.25f),
                    maximumSize = FloatSize3d(2.0f, 2.0f, 2.0f),
                    resizeEventListener = resizeListener,
                )
                .apply {
                    isFixedAspectRatioEnabled = true
                    addResizeEventListener(
                        ContextCompat.getMainExecutor(activity),
                        secondaryListener,
                    )
                }
        assertThat(gltfEntity.addComponent(component)).isTrue()
        assertThat(gltfEntity.getComponentsOfType(ResizableComponent::class.java))
            .containsExactly(component)

        val subspaceNode =
            checkNotNull(
                waitForSpatialScene(
                    description = "GltfModelEntity SubspaceNode to appear in app task",
                    timeout = SCENE_TIMEOUT,
                    select = { scene ->
                        scene.findTaskByActivity(sessionRule.activity)?.findNode {
                            it.name.startsWith(GLTF_ENTITY_SUBSPACE_PREFIX)
                        }
                    },
                    until = { it != null },
                )
            )
        assertThat(subspaceNode.hasSubspace).isTrue()

        // ResizableComponentImpl uses SplitEngine/Impress affordances rather than SysUI CPM
        // ReformOptions for GltfModelEntity; verify attach/bounds/CPM state and clean detach.
        // Tracking Impress affordance targeting in SpatialInteractionHelper under b/582911034.

        // Update bounds and listener registrations dynamically while attached, then detach.
        val updatedMin = FloatSize3d(0.35f, 0.35f, 0.35f)
        val updatedMax = FloatSize3d(1.75f, 1.75f, 1.75f)
        component.minimumEntitySize = updatedMin
        component.maximumEntitySize = updatedMax
        assertThat(component.minimumEntitySize).isEqualTo(updatedMin)
        assertThat(component.maximumEntitySize).isEqualTo(updatedMax)

        component.removeResizeEventListener(secondaryListener)
        gltfEntity.removeComponent(component)
        assertThat(gltfEntity.getComponentsOfType(ResizableComponent::class.java)).isEmpty()

        assertPose(gltfEntity.getPose(Space.ACTIVITY), initialPose, POSE_EPSILON)
        assertThat(gltfEntity.getScale(Space.ACTIVITY)).isWithin(POSE_EPSILON).of(1f)
    }

    @Test
    fun movableAndResizableComponents_coexistOnSamePanel() {
        val panelName = "everything_movable_resizable_panel"
        val session = sessionRule.session
        val activity = sessionRule.activity
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)
        val moveListener = RecordingMoveListener()

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, pixelSize = DEFAULT_PANEL_PX)
        }
        val movable =
            MovableComponent.createSystemMovable(
                    session = session,
                    scaleInZ = false,
                )
                .apply {
                    addMoveListener(
                        ContextCompat.getMainExecutor(activity),
                        moveListener,
                    )
                }
        val resizable =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(panel.addComponent(movable)).isTrue()
        assertThat(panel.addComponent(resizable)).isTrue()
        val initialSize = panel.size
        val initialPx = panel.sizeInPixels

        val initialNode =
            waitForResizablePanelNode(panelName, expectedSize = initialSize) { it.isMovable }

        // Step 1: Resize from BottomRight corner and confirm move events do NOT fire.
        initialNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.24f, -0.18f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("Coexistence step 1 resize END received") { resizeListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val resizedSize = panel.size
        val resizedPx = panel.sizeInPixels
        val postResizePose = panel.getPose(Space.ACTIVITY)
        assertThat(resizedSize.width).isGreaterThan(initialSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(resizedSize.height).isGreaterThan(initialSize.height + MIN_RESIZE_DELTA_METERS)
        assertThat(resizedPx.width).isGreaterThan(initialPx.width)
        assertThat(resizedPx.height).isGreaterThan(initialPx.height)
        assertThat(moveListener.hasStarted()).isFalse()

        // Step 2: Wait for CPM worldExtents to reflect resizedSize, then move panel cleanly without
        // triggering resize or corrupting dimensions.
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val postResizeNode =
            waitForResizablePanelNode(panelName, expectedSize = resizedSize) { it.isMovable }
        postResizeNode.performSpatialInteraction { moveNodeBy(delta = COEXIST_MOVE_DELTA) }

        waitUntil("Coexistence step 2 move END received") { moveListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val movedPose = panel.getPose(Space.ACTIVITY)
        val sizeAfterMove = panel.size
        val pxAfterMove = panel.sizeInPixels
        assertThat(moveListener.snapshotStates())
            .containsAtLeast(
                MoveState.START,
                MoveState.ONGOING,
                MoveState.END,
            )
        assertThat(Vector3.distance(movedPose.translation, postResizePose.translation))
            .isAtLeast(MIN_COEXIST_MOVE_DISTANCE_METERS)
        assertThat(resizeListener.snapshotStates()).isEmpty()
        assertThat(sizeAfterMove).isEqualTo(resizedSize)
        assertThat(pxAfterMove).isEqualTo(resizedPx)

        // Step 3: Wait for CPM node to reflect the moved position and resized extents, then verify
        // corner resize still succeeds at the new moved location.
        moveListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForResizablePanelNode(panelName, expectedSize = resizedSize) { node ->
                node.isMovable && Vector3.distance(node.position, postResizeNode.position) >= 0.08f
            }
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(-0.14f, 0.10f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Coexistence step 3 post-move resize END received") { resizeListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val finalSize = panel.size
        assertThat(finalSize.width).isLessThan(resizedSize.width - 0.05f)
        assertThat(finalSize.height).isLessThan(resizedSize.height - 0.04f)
        assertThat(moveListener.hasStarted()).isFalse()
    }

    @Test
    fun fixedAspectRatio_portraitToLandscapeRelockWhileAttached_enforcesUpdatedRatio() {
        val panelName = "portrait_landscape_relock_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, dimensions = PORTRAIT_PANEL_SIZE)
        }
        val portraitSize = panel.size
        val portraitAspect = portraitSize.width / portraitSize.height
        assertThat(portraitAspect).isLessThan(1.0f)
        assertThat(portraitAspect)
            .isWithin(0.03f)
            .of(PORTRAIT_PANEL_SIZE.width / PORTRAIT_PANEL_SIZE.height)

        val component =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(panel.addComponent(component)).isTrue()
        component.isFixedAspectRatioEnabled = true
        assertThat(component.isFixedAspectRatioEnabled).isTrue()

        // Phase 1: Asymmetric drag on portrait panel preserves portrait (< 1.0) aspect ratio across
        // ONGOING events, END event, and settled PanelEntity.size.
        waitForResizablePanelNode(panelName, expectedSize = portraitSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(0.26f, -0.05f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Portrait fixed-aspect resize END received") { resizeListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val portraitOngoing = resizeListener.ongoingEvents()
        assertThat(portraitOngoing).isNotEmpty()
        portraitOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = portraitAspect,
                label = "Portrait ONGOING newSize",
            )
        }
        val portraitEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = portraitEndSize,
            expectedAspect = portraitAspect,
            label = "Portrait END newSize",
        )
        val grownPortraitSize = panel.size
        assertThat(grownPortraitSize.width)
            .isGreaterThan(portraitSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownPortraitSize.height)
            .isGreaterThan(portraitSize.height + MIN_RESIZE_DELTA_METERS)
        assertAspectRatioPreserved(
            size = grownPortraitSize,
            expectedAspect = portraitAspect,
            label = "Portrait settled panel.size",
        )

        // Phase 2: Reshape panel to landscape while component remains attached, sync affordanceSize
        // with fixed aspect enabled, and wait for CPM worldExtents before dragging.
        assertThat(panel.getComponentsOfType(ResizableComponent::class.java))
            .containsExactly(component)
        resizeListener.clear()
        panel.size = LANDSCAPE_PANEL_SIZE
        val landscapeSize = panel.size
        val landscapeAspect = landscapeSize.width / landscapeSize.height
        assertThat(landscapeAspect).isGreaterThan(1.0f)
        assertThat(landscapeAspect)
            .isWithin(0.04f)
            .of(LANDSCAPE_PANEL_SIZE.width / LANDSCAPE_PANEL_SIZE.height)

        component.affordanceSize = landscapeSize.to3d()
        assertThat(component.affordanceSize).isEqualTo(landscapeSize.to3d())
        assertThat(component.isFixedAspectRatioEnabled).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        waitForResizablePanelNode(panelName, expectedSize = landscapeSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(0.06f, -0.24f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Landscape re-locked resize END received") { resizeListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val landscapeOngoing = resizeListener.ongoingEvents()
        assertThat(landscapeOngoing).isNotEmpty()
        landscapeOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = landscapeAspect,
                label = "Landscape re-locked ONGOING newSize",
            )
        }
        val landscapeEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = landscapeEndSize,
            expectedAspect = landscapeAspect,
            label = "Landscape re-locked END newSize",
        )
        val grownLandscapeSize = panel.size
        assertThat(grownLandscapeSize.width)
            .isGreaterThan(landscapeSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownLandscapeSize.height)
            .isGreaterThan(landscapeSize.height + MIN_RESIZE_DELTA_METERS)
        assertAspectRatioPreserved(
            size = grownLandscapeSize,
            expectedAspect = landscapeAspect,
            label = "Landscape re-locked panel.size",
        )
    }

    @OptIn(ExperimentalSurfaceEntityPixelDimensionsApi::class)
    @Test
    fun fixedAspectRatio_enabledBeforeAttach_locksNonSquarePanelAndSurfaceRatios() {
        val panelName = "preattach_fixed_aspect_panel"
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        // Part A: Enable isFixedAspectRatioEnabled BEFORE PanelEntity.addComponent on a 2:1 panel.
        val panelListener = RecordingResizeListener(applyResizeToEntity = true)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, dimensions = TWO_BY_ONE_SIZE)
        }
        val initialPanelSize = panel.size
        val expectedPanelAspect = initialPanelSize.width / initialPanelSize.height
        assertThat(expectedPanelAspect)
            .isWithin(0.04f)
            .of(TWO_BY_ONE_SIZE.width / TWO_BY_ONE_SIZE.height)

        val panelComponent =
            ResizableComponent.create(
                    session = session,
                    resizeEventListener = panelListener,
                )
                .apply { isFixedAspectRatioEnabled = true }
        assertThat(panelComponent.isFixedAspectRatioEnabled).isTrue()
        assertThat(panel.addComponent(panelComponent)).isTrue()

        // Predominantly vertical drag ensures a stale 1:1 DIMS_ONE lock would fail the ~2.0 check.
        waitForResizablePanelNode(panelName, expectedSize = initialPanelSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(0.05f, -0.22f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Pre-attach fixed-aspect panel resize END received") { panelListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val panelOngoing = panelListener.ongoingEvents()
        assertThat(panelOngoing).isNotEmpty()
        panelOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = expectedPanelAspect,
                label = "Pre-attach Panel ONGOING newSize",
            )
        }
        val panelEndSize = checkNotNull(panelListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = panelEndSize,
            expectedAspect = expectedPanelAspect,
            label = "Pre-attach Panel END newSize",
        )
        val grownPanelSize = panel.size
        assertThat(grownPanelSize.width)
            .isGreaterThan(initialPanelSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownPanelSize.height).isGreaterThan(initialPanelSize.height + 0.03f)
        assertAspectRatioPreserved(
            size = grownPanelSize,
            expectedAspect = expectedPanelAspect,
            label = "Pre-attach PanelEntity.size",
        )

        // Hide the panel before spawning the 2:1 SurfaceEntity at SPATIAL_PANEL_POSE.
        panel.setEnabled(false)
        waitForSpatialScene(
            description = "Pre-attach 2:1 PanelEntity hidden before SurfaceEntity check",
            timeout = SCENE_TIMEOUT,
            select = { scene ->
                scene.findTaskByActivity(sessionRule.activity)?.findNode {
                    it.name == panelName && it.isEmbeddedWindowLeash
                }
            },
            until = { node -> node == null || !node.isVisible },
        )

        // Part B: Enable isFixedAspectRatioEnabled BEFORE SurfaceEntity.addComponent on a 2:1 Quad.
        val surfaceListener = RecordingResizeListener(applyResizeToEntity = true)
        val surface = sessionRule.onMainThread {
            val entity =
                SurfaceEntity.create(
                    session = this.session,
                    pose = SPATIAL_PANEL_POSE,
                    shape = SurfaceEntity.Shape.Quad(TWO_BY_ONE_SIZE),
                    parent = this.session.scene.activitySpace,
                )
            entity.setSurfacePixelDimensions(SURFACE_PIXEL_DIMENSIONS)
            val androidSurface = entity.getSurface()
            val canvas = androidSurface.lockHardwareCanvas()
            try {
                canvas.drawColor(AndroidColor.MAGENTA)
            } finally {
                androidSurface.unlockCanvasAndPost(canvas)
            }
            entity
        }
        val initialSurfaceDims = surface.dimensions
        val expectedSurfaceAspect = initialSurfaceDims.width / initialSurfaceDims.height
        assertThat(expectedSurfaceAspect)
            .isWithin(0.04f)
            .of(TWO_BY_ONE_SIZE.width / TWO_BY_ONE_SIZE.height)

        val surfaceComponent =
            ResizableComponent.create(
                    session = session,
                    resizeEventListener = surfaceListener,
                )
                .apply { isFixedAspectRatioEnabled = true }
        assertThat(surfaceComponent.isFixedAspectRatioEnabled).isTrue()
        assertThat(surface.addComponent(surfaceComponent)).isTrue()

        val surfaceNode = waitForResizableSurfaceNode()
        val cornerLocal =
            Vector3(
                (initialSurfaceDims.width / 2f) + DEFAULT_MOVE_HANDLE_MARGIN,
                -((initialSurfaceDims.height / 2f) + DEFAULT_MOVE_HANDLE_MARGIN),
                0f,
            )
        val dragDelta = Vector3(0.05f, -0.20f, 0f)
        surfaceNode.performSpatialInteraction {
            pointAt(offset = cornerLocal)
            press()
            animate(durationMs = DRAG_DURATION_MS) { fraction ->
                pointAt(offset = cornerLocal + (dragDelta * fraction))
            }
            release()
        }

        waitUntil("Pre-attach fixed-aspect SurfaceEntity resize END received") {
            surfaceListener.hasEnded()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(surfaceListener.snapshotStates())
            .containsAtLeast(ResizeEvent.ResizeState.START, ResizeEvent.ResizeState.END)
        val surfaceOngoing = surfaceListener.ongoingEvents()
        assertThat(surfaceOngoing).isNotEmpty()
        surfaceOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = expectedSurfaceAspect,
                label = "Pre-attach SurfaceEntity ONGOING newSize",
            )
        }
        val surfaceEndSize = checkNotNull(surfaceListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = surfaceEndSize,
            expectedAspect = expectedSurfaceAspect,
            label = "Pre-attach SurfaceEntity END newSize",
        )
        val grownSurfaceDims = surface.dimensions
        assertThat(grownSurfaceDims.width)
            .isGreaterThan(initialSurfaceDims.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownSurfaceDims.height).isGreaterThan(initialSurfaceDims.height + 0.03f)
        assertAspectRatioPreserved(
            size = grownSurfaceDims.to2d(),
            expectedAspect = expectedSurfaceAspect,
            label = "Pre-attach SurfaceEntity.dimensions",
        )
    }

    @Test
    fun fixedAspectRatio_mainPanelEntity_preservesRatioAcrossAsyncGrowAndShrink() {
        val session = sessionRule.session
        val mainPanel = session.scene.mainPanelEntity
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)
        assertThat(mainPanel.isEnabled(includeParents = false)).isTrue()

        sessionRule.onMainThread {
            val contentView =
                TextView(activity).apply {
                    text = "Resizable Fixed-Aspect MainPanel"
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(AndroidColor.BLACK)
                    setBackgroundColor(AndroidColor.WHITE)
                }
            activity.setContentView(contentView)
        }

        mainPanel.size = MAIN_PANEL_INITIAL_SIZE
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitUntil(
            description = "mainPanelEntity async initial bounds settled",
            timeout = SCENE_TIMEOUT,
        ) {
            val currentSize = mainPanel.size
            val currentPx = mainPanel.sizeInPixels
            currentPx.width > 0 &&
                currentPx.height > 0 &&
                abs(currentSize.width - MAIN_PANEL_INITIAL_SIZE.width) <= BOUND_CLAMP_TOLERANCE &&
                abs(currentSize.height - MAIN_PANEL_INITIAL_SIZE.height) <= BOUND_CLAMP_TOLERANCE
        }

        val initialSize = mainPanel.size
        val initialPx = mainPanel.sizeInPixels
        val initialAspect = initialSize.width / initialSize.height
        assertThat(initialAspect)
            .isWithin(0.05f)
            .of(MAIN_PANEL_INITIAL_SIZE.width / MAIN_PANEL_INITIAL_SIZE.height)

        val component =
            ResizableComponent.create(
                session = session,
                resizeEventListener = resizeListener,
            )
        assertThat(mainPanel.addComponent(component)).isTrue()
        component.isFixedAspectRatioEnabled = true
        assertThat(component.isFixedAspectRatioEnabled).isTrue()

        // Phase 1: Asymmetric outward grow on MainPanelAndTaskWindowLeashNode.
        val initialMainNode = waitForResizableMainPanelNode(expectedSize = initialSize)
        initialMainNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.28f, -0.05f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("mainPanelEntity grow resize END received") { resizeListener.hasEnded() }
        val growOngoing = resizeListener.ongoingEvents()
        assertThat(growOngoing).isNotEmpty()
        growOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = initialAspect,
                label = "mainPanelEntity grow ONGOING newSize",
            )
        }
        val growEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = growEndSize,
            expectedAspect = initialAspect,
            label = "mainPanelEntity grow END newSize",
        )

        waitUntil(
            description = "mainPanelEntity async WindowManager bounds settled after grow",
            timeout = SCENE_TIMEOUT,
        ) {
            val currentSize = mainPanel.size
            val currentPx = mainPanel.sizeInPixels
            currentPx.width > initialPx.width &&
                currentPx.height > initialPx.height &&
                abs(currentSize.width - growEndSize.width) <= BOUND_CLAMP_TOLERANCE &&
                abs(currentSize.height - growEndSize.height) <= BOUND_CLAMP_TOLERANCE
        }
        val grownSize = mainPanel.size
        val grownPx = mainPanel.sizeInPixels
        assertThat(grownSize.width).isGreaterThan(initialSize.width + MIN_RESIZE_DELTA_METERS)
        assertThat(grownSize.height).isGreaterThan(initialSize.height + 0.03f)
        assertAspectRatioPreserved(
            size = grownSize,
            expectedAspect = initialAspect,
            label = "Grown mainPanelEntity.size",
        )

        // Phase 2: Re-capture settled MainPanelAndTaskWindowLeashNode and shrink inward
        // proportionally to prevent a 0x0 proposedSize abort (b/567260003).
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val grownMainNode = waitForResizableMainPanelNode(expectedSize = grownSize)
        grownMainNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(-grownSize.width * 0.20f, grownSize.height * 0.06f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("mainPanelEntity shrink resize END received") { resizeListener.hasEnded() }
        val shrinkOngoing = resizeListener.ongoingEvents()
        assertThat(shrinkOngoing).isNotEmpty()
        shrinkOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = initialAspect,
                label = "mainPanelEntity shrink ONGOING newSize",
            )
        }
        val shrinkEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = shrinkEndSize,
            expectedAspect = initialAspect,
            label = "mainPanelEntity shrink END newSize",
        )

        waitUntil(
            description = "mainPanelEntity async WindowManager bounds settled after shrink",
            timeout = SCENE_TIMEOUT,
        ) {
            val currentSize = mainPanel.size
            val currentPx = mainPanel.sizeInPixels
            currentPx.width < grownPx.width &&
                currentPx.height < grownPx.height &&
                abs(currentSize.width - shrinkEndSize.width) <= BOUND_CLAMP_TOLERANCE &&
                abs(currentSize.height - shrinkEndSize.height) <= BOUND_CLAMP_TOLERANCE
        }
        val shrunkSize = mainPanel.size
        assertThat(shrunkSize.width).isLessThan(grownSize.width - MIN_RESIZE_DELTA_METERS)
        assertThat(shrunkSize.height).isLessThan(grownSize.height - 0.03f)
        assertAspectRatioPreserved(
            size = shrunkSize,
            expectedAspect = initialAspect,
            label = "Shrunk mainPanelEntity.size",
        )
    }

    @Test
    fun fixedAspectRatio_withMinMaxBounds_clampsCleanlyAndPreservesRatioOnRecovery() {
        val panelName = "fixed_aspect_min_max_panel"
        val session = sessionRule.session
        val resizeListener = RecordingResizeListener(applyResizeToEntity = true)

        session.scene.mainPanelEntity.setEnabled(false)
        val panel = sessionRule.onMainThread {
            createSpatialPanel(name = panelName, dimensions = SIXTEEN_BY_NINE_PANEL_SIZE)
        }
        val initialSize = panel.size
        val initialAspect = initialSize.width / initialSize.height
        val minBounds = FloatSize3d(initialSize.width * 0.80f, initialSize.height * 0.80f, 0f)
        val maxBounds = FloatSize3d(initialSize.width * 1.25f, initialSize.height * 1.25f, 0f)

        val component =
            ResizableComponent.create(
                session = session,
                minimumSize = minBounds,
                maximumSize = maxBounds,
                resizeEventListener = resizeListener,
            )
        assertThat(panel.addComponent(component)).isTrue()
        component.isFixedAspectRatioEnabled = true
        assertThat(component.isFixedAspectRatioEnabled).isTrue()

        // Phase 1: Asymmetric outward drag overshooting maximumEntitySize on both axes regardless
        // of fixed-ratio axis projection (1.45x X / 1.28x Y even before 2x corner scaling).
        waitForResizablePanelNode(panelName, expectedSize = initialSize).performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(initialSize.width * 0.45f, -initialSize.height * 0.28f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        waitUntil("Fixed-aspect max-clamped resize END received") { resizeListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val maxOngoing = resizeListener.ongoingEvents()
        assertThat(maxOngoing).isNotEmpty()
        maxOngoing.forEach { event ->
            val candidate = event.newSize.to2d()
            assertThat(candidate.width).isAtMost(maxBounds.width + BOUND_CLAMP_TOLERANCE)
            assertThat(candidate.height).isAtMost(maxBounds.height + BOUND_CLAMP_TOLERANCE)
            assertThat(candidate.width).isAtLeast(minBounds.width - BOUND_CLAMP_TOLERANCE)
            assertThat(candidate.height).isAtLeast(minBounds.height - BOUND_CLAMP_TOLERANCE)
            assertAspectRatioPreserved(
                size = candidate,
                expectedAspect = initialAspect,
                label = "Max-clamped ONGOING newSize",
            )
        }
        val maxEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertThat(maxEndSize.width).isAtMost(maxBounds.width + BOUND_CLAMP_TOLERANCE)
        assertThat(maxEndSize.height).isAtMost(maxBounds.height + BOUND_CLAMP_TOLERANCE)
        assertAspectRatioPreserved(
            size = maxEndSize,
            expectedAspect = initialAspect,
            label = "Max-clamped END newSize",
        )
        val afterMaxSize = panel.size
        assertThat(afterMaxSize.width).isAtMost(maxBounds.width + BOUND_CLAMP_TOLERANCE)
        assertThat(afterMaxSize.height).isAtMost(maxBounds.height + BOUND_CLAMP_TOLERANCE)
        assertThat(afterMaxSize.width).isAtLeast(maxBounds.width - 0.04f)
        assertThat(afterMaxSize.height).isAtLeast(maxBounds.height - 0.04f)
        assertAspectRatioPreserved(
            size = afterMaxSize,
            expectedAspect = initialAspect,
            label = "Max-clamped panel.size",
        )

        // Phase 2: Proportional inward shrink overshooting minimumEntitySize on both axes
        // (0.20x initial under 2x corner doubling, 0.725x under 1x) without reaching a 0x0 raw
        // proposedSize (b/567260003).
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForResizablePanelNode(panelName, expectedSize = afterMaxSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(-afterMaxSize.width * 0.42f, afterMaxSize.height * 0.42f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Fixed-aspect min-clamped resize END received") { resizeListener.hasEnded() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val minOngoing = resizeListener.ongoingEvents()
        assertThat(minOngoing).isNotEmpty()
        minOngoing.forEach { event ->
            val candidate = event.newSize.to2d()
            assertThat(candidate.width).isAtLeast(minBounds.width - BOUND_CLAMP_TOLERANCE)
            assertThat(candidate.height).isAtLeast(minBounds.height - BOUND_CLAMP_TOLERANCE)
            assertThat(candidate.width).isAtMost(maxBounds.width + BOUND_CLAMP_TOLERANCE)
            assertThat(candidate.height).isAtMost(maxBounds.height + BOUND_CLAMP_TOLERANCE)
            assertAspectRatioPreserved(
                size = candidate,
                expectedAspect = initialAspect,
                label = "Min-clamped ONGOING newSize",
            )
        }
        val minEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertThat(minEndSize.width).isAtLeast(minBounds.width - BOUND_CLAMP_TOLERANCE)
        assertThat(minEndSize.height).isAtLeast(minBounds.height - BOUND_CLAMP_TOLERANCE)
        assertAspectRatioPreserved(
            size = minEndSize,
            expectedAspect = initialAspect,
            label = "Min-clamped END newSize",
        )
        val afterMinSize = panel.size
        assertThat(afterMinSize.width).isLessThan(afterMaxSize.width - MIN_RESIZE_DELTA_METERS)
        assertThat(afterMinSize.width).isAtLeast(minBounds.width - BOUND_CLAMP_TOLERANCE)
        assertThat(afterMinSize.height).isAtLeast(minBounds.height - BOUND_CLAMP_TOLERANCE)
        assertThat(afterMinSize.width).isAtMost(minBounds.width + 0.04f)
        assertThat(afterMinSize.height).isAtMost(minBounds.height + 0.04f)
        assertAspectRatioPreserved(
            size = afterMinSize,
            expectedAspect = initialAspect,
            label = "Min-clamped panel.size",
        )

        // Phase 3: Re-drag asymmetrically back into the interior valid range and confirm aspect
        // ratio stays uncorrupted after hitting both clamps.
        resizeListener.clear()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        waitForResizablePanelNode(panelName, expectedSize = afterMinSize)
            .performSpatialInteraction {
                resizeNodeBy(
                    delta = Vector3(initialSize.width * 0.15f, -initialSize.height * 0.06f, 0f),
                    alignment = SpatialAlignment.BottomRight,
                )
            }

        waitUntil("Fixed-aspect recovery mid-range resize END received") {
            resizeListener.hasEnded()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val recoveryOngoing = resizeListener.ongoingEvents()
        assertThat(recoveryOngoing).isNotEmpty()
        recoveryOngoing.forEach { event ->
            assertAspectRatioPreserved(
                size = event.newSize.to2d(),
                expectedAspect = initialAspect,
                label = "Recovery ONGOING newSize",
            )
        }
        val recoveryEndSize = checkNotNull(resizeListener.lastEndEvent()).newSize.to2d()
        assertAspectRatioPreserved(
            size = recoveryEndSize,
            expectedAspect = initialAspect,
            label = "Recovery END newSize",
        )
        val recoveredSize = panel.size
        assertThat(recoveredSize.width).isGreaterThan(minBounds.width + 0.03f)
        assertThat(recoveredSize.width).isLessThan(maxBounds.width - 0.03f)
        assertThat(recoveredSize.height).isGreaterThan(minBounds.height + 0.02f)
        assertThat(recoveredSize.height).isLessThan(maxBounds.height - 0.02f)
        assertAspectRatioPreserved(
            size = recoveredSize,
            expectedAspect = initialAspect,
            label = "Recovered panel.size",
        )
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
                pose = COMPOSE_PANEL_POSE,
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

    private fun XrSessionScope<ComponentActivity>.createSpatialPanel(
        name: String,
        pixelSize: IntSize2d,
    ): PanelEntity {
        val view =
            TextView(activity).apply {
                text = name
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(AndroidColor.WHITE)
                setBackgroundColor(AndroidColor.DKGRAY)
            }
        return PanelEntity.create(
            session = session,
            view = view,
            pixelDimensions = pixelSize,
            name = name,
            pose = SPATIAL_PANEL_POSE,
            parent = session.scene.activitySpace,
        )
    }

    private fun XrSessionScope<ComponentActivity>.createSpatialPanel(
        name: String,
        dimensions: FloatSize2d,
    ): PanelEntity {
        val view =
            TextView(activity).apply {
                text = name
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(AndroidColor.WHITE)
                setBackgroundColor(AndroidColor.DKGRAY)
            }
        return PanelEntity.create(
            session = session,
            view = view,
            dimensions = dimensions,
            name = name,
            pose = SPATIAL_PANEL_POSE,
            parent = session.scene.activitySpace,
        )
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
        selectNode: (SnapshotXrSpace) -> SnapshotXrSpace?,
    ): SnapshotXrSpace {
        // Let pending layout and component transactions submit before polling CPM, so the caller
        // samples its "before" state after the UI has synced.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val node =
            waitForSpatialScene(
                description = "a resizable $description",
                timeout = SCENE_TIMEOUT,
                pollInterval = POLL_INTERVAL,
                select = { scene -> scene.findTaskByActivity(activity)?.let(selectNode) },
                until = { it != null && it.isResizable && it.worldExtents != null },
            )
        return checkNotNull(node)
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
            timeout = SCENE_TIMEOUT,
            pollInterval = POLL_INTERVAL,
        ) {
            val size = panel.sizeInPixels
            size.width > initialSizeInPixels.width && size.height > initialSizeInPixels.height
        }
    }

    private fun waitForResizableMainPanelNode(expectedSize: FloatSize2d? = null): SnapshotXrSpace {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return checkNotNull(
            waitForSpatialScene(
                description = "Resizable MainPanelAndTaskWindowLeashNode ready",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == MAIN_PANEL_NODE_NAME
                    }
                },
                until = { node ->
                    if (
                        node == null ||
                            !node.isVisible ||
                            !node.isResizable ||
                            node.worldExtents == null
                    ) {
                        false
                    } else if (expectedSize == null) {
                        true
                    } else {
                        val surfaceBounds = node.effectiveSurfaceBoundsInMeters
                        val boundsMatch =
                            if (surfaceBounds != null) {
                                abs(surfaceBounds.x - expectedSize.width) <=
                                    EXTENTS_SETTLE_TOLERANCE &&
                                    abs(surfaceBounds.y - expectedSize.height) <=
                                        EXTENTS_SETTLE_TOLERANCE
                            } else {
                                val extents = node.worldExtents ?: return@waitForSpatialScene false
                                val sx = if (abs(node.scale.x) > 1e-6f) node.scale.x else 1f
                                val sy = if (abs(node.scale.y) > 1e-6f) node.scale.y else 1f
                                abs(extents.x - expectedSize.width * sx) <=
                                    EXTENTS_SETTLE_TOLERANCE &&
                                    abs(extents.y - expectedSize.height * sy) <=
                                        EXTENTS_SETTLE_TOLERANCE
                            }
                        val brAffordance =
                            node.resizeAffordanceNodes.firstOrNull {
                                it.name.contains("BottomRight", ignoreCase = true)
                            }
                        val affordanceMatches =
                            if (brAffordance != null && brAffordance.worldExtents != null) {
                                val localBr = node.transformPointFromWorld(brAffordance.position)
                                abs(localBr.x - (expectedSize.width / 2f)) <= 0.12f &&
                                    abs(localBr.y + (expectedSize.height / 2f)) <= 0.12f
                            } else {
                                true
                            }
                        boundsMatch && affordanceMatches
                    }
                },
            )
        )
    }

    private fun assertAspectRatioPreserved(
        size: FloatSize2d,
        expectedAspect: Float,
        label: String,
    ) {
        assertWithMessage("$label height must stay positive, was $size")
            .that(size.height)
            .isGreaterThan(0f)
        val actualAspect = size.width / size.height
        assertWithMessage(
                "Expected $label aspect ratio near $expectedAspect (size=$size), was $actualAspect"
            )
            .that(abs(actualAspect - expectedAspect))
            .isLessThan(ASPECT_RATIO_TOLERANCE)
    }

    private fun waitForResizablePanelNode(
        panelName: String,
        expectedSize: FloatSize2d? = null,
        predicate: (SnapshotXrSpace) -> Boolean = { true },
    ): SnapshotXrSpace =
        checkNotNull(
            waitForSpatialScene(
                description = "Resizable panel SnapshotXrSpace '$panelName' ready",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(sessionRule.activity)?.findNode {
                        it.name == panelName &&
                            it.isEmbeddedWindowLeash &&
                            it.isVisible &&
                            it.worldExtents != null &&
                            it.isResizable
                    }
                },
                until = { node ->
                    if (node == null || !predicate(node)) {
                        false
                    } else if (expectedSize == null) {
                        true
                    } else {
                        val extents = node.worldExtents ?: return@waitForSpatialScene false
                        val sx = if (abs(node.scale.x) > 1e-6f) node.scale.x else 1f
                        val sy = if (abs(node.scale.y) > 1e-6f) node.scale.y else 1f
                        abs(extents.x - expectedSize.width * sx) <= EXTENTS_SETTLE_TOLERANCE &&
                            abs(extents.y - expectedSize.height * sy) <= EXTENTS_SETTLE_TOLERANCE
                    }
                },
            )
        )

    private fun waitForResizableSurfaceNode(): SnapshotXrSpace =
        checkNotNull(
            waitForSpatialScene(
                description = "Resizable SurfaceEntity parent SnapshotXrSpace ready",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task = scene.findTaskByActivity(sessionRule.activity)
                    val subspace = task?.findNode {
                        it.name.startsWith(SURFACE_ENTITY_SUBSPACE_PREFIX)
                    }
                    subspace?.parent
                },
                until = { parentNode ->
                    parentNode != null && parentNode.isVisible && parentNode.isResizable
                },
            )
        )

    // Level 2 verification only: SDK entity properties and ResizeEvent callbacks across settle.
    private fun assertNoResizeAcrossSettleWindow(
        panel: PanelEntity,
        resizeListener: RecordingResizeListener,
        expectedSize: FloatSize2d,
        expectedPixelSize: IntSize2d,
        expectedPose: Pose,
    ) {
        val anchorTime = TimeSource.Monotonic.markNow()
        val deadline = anchorTime + SETTLE_WINDOW
        var consecutiveCleanPolls = 0

        waitUntil(
            description = "No resize delivered for detached component across settle window",
            timeout = NO_OP_POLL_TIMEOUT,
            pollInterval = NO_OP_POLL_INTERVAL,
        ) {
            assertThat(resizeListener.snapshotStates()).isEmpty()
            assertThat(panel.size).isEqualTo(expectedSize)
            assertThat(panel.sizeInPixels).isEqualTo(expectedPixelSize)
            assertPose(panel.getPose(Space.ACTIVITY), expectedPose, POSE_EPSILON)

            consecutiveCleanPolls += 1
            consecutiveCleanPolls >= SETTLE_POLL_COUNT && TimeSource.Monotonic.markNow() >= deadline
        }
    }

    private fun assertAlphaRemainsVisibleAcrossSettleWindow(panelName: String) {
        val deadline = TimeSource.Monotonic.markNow() + SETTLE_WINDOW
        var cleanPolls = 0

        waitForSpatialScene(
            description = "Panel requestedAlpha remains visible during active resize",
            timeout = NO_OP_POLL_TIMEOUT,
            pollInterval = NO_OP_POLL_INTERVAL,
            select = { scene ->
                scene.findTaskByActivity(sessionRule.activity)?.findNode {
                    it.name == panelName && it.isEmbeddedWindowLeash
                }
            },
            until = { node ->
                if (node == null) return@waitForSpatialScene false
                assertThat(node.requestedAlphaOrDefault(0f)).isAtLeast(ALPHA_VISIBLE_THRESHOLD)
                cleanPolls += 1
                cleanPolls >= SETTLE_POLL_COUNT && TimeSource.Monotonic.markNow() >= deadline
            },
        )
    }

    private fun SnapshotXrSpace.requestedAlphaOrDefault(defaultAlpha: Float): Float =
        properties["requestedAlpha"]?.toFloatOrNull() ?: defaultAlpha

    private enum class MoveState {
        START,
        ONGOING,
        END,
    }

    private class RecordingMoveListener : EntityMoveListener {
        private val states = CopyOnWriteArrayList<MoveState>()

        override fun onMoveStart(
            entity: Entity,
            initialInputRay: Ray,
            initialPose: Pose,
            initialScale: Float,
            initialParent: Entity,
        ) {
            states.add(MoveState.START)
        }

        override fun onMoveUpdate(
            entity: Entity,
            currentInputRay: Ray,
            currentPose: Pose,
            currentScale: Float,
        ) {
            states.add(MoveState.ONGOING)
        }

        override fun onMoveEnd(
            entity: Entity,
            finalInputRay: Ray,
            finalPose: Pose,
            finalScale: Float,
            updatedParent: Entity?,
        ) {
            states.add(MoveState.END)
        }

        fun snapshotStates(): List<MoveState> = states.toList()

        fun hasStarted(): Boolean = MoveState.START in states

        fun hasEnded(): Boolean = MoveState.END in states

        fun clear() {
            states.clear()
        }
    }

    private companion object {
        /** Timeout applied when waiting for the end of a resize to be reported by the component. */
        val RESIZE_TIMEOUT = 5.seconds

        /** Interval between two consecutive polls. */
        val POLL_INTERVAL = 100.milliseconds

        /**
         * Size of the Compose spatial panel and of the activity panel created by baseline tests.
         */
        val PANEL_PIXEL_DIMENSIONS = IntSize2d(1280, 960)

        /**
         * Initial pose of the Compose spatial panel and of the activity panel in baseline tests.
         */
        val COMPOSE_PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))

        /**
         * Corner of the resize affordance that is dragged in baseline tests. Only the corners of
         * the affordance resize the panel; its edges are used to move it.
         */
        val RESIZE_CORNER = SpatialAlignment.BottomRight

        /**
         * Drag applied to [RESIZE_CORNER] in baseline tests, expressed in the node's local space.
         */
        val RESIZE_DELTA = Vector3(0.2f, -0.2f, 0f)

        const val GLTF_DIR = "models"
        const val BOUNDING_BOX_MODEL_ASSET = "BoundingBoxBlue.glb"
        const val SURFACE_ENTITY_SUBSPACE_PREFIX = "stereo_surface_panel_entity_subspace_"
        const val GLTF_ENTITY_SUBSPACE_PREFIX = "gltf_entity_subspace_"

        const val DRAG_DURATION_MS = 250L
        const val SETTLE_POLL_COUNT = 3

        const val MIN_RESIZE_DELTA_METERS = 0.05f
        const val ASPECT_RATIO_TOLERANCE = 0.04f
        const val BOUND_CLAMP_TOLERANCE = 0.02f
        const val EXTENTS_SETTLE_TOLERANCE = 0.03f
        const val ALPHA_HIDDEN_THRESHOLD = 0.01f
        const val ALPHA_VISIBLE_THRESHOLD = 0.99f
        const val POSE_EPSILON = 1e-4f
        const val MIN_COEXIST_MOVE_DISTANCE_METERS = 0.14f

        val DEFAULT_PANEL_PX = IntSize2d(640, 480)
        val LARGE_PANEL_PX = IntSize2d(1280, 960)
        val PORTRAIT_PANEL_SIZE = FloatSize2d(0.50f, 0.90f)
        val LANDSCAPE_PANEL_SIZE = FloatSize2d(0.90f, 0.50f)
        val TWO_BY_ONE_SIZE = FloatSize2d(1.00f, 0.50f)
        val MAIN_PANEL_INITIAL_SIZE = FloatSize2d(0.96f, 0.60f)
        val SIXTEEN_BY_NINE_PANEL_SIZE = FloatSize2d(0.80f, 0.45f)
        val SURFACE_QUAD_SIZE = FloatSize2d(1.0f, 0.8f)
        val SURFACE_PIXEL_DIMENSIONS = IntSize2d(256, 256)
        val SPATIAL_PANEL_POSE = Pose(Vector3(0f, 0f, -1.5f), Quaternion.Identity)
        val COEXIST_MOVE_DELTA = Vector3(0.18f, 0.10f, 0f)

        val SCENE_TIMEOUT: Duration = 10.seconds
        val SETTLE_WINDOW: Duration = 300.milliseconds
        val NO_OP_POLL_TIMEOUT: Duration = 3.seconds
        val NO_OP_POLL_INTERVAL: Duration = 100.milliseconds
    }
}

/**
 * A resize event listener that optionally applies the size proposed at the end of a resize to the
 * resized entity, counts down [resizeEnded] on [ResizeEvent.ResizeState.END], and records all
 * delivered events for inspection.
 */
private class RecordingResizeListener(@Volatile var applyResizeToEntity: Boolean = true) :
    Consumer<ResizeEvent> {
    private val events = CopyOnWriteArrayList<ResizeEvent>()

    val resizeEnded = CountDownLatch(1)

    /** State of the last received resize event, or null if no event has been received yet. */
    @Volatile
    var lastResizeState: ResizeEvent.ResizeState? = null
        private set

    override fun accept(event: ResizeEvent) {
        lastResizeState = event.resizeState
        if (applyResizeToEntity && event.resizeState == ResizeEvent.ResizeState.END) {
            when (val entity = event.entity) {
                is PanelEntity -> entity.size = event.newSize.to2d()
                is SurfaceEntity -> entity.shape = SurfaceEntity.Shape.Quad(event.newSize.to2d())
            }
        }
        events.add(event)
        if (event.resizeState == ResizeEvent.ResizeState.END) {
            resizeEnded.countDown()
        }
    }

    fun snapshotStates(): List<ResizeEvent.ResizeState> = events.map { it.resizeState }

    fun ongoingEvents(): List<ResizeEvent> = events.filter {
        it.resizeState == ResizeEvent.ResizeState.ONGOING
    }

    fun hasOngoing(): Boolean = events.any { it.resizeState == ResizeEvent.ResizeState.ONGOING }

    fun hasEnded(): Boolean = events.any { it.resizeState == ResizeEvent.ResizeState.END }

    fun lastEndEvent(): ResizeEvent? = events.lastOrNull {
        it.resizeState == ResizeEvent.ResizeState.END
    }

    fun clear() {
        events.clear()
        lastResizeState = null
    }
}

/** The content shown by the main panel and by the spatial panel created by baseline tests. */
@Composable
private fun CenteredLabel(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 32.sp, color = Color.Black)
    }
}
