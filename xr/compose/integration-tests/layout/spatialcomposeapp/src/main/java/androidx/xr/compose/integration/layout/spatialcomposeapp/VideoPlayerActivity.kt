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

package androidx.xr.compose.integration.layout.spatialcomposeapp

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialExternalSurface180Hemisphere
import androidx.xr.compose.subspace.SpatialExternalSurface360Sphere
import androidx.xr.compose.subspace.SpatialExternalSurfaceDefaults
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SurfaceProtection
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialFeatheringEffect
import androidx.xr.compose.subspace.layout.SpatialSmoothFeatheringEffect
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.alpha
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.onPointSourceParams
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SpatialMediaPlayer
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import java.io.File
import kotlin.math.roundToInt

/**
 * This is a sample activity that shows how to use Compose XR to create a SurfaceEntity and use it
 * to display a video.
 */
class VideoPlayerActivity : ComponentActivity() {
    private val TAG = "VideoPlayerActivity"
    private lateinit var mediaPlayer: MediaPlayer

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    private var surfaceEntity: SurfaceEntity? = null
    private var movableComponent: MovableComponent? = null

    private val menuState = mutableStateOf(VideoMenuState.HOME)
    private val videoPlayingState = mutableStateOf(false)
    private val mediaUriState: MutableState<Uri?> = mutableStateOf(null)
    private val useDrmState = mutableStateOf(false)

