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
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpaceChangeEvent
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSettledSpatialTask
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialContinuityTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun mainPanelKeyEntity_afterFullSpaceDrag_preservesContinuityAcrossHomeAndFullSpace() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()

        val scene = sessionRule.session.scene
        val mainPanel = scene.mainPanelEntity
        assertThat(scene.keyEntity).isSameInstanceAs(mainPanel)

        val movableComponent =
            MovableComponent.createSystemMovable(
                session = sessionRule.session,
                scaleInZ = false,
            )
        val moveEndLatch = CountDownLatch(1)
        val moveListener =
            object : EntityMoveListener {
                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    moveEndLatch.countDown()
                }
            }
        movableComponent.addMoveListener(Executor { it.run() }, moveListener)
        assertThat(mainPanel.addComponent(movableComponent)).isTrue()

        val initialActivityPose = mainPanel.getPose(Space.ACTIVITY)
        val initialNode = waitForMovableMainPanelNode(sessionRule.activity)

        initialNode.performSpatialInteraction { moveNodeBy(DRAG_DELTA) }

        assertThat(moveEndLatch.await(EVENT_WAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        waitUntil("Dragged mainPanel pose moves away from initial ActivitySpace pose") {
            (mainPanel.getPose(Space.ACTIVITY).translation - initialActivityPose.translation)
                .length > MIN_DRAG_DISTANCE_METERS
        }

        val draggedWorldNode =
            waitForMainPanelShiftedFromActivityOrigin(
                hostActivity = sessionRule.activity,
                minDistanceMeters = MIN_DRAG_DISTANCE_METERS,
            )
        val draggedWorldPos = draggedWorldNode.position

        sessionRule.requestHomeSpace()
        val hsmContinuityNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "HSM task window (${hsmContinuityNode.position}) should shift away from baseline " +
                    "(${baselineNode.position}) toward dragged keyEntity ($draggedWorldPos)"
            )
            .that((hsmContinuityNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
        assertWithMessage(
                "HSM task window (${hsmContinuityNode.position}) should land closer to dragged " +
                    "keyEntity ($draggedWorldPos) than baseline (${baselineNode.position})"
            )
            .that((hsmContinuityNode.position - draggedWorldPos).length)
            .isLessThan((hsmContinuityNode.position - baselineNode.position).length)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        waitUntil("Returning to Full Space resets mainPanelEntity pose to origin") {
            mainPanel.getPose(Space.ACTIVITY).translation.length < TRANSFORM_EPSILON
        }
        val returnedWorldNode = waitForMainPanelNode(sessionRule.activity)
        assertWithMessage(
                "Returned Full Space main panel (${returnedWorldNode.position}) should preserve " +
                    "continuity offset from baseline (${baselineNode.position})"
            )
            .that((returnedWorldNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
    }

    @Test
    fun customPanelKeyEntity_continuityFollowsAssignedPanelOverMainPanel() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()

        val scene = sessionRule.session.scene
        val mainPanel = scene.mainPanelEntity
        mainPanel.setPose(LEFT_PANEL_POSE)

        val customPanel = createCustomPanel()
        scene.keyEntity = customPanel

        val separatedNodes =
            waitForSeparatedPanelsInSingleSnapshot(
                hostActivity = sessionRule.activity,
                customPanelName = CUSTOM_PANEL_NAME,
                minDistanceMeters = 0.25f,
            )
        val mainWorldPos = separatedNodes.first.position
        val customWorldPos = separatedNodes.second.position

        sessionRule.requestHomeSpace()
        val hsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should shift from baseline " +
                    "(${baselineNode.position}) toward custom keyEntity ($customWorldPos)"
            )
            .that((hsmNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should be closer to custom keyEntity " +
                    "($customWorldPos) than non-key mainPanel ($mainWorldPos)"
            )
            .that((hsmNode.position - customWorldPos).length + 0.10f)
            .isLessThan((hsmNode.position - mainWorldPos).length)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        waitUntil("Returning to Full Space resets custom keyEntity pose to origin") {
            customPanel.getPose(Space.ACTIVITY).translation.length < TRANSFORM_EPSILON
        }
        assertPose(customPanel.getPose(Space.ACTIVITY), Pose.Identity, TRANSFORM_EPSILON)
        assertVector3(
            mainPanel.getPose(Space.ACTIVITY).translation,
            LEFT_PANEL_POSE.translation,
            TRANSFORM_EPSILON,
        )
    }

    @Test
    fun gltfModelKeyEntity_continuityPreservedAcrossSpaceTransitions() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()

        val scene = sessionRule.session.scene
        scene.mainPanelEntity.setEnabled(false)
        val model = sessionRule.onMainThread {
            GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
        }
        val gltfEntity = sessionRule.onMainThread {
            GltfModelEntity.create(
                session = session,
                model = model,
                pose = GLTF_OFFSET_POSE,
                parent = session.scene.activitySpace,
            )
        }
        scene.keyEntity = gltfEntity

        val gltfNode =
            waitForGltfSubspaceParentNode(
                hostActivity = sessionRule.activity,
                minOffsetFromActivityOriginMeters = 0.25f,
            )
        val gltfWorldPos = gltfNode.position

        sessionRule.requestHomeSpace()
        val hsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should shift from baseline " +
                    "(${baselineNode.position}) toward glTF keyEntity ($gltfWorldPos)"
            )
            .that((hsmNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should land closer to glTF keyEntity " +
                    "($gltfWorldPos) than baseline (${baselineNode.position})"
            )
            .that((hsmNode.position - gltfWorldPos).length)
            .isLessThan((hsmNode.position - baselineNode.position).length)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        waitUntil("Returning to Full Space resets glTF keyEntity pose to origin") {
            gltfEntity.getPose(Space.ACTIVITY).translation.length < TRANSFORM_EPSILON
        }
        assertPose(gltfEntity.getPose(Space.ACTIVITY), Pose.Identity, TRANSFORM_EPSILON)
    }

    @Test
    fun clearingKeyEntity_revokesContinuityAndSkipsDefaultModeUpdate() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()

        val scene = sessionRule.session.scene
        val mainPanel = scene.mainPanelEntity
        mainPanel.setPose(RIGHT_PANEL_POSE)
        mainPanel.setScale(CUSTOM_SCALE)

        waitForMainPanelShiftedFromActivityOrigin(
            hostActivity = sessionRule.activity,
            minDistanceMeters = MIN_DRAG_DISTANCE_METERS,
        )

        // Confirm continuity works while keyEntity is set before testing revocation.
        sessionRule.requestHomeSpace()
        val shiftedHsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertThat((shiftedHsmNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        scene.keyEntity = null
        mainPanel.setPose(CLEAR_PANEL_POSE)
        mainPanel.setScale(CUSTOM_SCALE)

        waitForMainPanelShiftedFromActivityOrigin(
            hostActivity = sessionRule.activity,
            minDistanceMeters = MIN_DRAG_DISTANCE_METERS,
        )

        sessionRule.requestHomeSpace()
        val revokedHsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "With keyEntity = null, HSM window (${revokedHsmNode.position}) should return " +
                    "near baseline (${baselineNode.position}) rather than tracking cleared pose"
            )
            .that((revokedHsmNode.position - baselineNode.position).length)
            .isLessThan(MAX_BASELINE_RETURN_DRIFT_METERS)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        assertPose(mainPanel.getPose(Space.ACTIVITY), CLEAR_PANEL_POSE, TRANSFORM_EPSILON)
        assertThat(mainPanel.getScale(Space.ACTIVITY)).isWithin(TRANSFORM_EPSILON).of(CUSTOM_SCALE)
    }

    @Test
    fun customSpaceChangedListener_receivesRecommendedContinuityEventOnFullSpaceReturn() {
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        val scene = sessionRule.session.scene
        val mainPanel = scene.mainPanelEntity
        mainPanel.setPose(ROTATED_KEY_POSE)
        mainPanel.setScale(CUSTOM_SCALE)

        waitForMainPanelShiftedFromActivityOrigin(
            hostActivity = sessionRule.activity,
            minDistanceMeters = MIN_DRAG_DISTANCE_METERS,
        )

        sessionRule.requestHomeSpace()
        waitForHomeSpaceTaskNode(sessionRule.activity)

        val capturedEvent = AtomicReference<SpaceChangeEvent?>()
        val latch = CountDownLatch(1)
        scene.setSpaceChangedListener(
            Executor { it.run() },
            Consumer { event ->
                capturedEvent.set(event)
                latch.countDown()
            },
        )

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )
        assertThat(latch.await(EVENT_WAIT_SECONDS, TimeUnit.SECONDS)).isTrue()

        val event = checkNotNull(capturedEvent.get())
        assertThat(event.recommendedScale).isGreaterThan(0f)
        assertThat(event.recommendedScale.isFinite()).isTrue()
        assertVector3(event.recommendedPose.translation, Vector3.Zero, TRANSFORM_EPSILON)
        assertFiniteUnitQuaternion(event.recommendedPose.rotation)
        assertPose(mainPanel.getPose(Space.ACTIVITY), ROTATED_KEY_POSE, TRANSFORM_EPSILON)
        assertThat(mainPanel.getScale(Space.ACTIVITY)).isWithin(TRANSFORM_EPSILON).of(CUSTOM_SCALE)
    }

    @Test
    fun launchedActivityInFullSpace_inheritsPrecedingKeyEntityContinuity() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        val scene = sessionRule.session.scene
        val mainPanel = scene.mainPanelEntity
        assertThat(scene.keyEntity).isSameInstanceAs(mainPanel)
        mainPanel.setPose(RIGHT_PANEL_POSE)

        val shiftedNode =
            waitForMainPanelShiftedFromActivityOrigin(
                hostActivity = sessionRule.activity,
                minDistanceMeters = MIN_DRAG_DISTANCE_METERS,
            )
        val shiftedKeyWorldPos = shiftedNode.position

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(ComponentActivity::class.java.name, null, false)

        try {
            sessionRule.onMainThread {
                sessionRule.activity.startActivity(
                    Intent(sessionRule.activity, ComponentActivity::class.java)
                )
            }
            val activityB =
                instrumentation.waitForMonitorWithTimeout(monitor, ACTIVITY_LAUNCH_TIMEOUT_MS)
                    as? ComponentActivity
                    ?: error("Timed out waiting for second ComponentActivity launch")

            waitUntil("Second activity B reaches RESUMED state") {
                sessionRule.onMainThread {
                    activityB.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                }
            }

            val inheritedNode =
                checkNotNull(
                    waitForSpatialScene(
                        description = "Launched activity B window inherits keyEntity continuity",
                        timeout = SCENE_TIMEOUT,
                        select = { currentScene ->
                            val task = currentScene.findTaskByActivity(activityB)
                            val candidate = task?.let { resolveTaskWindowNode(it) }
                            if (
                                candidate != null &&
                                    (candidate.position - shiftedKeyWorldPos).length < 0.25f
                            ) {
                                candidate
                            } else {
                                null
                            }
                        },
                        until = { it != null },
                    )
                )
            assertWithMessage(
                    "Activity B window (${inheritedNode.position}) should shift away from " +
                        "baseline (${baselineNode.position}) to inherit A's keyEntity ($shiftedKeyWorldPos)"
                )
                .that((inheritedNode.position - baselineNode.position).length)
                .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
        } finally {
            instrumentation.removeMonitor(monitor)
            finishActivitiesOtherThan(sessionRule.activity)
        }
    }

    @Test
    fun unsafeExtremeKeyEntityPose_triggersSystemGuardrailFallback() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            expectedMainPanelVisible = true,
            timeout = SCENE_TIMEOUT,
        )

        val mainPanel = sessionRule.session.scene.mainPanelEntity
        mainPanel.setPose(UNSAFE_EXTREME_POSE)

        val extremeNode =
            waitForMainPanelShiftedFromActivityOrigin(
                hostActivity = sessionRule.activity,
                minDistanceMeters = 5.0f,
            )

        sessionRule.requestHomeSpace()
        val hsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should clamp/reject extreme keyEntity pose " +
                    "(${extremeNode.position}) instead of teleporting far outside FOV"
            )
            .that((hsmNode.position - baselineNode.position).length)
            .isLessThan(MAX_GUARDRAIL_DEVIATION_METERS)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should remain far from unsafe pose " +
                    "(${extremeNode.position})"
            )
            .that((hsmNode.position - extremeNode.position).length)
            .isGreaterThan(10.0f)
    }

    private fun createCustomPanel(): PanelEntity {
        return sessionRule.onMainThread {
            val view = View(sessionRule.activity).apply { setBackgroundColor(Color.BLUE) }
            PanelEntity.create(
                session = session,
                view = view,
                pixelDimensions = PANEL_PIXEL_SIZE,
                name = CUSTOM_PANEL_NAME,
                pose = RIGHT_PANEL_POSE,
                parent = session.scene.activitySpace,
            )
        }
    }

    private fun waitForMainPanelNode(hostActivity: Activity): SpatialNode {
        return checkNotNull(
            waitForSpatialScene(
                description = "Main panel task window node present and visible",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task =
                        scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                    resolveTaskWindowNode(task)
                },
                until = { it != null },
            )
        )
    }

    private fun waitForMainPanelShiftedFromActivityOrigin(
        hostActivity: Activity,
        minDistanceMeters: Float,
    ): SpatialNode {
        return checkNotNull(
            waitForSpatialScene(
                description =
                    "Main panel world node shifted at least ${minDistanceMeters}m from ActivitySpace origin",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task =
                        scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                    val originNode =
                        task.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                            ?: return@waitForSpatialScene null
                    val panelNode = resolveTaskWindowNode(task) ?: return@waitForSpatialScene null
                    if ((panelNode.position - originNode.position).length >= minDistanceMeters) {
                        panelNode
                    } else {
                        null
                    }
                },
                until = { it != null },
            )
        )
    }

    private fun waitForMovableMainPanelNode(hostActivity: Activity): SpatialNode {
        return checkNotNull(
            waitForSpatialScene(
                description = "Main panel node becomes movable in CPM",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task =
                        scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                    val node = resolveTaskWindowNode(task) ?: return@waitForSpatialScene null
                    if (node.isMovable) node else null
                },
                until = { it != null },
            )
        )
    }

    private fun waitForSeparatedPanelsInSingleSnapshot(
        hostActivity: Activity,
        customPanelName: String,
        minDistanceMeters: Float,
    ): Pair<SpatialNode, SpatialNode> {
        return checkNotNull(
            waitForSpatialScene(
                description = "Main panel and $customPanelName separated in CPM",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task =
                        scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                    val mainNode = resolveTaskWindowNode(task) ?: return@waitForSpatialScene null
                    val customNode =
                        task.embeddedWindowLeashes.firstOrNull {
                            it.isVisible &&
                                it.worldExtents != null &&
                                it.name.contains(customPanelName)
                        } ?: return@waitForSpatialScene null
                    if ((customNode.position - mainNode.position).length >= minDistanceMeters) {
                        Pair(mainNode, customNode)
                    } else {
                        null
                    }
                },
                until = { it != null },
            )
        )
    }

    private fun waitForGltfSubspaceParentNode(
        hostActivity: Activity,
        minOffsetFromActivityOriginMeters: Float,
    ): SpatialNode {
        return checkNotNull(
            waitForSpatialScene(
                description = "glTF entity CPM parent node settled at key offset",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task =
                        scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                    val originNode =
                        task.findNodeByName(ACTIVITY_SPACE_NODE_NAME)
                            ?: return@waitForSpatialScene null
                    task.activitySpaceRoot
                        .findNodes { node ->
                            node.isVisible &&
                                node.name != ACTIVITY_SPACE_NODE_NAME &&
                                node.children.any { child ->
                                    child.isVisible &&
                                        child.hasSubspace &&
                                        child.name.startsWith("gltf_entity_subspace_")
                                } &&
                                (node.position - originNode.position).length >=
                                    minOffsetFromActivityOriginMeters
                        }
                        .firstOrNull()
                },
                until = { it != null },
            )
        )
    }

    private fun waitForHomeSpaceTaskNode(hostActivity: Activity): SpatialNode {
        waitForSpatialScene(
            description = "Activity transitions into Home Space with visible task node",
            timeout = SCENE_TIMEOUT,
            select = { scene ->
                val task = scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                if (!task.isFullSpace && resolveTaskWindowNode(task) != null) task else null
            },
            until = { it != null },
        )
        val settled =
            waitForSettledSpatialTask(
                activity = hostActivity,
                requireFullSpace = false,
                expectedMainPanelVisible = true,
                timeout = SCENE_TIMEOUT,
            )
        return checkNotNull(resolveTaskWindowNode(settled))
    }

    private fun resolveTaskWindowNode(task: SpatialTask): SpatialNode? =
        task.taskWindowLeash?.takeIf { it.isVisible && it.worldExtents != null }
            ?: task.topLevelWindowLeashes.firstOrNull { it.isVisible && it.worldExtents != null }

    private fun assertFiniteUnitQuaternion(rotation: Quaternion) {
        assertThat(rotation.x.isFinite()).isTrue()
        assertThat(rotation.y.isFinite()).isTrue()
        assertThat(rotation.z.isFinite()).isTrue()
        assertThat(rotation.w.isFinite()).isTrue()
        val normSq =
            rotation.x * rotation.x +
                rotation.y * rotation.y +
                rotation.z * rotation.z +
                rotation.w * rotation.w
        assertWithMessage("Quaternion norm^2 ($normSq) must be close to 1")
            .that(normSq)
            .isWithin(TRANSFORM_EPSILON)
            .of(1f)
    }

    private fun finishActivitiesOtherThan(hostActivity: Activity) {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val stages =
            listOf(
                Stage.PRE_ON_CREATE,
                Stage.CREATED,
                Stage.STARTED,
                Stage.RESUMED,
                Stage.PAUSED,
                Stage.STOPPED,
                Stage.RESTARTED,
            )
        sessionRule.onMainThread {
            for (stage in stages) {
                for (candidate in monitor.getActivitiesInStage(stage)) {
                    if (
                        candidate !== hostActivity &&
                            !candidate.isFinishing &&
                            !candidate.isDestroyed
                    ) {
                        candidate.finish()
                    }
                }
            }
        }
    }

    private companion object {
        const val ACTIVITY_LAUNCH_TIMEOUT_MS = 5_000L
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val CUSTOM_PANEL_NAME = "continuity_custom_panel"
        const val EVENT_WAIT_SECONDS = 5L
        const val CUSTOM_SCALE = 1.35f
        const val TRANSFORM_EPSILON = 1e-3f
        const val MIN_DRAG_DISTANCE_METERS = 0.1f
        const val MIN_CONTINUITY_SHIFT_METERS = 0.15f
        const val MAX_BASELINE_RETURN_DRIFT_METERS = 0.15f
        const val MAX_GUARDRAIL_DEVIATION_METERS = 2.0f
        val SCENE_TIMEOUT = 5.seconds
        val PANEL_PIXEL_SIZE = IntSize2d(320, 240)
        val DRAG_DELTA = Vector3(0.22f, 0.08f, 0f)
        val LEFT_PANEL_POSE = Pose(Vector3(-0.12f, 0f, 0f), Quaternion.Identity)
        val RIGHT_PANEL_POSE = Pose(Vector3(0.52f, 0.10f, 0f), Quaternion.Identity)
        val CLEAR_PANEL_POSE = Pose(Vector3(0.45f, 0.30f, 0f), Quaternion.Identity)
        val GLTF_OFFSET_POSE = Pose(Vector3(0.45f, 0.12f, 0f), Quaternion.Identity)
        val ROTATED_KEY_POSE =
            Pose(Vector3(0.45f, 0.12f, 0f), Quaternion.fromAxisAngle(Vector3.Up, 20f))
        val UNSAFE_EXTREME_POSE = Pose(Vector3(0f, 0f, -50.0f), Quaternion.Identity)
    }
}
