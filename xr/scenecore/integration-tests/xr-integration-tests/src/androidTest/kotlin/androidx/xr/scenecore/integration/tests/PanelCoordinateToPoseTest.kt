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
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialAlignment
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device API, state, and interactive spatial-resize integration tests for
 * [PanelEntity.transformPixelCoordinatesToLocalPosition] and
 * [PanelEntity.transformNormalizedCoordinatesToLocalPosition] (b/573232780):
 * - Pixel and normalized coordinate transforms across corners, edges, and center against
 *   [Scene.virtualPixelDensity].
 * - Coordinate recomputation and child relative positioning across programmatic panel resizes
 *   ([IntSize2d] and [FloatSize2d]) and hierarchical scale changes.
 * - Linear extrapolation for out-of-bounds and sub-pixel coordinates.
 * - Coordinate transforms on [Scene.mainPanelEntity] and [ActivityPanelEntity].
 * - Full interactive CUJ flows matching `PanelCoordinateActivity` (`models/xyzArrows.glb`
 *   [GltfModelEntity], [MovableComponent], continuous [ResizableComponent] listener, top-right
 *   corner drag widening and multi-stage resizes, mode switching, and post-gesture scaling).
 *
 * Tests run on the instrumentation thread. Only panel creation and resizing, glTF calls, and reads
 * that must not interleave with resize callbacks use [XrSessionRule.onMainThread].
 */
