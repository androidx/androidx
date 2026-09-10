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

package androidx.xr.testutils

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.platform.requestHomeSpace
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@LargeTest
@XrDeviceTest
@RunWith(AndroidJUnit4::class)
class SpatialSceneHelperAndroidTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val globalTimeout: Timeout = Timeout.seconds(20)

    @Test
    fun spatialScene_captures2DWindowBoundsAndTransforms() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("2D Window Test Content") }
        }

        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(scene)

        val task = assertNotNull(scene.findTaskByPackageName(context.packageName))
        assertEquals(task, scene.findTaskByActivity(composeTestRule.activity))
        val allNodes = task.allNodes
        assertTrue(
            allNodes.isNotEmpty(),
            "Spatial scene should contain nodes for the running activity",
        )

        // Verify window leash and activity surface in 2D Home Space
        val windowLeash =
            assertNotNull(
                task.taskWindowLeash,
                "Expected window leash in 2D Home Space",
            )
        val extents = assertNotNull(windowLeash.worldExtents)
        assertTrue(extents.x > 0f, "Expected non-zero world extent width")
        assertTrue(extents.y > 0f, "Expected non-zero world extent height")
        assertTrue(windowLeash.canReceiveInput, "Expected window leash to be receptive to input")
        assertTrue(windowLeash.hasSurfaceBounds, "Expected window leash to have surface bounds")

        val activitySurface =
            assertNotNull(
                windowLeash.primarySurfaceNode,
                "Expected 2D Android activity surface in 2D Home Space",
            )
        assertTrue(activitySurface.canReceiveInput, "Expected activity surface to receive input")
    }

    @Test
    fun spatialScene_unscopedSceneCapturesFullHierarchyAndRootNodes() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Unscoped Scene Test Content") }
        }
        composeTestRule.waitForIdle()

        // Capture unscoped spatial scene
        val unscopedScene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(unscopedScene)

        // Root nodes and tasks
        val rootNodes = unscopedScene.rootNodes
        assertTrue(rootNodes.isNotEmpty(), "Expected at least one root node in full scene")
        val tasks = unscopedScene.tasks
        assertTrue(tasks.isNotEmpty(), "Expected tasks to be populated in unscoped scene")

        // Root node integrity
        val root = rootNodes.first()
        assertTrue(root.name.isNotBlank(), "Root node name should not be blank")
        assertTrue(root.id >= 0, "Root node should have a valid non-negative id")
        assertEquals(0, root.depth, "Root node depth should be 0")
        assertNull(root.parent, "Root node should not have a parent")
        assertTrue(root.children.isNotEmpty(), "Root node should have child nodes")
        assertTrue(root.getDescendants().isNotEmpty(), "Root node should have descendants")
    }

    @Test
    fun spatialScene_packageScopingAndAppRootDiscovery() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Scoped Scene App Root Test") }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName

        val rawDumpsys = SpatialSceneHelper.executeShellCommand("dumpsys spf_cpm")
        val activitiesDump = SpatialSceneHelper.executeShellCommand("dumpsys activity activities")
        val taskPackageMap = SpatialSceneHelper.parseTaskPackageMap(activitiesDump)
        val capturedScene = SpatialSceneHelper.parseSpatialScene(rawDumpsys, taskPackageMap)

        val capturedTask = assertNotNull(capturedScene.findTaskByPackageName(packageName))

        // activitySpaceRoot discovery
        val taskRoot = capturedTask.activitySpaceRoot
        assertNotNull(taskRoot, "Expected task activitySpaceRoot to be resolved for $packageName")

        // Scoped allNodes
        val scopedNodes = capturedTask.allNodes
        assertTrue(scopedNodes.isNotEmpty(), "Expected scoped allNodes to contain app nodes")
        assertTrue(
            scopedNodes.contains(taskRoot),
            "Expected scoped allNodes to contain task activitySpaceRoot",
        )

        // Scoped inputTargets
        val inputTargets = capturedTask.inputTargets
        assertTrue(inputTargets.isNotEmpty(), "Expected input targets")
    }

    @Test
    fun spatialScene_queryMethodsAndPredicates() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Query Test Content") }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))

        // Window leash discovery
        val windowLeash =
            assertNotNull(
                task.taskWindowLeash,
                "Expected window leash to resolve for $packageName",
            )
        assertTrue(windowLeash.hasSurfaceBounds, "Expected window leash to have surface bounds")

        // findNodeByName & findNodesByName (case-insensitive substring)
        val packageSimpleName = packageName.substringAfterLast('.')
        val matchingNodes = task.findNodesByName(packageSimpleName)
        assertTrue(
            matchingNodes.isNotEmpty(),
            "Expected findNodesByName to find nodes matching '$packageSimpleName'",
        )
        val firstMatching = task.findNodeByName(packageSimpleName)
        assertNotNull(firstMatching)
        assertEquals(matchingNodes.first(), firstMatching)

        // Lowercase and uppercase matching to verify case-insensitivity
        val lowerMatch = task.findNodeByName(packageSimpleName.lowercase())
        val upperMatch = task.findNodeByName(packageSimpleName.uppercase())
        assertNotNull(lowerMatch, "Expected lowercase search to match")
        assertNotNull(upperMatch, "Expected uppercase search to match")
        assertEquals(lowerMatch.id, upperMatch.id)

        // findNode and findNodes with custom predicate
        val allNodes = task.allNodes
        val inputReceivingNodes = task.findNodes { it.canReceiveInput }
        assertEquals(allNodes.filter { it.canReceiveInput }, inputReceivingNodes)

        val firstInputReceiving = task.findNode { it.canReceiveInput }
        assertEquals(inputReceivingNodes.firstOrNull(), firstInputReceiving)

        val nonExistentNode = task.findNodeByName("NonExistentNodeName_xyz_123")
        assertNull(nonExistentNode, "Expected null for non-existent node search")
        assertTrue(task.findNodesByName("NonExistentNodeName_xyz_123").isEmpty())
    }

    @Test
    fun spatialNode_hierarchyRelationshipsAndDescendants() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Hierarchy Test Content") }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val appRoot = task.activitySpaceRoot

        val descendants = appRoot.getDescendants()
        if (descendants.isNotEmpty()) {
            val child = appRoot.children.firstOrNull()
            assertNotNull(child, "Expected at least one direct child when descendants is not empty")
            assertEquals(appRoot, child.parent, "Child's parent reference must point to appRoot")
            assertTrue(appRoot.children.contains(child), "appRoot.children must contain child node")
            assertTrue(
                child.isDescendantOf(appRoot),
                "child.isDescendantOf(appRoot) must return true",
            )
            assertFalse(
                appRoot.isDescendantOf(child),
                "appRoot.isDescendantOf(child) must return false",
            )

            // Deep descendant verification if present
            val deepestDescendant = descendants.last()
            assertTrue(
                deepestDescendant.isDescendantOf(appRoot),
                "Deepest descendant must be descendant of appRoot",
            )
            deepestDescendant.parent?.let { parent ->
                assertTrue(
                    deepestDescendant.isDescendantOf(parent),
                    "Deepest descendant must be descendant of its direct parent",
                )
            }
        }
    }

    @Test
    fun spatialNode_metadataAttributesAndPropertiesExtraction() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Node Attributes Test Content") }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode = task.taskWindowLeash ?: task.allNodes.first()

        // Metadata fields
        assertTrue(targetNode.header.isNotBlank(), "Node header must not be blank")
        assertTrue(targetNode.name.isNotBlank(), "Node name must not be blank")
        assertTrue(targetNode.id >= 0, "Node id must be non-negative")
        assertTrue(targetNode.depth >= 0, "Node depth must be non-negative")

        // Attributes map
        val attributes = targetNode.attributes
        assertNotNull(attributes)
        val posAttr = targetNode.attributes["ImpNodeWorldPosition"]
        if (posAttr != null) {
            assertTrue(posAttr.size >= 3, "ImpNodeWorldPosition attribute must have >= 3 floats")
            assertEquals(posAttr[0], targetNode.position.x)
            assertEquals(posAttr[1], targetNode.position.y)
            assertEquals(posAttr[2], targetNode.position.z)
        }
        assertNull(
            targetNode.attributes["NonExistentAttribute_xyz"],
            "Expected null for missing attribute",
        )

        // Properties map
        val properties = targetNode.properties
        assertNotNull(properties)
        assertNull(
            targetNode.properties["NonExistentProperty_xyz"],
            "Expected null for missing property",
        )

        // toString format
        val nodeString = targetNode.toString()
        assertTrue(
            nodeString.contains("SpatialNode(") &&
                nodeString.contains("name='${targetNode.name}'") &&
                nodeString.contains("id=${targetNode.id}") &&
                nodeString.contains("depth=${targetNode.depth}"),
            "SpatialNode.toString() must contain standard node details, got: $nodeString",
        )
    }

    @Test
    fun spatialNode_spatialTransformsAndBoundingExtents() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Spatial Node Transforms Content") }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.findNode { it.hasSurfaceBounds },
                "Expected spatial node with bounds for $packageName",
            )

        // Position, Rotation, Scale, Pose
        assertNotNull(targetNode.position)
        assertNotNull(targetNode.rotation)
        assertNotNull(targetNode.scale)
        val pose = targetNode.pose
        assertEquals(targetNode.position, pose.translation)
        assertEquals(targetNode.rotation, pose.rotation)

        // World extents
        val extents = assertNotNull(targetNode.worldExtents)
        assertTrue(extents.x > 0f, "Expected positive world extents width, got ${extents.x}")
        assertTrue(extents.y > 0f, "Expected positive world extents height, got ${extents.y}")
        assertTrue(extents.z > 0f, "Expected positive world extents depth, got ${extents.z}")

        // Input target & canReceiveInput
        assertTrue(targetNode.canReceiveInput, "Expected node to be receptive to input")

        // Surface bounds in meters & pixels if present
        val boundsInMeters = targetNode.surfaceBoundsInMeters
        if (boundsInMeters != null) {
            assertTrue(boundsInMeters.x > 0f, "Expected surfaceBoundsInMeters.x > 0")
            assertTrue(boundsInMeters.y > 0f, "Expected surfaceBoundsInMeters.y > 0")
        }

        val boundsInPixels = targetNode.surfaceBoundsInPixels
        if (boundsInPixels != null) {
            assertTrue(boundsInPixels.x > 0f, "Expected surfaceBoundsInPixels.x > 0")
            assertTrue(boundsInPixels.y > 0f, "Expected surfaceBoundsInPixels.y > 0")
        }
    }

    @Test
    fun spatialScene_multiActivityQueryFilteringAndScoping() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("Scoping Primary Activity") }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val secondaryIntent =
            Intent(context, SecondarySpatialTestActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                )
            }

        val secondaryScenario =
            ActivityScenario.launch<SecondarySpatialTestActivity>(secondaryIntent)
        try {
            composeTestRule.waitForIdle()

            var scene: SpatialScene? = null
            composeTestRule.waitUntil(
                conditionDescription = "Secondary spatial activity node to appear in scene",
                timeoutMillis = 5000L,
            ) {
                val currentScene = SpatialSceneHelper.captureSpatialScene()
                if (
                    currentScene.tasks.any {
                        it.findNodesByName("SecondarySpatialTestActivity").isNotEmpty()
                    }
                ) {
                    scene = currentScene
                    true
                } else {
                    false
                }
            }
            val activeScene = assertNotNull(scene, "Expected active scene with secondary activity")

            // Query by predicate for activities across tasks
            val activityNodes =
                activeScene.tasks.flatMap { task ->
                    task.findNodes { node ->
                        node.name.contains("Activity") || node.header.contains("Activity")
                    }
                }
            assertTrue(
                activityNodes.isNotEmpty(),
                "Expected activity nodes in spatial scene, found ${activityNodes.size}",
            )

            // Verify each activity node has valid spatial positioning
            for (node in activityNodes) {
                assertNotNull(node.position)
                assertNotNull(node.rotation)
                assertNotNull(node.pose)
                node.worldExtents?.let { extents ->
                    assertTrue(extents.x > 0f)
                    assertTrue(extents.y > 0f)
                }
            }
        } finally {
            secondaryScenario.close()
        }
    }

    @Test
    fun spatialScene_fullSpaceSinglePanel_capturesSubspaceHierarchyAndPanelExtents() {
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(800.dp).height(600.dp).movable().resizable()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Full Space Single Panel Test Content", fontSize = 24.sp)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(scene, "Expected captured spatial scene in Full Space")
        val task =
            assertNotNull(
                scene.findTaskByPackageName(context.packageName),
                "Expected task for ${context.packageName} in Full Space",
            )

        // App root node in Full Space
        val appRoot = task.activitySpaceRoot
        assertNotNull(
            appRoot,
            "Expected activitySpaceRoot for ${context.packageName} in Full Space",
        )
        assertTrue(task.allNodes.contains(appRoot), "Expected allNodes to contain app root node")

        // Verify exact embedded leash count in Full Space (1 Subspace panel)
        val embeddedPanels = task.windowLeashes.filter { it.isEmbeddedWindowLeash }
        assertEquals(
            1,
            embeddedPanels.size,
            "Expected exactly 1 embedded panel in Full Space (the Subspace panel)",
        )

        val panelNode = embeddedPanels.single()
        assertTrue(panelNode.isEmbeddedWindowLeash, "Expected node to be an embedded window leash")
        assertTrue(panelNode.isWindowLeash, "Expected node to be identified as a window leash")
        assertTrue(
            panelNode.hasSurfaceBounds,
            "Expected panel to have surface bounds",
        )
        assertNotNull(panelNode.position)
        assertNotNull(panelNode.rotation)
        assertNotNull(panelNode.scale)
        assertEquals(panelNode.position, panelNode.pose.translation)

        assertTrue(
            task.topLevelWindowLeashes.contains(panelNode),
            "Expected topLevelWindowLeashes to contain panelNode",
        )

        // Verify bounds and extents
        panelNode.effectiveSurfaceBoundsInMeters?.let { bounds ->
            assertTrue(bounds.x > 0f, "Expected effectiveSurfaceBoundsInMeters.x > 0")
            assertTrue(bounds.y > 0f, "Expected effectiveSurfaceBoundsInMeters.y > 0")
        }
        panelNode.worldExtents?.let { extents ->
            assertTrue(extents.x > 0f, "Expected worldExtents.x > 0")
            assertTrue(extents.y > 0f, "Expected worldExtents.y > 0")
            assertTrue(extents.z > 0f, "Expected worldExtents.z > 0")
            panelNode.worldHalfExtents?.let { halfExtents ->
                assertEquals(extents.x / 2f, halfExtents.x, 1e-3f)
                assertEquals(extents.y / 2f, halfExtents.y, 1e-3f)
            }
        }

        // Verify children and input targets attached to panel
        assertTrue(panelNode.children.isNotEmpty(), "Expected panel to have children")
        assertNotNull(
            panelNode.ownInputTarget,
            "Expected panel to have its own primary input target",
        )
        for (child in panelNode.children) {
            assertEquals(panelNode, child.parent)
        }

        // Verify input receptive targets
        assertTrue(panelNode.canReceiveInput, "Expected panel node to receive input")
        val inputTargets = task.inputTargets
        assertTrue(
            inputTargets.isNotEmpty(),
            "Expected inputTargets to contain panel input targets",
        )
    }

    @Test
    fun spatialScene_fullSpaceSpatialRowAndColumnLayout_verifiesSpatialHierarchyAndRelativePositions() {
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }

        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    horizontalArrangement = SpatialArrangement.spacedBy(40.dp),
                    verticalAlignment = SpatialAlignment.CenterVertically,
                ) {
                    SpatialColumn(
                        verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                        horizontalAlignment = SpatialAlignment.CenterHorizontally,
                    ) {
                        SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(200.dp)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Red)) {
                                Text("Left Top Panel")
                            }
                        }
                        SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(200.dp)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Green)) {
                                Text("Left Bottom Panel")
                            }
                        }
                    }
                    SpatialPanel(modifier = SubspaceModifier.width(400.dp).height(420.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Yellow)) {
                            Text("Right Main Panel")
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(scene)
        val task = assertNotNull(scene.findTaskByPackageName(context.packageName))

        val appRoot = task.activitySpaceRoot
        val allAppNodes = task.allNodes
        assertTrue(allAppNodes.size >= 3, "Expected multiple nodes in Row/Column hierarchy")

        // Verify exact embedded leash count (3 Subspace panels)
        val allPanels = task.windowLeashes.filter { it.isEmbeddedWindowLeash }
        assertEquals(3, allPanels.size, "Expected exactly 3 embedded panels")

        for (panel in allPanels) {
            assertTrue(panel.isEmbeddedWindowLeash, "Expected node to be an embedded window leash")
            assertTrue(panel.isWindowLeash, "Expected node to be a window leash")
            assertTrue(
                panel.hasSurfaceBounds,
                "Expected panel to have surface bounds",
            )
            assertTrue(
                panel.isDescendantOf(appRoot),
                "Panel ${panel.name} should be descendant of appRoot",
            )
            assertNotNull(panel.pose)
            assertTrue(panel.canReceiveInput)
            assertNotNull(
                panel.ownInputTarget,
                "Expected panel to have its own input target",
            )
        }

        val spatialRow =
            assertNotNull(
                task.findNodeByName("SpatialRow"),
                "Expected SpatialRow layout container containing child layout",
            )
        val spatialColumn =
            assertNotNull(
                task.findNodeByName("SpatialColumn"),
                "Expected child SpatialColumn layout container",
            )

        assertTrue(
            spatialColumn.isDescendantOf(spatialRow),
            "Expected SpatialColumn to be descendant of SpatialRow",
        )
        val rowPanels = spatialRow.findNodes { it.isWindowLeash }
        assertTrue(rowPanels.isNotEmpty(), "Expected panels within SpatialRow")
        val colPanels = spatialColumn.findNodes { it.isWindowLeash }
        assertTrue(colPanels.isNotEmpty(), "Expected panels within SpatialColumn")

        val layouts = listOf(spatialRow, spatialColumn)
        for (layoutNode in layouts) {
            assertTrue(layoutNode.isDescendantOf(appRoot))
            assertFalse(
                layoutNode.isWindowLeash,
                "Layout container ${layoutNode.name} should not be a window leash",
            )
            assertFalse(
                layoutNode.isAndroid2d,
                "Layout container ${layoutNode.name} should not be an Android 2D buffer",
            )
            assertTrue(
                layoutNode.children.isNotEmpty(),
                "Layout container should have child nodes",
            )
        }
    }

    @Test
    fun spatialScene_fullSpaceMainPanel_capturesMainWindowAndSubspaceIntegration() {
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }

        composeTestRule.setContent {
            // Main 2D content
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
                Text("2D Window Content for Main Panel")
            }

            // Subspace incorporating SpatialMainPanel and an auxiliary SpatialPanel
            Subspace {
                SpatialRow(horizontalArrangement = SpatialArrangement.spacedBy(50.dp)) {
                    SpatialMainPanel(
                        modifier = SubspaceModifier.width(600.dp).height(400.dp).movable()
                    )
                    SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(400.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Cyan)) {
                            Text("Auxiliary Side Panel")
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(scene)
        val task = assertNotNull(scene.findTaskByPackageName(context.packageName))

        // Verify exact top-level window leashes (1 task window leash + 1 embedded panel)
        val panels = task.topLevelWindowLeashes
        assertEquals(
            2,
            panels.size,
            "Expected exactly 2 top-level window leashes (main panel + auxiliary panel) in Full Space",
        )

        // Find primary task window leash node
        val mainPanel =
            assertNotNull(task.taskWindowLeash, "Expected taskWindowLeash for Main Panel")
        assertNotNull(mainPanel.pose)

        // Find auxiliary panel
        val auxPanel =
            assertNotNull(
                panels.firstOrNull { it.isEmbeddedWindowLeash },
                "Expected distinct auxiliary panel in subspace",
            )
        assertNotNull(auxPanel.pose)

        val spatialRow = assertNotNull(task.findNodeByName("SpatialRow"))
        assertTrue(
            mainPanel.isDescendantOf(spatialRow),
            "Expected main window panel to be descendant of SpatialRow",
        )
        assertTrue(
            auxPanel.isDescendantOf(spatialRow),
            "Expected auxPanel to be descendant of SpatialRow",
        )
        val panelsInRow = spatialRow.findNodes { it.isWindowLeash }
        assertTrue(panelsInRow.size >= 2, "Expected at least 2 panels in SpatialRow")

        // Input targets should include main window and side panel
        val inputTargets = task.inputTargets
        assertTrue(
            inputTargets.size >= 2,
            "Expected at least 2 input targets (main window + aux panel), found ${inputTargets.size}",
        )
    }

    @Test
    fun spatialScene_fullSpaceDynamicTransformation_capturesUpdatedTransformsAfterRecomposition() {
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }

        var panelOffsetX by mutableStateOf(0.dp)
        var panelWidth by mutableStateOf(400.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialBox {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.offset(x = panelOffsetX)
                                .width(panelWidth)
                                .height(300.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Magenta)) {
                            Text("Dynamic Transform Panel")
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val initialScene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(initialScene)
        val initialTask = assertNotNull(initialScene.findTaskByPackageName(context.packageName))
        val initialPanels = initialTask.windowLeashes.filter { it.isEmbeddedWindowLeash }
        assertEquals(1, initialPanels.size, "Expected 1 panel initially (the Subspace panel)")
        val initialPanel = initialPanels.single()
        val initialPose = initialPanel.pose

        // Mutate offset and width to trigger recomposition
        panelOffsetX = 150.dp
        panelWidth = 600.dp
        composeTestRule.waitForIdle()

        val updatedScene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(updatedScene)
        val updatedTask = assertNotNull(updatedScene.findTaskByPackageName(context.packageName))
        val updatedPanels = updatedTask.windowLeashes.filter { it.isEmbeddedWindowLeash }
        assertEquals(
            1,
            updatedPanels.size,
            "Expected 1 panel after update (the Subspace panel)",
        )
        val updatedPanel = updatedPanels.single()

        // Verify initial scene snapshot remained immutable
        assertEquals(initialPose, initialPanel.pose, "Initial snapshot must remain unchanged")
        assertNotNull(updatedPanel.pose)
    }

    @Test
    fun spatialScene_fullSpaceTransition_handlesSpaceModeChange() {
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(modifier = SubspaceModifier.width(500.dp).height(400.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) { Text("Space Transition Test Content") }
                }
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fullSpaceScene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(fullSpaceScene)
        val fullSpaceTask = assertNotNull(fullSpaceScene.findTaskByPackageName(context.packageName))
        val fullSpaceAppRoot = fullSpaceTask.activitySpaceRoot
        assertNotNull(fullSpaceAppRoot, "Expected app root in full space")
        assertTrue(fullSpaceTask.allNodes.isNotEmpty())
        assertTrue(
            fullSpaceTask.windowLeashes.isNotEmpty(),
            "Expected window leashes in Full Space",
        )
        assertTrue(
            fullSpaceTask.topLevelWindowLeashes.isNotEmpty(),
            "Expected topLevelWindowLeashes in Full Space",
        )

        // Transition to Home Space
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestHomeSpace() }
        composeTestRule.waitForIdle()

        val homeSpaceScene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(homeSpaceScene)
        val homeSpaceTask = assertNotNull(homeSpaceScene.findTaskByPackageName(context.packageName))
        assertNotNull(
            homeSpaceTask.taskWindowLeash,
            "Expected taskWindowLeash in home space",
        )

        // Return to Full Space
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }
        composeTestRule.waitForIdle()

        val restoredFullSpaceScene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(restoredFullSpaceScene)
        val restoredFullSpaceTask =
            assertNotNull(restoredFullSpaceScene.findTaskByPackageName(context.packageName))
        assertNotNull(restoredFullSpaceTask.activitySpaceRoot)
        assertTrue(
            restoredFullSpaceTask.windowLeashes.isNotEmpty(),
            "Expected window leashes in restored Full Space",
        )
        assertTrue(
            restoredFullSpaceTask.topLevelWindowLeashes.isNotEmpty(),
            "Expected topLevelWindowLeashes in restored Full Space",
        )
    }

    @Test
    fun spatialScene_fullSpaceMovableSpatialRow_capturesAffordanceAndChildHierarchy() {
        runBlocking(Dispatchers.Main) { composeTestRule.activity.requestFullSpace() }

        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier = SubspaceModifier.movable(),
                    horizontalArrangement = SpatialArrangement.spacedBy(30.dp),
                ) {
                    SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(200.dp)) {
                        Orbiter(position = OrbiterPosition.TopCenter()) { Text("Panel 1 Handle") }
                        Box(modifier = Modifier.fillMaxSize().background(Color.Red)) {
                            Text("Panel 1")
                        }
                    }
                    SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(200.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Blue)) {
                            Text("Panel 2")
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scene = SpatialSceneHelper.captureSpatialScene()
        assertNotNull(scene)
        val task = assertNotNull(scene.findTaskByPackageName(context.packageName))

        val spatialRow =
            assertNotNull(task.findNodeByName("SpatialRow"), "Expected SpatialRow layout container")
        assertFalse(spatialRow.isWindowLeash)
        assertFalse(spatialRow.isAndroid2d)
        assertTrue(spatialRow.isMovable, "Expected movable reform options on SpatialRow")

        // Find direct child panels under SpatialRow
        val childPanels = spatialRow.children.filter { it.isWindowLeash }
        assertEquals(
            2,
            childPanels.size,
            "Expected exactly 2 child panels under SpatialRow",
        )

        val panel1 = childPanels[0]
        val panel2 = childPanels[1]

        assertTrue(panel1.isWindowLeash)
        assertEquals(spatialRow, panel1.parent)
        assertTrue(panel1.children.isNotEmpty(), "Expected panel 1 to have attached children")
        val panel1AttachedPanels = panel1.childWindowLeashes
        assertEquals(
            1,
            panel1AttachedPanels.size,
            "Expected exactly 1 embedded satellite window attached to panel 1",
        )
        val orbiterPanel = panel1AttachedPanels.single()
        assertTrue(
            orbiterPanel.isEmbeddedWindowLeash,
            "Orbiter panel must be identified as an embedded window leash",
        )
        assertEquals(
            panel1,
            orbiterPanel.parentWindowLeash,
            "Orbiter panel's parentWindowLeash should resolve to panel1",
        )
        assertNotNull(
            panel1.ownInputTarget,
            "Expected panel 1 to have its own input target",
        )

        assertTrue(panel2.isWindowLeash)
        assertEquals(spatialRow, panel2.parent)
        assertTrue(panel2.children.isNotEmpty(), "Expected panel 2 to have attached children")
        val panel2AttachedPanels = panel2.childWindowLeashes
        assertEquals(0, panel2AttachedPanels.size, "Expected 0 attached windows on panel 2")
        assertNotNull(
            panel2.ownInputTarget,
            "Expected panel 2 to have its own input target",
        )

        val embeddedPanels = task.windowLeashes.filter { it.isEmbeddedWindowLeash }
        assertTrue(embeddedPanels.contains(orbiterPanel))
    }
}
