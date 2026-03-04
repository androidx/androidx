/*
 * Copyright 2025 The Android Open Source Project
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

/*
 * For the proper playback of MV-HEVC videos, this sample requires ExoPlayer
 * 1.6.0 or higher and a multiview hardware decoder on the device.
 */

package androidx.xr.scenecore.testapp.surfacecustommesh

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.arcore.ArDevice
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.scene
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import kotlinx.coroutines.launch

private const val TAG = "JXR-SurfaceEntity-SurfaceEntityCustomMeshActivity"

object VideoButtonColors {
    val StandardPlayback = Color(0xFF42A5F5) // Blue 400
    val Multiview = Color(0xFF26A69A) // Teal 400
    val VR = Color(0xFF7E57C2) // Deep Purple 400
    val DRM = Color(0xFF78909C) // Blue Grey 400
    val HDR = Color(0xFF66BB6A) // Green 400
    val Transformations = Color(0xFF757575) // Grey 600
    val DefaultButton = Color(0xFF42A5F5) // Blue 400
}

class SurfaceEntityCustomMeshActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
    private val activity = this

    private val REQUEST_READ_MEDIA_VIDEO: Int = 1
    private var hasPermission: Boolean = false

    // TODO: b/393150833 - Refactor these vars into a common UI struct to reduce nullability.
    private var surfaceEntity: SurfaceEntity? = null
    private var movableComponent: MovableComponent? = null // movable component for surfaceEntity
    private var movableComponentMP: MovableComponent? = null // movable component for mainPanel
    private var videoPlaying by mutableStateOf<Boolean>(false)
    private var controlPanelEntity: PanelEntity? = null
    private var alphaMaskTexture: Texture? = null
    private var movieParent: Entity? = null
    // This is a custom move listener which moves the movieParent instead of the surfaceEntity
    // directly. This allows for the SurfaceEntity to be independently rotated without impacting
    // the player controls which are attached to the movieParent.
    private var moveListener =
        object : EntityMoveListener {
            override fun onMoveUpdate(
                entity: Entity,
                currentInputRay: Ray,
                currentPose: Pose,
                currentScale: Float,
            ) {
                check(entity == surfaceEntity) {
                    "Listener should only be attached to surfaceEntity."
                }
                var curParentPose = movieParent!!.getPose()
                // Apply the currentPose to the movieParent to move the surfaceEntity.
                movieParent?.setPose(curParentPose.compose(currentPose))
            }
        }

    private val triangleMesh by lazy {
        val positions = floatArrayOf(-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)

        val posBuffer =
            ByteBuffer.allocateDirect(positions.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(positions)
                    position(0)
                }
            }

        val texCoordsBuffer =
            ByteBuffer.allocateDirect(texCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(texCoords)
                    position(0)
                }
            }

        SurfaceEntity.Shape.TriangleMesh(positions = posBuffer, texCoords = texCoordsBuffer)
    }

    private val triangleMeshByTriangleStripLeft by lazy {
        // A quad mesh represented as two triangles with indices.
        val positions =
            floatArrayOf(-1.0f, -0.5f, 0.0f, 0.0f, -0.5f, 0.0f, -1.0f, 0.5f, 0.0f, 0.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)
        val indices = intArrayOf(0, 1, 2, 3)

        val posBuffer =
            ByteBuffer.allocateDirect(positions.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(positions)
                    position(0)
                }
            }

        val texCoordsBuffer =
            ByteBuffer.allocateDirect(texCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(texCoords)
                    position(0)
                }
            }

        val indicesBuffer =
            ByteBuffer.allocateDirect(indices.size * 4).run { // 4 bytes per int
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices)
                    position(0)
                }
            }

        SurfaceEntity.Shape.TriangleMesh(
            positions = posBuffer,
            texCoords = texCoordsBuffer,
            indices = indicesBuffer,
        )
    }

    private val triangleMeshByTriangleStripRight by lazy {
        // A quad mesh represented as two triangles with indices.
        val positions =
            floatArrayOf(0.0f, -0.5f, 0.0f, 1.0f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 1.0f, 0.5f, 0.0f)
        val texCoords = floatArrayOf(0.5f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f)
        val indices = intArrayOf(0, 1, 2, 3)

        val posBuffer =
            ByteBuffer.allocateDirect(positions.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(positions)
                    position(0)
                }
            }

        val texCoordsBuffer =
            ByteBuffer.allocateDirect(texCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(texCoords)
                    position(0)
                }
            }

        val indicesBuffer =
            ByteBuffer.allocateDirect(indices.size * 4).run { // 4 bytes per int
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices)
                    position(0)
                }
            }

        SurfaceEntity.Shape.TriangleMesh(
            positions = posBuffer,
            texCoords = texCoordsBuffer,
            indices = indicesBuffer,
        )
    }

    private val triangleMeshByTriangleFanLeft by lazy {
        // A triangle fan version of a circle. One center point surrounded by
        // numOuterVertices in a ring.
        // We don't expect this to render correctly; The triangle fan will fall
        // back to triangle list because it is not yet natively supported.
        val numOuterVertices = 6
        val totalVertexElements = numOuterVertices + 1

        val positions = FloatArray(3 * totalVertexElements)
        val texCoords = FloatArray(2 * totalVertexElements)

        val centerPos = floatArrayOf(-0.5f, 0.0f, 0.0f)
        val centerUV = floatArrayOf(0.25f, 0.5f)

        // Vertex 1: Center
        positions[0] = centerPos[0]
        positions[1] = centerPos[1]
        positions[2] = centerPos[2]
        texCoords[0] = centerUV[0]
        texCoords[1] = centerUV[1]

        for (i in 0 until numOuterVertices) {
            val posIdx = (i + 1) * 3
            val uvIdx = (i + 1) * 2

            val angle = i.toFloat() / numOuterVertices * 2.0f * Math.PI.toFloat()

            val cosA = kotlin.math.cos(angle)
            val sinA = kotlin.math.sin(angle)

            val outerPos = floatArrayOf(-0.5f + 0.5f * cosA, 0.5f * sinA, 0.0f)
            val outerUV = floatArrayOf(0.25f + 0.25f * cosA, 0.5f + 0.5f * sinA)

            // Vertex: OuterI
            positions[posIdx + 0] = outerPos[0]
            positions[posIdx + 1] = outerPos[1]
            positions[posIdx + 2] = outerPos[2]
            texCoords[uvIdx + 0] = outerUV[0]
            texCoords[uvIdx + 1] = outerUV[1]
        }

        val posBuffer =
            ByteBuffer.allocateDirect(positions.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(positions)
                    position(0)
                }
            }

        val texCoordsBuffer =
            ByteBuffer.allocateDirect(texCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(texCoords)
                    position(0)
                }
            }

        SurfaceEntity.Shape.TriangleMesh(positions = posBuffer, texCoords = texCoordsBuffer)
    }

    private val triangleMeshByTriangleFanRight by lazy {
        // A triangle fan version of a circle. One center point surrounded by
        // numOuterVertices in a ring.
        // We don't expect this to render correctly; The triangle fan will fall
        // back to triangle list because it is not yet natively supported.
        val numOuterVertices = 6
        val totalVertexElements = numOuterVertices + 1

        val positions = FloatArray(3 * totalVertexElements)
        val texCoords = FloatArray(2 * totalVertexElements)

        // Right eye version
        val centerPos = floatArrayOf(0.5f, 0.0f, 0.0f)
        val centerUV = floatArrayOf(0.75f, 0.5f)

        // Vertex 1: Center
        positions[0] = centerPos[0]
        positions[1] = centerPos[1]
        positions[2] = centerPos[2]
        texCoords[0] = centerUV[0]
        texCoords[1] = centerUV[1]

        for (i in 0 until numOuterVertices) {
            val posIdx = (i + 1) * 3
            val uvIdx = (i + 1) * 2

            val angle = i.toFloat() / numOuterVertices * 2.0f * Math.PI.toFloat()

            val cosA = kotlin.math.cos(angle)
            val sinA = kotlin.math.sin(angle)

            // Right eye version
            val outerPos = floatArrayOf(0.5f + 0.5f * cosA, 0.5f * sinA, 0.0f)
            val outerUV = floatArrayOf(0.75f + 0.25f * cosA, 0.5f + 0.5f * sinA)

            // Vertex: OuterI
            positions[posIdx + 0] = outerPos[0]
            positions[posIdx + 1] = outerPos[1]
            positions[posIdx + 2] = outerPos[2]
            texCoords[uvIdx + 0] = outerUV[0]
            texCoords[uvIdx + 1] = outerUV[1]
        }

        val posBuffer =
            ByteBuffer.allocateDirect(positions.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(positions)
                    position(0)
                }
            }

        val texCoordsBuffer =
            ByteBuffer.allocateDirect(texCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(texCoords)
                    position(0)
                }
            }

        SurfaceEntity.Shape.TriangleMesh(positions = posBuffer, texCoords = texCoordsBuffer)
    }

    private var currentPoseForVideo: Pose? = null
    private var currentVideoSize: VideoSize? = null
    // When the video is recorded using a rotated phone, the encoded video
    // bitstream may have a different orientation than the device's display.
    // To correct the orientation, we need to rotate the video content by
    // the same amount as the device's display.
    private var currentVideoRotationDegrees: Int = 0
    private var currentPixelAspectRatio: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = (Session.create(this) as SessionCreateSuccess).session
        session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN))
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        checkExternalStoragePermission()

        // Set up the MoveableComponent so the user can move the Main Panel out of the way of
        // video canvases which appear behind it.
        if (movableComponentMP == null) {
            movableComponentMP = MovableComponent.createSystemMovable(session)
            val unused = session.scene.mainPanelEntity.addComponent(movableComponentMP!!)
        }

        // This will be re-used throughout the life of the Activity.
        movieParent = Entity.create(session, "movieParent")

        lifecycleScope.launch {
            alphaMaskTexture = Texture.create(session, Paths.get("textures", "alpha_mask.png"))
        }
        setContent { HelloWorld(session, activity) }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        if (alphaMaskTexture != null) {
            // TODO: b/450175596 - Restore alpha mask closure.
            alphaMaskTexture = null
        }
    }

    /** A simple composable that toggles the passthrough on and off to test environment changes. */
    // TODO: b/324947709 - Refactor common @Composable code into a utility library for common usage
    // across sample apps.
    @Composable
    fun HelloWorld(session: Session, activity: SurfaceEntityCustomMeshActivity) {
        // Add a panel to the main activity with a button to toggle passthrough
        LaunchedEffect(Unit) {
            activity.setContentView(
                createButtonViewUsingCompose(activity = activity, session = session)
            )
        }
    }

    private fun togglePassthrough(session: Session) {
        val passthroughOpacity: Float = session.scene.spatialEnvironment.currentPassthroughOpacity
        Log.i(TAG, "TogglePassthrough!")
        when (passthroughOpacity) {
            0.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 1.0f
            1.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        }
    }

    // Request the external storage permission so that we can read large files from the SDCard
    private fun checkExternalStoragePermission() {
        if (
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = false
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO),
                REQUEST_READ_MEDIA_VIDEO,
            )
        } else {
            hasPermission = true
        }
    }

    private fun setupControlPanel(session: Session, arDevice: ArDevice) {
        // Dispose previous control panel if it exists
        controlPanelEntity?.dispose()
        controlPanelEntity = null

        // Technically this leaks, but it's a sample / test app.
        val panelContentView =
            ComposeView(this).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { VideoPlayerControls(session, arDevice) }
            }

        controlPanelEntity =
            PanelEntity.create(
                session,
                panelContentView,
                // These are about the right size to fit the buttons without a lot of invisible
                // panel edges
                IntSize2d(640, 480),
                "playerControls",
                Pose.Identity,
            )

        // TODO: b/413478924 - Use controlPanelEntity.view when the api is available.
        val parentView: View =
            if (panelContentView.parent != null && panelContentView.parent is View)
                panelContentView.parent as View
            else panelContentView

        parentView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        parentView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        parentView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
    }

    fun initializeExoPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    fun fastRewindOneSec() {
        if (videoPlaying) {
            exoPlayer?.seekTo(exoPlayer!!.currentPosition - 1000)
        }
    }

    fun fastForwardOneSec() {
        if (videoPlaying) {
            exoPlayer?.seekTo(exoPlayer!!.currentPosition + 1000)
        }
    }

    fun destroySurfaceEntity() {
        videoPlaying = false
        exoPlayer?.release()
        exoPlayer = null

        surfaceEntity?.dispose()
        surfaceEntity = null

        controlPanelEntity?.dispose()
        controlPanelEntity = null

        currentPoseForVideo = null
        currentVideoSize = null
        currentVideoRotationDegrees = 0
    }

    private fun createButtonViewUsingCompose(
        activity: SurfaceEntityCustomMeshActivity,
        session: Session,
    ): View {
        val arDevice = ArDevice.getInstance(session)
        val view =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { SurfaceEntityCustomMeshActivityUI(session, arDevice, activity) }
            }
        view.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        view.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        return view
    }

    @Composable
    fun VideoPlayerControls(session: Session, arDevice: ArDevice) {
        var featherRadiusX by remember { mutableFloatStateOf(0.0f) }
        var featherRadiusY by remember { mutableFloatStateOf(0.0f) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { fastRewindOneSec() }) { Text(text = "1s-", fontSize = 10.sp) }
                Button(onClick = { fastForwardOneSec() }) { Text(text = "1s+", fontSize = 10.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { destroySurfaceEntity() }) {
                    Text(text = "End Video", fontSize = 10.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Feather Radius", fontSize = 10.sp)
                Column {
                    Slider(
                        value = featherRadiusX,
                        onValueChange = {
                            featherRadiusX = it
                            surfaceEntity!!.edgeFeatheringParams =
                                SurfaceEntity.EdgeFeatheringParams.RectangleFeather(
                                    featherRadiusX,
                                    featherRadiusY,
                                )
                        },
                        valueRange = 0.0f..0.5f,
                    )
                    Slider(
                        value = featherRadiusY,
                        onValueChange = {
                            featherRadiusY = it
                            surfaceEntity!!.edgeFeatheringParams =
                                SurfaceEntity.EdgeFeatheringParams.RectangleFeather(
                                    featherRadiusX,
                                    featherRadiusY,
                                )
                        },
                        valueRange = 0.0f..0.5f,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.MONO }) {
                    Text(text = "Mono", fontSize = 10.sp)
                }
                Button(
                    onClick = { surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM }
                ) {
                    Text(text = "Top-Bottom", fontSize = 10.sp)
                }
                Button(
                    onClick = { surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE }
                ) {
                    Text(text = "Side-by-Side", fontSize = 10.sp)
                }
            } // end row
        } // end column
    }

    // Note that pose here will be ignored if the shape is not a Quad
    // TODO: b/450174938 - Update this to take a Pose for the controlPanel
    @Suppress("UnsafeOptInUsageError")
    @Composable
    fun PlayVideoButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        videoUri: String,
        stereoMode: SurfaceEntity.StereoMode,
        pose: Pose,
        shape: SurfaceEntity.Shape,
        buttonText: String,
        buttonColor: Color = VideoButtonColors.DefaultButton,
        enabled: Boolean = true,
        loop: Boolean = false,
        protected: Boolean = false,
    ) {
        val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
        val currentExoPlayer = remember { mutableStateOf(exoPlayer) }
        val file = File(videoUri)
        if (!file.exists()) {
            Toast.makeText(
                    activity,
                    "File ($videoUri) does not exist. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        Button(
            enabled = enabled,
            onClick = {
                var actualPose = pose
                if (
                    !(shape is SurfaceEntity.Shape.Quad || shape is SurfaceEntity.Shape.CustomMesh)
                ) {
                    actualPose =
                        session.scene.perceptionSpace.transformPoseTo(
                            arDevice.state.value.devicePose,
                            session.scene.activitySpace,
                        )
                }

                // Create SurfaceEntity and MovableComponent if they don't exist.
                if (surfaceEntity == null) {

                    val surfaceContentLevel =
                        if (protected) {
                            SurfaceEntity.SurfaceProtection.PROTECTED
                        } else {
                            SurfaceEntity.SurfaceProtection.NONE
                        }

                    movieParent!!.parent = session.scene.activitySpace
                    movieParent!!.setPose(actualPose)

                    surfaceEntity =
                        SurfaceEntity.create(
                            session = session,
                            pose = Pose.Identity,
                            shape = shape,
                            stereoMode = stereoMode,
                            superSampling = SurfaceEntity.SuperSampling.PENTAGON,
                            surfaceProtection = surfaceContentLevel,
                        )

                    surfaceEntity?.parent = movieParent!!
                    surfaceEntity?.setPose(Pose.Identity)

                    // Make the video player movable (to make it easier to look at it from different
                    // angles and distances) (only on quad canvas)
                    movableComponent =
                        MovableComponent.createCustomMovable(
                            session,
                            scaleInZ = false,
                            executor = Runnable::run,
                            entityMoveListener = moveListener,
                        )

                    // The quad has a radius of 1.0 meters
                    movableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)

                    if (shape is SurfaceEntity.Shape.Quad) {
                        val unused = surfaceEntity!!.addComponent(movableComponent!!)
                    }
                }
                currentPoseForVideo = actualPose

                // Get or initialize the ExoPlayer.
                val player = initializeExoPlayer(activity)
                // Update the Composable's state.
                currentExoPlayer.value = player
                // Set the video surface.
                player.setVideoSurface(surfaceEntity!!.getSurface())
                // Clear previous media items.
                player.clearMediaItems()

                val mediaItem =
                    if (protected) {
                        MediaItem.Builder()
                            .setUri(videoUri)
                            .setDrmConfiguration(
                                DrmConfiguration.Builder(C.WIDEVINE_UUID)
                                    .setLicenseUri(drmLicenseUrl)
                                    .build()
                            )
                            .build()
                    } else {
                        MediaItem.fromUri(videoUri)
                    }
                player.setMediaItem(mediaItem)

                player.addListener(
                    object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            Log.i(TAG, "Raw ${videoSize.width}x${videoSize.height}")
                            currentVideoSize = videoSize
                        }

                        override fun onTracksChanged(tracks: Tracks) {
                            super.onTracksChanged(tracks)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            Log.i(TAG, "onPlaybackStateChanged: $playbackState")

                            if (playbackState == Player.STATE_ENDED) {
                                destroySurfaceEntity()
                            } else {
                                // Note that this doesn't exactly line up with the ExoPlayer
                                // isPlaying property, because the UI (for this app) counts as
                                // "playing" even when buffering or paused.
                                videoPlaying = true
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "Player error: $error")
                        }
                    }
                )
                if (loop) {
                    player.setRepeatMode(Player.REPEAT_MODE_ALL)
                }
                player.playWhenReady = true
                player.prepare()
                setupControlPanel(session, arDevice)
            },
            modifier = Modifier.fillMaxWidth().height(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }

    /**
     * A [Composable] button that plays the "Big Buck Bunny" video on a custom triangular mesh.
     *
     * This button, when clicked, creates a [SurfaceEntity] with a [SurfaceEntity.Shape.CustomMesh]
     * defined by a simple triangle. The video is monoscopic, with mesh data provided only for the
     * left eye but it will display on both eyes.
     *
     * @param session The active [Session].
     * @param arDevice The active [ArDevice].
     * @param activity The current [Activity].
     * @param enabled Whether the button is enabled.
     *
     * TODO: b/470324527 - Allow the CustomMesh API to provide only left eye mesh and display
     *   exclusively on the left eye.
     */
    @Composable
    fun TriangleMeshButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to "/sdcard/Download/vid_bigbuckbunny.mp4".
            videoUri =
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/vid_bigbuckbunny.mp4",
            stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.CustomMesh(leftEye = triangleMesh),
            buttonText = "Play Big Buck Bunny, Triangle Mesh",
            buttonColor = VideoButtonColors.StandardPlayback,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun TriangleStripButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
        loop: Boolean = false,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to
            // "/sdcard/Download/vid_bigbuckbunny.mp4".
            videoUri =
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/vid_bigbuckbunny.mp4",
            stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = triangleMeshByTriangleStripLeft,
                    rightEye = triangleMeshByTriangleStripRight,
                    drawMode = SurfaceEntity.DrawMode.TRIANGLE_STRIP,
                ),
            buttonText = "Play Big Buck Bunny, Triangle Strip",
            buttonColor = VideoButtonColors.StandardPlayback,
            enabled = enabled,
            protected = false,
        )
    }

    // We don't expect this to render correctly; The triangle fan will fall back to a triangle
    // list because it is not yet natively supported.
    // TODO: b/474464351 - Crash in C++ after fallbacks from Triangle Fan to Triangles
    @Composable
    fun TriangleFanButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = false,
        loop: Boolean = false,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to
            // "/sdcard/Download/vid_bigbuckbunny.mp4".
            videoUri =
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/vid_bigbuckbunny.mp4",
            stereoMode = SurfaceEntity.StereoMode.MONO,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape =
                SurfaceEntity.Shape.CustomMesh(
                    leftEye = triangleMeshByTriangleFanLeft,
                    rightEye = triangleMeshByTriangleFanRight,
                    drawMode = SurfaceEntity.DrawMode.TRIANGLE_FAN,
                ),
            buttonText = "Play Big Buck Bunny, Triangle Fan",
            buttonColor = VideoButtonColors.StandardPlayback,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun SurfaceEntityCustomMeshActivityUI(
        session: Session,
        arDevice: ArDevice,
        activity: SurfaceEntityCustomMeshActivity,
    ) {
        val videoPaused = remember { mutableStateOf(false) }
        val alphaMaskEnabled = remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp),
            ) {
                Text(text = "System APIs", fontSize = 30.sp)
                Button(onClick = { togglePassthrough(session) }) {
                    Text(text = "Toggle Passthrough", fontSize = 18.sp)
                }
                Button(onClick = { ActivityCompat.recreate(activity) }) {
                    Text(text = "Recreate Activity", fontSize = 18.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(4.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "SurfaceEntity", fontSize = 30.sp)
                if (videoPlaying == false) {

                    // High level testcases
                    TriangleMeshButton(session, arDevice, activity)
                    TriangleStripButton(session, arDevice, activity)
                    TriangleFanButton(session, arDevice, activity)
                } else {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).padding(8.dp),
                    ) {
                        VideoPlayerControls(session, arDevice)
                        Button(
                            onClick = {
                                videoPaused.value = !videoPaused.value
                                if (videoPaused.value) {
                                    exoPlayer?.pause()
                                } else {
                                    exoPlayer?.play()
                                }
                            }
                        ) {
                            Text(text = "Toggle Pause Stereo video", fontSize = 18.sp)
                        }
                        Button(
                            onClick = {
                                alphaMaskEnabled.value = !alphaMaskEnabled.value
                                if (alphaMaskEnabled.value) {
                                    surfaceEntity!!.primaryAlphaMaskTexture = alphaMaskTexture
                                } else {
                                    // Clear the alpha mask texture.
                                    surfaceEntity!!.primaryAlphaMaskTexture = null
                                }
                            }
                        ) {
                            Text(text = "Toggle Alpha Mask", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
