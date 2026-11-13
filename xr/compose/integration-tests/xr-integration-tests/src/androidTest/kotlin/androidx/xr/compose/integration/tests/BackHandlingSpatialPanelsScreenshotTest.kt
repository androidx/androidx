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

package androidx.xr.compose.integration.tests

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.MovePolicy
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialMoveEventType
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialActivityPanelController
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import androidx.xr.testutils.captureSceneToBitmap
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Golden screenshot tests for the Backhandling Spatial Panels CUJ test case in Compose XR.
 *
 * Covers visual verification of:
 * - [SpatialDialog] presentation and dismissal after system back press
 * - Closing a moved [SpatialPanel] via its `BackHandler`
 * - Embedded activity panel presentation inside [SpatialActivityPanel] and dismissal after back
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class BackHandlingSpatialPanelsScreenshotTest {

    @get:Rule(order = 0) val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var usedSpatialInteraction = false

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(context),
        )
    }

    @After
    fun tearDown() {
        if (usedSpatialInteraction) {
            resetSpatialInteraction()
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_afterClickingPanelButtonToOpenSpatialDialog_dismissesDialog() {
        var showDialog by mutableStateOf(false)
        var dismissCount by mutableIntStateOf(0)

        composeTestRule.setXrScreenshotContent {
            SpatialRow(
                modifier = SubspaceModifier.width(560.dp).height(240.dp).testTag("dialog_row"),
                horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                verticalAlignment = SpatialAlignment.CenterVertically,
            ) {
                SpatialPanel(
                    modifier = SubspaceModifier.width(220.dp).height(200.dp).testTag("left_panel")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Green),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Panel Left", color = Color.Black, fontSize = 20.sp)
                    }
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(260.dp).height(200.dp).testTag("panel_top_right")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Color.Blue).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        Text(text = "Panel Top Right", color = Color.White, fontSize = 20.sp)
                        Button(onClick = { showDialog = true }) { Text("Show Dialog") }
                    }

                    if (showDialog) {
                        SpatialDialog(
                            onDismissRequest = {
                                dismissCount++
                                showDialog = false
                            }
                        ) {
                            Card(
                                modifier = Modifier.size(200.dp, 120.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Red),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Spatial Dialog Content",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val initialPanels = awaitVisiblePanels(expectedCount = 2)
        val rightPanelNode = initialPanels.maxByOrNull { it.position.x }!!

        val buttonBounds =
            composeTestRule.onNodeWithText("Show Dialog").fetchSemanticsNode().boundsInWindow
        val buttonRect =
            Rect(
                buttonBounds.left.roundToInt(),
                buttonBounds.top.roundToInt(),
                buttonBounds.right.roundToInt(),
                buttonBounds.bottom.roundToInt(),
            )

        performInteraction(rightPanelNode) {
            pointAt(boundsInWindow = buttonRect, targetNode = rightPanelNode)
            select()
        }

        composeTestRule.waitUntil(
            conditionDescription = "SpatialDialog opened after clicking Show Dialog",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            showDialog
        }
        composeTestRule.onNodeWithText("Spatial Dialog Content").assertIsDisplayed()
        awaitVisiblePanels(expectedCount = 3)

        val openBitmap = captureSceneToBitmap(padding = 16, autoCrop = true)
        try {
            openBitmap.assertAgainstGolden(
                screenshotRule,
                "compose_back_handling_spatial_dialog_open",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
        } finally {
            openBitmap.recycle()
        }

        sendGNavBack()

        composeTestRule.waitUntil(
            conditionDescription = "SpatialDialog dismissed on system back press",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            !showDialog && dismissCount == 1
        }
        composeTestRule.onNodeWithText("Spatial Dialog Content").assertDoesNotExist()
        composeTestRule.onNodeWithText("Panel Top Right").assertIsDisplayed()
        awaitVisiblePanels(expectedCount = 2)

        val dismissedBitmap = composeTestRule.captureToBitmap()
        try {
            dismissedBitmap.assertAgainstGolden(
                screenshotRule,
                "compose_back_handling_spatial_dialog_dismissed",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
        } finally {
            dismissedBitmap.recycle()
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_movableSpatialPanelWithBackHandler_closesMovedPanel() {
        var isSecondaryPanelOpen by mutableStateOf(true)
        var panelBackCount by mutableIntStateOf(0)
        var deltaXAccumulated by mutableFloatStateOf(0f)
        val moveEnded = CountDownLatch(1)

        composeTestRule.setXrScreenshotContent {
            val systemMovePolicy = remember {
                MovePolicy.system(scaleWithDistance = false) { event ->
                    deltaXAccumulated += event.pose.translation.x - event.previousPose.translation.x
                    if (event.type == SpatialMoveEventType.End) {
                        moveEnded.countDown()
                    }
                }
            }
            SpatialRow(
                modifier =
                    SubspaceModifier.width(560.dp).height(260.dp).testTag("movable_back_row"),
                horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                verticalAlignment = SpatialAlignment.CenterVertically,
            ) {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(240.dp).height(200.dp).testTag("primary_panel")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Green),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Primary App Panel", color = Color.Black, fontSize = 20.sp)
                    }
                }

                if (isSecondaryPanelOpen) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(280.dp)
                                .height(220.dp)
                                .movable(enabled = true, movePolicy = systemMovePolicy)
                                .testTag("movable_closable_panel")
                    ) {
                        BackHandler(enabled = true) {
                            panelBackCount++
                            isSecondaryPanelOpen = false
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Red),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Movable Closable Panel",
                                color = Color.White,
                                fontSize = 20.sp,
                            )
                        }
                    }
                }
            }
        }

        val initialPanels = awaitVisiblePanels(expectedCount = 2, requireMovable = true)
        val movableNode = initialPanels.first { it.isMovable }

        performInteraction(movableNode) { moveNodeBy(delta = Vector3(0.20f, 0.08f, 0f)) }

        assertThat(moveEnded.await(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        composeTestRule.waitUntil(
            conditionDescription = "Movable panel translated after spatial move interaction",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            deltaXAccumulated > 50f
        }

        val movedBitmap = composeTestRule.captureToBitmap()
        try {
            movedBitmap.assertAgainstGolden(
                screenshotRule,
                "compose_back_handling_moved_panel_before_back",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
        } finally {
            movedBitmap.recycle()
        }

        sendGNavBack()

        composeTestRule.waitUntil(
            conditionDescription = "Moved SpatialPanel closed via its BackHandler",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            !isSecondaryPanelOpen && panelBackCount == 1
        }
        // SpatialPanel removes its Subspace node and detaches its entity from CPM on close;
        // assert removal via Subspace semantics and CPM visible panel count before capturing.
        composeTestRule.onSubspaceNodeWithTag("movable_closable_panel").assertDoesNotExist()
        composeTestRule.onNodeWithText("Primary App Panel").assertIsDisplayed()
        assertThat(composeTestRule.activity.isFinishing).isFalse()
        awaitVisiblePanels(expectedCount = 1)

        val closedBitmap = composeTestRule.captureToBitmap()
        try {
            closedBitmap.assertAgainstGolden(
                screenshotRule,
                "compose_back_handling_moved_panel_closed",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
        } finally {
            closedBitmap.recycle()
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_afterFocusingSpatialActivityPanel_finishesEmbeddedActivityThenHost() {
        val embeddedActivityRef = AtomicReference<EmbeddedPanelTestActivity?>()
        val embeddedDestroyed = CountDownLatch(1)
        val application = composeTestRule.activity.application
        val lifecycleCallbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {
                    if (activity is EmbeddedPanelTestActivity) {
                        embeddedActivityRef.set(activity)
                    }
                }

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {
                    if (activity is EmbeddedPanelTestActivity) {
                        embeddedDestroyed.countDown()
                    }
                }
            }
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)

        var showActivityPanel by mutableStateOf(true)

        try {
            composeTestRule.setXrScreenshotContent {
                SpatialRow(
                    modifier =
                        SubspaceModifier.width(560.dp).height(240.dp).testTag("activity_panel_row"),
                    horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                    verticalAlignment = SpatialAlignment.CenterVertically,
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(240.dp).height(200.dp).testTag("host_panel")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Green),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "Host Compose Panel", color = Color.Black, fontSize = 20.sp)
                        }
                    }

                    if (showActivityPanel) {
                        SpatialActivityPanel(
                            controller =
                                rememberSpatialActivityPanelController(
                                    Intent(
                                        composeTestRule.activity,
                                        EmbeddedPanelTestActivity::class.java,
                                    )
                                ),
                            modifier =
                                SubspaceModifier.width(260.dp)
                                    .height(200.dp)
                                    .testTag("embedded_activity_panel"),
                        )
                    }
                }
            }

            composeTestRule.waitUntil(
                conditionDescription =
                    "EmbeddedPanelTestActivity resumed inside SpatialActivityPanel",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                embeddedActivityRef.get() != null
            }
            val embeddedActivity = checkNotNull(embeddedActivityRef.get())

            val panels = awaitVisiblePanels(expectedCount = 2)
            val activityPanelNode =
                panels.firstOrNull { it.isActivityWindowLeash }
                    ?: panels.maxByOrNull { it.position.x }!!

            val visibleBitmap = captureSceneToBitmap(padding = 16, autoCrop = true)
            try {
                visibleBitmap.assertAgainstGolden(
                    screenshotRule,
                    "compose_back_handling_activity_panel_visible",
                    MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
                )
            } finally {
                visibleBitmap.recycle()
            }

            // Click the embedded Activity panel to move window focus to EmbeddedPanelTestActivity.
            performInteraction(activityPanelNode) {
                pointAt(targetNode = activityPanelNode)
                select()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            sendGNavBack()

            composeTestRule.waitUntil(
                conditionDescription =
                    "Focused EmbeddedPanelTestActivity destroyed on first back press",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                embeddedActivity.isDestroyed
            }
            assertThat(embeddedDestroyed.await(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(composeTestRule.activity.isFinishing).isFalse()
            awaitVisiblePanels(expectedCount = 1)

            // Remove finished activity panel container from Subspace layout before capturing
            // Subspace-cropped screenshot.
            showActivityPanel = false
            composeTestRule.onNodeWithText("Host Compose Panel").assertIsDisplayed()
            awaitVisiblePanels(expectedCount = 1)

            val closedBitmap = composeTestRule.captureToBitmap()
            try {
                closedBitmap.assertAgainstGolden(
                    screenshotRule,
                    "compose_back_handling_activity_panel_closed",
                    MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
                )
            } finally {
                closedBitmap.recycle()
            }
        } finally {
            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }

    private fun performInteraction(
        targetNode: SpatialNode,
        block: androidx.xr.testutils.SpatialInteractionScope.() -> Unit,
    ) {
        usedSpatialInteraction = true
        targetNode.performSpatialInteraction(block = block)
        composeTestRule.waitForIdle()
    }

    private fun sendGNavBack() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val dispatched = uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        check(dispatched) { "Failed to dispatch AccessibilityService.GLOBAL_ACTION_BACK" }
    }

    private fun awaitVisiblePanels(
        expectedCount: Int,
        requireMovable: Boolean = false,
    ): List<SpatialNode> {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val taskId = composeTestRule.activity.taskId
        return checkNotNull(
            waitForSpatialScene(
                description = "$expectedCount visible panel nodes in CPM spatial scene",
                timeout = 10.seconds,
                select = { scene ->
                    val task =
                        scene.findTaskById(taskId)
                            ?: scene.tasks
                                .filter { it.packageName == packageName }
                                .maxByOrNull { it.taskId }
                    task?.let(::visiblePanelsForTask)
                },
                until = { panels ->
                    panels != null &&
                        panels.size == expectedCount &&
                        (!requireMovable || panels.any { it.isMovable })
                },
            )
        )
    }

    private fun visiblePanelsForTask(task: SpatialTask): List<SpatialNode> =
        task.windowLeashes
            .filter {
                (it.isEmbeddedWindowLeash || it.isActivityWindowLeash) &&
                    it.isVisible &&
                    it.effectiveSurfaceBoundsInMeters != null &&
                    it.worldExtents != null
            }
            .ifEmpty {
                task.findNodes {
                    it.name.startsWith("ViewPanel") &&
                        it.isVisible &&
                        it.effectiveSurfaceBoundsInMeters != null &&
                        it.worldExtents != null
                }
            }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
    }
}
