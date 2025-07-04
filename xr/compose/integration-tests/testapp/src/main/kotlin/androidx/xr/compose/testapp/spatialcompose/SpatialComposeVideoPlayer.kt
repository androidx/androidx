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

package androidx.xr.compose.testapp.spatialcompose

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialSmoothFeatheringEffect
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.onPointSourceParams
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SpatialMediaPlayer
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import kotlin.getValue
import kotlin.math.roundToInt

class SpatialComposeVideoPlayer : ComponentActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    private var surfaceEntity: SurfaceEntity? = null
    private var movableComponent: MovableComponent? = null

    private val menuState = mutableStateOf(VideoMenuState.HOME)
    private val videoPlayingState = mutableStateOf(false)
    private val mediaUriState: MutableState<Uri?> = mutableStateOf(null)

    private var oldFeatheringType = FeatheringType.PERCENT

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
        session.configure(Config(headTracking = Config.HeadTrackingMode.LAST_KNOWN))
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        setContent { Subspace { VideoOptionsContent(session) } }
    }

    @Composable
    private fun VideoOptionsContent(session: Session) {
        var isAudioSpatialized by remember { mutableStateOf(true) }
        val menu = menuState.value
        val videoPlaying = videoPlayingState.value
        val videoUri = mediaUriState.value
        var stereoMode by remember { mutableStateOf(StereoMode.Mono) }
        var featheringType by remember { mutableStateOf(FeatheringType.PERCENT) }
        var featheringValue by remember { mutableFloatStateOf(0f) }

        SpatialColumn {
            SpatialPanel(SubspaceModifier.height(600.dp).width(600.dp).movable()) {
                CommonTestScaffold(
                    title = "Video Player Tests",
                    showBottomBar = true,
                    onClickBackArrow = { this@SpatialComposeVideoPlayer.finish() },
                ) { padding ->
                    Column(
                        modifier =
                            Modifier.background(Color.LightGray).fillMaxSize().padding(padding)
                    ) {
                        BackHandler {
                            Log.i(
                                "BackHandler",
                                "Gnav BACK is being handled by Surface Entity back handler",
                            )
                            releaseMediaPlayer()
                            finish()
                        }

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
            }

            SpatialLayoutSpacer(SubspaceModifier.height(20.dp))

            if (videoPlaying && menu == VideoMenuState.VIDEO_IN_SPATIAL_PANEL) {
                VideoInSpatialPanel(isAudioSpatialized = isAudioSpatialized)
            } else if (videoPlaying && menu == VideoMenuState.VIDEO_IN_SPATIAL_EXTERNAL_SURFACE) {
                VideoInSpatialExternalSurface(stereoMode, featheringType, featheringValue)
            } else {
                SpatialLayoutSpacer(SubspaceModifier.height(600.dp))
            }
        }
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

                        mediaPlayer.setDataSource(
                            this@SpatialComposeVideoPlayer,
                            mediaUriState.value!!,
                        )
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
    fun LaunchSurfaceEntityButton() {
        Button(
            onClick = {
                surfaceEntity =
                    SurfaceEntity.create(
                        session,
                        SurfaceEntity.StereoMode.TOP_BOTTOM,
                        Pose(Vector3(0f, -0.45f, 0f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
                    )
                // Make the video player movable (to make it easier to look at it from different
                // angles and distances)
                movableComponent = MovableComponent.createSystemMovable(session)

                // The quad has a radius of 1.0 meters
                movableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)
                @Suppress("UNUSED_VARIABLE")
                val unused = surfaceEntity!!.addComponent(movableComponent!!)

                // Set up MediaPlayer
                mediaPlayer = MediaPlayer()
                mediaPlayer.setSurface(surfaceEntity!!.getSurface())

                mediaPlayer.setDataSource(this@SpatialComposeVideoPlayer, mediaUriState.value!!)
                mediaPlayer.isLooping = true

                mediaPlayer.setOnCompletionListener { mediaPlayer.release() }
                mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
                    check(width >= 0 && height >= 0) { "Video dimensions must be positive" }
                    // Resize the canvas to match the video aspect ratio - accounting for the stereo
                    // mode.
                    var dimensions = getCanvasAspectRatio(surfaceEntity!!.stereoMode, width, height)
                    surfaceEntity!!.canvasShape =
                        SurfaceEntity.CanvasShape.Quad(dimensions.width, dimensions.height)

                    // Resize the MovableComponent to match the canvas dimensions.
                    movableComponent!!.size = surfaceEntity!!.dimensions
                }
                mediaPlayer.setOnPreparedListener { mediaPlayer.start() }
                mediaPlayer.prepareAsync()
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
        val session = LocalSession.current

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
                    .onPointSourceParams {
                        SpatialMediaPlayer.setPointSourceParams(session!!, mediaPlayer, it)
                        mediaPlayer.prepareAsync()
                    }
                    .movable()
                    .resizable(),
            stereoMode = stereoMode,
            featheringEffect =
                when (featheringType) {
                    FeatheringType.PERCENT ->
                        SpatialSmoothFeatheringEffect(
                            percentHorizontal =
                                animatedFeatheringValue.roundToInt().coerceAtMost(50),
                            percentVertical = animatedFeatheringValue.roundToInt().coerceAtMost(50),
                        )
                    FeatheringType.PIXEL ->
                        SpatialSmoothFeatheringEffect(
                            horizontal = animatedFeatheringValue,
                            vertical = animatedFeatheringValue,
                        )
                    FeatheringType.DP ->
                        SpatialSmoothFeatheringEffect(
                            horizontal = animatedFeatheringValue.dp,
                            vertical = animatedFeatheringValue.dp,
                        )
                },
        ) {
            onSurfaceCreated {
                mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(this@SpatialComposeVideoPlayer, mediaUriState.value!!)
                mediaPlayer.isLooping = true
                mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
                    // Keeps the width of the video locked to 600dp and updates the height to match
                    // video
                    // aspect ratio.
                    videoHeight = videoWidth * height / width
                }
                mediaPlayer.setOnPreparedListener { mediaPlayer.start() }
                mediaPlayer.setSurface(it)
            }

            onSurfaceDestroyed { mediaPlayer.release() }

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
                        if (isPaused) mediaPlayer.start() else mediaPlayer.pause()
                        isPaused = !isPaused
                    }
                ) {
                    Text(text = if (isPaused) "Play" else "Pause")
                }
            }
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
                        surfaceEntity!!.canvasShape = SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f)
                        // Move the Quad-shaped canvas to a spot in front of the User.
                        surfaceEntity!!.setPose(
                            session.scene.spatialUser.head?.transformPoseTo(
                                Pose(
                                    Vector3(0.0f, 0.0f, -1.5f),
                                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                                ),
                                session.scene.activitySpace,
                            )!!
                        )
                    }
                ) {
                    Text(text = "Set Quad", fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        surfaceEntity!!.canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(1.0f)
                    }
                ) {
                    Text(text = "Set Vr360", fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        surfaceEntity!!.canvasShape =
                            SurfaceEntity.CanvasShape.Vr180Hemisphere(1.0f)
                    }
                ) {
                    Text(text = "Set Vr180", fontSize = 10.sp)
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
            }
        }
    }

    fun releaseMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        videoPlayingState.value = false
        surfaceEntity?.dispose()
    }

    fun getCanvasAspectRatio(stereoMode: Int, videoWidth: Int, videoHeight: Int): Dimensions {
        when (stereoMode) {
            SurfaceEntity.StereoMode.MONO ->
                return Dimensions(1.0f, videoHeight.toFloat() / videoWidth, 0.0f)
            SurfaceEntity.StereoMode.TOP_BOTTOM ->
                return Dimensions(1.0f, 0.5f * videoHeight.toFloat() / videoWidth, 0.0f)
            SurfaceEntity.StereoMode.SIDE_BY_SIDE ->
                return Dimensions(1.0f, 2.0f * videoHeight.toFloat() / videoWidth, 0.0f)
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
                if (!videoPlaying) {
                    // High level testcases
                    Button(onClick = { menuState.value = VideoMenuState.HOME }) {
                        Text("Main Menu")
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
}
