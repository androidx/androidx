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

package androidx.xr.scenecore.testapp.surfaceplayback

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
import androidx.media3.common.ColorInfo
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
import androidx.xr.runtime.math.FloatSize2d
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
import androidx.xr.scenecore.testapp.common.isDrmSupported
import androidx.xr.scenecore.testapp.common.isMvHevcSupported
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import kotlinx.coroutines.launch

private const val TAG = "JXR-SurfaceEntity-SurfaceEntityPlaybackActivity"

object VideoButtonColors {
    val StandardPlayback = Color(0xFF42A5F5) // Blue 400
    val Multiview = Color(0xFF26A69A) // Teal 400
    val VR = Color(0xFF7E57C2) // Deep Purple 400
    val DRM = Color(0xFF78909C) // Blue Grey 400
    val HDR = Color(0xFF66BB6A) // Green 400
    val Transformations = Color(0xFF757575) // Grey 600
    val DefaultButton = Color(0xFF42A5F5) // Blue 400
}

// Previously known as VideoPlayerTestActivity
class SurfaceEntityPlaybackActivity : ComponentActivity() {
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

    enum class ColorCorrectionMode {
        BEST_EFFORT,
        USER_MANAGED,
    }

    var colorCorrectionMode by mutableStateOf(ColorCorrectionMode.BEST_EFFORT)
        private set

    fun toggleColorCorrectionMode() {
        colorCorrectionMode =
            if (colorCorrectionMode == ColorCorrectionMode.BEST_EFFORT) {
                ColorCorrectionMode.USER_MANAGED
            } else {
                ColorCorrectionMode.BEST_EFFORT
            }
        Log.d(TAG, "ColorCorrectionMode toggled to: $colorCorrectionMode")
    }

    enum class SuperSamplingMode {
        NONE,
        DEFAULT,
    }

    var superSamplingMode by mutableStateOf(SuperSamplingMode.DEFAULT)
        private set

