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

import android.graphics.Color
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.MainPanelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.SpatialInteractionScope
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.XrDeviceConfig
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device integration tests for [MainPanelEntity.addPerceivedResolutionChangedListener],
 * [MainPanelEntity.removePerceivedResolutionChangedListener], [PanelEntity.getPerceivedResolution],
 * and [SurfaceEntity.getPerceivedResolution].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class PerceivedResolutionTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions(HEAD_TRACKING_PERMISSION))

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun addPerceivedResolutionChangedListener_whenDeviceTrackingDisabled_throwsIllegalStateException() {
        val session = sessionRule.session
        assertThat(session.config.deviceTracking).isEqualTo(DeviceTrackingMode.DISABLED)

        assertThrows(IllegalStateException::class.java) {
            session.scene.mainPanelEntity.addPerceivedResolutionChangedListener {}
        }
    }

    // Method-level @XrDeviceTest: XrDeviceFilter only reads include from it; emulator excluded per
    // b/434252357.
    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun mainPanelEntity_perceivedResolutionListener_reportsZeroInFullSpaceThenNonZeroInHomeSpace() {
        val recorder = PerceivedResolutionRecorder()
        val mainPanel = sessionRule.session.scene.mainPanelEntity

        // Adding a listener does not replay the last value and SpaceFlinger only emits on change,
        // so hiding the main panel creates a deterministic SpatialVisibility transition.
        configureSpatialDeviceTracking(sessionRule.session)
        mainPanel.addPerceivedResolutionChangedListener(recorder)
        mainPanel.setEnabled(false)
        recorder.awaitResolution("zero resolution in Full Space Mode") { size ->
            size.width == 0 && size.height == 0
        }

        val markBeforeHomeSpace = recorder.mark()
        sessionRule.requestHomeSpace()
        mainPanel.setEnabled(true)
        recorder.awaitResolution(
            description = "non-zero resolution after requestHomeSpace",
            sinceMark = markBeforeHomeSpace,
        ) { size ->
            size.width > 0 && size.height > 0
        }
    }

    // Method-level @XrDeviceTest: XrDeviceFilter only reads include from it; emulator excluded per
    // b/434252357.
    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun mainPanelEntity_removePerceivedResolutionChangedListener_stopsDeliveringCallbacks() {
        val removedRecorder = PerceivedResolutionRecorder()
        val activeRecorder = PerceivedResolutionRecorder()
        val mainPanel = sessionRule.session.scene.mainPanelEntity

        configureSpatialDeviceTracking(sessionRule.session)
        mainPanel.addPerceivedResolutionChangedListener(removedRecorder)
        mainPanel.addPerceivedResolutionChangedListener(activeRecorder)
        mainPanel.setEnabled(false)
        // Confirm removedRecorder receives callbacks before unregistering so later silence is
        // meaningful.
        removedRecorder.awaitResolution("zero resolution before removal") { size ->
            size.width == 0 && size.height == 0
        }

        // Unregister removedRecorder and drain the main Looper before taking markAfterRemoval so
        // pre-removal in-flight callbacks are not miscounted.
        mainPanel.removePerceivedResolutionChangedListener(removedRecorder)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val markAfterRemoval = removedRecorder.mark()
        val activeMark = activeRecorder.mark()

        sessionRule.requestHomeSpace()
        mainPanel.setEnabled(true)
        activeRecorder.awaitResolution(
            description = "non-zero Home Space resolution on activeRecorder",
            sinceMark = activeMark,
        ) { size ->
            size.width > 0 && size.height > 0
        }
        // activeRecorder's callback is the arrival anchor; wait one settle window to catch late
        // deliveries on removedRecorder without sleeping unconditionally.
        assertWithMessage("Unexpected callback delivered after listener removal")
            .that(removedRecorder.awaitEventSince(markAfterRemoval, NO_CALLBACK_SETTLE_TIMEOUT))
            .isFalse()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertThat(removedRecorder.eventsSince(markAfterRemoval)).isEmpty()
    }

    @Test
    fun mainPanelEntityAndPanelEntity_inFullSpaceMode_scalesWithDistanceTooCloseAndRotationInvariance() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val session = sessionRule.session

        session.scene.mainPanelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()

        val mediumPose = poseInFrontOfCamera(session, renderViewpoint, MEDIUM_DISTANCE_M)
        val panelEntity =
            sessionRule.createWhitePanelEntity(
                name = "perceivedResolutionDistancePanel",
                pose = mediumPose,
            )
        val mediumResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()

        val rotatedInPlacePose =
            Pose(
                mediumPose.translation,
                mediumPose.rotation *
                    Quaternion.fromAxisAngle(Vector3.Up, ENTITY_YAW_ROTATION_DEG) *
                    Quaternion.fromAxisAngle(Vector3.Right, ENTITY_PITCH_ROTATION_DEG),
            )
        panelEntity.setPose(rotatedInPlacePose)
        val rotatedResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(actual = rotatedResolution, base = mediumResolution, expectedRatio = 1.0f)

        panelEntity.setPose(poseInFrontOfCamera(session, renderViewpoint, FAR_DISTANCE_M))
        val farResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(
            actual = farResolution,
            base = mediumResolution,
            expectedRatio = MEDIUM_DISTANCE_M / FAR_DISTANCE_M,
        )

        panelEntity.setPose(poseInFrontOfCamera(session, renderViewpoint, NEAR_DISTANCE_M))
        val nearResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(
            actual = nearResolution,
            base = mediumResolution,
            expectedRatio = MEDIUM_DISTANCE_M / NEAR_DISTANCE_M,
        )

        panelEntity.setPose(cameraPoseInActivitySpace(session, renderViewpoint))
        assertThat(panelEntity.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun panelEntity_inFullSpaceMode_scalesWithSizeScaleAndDetachment() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val session = sessionRule.session
        val panelEntity =
            sessionRule.createWhitePanelEntity(
                name = "perceivedResolutionSizeScalePanel",
                pose = poseInFrontOfCamera(session, renderViewpoint, MEDIUM_DISTANCE_M),
            )
        val baselineResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()

        // PanelEntity.size relayouts the panel's views and requires the main thread.
        sessionRule.onMainThread { panelEntity.size = EXPANDED_PANEL_SIZE_M }
        val resizedResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledByAxes(
            actual = resizedResolution,
            base = baselineResolution,
            expectedWidthRatio = EXPANDED_PANEL_SIZE_M.width / INITIAL_PANEL_SIZE_M.width,
            expectedHeightRatio = EXPANDED_PANEL_SIZE_M.height / INITIAL_PANEL_SIZE_M.height,
        )

        panelEntity.setScale(PANEL_SCALE_FACTOR)
        val scaledResolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(
            actual = scaledResolution,
            base = resizedResolution,
            expectedRatio = PANEL_SCALE_FACTOR,
        )

        val activitySpace = session.scene.activitySpace
        assertThat(activitySpace.children).contains(panelEntity)
        panelEntity.parent = null
        assertThat(activitySpace.children).doesNotContain(panelEntity)
        session.scene.mainPanelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
    }

    // Method-level @XrDeviceTest: XrDeviceFilter only reads @RequiresSpatialApi from it.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun panelEntity_spatialMoveGestures_scalesPerceivedResolutionWithDistance() {
        val panelName = "movablePerceivedResolutionPanel"
        val moveAwayListener = OneShotMoveEndListener()
        val moveCloserListener = OneShotMoveEndListener()
        val session = sessionRule.session
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()

        session.scene.mainPanelEntity.setEnabled(false)
        val panelEntity = createInteractivePanel(panelName)
        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)
        movableComponent.addMoveListener(moveAwayListener)
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        val initialNode =
            awaitInteractivePanelNode(
                panelName = panelName,
                requireMovable = true,
                requireResizable = false,
            )
        val initialSnapshot = capturePanelDistanceAndResolution(renderViewpoint, panelEntity)
        val cameraInPanelLocal =
            session.scene.activitySpace.transformPositionTo(
                cameraPoseInActivitySpace(session, renderViewpoint).translation,
                panelEntity,
            )
        val moveAwayX =
            if (cameraInPanelLocal.x <= 0f) {
                LATERAL_MOVE_DISTANCE_M
            } else {
                -LATERAL_MOVE_DISTANCE_M
            }
        val moveFurtherAwayDelta = Vector3(moveAwayX, 0f, 0f)

        initialNode.performSpatialInteraction { dragMoveAffordanceBy(moveFurtherAwayDelta) }
        moveAwayListener.awaitMoveEnd("first move (further away)")
        // System move updates the panel pose on the main thread.
        waitUntil(
            description = "panel distance to increase beyond ${initialSnapshot.distanceMeters}m",
            timeout = CALLBACK_TIMEOUT,
        ) {
            sessionRule.onMainThread {
                distanceToCamera(session, renderViewpoint, panelEntity) >
                    initialSnapshot.distanceMeters + MIN_DISTANCE_DELTA_M
            }
        }

        val afterMoveAway = capturePanelDistanceAndResolution(renderViewpoint, panelEntity)
        assertScaledBy(
            actual = afterMoveAway.resolution,
            base = initialSnapshot.resolution,
            expectedRatio = initialSnapshot.distanceMeters / afterMoveAway.distanceMeters,
        )

        // Re-capture the node after SpaceFlinger commits the move before starting the return drag.
        movableComponent.removeMoveListener(moveAwayListener)
        movableComponent.addMoveListener(moveCloserListener)
        val moveCloserLocalDelta = sessionRule.onMainThread {
            session.scene.activitySpace.transformPositionTo(
                INTERACTIVE_PANEL_POSE.translation,
                panelEntity,
            )
        }
        val movedFarNode =
            awaitInteractivePanelNode(
                panelName = panelName,
                requireMovable = true,
                requireResizable = false,
                movedFromPosition = initialNode.position,
            )
        movedFarNode.performSpatialInteraction { dragMoveAffordanceBy(moveCloserLocalDelta) }
        moveCloserListener.awaitMoveEnd("second move (closer)")
        waitUntil(
            description = "panel distance to decrease below ${afterMoveAway.distanceMeters}m",
            timeout = CALLBACK_TIMEOUT,
        ) {
            sessionRule.onMainThread {
                distanceToCamera(session, renderViewpoint, panelEntity) <
                    afterMoveAway.distanceMeters - MIN_DISTANCE_DELTA_M
            }
        }

        val afterMoveCloser = capturePanelDistanceAndResolution(renderViewpoint, panelEntity)
        assertScaledBy(
            actual = afterMoveCloser.resolution,
            base = afterMoveAway.resolution,
            expectedRatio = afterMoveAway.distanceMeters / afterMoveCloser.distanceMeters,
        )
    }

    // Method-level @XrDeviceTest: XrDeviceFilter only reads @RequiresSpatialApi from it.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun panelEntity_spatialResizeGesture_scalesPerceivedResolutionWithSize() {
        val panelName = "resizablePerceivedResolutionPanel"
        val resizeListener = ApplyingResizeEndListener()
        val session = sessionRule.session
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()

        session.scene.mainPanelEntity.setEnabled(false)
        val panelEntity = createInteractivePanel(panelName)
        val resizableComponent =
            ResizableComponent.create(session = session, resizeEventListener = resizeListener)
        assertThat(panelEntity.addComponent(resizableComponent)).isTrue()

        val panelNode =
            awaitInteractivePanelNode(
                panelName = panelName,
                requireMovable = false,
                requireResizable = true,
            )
        val preResize = sessionRule.onMainThread {
            PanelSizeSnapshot(
                sizeInPixels = panelEntity.sizeInPixels,
                sizeInMeters = panelEntity.size,
                perceivedResolution =
                    panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess(),
            )
        }

        panelNode.performSpatialInteraction {
            moveOriginTo(Vector3(0f, 0f, CLOSE_RAY_ORIGIN_OFFSET_Z))
            resizeNodeBy(
                delta = RESIZE_OUTWARD_DELTA,
                alignment = SpatialAlignment.BottomRight,
                marginMeters = RESIZE_AFFORDANCE_MARGIN_METERS,
            )
        }
        resizeListener.awaitResizeEnd("panelNode=${panelNode.name}")
        // ApplyingResizeEndListener updates panelEntity.size on the main thread.
        waitUntil(
            description =
                "panel '$panelName' pixel dimensions to grow beyond ${preResize.sizeInPixels}",
            timeout = POLL_TIMEOUT,
        ) {
            val current = sessionRule.onMainThread { panelEntity.sizeInPixels }
            current.width > preResize.sizeInPixels.width &&
                current.height > preResize.sizeInPixels.height
        }

        val postResize = sessionRule.onMainThread {
            PanelSizeSnapshot(
                sizeInPixels = panelEntity.sizeInPixels,
                sizeInMeters = panelEntity.size,
                perceivedResolution =
                    panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess(),
            )
        }
        assertScaledByAxes(
            actual = postResize.perceivedResolution,
            base = preResize.perceivedResolution,
            expectedWidthRatio = postResize.sizeInMeters.width / preResize.sizeInMeters.width,
            expectedHeightRatio = postResize.sizeInMeters.height / preResize.sizeInMeters.height,
        )
    }

    @Test
    fun surfaceEntity_inFullSpaceMode_scalesAcrossQuadSphereAndHemisphere() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val session = sessionRule.session
        val surfaceEntity = sessionRule.createQuadSurfaceEntity(renderViewpoint)

        val quadMediumResolution =
            surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()

        surfaceEntity.setPose(poseInFrontOfCamera(session, renderViewpoint, FAR_DISTANCE_M))
        val quadFarResolution =
            surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(
            actual = quadFarResolution,
            base = quadMediumResolution,
            expectedRatio = MEDIUM_DISTANCE_M / FAR_DISTANCE_M,
        )

        surfaceEntity.setPose(poseInFrontOfCamera(session, renderViewpoint, NEAR_DISTANCE_M))
        val quadNearResolution =
            surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(
            actual = quadNearResolution,
            base = quadMediumResolution,
            expectedRatio = MEDIUM_DISTANCE_M / NEAR_DISTANCE_M,
        )

        // A Sphere centered closer than its radius surrounds the camera (closest face <= 0m).
        surfaceEntity.setPose(
            poseInFrontOfCamera(session, renderViewpoint, SURROUNDING_SPHERE_OFFSET_M)
        )
        sessionRule.onMainThread {
            surfaceEntity.shape = SurfaceEntity.Shape.Sphere(SURFACE_SPHERE_RADIUS_M)
        }
        assertThat(surfaceEntity.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)

        // Sphere (2r x 2r x 2r) closest face is d - r, whereas Hemisphere (2r x 2r x r) is d - r/2,
        // yielding a ratio of (3.0 - 1.0) / (3.0 - 0.5) = 0.8 at 3.0m.
        surfaceEntity.setPose(poseInFrontOfCamera(session, renderViewpoint, FAR_SPHERE_DISTANCE_M))
        val farSphereResolution =
            surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()

        sessionRule.onMainThread {
            surfaceEntity.shape = SurfaceEntity.Shape.Hemisphere(SURFACE_SPHERE_RADIUS_M)
        }
        val farHemisphereResolution =
            surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        val expectedHemisphereToSphereRatio =
            (FAR_SPHERE_DISTANCE_M - SURFACE_SPHERE_RADIUS_M) /
                (FAR_SPHERE_DISTANCE_M - (SURFACE_SPHERE_RADIUS_M / 2.0f))
        assertScaledBy(
            actual = farHemisphereResolution,
            base = farSphereResolution,
            expectedRatio = expectedHemisphereToSphereRatio,
        )
    }

    // Method-level @XrDeviceTest: XrDeviceFilter only reads @RequiresSpatialApi from it.
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun surfaceEntity_withPreferredSkyboxAndGeometryEnvironment_scalesAndDetachesCleanly() {
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()
        val session = sessionRule.session

        // Asset creation and SpatialEnvironment preferences require the main thread.
        sessionRule.onMainThread {
            val blueSkybox =
                ImageBasedLightingAsset.createFromZip(
                    session,
                    Paths.get("skyboxes", "BlueSkybox.zip"),
                )
            val environmentGeometry =
                GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            session.scene.spatialEnvironment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(blueSkybox, environmentGeometry)
        }
        sessionRule.waitForSpatialEnvironmentSettled()

        val surfaceEntity = sessionRule.createQuadSurfaceEntity(renderViewpoint)

        val mediumResolution = surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        surfaceEntity.setPose(poseInFrontOfCamera(session, renderViewpoint, FAR_DISTANCE_M))
        val farResolution = surfaceEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
        assertScaledBy(
            actual = farResolution,
            base = mediumResolution,
            expectedRatio = MEDIUM_DISTANCE_M / FAR_DISTANCE_M,
        )

        val activitySpace = session.scene.activitySpace
        assertThat(activitySpace.children).contains(surfaceEntity)
        surfaceEntity.parent = null
        assertThat(activitySpace.children).doesNotContain(surfaceEntity)
        session.scene.mainPanelEntity.getPerceivedResolution(renderViewpoint).assertSuccess()
    }

    // TODO(b/584678840): Remove @Ignore once PanelEntity and SurfaceEntity getPerceivedResolution
    //  return InvalidRenderViewpoint in Home Space Mode as their KDoc documents.
    @Ignore(
        "b/584678840: getPerceivedResolution doesn't return InvalidRenderViewpoint in Home Space"
    )
    @Test
    fun getPerceivedResolution_inHomeSpaceMode_returnsInvalidRenderViewpoint() {
        sessionRule.requestHomeSpace()
        val renderViewpoint = sessionRule.configureSpatialTrackingAndAwaitRenderViewpoint()

        val panelEntity =
            sessionRule.createWhitePanelEntity(
                name = "homeSpaceInvalidViewpointPanel",
                pose = Pose(Vector3(0f, 0f, -MEDIUM_DISTANCE_M)),
            )
        val surfaceEntity = createFixedQuadSurfaceEntity()

        assertThat(panelEntity.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.InvalidRenderViewpoint::class.java)
        assertThat(surfaceEntity.getPerceivedResolution(renderViewpoint))
            .isInstanceOf(PerceivedResolutionResult.InvalidRenderViewpoint::class.java)
    }

    // TODO(b/584678921): Remove @Ignore once SurfaceEntity.getPerceivedResolution enforces
    //  DeviceTrackingMode.SPATIAL as its @throws IllegalStateException KDoc documents.
    @Ignore("b/584678921: SurfaceEntity.getPerceivedResolution doesn't check DeviceTrackingMode")
    @Test
    fun surfaceEntity_getPerceivedResolutionWhenDeviceTrackingDisabled_throwsIllegalStateException() {
        val session = sessionRule.session
        assertThat(session.config.deviceTracking).isEqualTo(DeviceTrackingMode.DISABLED)
        val viewpoint = stereoOrMonoRenderViewpoint(session)
        val surfaceEntity = createFixedQuadSurfaceEntity()

        assertThrows(IllegalStateException::class.java) {
            surfaceEntity.getPerceivedResolution(viewpoint)
        }
    }

    // Translates origin alongside target so the move plane shifts laterally instead of orbiting a
    // fixed origin at constant distance.
    private fun SpatialInteractionScope.dragMoveAffordanceBy(delta: Vector3) {
        val originNormalOffset = Vector3(0f, 0f, CLOSE_RAY_ORIGIN_OFFSET_Z)
        pointAtMoveAffordance()
        val startTarget = currentTarget
        moveOriginTo(startTarget + originNormalOffset)
        press()
        animate { fraction ->
            val target = startTarget + (delta * fraction)
            moveOriginTo(target + originNormalOffset)
            pointAt(offset = target)
        }
        release()
    }

    private fun createInteractivePanel(name: String): PanelEntity = sessionRule.onMainThread {
        PanelEntity.create(
            session = session,
            view = View(activity).apply { setBackgroundColor(Color.WHITE) },
            pixelDimensions = INTERACTIVE_PANEL_PIXELS,
            name = name,
            pose = INTERACTIVE_PANEL_POSE,
            parent = session.scene.activitySpace,
        )
    }

    private fun createFixedQuadSurfaceEntity(): SurfaceEntity = sessionRule.onMainThread {
        SurfaceEntity.create(
            session = session,
            pose = Pose(Vector3(0f, 0f, -MEDIUM_DISTANCE_M)),
            shape = SurfaceEntity.Shape.Quad(SURFACE_QUAD_EXTENTS_M),
            stereoMode = SurfaceEntity.StereoMode.MONO,
            parent = session.scene.activitySpace,
        )
    }

    // Read on the main thread because system move gestures update the panel pose there.
    private fun capturePanelDistanceAndResolution(
        renderViewpoint: RenderViewpoint,
        panelEntity: PanelEntity,
    ): EntityDistanceAndResolution = sessionRule.onMainThread {
        EntityDistanceAndResolution(
            distanceMeters = distanceToCamera(session, renderViewpoint, panelEntity),
            resolution = panelEntity.getPerceivedResolution(renderViewpoint).assertSuccess(),
        )
    }

    private fun awaitInteractivePanelNode(
        panelName: String,
        requireMovable: Boolean,
        requireResizable: Boolean,
        movedFromPosition: Vector3? = null,
    ): SpatialNode =
        checkNotNull(
            SpatialSceneHelper.waitForSpatialScene(
                description = "settled interactive spatial panel '$panelName'",
                timeout = POLL_TIMEOUT,
                select = { scene ->
                    val task = scene.findTaskByActivity(sessionRule.activity)
                    val settled =
                        task?.isSettled(
                            expectedEmbeddedPanelCount = 1,
                            expectedMainPanelVisible = false,
                            requireFullSpace = true,
                        ) == true
                    if (settled) {
                        task?.findNode { it.name == panelName && it.isEmbeddedWindowLeash }
                    } else {
                        null
                    }
                },
                until = { candidate ->
                    val alphaRestored =
                        candidate?.properties?.get("computedAlpha") != "0.000" &&
                            candidate?.primarySurfaceNode?.properties?.get("computedAlpha") !=
                                "0.000"
                    candidate != null &&
                        alphaRestored &&
                        candidate.effectiveBufferSize != null &&
                        candidate.worldExtents != null &&
                        (!requireMovable || candidate.isMovable) &&
                        (!requireResizable || candidate.isResizable) &&
                        (movedFromPosition == null ||
                            (candidate.position - movedFromPosition).length >
                                MIN_COMMITTED_MOVE_SHIFT_M)
                },
            )
        )

    private data class PanelSizeSnapshot(
        val sizeInPixels: IntSize2d,
        val sizeInMeters: FloatSize2d,
        val perceivedResolution: IntSize2d,
    )

    private class OneShotMoveEndListener : EntityMoveListener {
        private val moveEnded = CountDownLatch(1)

        override fun onMoveStart(
            entity: Entity,
            initialInputRay: Ray,
            initialPose: Pose,
            initialScale: Float,
            initialParent: Entity,
        ) {}

        override fun onMoveUpdate(
            entity: Entity,
            currentInputRay: Ray,
            currentPose: Pose,
            currentScale: Float,
        ) {}

        override fun onMoveEnd(
            entity: Entity,
            finalInputRay: Ray,
            finalPose: Pose,
            finalScale: Float,
            updatedParent: Entity?,
        ) {
            moveEnded.countDown()
        }

        fun awaitMoveEnd(label: String) {
            assertWithMessage("Timed out waiting for MovableComponent.onMoveEnd ($label)")
                .that(moveEnded.await(CALLBACK_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS))
                .isTrue()
        }
    }

    private class ApplyingResizeEndListener : Consumer<ResizeEvent> {
        private val resizeEnded = CountDownLatch(1)

        override fun accept(event: ResizeEvent) {
            if (event.resizeState == ResizeEvent.ResizeState.END) {
                (event.entity as? PanelEntity)?.size = event.newSize.to2d()
                resizeEnded.countDown()
            }
        }

        fun awaitResizeEnd(diagnostics: String) {
            assertWithMessage(
                    "Timed out waiting for ResizableComponent ResizeState.END ($diagnostics)"
                )
                .that(
                    resizeEnded.await(CALLBACK_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                )
                .isTrue()
        }
    }

    private companion object {
        const val NEAR_DISTANCE_M = 0.75f
        const val FAR_DISTANCE_M = 3.0f
        const val SURROUNDING_SPHERE_OFFSET_M = 0.25f
        const val FAR_SPHERE_DISTANCE_M = 3.0f
        const val PANEL_SCALE_FACTOR = 2.0f
        const val ENTITY_YAW_ROTATION_DEG = 60f
        const val ENTITY_PITCH_ROTATION_DEG = 45f
        const val CLOSE_RAY_ORIGIN_OFFSET_Z = 0.35f
        const val RESIZE_AFFORDANCE_MARGIN_METERS = 0.02f
        const val MIN_COMMITTED_MOVE_SHIFT_M = 0.25f
        const val MIN_DISTANCE_DELTA_M = 0.05f
        const val LATERAL_MOVE_DISTANCE_M = 0.6f

        /** Settle window (>= 300 ms) after an arrival anchor before asserting silence. */
        val NO_CALLBACK_SETTLE_TIMEOUT = 300.milliseconds

        val POLL_TIMEOUT = 10.seconds
        val EXPANDED_PANEL_SIZE_M = FloatSize2d(2.0f, 1.5f)
        val INTERACTIVE_PANEL_PIXELS = IntSize2d(1280, 960)
        /** Placed 15cm in front of ActivitySpace origin so task-root sinks cannot occlude rays. */
        val INTERACTIVE_PANEL_POSE = Pose(Vector3(0f, 0f, 0.15f))
        val RESIZE_OUTWARD_DELTA = Vector3(0.2f, -0.2f, 0f)
    }
}
