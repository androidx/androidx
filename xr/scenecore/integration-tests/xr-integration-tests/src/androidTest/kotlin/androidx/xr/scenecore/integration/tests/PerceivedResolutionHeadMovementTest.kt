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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.MainPanelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.FakeHeadTrackingRule
import androidx.xr.testutils.XrDeviceConfig
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitForSettledSpatialTask
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device integration tests for [MainPanelEntity.addPerceivedResolutionChangedListener],
 * [PanelEntity.getPerceivedResolution], and [SurfaceEntity.getPerceivedResolution] under simulated
 * 6DoF head translation and rotation via [FakeHeadTrackingRule].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class PerceivedResolutionHeadMovementTest {

    @get:Rule(order = 0) val fakeHeadTrackingRule = FakeHeadTrackingRule()

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions(HEAD_TRACKING_PERMISSION))

    @get:Rule(order = 2) val sessionRule = createXrSessionRule()

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    // Method-level @XrDeviceTest: XrDeviceFilter only reads include from it; emulator excluded per
    // b/434252357.
    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun mainPanelEntity_inHomeSpaceMode_headMovementScalesPerceivedResolutionWithDistance() {
        val recorder = PerceivedResolutionRecorder()
        val mainPanel = sessionRule.session.scene.mainPanelEntity

        // Adding a listener does not replay the last value and SpaceFlinger only emits on change,
        // so derive a deterministic Home Space baseline: hide the main panel in Full Space Mode
        // ((0, 0)), then request Home Space Mode and show it again (non-zero).
        configureSpatialDeviceTracking(sessionRule.session)
        mainPanel.addPerceivedResolutionChangedListener(recorder)
        mainPanel.setEnabled(false)
        recorder.awaitResolution("zero resolution in Full Space Mode") { size ->
            size.width == 0 && size.height == 0
        }
        val markBeforeHomeSpace = recorder.mark()
        sessionRule.requestHomeSpace()
        mainPanel.setEnabled(true)

        // Wait for the settled Home Space task before reading windowWorldPosition or initialHsmSize
        // so neither reflects an in-flight transition.
        val windowWorldPosition = locateHomeSpaceWindowWorldPosition()
        val initialHsmSize =
            recorder.awaitResolution(
                description = "initial non-zero resolution in Home Space Mode",
                sinceMark = markBeforeHomeSpace,
            ) { size ->
                size.width > 0 && size.height > 0
            }
        val awayFromWindowDirection = (Vector3.Zero - windowWorldPosition).toNormalized()

        // Step far enough back along awayFromWindowDirection to exceed SpaceFlinger's 100px
        // listener update threshold.
        val markBeforeStepBack = recorder.mark()
        fakeHeadTrackingRule.performHeadMovement {
            moveHeadTo(awayFromWindowDirection * HSM_HEAD_STEP_BACK_M)
        }
        recorder.awaitResolution(
            description = "smaller Home Space resolution after stepping head back",
            sinceMark = markBeforeStepBack,
        ) {
            it.width > 0 &&
                it.height > 0 &&
                it.width < initialHsmSize.width &&
                it.height < initialHsmSize.height
        }

        val closeDistanceM = windowWorldPosition.length * HSM_CLOSE_DISTANCE_FRACTION
        val markBeforeStepClose = recorder.mark()
        fakeHeadTrackingRule.performHeadMovement {
            moveHeadTo(windowWorldPosition + (awayFromWindowDirection * closeDistanceM))
        }
        val nearHsmSize =
            recorder.awaitResolution(
                description = "larger Home Space resolution after stepping head closer",
                sinceMark = markBeforeStepClose,
            ) {
                it.width > initialHsmSize.width && it.height > initialHsmSize.height
            }

        val markBeforeReset = recorder.mark()
        fakeHeadTrackingRule.resetHeadMovement()
        recorder.awaitResolution(
            description = "Home Space resolution returning below nearHsmSize at origin",
            sinceMark = markBeforeReset,
        ) {
            it.width > 0 &&
                it.height > 0 &&
                it.width < nearHsmSize.width &&
                it.height < nearHsmSize.height
        }
    }

    @Test
    fun fullSpaceEntities_headRotation180Degrees_disregardsViewingDirection() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val baselinePose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "initial RenderViewpoint pose to settle",
            ) {
                true
            }
        val initialForward = baselinePose.forward.toNormalized()

        val stationaryPanel = createStationaryPanel(renderViewpoint)
        val stationarySurface = sessionRule.createQuadSurfaceEntity(renderViewpoint)
        val baseline =
            captureThreeEntitySnapshot(
                renderViewpoint = renderViewpoint,
                panel = stationaryPanel,
                surface = stationarySurface,
                expectedPose = baselinePose,
            )

        fakeHeadTrackingRule.performHeadMovement {
            rotateHeadTo(Quaternion.fromAxisAngle(Vector3.Up, LOOK_AWAY_YAW_DEG))
        }
        val settledLookingAwayPose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "RenderViewpoint to settle at 180-degree head rotation",
            ) { pose ->
                pose.forward.toNormalized().dot(initialForward) < LOOK_AWAY_DOT_THRESHOLD
            }

        val lookingAway =
            captureThreeEntitySnapshot(
                renderViewpoint = renderViewpoint,
                panel = stationaryPanel,
                surface = stationarySurface,
                expectedPose = settledLookingAwayPose,
            )
        assertThreeEntityDistanceRatios(actual = lookingAway, base = baseline)
    }

    @Test
    fun fullSpaceEntities_headLocomotionBackwardAndForward_scalesPerceivedResolutionProportionally() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val session = sessionRule.session
        val baselinePose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "initial RenderViewpoint pose to settle",
            ) {
                true
            }

        // Place mainPanelEntity explicitly at MEDIUM_DISTANCE_M rather than relying on the OS
        // spawn distance across device configs so stepping forward stays clear of the near plane.
        session.scene.mainPanelEntity.setPose(
            poseInFrontOfCamera(session, renderViewpoint, MEDIUM_DISTANCE_M)
        )
        val stationaryPanel = createStationaryPanel(renderViewpoint)
        val stationarySurface = sessionRule.createQuadSurfaceEntity(renderViewpoint)
        val baseline =
            captureThreeEntitySnapshot(
                renderViewpoint = renderViewpoint,
                panel = stationaryPanel,
                surface = stationarySurface,
                expectedPose = baselinePose,
            )

        val expectedSteppedBackTranslation =
            baselinePose.translation + Vector3(0f, 0f, FSM_HEAD_STEP_BACK_M)
        fakeHeadTrackingRule.performHeadMovement {
            moveHeadTo(Vector3(0f, 0f, FSM_HEAD_STEP_BACK_M))
        }
        val steppedBackPose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "RenderViewpoint to settle ${FSM_HEAD_STEP_BACK_M}m back",
            ) { pose ->
                Vector3.distance(pose.translation, expectedSteppedBackTranslation) <=
                    TARGET_DISTANCE_TOLERANCE_M
            }

        val afterSteppingBack =
            captureThreeEntitySnapshot(
                renderViewpoint = renderViewpoint,
                panel = stationaryPanel,
                surface = stationarySurface,
                expectedPose = steppedBackPose,
            )
        assertThreeEntityDistanceRatios(actual = afterSteppingBack, base = baseline)

        val expectedSteppedForwardTranslation =
            baselinePose.translation + Vector3(0f, 0f, -FSM_HEAD_STEP_FORWARD_M)
        fakeHeadTrackingRule.performHeadMovement {
            moveHeadTo(Vector3(0f, 0f, -FSM_HEAD_STEP_FORWARD_M))
        }
        val steppedForwardPose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "RenderViewpoint to settle ${FSM_HEAD_STEP_FORWARD_M}m forward",
            ) { pose ->
                Vector3.distance(pose.translation, expectedSteppedForwardTranslation) <=
                    TARGET_DISTANCE_TOLERANCE_M
            }

        val afterSteppingCloser =
            captureThreeEntitySnapshot(
                renderViewpoint = renderViewpoint,
                panel = stationaryPanel,
                surface = stationarySurface,
                expectedPose = steppedForwardPose,
            )
        assertThreeEntityDistanceRatios(actual = afterSteppingCloser, base = baseline)
    }

    @Test
    fun surfaceEntity_stationarySurroundingSphere_headWalkOutTransitionsTooCloseToSuccessAndHemisphereRatio() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val baselinePose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "initial RenderViewpoint pose to settle",
            ) {
                true
            }

        val stationarySurface = sessionRule.createQuadSurfaceEntity(renderViewpoint)
        sessionRule.onMainThread {
            stationarySurface.shape = SurfaceEntity.Shape.Sphere(SURFACE_SPHERE_RADIUS_M)
        }

        val expectedInsideTranslation =
            baselinePose.translation + Vector3(0f, 0f, -FSM_HEAD_STEP_FORWARD_M)
        fakeHeadTrackingRule.performHeadMovement {
            poseHeadBy(Pose(Vector3(0f, 0f, -FSM_HEAD_STEP_FORWARD_M)))
        }
        awaitSettledRenderViewpointPose(
            renderViewpoint = renderViewpoint,
            description = "RenderViewpoint to settle inside the 1.0m stationary sphere",
        ) { pose ->
            Vector3.distance(pose.translation, expectedInsideTranslation) <=
                TARGET_DISTANCE_TOLERANCE_M
        }
        assertThat(stationarySurface.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)

        val netExitedOffsetZ = FSM_SPHERE_EXIT_STEP_BACK_M - FSM_HEAD_STEP_FORWARD_M
        val expectedExitedTranslation = baselinePose.translation + Vector3(0f, 0f, netExitedOffsetZ)
        fakeHeadTrackingRule.performHeadMovement {
            poseHeadBy(Pose(Vector3(0f, 0f, FSM_SPHERE_EXIT_STEP_BACK_M)))
        }
        val exitedPose =
            awaitSettledRenderViewpointPose(
                renderViewpoint = renderViewpoint,
                description = "RenderViewpoint to settle outside the stationary sphere",
            ) { pose ->
                Vector3.distance(pose.translation, expectedExitedTranslation) <=
                    TARGET_DISTANCE_TOLERANCE_M
            }

        val exitedSphereResolution =
            stationarySurface.getPerceivedResolution(renderViewpoint).assertSuccess()
        sessionRule.onMainThread {
            stationarySurface.shape = SurfaceEntity.Shape.Hemisphere(SURFACE_SPHERE_RADIUS_M)
        }
        val exitedHemisphereResolution =
            stationarySurface.getPerceivedResolution(renderViewpoint).assertSuccess()
        val distanceFromCenter = distanceToCameraPose(exitedPose, stationarySurface)
        assertRenderViewpointPoseUnchanged(exitedPose, renderViewpoint.state.value.pose)

        val expectedHemisphereRatio =
            (distanceFromCenter - SURFACE_SPHERE_RADIUS_M) /
                (distanceFromCenter - (SURFACE_SPHERE_RADIUS_M / 2.0f))
        assertScaledBy(
            actual = exitedHemisphereResolution,
            base = exitedSphereResolution,
            expectedRatio = expectedHemisphereRatio,
        )
    }

    private fun awaitSettledRenderViewpointPose(
        renderViewpoint: RenderViewpoint,
        description: String,
        targetReached: (Pose) -> Boolean,
    ): Pose {
        var previousPose: Pose? = null
        var settledPose: Pose? = null
        waitUntil(description = description, timeout = CALLBACK_TIMEOUT) {
            val currentPose = renderViewpoint.state.value.pose
            val previous = previousPose
            previousPose = currentPose
            val isStable = previous != null && posesMatch(previous, currentPose)
            if (isStable && targetReached(currentPose)) {
                settledPose = currentPose
                true
            } else {
                false
            }
        }
        return checkNotNull(settledPose)
    }

    private fun posesMatch(first: Pose, second: Pose): Boolean =
        Vector3.distance(first.translation, second.translation) <= SETTLED_POSITION_TOLERANCE_M &&
            abs(first.rotation.dot(second.rotation)) >= SETTLED_ROTATION_DOT_THRESHOLD

    private fun assertRenderViewpointPoseUnchanged(expected: Pose, actual: Pose) {
        assertWithMessage(
                "RenderViewpoint pose shifted mid-measurement: expected=$expected actual=$actual"
            )
            .that(posesMatch(expected, actual))
            .isTrue()
    }

    private fun distanceToCameraPose(renderPose: Pose, entity: Entity): Float =
        Vector3.distance(
            sessionRule.session.scene.perceptionSpace
                .getScenePoseFromPerceptionPose(renderPose)
                .poseInActivitySpace
                .translation,
            entity.getPose(Space.ACTIVITY).translation,
        )

    private fun createStationaryPanel(renderViewpoint: RenderViewpoint): PanelEntity =
        sessionRule.createWhitePanelEntity(
            name = "stationaryHeadTrackingPanel",
            pose = poseInFrontOfCamera(sessionRule.session, renderViewpoint, MEDIUM_DISTANCE_M),
        )

    private fun captureThreeEntitySnapshot(
        renderViewpoint: RenderViewpoint,
        panel: PanelEntity,
        surface: SurfaceEntity,
        expectedPose: Pose,
    ): ThreeEntitySnapshot {
        val mainPanel = sessionRule.session.scene.mainPanelEntity
        val snapshot =
            ThreeEntitySnapshot(
                mainPanel = captureEntitySnapshot(renderViewpoint, mainPanel, expectedPose),
                panel = captureEntitySnapshot(renderViewpoint, panel, expectedPose),
                surface = captureEntitySnapshot(renderViewpoint, surface, expectedPose),
            )
        assertRenderViewpointPoseUnchanged(expectedPose, renderViewpoint.state.value.pose)
        return snapshot
    }

    private fun captureEntitySnapshot(
        renderViewpoint: RenderViewpoint,
        entity: Entity,
        renderPose: Pose,
    ): EntityDistanceAndResolution {
        val resolution =
            when (entity) {
                is PanelEntity -> entity.getPerceivedResolution(renderViewpoint).assertSuccess()
                is SurfaceEntity -> entity.getPerceivedResolution(renderViewpoint).assertSuccess()
                else -> error("Unsupported entity type: ${entity::class.java.simpleName}")
            }
        return EntityDistanceAndResolution(
            distanceMeters = distanceToCameraPose(renderPose, entity),
            resolution = resolution,
        )
    }

    private fun assertThreeEntityDistanceRatios(
        actual: ThreeEntitySnapshot,
        base: ThreeEntitySnapshot,
    ) {
        assertScaledBy(
            actual = actual.mainPanel.resolution,
            base = base.mainPanel.resolution,
            expectedRatio = base.mainPanel.distanceMeters / actual.mainPanel.distanceMeters,
        )
        assertScaledBy(
            actual = actual.panel.resolution,
            base = base.panel.resolution,
            expectedRatio = base.panel.distanceMeters / actual.panel.distanceMeters,
        )
        assertScaledBy(
            actual = actual.surface.resolution,
            base = base.surface.resolution,
            expectedRatio = base.surface.distanceMeters / actual.surface.distanceMeters,
        )
    }

    private fun locateHomeSpaceWindowWorldPosition(): Vector3 {
        val settledTask =
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                expectedMainPanelVisible = true,
                requireFullSpace = false,
                timeout = CALLBACK_TIMEOUT,
            )
        val windowWorldPosition =
            checkNotNull(settledTask.taskWindowLeash?.position) {
                "Expected Home Space taskWindowLeash to have a world position"
            }
        assertWithMessage(
                "Expected Home Space taskWindowLeash away from head origin: $windowWorldPosition"
            )
            .that(windowWorldPosition.length)
            .isGreaterThan(MIN_HSM_WINDOW_DISTANCE_M)
        return windowWorldPosition
    }

    private data class ThreeEntitySnapshot(
        val mainPanel: EntityDistanceAndResolution,
        val panel: EntityDistanceAndResolution,
        val surface: EntityDistanceAndResolution,
    )

    private companion object {
        const val LOOK_AWAY_YAW_DEG = 180f
        const val LOOK_AWAY_DOT_THRESHOLD = -0.99f
        const val TARGET_DISTANCE_TOLERANCE_M = 0.05f
        const val SETTLED_POSITION_TOLERANCE_M = 1e-3f
        const val SETTLED_ROTATION_DOT_THRESHOLD = 0.9999f
        const val HSM_HEAD_STEP_BACK_M = 1.5f
        const val HSM_CLOSE_DISTANCE_FRACTION = 0.5f
        const val MIN_HSM_WINDOW_DISTANCE_M = 0.8f
        const val FSM_HEAD_STEP_BACK_M = 1.0f
        const val FSM_HEAD_STEP_FORWARD_M = 0.75f
        const val FSM_SPHERE_EXIT_STEP_BACK_M = 2.25f
    }
}
