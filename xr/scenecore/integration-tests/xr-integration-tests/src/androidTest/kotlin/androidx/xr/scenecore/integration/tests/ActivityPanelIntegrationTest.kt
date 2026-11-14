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
import android.app.Instrumentation
import android.content.Intent
import android.widget.TextClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.captureSpatialScene
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device integration tests for [ActivityPanelEntity] and the R12 Activity Panel CUJ
 * (b/573234247, b/583439191).
 *
 * Uses `ComponentActivity` with [Instrumentation.ActivityMonitor] rather than launching a TestApp
 * activity directly (`go/jxr-integration-testing` §1.3), and finishes every embedded `Activity` and
 * waits for `isDestroyed` in `finally` before `sessionRule` tears down the host activity
 * (b/584286729, b/584288042).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class ActivityPanelIntegrationTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun activityPanel_createAndConfigureProperties_updatesPoseBoundsAndParent() {
        sessionRule.requestFullSpace()

        val hostActivity = sessionRule.activity
        val session = sessionRule.session
        var embeddedActivity: ComponentActivity? = null
        var monitor: Instrumentation.ActivityMonitor? = null

        try {
            monitor = registerComponentActivityMonitor()

            val panel = sessionRule.onMainThread {
                ActivityPanelEntity.create(
                        session = this.session,
                        pixelDimensions = INITIAL_PIXEL_SIZE,
                        name = TEST_PANEL_NAME,
                        pose = INITIAL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                    .also { it.startActivity(Intent(activity, ComponentActivity::class.java)) }
            }

            val captured =
                waitForEmbeddedComponentActivity(
                    monitor = monitor,
                    hostActivity = hostActivity,
                    onActivityLaunched = { embeddedActivity = it },
                )

            val liveTickCount = AtomicInteger(0)
            sessionRule.onMainThread {
                captured.setContent {
                    LiveEmbeddedContent(
                        label = "Embedded Panel",
                        clicks = 0,
                        onTick = { liveTickCount.set(it) },
                        onClick = {},
                    )
                }
            }
            waitUntil(
                "Live UI ticker starts on embedded ComponentActivity",
                timeout = SCENE_TIMEOUT,
            ) {
                liveTickCount.get() >= 2
            }

            assertThat(panel.parent).isEqualTo(session.scene.activitySpace)
            assertThat(panel.isEnabled(includeParents = false)).isTrue()
            assertThat(panel.sizeInPixels).isEqualTo(INITIAL_PIXEL_SIZE)
            assertPose(panel.getPose(Space.PARENT), INITIAL_POSE)

            val (initialCpmNode, initialActivitySpace) =
                waitForActivityPanelNodeWithActivitySpace(
                    hostActivity = hostActivity,
                    panelName = TEST_PANEL_NAME,
                    requireActivityWindowLeash = true,
                    requireSurfaceBoundsInPixels = true,
                )
            assertThat(initialCpmNode.isVisible).isTrue()
            assertThat(initialCpmNode.isMovable).isFalse()
            assertThat(initialCpmNode.isResizable).isFalse()
            assertThat(initialCpmNode.worldExtents).isNotNull()
            val initialLocalPos =
                initialActivitySpace.transformPointFromWorld(initialCpmNode.position)

            // ActivityPanelEntity exposes bounds updates via sizeInPixels (which delegates to
            // ActivityPanel.setWindowBounds) and size rather than a moveToBounds API.
            panel.sizeInPixels = UPDATED_PIXEL_SIZE
            assertThat(panel.sizeInPixels).isEqualTo(UPDATED_PIXEL_SIZE)

            val initialMetricSize = panel.size
            assertThat(initialMetricSize.width).isGreaterThan(0f)
            assertThat(initialMetricSize.height).isGreaterThan(0f)

            panel.size = UPDATED_METRIC_SIZE
            assertThat(panel.sizeInPixels.width).isGreaterThan(0)
            assertThat(panel.sizeInPixels.height).isGreaterThan(0)
            assertThat(panel.size.width)
                .isWithin(METRIC_SIZE_TOLERANCE)
                .of(UPDATED_METRIC_SIZE.width)
            assertThat(panel.size.height)
                .isWithin(METRIC_SIZE_TOLERANCE)
                .of(UPDATED_METRIC_SIZE.height)

            panel.cornerRadius = UPDATED_CORNER_RADIUS
            assertThat(panel.cornerRadius).isWithin(FLOAT_TOLERANCE).of(UPDATED_CORNER_RADIUS)

            panel.setPose(UPDATED_POSE, Space.PARENT)
            assertPose(panel.getPose(Space.PARENT), UPDATED_POSE)

            panel.parent = session.scene.mainPanelEntity
            assertThat(panel.parent).isEqualTo(session.scene.mainPanelEntity)
            panel.parent = session.scene.activitySpace
            assertThat(panel.parent).isEqualTo(session.scene.activitySpace)

            panel.setEnabled(false)
            assertThat(panel.isEnabled(includeParents = false)).isFalse()
            waitForSpatialScene(
                description = "disabled ActivityPanel hidden in CPM",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    scene.findTaskByActivity(hostActivity)?.findNode { it.name == TEST_PANEL_NAME }
                },
                until = { node -> node == null || !node.isVisible },
            )

            panel.setEnabled(true)
            assertThat(panel.isEnabled(includeParents = false)).isTrue()

            val movable = MovableComponent.createSystemMovable(session = session)
            val resizable =
                ResizableComponent.create(
                    session = session,
                    minimumSize = FloatSize3d(0.2f, 0.2f, 0f),
                    maximumSize = FloatSize3d(2.0f, 2.0f, 0f),
                    resizeEventListener = Consumer { _ -> },
                )
            assertThat(panel.addComponent(movable)).isTrue()
            assertThat(panel.addComponent(resizable)).isTrue()

            val expectedLocalDelta = UPDATED_POSE.translation - INITIAL_POSE.translation
            var configuredNodeRef: SpatialNode? = null
            var configuredActivitySpaceRef: SpatialNode? = null
            waitUntil(
                "CPM reflects configured ActivityPanel visibility, capabilities, bounds, and pose",
                timeout = SCENE_TIMEOUT,
            ) {
                val scene = captureSpatialScene()
                val task = scene.findTaskByActivity(hostActivity)
                val candidate = task?.findNode {
                    it.name == TEST_PANEL_NAME &&
                        it.isVisible &&
                        it.isActivityWindowLeash &&
                        it.isMovable &&
                        it.isResizable &&
                        it.worldExtents != null
                }
                val activitySpaceCandidate = task?.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                val boundsMeters = candidate?.effectiveSurfaceBoundsInMeters
                val boundsMatches =
                    boundsMeters != null &&
                        abs(boundsMeters.x - UPDATED_METRIC_SIZE.width) <= WORLD_DELTA_TOLERANCE &&
                        abs(boundsMeters.y - UPDATED_METRIC_SIZE.height) <= WORLD_DELTA_TOLERANCE
                val positionSettled =
                    if (candidate != null && activitySpaceCandidate != null) {
                        val candidateDelta =
                            activitySpaceCandidate.transformPointFromWorld(candidate.position) -
                                initialLocalPos
                        abs(candidateDelta.x - expectedLocalDelta.x) <= WORLD_DELTA_TOLERANCE &&
                            abs(candidateDelta.y - expectedLocalDelta.y) <= WORLD_DELTA_TOLERANCE &&
                            abs(candidateDelta.z - expectedLocalDelta.z) <= WORLD_DELTA_TOLERANCE
                    } else {
                        false
                    }
                if (
                    candidate != null &&
                        activitySpaceCandidate != null &&
                        boundsMatches &&
                        positionSettled
                ) {
                    configuredNodeRef = candidate
                    configuredActivitySpaceRef = activitySpaceCandidate
                    true
                } else {
                    false
                }
            }

            val configuredNode = checkNotNull(configuredNodeRef)
            val configuredActivitySpace = checkNotNull(configuredActivitySpaceRef)
            assertThat(configuredNode.isVisible).isTrue()
            assertThat(configuredNode.isMovable).isTrue()
            assertThat(configuredNode.isResizable).isTrue()
            val configuredExtents = checkNotNull(configuredNode.worldExtents)
            assertThat(configuredExtents.x).isGreaterThan(0f)
            assertThat(configuredExtents.y).isGreaterThan(0f)
            val configuredBoundsMeters = checkNotNull(configuredNode.effectiveSurfaceBoundsInMeters)
            assertThat(configuredBoundsMeters.x)
                .isWithin(WORLD_DELTA_TOLERANCE)
                .of(UPDATED_METRIC_SIZE.width)
            assertThat(configuredBoundsMeters.y)
                .isWithin(WORLD_DELTA_TOLERANCE)
                .of(UPDATED_METRIC_SIZE.height)
            val configuredLocalPos =
                configuredActivitySpace.transformPointFromWorld(configuredNode.position)
            assertVector3(
                actual = configuredLocalPos - initialLocalPos,
                expected = expectedLocalDelta,
                epsilon = WORLD_DELTA_TOLERANCE,
            )
            assertThat(captured.isFinishing).isFalse()
            assertThat(captured.isDestroyed).isFalse()
        } finally {
            cleanUpEmbeddedActivities(
                hostActivity = hostActivity,
                embeddedActivities = listOfNotNull(embeddedActivity),
                monitor = monitor,
            )
        }
    }

    @Test
    fun activityPanel_homeSpaceTransition_togglesEmbedActivityCapability() {
        sessionRule.requestFullSpace()

        val hostActivity = sessionRule.activity
        val session = sessionRule.session
        var embeddedActivity: ComponentActivity? = null
        var monitor: Instrumentation.ActivityMonitor? = null

        try {
            monitor = registerComponentActivityMonitor()

            sessionRule.onMainThread {
                ActivityPanelEntity.create(
                        session = this.session,
                        pixelDimensions = INITIAL_PIXEL_SIZE,
                        name = TEST_PANEL_NAME,
                        pose = INITIAL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                    .startActivity(Intent(activity, ComponentActivity::class.java))
            }

            val captured =
                waitForEmbeddedComponentActivity(
                    monitor = monitor,
                    hostActivity = hostActivity,
                    onActivityLaunched = { embeddedActivity = it },
                )

            val liveTickCount = AtomicInteger(0)
            sessionRule.onMainThread {
                captured.setContent {
                    LiveEmbeddedContent(
                        label = "Home Space Transition Panel",
                        clicks = 0,
                        onTick = { liveTickCount.set(it) },
                        onClick = {},
                    )
                }
            }
            waitUntil(
                "Live UI ticker starts on embedded ComponentActivity",
                timeout = SCENE_TIMEOUT,
            ) {
                liveTickCount.get() >= 2
            }

            val (fullSpaceNode, _) =
                waitForActivityPanelNodeWithActivitySpace(
                    hostActivity = hostActivity,
                    panelName = TEST_PANEL_NAME,
                    requireActivityWindowLeash = true,
                )
            assertThat(fullSpaceNode.isVisible).isTrue()

            sessionRule.requestHomeSpace()
            val homeCapabilities = session.scene.spatialCapabilities
            assertThat(homeCapabilities).doesNotContain(SpatialCapability.SPATIAL_UI)
            assertThat(homeCapabilities).doesNotContain(SpatialCapability.EMBED_ACTIVITY)

            val homeTask =
                checkNotNull(
                    waitForSpatialScene(
                        description =
                            "Home Space hides embedded ActivityPanel and embedded window leashes",
                        timeout = SCENE_TIMEOUT,
                        select = { scene -> scene.findTaskByActivity(hostActivity) },
                        until = { task ->
                            task != null &&
                                !task.isFullSpace &&
                                task.activityWindowLeashes.none { it.isVisible } &&
                                task.embeddedWindowLeashes.none { it.isVisible } &&
                                task.findNode { it.name == TEST_PANEL_NAME && it.isVisible } == null
                        },
                    )
                )
            assertThat(homeTask.activityWindowLeashes.none { it.isVisible }).isTrue()
            assertThat(homeTask.embeddedWindowLeashes.none { it.isVisible }).isTrue()

            sessionRule.requestFullSpace()
            val fullCapabilities = session.scene.spatialCapabilities
            assertThat(fullCapabilities).contains(SpatialCapability.SPATIAL_UI)
            assertThat(fullCapabilities).contains(SpatialCapability.EMBED_ACTIVITY)
            assertThat(captured.isDestroyed).isFalse()

            val (reemergedNode, _) =
                waitForActivityPanelNodeWithActivitySpace(
                    hostActivity = hostActivity,
                    panelName = TEST_PANEL_NAME,
                    requireActivityWindowLeash = true,
                )
            assertThat(reemergedNode.isVisible).isTrue()
            assertThat(reemergedNode.isActivityWindowLeash).isTrue()
            assertThat(reemergedNode.worldExtents).isNotNull()

            val tickAfterReturn = liveTickCount.get()
            waitUntil(
                "Live UI ticker keeps advancing after returning to Full Space",
                timeout = SCENE_TIMEOUT,
            ) {
                liveTickCount.get() > tickAfterReturn
            }
        } finally {
            cleanUpEmbeddedActivities(
                hostActivity = hostActivity,
                embeddedActivities = listOfNotNull(embeddedActivity),
                monitor = monitor,
            )
        }
    }

    @Test
    fun activityPanel_transferActivity_reparentsRunningActivityIntoTargetPanel() {
        sessionRule.requestFullSpace()

        val hostActivity = sessionRule.activity
        var embeddedActivity: ComponentActivity? = null
        var monitor: Instrumentation.ActivityMonitor? = null

        try {
            monitor = registerComponentActivityMonitor()

            sessionRule.onMainThread {
                ActivityPanelEntity.create(
                        session = session,
                        pixelDimensions = INITIAL_PIXEL_SIZE,
                        name = "${TEST_PANEL_NAME}_source",
                        pose = INITIAL_POSE,
                        parent = session.scene.activitySpace,
                    )
                    .startActivity(Intent(activity, ComponentActivity::class.java))
            }

            val captured =
                waitForEmbeddedComponentActivity(
                    monitor = monitor,
                    hostActivity = hostActivity,
                    onActivityLaunched = { embeddedActivity = it },
                )
            val (sourceNode, sourceActivitySpace) =
                waitForActivityPanelNodeWithActivitySpace(
                    hostActivity = hostActivity,
                    panelName = "${TEST_PANEL_NAME}_source",
                    requireActivityWindowLeash = true,
                )
            assertThat(sourceNode.isActivityWindowLeash).isTrue()
            assertThat(sourceNode.primarySurfaceNode).isNotNull()
            val sourceLocalPos = sourceActivitySpace.transformPointFromWorld(sourceNode.position)

            sessionRule.onMainThread {
                ActivityPanelEntity.create(
                        session = session,
                        pixelDimensions = UPDATED_PIXEL_SIZE,
                        name = "${TEST_PANEL_NAME}_destination",
                        pose = UPDATED_POSE,
                        parent = session.scene.activitySpace,
                    )
                    .transferActivity(captured)
            }

            val liveTickCount = AtomicInteger(0)
            sessionRule.onMainThread {
                captured.setContent {
                    LiveEmbeddedContent(
                        label = "Transferred Panel",
                        clicks = 0,
                        onTick = { liveTickCount.set(it) },
                        onClick = {},
                    )
                }
            }

            waitUntil(
                "Transferred ComponentActivity remains active with live UI",
                timeout = SCENE_TIMEOUT,
            ) {
                sessionRule.onMainThread {
                    !captured.isFinishing &&
                        !captured.isDestroyed &&
                        captured.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                } && liveTickCount.get() >= 2
            }

            val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertWithMessage("Expected Transferred Panel UI in transferred ActivityPanel")
                .that(
                    uiDevice.wait(
                        Until.findObject(By.textContains("Transferred Panel")),
                        UI_WAIT_MS,
                    )
                )
                .isNotNull()

            var destNodeRef: SpatialNode? = null
            var destActivitySpaceRef: SpatialNode? = null
            var sourceNodeAfterRef: SpatialNode? = null
            waitUntil(
                "Destination ActivityPanel receives embedded activity surface and source releases it",
                timeout = SCENE_TIMEOUT,
            ) {
                val scene = captureSpatialScene()
                val task = scene.findTaskByActivity(hostActivity)
                val destCandidate = task?.findNode {
                    it.name == "${TEST_PANEL_NAME}_destination" &&
                        it.isVisible &&
                        it.worldExtents != null &&
                        it.isActivityWindowLeash &&
                        it.primarySurfaceNode != null
                }
                val sourceAfterCandidate = task?.findNode { it.name == "${TEST_PANEL_NAME}_source" }
                val activitySpaceCandidate = task?.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                if (
                    destCandidate != null &&
                        activitySpaceCandidate != null &&
                        sourceAfterCandidate != null &&
                        sourceAfterCandidate.primarySurfaceNode == null
                ) {
                    destNodeRef = destCandidate
                    destActivitySpaceRef = activitySpaceCandidate
                    sourceNodeAfterRef = sourceAfterCandidate
                    true
                } else {
                    false
                }
            }

            val destNode = checkNotNull(destNodeRef)
            val destActivitySpace = checkNotNull(destActivitySpaceRef)
            assertThat(destNode.isVisible).isTrue()
            assertThat(destNode.isActivityWindowLeash).isTrue()
            assertThat(destNode.primarySurfaceNode).isNotNull()
            assertWithMessage("Source ActivityPanel must release its embedded activity surface")
                .that(sourceNodeAfterRef?.primarySurfaceNode)
                .isNull()
            val destLocalPos = destActivitySpace.transformPointFromWorld(destNode.position)
            assertVector3(
                actual = destLocalPos - sourceLocalPos,
                expected = UPDATED_POSE.translation - INITIAL_POSE.translation,
                epsilon = WORLD_DELTA_TOLERANCE,
            )
        } finally {
            cleanUpEmbeddedActivities(
                hostActivity = hostActivity,
                embeddedActivities = listOfNotNull(embeddedActivity),
                monitor = monitor,
            )
        }
    }

    @Test
    fun activityPanel_cuj_createInspectLiveUiAndInteractBothPanels() {
        sessionRule.requestFullSpace()

        val hostActivity = sessionRule.activity
        val session = sessionRule.session
        val activityPanelRef = AtomicReference<ActivityPanelEntity?>(null)
        var embeddedActivity: ComponentActivity? = null
        var monitor: Instrumentation.ActivityMonitor? = null

        val mainClickCount = AtomicInteger(0)
        val mainClickUiState = mutableIntStateOf(0)
        val embeddedClickCount = AtomicInteger(0)
        val embeddedClickUiState = mutableIntStateOf(0)
        val embeddedTickCount = AtomicInteger(0)

        try {
            monitor = registerComponentActivityMonitor()

            session.scene.mainPanelEntity.setPose(MAIN_PANEL_LEFT_POSE, Space.PARENT)

            sessionRule.onMainThread {
                activity.setContent {
                    val clicks by mainClickUiState
                    CenteredButtonPanelContent(
                        title = "Main Panel",
                        clicksText = "Main Clicks: $clicks",
                        onClick = {
                            val nextClicks = mainClickCount.incrementAndGet()
                            mainClickUiState.intValue = nextClicks
                            if (activityPanelRef.get() == null) {
                                val panel =
                                    ActivityPanelEntity.create(
                                        session = this.session,
                                        pixelDimensions = INITIAL_PIXEL_SIZE,
                                        name = CUJ_PANEL_NAME,
                                        pose = SECONDARY_PANEL_RIGHT_POSE,
                                        parent = this.session.scene.activitySpace,
                                    )
                                activityPanelRef.set(panel)
                                panel.startActivity(Intent(activity, ComponentActivity::class.java))
                            }
                        },
                    )
                }
            }

            // Step 1: Click Main Panel button to spawn the secondary ActivityPanelEntity.
            val initialMainNode = waitForMainPanelNode(hostActivity)
            initialMainNode.performSpatialInteraction {
                pointAt()
                select()
            }
            waitUntil(
                "Main panel click creates secondary ActivityPanelEntity",
                timeout = SCENE_TIMEOUT,
            ) {
                mainClickCount.get() == 1 && activityPanelRef.get() != null
            }

            // Step 2: Capture embedded ComponentActivity and populate live updating UI + Button.
            val captured =
                waitForEmbeddedComponentActivity(
                    monitor = monitor,
                    hostActivity = hostActivity,
                    onActivityLaunched = { embeddedActivity = it },
                )

            sessionRule.onMainThread {
                captured.setContent {
                    val clicks by embeddedClickUiState
                    LiveEmbeddedContent(
                        label = "Secondary Activity Panel",
                        clicks = clicks,
                        onTick = { embeddedTickCount.set(it) },
                        onClick = {
                            val nextClicks = embeddedClickCount.incrementAndGet()
                            embeddedClickUiState.intValue = nextClicks
                        },
                    )
                }
            }

            waitUntil("Live UI ticker updates in embedded ActivityPanel", timeout = SCENE_TIMEOUT) {
                embeddedTickCount.get() >= 3
            }

            val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertWithMessage("Expected Secondary Activity Panel button text in UI hierarchy")
                .that(
                    uiDevice.wait(
                        Until.findObject(By.textContains("Embedded Clicks: 0")),
                        UI_WAIT_MS,
                    )
                )
                .isNotNull()

            // Step 3: Verify secondary ActivityPanel appears in the 3D spatial scene graph.
            val (activityPanelNode, _) =
                waitForActivityPanelNodeWithActivitySpace(hostActivity, CUJ_PANEL_NAME)
            assertThat(activityPanelNode.isVisible).isTrue()
            assertThat(activityPanelNode.isActivityWindowLeash).isTrue()
            assertThat(activityPanelNode.worldExtents).isNotNull()

            // Step 4: Interact/click on Secondary Activity Panel and verify live UI updates.
            activityPanelNode.performSpatialInteraction {
                pointAt()
                select()
            }
            waitUntil(
                "Secondary ActivityPanel click increments click counter",
                timeout = SCENE_TIMEOUT,
            ) {
                embeddedClickCount.get() == 1
            }
            assertWithMessage("Expected updated Embedded Clicks: 1 after spatial click")
                .that(
                    uiDevice.wait(
                        Until.findObject(By.textContains("Embedded Clicks: 1")),
                        UI_WAIT_MS,
                    )
                )
                .isNotNull()

            val tickAfterInteraction = embeddedTickCount.get()
            waitUntil("Live UI ticker keeps advancing after interaction", timeout = SCENE_TIMEOUT) {
                embeddedTickCount.get() > tickAfterInteraction
            }

            // Step 5: Interact/click back on Main Panel and verify main panel responds.
            val currentMainNode = waitForMainPanelNode(hostActivity)
            currentMainNode.performSpatialInteraction {
                pointAt()
                select()
            }
            waitUntil(
                "Second click on Main Panel increments main click count",
                timeout = SCENE_TIMEOUT,
            ) {
                mainClickCount.get() == 2
            }
            assertWithMessage("Expected updated Main Clicks: 2 after clicking back on Main Panel")
                .that(
                    uiDevice.wait(Until.findObject(By.textContains("Main Clicks: 2")), UI_WAIT_MS)
                )
                .isNotNull()
        } finally {
            cleanUpEmbeddedActivities(
                hostActivity = hostActivity,
                embeddedActivities = listOfNotNull(embeddedActivity),
                monitor = monitor,
            )
        }
    }

    @Test
    fun activityPanel_spatialSceneBounds_reflectsSizeInPixelsAndPoseUpdate() {
        sessionRule.requestFullSpace()

        val hostActivity = sessionRule.activity
        val session = sessionRule.session
        var embeddedActivity: ComponentActivity? = null
        var monitor: Instrumentation.ActivityMonitor? = null

        try {
            monitor = registerComponentActivityMonitor()

            session.scene.mainPanelEntity.setEnabled(false)

            val activityPanel = sessionRule.onMainThread {
                ActivityPanelEntity.create(
                        session = this.session,
                        pixelDimensions = INITIAL_PIXEL_SIZE,
                        name = TEST_PANEL_NAME,
                        pose = INITIAL_POSE,
                        parent = this.session.scene.activitySpace,
                    )
                    .also { it.startActivity(Intent(activity, ComponentActivity::class.java)) }
            }

            val captured =
                waitForEmbeddedComponentActivity(
                    monitor = monitor,
                    hostActivity = hostActivity,
                    onActivityLaunched = { embeddedActivity = it },
                )

            sessionRule.onMainThread {
                captured.setContent {
                    CenteredButtonPanelContent(
                        title = "Resized Panel",
                        clicksText = "Static",
                        onClick = {},
                    )
                }
            }

            val (initialNode, initialActivitySpace) =
                waitForActivityPanelNodeWithActivitySpace(
                    hostActivity = hostActivity,
                    panelName = TEST_PANEL_NAME,
                    requireSurfaceBoundsInPixels = true,
                )
            val initialExtents = checkNotNull(initialNode.worldExtents)
            val initialBoundsPx = checkNotNull(initialNode.effectiveSurfaceBoundsInPixels)
            assertThat(initialBoundsPx.x)
                .isWithin(PIXEL_TOLERANCE)
                .of(INITIAL_PIXEL_SIZE.width.toFloat())
            assertThat(initialBoundsPx.y)
                .isWithin(PIXEL_TOLERANCE)
                .of(INITIAL_PIXEL_SIZE.height.toFloat())
            val initialLocalPos = initialActivitySpace.transformPointFromWorld(initialNode.position)

            activityPanel.sizeInPixels = UPDATED_PIXEL_SIZE
            activityPanel.setPose(UPDATED_POSE, Space.PARENT)

            val expectedLocalDelta = UPDATED_POSE.translation - INITIAL_POSE.translation
            var updatedNode: SpatialNode? = null
            var updatedActivitySpace: SpatialNode? = null
            waitUntil(
                "CPM scene node reflects updated ActivityPanel sizeInPixels and pose",
                timeout = SCENE_TIMEOUT,
            ) {
                val scene = captureSpatialScene()
                val task = scene.findTaskByActivity(hostActivity)
                val candidate = task?.findNode { it.name == TEST_PANEL_NAME && it.isVisible }
                val candidateBounds = candidate?.effectiveSurfaceBoundsInPixels
                val candidateExtents = candidate?.worldExtents
                val activitySpaceCandidate = task?.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                val boundsMatches =
                    candidateBounds != null &&
                        abs(candidateBounds.x - UPDATED_PIXEL_SIZE.width) <= PIXEL_TOLERANCE &&
                        abs(candidateBounds.y - UPDATED_PIXEL_SIZE.height) <= PIXEL_TOLERANCE
                val extentsGrew =
                    candidateExtents != null &&
                        candidateExtents.x > initialExtents.x &&
                        candidateExtents.y > initialExtents.y
                val positionSettled =
                    if (candidate != null && activitySpaceCandidate != null) {
                        val candidateDelta =
                            activitySpaceCandidate.transformPointFromWorld(candidate.position) -
                                initialLocalPos
                        abs(candidateDelta.x - expectedLocalDelta.x) <= WORLD_DELTA_TOLERANCE &&
                            abs(candidateDelta.y - expectedLocalDelta.y) <= WORLD_DELTA_TOLERANCE &&
                            abs(candidateDelta.z - expectedLocalDelta.z) <= WORLD_DELTA_TOLERANCE
                    } else {
                        false
                    }
                if (
                    candidate != null &&
                        activitySpaceCandidate != null &&
                        boundsMatches &&
                        extentsGrew &&
                        positionSettled
                ) {
                    updatedNode = candidate
                    updatedActivitySpace = activitySpaceCandidate
                    true
                } else {
                    false
                }
            }

            val settledUpdatedNode = checkNotNull(updatedNode)
            val settledActivitySpace = checkNotNull(updatedActivitySpace)
            val updatedBoundsPx = checkNotNull(settledUpdatedNode.effectiveSurfaceBoundsInPixels)
            assertThat(updatedBoundsPx.x)
                .isWithin(PIXEL_TOLERANCE)
                .of(UPDATED_PIXEL_SIZE.width.toFloat())
            assertThat(updatedBoundsPx.y)
                .isWithin(PIXEL_TOLERANCE)
                .of(UPDATED_PIXEL_SIZE.height.toFloat())
            val updatedLocalPos =
                settledActivitySpace.transformPointFromWorld(settledUpdatedNode.position)
            assertVector3(
                actual = updatedLocalPos - initialLocalPos,
                expected = expectedLocalDelta,
                epsilon = WORLD_DELTA_TOLERANCE,
            )
        } finally {
            cleanUpEmbeddedActivities(
                hostActivity = hostActivity,
                embeddedActivities = listOfNotNull(embeddedActivity),
                monitor = monitor,
            )
        }
    }

    private fun registerComponentActivityMonitor(): Instrumentation.ActivityMonitor =
        InstrumentationRegistry.getInstrumentation()
            .addMonitor(ComponentActivity::class.java.name, null, false)

    private fun waitForEmbeddedComponentActivity(
        monitor: Instrumentation.ActivityMonitor,
        hostActivity: ComponentActivity,
        onActivityLaunched: (ComponentActivity) -> Unit,
    ): ComponentActivity {
        val activity = monitor.waitForActivityWithTimeout(ACTIVITY_LAUNCH_TIMEOUT_MS)
        assertWithMessage("Timed out waiting for embedded ComponentActivity to launch")
            .that(activity)
            .isNotNull()
        assertThat(activity).isInstanceOf(ComponentActivity::class.java)
        assertWithMessage("Expected newly launched embedded ComponentActivity, not host instance")
            .that(activity)
            .isNotSameInstanceAs(hostActivity)
        val embedded = activity as ComponentActivity
        onActivityLaunched(embedded)
        waitUntil("Embedded ComponentActivity reaches RESUMED state", timeout = SCENE_TIMEOUT) {
            sessionRule.onMainThread {
                embedded.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        }
        return embedded
    }

    private fun waitForMainPanelNode(hostActivity: ComponentActivity): SpatialNode {
        var found: SpatialNode? = null
        waitUntil("Main panel SpatialNode is visible with extents", timeout = SCENE_TIMEOUT) {
            val scene = captureSpatialScene()
            val task = scene.findTaskByActivity(hostActivity)
            val candidate =
                task?.taskWindowLeash?.takeIf { it.isVisible && it.worldExtents != null }
                    ?: task?.topLevelWindowLeashes?.firstOrNull {
                        it.isVisible && it.worldExtents != null
                    }
            if (candidate != null) {
                found = candidate
                true
            } else {
                false
            }
        }
        return checkNotNull(found)
    }

    private fun waitForActivityPanelNodeWithActivitySpace(
        hostActivity: ComponentActivity,
        panelName: String,
        requireActivityWindowLeash: Boolean = false,
        requireSurfaceBoundsInPixels: Boolean = false,
    ): Pair<SpatialNode, SpatialNode> {
        var found: Pair<SpatialNode, SpatialNode>? = null
        waitUntil(
            "ActivityPanel SpatialNode '$panelName' is visible with extents",
            timeout = SCENE_TIMEOUT,
        ) {
            val scene = captureSpatialScene()
            val task = scene.findTaskByActivity(hostActivity)
            val candidate = task?.findNode {
                it.name == panelName &&
                    it.isVisible &&
                    it.worldExtents != null &&
                    (!requireActivityWindowLeash || it.isActivityWindowLeash) &&
                    (!requireSurfaceBoundsInPixels || it.effectiveSurfaceBoundsInPixels != null)
            }
            val activitySpaceNode = task?.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
            if (candidate != null && activitySpaceNode != null) {
                found = candidate to activitySpaceNode
                true
            } else {
                false
            }
        }
        return checkNotNull(found)
    }

    private fun cleanUpEmbeddedActivities(
        hostActivity: ComponentActivity,
        embeddedActivities: List<ComponentActivity>,
        monitor: Instrumentation.ActivityMonitor?,
    ) {
        try {
            // Finish any embedded activities (both captured references and any still tracked by
            // ActivityLifecycleMonitorRegistry) and wait for ON_DESTROY before returning from @Test
            // so XrSessionRule's ActivityScenarioRule teardown never finishes the host while an
            // ActivityPanel TaskFragment child is still alive (b/584286729, b/584288042).
            val nonDestroyedStages = Stage.values().filter { it != Stage.DESTROYED }
            val aliveNonHostActivities = {
                val lifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()
                nonDestroyedStages
                    .flatMap { stage -> lifecycleMonitor.getActivitiesInStage(stage) }
                    .filter { activity -> activity !== hostActivity && !activity.isDestroyed }
            }
            val toFinish = linkedSetOf<Activity>()
            sessionRule.onMainThread {
                toFinish.addAll(embeddedActivities.filter { it !== hostActivity })
                toFinish.addAll(aliveNonHostActivities())
                for (embedded in toFinish) {
                    if (!embedded.isFinishing && !embedded.isDestroyed) {
                        embedded.finish()
                    }
                }
            }
            if (toFinish.isNotEmpty()) {
                waitUntil("Embedded ComponentActivity destroyed", timeout = CLEANUP_TIMEOUT) {
                    sessionRule.onMainThread {
                        toFinish.all { it.isDestroyed } && aliveNonHostActivities().isEmpty()
                    }
                }
            }
        } finally {
            monitor?.let { InstrumentationRegistry.getInstrumentation().removeMonitor(it) }
        }
    }

    @Composable
    private fun CenteredButtonPanelContent(
        title: String,
        clicksText: String,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxSize(0.85f),
                contentPadding = PaddingValues(4.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = title)
                    Text(text = clicksText)
                }
            }
        }
    }

    @Composable
    private fun LiveEmbeddedContent(
        label: String,
        clicks: Int,
        onTick: (Int) -> Unit,
        onClick: () -> Unit,
    ) {
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(LIVE_TICK_INTERVAL_MS)
                tick += 1
                onTick(tick)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxSize(0.85f),
                contentPadding = PaddingValues(4.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = label)
                    Text(text = "Embedded Clicks: $clicks")
                    Text(text = "Live Tick: $tick")
                    AndroidView(
                        factory = { context ->
                            TextClock(context).apply { format24Hour = "HH:mm:ss" }
                        }
                    )
                }
            }
        }
    }

    private companion object {
        const val TEST_PANEL_NAME = "test_activity_panel"
        const val CUJ_PANEL_NAME = "cuj_activity_panel"
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val ACTIVITY_LAUNCH_TIMEOUT_MS = 10_000L
        const val UI_WAIT_MS = 5_000L
        const val LIVE_TICK_INTERVAL_MS = 250L
        const val FLOAT_TOLERANCE = 1e-4f
        const val METRIC_SIZE_TOLERANCE = 0.02f
        const val PIXEL_TOLERANCE = 2f
        const val WORLD_DELTA_TOLERANCE = 0.05f
        const val UPDATED_CORNER_RADIUS = 0.05f

        val SCENE_TIMEOUT = 10.seconds
        val CLEANUP_TIMEOUT = 10.seconds

        val INITIAL_PIXEL_SIZE = IntSize2d(640, 480)
        val UPDATED_PIXEL_SIZE = IntSize2d(960, 720)
        val UPDATED_METRIC_SIZE = FloatSize2d(0.8f, 0.6f)

        val INITIAL_POSE = Pose(Vector3(0.1f, 0.1f, 0.0f))
        val UPDATED_POSE = Pose(Vector3(0.35f, -0.1f, 0.05f))
        val MAIN_PANEL_LEFT_POSE = Pose(Vector3(-0.6f, 0.0f, 0.0f))
        val SECONDARY_PANEL_RIGHT_POSE = Pose(Vector3(0.55f, 0.0f, 0.05f))
    }
}
