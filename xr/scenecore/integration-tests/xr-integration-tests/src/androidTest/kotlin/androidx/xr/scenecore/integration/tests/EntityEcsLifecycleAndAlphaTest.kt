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

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Integration tests validating SceneCore ECS [Entity] hierarchical alpha propagation into CPM
 * `computedAlpha`, `contentDescription` propagation to Android accessibility / Views, and visual
 * child lifecycle across [PanelEntity] and [GltfModelEntity] in CPM (b/581430543).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class EntityEcsLifecycleAndAlphaTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @Test
    fun alphaHierarchy_threeLevelPropagation_updatesCpmComputedAlpha() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace

        val root =
            Entity.create(
                session = session,
                name = "alpha_root",
                pose = Pose(Vector3(0f, 0f, -1.8f)),
                parent = activitySpace,
            )
        val middle = Entity.create(session = session, name = "alpha_middle", parent = root)

        val (childPanel, childGltf) =
            sessionRule.onMainThread {
                val model = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
                val panel =
                    PanelEntity.create(
                        session = session,
                        view = TextView(activity).apply { text = ALPHA_PANEL_NAME },
                        pixelDimensions = IntSize2d(480, 320),
                        name = ALPHA_PANEL_NAME,
                        pose = Pose(Vector3(-0.4f, 0f, 0f)),
                        parent = middle,
                    )
                val gltf =
                    GltfModelEntity.create(
                        session = session,
                        model = model,
                        pose = Pose(Vector3(0.4f, 0f, 0f)),
                        parent = middle,
                    )
                panel to gltf
            }

        // 3-level multiplicative alpha propagation: 0.8 * 0.5 * 0.5 = 0.2.
        root.setAlpha(0.8f)
        middle.setAlpha(0.5f)
        childPanel.setAlpha(0.5f)
        childGltf.setAlpha(0.5f)

        assertThat(childPanel.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.2f)
        assertThat(childGltf.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.2f)

        waitForSpatialScene(
            description = "3-level alpha hierarchy propagates computedAlpha ~= 0.2 to CPM leaves",
            timeout = 5.seconds,
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task != null && hasComputedAlpha(task, ALPHA_PANEL_NAME, expectedAlpha = 0.2f)
            },
        )

        // Updating root ancestor alpha to 1.0 updates effective child alpha: 1.0 * 0.5 * 0.5 =
        // 0.25.
        root.setAlpha(1.0f)
        assertThat(childPanel.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.25f)
        assertThat(childGltf.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.25f)

        waitForSpatialScene(
            description = "ancestor alpha update propagates computedAlpha ~= 0.25 to CPM leaves",
            timeout = 5.seconds,
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task != null && hasComputedAlpha(task, ALPHA_PANEL_NAME, expectedAlpha = 0.25f)
            },
        )
    }

    @Test
    fun contentDescription_panelAndGltf_exposesAccessibilityState() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace

        val (panelView, targetPanel, targetGltf) =
            sessionRule.onMainThread {
                val model = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
                val view = TextView(activity).apply { text = "accessibility_panel_body" }
                val panel =
                    PanelEntity.create(
                        session = session,
                        view = view,
                        pixelDimensions = IntSize2d(320, 240),
                        name = "cd_target_panel",
                        pose = Pose(Vector3(-0.4f, 0f, -1.6f)),
                        parent = activitySpace,
                    )
                val gltf =
                    GltfModelEntity.create(
                        session = session,
                        model = model,
                        pose = Pose(Vector3(0.4f, 0f, -1.6f)),
                        parent = activitySpace,
                    )
                Triple(view, panel, gltf)
            }

        waitForSpatialScene(
            description = "accessibility target panel becomes visible in CPM",
            timeout = 10.seconds,
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task != null &&
                    task.embeddedWindowLeashes.any {
                        it.name.contains("cd_target_panel") && it.isVisible
                    }
            },
        )

        configureMultiWindowUiAutomation()

        class AccessibilitySnapshot(
            val panelDesc: String,
            val rootViewDesc: String?,
            val rootFocusable: Boolean,
            val gltfDesc: String,
        )

        val (initialPanelDesc, initialGltfDesc) =
            sessionRule.onMainThread {
                val initialPanel = targetPanel.contentDescription.toString()
                val initialGltf = targetGltf.contentDescription.toString()
                targetPanel.contentDescription = "Accessible Panel Node"
                targetGltf.contentDescription = "Accessible Gltf Node"
                initialPanel to initialGltf
            }
        assertThat(initialPanelDesc).isEmpty()
        assertThat(initialGltfDesc).isEmpty()

        val accessible = sessionRule.onMainThread {
            AccessibilitySnapshot(
                panelDesc = targetPanel.contentDescription.toString(),
                rootViewDesc = panelView.rootView.contentDescription?.toString(),
                rootFocusable = panelView.rootView.isFocusable,
                gltfDesc = targetGltf.contentDescription.toString(),
            )
        }
        assertThat(accessible.panelDesc).isEqualTo("Accessible Panel Node")
        assertThat(accessible.rootViewDesc).isEqualTo("Accessible Panel Node")
        assertThat(accessible.rootFocusable).isTrue()
        assertThat(accessible.gltfDesc).isEqualTo("Accessible Gltf Node")

        // Non-panel Entities expose their contentDescription through an accessibility View on the
        // Activity's decorView, observable via UiAutomation AccessibilityNodeInfo traversal.
        waitUntil(
            description = "UiAutomation exposes GltfModelEntity contentDescription",
            timeout = 10.seconds,
        ) {
            hasAccessibilityContentDescription("Accessible Gltf Node")
        }

        val updatedRootViewDesc = sessionRule.onMainThread {
            targetPanel.contentDescription = "Updated Panel Node"
            targetGltf.contentDescription = "Updated Gltf Node"
            panelView.rootView.contentDescription?.toString()
        }
        assertThat(updatedRootViewDesc).isEqualTo("Updated Panel Node")

        waitUntil("UiAutomation reflects updated GltfModelEntity contentDescription") {
            hasAccessibilityContentDescription("Updated Gltf Node") &&
                !hasAccessibilityContentDescription("Accessible Gltf Node")
        }

        val cleared = sessionRule.onMainThread {
            targetPanel.contentDescription = ""
            targetGltf.contentDescription = ""
            AccessibilitySnapshot(
                panelDesc = targetPanel.contentDescription.toString(),
                rootViewDesc = panelView.rootView.contentDescription?.toString(),
                rootFocusable = panelView.rootView.isFocusable,
                gltfDesc = targetGltf.contentDescription.toString(),
            )
        }
        assertThat(cleared.panelDesc).isEmpty()
        assertThat(cleared.rootViewDesc).isEmpty()
        assertThat(cleared.gltfDesc).isEmpty()

        waitUntil("UiAutomation removes cleared GltfModelEntity accessibility node") {
            !hasAccessibilityContentDescription("Updated Gltf Node")
        }
    }

    @Test
    fun visualChildrenLifecycle_panelAndGltfHierarchy_propagatesCpmState() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace

        val containerPose = Pose(Vector3(0f, 0f, -1.8f))
        val container =
            Entity.create(
                    session = session,
                    name = "visual_root_container",
                    pose = containerPose,
                )
                .apply {
                    setScale(1.5f, Space.PARENT)
                    setAlpha(0.5f)
                }

        val (panel, gltfEntity) =
            sessionRule.onMainThread {
                val model = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
                val childPanel =
                    PanelEntity.create(
                            session = session,
                            view = TextView(activity).apply { text = VISUAL_PANEL_NAME },
                            pixelDimensions = IntSize2d(480, 320),
                            name = VISUAL_PANEL_NAME,
                            pose = Pose(Vector3(-0.4f, 0f, 0f)),
                            parent = container,
                        )
                        .apply { setAlpha(0.6f) }
                val childGltf =
                    GltfModelEntity.create(
                            session = session,
                            model = model,
                            pose = Pose(Vector3(0.4f, 0f, 0f)),
                            parent = container,
                        )
                        .apply { setAlpha(0.8f) }
                childPanel to childGltf
            }

        // Detached container preserves child alpha propagation and parent-child linkage.
        assertThat(container.parent).isNull()
        assertThat(container.children).containsExactly(panel, gltfEntity)
        assertThat(panel.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.3f)
        assertThat(gltfEntity.getAlpha(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(0.4f)

        // Attaching container to ActivitySpace propagates pose, scale, and alpha into CPM.
        container.parent = activitySpace
        assertPose(panel.getPose(Space.ACTIVITY), Pose(Vector3(-0.6f, 0f, -1.8f)), FLOAT_TOLERANCE)
        assertPose(
            gltfEntity.getPose(Space.ACTIVITY),
            Pose(Vector3(0.6f, 0f, -1.8f)),
            FLOAT_TOLERANCE,
        )
        assertThat(panel.getScale(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(1.5f)
        assertThat(gltfEntity.getScale(Space.ACTIVITY)).isWithin(FLOAT_TOLERANCE).of(1.5f)

        val snapshot =
            checkNotNull(
                waitForSpatialScene(
                    description = "panel and glTF children visible in CPM with inherited state",
                    timeout = 5.seconds,
                    select = { scene ->
                        val task =
                            scene.findTaskByActivity(sessionRule.activity)
                                ?: return@waitForSpatialScene null
                        val spaceRoot =
                            task.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                                ?: return@waitForSpatialScene null
                        val panelNode =
                            task.embeddedWindowLeashes.firstOrNull {
                                it.name.contains(VISUAL_PANEL_NAME) &&
                                    it.isVisible &&
                                    it.worldExtents != null
                            } ?: return@waitForSpatialScene null
                        val gltfPair =
                            findVisibleGltfNodeAndSubspace(task) ?: return@waitForSpatialScene null
                        Triple(spaceRoot, panelNode, gltfPair)
                    },
                    until = { it != null },
                )
            )

        val (activitySpaceNode, panelNode, gltfPair) = snapshot
        val (gltfNode, gltfSubspace) = gltfPair

        assertVector3(
            panelNode.position,
            activitySpaceNode.transformPointToWorld(Vector3(-0.6f, 0f, -1.8f)),
            CPM_FLOAT_TOLERANCE,
        )
        assertVector3(
            gltfNode.position,
            activitySpaceNode.transformPointToWorld(Vector3(0.6f, 0f, -1.8f)),
            CPM_FLOAT_TOLERANCE,
        )
        assertVector3(
            gltfNode.scale,
            activitySpaceNode.scale * 1.5f,
            CPM_FLOAT_TOLERANCE,
        )

        val panelComputedAlpha =
            checkNotNull(panelNode.properties["computedAlpha"]?.toFloatOrNull())
        val gltfComputedAlpha =
            checkNotNull(
                (gltfSubspace.properties["computedAlpha"] ?: gltfNode.properties["computedAlpha"])
                    ?.toFloatOrNull()
            )
        assertThat(panelComputedAlpha).isWithin(CPM_FLOAT_TOLERANCE).of(0.3f)
        assertThat(gltfComputedAlpha).isWithin(CPM_FLOAT_TOLERANCE).of(0.4f)

        // Toggling container enabled state hides and restores both CPM children.
        container.setEnabled(false)
        waitForSpatialScene(
            description = "disabling container hides both panel and glTF children in CPM",
            timeout = 5.seconds,
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task != null &&
                    task.embeddedWindowLeashes.none {
                        it.name.contains(VISUAL_PANEL_NAME) && it.isVisible
                    } &&
                    findVisibleGltfNodeAndSubspace(task) == null
            },
        )

        container.setEnabled(true)
        waitForSpatialScene(
            description = "re-enabling container restores panel and glTF visibility in CPM",
            timeout = 5.seconds,
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task != null &&
                    task.embeddedWindowLeashes.any {
                        it.name.contains(VISUAL_PANEL_NAME) && it.isVisible
                    } &&
                    findVisibleGltfNodeAndSubspace(task) != null
            },
        )

        // Detaching both children removes them from the scene graph and visible CPM hierarchy.
        panel.parent = null
        gltfEntity.parent = null
        container.parent = null
        assertThat(panel.parent).isNull()
        assertThat(gltfEntity.parent).isNull()
        assertThat(container.parent).isNull()
        assertThrows(IllegalStateException::class.java) { gltfEntity.getPose(Space.ACTIVITY) }

        waitForSpatialScene(
            description = "detaching visual children removes panel and glTF nodes from CPM",
            timeout = 5.seconds,
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task != null &&
                    task.embeddedWindowLeashes.none {
                        it.name.contains(VISUAL_PANEL_NAME) && it.isVisible
                    } &&
                    findVisibleGltfNodeAndSubspace(task) == null
            },
        )
    }

    private fun hasComputedAlpha(
        task: SpatialTask,
        panelName: String,
        expectedAlpha: Float,
    ): Boolean {
        val panelNode =
            task.embeddedWindowLeashes.firstOrNull {
                it.name.contains(panelName) && it.isVisible
            } ?: return false
        val gltfPair = findVisibleGltfNodeAndSubspace(task) ?: return false
        val panelAlpha = panelNode.properties["computedAlpha"]?.toFloatOrNull() ?: return false
        val gltfAlpha =
            (gltfPair.second.properties["computedAlpha"]
                    ?: gltfPair.first.properties["computedAlpha"])
                ?.toFloatOrNull() ?: return false
        return abs(panelAlpha - expectedAlpha) <= CPM_FLOAT_TOLERANCE &&
            abs(gltfAlpha - expectedAlpha) <= CPM_FLOAT_TOLERANCE
    }

    private fun findVisibleGltfNodeAndSubspace(task: SpatialTask): Pair<SpatialNode, SpatialNode>? {
        val gltfNode =
            task.activitySpaceRoot
                .findNodes { node ->
                    node.isVisible &&
                        node.name != ACTIVITY_SPACE_NODE_NAME &&
                        node.children.any { child ->
                            child.isVisible &&
                                child.hasSubspace &&
                                child.name.startsWith(GLTF_SUBSPACE_PREFIX)
                        }
                }
                .firstOrNull() ?: return null
        val subspace =
            gltfNode.children.firstOrNull {
                it.isVisible && it.hasSubspace && it.name.startsWith(GLTF_SUBSPACE_PREFIX)
            } ?: return null
        return gltfNode to subspace
    }

    private fun configureMultiWindowUiAutomation(): UiAutomation {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.serviceInfo?.let { info ->
            val requiredFlags =
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            if ((info.flags and requiredFlags) != requiredFlags) {
                info.flags = info.flags or requiredFlags
                uiAutomation.serviceInfo = info
            }
        }
        return uiAutomation
    }

    private fun hasAccessibilityContentDescription(expected: String): Boolean {
        val uiAutomation = configureMultiWindowUiAutomation()
        val roots = buildList {
            uiAutomation.rootInActiveWindow?.let(::add)
            uiAutomation.windows?.mapNotNullTo(this) { it.root }
        }
        return roots.any { root -> containsAccessibilityContentDescription(root, expected) }
    }

    private fun containsAccessibilityContentDescription(
        node: AccessibilityNodeInfo,
        expected: String,
    ): Boolean {
        if (node.contentDescription?.toString() == expected) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsAccessibilityContentDescription(child, expected)) {
                return true
            }
        }
        return false
    }

    private companion object {
        const val FLOAT_TOLERANCE = 1e-4f
        const val CPM_FLOAT_TOLERANCE = 5e-2f
        const val ALPHA_PANEL_NAME = "alpha_child_panel"
        const val VISUAL_PANEL_NAME = "visual_child_panel"
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val GLTF_SUBSPACE_PREFIX = "gltf_entity_subspace_"
    }
}
