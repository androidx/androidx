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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device API and state integration tests for
 * [PanelEntity.transformPixelCoordinatesToLocalPosition] and
 * [PanelEntity.transformNormalizedCoordinatesToLocalPosition] (b/573232780):
 * - Pixel and normalized coordinate transforms across corners, edges, and center against
 *   [Scene.virtualPixelDensity].
 * - Coordinate recomputation and child relative positioning across programmatic panel resizes
 *   ([IntSize2d] and [FloatSize2d]) and hierarchical scale changes.
 * - Linear extrapolation for out-of-bounds and sub-pixel coordinates.
 * - Coordinate transforms on [Scene.mainPanelEntity] and [ActivityPanelEntity].
 *
 * Tests run on the instrumentation thread. Only panel creation and resizing use
 * [XrSessionRule.onMainThread] because they update the panel's view hierarchy.
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

    private companion object {
        const val LOCAL_EPSILON = 1e-4f
        const val ACTIVITY_EPSILON = 1e-3f
        val INITIAL_PANEL_PIXELS = IntSize2d(1000, 1000)
        val WIDENED_PANEL_PIXELS =
            IntSize2d(INITIAL_PANEL_PIXELS.width * 2, INITIAL_PANEL_PIXELS.height)
        val ACTIVITY_PANEL_PIXELS = IntSize2d(1280, 960)
        val SECONDARY_PANEL_POSE = Pose(Vector3(0.7f, 0.7f, -0.05f))
    }
}
