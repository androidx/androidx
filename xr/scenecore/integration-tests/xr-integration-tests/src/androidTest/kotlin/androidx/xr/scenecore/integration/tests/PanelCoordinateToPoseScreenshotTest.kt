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

import android.graphics.Color as AndroidColor
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.FakeHeadTrackingRule
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitForSpatialScene
import java.nio.file.Paths
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device 6DoF head-gaze golden screenshot tests for
 * [PanelEntity.transformPixelCoordinatesToLocalPosition] and
 * [PanelEntity.transformNormalizedCoordinatesToLocalPosition] (b/573232780).
 *
 * Places an off-center spatial panel (`30°` right of the identity `-Z` gaze axis) with a child
 * `models/xyzArrows.glb` [GltfModelEntity] positioned via 2D-to-3D panel coordinate transforms,
 * rotates simulated 6DoF head gaze via [FakeHeadTrackingRule] to look directly at the panel, and
 * asserts the auto-cropped panel + 3D widget rendering against golden images.
 */
@RunWith(AndroidJUnit4::class)
@XrDeviceTest
@LargeTest
class PanelCoordinateToPoseScreenshotTest {

    @get:Rule(order = 0) val fakeHeadTrackingRule = FakeHeadTrackingRule()

    @get:Rule(order = 1) val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    @get:Rule(order = 2) val sessionRule = createXrSessionRule()

    @get:Rule val globalTimeout: Timeout = Timeout.seconds(60)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
        val scene = sessionRule.session.scene
        scene.mainPanelEntity.setEnabled(false)

        sessionRule.onMainThread {
            val studioLightingIbl =
                ImageBasedLightingAsset.createFromZip(session, STUDIO_LIGHTING_IBL_PATH)
            scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
            scene.spatialEnvironment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(studioLightingIbl, null)
        }

        sessionRule.waitForSpatialEnvironmentSettled()
    }

    @Test
    fun panelCoordinate_pixelTopLeft_matchesGolden() {
        renderXyzWidgetAtPanelCoordinateAndAssertGolden(
            goldenIdentifier = "panel_coordinate_xyz_pixel_top_left"
        ) { panel ->
            panel.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f))
        }
    }

    @Test
    fun panelCoordinate_pixelBottomRight_matchesGolden() {
        renderXyzWidgetAtPanelCoordinateAndAssertGolden(
            goldenIdentifier = "panel_coordinate_xyz_pixel_bottom_right"
        ) { panel ->
            panel.transformPixelCoordinatesToLocalPosition(
                Vector2(INITIAL_PANEL_PIXELS.width.toFloat(), INITIAL_PANEL_PIXELS.height.toFloat())
            )
        }
    }

    @Test
    fun panelCoordinate_normalizedCenter_matchesGolden() {
        renderXyzWidgetAtPanelCoordinateAndAssertGolden(
            goldenIdentifier = "panel_coordinate_xyz_normalized_center"
        ) { panel ->
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f))
        }
    }

    @Test
    fun panelCoordinate_normalizedBottomLeft_matchesGolden() {
        renderXyzWidgetAtPanelCoordinateAndAssertGolden(
            goldenIdentifier = "panel_coordinate_xyz_normalized_bottom_left"
        ) { panel ->
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, -1f))
        }
    }

    private fun renderXyzWidgetAtPanelCoordinateAndAssertGolden(
        goldenIdentifier: String,
        computeLocalPosition: (PanelEntity) -> Vector3,
    ) {
        sessionRule.onMainThread {
            val xyzModel = GltfModel.create(session, XYZ_ARROWS_MODEL_PATH)
            val contentView = View(activity).apply { setBackgroundColor(AndroidColor.DKGRAY) }
            val panel =
                PanelEntity.create(
                    session = session,
                    view = contentView,
                    pixelDimensions = INITIAL_PANEL_PIXELS,
                    name = PANEL_NAME,
                    pose = OFF_CENTER_PANEL_POSE,
                    parent = session.scene.activitySpace,
                )
            GltfModelEntity.create(
                    session = session,
                    model = xyzModel,
                    pose = Pose(computeLocalPosition(panel)),
                    parent = panel,
                )
                .apply {
                    loadNodesBeforeTeardown()
                    setScale(XYZ_WIDGET_SCALE)
                }
        }

        val panelNode = awaitVisiblePanelAndGltfSubspace()

        fakeHeadTrackingRule.performHeadMovement {
            lookAt(panelNode, alignment = SpatialAlignment.Center)
        }

        sessionRule
            .captureToBitmap(padding = CROP_PADDING_PX, autoCrop = true)
            .assertAgainstGolden(
                screenshotRule,
                goldenIdentifier,
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    private fun awaitVisiblePanelAndGltfSubspace(): SpatialNode =
        checkNotNull(
            waitForSpatialScene(
                description = "off-center panel '$PANEL_NAME' and glTF subspace to become visible",
                timeout = SCENE_TIMEOUT,
                pollInterval = POLL_INTERVAL,
                select = { spatialScene ->
                    val task = spatialScene.findTaskByActivity(sessionRule.activity)
                    val leash = task?.embeddedWindowLeashes?.firstOrNull { it.name == PANEL_NAME }
                    if (
                        task != null &&
                            task.isFullSpace &&
                            task.taskWindowLeash?.isVisible != true &&
                            task.subspaceNodes.count { it.isVisible } == 1 &&
                            leash != null &&
                            leash.isVisible &&
                            leash.worldExtents != null
                    ) {
                        leash
                    } else {
                        null
                    }
                },
                until = { it != null },
            )
        )

    private companion object {
        const val PANEL_NAME = "screenshotCoordPanel"
        const val XYZ_WIDGET_SCALE = 0.18f
        const val CROP_PADDING_PX = 16
        val INITIAL_PANEL_PIXELS = IntSize2d(1000, 1000)
        // Placed 1.6m away, 30 degrees right of the identity (-Z) gaze axis, rotated -30 degrees
        // around +Y so its front (+Z) normal faces the origin squarely once head gaze turns to it.
        val OFF_CENTER_PANEL_POSE =
            Pose(
                translation = Vector3(0.8f, 0f, -1.3856f),
                rotation = Quaternion.fromAxisAngle(Vector3.Up, -30f),
            )
        val STUDIO_LIGHTING_IBL_PATH = Paths.get("skyboxes", "studio_lighting_ibl.zip")
        val XYZ_ARROWS_MODEL_PATH = Paths.get("models", "xyzArrows.glb")
        val SCENE_TIMEOUT = 5.seconds
        val POLL_INTERVAL = 100.milliseconds
    }
}
