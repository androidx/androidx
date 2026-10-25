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

package androidx.xr.compose.integration.tests

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.SystemClock
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialExternalSurfaceHemisphere
import androidx.xr.compose.subspace.SpatialExternalSurfaceScope
import androidx.xr.compose.subspace.SpatialExternalSurfaceSphere
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SuperSamplingPattern
import androidx.xr.compose.subspace.draw.SpatialFeatheringEffect
import androidx.xr.compose.subspace.draw.spatialSmoothFeatheringEffect
import androidx.xr.compose.subspace.layout.MovePolicy
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ExperimentalSurfaceEntityPixelDimensionsApi
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.DEFAULT_MOVE_HANDLE_MARGIN
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Stereo Video in SpatialExternalSurface CUJ in Compose XR.
 *
 * Covers:
 * - Planar video rendering on [SpatialExternalSurface] with [StereoMode.Mono],
 *   [StereoMode.TopBottom], and [StereoMode.SideBySide]
 * - Dynamic stereo mode switching across [StereoMode.Mono], [StereoMode.TopBottom], and
 *   [StereoMode.SideBySide]
 * - 180-degree dome surface rendering on [SpatialExternalSurfaceHemisphere]
 * - 360-degree spherical surface rendering on [SpatialExternalSurfaceSphere]
 * - Dynamic surface geometry switching (Quad -> Hemisphere -> Sphere)
 * - Spatial feathering effect application across percent and Dp metrics
 * - Surface lifecycle callbacks
 *   ([androidx.xr.compose.subspace.SpatialExternalSurfaceScope.onSurfaceCreated] and
 *   [androidx.xr.compose.subspace.SpatialExternalSurfaceScope.onSurfaceDestroyed])
 * - Real asset video playback (`videos/sample_video.mp4`) via [MediaPlayer] to [Surface]
 * - Real asset image rendering (`images/earth.jpg`) via [SurfaceEntity.setSurfacePixelDimensions]
 *   and [Surface.lockHardwareCanvas]
 * - Super-sampling configuration ([SuperSamplingPattern.Pentagon] vs [SuperSamplingPattern.None])
 * - Overlaid 2D Compose controls and UI interaction within [SpatialExternalSurface]
 * - Subspace spatial layout sizing, offset, movable, and resizable modifiers
 *
 * Future CUJ roadmap / pending items:
 * - TODO(b/570945399): Migrate
 *   `spatialExternalSurface_imageAssetRendering_rendersBitmapSuccessfully` away from direct
 *   [SurfaceEntity.setSurfacePixelDimensions] calls once [SpatialExternalSurface] exposes Surface
 *   pixel dimension configuration directly.
 * - TODO(b/570945399): Add MV-HEVC ([StereoMode.MultiviewLeftPrimary] and
 *   [StereoMode.MultiviewRightPrimary]) rendering and dedicated 180°/360° stereo video assets once
 *   AOSP-owned assets are uploaded to `prebuilts/androidx/xr/assets`.
 * - TODO(b/570945399): Add DRM surface protection
 *   ([androidx.xr.compose.subspace.SpatialExternalSurfaceProtection.Protected]) and dynamic
 *   protection toggling tests once an AOSP-owned DRM-protected video asset is available.
 * - TODO(b/576448890): Add visual rendering verification for stereo mode UV mapping and shader edge
 *   feathering once SplitEngine pixel/screenshot inspection is supported in integration tests.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SpatialExternalSurfaceTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var appContext: Context
    private var showSubspace by mutableStateOf(true)
    private val activeSessionRef = AtomicReference<Session?>(null)

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        showSubspace = true
        activeSessionRef.set(null)
        assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(appContext),
        )
    }

    /**
     * Installs Compose content wrapped in a controllable [showSubspace] gate, transitions the host
     * [ComponentActivity] from Home Space Mode into Full Space Mode (`isSpatialUiEnabled == true`)
     * so 3D [Subspace] and [SurfaceEntity] nodes are visually rendered by the XR compositor, and
     * captures the active [Session] for clean teardown in [tearDownSubspaceAndVideo].
     */
    private fun setSubspaceContent(content: @Composable () -> Unit) {
        showSubspace = true
        val spatialUiReady = AtomicBoolean(false)
        composeTestRule.setContent {
            val spatialCapabilities = LocalSpatialCapabilities.current
            val session = LocalSession.current
            activeSessionRef.set(session)
            spatialUiReady.set(spatialCapabilities.isSpatialUiEnabled)
            LaunchedEffect(session, spatialCapabilities.isSpatialUiEnabled) {
                if (session != null && spatialCapabilities.isSpatialUiEnabled) {
                    runCatching {
                        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
                    }
                }
            }
            if (showSubspace && spatialCapabilities.isSpatialUiEnabled) {
                content()
            }
        }

        runBlocking(Dispatchers.Default) { composeTestRule.activity.requestFullSpace() }
        composeTestRule.waitUntil(timeoutMillis = 10_000L) { spatialUiReady.get() }
        composeTestRule.waitForIdle()
    }

    /**
     * Cleanly stops and detaches [MediaPlayer] instances, unmounts the [Subspace] composition, and
     * disposes any [SurfaceEntity] instances while the [Session] and `SplitEngineBridge` are still
     * active, preventing asynchronous `imp_bg_wk` texture release calls from racing with
     * `SpatialRenderingRuntime.destroy()` during `Activity.onDestroy()`.
     */
    @Suppress("DEPRECATION", "RestrictedApiAndroidX")
    private fun tearDownSubspaceAndVideo(vararg sessions: VideoPlaybackSession) {
        sessions.forEach { it.release() }

        runCatching {
            composeTestRule.runOnUiThread { showSubspace = false }
            composeTestRule.waitForIdle()
        }

        runCatching {
            composeTestRule.runOnUiThread {
                val session = activeSessionRef.getAndSet(null)
                val surfaceEntities =
                    session?.scene?.getEntitiesOfType(SurfaceEntity::class.java).orEmpty()
                surfaceEntities.forEach { entity -> runCatching { entity.dispose() } }
            }
            composeTestRule.waitForIdle()
        }

        SystemClock.sleep(TEARDOWN_SETTLE_MS)
    }

    /** Validates that SpatialExternalSurface renders in Mono mode with specified dimensions. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_mono_rendersWithConfiguredDimensions() {
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .testTag("mono_video_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            composeTestRule.waitForIdle()
            val surfaceEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("mono_video_surface")
                        .assertExists()
                        .assertWidthIsEqualTo(640.dp)
                        .assertHeightIsEqualTo(360.dp)
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates StereoMode.TopBottom and StereoMode.SideBySide planar video surfaces. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_stereoModes_rendersTopBottomAndSideBySide() {
        val topBottomSession = VideoPlaybackSession(appContext)
        val sideBySideSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.TopBottom,
                        modifier =
                            SubspaceModifier.width(400.dp)
                                .height(300.dp)
                                .testTag("top_bottom_surface"),
                    ) {
                        topBottomSession.bind(this)
                    }
                    SpatialExternalSurface(
                        stereoMode = StereoMode.SideBySide,
                        modifier =
                            SubspaceModifier.width(400.dp)
                                .height(300.dp)
                                .testTag("side_by_side_surface"),
                    ) {
                        sideBySideSession.bind(this)
                    }
                }
            }

            val topBottomEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("top_bottom_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            val sideBySideEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("side_by_side_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(topBottomEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)
            assertThat(sideBySideEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.SIDE_BY_SIDE)
            topBottomSession.awaitAndAssertVideoRendered(composeTestRule)
            sideBySideSession.awaitAndAssertVideoRendered(composeTestRule)
        } finally {
            tearDownSubspaceAndVideo(topBottomSession, sideBySideSession)
        }
    }

    /**
     * Validates dynamic stereo mode switching at runtime across Mono, TopBottom, and SideBySide.
     */
    // TODO(b/576448890): Add visual rendering verification for stereo mode UV mapping once
    // SplitEngine per-eye screenshot/pixel inspection is supported in integration tests.
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_dynamicStereoModeSwitching_updatesSuccessfully() {
        val videoSession = VideoPlaybackSession(appContext)
        var currentStereoMode by mutableStateOf(StereoMode.Mono)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = currentStereoMode,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .testTag("dynamic_stereo_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            val surfaceEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("dynamic_stereo_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
            videoSession.awaitAndAssertVideoRendered(composeTestRule)

            // Switch to TopBottom
            composeTestRule.runOnUiThread { currentStereoMode = StereoMode.TopBottom }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("dynamic_stereo_surface").assertExists()
            assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)
            assertThat(videoSession.surface.get()?.isValid).isTrue()

            // Switch to SideBySide
            composeTestRule.runOnUiThread { currentStereoMode = StereoMode.SideBySide }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("dynamic_stereo_surface").assertExists()
            assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.SIDE_BY_SIDE)
            assertThat(videoSession.surface.get()?.isValid).isTrue()

            // Revert to Mono
            composeTestRule.runOnUiThread { currentStereoMode = StereoMode.Mono }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("dynamic_stereo_surface").assertExists()
            assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
            assertThat(videoSession.surface.get()?.isValid).isTrue()
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates that 180-degree SpatialExternalSurfaceHemisphere renders correctly. */
    // TODO(b/570945399): Replace sample_video.mp4 with a dedicated 180-degree stereo video asset
    // once uploaded to prebuilts/androidx/xr/assets.
    @Test
    @XrDeviceTest
    fun spatialExternalSurfaceHemisphere_rendersSuccessfully() {
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurfaceHemisphere(
                        stereoMode = StereoMode.Mono,
                        modifier = SubspaceModifier.testTag("hemisphere_video_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            val surfaceEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("hemisphere_video_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(surfaceEntity.shape).isInstanceOf(SurfaceEntity.Shape.Hemisphere::class.java)
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates that 360-degree SpatialExternalSurfaceSphere renders correctly. */
    // TODO(b/570945399): Replace sample_video.mp4 with a dedicated 360-degree stereo video asset
    // once uploaded to prebuilts/androidx/xr/assets.
    @Test
    @XrDeviceTest
    fun spatialExternalSurfaceSphere_rendersSuccessfully() {
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurfaceSphere(
                        stereoMode = StereoMode.Mono,
                        modifier = SubspaceModifier.testTag("sphere_video_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            val surfaceEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("sphere_video_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(surfaceEntity.shape).isInstanceOf(SurfaceEntity.Shape.Sphere::class.java)
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates dynamic switching across Quad, Hemisphere, and Sphere surface geometries. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_dynamicSurfaceGeometrySwitching_recomposesCleanly() {
        var surfaceType by mutableStateOf("QUAD")
        val quadSession = VideoPlaybackSession(appContext)
        val hemisphereSession = VideoPlaybackSession(appContext)
        val sphereSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    when (surfaceType) {
                        "QUAD" ->
                            SpatialExternalSurface(
                                stereoMode = StereoMode.Mono,
                                modifier =
                                    SubspaceModifier.width(640.dp)
                                        .height(360.dp)
                                        .testTag("active_video_surface"),
                            ) {
                                quadSession.bind(this)
                            }
                        "HEMISPHERE" ->
                            SpatialExternalSurfaceHemisphere(
                                stereoMode = StereoMode.Mono,
                                modifier = SubspaceModifier.testTag("active_video_surface"),
                            ) {
                                hemisphereSession.bind(this)
                            }
                        "SPHERE" ->
                            SpatialExternalSurfaceSphere(
                                stereoMode = StereoMode.Mono,
                                modifier = SubspaceModifier.testTag("active_video_surface"),
                            ) {
                                sphereSession.bind(this)
                            }
                    }
                }
            }

            val initialQuadEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("active_video_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(initialQuadEntity.shape).isInstanceOf(SurfaceEntity.Shape.Quad::class.java)
            quadSession.awaitAndAssertVideoRendered(composeTestRule)

            // Switch to Hemisphere (180)
            composeTestRule.runOnUiThread { surfaceType = "HEMISPHERE" }
            composeTestRule.waitForIdle()
            val hemisphereEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("active_video_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(hemisphereEntity.shape)
                .isInstanceOf(SurfaceEntity.Shape.Hemisphere::class.java)
            hemisphereSession.awaitAndAssertVideoRendered(composeTestRule)

            // Switch to Sphere (360)
            composeTestRule.runOnUiThread { surfaceType = "SPHERE" }
            composeTestRule.waitForIdle()
            val sphereEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("active_video_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(sphereEntity.shape).isInstanceOf(SurfaceEntity.Shape.Sphere::class.java)
            sphereSession.awaitAndAssertVideoRendered(composeTestRule)

            // Revert back to Quad
            quadSession.reset()
            composeTestRule.runOnUiThread { surfaceType = "QUAD" }
            composeTestRule.waitForIdle()
            val revertedQuadEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("active_video_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            assertThat(revertedQuadEntity.shape).isInstanceOf(SurfaceEntity.Shape.Quad::class.java)
            quadSession.awaitAndAssertVideoRendered(composeTestRule)
        } finally {
            tearDownSubspaceAndVideo(quadSession, hemisphereSession, sphereSession)
        }
    }

    /** Validates spatial smooth feathering effect application across percent and Dp metrics. */
    // TODO(b/576448890): Add visual rendering verification for shader edge feathering once
    // SplitEngine pixel/screenshot inspection is supported in integration tests.
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_featheringEffect_appliesPercentAndDp() {
        val videoSession = VideoPlaybackSession(appContext)
        var feathering by
            mutableStateOf<SpatialFeatheringEffect?>(
                spatialSmoothFeatheringEffect(horizontalPercent = 25, verticalPercent = 25)
            )

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        featheringEffect = feathering,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .testTag("feathered_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            val surfaceEntity =
                checkNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("feathered_surface")
                        .assertExists()
                        .fetchSemanticsNode()
                        .semanticsEntity as? SurfaceEntity
                )
            val initialFeather =
                surfaceEntity.edgeFeatheringParams
                    as SurfaceEntity.EdgeFeatheringParams.RectangleFeather
            assertThat(initialFeather.leftRight).isWithin(1e-3f).of(0.25f)
            assertThat(initialFeather.topBottom).isWithin(1e-3f).of(0.25f)
            videoSession.awaitAndAssertVideoRendered(composeTestRule)

            // Update to heavy 50% feathering
            composeTestRule.runOnUiThread {
                feathering =
                    spatialSmoothFeatheringEffect(horizontalPercent = 50, verticalPercent = 50)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("feathered_surface").assertExists()
            val updatedPercentFeather =
                surfaceEntity.edgeFeatheringParams
                    as SurfaceEntity.EdgeFeatheringParams.RectangleFeather
            assertThat(updatedPercentFeather.leftRight).isWithin(1e-3f).of(0.50f)
            assertThat(updatedPercentFeather.topBottom).isWithin(1e-3f).of(0.50f)
            assertThat(videoSession.surface.get()?.isValid).isTrue()

            // Update to Dp feathering (10.dp on 640.dp x 360.dp half-extents: 10/320 and 10/180)
            composeTestRule.runOnUiThread {
                feathering = spatialSmoothFeatheringEffect(horizontal = 10.dp, vertical = 10.dp)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("feathered_surface").assertExists()
            val dpFeather =
                surfaceEntity.edgeFeatheringParams
                    as SurfaceEntity.EdgeFeatheringParams.RectangleFeather
            assertThat(dpFeather.leftRight).isWithin(1e-3f).of(10f / 320f)
            assertThat(dpFeather.topBottom).isWithin(1e-3f).of(10f / 180f)
            assertThat(videoSession.surface.get()?.isValid).isTrue()

            // Clear feathering
            composeTestRule.runOnUiThread { feathering = null }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("feathered_surface").assertExists()
            assertThat(surfaceEntity.edgeFeatheringParams)
                .isInstanceOf(SurfaceEntity.EdgeFeatheringParams.NoFeathering::class.java)
            assertThat(videoSession.surface.get()?.isValid).isTrue()
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /**
     * Validates that an active Surface acquired from onSurfaceCreated can receive real video
     * playback frames from MediaPlayer using sample_video.mp4 from assets and invokes
     * onSurfaceDestroyed when removed from the composition.
     */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_videoAssetPlayback_rendersVideoSuccessfully() {
        val videoSession = VideoPlaybackSession(appContext)
        var isVisible by mutableStateOf(true)

        try {
            setSubspaceContent {
                Subspace {
                    if (isVisible) {
                        SpatialExternalSurface(
                            stereoMode = StereoMode.Mono,
                            modifier =
                                SubspaceModifier.width(640.dp)
                                    .height(360.dp)
                                    .testTag("video_playback_surface"),
                        ) {
                            videoSession.bind(this)
                        }
                    }
                }
            }

            composeTestRule.onSubspaceNodeWithTag("video_playback_surface").assertExists()
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
            val createdSurface = checkNotNull(videoSession.surface.get())

            // Hide the composable to verify onSurfaceDestroyed callback
            composeTestRule.runOnUiThread { isVisible = false }
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                videoSession.destroyedSurface.get() != null
            }
            assertThat(videoSession.destroyedSurface.get()).isSameInstanceAs(createdSurface)
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /**
     * Validates that an active Surface acquired from onSurfaceCreated can render a still image
     * (`images/earth.jpg` from assets) via [Surface.lockHardwareCanvas] once
     * [SurfaceEntity.setSurfacePixelDimensions] is configured, and invokes onSurfaceDestroyed when
     * removed from the composition.
     */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_imageAssetRendering_rendersBitmapSuccessfully() {
        val imageSession = ImageRenderSession(appContext, activeSessionRef)
        var isVisible by mutableStateOf(true)

        try {
            setSubspaceContent {
                Subspace {
                    if (isVisible) {
                        SpatialExternalSurface(
                            stereoMode = StereoMode.Mono,
                            modifier =
                                SubspaceModifier.width(640.dp)
                                    .height(360.dp)
                                    .testTag("image_playback_surface"),
                        ) {
                            imageSession.bind(this)
                        }
                    }
                }
            }

            composeTestRule
                .onSubspaceNodeWithTag("image_playback_surface")
                .assertExists()
                .assertWidthIsEqualTo(640.dp)
                .assertHeightIsEqualTo(360.dp)
            imageSession.awaitAndAssertImageRendered(composeTestRule)
            val createdSurface = checkNotNull(imageSession.surface.get())

            // Hide the composable to verify onSurfaceDestroyed callback
            composeTestRule.runOnUiThread { isVisible = false }
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                imageSession.destroyedSurface.get() != null
            }
            assertThat(imageSession.destroyedSurface.get()).isSameInstanceAs(createdSurface)
        } finally {
            tearDownSubspaceAndVideo()
        }
    }

    /** Validates SuperSamplingPattern configuration on SpatialExternalSurface. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_superSamplingPattern_configuresSuccessfully() {
        var pattern by mutableStateOf(SuperSamplingPattern.Pentagon)
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        superSamplingPattern = pattern,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .testTag("supersampled_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            composeTestRule.onSubspaceNodeWithTag("supersampled_surface").assertExists()
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
            val initialSurface = videoSession.surface.get()

            // Switch to SuperSamplingPattern.None. Because superSampling is a creation-time
            // parameter on SurfaceEntity (keyed via remember(surfaceProtection,
            // superSamplingPattern)), changing superSamplingPattern recreates the underlying
            // Surface and triggers onSurfaceCreated again.
            videoSession.reset()
            composeTestRule.runOnUiThread { pattern = SuperSamplingPattern.None }
            composeTestRule.waitForIdle()
            composeTestRule.onSubspaceNodeWithTag("supersampled_surface").assertExists()
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
            assertThat(videoSession.surface.get()).isNotSameInstanceAs(initialSurface)
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates that 2D Compose UI controls render on top of the surface and receive clicks. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_contentOverlay_rendersChildComposables() {
        var playClicked = false
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .testTag("interactive_surface"),
                    ) {
                        videoSession.bind(this)
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(100.dp)
                                    .offset(z = 10.dp)
                                    .testTag("control_panel")
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Button(
                                    onClick = { playClicked = true },
                                    modifier = Modifier.testTag("play_button"),
                                ) {
                                    Text("Play Video")
                                }
                            }
                        }
                    }
                }
            }

            composeTestRule.onSubspaceNodeWithTag("interactive_surface").assertExists()
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
            composeTestRule.onSubspaceNodeWithTag("control_panel").assertExists()
            composeTestRule.onNodeWithTag("play_button").assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()

            assertThat(playClicked).isTrue()
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates positioning and spatial offset modifiers on SpatialExternalSurface. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_spatialModifiers_appliesOffsetAndPosition() {
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .offset(x = 50.dp, y = 25.dp, z = (-10).dp)
                                .testTag("offset_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            composeTestRule
                .onSubspaceNodeWithTag("offset_surface")
                .assertExists()
                .assertWidthIsEqualTo(640.dp)
                .assertHeightIsEqualTo(360.dp)
                .assertPositionInRootIsEqualTo(50.dp, 25.dp, (-10).dp)
            videoSession.awaitAndAssertVideoRendered(composeTestRule)
        } finally {
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    /** Validates attaching movable and resizable modifiers to SpatialExternalSurface. */
    @Test
    @XrDeviceTest
    fun spatialExternalSurface_movableAndResizable_attachesPolicies() {
        val observedMoveEvents = AtomicInteger(0)
        val videoSession = VideoPlaybackSession(appContext)

        try {
            setSubspaceContent {
                val movePolicy = remember {
                    MovePolicy.system(scaleWithDistance = false) { _ ->
                        observedMoveEvents.incrementAndGet()
                    }
                }
                Subspace {
                    SpatialExternalSurface(
                        stereoMode = StereoMode.Mono,
                        modifier =
                            SubspaceModifier.width(640.dp)
                                .height(360.dp)
                                .movable(enabled = true, movePolicy = movePolicy)
                                .resizable()
                                .testTag("movable_surface"),
                    ) {
                        videoSession.bind(this)
                    }
                }
            }

            val semanticsNode =
                composeTestRule
                    .onSubspaceNodeWithTag("movable_surface")
                    .assertExists()
                    .assertWidthIsEqualTo(640.dp)
                    .assertHeightIsEqualTo(360.dp)
                    .fetchSemanticsNode()
            val components = checkNotNull(semanticsNode.components)
            val movableComponent = components.filterIsInstance<MovableComponent>().single()
            assertThat(components.filterIsInstance<ResizableComponent>()).hasSize(1)

            // Verify that the underlying CPM scene graph node has both movable and resizable
            // reform options enabled (reformOptions=<enable=3, ...>).
            val surfaceNode = checkNotNull(findMovableAndResizableSurfaceNode())
            assertThat(surfaceNode.isMovable).isTrue()
            assertThat(surfaceNode.isResizable).isTrue()

            videoSession.awaitAndAssertVideoRendered(composeTestRule)

            // Perform 3D spatial move interaction by dragging the bottom-center move affordance.
            // Because SplitEngine SurfaceEntity nodes do not emit root2DSurfaceBoundsInMeters in
            // dumpsys spf_cpm (worldHalfExtents is null), include the surface's half-height in
            // world meters when targeting the bottom move handle.
            val initialPosition = surfaceNode.position
            val moveHandleMarginMeters =
                if (surfaceNode.worldHalfExtents != null) {
                    DEFAULT_MOVE_HANDLE_MARGIN
                } else {
                    (movableComponent.size.height / 2f) * surfaceNode.scale.y +
                        DEFAULT_MOVE_HANDLE_MARGIN
                }

            surfaceNode.performSpatialInteraction {
                moveNodeBy(
                    delta = Vector3(0.25f, 0f, 0f),
                    marginMeters = moveHandleMarginMeters,
                )
            }
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 5_000L) { observedMoveEvents.get() > 0 }

            var updatedPosition = initialPosition
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                val updatedNode = findMovableAndResizableSurfaceNode()
                if (updatedNode != null) {
                    updatedPosition = updatedNode.position
                    (updatedPosition - initialPosition).length > 0.05f
                } else {
                    false
                }
            }
            assertThat(updatedPosition.x).isGreaterThan(initialPosition.x)
        } finally {
            resetSpatialInteraction()
            tearDownSubspaceAndVideo(videoSession)
        }
    }

    private fun findMovableAndResizableSurfaceNode(): SpatialNode? {
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task =
            scene.findTaskById(composeTestRule.activity.taskId)
                ?: scene.findTaskByPackageName(appContext.packageName)
                ?: return null
        return task.subspaceNodes.firstNotNullOfOrNull { subspace ->
            subspace.parent?.takeIf { it.isMovable && it.isResizable }
        }
            ?: task.findNode {
                it.isMovable &&
                    it.isResizable &&
                    !it.isSysUi &&
                    !it.isTaskWindowLeash &&
                    !it.name.startsWith("space-transform-task") &&
                    it != task.activitySpaceRoot
            }
    }

    /**
     * Helper that decodes [EARTH_IMAGE_ASSET] (`images/earth.jpg`) from assets, configures the
     * underlying [SurfaceEntity] buffer resolution via [SurfaceEntity.setSurfacePixelDimensions],
     * and blits the bitmap onto the [Surface] via [Surface.lockHardwareCanvas].
     */
    @OptIn(ExperimentalSurfaceEntityPixelDimensionsApi::class)
    @Suppress("DEPRECATION", "RestrictedApiAndroidX")
    private class ImageRenderSession(
        private val context: Context,
        private val sessionRef: AtomicReference<Session?>,
    ) {
        val surface = AtomicReference<Surface?>(null)
        val destroyedSurface = AtomicReference<Surface?>(null)
        private val imageRendered = AtomicBoolean(false)
        private val renderedBitmapWidth = AtomicInteger(0)
        private val renderedBitmapHeight = AtomicInteger(0)

        fun bind(scope: SpatialExternalSurfaceScope) {
            scope.onSurfaceCreated { createdSurface ->
                surface.set(createdSurface)
                val bitmap =
                    context.assets.open(EARTH_IMAGE_ASSET).use { input ->
                        checkNotNull(BitmapFactory.decodeStream(input)) {
                            "Failed to decode asset $EARTH_IMAGE_ASSET"
                        }
                    }
                val surfaceEntities =
                    sessionRef.get()?.scene?.getEntitiesOfType(SurfaceEntity::class.java).orEmpty()
                val matchingEntity =
                    surfaceEntities.firstOrNull { it.getSurface() == createdSurface }
                        ?: surfaceEntities.lastOrNull()
                matchingEntity?.setSurfacePixelDimensions(IntSize2d(bitmap.width, bitmap.height))

                val canvas = createdSurface.lockHardwareCanvas()
                try {
                    canvas.drawColor(Color.BLACK)
                    val dstRect = Rect(0, 0, canvas.width, canvas.height)
                    canvas.drawBitmap(bitmap, null, dstRect, null)
                } finally {
                    createdSurface.unlockCanvasAndPost(canvas)
                }
                renderedBitmapWidth.set(bitmap.width)
                renderedBitmapHeight.set(bitmap.height)
                imageRendered.set(true)
                bitmap.recycle()
            }
            scope.onSurfaceDestroyed { destroyed -> destroyedSurface.set(destroyed) }
        }

        fun awaitAndAssertImageRendered(composeTestRule: ComposeTestRule) {
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                imageRendered.get() && surface.get()?.isValid == true
            }
            assertThat(surface.get()?.isValid).isTrue()
            assertThat(renderedBitmapWidth.get()).isGreaterThan(0)
            assertThat(renderedBitmapHeight.get()).isGreaterThan(0)
        }
    }

    /**
     * Helper that attaches a [MediaPlayer] playing [SAMPLE_VIDEO_ASSET] to a
     * [SpatialExternalSurfaceScope] and verifies frame delivery to the underlying [Surface].
     */
    private class VideoPlaybackSession(private val context: Context) {
        val surface = AtomicReference<Surface?>(null)
        val destroyedSurface = AtomicReference<Surface?>(null)
        private val mediaPlayerRef = AtomicReference<MediaPlayer?>(null)
        private val firstFrameRendered = AtomicBoolean(false)
        private val decodedWidth = AtomicInteger(0)
        private val decodedHeight = AtomicInteger(0)
        private val playbackError = AtomicBoolean(false)

        fun bind(scope: SpatialExternalSurfaceScope) {
            scope.onSurfaceCreated { createdSurface ->
                release()
                surface.set(createdSurface)
                val player =
                    MediaPlayer().apply {
                        context.assets.openFd(SAMPLE_VIDEO_ASSET).use { afd ->
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        }
                        setSurface(createdSurface)
                        isLooping = true
                        setOnVideoSizeChangedListener { _, width, height ->
                            decodedWidth.set(width)
                            decodedHeight.set(height)
                        }
                        setOnInfoListener { _, what, _ ->
                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                firstFrameRendered.set(true)
                            }
                            false
                        }
                        setOnErrorListener { _, _, _ ->
                            playbackError.set(true)
                            true
                        }
                        prepare()
                        start()
                    }
                mediaPlayerRef.set(player)
            }
            scope.onSurfaceDestroyed { destroyed ->
                destroyedSurface.set(destroyed)
                release()
            }
        }

        fun awaitAndAssertVideoRendered(composeTestRule: ComposeTestRule) {
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                val player = mediaPlayerRef.get()
                val position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
                surface.get()?.isValid == true &&
                    decodedWidth.get() > 0 &&
                    decodedHeight.get() > 0 &&
                    (firstFrameRendered.get() || position > 0)
            }
            // Keep video playing so rendered frames are visually observable on the emulator/device.
            SystemClock.sleep(MIN_VIDEO_PLAYBACK_MS)
            val player = checkNotNull(mediaPlayerRef.get())
            assertThat(playbackError.get()).isFalse()
            assertThat(surface.get()?.isValid).isTrue()
            assertThat(decodedWidth.get()).isGreaterThan(0)
            assertThat(decodedHeight.get()).isGreaterThan(0)
            assertThat(player.isPlaying).isTrue()
        }

        fun reset() {
            release()
            surface.set(null)
            destroyedSurface.set(null)
            firstFrameRendered.set(false)
            decodedWidth.set(0)
            decodedHeight.set(0)
            playbackError.set(false)
        }

        fun release() {
            mediaPlayerRef.getAndSet(null)?.let { player ->
                runCatching { player.setSurface(null) }
                runCatching {
                    if (player.isPlaying) {
                        player.stop()
                    }
                }
                runCatching { player.release() }
            }
        }
    }

    private companion object {
        private const val EARTH_IMAGE_ASSET = "images/earth.jpg"
        private const val SAMPLE_VIDEO_ASSET = "videos/sample_video.mp4"
        private const val MIN_VIDEO_PLAYBACK_MS = 2500L
        private const val TEARDOWN_SETTLE_MS = 500L
    }
}
