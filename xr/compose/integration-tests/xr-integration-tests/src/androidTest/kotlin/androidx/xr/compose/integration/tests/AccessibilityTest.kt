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

@file:kotlin.OptIn(
    androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi::class,
    androidx.xr.compose.subspace.ExperimentalSpatialGltfModelApi::class,
)

package androidx.xr.compose.integration.tests

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.animation.follow.FollowTarget
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.subspace.semantics.contentDescription
import androidx.xr.compose.subspace.semantics.semantics
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * End-to-end integration tests verifying [UiAutomation] accessibility hierarchy exposure and
 * [AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED] events across creation and removal for
 * Compose for XR spatial entities ([SpatialPanel], [SpatialGltfModel], [SpatialExternalSurface],
 * and anchor-following [Subspace]).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class AccessibilityTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 2) val timeout: Timeout = Timeout.seconds(60)

    private var uiAutomation: UiAutomation? = null

    @After
    fun tearDown() {
        uiAutomation?.setOnAccessibilityEventListener(null)
        uiAutomation = null
    }

    // TODO(b/585301648): Add SpatialEnvironment accessibility test once SpatialEnvironment
    // accessibility is implemented.

    @Test
    fun spatialPanel_createAndRemove_emitsAccessibilityFocusEventsAndCleansUp() {
        var showPanel by mutableStateOf(false)
        setFullSpaceContent {
            Subspace {
                if (showPanel) {
                    SpatialPanel(
                        SubspaceModifier.width(300.dp).height(150.dp).semantics {
                            contentDescription = EXPECTED_SPATIAL_PANEL_DESCRIPTION
                        }
                    ) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text("Spatial Panel")
                            }
                        }
                    }
                }
            }
        }

        verifyCreateFocusAndRemove(
            expectedDescriptions = setOf(EXPECTED_SPATIAL_PANEL_DESCRIPTION),
            setEntitiesVisible = { showPanel = it },
            requiresBaseEntityGcCleanup = false,
        )
    }

    @Test
    fun spatialEntities_createAndRemove_emitsAccessibilityFocusEventsAndCleansUp() {
        var showEntities by mutableStateOf(false)
        setFullSpaceContent {
            // Keep the Activity's main window displayed via SpatialMainPanel so that DecorView
            // remains enabled in UiAutomation.windows alongside BaseEntity's accessibility proxy
            // views.
            Surface(modifier = Modifier.fillMaxSize()) {}
            Subspace {
                SpatialMainPanel(SubspaceModifier.width(800.dp).height(650.dp))

                if (showEntities) {
                    val dragonModelState =
                        rememberSpatialGltfModelState(
                            source =
                                SpatialGltfModelSource.fromPath(
                                    Paths.get("models", "Dragon_Evolved.gltf")
                                )
                        )
                    SpatialGltfModel(
                        state = dragonModelState,
                        modifier =
                            SubspaceModifier.offset(y = 500.dp, z = (-1000).dp)
                                .scale(0.5f)
                                .semantics { contentDescription = EXPECTED_GLTF_MODEL_DESCRIPTION },
                    )

                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        modifier =
                            SubspaceModifier.width(400.dp).height(300.dp).semantics {
                                contentDescription = EXPECTED_SURFACE_DESCRIPTION
                            },
                    ) {}
                }
            }
        }

        verifyCreateFocusAndRemove(
            expectedDescriptions =
                setOf(EXPECTED_GLTF_MODEL_DESCRIPTION, EXPECTED_SURFACE_DESCRIPTION),
            setEntitiesVisible = { showEntities = it },
            requiresBaseEntityGcCleanup = true,
        )
    }

    @Test
    fun anchoredSubspace_createAndRemove_emitsAccessibilityFocusEventsAndCleansUp() {
        var showAnchoredSubspace by mutableStateOf(false)
        setFullSpaceContent {
            // Keep the Activity's main window displayed via SpatialMainPanel so that DecorView
            // remains enabled in UiAutomation.windows alongside BaseEntity's accessibility proxy
            // views.
            Surface(modifier = Modifier.fillMaxSize()) {}
            Subspace { SpatialMainPanel(SubspaceModifier.width(800.dp).height(650.dp)) }

            val session = LocalSession.current
            if (session != null && showAnchoredSubspace) {
                AnchoredGltfModel(session = session)
            }
        }

        verifyCreateFocusAndRemove(
            expectedDescriptions = setOf(EXPECTED_ANCHORED_MODEL_DESCRIPTION),
            setEntitiesVisible = { showAnchoredSubspace = it },
            requiresBaseEntityGcCleanup = true,
        )
    }

    private fun setFullSpaceContent(content: @Composable () -> Unit) {
        composeTestRule.setContent(content)
        composeTestRule.requestFullSpace()
    }

    private fun verifyCreateFocusAndRemove(
        expectedDescriptions: Set<String>,
        setEntitiesVisible: (Boolean) -> Unit,
        requiresBaseEntityGcCleanup: Boolean,
    ) {
        val focusedAnnouncements = CopyOnWriteArraySet<String>()
        val automation =
            createUiAutomation().apply {
                setOnAccessibilityEventListener { event ->
                    if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                        val desc = event.contentDescription?.toString()
                        if (!desc.isNullOrEmpty()) {
                            focusedAnnouncements.add(desc)
                        }
                    }
                }
            }
        uiAutomation = automation

        // 1. Create spatial entities and wait until they appear in UiAutomation.windows.
        setEntitiesVisible(true)
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            findFocusableNodesByDescription(automation, expectedDescriptions)
                .keys
                .containsAll(expectedDescriptions)
        }

        // 2. Focus each entity and verify TYPE_VIEW_ACCESSIBILITY_FOCUSED events are emitted.
        focusedAnnouncements.clear()
        composeTestRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            focusNodesByDescription(automation, expectedDescriptions - focusedAnnouncements)
            focusedAnnouncements.containsAll(expectedDescriptions)
        }

        // 3. Remove spatial entities and verify they are removed from UiAutomation.windows.
        setEntitiesVisible(false)
        composeTestRule.waitForIdle()

        if (requiresBaseEntityGcCleanup) {
            // TODO(b/532954892): BaseEntity-backed accessibility views (GltfModelEntity and
            // SurfaceEntity) currently remain attached to DecorView until ReferenceCleaner runs on
            // GC unless contentDescription is cleared on disposal. Remove explicit GC once fixed.
            Runtime.getRuntime().gc()
            System.runFinalization()
            Runtime.getRuntime().gc()
        }

        composeTestRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            findFocusableNodesByDescription(automation, expectedDescriptions).isEmpty()
        }
    }

    @Composable
    private fun AnchoredGltfModel(session: Session) {
        var anchor by remember { mutableStateOf<Anchor?>(null) }

        LaunchedEffect(session) {
            session.configure(
                Config.Builder().setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
            )
            val arDevice = ArDevice.getInstance(session)
            arDevice.state.first { it.trackingState == TrackingState.TRACKING }

            val anchorPose = Pose(Vector3(0.2f, -0.5f, -1.2f))
            val anchorResult = Anchor.create(session, anchorPose)
            if (anchorResult is AnchorCreateSuccess) {
                anchor = anchorResult.anchor
            }
            try {
                awaitCancellation()
            } finally {
                anchor?.detach()
            }
        }

        val currentAnchor = anchor
        if (currentAnchor != null) {
            Subspace(follow = FollowTarget.anchor(currentAnchor)) {
                val arrowsModelState =
                    rememberSpatialGltfModelState(
                        source =
                            SpatialGltfModelSource.fromPath(Paths.get("models", "xyzArrows.glb"))
                    )
                SpatialGltfModel(
                    state = arrowsModelState,
                    modifier =
                        SubspaceModifier.scale(0.5f).semantics {
                            contentDescription = EXPECTED_ANCHORED_MODEL_DESCRIPTION
                        },
                )
            }
        }
    }

    private fun createUiAutomation(): UiAutomation =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            .apply {
                serviceInfo = serviceInfo.apply {
                    flags =
                        flags or
                            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                            AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                }
            }

    private fun findFocusableNodesByDescription(
        uiAutomation: UiAutomation,
        targetDescriptions: Collection<String>,
    ): Map<String, AccessibilityNodeInfo> {
        val targetSet = targetDescriptions.toSet()
        val found = mutableMapOf<String, AccessibilityNodeInfo>()
        for (window in uiAutomation.windows) {
            window.root?.let { root -> collectMatchingFocusableNodes(root, targetSet, found) }
        }
        return found
    }

    private fun collectMatchingFocusableNodes(
        node: AccessibilityNodeInfo,
        targetDescriptions: Set<String>,
        out: MutableMap<String, AccessibilityNodeInfo>,
    ) {
        val desc = node.contentDescription?.toString()
        if (
            desc != null && desc in targetDescriptions && node.isFocusable && node.isVisibleToUser
        ) {
            out[desc] = node
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectMatchingFocusableNodes(child, targetDescriptions, out)
            }
        }
    }

    private fun focusNodesByDescription(
        uiAutomation: UiAutomation,
        descriptions: Collection<String>,
    ) {
        val nodes = findFocusableNodesByDescription(uiAutomation, descriptions)
        for (description in descriptions) {
            val node = nodes[description] ?: continue
            node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L

        const val EXPECTED_SPATIAL_PANEL_DESCRIPTION = "Semantics modifier override Spatial Panel"
        const val EXPECTED_GLTF_MODEL_DESCRIPTION = "Dragon Entity"
        const val EXPECTED_SURFACE_DESCRIPTION = "External Surface"
        const val EXPECTED_ANCHORED_MODEL_DESCRIPTION =
            "Anchored GLTF Model at [x=0.2, y=-0.5, z=-1.2]"
    }
}
