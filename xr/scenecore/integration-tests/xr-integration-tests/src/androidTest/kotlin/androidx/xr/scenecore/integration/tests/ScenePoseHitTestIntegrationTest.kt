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
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ScenePose
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitForSpatialScene
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Device integration tests for [ScenePose.hitTest] and [HitTestResult] across [ActivitySpace],
 * [Entity] frames, [PerceptionSpace] poses, [ArDevice] / [RenderViewpoint], and collidable
 * [GltfModelEntity] targets (CUJ R28 Digital Hit Test).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class ScenePoseHitTestIntegrationTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions("android.permission.HEAD_TRACKING"))

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    @get:Rule(order = 2) val timeout: Timeout = Timeout.seconds(60)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @Test
    fun activitySpace_hitTest_panelEntity_returnsPlaneHitPositionNormalAndDistance() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace
        session.scene.mainPanelEntity.setEnabled(false)

        val targetPanel =
            createTestPanel(
                name = "hit_target_panel",
                size = FloatSize2d(1.0f, 1.0f),
                pose = Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
                parent = activitySpace,
            )

        val hit =
            waitForHitTest(
                description = "SELF_SCENE center ray to hit PanelEntity",
                scenePose = activitySpace,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
                hitTestFilter = ScenePose.HitTestFilter.SELF_SCENE,
            ) {
                it.isIntersection()
            }!!

        assertThat(hit.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(hit.distance).isWithin(POSITION_TOLERANCE).of(2.0f)
        assertVector3(hit.hitPosition, Vector3(0f, 0f, -2.0f), POSITION_TOLERANCE)
        assertThat(hit.surfaceNormal).isNotNull()
        assertVector3(hit.surfaceNormal!!, Vector3(0f, 0f, 1f), NORMAL_TOLERANCE)

        // Default 2-arg ScenePose.hitTest overload matches explicit HitTestFilter.SELF_SCENE.
        val defaultOverloadHit =
            hitTestDefaultOverloadBlocking(
                scenePose = activitySpace,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
            )
        assertWithMessage(
                "Default 2-arg ScenePose.hitTest should hit panel consistently with SELF_SCENE: $defaultOverloadHit"
            )
            .that(defaultOverloadHit.isIntersection())
            .isTrue()
        assertThat(defaultOverloadHit!!.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(defaultOverloadHit.distance).isWithin(POSITION_TOLERANCE).of(hit.distance)
        assertVector3(
            defaultOverloadHit.hitPosition,
            Vector3(0f, 0f, -2.0f),
            POSITION_TOLERANCE,
        )
        assertThat(defaultOverloadHit.surfaceNormal).isNotNull()
        assertVector3(
            defaultOverloadHit.surfaceNormal!!,
            Vector3(0f, 0f, 1f),
            NORMAL_TOLERANCE,
        )

        val angledOrigin = Vector3(0.5f, 0.25f, 0f)
        val angledTarget = Vector3(0f, 0f, -2.0f)
        val angledRay = angledTarget - angledOrigin
        val angledHit =
            hitTestBlocking(
                scenePose = activitySpace,
                origin = angledOrigin,
                direction = angledRay.toNormalized(),
            )

        assertWithMessage("Expected angled ray to intersect PanelEntity: $angledHit")
            .that(angledHit.isIntersection())
            .isTrue()
        assertThat(angledHit!!.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(angledHit.distance.isFinite()).isTrue()
        assertThat(angledHit.distance).isGreaterThan(hit.distance)
        assertVector3(angledHit.hitPosition, angledTarget, POSITION_TOLERANCE)
        assertThat(angledHit.surfaceNormal).isNotNull()
        assertVector3(angledHit.surfaceNormal!!, Vector3(0f, 0f, 1f), NORMAL_TOLERANCE)

        // Rotate panel (pitch 15 deg, yaw 30 deg) and verify surfaceNormal aligns with rotated +Z.
        val tiltedRotation = Quaternion.fromEulerAngles(pitch = 15f, yaw = 30f, roll = 0f)
        val expectedTiltedNormal = (tiltedRotation * Vector3(0f, 0f, 1f)).toNormalized()
        targetPanel.setPose(Pose(Vector3(0f, 0f, -2.0f), tiltedRotation), Space.ACTIVITY)

        val tiltedHit =
            waitForHitTest(
                description = "ray to hit tilted PanelEntity with rotated surfaceNormal",
                scenePose = activitySpace,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    it!!.surfaceNormal != null &&
                    (it.surfaceNormal!! - expectedTiltedNormal).length < NORMAL_TOLERANCE
            }!!

        assertThat(tiltedHit.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertVector3(tiltedHit.hitPosition, Vector3(0f, 0f, -2.0f), POSITION_TOLERANCE)
        assertVector3(tiltedHit.surfaceNormal!!, expectedTiltedNormal, NORMAL_TOLERANCE)
    }

    @Test
    fun activitySpace_hitTest_mainPanelAndOccludingPanels_returnsClosestEnabledPanel() {
        val session = sessionRule.session
        val mainPanel = session.scene.mainPanelEntity
        val mainZ = -0.5f

        sessionRule.onMainThread {
            val view =
                TextView(sessionRule.activity).apply {
                    text = "main_panel_hit_test"
                    setBackgroundColor(Color.DKGRAY)
                    setTextColor(Color.WHITE)
                }
            sessionRule.activity.setContentView(view)
        }
        mainPanel.setEnabled(true)
        mainPanel.setPose(Pose(Vector3(0f, 0f, mainZ), Quaternion.Identity), Space.ACTIVITY)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        checkNotNull(
            waitForSpatialScene(
                description = "visible main panel SpatialNode with worldExtents",
                select = { scene ->
                    val task = scene.findTaskByActivity(sessionRule.activity)
                    (task?.findNodeByName(MAIN_PANEL_NODE_NAME) ?: task?.taskWindowLeash)?.takeIf {
                        it.isVisible && it.worldExtents != null
                    }
                },
                until = { it != null },
            )
        ) {
            "Expected visible main panel SpatialNode with worldExtents"
        }

        val rayOrigin = Vector3(0f, 0f, 1.0f)

        val mainPanelHit =
            waitForHitTest(
                description = "ray to hit MainPanelEntity at z=$mainZ",
                scenePose = session.scene.activitySpace,
                origin = rayOrigin,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    kotlin.math.abs(it!!.hitPosition.z - mainZ) < MAIN_PANEL_HIT_TOLERANCE
            }!!

        // TODO(b/584956584): Tighten back to SurfaceType.PLANE once WM's
        // ActivityRecordInputSink child node (IsContent2d() == false) no longer wins
        // coplanar DrawOrderComputer sort ahead of MainPanelAndTaskWindowLeashNode.
        assertThat(mainPanelHit.surfaceType)
            .isIn(listOf(HitTestResult.SurfaceType.PLANE, HitTestResult.SurfaceType.OBJECT))
        assertThat(mainPanelHit.distance.isFinite()).isTrue()
        assertThat(mainPanelHit.distance).isGreaterThan(0f)
        assertVector3(
            mainPanelHit.hitPosition,
            Vector3(0f, 0f, mainZ),
            MAIN_PANEL_HIT_TOLERANCE,
        )

        mainPanel.setEnabled(false)

        val nearPanel =
            createTestPanel(
                name = "near_panel",
                size = FloatSize2d(1.0f, 1.0f),
                pose = Pose(Vector3(0f, 0f, -1.5f), Quaternion.Identity),
                parent = session.scene.activitySpace,
            )
        createTestPanel(
            name = "far_panel",
            size = FloatSize2d(1.0f, 1.0f),
            pose = Pose(Vector3(0f, 0f, -3.0f), Quaternion.Identity),
            parent = session.scene.activitySpace,
        )

        val nearHit =
            waitForHitTest(
                description = "ray to hit near occluding PanelEntity",
                scenePose = session.scene.activitySpace,
                origin = rayOrigin,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    kotlin.math.abs(it!!.hitPosition.z - (-1.5f)) < POSITION_TOLERANCE
            }!!

        assertThat(nearHit.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(nearHit.distance).isGreaterThan(mainPanelHit.distance)
        assertVector3(nearHit.hitPosition, Vector3(0f, 0f, -1.5f), POSITION_TOLERANCE)

        nearPanel.setEnabled(false)

        val farHitAfterDisable =
            waitForHitTest(
                description = "ray to hit far PanelEntity when near panel is disabled",
                scenePose = session.scene.activitySpace,
                origin = rayOrigin,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    kotlin.math.abs(it!!.hitPosition.z - (-3.0f)) < POSITION_TOLERANCE
            }!!

        assertThat(farHitAfterDisable.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(farHitAfterDisable.distance).isGreaterThan(nearHit.distance)
        assertVector3(
            farHitAfterDisable.hitPosition,
            Vector3(0f, 0f, -3.0f),
            POSITION_TOLERANCE,
        )

        nearPanel.setEnabled(true)

        val nearHitAfterReenable =
            waitForHitTest(
                description = "ray to hit near PanelEntity again after re-enabling",
                scenePose = session.scene.activitySpace,
                origin = rayOrigin,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    kotlin.math.abs(it!!.hitPosition.z - (-1.5f)) < POSITION_TOLERANCE
            }!!

        assertThat(nearHitAfterReenable.distance).isLessThan(farHitAfterDisable.distance)
        assertThat(nearHitAfterReenable.distance).isWithin(POSITION_TOLERANCE).of(nearHit.distance)
        assertVector3(
            nearHitAfterReenable.hitPosition,
            Vector3(0f, 0f, -1.5f),
            POSITION_TOLERANCE,
        )
    }

    // TODO(b/584956584): Remove @Ignore once MainPanelEntity hitTest reports SurfaceType.PLANE
    // instead of WM's ActivityRecordInputSink child node (IsContent2d() == false because
    // ComponentActivity has no 2D content) winning coplanar DrawOrderComputer sort ahead of
    // MainPanelAndTaskWindowLeashNode and returning SurfaceType.OBJECT.
    @Ignore(
        "b/584956584: ScenePose.hitTest against MainPanelEntity returns SurfaceType.OBJECT instead of PLANE"
    )
    @Test
    fun activitySpace_hitTest_mainPanelEntity_returnsPlaneSurfaceType() {
        val session = sessionRule.session
        val mainPanel = session.scene.mainPanelEntity
        val mainZ = -0.5f

        sessionRule.onMainThread {
            val view =
                TextView(sessionRule.activity).apply {
                    text = "main_panel_surface_type_test"
                    setBackgroundColor(Color.DKGRAY)
                    setTextColor(Color.WHITE)
                }
            sessionRule.activity.setContentView(view)
        }
        mainPanel.setEnabled(true)
        mainPanel.setPose(Pose(Vector3(0f, 0f, mainZ), Quaternion.Identity), Space.ACTIVITY)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        checkNotNull(
            waitForSpatialScene(
                description = "visible main panel SpatialNode with worldExtents",
                select = { scene ->
                    val task = scene.findTaskByActivity(sessionRule.activity)
                    (task?.findNodeByName(MAIN_PANEL_NODE_NAME) ?: task?.taskWindowLeash)?.takeIf {
                        it.isVisible && it.worldExtents != null
                    }
                },
                until = { it != null },
            )
        ) {
            "Expected visible main panel SpatialNode with worldExtents"
        }

        val mainPanelHit =
            waitForHitTest(
                description = "ray to hit MainPanelEntity at z=$mainZ",
                scenePose = session.scene.activitySpace,
                origin = Vector3(0f, 0f, 1.0f),
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    kotlin.math.abs(it!!.hitPosition.z - mainZ) < MAIN_PANEL_HIT_TOLERANCE
            }!!

        assertThat(mainPanelHit.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
    }

    @Test
    fun activitySpace_hitTest_otherScenesWithoutOverlayPermission_completesWithoutHanging() {
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        createTestPanel(
            name = "other_scenes_filter_panel",
            size = FloatSize2d(0.5f, 0.5f),
            pose = Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
            parent = session.scene.activitySpace,
        )

        waitForHitTest(
            description = "SELF_SCENE baseline hit on panel before OTHER_SCENES call",
            scenePose = session.scene.activitySpace,
            origin = Vector3.Zero,
            direction = Vector3(0f, 0f, -1f),
            hitTestFilter = ScenePose.HitTestFilter.SELF_SCENE,
        ) {
            it.isIntersection()
        }

        var securityException: SecurityException? = null
        try {
            hitTestBlocking(
                scenePose = session.scene.activitySpace,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
                hitTestFilter =
                    ScenePose.HitTestFilter.SELF_SCENE or ScenePose.HitTestFilter.OTHER_SCENES,
            )
        } catch (e: SecurityException) {
            securityException = e
        }
        assertWithMessage(
                "Expected SecurityException when HitTestFilter.OTHER_SCENES is passed without ACCESS_XR_OVERLAY_SPACE"
            )
            .that(securityException)
            .isNotNull()
    }

    // TODO(b/476249571): ScenePose.hitTest currently returns HitTestResult(distance =
    // POSITIVE_INFINITY, surfaceType = UNKNOWN) instead of null on misses; re-enable once fixed.
    @Ignore(
        "b/476249571: ScenePose.hitTest returns HitTestResult with POSITIVE_INFINITY instead of null"
    )
    @Test
    fun activitySpace_hitTest_missAndUncollidedGltf_returnsNull() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace
        session.scene.mainPanelEntity.setEnabled(false)

        val targetPanel =
            createTestPanel(
                name = "miss_target_panel",
                size = FloatSize2d(0.5f, 0.5f),
                pose = Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
                parent = activitySpace,
            )

        waitForHitTest(
            description = "miss target panel registered in scene",
            scenePose = activitySpace,
            origin = Vector3.Zero,
            direction = Vector3(0f, 0f, -1f),
        ) {
            it.isIntersection()
        }

        val lateralMiss =
            hitTestBlocking(
                scenePose = activitySpace,
                origin = Vector3(2.0f, 0f, 0f),
                direction = Vector3(0f, 0f, -1f),
                hitTestFilter = ScenePose.HitTestFilter.SELF_SCENE,
            )
        assertThat(lateralMiss).isNull()

        val backwardMiss =
            hitTestBlocking(
                scenePose = activitySpace,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, 1f),
                hitTestFilter = ScenePose.HitTestFilter.SELF_SCENE,
            )
        assertThat(backwardMiss).isNull()

        targetPanel.removeAllComponents()
        targetPanel.parent = null

        val gltfEntity = sessionRule.onMainThread {
            val model = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            GltfModelEntity.create(
                session,
                model,
                Pose(Vector3(0f, 0f, -2.5f), Quaternion.Identity),
                parent = activitySpace,
            )
        }
        waitForSpatialScene(
            description = "visible uncollided GltfModelEntity subspace node",
            select = { scene -> scene.findTaskByActivity(sessionRule.activity) },
            until = { task ->
                task?.subspaceNodes?.any {
                    it.name.startsWith(GLTF_SUBSPACE_PREFIX) && it.isVisible
                } == true
            },
        )
        val targetCenterInActivity = gltfEntity.transformPositionTo(Vector3.Zero, activitySpace)
        val rayDir = targetCenterInActivity.toNormalized()

        val hitWithoutCollider = hitTestBlocking(activitySpace, Vector3.Zero, rayDir)
        assertThat(hitWithoutCollider).isNull()
    }

    @Test
    fun entityAndPerceptionPose_hitTest_operatesInLocalScenePoseFrame() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace
        val perceptionSpace = session.scene.perceptionSpace
        session.scene.mainPanelEntity.setEnabled(false)

        val parentPanel =
            createTestPanel(
                name = "parent_panel",
                size = FloatSize2d(1.0f, 1.0f),
                pose = Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
                parent = activitySpace,
            )
        val childPanel =
            createTestPanel(
                name = "child_panel",
                size = FloatSize2d(1.0f, 1.0f),
                pose = Pose(Vector3(1.5f, 0f, -1.0f), Quaternion.Identity),
                parent = parentPanel,
            )

        waitForHitTest(
            description = "parentPanel registered in scene",
            scenePose = activitySpace,
            origin = Vector3.Zero,
            direction = Vector3(0f, 0f, -1f),
        ) {
            it.isIntersection()
        }
        waitForHitTest(
            description = "childPanel registered in scene",
            scenePose = activitySpace,
            origin = Vector3(1.5f, 0f, 0f),
            direction = Vector3(0f, 0f, -1f),
        ) {
            it.isIntersection()
        }

        // Ray from parentPanel's local frame into childPanel at local (1.5, 0, -1.0).
        val parentToChildHit =
            hitTestBlocking(
                scenePose = parentPanel,
                origin = Vector3(1.5f, 0f, 0f),
                direction = Vector3(0f, 0f, -1f),
            )
        assertWithMessage("Expected parentPanel local ray to hit childPanel: $parentToChildHit")
            .that(parentToChildHit.isIntersection())
            .isTrue()
        assertThat(parentToChildHit!!.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(parentToChildHit.distance).isWithin(POSITION_TOLERANCE).of(1.0f)
        assertVector3(
            parentToChildHit.hitPosition,
            Vector3(1.5f, 0f, -1.0f),
            POSITION_TOLERANCE,
        )
        assertThat(parentToChildHit.surfaceNormal).isNotNull()
        assertVector3(
            parentToChildHit.surfaceNormal!!,
            Vector3(0f, 0f, 1f),
            NORMAL_TOLERANCE,
        )

        // Ray from childPanel's local frame back into parentPanel at local (-1.5, 0, +1.0).
        val childToParentHit =
            hitTestBlocking(
                scenePose = childPanel,
                origin = Vector3(-1.5f, 0f, 2.5f),
                direction = Vector3(0f, 0f, -1f),
            )
        assertWithMessage("Expected childPanel local ray to hit parentPanel: $childToParentHit")
            .that(childToParentHit.isIntersection())
            .isTrue()
        assertThat(childToParentHit!!.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(childToParentHit.distance.isFinite()).isTrue()
        assertThat(childToParentHit.distance).isGreaterThan(parentToChildHit.distance)
        assertVector3(
            childToParentHit.hitPosition,
            Vector3(-1.5f, 0f, 1.0f),
            POSITION_TOLERANCE,
        )

        // Synthetic observer ScenePose constructed via
        // PerceptionSpace.getScenePoseFromPerceptionPose.
        val panelPoseInPerception =
            activitySpace.transformPoseTo(
                parentPanel.getPose(Space.ACTIVITY),
                perceptionSpace,
            )
        val observerPoseInPerception =
            Pose(
                panelPoseInPerception.transformPoint(Vector3(0f, 0f, 1.2f)),
                panelPoseInPerception.rotation,
            )
        val observerScenePose =
            perceptionSpace.getScenePoseFromPerceptionPose(observerPoseInPerception)

        val perceptionHit =
            hitTestBlocking(
                scenePose = observerScenePose,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
            )
        assertWithMessage(
                "Expected perception-derived ScenePose ray to hit parentPanel: $perceptionHit"
            )
            .that(perceptionHit.isIntersection())
            .isTrue()
        assertThat(perceptionHit!!.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(perceptionHit.distance.isFinite()).isTrue()
        assertThat(perceptionHit.distance).isGreaterThan(parentToChildHit.distance)
        assertThat(perceptionHit.distance).isLessThan(childToParentHit.distance)
        assertVector3(
            perceptionHit.hitPosition,
            Vector3(0f, 0f, -1.2f),
            POSITION_TOLERANCE,
        )
        assertThat(perceptionHit.surfaceNormal).isNotNull()
        assertVector3(perceptionHit.surfaceNormal!!, Vector3(0f, 0f, 1f), NORMAL_TOLERANCE)
    }

    @Test
    fun arDeviceAndRenderViewpoint_hitTest_whenSpatialTrackingActive() {
        val session = sessionRule.session
        val spatialConfig =
            Config.Builder(session.config).setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
        assertThat(session.configure(spatialConfig))
            .isInstanceOf(SessionConfigureSuccess::class.java)

        val arDevice = ArDevice.getInstance(session)
        waitUntil(
            description = "ArDevice TrackingState.TRACKING",
            timeout = 5000.milliseconds,
            pollInterval = 50.milliseconds,
        ) {
            arDevice.state.value.trackingState == TrackingState.TRACKING
        }

        session.scene.mainPanelEntity.setEnabled(false)

        val activitySpace = session.scene.activitySpace
        val perceptionSpace = session.scene.perceptionSpace
        val headScenePose =
            perceptionSpace.getScenePoseFromPerceptionPose(arDevice.state.value.devicePose)
        val panelPoseAheadOfHead =
            headScenePose.transformPoseTo(
                Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
                activitySpace,
            )

        val targetPanel =
            createTestPanel(
                name = "head_tracked_hit_panel",
                size = FloatSize2d(2.0f, 2.0f),
                pose = panelPoseAheadOfHead,
                parent = activitySpace,
            )

        val headHit =
            waitForHitTest(
                description = "ArDevice ScenePose ray to hit panel",
                scenePose = headScenePose,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection()
            }!!

        assertThat(headHit.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(headHit.distance.isFinite()).isTrue()
        assertThat(headHit.distance).isGreaterThan(0f)
        assertVector3(headHit.hitPosition, Vector3(0f, 0f, -2.0f), TRACKED_TOLERANCE)
        assertThat(headHit.surfaceNormal).isNotNull()
        assertThat(headHit.surfaceNormal!!.dot(Vector3(0f, 0f, 1f))).isGreaterThan(0.9f)

        val xrDevice = XrDevice.getCurrentDevice(sessionRule.activity)
        val viewpoint =
            when {
                xrDevice.isRenderingModeSupported(RenderingMode.STEREO) ->
                    RenderViewpoint.left(session)
                xrDevice.isRenderingModeSupported(RenderingMode.MONO) ->
                    RenderViewpoint.mono(session)
                else -> null
            }
        assertWithMessage("Expected STEREO or MONO RenderViewpoint on XR device")
            .that(viewpoint)
            .isNotNull()

        val viewpointScenePose =
            perceptionSpace.getScenePoseFromPerceptionPose(viewpoint!!.state.value.pose)
        val viewpointPanelPose =
            viewpointScenePose.transformPoseTo(
                Pose(Vector3(0f, 0f, -1.5f), Quaternion.Identity),
                activitySpace,
            )
        targetPanel.setPose(viewpointPanelPose, Space.ACTIVITY)

        val viewpointHit =
            waitForHitTest(
                description = "RenderViewpoint ScenePose ray to hit repositioned panel",
                scenePose = viewpointScenePose,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection() &&
                    kotlin.math.abs(it!!.hitPosition.z - (-1.5f)) < TRACKED_TOLERANCE
            }!!

        assertThat(viewpointHit.surfaceType).isEqualTo(HitTestResult.SurfaceType.PLANE)
        assertThat(viewpointHit.distance.isFinite()).isTrue()
        assertThat(viewpointHit.distance).isGreaterThan(0f)
        assertThat(viewpointHit.distance).isLessThan(headHit.distance)
        assertVector3(viewpointHit.hitPosition, Vector3(0f, 0f, -1.5f), TRACKED_TOLERANCE)
        assertThat(viewpointHit.surfaceNormal).isNotNull()
        assertThat(viewpointHit.surfaceNormal!!.dot(Vector3(0f, 0f, 1f))).isGreaterThan(0.9f)
    }

    @Test
    fun activitySpace_hitTest_gltfModelEntityWithCollider_returnsObjectHit() {
        val session = sessionRule.session
        val activitySpace = session.scene.activitySpace
        session.scene.mainPanelEntity.setEnabled(false)

        val gltfEntity = sessionRule.onMainThread {
            val model = GltfModel.create(session, Paths.get("models", "BoundingBoxBlue.glb"))
            GltfModelEntity.create(
                session,
                model,
                Pose(Vector3(0f, 0f, -2.5f), Quaternion.Identity),
                parent = activitySpace,
            )
        }

        // BoundingBoxBlue.glb is a 1m cube centered at local Vector3.Zero.
        val targetCenterInActivity = gltfEntity.transformPositionTo(Vector3.Zero, activitySpace)
        val rayDir = targetCenterInActivity.toNormalized()

        // Attaching InteractableComponent enables the Impress collider on the glTF model.
        // Impress JNI nSet*ColliderEnabled requires main-thread node-ID affinity.
        val interactable = InteractableComponent.create(session, Executor { it.run() }) {}
        val interactableAdded = sessionRule.onMainThread { gltfEntity.addComponent(interactable) }
        assertThat(interactableAdded).isTrue()

        val hitWithCollider =
            waitForHitTest(
                description = "ray to hit GltfModelEntity after collider enabled",
                scenePose = activitySpace,
                origin = Vector3.Zero,
                direction = rayDir,
            ) {
                it.isIntersection()
            }!!

        assertThat(hitWithCollider.surfaceType).isEqualTo(HitTestResult.SurfaceType.OBJECT)
        assertThat(hitWithCollider.distance.isFinite()).isTrue()
        assertThat(hitWithCollider.distance).isGreaterThan(0f)
        val hitPos = hitWithCollider.hitPosition
        assertThat(hitPos.length).isAtLeast(1.5f)
        assertThat(hitPos.length).isAtMost(3.0f)
        assertVector3(hitPos.toNormalized(), rayDir, NORMAL_TOLERANCE)
        assertThat(hitWithCollider.surfaceNormal).isNotNull()
        val normal = hitWithCollider.surfaceNormal!!
        assertThat(normal.length).isWithin(NORMAL_TOLERANCE).of(1.0f)
        assertThat(normal.dot(-rayDir)).isAtLeast(0f)
    }

    @Test
    fun activitySpace_hitTest_panelEntity_returnsUnscaledMetricDistance() {
        val session = sessionRule.session
        session.scene.mainPanelEntity.setEnabled(false)

        createTestPanel(
            name = "metric_distance_panel",
            size = FloatSize2d(1.0f, 1.0f),
            pose = Pose(Vector3(0f, 0f, -2.0f), Quaternion.Identity),
            parent = session.scene.activitySpace,
        )

        val hit =
            waitForHitTest(
                description = "center ray to hit PanelEntity at z=-2.0",
                scenePose = session.scene.activitySpace,
                origin = Vector3.Zero,
                direction = Vector3(0f, 0f, -1f),
            ) {
                it.isIntersection()
            }!!

        assertThat(hit.distance).isWithin(POSITION_TOLERANCE).of(2.0f)
    }

    private fun createTestPanel(
        name: String,
        size: FloatSize2d,
        pose: Pose,
        parent: Entity,
    ): PanelEntity = sessionRule.onMainThread {
        val view = TextView(sessionRule.activity).apply { text = name }
        PanelEntity.create(sessionRule.session, view, size, name, pose = pose, parent = parent)
    }

    private fun waitForHitTest(
        description: String,
        scenePose: ScenePose,
        origin: Vector3,
        direction: Vector3,
        hitTestFilter: Int = ScenePose.HitTestFilter.SELF_SCENE,
        predicate: (HitTestResult?) -> Boolean,
    ): HitTestResult? {
        var latestResult: HitTestResult? = null
        waitUntil(
            description = description,
            timeout = 5000.milliseconds,
            pollInterval = 50.milliseconds,
        ) {
            latestResult = hitTestBlocking(scenePose, origin, direction, hitTestFilter)
            predicate(latestResult)
        }
        return latestResult
    }

    private fun hitTestBlocking(
        scenePose: ScenePose,
        origin: Vector3,
        direction: Vector3,
        hitTestFilter: Int? = ScenePose.HitTestFilter.SELF_SCENE,
    ): HitTestResult? = runBlocking {
        if (hitTestFilter == null) {
            scenePose.hitTest(origin, direction)
        } else {
            scenePose.hitTest(origin, direction, hitTestFilter)
        }
    }

    private fun hitTestDefaultOverloadBlocking(
        scenePose: ScenePose,
        origin: Vector3,
        direction: Vector3,
    ): HitTestResult? =
        hitTestBlocking(
            scenePose = scenePose,
            origin = origin,
            direction = direction,
            hitTestFilter = null,
        )

    private fun HitTestResult?.isIntersection(): Boolean =
        this != null && distance.isFinite() && surfaceType != HitTestResult.SurfaceType.UNKNOWN

    private companion object {
        const val POSITION_TOLERANCE = 1e-2f
        const val NORMAL_TOLERANCE = 1e-2f
        const val TRACKED_TOLERANCE = 5e-2f
        const val MAIN_PANEL_HIT_TOLERANCE = 5e-2f
        const val GLTF_SUBSPACE_PREFIX = "gltf_entity_subspace_"
    }
}
