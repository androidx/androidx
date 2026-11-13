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

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.runtime.cpm.parseSpatialScene
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpatialSceneHelperTest {
    @Test
    fun spatialNode_extractsAttributesCorrectly() {
        val attributes =
            mapOf(
                "ImpNodeWorldPosition" to floatArrayOf(1.0f, 2.0f, 3.0f),
                "ImpNodeWorldRotation" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f), // [w, x, y, z]
                "ImpNodeWorldScale" to floatArrayOf(0.5f, 0.5f, 0.5f),
                "root2DSurfaceBoundsInMeters" to floatArrayOf(0.4f, 0.3f),
            )
        val node = SpatialNode("TestNode", attributes = attributes)

        assertEquals(Vector3(1.0f, 2.0f, 3.0f), node.position)
        // Hamilton [1, 0, 0, 0] -> Quaternion(x=0, y=0, z=0, w=1)
        assertEquals(Quaternion(0.0f, 0.0f, 0.0f, 1.0f), node.rotation)
        assertEquals(Vector3(0.5f, 0.5f, 0.5f), node.scale)
        assertEquals(Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion.Identity), node.pose)
        assertEquals(Vector2(0.4f, 0.3f), node.surfaceBoundsInMeters)
    }

    @Test
    fun spatialNode_defaultsWhenAttributesMissing() {
        val node = SpatialNode("EmptyNode", attributes = emptyMap())

        assertEquals(Vector3.Zero, node.position)
        assertEquals(Quaternion.Identity, node.rotation)
        assertEquals(Vector3.One, node.scale)
        assertEquals(Pose.Identity, node.pose)
        assertNull(node.surfaceBoundsInMeters)
    }

    @Test
    fun spatialNode_equalsHashCodeAndToString() {
        val node1 = SpatialNode(header = "Node Test id=1:", name = "Test", id = 1, depth = 2)
        val node2 = SpatialNode(header = "Node Test id=1:", name = "Test", id = 1, depth = 2)
        val node3 = SpatialNode(header = "Node Other id=2:", name = "Other", id = 2, depth = 2)

        assertEquals(node1, node2)
        assertEquals(node1.hashCode(), node2.hashCode())
        assertFalse(node1 == node3)
        assertFalse(node1.equals("NotASpatialNode"))

        val str = node1.toString()
        assertTrue(str.contains("Test"))
        assertTrue(str.contains("1"))
        assertTrue(str.contains("2"))
    }

    @Test
    fun spatialNode_hierarchyRelationships() {
        val parent = SpatialNode("Parent", id = 1, depth = 0)
        val child = SpatialNode("Child", id = 2, depth = 2)
        val grandChild = SpatialNode("GrandChild", id = 3, depth = 4)

        parent.addChild(child)
        child.addChild(grandChild)

        assertEquals(parent, child.parent)
        assertEquals(child, grandChild.parent)
        assertTrue(parent.children.contains(child))
        assertTrue(child.children.contains(grandChild))

        assertTrue(grandChild.isDescendantOf(parent))
        assertTrue(grandChild.isDescendantOf(child))
        assertTrue(child.isDescendantOf(parent))
        assertFalse(parent.isDescendantOf(child))
        assertFalse(parent.isDescendantOf(grandChild))

        val descendants = parent.getDescendants()
        assertEquals(listOf(child, grandChild), descendants)
    }

    @Test
    fun spatialNode_windowPropertiesAndReformOptions() {
        val windowNode =
            SpatialNode(
                header = "Node window-task-10 (stateIndex=0) id=10:",
                name = "window-task-10",
                id = 10,
                depth = 2,
                properties =
                    mapOf(
                        "reformOptions" to "<enable=3, flags=7>",
                        "isAndroid2d" to "false",
                        "isAppSpatialEmbeddedWindowLeash" to "false",
                        "isSysUi" to "false",
                    ),
            )
        assertTrue(windowNode.isMovable)
        assertTrue(windowNode.isResizable)
        assertFalse(windowNode.isAndroid2d)
        assertFalse(windowNode.isEmbeddedWindowLeash)
        assertFalse(windowNode.isSysUi)

        val movableOnlyNode =
            SpatialNode(
                header = "Node SpatialRow (stateIndex=0) id=20:",
                name = "SpatialRow",
                id = 20,
                depth = 2,
                properties = mapOf("reformOptions" to "<enable=1, flags=7>"),
            )
        assertTrue(movableOnlyNode.isMovable)
        assertFalse(movableOnlyNode.isResizable)

        val surfaceNode =
            SpatialNode(
                header = "Node Surface (stateIndex=0) id=30:",
                name = "#30",
                id = 30,
                depth = 4,
                attributes = mapOf("touchableRegion" to floatArrayOf(0f, 0f, 800f, 600f)),
                properties =
                    mapOf("isAndroid2d" to "true", "input-id" to "123", "buffer" to "800x600"),
            )
        assertTrue(surfaceNode.isAndroid2d)
        assertEquals(123, surfaceNode.inputId)
        assertEquals(Vector2(800f, 600f), surfaceNode.bufferSize)
        assertTrue(surfaceNode.hasSurfaceBounds)
        assertTrue(surfaceNode.isInputTarget)
    }

    @Test
    fun spatialNode_propertyQueries_evaluatedCorrectly() {
        val rootNode = SpatialNode(header = "Node Root id=1:", name = "Root", id = 1, depth = 0)
        val taskRoot =
            SpatialNode(
                header = "Node space-root-task-1 id=2:",
                name = "space-root-task-1",
                id = 2,
                depth = 1,
            )
        val windowTask =
            SpatialNode(
                header = "Node window-task-1 id=5:",
                name = "window-task-1",
                id = 5,
                depth = 2,
                properties = mapOf("isSysUi" to "false"),
            )
        val windowOverlay =
            SpatialNode(
                header = "Node window-MIRROR id=6:",
                name = "window-MIRROR_SYSTEM_ALERT",
                id = 6,
                depth = 2,
                properties = mapOf("isSysUi" to "true"),
            )
        val panel =
            SpatialNode(
                header = "Node Panel id=21:",
                name = "ViewPanel:1",
                id = 21,
                depth = 7,
                properties = mapOf("isAppSpatialEmbeddedWindowLeash" to "true"),
            )

        assertTrue(taskRoot.isTaskRoot)
        assertEquals(1, taskRoot.taskId)
        assertFalse(rootNode.isTaskRoot)

        assertTrue(windowOverlay.isSysUi)
        assertFalse(windowTask.isSysUi)

        assertTrue(panel.isEmbeddedWindowLeash)
        assertFalse(windowTask.isEmbeddedWindowLeash)
    }

    @Test
    fun spatialScene_constructedDirectlyWithNodes_populatesHierarchyAndTasks() {
        val rootNode = SpatialNode(header = "Node Root id=1:", name = "Root", id = 1, depth = 0)
        val taskRoot =
            SpatialNode(
                header = "Node space-root-task-77 id=2:",
                name = "space-root-task-77",
                id = 2,
                depth = 1,
            )
        val windowNode =
            SpatialNode(
                header = "Node com.example.app/MainActivity#101 id=3:",
                name = "com.example.app/MainActivity#101",
                id = 3,
                depth = 2,
            )
        taskRoot.addChild(windowNode)
        rootNode.addChild(taskRoot)

        val directScene =
            SpatialScene(
                rootNodes = listOf(rootNode),
                taskPackageMap = mapOf(77 to "com.example.app"),
            )
        assertEquals(listOf(rootNode), directScene.rootNodes)
        val task = assertNotNull(directScene.findTaskById(77))
        assertEquals("com.example.app", task.packageName)
        assertEquals(task, directScene.findTaskByPackageName("com.example.app"))
    }

    @Test
    fun spatialNode_childOfHiddenTaskRoot_inheritsHiddenVisibility() {
        val rootNode = SpatialNode(header = "Node Root id=1:", name = "Root", id = 1, depth = 0)
        val hiddenTaskRoot =
            SpatialNode(
                header = "Node space-root-task-77 id=2:",
                name = "space-root-task-77",
                id = 2,
                depth = 1,
                properties = mapOf("requested-vis" to "hide"),
            )
        val childNode =
            SpatialNode(
                header = "Node com.example.app/MainActivity#101 id=3:",
                name = "com.example.app/MainActivity#101",
                id = 3,
                depth = 2,
                properties = mapOf("requested-vis" to "show"),
            )
        hiddenTaskRoot.addChild(childNode)
        rootNode.addChild(hiddenTaskRoot)

        assertFalse(hiddenTaskRoot.isVisible)
        assertFalse(childNode.isVisible)

        val visibleTaskRoot =
            SpatialNode(
                header = "Node space-root-task-88 id=4:",
                name = "space-root-task-88",
                id = 4,
                depth = 1,
                properties = mapOf("requested-vis" to "show"),
            )
        val visibleChildNode =
            SpatialNode(
                header = "Node com.example.app/SecondActivity#102 id=5:",
                name = "com.example.app/SecondActivity#102",
                id = 5,
                depth = 2,
                properties = mapOf("requested-vis" to "show"),
            )
        visibleTaskRoot.addChild(visibleChildNode)
        rootNode.addChild(visibleTaskRoot)

        assertTrue(visibleTaskRoot.isVisible)
        assertTrue(visibleChildNode.isVisible)
    }

    @Test
    fun spatialTask_nodeQueries_findsNodesByPredicateAndName() {
        val dump =
            """
            Node Root (stateIndex=0) id=1:
              Node space-root-task-1 (stateIndex=0) id=2:
                Node space-transform-spatial-1 (stateIndex=0) id=3:
                  Node PrimaryPanel (stateIndex=0) id=4:
                  isAppSpatialEmbeddedWindowLeash=true
                    Node ChildPanel (stateIndex=0) id=5:
                    isAppSpatialEmbeddedWindowLeash=true
                    Node #6 (stateIndex=0) id=6:
                    input-id=6 canReceiveInput=true
            """
                .trimIndent()

        val scene = parseSpatialScene(dump)
        val task = assertNotNull(scene.findTaskById(1))
        val panelNames = task.findNodes { it.isEmbeddedWindowLeash }.map { it.name }
        assertEquals(listOf("PrimaryPanel", "ChildPanel"), panelNames)

        val inputTarget = assertNotNull(task.findNode { it.isInputTarget })
        assertEquals("#6", inputTarget.name)

        val foundChild = assertNotNull(task.findNodeByName("Child"))
        assertEquals("ChildPanel", foundChild.name)
    }

    @Test
    fun spatialNode_nodeWithHashInName_propertiesParsedCorrectly() {
        val panel =
            SpatialNode(
                header = "Node Panel (stateIndex=0) id=10:",
                name = "Panel #1 - Main Selection",
                id = 10,
                depth = 2,
                properties = mapOf("isAppSpatialEmbeddedWindowLeash" to "true"),
            )
        assertEquals("Panel #1 - Main Selection", panel.name)
        assertTrue(panel.isEmbeddedWindowLeash)
    }

    @Test
    fun spatialNode_surfaceContainer_returnsActualSurfaceBounds() {
        val windowContainer =
            SpatialNode(
                header = "Node window-task-42 (stateIndex=0) id=1:",
                name = "window-task-42",
                id = 1,
                depth = 2,
            )
        val captionBar =
            SpatialNode(
                header = "Node TaskBar#0 (stateIndex=0) id=2:",
                name = "TaskBar#0",
                id = 2,
                depth = 3,
                properties = mapOf("isSysUi" to "true", "isAndroid2d" to "true"),
                attributes =
                    mapOf(
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(0.384f, 0.128f),
                        "root2DSurfaceBoundsInPixels" to floatArrayOf(384f, 128f),
                        "touchableRegion" to floatArrayOf(0f, 0f, 384f, 128f),
                    ),
            )
        val surface =
            SpatialNode(
                header = "Node com.example.app/MainActivity#0 (stateIndex=0) id=3:",
                name = "com.example.app/MainActivity#0",
                id = 3,
                depth = 3,
                properties = mapOf("isSysUi" to "false", "isAndroid2d" to "true"),
                attributes =
                    mapOf(
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(2.240f, 1.400f),
                        "root2DSurfaceBoundsInPixels" to floatArrayOf(2048f, 1280f),
                        "touchableRegion" to floatArrayOf(0f, 0f, 2048f, 1280f),
                    ),
            )

        // Attach decoration / SysUI caption bar and the application window surface to the container
        windowContainer.addChild(captionBar)
        windowContainer.addChild(surface)

        // Querying the surface container returns the actual surface bounds of the contained surface
        assertEquals(surface.surfaceBoundsInMeters, windowContainer.surfaceBoundsInMeters)
        assertEquals(surface.surfaceBoundsInPixels, windowContainer.surfaceBoundsInPixels)
        assertEquals(Vector2(2.240f, 1.400f), windowContainer.surfaceBoundsInMeters)
        assertEquals(Vector2(2048f, 1280f), windowContainer.surfaceBoundsInPixels)

        val containerTouch = assertNotNull(windowContainer.touchableRegion)
        val surfaceTouch = assertNotNull(surface.touchableRegion)
        assertEquals(surfaceTouch.left, containerTouch.left)
        assertEquals(surfaceTouch.top, containerTouch.top)
        assertEquals(surfaceTouch.right, containerTouch.right)
        assertEquals(surfaceTouch.bottom, containerTouch.bottom)

        assertEquals(surface.worldExtents, windowContainer.worldExtents)
    }

    @Test
    fun spatialNode_transformPointToWorld_andFromWorld_withScale() {
        val node =
            SpatialNode(
                name = "test-node",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(1.0f, 2.0f, 3.0f),
                        "ImpNodeWorldRotation" to floatArrayOf(0.7071068f, 0.0f, 0.7071068f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(0.5f, 2.0f, 4.0f),
                    ),
            )

        val localPoint = Vector3(1.0f, 1.0f, 1.0f)
        // Scaled local point: (0.5, 2.0, 4.0)
        // 90-degree Y rotation rotates (x, y, z) to (z, y, -x):
        // (4.0, 2.0, -0.5)
        // Add world position (1.0, 2.0, 3.0):
        // (5.0, 4.0, 2.5)
        val worldPoint = node.transformPointToWorld(localPoint)
        assertEquals(5.0f, worldPoint.x, 1e-4f)
        assertEquals(4.0f, worldPoint.y, 1e-4f)
        assertEquals(2.5f, worldPoint.z, 1e-4f)

        // Transform back from world to local
        val backToLocal = node.transformPointFromWorld(worldPoint)
        assertEquals(localPoint.x, backToLocal.x, 1e-4f)
        assertEquals(localPoint.y, backToLocal.y, 1e-4f)
        assertEquals(localPoint.z, backToLocal.z, 1e-4f)
    }

    @Test
    fun spatialNode_transformVectorToWorld_andFromWorld_withScale() {
        val node =
            SpatialNode(
                name = "test-node",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(10.0f, 20.0f, 30.0f),
                        "ImpNodeWorldRotation" to floatArrayOf(0.7071068f, 0.0f, 0.7071068f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(0.5f, 2.0f, 4.0f),
                    ),
            )

        val localVector = Vector3(1.0f, 1.0f, 1.0f)
        // Vectors ignore position offset; only scale and rotation apply:
        // (4.0, 2.0, -0.5)
        val worldVector = node.transformVectorToWorld(localVector)
        assertEquals(4.0f, worldVector.x, 1e-4f)
        assertEquals(2.0f, worldVector.y, 1e-4f)
        assertEquals(-0.5f, worldVector.z, 1e-4f)

        // Transform back from world to local
        val backToLocal = node.transformVectorFromWorld(worldVector)
        assertEquals(localVector.x, backToLocal.x, 1e-4f)
        assertEquals(localVector.y, backToLocal.y, 1e-4f)
        assertEquals(localVector.z, backToLocal.z, 1e-4f)
    }

    @Test
    fun spatialScene_toString_includesTaskAndRootNodeSummary() {
        val taskRoot = SpatialNode(name = "space-root-task-42", id = 42)
        val scene = SpatialScene(listOf(taskRoot), taskPackageMap = mapOf(42 to "com.example.xr"))

        val summary = scene.toString()
        assertTrue(summary.contains("SpatialTask(taskId=42, packageName=com.example.xr"))
        assertTrue(summary.contains("rootNodes=1"))
    }
}
