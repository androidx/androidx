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

@file:Suppress("RestrictedApiAndroidX")

package androidx.xr.scenecore.integration.tests

import android.content.Context
import android.media.MediaPlayer
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SurfaceEntityCustomMeshTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @Test
    fun surfaceEntity_customTriangleMesh_attachesAndPlaysVideo() {
        sessionRule.requestFullSpace()
        val (customMeshShape, entity) =
            sessionRule.onMainThread {
                session.scene.mainPanelEntity.setEnabled(false)
                val triangleMesh = createTriangleMeshDirect()
                val customMeshShape =
                    SurfaceEntity.Shape.CustomMesh(
                        leftEye = triangleMesh,
                        drawMode = SurfaceEntity.DrawMode.TRIANGLES,
                    )
                customMeshShape to
                    SurfaceEntity.create(
                        session = session,
                        pose = SURFACE_POSE,
                        shape = customMeshShape,
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                        surfaceProtection = SurfaceEntity.SurfaceProtection.NONE,
                        parent = session.scene.activitySpace,
                    )
            }
        startVideo(entity).use { video ->
            val dimensions = entity.dimensions
            assertThat(customMeshShape.drawMode).isEqualTo(SurfaceEntity.DrawMode.TRIANGLES)
            assertThat(customMeshShape.rightEye).isNull()
            assertThat(dimensions.width).isWithin(TOLERANCE).of(1.0f)
            assertThat(dimensions.height).isWithin(TOLERANCE).of(1.0f)
            assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
            assertAttachedToActivitySpace(entity)

            video.awaitAndAssertVideoRendered()
            // TODO(b/583873060): Verify video frames composited on custom mesh.
        }
    }

    @Test
    fun surfaceEntity_customTriangleMeshNonDirect_copiesBuffersToDirectAndPlaysVideo() {
        sessionRule.requestFullSpace()
        val (nonDirectMesh, entity) =
            sessionRule.onMainThread {
                session.scene.mainPanelEntity.setEnabled(false)
                val nonDirectMesh = createTriangleMeshNonDirect()
                val customMeshShape = SurfaceEntity.Shape.CustomMesh(leftEye = nonDirectMesh)
                nonDirectMesh to
                    SurfaceEntity.create(
                        session = session,
                        pose = SURFACE_POSE,
                        shape = customMeshShape,
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        parent = session.scene.activitySpace,
                    )
            }
        startVideo(entity).use { video ->
            val dimensions = entity.dimensions

            // TriangleMesh copies non-direct heap buffers into direct ByteBuffers upon
            // initialization.
            assertThat(nonDirectMesh.positions.isDirect).isTrue()
            assertThat(nonDirectMesh.texCoords.isDirect).isTrue()
            assertThat(dimensions.width).isWithin(TOLERANCE).of(1.0f)
            assertThat(dimensions.height).isWithin(TOLERANCE).of(1.0f)
            assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
            assertAttachedToActivitySpace(entity)

            video.awaitAndAssertVideoRendered()
        }
    }

    @Test
    fun surfaceEntity_customTriangleStrip_playsStereoQuadVideoWithIndices() {
        sessionRule.requestFullSpace()
        val (stripShape, entity) =
            sessionRule.onMainThread {
                session.scene.mainPanelEntity.setEnabled(false)
                val leftEyeMesh = createTriangleStripMeshLeft()
                val rightEyeMesh = createTriangleStripMeshRight()
                val stripShape =
                    SurfaceEntity.Shape.CustomMesh(
                        leftEye = leftEyeMesh,
                        rightEye = rightEyeMesh,
                        drawMode = SurfaceEntity.DrawMode.TRIANGLE_STRIP,
                    )
                stripShape to
                    SurfaceEntity.create(
                        session = session,
                        pose = SURFACE_POSE,
                        shape = stripShape,
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                        surfaceProtection = SurfaceEntity.SurfaceProtection.NONE,
                        parent = session.scene.activitySpace,
                    )
            }
        startVideo(entity).use { video ->
            val dimensions = entity.dimensions
            assertThat(stripShape.drawMode).isEqualTo(SurfaceEntity.DrawMode.TRIANGLE_STRIP)
            assertThat(stripShape.leftEye.indices).isNotNull()
            assertThat(stripShape.rightEye).isNotNull()
            assertThat(stripShape.rightEye!!.indices).isNotNull()
            // Left eye spans X [-1.0, 0.0] and right eye spans X [0.0, 1.0], forming a
            // 2.0m x 1.0m quad.
            assertThat(dimensions.width).isWithin(TOLERANCE).of(2.0f)
            assertThat(dimensions.height).isWithin(TOLERANCE).of(1.0f)
            assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
            assertAttachedToActivitySpace(entity)

            video.awaitAndAssertVideoRendered()
        }
    }

    @Test
    fun surfaceEntity_customTriangleFan_playsStereoFanVideo() {
        sessionRule.requestFullSpace()
        val (fanShape, entity) =
            sessionRule.onMainThread {
                session.scene.mainPanelEntity.setEnabled(false)
                val leftEyeMesh = createTriangleFanMesh(centerX = -0.5f, centerU = 0.25f)
                val rightEyeMesh = createTriangleFanMesh(centerX = 0.5f, centerU = 0.75f)
                val fanShape =
                    SurfaceEntity.Shape.CustomMesh(
                        leftEye = leftEyeMesh,
                        rightEye = rightEyeMesh,
                        drawMode = SurfaceEntity.DrawMode.TRIANGLE_FAN,
                    )
                fanShape to
                    SurfaceEntity.create(
                        session = session,
                        pose = SURFACE_POSE,
                        shape = fanShape,
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                        surfaceProtection = SurfaceEntity.SurfaceProtection.NONE,
                        parent = session.scene.activitySpace,
                    )
            }
        startVideo(entity).use { video ->
            val dimensions = entity.dimensions
            assertThat(fanShape.drawMode).isEqualTo(SurfaceEntity.DrawMode.TRIANGLE_FAN)
            assertThat(fanShape.rightEye).isNotNull()
            // Left fan center (-0.5, 0) + radius 0.5 -> min X = -1.0; Right fan center
            // (0.5, 0) + radius 0.5 -> max X = 1.0.
            // 6-vertex hexagon Y extents are +/- 0.5 * sin(60 deg) = +/- sqrt(3)/4 ->
            // height = sqrt(3)/2.
            val expectedFanHeight = sqrt(3.0f) / 2.0f
            assertThat(dimensions.width).isWithin(TOLERANCE).of(2.0f)
            assertThat(dimensions.height).isWithin(TOLERANCE).of(expectedFanHeight)
            assertThat(dimensions.depth).isWithin(TOLERANCE).of(0.0f)
            assertAttachedToActivitySpace(entity)

            video.awaitAndAssertVideoRendered()
        }
    }

    @Test
    fun surfaceEntity_customMesh_updatesShapeStereoModeFeatheringAndAlphaMaskDuringPlayback() {
        sessionRule.requestFullSpace()
        val entity = sessionRule.onMainThread {
            session.scene.mainPanelEntity.setEnabled(false)
            SurfaceEntity.create(
                session = session,
                pose = SURFACE_POSE,
                shape = SurfaceEntity.Shape.CustomMesh(leftEye = createTriangleMeshDirect()),
                stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                parent = session.scene.activitySpace,
            )
        }
        startVideo(entity).use { video ->
            val triangleDimensions = entity.dimensions
            assertAttachedToActivitySpace(entity)
            video.awaitAndAssertVideoRendered()

            sessionRule.onMainThread {
                // Transition shape to Triangle Strip (left + right eye quads).
                entity.shape =
                    SurfaceEntity.Shape.CustomMesh(
                        leftEye = createTriangleStripMeshLeft(),
                        rightEye = createTriangleStripMeshRight(),
                        drawMode = SurfaceEntity.DrawMode.TRIANGLE_STRIP,
                    )
                val stripDimensions = entity.dimensions

                // Transition shape to Triangle Fan (left + right eye rings).
                entity.shape =
                    SurfaceEntity.Shape.CustomMesh(
                        leftEye = createTriangleFanMesh(centerX = -0.5f, centerU = 0.25f),
                        rightEye = createTriangleFanMesh(centerX = 0.5f, centerU = 0.75f),
                        drawMode = SurfaceEntity.DrawMode.TRIANGLE_FAN,
                    )
                val fanDimensions = entity.dimensions

                // Verify stereo mode updates (Mono, Top-Bottom, Side-by-Side) from player
                // controls.
                entity.stereoMode = SurfaceEntity.StereoMode.MONO
                assertThat(entity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
                entity.stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE
                assertThat(entity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.SIDE_BY_SIDE)
                entity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
                assertThat(entity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)

                // Verify edge feathering updates are accepted. The getter returns the cached
                // value, so this only checks that the setter does not throw and keeps the
                // requested params.
                val feather = SurfaceEntity.EdgeFeatheringParams.RectangleFeather(0.15f, 0.25f)
                entity.edgeFeatheringParams = feather
                val featherAfterUpdate =
                    entity.edgeFeatheringParams
                        as SurfaceEntity.EdgeFeatheringParams.RectangleFeather

                // Texture.create suspends on the main dispatcher without blocking the main looper
                // while native Impress AssetLoader callbacks complete. The alpha mask getter
                // returns the cached value, so this only checks that setting and clearing it does
                // not throw.
                val alphaMaskPath = Paths.get(ALPHA_MASK_TEXTURE_ASSET)
                Texture.create(session, alphaMaskPath).use { alphaMaskTexture ->
                    entity.primaryAlphaMaskTexture = alphaMaskTexture
                    assertThat(entity.primaryAlphaMaskTexture).isSameInstanceAs(alphaMaskTexture)
                    entity.primaryAlphaMaskTexture = null
                    assertThat(entity.primaryAlphaMaskTexture).isNull()
                }

                assertThat(triangleDimensions.width).isWithin(TOLERANCE).of(1.0f)
                assertThat(stripDimensions.width).isWithin(TOLERANCE).of(2.0f)
                assertThat(fanDimensions.width).isWithin(TOLERANCE).of(2.0f)
                assertThat(fanDimensions.height).isWithin(TOLERANCE).of(sqrt(3.0f) / 2.0f)
                assertThat(featherAfterUpdate.leftRight).isWithin(TOLERANCE).of(0.15f)
                assertThat(featherAfterUpdate.topBottom).isWithin(TOLERANCE).of(0.25f)
                assertThat(video.surface.get()?.isValid).isTrue()
            }
        }
    }

    /**
     * Starts [SAMPLE_VIDEO_ASSET] on [entity]'s [Surface]. Must be called from the instrumentation
     * thread.
     *
     * The [MediaPlayer] is not owned by the XR Session, so the caller must close the returned
     * [VideoPlaybackSession] (with `use { }`) at the end of the test body. This releases the player
     * before the per-test Activity is destroyed and [entity] is torn down, which prevents
     * asynchronous `imp_bg_wk` texture release calls from racing with
     * `SpatialRenderingRuntime.destroy()`. If starting playback fails, the player is released
     * before the exception is rethrown.
     */
    private fun startVideo(entity: SurfaceEntity): VideoPlaybackSession {
        val video = sessionRule.onMainThread {
            VideoPlaybackSession(activity).apply { attach(entity.getSurface()) }
        }
        try {
            video.prepareAndStart()
        } catch (e: Throwable) {
            video.close()
            throw e
        }
        return video
    }

    /** Asserts that [entity] is enabled under the activity space at [SURFACE_POSE]. */
    private fun assertAttachedToActivitySpace(entity: SurfaceEntity) {
        assertThat(entity.parent).isSameInstanceAs(sessionRule.session.scene.activitySpace)
        assertThat(entity.isEnabled(includeParents = true)).isTrue()
        assertPose(entity.getPose(Space.ACTIVITY), SURFACE_POSE, TOLERANCE)
        assertThat(entity.getScale()).isWithin(TOLERANCE).of(1.0f)
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

    private fun <T : Buffer> toDirectBuffer(size: Int, block: (ByteBuffer) -> T): T =
        block(ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder())).apply {
            position(0)
        }

    /**
     * Helper that attaches a [MediaPlayer] playing [SAMPLE_VIDEO_ASSET] (`videos/sample_video.mp4`
     * from `prebuilts/androidx/xr/assets`) to a [SurfaceEntity]'s [Surface] and verifies frame
     * delivery. [close] stops and releases the player.
     */
    private class VideoPlaybackSession(private val context: Context) : AutoCloseable {
        val surface = AtomicReference<Surface?>(null)
        private val mediaPlayerRef = AtomicReference<MediaPlayer?>(null)
        private val firstFrameRendered = AtomicBoolean(false)
        private val decodedWidth = AtomicInteger(0)
        private val decodedHeight = AtomicInteger(0)
        private val playbackError = AtomicBoolean(false)

        /**
         * Creates a [MediaPlayer] that renders to [targetSurface].
         *
         * Must be called on the main thread so [MediaPlayer] callbacks are delivered on the main
         * looper. Call [prepareAndStart] afterwards from the instrumentation thread.
         */
        fun attach(targetSurface: Surface) {
            assertThat(targetSurface.isValid).isTrue()
            surface.set(targetSurface)
            firstFrameRendered.set(false)
            decodedWidth.set(0)
            decodedHeight.set(0)
            playbackError.set(false)

            // Release any existing MediaPlayer before allocating a new hardware video decoder.
            close()

            val player = MediaPlayer()
            mediaPlayerRef.set(player)
            player.apply {
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
            }
        }

        /**
         * Loads [SAMPLE_VIDEO_ASSET] into the player created by [attach] and starts playback.
         *
         * Must be called from the instrumentation thread, because `setDataSource` and `prepare`
         * block.
         */
        fun prepareAndStart() {
            val player = checkNotNull(mediaPlayerRef.get()) { "attach() must be called first." }
            context.assets.openFd(SAMPLE_VIDEO_ASSET).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.prepare()
            player.start()
        }

        /**
         * Blocks until the first video frame is rendered, then asserts the player state. Must be
         * called from the instrumentation thread.
         */
        fun awaitAndAssertVideoRendered() {
            try {
                waitUntil(
                    description = "MediaPlayer to render $SAMPLE_VIDEO_ASSET",
                    timeout = VIDEO_RENDER_TIMEOUT,
                    pollInterval = VIDEO_POLL_INTERVAL,
                ) {
                    surface.get()?.isValid == true &&
                        decodedWidth.get() > 0 &&
                        decodedHeight.get() > 0 &&
                        firstFrameRendered.get()
                }
            } catch (e: AssertionError) {
                throw AssertionError(
                    "Timed out waiting for MediaPlayer to render $SAMPLE_VIDEO_ASSET " +
                        "(firstFrameRendered=${firstFrameRendered.get()}, " +
                        "size=${decodedWidth.get()}x${decodedHeight.get()}, " +
                        "error=${playbackError.get()})",
                    e,
                )
            }

            val player = checkNotNull(mediaPlayerRef.get())
            assertThat(playbackError.get()).isFalse()
            assertThat(surface.get()?.isValid).isTrue()
            assertThat(decodedWidth.get()).isGreaterThan(0)
            assertThat(decodedHeight.get()).isGreaterThan(0)
            assertThat(player.isPlaying).isTrue()
        }

        override fun close() {
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
        const val SAMPLE_VIDEO_ASSET = "videos/sample_video.mp4"
        const val ALPHA_MASK_TEXTURE_ASSET = "textures/alpha_mask.png"

        val VIDEO_RENDER_TIMEOUT = 5.seconds
        val VIDEO_POLL_INTERVAL = 50.milliseconds
        val SURFACE_POSE = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion.Identity)
    }
}
