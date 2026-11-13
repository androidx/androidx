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
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.widget.Button as AndroidButton
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.ui.window.PopupProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.SpatialDialogProperties
import androidx.xr.compose.spatial.SpatialPopup
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
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
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Backhandling Spatial Panels CUJ test case in Compose XR.
 *
 * Covers:
 * - GNav / system back dispatching to dismiss [SpatialDialog] (`dismissOnBackPress = true` and
 *   `false`) opened via spatial interaction on a [SpatialPanel]
 * - GNav / system back dispatching to dismiss [SpatialPopup] while preserving its host panel
 * - GNav / system back dispatching to a `BackHandler` inside a moved [SpatialPanel]
 * - GNav / system back routing from a focused [SpatialAndroidViewPanel] back to the host activity
 * - GNav / system back routing to a focused embedded activity inside [SpatialActivityPanel] first,
 *   then to the host activity on the subsequent back press
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class BackHandlingSpatialPanelsTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier = SubspaceModifier.width(560.dp).height(240.dp).testTag("dialog_row"),
                    horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                    verticalAlignment = SpatialAlignment.CenterVertically,
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(220.dp).height(200.dp).testTag("left_panel")
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
        }

        enterFullSpace()
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
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_spatialDialogWithDismissOnBackPressFalse_remainsOpen() {
        var showDialog by mutableStateOf(true)
        var dismissCount by mutableIntStateOf(0)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(320.dp).height(240.dp).testTag("host_panel")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Host Panel", color = Color.White, fontSize = 20.sp)
                    }

                    if (showDialog) {
                        SpatialDialog(
                            onDismissRequest = {
                                dismissCount++
                                showDialog = false
                            },
                            properties = SpatialDialogProperties(dismissOnBackPress = false),
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
                                        text = "Protected Dialog",
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

        enterFullSpace()
        composeTestRule.onNodeWithText("Protected Dialog").assertIsDisplayed()
        awaitVisiblePanels(expectedCount = 2)

        sendGNavBack()

        // Allow main thread and window dispatcher to process any unintended dismissal after back.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        CountDownLatch(1).await(500L, TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()

        assertThat(dismissCount).isEqualTo(0)
        assertThat(showDialog).isTrue()
        composeTestRule.onNodeWithText("Protected Dialog").assertIsDisplayed()
        awaitVisiblePanels(expectedCount = 2)
        assertThat(composeTestRule.activity.isFinishing).isFalse()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_spatialPopup_dismissesPopupAndPreservesPanel() {
        var showPopup by mutableStateOf(false)
        var dismissCount by mutableIntStateOf(0)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(320.dp).height(240.dp).testTag("popup_host_panel")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Color.Blue).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        Text(text = "Popup Host Panel", color = Color.White, fontSize = 20.sp)
                        Button(onClick = { showPopup = true }) { Text("Show Popup") }
                    }

                    if (showPopup) {
                        SpatialPopup(
                            alignment = Alignment.Center,
                            onDismissRequest = {
                                dismissCount++
                                showPopup = false
                            },
                            properties =
                                PopupProperties(focusable = true, dismissOnBackPress = true),
                        ) {
                            Box(
                                modifier = Modifier.size(180.dp, 100.dp).background(Color.Magenta),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Spatial Popup Content",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        enterFullSpace()
        val hostPanelNode = awaitVisiblePanels(expectedCount = 1).single()
        val buttonBounds =
            composeTestRule.onNodeWithText("Show Popup").fetchSemanticsNode().boundsInWindow
        val buttonRect =
            Rect(
                buttonBounds.left.roundToInt(),
                buttonBounds.top.roundToInt(),
                buttonBounds.right.roundToInt(),
                buttonBounds.bottom.roundToInt(),
            )

        performInteraction(hostPanelNode) {
            pointAt(boundsInWindow = buttonRect, targetNode = hostPanelNode)
            select()
        }

        composeTestRule.waitUntil(
            conditionDescription = "SpatialPopup displayed after clicking Show Popup",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            showPopup
        }
        composeTestRule.onNodeWithText("Spatial Popup Content").assertIsDisplayed()
        awaitVisiblePanels(expectedCount = 2)

        sendGNavBack()

        composeTestRule.waitUntil(
            conditionDescription = "SpatialPopup dismissed on system back press",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            !showPopup && dismissCount == 1
        }
        composeTestRule.onNodeWithText("Spatial Popup Content").assertDoesNotExist()
        composeTestRule.onNodeWithText("Popup Host Panel").assertIsDisplayed()
        awaitVisiblePanels(expectedCount = 1)
        assertThat(composeTestRule.activity.isFinishing).isFalse()
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_movableSpatialPanelWithBackHandler_closesMovedPanel() {
        var isSecondaryPanelOpen by mutableStateOf(true)
        var panelBackCount by mutableIntStateOf(0)
        var deltaXAccumulated by mutableFloatStateOf(0f)
        val moveEnded = CountDownLatch(1)

        composeTestRule.setContent {
            Subspace {
                val systemMovePolicy = remember {
                    MovePolicy.system(scaleWithDistance = false) { event ->
                        deltaXAccumulated +=
                            event.pose.translation.x - event.previousPose.translation.x
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
        }

        enterFullSpace()
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

        sendGNavBack()

        composeTestRule.waitUntil(
            conditionDescription = "Moved SpatialPanel closed via its BackHandler",
            timeoutMillis = DEFAULT_TIMEOUT_MS,
        ) {
            !isSecondaryPanelOpen && panelBackCount == 1
        }
        // SpatialPanel removes its Subspace node and detaches its entity from CPM on close;
        // assert removal via Subspace semantics and CPM visible panel count.
        composeTestRule.onSubspaceNodeWithTag("movable_closable_panel").assertDoesNotExist()
        composeTestRule.onNodeWithText("Primary App Panel").assertIsDisplayed()
        assertThat(composeTestRule.activity.isFinishing).isFalse()
        awaitVisiblePanels(expectedCount = 1)
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun backPress_afterFocusingSpatialAndroidViewPanel_routesToHostActivity() {
        var viewClickCount by mutableIntStateOf(0)
        var hostBackCount by mutableIntStateOf(0)

        val callback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    hostBackCount++
                    isEnabled = false
                }
            }
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.addCallback(callback)
        }

        try {
            composeTestRule.setContent {
                Subspace {
                    SpatialRow(
                        modifier =
                            SubspaceModifier.width(560.dp)
                                .height(240.dp)
                                .testTag("android_view_row"),
                        horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                        verticalAlignment = SpatialAlignment.CenterVertically,
                    ) {
                        SpatialAndroidViewPanel(
                            factory = { context ->
                                FrameLayout(context).apply {
                                    setBackgroundColor(AndroidColor.GREEN)
                                    val button =
                                        AndroidButton(context).apply {
                                            text = "Panel Bottom Left (View)"
                                            gravity = Gravity.CENTER
                                            setOnClickListener { viewClickCount++ }
                                        }
                                    addView(
                                        button,
                                        FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                        ),
                                    )
                                }
                            },
                            modifier =
                                SubspaceModifier.width(240.dp)
                                    .height(200.dp)
                                    .testTag("bottom_left_view_panel"),
                        )

                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(240.dp)
                                    .height(200.dp)
                                    .testTag("sibling_panel")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Blue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Sibling Compose Panel",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                )
                            }
                        }
                    }
                }
            }

            enterFullSpace()
            val panels = awaitVisiblePanels(expectedCount = 2)
            val viewPanelNode = panels.minByOrNull { it.position.x }!!

            performInteraction(viewPanelNode) {
                pointAt(targetNode = viewPanelNode)
                select()
            }

            composeTestRule.waitUntil(
                conditionDescription = "SpatialAndroidViewPanel click received to focus View panel",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                viewClickCount >= 1
            }

            // First back reaches the host activity's OnBackPressedDispatcher despite View panel
            // focus.
            sendGNavBack()

            composeTestRule.waitUntil(
                conditionDescription =
                    "Host OnBackPressedCallback invoked after focusing View panel",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                hostBackCount == 1
            }
            assertThat(composeTestRule.activity.isFinishing).isFalse()

            // Re-focus View panel with callback disabled and send back again to finish host
            // activity.
            performInteraction(viewPanelNode) {
                pointAt(targetNode = viewPanelNode)
                select()
            }
            composeTestRule.waitUntil(
                conditionDescription =
                    "SpatialAndroidViewPanel second click received to re-focus View panel",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                viewClickCount >= 2
            }
            sendGNavBack()

            composeTestRule.waitUntil(
                conditionDescription = "Host ComponentActivity finishes on unhandled back press",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                composeTestRule.activity.isFinishing || composeTestRule.activity.isDestroyed
            }
        } finally {
            callback.remove()
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

        try {
            composeTestRule.setContent {
                Subspace {
                    SpatialRow(
                        modifier =
                            SubspaceModifier.width(560.dp)
                                .height(240.dp)
                                .testTag("activity_panel_row"),
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
                                Text(
                                    text = "Host Compose Panel",
                                    color = Color.Black,
                                    fontSize = 20.sp,
                                )
                            }
                        }

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

            enterFullSpace()
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

            composeTestRule.onNodeWithText("Host Compose Panel").assertIsDisplayed()
            awaitVisiblePanels(expectedCount = 1)

            // Subsequent back press without refocusing should route to and close the host activity.
            sendGNavBack()

            composeTestRule.waitUntil(
                conditionDescription = "Host ComponentActivity finished on second back press",
                timeoutMillis = DEFAULT_TIMEOUT_MS,
            ) {
                composeTestRule.activity.isFinishing || composeTestRule.activity.isDestroyed
            }
        } finally {
            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }

    private fun enterFullSpace() {
        runBlocking(Dispatchers.Default) { composeTestRule.activity.requestFullSpace() }
        composeTestRule.waitForIdle()
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
