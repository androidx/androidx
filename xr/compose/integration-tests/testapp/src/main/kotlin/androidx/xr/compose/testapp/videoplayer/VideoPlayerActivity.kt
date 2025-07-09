/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.testapp.videoplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.TextureSampler
import androidx.xr.scenecore.scene
import java.io.File
import java.nio.file.Paths
import kotlinx.coroutines.launch

class VideoPlayerActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
    private val activity = this

    private val requestReadMediaVideo: Int = 1
    private var hasPermission: Boolean = false

    private var surfaceEntity: SurfaceEntity? = null
    private var movableComponent: MovableComponent? = null // movable component for surfaceEntity
    private var videoPlaying by mutableStateOf(false)
    private var videoPaused by mutableStateOf(false)
    private var controlPanelEntity: PanelEntity? = null

    private lateinit var session: Session

    private var alphaMaskTexture: Texture? = null

    private val currentExoPlayer = mutableStateOf(exoPlayer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = (Session.create(this) as SessionCreateSuccess).session
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        session.configure(Config(headTracking = Config.HeadTrackingMode.LAST_KNOWN))

        checkExternalStoragePermission()

        // Load texture
        lifecycleScope.launch {
            alphaMaskTexture =
                Texture.create(
                    session,
                    Paths.get("textures", "alpha_mask.png"),
                    TextureSampler.create(),
                )
        }

        setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                SpatialVideoPlayerUi()
            } else {
                VideoPlayerUi()
            }
        }
    }

    @Composable
    private fun SpatialVideoPlayerUi() {
        Subspace {
            SpatialColumn {
                SpatialPanel(
                    modifier = SubspaceModifier.size(DpVolumeSize(960.dp, 720.dp, 0.dp)).movable()
                ) {
                    VideoPlayerTestActivityUI(true, getString(R.string.video_player_test))
                }
            }
        }
    }

    @Composable
    private fun VideoPlayerUi() {
        VideoPlayerTestActivityUI(true, getString(R.string.video_player_test))
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        if (alphaMaskTexture != null) {
            alphaMaskTexture!!.dispose()
            alphaMaskTexture = null
        }
    }

    private fun togglePassthrough(session: Session) {
        val passthroughOpacity: Float = session.scene.spatialEnvironment.currentPassthroughOpacity
        Log.i("TogglePassthrough", "TogglePassthrough!")
        when (passthroughOpacity) {
            0.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 1.0f
            1.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        }
    }

    // Request the external storage permission so that we can read large files from the SDCard
    private fun checkExternalStoragePermission() {
        if (
            checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = false
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_VIDEO), requestReadMediaVideo)
        } else {
            hasPermission = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == requestReadMediaVideo) {
            hasPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    fun initializeExoPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    fun destroySurfaceEntity() {
        videoPlaying = false
        exoPlayer?.release()
        exoPlayer = null
        surfaceEntity!!.dispose()
        surfaceEntity = null
    }

    fun getCanvasAspectRatio(stereoMode: Int, videoWidth: Int, videoHeight: Int): Dimensions {
        return when (stereoMode) {
            SurfaceEntity.StereoMode.MONO ->
                Dimensions(1.0f, videoHeight.toFloat() / videoWidth, 0.0f)

            SurfaceEntity.StereoMode.TOP_BOTTOM ->
                Dimensions(1.0f, 0.5f * videoHeight.toFloat() / videoWidth, 0.0f)

            SurfaceEntity.StereoMode.SIDE_BY_SIDE ->
                Dimensions(1.0f, 2.0f * videoHeight.toFloat() / videoWidth, 0.0f)

            else -> throw IllegalArgumentException("Unsupported stereo mode: $stereoMode")
        }
    }

    private fun quad() {
        surfaceEntity!!.canvasShape = SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f)
        // Move the Quad-shaped canvas to a spot in front of the User.
        surfaceEntity!!.setPose(
            session.scene.spatialUser.head?.transformPoseTo(
                Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
                session.scene.activitySpace,
            )!!
        )
    }

    @Composable
    fun VideoPlayerTestActivityUI(backButton: Boolean = false, title: String) {
        CommonTestScaffold(
            title = title,
            showBottomBar = backButton,
            onClickBackArrow = { activity.finish() },
            onClickRecreate = { activity.recreate() },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth()) { SystemApisCard(session) }

                Spacer(modifier = Modifier.height(8.dp).fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) { SurfaceEntityCard() }
                    if (videoPlaying) {
                        Column(modifier = Modifier.weight(1f)) { VideoPlayerControlsCard() }
                    }
                }
            }
        }
    }

    @Composable
    private fun SystemApisCard(session: Session) {
        val movableComponentMP = remember { mutableStateOf<MovableComponent?>(null) }
        if (movableComponentMP.value == null) {
            movableComponentMP.value = MovableComponent.createSystemMovable(session)
            session.scene.mainPanelEntity.addComponent(movableComponentMP.value!!)
        }
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "System APIs")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Toggle Passthrough", modifier) { togglePassthrough(session) }
                    ApiButton("Switch to FSM", modifier) {
                        session.scene.requestFullSpaceMode()
                        checkExternalStoragePermission()
                    }
                    ApiButton("Switch to HSM", modifier) { session.scene.requestHomeSpaceMode() }
                }
            }
        }
    }

    @Composable
    private fun SurfaceEntityCard() {
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "(Stereo) SurfaceEntity")

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)

                    if (videoPlaying) {
                        VideoButton(
                            VideoPlayerButtons.BIG_BUCK_BUNNY_BUTTON.ordinal,
                            modifier,
                            false,
                        )

                        VideoButton(
                            VideoPlayerButtons.MVHEVC_LEFT_PRIMARY_BUTTON.ordinal,
                            modifier,
                            false,
                        )

                        VideoButton(
                            VideoPlayerButtons.MVHEVC_RIGHT_PRIMARY_BUTTON.ordinal,
                            modifier,
                            false,
                        )
                    } else {
                        VideoButton(
                            VideoPlayerButtons.BIG_BUCK_BUNNY_BUTTON.ordinal,
                            modifier,
                            true,
                        )

                        VideoButton(
                            VideoPlayerButtons.MVHEVC_LEFT_PRIMARY_BUTTON.ordinal,
                            modifier,
                            true,
                        )

                        VideoButton(
                            VideoPlayerButtons.MVHEVC_RIGHT_PRIMARY_BUTTON.ordinal,
                            modifier,
                            true,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)

                    if (videoPlaying) {
                        VideoButton(VideoPlayerButtons.NAVER_180_BUTTON.ordinal, modifier, false)

                        VideoButton(
                            VideoPlayerButtons.NAVER_180_MVHEVC_BUTTON.ordinal,
                            modifier,
                            false,
                        )
                    } else {
                        VideoButton(VideoPlayerButtons.NAVER_180_BUTTON.ordinal, modifier, true)

                        VideoButton(
                            VideoPlayerButtons.NAVER_180_MVHEVC_BUTTON.ordinal,
                            modifier,
                            true,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)

                    if (videoPlaying) {
                        VideoButton(
                            VideoPlayerButtons.GALAXY_360_TOP_BOTTOM_BUTTON.ordinal,
                            modifier,
                            false,
                        )

                        VideoButton(
                            VideoPlayerButtons.GALAXY_360_MVHEVC_BUTTON.ordinal,
                            modifier,
                            false,
                        )
                    } else {
                        VideoButton(
                            VideoPlayerButtons.GALAXY_360_TOP_BOTTOM_BUTTON.ordinal,
                            modifier,
                            true,
                        )

                        VideoButton(
                            VideoPlayerButtons.GALAXY_360_MVHEVC_BUTTON.ordinal,
                            modifier,
                            true,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)
                    if (videoPlaying) {
                        VideoButton(
                            VideoPlayerButtons.DRM_PROTECTED_BIGGER_BLAZES_BUTTON.ordinal,
                            modifier,
                            false,
                        )

                        VideoButton(
                            VideoPlayerButtons.DRM_PROTECTED_MVHEVC_LEFT_PRIMARY_BUTTON.ordinal,
                            modifier,
                            false,
                        )
                    } else {
                        VideoButton(
                            VideoPlayerButtons.DRM_PROTECTED_BIGGER_BLAZES_BUTTON.ordinal,
                            modifier,
                            true,
                        )

                        VideoButton(
                            VideoPlayerButtons.DRM_PROTECTED_MVHEVC_LEFT_PRIMARY_BUTTON.ordinal,
                            modifier,
                            true,
                        )
                    }
                }
            }
        }
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

    @Composable
    private fun FeatherRadius() {
        var featherRadiusX by remember { mutableFloatStateOf(0.0f) }
        var featherRadiusY by remember { mutableFloatStateOf(0.0f) }
        Column {
            Text(text = "Feather Radius X: $featherRadiusX", fontSize = 14.sp)
            Slider(
                value = featherRadiusX,
                onValueChange = {
                    featherRadiusX = it
                    surfaceEntity!!.edgeFeather =
                        SurfaceEntity.EdgeFeatheringParams.SmoothFeather(
                            featherRadiusX,
                            featherRadiusY,
                        )
                },
                valueRange = 0.0f..0.5f,
            )
            Text(text = "Feather Radius Y: $featherRadiusY", fontSize = 14.sp)
            Slider(
                value = featherRadiusY,
                onValueChange = {
                    featherRadiusY = it
                    surfaceEntity!!.edgeFeather =
                        SurfaceEntity.EdgeFeatheringParams.SmoothFeather(
                            featherRadiusX,
                            featherRadiusY,
                        )
                },
                valueRange = 0.0f..0.5f,
            )
        }
    }

    private fun toggleVideoPause() {
        videoPaused = !videoPaused
        if (videoPaused) {
            exoPlayer?.pause()
        } else {
            exoPlayer?.play()
        }
    }

    @Composable
    private fun VideoPlayerControlsCard() {
        val alphaMaskEnabled = remember { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                ApiText(text = "Video Player Controls")

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)
                    ApiButton("-1s", modifier) { fastRewindOneSec() }
                    ApiButton("End Video", modifier) { destroySurfaceEntity() }
                    ApiButton("+1s", modifier) { fastForwardOneSec() }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow { FeatherRadius() }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)

                    ApiButton("Toggle\nPause Stereo Video", modifier) { toggleVideoPause() }

                    ApiButton("Toggle\nAlpha Mask", modifier, true) {
                        alphaMaskEnabled.value = !alphaMaskEnabled.value
                        if (alphaMaskEnabled.value) {
                            surfaceEntity!!.primaryAlphaMaskTexture = alphaMaskTexture
                        } else {
                            // Clear the alpha mask texture.
                            surfaceEntity!!.primaryAlphaMaskTexture = null
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Set Quad", modifier) { quad() }

                    ApiButton("Set Vr360", modifier) {
                        surfaceEntity!!.canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(1.0f)
                    }

                    ApiButton("Set Vr180", modifier) {
                        surfaceEntity!!.canvasShape =
                            SurfaceEntity.CanvasShape.Vr180Hemisphere(1.0f)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ApiRow {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Mono", modifier) {
                        surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.MONO
                    }

                    ApiButton("Top-Bottom", modifier) {
                        surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
                    }

                    ApiButton("Side-by-Side", modifier) {
                        surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE
                    }
                }
            }
        }
    }

    private fun playerPose(index: Int): Pose {
        return when (index) {
            VideoPlayerButtons.GALAXY_360_TOP_BOTTOM_BUTTON.ordinal,
            VideoPlayerButtons.GALAXY_360_MVHEVC_BUTTON.ordinal,
            VideoPlayerButtons.NAVER_180_MVHEVC_BUTTON.ordinal,
            VideoPlayerButtons.NAVER_180_BUTTON.ordinal -> {
                session.scene.spatialUser.head?.transformPoseTo(
                    Pose.Identity,
                    session.scene.activitySpace,
                )!!
            }

            else -> {
                defaultPose
            }
        }
    }

    @Composable
    private fun VideoButton(index: Int, modifier: Modifier, enabled: Boolean) {
        val attributes = videoAttributesMap[index]!!
        val videoPath = Environment.getExternalStorageDirectory().path + attributes.videoPath

        // Check if file exists
        val file = File(videoPath)
        if (!file.exists()) {
            Toast.makeText(
                    activity,
                    "File does not exist. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        ApiButton(attributes.buttonText, modifier, enabled) {
            createSurfaceEntity(
                attributes.stereoMode,
                playerPose(index),
                attributes.canvasShape,
                attributes.protected,
            )
            setupExoPlayer(videoPath, attributes.stereoMode, attributes.canvasShape)
        }
    }

    private fun createSurfaceEntity(
        stereoMode: Int,
        pose: Pose,
        canvasShape: SurfaceEntity.CanvasShape,
        protected: Boolean = false,
    ) {
        // Create SurfaceEntity and MovableComponent if they don't exist.
        if (surfaceEntity == null) {
            val surfaceContentLevel =
                if (protected) {
                    SurfaceEntity.ContentSecurityLevel.PROTECTED
                } else {
                    SurfaceEntity.ContentSecurityLevel.NONE
                }

            surfaceEntity =
                SurfaceEntity.create(session, stereoMode, pose, canvasShape, surfaceContentLevel)
            // Make the video player movable (to make it easier to look at it from different
            // angles and distances)
            movableComponent = MovableComponent.createSystemMovable(session)
            // The quad has a radius of 1.0 meters
            movableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)
            // component?.size = coordinates.size.toDimensionsInMeters(density)
            surfaceEntity!!.addComponent(movableComponent!!)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupControlPanel() {
        // Technically this leaks, but it's a sample / test app.
        val panelContentView =
            ComposeView(this).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { VideoPlayerControlsCard() }
            }

        controlPanelEntity =
            PanelEntity.create(
                session,
                panelContentView,
                pixelDimensions = IntSize2d(700, 900),
                "VideoControls",
                Pose(Vector3(0.0f, -0.6f, -0.85f)),
            )
        controlPanelEntity!!.parent = surfaceEntity!!

        val parentView: View =
            if (panelContentView.parent != null && panelContentView.parent is View)
                panelContentView.parent as View
            else panelContentView

        parentView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        parentView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        parentView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
    }

    private fun setupExoPlayer(
        videoUri: String,
        stereoMode: Int,
        canvasShape: SurfaceEntity.CanvasShape,
        protected: Boolean = false,
    ) {
        val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"

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
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
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
                    val width = videoSize.width
                    val height = videoSize.height
                    check(width >= 0 && height >= 0) {
                        "Condition (width >= 0 && height >= 0) failed: width=$width, height=$height"
                    }

                    // Resize the canvas to match the video aspect ratio - accounting for the stereo
                    // mode.
                    val dimensions = getCanvasAspectRatio(stereoMode, width, height)
                    // Set the dimensions of the Quad canvas to the video dimensions and attach the
                    // a MovableComponent.
                    if (canvasShape is SurfaceEntity.CanvasShape.Quad) {
                        surfaceEntity?.canvasShape =
                            SurfaceEntity.CanvasShape.Quad(dimensions.width, dimensions.height)
                        movableComponent?.size =
                            (surfaceEntity?.dimensions ?: Dimensions(1.0f, 1.0f, 1.0f))
                                as FloatSize3d
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Update videoPlaying based on ExoPlayer's isPlaying property.
                    videoPlaying = exoPlayer?.isPlaying ?: false // Use safe call and elvis operator

                    if (playbackState == Player.STATE_ENDED) {
                        destroySurfaceEntity()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoPlayerTestActivity", "Player error: $error")
                }
            }
        )
        player.playWhenReady = true
        player.prepare()
        setupControlPanel()
    }

    @Composable
    private fun ApiButton(
        text: String,
        modifier: Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            enabled = enabled,
        ) {
            Text(text, textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun ApiText(text: String) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

    @Composable
    private fun ApiRow(content: @Composable () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }

    companion object {
        val defaultPose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f))
        val defaultCanvasShape = SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f)
        var videoAttributesMap: IntObjectMap<VideoAttributes> =
            MutableIntObjectMap<VideoAttributes>(9).apply {
                put(
                    0,
                    VideoAttributes(
                        "Play Big Buck Bunny",
                        "/Download/vid_bigbuckbunny.mp4",
                        SurfaceEntity.StereoMode.TOP_BOTTOM,
                        false,
                        defaultPose,
                        defaultCanvasShape,
                    ),
                )
                put(
                    1,
                    VideoAttributes(
                        "Play MVHEVC Left Primary",
                        "/Download/mvhevc_flat_left_primary_1080.mov",
                        SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
                        false,
                        defaultPose,
                        defaultCanvasShape,
                    ),
                )
                put(
                    2,
                    VideoAttributes(
                        "Play MVHEVC Right Primary",
                        "/Download/mvhevc_flat_right_primary_1080.mov",
                        SurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY,
                        false,
                        defaultPose,
                        defaultCanvasShape,
                    ),
                )
                put(
                    3,
                    VideoAttributes(
                        "Play Naver 180 (Side-by-Side)",
                        "/Download/Naver180.mp4",
                        SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                        false,
                        defaultPose,
                        SurfaceEntity.CanvasShape.Vr180Hemisphere(1.0f),
                    ),
                )
                put(
                    4,
                    VideoAttributes(
                        "Play Naver 180 (MV-HEVC)",
                        "/Download/Naver180_MV-HEVC.mp4",
                        SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
                        false,
                        defaultPose,
                        SurfaceEntity.CanvasShape.Vr180Hemisphere(1.0f),
                    ),
                )
                put(
                    5,
                    VideoAttributes(
                        "Play Galaxy 360 (Top-Bottom)",
                        "/Download/Galaxy11_VR_3D360.mp4",
                        SurfaceEntity.StereoMode.TOP_BOTTOM,
                        false,
                        defaultPose,
                        SurfaceEntity.CanvasShape.Vr360Sphere(1.0f),
                    ),
                )
                put(
                    6,
                    VideoAttributes(
                        "Play Galaxy 360 (MV-HEVC)",
                        "/Download/Galaxy11_VR_3D360_MV-HEVC.mp4",
                        SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
                        false,
                        defaultPose,
                        SurfaceEntity.CanvasShape.Vr360Sphere(1.0f),
                    ),
                )
                put(
                    7,
                    VideoAttributes(
                        "Play DRM Protected For Bigger Blazes",
                        "/Download/sdr_singleview_protected.mp4",
                        SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                        true,
                        defaultPose,
                        defaultCanvasShape,
                    ),
                )
                put(
                    8,
                    VideoAttributes(
                        "Play DRM Protected MVHEVC Left Primary",
                        "/Download/mvhevc_flat_left_primary_1080_protected.mp4",
                        SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
                        true,
                        defaultPose,
                        defaultCanvasShape,
                    ),
                )
            }

        enum class VideoPlayerButtons {
            BIG_BUCK_BUNNY_BUTTON,
            MVHEVC_LEFT_PRIMARY_BUTTON,
            MVHEVC_RIGHT_PRIMARY_BUTTON,
            NAVER_180_BUTTON,
            NAVER_180_MVHEVC_BUTTON,
            GALAXY_360_TOP_BOTTOM_BUTTON,
            GALAXY_360_MVHEVC_BUTTON,
            DRM_PROTECTED_BIGGER_BLAZES_BUTTON,
            DRM_PROTECTED_MVHEVC_LEFT_PRIMARY_BUTTON,
        }
    }

    data class VideoAttributes(
        val buttonText: String,
        val videoPath: String,
        val stereoMode: Int,
        val protected: Boolean,
        var pose: Pose,
        var canvasShape: SurfaceEntity.CanvasShape,
    )
}