    fun toggleSuperSamplingMode() {
        superSamplingMode =
            if (superSamplingMode == SuperSamplingMode.NONE) {
                SuperSamplingMode.DEFAULT
            } else {
                SuperSamplingMode.NONE
            }
        Log.d(TAG, "SuperSamplingMode toggled to: $superSamplingMode")
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
        session.scene.keyEntity = session.scene.mainPanelEntity

        checkExternalStoragePermission()

        // Set up the MoveableComponent so the user can move the Main Panel out of the way of
        // video canvases which appear behind it.
        if (movableComponentMP == null) {
            movableComponentMP = MovableComponent.createSystemMovable(session)
            session.scene.mainPanelEntity.addComponent(movableComponentMP!!)
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
    fun HelloWorld(session: Session, activity: SurfaceEntityPlaybackActivity) {
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

    fun getCanvasAspectRatio(
        stereoMode: SurfaceEntity.StereoMode,
        videoWidth: Int,
        videoHeight: Int,
        pixelAspectRatio: Float,
    ): FloatSize3d {
        check(videoWidth > 0 && videoHeight > 0) { "Video dimensions must be positive." }
        check(pixelAspectRatio > 0f) { "Pixel aspect ratio must be positive." }
        val effectiveDisplayWidth = videoWidth.toFloat() * pixelAspectRatio

        return when (stereoMode) {
            SurfaceEntity.StereoMode.MONO,
            SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            SurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY ->
                FloatSize3d(1.0f, videoHeight.toFloat() / effectiveDisplayWidth, 0.0f)

            SurfaceEntity.StereoMode.TOP_BOTTOM ->
                FloatSize3d(1.0f, 0.5f * videoHeight.toFloat() / effectiveDisplayWidth, 0.0f)

            SurfaceEntity.StereoMode.SIDE_BY_SIDE ->
                FloatSize3d(1.0f, 2.0f * videoHeight.toFloat() / effectiveDisplayWidth, 0.0f)

            else -> throw IllegalArgumentException("Unsupported stereo mode: $stereoMode")
        }
    }

    private fun createButtonViewUsingCompose(
        activity: SurfaceEntityPlaybackActivity,
        session: Session,
    ): View {
        val arDevice = ArDevice.getInstance(session)
        val view =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { SurfaceEntityPlaybackActivityUI(session, arDevice, activity) }
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
                Button(
                    onClick = {
                        surfaceEntity!!.shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f))
                        // Move the Quad-shaped canvas to a spot in front of the User.
                        surfaceEntity!!.setPose(
                            session.scene.perceptionSpace
                                .getScenePoseFromPerceptionPose(arDevice.state.value.devicePose)
                                .transformPoseTo(
                                    Pose(
                                        Vector3(0.0f, 0.0f, -1.5f),
                                        Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                                    ),
                                    session.scene.activitySpace,
                                )
                        )
                    }
                ) {
                    Text(text = "Set Quad", fontSize = 10.sp)
                }
                Button(onClick = { surfaceEntity!!.shape = SurfaceEntity.Shape.Sphere(1.0f) }) {
                    Text(text = "Set Vr360", fontSize = 10.sp)
                }
                Button(onClick = { surfaceEntity!!.shape = SurfaceEntity.Shape.Hemisphere(1.0f) }) {
                    Text(text = "Set Vr180", fontSize = 10.sp)
                }
            } // end row
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

    private fun updateSurfaceEntityVisuals() {
        val currentSurfaceEntity = surfaceEntity ?: return
        val activePoseForVideo = currentPoseForVideo ?: return
        val activeVideoSize = currentVideoSize ?: return

        val newShapeDimensions =
            getCanvasAspectRatio(
                currentSurfaceEntity.stereoMode,
                activeVideoSize.width,
                activeVideoSize.height,
                currentPixelAspectRatio,
            )
        if (currentSurfaceEntity.shape is SurfaceEntity.Shape.Quad) {
            currentSurfaceEntity.shape =
                SurfaceEntity.Shape.Quad(
                    FloatSize2d(newShapeDimensions.width, newShapeDimensions.height)
                )
            movableComponent?.size = currentSurfaceEntity.dimensions
        }

        var controlOffsetY: Float = 0.0f
        var controlOffsetZ: Float = 0.0f

        Log.i(TAG, "SurfaceEntity visuals updated. Pose: $activePoseForVideo")
        var rotation =
            Quaternion.fromAxisAngle(Vector3.Forward, currentVideoRotationDegrees.toFloat())
        var newPose = surfaceEntity!!.getPose().compose(Pose(Vector3.Zero, rotation))
        surfaceEntity!!.setPose(newPose)
        controlPanelEntity!!.parent = movieParent!!

        // Position the control panel below and slightly in front of the video panel.
        if (currentSurfaceEntity.shape is SurfaceEntity.Shape.Quad) {
            controlOffsetY = -(newShapeDimensions.height / 2f) - 0.15f
            controlOffsetZ = 0.15f
        } else if (currentSurfaceEntity.shape is SurfaceEntity.Shape.Hemisphere) {
            // A rough approximation of a 30/60/90 triangle with radius 1.0f
            controlOffsetY = -0.50f
            controlOffsetZ = -0.85f
        } else if (currentSurfaceEntity.shape is SurfaceEntity.Shape.Sphere) {
            // A rough approximation of a 30/60/90 triangle with radius 1.0f
            controlOffsetY = -0.50f
            controlOffsetZ = -0.85f
        }
        controlPanelEntity!!.setPose(
            Pose(Vector3(0f, controlOffsetY, controlOffsetZ), Quaternion.Identity)
        )
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
                if (!(shape is SurfaceEntity.Shape.Quad)) {
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

                    val superSamplingMode =
                        if (
                            this@SurfaceEntityPlaybackActivity.superSamplingMode ==
                                SuperSamplingMode.DEFAULT
                        ) {
                            SurfaceEntity.SuperSampling.PENTAGON
                        } else {
                            SurfaceEntity.SuperSampling.NONE
                        }

                    movieParent!!.parent = session.scene.activitySpace
                    movieParent!!.setPose(actualPose)

                    surfaceEntity =
                        SurfaceEntity.create(
                            session = session,
                            pose = Pose.Identity,
                            shape = shape,
                            stereoMode = stereoMode,
                            superSampling = superSamplingMode,
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
                        surfaceEntity!!.addComponent(movableComponent!!)
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
                            updateSurfaceEntityVisuals()
                        }

                        override fun onTracksChanged(tracks: Tracks) {
                            super.onTracksChanged(tracks)

                            if (
                                this@SurfaceEntityPlaybackActivity.colorCorrectionMode ==
                                    ColorCorrectionMode.BEST_EFFORT
                            ) {
                                if (surfaceEntity != null) {
                                    surfaceEntity?.contentColorMetadata = null
                                    Log.d(
                                        TAG,
                                        "ColorCorrectionMode is BEST_EFFORT. Setting contentColorMetadata to null.",
                                    )
                                }
                            }

                            // Iterate through track groups to find the selected video track
                            for (trackGroup in tracks.groups) {
                                if (
                                    trackGroup.isSelected && trackGroup.type == C.TRACK_TYPE_VIDEO
                                ) {
                                    if (trackGroup.length > 0) {
                                        val videoFormat = trackGroup.getTrackFormat(0)

                                        // Extract color information if necessary
                                        if (
                                            colorCorrectionMode != ColorCorrectionMode.BEST_EFFORT
                                        ) {
                                            val colorInfo: ColorInfo? = videoFormat.colorInfo
                                            if (colorInfo != null && surfaceEntity != null) {
                                                val contentColorMetadata =
                                                    colorInfoToContentColorMetadata(colorInfo)
                                                surfaceEntity?.contentColorMetadata =
                                                    contentColorMetadata
                                                Log.d(
                                                    TAG,
                                                    "SurfaceEntity contentColorMetadata updated: $contentColorMetadata",
                                                )
                                            }
                                        }

                                        // Extract and update rotation
                                        var updateSurfaceEntityVisuals = false
                                        var newRotation = videoFormat.rotationDegrees
                                        Log.d(TAG, "newRotation: $newRotation")
                                        if (currentVideoRotationDegrees != newRotation) {
                                            currentVideoRotationDegrees = newRotation
                                            updateSurfaceEntityVisuals = true
                                        }

                                        // Extract and update pixel aspect ratio
                                        var newPixelAspectRatio = videoFormat.pixelWidthHeightRatio
                                        Log.d(TAG, "newPixelAspectRatio: $newPixelAspectRatio")
                                        if (currentPixelAspectRatio != newPixelAspectRatio) {
                                            currentPixelAspectRatio = newPixelAspectRatio
                                            updateSurfaceEntityVisuals = true
                                        }

                                        if (
                                            currentVideoSize != null && updateSurfaceEntityVisuals
                                        ) {
                                            updateSurfaceEntityVisuals()
                                        }
                                        break
                                    }
                                }
                            }
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
                    player.repeatMode = Player.REPEAT_MODE_ALL
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

    @Composable
    fun BigBuckBunnyButton(
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
                Environment.getExternalStorageDirectory().path + "/Download/vid_bigbuckbunny.mp4",
            stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[Stereo] Play Big Buck Bunny",
            buttonColor = VideoButtonColors.StandardPlayback,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun MVHEVCLeftPrimaryButton(
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
            // "/sdcard/Download/mvhevc_flat_left_primary_1080.mov".
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/mvhevc_flat_left_primary_1080.mov",
            stereoMode = SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[Multiview] Play MVHEVC Left Primary",
            buttonColor = VideoButtonColors.Multiview,
            enabled = enabled,
            loop = loop,
            protected = false,
        )
    }

    @Composable
    fun MVHEVCRightPrimaryButton(
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
            // "/sdcard/Download/mvhevc_flat_right_primary_1080.mov".
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/mvhevc_flat_right_primary_1080.mov",
            stereoMode = SurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[Multiview] Play MVHEVC Right Primary",
            buttonColor = VideoButtonColors.Multiview,
            enabled = enabled,
            loop = loop,
            protected = false,
        )
    }

    @Composable
    fun Naver180Button(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to "/sdcard/Download/Naver180.mp4".
            videoUri = Environment.getExternalStorageDirectory().path + "/Download/Naver180.mp4",
            stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            pose = Pose.Identity, // will be head pose
            shape = SurfaceEntity.Shape.Hemisphere(1.0f),
            buttonText = "[VR] Play Naver 180 (Side-by-Side)",
            buttonColor = VideoButtonColors.VR,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun Galaxy360Button(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to "/sdcard/Download/Galaxy11_VR_3D360.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path + "/Download/Galaxy11_VR_3D360.mp4",
            stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
            pose = Pose.Identity, // will be head pose
            shape = SurfaceEntity.Shape.Sphere(1.0f),
            buttonText = "[VR] Play Galaxy 360 (Top-Bottom)",
            buttonColor = VideoButtonColors.VR,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun Naver180MVHEVCButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to "/sdcard/Download/Naver180_MV-HEVC.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path + "/Download/Naver180_MV-HEVC.mp4",
            stereoMode = SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            pose = Pose.Identity, // will be head pose
            shape = SurfaceEntity.Shape.Hemisphere(1.0f),
            buttonText = "[VR] Play Naver 180 (MV-HEVC)",
            buttonColor = VideoButtonColors.VR,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun Galaxy360MVHEVCButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        PlayVideoButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to
            // "/sdcard/Download/Galaxy11_VR_3D360_MV-HEVC.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/Galaxy11_VR_3D360_MV-HEVC.mp4",
            stereoMode = SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            pose = Pose.Identity, // will be head pose
            shape = SurfaceEntity.Shape.Sphere(1.0f),
            buttonText = "[VR] Play Galaxy 360 (MV-HEVC)",
            buttonColor = VideoButtonColors.VR,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun SideBySideProtectedButton(
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
            // "/sdcard/Download/sdr_singleview_protected.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/sdr_singleview_protected.mp4",
            stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[DRM] Play Side-by-Side",
            buttonColor = VideoButtonColors.DRM,
            enabled = enabled,
            loop = loop,
            protected = true,
        )
    }

    @Composable
    fun MVHEVCLeftPrimaryProtectedButton(
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
            // "/sdcard/Download/mvhevc_flat_left_primary_1080_protected.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/mvhevc_flat_left_primary_1080_protected.mp4",
            stereoMode = SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[DRM] Play MVHEVC Left Primary",
            buttonColor = VideoButtonColors.DRM,
            enabled = isDrmSupported(),
            loop = loop,
            protected = true,
        )
    }

    @Composable
    fun HDRVideoPlaybackButton(
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
            // "/sdcard/Download/hdr_pq_1000nits_1080p.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/hdr_pq_1000nits_1080p.mp4",
            stereoMode = SurfaceEntity.StereoMode.MONO,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[HDR] Play HDR PQ Video",
            buttonColor = VideoButtonColors.HDR,
            enabled = enabled,
            loop = loop,
            protected = false,
        )
    }

    @Composable
    fun SingleViewRotated270HalfWidthButton(
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
            // "/sdcard/Download/single_view_rotated_270_half_width.mp4"
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/single_view_rotated_270_half_width.mp4",
            stereoMode = SurfaceEntity.StereoMode.MONO,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[Transform] Play Single View (Rot 270, PAR 0.5)",
            buttonColor = VideoButtonColors.Transformations,
            enabled = enabled,
            loop = loop,
            protected = false,
        )
    }

    @Composable
    fun MVHEVCLeftPrimaryRotated180Button(
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
            // "/sdcard/Download/mvhevc_left_primary_rotated_180.mp4".
            videoUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/mvhevc_left_primary_rotated_180.mp4",
            stereoMode = SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            buttonText = "[Transform] Play MVHEVC Left Primary (Rot 180)",
            buttonColor = VideoButtonColors.Transformations,
            enabled = enabled,
            loop = loop,
            protected = false,
        )
    }

