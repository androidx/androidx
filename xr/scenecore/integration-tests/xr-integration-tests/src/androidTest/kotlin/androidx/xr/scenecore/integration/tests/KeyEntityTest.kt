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
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpaceChangeEvent
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitForSettledSpatialTask
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class KeyEntityTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @Test
    fun activityPanelKeyEntity_preservesContinuityAcrossHomeAndFullSpace() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(ComponentActivity::class.java.name, null, false)

        try {
            val activityPanel = sessionRule.onMainThread {
                val panel =
                    ActivityPanelEntity.create(
                        session = session,
                        pixelDimensions = ACTIVITY_PANEL_PIXEL_SIZE,
                        name = ACTIVITY_KEY_PANEL_NAME,
                        parent = session.scene.activitySpace,
                    )
                panel.startActivity(Intent(sessionRule.activity, ComponentActivity::class.java))
                panel
            }

            val captured =
                instrumentation.waitForMonitorWithTimeout(monitor, ACTIVITY_LAUNCH_TIMEOUT_MS)
                    as? ComponentActivity
                    ?: error("Timed out waiting for embedded ComponentActivity launch")

            waitUntil("Embedded ComponentActivity reaches RESUMED state") {
                sessionRule.onMainThread {
                    captured.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                }
            }

            sessionRule.session.scene.mainPanelEntity.setPose(LEFT_PANEL_POSE)
            activityPanel.setPose(RIGHT_PANEL_POSE)
            sessionRule.session.scene.keyEntity = activityPanel

            val settledFsm =
                checkNotNull(
                    waitForSpatialScene(
                        description = "Main panel and key ActivityPanel separated in CPM",
                        timeout = SCENE_TIMEOUT,
                        select = { scene ->
                            val task = scene.findTaskByActivity(sessionRule.activity)
                            val mainNode =
                                task?.let { resolveTaskWindowNode(it) }
                                    ?: return@waitForSpatialScene null
                            val embeddedNode =
                                task.findNode {
                                    it.isVisible &&
                                        it.worldExtents != null &&
                                        it.name.contains(ACTIVITY_KEY_PANEL_NAME)
                                } ?: return@waitForSpatialScene null
                            if (
                                (embeddedNode.position - mainNode.position).length >=
                                    MIN_SEPARATION_METERS
                            ) {
                                Pair(mainNode, embeddedNode)
                            } else {
                                null
                            }
                        },
                        until = { it != null },
                    )
                )
            val mainWorldPos = settledFsm.first.position
            val embeddedWorldPos = settledFsm.second.position

            sessionRule.requestHomeSpace()
            val hsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
            assertWithMessage(
                    "HSM task window (${hsmNode.position}) should shift from baseline " +
                        "(${baselineNode.position}) toward ActivityPanel keyEntity ($embeddedWorldPos)"
                )
                .that((hsmNode.position - baselineNode.position).length)
                .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
            assertWithMessage(
                    "HSM window (${hsmNode.position}) should stay closer to key ActivityPanel " +
                        "($embeddedWorldPos) than non-key main panel ($mainWorldPos)"
                )
                .that((hsmNode.position - embeddedWorldPos).length + 0.05f)
                .isLessThan((hsmNode.position - mainWorldPos).length)

            sessionRule.requestFullSpace()
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                requireFullSpace = true,
                timeout = SCENE_TIMEOUT,
            )

            waitUntil("ActivityPanel keyEntity moves to recommended Full Space origin") {
                activityPanel.getPose(Space.ACTIVITY).translation.length < TRANSFORM_EPSILON
            }
            assertPose(
                activityPanel.getPose(Space.ACTIVITY),
                Pose.Identity,
                epsilon = TRANSFORM_EPSILON,
            )
            assertVector3(
                sessionRule.session.scene.mainPanelEntity.getPose(Space.ACTIVITY).translation,
                LEFT_PANEL_POSE.translation,
                epsilon = TRANSFORM_EPSILON,
            )
            assertThat(captured.isDestroyed).isFalse()
        } finally {
            instrumentation.removeMonitor(monitor)
            finishActivitiesOtherThan(sessionRule.activity)
        }
    }

    @Test
    fun gltfModelKeyEntity_defaultSpaceChangedListener_appliesRecommendedTransformOnFullSpaceEntry() {
        val model = sessionRule.onMainThread {
            GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
        }
        val gltfEntity = sessionRule.onMainThread {
            GltfModelEntity.create(
                session = session,
                model = model,
                pose = CUSTOM_OFFSET_POSE,
                parent = session.scene.activitySpace,
            )
        }
        gltfEntity.setScale(CUSTOM_SCALE)
        sessionRule.session.scene.keyEntity = gltfEntity

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        waitUntil("Default SpaceChangeListener translates glTF keyEntity away from custom pose") {
            val pose = gltfEntity.getPose(Space.ACTIVITY)
            (pose.translation - CUSTOM_OFFSET_POSE.translation).length > MIN_TRANSFORM_DELTA
        }

        val event = captureCachedSpaceChangeEvent(gltfEntity)
        assertThat(event.recommendedScale).isGreaterThan(0f)
        assertVector3(event.recommendedPose.translation, Vector3.Zero, TRANSFORM_EPSILON)
        assertPose(gltfEntity.getPose(Space.ACTIVITY), event.recommendedPose, TRANSFORM_EPSILON)
        assertThat(gltfEntity.getScale(Space.ACTIVITY))
            .isWithin(TRANSFORM_EPSILON)
            .of(event.recommendedScale)
    }

    @Test
    fun defaultSpaceChangedListener_appliesRecommendedTransformOnFullSpaceEntry() {
        val panel = createConfiguredPanel(CUSTOM_OFFSET_POSE, CUSTOM_SCALE)
        sessionRule.session.scene.keyEntity = panel

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        waitUntil("Default SpaceChangeListener applies recommended transform on Full Space entry") {
            val pose = panel.getPose(Space.ACTIVITY)
            (pose.translation - CUSTOM_OFFSET_POSE.translation).length > MIN_TRANSFORM_DELTA
        }

        val event = captureCachedSpaceChangeEvent(panel)
        assertThat(event.recommendedScale).isGreaterThan(0f)
        assertVector3(event.recommendedPose.translation, Vector3.Zero, TRANSFORM_EPSILON)
        assertPose(panel.getPose(Space.ACTIVITY), event.recommendedPose, TRANSFORM_EPSILON)
        assertThat(panel.getScale(Space.ACTIVITY))
            .isWithin(TRANSFORM_EPSILON)
            .of(event.recommendedScale)
    }

    @Test
    fun keyEntity_setFromNullToNonNullWithCachedEvent_appliesRecommendedTransform() {
        sessionRule.session.scene.keyEntity = null
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        val panel1 = createConfiguredPanel(CUSTOM_OFFSET_POSE, CUSTOM_SCALE)
        val panel2 = createConfiguredPanel(SECOND_OFFSET_POSE, scale = 1.0f)

        assertPose(panel1.getPose(Space.ACTIVITY), CUSTOM_OFFSET_POSE, TRANSFORM_EPSILON)

        sessionRule.session.scene.keyEntity = panel1
        waitUntil("Cached SpaceChangeEvent applies on null-to-non-null keyEntity transition") {
            (panel1.getPose(Space.ACTIVITY).translation - CUSTOM_OFFSET_POSE.translation).length >
                MIN_TRANSFORM_DELTA
        }

        val event = captureCachedSpaceChangeEvent(panel1)
        assertPose(panel1.getPose(Space.ACTIVITY), event.recommendedPose, TRANSFORM_EPSILON)
        assertThat(panel1.getScale(Space.ACTIVITY))
            .isWithin(TRANSFORM_EPSILON)
            .of(event.recommendedScale)

        sessionRule.session.scene.keyEntity = panel2
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertPose(panel2.getPose(Space.ACTIVITY), SECOND_OFFSET_POSE, TRANSFORM_EPSILON)
        assertThat(panel2.getScale(Space.ACTIVITY)).isWithin(TRANSFORM_EPSILON).of(1.0f)
    }

    @Test
    fun setSpaceChangedListener_overridesDefaultBehavior_andClearRestoresDefault() {
        val panel = createConfiguredPanel(CUSTOM_OFFSET_POSE, CUSTOM_SCALE)
        val scene = sessionRule.session.scene
        scene.keyEntity = panel

        val listenerCount = AtomicInteger(0)
        val lastEvent = AtomicReference<SpaceChangeEvent?>()
        val firstEventLatch = CountDownLatch(1)
        scene.setSpaceChangedListener(
            Executor { it.run() },
            Consumer { event ->
                listenerCount.incrementAndGet()
                lastEvent.set(event)
                firstEventLatch.countDown()
            },
        )

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )
        assertThat(firstEventLatch.await(EVENT_WAIT_SECONDS, TimeUnit.SECONDS)).isTrue()

        val receivedEvent = checkNotNull(lastEvent.get())
        assertThat(listenerCount.get()).isEqualTo(1)
        assertThat(receivedEvent.recommendedScale).isGreaterThan(0f)
        assertVector3(receivedEvent.recommendedPose.translation, Vector3.Zero, TRANSFORM_EPSILON)
        assertPose(panel.getPose(Space.ACTIVITY), CUSTOM_OFFSET_POSE, TRANSFORM_EPSILON)
        assertThat(panel.getScale(Space.ACTIVITY)).isWithin(TRANSFORM_EPSILON).of(CUSTOM_SCALE)

        scene.clearSpaceChangedListener()
        scene.keyEntity = null
        scene.keyEntity = panel

        assertThat(listenerCount.get()).isEqualTo(1)
        waitUntil("Restored default listener applies recommended transform to keyEntity") {
            (panel.getPose(Space.ACTIVITY).translation - receivedEvent.recommendedPose.translation)
                .length < TRANSFORM_EPSILON &&
                kotlin.math.abs(panel.getScale(Space.ACTIVITY) - receivedEvent.recommendedScale) <
                    TRANSFORM_EPSILON
        }
    }

    @Test
    fun nestedParentMove_propagatesChildKeyEntityHintAcrossTransitions() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        val scene = sessionRule.session.scene
        scene.mainPanelEntity.setPose(LEFT_PANEL_POSE)

        val container =
            Entity.create(
                session = sessionRule.session,
                name = "key_container",
                pose = Pose.Identity,
                parent = scene.activitySpace,
            )
        val childPanel = sessionRule.onMainThread {
            val content = View(sessionRule.activity).apply { setBackgroundColor(Color.GREEN) }
            PanelEntity.create(
                session = sessionRule.session,
                view = content,
                pixelDimensions = PANEL_PIXEL_SIZE,
                name = CHILD_KEY_PANEL_NAME,
                pose = Pose.Identity,
                parent = container,
            )
        }
        scene.keyEntity = childPanel

        val initialPair =
            waitForNamedEmbeddedAndMainPanel(
                hostActivity = sessionRule.activity,
                embeddedName = CHILD_KEY_PANEL_NAME,
            )

        // Move ONLY the ancestor container Entity; childPanel stays at local Pose.Identity.
        container.setPose(RIGHT_PANEL_POSE)

        val shiftedPair =
            checkNotNull(
                waitForSpatialScene(
                    description = "Parent Entity move propagates to child keyEntity CPM node",
                    timeout = SCENE_TIMEOUT,
                    select = { currentScene ->
                        val task = currentScene.findTaskByActivity(sessionRule.activity)
                        val mainNode =
                            task?.let { resolveTaskWindowNode(it) }
                                ?: return@waitForSpatialScene null
                        val childNode =
                            task.embeddedWindowLeashes.firstOrNull {
                                it.isVisible &&
                                    it.worldExtents != null &&
                                    it.name.contains(CHILD_KEY_PANEL_NAME)
                            } ?: return@waitForSpatialScene null
                        val movedDelta = (childNode.position - initialPair.second.position).length
                        if (
                            movedDelta >= MIN_CONTINUITY_SHIFT_METERS &&
                                (childNode.position - mainNode.position).length >=
                                    MIN_SEPARATION_METERS
                        ) {
                            Pair(mainNode, childNode)
                        } else {
                            null
                        }
                    },
                    until = { it != null },
                )
            )
        val mainWorldPos = shiftedPair.first.position
        val childWorldPos = shiftedPair.second.position

        sessionRule.requestHomeSpace()
        val hsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "HSM task window (${hsmNode.position}) should shift from baseline " +
                    "(${baselineNode.position}) to moved child keyEntity ($childWorldPos)"
            )
            .that((hsmNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should track moved child keyEntity " +
                    "($childWorldPos) over main panel ($mainWorldPos)"
            )
            .that((hsmNode.position - childWorldPos).length + 0.05f)
            .isLessThan((hsmNode.position - mainWorldPos).length)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )
        waitUntil("Nested keyEntity resets child panel to Space.ACTIVITY origin") {
            childPanel.getPose(Space.ACTIVITY).translation.length < TRANSFORM_EPSILON
        }
    }

    @Test
    fun safeReassignKeyEntityBeforeDetach_usesFallbackAcrossTransitions() {
        val baselineNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )

        val scene = sessionRule.session.scene
        scene.mainPanelEntity.setPose(Pose(Vector3(0f, -0.35f, 0f), Quaternion.Identity))

        val oldPanel = createNamedPanel(OLD_KEY_PANEL_NAME, LEFT_PANEL_POSE)
        val fallbackPanel = createNamedPanel(FALLBACK_KEY_PANEL_NAME, RIGHT_PANEL_POSE)

        scene.keyEntity = oldPanel
        waitForNamedEmbeddedAndMainPanel(sessionRule.activity, OLD_KEY_PANEL_NAME)

        // Safely reassign keyEntity BEFORE detaching oldPanel so old transform listeners detach
        // cleanly without leaving a dangling active keyEntity reference.
        scene.keyEntity = fallbackPanel
        oldPanel.parent = null

        val fallbackPair =
            checkNotNull(
                waitForSpatialScene(
                    description = "Old keyEntity removed and fallback keyEntity settled in CPM",
                    timeout = SCENE_TIMEOUT,
                    select = { currentScene ->
                        val task = currentScene.findTaskByActivity(sessionRule.activity)
                        val mainNode =
                            task?.let { resolveTaskWindowNode(it) }
                                ?: return@waitForSpatialScene null
                        val oldStillPresent =
                            task.embeddedWindowLeashes.any {
                                it.isVisible && it.name.contains(OLD_KEY_PANEL_NAME)
                            }
                        val fallbackNode =
                            task.embeddedWindowLeashes.firstOrNull {
                                it.isVisible &&
                                    it.worldExtents != null &&
                                    it.name.contains(FALLBACK_KEY_PANEL_NAME)
                            }
                        if (!oldStillPresent && fallbackNode != null) {
                            Pair(mainNode, fallbackNode)
                        } else {
                            null
                        }
                    },
                    until = { it != null },
                )
            )
        val mainWorldPos = fallbackPair.first.position
        val fallbackWorldPos = fallbackPair.second.position

        sessionRule.requestHomeSpace()
        val hsmNode = waitForHomeSpaceTaskNode(sessionRule.activity)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should track fallback keyEntity " +
                    "($fallbackWorldPos) away from baseline (${baselineNode.position})"
            )
            .that((hsmNode.position - baselineNode.position).length)
            .isGreaterThan(MIN_CONTINUITY_SHIFT_METERS)
        assertWithMessage(
                "HSM window (${hsmNode.position}) should remain closer to fallback keyEntity " +
                    "($fallbackWorldPos) than main panel ($mainWorldPos)"
            )
            .that((hsmNode.position - fallbackWorldPos).length + 0.05f)
            .isLessThan((hsmNode.position - mainWorldPos).length)

        sessionRule.requestFullSpace()
        waitForSettledSpatialTask(
            activity = sessionRule.activity,
            requireFullSpace = true,
            timeout = SCENE_TIMEOUT,
        )
        waitUntil("Fallback keyEntity receives recommended Full Space pose without errors") {
            fallbackPanel.getPose(Space.ACTIVITY).translation.length < TRANSFORM_EPSILON
        }
    }

    private fun createConfiguredPanel(pose: Pose, scale: Float): PanelEntity {
        val panel = createNamedPanel("key_entity_test_panel", pose)
        panel.setScale(scale)
        return panel
    }

    private fun createNamedPanel(name: String, pose: Pose): PanelEntity {
        return sessionRule.onMainThread {
            val view = View(sessionRule.activity).apply { setBackgroundColor(Color.BLUE) }
            PanelEntity.create(
                session = session,
                view = view,
                pixelDimensions = PANEL_PIXEL_SIZE,
                name = name,
                pose = pose,
                parent = session.scene.activitySpace,
            )
        }
    }

    private fun captureCachedSpaceChangeEvent(targetEntity: Entity): SpaceChangeEvent {
        val capturedEvent = AtomicReference<SpaceChangeEvent?>()
        val latch = CountDownLatch(1)
        val scene = sessionRule.session.scene
        scene.setSpaceChangedListener(
            Executor { it.run() },
            Consumer { event ->
                capturedEvent.set(event)
                latch.countDown()
            },
        )
        return try {
            scene.keyEntity = null
            scene.keyEntity = targetEntity
            assertWithMessage("Timed out waiting for cached SpaceChangeEvent replay")
                .that(latch.await(EVENT_WAIT_SECONDS, TimeUnit.SECONDS))
                .isTrue()
            checkNotNull(capturedEvent.get())
        } finally {
            scene.clearSpaceChangedListener()
        }
    }

    private fun waitForNamedEmbeddedAndMainPanel(
        hostActivity: Activity,
        embeddedName: String,
    ): Pair<SpatialNode, SpatialNode> {
        return checkNotNull(
            waitForSpatialScene(
                description = "Main panel and $embeddedName settled in CPM",
                timeout = SCENE_TIMEOUT,
                select = { scene ->
                    val task =
                        scene.findTaskByActivity(hostActivity) ?: return@waitForSpatialScene null
                    val mainNode = resolveTaskWindowNode(task) ?: return@waitForSpatialScene null
                    val embeddedNode =
                        task.embeddedWindowLeashes.firstOrNull {
                            it.isVisible &&
                                it.worldExtents != null &&
                                it.name.contains(embeddedName)
                        } ?: return@waitForSpatialScene null
                    Pair(mainNode, embeddedNode)
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
        const val EVENT_WAIT_SECONDS = 5L
        const val ACTIVITY_KEY_PANEL_NAME = "key_activity_panel"
        const val CHILD_KEY_PANEL_NAME = "child_key_panel"
        const val OLD_KEY_PANEL_NAME = "old_key_panel"
        const val FALLBACK_KEY_PANEL_NAME = "fallback_key_panel"
        const val CUSTOM_SCALE = 2.37f
        const val MIN_TRANSFORM_DELTA = 0.05f
        const val MIN_CONTINUITY_SHIFT_METERS = 0.15f
        const val MIN_SEPARATION_METERS = 0.25f
        const val TRANSFORM_EPSILON = 1e-3f
        val SCENE_TIMEOUT = 5.seconds
        val PANEL_PIXEL_SIZE = IntSize2d(320, 240)
        val ACTIVITY_PANEL_PIXEL_SIZE = IntSize2d(480, 360)
        val CUSTOM_OFFSET_POSE =
            Pose(Vector3(0.45f, 0.2f, -0.35f), Quaternion.fromAxisAngle(Vector3.Up, 20f))
        val SECOND_OFFSET_POSE = Pose(Vector3(-0.55f, 0.1f, -0.25f), Quaternion.Identity)
        val LEFT_PANEL_POSE = Pose(Vector3(-0.35f, 0f, 0f), Quaternion.Identity)
        val RIGHT_PANEL_POSE = Pose(Vector3(0.55f, 0.12f, 0f), Quaternion.Identity)
    }
}
