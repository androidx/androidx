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

@file:Suppress("RestrictedApiAndroidX", "DEPRECATION")

package androidx.xr.scenecore.integration.tests

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.resetSpatialInteraction
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for the [SurfaceEntity] Custom Mesh CUJ
 * (`SurfaceEntityCustomMeshActivity` scenario).
 *
 * Verifies creating, streaming video (`videos/sample_video.mp4` from `prebuilts/androidx/xr/assets`
 * via [MediaPlayer]) to, inspecting, and tearing down [SurfaceEntity] instances backed by
 * [SurfaceEntity.Shape.CustomMesh] across:
 * - Single-eye triangle list meshes ([SurfaceEntity.DrawMode.TRIANGLES]) with direct and non-direct
 *   buffers.
 * - Stereoscopic left/right eye indexed triangle strip quad meshes
 *   ([SurfaceEntity.DrawMode.TRIANGLE_STRIP]).
 * - Stereoscopic left/right eye triangle fan meshes ([SurfaceEntity.DrawMode.TRIANGLE_FAN]).
 * - Dynamic shape switching, stereo mode transitions, edge feathering, and alpha mask textures.
 */
@SuppressLint("RestrictedApiAndroidX")
@RunWith(AndroidJUnit4::class)
@LargeTest
class SurfaceEntityCustomMeshTest {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var appContext: Context
    private lateinit var session: Session
    private lateinit var videoSession: VideoPlaybackSession
    private var surfaceEntity: SurfaceEntity? = null

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        videoSession = VideoPlaybackSession(appContext)
    }

    /**
     * Cleanly stops and detaches [MediaPlayer] instances and disposes any [SurfaceEntity] while the
     * [Session] and `SplitEngineBridge` are still active, preventing asynchronous `imp_bg_wk`
     * texture release calls from racing with `SpatialRenderingRuntime.destroy()` during
     * `Activity.onDestroy()`.
     */
    @After
    fun tearDown() {
        resetSpatialInteraction()
        if (this::videoSession.isInitialized) {
            videoSession.release()
        }
        if (this::session.isInitialized) {
            runCatching {
                activityScenarioRule.scenario.onActivity {
                    surfaceEntity?.removeAllComponents()
                    surfaceEntity?.parent = null
                    surfaceEntity?.dispose()
                    surfaceEntity = null
                    session.scene.mainPanelEntity.setEnabled(true)
                }
            }
            SystemClock.sleep(TEARDOWN_SETTLE_MS)
        }
    }

    @Test
    @XrDeviceTest
    fun surfaceEntity_customTriangleMesh_playsVideoAndAttachesSubspace() {
        initSessionAndEnterFullSpace()

        lateinit var customMeshShape: SurfaceEntity.Shape.CustomMesh
        lateinit var dimensions: FloatSize3d
        lateinit var activityPose: Pose

        activityScenarioRule.scenario.onActivity {
            val triangleMesh = createTriangleMeshDirect()
            customMeshShape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = triangleMesh,
                    drawMode = SurfaceEntity.DrawMode.TRIANGLES,
                )
            val entity =
                SurfaceEntity.create(
                    session = session,
                    pose = SURFACE_POSE,
                    shape = customMeshShape,
                    stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                    superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                    surfaceProtection = SurfaceEntity.SurfaceProtection.NONE,
                    parent = session.scene.activitySpace,
                )
            surfaceEntity = entity
            videoSession.start(entity.getSurface())

            dimensions = entity.dimensions
            activityPose = entity.getPose(Space.ACTIVITY)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(customMeshShape.drawMode).isEqualTo(SurfaceEntity.DrawMode.TRIANGLES)
        assertThat(customMeshShape.rightEye).isNull()
        assertThat(dimensions.width).isWithin(TOLERANCE).of(1.0f)
        assertThat(dimensions.height).isWithin(TOLERANCE).of(1.0f)
        assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
        assertPose(activityPose, SURFACE_POSE, TOLERANCE)

        videoSession.awaitAndAssertVideoRendered()
        val subspaceNode = awaitSurfaceSubspaceNode(expectPresent = true)
        assertThat(subspaceNode).isNotNull()

        // Stop video and destroy the SurfaceEntity (equivalent to clicking "End Video" in the CUJ).
        stopVideoAndDestroySurfaceEntity()
        awaitSurfaceSubspaceNode(expectPresent = false)
    }

    @Test
    @XrDeviceTest
    fun surfaceEntity_customTriangleMeshNonDirect_copiesBuffersToDirectAndPlaysVideo() {
        initSessionAndEnterFullSpace()

        lateinit var nonDirectMesh: SurfaceEntity.Shape.TriangleMesh
        lateinit var dimensions: FloatSize3d

        activityScenarioRule.scenario.onActivity {
            nonDirectMesh = createTriangleMeshNonDirect()
            val customMeshShape = SurfaceEntity.Shape.CustomMesh(leftEye = nonDirectMesh)
            val entity =
                SurfaceEntity.create(
                    session = session,
                    pose = SURFACE_POSE,
                    shape = customMeshShape,
                    stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                    parent = session.scene.activitySpace,
                )
            surfaceEntity = entity
            videoSession.start(entity.getSurface())
            dimensions = entity.dimensions
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // TriangleMesh copies non-direct heap buffers into direct ByteBuffers upon initialization.
        assertThat(nonDirectMesh.positions.isDirect).isTrue()
        assertThat(nonDirectMesh.texCoords.isDirect).isTrue()
        assertThat(dimensions.width).isWithin(TOLERANCE).of(1.0f)
        assertThat(dimensions.height).isWithin(TOLERANCE).of(1.0f)
        assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)

        videoSession.awaitAndAssertVideoRendered()
        val subspaceNode = awaitSurfaceSubspaceNode(expectPresent = true)
        assertThat(subspaceNode).isNotNull()

        stopVideoAndDestroySurfaceEntity()
        awaitSurfaceSubspaceNode(expectPresent = false)
    }

    @Test
    @XrDeviceTest
    fun surfaceEntity_customTriangleStrip_playsStereoQuadVideoWithIndices() {
        initSessionAndEnterFullSpace()

        lateinit var stripShape: SurfaceEntity.Shape.CustomMesh
        lateinit var dimensions: FloatSize3d
        lateinit var activityPose: Pose

        activityScenarioRule.scenario.onActivity {
            val leftEyeMesh = createTriangleStripMeshLeft()
            val rightEyeMesh = createTriangleStripMeshRight()
            stripShape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = leftEyeMesh,
                    rightEye = rightEyeMesh,
                    drawMode = SurfaceEntity.DrawMode.TRIANGLE_STRIP,
                )
            val entity =
                SurfaceEntity.create(
                    session = session,
                    pose = SURFACE_POSE,
                    shape = stripShape,
                    stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                    superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                    surfaceProtection = SurfaceEntity.SurfaceProtection.NONE,
                    parent = session.scene.activitySpace,
                )
            surfaceEntity = entity
            videoSession.start(entity.getSurface())

            dimensions = entity.dimensions
            activityPose = entity.getPose(Space.ACTIVITY)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(stripShape.drawMode).isEqualTo(SurfaceEntity.DrawMode.TRIANGLE_STRIP)
        assertThat(stripShape.leftEye.indices).isNotNull()
        assertThat(stripShape.rightEye).isNotNull()
        assertThat(stripShape.rightEye!!.indices).isNotNull()
        // Left eye spans X [-1.0, 0.0] and right eye spans X [0.0, 1.0], forming a 2.0m x 1.0m
        // quad.
        assertThat(dimensions.width).isWithin(TOLERANCE).of(2.0f)
        assertThat(dimensions.height).isWithin(TOLERANCE).of(1.0f)
        assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
        assertPose(activityPose, SURFACE_POSE, TOLERANCE)

        videoSession.awaitAndAssertVideoRendered()
        val subspaceNode = awaitSurfaceSubspaceNode(expectPresent = true)
        assertThat(subspaceNode).isNotNull()

        stopVideoAndDestroySurfaceEntity()
        awaitSurfaceSubspaceNode(expectPresent = false)
    }

    @Test
    @XrDeviceTest
    fun surfaceEntity_customTriangleFan_playsStereoFanVideo() {
        initSessionAndEnterFullSpace()

        lateinit var fanShape: SurfaceEntity.Shape.CustomMesh
        lateinit var dimensions: FloatSize3d
        lateinit var activityPose: Pose

        activityScenarioRule.scenario.onActivity {
            val leftEyeMesh = createTriangleFanMesh(centerX = -0.5f, centerU = 0.25f)
            val rightEyeMesh = createTriangleFanMesh(centerX = 0.5f, centerU = 0.75f)
            fanShape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = leftEyeMesh,
                    rightEye = rightEyeMesh,
                    drawMode = SurfaceEntity.DrawMode.TRIANGLE_FAN,
                )
            val entity =
                SurfaceEntity.create(
                    session = session,
                    pose = SURFACE_POSE,
                    shape = fanShape,
                    stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                    superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                    surfaceProtection = SurfaceEntity.SurfaceProtection.NONE,
                    parent = session.scene.activitySpace,
                )
            surfaceEntity = entity
            videoSession.start(entity.getSurface())

            dimensions = entity.dimensions
            activityPose = entity.getPose(Space.ACTIVITY)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(fanShape.drawMode).isEqualTo(SurfaceEntity.DrawMode.TRIANGLE_FAN)
        assertThat(fanShape.rightEye).isNotNull()
        // Left fan center (-0.5, 0) + radius 0.5 -> min X = -1.0; Right fan center (0.5, 0) +
        // radius 0.5 -> max X = 1.0.
        // 6-vertex hexagon Y extents are +/- 0.5 * sin(60 deg) = +/- sqrt(3)/4 -> height =
        // sqrt(3)/2.
        val expectedFanHeight = sqrt(3.0f) / 2.0f
        assertThat(dimensions.width).isWithin(TOLERANCE).of(2.0f)
        assertThat(dimensions.height).isWithin(TOLERANCE).of(expectedFanHeight)
        assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
        assertPose(activityPose, SURFACE_POSE, TOLERANCE)

        videoSession.awaitAndAssertVideoRendered()
        val subspaceNode = awaitSurfaceSubspaceNode(expectPresent = true)
        assertThat(subspaceNode).isNotNull()

        stopVideoAndDestroySurfaceEntity()
        awaitSurfaceSubspaceNode(expectPresent = false)
    }

    @Test
    @XrDeviceTest
    fun surfaceEntity_customMesh_updatesShapeStereoModeFeatheringAndAlphaMaskDuringPlayback() {
        initSessionAndEnterFullSpace()

        lateinit var triangleDimensions: FloatSize3d
        lateinit var stripDimensions: FloatSize3d
        lateinit var fanDimensions: FloatSize3d
        lateinit var featherAfterUpdate: SurfaceEntity.EdgeFeatheringParams.RectangleFeather
        var alphaMaskSet = false
        var alphaMaskCleared = false

        activityScenarioRule.scenario.onActivity {
            val entity =
                SurfaceEntity.create(
                    session = session,
                    pose = SURFACE_POSE,
                    shape = SurfaceEntity.Shape.CustomMesh(leftEye = createTriangleMeshDirect()),
                    stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                    parent = session.scene.activitySpace,
                )
            surfaceEntity = entity
            videoSession.start(entity.getSurface())
            triangleDimensions = entity.dimensions
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        videoSession.awaitAndAssertVideoRendered()

        activityScenarioRule.scenario.onActivity {
            val entity = checkNotNull(surfaceEntity)

            // Transition shape to Triangle Strip (left + right eye quads).
            entity.shape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = createTriangleStripMeshLeft(),
                    rightEye = createTriangleStripMeshRight(),
                    drawMode = SurfaceEntity.DrawMode.TRIANGLE_STRIP,
                )
            stripDimensions = entity.dimensions

            // Transition shape to Triangle Fan (left + right eye rings).
            entity.shape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = createTriangleFanMesh(centerX = -0.5f, centerU = 0.25f),
                    rightEye = createTriangleFanMesh(centerX = 0.5f, centerU = 0.75f),
                    drawMode = SurfaceEntity.DrawMode.TRIANGLE_FAN,
                )
            fanDimensions = entity.dimensions

            // Verify stereo mode updates (Mono, Top-Bottom, Side-by-Side) from the player controls.
            entity.stereoMode = SurfaceEntity.StereoMode.MONO
            assertThat(entity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
            entity.stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE
            assertThat(entity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.SIDE_BY_SIDE)
            entity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
            assertThat(entity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)

            // Verify edge feathering updates.
            val feather = SurfaceEntity.EdgeFeatheringParams.RectangleFeather(0.15f, 0.25f)
            entity.edgeFeatheringParams = feather
            featherAfterUpdate =
                entity.edgeFeatheringParams as SurfaceEntity.EdgeFeatheringParams.RectangleFeather
        }

        // Load and apply alpha mask texture on Dispatchers.Main without blocking the main looper
        // inside onActivity while native Impress AssetLoader callbacks complete.
        runBlocking(Dispatchers.Main) {
            val entity = checkNotNull(surfaceEntity)
            Texture.create(session, Paths.get(ALPHA_MASK_TEXTURE_ASSET)).use { alphaMaskTexture ->
                entity.primaryAlphaMaskTexture = alphaMaskTexture
                alphaMaskSet = (entity.primaryAlphaMaskTexture == alphaMaskTexture)
                entity.primaryAlphaMaskTexture = null
                alphaMaskCleared = (entity.primaryAlphaMaskTexture == null)
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(triangleDimensions.width).isWithin(TOLERANCE).of(1.0f)
        assertThat(stripDimensions.width).isWithin(TOLERANCE).of(2.0f)
        assertThat(fanDimensions.width).isWithin(TOLERANCE).of(2.0f)
        assertThat(fanDimensions.height).isWithin(TOLERANCE).of(sqrt(3.0f) / 2.0f)
        assertThat(featherAfterUpdate.leftRight).isWithin(TOLERANCE).of(0.15f)
        assertThat(featherAfterUpdate.topBottom).isWithin(TOLERANCE).of(0.25f)
        assertThat(alphaMaskSet).isTrue()
        assertThat(alphaMaskCleared).isTrue()
        assertThat(videoSession.surface.get()?.isValid).isTrue()

        stopVideoAndDestroySurfaceEntity()
        awaitSurfaceSubspaceNode(expectPresent = false)
    }

    private fun initSessionAndEnterFullSpace() {
        val latch = CountDownLatch(1)
        val listener =
            Consumer<Set<SpatialCapability>> { capabilities ->
                if (capabilities.contains(SpatialCapability.SPATIAL_UI)) {
                    latch.countDown()
                }
            }

        activityScenarioRule.scenario.onActivity { activity ->
            val sessionResult = Session.create(activity = activity)
            check(sessionResult is SessionCreateSuccess) {
                "Failed to create XR session: $sessionResult"
            }
            session = sessionResult.session
            session.scene.addSpatialCapabilitiesChangedListener(listener)
            if (session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI)) {
                latch.countDown()
            } else {
                session.scene.requestFullSpace()
            }
        }

        assertWithMessage("Timed out waiting for the transition to full space mode")
            .that(latch.await(SCENE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
            .isTrue()

        activityScenarioRule.scenario.onActivity {
            session.scene.removeSpatialCapabilitiesChangedListener(listener)
            session.scene.mainPanelEntity.setEnabled(false)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun stopVideoAndDestroySurfaceEntity() {
        videoSession.release()
        activityScenarioRule.scenario.onActivity {
            surfaceEntity?.removeAllComponents()
            surfaceEntity?.parent = null
            surfaceEntity?.dispose()
            surfaceEntity = null
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /**
     * Polls `dumpsys spf_cpm` via [SpatialSceneHelper] until the surface entity subspace node
     * (`stereo_surface_panel_entity_subspace_*`) matches [expectPresent].
     */
    private fun awaitSurfaceSubspaceNode(expectPresent: Boolean): SpatialNode? {
        val packageName = appContext.packageName
        val deadline = SystemClock.uptimeMillis() + SCENE_TIMEOUT_MS
        var lastNode: SpatialNode? = null
        while (SystemClock.uptimeMillis() < deadline) {
            val scene = SpatialSceneHelper.captureSpatialScene()
            val task = scene.findTaskByPackageName(packageName)
            lastNode = task?.findNode { node ->
                node.name.startsWith(SURFACE_ENTITY_SUBSPACE_PREFIX)
            }
            if ((lastNode != null) == expectPresent) {
                return lastNode
            }
            SystemClock.sleep(SCENE_POLL_INTERVAL_MS)
        }
        assertWithMessage(
                "Timed out waiting for surface subspace node ($SURFACE_ENTITY_SUBSPACE_PREFIX) " +
                    "expectPresent=$expectPresent in package $packageName"
            )
            .that(lastNode != null)
            .isEqualTo(expectPresent)
        return lastNode
    }

    /** Single upward-pointing triangle centered on the origin using direct [ByteBuffer]s. */
    private fun createTriangleMeshDirect(): SurfaceEntity.Shape.TriangleMesh {
        val positions = floatArrayOf(-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
        return SurfaceEntity.Shape.TriangleMesh(
            positions = toDirectBuffer(positions.size) { it.asFloatBuffer().put(positions) },
            texCoords = toDirectBuffer(texCoords.size) { it.asFloatBuffer().put(texCoords) },
        )
    }

    /** Single upward-pointing triangle centered on the origin using non-direct heap buffers. */
    private fun createTriangleMeshNonDirect(): SurfaceEntity.Shape.TriangleMesh {
        val positions = floatArrayOf(-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
        return SurfaceEntity.Shape.TriangleMesh(
            positions = FloatBuffer.wrap(positions),
            texCoords = FloatBuffer.wrap(texCoords),
        )
    }

    /** Left-eye quad mesh represented as a triangle strip with indices covering X [-1.0, 0.0]. */
    private fun createTriangleStripMeshLeft(): SurfaceEntity.Shape.TriangleMesh {
        val positions =
            floatArrayOf(-1.0f, -0.5f, 0.0f, 0.0f, -0.5f, 0.0f, -1.0f, 0.5f, 0.0f, 0.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)
        val indices = intArrayOf(0, 1, 2, 3)
        return SurfaceEntity.Shape.TriangleMesh(
            positions = toDirectBuffer(positions.size) { it.asFloatBuffer().put(positions) },
            texCoords = toDirectBuffer(texCoords.size) { it.asFloatBuffer().put(texCoords) },
            indices = toDirectBuffer(indices.size) { it.asIntBuffer().put(indices) },
        )
    }

    /** Right-eye quad mesh represented as a triangle strip with indices covering X [0.0, 1.0]. */
    private fun createTriangleStripMeshRight(): SurfaceEntity.Shape.TriangleMesh {
        val positions =
            floatArrayOf(0.0f, -0.5f, 0.0f, 1.0f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.5f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f)
        val indices = intArrayOf(0, 1, 2, 3)
        return SurfaceEntity.Shape.TriangleMesh(
            positions = toDirectBuffer(positions.size) { it.asFloatBuffer().put(positions) },
            texCoords = toDirectBuffer(texCoords.size) { it.asFloatBuffer().put(texCoords) },
            indices = toDirectBuffer(indices.size) { it.asIntBuffer().put(indices) },
        )
    }

    /**
     * Closed 6-segment hexagonal triangle fan mesh with a center vertex at ([centerX], 0, 0)
     * surrounded by `FAN_OUTER_VERTICES + 1` outer vertices in a ring of radius 0.5m.
     */
    private fun createTriangleFanMesh(
        centerX: Float,
        centerU: Float,
    ): SurfaceEntity.Shape.TriangleMesh {
        val totalVertices = FAN_OUTER_VERTICES + 2
        val positions = FloatArray(3 * totalVertices)
        val texCoords = FloatArray(2 * totalVertices)

        positions[0] = centerX
        positions[1] = 0.0f
        positions[2] = 0.0f
        texCoords[0] = centerU
        texCoords[1] = 0.5f

        for (i in 0..FAN_OUTER_VERTICES) {
            val posIdx = (i + 1) * 3
            val uvIdx = (i + 1) * 2
            val angle = i.toFloat() / FAN_OUTER_VERTICES * 2.0f * PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            positions[posIdx] = centerX + 0.5f * cosA
            positions[posIdx + 1] = 0.5f * sinA
            positions[posIdx + 2] = 0.0f
            texCoords[uvIdx] = centerU + 0.25f * cosA
            texCoords[uvIdx + 1] = 0.5f + 0.5f * sinA
        }

        return SurfaceEntity.Shape.TriangleMesh(
            positions = toDirectBuffer(positions.size) { it.asFloatBuffer().put(positions) },
            texCoords = toDirectBuffer(texCoords.size) { it.asFloatBuffer().put(texCoords) },
        )
    }

    private fun assertPose(actual: Pose, expected: Pose, tolerance: Float) {
        assertThat(actual.translation.x).isWithin(tolerance).of(expected.translation.x)
        assertThat(actual.translation.y).isWithin(tolerance).of(expected.translation.y)
        assertThat(actual.translation.z).isWithin(tolerance).of(expected.translation.z)
        assertThat(abs(actual.rotation.dot(expected.rotation))).isWithin(tolerance).of(1.0f)
    }

    private fun <T : Buffer> toDirectBuffer(size: Int, block: (ByteBuffer) -> T): T =
        block(ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder())).apply {
            position(0)
        }

    /**
     * Helper that attaches a [MediaPlayer] playing [SAMPLE_VIDEO_ASSET] (`videos/sample_video.mp4`
     * from `prebuilts/androidx/xr/assets`) to a [SurfaceEntity]'s [Surface] and verifies frame
     * delivery.
     */
    private class VideoPlaybackSession(private val context: Context) {
        val surface = AtomicReference<Surface?>(null)
        private val mediaPlayerRef = AtomicReference<MediaPlayer?>(null)
        private val firstFrameRendered = AtomicBoolean(false)
        private val decodedWidth = AtomicInteger(0)
        private val decodedHeight = AtomicInteger(0)
        private val playbackError = AtomicBoolean(false)

        fun start(targetSurface: Surface) {
            assertThat(targetSurface.isValid).isTrue()
            surface.set(targetSurface)
            firstFrameRendered.set(false)
            decodedWidth.set(0)
            decodedHeight.set(0)
            playbackError.set(false)

            // Release any existing MediaPlayer before allocating a new hardware video decoder.
            release()

            val player = MediaPlayer()
            mediaPlayerRef.set(player)
            player.apply {
                context.assets.openFd(SAMPLE_VIDEO_ASSET).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                setSurface(targetSurface)
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
        }

        fun awaitAndAssertVideoRendered() {
            val deadline = SystemClock.uptimeMillis() + VIDEO_RENDER_TIMEOUT_MS
            var rendered = false
            while (SystemClock.uptimeMillis() < deadline) {
                val player = mediaPlayerRef.get()
                val position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
                if (
                    surface.get()?.isValid == true &&
                        decodedWidth.get() > 0 &&
                        decodedHeight.get() > 0 &&
                        (firstFrameRendered.get() || position > 0)
                ) {
                    rendered = true
                    break
                }
                SystemClock.sleep(VIDEO_POLL_INTERVAL_MS)
            }
            assertWithMessage("Timed out waiting for MediaPlayer to render $SAMPLE_VIDEO_ASSET")
                .that(rendered)
                .isTrue()

            // Keep video playing so rendered frames are visually observable on the emulator/device.
            SystemClock.sleep(MIN_VIDEO_PLAYBACK_MS)
            val player = checkNotNull(mediaPlayerRef.get())
            assertThat(playbackError.get()).isFalse()
            assertThat(surface.get()?.isValid).isTrue()
            assertThat(decodedWidth.get()).isGreaterThan(0)
            assertThat(decodedHeight.get()).isGreaterThan(0)
            assertThat(player.isPlaying).isTrue()
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
        const val TOLERANCE = 1e-3f
        const val FAN_OUTER_VERTICES = 6
        const val SCENE_TIMEOUT_MS = 5_000L
        const val SCENE_POLL_INTERVAL_MS = 100L
        const val VIDEO_RENDER_TIMEOUT_MS = 5_000L
        const val VIDEO_POLL_INTERVAL_MS = 50L
        const val MIN_VIDEO_PLAYBACK_MS = 2_500L
        const val TEARDOWN_SETTLE_MS = 500L
        const val SAMPLE_VIDEO_ASSET = "videos/sample_video.mp4"
        const val ALPHA_MASK_TEXTURE_ASSET = "textures/alpha_mask.png"
        const val SURFACE_ENTITY_SUBSPACE_PREFIX = "stereo_surface_panel_entity_subspace_"

        val SURFACE_POSE = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion.Identity)
    }
}