    @Composable
    fun SurfaceEntityPlaybackActivityUI(
        session: Session,
        arDevice: ArDevice,
        activity: SurfaceEntityPlaybackActivity,
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
                Button(onClick = { activity.toggleColorCorrectionMode() }) {
                    val buttonTextToDisplay =
                        if (activity.colorCorrectionMode == ColorCorrectionMode.BEST_EFFORT) {
                            "CC: Best Effort (Tap to User Managed)"
                        } else {
                            "CC: User Managed (Tap to Best Effort)"
                        }
                    Text(text = buttonTextToDisplay, fontSize = 18.sp)
                }
                Button(onClick = { activity.toggleSuperSamplingMode() }) {
                    val buttonTextToDisplay =
                        if (activity.superSamplingMode == SuperSamplingMode.DEFAULT) {
                            "SuperSampling: Enabled (Tap to disable)"
                        } else {
                            "SuperSampling: Disabled (Tap to enable)"
                        }
                    Text(text = buttonTextToDisplay, fontSize = 18.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(4.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "SurfaceEntity", fontSize = 30.sp)
                if (videoPlaying == false) {

                    // High level testcases
                    BigBuckBunnyButton(session, arDevice, activity)
                    MVHEVCLeftPrimaryButton(session, arDevice, activity, isMvHevcSupported())
                    MVHEVCRightPrimaryButton(session, arDevice, activity, isMvHevcSupported())
                    Naver180Button(session, arDevice, activity)
                    Naver180MVHEVCButton(session, arDevice, activity, isMvHevcSupported())
                    Galaxy360Button(session, arDevice, activity)
                    Galaxy360MVHEVCButton(session, arDevice, activity, isMvHevcSupported())
                    SideBySideProtectedButton(session, arDevice, activity)
                    MVHEVCLeftPrimaryProtectedButton(
                        session,
                        arDevice,
                        activity,
                        isMvHevcSupported(),
                    )
                    if (!isDrmSupported()) {
                        Text(
                            text = "DRM is not supported on this device.",
                            fontSize = 18.sp,
                            color = Color.Red,
                        )
                    }
                    HDRVideoPlaybackButton(session, arDevice, activity)
                    SingleViewRotated270HalfWidthButton(session, arDevice, activity)
                    MVHEVCLeftPrimaryRotated180Button(
                        session,
                        arDevice,
                        activity,
                        isMvHevcSupported(),
                    )
                    if (!isMvHevcSupported()) {
                        Text(
                            text = "MV-HEVC is not supported on this device.",
                            fontSize = 18.sp,
                            color = Color.Red,
                        )
                    }
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

    companion object {
        @Suppress("UnsafeOptInUsageError")
        fun parseMaxCLLFromHdrStaticInfo(hdrStaticInfoByteArray: ByteArray?): Int {
            // HdrStaticInfo follows CTA-861.3 standard, in which maxCLL, if available, is encoded
            // as a 16 bit unsigned integer starting from byte 23.
            if (hdrStaticInfoByteArray == null || hdrStaticInfoByteArray.size < 25) {
                return 0
            }
            return try {
                val buffer = ByteBuffer.wrap(hdrStaticInfoByteArray).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(23)
                buffer.getShort().toInt() and 0xFFFF
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing MaxCLL from HDR static info", e)
                0
            }
        }

        @Suppress("UnsafeOptInUsageError")
        fun colorInfoToContentColorMetadata(
            colorInfo: ColorInfo
        ): SurfaceEntity.ContentColorMetadata {
            val colorSpace: SurfaceEntity.ContentColorMetadata.ColorSpace =
                when (colorInfo.colorSpace) {
                    C.COLOR_SPACE_BT601 -> SurfaceEntity.ContentColorMetadata.ColorSpace.BT601_PAL
                    C.COLOR_SPACE_BT709 -> SurfaceEntity.ContentColorMetadata.ColorSpace.BT709
                    C.COLOR_SPACE_BT2020 -> SurfaceEntity.ContentColorMetadata.ColorSpace.BT2020
                    else -> error("Unknown color space: " + colorInfo.colorSpace)
                }
            Log.d(TAG, "colorSpace : $colorSpace")

            val colorTransfer: SurfaceEntity.ContentColorMetadata.ColorTransfer =
                when (colorInfo.colorTransfer) {
                    C.COLOR_TRANSFER_GAMMA_2_2 ->
                        SurfaceEntity.ContentColorMetadata.ColorTransfer.GAMMA_2_2

                    C.COLOR_TRANSFER_HLG -> SurfaceEntity.ContentColorMetadata.ColorTransfer.HLG
                    C.COLOR_TRANSFER_LINEAR ->
                        SurfaceEntity.ContentColorMetadata.ColorTransfer.LINEAR

                    C.COLOR_TRANSFER_SDR -> SurfaceEntity.ContentColorMetadata.ColorTransfer.SDR
                    C.COLOR_TRANSFER_SRGB -> SurfaceEntity.ContentColorMetadata.ColorTransfer.SRGB
                    C.COLOR_TRANSFER_ST2084 ->
                        SurfaceEntity.ContentColorMetadata.ColorTransfer.ST2084

                    else -> error("Unknown color transfer: " + colorInfo.colorTransfer)
                }
            Log.d(TAG, "colorTransfer: $colorTransfer")

            val colorRange: SurfaceEntity.ContentColorMetadata.ColorRange =
                when (colorInfo.colorRange) {
                    C.COLOR_RANGE_FULL -> SurfaceEntity.ContentColorMetadata.ColorRange.FULL
                    C.COLOR_RANGE_LIMITED -> SurfaceEntity.ContentColorMetadata.ColorRange.LIMITED
                    else -> error("Unknown color range: " + colorInfo.colorRange)
                }
            Log.d(TAG, "colorRange: $colorRange")

            val maxContentLightLevel = parseMaxCLLFromHdrStaticInfo(colorInfo.hdrStaticInfo)
            Log.d(TAG, "maxContentLightLevel: $maxContentLightLevel")

            return SurfaceEntity.ContentColorMetadata(
                colorSpace = colorSpace,
                colorTransfer = colorTransfer,
                colorRange = colorRange,
                maxContentLightLevel = maxContentLightLevel,
            )
        }
    }
}