    private var oldFeatheringType = FeatheringType.PERCENT
    private val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
    private val drmVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/sdr_singleview_protected.mp4"
    private val REQUEST_READ_MEDIA_VIDEO: Int = 1
    private var exoPlayer: ExoPlayer? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                mediaUriState.value = result.data?.data
            }
        }

    enum class VideoMenuState {
        HOME,
        VIDEO_IN_SPATIAL_EXTERNAL_SURFACE,
        VIDEO_IN_SPATIAL_PANEL,
        VIDEO_IN_SURFACE_ENTITY,
    }

    enum class FeatheringType {
        PERCENT,
        PIXEL,
        DP,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        setContent { Subspace { VideoOptionsContent(session) } }
    }

    /** Sets up the surface entity */
    @OptIn(ExperimentalComposeApi::class)
    @Composable
    private fun VideoOptionsContent(session: Session) {
        BackHandler {
            Log.i(TAG, "Navigating back using BackHandler")
            releaseMediaPlayer()
            finish()
        }

        if (useDrmState.value) {
            val file = File(drmVideoUri)
            if (!file.exists()) {
                Log.e(TAG, "Drm file does not exist. Did you adb push the asset?")
                Toast.makeText(
                        this@VideoPlayerActivity,
                        "Drm file does not exist. Did you adb push the asset?",
                        Toast.LENGTH_LONG,
                    )
                    .show()
                return
            }
        }

        var isAudioSpatialized by remember { mutableStateOf(true) }
        val menu = menuState.value
        val videoPlaying = videoPlayingState.value
        val videoUri = mediaUriState.value
        var stereoMode by remember { mutableStateOf(StereoMode.Mono) }
        var featheringType by remember { mutableStateOf(FeatheringType.PERCENT) }
        var featheringValue by remember { mutableFloatStateOf(0f) }
        var surfaceType by remember { mutableStateOf(SpatialExternalSurfaceType.QUAD) }

        if (videoPlaying && surfaceType == SpatialExternalSurfaceType.HEMISPHERE) {
            // Size and offset shouldn't get passed down from the box to the sphere, they are here
            // just for verification purposes and should be a no-op.
            SpatialBox(modifier = SubspaceModifier.size(500.dp).offset(x = 20000.dp)) {
                // Simple animation to verify radius and layout recomposition.
                val animatedRadius = remember { Animatable(500f) }
                val animatedOffset = remember { Animatable(initialValue = -1000f) }
                LaunchedEffect(Unit) {
                    animatedRadius.animateTo(
                        targetValue = SpatialExternalSurfaceDefaults.sphereRadius.value,
                        animationSpec = tween(durationMillis = 2000, easing = FastOutLinearInEasing),
                    )
                }
                // An initial offset is necessary to perceive the radius animation.
                LaunchedEffect(Unit) {
                    animatedOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 2000, easing = FastOutLinearInEasing),
                    )
                }
                SpatialExternalSurface180Hemisphere(
                    modifier = SubspaceModifier.offset(z = animatedOffset.value.dp),
                    stereoMode = stereoMode,
                    radius = animatedRadius.value.dp,
                    featheringEffect = getFeatheringEffect(featheringValue, featheringType),
                    surfaceProtection =
                        if (useDrmState.value) SurfaceProtection.Protected
                        else SurfaceProtection.None,
                ) {
                    onSurfaceCreated {
                        val player = ExoPlayer.Builder(this@VideoPlayerActivity).build()
                        exoPlayer = player
                        player.setVideoSurface(it)
                        player.setMediaItem(getMediaItem())
                        player.repeatMode = Player.REPEAT_MODE_ONE
                        player.playWhenReady = true
                        player.prepare()
                    }

                    onSurfaceDestroyed {
                        exoPlayer?.release()
                        exoPlayer = null
                    }

                    SphereVideoControlPanel(includeAnimationPanel = true)
                }
            }
        } else if (videoPlaying && surfaceType == SpatialExternalSurfaceType.SPHERE) {
            SpatialExternalSurface360Sphere(
                stereoMode = stereoMode,
                featheringEffect = getFeatheringEffect(featheringValue, featheringType),
                surfaceProtection =
                    if (useDrmState.value) SurfaceProtection.Protected else SurfaceProtection.None,
            ) {
                onSurfaceCreated {
                    val player = ExoPlayer.Builder(this@VideoPlayerActivity).build()
                    exoPlayer = player
                    player.setVideoSurface(it)
                    player.setMediaItem(getMediaItem())
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    player.playWhenReady = true
                    player.prepare()
                }

                onSurfaceDestroyed {
                    exoPlayer?.release()
                    exoPlayer = null
                }

                SphereVideoControlPanel()
            }
        } else {
            SpatialColumn {
                SpatialPanel(SubspaceModifier.height(600.dp).width(600.dp).movable()) {
                    Column(modifier = Modifier.background(Color.LightGray).fillMaxSize()) {
                        when (menu) {
                            VideoMenuState.HOME -> {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Button(
                                        onClick = {
                                            val intent =
                                                Intent(Intent.ACTION_PICK).apply {
                                                    type = "video/*"
                                                }
                                            pickMedia.launch(intent)
                                        }
                                    ) {
                                        Text("Select media")
                                    }

                                    Button(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        enabled = videoUri != null,
                                        onClick = {
                                            menuState.value = VideoMenuState.VIDEO_IN_SPATIAL_PANEL
                                        },
                                    ) {
                                        Text("Video in Spatial Panel (non-stereoscopic)")
                                    }

                                    Button(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        enabled = videoUri != null,
                                        onClick = {
                                            menuState.value =
                                                VideoMenuState.VIDEO_IN_SPATIAL_EXTERNAL_SURFACE
                                        },
                                    ) {
                                        Text("Video in Spatial External Surface")
                                    }

                                    Button(
                                        enabled = videoUri != null,
                                        onClick = {
                                            menuState.value = VideoMenuState.VIDEO_IN_SURFACE_ENTITY
                                        },
                                    ) {
                                        Text("Video in Surface Entity")
                                    }
                                }
                            }
                            VideoMenuState.VIDEO_IN_SPATIAL_PANEL -> {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Button(
                                        onClick = {
                                            videoPlayingState.value = false
                                            menuState.value = VideoMenuState.HOME
                                        }
                                    ) {
                                        Text("Main Menu")
                                    }

                                    Button(onClick = { videoPlayingState.value = !videoPlaying }) {
                                        if (videoPlaying) {
                                            Text("Stop Video")
                                        } else {
                                            Text("Start Video")
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(8.dp),
                                            text = "Spatialize audio with Video",
                                        )
                                        Switch(
                                            checked = isAudioSpatialized,
                                            enabled = !videoPlaying,
                                            onCheckedChange = {
                                                isAudioSpatialized = !isAudioSpatialized
                                            },
                                        )
                                    }
                                }
                            }
                            VideoMenuState.VIDEO_IN_SPATIAL_EXTERNAL_SURFACE -> {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Button(
                                        onClick = {
                                            videoPlayingState.value = false
                                            menuState.value = VideoMenuState.HOME
                                            surfaceType = SpatialExternalSurfaceType.QUAD
                                        }
                                    ) {
                                        Text("Main Menu")
                                    }

                                    Button(onClick = { useDrmState.value = !useDrmState.value }) {
                                        if (useDrmState.value) {
                                            Text("Use picker video uri")
                                        } else {
                                            Text("Use drm video uri")
                                        }
                                    }

                                    Button(onClick = { videoPlayingState.value = !videoPlaying }) {
                                        if (videoPlaying) {
                                            Text("Stop Video")
                                        } else {
                                            Text("Start Video")
                                        }
                                    }

                                    val text =
                                        "Current stereo mode: " +
                                            when (stereoMode) {
                                                StereoMode.Mono -> {
                                                    "Mono"
                                                }
                                                StereoMode.TopBottom -> {
                                                    "Top Bottom"
                                                }
                                                else -> {
                                                    "Side by Side"
                                                }
                                            }

                                    Text(text)

                                    Row(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Button(onClick = { stereoMode = StereoMode.Mono }) {
                                            Text("Mono")
                                        }
                                        Button(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            onClick = { stereoMode = StereoMode.TopBottom },
                                        ) {
                                            Text("Top Bottom")
                                        }
                                        Button(onClick = { stereoMode = StereoMode.SideBySide }) {
                                            Text("Side by Side")
                                        }
                                    }

                                    val surfaceText =
                                        when (surfaceType) {
                                            SpatialExternalSurfaceType.QUAD -> {
                                                "Quad"
                                            }
                                            SpatialExternalSurfaceType.HEMISPHERE -> {
                                                "Hemisphere"
                                            }
                                            else -> {
                                                "Sphere"
                                            }
                                        }

                                    Text("Current Surface Type: $surfaceText")

                                    Row(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Button(
                                            onClick = {
                                                surfaceType = SpatialExternalSurfaceType.QUAD
                                            }
                                        ) {
                                            Text("Quad")
                                        }
                                        Button(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            onClick = {
                                                surfaceType = SpatialExternalSurfaceType.HEMISPHERE
                                            },
                                        ) {
                                            Text("Hemisphere")
                                        }
                                        Button(
                                            onClick = {
                                                surfaceType = SpatialExternalSurfaceType.SPHERE
                                            }
                                        ) {
                                            Text("Sphere")
                                        }
                                    }

                                    Text(
                                        modifier = Modifier.padding(top = 24.dp),
                                        fontSize = 20.sp,
                                        text = "Feathering",
                                    )
                                    Text(
                                        "Clicking on a button will apply that feathering type with the value specified. " +
                                            "The value selected at the end of the slider drag will be animated. Large " +
                                            "values are coerced to 50 percent of width/height."
                                    )

                                    val floatRange = 0f..250f
                                    var sliderValue by remember { mutableFloatStateOf(0f) }

                                    Text(text = "Selected Value: ${sliderValue.roundToInt()}")
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = { newValue -> sliderValue = newValue },
                                        onValueChangeFinished = { featheringValue = sliderValue },
                                        valueRange = floatRange,
                                        steps =
                                            ((floatRange.endInclusive - floatRange.start) / 5)
                                                .toInt() - 1,
                                    )

                                    Row {
                                        Button(
                                            onClick = { featheringType = FeatheringType.PERCENT }
                                        ) {
                                            Text("Percent")
                                        }
                                        Button(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            onClick = { featheringType = FeatheringType.DP },
                                        ) {
                                            Text("Dp")
                                        }
                                        Button(
                                            onClick = { featheringType = FeatheringType.PIXEL }
                                        ) {
                                            Text("Pixel")
                                        }
                                    }
                                }
                            }
                            VideoMenuState.VIDEO_IN_SURFACE_ENTITY -> {
                                SurfaceEntityUI(session)
                            }
                        }
                    }
                }

                SpatialLayoutSpacer(SubspaceModifier.height(20.dp))

                if (videoPlaying && menu == VideoMenuState.VIDEO_IN_SPATIAL_PANEL) {
                    VideoInSpatialPanel(isAudioSpatialized = isAudioSpatialized)
                } else if (
                    videoPlaying && menu == VideoMenuState.VIDEO_IN_SPATIAL_EXTERNAL_SURFACE
                ) {
                    VideoInSpatialExternalSurface(stereoMode, featheringType, featheringValue)
                } else {
                    SpatialLayoutSpacer(SubspaceModifier.height(600.dp))
                }
            }
        }
    }

    enum class SpatialExternalSurfaceType {
        QUAD,
        HEMISPHERE,
        SPHERE,
    }

    @Composable
    fun VideoInSpatialPanel(isAudioSpatialized: Boolean) {
        val session = LocalSession.current

        SpatialPanel(
            modifier =
                SubspaceModifier.width(600.dp)
                    .height(600.dp)
                    .onPointSourceParams {
                        mediaPlayer = MediaPlayer()
                        if (isAudioSpatialized) {
                            SpatialMediaPlayer.setPointSourceParams(session!!, mediaPlayer, it)
                        }

                        mediaPlayer.setDataSource(this@VideoPlayerActivity, mediaUriState.value!!)
                        mediaPlayer.prepare()
                        mediaPlayer.isLooping = true
                        mediaPlayer.start()
                    }
                    .movable(enabled = true)
        ) {
            DisposableEffect(Unit) { onDispose { releaseMediaPlayer() } }

            AndroidExternalSurface {
                onSurface { surface, _, _ -> mediaPlayer.setSurface(surface) }
            }
        }
    }

    @Composable
    fun SphereVideoControlPanel(includeAnimationPanel: Boolean = false) {
        SpatialBox(modifier = SubspaceModifier.fillMaxSize()) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            // Having an alpha helps reduce depth perception issues with stereo video.
            SpatialPanel(
                modifier =
                    SubspaceModifier.width(600.dp)
                        .height(120.dp)
                        .alpha(if (isHovered) 0.9f else 0.3f)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color.Black)
                            .hoverable(interactionSource = interactionSource)
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            if (exoPlayer!!.isPlaying) {
                                exoPlayer!!.pause()
                            } else {
                                exoPlayer!!.play()
                            }
                        }
                    ) {
                        Text("Play/Pause")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { useDrmState.value = !useDrmState.value }) {
                        Text(text = if (useDrmState.value) "Use non-drm video" else "Use drm video")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { videoPlayingState.value = false }) { Text("End Video") }
                }
            }
            if (includeAnimationPanel) {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.size(1000.dp)
                            .align(SpatialAlignment.CenterLeft)
                            .rotate(axisAngle = Vector3(y = 1.0f), 90f)
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(16.dp)
                    ) {
                        Text(text = "Animation\nTest", color = Color.White, fontSize = 200.sp)
                    }
                }
            }
        }
    }

    @Composable
    fun LaunchSurfaceEntityButton() {
        Button(
            onClick = {
                surfaceEntity =
                    SurfaceEntity.create(
                        session = session,
                        stereoMode = SurfaceEntity.StereoMode.MONO,
                        pose =
                            Pose(
                                Vector3(0f, -0.45f, 0f),
                                rotation = Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                            ),
                        contentSecurityLevel =
                            if (useDrmState.value) SurfaceEntity.ContentSecurityLevel.PROTECTED
                            else SurfaceEntity.ContentSecurityLevel.NONE,
                    )
                // Make the video player movable (to make it easier to look at it from different
                // angles and distances)
                movableComponent = MovableComponent.createSystemMovable(session)

                // The quad has a radius of 1.0 meters
                movableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)
                @Suppress("UNUSED_VARIABLE")
                val unused = surfaceEntity!!.addComponent(movableComponent!!)

                val player = ExoPlayer.Builder(this@VideoPlayerActivity).build()
                exoPlayer = player
                player.setVideoSurface(surfaceEntity!!.getSurface())
                player.setMediaItem(getMediaItem())
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.addListener(
                    object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            val width = videoSize.width
                            val height = videoSize.height
                            if (height > 0 && width > 0) {
                                var dimensions =
                                    getCanvasAspectRatio(surfaceEntity!!.stereoMode, width, height)
                                surfaceEntity!!.canvasShape =
                                    SurfaceEntity.CanvasShape.Quad(
                                        dimensions.width,
                                        dimensions.height,
                                    )

                                // Resize the MovableComponent to match the canvas dimensions.
                                movableComponent!!.size = surfaceEntity!!.dimensions
                            }
                        }
                    }
                )

                player.playWhenReady = true
                player.prepare()
                videoPlayingState.value = true
            }
        ) {
            Text(text = "Launch Surface Entity", fontSize = 20.sp)
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    @Composable
    fun VideoInSpatialExternalSurface(
        stereoMode: StereoMode,
        featheringType: FeatheringType,
        featheringValue: Float,
    ) {
        var videoWidth by remember { mutableStateOf(600.dp) }
        var videoHeight by remember { mutableStateOf(600.dp) }
        var isPaused by remember { mutableStateOf(false) }

        // Animates if value is updated and feathering type is the same.
        val animatedFeatheringValue: Float by
            animateFloatAsState(
                targetValue = featheringValue,
                animationSpec = tween(if (oldFeatheringType == featheringType) 700 else 0),
            )
        oldFeatheringType = featheringType

        // The resizable modifier overrides the automatic width/height resizing logic when switching
        // stereo modes.
        SpatialExternalSurface(
            modifier =
                SubspaceModifier.width(
                        if (stereoMode == StereoMode.SideBySide) videoWidth / 2 else videoWidth
                    )
                    .height(
                        if (stereoMode == StereoMode.TopBottom) videoHeight / 2 else videoHeight
                    )
                    .movable()
                    .resizable(),
            stereoMode = stereoMode,
            featheringEffect = getFeatheringEffect(animatedFeatheringValue, featheringType),
            surfaceProtection =
                if (useDrmState.value) SurfaceProtection.Protected else SurfaceProtection.None,
        ) {
            onSurfaceCreated {
                val player = ExoPlayer.Builder(this@VideoPlayerActivity).build()
                exoPlayer = player
                player.setVideoSurface(it)
                player.setMediaItem(getMediaItem())
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.addListener(
                    object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            val width = videoSize.width
                            val height = videoSize.height
                            if (height > 0 && width > 0) {
                                videoHeight = videoWidth * height / width
                            }
                        }
                    }
                )

                player.playWhenReady = true
                player.prepare()
            }

            onSurfaceDestroyed {
                exoPlayer?.release()
                exoPlayer = null
            }

            SpatialBox(
                modifier = SubspaceModifier.fillMaxSize(),
                alignment = SpatialAlignment.TopRight,
            ) {
                SpatialPanel(SubspaceModifier.offset(z = 30.dp)) {
                    Button(onClick = { videoPlayingState.value = false }) { Text("Close") }
                }
            }

            // Offset avoids depth perception issues when playing stereoscopic video.
            Orbiter(position = ContentEdge.Bottom, offset = 48.dp) {
                Button(
                    onClick = {
                        if (isPaused) exoPlayer?.play() else exoPlayer?.pause()
                        isPaused = !isPaused
                    }
                ) {
                    Text(text = if (isPaused) "Play" else "Pause")
                }
            }
        }
    }

    fun getFeatheringEffect(value: Float, featheringType: FeatheringType): SpatialFeatheringEffect {
        return when (featheringType) {
            FeatheringType.PERCENT ->
                SpatialSmoothFeatheringEffect(
                    percentHorizontal = value.roundToInt().coerceAtMost(50),
                    percentVertical = value.roundToInt().coerceAtMost(50),
                )
            FeatheringType.PIXEL ->
                SpatialSmoothFeatheringEffect(horizontal = value, vertical = value)
            FeatheringType.DP ->
                SpatialSmoothFeatheringEffect(horizontal = value.dp, vertical = value.dp)
        }
    }

    @Composable
    fun VideoPlayerControls() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { releaseMediaPlayer() }) {
                    Text(text = "End Video", fontSize = 10.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val videoHeight = exoPlayer?.videoSize?.height
                        val videoWidth = exoPlayer?.videoSize?.width
                        val canvasHeight =
                            if (videoHeight != null && videoWidth != null) {
                                videoHeight.toFloat() / videoWidth.toFloat()
                            } else {
                                1.0f
                            }

                        surfaceEntity!!.canvasShape =
                            SurfaceEntity.CanvasShape.Quad(1.0f, canvasHeight)
                    }
                ) {
                    Text(text = "Set Quad", fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        surfaceEntity!!.canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(5.0f)
                    }
                ) {
                    Text(text = "Set Vr360", fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        surfaceEntity!!.canvasShape =
                            SurfaceEntity.CanvasShape.Vr180Hemisphere(5.0f)
                    }
                ) {
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

    fun releaseMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        exoPlayer?.release()
        exoPlayer = null
        videoPlayingState.value = false
        surfaceEntity?.dispose()
        surfaceEntity = null
    }

    fun getCanvasAspectRatio(stereoMode: Int, videoWidth: Int, videoHeight: Int): FloatSize3d {
        when (stereoMode) {
            SurfaceEntity.StereoMode.MONO ->
                return FloatSize3d(1.0f, videoHeight.toFloat() / videoWidth, 0.0f)
            SurfaceEntity.StereoMode.TOP_BOTTOM ->
                return FloatSize3d(1.0f, 0.5f * videoHeight.toFloat() / videoWidth, 0.0f)
            SurfaceEntity.StereoMode.SIDE_BY_SIDE ->
                return FloatSize3d(1.0f, 2.0f * videoHeight.toFloat() / videoWidth, 0.0f)
            else -> throw IllegalArgumentException("Unsupported stereo mode: $stereoMode")
        }
    }

    @Composable
    fun SurfaceEntityUI(session: Session) {
        val movableComponentMP = remember { mutableStateOf<MovableComponent?>(null) }
        val videoPaused = remember { mutableStateOf(false) }
        movableComponentMP.value = MovableComponent.createSystemMovable(session)
        @Suppress("UNUSED_VARIABLE")
        val unused = session.scene.mainPanelEntity.addComponent(movableComponentMP.value!!)
        val videoPlaying = videoPlayingState.value
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp),
            ) {
                Text(text = "(Stereo) SurfaceEntity", fontSize = 50.sp)
                if (videoPlaying == false) {
                    // High level testcases
                    Button(onClick = { menuState.value = VideoMenuState.HOME }) {
                        Text("Main Menu")
                    }
                    Button(onClick = { useDrmState.value = !useDrmState.value }) {
                        if (useDrmState.value) {
                            Text("Use picker video uri")
                        } else {
                            Text("Use drm video uri")
                        }
                    }
                    LaunchSurfaceEntityButton()
                } else {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).padding(8.dp),
                    ) {
                        VideoPlayerControls()
                        Button(
                            onClick = {
                                videoPaused.value = !videoPaused.value
                                if (videoPaused.value) {
                                    mediaPlayer.pause()
                                } else {
                                    mediaPlayer.start()
                                }
                            }
                        ) {
                            Text(text = "Toggle Pause Stereo video", fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }

    private fun getMediaItem(): MediaItem {
        return if (useDrmState.value) {
            MediaItem.Builder()
                .setUri(drmVideoUri)
                .setDrmConfiguration(
                    DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(drmLicenseUrl).build()
                )
                .build()
        } else {
            MediaItem.fromUri(mediaUriState.value!!)
        }
    }
}