@RunWith(AndroidJUnit4::class)
@XrDeviceTest
@LargeTest
class PanelCoordinateToPoseTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @get:Rule val globalTimeout: Timeout = Timeout.seconds(60)

    private val scene: Scene
        get() = sessionRule.session.scene

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun panelCoordinate_pixelCornersAndCenter_mapToPanelExtents() {
        scene.mainPanelEntity.setEnabled(false)
        val panel =
            createSpatialPanel(
                pixelDimensions = INITIAL_PANEL_PIXELS,
                name = "pixelCoordPanel",
                pose = SECONDARY_PANEL_POSE,
                parent = scene.activitySpace,
            )

        assertThat(panel.sizeInPixels).isEqualTo(INITIAL_PANEL_PIXELS)
        val fullWidthPx = INITIAL_PANEL_PIXELS.width.toFloat()
        val fullHeightPx = INITIAL_PANEL_PIXELS.height.toFloat()
        val density = scene.virtualPixelDensity
        val expectedWidth = density.convertPixelsToMeters(fullWidthPx)
        val expectedHeight = density.convertPixelsToMeters(fullHeightPx)
        assertThat(panel.size.width).isWithin(LOCAL_EPSILON).of(expectedWidth)
        assertThat(panel.size.height).isWithin(LOCAL_EPSILON).of(expectedHeight)

        val halfW = expectedWidth / 2f
        val halfH = expectedHeight / 2f

        // Top-left (0, 0) -> (-w/2, +h/2, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f)),
            Vector3(-halfW, halfH, 0f),
            LOCAL_EPSILON,
        )
        // Top-right (width, 0) -> (+w/2, +h/2, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(Vector2(fullWidthPx, 0f)),
            Vector3(halfW, halfH, 0f),
            LOCAL_EPSILON,
        )
        // Bottom-right (width, height) -> (+w/2, -h/2, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(Vector2(fullWidthPx, fullHeightPx)),
            Vector3(halfW, -halfH, 0f),
            LOCAL_EPSILON,
        )
        // Bottom-left (0, height) -> (-w/2, -h/2, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(Vector2(0f, fullHeightPx)),
            Vector3(-halfW, -halfH, 0f),
            LOCAL_EPSILON,
        )
        // Center (width/2, height/2) -> (0, 0, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(
                Vector2(fullWidthPx / 2f, fullHeightPx / 2f)
            ),
            Vector3.Zero,
            LOCAL_EPSILON,
        )
    }

    @Test
    fun panelCoordinate_normalizedCenterEdgeAndCorners_mapToPanelExtents() {
        scene.mainPanelEntity.setEnabled(false)
        val panel =
            createSpatialPanel(
                pixelDimensions = INITIAL_PANEL_PIXELS,
                name = "normalizedCoordPanel",
                pose = SECONDARY_PANEL_POSE,
                parent = scene.activitySpace,
            )

        val halfW = panel.size.width / 2f
        val halfH = panel.size.height / 2f

        // Center (0, 0) -> (0, 0, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f)),
            Vector3.Zero,
            LOCAL_EPSILON,
        )
        // Right edge center (1, 0) -> (+w/2, 0, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, 0f)),
            Vector3(halfW, 0f, 0f),
            LOCAL_EPSILON,
        )
        // Top-right corner (1, 1) -> (+w/2, +h/2, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, 1f)),
            Vector3(halfW, halfH, 0f),
            LOCAL_EPSILON,
        )
        // Bottom-left corner (-1, -1) -> (-w/2, -h/2, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, -1f)),
            Vector3(-halfW, -halfH, 0f),
            LOCAL_EPSILON,
        )
        // Top-left corner (-1, 1) -> (-w/2, +h/2, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, 1f)),
            Vector3(-halfW, halfH, 0f),
            LOCAL_EPSILON,
        )
        // Bottom-right corner (1, -1) -> (+w/2, -h/2, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, -1f)),
            Vector3(halfW, -halfH, 0f),
            LOCAL_EPSILON,
        )
    }

    @Test
    fun panelCoordinate_pixelResizeAndScale_updatesAndPreservesRelativeChildPosition() {
        val panel =
            createSpatialPanel(
                pixelDimensions = INITIAL_PANEL_PIXELS,
                name = "pixelResizeScalePanel",
                pose = SECONDARY_PANEL_POSE,
                parent = scene.mainPanelEntity,
            )
        val marker = Entity.create(sessionRule.session, name = "pixelMarker", parent = panel)
        val fixedPixel =
            Vector2(INITIAL_PANEL_PIXELS.width.toFloat(), INITIAL_PANEL_PIXELS.height.toFloat())

        // 1. On initial panel, (width, height) sits at bottom-right (+w/2, -h/2, 0).
        marker.setPose(Pose(panel.transformPixelCoordinatesToLocalPosition(fixedPixel)))
        assertMarkerNormalizedOffsetOnPanel(
            marker = marker,
            panel = panel,
            expectedU = 1f,
            expectedV = -1f,
        )

        // 2. Widen panel to 2x width and recompute pose: fixedPixel moves to bottom-center.
        sessionRule.onMainThread { panel.sizeInPixels = WIDENED_PANEL_PIXELS }
        assertThat(panel.sizeInPixels).isEqualTo(WIDENED_PANEL_PIXELS)
        val posBeforeScale = panel.transformPixelCoordinatesToLocalPosition(fixedPixel)
        marker.setPose(Pose(posBeforeScale))

        assertVector3(
            marker.getPose(Space.PARENT).translation,
            Vector3(0f, -panel.size.height / 2f, 0f),
            LOCAL_EPSILON,
        )
        assertMarkerNormalizedOffsetOnPanel(
            marker = marker,
            panel = panel,
            expectedU = 0f,
            expectedV = -1f,
        )
        val activityOffsetBeforeScale =
            marker.getPose(Space.ACTIVITY).translation - panel.getPose(Space.ACTIVITY).translation

        // 3. Scale panel to 2.0f: local coordinate output is unchanged, ActivitySpace offset
        // doubles, and child stays at the center of the bottom edge (0, -1).
        panel.setScale(2.0f)
        val posAfterScale = panel.transformPixelCoordinatesToLocalPosition(fixedPixel)
        assertVector3(posAfterScale, posBeforeScale, LOCAL_EPSILON)
        marker.setPose(Pose(posAfterScale))

        val activityOffsetAfterScale =
            marker.getPose(Space.ACTIVITY).translation - panel.getPose(Space.ACTIVITY).translation
        assertVector3(activityOffsetAfterScale, activityOffsetBeforeScale * 2.0f, ACTIVITY_EPSILON)
        assertMarkerNormalizedOffsetOnPanel(
            marker = marker,
            panel = panel,
            expectedU = 0f,
            expectedV = -1f,
        )
    }

    @Test
    fun panelCoordinate_normalizedResizeAndScale_preservesBottomLeftChildPosition() {
        val panel =
            createSpatialPanel(
                pixelDimensions = INITIAL_PANEL_PIXELS,
                name = "normalizedResizeScalePanel",
                pose = SECONDARY_PANEL_POSE,
                parent = scene.mainPanelEntity,
            )
        val marker = Entity.create(sessionRule.session, name = "normalizedMarker", parent = panel)
        val bottomLeftUv = Vector2(-1f, -1f)

        marker.setPose(Pose(panel.transformNormalizedCoordinatesToLocalPosition(bottomLeftUv)))
        assertMarkerNormalizedOffsetOnPanel(
            marker = marker,
            panel = panel,
            expectedU = -1f,
            expectedV = -1f,
        )

        // Repeatedly resize in pixels and in local units (matching PanelCoordinateActivity
        // resize listener) and verify (-1, -1) stays at the bottom-left corner.
        for (targetPixels in
            listOf(WIDENED_PANEL_PIXELS, IntSize2d(600, 1400), IntSize2d(1280, 720))) {
            sessionRule.onMainThread { panel.sizeInPixels = targetPixels }
            assertThat(panel.sizeInPixels).isEqualTo(targetPixels)
            marker.setPose(Pose(panel.transformNormalizedCoordinatesToLocalPosition(bottomLeftUv)))
            assertVector3(
                marker.getPose(Space.PARENT).translation,
                Vector3(-panel.size.width / 2f, -panel.size.height / 2f, 0f),
                LOCAL_EPSILON,
            )
            assertMarkerNormalizedOffsetOnPanel(
                marker = marker,
                panel = panel,
                expectedU = -1f,
                expectedV = -1f,
            )
        }

        sessionRule.onMainThread { panel.size = FloatSize2d(0.8f, 0.5f) }
        marker.setPose(Pose(panel.transformNormalizedCoordinatesToLocalPosition(bottomLeftUv)))
        assertVector3(
            marker.getPose(Space.PARENT).translation,
            Vector3(-panel.size.width / 2f, -panel.size.height / 2f, 0f),
            LOCAL_EPSILON,
        )

        // Change scale across multiple values: child remains at (-1, -1) relative to panel.
        for (scale in listOf(0.5f, 2.0f)) {
            panel.setScale(scale)
            marker.setPose(Pose(panel.transformNormalizedCoordinatesToLocalPosition(bottomLeftUv)))
            assertMarkerNormalizedOffsetOnPanel(
                marker = marker,
                panel = panel,
                expectedU = -1f,
                expectedV = -1f,
            )
        }
    }

    @Test
    fun panelCoordinate_outOfBoundsCoordinates_extrapolateLinearlyWithZeroZ() {
        scene.mainPanelEntity.setEnabled(false)
        val panel =
            createSpatialPanel(
                pixelDimensions = INITIAL_PANEL_PIXELS,
                name = "extrapolationPanel",
                pose = SECONDARY_PANEL_POSE,
                parent = scene.activitySpace,
            )

        val fullWidthPx = INITIAL_PANEL_PIXELS.width.toFloat()
        val fullHeightPx = INITIAL_PANEL_PIXELS.height.toFloat()
        val width = panel.size.width
        val height = panel.size.height

        // Pixel (-0.5*w, 1.5*h) -> u = -2, v = -2 -> (-width, -height, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(
                Vector2(-0.5f * fullWidthPx, 1.5f * fullHeightPx)
            ),
            Vector3(-width, -height, 0f),
            LOCAL_EPSILON,
        )
        // Pixel (2*w, -h) -> u = +3, v = +3 -> (+1.5*width, +1.5*height, 0)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(
                Vector2(2f * fullWidthPx, -fullHeightPx)
            ),
            Vector3(1.5f * width, 1.5f * height, 0f),
            LOCAL_EPSILON,
        )
        // Sub-pixel float coordinate
        val subPixelX = 0.2505f * fullWidthPx
        val subPixelY = 0.75025f * fullHeightPx
        val expectedSubPixelX = ((subPixelX / fullWidthPx) * 2f - 1f) * (width / 2f)
        val expectedSubPixelY = ((1f - subPixelY / fullHeightPx) * 2f - 1f) * (height / 2f)
        assertVector3(
            panel.transformPixelCoordinatesToLocalPosition(Vector2(subPixelX, subPixelY)),
            Vector3(expectedSubPixelX, expectedSubPixelY, 0f),
            LOCAL_EPSILON,
        )

        // Normalized (-2, 2) -> (-width, +height, 0) and (2, -2) -> (+width, -height, 0)
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(-2f, 2f)),
            Vector3(-width, height, 0f),
            LOCAL_EPSILON,
        )
        assertVector3(
            panel.transformNormalizedCoordinatesToLocalPosition(Vector2(2f, -2f)),
            Vector3(width, -height, 0f),
            LOCAL_EPSILON,
        )
    }

    @Test
    fun panelCoordinate_mainPanel_matchesWindowExtents() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val mainPanel = scene.mainPanelEntity
        val mainPixels = mainPanel.sizeInPixels
        assertThat(mainPixels.width).isGreaterThan(0)
        assertThat(mainPixels.height).isGreaterThan(0)
        val mainHalfW = mainPanel.size.width / 2f
        val mainHalfH = mainPanel.size.height / 2f

        assertVector3(
            mainPanel.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f)),
            Vector3(-mainHalfW, mainHalfH, 0f),
            LOCAL_EPSILON,
        )
        assertVector3(
            mainPanel.transformPixelCoordinatesToLocalPosition(
                Vector2(mainPixels.width.toFloat(), mainPixels.height.toFloat())
            ),
            Vector3(mainHalfW, -mainHalfH, 0f),
            LOCAL_EPSILON,
        )
        assertVector3(
            mainPanel.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f)),
            Vector3.Zero,
            LOCAL_EPSILON,
        )
        assertVector3(
            mainPanel.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, 1f)),
            Vector3(mainHalfW, mainHalfH, 0f),
            LOCAL_EPSILON,
        )
    }

    @Test
    fun panelCoordinate_activityPanel_matchesExtentsAndRecomputesAfterResize() {
        assumeTrue(
            "Requires SpatialCapability.EMBED_ACTIVITY",
            scene.spatialCapabilities.contains(SpatialCapability.EMBED_ACTIVITY),
        )

        scene.mainPanelEntity.setEnabled(false)
        val activityPanel = sessionRule.onMainThread {
            ActivityPanelEntity.create(
                session = session,
                pixelDimensions = ACTIVITY_PANEL_PIXELS,
                name = "coordActivityPanel",
                pose = Pose(Vector3(0f, 0f, -1.2f)),
                parent = scene.activitySpace,
            )
        }

        val initialWidthPx = ACTIVITY_PANEL_PIXELS.width.toFloat()
        val initialHeightPx = ACTIVITY_PANEL_PIXELS.height.toFloat()
        val density = scene.virtualPixelDensity
        val actHalfW = density.convertPixelsToMeters(initialWidthPx) / 2f
        val actHalfH = density.convertPixelsToMeters(initialHeightPx) / 2f

        assertVector3(
            activityPanel.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f)),
            Vector3(-actHalfW, actHalfH, 0f),
            LOCAL_EPSILON,
        )
        assertVector3(
            activityPanel.transformPixelCoordinatesToLocalPosition(
                Vector2(initialWidthPx, initialHeightPx)
            ),
            Vector3(actHalfW, -actHalfH, 0f),
            LOCAL_EPSILON,
        )
        assertVector3(
            activityPanel.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, -1f)),
            Vector3(-actHalfW, -actHalfH, 0f),
            LOCAL_EPSILON,
        )

        sessionRule.onMainThread {
            activityPanel.sizeInPixels =
                IntSize2d(ACTIVITY_PANEL_PIXELS.width * 2, ACTIVITY_PANEL_PIXELS.height)
        }
        assertVector3(
            activityPanel.transformPixelCoordinatesToLocalPosition(
                Vector2(initialWidthPx, initialHeightPx)
            ),
            Vector3(0f, -actHalfH, 0f),
            LOCAL_EPSILON,
        )
    }

    /**
     * CUJ Validations 3–5: places a `models/xyzArrows.glb` [GltfModelEntity] widget on a movable,
     * resizable panel and moves it across pixel `(0, 0)`, `(width, 0)`, and `(width, height)`.
     */
    @Test
    fun panelCoordinate_pixelMode_xyzWidgetFollowsCornerMoves() {
        val fullWidthPx = INITIAL_PANEL_PIXELS.width.toFloat()
        val fullHeightPx = INITIAL_PANEL_PIXELS.height.toFloat()
        var activePixel = Vector2(0f, 0f)
        val harness =
            createInteractiveWidgetPanel("cujPixelCornersPanel") { panel ->
                panel.transformPixelCoordinatesToLocalPosition(activePixel)
            }

        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = -1f,
            expectedV = 1f,
        )

        activePixel = Vector2(fullWidthPx, 0f)
        harness.updateWidgetPose()
        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = 1f,
            expectedV = 1f,
        )

        activePixel = Vector2(fullWidthPx, fullHeightPx)
        harness.updateWidgetPose()
        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = 1f,
            expectedV = -1f,
        )
    }

    /**
     * CUJ Validations 6–7: widens a panel via an injected `TopRight` resize drag while checking
     * that a pixel `(width, height)` widget moves inward along the bottom edge, then scales the
     * resized panel by `2.0f`.
     */
    @Test
    fun panelCoordinate_pixelMode_topRightResizeAndScale_updatesWidget() {
        val panelName = "cujPixelResizePanel"
        val activePixel =
            Vector2(INITIAL_PANEL_PIXELS.width.toFloat(), INITIAL_PANEL_PIXELS.height.toFloat())
        val harness =
            createInteractiveWidgetPanel(panelName) { panel ->
                panel.transformPixelCoordinatesToLocalPosition(activePixel)
            }

        val panelNode = awaitResizablePanelNode(sessionRule.activity, panelName)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val initialLocalWidth = harness.panel.size.width

        // Grab the top-right resize affordance and drag horizontally right by half the panel's
        // local width so symmetric CPM resizing widens the panel toward ~2x width.
        panelNode.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(initialLocalWidth / 2f, 0f, 0f),
                alignment = SpatialAlignment.TopRight,
                marginMeters = RESIZE_AFFORDANCE_MARGIN_METERS,
            )
        }

        // The resize listener updates the panel and widget before it counts END, so their state
        // is final once END is observed.
        waitUntil(
            description = "ResizableComponent ResizeState.END after TopRight horizontal widen",
            timeout = INTERACTION_TIMEOUT,
        ) {
            harness.resizeEndCount.get() >= 1
        }
        waitUntil(
            description = "panel '$panelName' width to grow beyond ${INITIAL_PANEL_PIXELS.width}",
            timeout = POLL_TIMEOUT,
        ) {
            harness.panel.sizeInPixels.width > INITIAL_PANEL_PIXELS.width
        }

        val widenedPixels = harness.panel.sizeInPixels
        assertThat(widenedPixels.width)
            .isGreaterThan((INITIAL_PANEL_PIXELS.width * MIN_WIDENED_WIDTH_RATIO).toInt())
        assertThat(abs(widenedPixels.height - INITIAL_PANEL_PIXELS.height))
            .isLessThan(MAX_HEIGHT_DRIFT_PX)

        val expectedU = (activePixel.x / widenedPixels.width.toFloat()) * 2f - 1f
        val expectedV = (1f - activePixel.y / widenedPixels.height.toFloat()) * 2f - 1f
        assertMarkerNormalizedOffsetOnPanel(
            marker = harness.xyzEntity,
            panel = harness.panel,
            expectedU = expectedU,
            expectedV = expectedV,
        )

        assertScaleDoublesActivityOffsetAndPreservesRelativePosition(
            panel = harness.panel,
            xyzEntity = harness.xyzEntity,
            expectedU = expectedU,
            expectedV = expectedV,
        ) {
            harness.updateWidgetPose()
        }
    }

    /**
     * CUJ Validations 8–12: switches a live `models/xyzArrows.glb` widget from Pixel `(width,
     * height)` to Normalized coordinates across `(0, 0)`, `(1, 0)`, `(1, 1)`, and `(-1, -1)`.
     */
    @Test
    fun panelCoordinate_modeSwitch_pixelToNormalizedUpdatesWidget() {
        val activePixel =
            Vector2(INITIAL_PANEL_PIXELS.width.toFloat(), INITIAL_PANEL_PIXELS.height.toFloat())
        var usePixelMode = true
        var activeUv = Vector2(0f, 0f)
        val harness =
            createInteractiveWidgetPanel("cujModeSwitchPanel") { panel ->
                if (usePixelMode) {
                    panel.transformPixelCoordinatesToLocalPosition(activePixel)
                } else {
                    panel.transformNormalizedCoordinatesToLocalPosition(activeUv)
                }
            }

        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = 1f,
            expectedV = -1f,
        )

        usePixelMode = false
        activeUv = Vector2(0f, 0f)
        harness.updateWidgetPose()
        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = 0f,
            expectedV = 0f,
        )

        activeUv = Vector2(1f, 0f)
        harness.updateWidgetPose()
        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = 1f,
            expectedV = 0f,
        )

        activeUv = Vector2(1f, 1f)
        harness.updateWidgetPose()
        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = 1f,
            expectedV = 1f,
        )

        activeUv = Vector2(-1f, -1f)
        harness.updateWidgetPose()
        assertMarkerNormalizedOffsetOnPanel(
            harness.xyzEntity,
            harness.panel,
            expectedU = -1f,
            expectedV = -1f,
        )
    }

    /**
     * CUJ Validations 13–14: drags the `TopRight` resize affordance across two distinct pixel sizes
     * while verifying `(-1, -1)` remains pinned at bottom-left, then scales to `2.0f`.
     */
    @Test
    fun panelCoordinate_normalizedMode_multiStageTopRightResizesAndScale_pinsBottomLeft() {
        val panelName = "cujNormalizedCoordPanel"
        val harness =
            createInteractiveWidgetPanel(panelName) { panel ->
                panel.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, -1f))
            }
        assertMarkerNormalizedOffsetOnPanel(
            marker = harness.xyzEntity,
            panel = harness.panel,
            expectedU = -1f,
            expectedV = -1f,
        )

        val panelNode = awaitResizablePanelNode(sessionRule.activity, panelName)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val sizeBeforeResize = harness.panel.sizeInPixels
        lateinit var sizeAfterFirstStep: IntSize2d

        panelNode.performSpatialInteraction {
            pointAtResizeAffordance(
                alignment = SpatialAlignment.TopRight,
                marginMeters = RESIZE_AFFORDANCE_MARGIN_METERS,
            )
            press()

            // Resize #1: expand to first target size and verify (-1, -1) bottom-left pinning.
            animate { fraction ->
                pointAtResizeAffordance(
                    offset = FIRST_RESIZE_LOCAL_DELTA * fraction,
                    alignment = SpatialAlignment.TopRight,
                    marginMeters = RESIZE_AFFORDANCE_MARGIN_METERS,
                )
            }
            // The drag is still active, so read on the main thread to avoid interleaving with a
            // resize callback.
            waitUntil(
                description = "panel '$panelName' sizeInPixels to grow beyond $sizeBeforeResize",
                timeout = POLL_TIMEOUT,
            ) {
                sessionRule.onMainThread {
                    val size = harness.panel.sizeInPixels
                    size.width > sizeBeforeResize.width && size.height > sizeBeforeResize.height
                }
            }
            sizeAfterFirstStep = sessionRule.onMainThread {
                assertMarkerNormalizedOffsetOnPanel(
                    marker = harness.xyzEntity,
                    panel = harness.panel,
                    expectedU = -1f,
                    expectedV = -1f,
                )
                harness.panel.sizeInPixels
            }

            // Resize #2: continue dragging TopRight to a second distinct pixel size.
            val secondDeltaStep = SECOND_RESIZE_LOCAL_DELTA - FIRST_RESIZE_LOCAL_DELTA
            animate { fraction ->
                pointAtResizeAffordance(
                    offset = FIRST_RESIZE_LOCAL_DELTA + secondDeltaStep * fraction,
                    alignment = SpatialAlignment.TopRight,
                    marginMeters = RESIZE_AFFORDANCE_MARGIN_METERS,
                )
            }
            release()
        }

        // The resize listener updates the panel and widget before it counts END, so their state
        // is final once END is observed.
        waitUntil(
            description = "TopRight multi-stage resize ResizeState.END",
            timeout = INTERACTION_TIMEOUT,
        ) {
            harness.resizeEndCount.get() >= 1
        }
        waitUntil(
            description = "panel '$panelName' sizeInPixels to grow beyond $sizeAfterFirstStep",
            timeout = POLL_TIMEOUT,
        ) {
            val size = harness.panel.sizeInPixels
            size.width > sizeAfterFirstStep.width && size.height > sizeAfterFirstStep.height
        }

        assertMarkerNormalizedOffsetOnPanel(
            marker = harness.xyzEntity,
            panel = harness.panel,
            expectedU = -1f,
            expectedV = -1f,
        )
        assertScaleDoublesActivityOffsetAndPreservesRelativePosition(
            panel = harness.panel,
            xyzEntity = harness.xyzEntity,
            expectedU = -1f,
            expectedV = -1f,
        ) {
            harness.updateWidgetPose()
        }
    }

    private class InteractiveWidgetHarness(
        val panel: PanelEntity,
        val xyzEntity: GltfModelEntity,
        val resizeEndCount: AtomicInteger,
        private val computeLocalPosition: (PanelEntity) -> Vector3,
    ) {
        fun updateWidgetPose() {
            xyzEntity.setPose(Pose(computeLocalPosition(panel)))
        }
    }

    private fun createInteractiveWidgetPanel(
        panelName: String,
        computeLocalPosition: (PanelEntity) -> Vector3,
    ): InteractiveWidgetHarness {
        val resizeEndCount = AtomicInteger(0)
        scene.mainPanelEntity.setEnabled(false)
        val panel =
            createSpatialPanel(
                pixelDimensions = INITIAL_PANEL_PIXELS,
                name = panelName,
                pose = INTERACTIVE_PANEL_POSE,
                parent = scene.activitySpace,
                backgroundColor = AndroidColor.BLACK,
            )
        val xyzEntity = createXyzWidget(parent = panel)
        val harness =
            InteractiveWidgetHarness(
                panel = panel,
                xyzEntity = xyzEntity,
                resizeEndCount = resizeEndCount,
                computeLocalPosition = computeLocalPosition,
            )

        val movable = MovableComponent.createSystemMovable(sessionRule.session, scaleInZ = false)
        val resizable =
            ResizableComponent.create(
                session = sessionRule.session,
                minimumSize = FloatSize3d(0.1f, 0.1f, 0.1f),
            ) { event ->
                // Runs on the main thread, which owns the panel's view hierarchy.
                panel.size = FloatSize2d(event.newSize.width, event.newSize.height)
                harness.updateWidgetPose()
                if (event.resizeState == ResizeEvent.ResizeState.END) {
                    resizeEndCount.incrementAndGet()
                }
            }
        assertThat(panel.addComponent(movable)).isTrue()
        assertThat(panel.addComponent(resizable)).isTrue()
        harness.updateWidgetPose()
        return harness
    }

    private fun assertScaleDoublesActivityOffsetAndPreservesRelativePosition(
        panel: PanelEntity,
        xyzEntity: GltfModelEntity,
        expectedU: Float,
        expectedV: Float,
        recomputeWidgetPose: () -> Unit,
    ) {
        val activityOffsetBeforeScale =
            xyzEntity.getPose(Space.ACTIVITY).translation -
                panel.getPose(Space.ACTIVITY).translation
        panel.setScale(2.0f)
        recomputeWidgetPose()

        val activityOffsetAfterScale =
            xyzEntity.getPose(Space.ACTIVITY).translation -
                panel.getPose(Space.ACTIVITY).translation
        assertVector3(activityOffsetAfterScale, activityOffsetBeforeScale * 2.0f, ACTIVITY_EPSILON)
        assertMarkerNormalizedOffsetOnPanel(
            marker = xyzEntity,
            panel = panel,
            expectedU = expectedU,
            expectedV = expectedV,
        )
    }

    /**
     * Asserts that [marker]'s origin, transformed into [panel]'s coordinate space via
     * [Entity.transformPositionTo] (which accounts for both parent rotation and [panel]'s scale in
     * [Space.ACTIVITY]), lies at normalized coordinates ([expectedU], [expectedV]) on [panel].
     */
    private fun assertMarkerNormalizedOffsetOnPanel(
        marker: Entity,
        panel: PanelEntity,
        expectedU: Float,
        expectedV: Float,
    ) {
        val positionOnPanel = marker.transformPositionTo(Vector3.Zero, panel)
        val halfW = panel.size.width / 2f
        val halfH = panel.size.height / 2f
        assertVector3(
            positionOnPanel,
            Vector3(expectedU * halfW, expectedV * halfH, 0f),
            ACTIVITY_EPSILON,
        )
    }

    /** Creates a [PanelEntity] that shows a [TextView] labeled with [name]. */
    private fun createSpatialPanel(
        pixelDimensions: IntSize2d,
        name: String,
        pose: Pose,
        parent: Entity,
        backgroundColor: Int = AndroidColor.DKGRAY,
    ): PanelEntity = sessionRule.onMainThread {
        val contentView =
            TextView(activity).apply {
                text = name
                setBackgroundColor(backgroundColor)
                setTextColor(AndroidColor.WHITE)
            }
        PanelEntity.create(
            session = session,
            view = contentView,
            pixelDimensions = pixelDimensions,
            name = name,
            pose = pose,
            parent = parent,
        )
    }

    /**
     * Creates the `models/xyzArrows.glb` widget as a child of [parent] on the main thread, which
     * the glTF APIs require.
     */
    private fun createXyzWidget(parent: Entity): GltfModelEntity = sessionRule.onMainThread {
        val xyzModel = GltfModel.create(session, XYZ_ARROWS_MODEL_PATH)
        GltfModelEntity.create(session = session, model = xyzModel, parent = parent).apply {
            loadNodesBeforeTeardown()
            setScale(CUJ_XYZ_WIDGET_SCALE)
        }
    }

    private fun awaitResizablePanelNode(
        activity: ComponentActivity,
        panelName: String,
    ): SpatialNode =
        checkNotNull(
            SpatialSceneHelper.waitForSpatialScene(
                description = "resizable spatial panel '$panelName' with settled extents",
                timeout = SCENE_TIMEOUT,
                pollInterval = POLL_INTERVAL,
                select = { spatialScene ->
                    spatialScene.findTaskByActivity(activity)?.embeddedWindowLeashes?.firstOrNull {
                        it.name == panelName
                    }
                },
                until = { node ->
                    // A new panel fades in, so also wait until it and its surface are visible.
                    node != null &&
                        node.isResizable &&
                        node.worldExtents != null &&
                        node.hasVisibleAlpha() &&
                        node.primarySurfaceNode?.hasVisibleAlpha() != false
                },
            )
        )

    /** Whether this node's `computedAlpha` is above zero, treating a missing value as visible. */
    private fun SpatialNode.hasVisibleAlpha(): Boolean =
        properties["computedAlpha"]?.toFloatOrNull()?.let { it > 0f } ?: true

    private companion object {
        const val LOCAL_EPSILON = 1e-4f
        const val ACTIVITY_EPSILON = 1e-3f
        const val CUJ_XYZ_WIDGET_SCALE = 0.2f
        const val RESIZE_AFFORDANCE_MARGIN_METERS = 0.02f
        // The TopRight drag by half the panel width nominally doubles the width; 1.3x leaves
        // margin for how far the system resize follows the injected drag.
        const val MIN_WIDENED_WIDTH_RATIO = 1.3f
        // The TopRight drag has no vertical component, so the height should barely change.
        const val MAX_HEIGHT_DRIFT_PX = 150
        val XYZ_ARROWS_MODEL_PATH = Paths.get("models", "xyzArrows.glb")
        val INITIAL_PANEL_PIXELS = IntSize2d(1000, 1000)
        val WIDENED_PANEL_PIXELS =
            IntSize2d(INITIAL_PANEL_PIXELS.width * 2, INITIAL_PANEL_PIXELS.height)
        val ACTIVITY_PANEL_PIXELS = IntSize2d(1280, 960)
        val SECONDARY_PANEL_POSE = Pose(Vector3(0.7f, 0.7f, -0.05f))
        val INTERACTIVE_PANEL_POSE = Pose(Vector3(0f, 0f, 0.1f))
        val FIRST_RESIZE_LOCAL_DELTA = Vector3(0.08f, 0.06f, 0f)
        val SECOND_RESIZE_LOCAL_DELTA = Vector3(0.18f, 0.14f, 0f)
        val INTERACTION_TIMEOUT = 5.seconds
        val SCENE_TIMEOUT = 5.seconds
        val POLL_TIMEOUT = 10.seconds
        val POLL_INTERVAL = 100.milliseconds
    }
}

/**
 * Reads [GltfModelEntity.nodes] while the renderer is still running.
 *
 * When the activity is destroyed, `Session` destroys its rendering runtime before its scene runtime
 * disposes entities. `GltfFeatureImpl.dispose()` then builds the lazy `nodes` list if nothing read
 * it before, querying the torn-down renderer, which intermittently crashes the test process with
 * "IllegalArgumentException: Illegal Capacity: -1".
 */
internal fun GltfModelEntity.loadNodesBeforeTeardown() {
    // TODO(b/584771482): Remove once entities are disposed before the rendering runtime is
    // destroyed.
    assertThat(nodes).isNotEmpty()
}
